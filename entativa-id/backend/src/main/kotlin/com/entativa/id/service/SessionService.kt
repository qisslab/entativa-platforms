package com.entativa.id.service

import com.entativa.id.config.*
import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Session Management Service for Entativa ID
 * Handles user session lifecycle, device tracking, and security monitoring
 * 
 * @author Neo Qiss
 * @status Production-ready session management
 */
class SessionService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(SessionService::class.java)
    
    companion object {
        private const val SESSION_CACHE_TTL_SECONDS = 3600 // 1 hour
        private const val MAX_SESSIONS_PER_USER = 10
        private const val SESSION_CLEANUP_INTERVAL_HOURS = 24
        private const val SUSPICIOUS_LOGIN_THRESHOLD = 5
    }
    
    /**
     * Create new session for user
     */
    suspend fun createSession(request: CreateSessionRequest): Result<Session> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Creating session for user: ${request.userId}")
                
                // Check session limits
                val activeSessionCount = getActiveSessionCount(request.userId)
                if (activeSessionCount >= MAX_SESSIONS_PER_USER) {
                    // Remove oldest session
                    removeOldestSession(request.userId)
                }
                
                val sessionId = UUID.randomUUID().toString()
                val now = Instant.now()
                val expiresAt = now.plusSeconds(request.expiresInSeconds ?: 86400) // Default 24 hours
                
                // Determine device type and location
                val deviceInfo = parseDeviceInfo(request.userAgent)
                val location = determineLocation(request.ipAddress)
                
                // Create session record
                val session = Session(
                    id = sessionId,
                    userId = request.userId,
                    deviceId = request.deviceId,
                    clientId = request.clientId,
                    ipAddress = request.ipAddress,
                    userAgent = request.userAgent,
                    deviceType = deviceInfo.deviceType,
                    deviceName = deviceInfo.deviceName,
                    browserName = deviceInfo.browserName,
                    browserVersion = deviceInfo.browserVersion,
                    osName = deviceInfo.osName,
                    osVersion = deviceInfo.osVersion,
                    location = location,
                    isMobile = deviceInfo.isMobile,
                    isSecure = request.isSecure ?: false,
                    createdAt = now.toString(),
                    lastActivity = now.toString(),
                    expiresAt = expiresAt.toString(),
                    isActive = true
                )
                
                // Store in database
                transaction {
                    SessionsTable.insert {
                        it[id] = UUID.fromString(sessionId)
                        it[identityId] = UUID.fromString(request.userId)
                        it[deviceId] = request.deviceId
                        it[this.clientId] = request.clientId
                        it[this.ipAddress] = request.ipAddress
                        it[this.userAgent] = request.userAgent
                        it[deviceType] = deviceInfo.deviceType
                        it[deviceName] = deviceInfo.deviceName
                        it[browserName] = deviceInfo.browserName
                        it[browserVersion] = deviceInfo.browserVersion
                        it[osName] = deviceInfo.osName
                        it[osVersion] = deviceInfo.osVersion
                        it[this.location] = location
                        it[isMobile] = deviceInfo.isMobile
                        it[isSecure] = request.isSecure ?: false
                        it[this.createdAt] = now
                        it[lastActivity] = now
                        it[this.expiresAt] = expiresAt
                    }
                }
                
                // Cache session
                cacheManager.cacheData("session:$sessionId", session, SESSION_CACHE_TTL_SECONDS)
                
                // Log session creation
                auditSessionAction(request.userId, "session_created", mapOf(
                    "session_id" to sessionId,
                    "device_type" to deviceInfo.deviceType,
                    "location" to location,
                    "ip_address" to (request.ipAddress ?: "unknown")
                ))
                
                // Check for suspicious activity
                checkSuspiciousActivity(request.userId, request.ipAddress, location)
                
                logger.info("✅ Session created successfully: $sessionId")
                Result.success(session)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create session for user: ${request.userId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get session by ID
     */
    suspend fun getSession(sessionId: String): Session? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<Session>("session:$sessionId")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val session = transaction {
                    SessionsTable.select { SessionsTable.id eq UUID.fromString(sessionId) }
                        .singleOrNull()
                        ?.let { record ->
                            buildSessionFromRecord(record)
                        }
                }
                
                if (session != null) {
                    cacheManager.cacheData("session:$sessionId", session, SESSION_CACHE_TTL_SECONDS)
                }
                
                session
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get session: $sessionId", e)
                null
            }
        }
    }
    
    /**
     * Update session activity
     */
    suspend fun updateSessionActivity(sessionId: String, ipAddress: String? = null): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                
                // Update database
                val updated = transaction {
                    SessionsTable.update({ SessionsTable.id eq UUID.fromString(sessionId) }) {
                        it[lastActivity] = now
                        if (ipAddress != null) {
                            it[this.ipAddress] = ipAddress
                        }
                    } > 0
                }
                
                if (updated) {
                    // Update cached session
                    val session = getSession(sessionId)
                    if (session != null) {
                        val updatedSession = session.copy(
                            lastActivity = now.toString(),
                            ipAddress = ipAddress ?: session.ipAddress
                        )
                        cacheManager.cacheData("session:$sessionId", updatedSession, SESSION_CACHE_TTL_SECONDS)
                    }
                    
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
     * Terminate session
     */
    suspend fun terminateSession(sessionId: String, reason: String = "user_logout"): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚪 Terminating session: $sessionId")
                
                // Get session info for audit
                val session = getSession(sessionId)
                
                // Mark session as inactive in database
                val terminated = transaction {
                    SessionsTable.update({ SessionsTable.id eq UUID.fromString(sessionId) }) {
                        it[isActive] = false
                        it[revokedAt] = Instant.now()
                        it[revokeReason] = reason
                    } > 0
                }
                
                if (terminated) {
                    // Remove from cache
                    cacheManager.invalidateCache("session:$sessionId")
                    
                    // Log termination
                    if (session != null) {
                        auditSessionAction(session.userId, "session_terminated", mapOf(
                            "session_id" to sessionId,
                            "reason" to reason,
                            "duration_seconds" to (Instant.now().epochSecond - Instant.parse(session.createdAt).epochSecond).toString()
                        ))
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
     * Get all active sessions for user
     */
    suspend fun getUserSessions(userId: String): List<Session> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    SessionsTable.select { 
                        (SessionsTable.identityId eq UUID.fromString(userId)) and
                        (SessionsTable.isActive eq true) and
                        (SessionsTable.expiresAt greater Instant.now())
                    }.map { record ->
                        buildSessionFromRecord(record)
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get user sessions: $userId", e)
                emptyList()
            }
        }
    }
    
    /**
     * Terminate all sessions for user
     */
    suspend fun terminateAllUserSessions(userId: String, reason: String = "security_action"): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚫 Terminating all sessions for user: $userId")
                
                // Get session IDs for cache invalidation
                val sessionIds = transaction {
                    SessionsTable.slice(SessionsTable.id)
                        .select { 
                            (SessionsTable.identityId eq UUID.fromString(userId)) and
                            (SessionsTable.isActive eq true)
                        }.map { it[SessionsTable.id].value.toString() }
                }
                
                // Terminate all sessions
                val terminatedCount = transaction {
                    SessionsTable.update({ 
                        (SessionsTable.identityId eq UUID.fromString(userId)) and
                        (SessionsTable.isActive eq true)
                    }) {
                        it[isActive] = false
                        it[revokedAt] = Instant.now()
                        it[revokeReason] = reason
                    }
                }
                
                // Invalidate cached sessions
                sessionIds.forEach { sessionId ->
                    cacheManager.invalidateCache("session:$sessionId")
                }
                
                // Log bulk termination
                auditSessionAction(userId, "all_sessions_terminated", mapOf(
                    "reason" to reason,
                    "terminated_count" to terminatedCount.toString()
                ))
                
                logger.info("✅ Terminated $terminatedCount sessions for user: $userId")
                Result.success(terminatedCount)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to terminate all user sessions: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Validate session and check expiry
     */
    suspend fun validateSession(sessionId: String): Result<Session> {
        return withContext(Dispatchers.IO) {
            try {
                val session = getSession(sessionId)
                    ?: return@withContext Result.failure(
                        SecurityException("Session not found")
                    )
                
                // Check if session is active
                if (!session.isActive) {
                    return@withContext Result.failure(
                        SecurityException("Session is inactive")
                    )
                }
                
                // Check expiration
                val expiresAt = Instant.parse(session.expiresAt)
                if (expiresAt.isBefore(Instant.now())) {
                    // Mark as expired
                    terminateSession(sessionId, "expired")
                    return@withContext Result.failure(
                        SecurityException("Session expired")
                    )
                }
                
                Result.success(session)
                
            } catch (e: Exception) {
                logger.error("❌ Session validation failed: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get session analytics for user
     */
    suspend fun getSessionAnalytics(userId: String, days: Int = 30): SessionAnalytics {
        return withContext(Dispatchers.IO) {
            try {
                val since = Instant.now().minusSeconds(days * 24 * 3600L)
                
                transaction {
                    val sessions = SessionsTable.select {
                        (SessionsTable.identityId eq UUID.fromString(userId)) and
                        (SessionsTable.createdAt greater since)
                    }.toList()
                    
                    val totalSessions = sessions.size
                    val activeSessions = sessions.count { 
                        it[SessionsTable.isActive] && 
                        it[SessionsTable.expiresAt].isAfter(Instant.now()) 
                    }
                    
                    val deviceTypes = sessions.groupBy { it[SessionsTable.deviceType] }
                        .mapValues { it.value.size }
                    
                    val locations = sessions.mapNotNull { it[SessionsTable.location] }
                        .groupBy { it }
                        .mapValues { it.value.size }
                    
                    val avgSessionDuration = sessions.mapNotNull { record ->
                        val start = record[SessionsTable.createdAt]
                        val end = record[SessionsTable.revokedAt] ?: record[SessionsTable.lastActivity]
                        if (end != null) {
                            end.epochSecond - start.epochSecond
                        } else null
                    }.average().takeIf { !it.isNaN() }?.toLong() ?: 0L
                    
                    SessionAnalytics(
                        userId = userId,
                        totalSessions = totalSessions,
                        activeSessions = activeSessions,
                        deviceTypes = deviceTypes,
                        locations = locations,
                        averageSessionDurationSeconds = avgSessionDuration,
                        lastSessionAt = sessions.maxByOrNull { it[SessionsTable.createdAt] }
                            ?.get(SessionsTable.createdAt)?.toString()
                    )
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get session analytics: $userId", e)
                SessionAnalytics(
                    userId = userId,
                    totalSessions = 0,
                    activeSessions = 0,
                    deviceTypes = emptyMap(),
                    locations = emptyMap(),
                    averageSessionDurationSeconds = 0,
                    lastSessionAt = null
                )
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
                val expiredSessions = transaction {
                    SessionsTable.slice(SessionsTable.id)
                        .select { 
                            (SessionsTable.expiresAt less now) and
                            (SessionsTable.isActive eq true)
                        }.map { it[SessionsTable.id].value.toString() }
                }
                
                // Mark as inactive
                val cleanedCount = transaction {
                    SessionsTable.update({ 
                        (SessionsTable.expiresAt less now) and
                        (SessionsTable.isActive eq true)
                    }) {
                        it[isActive] = false
                        it[revokedAt] = now
                        it[revokeReason] = "expired"
                    }
                }
                
                // Invalidate cached sessions
                expiredSessions.forEach { sessionId ->
                    cacheManager.invalidateCache("session:$sessionId")
                }
                
                logger.info("✅ Cleaned up $cleanedCount expired sessions")
                Result.success(cleanedCount)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to cleanup expired sessions", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun getActiveSessionCount(userId: String): Int {
        return try {
            transaction {
                SessionsTable.select { 
                    (SessionsTable.identityId eq UUID.fromString(userId)) and
                    (SessionsTable.isActive eq true) and
                    (SessionsTable.expiresAt greater Instant.now())
                }.count().toInt()
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to get active session count: $userId", e)
            0
        }
    }
    
    private suspend fun removeOldestSession(userId: String) {
        try {
            val oldestSession = transaction {
                SessionsTable.slice(SessionsTable.id)
                    .select { 
                        (SessionsTable.identityId eq UUID.fromString(userId)) and
                        (SessionsTable.isActive eq true)
                    }
                    .orderBy(SessionsTable.createdAt, SortOrder.ASC)
                    .limit(1)
                    .singleOrNull()
                    ?.get(SessionsTable.id)?.value?.toString()
            }
            
            if (oldestSession != null) {
                terminateSession(oldestSession, "session_limit_exceeded")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to remove oldest session for user: $userId", e)
        }
    }
    
    private fun parseDeviceInfo(userAgent: String?): DeviceInfo {
        if (userAgent.isNullOrBlank()) {
            return DeviceInfo(
                deviceType = "unknown",
                deviceName = "Unknown Device",
                browserName = "Unknown",
                browserVersion = null,
                osName = "Unknown",
                osVersion = null,
                isMobile = false
            )
        }
        
        // Simple user agent parsing - in production use a proper library
        val isMobile = userAgent.contains("Mobile", ignoreCase = true) || 
                      userAgent.contains("Android", ignoreCase = true) ||
                      userAgent.contains("iPhone", ignoreCase = true)
        
        val browserName = when {
            userAgent.contains("Chrome") -> "Chrome"
            userAgent.contains("Safari") -> "Safari"
            userAgent.contains("Firefox") -> "Firefox"
            userAgent.contains("Edge") -> "Edge"
            else -> "Unknown"
        }
        
        val osName = when {
            userAgent.contains("Windows") -> "Windows"
            userAgent.contains("Mac OS") -> "macOS"
            userAgent.contains("Linux") -> "Linux"
            userAgent.contains("Android") -> "Android"
            userAgent.contains("iOS") -> "iOS"
            else -> "Unknown"
        }
        
        return DeviceInfo(
            deviceType = if (isMobile) "mobile" else "desktop",
            deviceName = if (isMobile) "Mobile Device" else "Desktop",
            browserName = browserName,
            browserVersion = null, // TODO: Extract version
            osName = osName,
            osVersion = null, // TODO: Extract version
            isMobile = isMobile
        )
    }
    
    private fun determineLocation(ipAddress: String?): String {
        // TODO: Implement IP geolocation service
        return "Unknown"
    }
    
    private suspend fun checkSuspiciousActivity(userId: String, ipAddress: String?, location: String) {
        try {
            val recentSessions = transaction {
                SessionsTable.select {
                    (SessionsTable.identityId eq UUID.fromString(userId)) and
                    (SessionsTable.createdAt greater Instant.now().minusSeconds(3600)) // Last hour
                }.count()
            }
            
            if (recentSessions >= SUSPICIOUS_LOGIN_THRESHOLD) {
                auditSessionAction(userId, "suspicious_activity_detected", mapOf(
                    "recent_sessions" to recentSessions.toString(),
                    "ip_address" to (ipAddress ?: "unknown"),
                    "location" to location
                ))
                
                logger.warn("⚠️ Suspicious activity detected for user: $userId")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to check suspicious activity", e)
        }
    }
    
    private fun buildSessionFromRecord(record: ResultRow): Session {
        return Session(
            id = record[SessionsTable.id].value.toString(),
            userId = record[SessionsTable.identityId].value.toString(),
            deviceId = record[SessionsTable.deviceId],
            clientId = record[SessionsTable.clientId],
            ipAddress = record[SessionsTable.ipAddress],
            userAgent = record[SessionsTable.userAgent],
            deviceType = record[SessionsTable.deviceType],
            deviceName = record[SessionsTable.deviceName],
            browserName = record[SessionsTable.browserName],
            browserVersion = record[SessionsTable.browserVersion],
            osName = record[SessionsTable.osName],
            osVersion = record[SessionsTable.osVersion],
            location = record[SessionsTable.location],
            isMobile = record[SessionsTable.isMobile],
            isSecure = record[SessionsTable.isSecure],
            createdAt = record[SessionsTable.createdAt].toString(),
            lastActivity = record[SessionsTable.lastActivity]?.toString() ?: record[SessionsTable.createdAt].toString(),
            expiresAt = record[SessionsTable.expiresAt].toString(),
            isActive = record[SessionsTable.isActive]
        )
    }
    
    private suspend fun auditSessionAction(userId: String, action: String, details: Map<String, String>) {
        try {
            transaction {
                IdentityAuditLogTable.insert {
                    it[identityId] = UUID.fromString(userId)
                    it[this.action] = action
                    it[this.details] = details
                    it[createdAt] = Instant.now()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to log session audit event", e)
        }
    }
}
