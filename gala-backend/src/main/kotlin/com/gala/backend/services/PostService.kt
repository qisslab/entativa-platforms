package com.gala.backend.services

import com.gala.backend.data.repositories.PostRepository
import com.gala.backend.data.repositories.UserRepository
import com.gala.backend.data.repositories.InteractionRepository
import com.gala.backend.data.repositories.FollowRepository
import com.gala.backend.data.repositories.StoryRepository
import com.gala.backend.data.models.*
import com.gala.backend.auth.AuthenticationService
import com.gala.backend.notifications.NotificationService
import com.gala.backend.analytics.PostAnalyticsService
import com.gala.backend.algorithms.DiscoveryAlgorithm
import com.gala.backend.algorithms.ExploreRankingEngine
import com.gala.backend.messaging.EventPublisher
import com.gala.backend.validation.PostValidator
import com.gala.backend.media.ImageProcessingService
import com.gala.backend.content.ContentModerationService
import com.gala.backend.visual.VisualAnalysisService
import com.gala.backend.feed.ExploreFeedService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gala Post Service - Instagram-like Visual Content Management
 * Handles strict caption validation, image optimization, and visual content interactions
 * 
 * @author Neo Qiss
 * @status Production-ready with advanced visual content algorithms
 */
@Singleton
class PostService @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val interactionRepository: InteractionRepository,
    private val followRepository: FollowRepository,
    private val storyRepository: StoryRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val postAnalyticsService: PostAnalyticsService,
    private val discoveryAlgorithm: DiscoveryAlgorithm,
    private val exploreRankingEngine: ExploreRankingEngine,
    private val eventPublisher: EventPublisher,
    private val postValidator: PostValidator,
    private val imageProcessingService: ImageProcessingService,
    private val contentModerationService: ContentModerationService,
    private val visualAnalysisService: VisualAnalysisService,
    private val exploreFeedService: ExploreFeedService
) {
    
    private val logger = LoggerFactory.getLogger(PostService::class.java)
    
    companion object {
        const val MAX_CAPTION_WIDTH_PIXELS = 320  // Estimated single line width at standard font
        const val MAX_IMAGES_PER_POST = 10        // Maximum images per post
        const val MAX_IMAGE_SIZE = 20 * 1024 * 1024  // 20MB per image (high quality for visual platform)
        const val SUPPORTED_IMAGE_FORMATS = setOf("jpg", "jpeg", "png", "heic", "webp")
        const val STANDARD_FONT_SIZE = 14         // Standard caption font size
        const val STORY_EXPIRY_HOURS = 24         // Stories expire after 24 hours
        val CAPTION_FONT = Font("Arial", Font.PLAIN, STANDARD_FONT_SIZE)
    }

    /**
     * Create a new Gala post with strict visual content validation
     */
    suspend fun createPost(request: CreateGalaPostRequest): GalaPost = coroutineScope {
        logger.info("Creating Gala post for user: ${request.userId}")
        
        try {
            // Validate user authentication
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            // Validate image count
            if (request.images.isEmpty()) {
                throw IllegalArgumentException("Gala posts must contain at least one image")
            }

            if (request.images.size > MAX_IMAGES_PER_POST) {
                throw IllegalArgumentException("Post cannot have more than $MAX_IMAGES_PER_POST images")
            }

            // Strict caption validation for one-line enforcement
            if (!request.caption.isNullOrBlank()) {
                validateCaptionOneLine(request.caption)
            }

            // Validate post structure
            val validationResult = postValidator.validateGalaPost(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Post validation failed: ${validationResult.errors.joinToString()}")
            }

            // Validate and process images
            val processedImages = request.images.map { image ->
                async {
                    validateAndProcessGalaImage(image, request.userId)
                }
            }.awaitAll()

            // Visual content analysis
            val visualAnalysis = visualAnalysisService.analyzeGalaPost(
                images = processedImages,
                caption = request.caption,
                hashtags = extractHashtags(request.caption ?: "")
            )

            // Content moderation
            val moderationResult = contentModerationService.moderateGalaContent(
                caption = request.caption,
                images = processedImages,
                userId = request.userId,
                hashtags = visualAnalysis.hashtags,
                visualContent = visualAnalysis
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Post content violates community guidelines: ${moderationResult.reason}")
            }

            val postId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create post
            val post = GalaPost(
                id = postId,
                userId = request.userId,
                caption = request.caption?.trim(),
                images = processedImages,
                location = request.location,
                hashtags = visualAnalysis.hashtags,
                mentions = extractMentions(request.caption ?: ""),
                createdAt = now,
                updatedAt = now,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                savesCount = 0,
                reachCount = 0,
                impressionsCount = 0,
                isEdited = false,
                isDeleted = false,
                aestheticScore = visualAnalysis.aestheticScore,
                engagementPotential = visualAnalysis.engagementPotential,
                trendingScore = 0.0,
                visualCategories = visualAnalysis.categories,
                colorPalette = visualAnalysis.dominantColors,
                imageQualityScore = visualAnalysis.qualityScore
            )

            val savedPost = postRepository.save(post)

            // Async operations
            async {
                // Add to explore feed algorithm
                exploreRankingEngine.indexPost(savedPost, visualAnalysis)
                
                // Update discovery algorithm with visual features
                discoveryAlgorithm.indexVisualContent(savedPost, visualAnalysis)
                
                // Send hashtag notifications to followers
                if (visualAnalysis.hashtags.isNotEmpty()) {
                    notificationService.sendHashtagNotifications(savedPost, visualAnalysis.hashtags)
                }
                
                // Send mention notifications
                if (post.mentions.isNotEmpty()) {
                    notificationService.sendGalaMentionNotifications(savedPost, post.mentions)
                }
                
                // Track analytics
                postAnalyticsService.trackGalaPostCreation(savedPost, visualAnalysis)
                
                // Publish events
                eventPublisher.publishGalaPostCreated(savedPost)
            }

            logger.info("Successfully created Gala post: $postId")
            savedPost

        } catch (e: Exception) {
            logger.error("Failed to create Gala post for user: ${request.userId}", e)
            throw e
        }
    }

    /**
     * Create a story with 24-hour expiry
     */
    suspend fun createStory(request: CreateGalaStoryRequest): GalaStory = coroutineScope {
        logger.info("Creating Gala story for user: ${request.userId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            // Validate image
            val processedImage = validateAndProcessGalaImage(request.image, request.userId)

            // Visual analysis for story
            val visualAnalysis = visualAnalysisService.analyzeGalaStory(
                image = processedImage,
                text = request.text,
                stickers = request.stickers
            )

            // Content moderation
            val moderationResult = contentModerationService.moderateGalaStory(
                image = processedImage,
                text = request.text,
                userId = request.userId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Story content violates community guidelines: ${moderationResult.reason}")
            }

            val storyId = UUID.randomUUID().toString()
            val now = Instant.now()
            val expiresAt = now.plusSeconds(STORY_EXPIRY_HOURS * 3600)

            // Create story
            val story = GalaStory(
                id = storyId,
                userId = request.userId,
                image = processedImage,
                text = request.text,
                stickers = request.stickers,
                backgroundColor = request.backgroundColor,
                createdAt = now,
                expiresAt = expiresAt,
                viewsCount = 0,
                repliesCount = 0,
                isDeleted = false,
                viewers = emptyList()
            )

            val savedStory = storyRepository.save(story)

            // Async operations
            async {
                // Notify followers about new story
                notificationService.sendStoryNotifications(savedStory, user)
                
                // Track analytics
                postAnalyticsService.trackStoryCreation(savedStory)
                
                // Publish events
                eventPublisher.publishGalaStoryCreated(savedStory)
            }

            logger.info("Successfully created Gala story: $storyId")
            savedStory

        } catch (e: Exception) {
            logger.error("Failed to create Gala story for user: ${request.userId}", e)
            throw e
        }
    }

    /**
     * Like a post with visual engagement tracking
     */
    suspend fun likePost(userId: String, postId: String): GalaPostInteraction = coroutineScope {
        logger.info("User $userId liking Gala post $postId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            val post = postRepository.findById(postId)
                ?: throw IllegalArgumentException("Post not found: $postId")

            // Check if already liked
            val existingLike = interactionRepository.findLike(userId, postId)
            if (existingLike != null) {
                throw IllegalArgumentException("Post already liked by user")
            }

            val now = Instant.now()

            // Create like interaction
            val like = GalaPostInteraction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                postId = postId,
                type = GalaInteractionType.LIKE,
                createdAt = now,
                isActive = true
            )

            val savedLike = interactionRepository.save(like)

            // Update post counts
            postRepository.incrementLikesCount(postId)

            // Async operations
            async {
                val postOwner = userRepository.findById(post.userId)!!
                
                // Send notification to post owner (if not self-like)
                if (userId != post.userId) {
                    notificationService.sendGalaLikeNotification(user, postOwner, post)
                }
                
                // Update aesthetic and engagement scoring
                exploreRankingEngine.recordInteraction(user, post, GalaInteractionType.LIKE)
                
                // Update discovery algorithm
                discoveryAlgorithm.recordVisualEngagement(user, post, GalaInteractionType.LIKE)
                
                // Track analytics
                postAnalyticsService.trackGalaInteraction(savedLike, user, post)
                
                // Publish events
                eventPublisher.publishGalaPostLiked(savedLike)
            }

            logger.info("Successfully liked Gala post: $postId")
            savedLike

        } catch (e: Exception) {
            logger.error("Failed to like Gala post $postId by user $userId", e)
            throw e
        }
    }

    /**
     * Save a post to collection
     */
    suspend fun savePost(userId: String, postId: String): GalaPostSave = coroutineScope {
        logger.info("User $userId saving Gala post $postId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            val post = postRepository.findById(postId)
                ?: throw IllegalArgumentException("Post not found: $postId")

            // Check if already saved
            val existingSave = interactionRepository.findSave(userId, postId)
            if (existingSave != null) {
                throw IllegalArgumentException("Post already saved by user")
            }

            val now = Instant.now()

            // Create save
            val save = GalaPostSave(
                id = UUID.randomUUID().toString(),
                userId = userId,
                postId = postId,
                collectionId = null, // Default collection
                createdAt = now
            )

            val savedSave = interactionRepository.saveSave(save)

            // Update post saves count
            postRepository.incrementSavesCount(postId)

            // Async operations
            async {
                // Update discovery algorithm (saves are high-value signals)
                discoveryAlgorithm.recordHighValueInteraction(user, post, GalaInteractionType.SAVE)
                
                // Update explore ranking (saved posts are high quality indicators)
                exploreRankingEngine.recordQualitySignal(post, GalaInteractionType.SAVE)
                
                // Track analytics
                postAnalyticsService.trackGalaSave(savedSave, user, post)
                
                // Publish events
                eventPublisher.publishGalaPostSaved(savedSave)
            }

            logger.info("Successfully saved Gala post: $postId")
            savedSave

        } catch (e: Exception) {
            logger.error("Failed to save Gala post $postId by user $userId", e)
            throw e
        }
    }

    /**
     * Get user's explore feed with visual content ranking
     */
    suspend fun getExploreFeed(
        userId: String,
        limit: Int = 20,
        category: String? = null
    ): GalaExploreResponse = coroutineScope {
        logger.debug("Getting explore feed for user: $userId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Get visually ranked explore content
            val explorePosts = exploreRankingEngine.generateExploreFeed(
                user = user,
                limit = limit,
                category = category,
                includeAesthetics = true,
                includeTrending = true
            )

            // Enhance with interaction data
            val enhancedPosts = explorePosts.map { post ->
                async {
                    val userInteractions = interactionRepository.getUserPostInteractions(userId, post.id)
                    val postOwner = userRepository.findById(post.userId)!!
                    val isFollowing = followRepository.isFollowing(userId, post.userId)
                    
                    GalaExplorePost(
                        post = post,
                        owner = postOwner,
                        isFollowing = isFollowing,
                        isLikedByUser = userInteractions.any { it.type == GalaInteractionType.LIKE },
                        isSavedByUser = userInteractions.any { it.type == GalaInteractionType.SAVE },
                        aestheticRating = post.aestheticScore,
                        trendingReason = exploreRankingEngine.getTrendingReason(post)
                    )
                }
            }.awaitAll()

            // Track explore feed view
            async {
                postAnalyticsService.trackExploreFeedView(user, enhancedPosts.size, category)
            }

            GalaExploreResponse(
                posts = enhancedPosts,
                hasMore = explorePosts.size == limit,
                category = category,
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to get explore feed for user: $userId", e)
            throw e
        }
    }

    /**
     * Get user's active stories
     */
    suspend fun getUserStories(userId: String, viewerId: String): List<GalaStoryPreview> = coroutineScope {
        logger.debug("Getting stories for user: $userId")
        
        try {
            val user = userRepository.findById(userId)
                ?: throw IllegalArgumentException("User not found: $userId")

            // Get active stories (not expired)
            val activeStories = storyRepository.getActiveStories(userId)

            // Convert to previews with view status
            activeStories.map { story ->
                async {
                    val hasViewed = viewerId in story.viewers
                    GalaStoryPreview(
                        storyId = story.id,
                        thumbnailUrl = story.image.thumbnailUrl,
                        createdAt = story.createdAt,
                        expiresAt = story.expiresAt,
                        hasViewed = hasViewed,
                        isOwner = viewerId == userId
                    )
                }
            }.awaitAll()

        } catch (e: Exception) {
            logger.error("Failed to get stories for user: $userId", e)
            emptyList()
        }
    }

    // Helper methods

    /**
     * Validate that caption fits on a single line
     */
    private fun validateCaptionOneLine(caption: String) {
        if (caption.isBlank()) return

        // Create a graphics context to measure text width
        val bufferedImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = bufferedImage.createGraphics()
        graphics.font = CAPTION_FONT
        val fontMetrics: FontMetrics = graphics.fontMetrics

        // Check if text contains line breaks
        if (caption.contains('\n') || caption.contains('\r')) {
            throw IllegalArgumentException("Caption cannot contain line breaks - Gala enforces single-line captions")
        }

        // Measure text width
        val textWidth = fontMetrics.stringWidth(caption)
        
        graphics.dispose()

        if (textWidth > MAX_CAPTION_WIDTH_PIXELS) {
            throw IllegalArgumentException(
                "Caption is too long for a single line. Maximum width is $MAX_CAPTION_WIDTH_PIXELS pixels. " +
                "Current caption width: $textWidth pixels. Please shorten your caption."
            )
        }

        logger.debug("Caption validation passed - Width: ${textWidth}px (max: ${MAX_CAPTION_WIDTH_PIXELS}px)")
    }

    private suspend fun validateAndProcessGalaImage(
        image: GalaImageItem,
        userId: String
    ): ProcessedGalaImage {
        // Validate file format
        val fileExtension = image.url.substringAfterLast(".", "").lowercase()
        
        if (!SUPPORTED_IMAGE_FORMATS.contains(fileExtension)) {
            throw IllegalArgumentException("Unsupported image format: $fileExtension. Supported formats: ${SUPPORTED_IMAGE_FORMATS.joinToString()}")
        }

        if (image.sizeBytes > MAX_IMAGE_SIZE) {
            throw IllegalArgumentException("Image size exceeds maximum allowed size of ${MAX_IMAGE_SIZE / (1024 * 1024)}MB")
        }

        // Process image through Gala's visual optimization
        return imageProcessingService.processGalaImage(
            image = image,
            userId = userId,
            applyAestheticEnhancement = true,
            generateMultipleResolutions = true,
            optimizeForMobile = true
        )
    }

    private fun extractHashtags(content: String): List<String> {
        val hashtagRegex = "#[\\w\\u00C0-\\u024F]+".toRegex() // Support international characters
        return hashtagRegex.findAll(content).map { it.value.substring(1).lowercase() }.distinct().toList()
    }

    private fun extractMentions(content: String): List<String> {
        val mentionRegex = "@[\\w.]+".toRegex()
        return mentionRegex.findAll(content).map { it.value.substring(1) }.distinct().toList()
    }
}

// Data classes for Gala-specific post features

data class CreateGalaPostRequest(
    val userId: String,
    val caption: String?,
    val images: List<GalaImageItem>,
    val location: GalaLocation?,
    val altText: List<String>? // Accessibility support
)

data class CreateGalaStoryRequest(
    val userId: String,
    val image: GalaImageItem,
    val text: String?,
    val stickers: List<GalaSticker>,
    val backgroundColor: String?
)

data class GalaPost(
    val id: String,
    val userId: String,
    val caption: String?,
    val images: List<ProcessedGalaImage>,
    val location: GalaLocation?,
    val hashtags: List<String>,
    val mentions: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val savesCount: Int,
    val reachCount: Int,
    val impressionsCount: Int,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val aestheticScore: Double,
    val engagementPotential: Double,
    val trendingScore: Double,
    val visualCategories: List<String>,
    val colorPalette: List<String>,
    val imageQualityScore: Double
)

data class GalaStory(
    val id: String,
    val userId: String,
    val image: ProcessedGalaImage,
    val text: String?,
    val stickers: List<GalaSticker>,
    val backgroundColor: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val viewsCount: Int,
    val repliesCount: Int,
    val isDeleted: Boolean,
    val viewers: List<String>
)

data class GalaPostInteraction(
    val id: String,
    val userId: String,
    val postId: String,
    val type: GalaInteractionType,
    val createdAt: Instant,
    val isActive: Boolean
)

data class GalaPostSave(
    val id: String,
    val userId: String,
    val postId: String,
    val collectionId: String?,
    val createdAt: Instant
)

data class GalaExplorePost(
    val post: GalaPost,
    val owner: Any, // User object
    val isFollowing: Boolean,
    val isLikedByUser: Boolean,
    val isSavedByUser: Boolean,
    val aestheticRating: Double,
    val trendingReason: String?
)

data class GalaExploreResponse(
    val posts: List<GalaExplorePost>,
    val hasMore: Boolean,
    val category: String?,
    val timestamp: Instant
)

data class GalaStoryPreview(
    val storyId: String,
    val thumbnailUrl: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val hasViewed: Boolean,
    val isOwner: Boolean
)

data class GalaImageItem(
    val url: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val altText: String?
)

data class ProcessedGalaImage(
    val originalUrl: String,
    val processedUrl: String,
    val thumbnailUrl: String,
    val mediumUrl: String,
    val highResUrl: String,
    val width: Int,
    val height: Int,
    val aspectRatio: Double,
    val dominantColors: List<String>,
    val aestheticScore: Double,
    val qualityScore: Double,
    val altText: String?
)

data class GalaLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String?
)

data class GalaSticker(
    val type: String,
    val position: GalaPosition,
    val data: Map<String, Any>
)

data class GalaPosition(
    val x: Double,
    val y: Double,
    val rotation: Double,
    val scale: Double
)

enum class GalaInteractionType {
    LIKE, COMMENT, SHARE, SAVE, VIEW
}
