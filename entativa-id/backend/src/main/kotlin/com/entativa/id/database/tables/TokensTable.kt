package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Tokens Table Definition for Entativa ID
 * Manages all types of tokens including OAuth, JWT, session, and security tokens
 * 
 * @author Neo Qiss
 * @status Production-ready token management with enterprise security and tracking
 */
object TokensTable : UUIDTable("tokens") {
    
    // Core Token Information
    val tokenId: Column<String> = varchar("token_id", 128).uniqueIndex()
    val tokenType: Column<String> = varchar("token_type", 30) // ACCESS, REFRESH, ID, AUTHORIZATION_CODE, DEVICE_CODE, etc.
    val tokenValue: Column<String?> = text("token_value").nullable() // Hashed for security
    val tokenHash: Column<String> = varchar("token_hash", 256).index()
    val jti: Column<String?> = varchar("jti", 128).nullable() // JWT ID for JWT tokens
    
    // Token Ownership & Context
    val userId: Column<String?> = varchar("user_id", 100).nullable().index()
    val clientId: Column<String> = varchar("client_id", 128).index()
    val sessionId: Column<String?> = varchar("session_id", 128).nullable().index()
    val deviceId: Column<String?> = varchar("device_id", 128).nullable()
    val parentTokenId: Column<String?> = varchar("parent_token_id", 128).nullable() // For token families
    
    // Token Lifecycle
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, EXPIRED, REVOKED, USED, SUSPENDED
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isRevoked: Column<Boolean> = bool("is_revoked").default(false)
    val revokedAt: Column<Instant?> = timestamp("revoked_at").nullable()
    val revokedBy: Column<String?> = varchar("revoked_by", 100).nullable()
    val revocationReason: Column<String?> = varchar("revocation_reason", 200).nullable()
    
    // Token Timing
    val issuedAt: Column<Instant> = timestamp("issued_at").default(Instant.now())
    val expiresAt: Column<Instant> = timestamp("expires_at")
    val notBefore: Column<Instant?> = timestamp("not_before").nullable()
    val lastUsedAt: Column<Instant?> = timestamp("last_used_at").nullable()
    val gracePeriodExpiresAt: Column<Instant?> = timestamp("grace_period_expires_at").nullable()
    
    // OAuth2 Specific
    val scopes: Column<String> = text("scopes").default("[]") // JSON array of granted scopes
    val audience: Column<String> = text("audience").default("[]") // JSON array of intended audiences
    val grantType: Column<String?> = varchar("grant_type", 50).nullable()
    val responseType: Column<String?> = varchar("response_type", 50).nullable()
    val redirectUri: Column<String?> = varchar("redirect_uri", 500).nullable()
    val state: Column<String?> = varchar("state", 512).nullable()
    val nonce: Column<String?> = varchar("nonce", 512).nullable()
    
    // PKCE (Proof Key for Code Exchange)
    val codeChallenge: Column<String?> = varchar("code_challenge", 128).nullable()
    val codeChallengeMethod: Column<String?> = varchar("code_challenge_method", 10).nullable() // S256, plain
    val codeVerifier: Column<String?> = varchar("code_verifier", 128).nullable()
    
    // Token Format & Encryption
    val tokenFormat: Column<String> = varchar("token_format", 20).default("JWT") // JWT, OPAQUE, BEARER
    val algorithm: Column<String?> = varchar("algorithm", 20).nullable() // RS256, HS256, etc.
    val keyId: Column<String?> = varchar("key_id", 100).nullable()
    val isEncrypted: Column<Boolean> = bool("is_encrypted").default(false)
    val encryptionAlgorithm: Column<String?> = varchar("encryption_algorithm", 20).nullable()
    
    // Token Usage & Analytics
    val useCount: Column<Int> = integer("use_count").default(0)
    val maxUses: Column<Int?> = integer("max_uses").nullable() // For single-use tokens
    val isReusable: Column<Boolean> = bool("is_reusable").default(true)
    val lastAccessIP: Column<String?> = varchar("last_access_ip", 45).nullable()
    val lastUserAgent: Column<String?> = text("last_user_agent").nullable()
    
    // Token Family & Rotation
    val tokenFamily: Column<String?> = varchar("token_family", 128).nullable()
    val generationNumber: Column<Int> = integer("generation_number").default(1)
    val rotationEnabled: Column<Boolean> = bool("rotation_enabled").default(false)
    val rotatedFromId: Column<String?> = varchar("rotated_from_id", 128).nullable()
    val rotatedToId: Column<String?> = varchar("rotated_to_id", 128).nullable()
    val rotatedAt: Column<Instant?> = timestamp("rotated_at").nullable()
    
    // Security & Fraud Detection
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudFlags: Column<String> = text("fraud_flags").default("[]") // JSON array
    val suspiciousActivity: Column<Boolean> = bool("suspicious_activity").default(false)
    val geolocationRestricted: Column<Boolean> = bool("geolocation_restricted").default(false)
    val allowedCountries: Column<String> = text("allowed_countries").default("[]") // JSON array
    
    // Device & Platform Context
    val platformType: Column<String?> = varchar("platform_type", 50).nullable() // WEB, MOBILE, DESKTOP, API
    val platformVersion: Column<String?> = varchar("platform_version", 50).nullable()
    val deviceFingerprint: Column<String?> = varchar("device_fingerprint", 256).nullable()
    val deviceTrusted: Column<Boolean> = bool("device_trusted").default(false)
    val requiresDeviceAuth: Column<Boolean> = bool("requires_device_auth").default(false)
    
    // Multi-Factor Authentication
    val mfaRequired: Column<Boolean> = bool("mfa_required").default(false)
    val mfaVerified: Column<Boolean> = bool("mfa_verified").default(false)
    val mfaMethod: Column<String?> = varchar("mfa_method", 50).nullable()
    val mfaVerifiedAt: Column<Instant?> = timestamp("mfa_verified_at").nullable()
    val amr: Column<String> = text("amr").default("[]") // Authentication Methods References (JSON array)
    val acr: Column<String?> = varchar("acr", 50).nullable() // Authentication Context Class Reference
    
    // Claims & Attributes
    val customClaims: Column<String> = text("custom_claims").default("{}") // JSON object
    val userAttributes: Column<String> = text("user_attributes").default("{}") // JSON object
    val clientAttributes: Column<String> = text("client_attributes").default("{}") // JSON object
    val contextAttributes: Column<String> = text("context_attributes").default("{}") // JSON object
    
    // Consent & Privacy
    val consentGiven: Column<Boolean> = bool("consent_given").default(false)
    val consentId: Column<String?> = varchar("consent_id", 128).nullable()
    val consentScopes: Column<String> = text("consent_scopes").default("[]") // JSON array
    val consentTimestamp: Column<Instant?> = timestamp("consent_timestamp").nullable()
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("NORMAL")
    
    // Token Validation
    val issuer: Column<String?> = varchar("issuer", 500).nullable()
    val subject: Column<String?> = varchar("subject", 256).nullable()
    val authTime: Column<Instant?> = timestamp("auth_time").nullable()
    val sessionState: Column<String?> = varchar("session_state", 256).nullable()
    val azp: Column<String?> = varchar("azp", 128).nullable() // Authorized party
    
    // Rate Limiting & Throttling
    val rateLimitRemaining: Column<Int?> = integer("rate_limit_remaining").nullable()
    val rateLimitReset: Column<Instant?> = timestamp("rate_limit_reset").nullable()
    val throttled: Column<Boolean> = bool("throttled").default(false)
    val throttleReason: Column<String?> = varchar("throttle_reason", 200).nullable()
    
    // Cross-Platform Sync
    val syncStatus: Column<String> = varchar("sync_status", 20).default("NOT_APPLICABLE") // SYNCED, PENDING, FAILED, NOT_APPLICABLE
    val syncedPlatforms: Column<String> = text("synced_platforms").default("[]") // JSON array
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncFailureReason: Column<String?> = text("sync_failure_reason").nullable()
    
    // Token Metadata & Extensions
    val metadata: Column<String> = text("metadata").default("{}") // JSON object for extensible metadata
    val extensions: Column<String> = text("extensions").default("{}") // JSON object for custom extensions
    val tags: Column<String> = text("tags").default("[]") // JSON array for categorization
    val notes: Column<String?> = text("notes").nullable()
    
    // Compliance & Audit
    val auditLevel: Column<String> = varchar("audit_level", 20).default("STANDARD") // MINIMAL, STANDARD, DETAILED, FULL
    val complianceFlags: Column<String> = text("compliance_flags").default("[]") // JSON array
    val dataClassification: Column<String> = varchar("data_classification", 20).default("INTERNAL")
    val retentionPolicy: Column<String> = varchar("retention_policy", 50).default("STANDARD")
    
    // Performance & Optimization
    val cacheEnabled: Column<Boolean> = bool("cache_enabled").default(true)
    val cacheKey: Column<String?> = varchar("cache_key", 256).nullable()
    val cacheExpiresAt: Column<Instant?> = timestamp("cache_expires_at").nullable()
    val compressionEnabled: Column<Boolean> = bool("compression_enabled").default(false)
    val optimizationLevel: Column<String> = varchar("optimization_level", 20).default("STANDARD")
    
    // Error Tracking
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastError: Column<String?> = text("last_error").nullable()
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val healthStatus: Column<String> = varchar("health_status", 20).default("HEALTHY") // HEALTHY, WARNING, ERROR
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // IP & Location Tracking
    val issueIP: Column<String?> = varchar("issue_ip", 45).nullable()
    val issueCountry: Column<String?> = varchar("issue_country", 3).nullable()
    val issueRegion: Column<String?> = varchar("issue_region", 100).nullable()
    val issueCity: Column<String?> = varchar("issue_city", 100).nullable()
    val isVPN: Column<Boolean> = bool("is_vpn").default(false)
    val isTor: Column<Boolean> = bool("is_tor").default(false)
    val isProxy: Column<Boolean> = bool("is_proxy").default(false)
}
