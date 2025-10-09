package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * Entativa ID User - Core identity representation
 * The foundational identity that spans across all Entativa platforms
 * 
 * @author Neo Qiss
 * @status Production-ready with comprehensive security features
 */
@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val eid: String, // The unique @handle across all platforms
    val email: String,
    val phone: String? = null,
    val passwordHash: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val profileCompleted: Boolean = false,
    
    // Verification and reputation
    val verificationStatus: VerificationStatus = VerificationStatus.NONE,
    val verificationBadge: VerificationBadge? = null,
    val verificationDate: String? = null, // ISO-8601 timestamp
    val reputationScore: Int = 1000,
    
    // Security tracking
    val failedLoginAttempts: Int = 0,
    val lockedUntil: String? = null, // ISO-8601 timestamp
    val passwordChangedAt: String = Instant.now().toString(),
    val lastLoginAt: String? = null,
    
    // Metadata
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString(),
    val createdBy: String = "self_registration",
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val countryCode: String? = null,
    
    // Privacy settings
    val privacySettings: PrivacySettings = PrivacySettings()
)

@Serializable
enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
    PENDING_VERIFICATION,
    PENDING_DELETION
}

@Serializable
enum class VerificationStatus {
    NONE,
    PENDING,
    VERIFIED,
    CELEBRITY,
    COMPANY,
    GOVERNMENT
}

@Serializable
enum class VerificationBadge {
    BLUE,       // General verification
    GOLD,       // Celebrity/VIP
    BUSINESS,   // Company/Organization
    GOVERNMENT  // Government/Official
}

@Serializable
data class PrivacySettings(
    val profileVisibility: Visibility = Visibility.PUBLIC,
    val emailVisibility: Visibility = Visibility.PRIVATE,
    val phoneVisibility: Visibility = Visibility.PRIVATE,
    val birthdateVisibility: Visibility = Visibility.PRIVATE,
    val locationVisibility: Visibility = Visibility.PUBLIC,
    val allowSearchByEmail: Boolean = false,
    val allowSearchByPhone: Boolean = false,
    val allowPlatformDataSharing: Boolean = true,
    val allowAnalyticsTracking: Boolean = true,
    val marketingEmailsEnabled: Boolean = false
)

@Serializable
enum class Visibility {
    PUBLIC,
    FRIENDS,
    PRIVATE
}

/**
 * User creation request
 */
@Serializable
data class CreateUserRequest(
    val eid: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val firstName: String,
    val lastName: String,
    val displayName: String? = null,
    val dateOfBirth: String? = null, // ISO-8601 date
    val countryCode: String? = null,
    val acceptedTerms: Boolean,
    val acceptedPrivacy: Boolean,
    val marketingOptIn: Boolean = false,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * User update request
 */
@Serializable
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val privacySettings: PrivacySettings? = null
)

/**
 * User response (safe for API responses)
 */
@Serializable
data class UserResponse(
    val id: String,
    val eid: String,
    val email: String,
    val phone: String? = null,
    val status: UserStatus,
    val emailVerified: Boolean,
    val phoneVerified: Boolean,
    val twoFactorEnabled: Boolean,
    val profileCompleted: Boolean,
    val verificationStatus: VerificationStatus,
    val verificationBadge: VerificationBadge? = null,
    val verificationDate: String? = null,
    val reputationScore: Int,
    val createdAt: String,
    val lastLoginAt: String? = null,
    val profile: ProfileSummary? = null,
    val connectedPlatforms: List<String> = emptyList(),
    val securitySummary: SecuritySummary
)

@Serializable
data class ProfileSummary(
    val firstName: String,
    val lastName: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val website: String? = null
)

@Serializable
data class SecuritySummary(
    val twoFactorEnabled: Boolean,
    val lastPasswordChange: String,
    val activeSessions: Int,
    val connectedApps: Int,
    val securityScore: Int // 0-100 based on security practices
)
