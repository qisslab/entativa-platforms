package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Profiles Table Definition for Entativa ID
 * Manages platform-specific user profiles with rich customization and privacy controls
 * 
 * @author Neo Qiss
 * @status Production-ready profile management with cross-platform synchronization
 */
object ProfilesTable : UUIDTable("profiles") {
    
    // Core Profile Information
    val userId: Column<String> = varchar("user_id", 100).index()
    val platformId: Column<String> = varchar("platform_id", 50).index() // GALA, PIKA, PLAYPODS, SONET
    val handle: Column<String> = varchar("handle", 64).index()
    val profileType: Column<String> = varchar("profile_type", 20).default("PERSONAL") // PERSONAL, BUSINESS, CREATOR, BRAND, ORGANIZATION
    
    // Basic Profile Data
    val displayName: Column<String> = varchar("display_name", 200)
    val firstName: Column<String?> = varchar("first_name", 100).nullable()
    val lastName: Column<String?> = varchar("last_name", 100).nullable()
    val middleName: Column<String?> = varchar("middle_name", 100).nullable()
    val preferredName: Column<String?> = varchar("preferred_name", 100).nullable()
    val pronouns: Column<String?> = varchar("pronouns", 50).nullable()
    val title: Column<String?> = varchar("title", 100).nullable() // Professional title
    
    // Extended Profile Information
    val bio: Column<String?> = text("bio").nullable()
    val shortBio: Column<String?> = varchar("short_bio", 500).nullable()
    val tagline: Column<String?> = varchar("tagline", 200).nullable()
    val description: Column<String?> = text("description").nullable()
    val mission: Column<String?> = text("mission").nullable()
    val story: Column<String?> = text("story").nullable()
    
    // Demographics
    val dateOfBirth: Column<Instant?> = timestamp("date_of_birth").nullable()
    val age: Column<Int?> = integer("age").nullable()
    val gender: Column<String?> = varchar("gender", 50).nullable()
    val nationality: Column<String?> = varchar("nationality", 3).nullable() // ISO country code
    val ethnicity: Column<String?> = varchar("ethnicity", 100).nullable()
    val languages: Column<String> = text("languages").default("[]") // JSON array
    val primaryLanguage: Column<String> = varchar("primary_language", 10).default("en")
    
    // Location & Geography
    val location: Column<String?> = varchar("location", 200).nullable()
    val city: Column<String?> = varchar("city", 100).nullable()
    val state: Column<String?> = varchar("state", 100).nullable()
    val country: Column<String?> = varchar("country", 3).nullable() // ISO country code
    val postalCode: Column<String?> = varchar("postal_code", 20).nullable()
    val timezone: Column<String> = varchar("timezone", 50).default("UTC")
    val coordinates: Column<String?> = varchar("coordinates", 50).nullable() // lat,lng
    val locationPublic: Column<Boolean> = bool("location_public").default(false)
    
    // Contact Information
    val email: Column<String?> = varchar("email", 254).nullable()
    val phoneNumber: Column<String?> = varchar("phone_number", 20).nullable()
    val website: Column<String?> = varchar("website", 500).nullable()
    val blogUrl: Column<String?> = varchar("blog_url", 500).nullable()
    val portfolioUrl: Column<String?> = varchar("portfolio_url", 500).nullable()
    val contactPublic: Column<Boolean> = bool("contact_public").default(false)
    val allowDirectMessages: Column<Boolean> = bool("allow_direct_messages").default(true)
    
    // Social Media Links
    val socialLinks: Column<String> = text("social_links").default("{}") // JSON object {twitter: @handle, linkedin: url, etc.}
    val socialVerified: Column<String> = text("social_verified").default("{}") // JSON object with verification status
    val externalProfiles: Column<String> = text("external_profiles").default("[]") // JSON array of external profile links
    
    // Visual Identity
    val avatarUrl: Column<String?> = varchar("avatar_url", 500).nullable()
    val coverImageUrl: Column<String?> = varchar("cover_image_url", 500).nullable()
    val bannerUrl: Column<String?> = varchar("banner_url", 500).nullable()
    val logoUrl: Column<String?> = varchar("logo_url", 500).nullable()
    val galleryImages: Column<String> = text("gallery_images").default("[]") // JSON array of image URLs
    val brandColors: Column<String> = text("brand_colors").default("{}") // JSON object {primary: #hex, secondary: #hex}
    val theme: Column<String> = varchar("theme", 20).default("DEFAULT") // DEFAULT, DARK, LIGHT, CUSTOM
    val customCSS: Column<String?> = text("custom_css").nullable()
    
    // Professional Information
    val occupation: Column<String?> = varchar("occupation", 200).nullable()
    val company: Column<String?> = varchar("company", 200).nullable()
    val companyRole: Column<String?> = varchar("company_role", 100).nullable()
    val industry: Column<String?> = varchar("industry", 100).nullable()
    val skills: Column<String> = text("skills").default("[]") // JSON array
    val expertise: Column<String> = text("expertise").default("[]") // JSON array
    val certifications: Column<String> = text("certifications").default("[]") // JSON array
    val experience: Column<String> = text("experience").default("[]") // JSON array of experience objects
    val education: Column<String> = text("education").default("[]") // JSON array of education objects
    val achievements: Column<String> = text("achievements").default("[]") // JSON array
    
    // Platform-Specific Features
    val platformFeatures: Column<String> = text("platform_features").default("{}") // JSON object
    val platformSettings: Column<String> = text("platform_settings").default("{}") // JSON object
    val customizations: Column<String> = text("customizations").default("{}") // JSON object
    val integrations: Column<String> = text("integrations").default("[]") // JSON array
    val widgets: Column<String> = text("widgets").default("[]") // JSON array
    val layout: Column<String> = text("layout").default("{}") // JSON object
    
    // Content & Media
    val contentCategories: Column<String> = text("content_categories").default("[]") // JSON array
    val contentTags: Column<String> = text("content_tags").default("[]") // JSON array
    val favoriteTopics: Column<String> = text("favorite_topics").default("[]") // JSON array
    val interests: Column<String> = text("interests").default("[]") // JSON array
    val hobbies: Column<String> = text("hobbies").default("[]") // JSON array
    val mediaPreferences: Column<String> = text("media_preferences").default("{}") // JSON object
    
    // Privacy & Visibility
    val profileVisibility: Column<String> = varchar("profile_visibility", 20).default("PUBLIC") // PUBLIC, PRIVATE, FRIENDS_ONLY, CUSTOM
    val searchable: Column<Boolean> = bool("searchable").default(true)
    val indexable: Column<Boolean> = bool("indexable").default(true)
    val showInDirectory: Column<Boolean> = bool("show_in_directory").default(true)
    val allowFollowers: Column<Boolean> = bool("allow_followers").default(true)
    val requireFollowApproval: Column<Boolean> = bool("require_follow_approval").default(false)
    val showFollowerCount: Column<Boolean> = bool("show_follower_count").default(true)
    val showFollowingCount: Column<Boolean> = bool("show_following_count").default(true)
    val showOnlineStatus: Column<Boolean> = bool("show_online_status").default(true)
    val showLastSeen: Column<Boolean> = bool("show_last_seen").default(false)
    
    // Verification & Trust
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    val verifiedAt: Column<Instant?> = timestamp("verified_at").nullable()
    val verificationBadges: Column<String> = text("verification_badges").default("[]") // JSON array
    val trustLevel: Column<String> = varchar("trust_level", 20).default("NORMAL") // LOW, NORMAL, HIGH, VERIFIED
    val credibilityScore: Column<Double> = double("credibility_score").default(0.0)
    val reputationScore: Column<Double> = double("reputation_score").default(0.0)
    val authenticityScore: Column<Double> = double("authenticity_score").default(0.0)
    
    // Engagement & Analytics
    val followerCount: Column<Long> = long("follower_count").default(0)
    val followingCount: Column<Long> = long("following_count").default(0)
    val connectionCount: Column<Long> = long("connection_count").default(0)
    val postCount: Column<Long> = long("post_count").default(0)
    val likeCount: Column<Long> = long("like_count").default(0)
    val commentCount: Column<Long> = long("comment_count").default(0)
    val shareCount: Column<Long> = long("share_count").default(0)
    val viewCount: Column<Long> = long("view_count").default(0)
    val engagementRate: Column<Double> = double("engagement_rate").default(0.0)
    val influenceScore: Column<Double> = double("influence_score").default(0.0)
    
    // Activity & Status
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, INACTIVE, SUSPENDED, ARCHIVED
    val currentStatus: Column<String?> = varchar("current_status", 500).nullable() // Custom status message
    val mood: Column<String?> = varchar("mood", 50).nullable()
    val availability: Column<String> = varchar("availability", 20).default("AVAILABLE") // AVAILABLE, BUSY, AWAY, DO_NOT_DISTURB
    val lastActiveAt: Column<Instant?> = timestamp("last_active_at").nullable()
    val onlineStatus: Column<String> = varchar("online_status", 20).default("OFFLINE") // ONLINE, OFFLINE, AWAY, BUSY
    val presenceMessage: Column<String?> = varchar("presence_message", 200).nullable()
    
    // Synchronization & Cross-Platform
    val isPrimary: Column<Boolean> = bool("is_primary").default(false)
    val isUnified: Column<Boolean> = bool("is_unified").default(true)
    val syncEnabled: Column<Boolean> = bool("sync_enabled").default(true)
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED, DISABLED
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncVersion: Column<Long> = long("sync_version").default(1)
    val conflictResolution: Column<String> = varchar("conflict_resolution", 20).default("LATEST_WINS")
    val syncFailureReason: Column<String?> = text("sync_failure_reason").nullable()
    
    // Preferences & Settings
    val preferences: Column<String> = text("preferences").default("{}") // JSON object
    val notificationSettings: Column<String> = text("notification_settings").default("{}") // JSON object
    val privacySettings: Column<String> = text("privacy_settings").default("{}") // JSON object
    val securitySettings: Column<String> = text("security_settings").default("{}") // JSON object
    val displaySettings: Column<String> = text("display_settings").default("{}") // JSON object
    val contentSettings: Column<String> = text("content_settings").default("{}") // JSON object
    
    // Monetization & Business
    val monetizationEnabled: Column<Boolean> = bool("monetization_enabled").default(false)
    val subscriptionTier: Column<String> = varchar("subscription_tier", 20).default("FREE")
    val premiumFeatures: Column<String> = text("premium_features").default("[]") // JSON array
    val businessAccount: Column<Boolean> = bool("business_account").default(false)
    val businessCategory: Column<String?> = varchar("business_category", 100).nullable()
    val businessLicense: Column<String?> = varchar("business_license", 200).nullable()
    val taxId: Column<String?> = varchar("tax_id", 100).nullable()
    val paymentMethods: Column<String> = text("payment_methods").default("[]") // JSON array
    
    // Content Moderation
    val moderationStatus: Column<String> = varchar("moderation_status", 20).default("APPROVED") // PENDING, APPROVED, FLAGGED, RESTRICTED
    val contentWarnings: Column<String> = text("content_warnings").default("[]") // JSON array
    val restrictionReason: Column<String?> = text("restriction_reason").nullable()
    val appealStatus: Column<String?> = varchar("appeal_status", 20).nullable()
    val lastModeratedAt: Column<Instant?> = timestamp("last_moderated_at").nullable()
    val moderatedBy: Column<String?> = varchar("moderated_by", 100).nullable()
    
    // Compliance & Legal
    val ageVerified: Column<Boolean> = bool("age_verified").default(false)
    val identityVerified: Column<Boolean> = bool("identity_verified").default(false)
    val businessVerified: Column<Boolean> = bool("business_verified").default(false)
    val complianceLevel: Column<String> = varchar("compliance_level", 20).default("STANDARD")
    val regulatoryFlags: Column<String> = text("regulatory_flags").default("[]") // JSON array
    val dataProcessingConsent: Column<Boolean> = bool("data_processing_consent").default(false)
    val marketingConsent: Column<Boolean> = bool("marketing_consent").default(false)
    val analyticsConsent: Column<Boolean> = bool("analytics_consent").default(true)
    
    // Custom Fields & Extensions
    val customFields: Column<String> = text("custom_fields").default("{}") // JSON object
    val metadata: Column<String> = text("metadata").default("{}") // JSON object
    val extensions: Column<String> = text("extensions").default("{}") // JSON object
    val apiData: Column<String> = text("api_data").default("{}") // JSON object for API integrations
    val thirdPartyData: Column<String> = text("third_party_data").default("{}") // JSON object
    
    // Performance & Caching
    val cacheVersion: Column<Long> = long("cache_version").default(1)
    val lastCacheUpdate: Column<Instant?> = timestamp("last_cache_update").nullable()
    val searchIndexed: Column<Boolean> = bool("search_indexed").default(true)
    val searchKeywords: Column<String> = text("search_keywords").default("[]") // JSON array
    val popularityScore: Column<Double> = double("popularity_score").default(0.0)
    
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
    val archiveDate: Column<Instant?> = timestamp("archive_date").nullable()
    val archiveReason: Column<String?> = varchar("archive_reason", 500).nullable()
}
