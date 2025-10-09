package com.entativa.pika.yeeting

import com.entativa.posting.proto.*
import com.entativa.pika.core.auth.AuthenticationService
import com.entativa.pika.core.validation.YeetValidator
import com.entativa.pika.data.repositories.YeetRepository
import com.entativa.pika.data.repositories.ThreadRepository
import com.entativa.pika.data.repositories.UserRepository
import com.entativa.pika.messaging.EventPublisher
import com.entativa.pika.media.MediaProcessingService
import com.entativa.pika.analytics.AnalyticsService
import com.entativa.pika.algorithms.ThreadDistributionEngine
import com.entativa.pika.content.ContentModerationService
import com.entativa.pika.notifications.NotificationService
import com.entativa.pika.algorithms.TrendingEngine
import com.entativa.pika.algorithms.RealtimeEngine
import com.entativa.pika.ai.ContextAnalyzer
import com.entativa.pika.threading.ThreadManager

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pika Yeeting Service - Threads-like real-time conversation platform
 * Handles yeets (posts), replies, threads with advanced conversation management
 */
@Singleton
class PikaYeetingService @Inject constructor(
    private val yeetRepository: YeetRepository,
    private val threadRepository: ThreadRepository,
    private val userRepository: UserRepository,
    private val authService: AuthenticationService,
    private val yeetValidator: YeetValidator,
    private val mediaProcessingService: MediaProcessingService,
    private val contentModerationService: ContentModerationService,
    private val threadDistributionEngine: ThreadDistributionEngine,
    private val analyticsService: AnalyticsService,
    private val eventPublisher: EventPublisher,
    private val notificationService: NotificationService,
    private val trendingEngine: TrendingEngine,
    private val realtimeEngine: RealtimeEngine,
    private val contextAnalyzer: ContextAnalyzer,
    private val threadManager: ThreadManager
) : PostingServiceGrpcKt.PostingServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(PikaYeetingService::class.java)
    
    companion object {
        const val MAX_YEET_LENGTH = 500 // Pika's character limit
        const val MAX_THREAD_DEPTH = 10 // Maximum reply depth
        const val TRENDING_THRESHOLD = 100 // Engagement threshold for trending
    }

    override suspend fun createPost(request: CreatePostRequest): Post = coroutineScope {
        logger.info("Creating Pika ${request.postType} for user: ${request.userId}")
        
        try {
            // Validate authentication
            val user = authService.validateUserSession(request.userId)
                ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid user session"))

            // Validate yeet structure and content
            val validationResult = yeetValidator.validateYeet(request)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Yeet validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Check character limit
            if (request.content.length > MAX_YEET_LENGTH) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Yeet exceeds maximum length of $MAX_YEET_LENGTH characters")
                )
            }

            // Process media (Pika supports limited media - mainly images and GIFs)
            val processedMedia = if (request.mediaList.isNotEmpty()) {
                request.mediaList.map { media ->
                    async {
                        mediaProcessingService.processPikaMedia(
                            url = media.url,
                            contentType = request.contentType,
                            maxSize = 5 * 1024 * 1024, // 5MB limit
                            userId = request.userId
                        )
                    }
                }.awaitAll()
            } else {
                emptyList()
            }

            // Advanced content moderation with real-time analysis
            val moderationResult = contentModerationService.moderatePikaContent(
                text = request.content,
                media = processedMedia,
                userId = request.userId,
                parentYeetId = request.parentPostId,
                isReply = request.postType == PostType.PIKA_REPLY
            )

            if (moderationResult.isBlocked) {
                logger.warn("Pika yeet blocked by content moderation for user: ${request.userId}")
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Content violates community guidelines: ${moderationResult.reason}")
                )
            }

            // Analyze context and extract insights
            val contextAnalysis = contextAnalyzer.analyzeYeetContext(
                content = request.content,
                mentions = request.mentionsList,
                hashtags = request.hashtagsList,
                parentYeetId = request.parentPostId,
                userId = request.userId
            )

            val yeetId = UUID.randomUUID().toString()
            val now = Instant.now()
            
            // Create yeet based on type
            val yeet = when (request.postType) {
                PostType.PIKA_YEET -> createOriginalYeet(request, yeetId, processedMedia, contextAnalysis, now)
                PostType.PIKA_REPLY -> createReplyYeet(request, yeetId, processedMedia, contextAnalysis, now)
                else -> throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Unsupported post type for Pika: ${request.postType}")
                )
            }

            // Handle threading logic
            val threadInfo = if (request.postType == PostType.PIKA_REPLY) {
                handleThreading(request.parentPostId, yeet)
            } else {
                null
            }

            // Save yeet to repository
            val savedYeet = yeetRepository.createYeet(yeet)

            // Update thread structure if it's a reply
            threadInfo?.let { 
                threadRepository.addReplyToThread(it.threadId, savedYeet)
            }

            // Async post-creation operations
            async {
                // Real-time distribution to feeds and followers
                threadDistributionEngine.distributeYeet(savedYeet, user, threadInfo)
                
                // Send real-time updates to connected clients
                realtimeEngine.broadcastYeetCreated(savedYeet)
                
                // Handle notifications for mentions and replies
                if (request.mentionsList.isNotEmpty()) {
                    notificationService.sendMentionNotifications(savedYeet, request.mentionsList)
                }
                
                if (request.postType == PostType.PIKA_REPLY && request.parentPostId.isNotEmpty()) {
                    notificationService.sendReplyNotification(savedYeet, request.parentPostId)
                }
                
                // Check for trending potential
                if (contextAnalysis.trendingScore > TRENDING_THRESHOLD) {
                    trendingEngine.evaluateForTrending(savedYeet, contextAnalysis)
                }
                
                // Publish events for other services
                eventPublisher.publishPikaYeetCreated(savedYeet, threadInfo)
                
                // Track analytics
                analyticsService.trackPikaYeetCreation(savedYeet, user, contextAnalysis)
            }

            // Handle cross-posting if enabled
            if (request.crossPostEnabled && request.crossPostPlatformsList.isNotEmpty()) {
                async {
                    handlePikaCrossPosting(savedYeet, request.crossPostPlatformsList)
                }
            }

            logger.info("Successfully created Pika ${request.postType}: $yeetId")
            savedYeet

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to create Pika yeet for user: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to create yeet: ${e.message}"))
        }
    }

    private suspend fun createOriginalYeet(
        request: CreatePostRequest,
        yeetId: String,
        processedMedia: List<ProcessedMediaMetadata>,
        contextAnalysis: YeetContextAnalysis,
        now: Instant
    ): Post {
        return Post.newBuilder()
            .setId(yeetId)
            .setUserId(request.userId)
            .setPostType(PostType.PIKA_YEET)
            .setContentType(request.contentType)
            .setContent(request.content)
            .addAllMedia(processedMedia.map { it.toProto() })
            .setPrivacy(request.privacy)
            .setLocation(request.location)
            .addAllHashtags(request.hashtagsList)
            .addAllMentions(request.mentionsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(extractPikaSpecificData(request, contextAnalysis))
            .build()
    }

    private suspend fun createReplyYeet(
        request: CreatePostRequest,
        yeetId: String,
        processedMedia: List<ProcessedMediaMetadata>,
        contextAnalysis: YeetContextAnalysis,
        now: Instant
    ): Post {
        // Validate parent yeet exists
        val parentYeet = yeetRepository.getYeetById(request.parentPostId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Parent yeet not found"))

        // Check thread depth
        val threadDepth = threadManager.calculateThreadDepth(request.parentPostId)
        if (threadDepth >= MAX_THREAD_DEPTH) {
            throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Maximum thread depth of $MAX_THREAD_DEPTH reached")
            )
        }

        return Post.newBuilder()
            .setId(yeetId)
            .setUserId(request.userId)
            .setPostType(PostType.PIKA_REPLY)
            .setContentType(request.contentType)
            .setContent(request.content)
            .addAllMedia(processedMedia.map { it.toProto() })
            .setPrivacy(request.privacy)
            .setLocation(request.location)
            .addAllHashtags(request.hashtagsList)
            .addAllMentions(request.mentionsList)
            .setParentPostId(request.parentPostId)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(buildMap {
                putAll(extractPikaSpecificData(request, contextAnalysis))
                put("thread_depth", threadDepth.toString())
                put("parent_yeet_id", request.parentPostId)
                put("parent_user_id", parentYeet.userId)
                put("is_reply", "true")
            })
            .build()
    }

    private suspend fun handleThreading(parentYeetId: String, yeet: Post): ThreadInfo {
        // Get or create thread structure
        val thread = threadRepository.getThreadByYeetId(parentYeetId)
            ?: threadManager.createNewThread(parentYeetId)

        // Update thread statistics
        threadManager.updateThreadStats(thread.threadId, yeet)

        return ThreadInfo(
            threadId = thread.threadId,
            rootYeetId = thread.rootYeetId,
            depth = threadManager.calculateThreadDepth(parentYeetId) + 1,
            participantCount = threadManager.getThreadParticipantCount(thread.threadId)
        )
    }

    override suspend fun updatePost(request: UpdatePostRequest): Post = coroutineScope {
        logger.info("Updating Pika yeet: ${request.postId}")
        
        try {
            val existingYeet = yeetRepository.getYeetById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Yeet not found"))

            // Validate user ownership
            if (existingYeet.userId != request.userId) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("User not authorized to update this yeet")
                )
            }

            // Pika allows limited editing (within 15 minutes and if no replies)
            val now = Instant.now()
            val createdAt = Instant.ofEpochSecond(existingYeet.createdAt)
            val editWindow = java.time.Duration.between(createdAt, now)
            
            if (editWindow.toMinutes() > 15) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Yeet can only be edited within 15 minutes of creation")
                )
            }

            if (existingYeet.commentCount > 0) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Yeet cannot be edited after receiving replies")
                )
            }

            // Validate updated content
            if (request.content.length > MAX_YEET_LENGTH) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Updated yeet exceeds maximum length of $MAX_YEET_LENGTH characters")
                )
            }

            // Re-moderate updated content
            val moderationResult = contentModerationService.moderatePikaContent(
                text = request.content,
                media = existingYeet.mediaList,
                userId = request.userId,
                isEdit = true
            )

            if (moderationResult.isBlocked) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Updated content violates community guidelines")
                )
            }

            // Create updated yeet
            val updatedYeet = existingYeet.toBuilder()
                .setContent(request.content)
                .setUpdatedAt(now.epochSecond)
                .setIsEdited(true)
                .putPlatformSpecificData("edit_count", 
                    (existingYeet.platformSpecificDataMap["edit_count"]?.toIntOrNull() ?: 0 + 1).toString()
                )
                .build()

            val savedYeet = yeetRepository.updateYeet(updatedYeet)

            // Async operations
            async {
                // Broadcast update to real-time clients
                realtimeEngine.broadcastYeetUpdated(savedYeet, existingYeet)
                
                // Re-evaluate trending status
                val contextAnalysis = contextAnalyzer.analyzeYeetContext(
                    content = savedYeet.content,
                    mentions = savedYeet.mentionsList,
                    hashtags = savedYeet.hashtagsList,
                    parentYeetId = savedYeet.parentPostId,
                    userId = savedYeet.userId
                )
                trendingEngine.reevaluateForTrending(savedYeet, contextAnalysis)
                
                eventPublisher.publishPikaYeetUpdated(savedYeet, existingYeet)
                analyticsService.trackPikaYeetUpdate(savedYeet)
            }

            logger.info("Successfully updated Pika yeet: ${request.postId}")
            savedYeet

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update Pika yeet: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to update yeet: ${e.message}"))
        }
    }

    override suspend fun deletePost(request: DeletePostRequest): DeletePostResponse = coroutineScope {
        logger.info("Deleting Pika yeet: ${request.postId}")
        
        try {
            val existingYeet = yeetRepository.getYeetById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Yeet not found"))

            // Validate user ownership or admin privileges
            if (existingYeet.userId != request.userId) {
                val user = userRepository.getUserById(request.userId)
                if (user?.role != "ADMIN" && user?.role != "MODERATOR") {
                    throw StatusException(
                        Status.PERMISSION_DENIED.withDescription("User not authorized to delete this yeet")
                    )
                }
            }

            // Handle thread cleanup if this yeet has replies
            val threadInfo = if (existingYeet.commentCount > 0) {
                threadManager.handleYeetDeletion(existingYeet)
            } else {
                null
            }

            // Soft delete the yeet
            val deletedYeet = existingYeet.toBuilder()
                .setIsDeleted(true)
                .setContent("[This yeet was deleted]")
                .setUpdatedAt(Instant.now().epochSecond)
                .build()

            yeetRepository.updateYeet(deletedYeet)

            // Async cleanup operations
            async {
                // Remove from trending if applicable
                trendingEngine.removeFromTrending(deletedYeet.id)
                
                // Update thread structure
                threadInfo?.let { 
                    threadRepository.updateThreadAfterDeletion(it.threadId, deletedYeet)
                }
                
                // Clean up media
                mediaProcessingService.cleanupPikaMedia(deletedYeet.mediaList)
                
                // Broadcast deletion to real-time clients
                realtimeEngine.broadcastYeetDeleted(deletedYeet)
                
                eventPublisher.publishPikaYeetDeleted(deletedYeet)
                analyticsService.trackPikaYeetDeletion(deletedYeet)
            }

            logger.info("Successfully deleted Pika yeet: ${request.postId}")
            
            DeletePostResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Yeet deleted successfully")
                .build()

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete Pika yeet: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to delete yeet: ${e.message}"))
        }
    }

    override suspend fun getUserPosts(request: GetUserPostsRequest): GetUserPostsResponse {
        logger.debug("Fetching Pika yeets for user: ${request.userId}")
        
        try {
            val platforms = request.platformsList.ifEmpty { 
                listOf(PostType.PIKA_YEET, PostType.PIKA_REPLY) 
            }
            
            val yeets = yeetRepository.getUserYeets(
                userId = request.userId,
                platforms = platforms,
                limit = request.limit.takeIf { it > 0 } ?: 50,
                offset = request.offset,
                since = if (request.since > 0) Instant.ofEpochSecond(request.since) else null,
                until = if (request.until > 0) Instant.ofEpochSecond(request.until) else null,
                includeReplies = platforms.contains(PostType.PIKA_REPLY)
            )

            return GetUserPostsResponse.newBuilder()
                .addAllPosts(yeets.items)
                .setHasMore(yeets.hasMore)
                .setNextCursor(yeets.nextCursor ?: "")
                .build()

        } catch (e: Exception) {
            logger.error("Failed to fetch Pika user yeets: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to fetch yeets: ${e.message}"))
        }
    }

    // Helper methods

    private fun extractPikaSpecificData(
        request: CreatePostRequest, 
        contextAnalysis: YeetContextAnalysis
    ): Map<String, String> {
        return buildMap {
            // Pika-specific features
            put("trending_score", contextAnalysis.trendingScore.toString())
            put("sentiment", contextAnalysis.sentiment)
            put("language", contextAnalysis.language)
            put("toxicity_score", contextAnalysis.toxicityScore.toString())
            put("engagement_prediction", contextAnalysis.engagementPrediction.toString())
            put("topic_categories", contextAnalysis.topicCategories.joinToString(","))
            put("reply_probability", contextAnalysis.replyProbability.toString())
            put("virality_score", contextAnalysis.viralityScore.toString())
            put("character_count", request.content.length.toString())
            put("has_media", request.mediaList.isNotEmpty().toString())
            put("mention_count", request.mentionsList.size.toString())
            put("hashtag_count", request.hashtagsList.size.toString())
            put("is_thread_starter", (request.parentPostId.isEmpty()).toString())
            put("real_time", "true") // All Pika yeets are real-time
        }
    }

    private suspend fun handlePikaCrossPosting(yeet: Post, targetPlatforms: List<PostType>) {
        targetPlatforms.forEach { platform ->
            try {
                when (platform) {
                    PostType.SONET_POST -> {
                        // Convert to Sonet format
                        val crossPostRequest = createSonetCrossPost(yeet)
                        sonetPostingClient.createPost(crossPostRequest)
                    }
                    PostType.GALA_POST -> {
                        // Convert to Gala format (if has media)
                        if (yeet.mediaList.isNotEmpty()) {
                            val crossPostRequest = createGalaCrossPost(yeet)
                            galaPostingClient.createPost(crossPostRequest)
                        }
                    }
                    else -> {
                        logger.warn("Cross-posting from Pika to $platform not supported yet")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to cross-post Pika yeet to $platform", e)
            }
        }
    }

    private fun createSonetCrossPost(pikaYeet: Post): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(pikaYeet.userId)
            .setPostType(PostType.SONET_POST)
            .setContentType(pikaYeet.contentType)
            .setContent("${pikaYeet.content}\n\n🐦 Yeeted from Pika")
            .addAllMedia(pikaYeet.mediaList)
            .setPrivacy(pikaYeet.privacy)
            .addAllHashtags(pikaYeet.hashtagsList)
            .addAllMentions(pikaYeet.mentionsList)
            .build()
    }

    private fun createGalaCrossPost(pikaYeet: Post): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(pikaYeet.userId)
            .setPostType(PostType.GALA_POST)
            .setContentType(pikaYeet.contentType)
            .setContent(pikaYeet.content)
            .addAllMedia(pikaYeet.mediaList)
            .setPrivacy(pikaYeet.privacy)
            .addAllHashtags(pikaYeet.hashtagsList)
            .build()
    }

    // Injected gRPC clients
    @Inject
    lateinit var sonetPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    
    @Inject
    lateinit var galaPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
}

// Data classes for Pika-specific features
data class YeetContextAnalysis(
    val trendingScore: Double,
    val sentiment: String,
    val language: String,
    val toxicityScore: Double,
    val engagementPrediction: Double,
    val topicCategories: List<String>,
    val replyProbability: Double,
    val viralityScore: Double
)

data class ThreadInfo(
    val threadId: String,
    val rootYeetId: String,
    val depth: Int,
    val participantCount: Int
)

data class ProcessedMediaMetadata(
    val originalUrl: String,
    val processedUrl: String,
    val thumbnailUrl: String,
    val altText: String,
    val fileSize: Long,
    val mimeType: String
) {
    fun toProto(): MediaMetadata {
        return MediaMetadata.newBuilder()
            .setUrl(processedUrl)
            .setThumbnailUrl(thumbnailUrl)
            .setAltText(altText)
            .setSizeBytes(fileSize)
            .setMimeType(mimeType)
            .build()
    }
}