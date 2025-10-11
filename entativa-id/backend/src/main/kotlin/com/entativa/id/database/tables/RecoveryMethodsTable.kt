package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Recovery Methods Table Definition for Entativa ID
 * Manages account recovery methods including security questions, backup emails, recovery codes, and emergency contacts
 * 
 * @author Neo Qiss
 * @status Production-ready account recovery with enterprise security features
 */
object RecoveryMethodsTable : UUIDTable("recovery_methods") {
    
    // Core Recovery Method Information
    val userId: Column<String> = varchar("user_id", 100).index()
    val methodType: Column<String> = varchar("method_type", 30) // EMAIL, SMS, SECURITY_QUESTIONS, BACKUP_CODES, EMERGENCY_CONTACT, TRUSTED_DEVICE, BIOMETRIC
    val methodName: Column<String> = varchar("method_name", 200) // User-friendly name
    val methodIdentifier: Column<String> = varchar("method_identifier", 500) // Email, phone, device ID, etc.
    val priority: Column<Int> = integer("priority").default(1) // Lower numbers = higher priority
    
    // Method Status and Security
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    val isPrimary: Column<Boolean> = bool("is_primary").default(false)
    val isEmergencyOnly: Column<Boolean> = bool("is_emergency_only").default(false)
    val trustLevel: Column<String> = varchar("trust_level", 20).default("STANDARD") // LOW, STANDARD, HIGH, MAXIMUM
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val requiresMFA: Column<Boolean> = bool("requires_mfa").default(false)
    
    // Email Recovery Settings
    val recoveryEmail: Column<String?> = varchar("recovery_email", 254).nullable()
    val recoveryEmailVerified: Column<Boolean> = bool("recovery_email_verified").default(false)
    val recoveryEmailVerifiedAt: Column<Instant?> = timestamp("recovery_email_verified_at").nullable()
    val emailProvider: Column<String?> = varchar("email_provider", 50).nullable()
    val emailEncrypted: Column<Boolean> = bool("email_encrypted").default(false)
    val emailAlias: Column<String?> = varchar("email_alias", 200).nullable()
    
    // SMS Recovery Settings
    val recoveryPhone: Column<String?> = varchar("recovery_phone", 20).nullable()
    val phoneCountryCode: Column<String?> = varchar("phone_country_code", 5).nullable()
    val phoneVerified: Column<Boolean> = bool("phone_verified").default(false)
    val phoneVerifiedAt: Column<Instant?> = timestamp("phone_verified_at").nullable()
    val smsProvider: Column<String?> = varchar("sms_provider", 50).nullable()
    val phoneType: Column<String?> = varchar("phone_type", 20).nullable() // MOBILE, LANDLINE, VOIP
    
    // Security Questions
    val securityQuestions: Column<String> = text("security_questions").default("[]") // JSON array of question/answer pairs (hashed)
    val questionCount: Column<Int> = integer("question_count").default(0)
    val requiredCorrectAnswers: Column<Int> = integer("required_correct_answers").default(2)
    val maxAnswerAttempts: Column<Int> = integer("max_answer_attempts").default(3)
    val answerAttempts: Column<Int> = integer("answer_attempts").default(0)
    val answersLastUpdated: Column<Instant?> = timestamp("answers_last_updated").nullable()
    val customQuestions: Column<Boolean> = bool("custom_questions").default(false)
    
    // Backup Recovery Codes
    val backupCodes: Column<String?> = text("backup_codes").nullable() // JSON array of hashed codes
    val totalBackupCodes: Column<Int> = integer("total_backup_codes").default(0)
    val usedBackupCodes: Column<Int> = integer("used_backup_codes").default(0)
    val remainingBackupCodes: Column<Int> = integer("remaining_backup_codes").default(0)
    val backupCodesGenerated: Column<Boolean> = bool("backup_codes_generated").default(false)
    val backupCodesGeneratedAt: Column<Instant?> = timestamp("backup_codes_generated_at").nullable()
    val backupCodesLastUsed: Column<Instant?> = timestamp("backup_codes_last_used").nullable()
    val backupCodesEncryption: Column<String> = varchar("backup_codes_encryption", 20).default("AES256")
    
    // Emergency Contacts
    val emergencyContactName: Column<String?> = varchar("emergency_contact_name", 200).nullable()
    val emergencyContactEmail: Column<String?> = varchar("emergency_contact_email", 254).nullable()
    val emergencyContactPhone: Column<String?> = varchar("emergency_contact_phone", 20).nullable()
    val emergencyContactRelationship: Column<String?> = varchar("emergency_contact_relationship", 100).nullable()
    val emergencyContactVerified: Column<Boolean> = bool("emergency_contact_verified").default(false)
    val emergencyContactVerifiedAt: Column<Instant?> = timestamp("emergency_contact_verified_at").nullable()
    val emergencyContactConsent: Column<Boolean> = bool("emergency_contact_consent").default(false)
    val emergencyContactInstructions: Column<String?> = text("emergency_contact_instructions").nullable()
    
    // Trusted Device Recovery
    val trustedDeviceId: Column<String?> = varchar("trusted_device_id", 128).nullable()
    val deviceName: Column<String?> = varchar("device_name", 200).nullable()
    val deviceFingerprint: Column<String?> = varchar("device_fingerprint", 256).nullable()
    val deviceType: Column<String?> = varchar("device_type", 50).nullable()
    val deviceRegisteredAt: Column<Instant?> = timestamp("device_registered_at").nullable()
    val deviceLastSeen: Column<Instant?> = timestamp("device_last_seen").nullable()
    val deviceTrustLevel: Column<String> = varchar("device_trust_level", 20).default("STANDARD")
    val deviceRequiresBiometric: Column<Boolean> = bool("device_requires_biometric").default(false)
    
    // Biometric Recovery
    val biometricType: Column<String?> = varchar("biometric_type", 30).nullable() // FINGERPRINT, FACE_ID, VOICE, IRIS
    val biometricTemplateId: Column<String?> = varchar("biometric_template_id", 128).nullable()
    val biometricQuality: Column<Double?> = double("biometric_quality").nullable()
    val biometricEnrolledAt: Column<Instant?> = timestamp("biometric_enrolled_at").nullable()
    val biometricLastUsed: Column<Instant?> = timestamp("biometric_last_used").nullable()
    val biometricBackupAvailable: Column<Boolean> = bool("biometric_backup_available").default(false)
    val biometricDeviceBinding: Column<Boolean> = bool("biometric_device_binding").default(true)
    
    // Recovery Process Tracking
    val lastUsedAt: Column<Instant?> = timestamp("last_used_at").nullable()
    val totalUsageCount: Column<Long> = long("total_usage_count").default(0)
    val successfulRecoveries: Column<Long> = long("successful_recoveries").default(0)
    val failedRecoveryAttempts: Column<Long> = long("failed_recovery_attempts").default(0)
    val lastSuccessfulRecovery: Column<Instant?> = timestamp("last_successful_recovery").nullable()
    val lastFailedAttempt: Column<Instant?> = timestamp("last_failed_attempt").nullable()
    val consecutiveFailures: Column<Int> = integer("consecutive_failures").default(0)
    val isLocked: Column<Boolean> = bool("is_locked").default(false)
    val lockedUntil: Column<Instant?> = timestamp("locked_until").nullable()
    val lockReason: Column<String?> = varchar("lock_reason", 200).nullable()
    
    // Verification and Validation
    val verificationCode: Column<String?> = varchar("verification_code", 20).nullable()
    val verificationCodeExpiresAt: Column<Instant?> = timestamp("verification_code_expires_at").nullable()
    val verificationAttempts: Column<Int> = integer("verification_attempts").default(0)
    val maxVerificationAttempts: Column<Int> = integer("max_verification_attempts").default(5)
    val verificationMethod: Column<String?> = varchar("verification_method", 50).nullable()
    val verificationToken: Column<String?> = varchar("verification_token", 255).nullable()
    val verificationTokenExpiresAt: Column<Instant?> = timestamp("verification_token_expires_at").nullable()
    val verifiedBy: Column<String?> = varchar("verified_by", 100).nullable()
    
    // Recovery Limitations and Rules
    val maxUsagesPerDay: Column<Int> = integer("max_usages_per_day").default(3)
    val maxUsagesPerMonth: Column<Int> = integer("max_usages_per_month").default(10)
    val currentDayUsages: Column<Int> = integer("current_day_usage").default(0)
    val currentMonthUsages: Column<Int> = integer("current_month_usage").default(0)
    val usageCounterResetDaily: Column<Instant?> = timestamp("usage_counter_reset_daily").nullable()
    val usageCounterResetMonthly: Column<Instant?> = timestamp("usage_counter_reset_monthly").nullable()
    val cooldownPeriodHours: Column<Int> = integer("cooldown_period_hours").default(24)
    val lastCooldownReset: Column<Instant?> = timestamp("last_cooldown_reset").nullable()
    
    // Geographic and Network Restrictions
    val allowedCountries: Column<String> = text("allowed_countries").default("[]") // JSON array
    val blockedCountries: Column<String> = text("blocked_countries").default("[]") // JSON array
    val allowedIPRanges: Column<String> = text("allowed_ip_ranges").default("[]") // JSON array
    val blockedIPRanges: Column<String> = text("blocked_ip_ranges").default("[]") // JSON array
    val geoRestrictionEnabled: Column<Boolean> = bool("geo_restriction_enabled").default(false)
    val vpnAllowed: Column<Boolean> = bool("vpn_allowed").default(false)
    val torAllowed: Column<Boolean> = bool("tor_allowed").default(false)
    val proxyAllowed: Column<Boolean> = bool("proxy_allowed").default(false)
    
    // Time-Based Restrictions
    val timeRestrictionEnabled: Column<Boolean> = bool("time_restriction_enabled").default(false)
    val allowedTimeZones: Column<String> = text("allowed_time_zones").default("[]") // JSON array
    val allowedHours: Column<String> = text("allowed_hours").default("[]") // JSON array [0-23]
    val allowedDaysOfWeek: Column<String> = text("allowed_days_of_week").default("[]") // JSON array [0-6]
    val businessHoursOnly: Column<Boolean> = bool("business_hours_only").default(false)
    val businessHoursStart: Column<String?> = varchar("business_hours_start", 5).nullable() // HH:MM
    val businessHoursEnd: Column<String?> = varchar("business_hours_end", 5).nullable() // HH:MM
    
    // Encryption and Security
    val encryptionEnabled: Column<Boolean> = bool("encryption_enabled").default(true)
    val encryptionAlgorithm: Column<String> = varchar("encryption_algorithm", 20).default("AES256")
    val encryptionKeyId: Column<String?> = varchar("encryption_key_id", 128).nullable()
    val saltValue: Column<String?> = varchar("salt_value", 256).nullable()
    val hashAlgorithm: Column<String> = varchar("hash_algorithm", 20).default("PBKDF2")
    val hashIterations: Column<Int> = integer("hash_iterations").default(100000)
    val dataIntegrityCheck: Column<String?> = varchar("data_integrity_check", 256).nullable()
    
    // Compliance and Audit
    val complianceLevel: Column<String> = varchar("compliance_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, CRITICAL
    val auditLevel: Column<String> = varchar("audit_level", 20).default("NORMAL") // MINIMAL, NORMAL, DETAILED, FULL
    val dataRetentionDays: Column<Int> = integer("data_retention_days").default(2555) // 7 years
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("NORMAL")
    val personalDataIncluded: Column<Boolean> = bool("personal_data_included").default(true)
    val gdprCompliant: Column<Boolean> = bool("gdpr_compliant").default(true)
    val rightToDelete: Column<Boolean> = bool("right_to_delete").default(true)
    
    // User Experience and Preferences
    val userFriendlyName: Column<String?> = varchar("user_friendly_name", 200).nullable()
    val description: Column<String?> = text("description").nullable()
    val instructions: Column<String?> = text("instructions").nullable()
    val iconUrl: Column<String?> = varchar("icon_url", 500).nullable()
    val color: Column<String?> = varchar("color", 7).nullable() // Hex color
    val isHidden: Column<Boolean> = bool("is_hidden").default(false)
    val sortOrder: Column<Int> = integer("sort_order").default(0)
    val showInRecoveryList: Column<Boolean> = bool("show_in_recovery_list").default(true)
    
    // Notifications and Alerts
    val notifyOnUsage: Column<Boolean> = bool("notify_on_usage").default(true)
    val notifyOnFailure: Column<Boolean> = bool("notify_on_failure").default(true)
    val notifyOnLockout: Column<Boolean> = bool("notify_on_lockout").default(true)
    val notifyOnVerification: Column<Boolean> = bool("notify_on_verification").default(true)
    val notificationPreferences: Column<String> = text("notification_preferences").default("{}") // JSON object
    val alertThresholds: Column<String> = text("alert_thresholds").default("{}") // JSON object
    val escalationPath: Column<String> = text("escalation_path").default("[]") // JSON array
    
    // Error Tracking and Monitoring
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastError: Column<String?> = text("last_error").nullable()
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val errorThreshold: Column<Int> = integer("error_threshold").default(10)
    val healthStatus: Column<String> = varchar("health_status", 20).default("HEALTHY") // HEALTHY, WARNING, ERROR, CRITICAL
    val monitoringEnabled: Column<Boolean> = bool("monitoring_enabled").default(true)
    val performanceMetrics: Column<String> = text("performance_metrics").default("{}") // JSON object
    
    // Integration and API Access
    val apiAccessEnabled: Column<Boolean> = bool("api_access_enabled").default(false)
    val webhookUrl: Column<String?> = varchar("webhook_url", 500).nullable()
    val webhookSecret: Column<String?> = varchar("webhook_secret", 256).nullable()
    val webhookEvents: Column<String> = text("webhook_events").default("[]") // JSON array
    val integrationData: Column<String> = text("integration_data").default("{}") // JSON object
    val thirdPartyProvider: Column<String?> = varchar("third_party_provider", 100).nullable()
    val providerSettings: Column<String> = text("provider_settings").default("{}") // JSON object
    
    // Recovery Templates and Automation
    val useRecoveryTemplate: Column<Boolean> = bool("use_recovery_template").default(false)
    val recoveryTemplateId: Column<String?> = varchar("recovery_template_id", 128).nullable()
    val automationRules: Column<String> = text("automation_rules").default("[]") // JSON array
    val workflowId: Column<String?> = varchar("workflow_id", 128).nullable()
    val recoveryScript: Column<String?> = text("recovery_script").nullable()
    val customParameters: Column<String> = text("custom_parameters").default("{}") // JSON object
    
    // Custom Attributes and Extensions
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON object
    val metadata: Column<String> = text("metadata").default("{}") // JSON object
    val extensions: Column<String> = text("extensions").default("{}") // JSON object
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val notes: Column<String?> = text("notes").nullable()
    val internalNotes: Column<String?> = text("internal_notes").nullable()
    
    // Lifecycle Management
    val expiresAt: Column<Instant?> = timestamp("expires_at").nullable()
    val autoRenew: Column<Boolean> = bool("auto_renew").default(true)
    val renewalNotificationSent: Column<Boolean> = bool("renewal_notification_sent").default(false)
    val gracePeriodDays: Column<Int> = integer("grace_period_days").default(30)
    val deactivatedAt: Column<Instant?> = timestamp("deactivated_at").nullable()
    val deactivationReason: Column<String?> = varchar("deactivation_reason", 500).nullable()
    val reactivationCount: Column<Int> = integer("reactivation_count").default(0)
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // Soft Delete
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
}
