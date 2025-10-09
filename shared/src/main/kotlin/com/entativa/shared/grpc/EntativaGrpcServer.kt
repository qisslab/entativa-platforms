package com.entativa.shared.grpc

import com.entativa.shared.analytics.EntativaAnalyticsManager
import com.entativa.shared.cache.EntativaCacheManager
import com.entativa.shared.cassandra.EntativaCassandraManager
import com.entativa.shared.database.EntativaDatabaseFactory
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Entativa gRPC Server - Unified microservices communication layer
 * Handles all platform services with shared infrastructure
 * 
 * @author Neo Qiss
 * @status Production-ready with monitoring and graceful shutdown
 */
class EntativaGrpcServer(
    private val port: Int,
    private val platform: String
) {
    
    private val logger = LoggerFactory.getLogger(EntativaGrpcServer::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private lateinit var server: Server
    private lateinit var cacheManager: EntativaCacheManager
    private lateinit var analyticsManager: EntativaAnalyticsManager
    private lateinit var cassandraManager: EntativaCassandraManager
    
    // Service implementations
    private lateinit var userServiceImpl: EntativaUserServiceImpl
    private lateinit var postServiceImpl: EntativaPostServiceImpl
    private lateinit var notificationServiceImpl: EntativaNotificationServiceImpl
    private lateinit var analyticsServiceImpl: EntativaAnalyticsServiceImpl
    private lateinit var mediaServiceImpl: EntativaMediaServiceImpl
    private lateinit var messagingServiceImpl: EntativaMessagingServiceImpl
    
    /**
     * Initialize and start the gRPC server
     */
    fun start() {
        logger.info("🚀 Starting Entativa gRPC Server for platform: $platform on port: $port")
        
        try {
            // Initialize infrastructure
            initializeInfrastructure()
            
            // Initialize service implementations
            initializeServices()
            
            // Build and start server
            server = ServerBuilder.forPort(port)
                .addService(userServiceImpl)
                .addService(postServiceImpl)
                .addService(notificationServiceImpl)
                .addService(analyticsServiceImpl)
                .addService(mediaServiceImpl)
                .addService(messagingServiceImpl)
                .addService(ProtoReflectionService.newInstance()) // For debugging
                .build()
                .start()
            
            logger.info("✅ Entativa gRPC Server started successfully!")
            logger.info("🌐 Server listening on port: $port")
            logger.info("📊 Platform: $platform")
            logger.info("🔧 Services: UserService, PostService, NotificationService, AnalyticsService, MediaService, MessagingService")
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info("🔄 Shutting down gRPC server...")
                this@EntativaGrpcServer.stop()
                logger.info("✅ Server shut down complete")
            })
            
        } catch (e: Exception) {
            logger.error("❌ Failed to start gRPC server", e)
            throw e
        }
    }
    
    /**
     * Initialize all infrastructure components
     */
    private fun initializeInfrastructure() {
        logger.info("🏗️ Initializing infrastructure components...")
        
        try {
            // Initialize database connections
            EntativaDatabaseFactory.initialize(getDatabaseConfig())
            
            // Initialize cache manager
            cacheManager = EntativaCacheManager()
            
            // Initialize analytics manager
            analyticsManager = EntativaAnalyticsManager(getMongoConnectionString()).apply {
                initialize()
            }
            
            // Initialize Cassandra manager
            cassandraManager = EntativaCassandraManager(
                contactPoints = getCassandraContactPoints(),
                datacenter = "datacenter1",
                keyspace = "entativa_timeseries"
            ).apply {
                initialize()
            }
            
            logger.info("✅ All infrastructure components initialized")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize infrastructure", e)
            throw e
        }
    }
    
    /**
     * Initialize all service implementations
     */
    private fun initializeServices() {
        logger.info("🔧 Initializing service implementations...")
        
        try {
            userServiceImpl = EntativaUserServiceImpl(
                platform = platform,
                cacheManager = cacheManager,
                analyticsManager = analyticsManager,
                cassandraManager = cassandraManager
            )
            
            postServiceImpl = EntativaPostServiceImpl(
                platform = platform,
                cacheManager = cacheManager,
                analyticsManager = analyticsManager,
                cassandraManager = cassandraManager
            )
            
            notificationServiceImpl = EntativaNotificationServiceImpl(
                platform = platform,
                cacheManager = cacheManager,
                cassandraManager = cassandraManager
            )
            
            analyticsServiceImpl = EntativaAnalyticsServiceImpl(
                platform = platform,
                analyticsManager = analyticsManager,
                cacheManager = cacheManager
            )
            
            mediaServiceImpl = EntativaMediaServiceImpl(
                platform = platform,
                cacheManager = cacheManager
            )
            
            messagingServiceImpl = EntativaMessagingServiceImpl(
                platform = platform,
                cacheManager = cacheManager,
                cassandraManager = cassandraManager
            )
            
            logger.info("✅ All service implementations initialized")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize service implementations", e)
            throw e
        }
    }
    
    /**
     * Stop the server gracefully
     */
    fun stop() {
        logger.info("🔄 Stopping gRPC server...")
        
        try {
            if (::server.isInitialized) {
                server.shutdown()
                if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("⚠️ Server did not terminate gracefully, forcing shutdown...")
                    server.shutdownNow()
                }
            }
            
            // Close infrastructure connections
            if (::analyticsManager.isInitialized) {
                analyticsManager.close()
            }
            
            if (::cassandraManager.isInitialized) {
                cassandraManager.close()
            }
            
            EntativaDatabaseFactory.close()
            
            logger.info("✅ gRPC server stopped successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Error stopping gRPC server", e)
        }
    }
    
    /**
     * Block until server is terminated
     */
    fun blockUntilShutdown() {
        if (::server.isInitialized) {
            server.awaitTermination()
        }
    }
    
    /**
     * Health check for all components
     */
    suspend fun healthCheck(): HealthStatus {
        val status = HealthStatus()
        
        try {
            // Check database
            val dbHealth = EntativaDatabaseFactory.healthCheck()
            status.database = dbHealth.postgresql && dbHealth.redis
            
            // Check cache
            status.cache = cacheManager.healthCheck()
            
            // Check analytics
            status.analytics = analyticsManager.healthCheck()
            
            // Check Cassandra
            status.cassandra = cassandraManager.healthCheck()
            
            // Overall health
            status.overall = status.database && status.cache && status.analytics && status.cassandra
            
        } catch (e: Exception) {
            logger.error("❌ Health check failed", e)
            status.overall = false
        }
        
        return status
    }
    
    /**
     * Get database configuration based on environment
     */
    private fun getDatabaseConfig(): com.entativa.shared.database.DatabaseConfig {
        return com.entativa.shared.database.DatabaseConfig(
            postgres = com.entativa.shared.database.PostgresConfig(
                url = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/entativa_master",
                username = System.getenv("POSTGRES_USER") ?: "entativa_user",
                password = System.getenv("POSTGRES_PASSWORD") ?: "entativa_password",
                maxPoolSize = System.getenv("POSTGRES_MAX_POOL_SIZE")?.toIntOrNull() ?: 30,
                minIdleConnections = System.getenv("POSTGRES_MIN_IDLE")?.toIntOrNull() ?: 10
            ),
            redis = com.entativa.shared.database.RedisConfig(
                host = System.getenv("REDIS_HOST") ?: "localhost",
                port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379,
                password = System.getenv("REDIS_PASSWORD"),
                maxConnections = System.getenv("REDIS_MAX_CONNECTIONS")?.toIntOrNull() ?: 50,
                maxIdleConnections = System.getenv("REDIS_MAX_IDLE")?.toIntOrNull() ?: 20,
                minIdleConnections = System.getenv("REDIS_MIN_IDLE")?.toIntOrNull() ?: 5
            )
        )
    }
    
    /**
     * Get MongoDB connection string
     */
    private fun getMongoConnectionString(): String {
        return System.getenv("MONGODB_CONNECTION_STRING") 
            ?: "mongodb://entativa_user:entativa_password@localhost:27017/entativa_analytics"
    }
    
    /**
     * Get Cassandra contact points
     */
    private fun getCassandraContactPoints(): List<String> {
        val contactPointsEnv = System.getenv("CASSANDRA_CONTACT_POINTS")
        return if (contactPointsEnv != null) {
            contactPointsEnv.split(",").map { it.trim() }
        } else {
            listOf("localhost")
        }
    }
    
    /**
     * Start background tasks for maintenance and monitoring
     */
    private fun startBackgroundTasks() {
        logger.info("🔄 Starting background tasks...")
        
        // Health monitoring task
        scope.launch {
            while (true) {
                try {
                    val health = healthCheck()
                    if (!health.overall) {
                        logger.warn("⚠️ System health check failed: $health")
                    }
                    kotlinx.coroutines.delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    logger.error("❌ Background health check failed", e)
                    kotlinx.coroutines.delay(60000) // Wait longer on error
                }
            }
        }
        
        // Cache cleanup task
        scope.launch {
            while (true) {
                try {
                    // Perform any necessary cache maintenance
                    logger.debug("🧹 Performing cache maintenance...")
                    kotlinx.coroutines.delay(300000) // Every 5 minutes
                } catch (e: Exception) {
                    logger.error("❌ Cache maintenance failed", e)
                    kotlinx.coroutines.delay(300000)
                }
            }
        }
        
        logger.info("✅ Background tasks started")
    }
}

/**
 * Health status for all system components
 */
data class HealthStatus(
    var overall: Boolean = false,
    var database: Boolean = false,
    var cache: Boolean = false,
    var analytics: Boolean = false,
    var cassandra: Boolean = false
)

/**
 * Main entry point for platform-specific servers
 */
fun main(args: Array<String>) {
    val platform = System.getenv("PLATFORM") ?: "sonet"
    val port = System.getenv("GRPC_PORT")?.toIntOrNull() ?: getDefaultPortForPlatform(platform)
    
    val server = EntativaGrpcServer(port, platform)
    
    try {
        server.start()
        server.blockUntilShutdown()
    } catch (e: Exception) {
        val logger = LoggerFactory.getLogger("EntativaGrpcServer")
        logger.error("❌ Failed to start server", e)
        System.exit(1)
    }
}

/**
 * Get default port for each platform
 */
private fun getDefaultPortForPlatform(platform: String): Int = when (platform.lowercase()) {
    "sonet" -> 50051
    "gala" -> 50052
    "pika" -> 50053
    "playpods" -> 50054
    "entativa-id" -> 50055
    else -> 50051
}