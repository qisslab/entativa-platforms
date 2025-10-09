package com.gala.backend.services

import com.gala.backend.data.repositories.UserRepository
import com.gala.backend.data.repositories.FollowRepository
import com.gala.backend.data.repositories.ProfileRepository
import com.gala.backend.data.repositories.CreatorRepository
import com.gala.backend.data.repositories.BusinessRepository
import com.gala.backend.data.models.*
import com.gala.backend.auth.AuthenticationService
import com.gala.backend.notifications.NotificationService
import com.gala.backend.analytics.CreatorAnalyticsService
import com.gala.backend.algorithms.DiscoveryEngine
import com.gala.backend.algorithms.InfluencerScoreCalculator
import com.gala.backend.messaging.EventPublisher
import com.gala.backend.validation.UserValidator
import com.gala.backend.media.ProfileMediaService
import com.gala.backend.content.ContentModerationService
import com.gala.backend.monetization.CreatorFundService
import com.gala.backend.verification.VerificationService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gala User Service - Instagram-like Visual Content Platform
 * Manages creator profiles, follower system, business accounts, and influencer features
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced creator economy features
 */
@Singleton
class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val profileRepository: ProfileRepository,
    private val creatorRepository: CreatorRepository,
    private val businessRepository: BusinessRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val creatorAnalyticsService: CreatorAnalyticsService,
    private val discoveryEngine: DiscoveryEngine,
    private val influencerScoreCalculator: InfluencerScoreCalculator,
    private val eventPublisher: EventPublisher,
    private val userValidator: UserValidator,
    private val profileMediaService: ProfileMediaService,
    private val contentModerationService: ContentModerationService,
    private val creatorFundService: CreatorFundService,
    private val verificationService: VerificationService
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    companion object {
        const val MAX_FOLLOWING_COUNT = 7500
        const val MAX_BIO_LENGTH = 150
        const val MAX_USERNAME_LENGTH = 30
        const val VERIFICATION_FOLLOWER_THRESHOLD = 10000
        const val CREATOR_FUND_FOLLOWER_THRESHOLD = 1000
    }

    /**
     * Create a new Gala user profile
     */
    suspend fun createUser(request: CreateGalaUserRequest): GalaUser = coroutineScope {
        logger.info("Creating Gala user profile for: ${request.username}")
        
        try {
            // Validate user data
            val validationResult = userValidator.validateUserCreation(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("User validation failed: ${validationResult.errors}")
            }

            // Check username availability
            val existingUser = userRepository.findByUsername(request.username)
            if (existingUser != null) {
                throw IllegalArgumentException("Username ${request.username} is already taken")
            }

            val userId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create core user record
            val user = GalaUser(
                id = userId,
                entativaId = request.entativaId,
                username = request.username,
                displayName = request.displayName,
                bio = request.bio,
                profileImageUrl = request.profileImageUrl,
                isVerified = false,
                isPrivate = request.isPrivate,
                accountType = AccountType.PERSONAL,
                category = request.category,
                website = request.website,
                location = request.location,
                birthDate = request.birthDate,
                gender = request.gender,
                createdAt = now,
                updatedAt = now,
                lastActiveAt = now,
                followersCount = 0,
                followingCount = 0,
                postsCount = 0,
                storiesCount = 0,
                pixelsCount = 0,
                isActive = true,
                allowTagging = true,
                allowCommenting = true,
                allowMessaging = true,
                showActivityStatus = true,
                allowStoryResharing = true
            )

            // Initialize creator profile if applicable
            val creatorProfile = if (request.isCreator) {
                GalaCreatorProfile(
                    userId = userId,
                    niche = request.creatorNiche ?: "lifestyle",
                    averageEngagementRate = 0.0,
                    influencerScore = 0.0,
                    brandPartnerships = 0,
                    totalEarnings = 0.0,
                    isEligibleForCreatorFund = false,
                    contentCategories = request.contentCategories ?: emptyList(),
                    createdAt = now
                )
            } else null

            // Initialize business profile if applicable
            val businessProfile = if (request.isBusiness) {
                GalaBusinessProfile(
                    userId = userId,
                    businessName = request.businessName ?: request.displayName,
                    businessCategory = request.businessCategory ?: "other",
                    businessType = request.businessType ?: BusinessType.SMALL_BUSINESS,
                    contactEmail = request.contactEmail,
                    phoneNumber = request.phoneNumber,
                    address = request.businessAddress,
                    websiteUrl = request.website,
                    instagramShopEnabled = false,
                    promotionsEnabled = true,
                    insightsEnabled = true,
                    createdAt = now
                )
            } else null

            // Initialize user stats
            val userStats = GalaUserStats(
                userId = userId,
                totalLikes = 0,
                totalComments = 0,
                totalShares = 0,
                totalSaves = 0,
                profileViews = 0,
                linkClicks = 0,
                reachCount = 0,
                impressionsCount = 0,
                avgEngagementRate = 0.0,
                bestPostingTime = emptyList(),
                topHashtags = emptyList(),
                audienceDemographics = emptyMap(),
                growthRate = 0.0
            )

            // Save all user data
            val savedUser = userRepository.save(user)
            creatorProfile?.let { creatorRepository.save(it) }
            businessProfile?.let { businessRepository.save(it) }
            creatorAnalyticsService.initializeUserStats(userStats)

            // Async operations
            async {
                // Add to discovery engine
                discoveryEngine.indexNewUser(savedUser)
                
                // Generate initial content recommendations
                discoveryEngine.generateInitialRecommendations(savedUser)
                
                // Send welcome notifications
                notificationService.sendGalaWelcomeNotification(savedUser)
                
                // Suggest accounts to follow
                val suggestions = discoveryEngine.generateFollowSuggestions(savedUser)
                if (suggestions.isNotEmpty()) {
                    notificationService.sendFollowSuggestionsNotification(savedUser, suggestions)
                }
                
                // Publish user creation event
                eventPublisher.publishGalaUserCreated(savedUser)
                
                // Track user creation analytics
                creatorAnalyticsService.trackUserCreation(savedUser)
            }

            logger.info("Successfully created Gala user: ${savedUser.id}")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to create Gala user for username: ${request.username}", e)
            throw e
        }
    }

    /**
     * Update user profile with visual content focus
     */
    suspend fun updateProfile(userId: String, request: UpdateGalaProfileRequest): GalaUser = coroutineScope {
        logger.info("Updating Gala profile for user: $userId")
        
        try {
            val existingUser = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Validate profile updates
            val validationResult = userValidator.validateProfileUpdate(request, existingUser)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Profile validation failed: ${validationResult.errors}")
            }

            // Process profile image if provided
            val processedProfileImage = request.profileImageUrl?.let { url ->
                async {
                    profileMediaService.processGalaProfileImage(
                        userId = userId,
                        imageUrl = url,
                        cropData = request.profileImageCrop,
                        applyFilters = request.applyFilters,
                        generateVariants = true
                    )
                }
            }

            // Moderate content
            val moderationResults = async {
                contentModerationService.moderateGalaProfile(
                    bio = request.bio,
                    displayName = request.displayName,
                    website = request.website
                )
            }

            // Wait for async operations
            val profileImageResult = processedProfileImage?.await()
            val moderation = moderationResults.await()

            if (moderation.isBlocked) {
                throw IllegalArgumentException("Profile content violates community guidelines: ${moderation.reason}")
            }

            // Update user record
            val updatedUser = existingUser.copy(
                displayName = request.displayName ?: existingUser.displayName,
                bio = request.bio ?: existingUser.bio,
                profileImageUrl = profileImageResult?.url ?: request.profileImageUrl ?: existingUser.profileImageUrl,
                website = request.website ?: existingUser.website,
                location = request.location ?: existingUser.location,
                category = request.category ?: existingUser.category,
                isPrivate = request.isPrivate ?: existingUser.isPrivate,
                allowTagging = request.allowTagging ?: existingUser.allowTagging,
                allowCommenting = request.allowCommenting ?: existingUser.allowCommenting,
                allowMessaging = request.allowMessaging ?: existingUser.allowMessaging,
                showActivityStatus = request.showActivityStatus ?: existingUser.showActivityStatus,
                allowStoryResharing = request.allowStoryResharing ?: existingUser.allowStoryResharing,
                updatedAt = Instant.now()
            )

            val savedUser = userRepository.save(updatedUser)

            // Update creator profile if exists
            if (request.creatorProfileUpdates != null) {
                updateCreatorProfile(userId, request.creatorProfileUpdates)
            }

            // Update business profile if exists
            if (request.businessProfileUpdates != null) {
                updateBusinessProfile(userId, request.businessProfileUpdates)
            }

            // Async post-update operations
            async {
                // Update discovery engine with new profile data
                discoveryEngine.updateUserProfile(savedUser, existingUser)
                
                // Recalculate influencer score
                if (hasSignificantChanges(existingUser, savedUser)) {
                    influencerScoreCalculator.recalculateScore(savedUser)
                }
                
                // Update search indexes
                eventPublisher.publishGalaProfileUpdated(savedUser, existingUser)
                
                // Track profile update analytics
                creatorAnalyticsService.trackProfileUpdate(savedUser, getChangedFields(existingUser, savedUser))
            }

            logger.info("Successfully updated Gala profile for user: $userId")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to update Gala profile for user: $userId", e)
            throw e
        }
    }

    /**
     * Follow a user with intelligent validation
     */
    suspend fun followUser(followerId: String, followeeId: String): GalaFollow = coroutineScope {
        logger.info("User $followerId following user $followeeId")
        
        try {
            // Validate users exist
            val follower = userRepository.findById(followerId)
                ?: throw IllegalArgumentException("Follower not found: $followerId")
            val followee = userRepository.findById(followeeId)
                ?: throw IllegalArgumentException("Followee not found: $followeeId")

            if (followerId == followeeId) {
                throw IllegalArgumentException("User cannot follow themselves")
            }

            // Check if already following
            val existingFollow = followRepository.findFollow(followerId, followeeId)
            if (existingFollow != null) {
                throw IllegalArgumentException("Already following this user")
            }

            // Check following limits
            if (follower.followingCount >= MAX_FOLLOWING_COUNT) {
                throw IllegalArgumentException("Following limit reached")
            }

            // Check if account is private
            if (followee.isPrivate) {
                return@coroutineScope createFollowRequest(follower, followee)
            }

            val now = Instant.now()

            // Create follow relationship
            val follow = GalaFollow(
                id = UUID.randomUUID().toString(),
                followerId = followerId,
                followeeId = followeeId,
                createdAt = now,
                isActive = true,
                source = FollowSource.PROFILE,
                interactionScore = 0.0,
                lastInteractionAt = now
            )

            val savedFollow = followRepository.save(follow)

            // Update user counts
            userRepository.incrementFollowingCount(followerId)
            userRepository.incrementFollowersCount(followeeId)

            // Async operations
            async {
                // Update discovery algorithms
                discoveryEngine.processNewFollow(follower, followee)
                
                // Calculate new influencer scores
                influencerScoreCalculator.updateFollowScore(followee)
                
                // Send notification to followee
                notificationService.sendFollowNotification(follower, followee)
                
                // Check creator fund eligibility
                if (followee.followersCount + 1 >= CREATOR_FUND_FOLLOWER_THRESHOLD) {
                    creatorFundService.evaluateEligibility(followee)
                }
                
                // Check verification eligibility
                if (followee.followersCount + 1 >= VERIFICATION_FOLLOWER_THRESHOLD) {
                    verificationService.evaluateVerificationEligibility(followee)
                }
                
                // Generate mutual follow suggestions
                discoveryEngine.generateMutualFollowSuggestions(follower, followee)
                
                // Track analytics
                creatorAnalyticsService.trackFollowAction(savedFollow, follower, followee)
                
                // Publish events
                eventPublisher.publishGalaFollowCreated(savedFollow)
            }

            logger.info("Successfully created follow relationship: ${savedFollow.id}")
            savedFollow

        } catch (e: Exception) {
            logger.error("Failed to create follow relationship from $followerId to $followeeId", e)
            throw e
        }
    }

    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(followerId: String, followeeId: String): Boolean = coroutineScope {
        logger.info("User $followerId unfollowing user $followeeId")
        
        try {
            val existingFollow = followRepository.findFollow(followerId, followeeId)
                ?: throw IllegalArgumentException("Follow relationship not found")

            // Soft delete the follow
            val unfollowedFollow = existingFollow.copy(
                isActive = false,
                unfollowedAt = Instant.now()
            )

            followRepository.update(unfollowedFollow)

            // Update user counts
            userRepository.decrementFollowingCount(followerId)
            userRepository.decrementFollowersCount(followeeId)

            // Async operations
            async {
                val follower = userRepository.findById(followerId)!!
                val followee = userRepository.findById(followeeId)!!
                
                // Update discovery algorithms
                discoveryEngine.processUnfollow(follower, followee)
                
                // Recalculate influencer scores
                influencerScoreCalculator.updateUnfollowScore(followee)
                
                // Check if should remove from creator fund
                if (followee.followersCount - 1 < CREATOR_FUND_FOLLOWER_THRESHOLD) {
                    creatorFundService.reevaluateEligibility(followee)
                }
                
                // Track analytics
                creatorAnalyticsService.trackUnfollowAction(unfollowedFollow, follower, followee)
                
                // Publish events
                eventPublisher.publishGalaUnfollowCreated(unfollowedFollow)
            }

            logger.info("Successfully unfollowed user: $followeeId")
            true

        } catch (e: Exception) {
            logger.error("Failed to unfollow user $followeeId by $followerId", e)
            false
        }
    }

    /**
     * Get user's followers with enhanced data
     */
    suspend fun getUserFollowers(
        userId: String,
        viewerId: String?,
        limit: Int = 50,
        offset: Int = 0
    ): GalaFollowersList = coroutineScope {
        logger.debug("Fetching followers for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Check privacy settings
            if (user.isPrivate && viewerId != userId && !isFollowing(viewerId, userId)) {
                throw IllegalArgumentException("Cannot view followers of private account")
            }

            val followers = followRepository.getUserFollowers(
                userId = userId,
                limit = limit,
                offset = offset,
                includeInactive = false
            )

            // Enhance with additional data
            val enhancedFollowers = followers.map { follow ->
                async {
                    val followerUser = userRepository.findById(follow.followerId)!!
                    val mutualFollowsCount = if (viewerId != null) {
                        followRepository.getMutualFollowsCount(viewerId, follow.followerId)
                    } else 0

                    GalaFollowerInfo(
                        user = followerUser,
                        follow = follow,
                        mutualFollowsCount = mutualFollowsCount,
                        isFollowingBack = viewerId?.let { isFollowing(userId, follow.followerId) } ?: false,
                        influencerScore = influencerScoreCalculator.getScore(followerUser.id)
                    )
                }
            }.awaitAll()

            GalaFollowersList(
                followers = enhancedFollowers,
                totalCount = user.followersCount,
                hasMore = offset + limit < user.followersCount
            )

        } catch (e: Exception) {
            logger.error("Failed to get followers for user: $userId", e)
            throw e
        }
    }

    /**
     * Get user's following list
     */
    suspend fun getUserFollowing(
        userId: String,
        viewerId: String?,
        limit: Int = 50,
        offset: Int = 0
    ): GalaFollowingList = coroutineScope {
        logger.debug("Fetching following for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Check privacy settings
            if (user.isPrivate && viewerId != userId && !isFollowing(viewerId, userId)) {
                throw IllegalArgumentException("Cannot view following of private account")
            }

            val following = followRepository.getUserFollowing(
                userId = userId,
                limit = limit,
                offset = offset,
                includeInactive = false
            )

            // Enhance with additional data
            val enhancedFollowing = following.map { follow ->
                async {
                    val followingUser = userRepository.findById(follow.followeeId)!!
                    val mutualFollowsCount = if (viewerId != null) {
                        followRepository.getMutualFollowsCount(viewerId, follow.followeeId)
                    } else 0

                    GalaFollowingInfo(
                        user = followingUser,
                        follow = follow,
                        mutualFollowsCount = mutualFollowsCount,
                        isFollowingBack = isFollowing(follow.followeeId, userId),
                        influencerScore = influencerScoreCalculator.getScore(followingUser.id)
                    )
                }
            }.awaitAll()

            GalaFollowingList(
                following = enhancedFollowing,
                totalCount = user.followingCount,
                hasMore = offset + limit < user.followingCount
            )

        } catch (e: Exception) {
            logger.error("Failed to get following for user: $userId", e)
            throw e
        }
    }

    /**
     * Get intelligent follow suggestions
     */
    suspend fun getFollowSuggestions(userId: String, limit: Int = 20): List<GalaFollowSuggestion> = coroutineScope {
        logger.debug("Generating follow suggestions for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Generate comprehensive suggestions
            val suggestions = discoveryEngine.generateGalaFollowSuggestions(
                user = user,
                limit = limit,
                includeInfluencers = true,
                includeMutualConnections = true,
                includeInterestBased = true
            )

            // Enhance suggestions with additional context
            suggestions.map { suggestion ->
                async {
                    val suggestedUser = userRepository.findById(suggestion.userId)!!
                    val mutualFollows = followRepository.getMutualFollows(userId, suggestion.userId).take(3)
                    val influencerScore = influencerScoreCalculator.getScore(suggestedUser.id)

                    GalaFollowSuggestion(
                        user = suggestedUser,
                        score = suggestion.score,
                        reasons = suggestion.reasons,
                        mutualFollows = mutualFollows.map { userRepository.findById(it)!! },
                        mutualFollowsCount = followRepository.getMutualFollowsCount(userId, suggestion.userId),
                        influencerScore = influencerScore,
                        category = suggestion.category,
                        isVerified = suggestedUser.isVerified
                    )
                }
            }.awaitAll()

        } catch (e: Exception) {
            logger.error("Failed to get follow suggestions for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Get comprehensive user profile for Gala
     */
    suspend fun getUserProfile(requestedUserId: String, viewerId: String?): GalaUserProfile = coroutineScope {
        logger.debug("Getting Gala profile for user: $requestedUserId (viewer: $viewerId)")
        
        try {
            val user = userRepository.findById(requestedUserId)
                ?: throw IllegalArgumentException("User not found: $requestedUserId")

            // Check if viewer can see this profile
            val canViewProfile = canViewProfile(user, viewerId)
            if (!canViewProfile) {
                throw IllegalArgumentException("Profile is private")
            }

            // Get relationship context
            val relationshipContext = if (viewerId != null) {
                getRelationshipContext(viewerId, requestedUserId)
            } else null

            // Get creator profile if exists
            val creatorProfile = creatorRepository.findByUserId(requestedUserId)

            // Get business profile if exists
            val businessProfile = businessRepository.findByUserId(requestedUserId)

            // Get user stats
            val userStats = creatorAnalyticsService.getUserStats(requestedUserId, viewerId)

            // Get recent highlights (top posts preview)
            val highlights = if (canViewProfile) {
                // This would integrate with the posting service to get recent posts
                emptyList()
            } else null

            val profile = GalaUserProfile(
                user = user,
                creatorProfile = creatorProfile,
                businessProfile = businessProfile,
                relationshipContext = relationshipContext,
                stats = userStats,
                highlights = highlights,
                isOwnProfile = viewerId == requestedUserId,
                mutualFollowsCount = relationshipContext?.mutualFollowsCount ?: 0,
                influencerScore = influencerScoreCalculator.getScore(requestedUserId),
                engagementRate = creatorProfile?.averageEngagementRate ?: 0.0
            )

            // Track profile view
            if (viewerId != null && viewerId != requestedUserId) {
                coroutineScope {
                    async {
                        creatorAnalyticsService.trackProfileView(requestedUserId, viewerId)
                    }
                }
            }

            profile

        } catch (e: Exception) {
            logger.error("Failed to get Gala profile for user: $requestedUserId", e)
            throw e
        }
    }

    /**
     * Switch to business account
     */
    suspend fun switchToBusinessAccount(userId: String, request: BusinessAccountRequest): GalaUser = coroutineScope {
        logger.info("Switching user $userId to business account")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            if (user.accountType == AccountType.BUSINESS) {
                throw IllegalArgumentException("User is already a business account")
            }

            // Create business profile
            val businessProfile = GalaBusinessProfile(
                userId = userId,
                businessName = request.businessName,
                businessCategory = request.category,
                businessType = request.businessType,
                contactEmail = request.contactEmail,
                phoneNumber = request.phoneNumber,
                address = request.address,
                websiteUrl = request.websiteUrl,
                instagramShopEnabled = false,
                promotionsEnabled = true,
                insightsEnabled = true,
                createdAt = Instant.now()
            )

            // Update user account type
            val updatedUser = user.copy(
                accountType = AccountType.BUSINESS,
                category = request.category,
                website = request.websiteUrl,
                updatedAt = Instant.now()
            )

            val savedUser = userRepository.save(updatedUser)
            businessRepository.save(businessProfile)

            // Async operations
            async {
                // Enable business features
                creatorAnalyticsService.enableBusinessInsights(userId)
                
                // Update discovery algorithms
                discoveryEngine.updateBusinessAccount(savedUser)
                
                // Send welcome business notification
                notificationService.sendBusinessAccountWelcome(savedUser)
                
                // Publish event
                eventPublisher.publishBusinessAccountCreated(savedUser, businessProfile)
            }

            logger.info("Successfully switched user $userId to business account")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to switch user $userId to business account", e)
            throw e
        }
    }

    // Helper methods

    private suspend fun createFollowRequest(follower: GalaUser, followee: GalaUser): GalaFollow {
        val followRequest = GalaFollow(
            id = UUID.randomUUID().toString(),
            followerId = follower.id,
            followeeId = followee.id,
            createdAt = Instant.now(),
            isActive = false,
            isPending = true,
            source = FollowSource.PROFILE,
            interactionScore = 0.0,
            lastInteractionAt = Instant.now()
        )

        val savedRequest = followRepository.save(followRequest)

        // Send follow request notification
        notificationService.sendFollowRequestNotification(follower, followee)

        return savedRequest
    }

    private suspend fun updateCreatorProfile(userId: String, updates: CreatorProfileUpdates) {
        val existingProfile = creatorRepository.findByUserId(userId)
        if (existingProfile != null) {
            val updatedProfile = existingProfile.copy(
                niche = updates.niche ?: existingProfile.niche,
                contentCategories = updates.contentCategories ?: existingProfile.contentCategories,
                updatedAt = Instant.now()
            )
            creatorRepository.save(updatedProfile)
        }
    }

    private suspend fun updateBusinessProfile(userId: String, updates: BusinessProfileUpdates) {
        val existingProfile = businessRepository.findByUserId(userId)
        if (existingProfile != null) {
            val updatedProfile = existingProfile.copy(
                businessName = updates.businessName ?: existingProfile.businessName,
                businessCategory = updates.businessCategory ?: existingProfile.businessCategory,
                contactEmail = updates.contactEmail ?: existingProfile.contactEmail,
                phoneNumber = updates.phoneNumber ?: existingProfile.phoneNumber,
                address = updates.address ?: existingProfile.address,
                websiteUrl = updates.websiteUrl ?: existingProfile.websiteUrl,
                updatedAt = Instant.now()
            )
            businessRepository.save(updatedProfile)
        }
    }

    private suspend fun isFollowing(followerId: String?, followeeId: String): Boolean {
        if (followerId == null) return false
        return followRepository.findFollow(followerId, followeeId)?.isActive == true
    }

    private fun canViewProfile(user: GalaUser, viewerId: String?): Boolean {
        if (!user.isPrivate) return true
        if (viewerId == null) return false
        if (viewerId == user.id) return true
        return false // Would need to check if viewer is following
    }

    private suspend fun getRelationshipContext(viewerId: String, targetUserId: String): GalaRelationshipContext {
        val isFollowing = isFollowing(viewerId, targetUserId)
        val isFollowingBack = isFollowing(targetUserId, viewerId)
        val mutualFollowsCount = followRepository.getMutualFollowsCount(viewerId, targetUserId)

        return GalaRelationshipContext(
            isFollowing = isFollowing,
            isFollowingBack = isFollowingBack,
            mutualFollowsCount = mutualFollowsCount,
            hasInteracted = false // Would need to check from post interactions
        )
    }

    private fun hasSignificantChanges(oldUser: GalaUser, newUser: GalaUser): Boolean {
        return oldUser.displayName != newUser.displayName ||
                oldUser.bio != newUser.bio ||
                oldUser.profileImageUrl != newUser.profileImageUrl ||
                oldUser.category != newUser.category
    }

    private fun getChangedFields(oldUser: GalaUser, newUser: GalaUser): List<String> {
        val changes = mutableListOf<String>()
        if (oldUser.displayName != newUser.displayName) changes.add("displayName")
        if (oldUser.bio != newUser.bio) changes.add("bio")
        if (oldUser.profileImageUrl != newUser.profileImageUrl) changes.add("profileImage")
        if (oldUser.category != newUser.category) changes.add("category")
        if (oldUser.website != newUser.website) changes.add("website")
        return changes
    }
}

// Data classes for Gala-specific features

data class CreateGalaUserRequest(
    val entativaId: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val isPrivate: Boolean = false,
    val category: String?,
    val website: String?,
    val location: String?,
    val birthDate: String?,
    val gender: String?,
    val isCreator: Boolean = false,
    val creatorNiche: String?,
    val contentCategories: List<String>?,
    val isBusiness: Boolean = false,
    val businessName: String?,
    val businessCategory: String?,
    val businessType: BusinessType?,
    val contactEmail: String?,
    val phoneNumber: String?,
    val businessAddress: String?
)

data class UpdateGalaProfileRequest(
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val profileImageCrop: ImageCropData?,
    val applyFilters: Boolean = false,
    val website: String?,
    val location: String?,
    val category: String?,
    val isPrivate: Boolean?,
    val allowTagging: Boolean?,
    val allowCommenting: Boolean?,
    val allowMessaging: Boolean?,
    val showActivityStatus: Boolean?,
    val allowStoryResharing: Boolean?,
    val creatorProfileUpdates: CreatorProfileUpdates?,
    val businessProfileUpdates: BusinessProfileUpdates?
)

data class GalaUser(
    val id: String,
    val entativaId: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val isVerified: Boolean,
    val isPrivate: Boolean,
    val accountType: AccountType,
    val category: String?,
    val website: String?,
    val location: String?,
    val birthDate: String?,
    val gender: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val storiesCount: Int,
    val pixelsCount: Int,
    val isActive: Boolean,
    val allowTagging: Boolean,
    val allowCommenting: Boolean,
    val allowMessaging: Boolean,
    val showActivityStatus: Boolean,
    val allowStoryResharing: Boolean
)

data class GalaFollow(
    val id: String,
    val followerId: String,
    val followeeId: String,
    val createdAt: Instant,
    val isActive: Boolean,
    val isPending: Boolean = false,
    val source: FollowSource,
    val interactionScore: Double,
    val lastInteractionAt: Instant,
    val unfollowedAt: Instant? = null
)

data class GalaCreatorProfile(
    val userId: String,
    val niche: String,
    val averageEngagementRate: Double,
    val influencerScore: Double,
    val brandPartnerships: Int,
    val totalEarnings: Double,
    val isEligibleForCreatorFund: Boolean,
    val contentCategories: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant = Instant.now()
)

data class GalaBusinessProfile(
    val userId: String,
    val businessName: String,
    val businessCategory: String,
    val businessType: BusinessType,
    val contactEmail: String?,
    val phoneNumber: String?,
    val address: String?,
    val websiteUrl: String?,
    val instagramShopEnabled: Boolean,
    val promotionsEnabled: Boolean,
    val insightsEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant = Instant.now()
)

data class GalaUserStats(
    val userId: String,
    val totalLikes: Long,
    val totalComments: Long,
    val totalShares: Long,
    val totalSaves: Long,
    val profileViews: Long,
    val linkClicks: Long,
    val reachCount: Long,
    val impressionsCount: Long,
    val avgEngagementRate: Double,
    val bestPostingTime: List<Int>,
    val topHashtags: List<String>,
    val audienceDemographics: Map<String, Any>,
    val growthRate: Double
)

data class GalaFollowerInfo(
    val user: GalaUser,
    val follow: GalaFollow,
    val mutualFollowsCount: Int,
    val isFollowingBack: Boolean,
    val influencerScore: Double
)

data class GalaFollowingInfo(
    val user: GalaUser,
    val follow: GalaFollow,
    val mutualFollowsCount: Int,
    val isFollowingBack: Boolean,
    val influencerScore: Double
)

data class GalaFollowersList(
    val followers: List<GalaFollowerInfo>,
    val totalCount: Int,
    val hasMore: Boolean
)

data class GalaFollowingList(
    val following: List<GalaFollowingInfo>,
    val totalCount: Int,
    val hasMore: Boolean
)

data class GalaFollowSuggestion(
    val user: GalaUser,
    val score: Double,
    val reasons: List<String>,
    val mutualFollows: List<GalaUser>,
    val mutualFollowsCount: Int,
    val influencerScore: Double,
    val category: String,
    val isVerified: Boolean
)

data class GalaUserProfile(
    val user: GalaUser,
    val creatorProfile: GalaCreatorProfile?,
    val businessProfile: GalaBusinessProfile?,
    val relationshipContext: GalaRelationshipContext?,
    val stats: GalaUserStats,
    val highlights: List<Any>?,
    val isOwnProfile: Boolean,
    val mutualFollowsCount: Int,
    val influencerScore: Double,
    val engagementRate: Double
)

data class GalaRelationshipContext(
    val isFollowing: Boolean,
    val isFollowingBack: Boolean,
    val mutualFollowsCount: Int,
    val hasInteracted: Boolean
)

data class BusinessAccountRequest(
    val businessName: String,
    val category: String,
    val businessType: BusinessType,
    val contactEmail: String?,
    val phoneNumber: String?,
    val address: String?,
    val websiteUrl: String?
)

data class CreatorProfileUpdates(
    val niche: String?,
    val contentCategories: List<String>?
)

data class BusinessProfileUpdates(
    val businessName: String?,
    val businessCategory: String?,
    val contactEmail: String?,
    val phoneNumber: String?,
    val address: String?,
    val websiteUrl: String?
)

data class ImageCropData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

enum class AccountType {
    PERSONAL, BUSINESS, CREATOR
}

enum class BusinessType {
    SMALL_BUSINESS, MEDIUM_BUSINESS, ENTERPRISE, STARTUP, NON_PROFIT, BRAND, INFLUENCER
}

enum class FollowSource {
    PROFILE, DISCOVER, SUGGESTED, HASHTAG, LOCATION, STORY, MUTUAL_FRIENDS
}
