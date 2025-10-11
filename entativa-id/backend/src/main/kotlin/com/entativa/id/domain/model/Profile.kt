package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

/**
 * Profile Domain Models for Entativa ID
 * Platform-specific and unified profile management with cross-platform integration
 * 
 * @author Neo Qiss
 * @status Production-ready profile system with cross-platform sync
 */

/**
 * Profile - Platform-specific profile entity
 */
@Serializable
data class Profile(
    val id: String,
    val userId: String,
    val platform: String, // SONET, GALA, PIKA, PLAYPODS, ENTATIVA_ID
    val handle: String,
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val isPublic: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val profileType: String = "PERSONAL", // PERSONAL, BUSINESS, CREATOR, ORGANIZATION
    val visibility: String = "PUBLIC", // PUBLIC, FRIENDS, PRIVATE, CUSTOM
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String = "en",
    val birthDate: LocalDate? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: String? = null, // JSON array of interests
    val customFields: String? = null, // JSON object for platform-specific fields
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val postCount: Long = 0,
    val loginCount: Long = 0,
    val lastActiveAt: Instant? = null,
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED, DISABLED
    val syncData: String? = null, // JSON sync metadata
    val lastSyncedAt: Instant? = null,
    val syncFailureReason: String? = null,
    val syncRetryCount: Int = 0,
    val reputation: Double = 0.0,
    val trustScore: Double = 50.0,
    val verificationLevel: String = "NONE", // NONE, BASIC, VERIFIED, PREMIUM
    val verificationDocuments: String? = null, // JSON array of verification docs
    val verifiedAt: Instant? = null,
    val verifiedBy: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * UnifiedProfile - Cross-platform unified profile
 */
@Serializable
data class UnifiedProfile(
    val id: String,
    val userId: String,
    val globalHandle: String, // The @handle across all platforms
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val isPublic: Boolean = true,
    val isVerified: Boolean = false,
    val profileType: String = "PERSONAL",
    val visibility: String = "PUBLIC",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String = "en",
    val birthDate: LocalDate? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: String? = null, // JSON array of unified interests
    val socialLinks: String? = null, // JSON object of social media links
    val customFields: String? = null, // JSON object for custom fields
    val connectedPlatforms: String? = null, // JSON array of connected platforms
    val syncEnabledPlatforms: String? = null, // JSON array of platforms with sync enabled
    val syncedPlatforms: String? = null, // JSON array of successfully synced platforms
    val syncStatus: String = "PENDING",
    val lastSyncedAt: Instant? = null,
    val lastSyncData: String? = null, // JSON sync metadata
    val syncRetryCount: Int = 0,
    val totalSyncs: Long = 0,
    val aggregatedFollowerCount: Long = 0, // Sum from all platforms
    val aggregatedFollowingCount: Long = 0,
    val aggregatedPostCount: Long = 0,
    val globalReputation: Double = 0.0,
    val globalTrustScore: Double = 50.0,
    val verificationLevel: String = "NONE",
    val verificationDocuments: String? = null,
    val verifiedAt: Instant? = null,
    val verifiedBy: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * Create profile request
 */
@Serializable
data class CreateProfileRequest(
    val userId: String,
    val platform: String,
    val handle: String,
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val isPublic: Boolean = true,
    val isVerified: Boolean = false,
    val isPrimary: Boolean = false,
    val profileType: String = "PERSONAL",
    val visibility: String = "PUBLIC",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String = "en",
    val birthDate: LocalDate? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: String? = null,
    val customFields: String? = null,
    val createdBy: String
)

/**
 * Create unified profile request
 */
@Serializable
data class CreateUnifiedProfileRequest(
    val userId: String,
    val globalHandle: String,
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean = true,
    val isPublic: Boolean = true,
    val isVerified: Boolean = false,
    val profileType: String = "PERSONAL",
    val visibility: String = "PUBLIC",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String = "en",
    val birthDate: LocalDate? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: String? = null,
    val socialLinks: String? = null,
    val customFields: String? = null,
    val syncEnabledPlatforms: String? = null,
    val createdBy: String
)

/**
 * Update profile request
 */
@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: String? = null,
    val visibility: String? = null,
    val isPublic: Boolean? = null,
    val customFields: String? = null,
    val updatedBy: String
)

/**
 * Profile response for API
 */
@Serializable
data class ProfileResponse(
    val id: String,
    val platform: String,
    val handle: String,
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean,
    val isPublic: Boolean,
    val isVerified: Boolean,
    val isPrimary: Boolean,
    val profileType: String,
    val visibility: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String> = emptyList(),
    val followerCount: Long,
    val followingCount: Long,
    val postCount: Long,
    val reputation: Double,
    val trustScore: Double,
    val verificationLevel: String,
    val lastActiveAt: Instant? = null,
    val createdAt: Instant
)

/**
 * Unified profile response for API
 */
@Serializable
data class UnifiedProfileResponse(
    val id: String,
    val globalHandle: String,
    val displayName: String? = null,
    val bio: String? = null,
    val isActive: Boolean,
    val isPublic: Boolean,
    val isVerified: Boolean,
    val profileType: String,
    val visibility: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val website: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String> = emptyList(),
    val socialLinks: Map<String, String> = emptyMap(),
    val connectedPlatforms: List<String> = emptyList(),
    val aggregatedFollowerCount: Long,
    val aggregatedFollowingCount: Long,
    val aggregatedPostCount: Long,
    val globalReputation: Double,
    val globalTrustScore: Double,
    val verificationLevel: String,
    val createdAt: Instant
)

/**
 * Profile synchronization request
 */
@Serializable
data class ProfileSyncRequest(
    val profileId: String,
    val targetPlatforms: List<String>,
    val syncFields: List<String> = emptyList(), // Empty = sync all
    val overwriteExisting: Boolean = false,
    val validateBeforeSync: Boolean = true,
    val initiatedBy: String
)

/**
 * Profile synchronization response
 */
@Serializable
data class ProfileSyncResponse(
    val syncId: String,
    val status: SyncStatus,
    val targetPlatforms: List<String>,
    val syncResults: List<PlatformSyncResult>,
    val summary: SyncSummary,
    val initiatedAt: Instant,
    val completedAt: Instant? = null
)

@Serializable
data class PlatformSyncResult(
    val platform: String,
    val status: SyncStatus,
    val syncedFields: List<String> = emptyList(),
    val failedFields: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Serializable
data class SyncSummary(
    val totalPlatforms: Int,
    val successfulPlatforms: Int,
    val failedPlatforms: Int,
    val partialPlatforms: Int,
    val totalFields: Int,
    val syncedFields: Int,
    val failedFields: Int
)

@Serializable
enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PARTIAL,
    CANCELLED
}

/**
 * Profile verification request
 */
@Serializable
data class ProfileVerificationRequest(
    val profileId: String,
    val verificationType: VerificationType,
    val documents: List<VerificationDocument>,
    val additionalInfo: String? = null,
    val requestedBy: String
)

@Serializable
data class VerificationDocument(
    val type: DocumentType,
    val url: String,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val checksum: String? = null
)

@Serializable
enum class VerificationType {
    IDENTITY,
    BUSINESS,
    CELEBRITY,
    INFLUENCER,
    ORGANIZATION,
    GOVERNMENT,
    PREMIUM
}

@Serializable
enum class DocumentType {
    GOVERNMENT_ID,
    PASSPORT,
    BUSINESS_LICENSE,
    TAX_CERTIFICATE,
    SOCIAL_MEDIA_VERIFICATION,
    WEBSITE_VERIFICATION,
    EMPLOYMENT_LETTER,
    NEWS_ARTICLE,
    WIKIPEDIA_PAGE,
    OFFICIAL_STATEMENT,
    OTHER
}

/**
 * Profile verification response
 */
@Serializable
data class ProfileVerificationResponse(
    val verificationId: String,
    val status: VerificationStatus,
    val estimatedProcessingTime: String,
    val requiredActions: List<String> = emptyList(),
    val submittedAt: Instant
)

@Serializable
enum class VerificationStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    REQUIRES_MORE_INFO
}

/**
 * Profile analytics
 */
@Serializable
data class ProfileAnalytics(
    val profileId: String,
    val platform: String? = null, // null for unified profile
    val period: AnalyticsPeriod,
    val views: Long,
    val uniqueViews: Long,
    val followerGrowth: Long,
    val engagement: ProfileEngagement,
    val contentMetrics: ContentMetrics,
    val audienceMetrics: AudienceMetrics,
    val generatedAt: Instant
)

@Serializable
data class ProfileEngagement(
    val totalEngagements: Long,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val mentions: Long,
    val engagementRate: Double
)

@Serializable
data class ContentMetrics(
    val postsCreated: Long,
    val storiesCreated: Long,
    val mediaUploaded: Long,
    val averagePostEngagement: Double,
    val topPerformingContent: List<String> = emptyList()
)

@Serializable
data class AudienceMetrics(
    val totalReach: Long,
    val uniqueReach: Long,
    val impressions: Long,
    val topCountries: Map<String, Long> = emptyMap(),
    val topAgeGroups: Map<String, Long> = emptyMap(),
    val genderDistribution: Map<String, Long> = emptyMap()
)

/**
 * Profile privacy settings
 */
@Serializable
data class ProfilePrivacySettings(
    val profileId: String,
    val showEmail: Boolean = false,
    val showPhone: Boolean = false,
    val showBirthDate: Boolean = false,
    val showLocation: Boolean = true,
    val showLastActive: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val allowTagging: Boolean = true,
    val allowMentions: Boolean = true,
    val allowSearchByEmail: Boolean = false,
    val allowSearchByPhone: Boolean = false,
    val blockedUsers: List<String> = emptyList(),
    val restrictedUsers: List<String> = emptyList(),
    val customVisibilityRules: String? = null, // JSON rules
    val updatedAt: Instant,
    val updatedBy: String
)

/**
 * Profile content moderation
 */
@Serializable
data class ProfileModerationInfo(
    val profileId: String,
    val moderationLevel: ModerationLevel,
    val contentWarnings: List<String> = emptyList(),
    val restrictionReasons: List<String> = emptyList(),
    val isUnderReview: Boolean = false,
    val reviewStartedAt: Instant? = null,
    val lastModeratedAt: Instant? = null,
    val moderatedBy: String? = null,
    val moderationNotes: String? = null,
    val appealCount: Int = 0,
    val lastAppealAt: Instant? = null
)

@Serializable
enum class ModerationLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    RESTRICTED,
    SUSPENDED
}

/**
 * Profile constants and enums
 */
object ProfileTypes {
    const val PERSONAL = "PERSONAL"
    const val BUSINESS = "BUSINESS"
    const val CREATOR = "CREATOR"
    const val ORGANIZATION = "ORGANIZATION"
    const val GOVERNMENT = "GOVERNMENT"
    const val BOT = "BOT"
}

object Platforms {
    const val SONET = "SONET"
    const val GALA = "GALA"
    const val PIKA = "PIKA"
    const val PLAYPODS = "PLAYPODS"
    const val ENTATIVA_ID = "ENTATIVA_ID"
}

object VisibilityLevels {
    const val PUBLIC = "PUBLIC"
    const val FRIENDS = "FRIENDS"
    const val PRIVATE = "PRIVATE"
    const val CUSTOM = "CUSTOM"
}

object VerificationLevels {
    const val NONE = "NONE"
    const val BASIC = "BASIC"
    const val VERIFIED = "VERIFIED"
    const val PREMIUM = "PREMIUM"
    const val CELEBRITY = "CELEBRITY"
    const val BUSINESS = "BUSINESS"
    const val GOVERNMENT = "GOVERNMENT"
}
