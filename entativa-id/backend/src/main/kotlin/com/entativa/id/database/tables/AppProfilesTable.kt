package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * App Profiles Table Definition for Entativa ID
 * Manages user profiles specific to connected applications with app-specific customizations
 * 
 * @author Neo Qiss
 * @status Production-ready app-specific profile management with privacy controls
 */
object AppProfilesTable : UUIDTable("app_profiles") {
    
    // Core App Profile Information
    val userId: Column<String> = varchar("user_id", 100).index()
    val appId: Column<String> = varchar("app_id", 128).index()
    val connectionId: Column<String> = varchar("connection_id", 128).index()
    val profileId: Column<String> = varchar("profile_id", 128).uniqueIndex()
    val appProfileType: Column<String> = varchar("app_profile_type", 30).default("STANDARD") // STANDARD, BUSINESS, DEVELOPER, PREMIUM, CUSTOM
    
    // App-Specific Identity
    val appDisplayName: Column<String?> = varchar("app_display_name", 200).nullable()
    val appUsername: Column<String?> = varchar("app_username", 100).nullable()
    val appHandle: Column<String?> = varchar("app_handle", 64).nullable()
    val appUserId: Column<String?> = varchar("app_user_id", 200).nullable() // External app's user ID
    val appEmail: Column<String?> = varchar("app_email", 254).nullable()
    val externalIdentifiers: Column<String> = text("external_identifiers").default("{}") // JSON object
    val linkedAccounts: Column<String> = text("linked_accounts").default("[]") // JSON array
    
    // Profile Status and Permissions
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, SUSPENDED, REVOKED, EXPIRED, PENDING
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    val isPrimary: Column<Boolean> = bool("is_primary").default(false)
    val isPublic: Column<Boolean> = bool("is_public").default(false)
    val accessLevel: Column<String> = varchar("access_level", 20).default("STANDARD") // BASIC, STANDARD, PREMIUM, ADMIN, DEVELOPER
    val permissionLevel: Column<String> = varchar("permission_level", 20).default("READ") // READ, WRITE, ADMIN, FULL
    
    // App-Specific Profile Data
    val appProfileData: Column<String> = text("app_profile_data").default("{}") // JSON object
    val appSettings: Column<String> = text("app_settings").default("{}") // JSON object
    val appPreferences: Column<String> = text("app_preferences").default("{}") // JSON object
    val appCustomizations: Column<String> = text("app_customizations").default("{}") // JSON object
    val appMetadata: Column<String> = text("app_metadata").default("{}") // JSON object
    val appTags: Column<String> = text("app_tags").default("[]") // JSON array
    val appLabels: Column<String> = text("app_labels").default("{}") // JSON object
    
    // Data Sharing and Privacy
    val sharedFields: Column<String> = text("shared_fields").default("[]") // JSON array of fields shared with app
    val privateFields: Column<String> = text("private_fields").default("[]") // JSON array of fields kept private
    val dataConsent: Column<String> = text("data_consent").default("{}") // JSON object of consent choices
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("STANDARD") // MINIMAL, STANDARD, ENHANCED, MAXIMUM
    val allowDataExport: Column<Boolean> = bool("allow_data_export").default(false)
    val allowDataSharing: Column<Boolean> = bool("allow_data_sharing").default(false)
    val allowAnalytics: Column<Boolean> = bool("allow_analytics").default(true)
    val allowMarketing: Column<Boolean> = bool("allow_marketing").default(false)
    val allowNotifications: Column<Boolean> = bool("allow_notifications").default(true)
    
    // App-Specific Contact Information
    val appContactEmail: Column<String?> = varchar("app_contact_email", 254).nullable()
    val appContactPhone: Column<String?> = varchar("app_contact_phone", 20).nullable()
    val appSocialLinks: Column<String> = text("app_social_links").default("{}") // JSON object
    val appWebsite: Column<String?> = varchar("app_website", 500).nullable()
    val contactPreferences: Column<String> = text("contact_preferences").default("{}") // JSON object
    val communicationChannels: Column<String> = text("communication_channels").default("[]") // JSON array
    
    // Profile Synchronization
    val syncEnabled: Column<Boolean> = bool("sync_enabled").default(true)
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED, DISABLED
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncVersion: Column<Long> = long("sync_version").default(1)
    val syncConflicts: Column<String> = text("sync_conflicts").default("[]") // JSON array
    val syncDirection: Column<String> = varchar("sync_direction", 20).default("BIDIRECTIONAL") // IMPORT, EXPORT, BIDIRECTIONAL
    val autoSyncEnabled: Column<Boolean> = bool("auto_sync_enabled").default(true)
    val lastSyncError: Column<String?> = text("last_sync_error").nullable()
    
    // App-Specific Features and Capabilities
    val enabledFeatures: Column<String> = text("enabled_features").default("[]") // JSON array
    val disabledFeatures: Column<String> = text("disabled_features").default("[]") // JSON array
    val featurePermissions: Column<String> = text("feature_permissions").default("{}") // JSON object
    val appCapabilities: Column<String> = text("app_capabilities").default("[]") // JSON array
    val usageQuotas: Column<String> = text("usage_quotas").default("{}") // JSON object
    val rateLimits: Column<String> = text("rate_limits").default("{}") // JSON object
    val accessRestrictions: Column<String> = text("access_restrictions").default("[]") // JSON array
    
    // Usage Analytics and Statistics
    val totalSessions: Column<Long> = long("total_sessions").default(0)
    val totalUsageTime: Column<Long> = long("total_usage_time").default(0) // seconds
    val lastActiveAt: Column<Instant?> = timestamp("last_active_at").nullable()
    val firstUsedAt: Column<Instant?> = timestamp("first_used_at").nullable()
    val averageSessionLength: Column<Double> = double("average_session_length").default(0.0) // seconds
    val usageFrequency: Column<String> = varchar("usage_frequency", 20).default("UNKNOWN") // DAILY, WEEKLY, MONTHLY, RARELY
    val engagementScore: Column<Double> = double("engagement_score").default(0.0)
    val retentionScore: Column<Double> = double("retention_score").default(0.0)
    
    // App Billing and Subscription
    val subscriptionTier: Column<String> = varchar("subscription_tier", 30).default("FREE") // FREE, BASIC, PREMIUM, ENTERPRISE
    val subscriptionStatus: Column<String> = varchar("subscription_status", 20).default("ACTIVE") // ACTIVE, CANCELLED, EXPIRED, TRIAL
    val subscriptionExpiresAt: Column<Instant?> = timestamp("subscription_expires_at").nullable()
    val billingCycle: Column<String> = varchar("billing_cycle", 20).default("MONTHLY") // MONTHLY, YEARLY, ONE_TIME
    val lastBillingDate: Column<Instant?> = timestamp("last_billing_date").nullable()
    val nextBillingDate: Column<Instant?> = timestamp("next_billing_date").nullable()
    val totalSpent: Column<Double> = double("total_spent").default(0.0)
    val currency: Column<String> = varchar("currency", 3).default("USD")
    
    // App-Specific Settings and Preferences
    val notificationSettings: Column<String> = text("notification_settings").default("{}") // JSON object
    val displaySettings: Column<String> = text("display_settings").default("{}") // JSON object
    val behaviorSettings: Column<String> = text("behavior_settings").default("{}") // JSON object
    val securitySettings: Column<String> = text("security_settings").default("{}") // JSON object
    val accessibilitySettings: Column<String> = text("accessibility_settings").default("{}") // JSON object
    val localizationSettings: Column<String> = text("localization_settings").default("{}") // JSON object
    val experimentalFeatures: Column<Boolean> = bool("experimental_features").default(false)
    
    // Content and Media Preferences
    val contentPreferences: Column<String> = text("content_preferences").default("{}") // JSON object
    val mediaPreferences: Column<String> = text("media_preferences").default("{}") // JSON object
    val contentFilters: Column<String> = text("content_filters").default("[]") // JSON array
    val blockedContent: Column<String> = text("blocked_content").default("[]") // JSON array
    val favoriteContent: Column<String> = text("favorite_content").default("[]") // JSON array
    val recommendationPrefs: Column<String> = text("recommendation_prefs").default("{}") // JSON object
    val aiPersonalization: Column<Boolean> = bool("ai_personalization").default(true)
    
    // Security and Trust
    val trustLevel: Column<String> = varchar("trust_level", 20).default("STANDARD") // LOW, STANDARD, HIGH, VERIFIED
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudFlags: Column<String> = text("fraud_flags").default("[]") // JSON array
    val securityIncidents: Column<String> = text("security_incidents").default("[]") // JSON array
    val lastSecurityCheck: Column<Instant?> = timestamp("last_security_check").nullable()
    val mfaEnabled: Column<Boolean> = bool("mfa_enabled").default(false)
    val sessionSecurity: Column<String> = varchar("session_security", 20).default("STANDARD")
    
    // Integration and API Access
    val apiAccess: Column<Boolean> = bool("api_access").default(false)
    val apiKey: Column<String?> = varchar("api_key", 256).nullable() // Encrypted
    val apiQuota: Column<String> = text("api_quota").default("{}") // JSON object
    val webhookEndpoints: Column<String> = text("webhook_endpoints").default("[]") // JSON array
    val integrationData: Column<String> = text("integration_data").default("{}") // JSON object
    val customEndpoints: Column<String> = text("custom_endpoints").default("[]") // JSON array
    val dataExportFormats: Column<String> = text("data_export_formats").default("[]") // JSON array
    val allowThirdPartyAccess: Column<Boolean> = bool("allow_third_party_access").default(false)
    
    // Content Creation and Management
    val contentCreator: Column<Boolean> = bool("content_creator").default(false)
    val createdContent: Column<String> = text("created_content").default("[]") // JSON array of content IDs
    val publishedContent: Column<String> = text("published_content").default("[]") // JSON array
    val draftContent: Column<String> = text("draft_content").default("[]") // JSON array
    val contentCategories: Column<String> = text("content_categories").default("[]") // JSON array
    val contentLicensing: Column<String> = text("content_licensing").default("{}") // JSON object
    val monetizationEnabled: Column<Boolean> = bool("monetization_enabled").default(false)
    val contentRights: Column<String> = text("content_rights").default("{}") // JSON object
    
    // Social and Community Features
    val socialProfile: Column<String> = text("social_profile").default("{}") // JSON object
    val followersCount: Column<Long> = long("followers_count").default(0)
    val followingCount: Column<Long> = long("following_count").default(0)
    val connectionsCount: Column<Long> = long("connections_count").default(0)
    val socialConnections: Column<String> = text("social_connections").default("[]") // JSON array
    val groupMemberships: Column<String> = text("group_memberships").default("[]") // JSON array
    val communityRole: Column<String?> = varchar("community_role", 50).nullable()
    val reputationScore: Column<Double> = double("reputation_score").default(0.0)
    val socialScore: Column<Double> = double("social_score").default(0.0)
    
    // App-Specific Achievements and Gamification
    val achievements: Column<String> = text("achievements").default("[]") // JSON array
    val badges: Column<String> = text("badges").default("[]") // JSON array
    val points: Column<Long> = long("points").default(0)
    val level: Column<Int> = integer("level").default(1)
    val experience: Column<Long> = long("experience").default(0)
    val streaks: Column<String> = text("streaks").default("{}") // JSON object
    val milestones: Column<String> = text("milestones").default("[]") // JSON array
    val leaderboardPosition: Column<Int?> = integer("leaderboard_position").nullable()
    val competitiveRating: Column<Double?> = double("competitive_rating").nullable()
    
    // Compliance and Legal
    val complianceStatus: Column<String> = varchar("compliance_status", 20).default("COMPLIANT") // COMPLIANT, NON_COMPLIANT, PENDING, UNKNOWN
    val ageVerified: Column<Boolean> = bool("age_verified").default(false)
    val parentalConsent: Column<Boolean> = bool("parental_consent").default(false)
    val termsAccepted: Column<Boolean> = bool("terms_accepted").default(false)
    val privacyPolicyAccepted: Column<Boolean> = bool("privacy_policy_accepted").default(false)
    val dataProcessingConsent: Column<Boolean> = bool("data_processing_consent").default(false)
    val cookieConsent: Column<Boolean> = bool("cookie_consent").default(false)
    val rightToErasure: Column<Boolean> = bool("right_to_erasure").default(true)
    val dataPortability: Column<Boolean> = bool("data_portability").default(true)
    
    // Performance and Quality Metrics
    val profileCompleteness: Column<Double> = double("profile_completeness").default(0.0)
    val dataQuality: Column<Double> = double("data_quality").default(0.0)
    val userSatisfaction: Column<Double> = double("user_satisfaction").default(0.0)
    val performanceScore: Column<Double> = double("performance_score").default(0.0)
    val reliabilityScore: Column<Double> = double("reliability_score").default(0.0)
    val responseTime: Column<Double> = double("response_time").default(0.0) // milliseconds
    val uptime: Column<Double> = double("uptime").default(100.0) // percentage
    val errorRate: Column<Double> = double("error_rate").default(0.0) // percentage
    
    // App-Specific Events and Triggers
    val eventHandlers: Column<String> = text("event_handlers").default("{}") // JSON object
    val triggers: Column<String> = text("triggers").default("[]") // JSON array
    val automationRules: Column<String> = text("automation_rules").default("[]") // JSON array
    val customWorkflows: Column<String> = text("custom_workflows").default("[]") // JSON array
    val scheduledTasks: Column<String> = text("scheduled_tasks").default("[]") // JSON array
    val eventHistory: Column<String> = text("event_history").default("[]") // JSON array
    val lastEventAt: Column<Instant?> = timestamp("last_event_at").nullable()
    
    // Feedback and Support
    val feedbackProvided: Column<String> = text("feedback_provided").default("[]") // JSON array
    val supportTickets: Column<String> = text("support_tickets").default("[]") // JSON array
    val bugReports: Column<String> = text("bug_reports").default("[]") // JSON array
    val featureRequests: Column<String> = text("feature_requests").default("[]") // JSON array
    val lastFeedbackAt: Column<Instant?> = timestamp("last_feedback_at").nullable()
    val supportInteractions: Column<Int> = integer("support_interactions").default(0)
    val satisfactionRating: Column<Double?> = double("satisfaction_rating").nullable()
    val npsScore: Column<Int?> = integer("nps_score").nullable()
    
    // Custom Extensions and Plugins
    val installedPlugins: Column<String> = text("installed_plugins").default("[]") // JSON array
    val enabledExtensions: Column<String> = text("enabled_extensions").default("[]") // JSON array
    val customScripts: Column<String> = text("custom_scripts").default("[]") // JSON array
    val thirdPartyIntegrations: Column<String> = text("third_party_integrations").default("[]") // JSON array
    val personalizations: Column<String> = text("personalizations").default("{}") // JSON object
    val customizations: Column<String> = text("customizations").default("{}") // JSON object
    val themes: Column<String> = text("themes").default("{}") // JSON object
    
    // Data Import/Export
    val importHistory: Column<String> = text("import_history").default("[]") // JSON array
    val exportHistory: Column<String> = text("export_history").default("[]") // JSON array
    val dataBackups: Column<String> = text("data_backups").default("[]") // JSON array
    val lastImportAt: Column<Instant?> = timestamp("last_import_at").nullable()
    val lastExportAt: Column<Instant?> = timestamp("last_export_at").nullable()
    val lastBackupAt: Column<Instant?> = timestamp("last_backup_at").nullable()
    val migrationData: Column<String> = text("migration_data").default("{}") // JSON object
    val dataVersion: Column<String> = varchar("data_version", 20).default("1.0")
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // Lifecycle Management
    val activatedAt: Column<Instant?> = timestamp("activated_at").nullable()
    val deactivatedAt: Column<Instant?> = timestamp("deactivated_at").nullable()
    val suspendedAt: Column<Instant?> = timestamp("suspended_at").nullable()
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
    val archivedAt: Column<Instant?> = timestamp("archived_at").nullable()
    val archiveReason: Column<String?> = varchar("archive_reason", 500).nullable()
    val expiresAt: Column<Instant?> = timestamp("expires_at").nullable()
}
