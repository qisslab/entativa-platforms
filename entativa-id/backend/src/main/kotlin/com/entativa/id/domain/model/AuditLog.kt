package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Audit Log Domain Models for Entativa ID
 * Comprehensive audit logging and compliance system
 * 
 * @author Neo Qiss
 * @status Production-ready audit system with compliance features
 */

/**
 * AuditLog - Main audit log entity for compliance and security tracking
 */
@Serializable
data class AuditLog(
    val id: String,
    val userId: String? = null,
    val sessionId: String? = null,
    val action: String, // LOGIN, LOGOUT, CREATE_USER, UPDATE_PROFILE, etc.
    val resource: String, // USER, PROFILE, TOKEN, DEVICE, etc.
    val resourceId: String? = null,
    val status: String, // SUCCESS, FAILURE, PENDING
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val platform: String? = null,
    val location: String? = null,
    val deviceId: String? = null,
    val deviceFingerprint: String? = null,
    val requestId: String? = null,
    val traceId: String? = null,
    val method: String? = null, // HTTP method or API method
    val endpoint: String? = null,
    val queryParams: String? = null, // JSON string
    val requestBody: String? = null, // JSON string (sanitized)
    val responseStatus: Int? = null,
    val responseBody: String? = null, // JSON string (sanitized)
    val duration: Long? = null, // milliseconds
    val oldValues: String? = null, // JSON of previous values
    val newValues: String? = null, // JSON of new values
    val changes: String? = null, // JSON array of changes
    val reason: String? = null,
    val severity: String = "INFO", // DEBUG, INFO, WARN, ERROR, CRITICAL
    val riskLevel: String = "LOW", // LOW, MEDIUM, HIGH, CRITICAL
    val complianceFlags: String? = null, // JSON array of compliance flags
    val tags: String? = null, // JSON array of tags
    val metadata: String? = null, // Additional metadata
    val context: String? = null, // Additional context information
    val correlationId: String? = null,
    val parentEventId: String? = null,
    val eventSource: String = "ENTATIVA_ID",
    val eventVersion: String = "1.0",
    val archived: Boolean = false,
    val archivedAt: Instant? = null,
    val createdAt: Instant
)

/**
 * Create audit log request
 */
@Serializable
data class CreateAuditLogRequest(
    val userId: String? = null,
    val sessionId: String? = null,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val status: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val platform: String? = null,
    val location: String? = null,
    val deviceId: String? = null,
    val deviceFingerprint: String? = null,
    val requestId: String? = null,
    val traceId: String? = null,
    val method: String? = null,
    val endpoint: String? = null,
    val queryParams: String? = null,
    val requestBody: String? = null,
    val responseStatus: Int? = null,
    val responseBody: String? = null,
    val duration: Long? = null,
    val oldValues: String? = null,
    val newValues: String? = null,
    val changes: String? = null,
    val reason: String? = null,
    val severity: String = "INFO",
    val riskLevel: String = "LOW",
    val complianceFlags: String? = null,
    val tags: String? = null,
    val metadata: String? = null,
    val context: String? = null,
    val correlationId: String? = null,
    val parentEventId: String? = null,
    val eventSource: String = "ENTATIVA_ID",
    val eventVersion: String = "1.0"
)

/**
 * Audit log response for API
 */
@Serializable
data class AuditLogResponse(
    val id: String,
    val userId: String? = null,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val status: String,
    val severity: String,
    val riskLevel: String,
    val platform: String? = null,
    val location: String? = null,
    val reason: String? = null,
    val createdAt: Instant
)

/**
 * Audit statistics
 */
@Serializable
data class AuditStatistics(
    val totalEvents: Long,
    val successEvents: Long,
    val failureEvents: Long,
    val securityEvents: Long,
    val uniqueUsers: Long,
    val successRate: Double,
    val failureRate: Double,
    val securityEventRate: Double,
    val generatedAt: Instant
)

/**
 * Compliance report
 */
@Serializable
data class ComplianceReport(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalEvents: Long,
    val accessEvents: Long,
    val dataEvents: Long,
    val securityEvents: Long,
    val failureEvents: Long,
    val complianceStandard: String? = null,
    val violations: Long,
    val generatedAt: Instant,
    val events: List<AuditLog> = emptyList()
)

/**
 * Security alert based on audit logs
 */
@Serializable
data class SecurityAlert(
    val id: String,
    val type: SecurityAlertType,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val userId: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val location: String? = null,
    val triggerEvents: List<String>, // Audit log IDs
    val riskScore: Double,
    val isResolved: Boolean = false,
    val resolvedAt: Instant? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val actionsTaken: List<String> = emptyList(),
    val metadata: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class SecurityAlertType {
    SUSPICIOUS_LOGIN,
    MULTIPLE_FAILED_LOGINS,
    UNUSUAL_LOCATION,
    DEVICE_CHANGE,
    PRIVILEGE_ESCALATION,
    DATA_BREACH_ATTEMPT,
    ACCOUNT_TAKEOVER,
    FRAUD_INDICATORS,
    COMPLIANCE_VIOLATION,
    SYSTEM_COMPROMISE
}

@Serializable
enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Audit trail for a specific resource
 */
@Serializable
data class AuditTrail(
    val resourceType: String,
    val resourceId: String,
    val events: List<AuditLogResponse>,
    val totalEvents: Long,
    val timespan: AuditTimespan,
    val summary: AuditTrailSummary,
    val generatedAt: Instant
)

@Serializable
data class AuditTimespan(
    val start: Instant,
    val end: Instant,
    val durationDays: Long
)

@Serializable
data class AuditTrailSummary(
    val totalActions: Long,
    val uniqueUsers: Long,
    val successActions: Long,
    val failureActions: Long,
    val mostCommonActions: List<ActionCount>,
    val securityEvents: Long,
    val lastModified: Instant? = null,
    val lastModifiedBy: String? = null
)

@Serializable
data class ActionCount(
    val action: String,
    val count: Long,
    val lastOccurrence: Instant
)

/**
 * Audit search filters
 */
@Serializable
data class AuditSearchFilters(
    val userId: String? = null,
    val sessionId: String? = null,
    val action: String? = null,
    val resource: String? = null,
    val status: String? = null,
    val severity: String? = null,
    val riskLevel: String? = null,
    val ipAddress: String? = null,
    val platform: String? = null,
    val deviceId: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val tags: List<String>? = null,
    val complianceFlags: List<String>? = null,
    val textSearch: String? = null,
    val limit: Int = 100,
    val offset: Int = 0,
    val orderBy: String = "createdAt",
    val orderDirection: String = "DESC"
)

/**
 * Audit log aggregation result
 */
@Serializable
data class AuditAggregation(
    val groupBy: String,
    val results: List<AggregationResult>,
    val totalGroups: Long,
    val generatedAt: Instant
)

@Serializable
data class AggregationResult(
    val key: String,
    val count: Long,
    val successCount: Long,
    val failureCount: Long,
    val uniqueUsers: Long,
    val firstOccurrence: Instant,
    val lastOccurrence: Instant
)

/**
 * Data retention policy for audit logs
 */
@Serializable
data class AuditRetentionPolicy(
    val id: String,
    val name: String,
    val description: String,
    val retentionDays: Int,
    val archiveAfterDays: Int,
    val deleteAfterDays: Int? = null,
    val applicableActions: List<String> = emptyList(),
    val applicableResources: List<String> = emptyList(),
    val complianceRequirement: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null
)

/**
 * Audit log export request
 */
@Serializable
data class AuditExportRequest(
    val format: ExportFormat,
    val filters: AuditSearchFilters,
    val includeFields: List<String>? = null,
    val excludeFields: List<String>? = null,
    val compression: Boolean = true,
    val encryption: Boolean = false,
    val requestedBy: String,
    val purpose: String,
    val retentionDays: Int = 30
)

@Serializable
enum class ExportFormat {
    JSON,
    CSV,
    XML,
    PARQUET
}

/**
 * Audit log export result
 */
@Serializable
data class AuditExportResult(
    val id: String,
    val status: ExportStatus,
    val format: ExportFormat,
    val recordCount: Long,
    val fileSize: Long, // bytes
    val downloadUrl: String? = null,
    val expiresAt: Instant? = null,
    val error: String? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val requestedBy: String
)

@Serializable
enum class ExportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED
}

/**
 * Audit log patterns for anomaly detection
 */
@Serializable
data class AuditPattern(
    val id: String,
    val name: String,
    val description: String,
    val pattern: String, // Regex or query pattern
    val severity: AlertSeverity,
    val threshold: Int, // Number of occurrences to trigger alert
    val timeWindow: Int, // Minutes
    val isActive: Boolean = true,
    val actions: List<String> = emptyList(), // Actions to take when pattern matches
    val lastTriggered: Instant? = null,
    val triggerCount: Long = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String
)

/**
 * Audit actions and constants
 */
object AuditActions {
    // Authentication
    const val LOGIN = "LOGIN"
    const val LOGOUT = "LOGOUT"
    const val LOGIN_FAILED = "LOGIN_FAILED"
    const val PASSWORD_RESET = "PASSWORD_RESET"
    const val PASSWORD_CHANGE = "PASSWORD_CHANGE"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED"
    
    // User Management
    const val USER_CREATE = "USER_CREATE"
    const val USER_UPDATE = "USER_UPDATE"
    const val USER_DELETE = "USER_DELETE"
    const val USER_SUSPEND = "USER_SUSPEND"
    const val USER_ACTIVATE = "USER_ACTIVATE"
    const val EMAIL_VERIFY = "EMAIL_VERIFY"
    const val PHONE_VERIFY = "PHONE_VERIFY"
    
    // Profile Management
    const val PROFILE_CREATE = "PROFILE_CREATE"
    const val PROFILE_UPDATE = "PROFILE_UPDATE"
    const val PROFILE_DELETE = "PROFILE_DELETE"
    const val PROFILE_SYNC = "PROFILE_SYNC"
    
    // Handle Management
    const val HANDLE_CLAIM = "HANDLE_CLAIM"
    const val HANDLE_RELEASE = "HANDLE_RELEASE"
    const val HANDLE_TRANSFER = "HANDLE_TRANSFER"
    const val HANDLE_RESERVE = "HANDLE_RESERVE"
    
    // Security
    const val MFA_ENABLE = "MFA_ENABLE"
    const val MFA_DISABLE = "MFA_DISABLE"
    const val MFA_VERIFY = "MFA_VERIFY"
    const val DEVICE_REGISTER = "DEVICE_REGISTER"
    const val DEVICE_TRUST = "DEVICE_TRUST"
    const val DEVICE_BLOCK = "DEVICE_BLOCK"
    const val TOKEN_CREATE = "TOKEN_CREATE"
    const val TOKEN_REVOKE = "TOKEN_REVOKE"
    const val TOKEN_REFRESH = "TOKEN_REFRESH"
    
    // Data Access
    const val DATA_ACCESS = "DATA_ACCESS"
    const val DATA_EXPORT = "DATA_EXPORT"
    const val DATA_DELETE = "DATA_DELETE"
    const val PRIVACY_CHANGE = "PRIVACY_CHANGE"
    
    // Administrative
    const val ADMIN_ACCESS = "ADMIN_ACCESS"
    const val POLICY_CHANGE = "POLICY_CHANGE"
    const val PERMISSION_GRANT = "PERMISSION_GRANT"
    const val PERMISSION_REVOKE = "PERMISSION_REVOKE"
}

object AuditResources {
    const val USER = "USER"
    const val PROFILE = "PROFILE"
    const val HANDLE = "HANDLE"
    const val TOKEN = "TOKEN"
    const val DEVICE = "DEVICE"
    const val MFA_METHOD = "MFA_METHOD"
    const val RECOVERY_METHOD = "RECOVERY_METHOD"
    const val SESSION = "SESSION"
    const val OAUTH_CLIENT = "OAUTH_CLIENT"
    const val PERMISSION = "PERMISSION"
    const val POLICY = "POLICY"
    const val SYSTEM = "SYSTEM"
}

object AuditStatus {
    const val SUCCESS = "SUCCESS"
    const val FAILURE = "FAILURE"
    const val PENDING = "PENDING"
    const val PARTIAL = "PARTIAL"
}

object AuditSeverity {
    const val DEBUG = "DEBUG"
    const val INFO = "INFO"
    const val WARN = "WARN"
    const val ERROR = "ERROR"
    const val CRITICAL = "CRITICAL"
}

object RiskLevels {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}
