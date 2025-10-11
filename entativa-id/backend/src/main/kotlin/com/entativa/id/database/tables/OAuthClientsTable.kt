package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * OAuth Clients Table Definition for Entativa ID
 * Manages OAuth2 client applications across all Entativa platforms and third-party integrations
 * 
 * @author Neo Qiss
 * @status Production-ready OAuth2 authorization server with enterprise security
 */
object OAuthClientsTable : UUIDTable("oauth_clients") {
    
    // Core Client Information
    val clientId: Column<String> = varchar("client_id", 128).uniqueIndex()
    val clientSecret: Column<String?> = varchar("client_secret", 512).nullable()
    val clientSecretHash: Column<String?> = varchar("client_secret_hash", 256).nullable()
    val clientName: Column<String> = varchar("client_name", 200)
    val clientDescription: Column<String?> = text("client_description").nullable()
    val clientType: Column<String> = varchar("client_type", 20).default("CONFIDENTIAL") // PUBLIC, CONFIDENTIAL
    
    // Application Details
    val applicationName: Column<String> = varchar("application_name", 200)
    val applicationUrl: Column<String?> = varchar("application_url", 500).nullable()
    val organizationName: Column<String?> = varchar("organization_name", 200).nullable()
    val organizationUrl: Column<String?> = varchar("organization_url", 500).nullable()
    val supportEmail: Column<String?> = varchar("support_email", 254).nullable()
    val privacyPolicyUrl: Column<String?> = varchar("privacy_policy_url", 500).nullable()
    val termsOfServiceUrl: Column<String?> = varchar("terms_of_service_url", 500).nullable()
    
    // Platform Integration
    val platformType: Column<String> = varchar("platform_type", 50).default("ENTATIVA") // ENTATIVA, THIRD_PARTY
    val entativaPlatform: Column<String?> = varchar("entativa_platform", 50).nullable() // GALA, PIKA, PLAYPODS, SONET
    val isFirstParty: Column<Boolean> = bool("is_first_party").default(false)
    val isTrusted: Column<Boolean> = bool("is_trusted").default(false)
    val trustLevel: Column<String> = varchar("trust_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, VERIFIED
    
    // OAuth2 Configuration
    val grantTypes: Column<String> = text("grant_types").default("[\"authorization_code\"]") // JSON array
    val responseTypes: Column<String> = text("response_types").default("[\"code\"]") // JSON array
    val scopes: Column<String> = text("scopes").default("[\"openid\",\"profile\",\"email\"]") // JSON array
    val defaultScopes: Column<String> = text("default_scopes").default("[\"openid\",\"profile\"]") // JSON array
    val allowedScopes: Column<String> = text("allowed_scopes").default("[\"openid\",\"profile\",\"email\"]") // JSON array
    
    // Redirect URIs & Security
    val redirectUris: Column<String> = text("redirect_uris") // JSON array - required
    val allowedOrigins: Column<String> = text("allowed_origins").default("[]") // JSON array for CORS
    val postLogoutRedirectUris: Column<String> = text("post_logout_redirect_uris").default("[]") // JSON array
    val allowWildcardRedirects: Column<Boolean> = bool("allow_wildcard_redirects").default(false)
    val requireExactRedirectMatch: Column<Boolean> = bool("require_exact_redirect_match").default(true)
    
    // Client Status & Lifecycle
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, INACTIVE, SUSPENDED, REVOKED
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isApproved: Column<Boolean> = bool("is_approved").default(false)
    val approvedBy: Column<String?> = varchar("approved_by", 100).nullable()
    val approvedAt: Column<Instant?> = timestamp("approved_at").nullable()
    val suspendedAt: Column<Instant?> = timestamp("suspended_at").nullable()
    val suspensionReason: Column<String?> = text("suspension_reason").nullable()
    
    // Security Settings
    val requirePKCE: Column<Boolean> = bool("require_pkce").default(true)
    val requireClientAuth: Column<Boolean> = bool("require_client_auth").default(true)
    val allowPlainTextPKCE: Column<Boolean> = bool("allow_plain_text_pkce").default(false)
    val requireSignedJWT: Column<Boolean> = bool("require_signed_jwt").default(false)
    val accessTokenFormat: Column<String> = varchar("access_token_format", 20).default("JWT") // JWT, OPAQUE
    val idTokenSigningAlg: Column<String> = varchar("id_token_signing_alg", 20).default("RS256")
    
    // Token Lifetimes (in seconds)
    val accessTokenLifetime: Column<Int> = integer("access_token_lifetime").default(3600) // 1 hour
    val refreshTokenLifetime: Column<Int> = integer("refresh_token_lifetime").default(2592000) // 30 days
    val authorizationCodeLifetime: Column<Int> = integer("authorization_code_lifetime").default(600) // 10 minutes
    val idTokenLifetime: Column<Int> = integer("id_token_lifetime").default(3600) // 1 hour
    val deviceCodeLifetime: Column<Int> = integer("device_code_lifetime").default(1800) // 30 minutes
    
    // Refresh Token Settings
    val allowRefreshTokenRotation: Column<Boolean> = bool("allow_refresh_token_rotation").default(true)
    val refreshTokenGracePeriod: Column<Int> = integer("refresh_token_grace_period").default(300) // 5 minutes
    val revokeRefreshTokenOnUse: Column<Boolean> = bool("revoke_refresh_token_on_use").default(false)
    val maxRefreshTokenAge: Column<Int> = integer("max_refresh_token_age").default(7776000) // 90 days
    
    // Rate Limiting & Throttling
    val rateLimit: Column<Int> = integer("rate_limit").default(1000) // requests per hour
    val burstLimit: Column<Int> = integer("burst_limit").default(100) // requests per minute
    val throttleEnabled: Column<Boolean> = bool("throttle_enabled").default(true)
    val currentUsage: Column<Long> = long("current_usage").default(0)
    val lastUsageReset: Column<Instant?> = timestamp("last_usage_reset").nullable()
    
    // Consent & User Experience
    val requireUserConsent: Column<Boolean> = bool("require_user_consent").default(true)
    val skipConsentForTrusted: Column<Boolean> = bool("skip_consent_for_trusted").default(false)
    val consentMessage: Column<String?> = text("consent_message").nullable()
    val logoUrl: Column<String?> = varchar("logo_url", 500).nullable()
    val clientUri: Column<String?> = varchar("client_uri", 500).nullable()
    val tosUri: Column<String?> = varchar("tos_uri", 500).nullable()
    val policyUri: Column<String?> = varchar("policy_uri", 500).nullable()
    
    // Analytics & Monitoring
    val totalAuthorizations: Column<Long> = long("total_authorizations").default(0)
    val totalTokensIssued: Column<Long> = long("total_tokens_issued").default(0)
    val totalActiveUsers: Column<Long> = long("total_active_users").default(0)
    val lastUsedAt: Column<Instant?> = timestamp("last_used_at").nullable()
    val monthlyActiveUsers: Column<Int> = integer("monthly_active_users").default(0)
    val peakConcurrentUsers: Column<Int> = integer("peak_concurrent_users").default(0)
    
    // Error Tracking & Debugging
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastError: Column<String?> = text("last_error").nullable()
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val debugMode: Column<Boolean> = bool("debug_mode").default(false)
    val logLevel: Column<String> = varchar("log_level", 10).default("INFO")
    
    // Webhook & Notifications
    val webhookUrl: Column<String?> = varchar("webhook_url", 500).nullable()
    val webhookSecret: Column<String?> = varchar("webhook_secret", 256).nullable()
    val webhookEvents: Column<String> = text("webhook_events").default("[]") // JSON array
    val notificationEmail: Column<String?> = varchar("notification_email", 254).nullable()
    val alertsEnabled: Column<Boolean> = bool("alerts_enabled").default(true)
    
    // API Keys & Additional Auth
    val apiKey: Column<String?> = varchar("api_key", 256).nullable()
    val apiKeyHash: Column<String?> = varchar("api_key_hash", 256).nullable()
    val publicKey: Column<String?> = text("public_key").nullable() // For JWT verification
    val jwksUri: Column<String?> = varchar("jwks_uri", 500).nullable()
    val additionalMetadata: Column<String> = text("additional_metadata").default("{}") // JSON
    
    // Advanced Features
    val supportsPAR: Column<Boolean> = bool("supports_par").default(false) // Pushed Authorization Requests
    val supportsMTLS: Column<Boolean> = bool("supports_mtls").default(false) // Mutual TLS
    val supportsDPOP: Column<Boolean> = bool("supports_dpop").default(false) // Demonstrating Proof-of-Possession
    val supportsJAR: Column<Boolean> = bool("supports_jar").default(false) // JWT Authorization Requests
    val deviceFlowEnabled: Column<Boolean> = bool("device_flow_enabled").default(false)
    
    // Compliance & Regulations
    val dataProcessingAgreement: Column<Boolean> = bool("data_processing_agreement").default(false)
    val gdprCompliant: Column<Boolean> = bool("gdpr_compliant").default(false)
    val ccpaCompliant: Column<Boolean> = bool("ccpa_compliant").default(false)
    val hipaaCompliant: Column<Boolean> = bool("hipaa_compliant").default(false)
    val sox compliance: Column<Boolean> = bool("sox_compliance").default(false)
    val dataRetentionDays: Column<Int> = integer("data_retention_days").default(365)
    
    // Developer Information
    val developerId: Column<String?> = varchar("developer_id", 100).nullable()
    val developerEmail: Column<String?> = varchar("developer_email", 254).nullable()
    val teamMembers: Column<String> = text("team_members").default("[]") // JSON array
    val technicalContact: Column<String?> = varchar("technical_contact", 254).nullable()
    val businessContact: Column<String?> = varchar("business_contact", 254).nullable()
    
    // Environment & Deployment
    val environment: Column<String> = varchar("environment", 20).default("PRODUCTION") // DEVELOPMENT, STAGING, PRODUCTION
    val version: Column<String> = varchar("version", 20).default("1.0.0")
    val deploymentRegion: Column<String?> = varchar("deployment_region", 50).nullable()
    val availabilityZones: Column<String> = text("availability_zones").default("[]") // JSON array
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String> = varchar("created_by", 100)
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val clientVersion: Column<Long> = long("client_version").default(1)
    
    // Soft Delete
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
    
    // Client Secret Management
    val secretExpiresAt: Column<Instant?> = timestamp("secret_expires_at").nullable()
    val secretRotationEnabled: Column<Boolean> = bool("secret_rotation_enabled").default(false)
    val secretRotationDays: Column<Int> = integer("secret_rotation_days").default(90)
    val lastSecretRotation: Column<Instant?> = timestamp("last_secret_rotation").nullable()
    val previousSecretHash: Column<String?> = varchar("previous_secret_hash", 256).nullable()
}
