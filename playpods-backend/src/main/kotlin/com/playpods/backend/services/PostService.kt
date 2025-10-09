package com.playpods.backend.services

import com.playpods.backend.data.repositories.PostRepository
import com.playpods.backend.data.repositories.UserRepository
import com.playpods.backend.data.repositories.ChannelRepository
import com.playpods.backend.data.repositories.InteractionRepository
import com.playpods.backend.data.repositories.PlaylistRepository
import com.playpods.backend.data.models.*
import com.playpods.backend.auth.AuthenticationService
import com.playpods.backend.notifications.NotificationService
import com.playpods.backend.analytics.ContentAnalyticsService
import com.playpods.backend.algorithms.RecommendationEngine
import com.playpods.backend.algorithms.TrendingEngine
import com.playpods.backend.messaging.EventPublisher
import com.playpods.backend.validation.ContentValidator
import com.playpods.backend.media.VideoProcessingService
import com.playpods.backend.media.AudioProcessingService
import com.playpods.backend.content.ContentModerationService
import com.playpods.backend.creator.CreatorToolsService
import com.playpods.backend.monetization.MonetizationService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlayPods Post Service - YouTube-like Content Management
 * Handles video uploads, Pixels (Shorts), podcasts, and creator-focused interactions
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced creator economy features
 */
@Singleton
class PostService @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val interactionRepository: InteractionRepository,
    private val playlistRepository: PlaylistRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val contentAnalyticsService: ContentAnalyticsService,
    private val recommendationEngine: RecommendationEngine,
    private val trendingEngine: TrendingEngine,
    private val eventPublisher: EventPublisher,
    private val contentValidator: ContentValidator,
    private val videoProcessingService: VideoProcessingService,
    private val audioProcessingService: AudioProcessingService,
    private val contentModerationService: ContentModerationService,
    private val creatorToolsService: CreatorToolsService,
    private val monetizationService: MonetizationService
) {
    
    private val logger = LoggerFactory.getLogger(PostService::class.java)
    
    companion object {
        const val MAX_CAPTION_LENGTH = 500           // PlayPods caption limit
        const val MAX_PIXEL_DURATION = 60           // 60 seconds for Pixels (Shorts)
        const val MAX_REGULAR_VIDEO_DURATION = 12 * 3600  // 12 hours for regular videos
        const val MAX_PODCAST_DURATION = 10 * 3600   // 10 hours for podcasts
        const val MAX_VIDEO_SIZE = 128L * 1024 * 1024 * 1024  // 128GB for premium creators
        const val MAX_STANDARD_VIDEO_SIZE = 2L * 1024 * 1024 * 1024  // 2GB for standard users
        const val SUPPORTED_VIDEO_FORMATS = setOf("mp4", "mov", "avi", "webm", "mkv", "m4v")
        const val SUPPORTED_AUDIO_FORMATS = setOf("mp3", "wav", "aac", "flac", "ogg", "m4a")
        const val PIXELS_TRENDING_THRESHOLD = 1000   // Views needed for Pixels trending
        const val VIDEO_TRENDING_THRESHOLD = 10000   // Views needed for regular video trending
    }

    /**
     * Upload a video content with comprehensive processing
     */
    suspend fun uploadVideo(request: CreatePlayPodsVideoRequest): PlayPodsVideo = coroutineScope {
        logger.info("Uploading video for channel: ${request.channelId}")
        
        try {
            // Validate user and channel
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val channel = channelRepository.findById(request.channelId)
                ?: throw IllegalArgumentException("Channel not found: ${request.channelId}")

            if (channel.userId != request.userId) {
                throw IllegalArgumentException("User not authorized to post to this channel")
            }

            // Validate caption length
            if (request.caption.length > MAX_CAPTION_LENGTH) {
                throw IllegalArgumentException("Caption exceeds maximum length of $MAX_CAPTION_LENGTH characters")
            }

            // Validate video file
            validateVideoFile(request.videoFile, user, VideoType.REGULAR)

            // Validate content structure
            val validationResult = contentValidator.validatePlayPodsVideo(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Video validation failed: ${validationResult.errors.joinToString()}")
            }

            // Process video through advanced pipeline
            val processedVideo = videoProcessingService.processPlayPodsVideo(
                videoFile = request.videoFile,
                channelId = request.channelId,
                userId = request.userId,
                generateThumbnails = true,
                createPreview = true,
                optimizeForStreaming = true,
                generateClosedCaptions = request.autoGenerateCaptions,
                enableHDR = request.enableHDR
            )

            // Content moderation and analysis
            val moderationResult = contentModerationService.moderatePlayPodsVideo(
                caption = request.caption,
                title = request.title,
                video = processedVideo,
                tags = request.tags,
                channelId = request.channelId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Video content violates community guidelines: ${moderationResult.reason}")
            }

            // Advanced content analysis
            val contentAnalysis = creatorToolsService.analyzeVideoContent(
                video = processedVideo,
                title = request.title,
                caption = request.caption,
                tags = request.tags,
                channelHistory = channel.contentHistory
            )

            val videoId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create video
            val video = PlayPodsVideo(
                id = videoId,
                channelId = request.channelId,
                userId = request.userId,
                title = request.title,
                caption = request.caption,
                videoFile = processedVideo,
                thumbnails = processedVideo.thumbnails,
                duration = processedVideo.duration,
                visibility = request.visibility,
                category = request.category,
                tags = request.tags,
                language = request.language,
                location = request.location,
                uploadedAt = now,
                publishedAt = if (request.visibility == VideoVisibility.PUBLIC) now else null,
                scheduledPublishAt = request.scheduledPublishAt,
                isProcessing = false,
                views = 0,
                likes = 0,
                dislikes = 0,
                comments = 0,
                shares = 0,
                watchTime = 0,
                isDeleted = false,
                contentRating = request.contentRating,
                monetizationEnabled = request.enableMonetization && channel.isMonetized,
                allowComments = request.allowComments,
                allowRatings = request.allowRatings,
                qualityScore = contentAnalysis.qualityScore,
                engagementPrediction = contentAnalysis.engagementPrediction,
                viralityPotential = contentAnalysis.viralityPotential,
                topicCategories = contentAnalysis.topics,
                audienceMatch = contentAnalysis.audienceCompatibility
            )

            val savedVideo = postRepository.save(video)

            // Async operations
            async {
                // Add to recommendation engine
                recommendationEngine.indexVideo(savedVideo, contentAnalysis)
                
                // Evaluate for trending (if public)
                if (request.visibility == VideoVisibility.PUBLIC) {
                    trendingEngine.evaluateVideoForTrending(savedVideo, contentAnalysis)
                }
                
                // Notify subscribers (if public)
                if (request.visibility == VideoVisibility.PUBLIC) {
                    notificationService.notifySubscribersOfNewVideo(channel, savedVideo)
                }
                
                // Add to channel analytics
                contentAnalyticsService.trackVideoUpload(savedVideo, channel, contentAnalysis)
                
                // Check monetization eligibility
                if (savedVideo.monetizationEnabled) {
                    monetizationService.evaluateVideoMonetization(savedVideo, channel)
                }
                
                // Auto-add to relevant playlists
                creatorToolsService.suggestPlaylistAddition(savedVideo, channel)
                
                // Publish events
                eventPublisher.publishPlayPodsVideoUploaded(savedVideo)
            }

            logger.info("Successfully uploaded video: $videoId")
            savedVideo

        } catch (e: Exception) {
            logger.error("Failed to upload video for channel: ${request.channelId}", e)
            throw e
        }
    }

    /**
     * Create a Pixel (60-second short video)
     */
    suspend fun createPixel(request: CreatePlayPodsPixelRequest): PlayPodsPixel = coroutineScope {
        logger.info("Creating Pixel for channel: ${request.channelId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val channel = channelRepository.findById(request.channelId)
                ?: throw IllegalArgumentException("Channel not found: ${request.channelId}")

            if (channel.userId != request.userId) {
                throw IllegalArgumentException("User not authorized to create Pixels on this channel")
            }

            // Validate caption
            if (request.caption.length > MAX_CAPTION_LENGTH) {
                throw IllegalArgumentException("Caption exceeds maximum length of $MAX_CAPTION_LENGTH characters")
            }

            // Validate Pixel video file
            validateVideoFile(request.videoFile, user, VideoType.PIXEL)

            // Ensure duration is within Pixel limits
            if (request.videoFile.duration > MAX_PIXEL_DURATION) {
                throw IllegalArgumentException("Pixel duration cannot exceed $MAX_PIXEL_DURATION seconds")
            }

            // Process Pixel through specialized pipeline
            val processedPixel = videoProcessingService.processPlayPodsPixel(
                videoFile = request.videoFile,
                channelId = request.channelId,
                userId = request.userId,
                optimizeForMobile = true,
                enhanceForVertical = true,
                addAutoEffects = request.autoEnhance,
                generateThumbnail = true
            )

            // Content moderation for Pixel
            val moderationResult = contentModerationService.moderatePlayPodsPixel(
                caption = request.caption,
                video = processedPixel,
                music = request.musicTrack,
                effects = request.effects,
                channelId = request.channelId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Pixel content violates community guidelines: ${moderationResult.reason}")
            }

            // Pixel-specific content analysis
            val pixelAnalysis = creatorToolsService.analyzePixelContent(
                video = processedPixel,
                caption = request.caption,
                music = request.musicTrack,
                effects = request.effects,
                hashtags = extractHashtags(request.caption)
            )

            val pixelId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create Pixel
            val pixel = PlayPodsPixel(
                id = pixelId,
                channelId = request.channelId,
                userId = request.userId,
                caption = request.caption,
                videoFile = processedPixel,
                thumbnail = processedPixel.thumbnail,
                duration = processedPixel.duration,
                musicTrack = request.musicTrack,
                effects = request.effects,
                hashtags = extractHashtags(request.caption),
                createdAt = now,
                publishedAt = now,
                views = 0,
                likes = 0,
                comments = 0,
                shares = 0,
                isDeleted = false,
                trendingScore = pixelAnalysis.trendingPotential,
                viralityScore = pixelAnalysis.viralityScore,
                engagementRate = 0.0,
                completionRate = 0.0,
                isVertical = processedPixel.isVertical,
                aspectRatio = processedPixel.aspectRatio
            )

            val savedPixel = postRepository.savePixel(pixel)

            // Async operations
            async {
                // Add to Pixel recommendation feed
                recommendationEngine.indexPixel(savedPixel, pixelAnalysis)
                
                // Evaluate for Pixel trending
                trendingEngine.evaluatePixelForTrending(savedPixel, pixelAnalysis)
                
                // Notify channel subscribers about new Pixel
                notificationService.notifySubscribersOfNewPixel(channel, savedPixel)
                
                // Track Pixel analytics
                contentAnalyticsService.trackPixelCreation(savedPixel, channel, pixelAnalysis)
                
                // Music licensing checks if using copyrighted music
                request.musicTrack?.let { music ->
                    monetizationService.checkMusicLicensing(savedPixel, music)
                }
                
                // Publish events
                eventPublisher.publishPlayPodsPixelCreated(savedPixel)
            }

            logger.info("Successfully created Pixel: $pixelId")
            savedPixel

        } catch (e: Exception) {
            logger.error("Failed to create Pixel for channel: ${request.channelId}", e)
            throw e
        }
    }

    /**
     * Upload a podcast episode
     */
    suspend fun uploadPodcast(request: CreatePlayPodsPodcastRequest): PlayPodsPodcast = coroutineScope {
        logger.info("Uploading podcast for channel: ${request.channelId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val channel = channelRepository.findById(request.channelId)
                ?: throw IllegalArgumentException("Channel not found: ${request.channelId}")

            if (channel.userId != request.userId) {
                throw IllegalArgumentException("User not authorized to upload podcasts to this channel")
            }

            // Validate caption
            if (request.description.length > MAX_CAPTION_LENGTH) {
                throw IllegalArgumentException("Description exceeds maximum length of $MAX_CAPTION_LENGTH characters")
            }

            // Validate audio file
            validateAudioFile(request.audioFile, user)

            // Process podcast through audio pipeline
            val processedPodcast = audioProcessingService.processPlayPodsPodcast(
                audioFile = request.audioFile,
                channelId = request.channelId,
                userId = request.userId,
                enhanceAudio = true,
                generateTranscript = request.autoGenerateTranscript,
                createChapters = request.autoCreateChapters,
                normalizeVolume = true
            )

            // Content moderation for podcast
            val moderationResult = contentModerationService.moderatePlayPodsPodcast(
                title = request.title,
                description = request.description,
                audio = processedPodcast,
                transcript = processedPodcast.transcript,
                channelId = request.channelId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Podcast content violates community guidelines: ${moderationResult.reason}")
            }

            // Podcast content analysis
            val podcastAnalysis = creatorToolsService.analyzePodcastContent(
                audio = processedPodcast,
                title = request.title,
                description = request.description,
                category = request.category,
                transcript = processedPodcast.transcript
            )

            val podcastId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create podcast
            val podcast = PlayPodsPodcast(
                id = podcastId,
                channelId = request.channelId,
                userId = request.userId,
                title = request.title,
                description = request.description,
                audioFile = processedPodcast,
                coverArt = request.coverArt,
                duration = processedPodcast.duration,
                episodeNumber = request.episodeNumber,
                seasonNumber = request.seasonNumber,
                category = request.category,
                tags = request.tags,
                language = request.language,
                transcript = processedPodcast.transcript,
                chapters = processedPodcast.chapters,
                uploadedAt = now,
                publishedAt = if (request.visibility == PodcastVisibility.PUBLIC) now else null,
                scheduledPublishAt = request.scheduledPublishAt,
                visibility = request.visibility,
                views = 0,
                likes = 0,
                comments = 0,
                shares = 0,
                totalListenTime = 0,
                isDeleted = false,
                contentRating = request.contentRating,
                monetizationEnabled = request.enableMonetization && channel.isMonetized,
                allowComments = request.allowComments,
                qualityScore = podcastAnalysis.qualityScore,
                engagementPrediction = podcastAnalysis.engagementPrediction,
                topicCategories = podcastAnalysis.topics,
                speakerCount = podcastAnalysis.speakerCount,
                averageListenDuration = 0.0
            )

            val savedPodcast = postRepository.savePodcast(podcast)

            // Async operations
            async {
                // Add to podcast recommendations
                recommendationEngine.indexPodcast(savedPodcast, podcastAnalysis)
                
                // Notify podcast subscribers
                if (request.visibility == PodcastVisibility.PUBLIC) {
                    notificationService.notifySubscribersOfNewPodcast(channel, savedPodcast)
                }
                
                // Track podcast analytics
                contentAnalyticsService.trackPodcastUpload(savedPodcast, channel, podcastAnalysis)
                
                // Check monetization
                if (savedPodcast.monetizationEnabled) {
                    monetizationService.evaluatePodcastMonetization(savedPodcast, channel)
                }
                
                // Auto-add to podcast playlists
                creatorToolsService.suggestPodcastPlaylistAddition(savedPodcast, channel)
                
                // Publish events
                eventPublisher.publishPlayPodsPodcastUploaded(savedPodcast)
            }

            logger.info("Successfully uploaded podcast: $podcastId")
            savedPodcast

        } catch (e: Exception) {
            logger.error("Failed to upload podcast for channel: ${request.channelId}", e)
            throw e
        }
    }

    /**
     * Like content with engagement tracking
     */
    suspend fun likeContent(userId: String, contentId: String, contentType: PlayPodsContentType): PlayPodsInteraction = coroutineScope {
        logger.info("User $userId liking $contentType $contentId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Check if already liked
            val existingLike = interactionRepository.findLike(userId, contentId, contentType)
            if (existingLike != null) {
                throw IllegalArgumentException("Content already liked by user")
            }

            val now = Instant.now()

            // Create like interaction
            val like = PlayPodsInteraction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                contentId = contentId,
                contentType = contentType,
                type = InteractionType.LIKE,
                createdAt = now,
                isActive = true
            )

            val savedLike = interactionRepository.save(like)

            // Update content like count
            when (contentType) {
                PlayPodsContentType.VIDEO -> postRepository.incrementVideoLikes(contentId)
                PlayPodsContentType.PIXEL -> postRepository.incrementPixelLikes(contentId)
                PlayPodsContentType.PODCAST -> postRepository.incrementPodcastLikes(contentId)
            }

            // Async operations
            async {
                // Get content owner for notifications
                val content = getContentById(contentId, contentType)
                val contentOwner = userRepository.findById(content.userId)!!
                
                // Send notification (if not self-like)
                if (userId != content.userId) {
                    notificationService.sendPlayPodsLikeNotification(user, contentOwner, content, contentType)
                }
                
                // Update recommendation algorithms
                recommendationEngine.recordEngagement(user, content, InteractionType.LIKE)
                
                // Check for trending boost
                when (contentType) {
                    PlayPodsContentType.VIDEO -> {
                        if ((content as PlayPodsVideo).likes + 1 >= VIDEO_TRENDING_THRESHOLD) {
                            trendingEngine.evaluateVideoForTrending(content, null)
                        }
                    }
                    PlayPodsContentType.PIXEL -> {
                        if ((content as PlayPodsPixel).likes + 1 >= PIXELS_TRENDING_THRESHOLD) {
                            trendingEngine.evaluatePixelForTrending(content, null)
                        }
                    }
                    PlayPodsContentType.PODCAST -> {
                        // Podcasts have different trending metrics
                        trendingEngine.evaluatePodcastEngagement(content as PlayPodsPodcast)
                    }
                }
                
                // Track analytics
                contentAnalyticsService.trackPlayPodsInteraction(savedLike, user, content)
                
                // Publish events
                eventPublisher.publishPlayPodsContentLiked(savedLike)
            }

            logger.info("Successfully liked $contentType: $contentId")
            savedLike

        } catch (e: Exception) {
            logger.error("Failed to like $contentType $contentId by user $userId", e)
            throw e
        }
    }

    /**
     * Get trending content across all PlayPods content types
     */
    suspend fun getTrendingContent(
        userId: String,
        contentType: PlayPodsContentType? = null,
        limit: Int = 20
    ): PlayPodsTrendingResponse = coroutineScope {
        logger.debug("Getting trending content for user: $userId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Get trending content by type
            val trendingContent = trendingEngine.getTrendingPlayPodsContent(
                user = user,
                contentType = contentType,
                limit = limit,
                includePersonalization = true
            )

            // Enhance with user interaction data
            val enhancedContent = trendingContent.map { content ->
                async {
                    val userInteractions = interactionRepository.getUserContentInteractions(userId, content.id, content.type)
                    val contentOwner = userRepository.findById(content.userId)!!
                    val channel = channelRepository.findById(content.channelId)!!
                    
                    PlayPodsTrendingItem(
                        content = content,
                        owner = contentOwner,
                        channel = channel,
                        isLikedByUser = userInteractions.any { it.type == InteractionType.LIKE },
                        isSubscribed = followRepository.isSubscribed(userId, content.channelId),
                        trendingReason = trendingEngine.getTrendingReason(content.id),
                        trendingScore = trendingEngine.getTrendingScore(content.id),
                        personalizedScore = recommendationEngine.getPersonalizedScore(user, content)
                    )
                }
            }.awaitAll()

            // Track trending view
            async {
                contentAnalyticsService.trackTrendingView(user, enhancedContent.size, contentType)
            }

            PlayPodsTrendingResponse(
                content = enhancedContent,
                contentType = contentType,
                hasMore = trendingContent.size == limit,
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to get trending content for user: $userId", e)
            throw e
        }
    }

    // Helper methods

    private fun validateVideoFile(videoFile: VideoFile, user: Any, videoType: VideoType) {
        // Check file format
        val fileExtension = videoFile.url.substringAfterLast(".", "").lowercase()
        if (!SUPPORTED_VIDEO_FORMATS.contains(fileExtension)) {
            throw IllegalArgumentException("Unsupported video format: $fileExtension")
        }

        // Check file size based on user tier
        val maxSize = if (user.isPremium) MAX_VIDEO_SIZE else MAX_STANDARD_VIDEO_SIZE
        if (videoFile.sizeBytes > maxSize) {
            throw IllegalArgumentException("Video file exceeds size limit")
        }

        // Check duration based on video type
        when (videoType) {
            VideoType.PIXEL -> {
                if (videoFile.duration > MAX_PIXEL_DURATION) {
                    throw IllegalArgumentException("Pixel duration cannot exceed $MAX_PIXEL_DURATION seconds")
                }
            }
            VideoType.REGULAR -> {
                if (videoFile.duration > MAX_REGULAR_VIDEO_DURATION) {
                    throw IllegalArgumentException("Video duration cannot exceed ${MAX_REGULAR_VIDEO_DURATION / 3600} hours")
                }
            }
        }
    }

    private fun validateAudioFile(audioFile: AudioFile, user: Any) {
        // Check file format
        val fileExtension = audioFile.url.substringAfterLast(".", "").lowercase()
        if (!SUPPORTED_AUDIO_FORMATS.contains(fileExtension)) {
            throw IllegalArgumentException("Unsupported audio format: $fileExtension")
        }

        // Check duration
        if (audioFile.duration > MAX_PODCAST_DURATION) {
            throw IllegalArgumentException("Podcast duration cannot exceed ${MAX_PODCAST_DURATION / 3600} hours")
        }
    }

    private fun extractHashtags(content: String): List<String> {
        val hashtagRegex = "#\\w+".toRegex()
        return hashtagRegex.findAll(content).map { it.value.substring(1).lowercase() }.distinct().toList()
    }

    private suspend fun getContentById(contentId: String, contentType: PlayPodsContentType): Any {
        return when (contentType) {
            PlayPodsContentType.VIDEO -> postRepository.findVideoById(contentId)
                ?: throw IllegalArgumentException("Video not found: $contentId")
            PlayPodsContentType.PIXEL -> postRepository.findPixelById(contentId)
                ?: throw IllegalArgumentException("Pixel not found: $contentId")
            PlayPodsContentType.PODCAST -> postRepository.findPodcastById(contentId)
                ?: throw IllegalArgumentException("Podcast not found: $contentId")
        }
    }
}

// Data classes for PlayPods-specific features

data class CreatePlayPodsVideoRequest(
    val userId: String,
    val channelId: String,
    val title: String,
    val caption: String,
    val videoFile: VideoFile,
    val visibility: VideoVisibility,
    val category: String,
    val tags: List<String>,
    val language: String,
    val location: String?,
    val scheduledPublishAt: Instant?,
    val contentRating: ContentRating,
    val enableMonetization: Boolean,
    val allowComments: Boolean,
    val allowRatings: Boolean,
    val autoGenerateCaptions: Boolean,
    val enableHDR: Boolean
)

data class CreatePlayPodsPixelRequest(
    val userId: String,
    val channelId: String,
    val caption: String,
    val videoFile: VideoFile,
    val musicTrack: MusicTrack?,
    val effects: List<VideoEffect>,
    val autoEnhance: Boolean
)

data class CreatePlayPodsPodcastRequest(
    val userId: String,
    val channelId: String,
    val title: String,
    val description: String,
    val audioFile: AudioFile,
    val coverArt: String?,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val category: String,
    val tags: List<String>,
    val language: String,
    val visibility: PodcastVisibility,
    val scheduledPublishAt: Instant?,
    val contentRating: ContentRating,
    val enableMonetization: Boolean,
    val allowComments: Boolean,
    val autoGenerateTranscript: Boolean,
    val autoCreateChapters: Boolean
)

data class PlayPodsVideo(
    val id: String,
    val channelId: String,
    val userId: String,
    val title: String,
    val caption: String,
    val videoFile: ProcessedVideoFile,
    val thumbnails: List<VideoThumbnail>,
    val duration: Int,
    val visibility: VideoVisibility,
    val category: String,
    val tags: List<String>,
    val language: String,
    val location: String?,
    val uploadedAt: Instant,
    val publishedAt: Instant?,
    val scheduledPublishAt: Instant?,
    val isProcessing: Boolean,
    val views: Long,
    val likes: Int,
    val dislikes: Int,
    val comments: Int,
    val shares: Int,
    val watchTime: Long,
    val isDeleted: Boolean,
    val contentRating: ContentRating,
    val monetizationEnabled: Boolean,
    val allowComments: Boolean,
    val allowRatings: Boolean,
    val qualityScore: Double,
    val engagementPrediction: Double,
    val viralityPotential: Double,
    val topicCategories: List<String>,
    val audienceMatch: Double
) {
    val type = PlayPodsContentType.VIDEO
}

data class PlayPodsPixel(
    val id: String,
    val channelId: String,
    val userId: String,
    val caption: String,
    val videoFile: ProcessedVideoFile,
    val thumbnail: VideoThumbnail,
    val duration: Int,
    val musicTrack: MusicTrack?,
    val effects: List<VideoEffect>,
    val hashtags: List<String>,
    val createdAt: Instant,
    val publishedAt: Instant,
    val views: Long,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val isDeleted: Boolean,
    val trendingScore: Double,
    val viralityScore: Double,
    val engagementRate: Double,
    val completionRate: Double,
    val isVertical: Boolean,
    val aspectRatio: String
) {
    val type = PlayPodsContentType.PIXEL
}

data class PlayPodsPodcast(
    val id: String,
    val channelId: String,
    val userId: String,
    val title: String,
    val description: String,
    val audioFile: ProcessedAudioFile,
    val coverArt: String?,
    val duration: Int,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val category: String,
    val tags: List<String>,
    val language: String,
    val transcript: String?,
    val chapters: List<PodcastChapter>,
    val uploadedAt: Instant,
    val publishedAt: Instant?,
    val scheduledPublishAt: Instant?,
    val visibility: PodcastVisibility,
    val views: Long,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    val totalListenTime: Long,
    val isDeleted: Boolean,
    val contentRating: ContentRating,
    val monetizationEnabled: Boolean,
    val allowComments: Boolean,
    val qualityScore: Double,
    val engagementPrediction: Double,
    val topicCategories: List<String>,
    val speakerCount: Int,
    val averageListenDuration: Double
) {
    val type = PlayPodsContentType.PODCAST
}

data class PlayPodsInteraction(
    val id: String,
    val userId: String,
    val contentId: String,
    val contentType: PlayPodsContentType,
    val type: InteractionType,
    val createdAt: Instant,
    val isActive: Boolean
)

data class PlayPodsTrendingItem(
    val content: Any, // Video, Pixel, or Podcast
    val owner: Any, // User object
    val channel: Any, // Channel object
    val isLikedByUser: Boolean,
    val isSubscribed: Boolean,
    val trendingReason: String,
    val trendingScore: Double,
    val personalizedScore: Double
)

data class PlayPodsTrendingResponse(
    val content: List<PlayPodsTrendingItem>,
    val contentType: PlayPodsContentType?,
    val hasMore: Boolean,
    val timestamp: Instant
)

data class VideoFile(
    val url: String,
    val sizeBytes: Long,
    val duration: Int,
    val width: Int,
    val height: Int,
    val frameRate: Double,
    val bitrate: Int
)

data class AudioFile(
    val url: String,
    val sizeBytes: Long,
    val duration: Int,
    val bitrate: Int,
    val sampleRate: Int,
    val channels: Int
)

data class ProcessedVideoFile(
    val originalUrl: String,
    val streamingUrls: Map<String, String>, // Resolution -> URL
    val duration: Int,
    val width: Int,
    val height: Int,
    val thumbnails: List<VideoThumbnail>,
    val thumbnail: VideoThumbnail,
    val isVertical: Boolean,
    val aspectRatio: String,
    val hasHDR: Boolean
)

data class ProcessedAudioFile(
    val originalUrl: String,
    val streamingUrl: String,
    val duration: Int,
    val bitrate: Int,
    val transcript: String?,
    val chapters: List<PodcastChapter>
)

data class VideoThumbnail(
    val url: String,
    val width: Int,
    val height: Int,
    val timeIndex: Int
)

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val isLicensed: Boolean
)

data class VideoEffect(
    val type: String,
    val parameters: Map<String, Any>
)

data class PodcastChapter(
    val title: String,
    val startTime: Int,
    val endTime: Int,
    val description: String?
)

enum class PlayPodsContentType {
    VIDEO, PIXEL, PODCAST
}

enum class VideoVisibility {
    PUBLIC, UNLISTED, PRIVATE, SCHEDULED
}

enum class PodcastVisibility {
    PUBLIC, UNLISTED, PRIVATE, SCHEDULED
}

enum class ContentRating {
    GENERAL, TEEN, MATURE, EXPLICIT
}

enum class InteractionType {
    LIKE, DISLIKE, COMMENT, SHARE, SAVE, SUBSCRIBE
}

enum class VideoType {
    REGULAR, PIXEL
}