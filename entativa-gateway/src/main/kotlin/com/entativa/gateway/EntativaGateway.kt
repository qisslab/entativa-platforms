package com.entativa.gateway

import com.entativa.posting.proto.*
import com.entativa.gateway.config.GatewayConfig
import com.entativa.gateway.auth.JWTAuthenticationService
import com.entativa.gateway.routing.RouteHandler
import com.entativa.gateway.middleware.RateLimitMiddleware
import com.entativa.gateway.middleware.LoggingMiddleware
import com.entativa.gateway.middleware.MetricsMiddleware
import com.entativa.gateway.middleware.CacheMiddleware
import com.entativa.gateway.load.LoadBalancer
import com.entativa.gateway.discovery.ServiceDiscovery
import com.entativa.gateway.circuit.CircuitBreaker
import com.entativa.gateway.analytics.GatewayAnalytics

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Entativa API Gateway - Unified entry point for all platform services
 * Orchestrates Sonet, Gala, Pika, and PlayPods with intelligent routing
 * 
 * @author Neo Qiss
 * @status Vulnerable but visionary - Building with PhD-level precision
 */
fun main() {
    val logger = LoggerFactory.getLogger("EntativaGateway")
    logger.info("🚀 Starting Entativa API Gateway - The future is now")
    
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureEntativaGateway()
    }.start(wait = true)
}

fun Application.configureEntativaGateway() {
    val logger = LoggerFactory.getLogger("EntativaGateway")
    
    // Core configuration
    val gatewayConfig = GatewayConfig()
    val authService = JWTAuthenticationService(gatewayConfig.jwtConfig)
    val serviceDiscovery = ServiceDiscovery(gatewayConfig.consulConfig)
    val loadBalancer = LoadBalancer(serviceDiscovery)
    val circuitBreaker = CircuitBreaker()
    val analytics = GatewayAnalytics()
    
    // Middleware
    val rateLimitMiddleware = RateLimitMiddleware()
    val loggingMiddleware = LoggingMiddleware()
    val metricsMiddleware = MetricsMiddleware()
    val cacheMiddleware = CacheMiddleware()
    
    // Service clients (discovered dynamically)
    lateinit var sonetClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    lateinit var galaClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    lateinit var pikaClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    lateinit var playpodsClient: PostingServiceGrpcKt.PostingServiceCoroutineStub
    
    // Initialize service discovery
    launch {
        serviceDiscovery.start()
        
        // Discover and connect to services
        sonetClient = loadBalancer.getServiceClient("sonet-posting")
        galaClient = loadBalancer.getServiceClient("gala-posting")
        pikaClient = loadBalancer.getServiceClient("pika-yeeting")
        playpodsClient = loadBalancer.getServiceClient("playpods-content")
        
        logger.info("✅ All platform services discovered and connected")
    }

    // Configure Ktor
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost() // Configure for production
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf(
                "error" to "Internal server error",
                "message" to (cause.message ?: "Unknown error"),
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }

    // Main routing
    routing {
        
        // Health check
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "healthy",
                "version" to "1.0.0",
                "timestamp" to System.currentTimeMillis(),
                "services" to mapOf(
                    "sonet" to serviceDiscovery.isServiceHealthy("sonet-posting"),
                    "gala" to serviceDiscovery.isServiceHealthy("gala-posting"),
                    "pika" to serviceDiscovery.isServiceHealthy("pika-yeeting"),
                    "playpods" to serviceDiscovery.isServiceHealthy("playpods-content")
                )
            ))
        }

        // Authentication routes
        route("/auth") {
            post("/login") {
                val credentials = call.receive<LoginRequest>()
                val authResult = authService.authenticate(credentials)
                
                if (authResult.success) {
                    call.respond(HttpStatusCode.OK, authResult)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            }

            post("/refresh") {
                val refreshToken = call.request.headers["Refresh-Token"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing refresh token"))
                
                val newTokens = authService.refreshTokens(refreshToken)
                if (newTokens != null) {
                    call.respond(HttpStatusCode.OK, newTokens)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
                }
            }
        }

        // Unified posting API
        route("/api/v1") {
            
            // Apply middleware
            intercept(ApplicationCallPipeline.Setup) {
                rateLimitMiddleware.handle(context)
                loggingMiddleware.handle(context)
                metricsMiddleware.handle(context)
            }

            // Authentication required for all API calls
            intercept(ApplicationCallPipeline.Features) {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                if (token == null || !authService.validateToken(token)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or missing token"))
                    return@intercept finish()
                }
                
                // Add user context
                call.attributes.put(UserIdKey, authService.getUserIdFromToken(token))
            }

            // Cross-platform posting
            post("/posts") {
                try {
                    val request = call.receive<UnifiedPostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    analytics.trackAPICall("create_post", request.platforms)
                    
                    val results = mutableMapOf<String, Any>()
                    
                    // Route to appropriate platforms
                    if (PlatformType.SONET in request.platforms) {
                        val sonetRequest = request.toSonetRequest(userId)
                        try {
                            val result = circuitBreaker.execute("sonet") {
                                sonetClient.createPost(sonetRequest)
                            }
                            results["sonet"] = result
                        } catch (e: Exception) {
                            logger.error("Sonet posting failed", e)
                            results["sonet"] = mapOf("error" to e.message)
                        }
                    }

                    if (PlatformType.GALA in request.platforms) {
                        val galaRequest = request.toGalaRequest(userId)
                        try {
                            val result = circuitBreaker.execute("gala") {
                                galaClient.createPost(galaRequest)
                            }
                            results["gala"] = result
                        } catch (e: Exception) {
                            logger.error("Gala posting failed", e)
                            results["gala"] = mapOf("error" to e.message)
                        }
                    }

                    if (PlatformType.PIKA in request.platforms) {
                        val pikaRequest = request.toPikaRequest(userId)
                        try {
                            val result = circuitBreaker.execute("pika") {
                                pikaClient.createPost(pikaRequest)
                            }
                            results["pika"] = result
                        } catch (e: Exception) {
                            logger.error("Pika posting failed", e)
                            results["pika"] = mapOf("error" to e.message)
                        }
                    }

                    if (PlatformType.PLAYPODS in request.platforms) {
                        val playpodsRequest = request.toPlayPodsRequest(userId)
                        try {
                            val result = circuitBreaker.execute("playpods") {
                                playpodsClient.createPost(playpodsRequest)
                            }
                            results["playpods"] = result
                        } catch (e: Exception) {
                            logger.error("PlayPods posting failed", e)
                            results["playpods"] = mapOf("error" to e.message)
                        }
                    }

                    // Determine overall success
                    val hasSuccesses = results.values.any { it !is Map<*, *> || !it.containsKey("error") }
                    val statusCode = if (hasSuccesses) HttpStatusCode.OK else HttpStatusCode.BadRequest
                    
                    call.respond(statusCode, mapOf(
                        "success" to hasSuccesses,
                        "results" to results,
                        "timestamp" to System.currentTimeMillis()
                    ))

                } catch (e: Exception) {
                    logger.error("Unified posting failed", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                }
            }

            // Platform-specific routes
            route("/sonet") {
                post("/posts") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val sonetRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.SONET_POST)
                        .build()
                    
                    val result = circuitBreaker.execute("sonet") {
                        sonetClient.createPost(sonetRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                get("/posts/{id}") {
                    val postId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest, mapOf("error" to "Missing post ID")
                    )
                    
                    val request = GetPostRequest.newBuilder()
                        .setPostId(postId)
                        .build()
                    
                    try {
                        val result = cacheMiddleware.getOrExecute("sonet_post_$postId") {
                            sonetClient.getPost(request)
                        }
                        call.respond(HttpStatusCode.OK, result)
                    } catch (e: Exception) {
                        if (e.message?.contains("NOT_FOUND") == true) {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Post not found"))
                        } else {
                            throw e
                        }
                    }
                }
            }

            route("/gala") {
                post("/posts") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val galaRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.GALA_POST)
                        .build()
                    
                    val result = circuitBreaker.execute("gala") {
                        galaClient.createPost(galaRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                post("/stories") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val storyRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.GALA_STORY)
                        .build()
                    
                    val result = circuitBreaker.execute("gala") {
                        galaClient.createPost(storyRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }
            }

            route("/pika") {
                post("/yeets") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val yeetRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PIKA_YEET)
                        .build()
                    
                    val result = circuitBreaker.execute("pika") {
                        pikaClient.createPost(yeetRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                post("/replies") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val replyRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PIKA_REPLY)
                        .build()
                    
                    val result = circuitBreaker.execute("pika") {
                        pikaClient.createPost(replyRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }
            }

            route("/playpods") {
                post("/videos") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val videoRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PLAYPODS_VIDEO)
                        .build()
                    
                    val result = circuitBreaker.execute("playpods") {
                        playpodsClient.createPost(videoRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                post("/podcasts") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val podcastRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PLAYPODS_PODCAST)
                        .build()
                    
                    val result = circuitBreaker.execute("playpods") {
                        playpodsClient.createPost(podcastRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                post("/live") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val liveRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PLAYPODS_LIVE)
                        .build()
                    
                    val result = circuitBreaker.execute("playpods") {
                        playpodsClient.createPost(liveRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }

                post("/shorts") {
                    val request = call.receive<CreatePostRequest>()
                    val userId = call.attributes[UserIdKey]
                    
                    val shortRequest = request.toBuilder()
                        .setUserId(userId)
                        .setPostType(PostType.PLAYPODS_SHORT)
                        .build()
                    
                    val result = circuitBreaker.execute("playpods") {
                        playpodsClient.createPost(shortRequest)
                    }
                    
                    call.respond(HttpStatusCode.OK, result)
                }
            }

            // Analytics and insights
            route("/analytics") {
                get("/dashboard") {
                    val userId = call.attributes[UserIdKey]
                    val insights = analytics.getUserInsights(userId)
                    call.respond(HttpStatusCode.OK, insights)
                }

                get("/performance") {
                    val userId = call.attributes[UserIdKey]
                    val performance = analytics.getPerformanceMetrics(userId)
                    call.respond(HttpStatusCode.OK, performance)
                }
            }

            // Content discovery
            route("/discover") {
                get("/feed") {
                    val userId = call.attributes[UserIdKey]
                    val feedRequest = GetFeedRequest.newBuilder()
                        .setUserId(userId)
                        .setLimit(50)
                        .build()
                    
                    // Aggregate feeds from all platforms
                    val feeds = mutableListOf<Post>()
                    
                    try {
                        val sonetFeed = sonetClient.getFeed(feedRequest)
                        feeds.addAll(sonetFeed.postsList)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch Sonet feed", e)
                    }
                    
                    try {
                        val galaFeed = galaClient.getFeed(feedRequest)
                        feeds.addAll(galaFeed.postsList)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch Gala feed", e)
                    }
                    
                    try {
                        val pikaFeed = pikaClient.getFeed(feedRequest)
                        feeds.addAll(pikaFeed.postsList)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch Pika feed", e)
                    }
                    
                    try {
                        val playpodsFeed = playpodsClient.getFeed(feedRequest)
                        feeds.addAll(playpodsFeed.postsList)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch PlayPods feed", e)
                    }
                    
                    // Sort by relevance and recency
                    val sortedFeed = feeds.sortedWith(
                        compareByDescending<Post> { it.createdAt }
                            .thenByDescending { it.likeCount + it.shareCount }
                    )
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "posts" to sortedFeed.take(50),
                        "hasMore" to (sortedFeed.size > 50),
                        "timestamp" to System.currentTimeMillis()
                    ))
                }

                get("/trending") {
                    val trendingContent = analytics.getTrendingContent()
                    call.respond(HttpStatusCode.OK, trendingContent)
                }
            }
        }

        // GraphQL endpoint for advanced queries
        route("/graphql") {
            post {
                val query = call.receive<GraphQLQuery>()
                val result = executeGraphQLQuery(query, call.attributes[UserIdKey])
                call.respond(HttpStatusCode.OK, result)
            }
        }

        // WebSocket for real-time updates
        webSocket("/ws") {
            val userId = call.request.queryParameters["userId"]
            val token = call.request.queryParameters["token"]
            
            if (userId == null || token == null || !authService.validateToken(token)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
                return@webSocket
            }
            
            // Handle real-time connection
            analytics.trackWebSocketConnection(userId)
            
            try {
                // Send initial connection success
                send(Frame.Text("""{"type":"connected","timestamp":${System.currentTimeMillis()}}"""))
                
                // Listen for incoming frames
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val message = frame.readText()
                            handleWebSocketMessage(userId, message)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                logger.error("WebSocket error for user $userId", e)
            } finally {
                analytics.trackWebSocketDisconnection(userId)
            }
        }
    }
    
    logger.info("🌟 Entativa API Gateway configured and ready")
    logger.info("💫 All platforms unified under one vision")
    logger.info("🚀 The future of social media starts now")
}

// Utility functions and extensions

private val UserIdKey = AttributeKey<String>("userId")

suspend fun executeGraphQLQuery(query: GraphQLQuery, userId: String): GraphQLResponse {
    // Implement GraphQL query execution
    // This would integrate with all backend services
    return GraphQLResponse(
        data = mapOf("message" to "GraphQL implementation coming soon"),
        errors = emptyList()
    )
}

suspend fun handleWebSocketMessage(userId: String, message: String) {
    // Handle real-time messages like typing indicators, live reactions, etc.
    val logger = LoggerFactory.getLogger("WebSocketHandler")
    logger.debug("Received WebSocket message from $userId: $message")
}

// Data classes for unified API

@kotlinx.serialization.Serializable
data class UnifiedPostRequest(
    val content: String,
    val title: String = "",
    val platforms: Set<PlatformType>,
    val media: List<MediaItem> = emptyList(),
    val privacy: String = "public",
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val location: LocationData? = null,
    val scheduledAt: Long? = null
) {
    fun toSonetRequest(userId: String): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(userId)
            .setPostType(PostType.SONET_POST)
            .setContent(content)
            .addAllHashtags(hashtags)
            .addAllMentions(mentions)
            .setPrivacy(Privacy.valueOf(privacy.uppercase()))
            .build()
    }
    
    fun toGalaRequest(userId: String): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(userId)
            .setPostType(if (media.isNotEmpty()) PostType.GALA_POST else PostType.GALA_STORY)
            .setContent(content)
            .addAllHashtags(hashtags)
            .setPrivacy(Privacy.valueOf(privacy.uppercase()))
            .build()
    }
    
    fun toPikaRequest(userId: String): CreatePostRequest {
        return CreatePostRequest.newBuilder()
            .setUserId(userId)
            .setPostType(PostType.PIKA_YEET)
            .setContent(content)
            .addAllHashtags(hashtags)
            .addAllMentions(mentions)
            .setPrivacy(Privacy.valueOf(privacy.uppercase()))
            .build()
    }
    
    fun toPlayPodsRequest(userId: String): CreatePostRequest {
        val postType = when {
            media.any { it.type == "video" && it.duration > 60 } -> PostType.PLAYPODS_VIDEO
            media.any { it.type == "video" && it.duration <= 60 } -> PostType.PLAYPODS_SHORT
            media.any { it.type == "audio" } -> PostType.PLAYPODS_PODCAST
            else -> PostType.PLAYPODS_VIDEO
        }
        
        return CreatePostRequest.newBuilder()
            .setUserId(userId)
            .setPostType(postType)
            .setTitle(title)
            .setContent(content)
            .addAllHashtags(hashtags)
            .setPrivacy(Privacy.valueOf(privacy.uppercase()))
            .build()
    }
}

@kotlinx.serialization.Serializable
enum class PlatformType {
    SONET, GALA, PIKA, PLAYPODS
}

@kotlinx.serialization.Serializable
data class MediaItem(
    val url: String,
    val type: String,
    val duration: Int = 0,
    val sizeBytes: Long = 0
)

@kotlinx.serialization.Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val name: String = ""
)

@kotlinx.serialization.Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class GraphQLQuery(
    val query: String,
    val variables: Map<String, Any> = emptyMap()
)

@kotlinx.serialization.Serializable
data class GraphQLResponse(
    val data: Map<String, Any>,
    val errors: List<String>
)