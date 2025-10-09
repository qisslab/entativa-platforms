package com.pika.backend.service

import com.pika.backend.data.repositories.UserRepository
import com.pika.backend.data.repositories.FollowRepository
import com.pika.backend.data.repositories.ConversationRepository
import com.pika.backend.data.repositories.ThreadRepository
import com.pika.backend.data.models.*
import com.pika.backend.auth.AuthenticationService
import com.pika.backend.notifications.NotificationService
import com.pika.backend.analytics.ConversationAnalyticsService
import com.pika.backend.algorithms.ThreadRecommendationEngine
import com.pika.backend.algorithms.ConversationQualityScorer
import com.pika.backend.messaging.EventPublisher
import com.pika.backend.validation.UserValidator
import com.pika.backend.content.ContentModerationService
import com.pika.backend.realtime.RealtimeConnectionManager
import com.pika.backend.algorithms.TrendingTopicsEngine

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pika User Service - Threads-like Real-time Conversation Platform
 * Manages user profiles, follows, conversation preferences, and real-time interactions
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced conversation intelligence
 */
@Singleton
class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val conversationRepository: ConversationRepository,
    private val threadRepository: ThreadRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val conversationAnalyticsService: ConversationAnalyticsService,
    private val threadRecommendationEngine: ThreadRecommendationEngine,
    private val conversationQualityScorer: ConversationQualityScorer,
    private val eventPublisher: EventPublisher,
    private val userValidator: UserValidator,
    private val contentModerationService: ContentModerationService,
    private val realtimeConnectionManager: RealtimeConnectionManager,
    private val trendingTopicsEngine: TrendingTopicsEngine
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    companion object {
        const val MAX_FOLLOWING_COUNT = 1000  // Lower than other platforms for quality conversations
        const val MAX_BIO_LENGTH = 160
        const val MAX_USERNAME_LENGTH = 30
        const val CONVERSATION_QUALITY_THRESHOLD = 0.7
        const val TRENDING_PARTICIPANT_THRESHOLD = 100
    }

    /**
     * Create a new Pika user profile
     */
    suspend fun createUser(request: CreatePikaUserRequest): PikaUser = coroutineScope {
        logger.info("Creating Pika user profile for: ${request.username}")
        
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
            val user = PikaUser(
                id = userId,
                entativaId = request.entativaId,
                username = request.username,
                displayName = request.displayName,
                bio = request.bio,
                profileImageUrl = request.profileImageUrl,
                isVerified = false,
                isPrivate = request.isPrivate,
                location = request.location,
                website = request.website,
                interests = request.interests,
                conversationStyle = request.conversationStyle ?: ConversationStyle.BALANCED,
                preferredTopics = request.preferredTopics ?: emptyList(),
                createdAt = now,
                updatedAt = now,
                lastActiveAt = now,
                followersCount = 0,
                followingCount = 0,
                yeetsCount = 0,
                repliesCount = 0,
                threadsStartedCount = 0,
                threadsParticipatedCount = 0,
                isActive = true,
                allowMentions = true,
                allowReplies = true,
                allowDirectMessages = true,
                showOnlineStatus = true,
                autoMuteNoise = request.autoMuteNoise ?: true,
                filterSensitiveContent = request.filterSensitiveContent ?: true
            )

            // Initialize conversation preferences
            val conversationPrefs = PikaConversationPreferences(
                userId = userId,
                preferredThreadDepth = request.preferredThreadDepth ?: 5,
                autoFollowInterestingConversations = request.autoFollowConversations ?: true,
                notifyOnMentions = true,
                notifyOnReplies = true,
                notifyOnTrendingParticipation = true,
                qualityFilter = ConversationQualityFilter.MEDIUM,
                topicNotifications = request.topicNotifications ?: emptyMap(),
                muteKeywords = request.muteKeywords ?: emptyList(),
                highlightKeywords = request.highlightKeywords ?: emptyList(),
                conversationSpeedPreference = ConversationSpeed.NORMAL,
                preferRealTimeNotifications = true,
                allowConversationSuggestions = true
            )

            // Initialize user stats
            val userStats = PikaUserStats(
                userId = userId,
                totalYeets = 0,
                totalReplies = 0,
                totalLikes = 0,
                totalReposts = 0,
                totalConversationsStarted = 0,
                totalConversationsJoined = 0,
                avgConversationLength = 0.0,
                avgResponseTime = 0.0,
                conversationQualityScore = 0.0,
                trendingParticipations = 0,
                topInteractionPartners = emptyList(),
                preferredPostingTimes = emptyList(),
                conversationTopics = emptyMap(),
                engagementRate = 0.0,
                followbackRate = 0.0
            )

            // Save all user data
            val savedUser = userRepository.save(user)
            conversationRepository.savePreferences(conversationPrefs)
            conversationAnalyticsService.initializeUserStats(userStats)

            // Async operations
            async {
                // Add to thread recommendation engine
                threadRecommendationEngine.indexNewUser(savedUser)
                
                // Generate initial conversation suggestions
                val initialSuggestions = threadRecommendationEngine.generateInitialConversationSuggestions(savedUser)
                if (initialSuggestions.isNotEmpty()) {
                    notificationService.sendConversationSuggestionsNotification(savedUser, initialSuggestions)
                }
                
                // Subscribe to trending topics based on interests
                user.interests.forEach { interest ->
                    trendingTopicsEngine.subscribeUserToTopic(userId, interest)
                }
                
                // Send welcome notifications
                notificationService.sendPikaWelcomeNotification(savedUser)
                
                // Suggest quality accounts to follow
                val followSuggestions = threadRecommendationEngine.generateQualityFollowSuggestions(savedUser)
                if (followSuggestions.isNotEmpty()) {
                    notificationService.sendFollowSuggestionsNotification(savedUser, followSuggestions)
                }
                
                // Publish user creation event
                eventPublisher.publishPikaUserCreated(savedUser)
                
                // Track user creation analytics
                conversationAnalyticsService.trackUserCreation(savedUser)
            }

            logger.info("Successfully created Pika user: ${savedUser.id}")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to create Pika user for username: ${request.username}", e)
            throw e
        }
    }

    /**
     * Update user profile with conversation-focused features
     */
    suspend fun updateProfile(userId: String, request: UpdatePikaProfileRequest): PikaUser = coroutineScope {
        logger.info("Updating Pika profile for user: $userId")
        
        try {
            val existingUser = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Validate profile updates
            val validationResult = userValidator.validateProfileUpdate(request, existingUser)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Profile validation failed: ${validationResult.errors}")
            }

            // Moderate content
            val moderationResults = async {
                contentModerationService.moderatePikaProfile(
                    bio = request.bio,
                    displayName = request.displayName,
                    interests = request.interests
                )
            }

            val moderation = moderationResults.await()
            if (moderation.isBlocked) {
                throw IllegalArgumentException("Profile content violates community guidelines: ${moderation.reason}")
            }

            // Update user record
            val updatedUser = existingUser.copy(
                displayName = request.displayName ?: existingUser.displayName,
                bio = request.bio ?: existingUser.bio,
                profileImageUrl = request.profileImageUrl ?: existingUser.profileImageUrl,
                location = request.location ?: existingUser.location,
                website = request.website ?: existingUser.website,
                interests = request.interests ?: existingUser.interests,
                conversationStyle = request.conversationStyle ?: existingUser.conversationStyle,
                preferredTopics = request.preferredTopics ?: existingUser.preferredTopics,
                isPrivate = request.isPrivate ?: existingUser.isPrivate,
                allowMentions = request.allowMentions ?: existingUser.allowMentions,
                allowReplies = request.allowReplies ?: existingUser.allowReplies,
                allowDirectMessages = request.allowDirectMessages ?: existingUser.allowDirectMessages,
                showOnlineStatus = request.showOnlineStatus ?: existingUser.showOnlineStatus,
                autoMuteNoise = request.autoMuteNoise ?: existingUser.autoMuteNoise,
                filterSensitiveContent = request.filterSensitiveContent ?: existingUser.filterSensitiveContent,
                updatedAt = Instant.now()
            )

            val savedUser = userRepository.save(updatedUser)

            // Update conversation preferences if provided
            request.conversationPreferencesUpdate?.let { prefsUpdate ->
                updateConversationPreferences(userId, prefsUpdate)
            }

            // Async post-update operations
            async {
                // Update thread recommendation engine with new profile data
                threadRecommendationEngine.updateUserProfile(savedUser, existingUser)
                
                // Update topic subscriptions based on new interests
                updateTopicSubscriptions(savedUser, existingUser)
                
                // Recalculate conversation quality score
                if (hasSignificantChanges(existingUser, savedUser)) {
                    conversationQualityScorer.recalculateUserScore(savedUser)
                }
                
                // Publish profile update event
                eventPublisher.publishPikaProfileUpdated(savedUser, existingUser)
                
                // Track profile update analytics
                conversationAnalyticsService.trackProfileUpdate(savedUser, getChangedFields(existingUser, savedUser))
            }

            logger.info("Successfully updated Pika profile for user: $userId")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to update Pika profile for user: $userId", e)
            throw e
        }
    }

    /**
     * Follow a user with conversation intelligence
     */
    suspend fun followUser(followerId: String, followeeId: String): PikaFollow = coroutineScope {
        logger.info("User $followerId following user $followeeId on Pika")
        
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

            // Check following limits (lower than other platforms for quality)
            if (follower.followingCount >= MAX_FOLLOWING_COUNT) {
                throw IllegalArgumentException("Following limit reached")
            }

            // Analyze conversation compatibility
            val compatibilityScore = conversationQualityScorer.calculateCompatibility(follower, followee)
            if (compatibilityScore < CONVERSATION_QUALITY_THRESHOLD) {
                logger.warn("Low conversation compatibility score: $compatibilityScore between $followerId and $followeeId")
            }

            val now = Instant.now()

            // Create follow relationship
            val follow = PikaFollow(
                id = UUID.randomUUID().toString(),
                followerId = followerId,
                followeeId = followeeId,
                createdAt = now,
                isActive = true,
                conversationCompatibility = compatibilityScore,
                mutualInterests = getMutualInterests(follower, followee),
                lastInteractionAt = now,
                conversationCount = 0,
                avgResponseTime = 0.0,
                qualityScore = 0.0
            )

            val savedFollow = followRepository.save(follow)

            // Update user counts
            userRepository.incrementFollowingCount(followerId)
            userRepository.incrementFollowersCount(followeeId)

            // Async operations
            async {
                // Update thread recommendation algorithms
                threadRecommendationEngine.processNewFollow(follower, followee)
                
                // Send notification to followee
                notificationService.sendPikaFollowNotification(follower, followee, compatibilityScore)
                
                // Generate conversation suggestions between the users
                val conversationSuggestions = threadRecommendationEngine.generateFollowupConversationSuggestions(
                    follower, followee
                )
                if (conversationSuggestions.isNotEmpty()) {
                    notificationService.sendConversationOpportunityNotification(
                        follower, followee, conversationSuggestions
                    )
                }
                
                // Update trending topics engine
                trendingTopicsEngine.processNewFollow(follower, followee)
                
                // Track analytics
                conversationAnalyticsService.trackFollowAction(savedFollow, follower, followee)
                
                // Publish events
                eventPublisher.publishPikaFollowCreated(savedFollow)
            }

            logger.info("Successfully created Pika follow relationship: ${savedFollow.id}")
            savedFollow

        } catch (e: Exception) {
            logger.error("Failed to create Pika follow relationship from $followerId to $followeeId", e)
            throw e
        }
    }

    /**
     * Get intelligent conversation suggestions for user
     */
    suspend fun getConversationSuggestions(userId: String, limit: Int = 20): List<PikaConversationSuggestion> = coroutineScope {
        logger.debug("Generating conversation suggestions for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            val userPrefs = conversationRepository.getPreferences(userId)
                ?: throw IllegalArgumentException("User preferences not found")

            // Generate comprehensive conversation suggestions
            val suggestions = threadRecommendationEngine.generateConversationSuggestions(
                user = user,
                preferences = userPrefs,
                limit = limit,
                includeTrending = true,
                includePersonalized = true,
                includeBreakingNews = true
            )

            // Enhance suggestions with additional context
            suggestions.map { suggestion ->
                async {
                    val participants = suggestion.participantIds.mapNotNull { 
                        userRepository.findById(it) 
                    }.take(3)
                    
                    val qualityScore = conversationQualityScorer.evaluateConversation(suggestion.threadId)
                    
                    PikaConversationSuggestion(
                        threadId = suggestion.threadId,
                        topic = suggestion.topic,
                        summary = suggestion.summary,
                        participants = participants,
                        participantCount = suggestion.participantIds.size,
                        qualityScore = qualityScore,
                        trendingScore = suggestion.trendingScore,
                        personalizedScore = suggestion.personalizedScore,
                        reasons = suggestion.reasons,
                        estimatedDuration = suggestion.estimatedDuration,
                        difficultyLevel = suggestion.difficultyLevel,
                        tags = suggestion.tags
                    )
                }
            }.awaitAll()

        } catch (e: Exception) {
            logger.error("Failed to get conversation suggestions for user: $userId", e)
            emptyList()
        }
    }

    /**
     * Join a conversation thread
     */
    suspend fun joinConversation(userId: String, threadId: String): PikaThreadParticipation = coroutineScope {
        logger.info("User $userId joining conversation thread: $threadId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            val thread = threadRepository.findById(threadId)
                ?: throw IllegalArgumentException("Thread not found: $threadId")

            // Check if already participating
            val existingParticipation = threadRepository.findParticipation(userId, threadId)
            if (existingParticipation != null) {
                throw IllegalArgumentException("Already participating in this conversation")
            }

            // Check conversation quality requirements
            val conversationQuality = conversationQualityScorer.evaluateConversation(threadId)
            val userPrefs = conversationRepository.getPreferences(userId)
            
            if (userPrefs?.qualityFilter != ConversationQualityFilter.OFF && 
                conversationQuality < userPrefs?.qualityFilter?.minimumScore ?: 0.0) {
                throw IllegalArgumentException("Conversation quality below user preferences")
            }

            val now = Instant.now()

            // Create participation record
            val participation = PikaThreadParticipation(
                id = UUID.randomUUID().toString(),
                userId = userId,
                threadId = threadId,
                joinedAt = now,
                isActive = true,
                messageCount = 0,
                firstMessageAt = null,
                lastMessageAt = null,
                avgResponseTime = 0.0,
                qualityContribution = 0.0,
                role = ParticipationRole.PARTICIPANT
            )

            val savedParticipation = threadRepository.saveParticipation(participation)

            // Update user stats
            userRepository.incrementThreadsParticipated(userId)

            // Async operations
            async {
                // Update thread metrics
                threadRepository.incrementParticipantCount(threadId)
                
                // Send notifications to other participants
                val otherParticipants = threadRepository.getActiveParticipants(threadId)
                    .filter { it.userId != userId }
                    .mapNotNull { userRepository.findById(it.userId) }
                
                otherParticipants.forEach { participant ->
                    notificationService.sendConversationJoinNotification(user, participant, thread)
                }
                
                // Update conversation recommendations
                threadRecommendationEngine.processConversationJoin(user, thread)
                
                // Check if conversation is trending
                if (threadRepository.getParticipantCount(threadId) >= TRENDING_PARTICIPANT_THRESHOLD) {
                    trendingTopicsEngine.evaluateForTrending(thread)
                }
                
                // Track analytics
                conversationAnalyticsService.trackConversationJoin(savedParticipation, user, thread)
                
                // Publish events
                eventPublisher.publishConversationJoined(savedParticipation)
            }

            logger.info("Successfully joined conversation: ${savedParticipation.id}")
            savedParticipation

        } catch (e: Exception) {
            logger.error("Failed to join conversation $threadId for user $userId", e)
            throw e
        }
    }

    /**
     * Get user's conversation activity dashboard
     */
    suspend fun getConversationDashboard(userId: String): PikaConversationDashboard = coroutineScope {
        logger.debug("Getting conversation dashboard for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            val userStats = conversationAnalyticsService.getUserStats(userId)
            val activeConversations = threadRepository.getUserActiveConversations(userId, limit = 10)
            val recentActivity = conversationAnalyticsService.getRecentActivity(userId, limit = 20)
            val trendings = trendingTopicsEngine.getUserTrendingTopics(userId, limit = 5)
            val suggestions = getConversationSuggestions(userId, limit = 10)

            // Get conversation quality insights
            val qualityInsights = async {
                conversationQualityScorer.getUserQualityInsights(userId)
            }

            // Get follow recommendations based on conversation compatibility
            val followRecommendations = async {
                threadRecommendationEngine.generateConversationBasedFollowSuggestions(user, limit = 5)
            }

            // Get real-time status
            val isOnline = realtimeConnectionManager.isUserOnline(userId)
            val onlineFollowers = if (user.showOnlineStatus) {
                realtimeConnectionManager.getOnlineFollowers(userId)
            } else emptyList()

            PikaConversationDashboard(
                user = user,
                stats = userStats,
                activeConversations = activeConversations.map { participation ->
                    val thread = threadRepository.findById(participation.threadId)!!
                    ConversationSummary(
                        thread = thread,
                        participation = participation,
                        unreadCount = threadRepository.getUnreadCount(userId, participation.threadId),
                        lastActivity = threadRepository.getLastActivity(participation.threadId)
                    )
                },
                recentActivity = recentActivity,
                trendingTopics = trendings,
                conversationSuggestions = suggestions,
                qualityInsights = qualityInsights.await(),
                followRecommendations = followRecommendations.await(),
                isOnline = isOnline,
                onlineFollowers = onlineFollowers.take(10)
            )

        } catch (e: Exception) {
            logger.error("Failed to get conversation dashboard for user: $userId", e)
            throw e
        }
    }

    /**
     * Update conversation preferences
     */
    suspend fun updateConversationPreferences(
        userId: String, 
        update: ConversationPreferencesUpdate
    ): PikaConversationPreferences {
        logger.info("Updating conversation preferences for user: $userId")
        
        try {
            val existingPrefs = conversationRepository.getPreferences(userId)
                ?: throw IllegalArgumentException("User preferences not found")

            val updatedPrefs = existingPrefs.copy(
                preferredThreadDepth = update.preferredThreadDepth ?: existingPrefs.preferredThreadDepth,
                autoFollowInterestingConversations = update.autoFollowConversations ?: existingPrefs.autoFollowInterestingConversations,
                notifyOnMentions = update.notifyOnMentions ?: existingPrefs.notifyOnMentions,
                notifyOnReplies = update.notifyOnReplies ?: existingPrefs.notifyOnReplies,
                notifyOnTrendingParticipation = update.notifyOnTrending ?: existingPrefs.notifyOnTrendingParticipation,
                qualityFilter = update.qualityFilter ?: existingPrefs.qualityFilter,
                topicNotifications = update.topicNotifications ?: existingPrefs.topicNotifications,
                muteKeywords = update.muteKeywords ?: existingPrefs.muteKeywords,
                highlightKeywords = update.highlightKeywords ?: existingPrefs.highlightKeywords,
                conversationSpeedPreference = update.conversationSpeed ?: existingPrefs.conversationSpeedPreference,
                preferRealTimeNotifications = update.preferRealTime ?: existingPrefs.preferRealTimeNotifications,
                allowConversationSuggestions = update.allowSuggestions ?: existingPrefs.allowConversationSuggestions,
                updatedAt = Instant.now()
            )

            val savedPrefs = conversationRepository.savePreferences(updatedPrefs)

            // Async operations
            coroutineScope {
                async {
                    // Update recommendation algorithms with new preferences
                    threadRecommendationEngine.updateUserPreferences(userId, savedPrefs)
                    
                    // Update topic subscriptions
                    trendingTopicsEngine.updateUserTopicPreferences(userId, savedPrefs.topicNotifications)
                    
                    // Track preference changes
                    conversationAnalyticsService.trackPreferencesUpdate(userId, savedPrefs)
                    
                    // Publish event
                    eventPublisher.publishConversationPreferencesUpdated(savedPrefs)
                }
            }

            logger.info("Successfully updated conversation preferences for user: $userId")
            savedPrefs

        } catch (e: Exception) {
            logger.error("Failed to update conversation preferences for user: $userId", e)
            throw e
        }
    }

    // Helper methods

    private fun getMutualInterests(user1: PikaUser, user2: PikaUser): List<String> {
        return user1.interests.intersect(user2.interests.toSet()).toList()
    }

    private suspend fun updateTopicSubscriptions(newUser: PikaUser, oldUser: PikaUser) {
        val addedInterests = newUser.interests - oldUser.interests.toSet()
        val removedInterests = oldUser.interests.toSet() - newUser.interests.toSet()

        addedInterests.forEach { interest ->
            trendingTopicsEngine.subscribeUserToTopic(newUser.id, interest)
        }

        removedInterests.forEach { interest ->
            trendingTopicsEngine.unsubscribeUserFromTopic(newUser.id, interest)
        }
    }

    private fun hasSignificantChanges(oldUser: PikaUser, newUser: PikaUser): Boolean {
        return oldUser.interests != newUser.interests ||
                oldUser.conversationStyle != newUser.conversationStyle ||
                oldUser.preferredTopics != newUser.preferredTopics
    }

    private fun getChangedFields(oldUser: PikaUser, newUser: PikaUser): List<String> {
        val changes = mutableListOf<String>()
        if (oldUser.displayName != newUser.displayName) changes.add("displayName")
        if (oldUser.bio != newUser.bio) changes.add("bio")
        if (oldUser.interests != newUser.interests) changes.add("interests")
        if (oldUser.conversationStyle != newUser.conversationStyle) changes.add("conversationStyle")
        if (oldUser.preferredTopics != newUser.preferredTopics) changes.add("preferredTopics")
        return changes
    }
}

// Data classes for Pika-specific features

data class CreatePikaUserRequest(
    val entativaId: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val isPrivate: Boolean = false,
    val location: String?,
    val website: String?,
    val interests: List<String>,
    val conversationStyle: ConversationStyle?,
    val preferredTopics: List<String>?,
    val preferredThreadDepth: Int?,
    val autoFollowConversations: Boolean?,
    val autoMuteNoise: Boolean?,
    val filterSensitiveContent: Boolean?,
    val topicNotifications: Map<String, Boolean>?,
    val muteKeywords: List<String>?,
    val highlightKeywords: List<String>?
)

data class UpdatePikaProfileRequest(
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val location: String?,
    val website: String?,
    val interests: List<String>?,
    val conversationStyle: ConversationStyle?,
    val preferredTopics: List<String>?,
    val isPrivate: Boolean?,
    val allowMentions: Boolean?,
    val allowReplies: Boolean?,
    val allowDirectMessages: Boolean?,
    val showOnlineStatus: Boolean?,
    val autoMuteNoise: Boolean?,
    val filterSensitiveContent: Boolean?,
    val conversationPreferencesUpdate: ConversationPreferencesUpdate?
)

data class PikaUser(
    val id: String,
    val entativaId: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val isVerified: Boolean,
    val isPrivate: Boolean,
    val location: String?,
    val website: String?,
    val interests: List<String>,
    val conversationStyle: ConversationStyle,
    val preferredTopics: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant,
    val followersCount: Int,
    val followingCount: Int,
    val yeetsCount: Int,
    val repliesCount: Int,
    val threadsStartedCount: Int,
    val threadsParticipatedCount: Int,
    val isActive: Boolean,
    val allowMentions: Boolean,
    val allowReplies: Boolean,
    val allowDirectMessages: Boolean,
    val showOnlineStatus: Boolean,
    val autoMuteNoise: Boolean,
    val filterSensitiveContent: Boolean
)

data class PikaFollow(
    val id: String,
    val followerId: String,
    val followeeId: String,
    val createdAt: Instant,
    val isActive: Boolean,
    val conversationCompatibility: Double,
    val mutualInterests: List<String>,
    val lastInteractionAt: Instant,
    val conversationCount: Int,
    val avgResponseTime: Double,
    val qualityScore: Double
)

data class PikaConversationPreferences(
    val userId: String,
    val preferredThreadDepth: Int,
    val autoFollowInterestingConversations: Boolean,
    val notifyOnMentions: Boolean,
    val notifyOnReplies: Boolean,
    val notifyOnTrendingParticipation: Boolean,
    val qualityFilter: ConversationQualityFilter,
    val topicNotifications: Map<String, Boolean>,
    val muteKeywords: List<String>,
    val highlightKeywords: List<String>,
    val conversationSpeedPreference: ConversationSpeed,
    val preferRealTimeNotifications: Boolean,
    val allowConversationSuggestions: Boolean,
    val updatedAt: Instant = Instant.now()
)

data class PikaUserStats(
    val userId: String,
    val totalYeets: Int,
    val totalReplies: Int,
    val totalLikes: Int,
    val totalReposts: Int,
    val totalConversationsStarted: Int,
    val totalConversationsJoined: Int,
    val avgConversationLength: Double,
    val avgResponseTime: Double,
    val conversationQualityScore: Double,
    val trendingParticipations: Int,
    val topInteractionPartners: List<String>,
    val preferredPostingTimes: List<Int>,
    val conversationTopics: Map<String, Int>,
    val engagementRate: Double,
    val followbackRate: Double
)

data class PikaThreadParticipation(
    val id: String,
    val userId: String,
    val threadId: String,
    val joinedAt: Instant,
    val isActive: Boolean,
    val messageCount: Int,
    val firstMessageAt: Instant?,
    val lastMessageAt: Instant?,
    val avgResponseTime: Double,
    val qualityContribution: Double,
    val role: ParticipationRole
)

data class PikaConversationSuggestion(
    val threadId: String,
    val topic: String,
    val summary: String,
    val participants: List<PikaUser>,
    val participantCount: Int,
    val qualityScore: Double,
    val trendingScore: Double,
    val personalizedScore: Double,
    val reasons: List<String>,
    val estimatedDuration: String,
    val difficultyLevel: ConversationDifficulty,
    val tags: List<String>
)

data class PikaConversationDashboard(
    val user: PikaUser,
    val stats: PikaUserStats,
    val activeConversations: List<ConversationSummary>,
    val recentActivity: List<Any>,
    val trendingTopics: List<Any>,
    val conversationSuggestions: List<PikaConversationSuggestion>,
    val qualityInsights: Any,
    val followRecommendations: List<Any>,
    val isOnline: Boolean,
    val onlineFollowers: List<PikaUser>
)

data class ConversationSummary(
    val thread: Any,
    val participation: PikaThreadParticipation,
    val unreadCount: Int,
    val lastActivity: Instant
)

data class ConversationPreferencesUpdate(
    val preferredThreadDepth: Int?,
    val autoFollowConversations: Boolean?,
    val notifyOnMentions: Boolean?,
    val notifyOnReplies: Boolean?,
    val notifyOnTrending: Boolean?,
    val qualityFilter: ConversationQualityFilter?,
    val topicNotifications: Map<String, Boolean>?,
    val muteKeywords: List<String>?,
    val highlightKeywords: List<String>?,
    val conversationSpeed: ConversationSpeed?,
    val preferRealTime: Boolean?,
    val allowSuggestions: Boolean?
)

enum class ConversationStyle {
    ANALYTICAL, CREATIVE, BALANCED, PASSIONATE, THOUGHTFUL, QUICK, DETAILED
}

enum class ConversationQualityFilter(val minimumScore: Double) {
    OFF(0.0), LOW(0.3), MEDIUM(0.5), HIGH(0.7), VERY_HIGH(0.9)
}

enum class ConversationSpeed {
    SLOW, NORMAL, FAST, REAL_TIME
}

enum class ParticipationRole {
    STARTER, PARTICIPANT, MODERATOR, OBSERVER
}

enum class ConversationDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}
