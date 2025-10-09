package com.entativa.shared.analytics

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Entativa Analytics Manager - MongoDB-powered analytics and insights
 * Tracks user behavior, content performance, and platform metrics
 * 
 * @author Neo Qiss
 * @status Production-ready with real-time analytics
 */
class EntativaAnalyticsManager(private val mongoConnectionString: String) {
    
    private val logger = LoggerFactory.getLogger(EntativaAnalyticsManager::class.java)
    private lateinit var mongoClient: MongoClient
    private lateinit var analyticsDatabase: MongoDatabase
    
    companion object {
        private const val DATABASE_NAME = "entativa_analytics"
        
        // Collection names for different analytics categories
        private const val USER_EVENTS_COLLECTION = "user_events"
        private const val POST_ANALYTICS_COLLECTION = "post_analytics" 
        private const val PLATFORM_METRICS_COLLECTION = "platform_metrics"
        private const val ENGAGEMENT_ANALYTICS_COLLECTION = "engagement_analytics"
        private const val PERFORMANCE_METRICS_COLLECTION = "performance_metrics"
        private const val USER_JOURNEY_COLLECTION = "user_journey"
        private const val CONTENT_INSIGHTS_COLLECTION = "content_insights"
        private const val MODERATION_EVENTS_COLLECTION = "moderation_events"
    }
    
    /**
     * Initialize MongoDB connection
     */
    fun initialize() {
        logger.info("📊 Initializing MongoDB Analytics connection...")
        
        try {
            mongoClient = MongoClients.create(mongoConnectionString)
            analyticsDatabase = mongoClient.getDatabase(DATABASE_NAME)
            
            // Create indexes for better performance
            createAnalyticsIndexes()
            
            logger.info("✅ MongoDB Analytics initialized successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize MongoDB Analytics", e)
            throw e
        }
    }
    
    /**
     * Create performance-optimized indexes
     */
    private fun createAnalyticsIndexes() {
        logger.info("🔍 Creating analytics indexes...")
        
        try {
            // User events indexes
            analyticsDatabase.getCollection(USER_EVENTS_COLLECTION).apply {
                createIndex(Document("userId", 1).append("timestamp", -1))
                createIndex(Document("platform", 1).append("eventType", 1))
                createIndex(Document("timestamp", -1))
            }
            
            // Post analytics indexes
            analyticsDatabase.getCollection(POST_ANALYTICS_COLLECTION).apply {
                createIndex(Document("postId", 1).append("platform", 1))
                createIndex(Document("authorId", 1).append("timestamp", -1))
                createIndex(Document("platform", 1).append("timestamp", -1))
            }
            
            // Platform metrics indexes
            analyticsDatabase.getCollection(PLATFORM_METRICS_COLLECTION).apply {
                createIndex(Document("platform", 1).append("date", -1))
                createIndex(Document("metricType", 1).append("timestamp", -1))
            }
            
            // Engagement analytics indexes
            analyticsDatabase.getCollection(ENGAGEMENT_ANALYTICS_COLLECTION).apply {
                createIndex(Document("contentId", 1).append("platform", 1))
                createIndex(Document("userId", 1).append("timestamp", -1))
                createIndex(Document("engagementType", 1).append("timestamp", -1))
            }
            
            logger.info("✅ Analytics indexes created")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to create analytics indexes", e)
        }
    }
    
    /**
     * Track user event (login, logout, registration, etc.)
     */
    suspend fun trackUserEvent(
        userId: String,
        platform: String,
        eventType: String,
        eventData: Map<String, Any> = emptyMap(),
        sessionId: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val event = Document().apply {
                    append("userId", userId)
                    append("platform", platform)
                    append("eventType", eventType)
                    append("eventData", Document(eventData))
                    append("timestamp", Instant.now())
                    append("sessionId", sessionId)
                    append("eventId", UUID.randomUUID().toString())
                }
                
                analyticsDatabase.getCollection(USER_EVENTS_COLLECTION).insertOne(event)
                
                logger.debug("✅ Tracked user event: $eventType for user $userId on $platform")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track user event", e)
                false
            }
        }
    }
    
    /**
     * Track post analytics (views, engagement, performance)
     */
    suspend fun trackPostAnalytics(
        postId: String,
        authorId: String,
        platform: String,
        metrics: PostMetrics
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val analytics = Document().apply {
                    append("postId", postId)
                    append("authorId", authorId)
                    append("platform", platform)
                    append("timestamp", Instant.now())
                    append("metrics", Document().apply {
                        append("views", metrics.views)
                        append("likes", metrics.likes)
                        append("shares", metrics.shares)
                        append("comments", metrics.comments)
                        append("saves", metrics.saves)
                        append("engagementRate", metrics.engagementRate)
                        append("reachRate", metrics.reachRate)
                        append("impressions", metrics.impressions)
                    })
                    append("contentType", metrics.contentType)
                    append("contentLength", metrics.contentLength)
                    append("mediaCount", metrics.mediaCount)
                }
                
                analyticsDatabase.getCollection(POST_ANALYTICS_COLLECTION).insertOne(analytics)
                
                logger.debug("✅ Tracked post analytics: $postId on $platform")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track post analytics", e)
                false
            }
        }
    }
    
    /**
     * Track platform-wide metrics
     */
    suspend fun trackPlatformMetrics(
        platform: String,
        metrics: PlatformMetrics
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val platformMetrics = Document().apply {
                    append("platform", platform)
                    append("date", LocalDateTime.now().toLocalDate().toString())
                    append("timestamp", Instant.now())
                    append("metrics", Document().apply {
                        append("dailyActiveUsers", metrics.dailyActiveUsers)
                        append("weeklyActiveUsers", metrics.weeklyActiveUsers)
                        append("monthlyActiveUsers", metrics.monthlyActiveUsers)
                        append("newUsers", metrics.newUsers)
                        append("totalPosts", metrics.totalPosts)
                        append("totalEngagements", metrics.totalEngagements)
                        append("averageSessionDuration", metrics.averageSessionDuration)
                        append("retentionRate", metrics.retentionRate)
                        append("churnRate", metrics.churnRate)
                    })
                }
                
                analyticsDatabase.getCollection(PLATFORM_METRICS_COLLECTION).insertOne(platformMetrics)
                
                logger.debug("✅ Tracked platform metrics for $platform")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track platform metrics", e)
                false
            }
        }
    }
    
    /**
     * Track engagement events (likes, comments, shares, etc.)
     */
    suspend fun trackEngagement(
        userId: String,
        contentId: String,
        platform: String,
        engagementType: String,
        targetUserId: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val engagement = Document().apply {
                    append("userId", userId)
                    append("contentId", contentId)
                    append("platform", platform)
                    append("engagementType", engagementType)
                    append("targetUserId", targetUserId)
                    append("timestamp", Instant.now())
                    append("engagementId", UUID.randomUUID().toString())
                }
                
                analyticsDatabase.getCollection(ENGAGEMENT_ANALYTICS_COLLECTION).insertOne(engagement)
                
                logger.debug("✅ Tracked engagement: $engagementType by user $userId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track engagement", e)
                false
            }
        }
    }
    
    /**
     * Track user journey and behavior patterns
     */
    suspend fun trackUserJourney(
        userId: String,
        platform: String,
        journeyStep: String,
        previousStep: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val journey = Document().apply {
                    append("userId", userId)
                    append("platform", platform)
                    append("journeyStep", journeyStep)
                    append("previousStep", previousStep)
                    append("timestamp", Instant.now())
                    append("metadata", Document(metadata))
                    append("journeyId", UUID.randomUUID().toString())
                }
                
                analyticsDatabase.getCollection(USER_JOURNEY_COLLECTION).insertOne(journey)
                
                logger.debug("✅ Tracked user journey: $journeyStep for user $userId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track user journey", e)
                false
            }
        }
    }
    
    /**
     * Track content insights (trending topics, hashtags, etc.)
     */
    suspend fun trackContentInsights(
        platform: String,
        contentType: String,
        insights: ContentInsights
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val contentInsights = Document().apply {
                    append("platform", platform)
                    append("contentType", contentType)
                    append("timestamp", Instant.now())
                    append("date", LocalDateTime.now().toLocalDate().toString())
                    append("insights", Document().apply {
                        append("trendingTopics", insights.trendingTopics)
                        append("popularHashtags", insights.popularHashtags)
                        append("viralContent", insights.viralContent)
                        append("contentCategories", insights.contentCategories)
                        append("sentimentAnalysis", insights.sentimentAnalysis)
                    })
                }
                
                analyticsDatabase.getCollection(CONTENT_INSIGHTS_COLLECTION).insertOne(contentInsights)
                
                logger.debug("✅ Tracked content insights for $platform")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track content insights", e)
                false
            }
        }
    }
    
    /**
     * Track moderation events
     */
    suspend fun trackModerationEvent(
        contentId: String,
        userId: String,
        platform: String,
        moderationAction: String,
        reason: String,
        moderatorId: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val moderation = Document().apply {
                    append("contentId", contentId)
                    append("userId", userId)
                    append("platform", platform)
                    append("moderationAction", moderationAction)
                    append("reason", reason)
                    append("moderatorId", moderatorId)
                    append("timestamp", Instant.now())
                    append("moderationId", UUID.randomUUID().toString())
                }
                
                analyticsDatabase.getCollection(MODERATION_EVENTS_COLLECTION).insertOne(moderation)
                
                logger.debug("✅ Tracked moderation event: $moderationAction for content $contentId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track moderation event", e)
                false
            }
        }
    }
    
    /**
     * Get user analytics for a specific time period
     */
    suspend fun getUserAnalytics(
        userId: String,
        platform: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): UserAnalytics? {
        return withContext(Dispatchers.IO) {
            try {
                val fromInstant = fromDate.toInstant(ZoneOffset.UTC)
                val toInstant = toDate.toInstant(ZoneOffset.UTC)
                
                // Get user events
                val userEvents = analyticsDatabase.getCollection(USER_EVENTS_COLLECTION)
                    .find(
                        Document("userId", userId)
                            .append("platform", platform)
                            .append("timestamp", Document("\$gte", fromInstant).append("\$lte", toInstant))
                    )
                    .into(mutableListOf())
                
                // Get user engagements
                val engagements = analyticsDatabase.getCollection(ENGAGEMENT_ANALYTICS_COLLECTION)
                    .find(
                        Document("userId", userId)
                            .append("platform", platform)
                            .append("timestamp", Document("\$gte", fromInstant).append("\$lte", toInstant))
                    )
                    .into(mutableListOf())
                
                UserAnalytics(
                    userId = userId,
                    platform = platform,
                    totalEvents = userEvents.size,
                    totalEngagements = engagements.size,
                    sessionCount = userEvents.filter { it.getString("eventType") == "login" }.size,
                    lastActive = userEvents.maxByOrNull { it.get("timestamp") as Instant }?.get("timestamp") as? Instant
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user analytics", e)
                null
            }
        }
    }
    
    /**
     * Get trending content for platform
     */
    suspend fun getTrendingContent(
        platform: String,
        timeWindow: Duration = Duration.ofHours(24),
        limit: Int = 10
    ): List<TrendingContent>? {
        return withContext(Dispatchers.IO) {
            try {
                val since = Instant.now().minus(timeWindow)
                
                // Aggregate engagement data to find trending content
                val pipeline = listOf(
                    Document("\$match", Document("platform", platform)
                        .append("timestamp", Document("\$gte", since))),
                    Document("\$group", Document("_id", "\$contentId")
                        .append("totalEngagements", Document("\$sum", 1))
                        .append("uniqueUsers", Document("\$addToSet", "\$userId"))),
                    Document("\$addFields", Document("uniqueUserCount", Document("\$size", "\$uniqueUsers"))),
                    Document("\$sort", Document("totalEngagements", -1)),
                    Document("\$limit", limit)
                )
                
                val results = analyticsDatabase.getCollection(ENGAGEMENT_ANALYTICS_COLLECTION)
                    .aggregate(pipeline)
                    .into(mutableListOf())
                
                results.map { doc ->
                    TrendingContent(
                        contentId = doc.getString("_id"),
                        platform = platform,
                        totalEngagements = doc.getInteger("totalEngagements"),
                        uniqueUsers = doc.getInteger("uniqueUserCount")
                    )
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get trending content", e)
                null
            }
        }
    }
    
    /**
     * Close MongoDB connection
     */
    fun close() {
        logger.info("🔄 Closing MongoDB Analytics connection...")
        try {
            mongoClient.close()
            logger.info("✅ MongoDB Analytics connection closed")
        } catch (e: Exception) {
            logger.error("❌ Error closing MongoDB Analytics connection", e)
        }
    }
    
    /**
     * Health check for MongoDB
     */
    suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                analyticsDatabase.runCommand(Document("ping", 1))
                true
            } catch (e: Exception) {
                logger.error("❌ MongoDB Analytics health check failed", e)
                false
            }
        }
    }
}

// Data classes for analytics
data class PostMetrics(
    val views: Long = 0,
    val likes: Long = 0,
    val shares: Long = 0,
    val comments: Long = 0,
    val saves: Long = 0,
    val engagementRate: Double = 0.0,
    val reachRate: Double = 0.0,
    val impressions: Long = 0,
    val contentType: String,
    val contentLength: Int = 0,
    val mediaCount: Int = 0
)

data class PlatformMetrics(
    val dailyActiveUsers: Long = 0,
    val weeklyActiveUsers: Long = 0,
    val monthlyActiveUsers: Long = 0,
    val newUsers: Long = 0,
    val totalPosts: Long = 0,
    val totalEngagements: Long = 0,
    val averageSessionDuration: Double = 0.0,
    val retentionRate: Double = 0.0,
    val churnRate: Double = 0.0
)

data class ContentInsights(
    val trendingTopics: List<String> = emptyList(),
    val popularHashtags: List<String> = emptyList(),
    val viralContent: List<String> = emptyList(),
    val contentCategories: Map<String, Long> = emptyMap(),
    val sentimentAnalysis: Map<String, Double> = emptyMap()
)

data class UserAnalytics(
    val userId: String,
    val platform: String,
    val totalEvents: Int,
    val totalEngagements: Int,
    val sessionCount: Int,
    val lastActive: Instant?
)

data class TrendingContent(
    val contentId: String,
    val platform: String,
    val totalEngagements: Int,
    val uniqueUsers: Int
)

enum class Duration(val seconds: Long) {
    MINUTE(60),
    HOUR(3600),
    DAY(86400),
    WEEK(604800);
    
    companion object {
        fun ofMinutes(minutes: Long) = Duration.MINUTE.copy(seconds = minutes * 60)
        fun ofHours(hours: Long) = Duration.HOUR.copy(seconds = hours * 3600)
        fun ofDays(days: Long) = Duration.DAY.copy(seconds = days * 86400)
    }
    
    fun copy(seconds: Long): Duration {
        return object : Duration(seconds) {}
    }
}