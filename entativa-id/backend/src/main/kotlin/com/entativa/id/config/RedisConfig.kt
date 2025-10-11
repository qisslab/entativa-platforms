package com.entativa.id.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession
import java.time.Duration

/**
 * Redis Configuration for Entativa ID
 * Handles caching, session storage, and distributed state management
 * 
 * @author Neo Qiss
 * @status Production-ready Redis configuration
 */
@Configuration
@EnableCaching
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
class RedisConfig {
    
    @Value("\${spring.redis.host:localhost}")
    private lateinit var redisHost: String
    
    @Value("\${spring.redis.port:6379}")
    private var redisPort: Int = 6379
    
    @Value("\${spring.redis.password:}")
    private var redisPassword: String = ""
    
    @Value("\${spring.redis.database:0}")
    private var redisDatabase: Int = 0
    
    @Value("\${spring.redis.timeout:2000}")
    private var redisTimeout: Int = 2000
    
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration().apply {
            hostName = redisHost
            port = redisPort
            database = redisDatabase
            if (redisPassword.isNotBlank()) {
                setPassword(redisPassword)
            }
        }
        
        return LettuceConnectionFactory(redisStandaloneConfiguration).apply {
            // Connection validation and timeout settings
            validateConnection = true
            // Set connection timeout
        }
    }
    
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        // Configure serializers
        val stringSerializer = StringRedisSerializer()
        val jsonSerializer = GenericJackson2JsonRedisSerializer(objectMapper())
        
        template.keySerializer = stringSerializer
        template.hashKeySerializer = stringSerializer
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer
        
        template.setDefaultSerializer(jsonSerializer)
        template.setEnableTransactionSupport(true)
        template.afterPropertiesSet()
        
        return template
    }
    
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // Default TTL: 1 hour
            .serializeKeysWith(
                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                    .fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                    .fromSerializer(GenericJackson2JsonRedisSerializer(objectMapper()))
            )
            .disableCachingNullValues()
        
        // Custom cache configurations for different data types
        val cacheConfigurations = mapOf(
            // User data cache - longer TTL
            "users" to defaultConfig.entryTtl(Duration.ofHours(6)),
            
            // Session cache - matches session timeout
            "sessions" to defaultConfig.entryTtl(Duration.ofHours(24)),
            
            // OAuth tokens - short TTL for security
            "oauth_tokens" to defaultConfig.entryTtl(Duration.ofMinutes(15)),
            
            // Authentication cache - medium TTL
            "auth" to defaultConfig.entryTtl(Duration.ofHours(2)),
            
            // Rate limiting cache
            "rate_limits" to defaultConfig.entryTtl(Duration.ofHours(1)),
            
            // Verification tokens - variable TTL (handled in service)
            "verification_tokens" to defaultConfig.entryTtl(Duration.ofHours(24)),
            
            // API keys cache
            "api_keys" to defaultConfig.entryTtl(Duration.ofHours(12)),
            
            // User preferences cache
            "user_preferences" to defaultConfig.entryTtl(Duration.ofDays(1)),
            
            // Device tracking cache
            "device_tracking" to defaultConfig.entryTtl(Duration.ofDays(30)),
            
            // Security events cache
            "security_events" to defaultConfig.entryTtl(Duration.ofHours(4)),
            
            // Application metadata cache
            "app_metadata" to defaultConfig.entryTtl(Duration.ofHours(12)),
            
            // Profile data cache
            "profiles" to defaultConfig.entryTtl(Duration.ofHours(6)),
            
            // Handle validation cache
            "handle_validation" to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            
            // Notification cache
            "notifications" to defaultConfig.entryTtl(Duration.ofMinutes(15))
        )
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build()
    }
    
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            findAndRegisterModules()
        }
    }
    
    /**
     * Redis template for session management
     */
    @Bean
    fun sessionRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        
        val stringSerializer = StringRedisSerializer()
        val jsonSerializer = GenericJackson2JsonRedisSerializer()
        
        template.keySerializer = stringSerializer
        template.hashKeySerializer = stringSerializer
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer
        
        template.afterPropertiesSet()
        return template
    }
    
    /**
     * Redis template for rate limiting
     */
    @Bean
    fun rateLimitRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Long> {
        val template = RedisTemplate<String, Long>()
        template.connectionFactory = connectionFactory
        
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = org.springframework.data.redis.serializer.GenericToStringSerializer(Long::class.java)
        
        template.afterPropertiesSet()
        return template
    }
    
    /**
     * Redis template for distributed locks
     */
    @Bean
    fun lockRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        
        val stringSerializer = StringRedisSerializer()
        template.keySerializer = stringSerializer
        template.valueSerializer = stringSerializer
        
        template.afterPropertiesSet()
        return template
    }
}
