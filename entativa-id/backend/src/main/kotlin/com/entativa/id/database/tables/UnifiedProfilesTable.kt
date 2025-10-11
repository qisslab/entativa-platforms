package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Unified Profiles Table Definition for Entativa ID
 * Manages cross-platform unified user profiles that sync across all Entativa platforms
 * 
 * @author Neo Qiss
 * @status Production-ready unified profile management with cross-platform synchronization
 */
object UnifiedProfilesTable : UUIDTable("unified_profiles") {
    
    // Core Unified Profile Information
    val userId: Column<String> = varchar("user_id", 100).uniqueIndex()
    val entativaId: Column<String> = varchar("entativa_id", 32).uniqueIndex()
    val primaryHandle: Column<String> = varchar("primary_handle", 64).uniqueIndex()
    val unifiedDisplayName: Column<String> = varchar("unified_display_name", 200)
    val profileType: Column<String> = varchar("profile_type", 20).default("PERSONAL") // PERSONAL, BUSINESS, CREATOR, ORGANIZATION, BRAND
    
    // Identity Synchronization
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED, CONFLICT, DISABLED
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncVersion: Column<Long> = long("sync_version").default(1)
    val masterPlatform: Column<String?> = varchar("master_platform", 50).nullable() // Platform that serves as source of truth
    val conflictResolutionStrategy: Column<String> = varchar("conflict_resolution_strategy", 20).default("LATEST_WINS")
    val syncLocked: Column<Boolean> = bool("sync_locked").default(false)
    val lockReason: Column<String?> = text("lock_reason").nullable()
    
    // Platform Availability Matrix
    val availableOnGala: Column<Boolean> = bool("available_on_gala").default(false)
    val availableOnPika: Column<Boolean> = bool("available_on_pika").default(false)
    val availableOnPlayPods: Column<Boolean> = bool("available_on_playpods").default(false)
    val availableOnSoNet: Column<Boolean> = bool("available_on_sonet").default(false)
    val platformPreferences: Column<String> = text("platform_preferences").default("{}") // JSON object
    val platformCustomizations: Column<String> = text("platform_customizations").default("{}") // JSON object
    
    // Unified Identity Data
    val firstName: Column<String?> = varchar("first_name", 100).nullable()
    val lastName: Column<String?> = varchar("last_name", 100).nullable()
    val middleName: Column<String?> = varchar("middle_name", 100).nullable()
    val preferredName: Column<String?> = varchar("preferred_name", 100).nullable()
    val pronouns: Column<String?> = varchar("pronouns", 50).nullable()
    val dateOfBirth: Column<Instant?> = timestamp("date_of_birth").nullable()
    val gender: Column<String?> = varchar("gender", 50).nullable()
    val location: Column<String?> = varchar("location", 200).nullable()
    val timezone: Column<String> = varchar("timezone", 50).default("UTC")
    val language: Column<String> = varchar("primary_language", 10).default("en")
    val languages: Column<String> = text("languages").default("[]") // JSON array
    
    // Unified Bio and Description
    val unifiedBio: Column<String?> = text("unified_bio").nullable()
    val shortBio: Column<String?> = varchar("short_bio", 500).nullable()
    val tagline: Column<String?> = varchar("tagline", 200).nullable()
    val mission: Column<String?> = text("mission").nullable()
    val story: Column<String?> = text("story").nullable()
    val personalityTraits: Column<String> = text("personality_traits").default("[]") // JSON array
    val values: Column<String> = text("values").default("[]") // JSON array
    
    // Visual Identity Unified
    val primaryAvatarUrl: Column<String?> = varchar("primary_avatar_url", 500).nullable()
    val primaryCoverImageUrl: Column<String?> = varchar("primary_cover_image_url", 500).nullable()
    val primaryBrandColor: Column<String?> = varchar("primary_brand_color", 7).nullable() // Hex color
    val secondaryBrandColor: Column<String?> = varchar("secondary_brand_color", 7).nullable()
    val visualTheme: Column<String> = varchar("visual_theme", 20).default("DEFAULT") // DEFAULT, DARK, LIGHT, VIBRANT, MINIMAL
    val logoUrl: Column<String?> = varchar("logo_url", 500).nullable()
    val brandAssets: Column<String> = text("brand_assets").default("{}") // JSON object
    
    // Contact Information Unified
    val primaryEmail: Column<String> = varchar("primary_email", 254)
    val alternateEmails: Column<String> = text("alternate_emails").default("[]") // JSON array
    val primaryPhone: Column<String?> = varchar("primary_phone", 20).nullable()
    val alternatePhones: Column<String> = text("alternate_phones").default("[]") // JSON array
    val website: Column<String?> = varchar("website", 500).nullable()
    val socialLinks: Column<String> = text("social_links").default("{}") // JSON object
    val verifiedContacts: Column<String> = text("verified_contacts").default("{}") // JSON object
    
    // Professional Information Unified
    val occupation: Column<String?> = varchar("occupation", 200).nullable()
    val company: Column<String?> = varchar("company", 200).nullable()
    val jobTitle: Column<String?> = varchar("job_title", 100).nullable()
    val industry: Column<String?> = varchar("industry", 100).nullable()
    val workLocation: Column<String?> = varchar("work_location", 200).nullable()
    val skills: Column<String> = text("skills").default("[]") // JSON array
    val expertise: Column<String> = text("expertise").default("[]") // JSON array
    val certifications: Column<String> = text("certifications").default("[]") // JSON array
    val education: Column<String> = text("education").default("[]") // JSON array
    val experience: Column<String> = text("experience").default("[]") // JSON array
    val achievements: Column<String> = text("achievements").default("[]") // JSON array
    val portfolio: Column<String> = text("portfolio").default("[]") // JSON array
    
    // Interests and Preferences Unified
    val interests: Column<String> = text("interests").default("[]") // JSON array
    val hobbies: Column<String> = text("hobbies").default("[]") // JSON array
    val favoriteTopics: Column<String> = text("favorite_topics").default("[]") // JSON array
    val contentPreferences: Column<String> = text("content_preferences").default("{}") // JSON object
    val communicationStyle: Column<String?> = varchar("communication_style", 50).nullable()
    val personalityType: Column<String?> = varchar("personality_type", 20).nullable() // MBTI, Big Five, etc.
    
    // Privacy and Visibility Unified
    val globalPrivacyLevel: Column<String> = varchar("global_privacy_level", 20).default("NORMAL") // PUBLIC, NORMAL, PRIVATE, RESTRICTED
    val profileVisibility: Column<String> = varchar("profile_visibility", 20).default("PUBLIC")
    val searchable: Column<Boolean> = bool("searchable").default(true)
    val allowCrossPlatformSearch: Column<Boolean> = bool("allow_cross_platform_search").default(true)
    val showAcrossPlatforms: Column<Boolean> = bool("show_across_platforms").default(true)
    val allowDirectMessages: Column<Boolean> = bool("allow_direct_messages").default(true)
    val allowFollowRequests: Column<Boolean> = bool("allow_follow_requests").default(true)
    val showConnectionCounts: Column<Boolean> = bool("show_connection_counts").default(true)
    val showActivity: Column<Boolean> = bool("show_activity").default(true)
    
    // Cross-Platform Analytics Unified
    val totalFollowersAcrossPlatforms: Column<Long> = long("total_followers_across_platforms").default(0)
    val totalFollowingAcrossPlatforms: Column<Long> = long("total_following_across_platforms").default(0)
    val totalContentAcrossPlatforms: Column<Long> = long("total_content_across_platforms").default(0)
    val totalEngagementAcrossPlatforms: Column<Long> = long("total_engagement_across_platforms").default(0)
    val crossPlatformInfluenceScore: Column<Double> = double("cross_platform_influence_score").default(0.0)
    val unifiedReputationScore: Column<Double> = double("unified_reputation_score").default(0.0)
    val globalTrustScore: Column<Double> = double("global_trust_score").default(0.0)
    
    // Verification Status Unified
    val isGloballyVerified: Column<Boolean> = bool("is_globally_verified").default(false)
    val verificationLevel: Column<String> = varchar("verification_level", 20).default("NONE") // NONE, BASIC, STANDARD, PREMIUM, CELEBRITY, BRAND
    val verificationBadges: Column<String> = text("verification_badges").default("[]") // JSON array
    val verifiedAt: Column<Instant?> = timestamp("verified_at").nullable()
    val verifiedBy: Column<String?> = varchar("verified_by", 100).nullable()
    val verificationDocuments: Column<String> = text("verification_documents").default("[]") // JSON array
    val kycCompleted: Column<Boolean> = bool("kyc_completed").default(false)
    val kycLevel: Column<String?> = varchar("kyc_level", 20).nullable()
    
    // Platform-Specific Customizations
    val galaCustomization: Column<String> = text("gala_customization").default("{}") // JSON object
    val pikaCustomization: Column<String> = text("pika_customization").default("{}") // JSON object
    val playpodsCustomization: Column<String> = text("playpods_customization").default("{}") // JSON object
    val sonetCustomization: Column<String> = text("sonet_customization").default("{}") // JSON object
    val platformSpecificHandles: Column<String> = text("platform_specific_handles").default("{}") // JSON object
    val platformSpecificBios: Column<String> = text("platform_specific_bios").default("{}") // JSON object
    
    // Sync Conflict Management
    val conflictHistory: Column<String> = text("conflict_history").default("[]") // JSON array
    val pendingChanges: Column<String> = text("pending_changes").default("{}") // JSON object
    val syncErrors: Column<String> = text("sync_errors").default("[]") // JSON array
    val manualResolutions: Column<String> = text("manual_resolutions").default("[]") // JSON array
    val autoResolvedConflicts: Column<Int> = integer("auto_resolved_conflicts").default(0)
    val manualResolvedConflicts: Column<Int> = integer("manual_resolved_conflicts").default(0)
    
    // Data Ownership and Control
    val dataOwnership: Column<String> = varchar("data_ownership", 20).default("USER_CONTROLLED") // USER_CONTROLLED, PLATFORM_MANAGED, HYBRID
    val editingPermissions: Column<String> = text("editing_permissions").default("{}") // JSON object
    val fieldLevelPermissions: Column<String> = text("field_level_permissions").default("{}") // JSON object
    val syncPermissions: Column<String> = text("sync_permissions").default("{}") // JSON object
    val platformAccessLevels: Column<String> = text("platform_access_levels").default("{}") // JSON object
    
    // Business and Monetization Unified
    val isBusinessProfile: Column<Boolean> = bool("is_business_profile").default(false)
    val businessCategory: Column<String?> = varchar("business_category", 100).nullable()
    val businessType: Column<String?> = varchar("business_type", 50).nullable()
    val businessLicense: Column<String?> = varchar("business_license", 200).nullable()
    val taxInformation: Column<String> = text("tax_information").default("{}") // JSON object (encrypted)
    val monetizationEnabled: Column<Boolean> = bool("monetization_enabled").default(false)
    val subscriptionTiers: Column<String> = text("subscription_tiers").default("[]") // JSON array
    val paymentMethods: Column<String> = text("payment_methods").default("[]") // JSON array
    
    // Content and Creator Features
    val isCreator: Column<Boolean> = bool("is_creator").default(false)
    val creatorCategory: Column<String?> = varchar("creator_category", 100).nullable()
    val contentTypes: Column<String> = text("content_types").default("[]") // JSON array
    val creativeFocus: Column<String> = text("creative_focus").default("[]") // JSON array
    val collaborationInterests: Column<String> = text("collaboration_interests").default("[]") // JSON array
    val brandPartnerships: Column<String> = text("brand_partnerships").default("[]") // JSON array
    val contentLicensing: Column<String> = text("content_licensing").default("{}") // JSON object
    
    // Accessibility and Assistive Features
    val accessibilityPreferences: Column<String> = text("accessibility_preferences").default("{}") // JSON object
    val assistiveTechnologies: Column<String> = text("assistive_technologies").default("[]") // JSON array
    val visualImpairmentSupport: Column<Boolean> = bool("visual_impairment_support").default(false)
    val hearingImpairmentSupport: Column<Boolean> = bool("hearing_impairment_support").default(false)
    val motorImpairmentSupport: Column<Boolean> = bool("motor_impairment_support").default(false)
    val cognitiveSupport: Column<Boolean> = bool("cognitive_support").default(false)
    val preferredInteractionMethods: Column<String> = text("preferred_interaction_methods").default("[]") // JSON array
    
    // API and Integration Management
    val apiAccess: Column<Boolean> = bool("api_access").default(false)
    val apiKeys: Column<String> = text("api_keys").default("[]") // JSON array (hashed)
    val webhookEndpoints: Column<String> = text("webhook_endpoints").default("[]") // JSON array
    val integratedServices: Column<String> = text("integrated_services").default("[]") // JSON array
    val dataExportFormats: Column<String> = text("data_export_formats").default("[]") // JSON array
    val automationRules: Column<String> = text("automation_rules").default("[]") // JSON array
    
    // Machine Learning and AI Features
    val aiPersonalizationEnabled: Column<Boolean> = bool("ai_personalization_enabled").default(true)
    val contentRecommendationPrefs: Column<String> = text("content_recommendation_prefs").default("{}") // JSON object
    val behavioralPatterns: Column<String> = text("behavioral_patterns").default("{}") // JSON object
    val interactionPreferences: Column<String> = text("interaction_preferences").default("{}") // JSON object
    val mlModelConsent: Column<Boolean> = bool("ml_model_consent").default(false)
    val personalizedExperienceLevel: Column<String> = varchar("personalized_experience_level", 20).default("STANDARD")
    
    // Compliance and Legal Unified
    val gdprCompliance: Column<Boolean> = bool("gdpr_compliance").default(false)
    val ccpaCompliance: Column<Boolean> = bool("ccpa_compliance").default(false)
    val coppaCompliance: Column<Boolean> = bool("coppa_compliance").default(false)
    val dataProcessingConsent: Column<String> = text("data_processing_consent").default("{}") // JSON object
    val rightsExercised: Column<String> = text("rights_exercised").default("[]") // JSON array
    val dataRetentionPreferences: Column<String> = text("data_retention_preferences").default("{}") // JSON object
    val legalDocuments: Column<String> = text("legal_documents").default("[]") // JSON array
    
    // Emergency and Recovery
    val emergencyContacts: Column<String> = text("emergency_contacts").default("[]") // JSON array
    val recoveryMethods: Column<String> = text("recovery_methods").default("[]") // JSON array
    val backupVerificationMethods: Column<String> = text("backup_verification_methods").default("[]") // JSON array
    val emergencyAccessCodes: Column<String?> = text("emergency_access_codes").nullable() // Encrypted
    val legacyContactInformation: Column<String> = text("legacy_contact_information").default("{}") // JSON object
    val digitalWillPreferences: Column<String> = text("digital_will_preferences").default("{}") // JSON object
    
    // Performance and Quality Metrics
    val profileCompleteness: Column<Double> = double("profile_completeness").default(0.0)
    val dataQualityScore: Column<Double> = double("data_quality_score").default(0.0)
    val syncQualityScore: Column<Double> = double("sync_quality_score").default(0.0)
    val userEngagementScore: Column<Double> = double("user_engagement_score").default(0.0)
    val platformHealthScore: Column<Double> = double("platform_health_score").default(0.0)
    val overallSatisfactionScore: Column<Double> = double("overall_satisfaction_score").default(0.0)
    
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
    val archivedAt: Column<Instant?> = timestamp("archived_at").nullable()
    val archiveReason: Column<String?> = varchar("archive_reason", 500).nullable()
}
