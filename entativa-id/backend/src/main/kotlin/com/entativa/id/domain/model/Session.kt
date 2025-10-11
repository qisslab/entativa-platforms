package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Session Domain Models for Entativa ID
 * Comprehensive session management with enterprise security and tracking
 * 
 * @author Neo Qiss
 * @status Production-ready session system with cross-platform support
 */

/**
 * Session - Main session entity for user authentication tracking
 */
@Serializable
data class Session(
    val id: String,
    val userId: String,
    val sessionToken: String? = null, // Only for display/logs, actual token is hashed
    val sessionHash: String,
    val deviceId: String? = null,
    val deviceFingerprint: String? = null,
    val isActive: Boolean = true,
    val isAuthenticated: Boolean = true,
    val isPersistent: Boolean = false,
    val isTrusted: Boolean = false,
    val sessionType: String = "WEB", // WEB, MOBILE, API, DESKTOP, TV
    val platform: String? = null, // SONET, GALA, PIKA, PLAYPODS, ENTATIVA_ID
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val screenResolution: String? = null,
    val lastActivityAt: Instant? = null,
    val expiresAt: Instant? = null,
    val extendedAt: Instant? = null,
    val terminatedAt: Instant? = null,
    val terminatedBy: String? = null,
    val terminationReason: String? = null,
    val loginMethod: String? = null, // PASSWORD, MFA, SSO, BIOMETRIC, API_KEY
    val mfaVerified: Boolean = false,
    val mfaMethod: String? = null,
    val riskScore: Double = 0.0,
    val riskFlags: String? = null, // JSON array of risk indicators
    val securityLevel: String = "STANDARD", // STANDARD, HIGH, CRITICAL
    val securityFlags: String? = null, // JSON array of security flags
    val activityCount: Long = 0,
    val requestCount: Long = 0,
    val dataTransferred: Long = 0, // bytes
    val warningCount: Int = 0,
    val failureCount: Int = 0,
    val suspiciousActivityCount: Int = 0,
    val permissions: String? = null, // JSON array of session permissions
    val scope: String? = null, // OAuth scope if applicable
    val clientId: String? = null, // OAuth client ID if applicable
    val sessionData: String? = null, // JSON session data
    val metadata: String? = null, // Additional metadata
    val syncedToPlatforms: String? = null, // JSON array of platforms
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val lastSyncedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * Create session request
 */
@Serializable
data class CreateSessionRequest(
    val userId: String,
    val sessionToken: String? = null,
    val sessionHash: String,
    val deviceId: String? = null,
    val deviceFingerprint: String? = null,
    val isActive: Boolean = true,
    val isAuthenticated: Boolean = true,
    val isPersistent: Boolean = false,
    val isTrusted: Boolean = false,
    val sessionType: String = "WEB",
    val platform: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val screenResolution: String? = null,
    val expiresAt: Instant? = null,
    val loginMethod: String? = null,
    val mfaVerified: Boolean = false,
    val mfaMethod: String? = null,
    val riskScore: Double = 0.0,
    val riskFlags: String? = null,
    val securityLevel: String = "STANDARD",
    val securityFlags: String? = null,
    val permissions: String? = null,
    val scope: String? = null,
    val clientId: String? = null,
    val sessionData: String? = null,
    val metadata: String? = null,
    val createdBy: String
)

/**
 * Session response for API
 */
@Serializable
data class SessionResponse(
    val id: String,
    val deviceId: String? = null,
    val isActive: Boolean,
    val isAuthenticated: Boolean,
    val isPersistent: Boolean,
    val isTrusted: Boolean,
    val sessionType: String,
    val platform: String? = null,
    val location: String? = null,
    val country: String? = null,
    val lastActivityAt: Instant? = null,
    val expiresAt: Instant? = null,
    val loginMethod: String? = null,
    val mfaVerified: Boolean,
    val riskScore: Double,
    val securityLevel: String,
    val activityCount: Long,
    val requestCount: Long,
    val createdAt: Instant
)

/**
 * Session activity tracking
 */
@Serializable
data class SessionActivity(
    val id: String,
    val sessionId: String,
    val userId: String,
    val activityType: ActivityType,
    val action: String,
    val resource: String? = null,
    val method: String? = null, // HTTP method or operation type
    val endpoint: String? = null,
    val statusCode: Int? = null,
    val responseSize: Long? = null, // bytes
    val duration: Long? = null, // milliseconds
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val deviceId: String? = null,
    val platform: String? = null,
    val riskScore: Double = 0.0,
    val isSuspicious: Boolean = false,
    val suspiciousReasons: String? = null, // JSON array
    val metadata: String? = null,
    val createdAt: Instant
)

@Serializable
enum class ActivityType {
    LOGIN,
    LOGOUT,
    PAGE_VIEW,
    API_CALL,
    DATA_ACCESS,
    PROFILE_UPDATE,
    PERMISSION_CHANGE,
    SECURITY_EVENT,
    FILE_UPLOAD,
    FILE_DOWNLOAD,
    SEARCH,
    INTERACTION,
    SYSTEM_ACTION
}

/**
 * Session security event
 */
@Serializable
data class SessionSecurityEvent(
    val id: String,
    val sessionId: String,
    val userId: String,
    val eventType: SecurityEventType,
    val severity: SecurityEventSeverity,
    val description: String,
    val details: String? = null, // JSON details
    val ipAddress: String? = null,
    val location: String? = null,
    val userAgent: String? = null,
    val deviceId: String? = null,
    val automaticAction: String? = null, // Action taken automatically
    val requiresAction: Boolean = false,
    val resolved: Boolean = false,
    val resolvedAt: Instant? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val metadata: String? = null,
    val createdAt: Instant
)

/**
 * Session validation result
 */
@Serializable
data class SessionValidationResult(
    val isValid: Boolean,
    val session: Session? = null,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val securityIssues: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList()
)

/**
 * Session extension request
 */
@Serializable
data class SessionExtensionRequest(
    val sessionId: String,
    val extendByHours: Int,
    val reason: String? = null,
    val requestedBy: String
)

/**
 * Session termination request
 */
@Serializable
data class SessionTerminationRequest(
    val sessionId: String,
    val reason: String,
    val notifyUser: Boolean = true,
    val terminatedBy: String
)

/**
 * Bulk session operation request
 */
@Serializable
data class BulkSessionOperationRequest(
    val operation: SessionOperation,
    val filters: SessionFilters,
    val reason: String,
    val performedBy: String
)

@Serializable
enum class SessionOperation {
    TERMINATE,
    EXTEND,
    MARK_SUSPICIOUS,
    UPDATE_RISK_SCORE,
    SYNC_TO_PLATFORMS,
    CLEANUP_EXPIRED
}

/**
 * Session search filters
 */
@Serializable
data class SessionFilters(
    val userId: String? = null,
    val deviceId: String? = null,
    val sessionType: String? = null,
    val platform: String? = null,
    val isActive: Boolean? = null,
    val isAuthenticated: Boolean? = null,
    val isTrusted: Boolean? = null,
    val country: String? = null,
    val minRiskScore: Double? = null,
    val maxRiskScore: Double? = null,
    val securityLevel: String? = null,
    val createdAfter: Instant? = null,
    val createdBefore: Instant? = null,
    val lastActivityAfter: Instant? = null,
    val lastActivityBefore: Instant? = null
)

/**
 * Session statistics
 */
@Serializable
data class SessionStatistics(
    val totalSessions: Long,
    val activeSessions: Long,
    val authenticatedSessions: Long,
    val trustedSessions: Long,
    val suspiciousSessions: Long,
    val sessionsByType: Map<String, Long>,
    val sessionsByPlatform: Map<String, Long>,
    val sessionsByCountry: Map<String, Long>,
    val averageSessionDuration: Double, // minutes
    val totalActivityCount: Long,
    val generatedAt: Instant
)

/**
 * Session analytics
 */
@Serializable
data class SessionAnalytics(
    val userId: String? = null,
    val period: AnalyticsPeriod,
    val totalSessions: Long,
    val uniqueDevices: Long,
    val uniqueLocations: Long,
    val averageSessionDuration: Double, // minutes
    val mostActiveHours: List<Int>,
    val topCountries: Map<String, Long>,
    val topPlatforms: Map<String, Long>,
    val topDeviceTypes: Map<String, Long>,
    val securityEvents: Long,
    val suspiciousActivities: Long,
    val riskTrends: List<RiskDataPoint>,
    val generatedAt: Instant
)

@Serializable
data class RiskDataPoint(
    val timestamp: Instant,
    val averageRiskScore: Double,
    val sessionCount: Long
)

/**
 * Session sync configuration
 */
@Serializable
data class SessionSyncConfig(
    val userId: String,
    val enableSync: Boolean = true,
    val syncPlatforms: List<String> = emptyList(),
    val syncSessionData: Boolean = true,
    val syncActivityData: Boolean = false,
    val syncSecurityEvents: Boolean = true,
    val syncFrequency: SyncFrequency = SyncFrequency.REAL_TIME,
    val lastSyncAt: Instant? = null,
    val syncErrors: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class SyncFrequency {
    REAL_TIME,
    EVERY_MINUTE,
    EVERY_5_MINUTES,
    EVERY_15_MINUTES,
    HOURLY,
    DISABLED
}

/**
 * Session policy configuration
 */
@Serializable
data class SessionPolicy(
    val id: String,
    val name: String,
    val description: String,
    val maxConcurrentSessions: Int = 5,
    val maxSessionDuration: Int = 480, // minutes (8 hours)
    val idleTimeout: Int = 30, // minutes
    val requireMFAForNewDevice: Boolean = true,
    val requireMFAForNewLocation: Boolean = true,
    val allowedCountries: List<String> = emptyList(), // Empty = all allowed
    val blockedCountries: List<String> = emptyList(),
    val maxRiskScore: Double = 75.0,
    val forceLogoutOnSuspicious: Boolean = true,
    val enableActivityTracking: Boolean = true,
    val enableLocationTracking: Boolean = true,
    val enableDeviceTracking: Boolean = true,
    val allowSessionExtension: Boolean = true,
    val maxExtensions: Int = 3,
    val notifyOnNewSession: Boolean = true,
    val notifyOnSuspicious: Boolean = true,
    val settings: String? = null, // JSON policy settings
    val isActive: Boolean = true,
    val applicableRoles: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null
)

/**
 * Session constants and enums
 */
object SessionTypes {
    const val WEB = "WEB"
    const val MOBILE = "MOBILE"
    const val API = "API"
    const val DESKTOP = "DESKTOP"
    const val TV = "TV"
    const val WATCH = "WATCH"
    const val IOT = "IOT"
    const val BOT = "BOT"
}

object LoginMethods {
    const val PASSWORD = "PASSWORD"
    const val MFA = "MFA"
    const val SSO = "SSO"
    const val BIOMETRIC = "BIOMETRIC"
    const val API_KEY = "API_KEY"
    const val OAUTH = "OAUTH"
    const val SOCIAL = "SOCIAL"
    const val MAGIC_LINK = "MAGIC_LINK"
}

object SessionSecurityLevels {
    const val STANDARD = "STANDARD"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}

object TerminationReasons {
    const val USER_LOGOUT = "USER_LOGOUT"
    const val TIMEOUT = "TIMEOUT"
    const val SECURITY_VIOLATION = "SECURITY_VIOLATION"
    const val ADMIN_ACTION = "ADMIN_ACTION"
    const val POLICY_VIOLATION = "POLICY_VIOLATION"
    const val SYSTEM_MAINTENANCE = "SYSTEM_MAINTENANCE"
    const val CONCURRENT_LIMIT = "CONCURRENT_LIMIT"
    const val SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY"
}
