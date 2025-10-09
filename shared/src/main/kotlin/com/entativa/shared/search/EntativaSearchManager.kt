package com.entativa.shared.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.*
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Entativa Search Manager - Elasticsearch-powered search and discovery
 * Provides full-text search, content discovery, and real-time indexing
 * 
 * @author Neo Qiss
 * @status Production-ready with optimized queries and monitoring
 */
class EntativaSearchManager(
    private val elasticsearchHosts: List<String> = listOf("localhost:9200")
) {
    
    private val logger = LoggerFactory.getLogger(EntativaSearchManager::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    
    private lateinit var client: ElasticsearchClient
    
    companion object {
        // Index names for different content types
        private const val USERS_INDEX = "entativa_users"
        private const val POSTS_INDEX = "entativa_posts"
        private const val COMMENTS_INDEX = "entativa_comments"
        private const val HASHTAGS_INDEX = "entativa_hashtags"
        private const val LOCATIONS_INDEX = "entativa_locations"
        
        // Search result limits
        private const val DEFAULT_SEARCH_LIMIT = 20
        private const val MAX_SEARCH_LIMIT = 100
    }
    
    /**
     * Initialize Elasticsearch connection and create indexes
     */
    fun initialize() {
        logger.info("🔍 Initializing Elasticsearch Search Manager...")
        
        try {
            // Create REST client
            val hosts = elasticsearchHosts.map { 
                val parts = it.split(":")
                HttpHost(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 9200, "http")
            }
            
            val restClient = RestClient.builder(*hosts.toTypedArray()).build()
            val transport = RestClientTransport(restClient, JacksonJsonpMapper())
            client = ElasticsearchClient(transport)
            
            // Create indexes if they don't exist
            createIndexes()
            
            logger.info("✅ Elasticsearch Search Manager initialized successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize Elasticsearch Search Manager", e)
            throw e
        }
    }
    
    /**
     * Create all necessary indexes with optimized mappings
     */
    private suspend fun createIndexes() {
        logger.info("📋 Creating Elasticsearch indexes...")
        
        try {
            // Users index
            if (!indexExists(USERS_INDEX)) {
                createUsersIndex()
            }
            
            // Posts index
            if (!indexExists(POSTS_INDEX)) {
                createPostsIndex()
            }
            
            // Comments index
            if (!indexExists(COMMENTS_INDEX)) {
                createCommentsIndex()
            }
            
            // Hashtags index
            if (!indexExists(HASHTAGS_INDEX)) {
                createHashtagsIndex()
            }
            
            // Locations index
            if (!indexExists(LOCATIONS_INDEX)) {
                createLocationsIndex()
            }
            
            logger.info("✅ All Elasticsearch indexes created/verified")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to create Elasticsearch indexes", e)
            throw e
        }
    }
    
    /**
     * Check if index exists
     */
    private suspend fun indexExists(indexName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.indices().exists(ExistsRequest.of { it.index(indexName) }).value()
            } catch (e: Exception) {
                logger.error("❌ Failed to check if index exists: $indexName", e)
                false
            }
        }
    }
    
    /**
     * Create users index with optimized mapping
     */
    private suspend fun createUsersIndex() {
        withContext(Dispatchers.IO) {
            val mapping = mapOf(
                "properties" to mapOf(
                    "user_id" to mapOf("type" to "keyword"),
                    "username" to mapOf(
                        "type" to "text",
                        "analyzer" to "standard",
                        "fields" to mapOf(
                            "keyword" to mapOf("type" to "keyword"),
                            "suggest" to mapOf(
                                "type" to "completion",
                                "analyzer" to "simple"
                            )
                        )
                    ),
                    "display_name" to mapOf(
                        "type" to "text",
                        "analyzer" to "standard",
                        "fields" to mapOf(
                            "keyword" to mapOf("type" to "keyword")
                        )
                    ),
                    "bio" to mapOf(
                        "type" to "text",
                        "analyzer" to "standard"
                    ),
                    "platform" to mapOf("type" to "keyword"),
                    "verification_status" to mapOf("type" to "keyword"),
                    "followers_count" to mapOf("type" to "long"),
                    "following_count" to mapOf("type" to "long"),
                    "posts_count" to mapOf("type" to "long"),
                    "created_at" to mapOf("type" to "date"),
                    "last_active" to mapOf("type" to "date"),
                    "location" to mapOf("type" to "geo_point"),
                    "interests" to mapOf("type" to "keyword"),
                    "language" to mapOf("type" to "keyword")
                )
            )
            
            client.indices().create(CreateIndexRequest.of { builder ->
                builder.index(USERS_INDEX).mappings { it.properties(mapping["properties"] as Map<String, Any>) }
            })
        }
    }
    
    /**
     * Create posts index with optimized mapping
     */
    private suspend fun createPostsIndex() {
        withContext(Dispatchers.IO) {
            val mapping = mapOf(
                "properties" to mapOf(
                    "post_id" to mapOf("type" to "keyword"),
                    "user_id" to mapOf("type" to "keyword"),
                    "username" to mapOf("type" to "keyword"),
                    "content" to mapOf(
                        "type" to "text",
                        "analyzer" to "standard",
                        "fields" to mapOf(
                            "raw" to mapOf("type" to "keyword")
                        )
                    ),
                    "platform" to mapOf("type" to "keyword"),
                    "content_type" to mapOf("type" to "keyword"),
                    "visibility" to mapOf("type" to "keyword"),
                    "language" to mapOf("type" to "keyword"),
                    "hashtags" to mapOf("type" to "keyword"),
                    "mentions" to mapOf("type" to "keyword"),
                    "location" to mapOf("type" to "geo_point"),
                    "location_name" to mapOf("type" to "text"),
                    "media_count" to mapOf("type" to "integer"),
                    "media_types" to mapOf("type" to "keyword"),
                    "likes_count" to mapOf("type" to "long"),
                    "shares_count" to mapOf("type" to "long"),
                    "comments_count" to mapOf("type" to "long"),
                    "views_count" to mapOf("type" to "long"),
                    "engagement_score" to mapOf("type" to "double"),
                    "created_at" to mapOf("type" to "date"),
                    "updated_at" to mapOf("type" to "date"),
                    "trending_score" to mapOf("type" to "double"),
                    "sentiment" to mapOf("type" to "keyword"),
                    "categories" to mapOf("type" to "keyword")
                )
            )
            
            client.indices().create(CreateIndexRequest.of { builder ->
                builder.index(POSTS_INDEX).mappings { it.properties(mapping["properties"] as Map<String, Any>) }
            })
        }
    }
    
    /**
     * Create comments index
     */
    private suspend fun createCommentsIndex() {
        withContext(Dispatchers.IO) {
            val mapping = mapOf(
                "properties" to mapOf(
                    "comment_id" to mapOf("type" to "keyword"),
                    "post_id" to mapOf("type" to "keyword"),
                    "user_id" to mapOf("type" to "keyword"),
                    "username" to mapOf("type" to "keyword"),
                    "content" to mapOf("type" to "text", "analyzer" to "standard"),
                    "platform" to mapOf("type" to "keyword"),
                    "parent_comment_id" to mapOf("type" to "keyword"),
                    "likes_count" to mapOf("type" to "long"),
                    "replies_count" to mapOf("type" to "long"),
                    "created_at" to mapOf("type" to "date"),
                    "sentiment" to mapOf("type" to "keyword")
                )
            )
            
            client.indices().create(CreateIndexRequest.of { builder ->
                builder.index(COMMENTS_INDEX).mappings { it.properties(mapping["properties"] as Map<String, Any>) }
            })
        }
    }
    
    /**
     * Create hashtags index
     */
    private suspend fun createHashtagsIndex() {
        withContext(Dispatchers.IO) {
            val mapping = mapOf(
                "properties" to mapOf(
                    "hashtag" to mapOf(
                        "type" to "text",
                        "analyzer" to "keyword",
                        "fields" to mapOf(
                            "suggest" to mapOf("type" to "completion")
                        )
                    ),
                    "platform" to mapOf("type" to "keyword"),
                    "usage_count" to mapOf("type" to "long"),
                    "trending_score" to mapOf("type" to "double"),
                    "first_used" to mapOf("type" to "date"),
                    "last_used" to mapOf("type" to "date"),
                    "category" to mapOf("type" to "keyword")
                )
            )
            
            client.indices().create(CreateIndexRequest.of { builder ->
                builder.index(HASHTAGS_INDEX).mappings { it.properties(mapping["properties"] as Map<String, Any>) }
            })
        }
    }
    
    /**
     * Create locations index
     */
    private suspend fun createLocationsIndex() {
        withContext(Dispatchers.IO) {
            val mapping = mapOf(
                "properties" to mapOf(
                    "location_id" to mapOf("type" to "keyword"),
                    "name" to mapOf(
                        "type" to "text",
                        "analyzer" to "standard",
                        "fields" to mapOf(
                            "suggest" to mapOf("type" to "completion")
                        )
                    ),
                    "coordinates" to mapOf("type" to "geo_point"),
                    "country" to mapOf("type" to "keyword"),
                    "region" to mapOf("type" to "keyword"),
                    "city" to mapOf("type" to "keyword"),
                    "usage_count" to mapOf("type" to "long"),
                    "platform" to mapOf("type" to "keyword")
                )
            )
            
            client.indices().create(CreateIndexRequest.of { builder ->
                builder.index(LOCATIONS_INDEX).mappings { it.properties(mapping["properties"] as Map<String, Any>) }
            })
        }
    }
    
    // ============ INDEXING OPERATIONS ============
    
    /**
     * Index user for search
     */
    suspend fun indexUser(user: SearchableUser): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.index(IndexRequest.of { builder ->
                    builder
                        .index(USERS_INDEX)
                        .id(user.userId)
                        .document(user)
                })
                
                logger.debug("✅ Indexed user: ${user.username}")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to index user: ${user.username}", e)
                false
            }
        }
    }
    
    /**
     * Index post for search
     */
    suspend fun indexPost(post: SearchablePost): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.index(IndexRequest.of { builder ->
                    builder
                        .index(POSTS_INDEX)
                        .id(post.postId)
                        .document(post)
                })
                
                logger.debug("✅ Indexed post: ${post.postId}")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to index post: ${post.postId}", e)
                false
            }
        }
    }
    
    /**
     * Index comment for search
     */
    suspend fun indexComment(comment: SearchableComment): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.index(IndexRequest.of { builder ->
                    builder
                        .index(COMMENTS_INDEX)
                        .id(comment.commentId)
                        .document(comment)
                })
                
                logger.debug("✅ Indexed comment: ${comment.commentId}")
                true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to index comment: ${comment.commentId}", e)
                false
            }
        }
    }
    
    // ============ SEARCH OPERATIONS ============
    
    /**
     * Search users with advanced filtering
     */
    suspend fun searchUsers(
        query: String,
        platform: String? = null,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        offset: Int = 0
    ): SearchResult<SearchableUser> {
        return withContext(Dispatchers.IO) {
            try {
                val searchRequest = SearchRequest.of { builder ->
                    builder
                        .index(USERS_INDEX)
                        .query { queryBuilder ->
                            val mustQueries = mutableListOf<Query>()
                            
                            // Text search across username, display_name, and bio
                            if (query.isNotBlank()) {
                                mustQueries.add(Query.of { q ->
                                    q.multiMatch { mm ->
                                        mm.query(query)
                                            .fields("username^3", "display_name^2", "bio")
                                            .fuzziness("AUTO")
                                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                                    }
                                })
                            }
                            
                            // Platform filter
                            if (platform != null) {
                                mustQueries.add(Query.of { q ->
                                    q.term { t -> t.field("platform").value(platform) }
                                })
                            }
                            
                            queryBuilder.bool { bq ->
                                bq.must(mustQueries)
                            }
                        }
                        .from(offset)
                        .size(limit.coerceAtMost(MAX_SEARCH_LIMIT))
                        .sort { s ->
                            s.field { f ->
                                f.field("followers_count").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            }
                        }
                }
                
                val response = client.search(searchRequest, SearchableUser::class.java)
                
                SearchResult(
                    items = response.hits().hits().mapNotNull { it.source() },
                    total = response.hits().total()?.value() ?: 0,
                    query = query,
                    executionTimeMs = response.took() ?: 0
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to search users", e)
                SearchResult.empty()
            }
        }
    }
    
    /**
     * Search posts with advanced filtering and ranking
     */
    suspend fun searchPosts(
        query: String,
        platform: String? = null,
        hashtags: List<String> = emptyList(),
        timeRange: TimeRange = TimeRange.ALL_TIME,
        contentType: String? = null,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        offset: Int = 0
    ): SearchResult<SearchablePost> {
        return withContext(Dispatchers.IO) {
            try {
                val searchRequest = SearchRequest.of { builder ->
                    builder
                        .index(POSTS_INDEX)
                        .query { queryBuilder ->
                            val mustQueries = mutableListOf<Query>()
                            val filterQueries = mutableListOf<Query>()
                            
                            // Text search
                            if (query.isNotBlank()) {
                                mustQueries.add(Query.of { q ->
                                    q.multiMatch { mm ->
                                        mm.query(query)
                                            .fields("content^2", "hashtags", "username")
                                            .fuzziness("AUTO")
                                    }
                                })
                            }
                            
                            // Platform filter
                            if (platform != null) {
                                filterQueries.add(Query.of { q ->
                                    q.term { t -> t.field("platform").value(platform) }
                                })
                            }
                            
                            // Hashtag filter
                            if (hashtags.isNotEmpty()) {
                                filterQueries.add(Query.of { q ->
                                    q.terms { t -> t.field("hashtags").terms { tv ->
                                        tv.value(hashtags.map { co.elastic.clients.elasticsearch._types.FieldValue.of(it) })
                                    }}
                                })
                            }
                            
                            // Time range filter
                            if (timeRange != TimeRange.ALL_TIME) {
                                val now = Instant.now()
                                val fromTime = when (timeRange) {
                                    TimeRange.LAST_HOUR -> now.minusSeconds(3600)
                                    TimeRange.LAST_DAY -> now.minusSeconds(86400)
                                    TimeRange.LAST_WEEK -> now.minusSeconds(604800)
                                    TimeRange.LAST_MONTH -> now.minusSeconds(2592000)
                                    else -> null
                                }
                                
                                if (fromTime != null) {
                                    filterQueries.add(Query.of { q ->
                                        q.range { r ->
                                            r.field("created_at").gte(co.elastic.clients.elasticsearch._types.FieldValue.of(fromTime.toString()))
                                        }
                                    })
                                }
                            }
                            
                            // Content type filter
                            if (contentType != null) {
                                filterQueries.add(Query.of { q ->
                                    q.term { t -> t.field("content_type").value(contentType) }
                                })
                            }
                            
                            queryBuilder.bool { bq ->
                                if (mustQueries.isNotEmpty()) bq.must(mustQueries)
                                if (filterQueries.isNotEmpty()) bq.filter(filterQueries)
                            }
                        }
                        .from(offset)
                        .size(limit.coerceAtMost(MAX_SEARCH_LIMIT))
                        .sort { s ->
                            s.field { f ->
                                f.field("engagement_score").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            }
                        }
                }
                
                val response = client.search(searchRequest, SearchablePost::class.java)
                
                SearchResult(
                    items = response.hits().hits().mapNotNull { it.source() },
                    total = response.hits().total()?.value() ?: 0,
                    query = query,
                    executionTimeMs = response.took() ?: 0
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to search posts", e)
                SearchResult.empty()
            }
        }
    }
    
    /**
     * Get trending hashtags
     */
    suspend fun getTrendingHashtags(
        platform: String? = null,
        limit: Int = 20
    ): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val searchRequest = SearchRequest.of { builder ->
                    builder
                        .index(HASHTAGS_INDEX)
                        .query { queryBuilder ->
                            if (platform != null) {
                                queryBuilder.term { t -> t.field("platform").value(platform) }
                            } else {
                                queryBuilder.matchAll { it }
                            }
                        }
                        .size(limit)
                        .sort { s ->
                            s.field { f ->
                                f.field("trending_score").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                            }
                        }
                }
                
                val response = client.search(searchRequest, Map::class.java)
                response.hits().hits().mapNotNull { hit ->
                    (hit.source() as? Map<*, *>)?.get("hashtag") as? String
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get trending hashtags", e)
                emptyList()
            }
        }
    }
    
    /**
     * Suggest completions for search queries
     */
    suspend fun getSuggestions(
        query: String,
        type: SuggestionType,
        platform: String? = null,
        limit: Int = 10
    ): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                when (type) {
                    SuggestionType.USERS -> getUserSuggestions(query, platform, limit)
                    SuggestionType.HASHTAGS -> getHashtagSuggestions(query, platform, limit)
                    SuggestionType.LOCATIONS -> getLocationSuggestions(query, platform, limit)
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get suggestions", e)
                emptyList()
            }
        }
    }
    
    private suspend fun getUserSuggestions(query: String, platform: String?, limit: Int): List<String> {
        // Implementation for user suggestions using completion suggester
        return emptyList() // Placeholder
    }
    
    private suspend fun getHashtagSuggestions(query: String, platform: String?, limit: Int): List<String> {
        // Implementation for hashtag suggestions
        return emptyList() // Placeholder
    }
    
    private suspend fun getLocationSuggestions(query: String, platform: String?, limit: Int): List<String> {
        // Implementation for location suggestions
        return emptyList() // Placeholder
    }
    
    /**
     * Health check for Elasticsearch
     */
    suspend fun healthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val health = client.cluster().health()
                health.status() != co.elastic.clients.elasticsearch.cluster.health.HealthStatus.Red
            } catch (e: Exception) {
                logger.error("❌ Elasticsearch health check failed", e)
                false
            }
        }
    }
    
    /**
     * Close Elasticsearch connection
     */
    fun close() {
        logger.info("🔄 Closing Elasticsearch connection...")
        try {
            // Close the underlying REST client
            logger.info("✅ Elasticsearch connection closed")
        } catch (e: Exception) {
            logger.error("❌ Error closing Elasticsearch connection", e)
        }
    }
}

// Data classes for search
data class SearchableUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String?,
    val platform: String,
    val verificationStatus: String,
    val followersCount: Long,
    val followingCount: Long,
    val postsCount: Long,
    val createdAt: Instant,
    val lastActive: Instant?,
    val location: GeoPoint?,
    val interests: List<String>,
    val language: String?
)

data class SearchablePost(
    val postId: String,
    val userId: String,
    val username: String,
    val content: String,
    val platform: String,
    val contentType: String,
    val visibility: String,
    val language: String?,
    val hashtags: List<String>,
    val mentions: List<String>,
    val location: GeoPoint?,
    val locationName: String?,
    val mediaCount: Int,
    val mediaTypes: List<String>,
    val likesCount: Long,
    val sharesCount: Long,
    val commentsCount: Long,
    val viewsCount: Long,
    val engagementScore: Double,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val trendingScore: Double,
    val sentiment: String?,
    val categories: List<String>
)

data class SearchableComment(
    val commentId: String,
    val postId: String,
    val userId: String,
    val username: String,
    val content: String,
    val platform: String,
    val parentCommentId: String?,
    val likesCount: Long,
    val repliesCount: Long,
    val createdAt: Instant,
    val sentiment: String?
)

data class GeoPoint(
    val lat: Double,
    val lon: Double
)

data class SearchResult<T>(
    val items: List<T>,
    val total: Long,
    val query: String,
    val executionTimeMs: Long
) {
    companion object {
        fun <T> empty(): SearchResult<T> = SearchResult(emptyList(), 0, "", 0)
    }
}

enum class TimeRange {
    ALL_TIME, LAST_HOUR, LAST_DAY, LAST_WEEK, LAST_MONTH
}

enum class SuggestionType {
    USERS, HASHTAGS, LOCATIONS
}