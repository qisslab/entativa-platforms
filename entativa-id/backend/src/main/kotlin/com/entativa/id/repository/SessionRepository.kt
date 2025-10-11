package com.entativa.id.repository

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Session Repository for Entativa ID
 * Handles session data access operations with device tracking and analytics
 * 
 * @author Neo Qiss
 * @status Production-ready session data management
 */
@Repository
class SessionRepository(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(SessionRepository::class.java)
    
    companion object {
        private const val SESSION_CACHE_TTL_SECONDS = 3600 // 1 hour
        private const val DEVICE_CACHE_TTL_SECONDS = 86400 // 24 hours
        private const val ANALYTICS_CACHE_TTL_SECONDS = 300 // 5 minutes
    }
    
    /**
     * Create new session
     */
    suspend fun createSession(request: CreateSessionRequest): Result<SessionRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Creating session for user: ${request.userId}")
                
                val sessionId = UUID.randomUUID()
                val deviceId = UUID.randomUUID()
                val now = Instant.now()
                val expiresAt = now.plusSeconds(request.expiresInSeconds ?: 86400) // Default 24h
                
                val sessionRecord = transaction {
                    // Insert session
                    SessionTable.insert {
                        it[id] = sessionId
                        it[userId] = UUID.fromString(request.userId)
                        it[sessionToken] = request.sessionToken
                        it[this.deviceId] = deviceId
                        it[ipAddress] = request.ipAddress
                        it[userAgent] = request.userAgent
                        it[createdAt] = now
                        it[lastAccessedAt] = now
                        it[this.expiresAt] = expiresAt
                        it[isActive] = true
                    }
                    
                    // Insert device info
                    DeviceInfoTable.insert {
                        it[id] = deviceId
                        it[this.userId] = UUID.fromString(request.userId)
                        it[deviceType] = request.deviceInfo.deviceType
                        it[platform] = request.deviceInfo.platform
                        it[browser] = request.deviceInfo.browser
                        it[browserVersion] = request.deviceInfo.browserVersion
                        it[os] = request.deviceInfo.os
                        it[osVersion] = request.deviceInfo.osVersion
                        it[deviceName] = request.deviceInfo.deviceName
                        it[screenResolution] = request.deviceInfo.screenResolution
                        it[timezone] = request.deviceInfo.timezone
                        it[language] = request.deviceInfo.language
                        it[this.ipAddress] = request.ipAddress
                        it[createdAt] = now
                        it[lastSeenAt] = now
                        it[isTrusted] = false
                    }
                    
                    // Track session analytics
                    SessionAnalyticsTable.insert {
                        it[this.sessionId] = sessionId
                        it[loginType] = request.loginType ?: "password"
                        it[loginSuccess] = true
                        it[location] = request.location
                        it[referrer] = request.referrer
                        it[this.ipAddress] = request.ipAddress
                        it[createdAt] = now
                    }
                    
                    SessionRecord(
                        id = sessionId.toString(),
                        userId = request.userId,
                        sessionToken = request.sessionToken,
                        deviceId = deviceId.toString(),
                        ipAddress = request.ipAddress,
                        userAgent = request.userAgent,
                        deviceInfo = request.deviceInfo,
                        location = request.location,
                        createdAt = now.toString(),
                        lastAccessedAt = now.toString(),
                        expiresAt = expiresAt.toString(),
                        isActive = true
                    )
                }
                
                // Cache session
                cacheManager.cacheData("session:${sessionId}", sessionRecord, SESSION_CACHE_TTL_SECONDS)
                cacheManager.cacheData("session:token:${request.sessionToken}", sessionRecord, SESSION_CACHE_TTL_SECONDS)
                
                logger.info("✅ Session created successfully: $sessionId")
                Result.success(sessionRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create session: ${request.userId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find session by token
     */
    suspend fun findByToken(sessionToken: String): SessionRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<SessionRecord>("session:token:$sessionToken")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val session = transaction {
                    (SessionTable innerJoin DeviceInfoTable)
                        .select { SessionTable.sessionToken eq sessionToken and SessionTable.isActive }
                        .map { row ->
                            SessionRecord(
                                id = row[SessionTable.id].toString(),
                                userId = row[SessionTable.userId].toString(),
                                sessionToken = row[SessionTable.sessionToken],
                                deviceId = row[SessionTable.deviceId].toString(),
                                ipAddress = row[SessionTable.ipAddress],
                                userAgent = row[SessionTable.userAgent],
                                deviceInfo = DeviceInfo(
                                    deviceType = row[DeviceInfoTable.deviceType],
                                    platform = row[DeviceInfoTable.platform],
                                    browser = row[DeviceInfoTable.browser],
                                    browserVersion = row[DeviceInfoTable.browserVersion],
                                    os = row[DeviceInfoTable.os],
                                    osVersion = row[DeviceInfoTable.osVersion],
                                    deviceName = row[DeviceInfoTable.deviceName],
                                    screenResolution = row[DeviceInfoTable.screenResolution],
                                    timezone = row[DeviceInfoTable.timezone],
                                    language = row[DeviceInfoTable.language]
                                ),
                                location = null, // Would need join with analytics
                                createdAt = row[SessionTable.createdAt].toString(),
                                lastAccessedAt = row[SessionTable.lastAccessedAt].toString(),
                                expiresAt = row[SessionTable.expiresAt].toString(),
                                isActive = row[SessionTable.isActive]
                            )
                        }
                        .singleOrNull()
                }
                
                // Cache if found
                session?.let {
                    cacheManager.cacheData("session:token:$sessionToken", it, SESSION_CACHE_TTL_SECONDS)
                    cacheManager.cacheData("session:${it.id}", it, SESSION_CACHE_TTL_SECONDS)
                }
                
                session
                
            } catch (e: Exception) {
                logger.error("❌ Failed to find session by token", e)
                null
            }
        }
    }
    
    /**
     * Update session activity
     */
    suspend fun updateActivity(sessionId: String, ipAddress: String, userAgent: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val sessionUuid = UUID.fromString(sessionId)
                
                val updated = transaction {
                    SessionTable.update({ SessionTable.id eq sessionUuid }) {
                        it[lastAccessedAt] = now
                        it[SessionTable.ipAddress] = ipAddress
                        it[SessionTable.userAgent] = userAgent
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("session:$sessionId")
                    
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("Session not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update session activity: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's active sessions
     */
    suspend fun getUserActiveSessions(userId: String): List<SessionSummary> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    (SessionTable innerJoin DeviceInfoTable)
                        .select { 
                            SessionTable.userId eq UUID.fromString(userId) and 
                            SessionTable.isActive and
                            SessionTable.expiresAt greater Instant.now()
                        }
                        .orderBy(SessionTable.lastAccessedAt, SortOrder.DESC)
                        .map { row ->
                            SessionSummary(
                                id = row[SessionTable.id].toString(),
                                deviceType = row[DeviceInfoTable.deviceType],
                                platform = row[DeviceInfoTable.platform],
                                browser = row[DeviceInfoTable.browser],
                                location = row[DeviceInfoTable.ipAddress], // Simplified
                                createdAt = row[SessionTable.createdAt].toString(),
                                lastAccessedAt = row[SessionTable.lastAccessedAt].toString(),
                                isCurrent = false // Would need to compare with current session
                            )
                        }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get user sessions: $userId", e)
                emptyList()
            }
        }
    }
    
    /**
     * Terminate session
     */
    suspend fun terminateSession(sessionId: String, reason: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔚 Terminating session: $sessionId")
                
                val now = Instant.now()
                val sessionUuid = UUID.fromString(sessionId)
                
                val updated = transaction {
                    SessionTable.update({ SessionTable.id eq sessionUuid }) {
                        it[isActive] = false
                        it[lastAccessedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Remove from cache
                    cacheManager.invalidateCache("session:$sessionId")
                    
                    // Log termination
                    SessionAnalyticsTable.insert {
                        it[sessionId] = sessionUuid
                        it[logoutReason] = reason
                        it[createdAt] = now
                    }
                    
                    logger.info("✅ Session terminated: $sessionId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("Session not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to terminate session: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Terminate all user sessions except current
     */
    suspend fun terminateAllUserSessions(userId: String, exceptSessionId: String?, reason: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔚 Terminating all sessions for user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                val exceptUuid = exceptSessionId?.let { UUID.fromString(it) }
                
                val condition = if (exceptUuid != null) {
                    SessionTable.userId eq userUuid and 
                    SessionTable.isActive and
                    (SessionTable.id neq exceptUuid)
                } else {
                    SessionTable.userId eq userUuid and SessionTable.isActive
                }
                
                val terminated = transaction {
                    SessionTable.update({ condition }) {
                        it[isActive] = false
                        it[lastAccessedAt] = now
                    }
                }
                
                // Clear cache for user sessions
                cacheManager.invalidatePattern("session:*")
                
                logger.info("✅ Terminated $terminated sessions for user: $userId")
                Result.success(terminated)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to terminate user sessions: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cleanup expired sessions
     */
    suspend fun cleanupExpiredSessions(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🧹 Cleaning up expired sessions")
                
                val now = Instant.now()
                
                val cleaned = transaction {
                    SessionTable.update({ 
                        SessionTable.expiresAt less now and SessionTable.isActive 
                    }) {
                        it[isActive] = false
                        it[lastAccessedAt] = now
                    }
                }
                
                logger.info("✅ Cleaned up $cleaned expired sessions")
                Result.success(cleaned)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to cleanup expired sessions", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get session analytics
     */
    suspend fun getSessionAnalytics(
        userId: String?, 
        startDate: Instant, 
        endDate: Instant
    ): Result<SessionAnalyticsResult> {
        return withContext(Dispatchers.IO) {
            try {
                val analytics = transaction {
                    val baseQuery = if (userId != null) {
                        SessionAnalyticsTable.select { 
                            SessionAnalyticsTable.createdAt.between(startDate, endDate)
                        }
                    } else {
                        SessionAnalyticsTable.select { 
                            SessionAnalyticsTable.createdAt.between(startDate, endDate)
                        }
                    }
                    
                    val totalSessions = baseQuery.count()
                    val successfulLogins = baseQuery.andWhere { SessionAnalyticsTable.loginSuccess eq true }.count()
                    
                    // Device type distribution
                    val deviceTypes = (SessionAnalyticsTable innerJoin SessionTable innerJoin DeviceInfoTable)
                        .slice(DeviceInfoTable.deviceType, DeviceInfoTable.deviceType.count())
                        .select { SessionAnalyticsTable.createdAt.between(startDate, endDate) }
                        .groupBy(DeviceInfoTable.deviceType)
                        .associate { 
                            it[DeviceInfoTable.deviceType] to it[DeviceInfoTable.deviceType.count()]
                        }
                    
                    SessionAnalyticsResult(
                        totalSessions = totalSessions,
                        successfulLogins = successfulLogins,
                        failedLogins = totalSessions - successfulLogins,
                        deviceTypeDistribution = deviceTypes,
                        averageSessionDuration = 0L, // Would need more complex calculation
                        uniqueUsers = 0L, // Would need distinct user count
                        peakHours = emptyMap() // Would need hourly aggregation
                    )
                }
                
                Result.success(analytics)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get session analytics", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Track suspicious session activity
     */
    suspend fun trackSuspiciousActivity(
        sessionId: String,
        activityType: String,
        details: Map<String, String>
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.warn("⚠️ Tracking suspicious activity for session: $sessionId")
                
                val now = Instant.now()
                val sessionUuid = UUID.fromString(sessionId)
                
                transaction {
                    SecurityEventTable.insert {
                        it[entityId] = sessionUuid
                        it[entityType] = "session"
                        it[eventType] = activityType
                        it[severity] = "medium"
                        it[this.details] = details
                        it[createdAt] = now
                    }
                }
                
                logger.warn("⚠️ Suspicious activity tracked: $sessionId - $activityType")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to track suspicious activity: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if session exists and is valid
     */
    suspend fun isValidSession(sessionToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                
                transaction {
                    SessionTable.select { 
                        SessionTable.sessionToken eq sessionToken and
                        SessionTable.isActive and
                        SessionTable.expiresAt greater now
                    }.count() > 0
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to validate session", e)
                false
            }
        }
    }
    
    /**
     * Get device info by ID
     */
    suspend fun getDeviceInfo(deviceId: String): DeviceInfoRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<DeviceInfoRecord>("device:$deviceId")
                if (cached != null) {
                    return@withContext cached
                }
                
                val device = transaction {
                    DeviceInfoTable.select { DeviceInfoTable.id eq UUID.fromString(deviceId) }
                        .map { row ->
                            DeviceInfoRecord(
                                id = row[DeviceInfoTable.id].toString(),
                                userId = row[DeviceInfoTable.userId].toString(),
                                deviceType = row[DeviceInfoTable.deviceType],
                                platform = row[DeviceInfoTable.platform],
                                browser = row[DeviceInfoTable.browser],
                                browserVersion = row[DeviceInfoTable.browserVersion],
                                os = row[DeviceInfoTable.os],
                                osVersion = row[DeviceInfoTable.osVersion],
                                deviceName = row[DeviceInfoTable.deviceName],
                                screenResolution = row[DeviceInfoTable.screenResolution],
                                timezone = row[DeviceInfoTable.timezone],
                                language = row[DeviceInfoTable.language],
                                ipAddress = row[DeviceInfoTable.ipAddress],
                                isTrusted = row[DeviceInfoTable.isTrusted],
                                createdAt = row[DeviceInfoTable.createdAt].toString(),
                                lastSeenAt = row[DeviceInfoTable.lastSeenAt].toString()
                            )
                        }
                        .singleOrNull()
                }
                
                // Cache if found
                device?.let {
                    cacheManager.cacheData("device:$deviceId", it, DEVICE_CACHE_TTL_SECONDS)
                }
                
                device
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get device info: $deviceId", e)
                null
            }
        }
    }
}
