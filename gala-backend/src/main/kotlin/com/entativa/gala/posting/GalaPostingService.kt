package com.entativa.gala.posting

import com.entativa.posting.proto.*
import com.entativa.gala.core.auth.AuthenticationService
import com.entativa.gala.core.validation.PostValidator
import com.entativa.gala.data.repositories.PostRepository
import com.entativa.gala.data.repositories.UserRepository
import com.entativa.gala.data.repositories.StoryRepository
import com.entativa.gala.messaging.EventPublisher
import com.entativa.gala.media.ImageProcessingService
import com.entativa.gala.media.VideoProcessingService
import com.entativa.gala.media.FilterEngine
import com.entativa.gala.analytics.AnalyticsService
import com.entativa.gala.algorithms.FeedDistributionEngine
import com.entativa.gala.content.ContentModerationService
import com.entativa.gala.notifications.NotificationService
import com.entativa.gala.algorithms.HashtagEngine
import com.entativa.gala.ai.CaptionGeneratorService
import com.entativa.gala.discovery.ExploreEngine

import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gala Posting Service - Instagram-like visual content platform
 * Handles posts, stories, reels with advanced image/video processing
 */
@Singleton
class GalaPostingService @Inject constructor(
    private val postRepository: PostRepository,
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
    private val authService: AuthenticationService,
    private val postValidator: PostValidator,
    private val imageProcessingService: ImageProcessingService,
    private val videoProcessingService: VideoProcessingService,
    private val filterEngine: FilterEngine,
    private val contentModerationService: ContentModerationService,
    private val feedDistributionEngine: FeedDistributionEngine,
    private val analyticsService: AnalyticsService,
    private val eventPublisher: EventPublisher,
    private val notificationService: NotificationService,
    private val hashtagEngine: HashtagEngine,
    private val captionGeneratorService: CaptionGeneratorService,
    private val exploreEngine: ExploreEngine
) : PostingServiceGrpcKt.PostingServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(GalaPostingService::class.java)

    override suspend fun createPost(request: CreatePostRequest): Post = coroutineScope {
        logger.info("Creating Gala ${request.postType} for user: ${request.userId}")
        
        try {
            // Validate authentication
            val user = authService.validateUserSession(request.userId)
                ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid user session"))

            // Validate post structure
            val validationResult = postValidator.validateGalaPost(request)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Post validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Gala requires media content
            if (request.mediaList.isEmpty()) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Gala posts require at least one image or video")
                )
            }

            // Process media with Gala's advanced pipeline
            val processedMedia = request.mediaList.mapIndexed { index, media ->
                async {
                    processGalaMedia(media, request, index)
                }
            }.awaitAll()

            // Content moderation with visual AI
            val moderationResult = contentModerationService.moderateVisualContent(
                text = request.content,
                media = processedMedia,
                userId = request.userId,
                isStory = request.postType == PostType.GALA_STORY
            )

            if (moderationResult.isBlocked) {
                logger.warn("Gala post blocked by content moderation for user: ${request.userId}")
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Content violates community guidelines: ${moderationResult.reason}")
                )
            }

            // Auto-generate hashtags if enabled
            val enhancedHashtags = if (request.platformSpecificDataMap["auto_hashtags"] == "true") {
                val generatedTags = hashtagEngine.generateHashtags(
                    content = request.content,
                    media = processedMedia,
                    userInterests = user.interests
                )
                (request.hashtagsList + generatedTags).distinct()
            } else {
                request.hashtagsList
            }

            // Auto-generate caption if requested
            val finalContent = if (request.content.isEmpty() && request.platformSpecificDataMap["auto_caption"] == "true") {
                captionGeneratorService.generateCaption(processedMedia.first(), user.language ?: "en")
            } else {
                request.content
            }

            val postId = UUID.randomUUID().toString()
            val now = Instant.now()
            
            val post = if (request.postType == PostType.GALA_STORY) {
                createGalaStory(request, postId, finalContent, processedMedia, enhancedHashtags, now)
            } else {
                createGalaPost(request, postId, finalContent, processedMedia, enhancedHashtags, now)
            }

            // Save to appropriate repository
            val savedPost = if (request.postType == PostType.GALA_STORY) {
                storyRepository.createStory(post)
            } else {
                postRepository.createPost(post)
            }

            // Async post-creation operations
            async {
                // Distribute to feeds (stories have different distribution)
                if (request.postType == PostType.GALA_STORY) {
                    feedDistributionEngine.distributeStory(savedPost, user)
                } else {
                    feedDistributionEngine.distributePost(savedPost, user)
                    
                    // Add to explore if public and high quality
                    if (savedPost.privacy == PrivacyLevel.PUBLIC && 
                        moderationResult.qualityScore > 0.8) {
                        exploreEngine.addToExplore(savedPost)
                    }
                }
                
                // Send notifications
                if (request.mentionsList.isNotEmpty()) {
                    notificationService.sendMentionNotifications(savedPost, request.mentionsList)
                }
                
                // Publish real-time events
                eventPublisher.publishGalaPostCreated(savedPost)
                
                // Track analytics
                analyticsService.trackGalaPostCreation(savedPost, user, processedMedia)
            }

            // Handle cross-posting
            if (request.crossPostEnabled && request.crossPostPlatformsList.isNotEmpty()) {
                async {
                    handleGalaCrossPosting(savedPost, request.crossPostPlatformsList)
                }
            }

            logger.info("Successfully created Gala ${request.postType}: $postId")
            savedPost

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to create Gala post for user: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to create post: ${e.message}"))
        }
    }

    private suspend fun processGalaMedia(
        media: MediaMetadata, 
        request: CreatePostRequest, 
        index: Int
    ): ProcessedMediaMetadata = coroutineScope {
        when (request.contentType) {
            ContentType.IMAGE -> {
                // Apply filters if specified
                val filterName = request.platformSpecificDataMap["filter_${index}"] ?: "none"
                val filteredImage = if (filterName != "none") {
                    filterEngine.applyFilter(media.url, filterName)
                } else {
                    media.url
                }

                // Generate multiple sizes for Gala's responsive display
                val processedSizes = imageProcessingService.generateMultipleSizes(
                    imageUrl = filteredImage,
                    sizes = listOf(
                        ImageSize(150, 150, "thumbnail"),
                        ImageSize(320, 320, "small"),
                        ImageSize(640, 640, "medium"),
                        ImageSize(1080, 1080, "large"),
                        ImageSize(1350, 1080, "wide")
                    )
                )

                // Auto-enhance image quality
                val enhancedUrls = if (request.platformSpecificDataMap["auto_enhance"] == "true") {
                    imageProcessingService.enhanceImageQuality(processedSizes)
                } else {
                    processedSizes
                }

                ProcessedMediaMetadata(
                    originalUrl = media.url,
                    processedUrl = enhancedUrls["large"] ?: filteredImage,
                    thumbnailUrl = enhancedUrls["thumbnail"] ?: filteredImage,
                    sizes = enhancedUrls,
                    altText = media.altText,
                    filterApplied = filterName,
                    processingMetadata = mapOf(
                        "color_palette" to imageProcessingService.extractColorPalette(filteredImage),
                        "dominant_color" to imageProcessingService.getDominantColor(filteredImage),
                        "brightness" to imageProcessingService.analyzeBrightness(filteredImage).toString(),
                        "contrast" to imageProcessingService.analyzeContrast(filteredImage).toString()
                    )
                )
            }
            
            ContentType.VIDEO -> {
                // Process video for Gala reels
                val videoProcessingOptions = VideoProcessingOptions(
                    maxDuration = if (request.postType == PostType.GALA_STORY) 15 else 60,
                    aspectRatio = request.platformSpecificDataMap["aspect_ratio"] ?: "1:1",
                    quality = request.platformSpecificDataMap["video_quality"] ?: "1080p",
                    autoStabilize = request.platformSpecificDataMap["stabilize"] == "true",
                    autoEnhance = request.platformSpecificDataMap["auto_enhance"] == "true"
                )

                val processedVideo = videoProcessingService.processGalaVideo(
                    videoUrl = media.url,
                    options = videoProcessingOptions
                )

                // Generate thumbnails at different timestamps
                val thumbnails = videoProcessingService.generateThumbnails(
                    videoUrl = processedVideo.url,
                    timestamps = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
                )

                ProcessedMediaMetadata(
                    originalUrl = media.url,
                    processedUrl = processedVideo.url,
                    thumbnailUrl = thumbnails.first(),
                    sizes = mapOf(
                        "240p" to processedVideo.lowQualityUrl,
                        "480p" to processedVideo.mediumQualityUrl,
                        "720p" to processedVideo.highQualityUrl,
                        "1080p" to processedVideo.url
                    ),
                    altText = media.altText,
                    duration = processedVideo.durationSeconds,
                    thumbnails = thumbnails,
                    processingMetadata = mapOf(
                        "fps" to processedVideo.fps.toString(),
                        "bitrate" to processedVideo.bitrate.toString(),
                        "codec" to processedVideo.codec,
                        "has_audio" to processedVideo.hasAudio.toString()
                    )
                )
            }
            
            else -> throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Unsupported content type for Gala: ${request.contentType}")
            )
        }
    }

    private fun createGalaPost(
        request: CreatePostRequest,
        postId: String,
        content: String,
        processedMedia: List<ProcessedMediaMetadata>,
        hashtags: List<String>,
        now: Instant
    ): Post {
        return Post.newBuilder()
            .setId(postId)
            .setUserId(request.userId)
            .setPostType(PostType.GALA_POST)
            .setContentType(request.contentType)
            .setContent(content)
            .addAllMedia(processedMedia.map { it.toProto() })
            .setPrivacy(request.privacy)
            .setLocation(request.location)
            .addAllHashtags(hashtags)
            .addAllMentions(request.mentionsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(extractGalaSpecificData(request, processedMedia))
            .build()
    }

    private fun createGalaStory(
        request: CreatePostRequest,
        postId: String,
        content: String,
        processedMedia: List<ProcessedMediaMetadata>,
        hashtags: List<String>,
        now: Instant
    ): Post {
        // Stories expire after 24 hours
        val expiresAt = now.plus(24, ChronoUnit.HOURS)
        
        return Post.newBuilder()
            .setId(postId)
            .setUserId(request.userId)
            .setPostType(PostType.GALA_STORY)
            .setContentType(request.contentType)
            .setContent(content)
            .addAllMedia(processedMedia.map { it.toProto() })
            .setPrivacy(request.privacy)
            .setLocation(request.location)
            .addAllHashtags(hashtags)
            .addAllMentions(request.mentionsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(buildMap {
                putAll(extractGalaSpecificData(request, processedMedia))
                put("expires_at", expiresAt.epochSecond.toString())
                put("is_story", "true")
                put("story_background", request.platformSpecificDataMap["story_background"] ?: "")
                put("story_music", request.platformSpecificDataMap["story_music"] ?: "")
                put("story_stickers", request.platformSpecificDataMap["story_stickers"] ?: "")
            })
            .build()
    }

    override suspend fun updatePost(request: UpdatePostRequest): Post = coroutineScope {
        logger.info("Updating Gala post: ${request.postId}")
        
        try {
            val repository = if (request.platform == PostType.GALA_STORY) storyRepository else postRepository
            val existingPost = repository.getPostById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Post not found"))

            // Stories cannot be edited after creation
            if (existingPost.postType == PostType.GALA_STORY) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Stories cannot be edited after creation")
                )
            }

            // Validate user ownership
            if (existingPost.userId != request.userId) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("User not authorized to update this post")
                )
            }

            // Validate update (Gala allows limited updates)
            val validationResult = postValidator.validateGalaPostUpdate(request, existingPost)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Update validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Re-moderate updated content
            val moderationResult = contentModerationService.moderateVisualContent(
                text = request.content,
                media = existingPost.mediaList,
                userId = request.userId,
                isUpdate = true
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

            val savedPost = postRepository.updatePost(updatedPost)

            // Async operations
            async {
                // Re-distribute if privacy changed
                if (existingPost.privacy != request.privacy) {
                    feedDistributionEngine.redistributePost(savedPost)
                }
                
                // Update explore feed if needed
                if (savedPost.privacy == PrivacyLevel.PUBLIC) {
                    exploreEngine.updateInExplore(savedPost)
                } else {
                    exploreEngine.removeFromExplore(savedPost.id)
                }
                
                eventPublisher.publishGalaPostUpdated(savedPost, existingPost)
                analyticsService.trackGalaPostUpdate(savedPost)
            }

            logger.info("Successfully updated Gala post: ${request.postId}")
            savedPost

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update Gala post: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to update post: ${e.message}"))
        }
    }

    override suspend fun deletePost(request: DeletePostRequest): DeletePostResponse = coroutineScope {
        logger.info("Deleting Gala post: ${request.postId}")
        
        try {
            val repository = if (request.platform == PostType.GALA_STORY) storyRepository else postRepository
            val existingPost = repository.getPostById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Post not found"))

            // Validate user ownership
            if (existingPost.userId != request.userId) {
                val user = userRepository.getUserById(request.userId)
                if (user?.role != "ADMIN" && user?.role != "MODERATOR") {
                    throw StatusException(
                        Status.PERMISSION_DENIED.withDescription("User not authorized to delete this post")
                    )
                }
            }

            // Soft delete
            val deletedPost = existingPost.toBuilder()
                .setIsDeleted(true)
                .setUpdatedAt(Instant.now().epochSecond)
                .build()

            repository.updatePost(deletedPost)

            // Async cleanup
            async {
                // Remove from feeds and explore
                feedDistributionEngine.removePostFromFeeds(deletedPost)
                exploreEngine.removeFromExplore(deletedPost.id)
                
                // Clean up processed media files
                imageProcessingService.cleanupProcessedImages(deletedPost.mediaList)
                videoProcessingService.cleanupProcessedVideos(deletedPost.mediaList)
                
                eventPublisher.publishGalaPostDeleted(deletedPost)
                analyticsService.trackGalaPostDeletion(deletedPost)
            }

            logger.info("Successfully deleted Gala post: ${request.postId}")
            
            DeletePostResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Post deleted successfully")
                .build()

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete Gala post: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to delete post: ${e.message}"))
        }
    }

    override suspend fun getUserPosts(request: GetUserPostsRequest): GetUserPostsResponse {
        logger.debug("Fetching Gala posts for user: ${request.userId}")
        
        try {
            // Separate stories from regular posts
            val platforms = request.platformsList.ifEmpty { 
                listOf(PostType.GALA_POST, PostType.GALA_STORY) 
            }
            
            val posts = mutableListOf<Post>()
            
            if (platforms.contains(PostType.GALA_POST)) {
                val galaPosts = postRepository.getUserPosts(
                    userId = request.userId,
                    platforms = listOf(PostType.GALA_POST),
                    limit = request.limit,
                    offset = request.offset,
                    since = if (request.since > 0) Instant.ofEpochSecond(request.since) else null,
                    until = if (request.until > 0) Instant.ofEpochSecond(request.until) else null
                )
                posts.addAll(galaPosts.items)
            }
            
            if (platforms.contains(PostType.GALA_STORY)) {
                val stories = storyRepository.getUserStories(
                    userId = request.userId,
                    limit = request.limit,
                    offset = request.offset,
                    includeExpired = false // Only active stories
                )
                posts.addAll(stories.items)
            }

            // Sort by creation time
            val sortedPosts = posts.sortedByDescending { it.createdAt }

            return GetUserPostsResponse.newBuilder()
                .addAllPosts(sortedPosts)
                .setHasMore(sortedPosts.size >= request.limit)
                .setNextCursor(sortedPosts.lastOrNull()?.id ?: "")
                .build()

        } catch (e: Exception) {
            logger.error("Failed to fetch Gala user posts: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to fetch posts: ${e.message}"))
        }
    }

    // Helper methods

    private fun extractGalaSpecificData(
        request: CreatePostRequest, 
        processedMedia: List<ProcessedMediaMetadata>
    ): Map<String, String> {
        return buildMap {
            // Gala-specific features
            put("filter_used", processedMedia.firstOrNull()?.filterApplied ?: "none")
            put("auto_enhanced", request.platformSpecificDataMap["auto_enhance"] ?: "false")
            put("aspect_ratio", request.platformSpecificDataMap["aspect_ratio"] ?: "1:1")
            put("color_palette", processedMedia.firstOrNull()?.processingMetadata?.get("color_palette") ?: "")
            put("dominant_color", processedMedia.firstOrNull()?.processingMetadata?.get("dominant_color") ?: "")
            put("is_carousel", (processedMedia.size > 1).toString())
            put("media_count", processedMedia.size.toString())
            put("has_location", request.hasLocation().toString())
            put("music_id", request.platformSpecificDataMap["music_id"] ?: "")
            put("collaboration_users", request.platformSpecificDataMap["collaboration_users"] ?: "")
            put("branded_content", request.platformSpecificDataMap["branded_content"] ?: "false")
            put("product_tags", request.platformSpecificDataMap["product_tags"] ?: "")
        }
    }

    private suspend fun handleGalaCrossPosting(post: Post, targetPlatforms: List<PostType>) {
        // Gala cross-posting logic - adapt visual content for other platforms
        targetPlatforms.forEach { platform ->
            try {
                when (platform) {
                    PostType.SONET_POST -> {
                        // Convert to Sonet format - keep first image, adapt caption
                        val crossPostRequest = createSonetCrossPost(post)
                        sonetPostingClient.createPost(crossPostRequest)
                    }
                    PostType.PIKA_YEET -> {
                        // Convert to Pika yeet - text only with link to Gala post
                        val crossPostRequest = createPikaCrossPost(post)
                        pikaPostingClient.createPost(crossPostRequest)
                    }
                    else -> {
                        logger.warn("Cross-posting from Gala to $platform not supported yet")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to cross-post Gala content to $platform", e)
            }
        }
    }

    private fun createSonetCrossPost(galaPost: Post): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(galaPost.userId)
            .setPostType(PostType.SONET_POST)
            .setContentType(galaPost.contentType)
            .setContent("${galaPost.content}\n\n📸 Posted from Gala")
            .addMedia(galaPost.mediaList.first()) // Use first image only
            .setPrivacy(galaPost.privacy)
            .setLocation(galaPost.location)
            .addAllHashtags(galaPost.hashtagsList)
            .build()
    }

    private fun createPikaCrossPost(galaPost: Post): CreatePostRequest {
        val shortContent = if (galaPost.content.length > 200) {
            galaPost.content.take(197) + "..."
        } else {
            galaPost.content
        }
        
        return CreatePostRequest.newBuilder()
            .setUserId(galaPost.userId)
            .setPostType(PostType.PIKA_YEET)
            .setContentType(ContentType.TEXT)
            .setContent("$shortContent\n\n🖼️ See full post on Gala: gala.app/p/${galaPost.id}")
            .setPrivacy(galaPost.privacy)
            .build()
    }

    // Injected gRPC clients
    @Inject
    lateinit var sonetPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    
    @Inject
    lateinit var pikaPostingClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
}

// Data classes for Gala-specific processing
data class ProcessedMediaMetadata(
    val originalUrl: String,
    val processedUrl: String,
    val thumbnailUrl: String,
    val sizes: Map<String, String>,
    val altText: String,
    val filterApplied: String? = null,
    val duration: Int? = null,
    val thumbnails: List<String> = emptyList(),
    val processingMetadata: Map<String, String> = emptyMap()
) {
    fun toProto(): MediaMetadata {
        return MediaMetadata.newBuilder()
            .setUrl(processedUrl)
            .setThumbnailUrl(thumbnailUrl)
            .setAltText(altText)
            .addAllTags(processingMetadata.keys.toList())
            .apply {
                duration?.let { setDurationSeconds(it) }
            }
            .build()
    }
}

data class ImageSize(val width: Int, val height: Int, val name: String)

data class VideoProcessingOptions(
    val maxDuration: Int,
    val aspectRatio: String,
    val quality: String,
    val autoStabilize: Boolean,
    val autoEnhance: Boolean
)