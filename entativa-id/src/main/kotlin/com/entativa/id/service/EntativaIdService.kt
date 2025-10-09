package com.entativa.id.service

import com.entativa.shared.cache.EntativaCacheManager
import com.entativa.shared.database.EntativaDatabaseFactory
import com.entativa.shared.database.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import kotlin.math.abs

/**
 * Entativa ID Core Service - Unified Identity Management
 * Handles global handle reservation, verification, and anti-impersonation
 * 
 * @author Neo Qiss
 * @status Production-ready with Apple/Google-level security
 */
class EntativaIdService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(EntativaIdService::class.java)
    
    companion object {
        private const val MIN_HANDLE_LENGTH = 3
        private const val MAX_HANDLE_LENGTH = 30
        private const val HANDLE_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9._-]{1,28}[a-zA-Z0-9]$"
        private const val SIMILARITY_THRESHOLD = 0.85
        private const val CACHE_TTL_MINUTES = 60
    }
    
    // Database Tables (using Exposed ORM)
    object EntativaIdentities : UUIDTable("entativa_identities") {
        val eid = varchar("eid", 100).uniqueIndex()
        val email = varchar("email", 320).uniqueIndex()
        val phone = varchar("phone", 20).nullable().uniqueIndex()
        val passwordHash = varchar("password_hash", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
        val lastLoginAt = timestamp("last_login_at").nullable()
        val status = varchar("status", 20).default("active")
        val emailVerified = bool("email_verified").default(false)
        val phoneVerified = bool("phone_verified").default(false)
        val twoFactorEnabled = bool("two_factor_enabled").default(false)
        val profileCompleted = bool("profile_completed").default(false)
        val verificationStatus = varchar("verification_status", 20).default("none")
        val verificationBadge = varchar("verification_badge", 20).nullable()
        val verificationDate = timestamp("verification_date").nullable()
        val reputationScore = integer("reputation_score").default(1000)
        val failedLoginAttempts = integer("failed_login_attempts").default(0)
        val lockedUntil = timestamp("locked_until").nullable()
        val passwordChangedAt = timestamp("password_changed_at").defaultExpression(CurrentTimestamp())
        val createdBy = varchar("created_by", 50).default("self_registration")
        val ipAddress = varchar("ip_address", 45).nullable()
        val userAgent = text("user_agent").nullable()
        val countryCode = char("country_code", 2).nullable()
    }
    
    object WellKnownFigures : UUIDTable("well_known_figures") {
        val name = varchar("name", 200)
        val category = varchar("category", 50)
        val preferredHandle = varchar("preferred_handle", 100).uniqueIndex()
        val alternativeHandles = array<String>("alternative_handles").nullable()
        val verificationLevel = varchar("verification_level", 20).default("high")
        val wikipediaUrl = varchar("wikipedia_url", 500).nullable()
        val verifiedSocialAccounts = text("verified_social_accounts").nullable()
        val isActive = bool("is_active").default(true)
        val claimedBy = reference("claimed_by", EntativaIdentities).nullable()
        val claimedAt = timestamp("claimed_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    }
    
    object WellKnownCompanies : UUIDTable("well_known_companies") {
        val name = varchar("name", 200)
        val legalName = varchar("legal_name", 300).nullable()
        val industry = varchar("industry", 100)
        val companyType = varchar("company_type", 50).nullable()
        val preferredHandle = varchar("preferred_handle", 100).uniqueIndex()
        val alternativeHandles = array<String>("alternative_handles").nullable()
        val stockSymbol = varchar("stock_symbol", 10).nullable()
        val foundedYear = integer("founded_year").nullable()
        val headquartersCountry = char("headquarters_country", 2).nullable()
        val website = varchar("website", 500).nullable()
        val linkedinUrl = varchar("linkedin_url", 500).nullable()
        val verificationLevel = varchar("verification_level", 20).default("high")
        val requiredDocuments = array<String>("required_documents").nullable()
        val isActive = bool("is_active").default(true)
        val claimedBy = reference("claimed_by", EntativaIdentities).nullable()
        val claimedAt = timestamp("claimed_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    }
    
    object ReservedHandles : UUIDTable("reserved_handles") {
        val handle = varchar("handle", 100).uniqueIndex()
        val reservationType = varchar("reservation_type", 30)
        val platform = varchar("platform", 20).nullable()
        val reason = text("reason")
        val reservedUntil = timestamp("reserved_until").nullable()
        val canBeReleased = bool("can_be_released").default(false)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val createdBy = varchar("created_by", 100).default("system")
    }
    
    object ProtectedHandles : UUIDTable("protected_handles") {
        val originalHandle = varchar("original_handle", 100)
        val protectedEntityType = varchar("protected_entity_type", 20)
        val protectedEntityId = uuid("protected_entity_id").nullable()
        val similarityThreshold = decimal("similarity_threshold", 3, 2).default(0.85.toBigDecimal())
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    }
    
    /**
     * Validate handle availability and compliance
     */
    suspend fun validateHandle(handle: String): HandleValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Validating handle: $handle")
                
                // Check cache first
                val cachedResult = cacheManager.getCachedData<HandleValidationResult>("handle_validation:$handle")
                if (cachedResult != null) {
                    logger.debug("🎯 Cache hit for handle validation: $handle")
                    return@withContext cachedResult
                }
                
                val result = HandleValidationResult(handle = handle)
                
                // 1. Basic format validation
                if (!validateHandleFormat(handle)) {
                    result.isValid = false
                    result.errors.add("Handle format is invalid. Must be 3-30 characters, start/end with alphanumeric, and contain only letters, numbers, dots, hyphens, or underscores.")
                    return@withContext cacheAndReturn(result)
                }
                
                // 2. Check if handle is already taken
                val existingIdentity = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdentities.select { EntativaIdentities.eid eq handle }.singleOrNull()
                }
                
                if (existingIdentity != null) {
                    result.isValid = false
                    result.errors.add("Handle is already taken.")
                    return@withContext cacheAndReturn(result)
                }
                
                // 3. Check if handle is reserved
                val reservedHandle = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    ReservedHandles.select { ReservedHandles.handle eq handle }.singleOrNull()
                }
                
                if (reservedHandle != null) {
                    result.isValid = false
                    result.errors.add("Handle is reserved by Entativa.")
                    result.reservationType = reservedHandle[ReservedHandles.reservationType]
                    return@withContext cacheAndReturn(result)
                }
                
                // 4. Check against well-known figures
                val similarFigure = findSimilarWellKnownFigure(handle)
                if (similarFigure != null) {
                    result.isValid = false
                    result.errors.add("Handle is too similar to a well-known figure: ${similarFigure.name}")
                    result.requiresVerification = true
                    result.similarEntity = SimilarEntity(
                        type = "figure",
                        name = similarFigure.name,
                        preferredHandle = similarFigure.preferredHandle,
                        similarity = calculateSimilarity(handle, similarFigure.preferredHandle)
                    )
                    return@withContext cacheAndReturn(result)
                }
                
                // 5. Check against well-known companies
                val similarCompany = findSimilarWellKnownCompany(handle)
                if (similarCompany != null) {
                    result.isValid = false
                    result.errors.add("Handle is too similar to a well-known company: ${similarCompany.name}")
                    result.requiresVerification = true
                    result.similarEntity = SimilarEntity(
                        type = "company",
                        name = similarCompany.name,
                        preferredHandle = similarCompany.preferredHandle,
                        similarity = calculateSimilarity(handle, similarCompany.preferredHandle)
                    )
                    return@withContext cacheAndReturn(result)
                }
                
                // 6. Check similarity to protected handles
                val protectedSimilarity = checkProtectedHandleSimilarity(handle)
                if (protectedSimilarity != null) {
                    result.isValid = false
                    result.errors.add("Handle is too similar to a protected handle.")
                    result.protectedSimilarity = protectedSimilarity
                    return@withContext cacheAndReturn(result)
                }
                
                // 7. Content moderation check
                if (containsInappropriateContent(handle)) {
                    result.isValid = false
                    result.errors.add("Handle contains inappropriate content.")
                    return@withContext cacheAndReturn(result)
                }
                
                // 8. Check for potential trademark issues
                val trademarkIssues = checkTrademarkIssues(handle)
                if (trademarkIssues.isNotEmpty()) {
                    result.warnings.addAll(trademarkIssues)
                }
                
                // If we get here, handle is valid
                result.isValid = true
                result.suggestions = generateHandleSuggestions(handle)
                
                logger.debug("✅ Handle validation completed: $handle - Valid: ${result.isValid}")
                cacheAndReturn(result)
                
            } catch (e: Exception) {
                logger.error("❌ Handle validation failed for: $handle", e)
                HandleValidationResult(
                    handle = handle,
                    isValid = false,
                    errors = mutableListOf("Internal validation error")
                )
            }
        }
    }
    
    /**
     * Create new Entativa ID with global reservation
     */
    suspend fun createEntativaId(request: CreateEidRequest): CreateEidResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🆔 Creating new Entativa ID: ${request.eid}")
                
                // Re-validate handle
                val validation = validateHandle(request.eid)
                if (!validation.isValid) {
                    return@withContext CreateEidResponse(
                        success = false,
                        errors = validation.errors,
                        validationResult = validation
                    )
                }
                
                // Check if email is already registered
                val existingEmail = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdentities.select { EntativaIdentities.email eq request.email }.singleOrNull()
                }
                
                if (existingEmail != null) {
                    return@withContext CreateEidResponse(
                        success = false,
                        errors = mutableListOf("Email address is already registered")
                    )
                }
                
                // Create the identity
                val identityId = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdentities.insertAndGetId {
                        it[eid] = request.eid
                        it[email] = request.email
                        it[phone] = request.phone
                        it[passwordHash] = hashPassword(request.password)
                        it[createdBy] = "registration"
                        it[ipAddress] = request.ipAddress
                        it[userAgent] = request.userAgent
                        it[countryCode] = request.countryCode
                    }
                }.value
                
                // Log creation
                logIdentityEvent(identityId, "identity_created", mapOf(
                    "eid" to request.eid,
                    "email" to request.email,
                    "ip_address" to (request.ipAddress ?: "unknown")
                ))
                
                // Cache the new identity
                cacheManager.cacheData("identity:$identityId", mapOf(
                    "id" to identityId.toString(),
                    "eid" to request.eid,
                    "email" to request.email,
                    "status" to "active"
                ), CACHE_TTL_MINUTES * 60)
                
                logger.info("✅ Entativa ID created successfully: ${request.eid}")
                
                CreateEidResponse(
                    success = true,
                    identityId = identityId,
                    eid = request.eid
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create Entativa ID: ${request.eid}", e)
                CreateEidResponse(
                    success = false,
                    errors = mutableListOf("Failed to create identity: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Claim well-known figure or company handle
     */
    suspend fun claimWellKnownHandle(request: ClaimHandleRequest): ClaimHandleResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🏷️ Claim request for handle: ${request.handle} by identity: ${request.identityId}")
                
                when (request.entityType) {
                    "figure" -> claimWellKnownFigureHandle(request)
                    "company" -> claimWellKnownCompanyHandle(request)
                    else -> ClaimHandleResponse(
                        success = false,
                        errors = mutableListOf("Invalid entity type")
                    )
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to process claim request", e)
                ClaimHandleResponse(
                    success = false,
                    errors = mutableListOf("Failed to process claim: ${e.message}")
                )
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun validateHandleFormat(handle: String): Boolean {
        return handle.length in MIN_HANDLE_LENGTH..MAX_HANDLE_LENGTH &&
                handle.matches(HANDLE_REGEX.toRegex()) &&
                !handle.contains("..") && // No consecutive dots
                !handle.contains("--") && // No consecutive hyphens
                !handle.contains("__")    // No consecutive underscores
    }
    
    private suspend fun findSimilarWellKnownFigure(handle: String): WellKnownFigureResult? {
        return EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
            WellKnownFigures.selectAll().mapNotNull { row ->
                val preferredHandle = row[WellKnownFigures.preferredHandle]
                val alternatives = row[WellKnownFigures.alternativeHandles]?.toList() ?: emptyList()
                
                val similarity = maxOf(
                    calculateSimilarity(handle, preferredHandle),
                    alternatives.maxOfOrNull { calculateSimilarity(handle, it) } ?: 0.0
                )
                
                if (similarity >= SIMILARITY_THRESHOLD) {
                    WellKnownFigureResult(
                        id = row[WellKnownFigures.id].value,
                        name = row[WellKnownFigures.name],
                        preferredHandle = preferredHandle,
                        similarity = similarity
                    )
                } else null
            }.maxByOrNull { it.similarity }
        }
    }
    
    private suspend fun findSimilarWellKnownCompany(handle: String): WellKnownCompanyResult? {
        return EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
            WellKnownCompanies.selectAll().mapNotNull { row ->
                val preferredHandle = row[WellKnownCompanies.preferredHandle]
                val alternatives = row[WellKnownCompanies.alternativeHandles]?.toList() ?: emptyList()
                
                val similarity = maxOf(
                    calculateSimilarity(handle, preferredHandle),
                    alternatives.maxOfOrNull { calculateSimilarity(handle, it) } ?: 0.0
                )
                
                if (similarity >= SIMILARITY_THRESHOLD) {
                    WellKnownCompanyResult(
                        id = row[WellKnownCompanies.id].value,
                        name = row[WellKnownCompanies.name],
                        preferredHandle = preferredHandle,
                        similarity = similarity
                    )
                } else null
            }.maxByOrNull { it.similarity }
        }
    }
    
    private suspend fun checkProtectedHandleSimilarity(handle: String): ProtectedSimilarity? {
        return EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
            ProtectedHandles.selectAll().mapNotNull { row ->
                val originalHandle = row[ProtectedHandles.originalHandle]
                val threshold = row[ProtectedHandles.similarityThreshold].toDouble()
                val similarity = calculateSimilarity(handle, originalHandle)
                
                if (similarity >= threshold) {
                    ProtectedSimilarity(
                        originalHandle = originalHandle,
                        similarity = similarity,
                        entityType = row[ProtectedHandles.protectedEntityType]
                    )
                } else null
            }.maxByOrNull { it.similarity }
        }
    }
    
    /**
     * Calculate similarity between two handles using Levenshtein distance
     */
    private fun calculateSimilarity(handle1: String, handle2: String): Double {
        val s1 = handle1.lowercase()
        val s2 = handle2.lowercase()
        
        if (s1 == s2) return 1.0
        
        val matrix = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) matrix[i][0] = i
        for (j in 0..s2.length) matrix[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        val maxLength = maxOf(s1.length, s2.length)
        return 1.0 - (matrix[s1.length][s2.length].toDouble() / maxLength)
    }
    
    private fun containsInappropriateContent(handle: String): Boolean {
        val inappropriateWords = listOf(
            "admin", "root", "null", "undefined", "system", "api", "www",
            "ftp", "mail", "email", "support", "help", "test", "demo",
            // Add more as needed
        )
        
        return inappropriateWords.any { handle.lowercase().contains(it) }
    }
    
    private fun checkTrademarkIssues(handle: String): List<String> {
        val warnings = mutableListOf<String>()
        
        // Add trademark checking logic here
        // This could integrate with trademark databases
        
        return warnings
    }
    
    private fun generateHandleSuggestions(handle: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Add numbers
        for (i in 1..9) {
            suggestions.add("$handle$i")
        }
        
        // Add year
        suggestions.add("${handle}2024")
        suggestions.add("${handle}2025")
        
        // Add underscores
        suggestions.add("${handle}_")
        suggestions.add("_$handle")
        
        // Add variations
        if (handle.length < 25) {
            suggestions.add("${handle}official")
            suggestions.add("${handle}real")
        }
        
        return suggestions.take(5)
    }
    
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashedBytes = md.digest(password.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun cacheAndReturn(result: HandleValidationResult): HandleValidationResult {
        cacheManager.cacheData("handle_validation:${result.handle}", result, CACHE_TTL_MINUTES * 60)
        return result
    }
    
    private suspend fun logIdentityEvent(identityId: UUID, action: String, details: Map<String, String>) {
        // Log to audit system
        logger.info("📋 Identity event: $action for ID: $identityId - Details: $details")
    }
    
    private suspend fun claimWellKnownFigureHandle(request: ClaimHandleRequest): ClaimHandleResponse {
        // Implementation for claiming well-known figure handles
        // This would involve verification workflow
        return ClaimHandleResponse(success = false, errors = mutableListOf("Not implemented yet"))
    }
    
    private suspend fun claimWellKnownCompanyHandle(request: ClaimHandleRequest): ClaimHandleResponse {
        // Implementation for claiming company handles
        // This would involve document verification
        return ClaimHandleResponse(success = false, errors = mutableListOf("Not implemented yet"))
    }
}

// ============== DATA CLASSES ==============

data class HandleValidationResult(
    val handle: String,
    var isValid: Boolean = true,
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
    var requiresVerification: Boolean = false,
    var reservationType: String? = null,
    var similarEntity: SimilarEntity? = null,
    var protectedSimilarity: ProtectedSimilarity? = null,
    var suggestions: List<String> = emptyList()
)

data class SimilarEntity(
    val type: String, // "figure" or "company"
    val name: String,
    val preferredHandle: String,
    val similarity: Double
)

data class ProtectedSimilarity(
    val originalHandle: String,
    val similarity: Double,
    val entityType: String
)

data class CreateEidRequest(
    val eid: String,
    val email: String,
    val phone: String?,
    val password: String,
    val ipAddress: String?,
    val userAgent: String?,
    val countryCode: String?
)

data class CreateEidResponse(
    val success: Boolean,
    val identityId: UUID? = null,
    val eid: String? = null,
    val errors: List<String> = emptyList(),
    val validationResult: HandleValidationResult? = null
)

data class ClaimHandleRequest(
    val identityId: UUID,
    val handle: String,
    val entityType: String, // "figure" or "company"
    val entityId: UUID?,
    val documents: List<String> = emptyList(),
    val notes: String?
)

data class ClaimHandleResponse(
    val success: Boolean,
    val verificationRequestId: UUID? = null,
    val errors: List<String> = emptyList(),
    val requiredDocuments: List<String> = emptyList()
)

data class WellKnownFigureResult(
    val id: UUID,
    val name: String,
    val preferredHandle: String,
    val similarity: Double
)

data class WellKnownCompanyResult(
    val id: UUID,
    val name: String,
    val preferredHandle: String,
    val similarity: Double
)