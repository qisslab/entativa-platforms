package com.playpods.backend.services

import com.playpods.backend.data.repositories.UserRepository
import com.playpods.backend.data.repositories.ChannelRepository
import com.playpods.backend.data.repositories.SubscriptionRepository
import com.playpods.backend.data.repositories.CreatorRepository
import com.playpods.backend.data.repositories.MonetizationRepository
import com.playpods.backend.data.models.*
import com.playpods.backend.auth.AuthenticationService
import com.playpods.backend.notifications.NotificationService
import com.playpods.backend.analytics.CreatorAnalyticsService
import com.playpods.backend.algorithms.RecommendationEngine
import com.playpods.backend.algorithms.ChannelGrowthAnalyzer
import com.playpods.backend.messaging.EventPublisher
import com.playpods.backend.validation.UserValidator
import com.playpods.backend.content.ContentModerationService
import com.playpods.backend.monetization.MonetizationEngine
import com.playpods.backend.verification.ChannelVerificationService
import com.playpods.backend.community.CommunityManagementService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlayPods User Service - YouTube-like Video and Podcast Platform
 * Manages creator channels, subscribers, monetization, and creator analytics
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced creator economy features
 */
@Singleton
class UserService @Inject constructor(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val creatorRepository: CreatorRepository,
    private val monetizationRepository: MonetizationRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val creatorAnalyticsService: CreatorAnalyticsService,
    private val recommendationEngine: RecommendationEngine,
    private val channelGrowthAnalyzer: ChannelGrowthAnalyzer,
    private val eventPublisher: EventPublisher,
    private val userValidator: UserValidator,
    private val contentModerationService: ContentModerationService,
    private val monetizationEngine: MonetizationEngine,
    private val channelVerificationService: ChannelVerificationService,
    private val communityManagementService: CommunityManagementService
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    companion object {
        const val MAX_CHANNELS_PER_USER = 100
        const val MONETIZATION_SUBSCRIBER_THRESHOLD = 1000
        const val MONETIZATION_WATCH_HOURS_THRESHOLD = 4000
        const val VERIFICATION_SUBSCRIBER_THRESHOLD = 100000
        const val CHANNEL_NAME_MAX_LENGTH = 100
        const val CHANNEL_DESCRIPTION_MAX_LENGTH = 1000
    }

    /**
     * Create a new PlayPods user and channel
     */
    suspend fun createUser(request: CreatePlayPodsUserRequest): PlayPodsUser = coroutineScope {
        logger.info("Creating PlayPods user and channel for: ${request.channelName}")
        
        try {
            // Validate user data
            val validationResult = userValidator.validateUserCreation(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("User validation failed: ${validationResult.errors}")
            }

            // Check channel name availability
            val existingChannel = channelRepository.findByChannelName(request.channelName)
            if (existingChannel != null) {
                throw IllegalArgumentException("Channel name ${request.channelName} is already taken")
            }

            val userId = UUID.randomUUID().toString()
            val channelId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create core user record
            val user = PlayPodsUser(
                id = userId,
                entativaId = request.entativaId,
                email = request.email,
                username = request.username,
                displayName = request.displayName,
                profileImageUrl = request.profileImageUrl,
                isVerified = false,
                accountType = request.accountType,
                country = request.country,
                language = request.language,
                timezone = request.timezone,
                createdAt = now,
                updatedAt = now,
                lastActiveAt = now,
                totalChannels = 1,
                totalSubscribers = 0,
                totalViews = 0,
                totalWatchTime = 0,
                isActive = true,
                primaryChannelId = channelId
            )

            // Create primary channel
            val channel = PlayPodsChannel(
                id = channelId,
                userId = userId,
                channelName = request.channelName,
                displayName = request.channelDisplayName ?: request.channelName,
                description = request.channelDescription,
                profileImageUrl = request.channelProfileImageUrl ?: request.profileImageUrl,
                bannerImageUrl = request.channelBannerImageUrl,
                category = request.channelCategory,
                keywords = request.channelKeywords ?: emptyList(),
                country = request.country,
                language = request.language,
                isVerified = false,
                isMonetized = false,
                subscribersCount = 0,
                videosCount = 0,
                totalViews = 0,
                totalWatchTime = 0,
                createdAt = now,
                updatedAt = now,
                status = ChannelStatus.ACTIVE,
                contentRating = ContentRating.GENERAL,
                allowComments = true,
                allowCommunityPosts = true,
                allowSubscriptions = true,
                showSubscriberCount = true,
                defaultUploadVisibility = VideoVisibility.PUBLIC
            )

            // Initialize creator profile
            val creatorProfile = PlayPodsCreatorProfile(
                userId = userId,
                channelId = channelId,
                creatorType = request.creatorType ?: CreatorType.INDIVIDUAL,
                niche = request.niche ?: "general",
                targetAudience = request.targetAudience ?: "general",
                contentStyle = request.contentStyle ?: "educational",
                uploadFrequency = request.uploadFrequency ?: UploadFrequency.WEEKLY,
                avgVideoLength = 0.0,
                engagementRate = 0.0,
                retention90Day = 0.0,
                clickThroughRate = 0.0,
                subscriberGrowthRate = 0.0,
                revenueGenerated = 0.0,
                sponsorshipDeals = 0,
                collaborationCount = 0,
                equipmentLevel = request.equipmentLevel ?: EquipmentLevel.BASIC,
                editingSkillLevel = request.editingSkillLevel ?: SkillLevel.BEGINNER,
                isEligibleForPartnerProgram = false,
                partnerProgramJoinedAt = null,
                createdAt = now
            )

            // Initialize channel analytics
            val channelStats = PlayPodsChannelStats(
                channelId = channelId,
                totalViews = 0,
                totalWatchTime = 0,
                totalSubscribers = 0,
                totalVideos = 0,
                totalPodcasts = 0,
                totalShorts = 0,
                totalLiveStreams = 0,
                avgViewDuration = 0.0,
                subscriberRetentionRate = 0.0,
                top10VideosViews = 0,
                revenueThisMonth = 0.0,
                estimatedMinutesWatched = 0,
                subscribersGained = 0,
                subscribersLost = 0,
                impressions = 0,
                clickThroughRate = 0.0,
                topGeographies = emptyMap(),
                topAgeGroups = emptyMap(),
                topDevices = emptyMap(),
                peakViewingHours = emptyList()
            )

            // Save all user data
            val savedUser = userRepository.save(user)
            val savedChannel = channelRepository.save(channel)
            creatorRepository.save(creatorProfile)
            creatorAnalyticsService.initializeChannelStats(channelStats)

            // Async operations
            async {
                // Add to recommendation engine
                recommendationEngine.indexNewChannel(savedChannel)
                
                // Generate initial channel recommendations
                val channelSuggestions = recommendationEngine.generateChannelRecommendations(savedUser)
                if (channelSuggestions.isNotEmpty()) {
                    notificationService.sendChannelSuggestionsNotification(savedUser, channelSuggestions)
                }
                
                // Set up default playlists
                createDefaultPlaylists(savedChannel)
                
                // Initialize community features
                communityManagementService.initializeChannelCommunity(savedChannel)
                
                // Send welcome notifications
                notificationService.sendPlayPodsWelcomeNotification(savedUser, savedChannel)
                
                // Provide creator onboarding tips
                notificationService.sendCreatorOnboardingTips(savedUser, savedChannel)
                
                // Publish user creation event
                eventPublisher.publishPlayPodsUserCreated(savedUser, savedChannel)
                
                // Track user creation analytics
                creatorAnalyticsService.trackUserCreation(savedUser, savedChannel)
            }

            logger.info("Successfully created PlayPods user and channel: ${savedUser.id}")
            savedUser

        } catch (e: Exception) {
            logger.error("Failed to create PlayPods user for channel: ${request.channelName}", e)
            throw e
        }
    }

    /**
     * Create additional channel for existing user
     */
    suspend fun createAdditionalChannel(userId: String, request: CreateChannelRequest): PlayPodsChannel = coroutineScope {
        logger.info("Creating additional channel for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Check channel limits
            if (user.totalChannels >= MAX_CHANNELS_PER_USER) {
                throw IllegalArgumentException("Maximum number of channels reached")
            }

            // Check channel name availability
            val existingChannel = channelRepository.findByChannelName(request.channelName)
            if (existingChannel != null) {
                throw IllegalArgumentException("Channel name ${request.channelName} is already taken")
            }

            // Validate channel data
            val validationResult = userValidator.validateChannelCreation(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Channel validation failed: ${validationResult.errors}")
            }

            val channelId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create additional channel
            val channel = PlayPodsChannel(
                id = channelId,
                userId = userId,
                channelName = request.channelName,
                displayName = request.displayName,
                description = request.description,
                profileImageUrl = request.profileImageUrl,
                bannerImageUrl = request.bannerImageUrl,
                category = request.category,
                keywords = request.keywords,
                country = request.country,
                language = request.language,
                isVerified = false,
                isMonetized = false,
                subscribersCount = 0,
                videosCount = 0,
                totalViews = 0,
                totalWatchTime = 0,
                createdAt = now,
                updatedAt = now,
                status = ChannelStatus.ACTIVE,
                contentRating = request.contentRating ?: ContentRating.GENERAL,
                allowComments = true,
                allowCommunityPosts = true,
                allowSubscriptions = true,
                showSubscriberCount = true,
                defaultUploadVisibility = request.defaultVisibility ?: VideoVisibility.PUBLIC
            )

            val savedChannel = channelRepository.save(channel)

            // Update user channel count
            userRepository.incrementChannelCount(userId)

            // Async operations
            async {
                // Add to recommendation engine
                recommendationEngine.indexNewChannel(savedChannel)
                
                // Set up default playlists
                createDefaultPlaylists(savedChannel)
                
                // Initialize community features
                communityManagementService.initializeChannelCommunity(savedChannel)
                
                // Initialize analytics
                val channelStats = PlayPodsChannelStats(
                    channelId = channelId,
                    totalViews = 0,
                    totalWatchTime = 0,
                    totalSubscribers = 0,
                    totalVideos = 0,
                    totalPodcasts = 0,
                    totalShorts = 0,
                    totalLiveStreams = 0,
                    avgViewDuration = 0.0,
                    subscriberRetentionRate = 0.0,
                    top10VideosViews = 0,
                    revenueThisMonth = 0.0,
                    estimatedMinutesWatched = 0,
                    subscribersGained = 0,
                    subscribersLost = 0,
                    impressions = 0,
                    clickThroughRate = 0.0,
                    topGeographies = emptyMap(),
                    topAgeGroups = emptyMap(),
                    topDevices = emptyMap(),
                    peakViewingHours = emptyList()
                )
                creatorAnalyticsService.initializeChannelStats(channelStats)
                
                // Publish event
                eventPublisher.publishChannelCreated(savedChannel)
                
                // Track analytics
                creatorAnalyticsService.trackChannelCreation(savedChannel)
            }

            logger.info("Successfully created additional channel: ${savedChannel.id}")
            savedChannel

        } catch (e: Exception) {
            logger.error("Failed to create additional channel for user: $userId", e)
            throw e
        }
    }

    /**
     * Subscribe to a channel
     */
    suspend fun subscribeToChannel(subscriberId: String, channelId: String): PlayPodsSubscription = coroutineScope {
        logger.info("User $subscriberId subscribing to channel $channelId")
        
        try {
            // Validate subscriber and channel exist
            val subscriber = userRepository.findById(subscriberId)
                ?: throw IllegalArgumentException("Subscriber not found: $subscriberId")
            val channel = channelRepository.findById(channelId)
                ?: throw IllegalArgumentException("Channel not found: $channelId")

            if (channel.userId == subscriberId) {
                throw IllegalArgumentException("User cannot subscribe to their own channel")
            }

            // Check if already subscribed
            val existingSubscription = subscriptionRepository.findSubscription(subscriberId, channelId)
            if (existingSubscription != null && existingSubscription.isActive) {
                throw IllegalArgumentException("Already subscribed to this channel")
            }

            val now = Instant.now()

            // Create or reactivate subscription
            val subscription = if (existingSubscription != null) {
                existingSubscription.copy(
                    isActive = true,
                    subscribedAt = now,
                    unsubscribedAt = null
                )
            } else {
                PlayPodsSubscription(
                    id = UUID.randomUUID().toString(),
                    subscriberId = subscriberId,
                    channelId = channelId,
                    subscribedAt = now,
                    isActive = true,
                    notificationsEnabled = true,
                    subscriptionSource = SubscriptionSource.CHANNEL_PAGE,
                    totalWatchTime = 0,
                    videosWatched = 0,
                    lastWatchedAt = null
                )
            }

            val savedSubscription = subscriptionRepository.save(subscription)

            // Update counts
            channelRepository.incrementSubscribers(channelId)
            userRepository.incrementTotalSubscribers(channel.userId)

            // Async operations
            async {
                val channelOwner = userRepository.findById(channel.userId)!!
                
                // Update recommendation algorithms
                recommendationEngine.processNewSubscription(subscriber, channel)
                
                // Send notification to channel owner
                notificationService.sendSubscriptionNotification(subscriber, channelOwner, channel)
                
                // Check monetization eligibility
                if (channel.subscribersCount + 1 >= MONETIZATION_SUBSCRIBER_THRESHOLD) {
                    val totalWatchHours = creatorAnalyticsService.getTotalWatchHours(channelId)
                    if (totalWatchHours >= MONETIZATION_WATCH_HOURS_THRESHOLD) {
                        monetizationEngine.evaluateMonetizationEligibility(channel)
                    }
                }
                
                // Check verification eligibility
                if (channel.subscribersCount + 1 >= VERIFICATION_SUBSCRIBER_THRESHOLD) {
                    channelVerificationService.evaluateVerificationEligibility(channel)
                }
                
                // Generate personalized recommendations for subscriber
                recommendationEngine.generateSubscriptionBasedRecommendations(subscriber, channel)
                
                // Track analytics
                creatorAnalyticsService.trackSubscription(savedSubscription, subscriber, channel)
                
                // Publish events
                eventPublisher.publishSubscriptionCreated(savedSubscription)
            }

            logger.info("Successfully created subscription: ${savedSubscription.id}")
            savedSubscription

        } catch (e: Exception) {
            logger.error("Failed to create subscription from $subscriberId to $channelId", e)
            throw e
        }
    }

    /**
     * Unsubscribe from a channel
     */
    suspend fun unsubscribeFromChannel(subscriberId: String, channelId: String): Boolean = coroutineScope {
        logger.info("User $subscriberId unsubscribing from channel $channelId")
        
        try {
            val existingSubscription = subscriptionRepository.findSubscription(subscriberId, channelId)
                ?: throw IllegalArgumentException("Subscription not found")

            if (!existingSubscription.isActive) {
                throw IllegalArgumentException("Already unsubscribed from this channel")
            }

            // Deactivate subscription
            val unsubscription = existingSubscription.copy(
                isActive = false,
                unsubscribedAt = Instant.now()
            )

            subscriptionRepository.save(unsubscription)

            // Update counts
            channelRepository.decrementSubscribers(channelId)
            val channel = channelRepository.findById(channelId)!!
            userRepository.decrementTotalSubscribers(channel.userId)

            // Async operations
            async {
                val subscriber = userRepository.findById(subscriberId)!!
                
                // Update recommendation algorithms
                recommendationEngine.processUnsubscription(subscriber, channel)
                
                // Check if monetization should be revoked
                if (channel.subscribersCount - 1 < MONETIZATION_SUBSCRIBER_THRESHOLD) {
                    monetizationEngine.reevaluateMonetizationEligibility(channel)
                }
                
                // Track analytics
                creatorAnalyticsService.trackUnsubscription(unsubscription, subscriber, channel)
                
                // Publish events
                eventPublisher.publishUnsubscriptionCreated(unsubscription)
            }

            logger.info("Successfully unsubscribed from channel: $channelId")
            true

        } catch (e: Exception) {
            logger.error("Failed to unsubscribe from channel $channelId by $subscriberId", e)
            false
        }
    }

    /**
     * Get comprehensive channel analytics dashboard
     */
    suspend fun getChannelDashboard(channelId: String, ownerId: String): PlayPodsChannelDashboard = coroutineScope {
        logger.debug("Getting channel dashboard for: $channelId")
        
        try {
            val channel = channelRepository.findById(channelId)
                ?: throw IllegalArgumentException("Channel not found: $channelId")

            if (channel.userId != ownerId) {
                throw IllegalArgumentException("Not authorized to view this channel dashboard")
            }

            // Get comprehensive analytics
            val analytics = async {
                creatorAnalyticsService.getChannelAnalytics(channelId)
            }

            // Get revenue information
            val revenueData = async {
                if (channel.isMonetized) {
                    monetizationEngine.getChannelRevenue(channelId)
                } else null
            }

            // Get growth analysis
            val growthAnalysis = async {
                channelGrowthAnalyzer.analyzeChannelGrowth(channelId)
            }

            // Get recent subscriber activity
            val recentSubscribers = async {
                subscriptionRepository.getRecentSubscribers(channelId, limit = 10)
            }

            // Get top performing content
            val topContent = async {
                creatorAnalyticsService.getTopPerformingContent(channelId, limit = 10)
            }

            // Get audience insights
            val audienceInsights = async {
                creatorAnalyticsService.getAudienceInsights(channelId)
            }

            // Get recommendations for growth
            val growthRecommendations = async {
                channelGrowthAnalyzer.generateGrowthRecommendations(channel)
            }

            PlayPodsChannelDashboard(
                channel = channel,
                analytics = analytics.await(),
                revenueData = revenueData.await(),
                growthAnalysis = growthAnalysis.await(),
                recentSubscribers = recentSubscribers.await().map { subscription ->
                    userRepository.findById(subscription.subscriberId)!!
                },
                topContent = topContent.await(),
                audienceInsights = audienceInsights.await(),
                growthRecommendations = growthRecommendations.await(),
                monetizationStatus = if (channel.isMonetized) {
                    monetizationEngine.getMonetizationStatus(channelId)
                } else null,
                verificationStatus = channelVerificationService.getVerificationStatus(channelId)
            )

        } catch (e: Exception) {
            logger.error("Failed to get channel dashboard for: $channelId", e)
            throw e
        }
    }

    /**
     * Apply for channel verification
     */
    suspend fun applyForVerification(channelId: String, ownerId: String): VerificationApplication = coroutineScope {
        logger.info("Applying for verification for channel: $channelId")
        
        try {
            val channel = channelRepository.findById(channelId)
                ?: throw IllegalArgumentException("Channel not found: $channelId")

            if (channel.userId != ownerId) {
                throw IllegalArgumentException("Not authorized to apply for verification")
            }

            if (channel.isVerified) {
                throw IllegalArgumentException("Channel is already verified")
            }

            // Check basic eligibility
            if (channel.subscribersCount < VERIFICATION_SUBSCRIBER_THRESHOLD) {
                throw IllegalArgumentException("Channel does not meet minimum subscriber requirement")
            }

            val application = channelVerificationService.submitVerificationApplication(
                channel = channel,
                reason = "Channel meets verification criteria"
            )

            // Async operations
            async {
                // Notify user of application submission
                val owner = userRepository.findById(ownerId)!!
                notificationService.sendVerificationApplicationSubmitted(owner, channel, application)
                
                // Track analytics
                creatorAnalyticsService.trackVerificationApplication(channel, application)
                
                // Publish event
                eventPublisher.publishVerificationApplicationSubmitted(application)
            }

            logger.info("Successfully submitted verification application: ${application.id}")
            application

        } catch (e: Exception) {
            logger.error("Failed to apply for verification for channel: $channelId", e)
            throw e
        }
    }

    /**
     * Apply for monetization program
     */
    suspend fun applyForMonetization(channelId: String, ownerId: String): MonetizationApplication = coroutineScope {
        logger.info("Applying for monetization for channel: $channelId")
        
        try {
            val channel = channelRepository.findById(channelId)
                ?: throw IllegalArgumentException("Channel not found: $channelId")

            if (channel.userId != ownerId) {
                throw IllegalArgumentException("Not authorized to apply for monetization")
            }

            if (channel.isMonetized) {
                throw IllegalArgumentException("Channel is already monetized")
            }

            // Check eligibility requirements
            val totalWatchHours = creatorAnalyticsService.getTotalWatchHours(channelId)
            
            if (channel.subscribersCount < MONETIZATION_SUBSCRIBER_THRESHOLD) {
                throw IllegalArgumentException("Channel does not meet minimum subscriber requirement ($MONETIZATION_SUBSCRIBER_THRESHOLD)")
            }

            if (totalWatchHours < MONETIZATION_WATCH_HOURS_THRESHOLD) {
                throw IllegalArgumentException("Channel does not meet minimum watch hours requirement ($MONETIZATION_WATCH_HOURS_THRESHOLD)")
            }

            val application = monetizationEngine.submitMonetizationApplication(
                channel = channel,
                watchHours = totalWatchHours
            )

            // Async operations
            async {
                // Notify user of application submission
                val owner = userRepository.findById(ownerId)!!
                notificationService.sendMonetizationApplicationSubmitted(owner, channel, application)
                
                // Track analytics
                creatorAnalyticsService.trackMonetizationApplication(channel, application)
                
                // Publish event
                eventPublisher.publishMonetizationApplicationSubmitted(application)
            }

            logger.info("Successfully submitted monetization application: ${application.id}")
            application

        } catch (e: Exception) {
            logger.error("Failed to apply for monetization for channel: $channelId", e)
            throw e
        }
    }

    /**
     * Get user's subscriptions with intelligent sorting
     */
    suspend fun getUserSubscriptions(
        userId: String,
        limit: Int = 50,
        offset: Int = 0,
        sortBy: SubscriptionSortOption = SubscriptionSortOption.RECENT_ACTIVITY
    ): PlayPodsSubscriptionsList = coroutineScope {
        logger.debug("Fetching subscriptions for user: $userId")
        
        try {
            val subscriptions = subscriptionRepository.getUserSubscriptions(
                userId = userId,
                limit = limit,
                offset = offset,
                sortBy = sortBy,
                includeInactive = false
            )

            // Enhance with channel data and activity
            val enhancedSubscriptions = subscriptions.map { subscription ->
                async {
                    val channel = channelRepository.findById(subscription.channelId)!!
                    val latestContent = creatorAnalyticsService.getLatestContent(subscription.channelId, limit = 3)
                    val unwatchedCount = creatorAnalyticsService.getUnwatchedContentCount(
                        userId, subscription.channelId
                    )

                    SubscriptionInfo(
                        subscription = subscription,
                        channel = channel,
                        latestContent = latestContent,
                        unwatchedCount = unwatchedCount,
                        totalWatchTime = subscription.totalWatchTime,
                        isLive = creatorAnalyticsService.isChannelLive(subscription.channelId)
                    )
                }
            }.awaitAll()

            val totalSubscriptions = subscriptionRepository.getUserSubscriptionCount(userId)

            PlayPodsSubscriptionsList(
                subscriptions = enhancedSubscriptions,
                totalCount = totalSubscriptions,
                hasMore = offset + limit < totalSubscriptions
            )

        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for user: $userId", e)
            throw e
        }
    }

    // Helper methods

    private suspend fun createDefaultPlaylists(channel: PlayPodsChannel) {
        val defaultPlaylists = listOf(
            "Liked Videos",
            "Watch Later",
            "Favorites"
        )

        defaultPlaylists.forEach { playlistName ->
            // This would integrate with playlist service
            logger.debug("Creating default playlist '$playlistName' for channel: ${channel.id}")
        }
    }

    private fun hasSignificantChanges(oldChannel: PlayPodsChannel, newChannel: PlayPodsChannel): Boolean {
        return oldChannel.displayName != newChannel.displayName ||
                oldChannel.description != newChannel.description ||
                oldChannel.category != newChannel.category ||
                oldChannel.keywords != newChannel.keywords
    }
}

// Data classes for PlayPods-specific features

data class CreatePlayPodsUserRequest(
    val entativaId: String,
    val email: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String?,
    val accountType: PlayPodsAccountType,
    val country: String,
    val language: String,
    val timezone: String,
    val channelName: String,
    val channelDisplayName: String?,
    val channelDescription: String?,
    val channelProfileImageUrl: String?,
    val channelBannerImageUrl: String?,
    val channelCategory: String,
    val channelKeywords: List<String>?,
    val creatorType: CreatorType?,
    val niche: String?,
    val targetAudience: String?,
    val contentStyle: String?,
    val uploadFrequency: UploadFrequency?,
    val equipmentLevel: EquipmentLevel?,
    val editingSkillLevel: SkillLevel?
)

data class CreateChannelRequest(
    val channelName: String,
    val displayName: String,
    val description: String?,
    val profileImageUrl: String?,
    val bannerImageUrl: String?,
    val category: String,
    val keywords: List<String>,
    val country: String,
    val language: String,
    val contentRating: ContentRating?,
    val defaultVisibility: VideoVisibility?
)

data class PlayPodsUser(
    val id: String,
    val entativaId: String,
    val email: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String?,
    val isVerified: Boolean,
    val accountType: PlayPodsAccountType,
    val country: String,
    val language: String,
    val timezone: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant,
    val totalChannels: Int,
    val totalSubscribers: Long,
    val totalViews: Long,
    val totalWatchTime: Long,
    val isActive: Boolean,
    val primaryChannelId: String
)

data class PlayPodsChannel(
    val id: String,
    val userId: String,
    val channelName: String,
    val displayName: String,
    val description: String?,
    val profileImageUrl: String?,
    val bannerImageUrl: String?,
    val category: String,
    val keywords: List<String>,
    val country: String,
    val language: String,
    val isVerified: Boolean,
    val isMonetized: Boolean,
    val subscribersCount: Long,
    val videosCount: Int,
    val totalViews: Long,
    val totalWatchTime: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: ChannelStatus,
    val contentRating: ContentRating,
    val allowComments: Boolean,
    val allowCommunityPosts: Boolean,
    val allowSubscriptions: Boolean,
    val showSubscriberCount: Boolean,
    val defaultUploadVisibility: VideoVisibility
)

data class PlayPodsSubscription(
    val id: String,
    val subscriberId: String,
    val channelId: String,
    val subscribedAt: Instant,
    val unsubscribedAt: Instant? = null,
    val isActive: Boolean,
    val notificationsEnabled: Boolean,
    val subscriptionSource: SubscriptionSource,
    val totalWatchTime: Long,
    val videosWatched: Int,
    val lastWatchedAt: Instant?
)

data class PlayPodsCreatorProfile(
    val userId: String,
    val channelId: String,
    val creatorType: CreatorType,
    val niche: String,
    val targetAudience: String,
    val contentStyle: String,
    val uploadFrequency: UploadFrequency,
    val avgVideoLength: Double,
    val engagementRate: Double,
    val retention90Day: Double,
    val clickThroughRate: Double,
    val subscriberGrowthRate: Double,
    val revenueGenerated: Double,
    val sponsorshipDeals: Int,
    val collaborationCount: Int,
    val equipmentLevel: EquipmentLevel,
    val editingSkillLevel: SkillLevel,
    val isEligibleForPartnerProgram: Boolean,
    val partnerProgramJoinedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant = Instant.now()
)

data class PlayPodsChannelStats(
    val channelId: String,
    val totalViews: Long,
    val totalWatchTime: Long,
    val totalSubscribers: Long,
    val totalVideos: Int,
    val totalPodcasts: Int,
    val totalPixels: Int,
    val totalLiveStreams: Int,
    val avgViewDuration: Double,
    val subscriberRetentionRate: Double,
    val top10VideosViews: Long,
    val revenueThisMonth: Double,
    val estimatedMinutesWatched: Long,
    val subscribersGained: Int,
    val subscribersLost: Int,
    val impressions: Long,
    val clickThroughRate: Double,
    val topGeographies: Map<String, Int>,
    val topAgeGroups: Map<String, Int>,
    val topDevices: Map<String, Int>,
    val peakViewingHours: List<Int>
)

data class PlayPodsChannelDashboard(
    val channel: PlayPodsChannel,
    val analytics: Any,
    val revenueData: Any?,
    val growthAnalysis: Any,
    val recentSubscribers: List<PlayPodsUser>,
    val topContent: List<Any>,
    val audienceInsights: Any,
    val growthRecommendations: List<String>,
    val monetizationStatus: Any?,
    val verificationStatus: Any
)

data class SubscriptionInfo(
    val subscription: PlayPodsSubscription,
    val channel: PlayPodsChannel,
    val latestContent: List<Any>,
    val unwatchedCount: Int,
    val totalWatchTime: Long,
    val isLive: Boolean
)

data class PlayPodsSubscriptionsList(
    val subscriptions: List<SubscriptionInfo>,
    val totalCount: Int,
    val hasMore: Boolean
)

data class VerificationApplication(
    val id: String,
    val channelId: String,
    val status: ApplicationStatus,
    val submittedAt: Instant,
    val reviewedAt: Instant?,
    val reason: String
)

data class MonetizationApplication(
    val id: String,
    val channelId: String,
    val status: ApplicationStatus,
    val submittedAt: Instant,
    val reviewedAt: Instant?,
    val watchHours: Long
)

enum class PlayPodsAccountType {
    PERSONAL, BRAND, ARTIST, BUSINESS, GOVERNMENT, NONPROFIT
}

enum class ChannelStatus {
    ACTIVE, SUSPENDED, TERMINATED, PENDING_REVIEW
}

enum class ContentRating {
    GENERAL, TEEN, MATURE, ADULT
}

enum class VideoVisibility {
    PUBLIC, UNLISTED, PRIVATE, SCHEDULED
}

enum class SubscriptionSource {
    CHANNEL_PAGE, VIDEO_PAGE, RECOMMENDATION, SEARCH, NOTIFICATION, LIVE_STREAM
}

enum class CreatorType {
    INDIVIDUAL, TEAM, COMPANY, BRAND, EDUCATIONAL, NONPROFIT
}

enum class UploadFrequency {
    DAILY, MULTIPLE_WEEKLY, WEEKLY, BIWEEKLY, MONTHLY, IRREGULAR
}

enum class EquipmentLevel {
    BASIC, INTERMEDIATE, PROFESSIONAL, STUDIO_GRADE
}

enum class SkillLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL
}

enum class ApplicationStatus {
    PENDING, UNDER_REVIEW, APPROVED, REJECTED, EXPIRED
}

enum class SubscriptionSortOption {
    RECENT_ACTIVITY, ALPHABETICAL, MOST_WATCHED, NEWEST_FIRST
}