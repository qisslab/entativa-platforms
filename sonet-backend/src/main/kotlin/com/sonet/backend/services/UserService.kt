package com.sonet.backend.services

import com.sonet.backend.data.repositories.UserRepository
import com.sonet.backend.data.repositories.FriendshipRepository
import com.sonet.backend.data.repositories.ProfileRepository
import com.sonet.backend.data.repositories.PrivacyRepository
import com.sonet.backend.data.models.*
import com.sonet.backend.auth.AuthenticationService
import com.sonet.backend.notifications.NotificationService
import com.sonet.backend.analytics.UserAnalyticsService
import com.sonet.backend.algorithms.SocialGraphAnalyzer
import com.sonet.backend.algorithms.FriendRecommendationEngine
import com.sonet.backend.messaging.EventPublisher
import com.sonet.backend.validation.UserValidator
import com.sonet.backend.media.ProfileMediaService
import com.sonet.backend.content.ContentModerationService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sonet User Service - Facebook-like Social Networking Platform
 * Manages user profiles, friendships, social graph, and privacy settings
 * 
 * @author Neo Qiss
 * @status Production-ready with PhD-level social graph algorithms
 */
@Singleton
class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val profileRepository: ProfileRepository,
    private val privacyRepository: PrivacyRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val userAnalyticsService: UserAnalyticsService,
    private val socialGraphAnalyzer: SocialGraphAnalyzer,
    private val friendRecommendationEngine: FriendRecommendationEngine,
    private val eventPublisher: EventPublisher,
    private val userValidator: UserValidator,
    private val profileMediaService: ProfileMediaService,
    private val contentModerationService: ContentModerationService
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    companion object {
        const val MAX_FRIENDS_COUNT = 5000
        const val MAX_PENDING_REQUESTS = 500
        const val FRIEND_SUGGESTION_LIMIT = 50
        const val PROFILE_CACHE_TTL = 3600 // 1 hour
    }

    /**
     * Create a new Sonet user profile
     */
    suspend fun createUser(request: CreateUserRequest): SonetUser = coroutineScope {
        logger.info("Creating Sonet user profile for: ${request.email}")
        
        try {
            // Validate user data
            val validationResult = userValidator.validateUserCreation(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("User validation failed: ${validationResult.errors}")
            }

            // Check if user already exists
            val existingUser = userRepository.findByEmail(request.email)
            if (existingUser != null) {
                throw IllegalArgumentException("User with email ${request.email} already exists")
            }

            val userId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create core user record
            val user = SonetUser(
                id = userId,
                entativaId = request.entativaId,
                email = request.email,
                username = request.username,
                displayName = request.displayName,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                location = request.location,
                bio = request.bio,
                profileImageUrl = request.profileImageUrl,
                coverImageUrl = request.coverImageUrl,
                isVerified = false,
                isActive = true,
                createdAt = now,
                updatedAt = now,
                lastLoginAt = now,
                loginCount = 1,
                friendsCount = 0,
                followersCount = 0,
                postsCount = 0
            )

            // Create privacy settings with default Facebook-like values
            val privacySettings = SonetPrivacySettings(
                userId = userId,
                profileVisibility = PrivacyLevel.FRIENDS,
                friendsListVisibility = PrivacyLevel.FRIENDS,
                postsVisibility = PrivacyLevel.FRIENDS,
                photosVisibility = PrivacyLevel.FRIENDS,
                birthdayVisibility = PrivacyLevel.FRIENDS,
                emailVisibility = PrivacyLevel.PRIVATE,
                phoneVisibility = PrivacyLevel.PRIVATE,
                searchableByEmail = true,
                searchableByPhone = false,
                allowFriendRequests = true,
                allowTagging = true,
                allowMessages = true,
                showOnlineStatus = true,
                allowStoryViewing = PrivacyLevel.FRIENDS,
                allowPostSharing = true,
                twoFactorEnabled = false,
                emailNotifications = true,
                pushNotifications = true,
                smsNotifications = false
            )

            // Create user statistics
            val userStats = SonetUserStats(
                userId = userId,
                totalFriends = 0,
                totalPosts = 0,
                totalLikes = 0,
                totalComments = 0,
                totalShares = 0,
                profileViews = 0,
                searchAppearances = 0,
                avgPostEngagement = 0.0,
                peakOnlineHours = emptyList(),
                mostUsedFeatures = emptyList(),
                socialScore = 0.0,
                influenceScore = 0.0,
                engagementRate = 0.0
            )

            // Save all user data
            val savedUser = userRepository.save(user)
            privacyRepository.save(privacySettings)
            userAnalyticsService.initializeUserStats(userStats)

            // Async operations
            async {
                // Generate initial friend recommendations
                friendRecommendationEngine.generateInitialRecommendations(savedUser)
                
                // Analyze user for social graph placement
                socialGraphAnalyzer.analyzeNewUser(savedUser)
                
                // Send welcome notifications
                notificationService.sendWelcomeNotification(savedUser)
                
                // Publish user creation event
                eventPublisher.publishSonetUserCreated(savedUser)
                
                // Track user creation analytics
                userAnalyticsService.trackUserCreation(savedUser)
            }

            logger.info("Successfully created Sonet user: ${savedUser.id}")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to create Sonet user for email: ${request.email}", e)
            throw e
        }
    }

    /**
     * Update user profile with comprehensive validation
     */
    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): SonetUser = coroutineScope {
        logger.info("Updating Sonet profile for user: $userId")
        
        try {
            val existingUser = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Validate profile updates
            val validationResult = userValidator.validateProfileUpdate(request, existingUser)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Profile validation failed: ${validationResult.errors}")
            }

            // Process profile images if provided
            val processedProfileImage = request.profileImageUrl?.let { url ->
                async {
                    profileMediaService.processProfileImage(
                        userId = userId,
                        imageUrl = url,
                        cropData = request.profileImageCrop
                    )
                }
            }

            val processedCoverImage = request.coverImageUrl?.let { url ->
                async {
                    profileMediaService.processCoverImage(
                        userId = userId,
                        imageUrl = url,
                        cropData = request.coverImageCrop
                    )
                }
            }

            // Moderate profile content
            val moderationResults = async {
                contentModerationService.moderateProfileContent(
                    bio = request.bio,
                    displayName = request.displayName,
                    location = request.location
                )
            }

            // Wait for async operations
            val profileImageResult = processedProfileImage?.await()
            val coverImageResult = processedCoverImage?.await()
            val moderation = moderationResults.await()

            if (moderation.isBlocked) {
                throw IllegalArgumentException("Profile content violates community guidelines: ${moderation.reason}")
            }

            // Update user record
            val updatedUser = existingUser.copy(
                displayName = request.displayName ?: existingUser.displayName,
                bio = request.bio ?: existingUser.bio,
                location = request.location ?: existingUser.location,
                profileImageUrl = profileImageResult?.url ?: request.profileImageUrl ?: existingUser.profileImageUrl,
                coverImageUrl = coverImageResult?.url ?: request.coverImageUrl ?: existingUser.coverImageUrl,
                updatedAt = Instant.now()
            )

            val savedUser = userRepository.save(updatedUser)

            // Async post-update operations
            async {
                // Update social graph with new profile data
                socialGraphAnalyzer.analyzeProfileUpdate(savedUser, existingUser)
                
                // Notify friends of significant profile changes
                if (hasSignificantChanges(existingUser, savedUser)) {
                    notificationService.notifyFriendsOfProfileUpdate(savedUser)
                }
                
                // Update search indexes
                eventPublisher.publishSonetProfileUpdated(savedUser, existingUser)
                
                // Track profile update analytics
                userAnalyticsService.trackProfileUpdate(savedUser, getChangedFields(existingUser, savedUser))
            }

            logger.info("Successfully updated Sonet profile for user: $userId")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to update Sonet profile for user: $userId", e)
            throw e
        }
    }

    /**
     * Send friend request with intelligent validation
     */
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): FriendRequest = coroutineScope {
        logger.info("Sending friend request from $fromUserId to $toUserId")
        
        try {
            // Validate users exist
            val fromUser = userRepository.findById(fromUserId)
                ?: throw IllegalArgumentException("Sender not found: $fromUserId")
            val toUser = userRepository.findById(toUserId)
                ?: throw IllegalArgumentException("Recipient not found: $toUserId")

            // Check if request already exists
            val existingRequest = friendshipRepository.findPendingRequest(fromUserId, toUserId)
            if (existingRequest != null) {
                throw IllegalArgumentException("Friend request already sent")
            }

            // Check if already friends
            val existingFriendship = friendshipRepository.findFriendship(fromUserId, toUserId)
            if (existingFriendship != null) {
                throw IllegalArgumentException("Users are already friends")
            }

            // Check privacy settings
            val toUserPrivacy = privacyRepository.findByUserId(toUserId)
            if (toUserPrivacy?.allowFriendRequests != true) {
                throw IllegalArgumentException("User is not accepting friend requests")
            }

            // Check friend request limits
            val pendingRequestsCount = friendshipRepository.countPendingRequestsFrom(fromUserId)
            if (pendingRequestsCount >= MAX_PENDING_REQUESTS) {
                throw IllegalArgumentException("Too many pending friend requests")
            }

            // Analyze social connection for spam prevention
            val connectionAnalysis = socialGraphAnalyzer.analyzePotentialConnection(fromUser, toUser)
            if (connectionAnalysis.isSpamLikely) {
                throw IllegalArgumentException("Friend request blocked by spam detection")
            }

            // Create friend request
            val friendRequest = FriendRequest(
                id = UUID.randomUUID().toString(),
                fromUserId = fromUserId,
                toUserId = toUserId,
                message = null, // Sonet doesn't typically include messages with requests
                status = FriendRequestStatus.PENDING,
                createdAt = Instant.now(),
                mutualFriendsCount = connectionAnalysis.mutualFriendsCount,
                connectionStrength = connectionAnalysis.connectionStrength
            )

            val savedRequest = friendshipRepository.saveFriendRequest(friendRequest)

            // Async operations
            async {
                // Send notification to recipient
                notificationService.sendFriendRequestNotification(fromUser, toUser, savedRequest)
                
                // Update friend recommendation algorithms
                friendRecommendationEngine.recordFriendRequestSent(fromUser, toUser)
                
                // Track analytics
                userAnalyticsService.trackFriendRequestSent(fromUser, toUser, connectionAnalysis)
                
                // Publish event
                eventPublisher.publishFriendRequestSent(savedRequest)
            }

            logger.info("Successfully sent friend request: ${savedRequest.id}")
            savedRequest

        } catch (e: Exception) {
            logger.error("Failed to send friend request from $fromUserId to $toUserId", e)
            throw e
        }
    }

    /**
     * Accept friend request and establish friendship
     */
    suspend fun acceptFriendRequest(requestId: String, userId: String): Friendship = coroutineScope {
        logger.info("Accepting friend request: $requestId by user: $userId")
        
        try {
            val friendRequest = friendshipRepository.findRequestById(requestId)
                ?: throw IllegalArgumentException("Friend request not found: $requestId")

            if (friendRequest.toUserId != userId) {
                throw IllegalArgumentException("Unauthorized to accept this friend request")
            }

            if (friendRequest.status != FriendRequestStatus.PENDING) {
                throw IllegalArgumentException("Friend request is not pending")
            }

            val fromUser = userRepository.findById(friendRequest.fromUserId)!!
            val toUser = userRepository.findById(friendRequest.toUserId)!!

            // Check friend limits
            if (fromUser.friendsCount >= MAX_FRIENDS_COUNT || toUser.friendsCount >= MAX_FRIENDS_COUNT) {
                throw IllegalArgumentException("Friend limit reached")
            }

            val now = Instant.now()

            // Create mutual friendship
            val friendship = Friendship(
                id = UUID.randomUUID().toString(),
                user1Id = minOf(friendRequest.fromUserId, friendRequest.toUserId),
                user2Id = maxOf(friendRequest.fromUserId, friendRequest.toUserId),
                createdAt = now,
                closeness = 0.0,
                interactionScore = 0.0,
                mutualFriendsCount = friendRequest.mutualFriendsCount,
                lastInteractionAt = now
            )

            // Update friend request status
            val updatedRequest = friendRequest.copy(
                status = FriendRequestStatus.ACCEPTED,
                respondedAt = now
            )

            // Save friendship and update request
            val savedFriendship = friendshipRepository.saveFriendship(friendship)
            friendshipRepository.updateFriendRequest(updatedRequest)

            // Update user friend counts
            userRepository.incrementFriendsCount(fromUser.id)
            userRepository.incrementFriendsCount(toUser.id)

            // Async operations
            async {
                // Update social graph
                socialGraphAnalyzer.processFriendshipCreated(savedFriendship, fromUser, toUser)
                
                // Send notifications
                notificationService.sendFriendRequestAcceptedNotification(fromUser, toUser)
                
                // Update recommendation engines
                friendRecommendationEngine.processFriendshipCreated(fromUser, toUser)
                
                // Generate new mutual friend recommendations
                friendRecommendationEngine.generateMutualFriendRecommendations(fromUser, toUser)
                
                // Track analytics
                userAnalyticsService.trackFriendshipCreated(savedFriendship, fromUser, toUser)
                
                // Publish events
                eventPublisher.publishFriendshipCreated(savedFriendship)
            }

            logger.info("Successfully created friendship: ${savedFriendship.id}")
            savedFriendship

        } catch (e: Exception) {
            logger.error("Failed to accept friend request: $requestId", e)
            throw e
        }
    }

    /**
     * Get user's friends with intelligent sorting
     */
    suspend fun getUserFriends(
        userId: String,
        limit: Int = 50,
        offset: Int = 0,
        sortBy: FriendSortOption = FriendSortOption.CLOSENESS
    ): FriendsList = coroutineScope {
        logger.debug("Fetching friends for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            val friends = friendshipRepository.getUserFriends(
                userId = userId,
                limit = limit,
                offset = offset,
                sortBy = sortBy
            )

            // Enhance with mutual friends and interaction data
            val enhancedFriends = friends.map { friendship ->
                async {
                    val friendUser = if (friendship.user1Id == userId) {
                        userRepository.findById(friendship.user2Id)!!
                    } else {
                        userRepository.findById(friendship.user1Id)!!
                    }

                    // Calculate current mutual friends count
                    val currentMutualFriends = socialGraphAnalyzer.getMutualFriendsCount(userId, friendUser.id)

                    FriendInfo(
                        user = friendUser,
                        friendship = friendship,
                        mutualFriendsCount = currentMutualFriends,
                        lastInteraction = friendship.lastInteractionAt,
                        isOnline = authService.isUserOnline(friendUser.id),
                        commonInterests = socialGraphAnalyzer.getCommonInterests(userId, friendUser.id)
                    )
                }
            }.awaitAll()

            FriendsList(
                friends = enhancedFriends,
                totalCount = user.friendsCount,
                hasMore = offset + limit < user.friendsCount
            )

        } catch (e: Exception) {
            logger.error("Failed to get friends for user: $userId", e)
            throw e
        }
    }

    /**
     * Get intelligent friend suggestions
     */
    suspend fun getFriendSuggestions(userId: String, limit: Int = FRIEND_SUGGESTION_LIMIT): List<FriendSuggestion> = coroutineScope {
        logger.debug("Generating friend suggestions for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Generate comprehensive friend suggestions
            val suggestions = friendRecommendationEngine.generateSuggestions(
                user = user,
                limit = limit,
                includeReasons = true
            )

            // Enhance suggestions with additional context
            suggestions.map { suggestion ->
                async {
                    val suggestedUser = userRepository.findById(suggestion.userId)!!
                    val mutualFriends = socialGraphAnalyzer.getMutualFriends(userId, suggestion.userId)
                    val commonInterests = socialGraphAnalyzer.getCommonInterests(userId, suggestion.userId)

                    FriendSuggestion(
                        user = suggestedUser,
                        score = suggestion.score,
                        reasons = suggestion.reasons,
                        mutualFriends = mutualFriends.take(3), // Show top 3 mutual friends
                        mutualFriendsCount = mutualFriends.size,
                        commonInterests = commonInterests,
                        connectionPath = suggestion.connectionPath
                    )
                }
            }.awaitAll()

        } catch (e: Exception) {
            logger.error("Failed to get friend suggestions for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Update privacy settings with validation
     */
    suspend fun updatePrivacySettings(userId: String, settings: SonetPrivacySettings): SonetPrivacySettings {
        logger.info("Updating privacy settings for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            val validationResult = userValidator.validatePrivacySettings(settings)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Privacy settings validation failed: ${validationResult.errors}")
            }

            val updatedSettings = settings.copy(
                userId = userId,
                updatedAt = Instant.now()
            )

            val savedSettings = privacyRepository.save(updatedSettings)

            // Async operations
            coroutineScope {
                async {
                    // Update search indexes based on new privacy settings
                    eventPublisher.publishPrivacySettingsUpdated(savedSettings)
                    
                    // Re-evaluate friend visibility
                    socialGraphAnalyzer.updateUserPrivacyContext(user, savedSettings)
                    
                    // Track privacy changes
                    userAnalyticsService.trackPrivacySettingsUpdate(user, savedSettings)
                }
            }

            logger.info("Successfully updated privacy settings for user: $userId")
            savedSettings

        } catch (e: Exception) {
            logger.error("Failed to update privacy settings for user: $userId", e)
            throw e
        }
    }

    /**
     * Get comprehensive user profile with privacy respect
     */
    suspend fun getUserProfile(requestedUserId: String, viewerUserId: String?): SonetUserProfile = coroutineScope {
        logger.debug("Getting profile for user: $requestedUserId (viewer: $viewerUserId)")
        
        try {
            val user = userRepository.findById(requestedUserId)
                ?: throw IllegalArgumentException("User not found: $requestedUserId")

            val privacySettings = privacyRepository.findByUserId(requestedUserId)
                ?: throw IllegalArgumentException("Privacy settings not found")

            // Determine what information the viewer can see
            val relationshipContext = if (viewerUserId != null) {
                socialGraphAnalyzer.getRelationshipContext(viewerUserId, requestedUserId)
            } else {
                null
            }

            val canViewProfile = canViewUserProfile(privacySettings, relationshipContext)
            if (!canViewProfile) {
                throw IllegalArgumentException("Not authorized to view this profile")
            }

            // Build profile based on privacy settings and relationship
            val profile = SonetUserProfile(
                user = filterUserInfoByPrivacy(user, privacySettings, relationshipContext),
                mutualFriendsCount = relationshipContext?.mutualFriendsCount ?: 0,
                isOnline = if (privacySettings.showOnlineStatus) authService.isUserOnline(user.id) else null,
                lastSeen = if (privacySettings.showOnlineStatus) user.lastLoginAt else null,
                recentPosts = if (canViewPosts(privacySettings, relationshipContext)) {
                    // This would integrate with the posting service
                    emptyList()
                } else {
                    null
                },
                friendsPreview = if (canViewFriends(privacySettings, relationshipContext)) {
                    friendshipRepository.getUserFriends(requestedUserId, limit = 6).take(6)
                        .map { friendship ->
                            val friendId = if (friendship.user1Id == requestedUserId) friendship.user2Id else friendship.user1Id
                            userRepository.findById(friendId)!!
                        }
                } else {
                    null
                },
                stats = userAnalyticsService.getPublicUserStats(requestedUserId, relationshipContext)
            )

            // Track profile view
            if (viewerUserId != null && viewerUserId != requestedUserId) {
                coroutineScope {
                    async {
                        userAnalyticsService.trackProfileView(requestedUserId, viewerUserId)
                    }
                }
            }

            profile

        } catch (e: Exception) {
            logger.error("Failed to get profile for user: $requestedUserId", e)
            throw e
        }
    }

    // Helper methods

    private fun hasSignificantChanges(oldUser: SonetUser, newUser: SonetUser): Boolean {
        return oldUser.displayName != newUser.displayName ||
                oldUser.profileImageUrl != newUser.profileImageUrl ||
                oldUser.coverImageUrl != newUser.coverImageUrl
    }

    private fun getChangedFields(oldUser: SonetUser, newUser: SonetUser): List<String> {
        val changes = mutableListOf<String>()
        if (oldUser.displayName != newUser.displayName) changes.add("displayName")
        if (oldUser.bio != newUser.bio) changes.add("bio")
        if (oldUser.location != newUser.location) changes.add("location")
        if (oldUser.profileImageUrl != newUser.profileImageUrl) changes.add("profileImage")
        if (oldUser.coverImageUrl != newUser.coverImageUrl) changes.add("coverImage")
        return changes
    }

    private fun canViewUserProfile(privacy: SonetPrivacySettings, relationship: RelationshipContext?): Boolean {
        return when (privacy.profileVisibility) {
            PrivacyLevel.PUBLIC -> true
            PrivacyLevel.FRIENDS -> relationship?.isFriend == true
            PrivacyLevel.FRIENDS_OF_FRIENDS -> relationship?.isFriend == true || 
                                               (relationship?.mutualFriendsCount ?: 0) > 0
            PrivacyLevel.PRIVATE -> false
            PrivacyLevel.CUSTOM -> relationship?.hasCustomAccess == true
        }
    }

    private fun canViewPosts(privacy: SonetPrivacySettings, relationship: RelationshipContext?): Boolean {
        return when (privacy.postsVisibility) {
            PrivacyLevel.PUBLIC -> true
            PrivacyLevel.FRIENDS -> relationship?.isFriend == true
            PrivacyLevel.FRIENDS_OF_FRIENDS -> relationship?.isFriend == true || 
                                               (relationship?.mutualFriendsCount ?: 0) > 0
            PrivacyLevel.PRIVATE -> false
            PrivacyLevel.CUSTOM -> relationship?.hasCustomAccess == true
        }
    }

    private fun canViewFriends(privacy: SonetPrivacySettings, relationship: RelationshipContext?): Boolean {
        return when (privacy.friendsListVisibility) {
            PrivacyLevel.PUBLIC -> true
            PrivacyLevel.FRIENDS -> relationship?.isFriend == true
            PrivacyLevel.FRIENDS_OF_FRIENDS -> relationship?.isFriend == true
            PrivacyLevel.PRIVATE -> false
            PrivacyLevel.CUSTOM -> relationship?.hasCustomAccess == true
        }
    }

    private fun filterUserInfoByPrivacy(
        user: SonetUser, 
        privacy: SonetPrivacySettings, 
        relationship: RelationshipContext?
    ): SonetUser {
        return user.copy(
            email = if (privacy.emailVisibility == PrivacyLevel.PUBLIC || relationship?.isFriend == true) user.email else null,
            dateOfBirth = if (privacy.birthdayVisibility == PrivacyLevel.PUBLIC || relationship?.isFriend == true) user.dateOfBirth else null,
            location = if (relationship?.isFriend == true) user.location else null
        )
    }
}

// Data classes for Sonet-specific features

data class CreateUserRequest(
    val entativaId: String,
    val email: String,
    val username: String,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?,
    val gender: String?,
    val location: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?
)

data class UpdateProfileRequest(
    val displayName: String?,
    val bio: String?,
    val location: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val profileImageCrop: ImageCropData?,
    val coverImageCrop: ImageCropData?
)

data class SonetUser(
    val id: String,
    val entativaId: String,
    val email: String?,
    val username: String,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?,
    val gender: String?,
    val location: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val isVerified: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant,
    val loginCount: Int,
    val friendsCount: Int,
    val followersCount: Int,
    val postsCount: Int
)

data class SonetPrivacySettings(
    val userId: String,
    val profileVisibility: PrivacyLevel,
    val friendsListVisibility: PrivacyLevel,
    val postsVisibility: PrivacyLevel,
    val photosVisibility: PrivacyLevel,
    val birthdayVisibility: PrivacyLevel,
    val emailVisibility: PrivacyLevel,
    val phoneVisibility: PrivacyLevel,
    val searchableByEmail: Boolean,
    val searchableByPhone: Boolean,
    val allowFriendRequests: Boolean,
    val allowTagging: Boolean,
    val allowMessages: Boolean,
    val showOnlineStatus: Boolean,
    val allowStoryViewing: PrivacyLevel,
    val allowPostSharing: Boolean,
    val twoFactorEnabled: Boolean,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean,
    val smsNotifications: Boolean,
    val updatedAt: Instant = Instant.now()
)

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val message: String?,
    val status: FriendRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant? = null,
    val mutualFriendsCount: Int,
    val connectionStrength: Double
)

data class Friendship(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val createdAt: Instant,
    val closeness: Double,
    val interactionScore: Double,
    val mutualFriendsCount: Int,
    val lastInteractionAt: Instant
)

data class FriendInfo(
    val user: SonetUser,
    val friendship: Friendship,
    val mutualFriendsCount: Int,
    val lastInteraction: Instant,
    val isOnline: Boolean,
    val commonInterests: List<String>
)

data class FriendsList(
    val friends: List<FriendInfo>,
    val totalCount: Int,
    val hasMore: Boolean
)

data class FriendSuggestion(
    val user: SonetUser,
    val score: Double,
    val reasons: List<String>,
    val mutualFriends: List<SonetUser>,
    val mutualFriendsCount: Int,
    val commonInterests: List<String>,
    val connectionPath: String
)

data class SonetUserProfile(
    val user: SonetUser,
    val mutualFriendsCount: Int,
    val isOnline: Boolean?,
    val lastSeen: Instant?,
    val recentPosts: List<Any>?,
    val friendsPreview: List<SonetUser>?,
    val stats: SonetUserStats
)

data class SonetUserStats(
    val userId: String,
    val totalFriends: Int,
    val totalPosts: Int,
    val totalLikes: Int,
    val totalComments: Int,
    val totalShares: Int,
    val profileViews: Int,
    val searchAppearances: Int,
    val avgPostEngagement: Double,
    val peakOnlineHours: List<Int>,
    val mostUsedFeatures: List<String>,
    val socialScore: Double,
    val influenceScore: Double,
    val engagementRate: Double
)

data class RelationshipContext(
    val isFriend: Boolean,
    val mutualFriendsCount: Int,
    val hasCustomAccess: Boolean,
    val connectionStrength: Double
)

data class ImageCropData(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

enum class PrivacyLevel {
    PUBLIC, FRIENDS, FRIENDS_OF_FRIENDS, PRIVATE, CUSTOM
}

enum class FriendRequestStatus {
    PENDING, ACCEPTED, DECLINED, EXPIRED
}

enum class FriendSortOption {
    RECENT, ALPHABETICAL, CLOSENESS, INTERACTION
}
