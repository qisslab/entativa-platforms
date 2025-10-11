package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Device Domain Models for Entativa ID
 * Comprehensive device management, tracking, and security
 * 
 * @author Neo Qiss
 * @status Production-ready device management with enterprise security
 */

/**
 * Device - Main device entity for tracking and management
 */
@Serializable
data class Device(
    val id: String,
    val userId: String,
    val deviceFingerprint: String,
    val deviceName: String,
    val deviceType: String, // MOBILE, DESKTOP, TABLET, TV, WATCH, CONSOLE, IOT
    val platform: String, // IOS, ANDROID, WINDOWS, MACOS, LINUX, WEB, OTHER
    val osName: String? = null,
    val osVersion: String? = null,
    val browserName: String? = null,
    val browserVersion: String? = null,
    val isActive: Boolean = true,
    val isTrusted: Boolean = false,
    val isRegistered: Boolean = false,
    val isPrimary: Boolean = false,
    val isBlocked: Boolean = false,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val screenResolution: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val location: String? = null,
    val hardwareInfo: String? = null, // JSON with hardware details
    val networkInfo: String? = null, // JSON with network details
    val securityLevel: String = "STANDARD", // STANDARD, HIGH, CRITICAL
    val trustScore: Double = 50.0, // 0-100
    val riskScore: Double = 0.0, // 0-100
    val fraudFlags: String? = null, // JSON array of fraud indicators
    val securityFlags: String? = null, // JSON array of security flags
    val deviceMetadata: String? = null, // Additional device metadata
    val registrationMethod: String? = null, // MANUAL, AUTO_DETECT, IMPORTED
    val firstSeenAt: Instant? = null,
    val lastSeenAt: Instant? = null,
    val loginCount: Long = 0,
    val lastSecurityCheck: Instant? = null,
    val trustedAt: Instant? = null,
    val trustedBy: String? = null,
    val unTrustedAt: Instant? = null,
    val unTrustedBy: String? = null,
    val trustChangeReason: String? = null,
    val blockedAt: Instant? = null,
    val blockedBy: String? = null,
    val blockReason: String? = null,
    val unblockedAt: Instant? = null,
    val unblockedBy: String? = null,
    val unblockReason: String? = null,
    val deactivatedAt: Instant? = null,
    val deactivationReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * Create device request
 */
@Serializable
data class CreateDeviceRequest(
    val userId: String,
    val deviceFingerprint: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val browserName: String? = null,
    val browserVersion: String? = null,
    val isActive: Boolean = true,
    val isTrusted: Boolean = false,
    val isRegistered: Boolean = false,
    val isPrimary: Boolean = false,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val screenResolution: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val location: String? = null,
    val hardwareInfo: String? = null,
    val networkInfo: String? = null,
    val securityLevel: String = "STANDARD",
    val trustScore: Double = 50.0,
    val riskScore: Double = 0.0,
    val deviceMetadata: String? = null,
    val registrationMethod: String? = null,
    val createdBy: String
)

/**
 * Device response for API
 */
@Serializable
data class DeviceResponse(
    val id: String,
    val deviceName: String,
    val deviceType: String,
    val platform: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val browserName: String? = null,
    val browserVersion: String? = null,
    val isActive: Boolean,
    val isTrusted: Boolean,
    val isRegistered: Boolean,
    val isPrimary: Boolean,
    val isBlocked: Boolean,
    val location: String? = null,
    val securityLevel: String,
    val trustScore: Double,
    val riskScore: Double,
    val lastSeenAt: Instant? = null,
    val loginCount: Long,
    val createdAt: Instant
)

/**
 * Device statistics
 */
@Serializable
data class DeviceStatistics(
    val totalDevices: Long,
    val activeDevices: Long,
    val trustedDevices: Long,
    val blockedDevices: Long,
    val trustRate: Double,
    val blockRate: Double,
    val generatedAt: Instant
)

/**
 * Device fingerprint details
 */
@Serializable
data class DeviceFingerprint(
    val id: String,
    val userId: String? = null,
    val fingerprint: String,
    val components: DeviceFingerprintComponents,
    val confidence: Double, // 0-100
    val uniqueness: Double, // 0-100
    val stability: Double, // 0-100
    val isBot: Boolean = false,
    val botScore: Double = 0.0,
    val riskFactors: List<String> = emptyList(),
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val seenCount: Long = 1,
    val createdAt: Instant
)

/**
 * Device fingerprint components
 */
@Serializable
data class DeviceFingerprintComponents(
    val userAgent: String? = null,
    val screenResolution: String? = null,
    val colorDepth: Int? = null,
    val pixelRatio: Double? = null,
    val timezone: String? = null,
    val language: String? = null,
    val platform: String? = null,
    val cookieEnabled: Boolean? = null,
    val javaEnabled: Boolean? = null,
    val plugins: List<String> = emptyList(),
    val fonts: List<String> = emptyList(),
    val canvas: String? = null,
    val webgl: String? = null,
    val audioContext: String? = null,
    val touchSupport: Boolean? = null,
    val hardwareConcurrency: Int? = null,
    val deviceMemory: Double? = null,
    val connectionType: String? = null,
    val batteryLevel: Double? = null,
    val chargingStatus: Boolean? = null
)

/**
 * Device trust evaluation
 */
@Serializable
data class DeviceTrustEvaluation(
    val deviceId: String,
    val currentTrustScore: Double,
    val newTrustScore: Double,
    val factors: List<TrustFactor>,
    val recommendation: TrustRecommendation,
    val reason: String,
    val evaluatedAt: Instant,
    val evaluatedBy: String
)

/**
 * Trust factor for device evaluation
 */
@Serializable
data class TrustFactor(
    val name: String,
    val weight: Double,
    val value: Double,
    val impact: Double, // positive or negative
    val description: String
)

@Serializable
enum class TrustRecommendation {
    TRUST,
    UNTRUST,
    MONITOR,
    BLOCK,
    REQUIRE_VERIFICATION
}

/**
 * Device security event
 */
@Serializable
data class DeviceSecurityEvent(
    val id: String,
    val deviceId: String,
    val userId: String,
    val eventType: SecurityEventType,
    val severity: SecurityEventSeverity,
    val description: String,
    val details: String? = null, // JSON details
    val ipAddress: String? = null,
    val location: String? = null,
    val userAgent: String? = null,
    val resolved: Boolean = false,
    val resolvedAt: Instant? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Instant,
    val createdBy: String = "SYSTEM"
)

@Serializable
enum class SecurityEventType {
    SUSPICIOUS_LOGIN,
    LOCATION_CHANGE,
    DEVICE_CHANGE,
    MULTIPLE_FAILURES,
    BOT_DETECTION,
    FRAUD_INDICATORS,
    UNUSUAL_ACTIVITY,
    MALWARE_DETECTED,
    VPN_DETECTED,
    TOR_DETECTED,
    DATACENTER_IP,
    SECURITY_POLICY_VIOLATION
}

@Serializable
enum class SecurityEventSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Device location tracking
 */
@Serializable
data class DeviceLocation(
    val id: String,
    val deviceId: String,
    val ipAddress: String,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val isp: String? = null,
    val organization: String? = null,
    val asn: String? = null,
    val isVpn: Boolean = false,
    val isTor: Boolean = false,
    val isDatacenter: Boolean = false,
    val isProxy: Boolean = false,
    val riskScore: Double = 0.0,
    val accuracy: String? = null, // GPS, NETWORK, PASSIVE
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val seenCount: Long = 1,
    val createdAt: Instant
)

/**
 * Device session tracking
 */
@Serializable
data class DeviceSession(
    val id: String,
    val deviceId: String,
    val userId: String,
    val sessionId: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val duration: Long? = null, // seconds
    val activities: List<String> = emptyList(),
    val ipAddresses: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val suspiciousActivities: List<String> = emptyList(),
    val riskScore: Double = 0.0,
    val createdAt: Instant
)

/**
 * Device preferences
 */
@Serializable
data class DevicePreferences(
    val deviceId: String,
    val userId: String,
    val notificationsEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
    val securityAlertsEnabled: Boolean = true,
    val locationTrackingEnabled: Boolean = false,
    val biometricAuthEnabled: Boolean = false,
    val autoLockEnabled: Boolean = true,
    val autoLockTimeout: Int = 900, // seconds
    val requireAuthForSensitive: Boolean = true,
    val allowRemoteWipe: Boolean = false,
    val syncEnabled: Boolean = true,
    val syncData: List<String> = emptyList(),
    val customSettings: Map<String, String> = emptyMap(),
    val updatedAt: Instant,
    val updatedBy: String
)

/**
 * Device management commands
 */
@Serializable
data class DeviceCommand(
    val id: String,
    val deviceId: String,
    val userId: String,
    val command: DeviceCommandType,
    val parameters: Map<String, String> = emptyMap(),
    val status: CommandStatus = CommandStatus.PENDING,
    val issuedAt: Instant,
    val issuedBy: String,
    val executedAt: Instant? = null,
    val result: String? = null,
    val error: String? = null,
    val expiresAt: Instant
)

@Serializable
enum class DeviceCommandType {
    LOCK,
    UNLOCK,
    WIPE,
    LOCATE,
    RING,
    MESSAGE,
    RESTRICT_ACCESS,
    ENABLE_NOTIFICATIONS,
    DISABLE_NOTIFICATIONS,
    SYNC_DATA,
    UPDATE_SECURITY,
    REVOKE_SESSIONS
}

@Serializable
enum class CommandStatus {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    EXECUTED,
    FAILED,
    EXPIRED,
    CANCELLED
}

/**
 * Device analytics
 */
@Serializable
data class DeviceAnalytics(
    val deviceId: String,
    val userId: String,
    val period: AnalyticsPeriod,
    val totalLogins: Long,
    val uniqueDays: Int,
    val averageSessionDuration: Long, // seconds
    val mostActiveHour: Int,
    val mostActiveDay: String,
    val locationCount: Int,
    val ipAddressCount: Int,
    val securityEvents: Long,
    val trustScoreChanges: List<TrustScoreChange>,
    val usagePatterns: List<UsagePattern>,
    val generatedAt: Instant
)

@Serializable
data class TrustScoreChange(
    val date: String, // ISO date
    val oldScore: Double,
    val newScore: Double,
    val reason: String
)

@Serializable
data class UsagePattern(
    val pattern: String,
    val frequency: Long,
    val lastSeen: Instant
)

@Serializable
enum class AnalyticsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}

/**
 * Device types and constants
 */
object DeviceTypes {
    const val MOBILE = "MOBILE"
    const val DESKTOP = "DESKTOP"
    const val TABLET = "TABLET"
    const val TV = "TV"
    const val WATCH = "WATCH"
    const val CONSOLE = "CONSOLE"
    const val IOT = "IOT"
    const val OTHER = "OTHER"
}

object Platforms {
    const val IOS = "IOS"
    const val ANDROID = "ANDROID"
    const val WINDOWS = "WINDOWS"
    const val MACOS = "MACOS"
    const val LINUX = "LINUX"
    const val WEB = "WEB"
    const val OTHER = "OTHER"
}

object RegistrationMethods {
    const val MANUAL = "MANUAL"
    const val AUTO_DETECT = "AUTO_DETECT"
    const val IMPORTED = "IMPORTED"
    const val API = "API"
}
