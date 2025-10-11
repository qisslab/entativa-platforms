package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * MFA Methods Table Definition for Entativa ID
 * Manages multi-factor authentication methods including TOTP, SMS, email, biometrics, and backup codes
 * 
 * @author Neo Qiss
 * @status Production-ready MFA management with enterprise security features
 */
object MFAMethodsTable : UUIDTable("mfa_methods") {
    
    // Core MFA Information
    val userId: Column<String> = varchar("user_id", 100).index()
    val methodType: Column<String> = varchar("method_type", 30) // TOTP, SMS, EMAIL, BIOMETRIC, BACKUP_CODES, HARDWARE_KEY, PUSH
    val methodName: Column<String> = varchar("method_name", 200) // User-friendly name
    val methodIdentifier: Column<String> = varchar("method_identifier", 500) // Phone, email, device ID, etc.
    
    // Method Status & Security
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    val isPrimary: Column<Boolean> = bool("is_primary").default(false)
    val isBackup: Column<Boolean> = bool("is_backup").default(false)
    val trustLevel: Column<String> = varchar("trust_level", 20).default("STANDARD") // LOW, STANDARD, HIGH, VERIFIED
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    
    // TOTP/Authenticator App Settings
    val totpSecret: Column<String?> = varchar("totp_secret", 512).nullable() // Base32 encoded secret
    val totpAlgorithm: Column<String> = varchar("totp_algorithm", 10).default("SHA1") // SHA1, SHA256, SHA512
    val totpDigits: Column<Int> = integer("totp_digits").default(6)
    val totpPeriod: Column<Int> = integer("totp_period").default(30) // seconds
    val totpIssuer: Column<String> = varchar("totp_issuer", 100).default("Entativa ID")
    val qrCodeGenerated: Column<Boolean> = bool("qr_code_generated").default(false)
    val qrCodeUrl: Column<String?> = text("qr_code_url").nullable()
    
    // SMS/Phone Settings
    val phoneNumber: Column<String?> = varchar("phone_number", 20).nullable()
    val phoneCountryCode: Column<String?> = varchar("phone_country_code", 5).nullable()
    val phoneVerified: Column<Boolean> = bool("phone_verified").default(false)
    val phoneVerifiedAt: Column<Instant?> = timestamp("phone_verified_at").nullable()
    val smsProvider: Column<String?> = varchar("sms_provider", 50).nullable()
    val smsDeliveryStatus: Column<String> = varchar("sms_delivery_status", 20).default("PENDING")
    
    // Email Settings
    val emailAddress: Column<String?> = varchar("email_address", 254).nullable()
    val emailVerified: Column<Boolean> = bool("email_verified").default(false)
    val emailVerifiedAt: Column<Instant?> = timestamp("email_verified_at").nullable()
    val emailProvider: Column<String?> = varchar("email_provider", 50).nullable()
    val emailDeliveryStatus: Column<String> = varchar("email_delivery_status", 20).default("PENDING")
    
    // Biometric Settings
    val biometricType: Column<String?> = varchar("biometric_type", 30).nullable() // FINGERPRINT, FACE_ID, VOICE, IRIS, PALM
    val biometricDeviceId: Column<String?> = varchar("biometric_device_id", 128).nullable()
    val biometricTemplateId: Column<String?> = varchar("biometric_template_id", 128).nullable()
    val biometricQuality: Column<Double?> = double("biometric_quality").nullable()
    val biometricEnrollmentDate: Column<Instant?> = timestamp("biometric_enrollment_date").nullable()
    val biometricLastUsed: Column<Instant?> = timestamp("biometric_last_used").nullable()
    
    // Hardware Key Settings (FIDO2/WebAuthn)
    val hardwareKeyId: Column<String?> = varchar("hardware_key_id", 256).nullable()
    val hardwareKeyType: Column<String?> = varchar("hardware_key_type", 50).nullable() // YUBIKEY, TITAN, SOLOKEY, etc.
    val credentialId: Column<String?> = text("credential_id").nullable() // Base64 encoded
    val publicKey: Column<String?> = text("public_key").nullable() // Public key for verification
    val attestationObject: Column<String?> = text("attestation_object").nullable()
    val clientDataJSON: Column<String?> = text("client_data_json").nullable()
    val signatureCounter: Column<Long> = long("signature_counter").default(0)
    
    // Push Notification Settings
    val pushToken: Column<String?> = text("push_token").nullable()
    val pushProvider: Column<String?> = varchar("push_provider", 30).nullable() // FCM, APNS, WNS
    val devicePlatform: Column<String?> = varchar("device_platform", 20).nullable() // ANDROID, IOS, WINDOWS
    val appId: Column<String?> = varchar("app_id", 100).nullable()
    val pushEndpoint: Column<String?> = text("push_endpoint").nullable()
    val pushKeys: Column<String?> = text("push_keys").nullable() // JSON with p256dh and auth keys
    
    // Backup Codes
    val backupCodes: Column<String?> = text("backup_codes").nullable() // JSON array of hashed codes
    val backupCodesGenerated: Column<Boolean> = bool("backup_codes_generated").default(false)
    val backupCodesUsedCount: Column<Int> = integer("backup_codes_used_count").default(0)
    val backupCodesLastRegenerated: Column<Instant?> = timestamp("backup_codes_last_regenerated").nullable()
    val usedBackupCodes: Column<String> = text("used_backup_codes").default("[]") // JSON array of used codes
    
    // Usage Statistics & Analytics
    val totalUses: Column<Long> = long("total_uses").default(0)
    val successfulUses: Column<Long> = long("successful_uses").default(0)
    val failedUses: Column<Long> = long("failed_uses").default(0)
    val lastUsedAt: Column<Instant?> = timestamp("last_used_at").nullable()
    val lastSuccessAt: Column<Instant?> = timestamp("last_success_at").nullable()
    val lastFailureAt: Column<Instant?> = timestamp("last_failure_at").nullable()
    val consecutiveFailures: Column<Int> = integer("consecutive_failures").default(0)
    
    // Rate Limiting & Security
    val rateLimitCount: Column<Int> = integer("rate_limit_count").default(0)
    val rateLimitWindow: Column<Instant?> = timestamp("rate_limit_window").nullable()
    val isLocked: Column<Boolean> = bool("is_locked").default(false)
    val lockedUntil: Column<Instant?> = timestamp("locked_until").nullable()
    val lockReason: Column<String?> = varchar("lock_reason", 200).nullable()
    val maxDailyUses: Column<Int> = integer("max_daily_uses").default(100)
    val dailyUseCount: Column<Int> = integer("daily_use_count").default(0)
    val dailyCountReset: Column<Instant?> = timestamp("daily_count_reset").nullable()
    
    // Device & Location Tracking
    val registeredFromIP: Column<String?> = varchar("registered_from_ip", 45).nullable()
    val registeredFromCountry: Column<String?> = varchar("registered_from_country", 3).nullable()
    val lastUsedFromIP: Column<String?> = varchar("last_used_from_ip", 45).nullable()
    val lastUsedFromCountry: Column<String?> = varchar("last_used_from_country", 3).nullable()
    val allowedCountries: Column<String> = text("allowed_countries").default("[]") // JSON array
    val blockedCountries: Column<String> = text("blocked_countries").default("[]") // JSON array
    val geoRestrictionEnabled: Column<Boolean> = bool("geo_restriction_enabled").default(false)
    
    // Device Information
    val deviceName: Column<String?> = varchar("device_name", 200).nullable()
    val deviceFingerprint: Column<String?> = varchar("device_fingerprint", 256).nullable()
    val userAgent: Column<String?> = text("user_agent").nullable()
    val operatingSystem: Column<String?> = varchar("operating_system", 100).nullable()
    val browserInfo: Column<String?> = varchar("browser_info", 200).nullable()
    val deviceTrusted: Column<Boolean> = bool("device_trusted").default(false)
    val deviceTrustedAt: Column<Instant?> = timestamp("device_trusted_at").nullable()
    
    // Verification & Enrollment
    val verificationCode: Column<String?> = varchar("verification_code", 20).nullable()
    val verificationCodeExpiresAt: Column<Instant?> = timestamp("verification_code_expires_at").nullable()
    val verificationAttempts: Column<Int> = integer("verification_attempts").default(0)
    val maxVerificationAttempts: Column<Int> = integer("max_verification_attempts").default(5)
    val enrollmentToken: Column<String?> = varchar("enrollment_token", 255).nullable()
    val enrollmentExpiresAt: Column<Instant?> = timestamp("enrollment_expires_at").nullable()
    
    // Recovery & Backup
    val hasRecoveryMethod: Column<Boolean> = bool("has_recovery_method").default(false)
    val recoveryMethodId: Column<String?> = varchar("recovery_method_id", 128).nullable()
    val canBeUsedForRecovery: Column<Boolean> = bool("can_be_used_for_recovery").default(true)
    val recoveryPriority: Column<Int> = integer("recovery_priority").default(1) // Lower numbers = higher priority
    val lastRecoveryUse: Column<Instant?> = timestamp("last_recovery_use").nullable()
    
    // Compliance & Audit
    val requiresCompliance: Column<Boolean> = bool("requires_compliance").default(false)
    val complianceLevel: Column<String> = varchar("compliance_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, CRITICAL
    val auditLevel: Column<String> = varchar("audit_level", 20).default("NORMAL") // MINIMAL, NORMAL, DETAILED, FULL
    val dataRetentionDays: Column<Int> = integer("data_retention_days").default(2555) // 7 years
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("NORMAL")
    
    // Method-Specific Configuration
    val configurationData: Column<String> = text("configuration_data").default("{}") // JSON for method-specific settings
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON for extensible attributes
    val providerSettings: Column<String> = text("provider_settings").default("{}") // JSON for provider-specific config
    val featureFlags: Column<String> = text("feature_flags").default("{}") // JSON for feature toggles
    
    // Integration & Sync
    val syncedToPlatforms: Column<String> = text("synced_to_platforms").default("[]") // JSON array
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncFailureReason: Column<String?> = text("sync_failure_reason").nullable()
    val crossPlatformEnabled: Column<Boolean> = bool("cross_platform_enabled").default(true)
    
    // Performance & Optimization
    val cacheEnabled: Column<Boolean> = bool("cache_enabled").default(true)
    val cacheExpiresAt: Column<Instant?> = timestamp("cache_expires_at").nullable()
    val optimizationLevel: Column<String> = varchar("optimization_level", 20).default("STANDARD")
    val precomputedData: Column<String?> = text("precomputed_data").nullable() // JSON for performance optimization
    
    // Error Tracking & Monitoring
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastError: Column<String?> = text("last_error").nullable()
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val healthStatus: Column<String> = varchar("health_status", 20).default("HEALTHY") // HEALTHY, WARNING, ERROR, CRITICAL
    val monitoringEnabled: Column<Boolean> = bool("monitoring_enabled").default(true)
    
    // Lifecycle Management
    val activatedAt: Column<Instant?> = timestamp("activated_at").nullable()
    val deactivatedAt: Column<Instant?> = timestamp("deactivated_at").nullable()
    val expiresAt: Column<Instant?> = timestamp("expires_at").nullable()
    val autoRenew: Column<Boolean> = bool("auto_renew").default(true)
    val renewalNoticeSent: Column<Boolean> = bool("renewal_notice_sent").default(false)
    val gracePeriodDays: Column<Int> = integer("grace_period_days").default(7)
    
    // User Experience
    val userFriendlyName: Column<String?> = varchar("user_friendly_name", 200).nullable()
    val description: Column<String?> = text("description").nullable()
    val iconUrl: Column<String?> = varchar("icon_url", 500).nullable()
    val color: Column<String?> = varchar("color", 7).nullable() // Hex color
    val sortOrder: Column<Int> = integer("sort_order").default(0)
    val isHidden: Column<Boolean> = bool("is_hidden").default(false)
    
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
