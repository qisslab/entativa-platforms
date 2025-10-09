package com.entativa.shared.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.Row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*

/**
 * Entativa Cassandra Manager - Time-series data and high-throughput operations
 * Optimized for feeds, social graphs, and real-time data processing
 * 
 * @author Neo Qiss
 * @status Production-ready with async operations and monitoring
 */
class EntativaCassandraManager(
    private val contactPoints: List<String>,
    private val datacenter: String = "datacenter1",
    private val keyspace: String = "entativa_timeseries"
) {
    
    private val logger = LoggerFactory.getLogger(EntativaCassandraManager::class.java)
    private lateinit var session: CqlSession
    
    // Prepared statements for high-performance operations
    private lateinit var insertUserFeedStatement: PreparedStatement
    private lateinit var getUserFeedStatement: PreparedStatement
    private lateinit var insertSocialGraphStatement: PreparedStatement
    private lateinit var getSocialGraphStatement: PreparedStatement
    private lateinit var insertActivityStatement: PreparedStatement
    private lateinit var getActivityStatement: PreparedStatement
    private lateinit var insertNotificationStatement: PreparedStatement
    private lateinit var getUserNotificationsStatement: PreparedStatement
    
    /**
     * Initialize Cassandra connection and prepare statements
     */
    fun initialize() {
        logger.info("🔗 Initializing Cassandra connection...")
        
        try {
            // Create session
            session = CqlSession.builder()
                .addContactPoints(contactPoints.map { InetSocketAddress.createUnresolved(it, 9042) })
                .withLocalDatacenter(datacenter)
                .withKeyspace(keyspace)
                .build()
            
            // Prepare statements for optimal performance
            prepareStatements()
            
            logger.info("✅ Cassandra connection initialized successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize Cassandra connection", e)
            throw e
        }
    }
    
    /**
     * Prepare all statements for high-performance operations
     */
    private fun prepareStatements() {
        logger.info("📝 Preparing Cassandra statements...")
        
        try {
            // User feed operations
            insertUserFeedStatement = session.prepare("""
                INSERT INTO user_feeds (user_id, platform, time_bucket, created_at, post_id, author_id, content_type, engagement_score)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                USING TTL 2592000
            """.trimIndent())
            
            getUserFeedStatement = session.prepare("""
                SELECT post_id, author_id, content_type, engagement_score, created_at
                FROM user_feeds
                WHERE user_id = ? AND platform = ? AND time_bucket >= ? AND time_bucket <= ?
                ORDER BY time_bucket DESC, created_at DESC
                LIMIT ?
            """.trimIndent())
            
            // Social graph operations
            insertSocialGraphStatement = session.prepare("""
                INSERT INTO social_graph (user_id, platform, relationship_type, target_user_id, created_at, metadata)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent())
            
            getSocialGraphStatement = session.prepare("""
                SELECT target_user_id, relationship_type, created_at, metadata
                FROM social_graph
                WHERE user_id = ? AND platform = ? AND relationship_type = ?
            """.trimIndent())
            
            // Activity stream operations
            insertActivityStatement = session.prepare("""
                INSERT INTO user_activities (user_id, platform, time_bucket, activity_id, activity_type, created_at, target_id, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                USING TTL 7776000
            """.trimIndent())
            
            getActivityStatement = session.prepare("""
                SELECT activity_id, activity_type, created_at, target_id, metadata
                FROM user_activities
                WHERE user_id = ? AND platform = ? AND time_bucket >= ? AND time_bucket <= ?
                ORDER BY time_bucket DESC, created_at DESC
                LIMIT ?
            """.trimIndent())
            
            // Notification operations
            insertNotificationStatement = session.prepare("""
                INSERT INTO user_notifications (user_id, platform, time_bucket, notification_id, notification_type, created_at, from_user_id, content_id, message, is_read)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false)
                USING TTL 2592000
            """.trimIndent())
            
            getUserNotificationsStatement = session.prepare("""
                SELECT notification_id, notification_type, created_at, from_user_id, content_id, message, is_read
                FROM user_notifications
                WHERE user_id = ? AND platform = ? AND time_bucket >= ? AND time_bucket <= ?
                ORDER BY time_bucket DESC, created_at DESC
                LIMIT ?
            """.trimIndent())
            
            logger.info("✅ Cassandra statements prepared")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to prepare Cassandra statements", e)
            throw e
        }
    }
    
    /**
     * Insert post into user feed (time-series optimized)
     */
    suspend fun insertUserFeedPost(
        userId: String,
        platform: String,
        postId: String,
        authorId: String,
        contentType: String,
        engagementScore: Double = 0.0
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val timeBucket = getTimeBucket(now)
                
                session.executeAsync(
                    insertUserFeedStatement.bind(
                        userId,
                        platform,
                        timeBucket,
                        now,
                        postId,
                        authorId,
                        contentType,
                        engagementScore
                    )
                ).await()
                
                logger.debug("✅ Inserted feed post: $postId for user $userId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to insert user feed post", e)
                false
            }
        }
    }
    
    /**
     * Get user feed for time range
     */
    suspend fun getUserFeed(
        userId: String,
        platform: String,
        limit: Int = 50,
        hoursBack: Int = 24
    ): List<FeedPost> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val startTime = now.minusSeconds(hoursBack * 3600L)
                val startBucket = getTimeBucket(startTime)
                val endBucket = getTimeBucket(now)
                
                val resultSet = session.executeAsync(
                    getUserFeedStatement.bind(
                        userId,
                        platform,
                        startBucket,
                        endBucket,
                        limit
                    )
                ).await()
                
                val posts = mutableListOf<FeedPost>()
                for (row in resultSet) {
                    posts.add(
                        FeedPost(
                            postId = row.getString("post_id") ?: "",
                            authorId = row.getString("author_id") ?: "",
                            contentType = row.getString("content_type") ?: "",
                            engagementScore = row.getDouble("engagement_score") ?: 0.0,
                            createdAt = row.getInstant("created_at") ?: Instant.now()
                        )
                    )
                }
                
                logger.debug("✅ Retrieved ${posts.size} feed posts for user $userId")
                posts
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user feed", e)
                emptyList()
            }
        }
    }
    
    /**
     * Insert social relationship
     */
    suspend fun insertSocialRelationship(
        userId: String,
        platform: String,
        relationshipType: String,
        targetUserId: String,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                session.executeAsync(
                    insertSocialGraphStatement.bind(
                        userId,
                        platform,
                        relationshipType,
                        targetUserId,
                        Instant.now(),
                        metadata
                    )
                ).await()
                
                logger.debug("✅ Inserted social relationship: $relationshipType between $userId and $targetUserId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to insert social relationship", e)
                false
            }
        }
    }
    
    /**
     * Get social relationships by type
     */
    suspend fun getSocialRelationships(
        userId: String,
        platform: String,
        relationshipType: String
    ): List<SocialRelationship> {
        return withContext(Dispatchers.IO) {
            try {
                val resultSet = session.executeAsync(
                    getSocialGraphStatement.bind(
                        userId,
                        platform,
                        relationshipType
                    )
                ).await()
                
                val relationships = mutableListOf<SocialRelationship>()
                for (row in resultSet) {
                    relationships.add(
                        SocialRelationship(
                            targetUserId = row.getString("target_user_id") ?: "",
                            relationshipType = row.getString("relationship_type") ?: "",
                            createdAt = row.getInstant("created_at") ?: Instant.now(),
                            metadata = row.getMap("metadata", String::class.java, String::class.java) ?: emptyMap()
                        )
                    )
                }
                
                logger.debug("✅ Retrieved ${relationships.size} $relationshipType relationships for user $userId")
                relationships
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get social relationships", e)
                emptyList()
            }
        }
    }
    
    /**
     * Insert user activity
     */
    suspend fun insertUserActivity(
        userId: String,
        platform: String,
        activityType: String,
        targetId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val timeBucket = getTimeBucket(now)
                val activityId = UUID.randomUUID().toString()
                
                session.executeAsync(
                    insertActivityStatement.bind(
                        userId,
                        platform,
                        timeBucket,
                        activityId,
                        activityType,
                        now,
                        targetId,
                        metadata
                    )
                ).await()
                
                logger.debug("✅ Inserted user activity: $activityType for user $userId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to insert user activity", e)
                false
            }
        }
    }
    
    /**
     * Get user activities for time range
     */
    suspend fun getUserActivities(
        userId: String,
        platform: String,
        limit: Int = 100,
        hoursBack: Int = 168 // 1 week
    ): List<UserActivity> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val startTime = now.minusSeconds(hoursBack * 3600L)
                val startBucket = getTimeBucket(startTime)
                val endBucket = getTimeBucket(now)
                
                val resultSet = session.executeAsync(
                    getActivityStatement.bind(
                        userId,
                        platform,
                        startBucket,
                        endBucket,
                        limit
                    )
                ).await()
                
                val activities = mutableListOf<UserActivity>()
                for (row in resultSet) {
                    activities.add(
                        UserActivity(
                            activityId = row.getString("activity_id") ?: "",
                            activityType = row.getString("activity_type") ?: "",
                            createdAt = row.getInstant("created_at") ?: Instant.now(),
                            targetId = row.getString("target_id"),
                            metadata = row.getMap("metadata", String::class.java, String::class.java) ?: emptyMap()
                        )
                    )
                }
                
                logger.debug("✅ Retrieved ${activities.size} activities for user $userId")
                activities
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user activities", e)
                emptyList()
            }
        }
    }
    
    /**
     * Insert notification
     */
    suspend fun insertNotification(
        userId: String,
        platform: String,
        notificationType: String,
        fromUserId: String? = null,
        contentId: String? = null,
        message: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val timeBucket = getTimeBucket(now)
                val notificationId = UUID.randomUUID().toString()
                
                session.executeAsync(
                    insertNotificationStatement.bind(
                        userId,
                        platform,
                        timeBucket,
                        notificationId,
                        notificationType,
                        now,
                        fromUserId,
                        contentId,
                        message
                    )
                ).await()
                
                logger.debug("✅ Inserted notification: $notificationType for user $userId")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to insert notification", e)
                false
            }
        }
    }
    
    /**
     * Get user notifications
     */
    suspend fun getUserNotifications(
        userId: String,
        platform: String,
        limit: Int = 50,
        hoursBack: Int = 168 // 1 week
    ): List<UserNotification> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val startTime = now.minusSeconds(hoursBack * 3600L)
                val startBucket = getTimeBucket(startTime)
                val endBucket = getTimeBucket(now)
                
                val resultSet = session.executeAsync(
                    getUserNotificationsStatement.bind(
                        userId,
                        platform,
                        startBucket,
                        endBucket,
                        limit
                    )
                ).await()
                
                val notifications = mutableListOf<UserNotification>()
                for (row in resultSet) {
                    notifications.add(
                        UserNotification(
                            notificationId = row.getString("notification_id") ?: "",
                            notificationType = row.getString("notification_type") ?: "",
                            createdAt = row.getInstant("created_at") ?: Instant.now(),
                            fromUserId = row.getString("from_user_id"),
                            contentId = row.getString("content_id"),
                            message = row.getString("message") ?: "",
                            isRead = row.getBoolean("is_read") ?: false
                        )
                    )
                }
                
                logger.debug("✅ Retrieved ${notifications.size} notifications for user $userId")
                notifications
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user notifications", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get time bucket for partitioning (hour-based for high throughput)
     */
    private fun getTimeBucket(instant: Instant): Instant {
        val epochHour = instant.epochSecond / 3600
        return Instant.ofEpochSecond(epochHour * 3600)
    }
    
    /**
     * Health check for Cassandra
     */
    suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val resultSet = session.executeAsync("SELECT release_version FROM system.local").await()
                resultSet.one() != null
            } catch (e: Exception) {
                logger.error("❌ Cassandra health check failed", e)
                false
            }
        }
    }
    
    /**
     * Close Cassandra connection
     */
    fun close() {
        logger.info("🔄 Closing Cassandra connection...")
        try {
            session.close()
            logger.info("✅ Cassandra connection closed")
        } catch (e: Exception) {
            logger.error("❌ Error closing Cassandra connection", e)
        }
    }
}

// Data classes for Cassandra operations
data class FeedPost(
    val postId: String,
    val authorId: String,
    val contentType: String,
    val engagementScore: Double,
    val createdAt: Instant
)

data class SocialRelationship(
    val targetUserId: String,
    val relationshipType: String,
    val createdAt: Instant,
    val metadata: Map<String, String>
)

data class UserActivity(
    val activityId: String,
    val activityType: String,
    val createdAt: Instant,
    val targetId: String?,
    val metadata: Map<String, String>
)

data class UserNotification(
    val notificationId: String,
    val notificationType: String,
    val createdAt: Instant,
    val fromUserId: String?,
    val contentId: String?,
    val message: String,
    val isRead: Boolean
)

// Activity types for social media
object ActivityType {
    const val POST_CREATED = "post_created"
    const val POST_LIKED = "post_liked"
    const val POST_SHARED = "post_shared"
    const val POST_COMMENTED = "post_commented"
    const val USER_FOLLOWED = "user_followed"
    const val USER_UNFOLLOWED = "user_unfollowed"
    const val FRIEND_ADDED = "friend_added"
    const val FRIEND_REMOVED = "friend_removed"
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val PROFILE_UPDATED = "profile_updated"
}

// Notification types
object NotificationType {
    const val LIKE = "like"
    const val COMMENT = "comment"
    const val SHARE = "share"
    const val FOLLOW = "follow"
    const val FRIEND_REQUEST = "friend_request"
    const val MENTION = "mention"
    const val MESSAGE = "message"
    const val SYSTEM = "system"
}

// Relationship types for social graph
object RelationshipType {
    const val FRIEND = "friend"
    const val FOLLOWER = "follower"
    const val FOLLOWING = "following"
    const val BLOCKED = "blocked"
    const val MUTED = "muted"
}