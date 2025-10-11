package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Recovery Method Domain Models for Entativa ID
 * Comprehensive account recovery management with security validation
 * 
 * @author Neo Qiss
 * @status Production-ready recovery system with enterprise security
 */

/**
 * RecoveryMethod - Main recovery method entity for account recovery
 */
@Serializable
data class RecoveryMethod(
    val id: String,
    val userId: String,
    val methodType: String, // EMAIL, SMS, SECURITY_QUESTIONS, BACKUP_EMAIL, TRUSTED_CONTACT
    val methodValue: String, // Email, phone, etc. (encrypted)
    val encryptedValue: String? = null, // Encrypted version of methodValue
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val priority: Int = 1, // Lower number = higher priority
    val securityLevel: String = "STANDARD", // STANDARD, HIGH, CRITICAL
    val trustScore: Double = 50.0, // 0-100
    val riskFlags: String? = null, // JSON array of risk indicators
    val maxAttempts: Int = 3,
    val expirationHours: Int = 24, // Recovery code expiration
    val settings: String? = null, // JSON settings specific to method type
    val metadata: String? = null, // Additional metadata
    val verifiedAt: Instant? = null,
    val verifiedBy: String? = null,
    val verificationCode: String? = null, // Last verification code used
    val activeRecoveryCode: String? = null, // Current active recovery code
    val recoveryCodeExpiresAt: Instant? = null,
    val recoveryInitiatedAt: Instant? = null,
    val recoveryInitiatedBy: String? = null,
    val lastUsedAt: Instant? = null,
    val usageCount: Long = 0,
    val lastUsedIp: String? = null,
    val lastUsedLocation: String? = null,
    val attemptCount: Int = 0,
    val lastAttemptAt: Instant? = null,
    val lastAttemptIp: String? = null,
    val lastSecurityCheck: Instant? = null,
    val deactivatedAt: Instant? = null,
    val deactivatedBy: String? = null,
    val deactivationReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * Create recovery method request
 */
@Serializable
data class CreateRecoveryMethodRequest(
    val userId: String,
    val methodType: String,
    val methodValue: String,
    val encryptedValue: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val priority: Int = 1,
    val securityLevel: String = "STANDARD",
    val trustScore: Double = 50.0,
    val maxAttempts: Int = 3,
    val expirationHours: Int = 24,
    val settings: String? = null,
    val metadata: String? = null,
    val createdBy: String
)

/**
 * Recovery method response for API
 */
@Serializable
data class RecoveryMethodResponse(
    val id: String,
    val methodType: String,
    val methodValue: String, // Masked/sanitized for security
    val isActive: Boolean,
    val isVerified: Boolean,
    val isPrimary: Boolean,
    val priority: Int,
    val securityLevel: String,
    val trustScore: Double,
    val lastUsedAt: Instant? = null,
    val usageCount: Long,
    val createdAt: Instant
)

/**
 * Recovery statistics
 */
@Serializable
data class RecoveryStatistics(
    val totalMethods: Long,
    val activeMethods: Long,
    val verifiedMethods: Long,
    val hasPrimaryMethod: Boolean,
    val primaryMethodType: String? = null,
    val generatedAt: Instant
)

/**
 * Account recovery request
 */
@Serializable
data class AccountRecoveryRequest(
    val identifier: String, // Email, phone, or username
    val methodType: String? = null, // Preferred recovery method
    val reason: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null
)

/**
 * Account recovery response
 */
@Serializable
data class AccountRecoveryResponse(
    val recoveryId: String,
    val availableMethods: List<AvailableRecoveryMethod>,
    val recommendedMethod: String? = null,
    val securityNotice: String,
    val nextSteps: List<String>,
    val expiresAt: Instant
)

/**
 * Available recovery method
 */
@Serializable
data class AvailableRecoveryMethod(
    val methodType: String,
    val methodHint: String, // Masked method value (e.g., "*****@example.com")
    val isRecommended: Boolean = false,
    val securityLevel: String,
    val estimatedDeliveryTime: String? = null
)

/**
 * Recovery code request
 */
@Serializable
data class RecoveryCodeRequest(
    val recoveryId: String,
    val methodId: String,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * Recovery code response
 */
@Serializable
data class RecoveryCodeResponse(
    val sent: Boolean,
    val methodHint: String,
    val expiresAt: Instant,
    val attemptsRemaining: Int,
    val nextAttemptAt: Instant? = null,
    val deliveryStatus: String? = null,
    val securityWarning: String? = null
)

/**
 * Recovery verification request
 */
@Serializable
data class RecoveryVerificationRequest(
    val recoveryId: String,
    val code: String,
    val newPassword: String? = null, // For password reset
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * Recovery verification response
 */
@Serializable
data class RecoveryVerificationResponse(
    val success: Boolean,
    val accessToken: String? = null, // Temporary token for account access
    val expiresAt: Instant? = null,
    val nextSteps: List<String> = emptyList(),
    val securityRecommendations: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

/**
 * Security questions setup request
 */
@Serializable
data class SecurityQuestionsSetupRequest(
    val userId: String,
    val questions: List<SecurityQuestion>
)

/**
 * Security question
 */
@Serializable
data class SecurityQuestion(
    val questionId: String,
    val customQuestion: String? = null, // For custom questions
    val answerHash: String, // Hashed answer
    val hints: List<String> = emptyList()
)

/**
 * Predefined security questions
 */
@Serializable
data class PredefinedQuestion(
    val id: String,
    val question: String,
    val category: String,
    val securityLevel: String,
    val language: String = "en"
)

/**
 * Trusted contact setup request
 */
@Serializable
data class TrustedContactSetupRequest(
    val userId: String,
    val contactEmail: String,
    val contactName: String,
    val relationship: String,
    val emergencyOnly: Boolean = false,
    val notificationPreferences: ContactNotificationPreferences
)

/**
 * Contact notification preferences
 */
@Serializable
data class ContactNotificationPreferences(
    val immediateNotification: Boolean = true,
    val waitingPeriod: Int = 0, // hours
    val requireContactVerification: Boolean = true,
    val includeAccountInfo: Boolean = false
)

/**
 * Trusted contact
 */
@Serializable
data class TrustedContact(
    val id: String,
    val userId: String,
    val contactEmail: String,
    val contactName: String,
    val relationship: String,
    val isVerified: Boolean = false,
    val verifiedAt: Instant? = null,
    val emergencyOnly: Boolean = false,
    val lastContactedAt: Instant? = null,
    val contactCount: Long = 0,
    val preferences: ContactNotificationPreferences,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Recovery attempt
 */
@Serializable
data class RecoveryAttempt(
    val id: String,
    val userId: String,
    val methodId: String,
    val attemptType: RecoveryAttemptType,
    val status: RecoveryAttemptStatus,
    val code: String? = null, // For verification codes
    val codeHash: String,
    val expiresAt: Instant,
    val verifiedAt: Instant? = null,
    val failureReason: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val metadata: String? = null,
    val createdAt: Instant
)

@Serializable
enum class RecoveryAttemptType {
    EMAIL_CODE,
    SMS_CODE,
    SECURITY_QUESTIONS,
    TRUSTED_CONTACT,
    BACKUP_EMAIL,
    ADMIN_OVERRIDE
}

@Serializable
enum class RecoveryAttemptStatus {
    PENDING,
    SENT,
    VERIFIED,
    EXPIRED,
    FAILED,
    BLOCKED
}

/**
 * Recovery policy configuration
 */
@Serializable
data class RecoveryPolicy(
    val id: String,
    val name: String,
    val description: String,
    val requiredMethods: Int = 1,
    val allowedMethods: List<String> = emptyList(),
    val securityQuestionsRequired: Boolean = false,
    val minimumSecurityQuestions: Int = 3,
    val trustedContactRequired: Boolean = false,
    val minimumTrustedContacts: Int = 1,
    val backupEmailRequired: Boolean = false,
    val maxAttemptsPerDay: Int = 5,
    val cooldownPeriodHours: Int = 1,
    val codeExpirationMinutes: Int = 15,
    val requireIdentityVerification: Boolean = true,
    val allowSelfService: Boolean = true,
    val adminOverrideRequired: Boolean = false,
    val auditRequired: Boolean = true,
    val notifyOnRecovery: Boolean = true,
    val settings: String? = null, // JSON policy settings
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null
)

/**
 * Recovery audit event
 */
@Serializable
data class RecoveryAuditEvent(
    val id: String,
    val userId: String,
    val methodId: String? = null,
    val eventType: RecoveryEventType,
    val action: String,
    val result: String, // SUCCESS, FAILURE, BLOCKED
    val reason: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val riskScore: Double = 0.0,
    val metadata: String? = null,
    val createdAt: Instant
)

@Serializable
enum class RecoveryEventType {
    SETUP,
    INITIATION,
    CODE_SENT,
    VERIFICATION,
    COMPLETION,
    METHOD_DISABLED,
    METHOD_REMOVED,
    POLICY_APPLIED,
    SECURITY_CHECK,
    SUSPICIOUS_ACTIVITY,
    ADMIN_INTERVENTION
)

/**
 * Recovery analytics
 */
@Serializable
data class RecoveryAnalytics(
    val userId: String? = null,
    val period: AnalyticsPeriod,
    val totalMethods: Long,
    val methodsByType: Map<String, Long>,
    val recoveryAttempts: Long,
    val successfulRecoveries: Long,
    val failedRecoveries: Long,
    val averageRecoveryTime: Double, // minutes
    val mostUsedMethod: String? = null,
    val securityEvents: Long,
    val riskScore: Double,
    val generatedAt: Instant
)

/**
 * Account lockout information
 */
@Serializable
data class AccountLockout(
    val userId: String,
    val reason: LockoutReason,
    val lockedAt: Instant,
    val lockedUntil: Instant? = null, // null = indefinite
    val attemptCount: Int,
    val lockoutLevel: LockoutLevel,
    val canRecover: Boolean = true,
    val recoveryMethods: List<String> = emptyList(),
    val administratorContact: String? = null,
    val metadata: String? = null
)

@Serializable
enum class LockoutReason {
    FAILED_RECOVERY_ATTEMPTS,
    SUSPICIOUS_ACTIVITY,
    SECURITY_VIOLATION,
    ADMIN_ACTION,
    POLICY_VIOLATION,
    FRAUD_DETECTION
}

@Serializable
enum class LockoutLevel {
    WARNING,
    TEMPORARY,
    EXTENDED,
    PERMANENT
}

/**
 * Recovery types and constants
 */
object RecoveryTypes {
    const val EMAIL = "EMAIL"
    const val SMS = "SMS"
    const val SECURITY_QUESTIONS = "SECURITY_QUESTIONS"
    const val BACKUP_EMAIL = "BACKUP_EMAIL"
    const val TRUSTED_CONTACT = "TRUSTED_CONTACT"
    const val PHONE_CALL = "PHONE_CALL"
    const val POSTAL_MAIL = "POSTAL_MAIL"
    const val IN_PERSON = "IN_PERSON"
    const val ADMIN_OVERRIDE = "ADMIN_OVERRIDE"
}

object RecoveryActions {
    const val PASSWORD_RESET = "PASSWORD_RESET"
    const val ACCOUNT_UNLOCK = "ACCOUNT_UNLOCK"
    const val EMAIL_CHANGE = "EMAIL_CHANGE"
    const val PHONE_CHANGE = "PHONE_CHANGE"
    const val MFA_RESET = "MFA_RESET"
    const val PROFILE_RECOVERY = "PROFILE_RECOVERY"
    const val ACCOUNT_VERIFICATION = "ACCOUNT_VERIFICATION"
}

object RecoverySecurityLevels {
    const val STANDARD = "STANDARD"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}
