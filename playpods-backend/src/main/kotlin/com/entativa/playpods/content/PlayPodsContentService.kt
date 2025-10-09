package com.entativa.playpods.content

import com.entativa.posting.proto.*
import com.entativa.playpods.core.auth.AuthenticationService
import com.entativa.playpods.core.validation.ContentValidator
import com.entativa.playpods.data.repositories.ContentRepository
import com.entativa.playpods.data.repositories.ChannelRepository
import com.entativa.playpods.data.repositories.UserRepository
import com.entativa.playpods.messaging.EventPublisher
import com.entativa.playpods.media.VideoProcessingService
import com.entativa.playpods.media.AudioProcessingService
import com.entativa.playpods.media.ThumbnailGenerationService
import com.entativa.playpods.analytics.AnalyticsService
import com.entativa.playpods.algorithms.RecommendationEngine
import com.entativa.playpods.content.ContentModerationService
import com.entativa.playpods.notifications.NotificationService
import com.entativa.playpods.ai.MetadataExtractor
import com.entativa.playpods.ai.ContentAnalyzer
import com.entativa.playpods.transcription.TranscriptionService
import com.entativa.playpods.monetization.MonetizationEngine
import com.entativa.playpods.cdn.CDNService
import com.entativa.playpods.queue.ProcessingQueue

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
 * PlayPods Content Upload Service - YouTube-like video platform
 * Handles video uploads, processing, live streaming with advanced media pipeline
 */
@Singleton
class PlayPodsContentService @Inject constructor(
    private val contentRepository: ContentRepository,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val authService: AuthenticationService,
    private val contentValidator: ContentValidator,
    private val videoProcessingService: VideoProcessingService,
    private val audioProcessingService: AudioProcessingService,
    private val thumbnailGenerationService: ThumbnailGenerationService,
    private val contentModerationService: ContentModerationService,
    private val recommendationEngine: RecommendationEngine,
    private val analyticsService: AnalyticsService,
    private val eventPublisher: EventPublisher,
    private val notificationService: NotificationService,
    private val metadataExtractor: MetadataExtractor,
    private val contentAnalyzer: ContentAnalyzer,
    private val transcriptionService: TranscriptionService,
    private val monetizationEngine: MonetizationEngine,
    private val cdnService: CDNService,
    private val processingQueue: ProcessingQueue
) : PostingServiceGrpcKt.PostingServiceCoroutineImplBase() {

    private val logger = LoggerFactory.getLogger(PlayPodsContentService::class.java)
    
    companion object {
        const val MAX_VIDEO_SIZE = 128L * 1024 * 1024 * 1024 // 128GB for premium
        const val MAX_STANDARD_VIDEO_SIZE = 2L * 1024 * 1024 * 1024 // 2GB for standard
        const val MAX_TITLE_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 5000
        const val SUPPORTED_VIDEO_FORMATS = setOf("mp4", "avi", "mov", "wmv", "flv", "webm", "mkv")
        const val SUPPORTED_AUDIO_FORMATS = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a")
    }

    override suspend fun createPost(request: CreatePostRequest): Post = coroutineScope {
        logger.info("Creating PlayPods ${request.postType} for user: ${request.userId}")
        
        try {
            // Validate authentication and channel access
            val user = authService.validateUserSession(request.userId)
                ?: throw StatusException(Status.UNAUTHENTICATED.withDescription("Invalid user session"))

            val channel = channelRepository.getUserChannel(request.userId)
                ?: throw StatusException(Status.PRECONDITION_FAILED.withDescription("User must have a PlayPods channel"))

            // Validate content structure
            val validationResult = contentValidator.validateContent(request, channel)
            if (!validationResult.isValid) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Content validation failed: ${validationResult.errors.joinToString()}")
                )
            }

            // Validate metadata
            if (request.title.length > MAX_TITLE_LENGTH) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Title exceeds maximum length of $MAX_TITLE_LENGTH characters")
                )
            }

            if (request.content.length > MAX_DESCRIPTION_LENGTH) {
                throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Description exceeds maximum length of $MAX_DESCRIPTION_LENGTH characters")
                )
            }

            val contentId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create content based on type
            val content = when (request.postType) {
                PostType.PLAYPODS_VIDEO -> createVideoContent(request, contentId, channel, now)
                PostType.PLAYPODS_PODCAST -> createPodcastContent(request, contentId, channel, now)
                PostType.PLAYPODS_LIVE -> createLiveStreamContent(request, contentId, channel, now)
                PostType.PLAYPODS_SHORT -> createShortContent(request, contentId, channel, now)
                else -> throw StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Unsupported content type for PlayPods: ${request.postType}")
                )
            }

            // Save initial content record
            val savedContent = contentRepository.createContent(content)

            // Queue async processing operations
            val processingJobId = processingQueue.enqueueContentProcessing(
                contentId = contentId,
                contentType = request.postType,
                mediaUrls = request.mediaList.map { it.url },
                priority = if (request.postType == PostType.PLAYPODS_LIVE) ProcessingPriority.HIGH else ProcessingPriority.NORMAL
            )

            // Start async processing pipeline
            async {
                processContentAsync(savedContent, request, channel, processingJobId)
            }

            // Handle notifications for subscribers
            async {
                if (request.postType != PostType.PLAYPODS_LIVE) {
                    notificationService.notifyChannelSubscribers(channel, savedContent)
                }
            }

            // Track initial analytics
            async {
                analyticsService.trackPlayPodsContentCreation(savedContent, channel, user)
            }

            logger.info("Successfully initiated PlayPods ${request.postType} creation: $contentId")
            savedContent

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to create PlayPods content for user: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to create content: ${e.message}"))
        }
    }

    private suspend fun createVideoContent(
        request: CreatePostRequest,
        contentId: String,
        channel: Channel,
        now: Instant
    ): Post = coroutineScope {
        val primaryMedia = request.mediaList.firstOrNull()
            ?: throw StatusException(Status.INVALID_ARGUMENT.withDescription("Video content requires media"))

        // Validate video file
        val fileExtension = primaryMedia.url.substringAfterLast(".", "").lowercase()
        if (!SUPPORTED_VIDEO_FORMATS.contains(fileExtension)) {
            throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Unsupported video format: $fileExtension")
            )
        }

        // Check file size limits based on user tier
        val maxSize = if (channel.isPremium) MAX_VIDEO_SIZE else MAX_STANDARD_VIDEO_SIZE
        if (primaryMedia.sizeBytes > maxSize) {
            throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Video file exceeds size limit")
            )
        }

        Post.newBuilder()
            .setId(contentId)
            .setUserId(request.userId)
            .setPostType(PostType.PLAYPODS_VIDEO)
            .setContentType(request.contentType)
            .setTitle(request.title)
            .setContent(request.content) // description
            .addAllMedia(request.mediaList)
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
            .putAllPlatformSpecificData(createPlayPodsVideoData(request, channel))
            .build()
    }

    private suspend fun createPodcastContent(
        request: CreatePostRequest,
        contentId: String,
        channel: Channel,
        now: Instant
    ): Post = coroutineScope {
        val primaryMedia = request.mediaList.firstOrNull()
            ?: throw StatusException(Status.INVALID_ARGUMENT.withDescription("Podcast content requires audio media"))

        // Validate audio file
        val fileExtension = primaryMedia.url.substringAfterLast(".", "").lowercase()
        if (!SUPPORTED_AUDIO_FORMATS.contains(fileExtension)) {
            throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Unsupported audio format: $fileExtension")
            )
        }

        Post.newBuilder()
            .setId(contentId)
            .setUserId(request.userId)
            .setPostType(PostType.PLAYPODS_PODCAST)
            .setContentType(request.contentType)
            .setTitle(request.title)
            .setContent(request.content)
            .addAllMedia(request.mediaList)
            .setPrivacy(request.privacy)
            .addAllHashtags(request.hashtagsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(createPlayPodsPodcastData(request, channel))
            .build()
    }

    private suspend fun createLiveStreamContent(
        request: CreatePostRequest,
        contentId: String,
        channel: Channel,
        now: Instant
    ): Post {
        // Validate channel can go live
        if (!channel.canGoLive) {
            throw StatusException(
                Status.PERMISSION_DENIED.withDescription("Channel not authorized for live streaming")
            )
        }

        return Post.newBuilder()
            .setId(contentId)
            .setUserId(request.userId)
            .setPostType(PostType.PLAYPODS_LIVE)
            .setContentType(request.contentType)
            .setTitle(request.title)
            .setContent(request.content)
            .setPrivacy(request.privacy)
            .addAllHashtags(request.hashtagsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(createPlayPodsLiveData(request, channel))
            .build()
    }

    private suspend fun createShortContent(
        request: CreatePostRequest,
        contentId: String,
        channel: Channel,
        now: Instant
    ): Post {
        val primaryMedia = request.mediaList.firstOrNull()
            ?: throw StatusException(Status.INVALID_ARGUMENT.withDescription("Short content requires video"))

        // Validate short format constraints
        if (primaryMedia.sizeBytes > 100 * 1024 * 1024) { // 100MB for shorts
            throw StatusException(
                Status.INVALID_ARGUMENT.withDescription("Short video exceeds size limit")
            )
        }

        return Post.newBuilder()
            .setId(contentId)
            .setUserId(request.userId)
            .setPostType(PostType.PLAYPODS_SHORT)
            .setContentType(request.contentType)
            .setTitle(request.title)
            .setContent(request.content)
            .addAllMedia(request.mediaList)
            .setPrivacy(request.privacy)
            .addAllHashtags(request.hashtagsList)
            .setCreatedAt(now.epochSecond)
            .setUpdatedAt(now.epochSecond)
            .setLikeCount(0)
            .setCommentCount(0)
            .setShareCount(0)
            .setViewCount(0)
            .setIsDeleted(false)
            .setIsEdited(false)
            .putAllPlatformSpecificData(createPlayPodsShortData(request, channel))
            .build()
    }

    private suspend fun processContentAsync(
        content: Post,
        request: CreatePostRequest,
        channel: Channel,
        processingJobId: String
    ) = coroutineScope {
        try {
            logger.info("Starting async processing for content: ${content.id}")

            // Extract metadata from media
            val mediaMetadata = async {
                metadataExtractor.extractPlayPodsMetadata(content.mediaList, content.postType)
            }

            // Content moderation
            val moderationResult = async {
                contentModerationService.moderatePlayPodsContent(
                    title = content.title,
                    description = content.content,
                    media = content.mediaList,
                    contentType = content.postType,
                    channelId = channel.id
                )
            }

            // Content analysis for recommendations
            val contentAnalysis = async {
                contentAnalyzer.analyzePlayPodsContent(
                    title = content.title,
                    description = content.content,
                    tags = content.hashtagsList,
                    media = content.mediaList,
                    channelHistory = channel.contentHistory
                )
            }

            // Process media based on type
            val processedMedia = when (content.postType) {
                PostType.PLAYPODS_VIDEO -> processVideo(content)
                PostType.PLAYPODS_PODCAST -> processPodcast(content)
                PostType.PLAYPODS_SHORT -> processShort(content)
                PostType.PLAYPODS_LIVE -> processLiveStream(content)
                else -> emptyList()
            }

            // Wait for all analysis to complete
            val metadata = mediaMetadata.await()
            val moderation = moderationResult.await()
            val analysis = contentAnalysis.await()

            // Handle moderation results
            if (moderation.isBlocked) {
                handleContentBlocked(content, moderation.reason)
                return@coroutineScope
            }

            // Generate thumbnails
            val thumbnails = async {
                if (content.postType in listOf(PostType.PLAYPODS_VIDEO, PostType.PLAYPODS_SHORT)) {
                    thumbnailGenerationService.generatePlayPodsThumbnails(
                        videoUrl = content.mediaList.first().url,
                        contentId = content.id,
                        analysisData = analysis
                    )
                } else {
                    emptyList()
                }
            }

            // Generate transcriptions
            val transcription = async {
                if (content.postType != PostType.PLAYPODS_LIVE) {
                    transcriptionService.transcribePlayPodsContent(
                        mediaUrl = content.mediaList.first().url,
                        language = analysis.detectedLanguage
                    )
                } else {
                    null
                }
            }

            // Upload to CDN
            val cdnUrls = async {
                cdnService.uploadPlayPodsMedia(
                    processedMedia = processedMedia,
                    contentId = content.id,
                    channelId = channel.id
                )
            }

            // Await processing completion
            val finalThumbnails = thumbnails.await()
            val finalTranscription = transcription.await()
            val finalCdnUrls = cdnUrls.await()

            // Update content with processed data
            val updatedContent = content.toBuilder()
                .clearMedia()
                .addAllMedia(finalCdnUrls.map { it.toProto() })
                .putAllPlatformSpecificData(buildMap {
                    putAll(content.platformSpecificDataMap)
                    put("processing_status", "completed")
                    put("processing_job_id", processingJobId)
                    put("duration_seconds", metadata.durationSeconds.toString())
                    put("resolution", metadata.resolution)
                    put("bitrate", metadata.bitrate.toString())
                    put("codec", metadata.codec)
                    put("file_size", metadata.fileSize.toString())
                    put("has_transcription", (finalTranscription != null).toString())
                    put("thumbnail_count", finalThumbnails.size.toString())
                    put("content_score", analysis.qualityScore.toString())
                    put("monetization_eligible", analysis.monetizationEligible.toString())
                    put("detected_language", analysis.detectedLanguage)
                    put("content_categories", analysis.categories.joinToString(","))
                    put("audience_rating", analysis.audienceRating)
                    finalTranscription?.let { put("transcription", it.text) }
                })
                .build()

            contentRepository.updateContent(updatedContent)

            // Post-processing operations
            async {
                // Add to recommendation engine
                recommendationEngine.indexPlayPodsContent(updatedContent, analysis)
                
                // Evaluate monetization eligibility
                if (analysis.monetizationEligible) {
                    monetizationEngine.evaluatePlayPodsContent(updatedContent, channel)
                }
                
                // Send completion notifications
                notificationService.notifyContentProcessingComplete(updatedContent, channel)
                
                // Publish events
                eventPublisher.publishPlayPodsContentProcessed(updatedContent, analysis)
                
                // Update analytics
                analyticsService.trackPlayPodsContentProcessingComplete(updatedContent, metadata, analysis)
            }

            logger.info("Successfully completed processing for content: ${content.id}")

        } catch (e: Exception) {
            logger.error("Failed to process PlayPods content: ${content.id}", e)
            handleProcessingFailure(content, processingJobId, e.message ?: "Unknown error")
        }
    }

    private suspend fun processVideo(content: Post): List<ProcessedMedia> = coroutineScope {
        val videoUrl = content.mediaList.first().url
        
        // Process multiple resolutions
        val resolutions = listOf("4K", "1080p", "720p", "480p", "360p")
        resolutions.map { resolution ->
            async {
                videoProcessingService.processPlayPodsVideo(
                    sourceUrl = videoUrl,
                    targetResolution = resolution,
                    contentId = content.id,
                    optimizeForStreaming = true
                )
            }
        }.awaitAll()
    }

    private suspend fun processPodcast(content: Post): List<ProcessedMedia> = coroutineScope {
        val audioUrl = content.mediaList.first().url
        
        listOf(
            async {
                audioProcessingService.processPlayPodsPodcast(
                    sourceUrl = audioUrl,
                    contentId = content.id,
                    enhanceAudio = true,
                    generateChapters = true
                )
            }
        ).awaitAll()
    }

    private suspend fun processShort(content: Post): List<ProcessedMedia> = coroutineScope {
        val videoUrl = content.mediaList.first().url
        
        listOf(
            async {
                videoProcessingService.processPlayPodsShort(
                    sourceUrl = videoUrl,
                    contentId = content.id,
                    optimizeForMobile = true,
                    addCaptions = true
                )
            }
        ).awaitAll()
    }

    private suspend fun processLiveStream(content: Post): List<ProcessedMedia> {
        // Live streams are processed in real-time
        return videoProcessingService.setupLiveStreamIngestion(
            streamKey = content.platformSpecificDataMap["stream_key"] ?: "",
            contentId = content.id,
            qualities = listOf("1080p", "720p", "480p")
        )
    }

    private suspend fun handleContentBlocked(content: Post, reason: String) {
        val blockedContent = content.toBuilder()
            .putPlatformSpecificData("processing_status", "blocked")
            .putPlatformSpecificData("blocked_reason", reason)
            .setPrivacy(Privacy.PRIVATE)
            .build()

        contentRepository.updateContent(blockedContent)
        
        // Notify channel owner
        notificationService.notifyContentBlocked(blockedContent, reason)
        
        // Log for review
        logger.warn("PlayPods content blocked: ${content.id}, reason: $reason")
    }

    private suspend fun handleProcessingFailure(content: Post, processingJobId: String, error: String) {
        val failedContent = content.toBuilder()
            .putPlatformSpecificData("processing_status", "failed")
            .putPlatformSpecificData("processing_job_id", processingJobId)
            .putPlatformSpecificData("error_message", error)
            .build()

        contentRepository.updateContent(failedContent)
        
        // Retry processing if it's a transient error
        processingQueue.requeueWithDelay(processingJobId, delay = 300) // 5 minutes
        
        logger.error("PlayPods content processing failed: ${content.id}, error: $error")
    }

    override suspend fun updatePost(request: UpdatePostRequest): Post {
        logger.info("Updating PlayPods content: ${request.postId}")
        
        try {
            val existingContent = contentRepository.getContentById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Content not found"))

            // Validate ownership
            if (existingContent.userId != request.userId) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("User not authorized to update this content")
                )
            }

            // Validate update constraints
            if (existingContent.postType == PostType.PLAYPODS_LIVE) {
                throw StatusException(
                    Status.PERMISSION_DENIED.withDescription("Live streams cannot be updated")
                )
            }

            // Create updated content
            val updatedContent = existingContent.toBuilder()
                .setTitle(request.title)
                .setContent(request.content)
                .setPrivacy(request.privacy)
                .clearHashtags()
                .addAllHashtags(request.hashtagsList)
                .setUpdatedAt(Instant.now().epochSecond)
                .setIsEdited(true)
                .build()

            val savedContent = contentRepository.updateContent(updatedContent)

            // Re-analyze content
            val analysis = contentAnalyzer.analyzePlayPodsContent(
                title = savedContent.title,
                description = savedContent.content,
                tags = savedContent.hashtagsList,
                media = savedContent.mediaList,
                channelHistory = emptyList()
            )

            // Update recommendation index
            recommendationEngine.updatePlayPodsContent(savedContent, analysis)

            logger.info("Successfully updated PlayPods content: ${request.postId}")
            savedContent

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to update PlayPods content: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to update content: ${e.message}"))
        }
    }

    override suspend fun deletePost(request: DeletePostRequest): DeletePostResponse {
        logger.info("Deleting PlayPods content: ${request.postId}")
        
        try {
            val existingContent = contentRepository.getContentById(request.postId)
                ?: throw StatusException(Status.NOT_FOUND.withDescription("Content not found"))

            // Validate ownership or admin privileges
            if (existingContent.userId != request.userId) {
                val user = userRepository.getUserById(request.userId)
                if (user?.role != "ADMIN" && user?.role != "MODERATOR") {
                    throw StatusException(
                        Status.PERMISSION_DENIED.withDescription("User not authorized to delete this content")
                    )
                }
            }

            // Soft delete
            val deletedContent = existingContent.toBuilder()
                .setIsDeleted(true)
                .setTitle("[Deleted Video]")
                .setContent("[This video was deleted]")
                .setUpdatedAt(Instant.now().epochSecond)
                .build()

            contentRepository.updateContent(deletedContent)

            // Cleanup operations
            cdnService.deletePlayPodsMedia(deletedContent.mediaList)
            recommendationEngine.removePlayPodsContent(deletedContent.id)
            
            logger.info("Successfully deleted PlayPods content: ${request.postId}")
            
            return DeletePostResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Content deleted successfully")
                .build()

        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to delete PlayPods content: ${request.postId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to delete content: ${e.message}"))
        }
    }

    override suspend fun getUserPosts(request: GetUserPostsRequest): GetUserPostsResponse {
        logger.debug("Fetching PlayPods content for user: ${request.userId}")
        
        try {
            val platforms = request.platformsList.ifEmpty { 
                listOf(PostType.PLAYPODS_VIDEO, PostType.PLAYPODS_PODCAST, PostType.PLAYPODS_LIVE, PostType.PLAYPODS_SHORT) 
            }
            
            val content = contentRepository.getUserContent(
                userId = request.userId,
                platforms = platforms,
                limit = request.limit.takeIf { it > 0 } ?: 50,
                offset = request.offset,
                since = if (request.since > 0) Instant.ofEpochSecond(request.since) else null,
                until = if (request.until > 0) Instant.ofEpochSecond(request.until) else null
            )

            return GetUserPostsResponse.newBuilder()
                .addAllPosts(content.items)
                .setHasMore(content.hasMore)
                .setNextCursor(content.nextCursor ?: "")
                .build()

        } catch (e: Exception) {
            logger.error("Failed to fetch PlayPods user content: ${request.userId}", e)
            throw StatusException(Status.INTERNAL.withDescription("Failed to fetch content: ${e.message}"))
        }
    }

    // Helper methods for platform-specific data

    private fun createPlayPodsVideoData(request: CreatePostRequest, channel: Channel): Map<String, String> {
        return buildMap {
            put("content_type", "video")
            put("channel_id", channel.id)
            put("channel_name", channel.name)
            put("processing_status", "queued")
            put("monetization_enabled", channel.monetizationEnabled.toString())
            put("age_restriction", request.platformSpecificDataMap["age_restriction"] ?: "none")
            put("category", request.platformSpecificDataMap["category"] ?: "Entertainment")
            put("language", request.platformSpecificDataMap["language"] ?: "en")
            put("license", request.platformSpecificDataMap["license"] ?: "standard")
            put("comments_enabled", request.platformSpecificDataMap["comments_enabled"] ?: "true")
            put("likes_visible", request.platformSpecificDataMap["likes_visible"] ?: "true")
            put("download_enabled", request.platformSpecificDataMap["download_enabled"] ?: "false")
            put("premiere_scheduled", request.platformSpecificDataMap["premiere_scheduled"] ?: "false")
        }
    }

    private fun createPlayPodsPodcastData(request: CreatePostRequest, channel: Channel): Map<String, String> {
        return buildMap {
            put("content_type", "podcast")
            put("channel_id", channel.id)
            put("processing_status", "queued")
            put("episode_number", request.platformSpecificDataMap["episode_number"] ?: "1")
            put("season_number", request.platformSpecificDataMap["season_number"] ?: "1")
            put("series_title", request.platformSpecificDataMap["series_title"] ?: "")
            put("explicit_content", request.platformSpecificDataMap["explicit_content"] ?: "false")
            put("transcript_enabled", "true")
            put("chapters_enabled", "true")
        }
    }

    private fun createPlayPodsLiveData(request: CreatePostRequest, channel: Channel): Map<String, String> {
        return buildMap {
            put("content_type", "live")
            put("channel_id", channel.id)
            put("stream_key", UUID.randomUUID().toString())
            put("stream_status", "scheduled")
            put("scheduled_start", request.platformSpecificDataMap["scheduled_start"] ?: "")
            put("max_viewers", "0")
            put("current_viewers", "0")
            put("chat_enabled", request.platformSpecificDataMap["chat_enabled"] ?: "true")
            put("dvr_enabled", request.platformSpecificDataMap["dvr_enabled"] ?: "true")
            put("auto_archive", request.platformSpecificDataMap["auto_archive"] ?: "true")
        }
    }

    private fun createPlayPodsShortData(request: CreatePostRequest, channel: Channel): Map<String, String> {
        return buildMap {
            put("content_type", "short")
            put("channel_id", channel.id)
            put("processing_status", "queued")
            put("vertical_video", "true")
            put("loop_enabled", "true")
            put("remix_enabled", request.platformSpecificDataMap["remix_enabled"] ?: "true")
            put("music_used", request.platformSpecificDataMap["music_used"] ?: "")
            put("original_audio", request.platformSpecificDataMap["original_audio"] ?: "true")
        }
    }
}

// Data classes for PlayPods-specific features
data class Channel(
    val id: String,
    val name: String,
    val userId: String,
    val isPremium: Boolean,
    val canGoLive: Boolean,
    val monetizationEnabled: Boolean,
    val contentHistory: List<String>
)

data class ProcessedMedia(
    val url: String,
    val resolution: String,
    val bitrate: Int,
    val codec: String,
    val fileSize: Long,
    val mimeType: String
) {
    fun toProto(): MediaMetadata {
        return MediaMetadata.newBuilder()
            .setUrl(url)
            .setSizeBytes(fileSize)
            .setMimeType(mimeType)
            .putMetadata("resolution", resolution)
            .putMetadata("bitrate", bitrate.toString())
            .putMetadata("codec", codec)
            .build()
    }
}

data class ContentMetadata(
    val durationSeconds: Long,
    val resolution: String,
    val bitrate: Int,
    val codec: String,
    val fileSize: Long
)

data class ContentAnalysis(
    val qualityScore: Double,
    val monetizationEligible: Boolean,
    val detectedLanguage: String,
    val categories: List<String>,
    val audienceRating: String
)

enum class ProcessingPriority {
    LOW, NORMAL, HIGH, URGENT
}