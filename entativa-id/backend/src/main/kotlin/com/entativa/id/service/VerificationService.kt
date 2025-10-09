package com.entativa.id.service

import com.entativa.id.config.*
import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Verification Service for Entativa ID
 * Handles celebrity/VIP protection, company verification, and document verification
 * 
 * @author Neo Qiss
 * @status Production-ready with comprehensive protection systems
 */
class VerificationService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(VerificationService::class.java)
    
    companion object {
        private const val CACHE_TTL_SECONDS = 7200 // 2 hours
        private const val DOCUMENT_VERIFICATION_TIMEOUT_DAYS = 30L
        private const val SIMILARITY_THRESHOLD = 85 // 85% similarity triggers protection
    }
    
    /**
     * Submit verification request for user
     */
    suspend fun submitVerificationRequest(
        userId: String, 
        request: VerificationRequest
    ): Result<VerificationRequestResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📋 Verification request submitted for user: $userId")
                
                // Validate request
                val validationResult = validateVerificationRequest(request)
                if (!validationResult.isSuccess) {
                    return@withContext Result.failure(
                        IllegalArgumentException(validationResult.exceptionOrNull()?.message)
                    )
                }
                
                // Check if user already has pending verification
                val existingRequest = transaction {
                    VerificationRequestsTable.select { 
                        (VerificationRequestsTable.identityId eq UUID.fromString(userId)) and
                        (VerificationRequestsTable.status eq "pending")
                    }.singleOrNull()
                }
                
                if (existingRequest != null) {
                    return@withContext Result.failure(
                        IllegalStateException("User already has a pending verification request")
                    )
                }
                
                val now = Instant.now()
                val requestId = UUID.randomUUID()
                
                // Create verification request
                transaction {
                    VerificationRequestsTable.insert {
                        it[id] = requestId
                        it[identityId] = UUID.fromString(userId)
                        it[type] = request.type.name.lowercase()
                        it[category] = request.category?.name?.lowercase()
                        it[reason] = request.reason
                        it[documents] = request.documents.joinToString(",")
                        it[socialProofs] = request.socialProofs.joinToString(",")
                        it[additionalInfo] = request.additionalInfo
                        it[createdAt] = now
                        it[expiresAt] = now.plusSeconds(DOCUMENT_VERIFICATION_TIMEOUT_DAYS * 24 * 3600)
                    }
                }
                
                // Log verification request
                auditVerificationAction(
                    identityId = UUID.fromString(userId),
                    action = "verification_request_submitted",
                    details = mapOf(
                        "request_id" to requestId.toString(),
                        "type" to request.type.name,
                        "category" to (request.category?.name ?: "none"),
                        "documents_count" to request.documents.size.toString()
                    )
                )
                
                logger.info("✅ Verification request created: $requestId")
                
                Result.success(VerificationRequestResponse(
                    id = requestId.toString(),
                    status = VerificationRequestStatus.PENDING,
                    submittedAt = now.toString(),
                    expiresAt = now.plusSeconds(DOCUMENT_VERIFICATION_TIMEOUT_DAYS * 24 * 3600).toString(),
                    estimatedProcessingTime = "3-7 business days"
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to submit verification request for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if handle is protected (celebrity/VIP/company) across all categories
     */
    suspend fun checkHandleProtection(handle: String): HandleProtectionResult {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🛡️ Checking handle protection across all categories: $handle")
                
                // Check cache first
                val cached = cacheManager.getCachedData<HandleProtectionResult>("protection:$handle")
                if (cached != null) {
                    return@withContext cached
                }
                
                val normalizedHandle = handle.lowercase().trim()
                
                // Check all specialized protection databases
                val result = transaction {
                    
                    // 1. Check Entertainment Celebrities
                    var protectionResult = checkEntertainmentCelebrities(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 2. Check Music Celebrities
                    protectionResult = checkMusicCelebrities(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 3. Check Sports Figures
                    protectionResult = checkSportsFigures(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 4. Check Digital Celebrities
                    protectionResult = checkDigitalCelebrities(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 5. Check Political Figures
                    protectionResult = checkPoliticalFigures(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 6. Check Government Organizations
                    protectionResult = checkGovernmentOrganizations(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 7. Check Corporations
                    protectionResult = checkCorporations(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 8. Check Business Leaders
                    protectionResult = checkBusinessLeaders(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 9. Check Financial Institutions
                    protectionResult = checkFinancialInstitutions(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 10. Check Tech Companies
                    protectionResult = checkTechCompanies(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 11. Check Media Organizations
                    protectionResult = checkMediaOrganizations(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 12. Check Journalists
                    protectionResult = checkJournalists(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 13. Check Scientists
                    protectionResult = checkScientists(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 14. Check Academic Institutions
                    protectionResult = checkAcademicInstitutions(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // 15. Check Legacy Protected Handles (backward compatibility)
                    protectionResult = checkLegacyProtectedHandles(normalizedHandle)
                    if (protectionResult.isProtected) return@transaction protectionResult
                    
                    // Handle is available
                    HandleProtectionResult(
                        isProtected = false,
                        protectionType = null,
                        category = null,
                        reason = null,
                        suggestedAlternatives = emptyList(),
                        requiresVerification = false
                    )
                }
                
                // Cache the result
                cacheManager.cacheData("protection:$handle", result, CACHE_TTL_SECONDS)
                
                result
                
            } catch (e: Exception) {
                logger.error("❌ Failed to check handle protection: $handle", e)
                HandleProtectionResult(
                    isProtected = true, // Fail secure
                    protectionType = ProtectionType.SYSTEM,
                    category = null,
                    reason = "Unable to verify handle availability",
                    suggestedAlternatives = emptyList(),
                    requiresVerification = true
                )
            }
        }
    }
    
    // ============== SPECIALIZED PROTECTION CHECKERS ==============
    
    private fun checkEntertainmentCelebrities(handle: String): HandleProtectionResult {
        val exactMatch = EntertainmentCelebritiesTable.select { 
            EntertainmentCelebritiesTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.CELEBRITY,
                category = ProtectionCategory.ENTERTAINMENT,
                reason = "Handle is protected for ${exactMatch[EntertainmentCelebritiesTable.fullName]} (${exactMatch[EntertainmentCelebritiesTable.category]})",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        // Check similarity
        val allCelebrities = EntertainmentCelebritiesTable.selectAll().toList()
        for (celebrity in allCelebrities) {
            val protectedHandle = celebrity[EntertainmentCelebritiesTable.handle]
            val similarity = calculateSimilarity(handle, protectedHandle)
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                return HandleProtectionResult(
                    isProtected = true,
                    protectionType = ProtectionType.CELEBRITY,
                    category = ProtectionCategory.ENTERTAINMENT,
                    reason = "Handle too similar to ${celebrity[EntertainmentCelebritiesTable.fullName]} (${similarity}% similarity)",
                    suggestedAlternatives = generateAlternatives(handle),
                    requiresVerification = true,
                    similarityScore = similarity
                )
            }
            
            // Check aliases
            val aliases = celebrity[EntertainmentCelebritiesTable.aliases]
            if (aliases.any { calculateSimilarity(handle, it) >= SIMILARITY_THRESHOLD }) {
                return HandleProtectionResult(
                    isProtected = true,
                    protectionType = ProtectionType.CELEBRITY,
                    category = ProtectionCategory.ENTERTAINMENT,
                    reason = "Handle similar to alias of ${celebrity[EntertainmentCelebritiesTable.fullName]}",
                    suggestedAlternatives = generateAlternatives(handle),
                    requiresVerification = true
                )
            }
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkMusicCelebrities(handle: String): HandleProtectionResult {
        val exactMatch = MusicCelebritiesTable.select { 
            MusicCelebritiesTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val monthlyListeners = exactMatch[MusicCelebritiesTable.monthlyListeners]
            val listenerText = if (monthlyListeners > 0) " (${monthlyListeners/1000000}M monthly listeners)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.CELEBRITY,
                category = ProtectionCategory.MUSIC,
                reason = "Handle is protected for ${exactMatch[MusicCelebritiesTable.fullName]}$listenerText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        // Check similarity with music celebrities
        val allMusicCelebrities = MusicCelebritiesTable.selectAll().toList()
        for (celebrity in allMusicCelebrities) {
            val protectedHandle = celebrity[MusicCelebritiesTable.handle]
            val similarity = calculateSimilarity(handle, protectedHandle)
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                return HandleProtectionResult(
                    isProtected = true,
                    protectionType = ProtectionType.CELEBRITY,
                    category = ProtectionCategory.MUSIC,
                    reason = "Handle too similar to music artist ${celebrity[MusicCelebritiesTable.fullName]} (${similarity}% similarity)",
                    suggestedAlternatives = generateAlternatives(handle),
                    requiresVerification = true,
                    similarityScore = similarity
                )
            }
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkSportsFigures(handle: String): HandleProtectionResult {
        val exactMatch = SportsFiguresTable.select { 
            SportsFiguresTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.CELEBRITY,
                category = ProtectionCategory.SPORTS,
                reason = "Handle is protected for ${exactMatch[SportsFiguresTable.fullName]} (${exactMatch[SportsFiguresTable.sport]})",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        // Check similarity with sports figures
        val allSportsFigures = SportsFiguresTable.selectAll().toList()
        for (athlete in allSportsFigures) {
            val protectedHandle = athlete[SportsFiguresTable.handle]
            val similarity = calculateSimilarity(handle, protectedHandle)
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                return HandleProtectionResult(
                    isProtected = true,
                    protectionType = ProtectionType.CELEBRITY,
                    category = ProtectionCategory.SPORTS,
                    reason = "Handle too similar to ${athlete[SportsFiguresTable.sport]} star ${athlete[SportsFiguresTable.fullName]} (${similarity}% similarity)",
                    suggestedAlternatives = generateAlternatives(handle),
                    requiresVerification = true,
                    similarityScore = similarity
                )
            }
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkDigitalCelebrities(handle: String): HandleProtectionResult {
        val exactMatch = DigitalCelebritiesTable.select { 
            DigitalCelebritiesTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.CELEBRITY,
                category = ProtectionCategory.DIGITAL,
                reason = "Handle is protected for ${exactMatch[DigitalCelebritiesTable.fullName]} (${exactMatch[DigitalCelebritiesTable.category]})",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkPoliticalFigures(handle: String): HandleProtectionResult {
        val exactMatch = PoliticalFiguresTable.select { 
            PoliticalFiguresTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.POLITICAL,
                category = ProtectionCategory.GOVERNMENT,
                reason = "Handle is protected for ${exactMatch[PoliticalFiguresTable.title]} ${exactMatch[PoliticalFiguresTable.fullName]}",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkGovernmentOrganizations(handle: String): HandleProtectionResult {
        val exactMatch = GovernmentOrganizationsTable.select { 
            GovernmentOrganizationsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.GOVERNMENT,
                category = ProtectionCategory.GOVERNMENT,
                reason = "Handle is protected for government organization ${exactMatch[GovernmentOrganizationsTable.organizationName]}",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkCorporations(handle: String): HandleProtectionResult {
        val exactMatch = CorporationsTable.select { 
            CorporationsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val marketCap = exactMatch[CorporationsTable.marketCapBillions]
            val marketCapText = if (marketCap > 0) " ($${marketCap}B market cap)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.COMPANY,
                category = ProtectionCategory.BUSINESS,
                reason = "Handle is protected for ${exactMatch[CorporationsTable.companyName]}$marketCapText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        // Check aliases and brands
        val allCorporations = CorporationsTable.selectAll().toList()
        for (corp in allCorporations) {
            val aliases = corp[CorporationsTable.aliases]
            if (aliases.any { calculateSimilarity(handle, it) >= SIMILARITY_THRESHOLD }) {
                return HandleProtectionResult(
                    isProtected = true,
                    protectionType = ProtectionType.COMPANY,
                    category = ProtectionCategory.BUSINESS,
                    reason = "Handle similar to brand/alias of ${corp[CorporationsTable.companyName]}",
                    suggestedAlternatives = generateAlternatives(handle),
                    requiresVerification = true
                )
            }
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkBusinessLeaders(handle: String): HandleProtectionResult {
        val exactMatch = BusinessLeadersTable.select { 
            BusinessLeadersTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val netWorth = exactMatch[BusinessLeadersTable.netWorthBillions]
            val wealthText = if (netWorth > 0) " ($${netWorth}B net worth)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.BUSINESS_LEADER,
                category = ProtectionCategory.BUSINESS,
                reason = "Handle is protected for ${exactMatch[BusinessLeadersTable.fullName]}$wealthText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkFinancialInstitutions(handle: String): HandleProtectionResult {
        val exactMatch = FinancialInstitutionsTable.select { 
            FinancialInstitutionsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.FINANCIAL,
                category = ProtectionCategory.BUSINESS,
                reason = "Handle is protected for financial institution ${exactMatch[FinancialInstitutionsTable.institutionName]}",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkTechCompanies(handle: String): HandleProtectionResult {
        val exactMatch = TechCompaniesTable.select { 
            TechCompaniesTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val valuation = exactMatch[TechCompaniesTable.valuationBillions]
            val valuationText = if (valuation > 0) " ($${valuation}B valuation)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.TECH_COMPANY,
                category = ProtectionCategory.BUSINESS,
                reason = "Handle is protected for tech company ${exactMatch[TechCompaniesTable.companyName]}$valuationText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkMediaOrganizations(handle: String): HandleProtectionResult {
        val exactMatch = MediaOrganizationsTable.select { 
            MediaOrganizationsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.MEDIA,
                category = ProtectionCategory.MEDIA,
                reason = "Handle is protected for media organization ${exactMatch[MediaOrganizationsTable.organizationName]}",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkJournalists(handle: String): HandleProtectionResult {
        val exactMatch = JournalistsTable.select { 
            JournalistsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.MEDIA,
                category = ProtectionCategory.MEDIA,
                reason = "Handle is protected for journalist ${exactMatch[JournalistsTable.fullName]} (${exactMatch[JournalistsTable.currentEmployer]})",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkScientists(handle: String): HandleProtectionResult {
        val exactMatch = ScientistsTable.select { 
            ScientistsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val nobelYear = exactMatch[ScientistsTable.nobelPrizeYear]
            val nobelText = if (nobelYear != null) " (Nobel Prize $nobelYear)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.ACADEMIC,
                category = ProtectionCategory.ACADEMIC,
                reason = "Handle is protected for scientist ${exactMatch[ScientistsTable.fullName]}$nobelText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkAcademicInstitutions(handle: String): HandleProtectionResult {
        val exactMatch = AcademicInstitutionsTable.select { 
            AcademicInstitutionsTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            val ranking = exactMatch[AcademicInstitutionsTable.rankingUs]
            val rankingText = if (ranking != null && ranking > 0) " (Ranked #$ranking)" else ""
            
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.ACADEMIC,
                category = ProtectionCategory.ACADEMIC,
                reason = "Handle is protected for ${exactMatch[AcademicInstitutionsTable.institutionName]}$rankingText",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun checkLegacyProtectedHandles(handle: String): HandleProtectionResult {
        // Check legacy protected handles table for backward compatibility
        val exactMatch = ProtectedHandlesTable.select { 
            ProtectedHandlesTable.handle eq handle 
        }.singleOrNull()
        
        if (exactMatch != null) {
            return HandleProtectionResult(
                isProtected = true,
                protectionType = ProtectionType.valueOf(exactMatch[ProtectedHandlesTable.type].uppercase()),
                category = exactMatch[ProtectedHandlesTable.category]?.let { 
                    ProtectionCategory.valueOf(it.uppercase()) 
                },
                reason = "Handle is protected for ${exactMatch[ProtectedHandlesTable.name]}",
                suggestedAlternatives = generateAlternatives(handle),
                requiresVerification = true
            )
        }
        
        return createNotProtectedResult()
    }
    
    private fun createNotProtectedResult(): HandleProtectionResult {
        return HandleProtectionResult(
            isProtected = false,
            protectionType = null,
            category = null,
            reason = null,
            suggestedAlternatives = emptyList(),
            requiresVerification = false
        )
    }
    
    /**
     * Get verification request status
     */
    suspend fun getVerificationStatus(userId: String): VerificationStatusResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Get user's verification status
                val userRecord = transaction {
                    EntativaIdentitiesTable.select { EntativaIdentitiesTable.id eq UUID.fromString(userId) }
                        .singleOrNull()
                } ?: return@withContext null
                
                val verificationStatus = VerificationStatus.valueOf(
                    userRecord[EntativaIdentitiesTable.verificationStatus].uppercase()
                )
                
                val verificationBadge = userRecord[EntativaIdentitiesTable.verificationBadge]?.let { 
                    VerificationBadge.valueOf(it.uppercase()) 
                }
                
                val verificationDate = userRecord[EntativaIdentitiesTable.verificationDate]?.toString()
                
                // Get pending requests
                val pendingRequests = transaction {
                    VerificationRequestsTable.select { 
                        (VerificationRequestsTable.identityId eq UUID.fromString(userId)) and
                        (VerificationRequestsTable.status eq "pending")
                    }.map { record ->
                        PendingVerificationRequest(
                            id = record[VerificationRequestsTable.id].value.toString(),
                            type = VerificationType.valueOf(record[VerificationRequestsTable.type].uppercase()),
                            submittedAt = record[VerificationRequestsTable.createdAt].toString(),
                            expiresAt = record[VerificationRequestsTable.expiresAt].toString()
                        )
                    }
                }
                
                VerificationStatusResponse(
                    currentStatus = verificationStatus,
                    verificationBadge = verificationBadge,
                    verificationDate = verificationDate,
                    pendingRequests = pendingRequests,
                    canSubmitNewRequest = pendingRequests.isEmpty(),
                    eligibleVerificationTypes = getEligibleVerificationTypes(userRecord)
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get verification status for user: $userId", e)
                null
            }
        }
    }
    
    /**
     * Process verification request (admin function)
     */
    suspend fun processVerificationRequest(
        requestId: String,
        decision: VerificationDecision,
        processedBy: String,
        notes: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚖️ Processing verification request: $requestId")
                
                val now = Instant.now()
                
                // Get verification request
                val requestRecord = transaction {
                    VerificationRequestsTable.select { VerificationRequestsTable.id eq UUID.fromString(requestId) }
                        .singleOrNull()
                } ?: return@withContext Result.failure(
                    IllegalArgumentException("Verification request not found")
                )
                
                val identityId = requestRecord[VerificationRequestsTable.identityId]
                val verificationType = VerificationType.valueOf(
                    requestRecord[VerificationRequestsTable.type].uppercase()
                )
                
                // Update request status
                transaction {
                    VerificationRequestsTable.update({ VerificationRequestsTable.id eq UUID.fromString(requestId) }) {
                        it[status] = decision.name.lowercase()
                        it[processedAt] = now
                        it[this.processedBy] = processedBy
                        it[processingNotes] = notes
                    }
                }
                
                // If approved, update user verification status
                if (decision == VerificationDecision.APPROVED) {
                    val badge = determineVerificationBadge(verificationType, requestRecord)
                    
                    transaction {
                        EntativaIdentitiesTable.update({ EntativaIdentitiesTable.id eq identityId }) {
                            it[verificationStatus] = VerificationStatus.VERIFIED.name.lowercase()
                            it[verificationBadge] = badge.name.lowercase()
                            it[verificationDate] = now
                            it[updatedAt] = now
                        }
                    }
                }
                
                // Log verification decision
                auditVerificationAction(
                    identityId = identityId,
                    action = "verification_processed",
                    details = mapOf(
                        "request_id" to requestId,
                        "decision" to decision.name,
                        "processed_by" to processedBy,
                        "verification_type" to verificationType.name
                    )
                )
                
                logger.info("✅ Verification request processed: $requestId - ${decision.name}")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to process verification request: $requestId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add protected handle to database
     */
    suspend fun addProtectedHandle(request: AddProtectedHandleRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🛡️ Adding protected handle: ${request.handle}")
                
                // Check if handle already protected
                val existing = transaction {
                    ProtectedHandlesTable.select { ProtectedHandlesTable.handle eq request.handle.lowercase() }
                        .singleOrNull()
                }
                
                if (existing != null) {
                    return@withContext Result.failure(
                        IllegalStateException("Handle is already protected")
                    )
                }
                
                // Add to protected handles
                transaction {
                    ProtectedHandlesTable.insert {
                        it[id] = UUID.randomUUID()
                        it[handle] = request.handle.lowercase()
                        it[name] = request.name
                        it[type] = request.type.name.lowercase()
                        it[category] = request.category?.name?.lowercase()
                        it[reason] = request.reason
                        it[addedBy] = request.addedBy
                        it[createdAt] = Instant.now()
                    }
                }
                
                // Clear protection cache for this handle
                cacheManager.invalidateCache("protection:${request.handle}")
                
                logger.info("✅ Protected handle added: ${request.handle}")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to add protected handle: ${request.handle}", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun validateVerificationRequest(request: VerificationRequest): Result<Unit> {
        val errors = mutableListOf<String>()
        
        if (request.reason.isBlank()) {
            errors.add("Reason is required")
        }
        
        if (request.documents.isEmpty()) {
            errors.add("At least one document is required")
        }
        
        if (request.type == VerificationType.CELEBRITY && request.socialProofs.isEmpty()) {
            errors.add("Social media proofs are required for celebrity verification")
        }
        
        if (request.type == VerificationType.COMPANY && request.category == null) {
            errors.add("Category is required for company verification")
        }
        
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException(errors.joinToString("; ")))
        }
    }
    
    private fun calculateSimilarity(str1: String, str2: String): Int {
        if (str1 == str2) return 100
        
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1
        
        if (longer.length == 0) return 100
        
        val distance = levenshteinDistance(longer, shorter)
        return ((longer.length - distance).toDouble() / longer.length * 100).toInt()
    }
    
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val matrix = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) matrix[i][0] = i
        for (j in 0..str2.length) matrix[0][j] = j
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return matrix[str1.length][str2.length]
    }
    
    private fun generateAlternatives(handle: String): List<String> {
        return listOf(
            "${handle}_official",
            "${handle}_verified",
            "real_$handle",
            "${handle}2024",
            "${handle}_eid"
        )
    }
    
    private fun determineVerificationBadge(
        type: VerificationType, 
        requestRecord: ResultRow
    ): VerificationBadge {
        return when (type) {
            VerificationType.CELEBRITY -> {
                val category = requestRecord[VerificationRequestsTable.category]
                when (category) {
                    "entertainment" -> VerificationBadge.BLUE_CHECKMARK
                    "sports" -> VerificationBadge.BLUE_CHECKMARK
                    "politics" -> VerificationBadge.GOLD_CHECKMARK
                    else -> VerificationBadge.BLUE_CHECKMARK
                }
            }
            VerificationType.COMPANY -> VerificationBadge.VERIFIED_ORGANIZATION
            VerificationType.GOVERNMENT -> VerificationBadge.GOVERNMENT_OFFICIAL
            VerificationType.MEDIA -> VerificationBadge.VERIFIED_JOURNALIST
            VerificationType.NONPROFIT -> VerificationBadge.VERIFIED_ORGANIZATION
            VerificationType.PERSONAL -> VerificationBadge.IDENTITY_VERIFIED
        }
    }
    
    private fun getEligibleVerificationTypes(userRecord: ResultRow): List<VerificationType> {
        val eligible = mutableListOf<VerificationType>()
        
        // All users can apply for personal verification
        eligible.add(VerificationType.PERSONAL)
        
        // Check if user has business-related indicators
        val email = userRecord[EntativaIdentitiesTable.email]
        if (email.contains("@") && !email.endsWith("@gmail.com") && 
            !email.endsWith("@yahoo.com") && !email.endsWith("@hotmail.com")) {
            eligible.add(VerificationType.COMPANY)
        }
        
        // Celebrity verification requires manual review
        eligible.add(VerificationType.CELEBRITY)
        
        return eligible
    }
    
    private suspend fun auditVerificationAction(
        identityId: UUID, 
        action: String, 
        details: Map<String, String>
    ) {
        try {
            transaction {
                VerificationAuditLogTable.insert {
                    it[this.identityId] = identityId
                    it[this.action] = action
                    it[this.details] = details
                    it[createdAt] = Instant.now()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to log verification audit event", e)
        }
    }
}