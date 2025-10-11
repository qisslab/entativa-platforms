package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Sessions Table Definition for Entativa ID
 * Manages user sessions across all platforms with advanced security and device tracking
 * 
 * @author Neo Qiss
 * @status Production-ready session management with enterprise security features
 */
object SessionsTable : UUIDTable("sessions") {
    
    // Core Session Information
    val sessionId: Column<String> = varchar("session_id", 128).uniqueIndex()
    val userId: Column<String> = varchar("user_id", 100).index()
    val platformId: Column<String> = varchar("platform_id", 50).index()
    val sessionType: Column<String> = varchar("session_type", 20).default("WEB") // WEB, MOBILE, DESKTOP, API, SERVICE
    
    // Session State
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, EXPIRED, TERMINATED, SUSPENDED
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isPersistent: Column<Boolean> = bool("is_persistent").default(false)
    val isSecure: Column<Boolean> = bool("is_secure").default(true)
    val requiresMFA: Column<Boolean> = bool("requires_mfa").default(false)
    val mfaVerified: Column<Boolean> = bool("mfa_verified").default(false)
    val mfaVerifiedAt: Column<Instant?> = timestamp("mfa_verified_at").nullable()
    
    // Session Timing
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val expiresAt: Column<Instant> = timestamp("expires_at")
    val lastActivityAt: Column<Instant> = timestamp("last_activity_at").default(Instant.now())
    val terminatedAt: Column<Instant?> = timestamp("terminated_at").nullable()
    val maxInactiveMinutes: Column<Int> = integer("max_inactive_minutes").default(480) // 8 hours
    val absoluteTimeoutMinutes: Column<Int> = integer("absolute_timeout_minutes").default(1440) // 24 hours
    
    // Device & Client Information
    val deviceId: Column<String?> = varchar("device_id", 128).nullable()
    val deviceFingerprint: Column<String?> = varchar("device_fingerprint", 256).nullable()
    val deviceName: Column<String?> = varchar("device_name", 200).nullable()
    val deviceType: Column<String?> = varchar("device_type", 50).nullable() // MOBILE, TABLET, DESKTOP, SMART_TV, CONSOLE
    val operatingSystem: Column<String?> = varchar("operating_system", 100).nullable()
    val browser: Column<String?> = varchar("browser", 100).nullable()
    val userAgent: Column<String?> = text("user_agent").nullable()
    val appVersion: Column<String?> = varchar("app_version", 50).nullable()
    
    // Network & Location
    val ipAddress: Column<String> = varchar("ip_address", 45) // IPv6 support
    val ipCountry: Column<String?> = varchar("ip_country", 3).nullable()
    val ipRegion: Column<String?> = varchar("ip_region", 100).nullable()
    val ipCity: Column<String?> = varchar("ip_city", 100).nullable()
    val ipISP: Column<String?> = varchar("ip_isp", 200).nullable()
    val isVPN: Column<Boolean> = bool("is_vpn").default(false)
    val isTor: Column<Boolean> = bool("is_tor").default(false)
    val isProxy: Column<Boolean> = bool("is_proxy").default(false)
    val geoLocation: Column<String?> = varchar("geo_location", 100).nullable() // lat,lng
    val timezone: Column<String?> = varchar("timezone", 50).nullable()
    
    // Security & Trust
    val trustLevel: Column<String> = varchar("trust_level", 20).default("NORMAL") // LOW, NORMAL, HIGH, VERIFIED
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudFlags: Column<String> = text("fraud_flags").default("[]") // JSON array
    val securityFlags: Column<String> = text("security_flags").default("[]") // JSON array
    val isTrustedDevice: Column<Boolean> = bool("is_trusted_device").default(false)
    val deviceTrustedAt: Column<Instant?> = timestamp("device_trusted_at").nullable()
    val concurrentSessions: Column<Int> = integer("concurrent_sessions").default(1)
    
    // Authentication Details
    val authenticationMethod: Column<String> = varchar("authentication_method", 50).default("PASSWORD")
    val authenticationFactors: Column<String> = text("authentication_factors").default("[]") // JSON array
    val biometricUsed: Column<Boolean> = bool("biometric_used").default(false)
    val biometricType: Column<String?> = varchar("biometric_type", 20).nullable()
    val ssoProvider: Column<String?> = varchar("sso_provider", 50).nullable()
    val ssoSessionId: Column<String?> = varchar("sso_session_id", 256).nullable()
    
    // Session Tokens
    val accessToken: Column<String?> = varchar("access_token", 512).nullable()
    val refreshToken: Column<String?> = varchar("refresh_token", 512).nullable()
    val csrfToken: Column<String?> = varchar("csrf_token", 256).nullable()
    val tokenHash: Column<String?> = varchar("token_hash", 256).nullable()
    val accessTokenExpiresAt: Column<Instant?> = timestamp("access_token_expires_at").nullable()
    val refreshTokenExpiresAt: Column<Instant?> = timestamp("refresh_token_expires_at").nullable()
    
    // Cross-Platform Sync
    val isMasterSession: Column<Boolean> = bool("is_master_session").default(false)
    val parentSessionId: Column<String?> = varchar("parent_session_id", 128).nullable()
    val linkedSessions: Column<String> = text("linked_sessions").default("[]") // JSON array of session IDs
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val crossPlatformEnabled: Column<Boolean> = bool("cross_platform_enabled").default(true)
    
    // Activity Tracking
    val requestCount: Column<Long> = long("request_count").default(0)
    val lastRequestAt: Column<Instant?> = timestamp("last_request_at").nullable()
    val lastPageView: Column<String?> = varchar("last_page_view", 500).nullable()
    val lastAction: Column<String?> = varchar("last_action", 200).nullable()
    val totalDurationSeconds: Column<Long> = long("total_duration_seconds").default(0)
    val idleTimeSeconds: Column<Long> = long("idle_time_seconds").default(0)
    
    // Session Features
    val rememberDevice: Column<Boolean> = bool("remember_device").default(false)
    val autoLogin: Column<Boolean> = bool("auto_login").default(false)
    val extendOnActivity: Column<Boolean> = bool("extend_on_activity").default(true)
    val strictSecurity: Column<Boolean> = bool("strict_security").default(false)
    val allowConcurrent: Column<Boolean> = bool("allow_concurrent").default(true)
    val singleSignOn: Column<Boolean> = bool("single_sign_on").default(true)
    
    // Termination & Cleanup
    val terminationReason: Column<String?> = varchar("termination_reason", 200).nullable()
    val terminatedBy: Column<String?> = varchar("terminated_by", 100).nullable() // USER, SYSTEM, ADMIN, SECURITY
    val autoTerminated: Column<Boolean> = bool("auto_terminated").default(false)
    val forceTerminated: Column<Boolean> = bool("force_terminated").default(false)
    val cleanupCompleted: Column<Boolean> = bool("cleanup_completed").default(false)
    val cleanupAt: Column<Instant?> = timestamp("cleanup_at").nullable()
    
    // Compliance & Audit
    val dataRetention: Column<String> = varchar("data_retention", 20).default("STANDARD") // MINIMAL, STANDARD, EXTENDED
    val auditLevel: Column<String> = varchar("audit_level", 20).default("NORMAL") // MINIMAL, NORMAL, DETAILED, FULL
    val privacyMode: Column<Boolean> = bool("privacy_mode").default(false)
    val loggingEnabled: Column<Boolean> = bool("logging_enabled").default(true)
    val analyticsEnabled: Column<Boolean> = bool("analytics_enabled").default(true)
    
    // Performance & Optimization
    val cacheEnabled: Column<Boolean> = bool("cache_enabled").default(true)
    val compressionEnabled: Column<Boolean> = bool("compression_enabled").default(true)
    val preloadEnabled: Column<Boolean> = bool("preload_enabled").default(true)
    val connectionPooled: Column<Boolean> = bool("connection_pooled").default(true)
    val lastOptimizedAt: Column<Instant?> = timestamp("last_optimized_at").nullable()
    
    // Custom Attributes
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON object
    val sessionMetadata: Column<String> = text("session_metadata").default("{}") // JSON object
    val platformSpecific: Column<String> = text("platform_specific").default("{}") // JSON object
    val featureFlags: Column<String> = text("feature_flags").default("{}") // JSON object
    
    // Version & Updates
    val version: Column<Long> = long("version").default(1)
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val schemaVersion: Column<Int> = integer("schema_version").default(1)
}
