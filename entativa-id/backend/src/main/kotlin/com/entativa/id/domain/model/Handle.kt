package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * Handle Management Models
 * Comprehensive handle validation, reservation, and protection system
 * 
 * @author Neo Qiss
 * @status Production-ready with anti-impersonation features
 */

/**
 * Handle validation result with detailed feedback
 */
@Serializable
data class HandleValidationResult(
    val handle: String,
    val isAvailable: Boolean,
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val requiresVerification: Boolean = false,
    val similarToProtected: SimilarityMatch? = null,
    val reservationType: String? = null,
    val estimatedApprovalTime: String? = null
)

/**
 * Similarity match for protected handles
 */
@Serializable
data class SimilarityMatch(
    val originalHandle: String,
    val entityName: String,
    val entityType: EntityType,
    val similarityScore: Double,
    val threshold: Double,
    val canClaim: Boolean = false
)

/**
 * Well-known figure in our protection database
 */
@Serializable
data class WellKnownFigure(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: FigureCategory,
    val preferredHandle: String,
    val alternativeHandles: List<String> = emptyList(),
    val verificationLevel: VerificationLevel,
    val wikipediaUrl: String? = null,
    val verifiedSocialAccounts: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val claimedBy: String? = null,
    val claimedAt: String? = null,
    val createdAt: String = Instant.now().toString()
)

/**
 * Well-known company in our protection database
 */
@Serializable
data class WellKnownCompany(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val legalName: String? = null,
    val industry: String,
    val companyType: CompanyType,
    val preferredHandle: String,
    val alternativeHandles: List<String> = emptyList(),
    val stockSymbol: String? = null,
    val foundedYear: Int? = null,
    val headquartersCountry: String? = null,
    val website: String? = null,
    val linkedinUrl: String? = null,
    val verificationLevel: VerificationLevel,
    val requiredDocuments: List<DocumentType> = emptyList(),
    val isActive: Boolean = true,
    val claimedBy: String? = null,
    val claimedAt: String? = null,
    val createdAt: String = Instant.now().toString()
)

/**
 * Reserved handle for system use
 */
@Serializable
data class ReservedHandle(
    val id: String = UUID.randomUUID().toString(),
    val handle: String,
    val reservationType: ReservationType,
    val platform: Platform? = null, // null means all platforms
    val reason: String,
    val reservedUntil: String? = null, // null means permanent
    val canBeReleased: Boolean = false,
    val createdAt: String = Instant.now().toString(),
    val createdBy: String = "system"
)

/**
 * Protected handle with similarity detection
 */
@Serializable
data class ProtectedHandle(
    val id: String = UUID.randomUUID().toString(),
    val originalHandle: String,
    val protectedEntityType: EntityType,
    val protectedEntityId: String,
    val similarityThreshold: Double = 0.85,
    val createdAt: String = Instant.now().toString()
)

/**
 * Handle claim request for celebrities/companies
 */
@Serializable
data class HandleClaimRequest(
    val userId: String,
    val requestedHandle: String,
    val entityType: EntityType,
    val entityId: String? = null,
    val claimType: ClaimType,
    val supportingDocuments: List<String> = emptyList(),
    val applicantNotes: String? = null,
    val contactEmail: String,
    val contactPhone: String? = null
)

/**
 * Handle claim response
 */
@Serializable
data class HandleClaimResponse(
    val success: Boolean,
    val claimId: String? = null,
    val status: ClaimStatus? = null,
    val estimatedReviewTime: String? = null,
    val requiredDocuments: List<DocumentType> = emptyList(),
    val errors: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList()
)

// ============== ENUMS ==============

@Serializable
enum class EntityType {
    WELL_KNOWN_FIGURE,
    COMPANY,
    RESERVED_HANDLE,
    GOVERNMENT,
    ORGANIZATION
}

@Serializable
enum class FigureCategory {
    CELEBRITY,
    POLITICIAN,
    ATHLETE,
    MUSICIAN,
    ACTOR,
    AUTHOR,
    INFLUENCER,
    JOURNALIST,
    SCIENTIST,
    BUSINESS_LEADER,
    ARTIST,
    ACTIVIST,
    OTHER
}

@Serializable
enum class CompanyType {
    PUBLIC_COMPANY,
    PRIVATE_COMPANY,
    NONPROFIT,
    GOVERNMENT,
    EDUCATIONAL,
    STARTUP,
    PARTNERSHIP,
    SOLE_PROPRIETORSHIP
}

@Serializable
enum class VerificationLevel {
    ULTRA_HIGH,  // Elon Musk, Taylor Swift, Joe Biden
    HIGH,        // Public figures, major companies
    MEDIUM,      // Regional figures, smaller companies
    LOW          // Local figures, small businesses
}

@Serializable
enum class ReservationType {
    ENTATIVA_SYSTEM,      // @entativa, @admin, @support
    PLATFORM_SPECIFIC,   // @sonet, @gala, @pika, @playpods
    FUTURE_EXPANSION,     // @marketplace, @creator, @premium
    TRADEMARK_PROTECTION, // Protected trademark handles
    ABUSE_PREVENTION,     // @abuse, @spam, @fake, @bot
    LEGAL_COMPLIANCE     // Legally required reservations
}

@Serializable
enum class Platform {
    ALL,
    SONET,
    GALA,
    PIKA,
    PLAYPODS
}

@Serializable
enum class ClaimType {
    WELL_KNOWN_FIGURE,
    COMPANY_OFFICIAL,
    GENERAL_VERIFICATION,
    TRADEMARK_HOLDER,
    GOVERNMENT_OFFICIAL
}

@Serializable
enum class ClaimStatus {
    PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    REQUIRES_MORE_INFO,
    DOCUMENTS_NEEDED
}

@Serializable
enum class DocumentType {
    GOVERNMENT_ID,
    PASSPORT,
    BUSINESS_REGISTRATION,
    TAX_DOCUMENT,
    DOMAIN_OWNERSHIP,
    SOCIAL_MEDIA_PROOF,
    EMPLOYMENT_VERIFICATION,
    TRADEMARK_CERTIFICATE,
    COURT_DOCUMENT,
    OFFICIAL_LETTERHEAD,
    OTHER
}

/**
 * Handle suggestion request
 */
@Serializable
data class HandleSuggestionRequest(
    val baseHandle: String,
    val preferredLength: Int? = null,
    val includeNumbers: Boolean = true,
    val includeUnderscores: Boolean = true,
    val excludeSimilarToProtected: Boolean = true
)

/**
 * Handle suggestion response
 */
@Serializable
data class HandleSuggestionResponse(
    val baseHandle: String,
    val suggestions: List<HandleSuggestion>
)

@Serializable
data class HandleSuggestion(
    val handle: String,
    val available: Boolean,
    val reason: String,
    val priority: Int // 1 = highest priority
)

/**
 * Handle search filters for admin
 */
@Serializable
data class HandleSearchFilter(
    val query: String? = null,
    val entityType: EntityType? = null,
    val verificationLevel: VerificationLevel? = null,
    val category: FigureCategory? = null,
    val industry: String? = null,
    val claimed: Boolean? = null,
    val active: Boolean? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Bulk handle operation request
 */
@Serializable
data class BulkHandleOperation(
    val operation: BulkOperation,
    val handles: List<String>,
    val reason: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class BulkOperation {
    RESERVE,
    RELEASE,
    PROTECT,
    UNPROTECT,
    IMPORT_FIGURES,
    IMPORT_COMPANIES
}

/**
 * Handle analytics for admin dashboard
 */
@Serializable
data class HandleAnalytics(
    val totalReserved: Int,
    val totalProtected: Int,
    val totalClaimed: Int,
    val pendingClaims: Int,
    val topSimilarityMatches: List<SimilarityMatch>,
    val recentClaims: List<HandleClaimSummary>,
    val popularSuggestions: List<String>
)

@Serializable
data class HandleClaimSummary(
    val claimId: String,
    val requestedHandle: String,
    val entityName: String,
    val entityType: EntityType,
    val status: ClaimStatus,
    val submittedAt: String,
    val reviewedAt: String? = null
)
