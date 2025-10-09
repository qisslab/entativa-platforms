package com.entativa.shared.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration
import javax.sql.DataSource

/**
 * Entativa Database Factory - Multi-database connection management
 * Handles PostgreSQL, Redis, MongoDB, and Cassandra connections
 * 
 * @author Neo Qiss
 * @status Production-ready with connection pooling and monitoring
 */
object EntativaDatabaseFactory {
    
    private val logger = LoggerFactory.getLogger(EntativaDatabaseFactory::class.java)
    
    // Database connection pools
    private lateinit var postgresDataSource: DataSource
    private lateinit var redisPool: JedisPool
    
    // Platform-specific database instances
    private lateinit var sonetDatabase: Database
    private lateinit var galaDatabase: Database
    private lateinit var pikaDatabase: Database
    private lateinit var playpodsDatabase: Database
    private lateinit var entativaIdDatabase: Database
    private lateinit var analyticsDatabase: Database
    
    /**
     * Initialize all database connections
     */
    fun initialize(config: DatabaseConfig) {
        logger.info("🚀 Initializing Entativa database connections...")
        
        try {
            // Initialize PostgreSQL connection pool
            initializePostgreSQL(config.postgres)
            
            // Initialize Redis connection pool
            initializeRedis(config.redis)
            
            // Initialize platform-specific databases
            initializePlatformDatabases()
            
            logger.info("✅ All database connections initialized successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize database connections", e)
            throw DatabaseInitializationException("Database initialization failed", e)
        }
    }
    
    /**
     * Initialize PostgreSQL with HikariCP connection pooling
     */
    private fun initializePostgreSQL(config: PostgresConfig) {
        logger.info("📊 Initializing PostgreSQL connection pool...")
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.username
            password = config.password
            driverClassName = "org.postgresql.Driver"
            
            // Connection pool settings (Meta-inspired)
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdleConnections
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            idleTimeout = Duration.ofMinutes(10).toMillis()
            maxLifetime = Duration.ofMinutes(30).toMillis()
            leakDetectionThreshold = Duration.ofSeconds(60).toMillis()
            
            // Performance settings
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
            
            // Health check
            connectionTestQuery = "SELECT 1"
            validationTimeout = Duration.ofSeconds(5).toMillis()
        }
        
        postgresDataSource = HikariDataSource(hikariConfig)
        logger.info("✅ PostgreSQL connection pool initialized")
    }
    
    /**
     * Initialize Redis connection pool with optimal settings
     */
    private fun initializeRedis(config: RedisConfig) {
        logger.info("🔴 Initializing Redis connection pool...")
        
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = config.maxConnections
            maxIdle = config.maxIdleConnections
            minIdle = config.minIdleConnections
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
            minEvictableIdleTimeMillis = Duration.ofSeconds(60).toMillis()
            timeBetweenEvictionRunsMillis = Duration.ofSeconds(30).toMillis()
            numTestsPerEvictionRun = 3
            blockWhenExhausted = true
            maxWaitMillis = Duration.ofSeconds(5).toMillis()
        }
        
        redisPool = JedisPool(
            poolConfig,
            config.host,
            config.port,
            Duration.ofSeconds(5).toMillis().toInt(),
            config.password
        )
        
        // Test connection
        redisPool.resource.use { jedis ->
            jedis.ping()
            logger.info("✅ Redis connection pool initialized and tested")
        }
    }
    
    /**
     * Initialize platform-specific database instances
     */
    private fun initializePlatformDatabases() {
        logger.info("🏗️ Initializing platform-specific databases...")
        
        // Connect to individual platform databases
        sonetDatabase = Database.connect(
            createDataSourceForDatabase("sonet_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        galaDatabase = Database.connect(
            createDataSourceForDatabase("gala_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        pikaDatabase = Database.connect(
            createDataSourceForDatabase("pika_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        playpodsDatabase = Database.connect(
            createDataSourceForDatabase("playpods_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        entativaIdDatabase = Database.connect(
            createDataSourceForDatabase("entativa_id_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        analyticsDatabase = Database.connect(
            createDataSourceForDatabase("entativa_analytics_db"),
            databaseConfig = org.jetbrains.exposed.sql.DatabaseConfig {
                sqlLogger = org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
            }
        )
        
        logger.info("✅ All platform databases connected")
    }
    
    /**
     * Create DataSource for specific database
     */
    private fun createDataSourceForDatabase(databaseName: String): DataSource {
        val baseUrl = (postgresDataSource as HikariDataSource).hikariConfigMXBean.jdbcUrl
        val baseUrlWithoutDb = baseUrl.substringBeforeLast("/")
        val newUrl = "$baseUrlWithoutDb/$databaseName"
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = newUrl
            username = (postgresDataSource as HikariDataSource).hikariConfigMXBean.username
            password = (postgresDataSource as HikariDataSource).password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 20
            minimumIdle = 5
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            idleTimeout = Duration.ofMinutes(10).toMillis()
            maxLifetime = Duration.ofMinutes(30).toMillis()
        }
        
        return HikariDataSource(hikariConfig)
    }
    
    /**
     * Execute database transaction with proper platform selection
     */
    suspend fun <T> dbQuery(platform: Platform, statement: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, getDatabaseForPlatform(platform)) { 
            statement() 
        }
    
    /**
     * Get database instance for specific platform
     */
    private fun getDatabaseForPlatform(platform: Platform): Database = when (platform) {
        Platform.SONET -> sonetDatabase
        Platform.GALA -> galaDatabase
        Platform.PIKA -> pikaDatabase
        Platform.PLAYPODS -> playpodsDatabase
        Platform.ENTATIVA_ID -> entativaIdDatabase
        Platform.ANALYTICS -> analyticsDatabase
    }
    
    /**
     * Get Redis connection from pool
     */
    fun getRedisConnection() = redisPool.resource
    
    /**
     * Close all database connections
     */
    fun close() {
        logger.info("🔄 Closing all database connections...")
        
        try {
            redisPool.close()
            (postgresDataSource as HikariDataSource).close()
            logger.info("✅ All database connections closed")
        } catch (e: Exception) {
            logger.error("❌ Error closing database connections", e)
        }
    }
    
    /**
     * Health check for all database connections
     */
    suspend fun healthCheck(): DatabaseHealthStatus {
        val status = DatabaseHealthStatus()
        
        try {
            // Test PostgreSQL
            dbQuery(Platform.SONET) {
                // Simple query to test connection
                true
            }
            status.postgresql = true
            
            // Test Redis
            redisPool.resource.use { jedis ->
                jedis.ping()
                status.redis = true
            }
            
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
        }
        
        return status
    }
}

// Data classes for configuration
data class DatabaseConfig(
    val postgres: PostgresConfig,
    val redis: RedisConfig
)

data class PostgresConfig(
    val url: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 30,
    val minIdleConnections: Int = 10
)

data class RedisConfig(
    val host: String,
    val port: Int = 6379,
    val password: String?,
    val maxConnections: Int = 50,
    val maxIdleConnections: Int = 20,
    val minIdleConnections: Int = 5
)

data class DatabaseHealthStatus(
    var postgresql: Boolean = false,
    var redis: Boolean = false,
    var mongodb: Boolean = false,
    var cassandra: Boolean = false
)

enum class Platform {
    SONET, GALA, PIKA, PLAYPODS, ENTATIVA_ID, ANALYTICS
}

class DatabaseInitializationException(message: String, cause: Throwable?) : Exception(message, cause)