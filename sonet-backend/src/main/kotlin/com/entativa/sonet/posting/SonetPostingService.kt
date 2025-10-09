package com.entativa.sonet.posting

import com.entativa.posting.proto.*
import com.entativa.sonet.core.auth.AuthenticationService
import com.entativa.sonet.core.validation.PostValidator
import com.entativa.sonet.data.repositories.PostRepository
import com.entativa.sonet.data.repositories.UserRepository
import com.entativa.sonet.messaging.EventPublisher
import com.entativa.sonet.media.MediaProcessingService
import com.entativa.sonet.analytics.AnalyticsService
import com.entativa.sonet.algorithms.FeedDistributionEngine
import com.entativa.sonet.content.ContentModerationService
import com.entativa.sonet.notifications.NotificationService

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
 * Sonet Posting Service - Facebook-like social posting with advanced features
 * Handles post creation, validation, media processing, and feed distribution
 */
@Singleton
class SonetPostingService @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val authService: AuthenticationService,
    private val postValidator: PostValidator,
    private val mediaProcessingService: MediaProcessingService,
    private val contentModerationService: ContentModerationService,
    private val feedDistributionEngine: FeedDistributionEngine,
    private val analyticsService: AnalyticsService,
    private val eventPublisher: EventPublisher,
    private val notificationService: NotificationService
) : PostingServiceGrpcKt.PostingServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(SonetPostingService::class.java)

    override suspend fun createPost(request: CreatePostRequest): Post = coroutineScope {
        logger.info("Creating Sonet post for user: ${request.userId}")
        
        try {
            // Validate authentication
            val user = authService.validateUserSession(request.userId)
                ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid user session"))

            // Validate post content and structure
            val validationResult = postValidator.validatePost(request)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Post validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Process media concurrently
            val processedMedia = if (request.mediaList.isNotEmpty()) {
                request.mediaList.map { media ->
                    async {
                        mediaProcessingService.processMedia(
                            url = media.url,
                            contentType = request.contentType,
                            userId = request.userId,
                            altText = media.altText
                        )
                    }
                }.awaitAll()
            } else {
                emptyList()
            }

            // Content moderation check
            val moderationResult = contentModerationService.moderateContent(
                text = request.content,
                media = processedMedia,
                userId = request.userId
            )

            if (moderationResult.isBlocked) {
                logger.warn("Post blocked by content moderation for user: ${request.userId}")
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Content violates community guidelines")
                )
            }

            // Create post entity
            val postId = UUID.randomUUID().toString()
            val now = Instant.now()
            
            val post = Post.newBuilder()
                .setId(postId)
                .setUserId(request.userId)
                .setPostType(PostType.SONET_POST)
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
                .putAllPlatformSpecificData(extractSonetSpecificData(request))
                .build()

            // Save to database with transaction
            val savedPost = postRepository.createPost(post)

            // Async operations that don't block response
            async {
                // Distribute to user feeds
                feedDistributionEngine.distributePost(savedPost, user)
                
                // Send notifications for mentions
                if (request.mentionsList.isNotEmpty()) {
                    notificationService.sendMentionNotifications(savedPost, request.mentionsList)
                }
                
                // Publish event for real-time updates
                eventPublisher.publishPostCreated(savedPost)
                
                // Track analytics
                analyticsService.trackPostCreation(savedPost, user)
            }

            // Handle cross-posting if enabled
            if (request.crossPostEnabled && request.crossPostPlatformsList.isNotEmpty()) {
                async {
                    handleCrossPosting(savedPost, request.crossPostPlatformsList)
                }
            }

            logger.info("Successfully created Sonet post: $postId")
            savedPost

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to create Sonet post for user: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to create post: ${e.message}"))
        }
    }

    override suspend fun getPost(request: GetPostRequest): Post {
        logger.debug("Fetching Sonet post: ${request.postId}")
        
        return postRepository.getPostById(request.postId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Post not found"))
    }

    override suspend fun updatePost(request: UpdatePostRequest): Post = coroutineScope {
        logger.info("Updating Sonet post: ${request.postId}")
        
        try {
            val existingPost = postRepository.getPostById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Post not found"))

            // Validate user ownership
            if (existingPost.userId != request.userId) {
                throw StatusException(Status.PERMISSION_DENIED.withDescription("User not authorized to update this post"))
            }

            // Validate update request
            val validationResult = postValidator.validatePostUpdate(request, existingPost)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Update validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Content moderation for updated content
            val moderationResult = contentModerationService.moderateContent(
                text = request.content,
                media = existingPost.mediaList,
                userId = request.userId
            )

            if (moderationResult.isBlocked) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Updated content violates community guidelines")
                )
            }

            // Create updated post
            val updatedPost = existingPost.toBuilder()
                .setContent(request.content)
                .setPrivacy(request.privacy)
                .clearHashtags()
                .addAllHashtags(request.hashtagsList)
                .setLocation(request.location)
                .setUpdatedAt(Instant.now().epochSecond)
                .setIsEdited(true)
                .build()

            // Save updated post
            val savedPost = postRepository.updatePost(updatedPost)

            // Async operations
            async {
                // Re-distribute to feeds if privacy changed
                if (existingPost.privacy != request.privacy) {
                    feedDistributionEngine.redistributePost(savedPost)
                }
                
                // Publish update event
                eventPublisher.publishPostUpdated(savedPost, existingPost)
                
                // Track analytics
                analyticsService.trackPostUpdate(savedPost)
            }

            logger.info("Successfully updated Sonet post: ${request.postId}")
            savedPost

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update Sonet post: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to update post: ${e.message}"))
        }
    }

    override suspend fun deletePost(request: DeletePostRequest): DeletePostResponse = coroutineScope {
        logger.info("Deleting Sonet post: ${request.postId}")
        
        try {
            val existingPost = postRepository.getPostById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Post not found"))

            // Validate user ownership or admin privileges
            if (existingPost.userId != request.userId) {
                val user = userRepository.getUserById(request.userId)
                if (user?.role != "ADMIN" && user?.role != "MODERATOR") {
                    throw StatusException(Status.PERMISSION_DENIED.withDescription("User not authorized to delete this post"))
                }
            }

            // Soft delete the post
            val deletedPost = existingPost.toBuilder()
                .setIsDeleted(true)
                .setUpdatedAt(Instant.now().epochSecond)
                .build()

            postRepository.updatePost(deletedPost)

            // Async cleanup operations
            async {
                // Remove from feeds
                feedDistributionEngine.removePostFromFeeds(deletedPost)
                
                // Clean up media files
                mediaProcessingService.cleanupMedia(deletedPost.mediaList)
                
                // Publish deletion event
                eventPublisher.publishPostDeleted(deletedPost)
                
                // Track analytics
                analyticsService.trackPostDeletion(deletedPost)
            }

            logger.info("Successfully deleted Sonet post: ${request.postId}")
            
            DeletePostResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Post deleted successfully")
                .build()

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete Sonet post: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to delete post: ${e.message}"))
        }
    }

    override suspend fun getUserPosts(request: GetUserPostsRequest): GetUserPostsResponse {
        logger.debug("Fetching Sonet posts for user: ${request.userId}")
        
        try {
            val posts = postRepository.getUserPosts(
                userId = request.userId,
                platforms = request.platformsList.ifEmpty { listOf(PostType.SONET_POST) },
                limit = request.limit.takeIf { it > 0 } ?: 20,
                offset = request.offset,
                since = if (request.since > 0) Instant.ofEpochSecond(request.since) else null,
                until = if (request.until > 0) Instant.ofEpochSecond(request.until) else null
            )

            return GetUserPostsResponse.newBuilder()
                .addAllPosts(posts.items)
                .setHasMore(posts.hasMore)
                .setNextCursor(posts.nextCursor ?: "")
                .build()

        } catch (e: Exception) {
            logger.error("Failed to fetch user posts: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to fetch posts: ${e.message}"))
        }
    }

    override suspend fun crossPost(request: CrossPostRequest): CrossPostResponse = coroutineScope {
        logger.info("Cross-posting to platforms: ${request.targetPlatformsList}")
        
        try {
            val createdPosts = mutableListOf<Post>()
            val failedPlatforms = mutableListOf<String>()
            val errors = mutableMapOf<String, String>()

            // Create posts on each target platform concurrently
            val results = request.targetPlatformsList.map { platform ->
                async {
                    try {
                        val platformRequest = adaptRequestForPlatform(request.basePost, platform, request.platformCustomizationsMap)
                        val grpcClient = getGrpcClientForPlatform(platform)
                        val post = grpcClient.createPost(platformRequest)
                        platform to Result.success(post)
                    } catch (e: Exception) {
                        platform to Result.failure(e)
                    }
                }
            }.awaitAll()

            // Process results
            results.forEach { (platform, result) ->
                result.fold(
                    onSuccess = { post -> createdPosts.add(post) },
                    onFailure = { error ->
                        failedPlatforms.add(platform.name)
                        errors[platform.name] = error.message ?: "Unknown error"
                    }
                )
            }

            // Track cross-posting analytics
            async {
                analyticsService.trackCrossPosting(
                    originalPlatform = PostType.SONET_POST,
                    targetPlatforms = request.targetPlatformsList,
                    successCount = createdPosts.size,
                    failureCount = failedPlatforms.size
                )
            }

            CrossPostResponse.newBuilder()
                .addAllCreatedPosts(createdPosts)
                .addAllFailedPlatforms(failedPlatforms)
                .putAllErrors(errors)
                .build()

        } catch (e: Exception) {
            logger.error("Failed to cross-post", e)
            throw StatusException(Status.INTERNAL.withDescription("Cross-posting failed: ${e.message}"))
        }
    }

    override suspend fun getPostAnalytics(request: GetPostAnalyticsRequest): PostAnalytics {
        logger.debug("Fetching analytics for post: ${request.postId}")
        
        try {
            return analyticsService.getPostAnalytics(
                postId = request.postId,
                platform = request.platform,
                fromDate = if (request.fromDate > 0) Instant.ofEpochSecond(request.fromDate) else null,
                toDate = if (request.toDate > 0) Instant.ofEpochSecond(request.toDate) else null
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch post analytics: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to fetch analytics: ${e.message}"))
        }
    }

    // Private helper methods

    private fun extractSonetSpecificData(request: CreatePostRequest): Map<String, String> {
        return buildMap {
            // Sonet-specific features
            put("feeling", request.platformSpecificDataMap["feeling"] ?: "")
            put("activity", request.platformSpecificDataMap["activity"] ?: "")
            put("background_color", request.platformSpecificDataMap["background_color"] ?: "")
            put("font_style", request.platformSpecificDataMap["font_style"] ?: "")
            put("is_milestone", request.platformSpecificDataMap["is_milestone"] ?: "false")
            put("milestone_type", request.platformSpecificDataMap["milestone_type"] ?: "")
            put("audience_restriction", request.platformSpecificDataMap["audience_restriction"] ?: "")
            put("post_template", request.platformSpecificDataMap["post_template"] ?: "")
        }
    }

    private suspend fun handleCrossPosting(post: Post, targetPlatforms: List<PostType>) = coroutineScope {
        targetPlatforms.map { platform ->
            async {
                try {
                    val crossPostRequest = createCrossPostRequest(post, platform)
                    val grpcClient = getGrpcClientForPlatform(platform)
                    grpcClient.createPost(crossPostRequest)
                    logger.info("Successfully cross-posted to $platform")
                } catch (e: Exception) {
                    logger.error("Failed to cross-post to $platform", e)
                }
            }
        }.awaitAll()
    }

    private fun createCrossPostRequest(post: Post, targetPlatform: PostType): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(post.userId)
            .setPostType(targetPlatform)
            .setContentType(post.contentType)
            .setContent(adaptContentForPlatform(post.content, targetPlatform))
            .addAllMedia(post.mediaList)
            .setPrivacy(post.privacy)
            .setLocation(post.location)
            .addAllHashtags(post.hashtagsList)
            .addAllMentions(post.mentionsList)
            .build()
    }

    private fun adaptContentForPlatform(content: String, platform: PostType): String {
        return when (platform) {
            PostType.PIKA_YEET -> {
                // Truncate for Pika's character limit
                if (content.length > 280) content.take(277) + "..." else content
            }
            PostType.GALA_POST -> {
                // Add Instagram-style hashtags at the end
                content
            }
            PostType.PLAYPODS_VIDEO -> {
                // Convert to video description format
                "Posted from Sonet: $content"
            }
            else -> content
        }
    }

    private fun adaptRequestForPlatform(
        baseRequest: CreatePostRequest, 
        platform: PostType, 
        customizations: Map<String, String>
    ): CreatePostRequest {
        return baseRequest.toBuilder()
            .setPostType(platform)
            .setContent(adaptContentForPlatform(baseRequest.content, platform))
            .putAllPlatformSpecificData(customizations)
            .build()
    }

    private fun getGrpcClientForPlatform(platform: PostType): PostingServiceGrpcKt.PostingServiceCoroutineStub {
        // Return appropriate gRPC client based on platform
        return when (platform) {
            PostType.GALA_POST, PostType.GALA_STORY -> galaPostingClient
            PostType.PIKA_YEET, PostType.PIKA_REPLY -> pikaPostingClient
            PostType.PLAYPODS_VIDEO, PostType.PLAYPODS_SHORT -> playpodsPostingClient
            else -> throw IllegalArgumentException("Unsupported platform: $platform")
        }
    }

    // gRPC clients - injected via DI
    @Inject
    lateinit var galaPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    
    @Inject
    lateinit var pikaPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    
    @Inject
    lateinit var playpodsPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
}