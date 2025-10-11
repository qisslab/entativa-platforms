package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Users Table Definition for Entativa ID
 * Core user account storage with comprehensive security and profile management
 * 
 * @author Neo Qiss
 * @status Production-ready database schema with enterprise security features
 */
object UsersTable : UUIDTable("users") {
    
    // Basic Identity
    val entativaId: Column<String> = varchar("entativa_id", 32).uniqueIndex()
    val handle: Column<String> = varchar("handle", 64).uniqueIndex()
    val email: Column<String> = varchar("email", 254).uniqueIndex()
    val phoneNumber: Column<String?> = varchar("phone_number", 20).nullable()
    val alternateEmail: Column<String?> = varchar("alternate_email", 254).nullable()
    
    // Profile Information
    val firstName: Column<String> = varchar("first_name", 100)
    val lastName: Column<String> = varchar("last_name", 100)
    val displayName: Column<String> = varchar("display_name", 200)
    val bio: Column<String?> = text("bio").nullable()
    val location: Column<String?> = varchar("location", 100).nullable()
    val website: Column<String?> = varchar("website", 500).nullable()
    val timezone: Column<String> = varchar("timezone", 50).default("UTC")
    val language: Column<String> = varchar("language", 10).default("en")
    val dateOfBirth: Column<Instant?> = timestamp("date_of_birth").nullable()
    
    // Security Credentials
    val passwordHash: Column<String?> = varchar("password_hash", 255).nullable()
    val passwordSalt: Column<String?> = varchar("password_salt", 255).nullable()
    val passphraseHash: Column<String?> = varchar("passphrase_hash", 500).nullable()
    val passphraseSalt: Column<String?> = varchar("passphrase_salt", 255).nullable()
    val passwordChangedAt: Column<Instant?> = timestamp("password_changed_at").nullable()
    val mustChangePassword: Column<Boolean> = bool("must_change_password").default(false)
    
    // Multi-Factor Authentication
    val mfaEnabled: Column<Boolean> = bool("mfa_enabled").default(false)
    val mfaBackupCodes: Column<String?> = text("mfa_backup_codes").nullable()
    val biometricEnabled: Column<Boolean> = bool("biometric_enabled").default(false)
    
    // Account Status & Security
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    val isSuspended: Column<Boolean> = bool("is_suspended").default(false)
    val isLocked: Column<Boolean> = bool("is_locked").default(false)
    val lockReason: Column<String?> = varchar("lock_reason", 500).nullable()
    val lockedAt: Column<Instant?> = timestamp("locked_at").nullable()
    val lockedUntil: Column<Instant?> = timestamp("locked_until").nullable()
    
    // Verification Status
    val emailVerified: Column<Boolean> = bool("email_verified").default(false)
    val emailVerifiedAt: Column<Instant?> = timestamp("email_verified_at").nullable()
    val phoneVerified: Column<Boolean> = bool("phone_verified").default(false)
    val phoneVerifiedAt: Column<Instant?> = timestamp("phone_verified_at").nullable()
    val identityVerified: Column<Boolean> = bool("identity_verified").default(false)
    val identityVerifiedAt: Column<Instant?> = timestamp("identity_verified_at").nullable()
    
    // Privacy & Preferences
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("NORMAL") // PUBLIC, NORMAL, PRIVATE, RESTRICTED
    val profileVisibility: Column<String> = varchar("profile_visibility", 20).default("PUBLIC")
    val searchableByEmail: Column<Boolean> = bool("searchable_by_email").default(true)
    val searchableByPhone: Column<Boolean> = bool("searchable_by_phone").default(false)
    val allowDirectMessages: Column<Boolean> = bool("allow_direct_messages").default(true)
    val showOnlineStatus: Column<Boolean> = bool("show_online_status").default(true)
    
    // Notifications Preferences
    val emailNotifications: Column<Boolean> = bool("email_notifications").default(true)
    val smsNotifications: Column<Boolean> = bool("sms_notifications").default(false)
    val pushNotifications: Column<Boolean> = bool("push_notifications").default(true)
    val marketingEmails: Column<Boolean> = bool("marketing_emails").default(false)
    val securityAlerts: Column<Boolean> = bool("security_alerts").default(true)
    
    // Platform Integration
    val unifiedProfile: Column<Boolean> = bool("unified_profile").default(true)
    val crossPlatformSync: Column<Boolean> = bool("cross_platform_sync").default(true)
    val sharedIdentity: Column<Boolean> = bool("shared_identity").default(true)
    
    // Analytics & Tracking
    val lastLoginAt: Column<Instant?> = timestamp("last_login_at").nullable()
    val lastActiveAt: Column<Instant?> = timestamp("last_active_at").nullable()
    val loginCount: Column<Long> = long("login_count").default(0)
    val failedLoginAttempts: Column<Int> = integer("failed_login_attempts").default(0)
    val lastFailedLoginAt: Column<Instant?> = timestamp("last_failed_login_at").nullable()
    
    // Device & Session Management
    val maxActiveSessions: Column<Int> = integer("max_active_sessions").default(10)
    val sessionTimeoutMinutes: Column<Int> = integer("session_timeout_minutes").default(480) // 8 hours
    val trustedDevicesCount: Column<Int> = integer("trusted_devices_count").default(0)
    
    // Subscription & Billing
    val subscriptionTier: Column<String> = varchar("subscription_tier", 20).default("FREE") // FREE, PREMIUM, BUSINESS, ENTERPRISE
    val subscriptionStatus: Column<String> = varchar("subscription_status", 20).default("ACTIVE")
    val subscriptionExpiresAt: Column<Instant?> = timestamp("subscription_expires_at").nullable()
    val billingEmail: Column<String?> = varchar("billing_email", 254).nullable()
    
    // Compliance & Legal
    val termsAcceptedAt: Column<Instant?> = timestamp("terms_accepted_at").nullable()
    val privacyPolicyAcceptedAt: Column<Instant?> = timestamp("privacy_policy_accepted_at").nullable()
    val cookieConsentGiven: Column<Boolean> = bool("cookie_consent_given").default(false)
    val cookieConsentAt: Column<Instant?> = timestamp("cookie_consent_at").nullable()
    val gdprConsentGiven: Column<Boolean> = bool("gdpr_consent_given").default(false)
    val gdprConsentAt: Column<Instant?> = timestamp("gdpr_consent_at").nullable()
    val dataRetentionOptOut: Column<Boolean> = bool("data_retention_opt_out").default(false)
    
    // Avatar & Media
    val avatarUrl: Column<String?> = varchar("avatar_url", 500).nullable()
    val coverImageUrl: Column<String?> = varchar("cover_image_url", 500).nullable()
    val profileImageHash: Column<String?> = varchar("profile_image_hash", 64).nullable()
    
    // Security Metadata
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudFlags: Column<String?> = text("fraud_flags").nullable() // JSON array of flags
    val ipWhitelist: Column<String?> = text("ip_whitelist").nullable() // JSON array of IPs
    val deviceFingerprint: Column<String?> = varchar("device_fingerprint", 128).nullable()
    
    // Recovery & Backup
    val recoveryEmail: Column<String?> = varchar("recovery_email", 254).nullable()
    val recoveryPhone: Column<String?> = varchar("recovery_phone", 20).nullable()
    val emergencyContacts: Column<String?> = text("emergency_contacts").nullable() // JSON
    val accountRecoveryToken: Column<String?> = varchar("account_recovery_token", 255).nullable()
    val recoveryTokenExpiresAt: Column<Instant?> = timestamp("recovery_token_expires_at").nullable()
    
    // Audit & Compliance
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    val dataClassification: Column<String> = varchar("data_classification", 20).default("PERSONAL") // PUBLIC, INTERNAL, CONFIDENTIAL, PERSONAL
    
    // Soft Delete
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
    
    // Platform-Specific Metadata
    val platformMetadata: Column<String?> = text("platform_metadata").nullable() // JSON for platform-specific data
    val customAttributes: Column<String?> = text("custom_attributes").nullable() // JSON for extensible attributes
    val tags: Column<String?> = text("tags").nullable() // JSON array of user tags
    
    // Migration & Legacy Support
    val legacyUserId: Column<String?> = varchar("legacy_user_id", 100).nullable()
    val migrationSource: Column<String?> = varchar("migration_source", 50).nullable()
    val migrationDate: Column<Instant?> = timestamp("migration_date").nullable()
    val migrationData: Column<String?> = text("migration_data").nullable() // JSON
}
