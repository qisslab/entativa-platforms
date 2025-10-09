package com.pika.backend.services

import com.pika.backend.data.repositories.PostRepository
import com.pika.backend.data.repositories.UserRepository
import com.pika.backend.data.repositories.InteractionRepository
import com.pika.backend.data.repositories.ThreadRepository
import com.pika.backend.data.repositories.FollowRepository
import com.pika.backend.data.models.*
import com.pika.backend.auth.AuthenticationService
import com.pika.backend.notifications.NotificationService
import com.pika.backend.analytics.PostAnalyticsService
import com.pika.backend.algorithms.ThreadRankingEngine
import com.pika.backend.algorithms.ConversationAnalyzer
import com.pika.backend.messaging.EventPublisher
import com.pika.backend.validation.PostValidator
import com.pika.backend.content.ContentModerationService
import com.pika.backend.realtime.RealtimeUpdateService
import com.pika.backend.threading.ThreadManager
import com.pika.backend.trending.TrendingAnalyzer

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pika Post Service - Threads-like Real-time Conversation Management
 * Handles yeets, replies, threading, and real-time conversation interactions
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced conversation intelligence
 */
@Singleton
class PostService @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val interactionRepository: InteractionRepository,
    private val threadRepository: ThreadRepository,
    private val followRepository: FollowRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val postAnalyticsService: PostAnalyticsService,
    private val threadRankingEngine: ThreadRankingEngine,
    private val conversationAnalyzer: ConversationAnalyzer,
    private val eventPublisher: EventPublisher,
    private val postValidator: PostValidator,
    private val contentModerationService: ContentModerationService,
    private val realtimeUpdateService: RealtimeUpdateService,
    private val threadManager: ThreadManager,
    private val trendingAnalyzer: TrendingAnalyzer
) {
    
    private val logger = LoggerFactory.getLogger(PostService::class.java)
    
    companion object {
        const val MAX_YEET_LENGTH = 500      // Pika character limit
        const val MAX_THREAD_DEPTH = 10     // Maximum nested reply depth
        const val MAX_IMAGES_PER_YEET = 4   // Limited visual content for conversation focus
        const val YEET_EDIT_WINDOW_MINUTES = 15  // Can edit within 15 minutes
        const val TRENDING_THRESHOLD = 50   // Minimum interactions for trending consideration
        const val REAL_TIME_UPDATE_DELAY = 100  // Milliseconds for real-time updates
    }

    /**
     * Create a new yeet (Pika post) with real-time validation
     */
    suspend fun createYeet(request: CreatePikaYeetRequest): PikaYeet = coroutineScope {
        logger.info("Creating Pika yeet for user: ${request.userId}")
        
        try {
            // Validate user authentication
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            // Strict character limit validation
            if (request.content.length > MAX_YEET_LENGTH) {
                throw IllegalArgumentException("Yeet exceeds maximum length of $MAX_YEET_LENGTH characters (current: ${request.content.length})")
            }

            // Validate image count (limited for conversation focus)
            if (request.images.size > MAX_IMAGES_PER_YEET) {
                throw IllegalArgumentException("Yeet cannot have more than $MAX_IMAGES_PER_YEET images")
            }

            // Real-time content validation
            val validationResult = postValidator.validatePikaYeet(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Yeet validation failed: ${validationResult.errors.joinToString()}")
            }

            // Process any attached images
            val processedImages = if (request.images.isNotEmpty()) {
                request.images.map { image ->
                    async {
                        validateAndProcessPikaImage(image, request.userId)
                    }
                }.awaitAll()
            } else {
                emptyList()
            }

            // Advanced conversation analysis
            val conversationAnalysis = conversationAnalyzer.analyzeYeetContent(
                content = request.content,
                images = processedImages,
                mentions = extractMentions(request.content),
                hashtags = extractHashtags(request.content),
                userId = request.userId
            )

            // Content moderation with conversation context
            val moderationResult = contentModerationService.moderatePikaContent(
                text = request.content,
                images = processedImages,
                userId = request.userId,
                conversationContext = conversationAnalysis
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Yeet content violates community guidelines: ${moderationResult.reason}")
            }

            val yeetId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create yeet
            val yeet = PikaYeet(
                id = yeetId,
                userId = request.userId,
                content = request.content,
                images = processedImages,
                mentions = extractMentions(request.content),
                hashtags = extractHashtags(request.content),
                threadId = null, // This is a root yeet
                parentYeetId = null,
                createdAt = now,
                updatedAt = now,
                likesCount = 0,
                replyCount = 0,
                reyeetCount = 0,
                quotesCount = 0,
                viewsCount = 0,
                isEdited = false,
                isDeleted = false,
                conversationScore = conversationAnalysis.qualityScore,
                engagementPotential = conversationAnalysis.engagementPotential,
                sentimentScore = conversationAnalysis.sentimentScore,
                topicCategories = conversationAnalysis.topics,
                viralityPotential = conversationAnalysis.viralityScore,
                threadDepth = 0
            )

            val savedYeet = postRepository.save(yeet)

            // Create thread for this yeet
            val thread = threadManager.createThread(savedYeet)

            // Async operations
            async {
                // Real-time distribution to followers
                realtimeUpdateService.distributeYeetRealtime(savedYeet, user)
                
                // Add to thread ranking algorithm
                threadRankingEngine.indexYeet(savedYeet, conversationAnalysis)
                
                // Check for trending potential
                if (conversationAnalysis.viralityScore > 0.7) {
                    trendingAnalyzer.evaluateForTrending(savedYeet, conversationAnalysis)
                }
                
                // Send mention notifications in real-time
                if (extractMentions(request.content).isNotEmpty()) {
                    notificationService.sendPikaMentionNotifications(savedYeet, extractMentions(request.content))
                }
                
                // Track analytics
                postAnalyticsService.trackPikaYeetCreation(savedYeet, conversationAnalysis)
                
                // Publish real-time events
                eventPublisher.publishPikaYeetCreated(savedYeet)
            }

            logger.info("Successfully created Pika yeet: $yeetId")
            savedYeet

        } catch (e: Exception) {
            logger.error("Failed to create Pika yeet for user: ${request.userId}", e)
            throw e
        }
    }

    /**
     * Reply to a yeet with intelligent threading
     */
    suspend fun replyToYeet(request: CreatePikaReplyRequest): PikaYeet = coroutineScope {
        logger.info("Creating reply to yeet ${request.parentYeetId} by user ${request.userId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val parentYeet = postRepository.findById(request.parentYeetId)
                ?: throw IllegalArgumentException("Parent yeet not found: ${request.parentYeetId}")

            // Character limit validation
            if (request.content.length > MAX_YEET_LENGTH) {
                throw IllegalArgumentException("Reply exceeds maximum length of $MAX_YEET_LENGTH characters")
            }

            // Calculate thread depth
            val currentDepth = threadManager.calculateThreadDepth(request.parentYeetId)
            if (currentDepth >= MAX_THREAD_DEPTH) {
                throw IllegalArgumentException("Maximum thread depth of $MAX_THREAD_DEPTH reached")
            }

            // Validate reply structure
            val validationResult = postValidator.validatePikaReply(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Reply validation failed: ${validationResult.errors.joinToString()}")
            }

            // Process images if any
            val processedImages = if (request.images.isNotEmpty()) {
                request.images.map { image ->
                    async {
                        validateAndProcessPikaImage(image, request.userId)
                    }
                }.awaitAll()
            } else {
                emptyList()
            }

            // Conversation context analysis
            val conversationContext = conversationAnalyzer.analyzeReplyContext(
                replyContent = request.content,
                parentYeet = parentYeet,
                threadHistory = threadManager.getThreadHistory(parentYeet.threadId ?: parentYeet.id),
                userId = request.userId
            )

            // Content moderation with thread context
            val moderationResult = contentModerationService.moderatePikaReply(
                text = request.content,
                images = processedImages,
                parentYeet = parentYeet,
                conversationContext = conversationContext,
                userId = request.userId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Reply violates community guidelines: ${moderationResult.reason}")
            }

            val replyId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create reply
            val reply = PikaYeet(
                id = replyId,
                userId = request.userId,
                content = request.content,
                images = processedImages,
                mentions = extractMentions(request.content),
                hashtags = extractHashtags(request.content),
                threadId = parentYeet.threadId ?: parentYeet.id,
                parentYeetId = request.parentYeetId,
                createdAt = now,
                updatedAt = now,
                likesCount = 0,
                replyCount = 0,
                reyeetCount = 0,
                quotesCount = 0,
                viewsCount = 0,
                isEdited = false,
                isDeleted = false,
                conversationScore = conversationContext.relevanceScore,
                engagementPotential = conversationContext.engagementPotential,
                sentimentScore = conversationContext.sentimentScore,
                topicCategories = conversationContext.topics,
                viralityPotential = 0.0, // Replies typically don't go viral independently
                threadDepth = currentDepth + 1
            )

            val savedReply = postRepository.save(reply)

            // Update parent yeet reply count
            postRepository.incrementReplyCount(request.parentYeetId)

            // Update thread structure
            threadManager.addReplyToThread(reply.threadId!!, savedReply)

            // Async operations
            async {
                val parentYeetOwner = userRepository.findById(parentYeet.userId)!!
                
                // Real-time notification to parent yeet owner
                if (request.userId != parentYeet.userId) {
                    realtimeUpdateService.sendRealtimeReplyNotification(user, parentYeetOwner, parentYeet, savedReply)
                }
                
                // Real-time updates to thread participants
                val threadParticipants = threadManager.getThreadParticipants(reply.threadId!!)
                realtimeUpdateService.updateThreadParticipants(threadParticipants, savedReply)
                
                // Update thread ranking
                threadRankingEngine.updateThreadWithReply(savedReply, conversationContext)
                
                // Send mention notifications
                if (reply.mentions.isNotEmpty()) {
                    notificationService.sendPikaReplyMentionNotifications(savedReply, reply.mentions)
                }
                
                // Track conversation analytics
                postAnalyticsService.trackPikaReply(savedReply, parentYeet, conversationContext)
                
                // Publish real-time events
                eventPublisher.publishPikaReplyCreated(savedReply)
            }

            logger.info("Successfully created reply: $replyId")
            savedReply

        } catch (e: Exception) {
            logger.error("Failed to create reply to yeet ${request.parentYeetId}", e)
            throw e
        }
    }

    /**
     * Like a yeet with real-time engagement tracking
     */
    suspend fun likeYeet(userId: String, yeetId: String): PikaYeetInteraction = coroutineScope {
        logger.info("User $userId liking yeet $yeetId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            val yeet = postRepository.findById(yeetId)
                ?: throw IllegalArgumentException("Yeet not found: $yeetId")

            // Check if already liked
            val existingLike = interactionRepository.findLike(userId, yeetId)
            if (existingLike != null) {
                throw IllegalArgumentException("Yeet already liked by user")
            }

            val now = Instant.now()

            // Create like interaction
            val like = PikaYeetInteraction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                yeetId = yeetId,
                type = PikaInteractionType.LIKE,
                createdAt = now,
                isActive = true
            )

            val savedLike = interactionRepository.save(like)

            // Update yeet likes count
            postRepository.incrementLikesCount(yeetId)

            // Real-time operations
            async {
                val yeetOwner = userRepository.findById(yeet.userId)!!
                
                // Real-time like notification
                if (userId != yeet.userId) {
                    realtimeUpdateService.sendRealtimeLikeNotification(user, yeetOwner, yeet)
                }
                
                // Real-time UI updates for all viewers
                realtimeUpdateService.broadcastLikeUpdate(yeet, savedLike)
                
                // Update conversation engagement scoring
                conversationAnalyzer.recordEngagement(yeet, PikaInteractionType.LIKE, user)
                
                // Update thread ranking if part of thread
                yeet.threadId?.let { threadId ->
                    threadRankingEngine.recordThreadEngagement(threadId, PikaInteractionType.LIKE)
                }
                
                // Check for trending potential
                if (yeet.likesCount + 1 >= TRENDING_THRESHOLD) {
                    trendingAnalyzer.evaluateYeetForTrending(yeet)
                }
                
                // Track analytics
                postAnalyticsService.trackPikaInteraction(savedLike, user, yeet)
                
                // Publish real-time events
                eventPublisher.publishPikaYeetLiked(savedLike)
            }

            logger.info("Successfully liked yeet: $yeetId")
            savedLike

        } catch (e: Exception) {
            logger.error("Failed to like yeet $yeetId by user $userId", e)
            throw e
        }
    }

    /**
     * Reyeet (retweet equivalent) with optional quote
     */
    suspend fun reyeet(request: CreatePikaReyeetRequest): PikaReyeet = coroutineScope {
        logger.info("User ${request.userId} reyeeting ${request.originalYeetId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val originalYeet = postRepository.findById(request.originalYeetId)
                ?: throw IllegalArgumentException("Original yeet not found: ${request.originalYeetId}")

            // Validate quote if provided
            request.quoteText?.let { quote ->
                if (quote.length > MAX_YEET_LENGTH) {
                    throw IllegalArgumentException("Quote text exceeds maximum length")
                }
            }

            // Check if already reyeeted
            val existingReyeet = interactionRepository.findReyeet(request.userId, request.originalYeetId)
            if (existingReyeet != null) {
                throw IllegalArgumentException("Yeet already reyeeted by user")
            }

            val reyeetId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create reyeet
            val reyeet = PikaReyeet(
                id = reyeetId,
                userId = request.userId,
                originalYeetId = request.originalYeetId,
                quoteText = request.quoteText,
                createdAt = now,
                likesCount = 0,
                replyCount = 0,
                reyeetCount = 0,
                isDeleted = false
            )

            val savedReyeet = interactionRepository.saveReyeet(reyeet)

            // Update original yeet reyeet count
            postRepository.incrementReyeetCount(request.originalYeetId)

            // Async operations
            async {
                val originalYeetOwner = userRepository.findById(originalYeet.userId)!!
                
                // Real-time distribution to user's followers
                realtimeUpdateService.distributeReyeetRealtime(savedReyeet, user, originalYeet)
                
                // Notification to original yeet owner
                if (request.userId != originalYeet.userId) {
                    realtimeUpdateService.sendRealtimeReyeetNotification(user, originalYeetOwner, originalYeet)
                }
                
                // Update engagement metrics
                conversationAnalyzer.recordEngagement(originalYeet, PikaInteractionType.REYEET, user)
                
                // Check for trending boost
                trendingAnalyzer.evaluateReyeetImpact(originalYeet, savedReyeet)
                
                // Track analytics
                postAnalyticsService.trackPikaReyeet(savedReyeet, user, originalYeet)
                
                // Publish events
                eventPublisher.publishPikaReyeetCreated(savedReyeet)
            }

            logger.info("Successfully created reyeet: $reyeetId")
            savedReyeet

        } catch (e: Exception) {
            logger.error("Failed to reyeet ${request.originalYeetId} by user ${request.userId}", e)
            throw e
        }
    }

    /**
     * Get thread conversation with intelligent ranking
     */
    suspend fun getThread(
        threadId: String,
        userId: String,
        limit: Int = 20,
        sortBy: ThreadSortOption = ThreadSortOption.CHRONOLOGICAL
    ): PikaThreadResponse = coroutineScope {
        logger.debug("Getting thread $threadId for user $userId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Get thread structure
            val thread = threadManager.getThread(threadId)
                ?: throw IllegalArgumentException("Thread not found: $threadId")

            // Get thread yeets with intelligent ranking
            val threadYeets = threadRankingEngine.getRankedThreadYeets(
                threadId = threadId,
                viewerUserId = userId,
                limit = limit,
                sortBy = sortBy
            )

            // Enhance with user interactions
            val enhancedYeets = threadYeets.map { yeet ->
                async {
                    val userInteractions = interactionRepository.getUserYeetInteractions(userId, yeet.id)
                    val yeetOwner = userRepository.findById(yeet.userId)!!
                    val isFollowing = followRepository.isFollowing(userId, yeet.userId)
                    
                    PikaThreadYeet(
                        yeet = yeet,
                        owner = yeetOwner,
                        isFollowing = isFollowing,
                        isLikedByUser = userInteractions.any { it.type == PikaInteractionType.LIKE },
                        hasReplied = threadYeets.any { it.parentYeetId == yeet.id && it.userId == userId },
                        conversationRelevance = conversationAnalyzer.calculateRelevanceToUser(yeet, user),
                        timeInThread = java.time.Duration.between(thread.createdAt, yeet.createdAt)
                    )
                }
            }.awaitAll()

            // Track thread view
            async {
                postAnalyticsService.trackThreadView(userId, threadId, enhancedYeets.size)
            }

            PikaThreadResponse(
                threadId = threadId,
                yeets = enhancedYeets,
                participantCount = threadManager.getParticipantCount(threadId),
                totalYeets = threadManager.getTotalYeetsInThread(threadId),
                conversationQuality = conversationAnalyzer.getThreadQualityScore(threadId),
                isActive = threadManager.isThreadActive(threadId),
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to get thread $threadId for user $userId", e)
            throw e
        }
    }

    /**
     * Get user's real-time feed
     */
    suspend fun getRealtimeFeed(
        userId: String,
        limit: Int = 20,
        includeReplies: Boolean = false
    ): PikaFeedResponse = coroutineScope {
        logger.debug("Getting real-time feed for user: $userId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Get real-time ranked feed
            val feedYeets = threadRankingEngine.generateRealtimeFeed(
                user = user,
                limit = limit,
                includeReplies = includeReplies,
                prioritizeConversations = true
            )

            // Enhance with real-time data
            val enhancedYeets = feedYeets.map { yeet ->
                async {
                    val userInteractions = interactionRepository.getUserYeetInteractions(userId, yeet.id)
                    val yeetOwner = userRepository.findById(yeet.userId)!!
                    val isFollowing = followRepository.isFollowing(userId, yeet.userId)
                    
                    PikaFeedYeet(
                        yeet = yeet,
                        owner = yeetOwner,
                        isFollowing = isFollowing,
                        isLikedByUser = userInteractions.any { it.type == PikaInteractionType.LIKE },
                        hasInteracted = userInteractions.isNotEmpty(),
                        conversationScore = yeet.conversationScore,
                        trendingReason = if (trendingAnalyzer.isTrending(yeet.id)) {
                            trendingAnalyzer.getTrendingReason(yeet.id)
                        } else null,
                        realtimeActivity = realtimeUpdateService.getRealtimeActivity(yeet.id)
                    )
                }
            }.awaitAll()

            // Track feed view
            async {
                postAnalyticsService.trackPikaFeedView(user, enhancedYeets.size)
            }

            PikaFeedResponse(
                yeets = enhancedYeets,
                hasMore = feedYeets.size == limit,
                includeReplies = includeReplies,
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to get real-time feed for user: $userId", e)
            throw e
        }
    }

    // Helper methods

    private suspend fun validateAndProcessPikaImage(
        image: PikaImageItem,
        userId: String
    ): ProcessedPikaImage {
        // Basic validation for conversation-focused platform
        val supportedFormats = setOf("jpg", "jpeg", "png", "gif", "webp")
        val fileExtension = image.url.substringAfterLast(".", "").lowercase()
        
        if (!supportedFormats.contains(fileExtension)) {
            throw IllegalArgumentException("Unsupported image format: $fileExtension")
        }

        if (image.sizeBytes > 5 * 1024 * 1024) { // 5MB limit for Pika
            throw IllegalArgumentException("Image size exceeds 5MB limit")
        }

        // Process with conversation context optimization
        return ProcessedPikaImage(
            originalUrl = image.url,
            processedUrl = image.url, // Simplified for now
            thumbnailUrl = "${image.url}_thumb",
            width = image.width,
            height = image.height,
            sizeBytes = image.sizeBytes
        )
    }

    private fun extractMentions(content: String): List<String> {
        val mentionRegex = "@\\w+".toRegex()
        return mentionRegex.findAll(content).map { it.value.substring(1) }.distinct().toList()
    }

    private fun extractHashtags(content: String): List<String> {
        val hashtagRegex = "#\\w+".toRegex()
        return hashtagRegex.findAll(content).map { it.value.substring(1).lowercase() }.distinct().toList()
    }
}

// Data classes for Pika-specific features

data class CreatePikaYeetRequest(
    val userId: String,
    val content: String,
    val images: List<PikaImageItem> = emptyList()
)

data class CreatePikaReplyRequest(
    val userId: String,
    val parentYeetId: String,
    val content: String,
    val images: List<PikaImageItem> = emptyList()
)

data class CreatePikaReyeetRequest(
    val userId: String,
    val originalYeetId: String,
    val quoteText: String? = null
)

data class PikaYeet(
    val id: String,
    val userId: String,
    val content: String,
    val images: List<ProcessedPikaImage>,
    val mentions: List<String>,
    val hashtags: List<String>,
    val threadId: String?,
    val parentYeetId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val likesCount: Int,
    val replyCount: Int,
    val reyeetCount: Int,
    val quotesCount: Int,
    val viewsCount: Int,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val conversationScore: Double,
    val engagementPotential: Double,
    val sentimentScore: Double,
    val topicCategories: List<String>,
    val viralityPotential: Double,
    val threadDepth: Int
)

data class PikaReyeet(
    val id: String,
    val userId: String,
    val originalYeetId: String,
    val quoteText: String?,
    val createdAt: Instant,
    val likesCount: Int,
    val replyCount: Int,
    val reyeetCount: Int,
    val isDeleted: Boolean
)

data class PikaYeetInteraction(
    val id: String,
    val userId: String,
    val yeetId: String,
    val type: PikaInteractionType,
    val createdAt: Instant,
    val isActive: Boolean
)

data class PikaThreadYeet(
    val yeet: PikaYeet,
    val owner: Any, // User object
    val isFollowing: Boolean,
    val isLikedByUser: Boolean,
    val hasReplied: Boolean,
    val conversationRelevance: Double,
    val timeInThread: java.time.Duration
)

data class PikaFeedYeet(
    val yeet: PikaYeet,
    val owner: Any, // User object
    val isFollowing: Boolean,
    val isLikedByUser: Boolean,
    val hasInteracted: Boolean,
    val conversationScore: Double,
    val trendingReason: String?,
    val realtimeActivity: RealtimeActivity
)

data class PikaThreadResponse(
    val threadId: String,
    val yeets: List<PikaThreadYeet>,
    val participantCount: Int,
    val totalYeets: Int,
    val conversationQuality: Double,
    val isActive: Boolean,
    val timestamp: Instant
)

data class PikaFeedResponse(
    val yeets: List<PikaFeedYeet>,
    val hasMore: Boolean,
    val includeReplies: Boolean,
    val timestamp: Instant
)

data class PikaImageItem(
    val url: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

data class ProcessedPikaImage(
    val originalUrl: String,
    val processedUrl: String,
    val thumbnailUrl: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

data class RealtimeActivity(
    val recentLikes: Int,
    val recentReplies: Int,
    val activeViewers: Int,
    val lastActivity: Instant
)

enum class PikaInteractionType {
    LIKE, REPLY, REYEET, QUOTE, VIEW
}

enum class ThreadSortOption {
    CHRONOLOGICAL, RELEVANCE, ENGAGEMENT, RECENT_ACTIVITY
}