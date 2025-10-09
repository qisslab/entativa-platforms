package com.sonet.backend.services

import com.sonet.backend.data.repositories.PostRepository
import com.sonet.backend.data.repositories.UserRepository
import com.sonet.backend.data.repositories.InteractionRepository
import com.sonet.backend.data.repositories.FriendshipRepository
import com.sonet.backend.data.models.*
import com.sonet.backend.auth.AuthenticationService
import com.sonet.backend.notifications.NotificationService
import com.sonet.backend.analytics.PostAnalyticsService
import com.sonet.backend.algorithms.FeedAlgorithm
import com.sonet.backend.algorithms.ContentRankingEngine
import com.sonet.backend.messaging.EventPublisher
import com.sonet.backend.validation.PostValidator
import com.sonet.backend.media.MediaValidationService
import com.sonet.backend.content.ContentModerationService
import com.sonet.backend.privacy.PrivacyService
import com.sonet.backend.feed.FeedDistributionService

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sonet Post Service - Facebook-like Post Management and Interactions
 * Handles post validation, interactions, privacy enforcement, and feed distribution
 * 
 * @author Neo Qiss
 * @status Production-ready with PhD-level social algorithms
 */
@Singleton
class PostService @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val interactionRepository: InteractionRepository,
    private val friendshipRepository: FriendshipRepository,
    private val authService: AuthenticationService,
    private val notificationService: NotificationService,
    private val postAnalyticsService: PostAnalyticsService,
    private val feedAlgorithm: FeedAlgorithm,
    private val contentRankingEngine: ContentRankingEngine,
    private val eventPublisher: EventPublisher,
    private val postValidator: PostValidator,
    private val mediaValidationService: MediaValidationService,
    private val contentModerationService: ContentModerationService,
    private val privacyService: PrivacyService,
    private val feedDistributionService: FeedDistributionService
) {
    
    private val logger = LoggerFactory.getLogger(PostService::class.java)
    
    companion object {
        const val MAX_POST_LENGTH = 1500  // Sonet character limit
        const val MAX_MEDIA_ITEMS = 10    // Maximum images/videos per post
        const val MAX_VIDEO_DURATION = 180 // 3 minutes in seconds
        const val MAX_IMAGE_SIZE = 10 * 1024 * 1024  // 10MB per image
        const val MAX_VIDEO_SIZE = 100 * 1024 * 1024 // 100MB per video
        const val SUPPORTED_IMAGE_FORMATS = setOf("jpg", "jpeg", "png", "gif", "webp")
        const val SUPPORTED_VIDEO_FORMATS = setOf("mp4", "mov", "avi", "webm")
    }

    /**
     * Validate and create a new Sonet post with comprehensive checks
     */
    suspend fun createPost(request: CreateSonetPostRequest): SonetPost = coroutineScope {
        logger.info("Creating Sonet post for user: ${request.userId}")
        
        try {
            // Validate user authentication
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            // Validate post content length
            if (request.content.length > MAX_POST_LENGTH) {
                throw IllegalArgumentException("Post content exceeds maximum length of $MAX_POST_LENGTH characters")
            }

            // Validate media items count
            if (request.mediaItems.size > MAX_MEDIA_ITEMS) {
                throw IllegalArgumentException("Post cannot have more than $MAX_MEDIA_ITEMS media items")
            }

            // Validate post structure
            val validationResult = postValidator.validateSonetPost(request)
            if (!validationResult.isValid) {
                throw IllegalArgumentException("Post validation failed: ${validationResult.errors.joinToString()}")
            }

            // Validate and process media items
            val processedMedia = if (request.mediaItems.isNotEmpty()) {
                request.mediaItems.map { mediaItem ->
                    async {
                        validateAndProcessSonetMedia(mediaItem, request.userId)
                    }
                }.awaitAll()
            } else {
                emptyList()
            }

            // Content moderation
            val moderationResult = contentModerationService.moderateSonetContent(
                text = request.content,
                media = processedMedia,
                userId = request.userId,
                mentions = request.mentions,
                links = extractLinks(request.content)
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Post content violates community guidelines: ${moderationResult.reason}")
            }

            // Privacy validation
            if (!privacyService.canUserPost(user, request.privacy)) {
                throw IllegalArgumentException("User privacy settings do not allow this post type")
            }

            val postId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create post
            val post = SonetPost(
                id = postId,
                userId = request.userId,
                content = request.content,
                mediaItems = processedMedia,
                privacy = request.privacy,
                location = request.location,
                mentions = request.mentions,
                hashtags = extractHashtags(request.content),
                links = extractLinks(request.content),
                createdAt = now,
                updatedAt = now,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                reachCount = 0,
                impressionsCount = 0,
                isEdited = false,
                isDeleted = false,
                engagementScore = 0.0,
                qualityScore = calculateInitialQualityScore(request.content, processedMedia),
                viralityPotential = 0.0
            )

            val savedPost = postRepository.save(post)

            // Async operations
            async {
                // Distribute to feeds based on privacy and friendships
                feedDistributionService.distributePost(savedPost, user)
                
                // Add to content ranking for discover feed
                contentRankingEngine.indexPost(savedPost)
                
                // Send mentions notifications
                if (request.mentions.isNotEmpty()) {
                    notificationService.sendMentionNotifications(savedPost, request.mentions)
                }
                
                // Track analytics
                postAnalyticsService.trackPostCreation(savedPost, user)
                
                // Publish events
                eventPublisher.publishSonetPostCreated(savedPost)
            }

            logger.info("Successfully created Sonet post: $postId")
            savedPost

        } catch (e: Exception) {
            logger.error("Failed to create Sonet post for user: ${request.userId}", e)
            throw e
        }
    }

    /**
     * Like a post with engagement tracking
     */
    suspend fun likePost(userId: String, postId: String): SonetPostInteraction = coroutineScope {
        logger.info("User $userId liking post $postId")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            val post = postRepository.findById(postId)
                ?: throw IllegalArgumentException("Post not found: $postId")

            // Check if user can see this post
            if (!privacyService.canUserSeePost(user, post)) {
                throw IllegalArgumentException("User cannot interact with this post")
            }

            // Check if already liked
            val existingLike = interactionRepository.findLike(userId, postId)
            if (existingLike != null) {
                throw IllegalArgumentException("Post already liked by user")
            }

            val now = Instant.now()

            // Create like interaction
            val like = SonetPostInteraction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                postId = postId,
                type = InteractionType.LIKE,
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
                    notificationService.sendLikeNotification(user, postOwner, post)
                }
                
                // Update engagement score
                postAnalyticsService.updateEngagementScore(post, InteractionType.LIKE)
                
                // Update feed algorithm with interaction
                feedAlgorithm.recordInteraction(user, post, InteractionType.LIKE)
                
                // Track analytics
                postAnalyticsService.trackInteraction(savedLike, user, post)
                
                // Publish events
                eventPublisher.publishPostLiked(savedLike)
            }

            logger.info("Successfully liked post: $postId by user: $userId")
            savedLike

        } catch (e: Exception) {
            logger.error("Failed to like post $postId by user $userId", e)
            throw e
        }
    }

    /**
     * Comment on a post with nested comment support
     */
    suspend fun commentOnPost(request: CreateCommentRequest): SonetComment = coroutineScope {
        logger.info("User ${request.userId} commenting on post ${request.postId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val post = postRepository.findById(request.postId)
                ?: throw IllegalArgumentException("Post not found: ${request.postId}")

            // Check if user can see this post
            if (!privacyService.canUserSeePost(user, post)) {
                throw IllegalArgumentException("User cannot comment on this post")
            }

            // Validate comment content
            if (request.content.isBlank()) {
                throw IllegalArgumentException("Comment content cannot be empty")
            }

            if (request.content.length > 500) { // Comments have shorter limit
                throw IllegalArgumentException("Comment exceeds maximum length of 500 characters")
            }

            // Content moderation for comment
            val moderationResult = contentModerationService.moderateComment(
                text = request.content,
                userId = request.userId,
                postId = request.postId,
                parentCommentId = request.parentCommentId
            )

            if (moderationResult.isBlocked) {
                throw IllegalArgumentException("Comment violates community guidelines: ${moderationResult.reason}")
            }

            val commentId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create comment
            val comment = SonetComment(
                id = commentId,
                userId = request.userId,
                postId = request.postId,
                parentCommentId = request.parentCommentId,
                content = request.content,
                mentions = extractMentions(request.content),
                createdAt = now,
                updatedAt = now,
                likesCount = 0,
                repliesCount = 0,
                isEdited = false,
                isDeleted = false
            )

            val savedComment = interactionRepository.saveComment(comment)

            // Update post comment count
            postRepository.incrementCommentsCount(request.postId)

            // Update parent comment reply count if it's a reply
            request.parentCommentId?.let { parentId ->
                interactionRepository.incrementRepliesCount(parentId)
            }

            // Async operations
            async {
                val postOwner = userRepository.findById(post.userId)!!
                
                // Send notification to post owner (if not self-comment)
                if (request.userId != post.userId) {
                    notificationService.sendCommentNotification(user, postOwner, post, savedComment)
                }
                
                // Send notification to parent comment owner (if replying)
                request.parentCommentId?.let { parentId ->
                    val parentComment = interactionRepository.findCommentById(parentId)
                    if (parentComment != null && parentComment.userId != request.userId) {
                        val parentCommentOwner = userRepository.findById(parentComment.userId)!!
                        notificationService.sendReplyNotification(user, parentCommentOwner, savedComment)
                    }
                }
                
                // Send mention notifications
                if (comment.mentions.isNotEmpty()) {
                    notificationService.sendCommentMentionNotifications(savedComment, comment.mentions)
                }
                
                // Update engagement score
                postAnalyticsService.updateEngagementScore(post, InteractionType.COMMENT)
                
                // Update feed algorithm
                feedAlgorithm.recordInteraction(user, post, InteractionType.COMMENT)
                
                // Track analytics
                postAnalyticsService.trackComment(savedComment, user, post)
                
                // Publish events
                eventPublisher.publishCommentCreated(savedComment)
            }

            logger.info("Successfully created comment: $commentId")
            savedComment

        } catch (e: Exception) {
            logger.error("Failed to create comment on post ${request.postId}", e)
            throw e
        }
    }

    /**
     * Share a post with custom message
     */
    suspend fun sharePost(request: SharePostRequest): SonetPostShare = coroutineScope {
        logger.info("User ${request.userId} sharing post ${request.postId}")
        
        try {
            val user = authService.validateUser(request.userId)
                ?: throw IllegalArgumentException("User not authenticated: ${request.userId}")

            val originalPost = postRepository.findById(request.postId)
                ?: throw IllegalArgumentException("Post not found: ${request.postId}")

            // Check if user can see and share this post
            if (!privacyService.canUserSeePost(user, originalPost)) {
                throw IllegalArgumentException("User cannot share this post")
            }

            // Validate share message if provided
            request.message?.let { message ->
                if (message.length > MAX_POST_LENGTH) {
                    throw IllegalArgumentException("Share message exceeds maximum length")
                }
            }

            val shareId = UUID.randomUUID().toString()
            val now = Instant.now()

            // Create share
            val share = SonetPostShare(
                id = shareId,
                userId = request.userId,
                originalPostId = request.postId,
                message = request.message,
                privacy = request.privacy,
                createdAt = now,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0
            )

            val savedShare = interactionRepository.saveShare(share)

            // Update original post share count
            postRepository.incrementSharesCount(request.postId)

            // Async operations
            async {
                val originalPostOwner = userRepository.findById(originalPost.userId)!!
                
                // Distribute share to user's feed
                feedDistributionService.distributeShare(savedShare, user, originalPost)
                
                // Send notification to original post owner
                if (request.userId != originalPost.userId) {
                    notificationService.sendShareNotification(user, originalPostOwner, originalPost)
                }
                
                // Update engagement score
                postAnalyticsService.updateEngagementScore(originalPost, InteractionType.SHARE)
                
                // Update feed algorithm
                feedAlgorithm.recordInteraction(user, originalPost, InteractionType.SHARE)
                
                // Track analytics
                postAnalyticsService.trackShare(savedShare, user, originalPost)
                
                // Publish events
                eventPublisher.publishPostShared(savedShare)
            }

            logger.info("Successfully shared post: $shareId")
            savedShare

        } catch (e: Exception) {
            logger.error("Failed to share post ${request.postId} by user ${request.userId}", e)
            throw e
        }
    }

    /**
     * Get user's personalized feed with advanced ranking
     */
    suspend fun getUserFeed(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
        feedType: SonetFeedType = SonetFeedType.NEWS_FEED
    ): SonetFeedResponse = coroutineScope {
        logger.debug("Getting feed for user: $userId, type: $feedType")
        
        try {
            val user = authService.validateUser(userId)
                ?: throw IllegalArgumentException("User not authenticated: $userId")

            // Get posts based on feed type and user's social graph
            val feedPosts = when (feedType) {
                SonetFeedType.NEWS_FEED -> {
                    feedAlgorithm.generateNewsFeed(user, limit, offset)
                }
                SonetFeedType.DISCOVER -> {
                    contentRankingEngine.generateDiscoverFeed(user, limit, offset)
                }
                SonetFeedType.FRIENDS_ONLY -> {
                    feedAlgorithm.generateFriendsOnlyFeed(user, limit, offset)
                }
            }

            // Enhance posts with interaction data
            val enhancedPosts = feedPosts.map { post ->
                async {
                    val userInteractions = interactionRepository.getUserPostInteractions(userId, post.id)
                    val postOwner = userRepository.findById(post.userId)!!
                    
                    SonetFeedPost(
                        post = post,
                        owner = postOwner,
                        userInteractions = userInteractions,
                        isLikedByUser = userInteractions.any { it.type == InteractionType.LIKE },
                        mutualFriendsCount = if (post.userId != userId) {
                            friendshipRepository.getMutualFriendsCount(userId, post.userId)
                        } else 0,
                        timeSincePosted = calculateTimeSince(post.createdAt)
                    )
                }
            }.awaitAll()

            // Track feed view analytics
            async {
                postAnalyticsService.trackFeedView(user, feedType, enhancedPosts.size)
            }

            SonetFeedResponse(
                posts = enhancedPosts,
                hasMore = feedPosts.size == limit,
                nextOffset = offset + limit,
                feedType = feedType,
                timestamp = Instant.now()
            )

        } catch (e: Exception) {
            logger.error("Failed to get feed for user: $userId", e)
            throw e
        }
    }

    /**
     * Get detailed post analytics for post owner
     */
    suspend fun getPostAnalytics(postId: String, ownerId: String): SonetPostAnalytics = coroutineScope {
        logger.debug("Getting analytics for post: $postId")
        
        try {
            val post = postRepository.findById(postId)
                ?: throw IllegalArgumentException("Post not found: $postId")

            if (post.userId != ownerId) {
                throw IllegalArgumentException("User not authorized to view these analytics")
            }

            postAnalyticsService.getPostAnalytics(postId)

        } catch (e: Exception) {
            logger.error("Failed to get analytics for post: $postId", e)
            throw e
        }
    }

    // Helper methods

    private suspend fun validateAndProcessSonetMedia(
        mediaItem: MediaItem,
        userId: String
    ): ProcessedSonetMedia {
        // Validate file format
        val fileExtension = mediaItem.url.substringAfterLast(".", "").lowercase()
        
        when (mediaItem.type) {
            MediaType.IMAGE -> {
                if (!SUPPORTED_IMAGE_FORMATS.contains(fileExtension)) {
                    throw IllegalArgumentException("Unsupported image format: $fileExtension")
                }
                if (mediaItem.sizeBytes > MAX_IMAGE_SIZE) {
                    throw IllegalArgumentException("Image size exceeds maximum allowed size")
                }
            }
            MediaType.VIDEO -> {
                if (!SUPPORTED_VIDEO_FORMATS.contains(fileExtension)) {
                    throw IllegalArgumentException("Unsupported video format: $fileExtension")
                }
                if (mediaItem.sizeBytes > MAX_VIDEO_SIZE) {
                    throw IllegalArgumentException("Video size exceeds maximum allowed size")
                }
                if (mediaItem.duration > MAX_VIDEO_DURATION) {
                    throw IllegalArgumentException("Video duration exceeds maximum of $MAX_VIDEO_DURATION seconds")
                }
            }
        }

        // Process media through validation service
        return mediaValidationService.processSonetMedia(mediaItem, userId)
    }

    private fun extractHashtags(content: String): List<String> {
        val hashtagRegex = "#\\w+".toRegex()
        return hashtagRegex.findAll(content).map { it.value.substring(1) }.toList()
    }

    private fun extractMentions(content: String): List<String> {
        val mentionRegex = "@\\w+".toRegex()
        return mentionRegex.findAll(content).map { it.value.substring(1) }.toList()
    }

    private fun extractLinks(content: String): List<String> {
        val urlRegex = "https?://[\\w\\.-]+(?:\\.[a-zA-Z]{2,})+(?:/\\S*)?".toRegex()
        return urlRegex.findAll(content).map { it.value }.toList()
    }

    private fun calculateInitialQualityScore(content: String, media: List<ProcessedSonetMedia>): Double {
        var score = 0.5 // Base score
        
        // Content quality factors
        if (content.length > 50) score += 0.1 // Substantial content
        if (content.contains("?") || content.contains("!")) score += 0.1 // Engaging
        if (media.isNotEmpty()) score += 0.2 // Has media
        if (extractHashtags(content).isNotEmpty()) score += 0.1 // Uses hashtags
        
        return minOf(1.0, score)
    }

    private fun calculateTimeSince(createdAt: Instant): String {
        val now = Instant.now()
        val duration = java.time.Duration.between(createdAt, now)
        
        return when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            else -> "${duration.toDays() / 7}w"
        }
    }
}

// Data classes for Sonet-specific post features

data class CreateSonetPostRequest(
    val userId: String,
    val content: String,
    val mediaItems: List<MediaItem>,
    val privacy: SonetPrivacy,
    val location: SonetLocation?,
    val mentions: List<String>
)

data class CreateCommentRequest(
    val userId: String,
    val postId: String,
    val parentCommentId: String?,
    val content: String
)

data class SharePostRequest(
    val userId: String,
    val postId: String,
    val message: String?,
    val privacy: SonetPrivacy
)

data class SonetPost(
    val id: String,
    val userId: String,
    val content: String,
    val mediaItems: List<ProcessedSonetMedia>,
    val privacy: SonetPrivacy,
    val location: SonetLocation?,
    val mentions: List<String>,
    val hashtags: List<String>,
    val links: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val reachCount: Int,
    val impressionsCount: Int,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val engagementScore: Double,
    val qualityScore: Double,
    val viralityPotential: Double
)

data class SonetComment(
    val id: String,
    val userId: String,
    val postId: String,
    val parentCommentId: String?,
    val content: String,
    val mentions: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val likesCount: Int,
    val repliesCount: Int,
    val isEdited: Boolean,
    val isDeleted: Boolean
)

data class SonetPostShare(
    val id: String,
    val userId: String,
    val originalPostId: String,
    val message: String?,
    val privacy: SonetPrivacy,
    val createdAt: Instant,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int
)

data class SonetPostInteraction(
    val id: String,
    val userId: String,
    val postId: String,
    val type: InteractionType,
    val createdAt: Instant,
    val isActive: Boolean
)

data class SonetFeedPost(
    val post: SonetPost,
    val owner: Any, // User object
    val userInteractions: List<SonetPostInteraction>,
    val isLikedByUser: Boolean,
    val mutualFriendsCount: Int,
    val timeSincePosted: String
)

data class SonetFeedResponse(
    val posts: List<SonetFeedPost>,
    val hasMore: Boolean,
    val nextOffset: Int,
    val feedType: SonetFeedType,
    val timestamp: Instant
)

data class MediaItem(
    val url: String,
    val type: MediaType,
    val sizeBytes: Long,
    val duration: Int = 0, // For videos
    val width: Int = 0,
    val height: Int = 0
)

data class ProcessedSonetMedia(
    val originalUrl: String,
    val processedUrl: String,
    val thumbnailUrl: String?,
    val type: MediaType,
    val sizeBytes: Long,
    val duration: Int,
    val width: Int,
    val height: Int,
    val altText: String?
)

data class SonetLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Int
)

data class SonetPostAnalytics(
    val postId: String,
    val impressions: Int,
    val reach: Int,
    val engagements: Int,
    val clickThroughRate: Double,
    val demographicBreakdown: Map<String, Int>,
    val hourlyActivity: Map<Int, Int>,
    val topComments: List<SonetComment>
)

enum class SonetPrivacy {
    PUBLIC, FRIENDS, FRIENDS_OF_FRIENDS, CUSTOM, PRIVATE
}

enum class MediaType {
    IMAGE, VIDEO
}

enum class InteractionType {
    LIKE, COMMENT, SHARE, REACTION
}

enum class SonetFeedType {
    NEWS_FEED, DISCOVER, FRIENDS_ONLY
}
