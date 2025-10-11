package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Connected Apps Table Definition for Entativa ID
 * Manages third-party applications and services connected to user accounts
 * 
 * @author Neo Qiss
 * @status Production-ready app connection management with OAuth2 and API integrations
 */
object ConnectedAppsTable : UUIDTable("connected_apps") {
    
    // Core Connection Information
    val userId: Column<String> = varchar("user_id", 100).index()
    val appId: Column<String> = varchar("app_id", 128).index()
    val appName: Column<String> = varchar("app_name", 200)
    val appType: Column<String> = varchar("app_type", 30) // OAUTH_APP, API_CLIENT, WEBHOOK, INTEGRATION, BOT, GAME
    val connectionId: Column<String> = varchar("connection_id", 128).uniqueIndex()
    val connectionType: Column<String> = varchar("connection_type", 30).default("OAUTH2") // OAUTH2, API_KEY, WEBHOOK, SSO, SAML
    
    // Application Details
    val appDescription: Column<String?> = text("app_description").nullable()
    val appUrl: Column<String?> = varchar("app_url", 500).nullable()
    val appLogoUrl: Column<String?> = varchar("app_logo_url", 500).nullable()
    val appCategory: Column<String?> = varchar("app_category", 100).nullable() // SOCIAL, PRODUCTIVITY, GAMING, FINANCE, etc.
    val appDeveloper: Column<String?> = varchar("app_developer", 200).nullable()
    val appVersion: Column<String?> = varchar("app_version", 50).nullable()
    val appPlatform: Column<String?> = varchar("app_platform", 50).nullable() // WEB, MOBILE, DESKTOP, API
    
    // OAuth2 Integration
    val oauthClientId: Column<String?> = varchar("oauth_client_id", 128).nullable()
    val oauthScopes: Column<String> = text("oauth_scopes").default("[]") // JSON array of granted scopes
    val accessToken: Column<String?> = text("access_token").nullable() // Encrypted
    val refreshToken: Column<String?> = text("refresh_token").nullable() // Encrypted
    val tokenType: Column<String> = varchar("token_type", 20).default("Bearer")
    val accessTokenExpiresAt: Column<Instant?> = timestamp("access_token_expires_at").nullable()
    val refreshTokenExpiresAt: Column<Instant?> = timestamp("refresh_token_expires_at").nullable()
    val tokenLastRefreshed: Column<Instant?> = timestamp("token_last_refreshed").nullable()
    
    // API Access
    val apiKey: Column<String?> = varchar("api_key", 256).nullable() // Encrypted
    val apiKeyHash: Column<String?> = varchar("api_key_hash", 256).nullable()
    val apiSecret: Column<String?> = varchar("api_secret", 512).nullable() // Encrypted
    val apiVersion: Column<String?> = varchar("api_version", 20).nullable()
    val apiEndpoint: Column<String?> = varchar("api_endpoint", 500).nullable()
    val rateLimitTier: Column<String> = varchar("rate_limit_tier", 20).default("STANDARD")
    val dailyRequestLimit: Column<Int> = integer("daily_request_limit").default(1000)
    val requestsUsedToday: Column<Int> = integer("requests_used_today").default(0)
    val requestCountResetAt: Column<Instant?> = timestamp("request_count_reset_at").nullable()
    
    // Connection Status
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, SUSPENDED, REVOKED, EXPIRED, ERROR
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isAuthorized: Column<Boolean> = bool("is_authorized").default(true)
    val isPaused: Column<Boolean> = bool("is_paused").default(false)
    val lastStatusCheck: Column<Instant?> = timestamp("last_status_check").nullable()
    val healthStatus: Column<String> = varchar("health_status", 20).default("HEALTHY") // HEALTHY, WARNING, ERROR, CRITICAL
    
    // Permissions and Scopes
    val permissions: Column<String> = text("permissions").default("[]") // JSON array of specific permissions
    val dataAccess: Column<String> = text("data_access").default("[]") // JSON array of data types accessible
    val writePermissions: Column<Boolean> = bool("write_permissions").default(false)
    val readPermissions: Column<Boolean> = bool("read_permissions").default(true)
    val adminPermissions: Column<Boolean> = bool("admin_permissions").default(false)
    val crossPlatformAccess: Column<Boolean> = bool("cross_platform_access").default(false)
    val platformAccess: Column<String> = text("platform_access").default("[]") // JSON array of accessible platforms
    
    // User Consent and Privacy
    val userConsent: Column<Boolean> = bool("user_consent").default(false)
    val consentGivenAt: Column<Instant?> = timestamp("consent_given_at").nullable()
    val consentVersion: Column<String?> = varchar("consent_version", 20).nullable()
    val privacyPolicyAccepted: Column<Boolean> = bool("privacy_policy_accepted").default(false)
    val termsAccepted: Column<Boolean> = bool("terms_accepted").default(false)
    val dataProcessingConsent: Column<Boolean> = bool("data_processing_consent").default(false)
    val marketingConsent: Column<Boolean> = bool("marketing_consent").default(false)
    val dataRetentionDays: Column<Int> = integer("data_retention_days").default(365)
    
    // Usage Analytics
    val totalRequests: Column<Long> = long("total_requests").default(0)
    val successfulRequests: Column<Long> = long("successful_requests").default(0)
    val failedRequests: Column<Long> = long("failed_requests").default(0)
    val lastRequestAt: Column<Instant?> = timestamp("last_request_at").nullable()
    val averageResponseTime: Column<Double> = double("average_response_time").default(0.0)
    val dataTransferred: Column<Long> = long("data_transferred").default(0) // bytes
    val requestsThisMonth: Column<Long> = long("requests_this_month").default(0)
    val monthlyResetAt: Column<Instant?> = timestamp("monthly_reset_at").nullable()
    
    // Security and Trust
    val trustLevel: Column<String> = varchar("trust_level", 20).default("STANDARD") // LOW, STANDARD, HIGH, VERIFIED
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudFlags: Column<String> = text("fraud_flags").default("[]") // JSON array
    val securityIncidents: Column<String> = text("security_incidents").default("[]") // JSON array
    val lastSecurityCheck: Column<Instant?> = timestamp("last_security_check").nullable()
    val certificateInfo: Column<String?> = text("certificate_info").nullable() // JSON object
    val encryptionLevel: Column<String> = varchar("encryption_level", 20).default("TLS12")
    
    // Webhook Configuration
    val webhookUrl: Column<String?> = varchar("webhook_url", 500).nullable()
    val webhookSecret: Column<String?> = varchar("webhook_secret", 256).nullable()
    val webhookEvents: Column<String> = text("webhook_events").default("[]") // JSON array
    val webhookEnabled: Column<Boolean> = bool("webhook_enabled").default(false)
    val webhookRetries: Column<Int> = integer("webhook_retries").default(3)
    val webhookTimeout: Column<Int> = integer("webhook_timeout").default(30) // seconds
    val lastWebhookSuccess: Column<Instant?> = timestamp("last_webhook_success").nullable()
    val lastWebhookFailure: Column<Instant?> = timestamp("last_webhook_failure").nullable()
    val webhookFailureCount: Column<Int> = integer("webhook_failure_count").default(0)
    
    // Integration Settings
    val syncEnabled: Column<Boolean> = bool("sync_enabled").default(false)
    val syncDirection: Column<String> = varchar("sync_direction", 20).default("BIDIRECTIONAL") // IMPORT, EXPORT, BIDIRECTIONAL
    val syncFrequency: Column<String> = varchar("sync_frequency", 20).default("MANUAL") // REAL_TIME, HOURLY, DAILY, WEEKLY, MANUAL
    val lastSyncAt: Column<Instant?> = timestamp("last_sync_at").nullable()
    val syncStatus: Column<String> = varchar("sync_status", 20).default("IDLE") // IDLE, RUNNING, COMPLETED, FAILED
    val syncErrors: Column<String> = text("sync_errors").default("[]") // JSON array
    val autoSyncEnabled: Column<Boolean> = bool("auto_sync_enabled").default(false)
    val syncBatchSize: Column<Int> = integer("sync_batch_size").default(100)
    
    // Data Mapping and Transformation
    val fieldMappings: Column<String> = text("field_mappings").default("{}") // JSON object
    val dataTransformations: Column<String> = text("data_transformations").default("[]") // JSON array
    val filterRules: Column<String> = text("filter_rules").default("[]") // JSON array
    val validationRules: Column<String> = text("validation_rules").default("[]") // JSON array
    val customMappings: Column<String> = text("custom_mappings").default("{}") // JSON object
    val defaultValues: Column<String> = text("default_values").default("{}") // JSON object
    
    // Error Handling and Monitoring
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastError: Column<String?> = text("last_error").nullable()
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val errorThreshold: Column<Int> = integer("error_threshold").default(10)
    val alertsEnabled: Column<Boolean> = bool("alerts_enabled").default(true)
    val monitoringEnabled: Column<Boolean> = bool("monitoring_enabled").default(true)
    val notificationEmail: Column<String?> = varchar("notification_email", 254).nullable()
    val escalationRules: Column<String> = text("escalation_rules").default("[]") // JSON array
    
    // Rate Limiting and Throttling
    val rateLimitEnabled: Column<Boolean> = bool("rate_limit_enabled").default(true)
    val requestsPerMinute: Column<Int> = integer("requests_per_minute").default(60)
    val requestsPerHour: Column<Int> = integer("requests_per_hour").default(1000)
    val burstLimit: Column<Int> = integer("burst_limit").default(10)
    val currentMinuteRequests: Column<Int> = integer("current_minute_requests").default(0)
    val currentHourRequests: Column<Int> = integer("current_hour_requests").default(0)
    val lastRateLimitReset: Column<Instant?> = timestamp("last_rate_limit_reset").nullable()
    val rateLimitExceeded: Column<Boolean> = bool("rate_limit_exceeded").default(false)
    
    // Billing and Usage
    val billingTier: Column<String> = varchar("billing_tier", 20).default("FREE") // FREE, BASIC, PREMIUM, ENTERPRISE
    val usageCost: Column<Double> = double("usage_cost").default(0.0)
    val monthlyCost: Column<Double> = double("monthly_cost").default(0.0)
    val costCurrency: Column<String> = varchar("cost_currency", 3).default("USD")
    val billingCycle: Column<String> = varchar("billing_cycle", 20).default("MONTHLY")
    val lastBillDate: Column<Instant?> = timestamp("last_bill_date").nullable()
    val nextBillDate: Column<Instant?> = timestamp("next_bill_date").nullable()
    val overage_charges: Column<Double> = double("overage_charges").default(0.0)
    
    // Compliance and Audit
    val complianceLevel: Column<String> = varchar("compliance_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, CRITICAL
    val auditTrailEnabled: Column<Boolean> = bool("audit_trail_enabled").default(true)
    val dataClassification: Column<String> = varchar("data_classification", 20).default("INTERNAL")
    val regulatoryRequirements: Column<String> = text("regulatory_requirements").default("[]") // JSON array
    val complianceChecks: Column<String> = text("compliance_checks").default("[]") // JSON array
    val lastComplianceCheck: Column<Instant?> = timestamp("last_compliance_check").nullable()
    val certifications: Column<String> = text("certifications").default("[]") // JSON array
    
    // User Experience
    val userNotes: Column<String?> = text("user_notes").nullable()
    val userRating: Column<Int?> = integer("user_rating").nullable() // 1-5 stars
    val userFeedback: Column<String?> = text("user_feedback").nullable()
    val isFavorite: Column<Boolean> = bool("is_favorite").default(false)
    val displayOrder: Column<Int> = integer("display_order").default(0)
    val isHidden: Column<Boolean> = bool("is_hidden").default(false)
    val customName: Column<String?> = varchar("custom_name", 200).nullable()
    val customIcon: Column<String?> = varchar("custom_icon", 500).nullable()
    val customColor: Column<String?> = varchar("custom_color", 7).nullable() // Hex color
    
    // Connection Lifecycle
    val connectedAt: Column<Instant> = timestamp("connected_at").default(Instant.now())
    val firstConnectedAt: Column<Instant> = timestamp("first_connected_at").default(Instant.now())
    val lastConnectedAt: Column<Instant> = timestamp("last_connected_at").default(Instant.now())
    val lastDisconnectedAt: Column<Instant?> = timestamp("last_disconnected_at").nullable()
    val connectionAttempts: Column<Int> = integer("connection_attempts").default(0)
    val reconnectionCount: Column<Int> = integer("reconnection_count").default(0)
    val totalDowntime: Column<Long> = long("total_downtime").default(0) // seconds
    val averageUptime: Column<Double> = double("average_uptime").default(100.0) // percentage
    
    // Feature Flags and Configuration
    val featureFlags: Column<String> = text("feature_flags").default("{}") // JSON object
    val configuration: Column<String> = text("configuration").default("{}") // JSON object
    val environment: Column<String> = varchar("environment", 20).default("PRODUCTION") // DEVELOPMENT, STAGING, PRODUCTION
    val debugMode: Column<Boolean> = bool("debug_mode").default(false)
    val testMode: Column<Boolean> = bool("test_mode").default(false)
    val maintenanceMode: Column<Boolean> = bool("maintenance_mode").default(false)
    val betaFeatures: Column<Boolean> = bool("beta_features").default(false)
    
    // Custom Attributes and Extensions
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON object
    val metadata: Column<String> = text("metadata").default("{}") // JSON object
    val integrationData: Column<String> = text("integration_data").default("{}") // JSON object
    val vendorSpecificData: Column<String> = text("vendor_specific_data").default("{}") // JSON object
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val labels: Column<String> = text("labels").default("{}") // JSON object
    
    // Administrative
    val adminNotes: Column<String?> = text("admin_notes").nullable()
    val internalFlags: Column<String> = text("internal_flags").default("[]") // JSON array
    val supportTickets: Column<String> = text("support_tickets").default("[]") // JSON array
    val isQuarantined: Column<Boolean> = bool("is_quarantined").default(false)
    val quarantineReason: Column<String?> = text("quarantine_reason").nullable()
    val quarantinedAt: Column<Instant?> = timestamp("quarantined_at").nullable()
    val reviewRequired: Column<Boolean> = bool("review_required").default(false)
    val reviewReason: Column<String?> = text("review_reason").nullable()
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // Soft Delete and Archival
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
    val archivedAt: Column<Instant?> = timestamp("archived_at").nullable()
    val archiveReason: Column<String?> = varchar("archive_reason", 500).nullable()
    val revokedAt: Column<Instant?> = timestamp("revoked_at").nullable()
    val revocationReason: Column<String?> = varchar("revocation_reason", 500).nullable()
}
