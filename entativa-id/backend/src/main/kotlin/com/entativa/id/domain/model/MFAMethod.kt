package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * MFA Method Domain Models for Entativa ID
 * Comprehensive Multi-Factor Authentication management
 * 
 * @author Neo Qiss
 * @status Production-ready MFA system with enterprise security
 */

/**
 * MFAMethod - Main MFA method entity for authentication
 */
@Serializable
data class MFAMethod(
    val id: String,
    val userId: String,
    val methodType: String, // TOTP, SMS, EMAIL, BIOMETRIC, HARDWARE_KEY, BACKUP_CODES
    val methodValue: String, // Phone number, email, device ID, etc. (encrypted)
    val encryptedSecret: String? = null, // For TOTP, encrypted
    val backupCodes: String? = null, // Encrypted backup codes
    val usedBackupCodes: String? = null, // List of used codes
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val isDefault: Boolean = false,
    val priority: Int = 1, // Lower number = higher priority
    val deviceId: String? = null,
    val platform: String? = null,
    val securityLevel: String = "STANDARD", // STANDARD, HIGH, CRITICAL
    val trustScore: Double = 50.0, // 0-100
    val riskFlags: String? = null, // JSON array of risk indicators
    val settings: String? = null, // JSON settings specific to method type
    val metadata: String? = null, // Additional metadata
    val verifiedAt: Instant? = null,
    val verifiedBy: String? = null,
    val verificationCode: String? = null, // Last verification code used
    val lastUsedAt: Instant? = null,
    val usageCount: Long = 0,
    val lastUsedIp: String? = null,
    val lastUsedDevice: String? = null,
    val lastUsedLocation: String? = null,
    val lastSecurityCheck: Instant? = null,
    val backupCodeUsedAt: Instant? = null,
    val backupCodeUsedBy: String? = null,
    val backupCodesRegeneratedAt: Instant? = null,
    val backupCodesRegeneratedBy: String? = null,
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
 * Create MFA method request
 */
@Serializable
data class CreateMFAMethodRequest(
    val userId: String,
    val methodType: String,
    val methodValue: String,
    val encryptedSecret: String? = null,
    val backupCodes: String? = null,
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val isDefault: Boolean = false,
    val priority: Int = 1,
    val deviceId: String? = null,
    val platform: String? = null,
    val securityLevel: String = "STANDARD",
    val trustScore: Double = 50.0,
    val settings: String? = null,
    val metadata: String? = null,
    val createdBy: String
)

/**
 * MFA method response for API
 */
@Serializable
data class MFAMethodResponse(
    val id: String,
    val methodType: String,
    val methodValue: String, // Masked/sanitized for security
    val isActive: Boolean,
    val isVerified: Boolean,
    val isPrimary: Boolean,
    val isDefault: Boolean,
    val priority: Int,
    val platform: String? = null,
    val securityLevel: String,
    val trustScore: Double,
    val lastUsedAt: Instant? = null,
    val usageCount: Long,
    val createdAt: Instant
)

/**
 * MFA statistics
 */
@Serializable
data class MFAStatistics(
    val totalMethods: Long,
    val activeMethods: Long,
    val verifiedMethods: Long,
    val hasPrimaryMethod: Boolean,
    val primaryMethodType: String? = null,
    val generatedAt: Instant
)

/**
 * MFA challenge request
 */
@Serializable
data class MFAChallengeRequest(
    val userId: String,
    val methodId: String? = null, // If null, use primary method
    val action: String, // LOGIN, SENSITIVE_OPERATION, etc.
    val context: String? = null, // Additional context
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * MFA challenge response
 */
@Serializable
data class MFAChallengeResponse(
    val challengeId: String,
    val methodType: String,
    val methodHint: String, // Masked method value (e.g., "*****@example.com")
    val expiresAt: Instant,
    val qrCode: String? = null, // For TOTP setup
    val allowBackupCodes: Boolean = false,
    val nextSteps: List<String> = emptyList()
)

/**
 * MFA verification request
 */
@Serializable
data class MFAVerificationRequest(
    val challengeId: String,
    val code: String,
    val isBackupCode: Boolean = false,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val trustDevice: Boolean = false
)

/**
 * MFA verification response
 */
@Serializable
data class MFAVerificationResponse(
    val success: Boolean,
    val deviceTrusted: Boolean = false,
    val backupCodesRemaining: Int? = null,
    val shouldRegenerateBackupCodes: Boolean = false,
    val errors: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList()
)

/**
 * TOTP setup request
 */
@Serializable
data class TOTPSetupRequest(
    val userId: String,
    val deviceName: String? = null,
    val issuer: String = "Entativa ID",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

/**
 * TOTP setup response
 */
@Serializable
data class TOTPSetupResponse(
    val methodId: String,
    val secret: String, // Base32 encoded secret (show once)
    val qrCode: String, // Data URL for QR code
    val backupCodes: List<String>, // Show once
    val manualEntryKey: String,
    val issuer: String,
    val accountName: String
)

/**
 * SMS MFA setup request
 */
@Serializable
data class SMSMFASetupRequest(
    val userId: String,
    val phoneNumber: String,
    val countryCode: String,
    val sendVerificationSMS: Boolean = true
)

/**
 * Email MFA setup request
 */
@Serializable
data class EmailMFASetupRequest(
    val userId: String,
    val emailAddress: String,
    val sendVerificationEmail: Boolean = true
)

/**
 * Biometric MFA setup request
 */
@Serializable
data class BiometricMFASetupRequest(
    val userId: String,
    val biometricType: BiometricType,
    val deviceId: String,
    val publicKey: String, // WebAuthn public key or biometric template hash
    val challenge: String,
    val signature: String
)

@Serializable
enum class BiometricType {
    FINGERPRINT,
    FACE_ID,
    TOUCH_ID,
    VOICE,
    IRIS,
    PALM_PRINT,
    RETINA
}

/**
 * Hardware key MFA setup request
 */
@Serializable
data class HardwareKeyMFASetupRequest(
    val userId: String,
    val keyType: String, // YUBIKEY, FIDO2, U2F
    val keyId: String,
    val publicKey: String,
    val attestation: String? = null,
    val deviceName: String? = null
)

/**
 * Backup codes generation request
 */
@Serializable
data class BackupCodesRequest(
    val userId: String,
    val methodId: String,
    val regenerate: Boolean = false,
    val codeCount: Int = 10,
    val codeLength: Int = 8
)

/**
 * Backup codes response
 */
@Serializable
data class BackupCodesResponse(
    val codes: List<String>,
    val generatedAt: Instant,
    val expiresAt: Instant? = null,
    val usageInstructions: String,
    val warning: String
)

/**
 * MFA policy configuration
 */
@Serializable
data class MFAPolicy(
    val id: String,
    val name: String,
    val description: String,
    val isRequired: Boolean = false,
    val requiredFor: List<String> = emptyList(), // Actions requiring MFA
    val allowedMethods: List<String> = emptyList(),
    val minimumMethods: Int = 1,
    val gracePeriodDays: Int = 7,
    val backupCodesRequired: Boolean = true,
    val totpRequired: Boolean = false,
    val biometricAllowed: Boolean = true,
    val smsAllowed: Boolean = true,
    val emailAllowed: Boolean = false,
    val hardwareKeyRequired: Boolean = false,
    val trustDeviceDays: Int = 30,
    val maxFailedAttempts: Int = 3,
    val lockoutDurationMinutes: Int = 15,
    val rememberDeviceDays: Int = 30,
    val settings: String? = null, // JSON policy settings
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null
)

/**
 * MFA challenge entity for tracking active challenges
 */
@Serializable
data class MFAChallenge(
    val id: String,
    val userId: String,
    val methodId: String,
    val challengeType: String,
    val action: String,
    val code: String? = null, // For SMS/Email
    val codeHash: String,
    val expiresAt: Instant,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val context: String? = null,
    val metadata: String? = null,
    val createdAt: Instant
)

/**
 * MFA audit event
 */
@Serializable
data class MFAAuditEvent(
    val id: String,
    val userId: String,
    val methodId: String? = null,
    val eventType: MFAEventType,
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
enum class MFAEventType {
    SETUP,
    VERIFICATION,
    BACKUP_CODE_USED,
    METHOD_DISABLED,
    METHOD_REMOVED,
    POLICY_APPLIED,
    SECURITY_CHECK,
    SUSPICIOUS_ACTIVITY
}

/**
 * MFA recovery request
 */
@Serializable
data class MFARecoveryRequest(
    val userId: String,
    val recoveryMethod: String, // EMAIL, SMS, SECURITY_QUESTIONS, ADMIN_OVERRIDE
    val contactValue: String, // Email or phone for recovery
    val reason: String,
    val supportingEvidence: String? = null
)

/**
 * MFA recovery response
 */
@Serializable
data class MFARecoveryResponse(
    val recoveryId: String,
    val status: RecoveryStatus,
    val nextSteps: List<String>,
    val estimatedProcessingTime: String,
    val contactMethod: String,
    val securityNotice: String
)

@Serializable
enum class RecoveryStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    COMPLETED
}

/**
 * MFA analytics
 */
@Serializable
data class MFAAnalytics(
    val userId: String? = null,
    val period: AnalyticsPeriod,
    val totalMethods: Long,
    val methodsByType: Map<String, Long>,
    val verificationAttempts: Long,
    val successfulVerifications: Long,
    val failedVerifications: Long,
    val backupCodesUsed: Long,
    val averageVerificationTime: Double, // seconds
    val securityEvents: Long,
    val riskScore: Double,
    val generatedAt: Instant
)

/**
 * MFA types and constants
 */
object MFATypes {
    const val TOTP = "TOTP"
    const val SMS = "SMS"
    const val EMAIL = "EMAIL"
    const val BIOMETRIC = "BIOMETRIC"
    const val HARDWARE_KEY = "HARDWARE_KEY"
    const val BACKUP_CODES = "BACKUP_CODES"
    const val PUSH_NOTIFICATION = "PUSH_NOTIFICATION"
    const val VOICE_CALL = "VOICE_CALL"
}

object MFAActions {
    const val LOGIN = "LOGIN"
    const val PASSWORD_CHANGE = "PASSWORD_CHANGE"
    const val EMAIL_CHANGE = "EMAIL_CHANGE"
    const val PHONE_CHANGE = "PHONE_CHANGE"
    const val PROFILE_UPDATE = "PROFILE_UPDATE"
    const val PAYMENT = "PAYMENT"
    const val DATA_EXPORT = "DATA_EXPORT"
    const val ACCOUNT_DELETION = "ACCOUNT_DELETION"
    const val ADMIN_ACCESS = "ADMIN_ACCESS"
    const val API_ACCESS = "API_ACCESS"
    const val SENSITIVE_OPERATION = "SENSITIVE_OPERATION"
}

object MFASecurityLevels {
    const val STANDARD = "STANDARD"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}

object ChallengeTypes {
    const val TOTP_CODE = "TOTP_CODE"
    const val SMS_CODE = "SMS_CODE"
    const val EMAIL_CODE = "EMAIL_CODE"
    const val BIOMETRIC_SCAN = "BIOMETRIC_SCAN"
    const val HARDWARE_KEY_TOUCH = "HARDWARE_KEY_TOUCH"
    const val PUSH_APPROVAL = "PUSH_APPROVAL"
    const val BACKUP_CODE = "BACKUP_CODE"
}
