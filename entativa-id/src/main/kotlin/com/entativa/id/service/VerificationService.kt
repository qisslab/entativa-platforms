package com.entativa.id.service

import com.entativa.shared.cache.EntativaCacheManager
import com.entativa.shared.database.EntativaDatabaseFactory
import com.entativa.shared.database.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Entativa ID Verification Service - Document-based identity verification
 * Handles celebrity, company, and VIP verification with document submission workflow
 * 
 * @author Neo Qiss
 * @status Production-ready with enterprise verification standards
 */
class VerificationService(
    private val cacheManager: EntativaCacheManager,
    private val fileStorageService: FileStorageService,
    private val notificationService: NotificationService
) {
    
    private val logger = LoggerFactory.getLogger(VerificationService::class.java)
    
    companion object {
        private const val MAX_DOCUMENT_SIZE_MB = 10
        private const val ALLOWED_DOCUMENT_TYPES = "application/pdf,image/jpeg,image/png,image/heic"
        private const val VERIFICATION_CACHE_TTL = 3600 // 1 hour
    }
    
    // Database Tables
    object VerificationRequests : UUIDTable("verification_requests") {
        val identityId = reference("identity_id", EntativaIdService.EntativaIdentities)
        val verificationType = varchar("verification_type", 20)
        val requestedHandle = varchar("requested_handle", 100).nullable()
        val claimType = varchar("claim_type", 30).nullable()
        val claimedEntityId = uuid("claimed_entity_id").nullable()
        val status = varchar("status", 20).default("pending")
        val priority = integer("priority").default(3)
        val assignedReviewer = uuid("assigned_reviewer").nullable()
        val reviewStartedAt = timestamp("review_started_at").nullable()
        val reviewCompletedAt = timestamp("review_completed_at").nullable()
        val adminNotes = text("admin_notes").nullable()
        val rejectionReason = text("rejection_reason").nullable()
        val applicantNotes = text("applicant_notes").nullable()
        val contactEmail = varchar("contact_email", 320).nullable()
        val contactPhone = varchar("contact_phone", 20).nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    }
    
    object VerificationDocuments : UUIDTable("verification_documents") {
        val verificationRequestId = reference("verification_request_id", VerificationRequests)
        val documentType = varchar("document_type", 50)
        val fileName = varchar("file_name", 255)
        val fileUrl = varchar("file_url", 500)
        val fileSize = integer("file_size").nullable()
        val mimeType = varchar("mime_type", 100).nullable()
        val uploadDate = timestamp("upload_date").defaultExpression(CurrentTimestamp())
        val isVerified = bool("is_verified").default(false)
        val verifiedBy = uuid("verified_by").nullable()
        val verifiedAt = timestamp("verified_at").nullable()
        val verificationNotes = text("verification_notes").nullable()
        val fileHash = varchar("file_hash", 64).nullable()
        val encrypted = bool("encrypted").default(true)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    }
    
    /**
     * Submit verification request with document upload
     */
    suspend fun submitVerificationRequest(request: VerificationSubmissionRequest): VerificationSubmissionResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📄 Processing verification request for identity: ${request.identityId}")
                
                // Validate request
                val validationResult = validateVerificationRequest(request)
                if (!validationResult.isValid) {
                    return@withContext VerificationSubmissionResponse(
                        success = false,
                        errors = validationResult.errors
                    )
                }
                
                // Check for existing pending requests
                val existingRequest = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    VerificationRequests.select { 
                        (VerificationRequests.identityId eq request.identityId) and 
                        (VerificationRequests.status inList listOf("pending", "under_review"))
                    }.singleOrNull()
                }
                
                if (existingRequest != null) {
                    return@withContext VerificationSubmissionResponse(
                        success = false,
                        errors = listOf("You already have a pending verification request")
                    )
                }
                
                // Determine verification type and priority
                val verificationDetails = determineVerificationDetails(request)
                
                // Create verification request
                val requestId = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    VerificationRequests.insertAndGetId {
                        it[identityId] = request.identityId
                        it[verificationType] = verificationDetails.type
                        it[requestedHandle] = request.requestedHandle
                        it[claimType] = verificationDetails.claimType
                        it[claimedEntityId] = verificationDetails.claimedEntityId
                        it[priority] = verificationDetails.priority
                        it[applicantNotes] = request.notes
                        it[contactEmail] = request.contactEmail
                        it[contactPhone] = request.contactPhone
                    }
                }.value
                
                // Process document uploads
                val uploadResults = mutableListOf<DocumentUploadResult>()
                for (document in request.documents) {
                    val uploadResult = processDocumentUpload(requestId, document)
                    uploadResults.add(uploadResult)
                }
                
                // Check if all required documents are uploaded
                val requiredDocuments = getRequiredDocuments(verificationDetails.type, verificationDetails.claimedEntityId)
                val uploadedTypes = uploadResults.filter { it.success }.map { it.documentType }
                val missingDocuments = requiredDocuments.filter { it !in uploadedTypes }
                
                // Notify admin team for high-priority requests
                if (verificationDetails.priority <= 2) {
                    notificationService.notifyAdminTeam(
                        "High Priority Verification Request",
                        "New ${verificationDetails.type} verification request for handle: ${request.requestedHandle}",
                        requestId
                    )
                }
                
                // Log submission
                logVerificationEvent(requestId, "request_submitted", mapOf(
                    "type" to verificationDetails.type,
                    "handle" to (request.requestedHandle ?: ""),
                    "documents_count" to uploadResults.size.toString()
                ))
                
                logger.info("✅ Verification request submitted successfully: $requestId")
                
                VerificationSubmissionResponse(
                    success = true,
                    requestId = requestId,
                    estimatedReviewTime = calculateEstimatedReviewTime(verificationDetails.priority),
                    requiredDocuments = requiredDocuments,
                    uploadedDocuments = uploadedTypes,
                    missingDocuments = missingDocuments
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to submit verification request", e)
                VerificationSubmissionResponse(
                    success = false,
                    errors = listOf("Failed to submit verification request: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Review verification request (admin function)
     */
    suspend fun reviewVerificationRequest(request: VerificationReviewRequest): VerificationReviewResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔍 Reviewing verification request: ${request.requestId}")
                
                // Get verification request
                val verificationRequest = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    VerificationRequests.select { VerificationRequests.id eq request.requestId }.singleOrNull()
                } ?: return@withContext VerificationReviewResponse(
                    success = false,
                    errors = listOf("Verification request not found")
                )
                
                // Update request status and reviewer
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    VerificationRequests.update({ VerificationRequests.id eq request.requestId }) {
                        it[status] = if (request.decision == "approved") "approved" else "rejected"
                        it[assignedReviewer] = request.reviewerId
                        it[reviewCompletedAt] = Instant.now()
                        it[adminNotes] = request.adminNotes
                        it[rejectionReason] = if (request.decision == "rejected") request.rejectionReason else null
                    }
                }
                
                // If approved, update identity verification status
                if (request.decision == "approved") {
                    val verificationType = verificationRequest[VerificationRequests.verificationType]
                    val badge = determineBadgeType(verificationType)
                    
                    EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                        EntativaIdService.EntativaIdentities.update({ 
                            EntativaIdService.EntativaIdentities.id eq verificationRequest[VerificationRequests.identityId] 
                        }) {
                            it[verificationStatus] = "verified"
                            it[verificationBadge] = badge
                            it[verificationDate] = Instant.now()
                        }
                    }
                    
                    // Update claimed entity if applicable
                    val claimedEntityId = verificationRequest[VerificationRequests.claimedEntityId]
                    val claimType = verificationRequest[VerificationRequests.claimType]
                    
                    if (claimedEntityId != null && claimType != null) {
                        updateClaimedEntity(claimType, claimedEntityId, verificationRequest[VerificationRequests.identityId])
                    }
                }
                
                // Notify applicant
                notificationService.notifyApplicant(
                    verificationRequest[VerificationRequests.identityId],
                    "Verification ${request.decision.capitalize()}",
                    if (request.decision == "approved") 
                        "Congratulations! Your verification request has been approved." 
                    else 
                        "Your verification request has been rejected. Reason: ${request.rejectionReason}",
                    request.requestId
                )
                
                // Log review
                logVerificationEvent(request.requestId, "request_reviewed", mapOf(
                    "decision" to request.decision,
                    "reviewer" to request.reviewerId.toString()
                ))
                
                logger.info("✅ Verification request reviewed: ${request.requestId} - Decision: ${request.decision}")
                
                VerificationReviewResponse(
                    success = true,
                    decision = request.decision,
                    badgeAwarded = if (request.decision == "approved") determineBadgeType(verificationRequest[VerificationRequests.verificationType]) else null
                )
                
            } catch (e: Exception) {
                logger.error("❌ Failed to review verification request", e)
                VerificationReviewResponse(
                    success = false,
                    errors = listOf("Failed to review request: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Get verification request details
     */
    suspend fun getVerificationRequest(requestId: UUID): VerificationRequestDetails? {
        return withContext(Dispatchers.IO) {
            try {
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    val query = VerificationRequests
                        .leftJoin(VerificationDocuments)
                        .select { VerificationRequests.id eq requestId }
                    
                    val rows = query.toList()
                    if (rows.isEmpty()) return@dbQuery null
                    
                    val firstRow = rows.first()
                    val documents = rows.mapNotNull { row ->
                        row.getOrNull(VerificationDocuments.id)?.let {
                            DocumentDetails(
                                id = it.value,
                                documentType = row[VerificationDocuments.documentType],
                                fileName = row[VerificationDocuments.fileName],
                                uploadDate = row[VerificationDocuments.uploadDate],
                                isVerified = row[VerificationDocuments.isVerified],
                                verificationNotes = row[VerificationDocuments.verificationNotes]
                            )
                        }
                    }.distinctBy { it.id }
                    
                    VerificationRequestDetails(
                        id = firstRow[VerificationRequests.id].value,
                        identityId = firstRow[VerificationRequests.identityId],
                        verificationType = firstRow[VerificationRequests.verificationType],
                        requestedHandle = firstRow[VerificationRequests.requestedHandle],
                        status = firstRow[VerificationRequests.status],
                        priority = firstRow[VerificationRequests.priority],
                        applicantNotes = firstRow[VerificationRequests.applicantNotes],
                        adminNotes = firstRow[VerificationRequests.adminNotes],
                        rejectionReason = firstRow[VerificationRequests.rejectionReason],
                        createdAt = firstRow[VerificationRequests.createdAt],
                        reviewCompletedAt = firstRow[VerificationRequests.reviewCompletedAt],
                        documents = documents
                    )
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get verification request details", e)
                null
            }
        }
    }
    
    /**
     * Get pending verification requests for admin review
     */
    suspend fun getPendingVerificationRequests(
        limit: Int = 50,
        priority: Int? = null
    ): List<VerificationRequestSummary> {
        return withContext(Dispatchers.IO) {
            try {
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    var query = VerificationRequests
                        .select { VerificationRequests.status eq "pending" }
                        .orderBy(VerificationRequests.priority, SortOrder.ASC)
                        .orderBy(VerificationRequests.createdAt, SortOrder.ASC)
                    
                    if (priority != null) {
                        query = query.andWhere { VerificationRequests.priority eq priority }
                    }
                    
                    query.limit(limit).map { row ->
                        VerificationRequestSummary(
                            id = row[VerificationRequests.id].value,
                            verificationType = row[VerificationRequests.verificationType],
                            requestedHandle = row[VerificationRequests.requestedHandle],
                            priority = row[VerificationRequests.priority],
                            createdAt = row[VerificationRequests.createdAt],
                            waitTime = java.time.Duration.between(row[VerificationRequests.createdAt], Instant.now())
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get pending verification requests", e)
                emptyList()
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun validateVerificationRequest(request: VerificationSubmissionRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate basic fields
        if (request.verificationType.isBlank()) {
            errors.add("Verification type is required")
        }
        
        if (request.contactEmail.isBlank()) {
            errors.add("Contact email is required")
        }
        
        // Validate documents
        if (request.documents.isEmpty()) {
            errors.add("At least one document is required")
        }
        
        request.documents.forEach { doc ->
            if (doc.content.size > MAX_DOCUMENT_SIZE_MB * 1024 * 1024) {
                errors.add("Document ${doc.fileName} exceeds maximum size of ${MAX_DOCUMENT_SIZE_MB}MB")
            }
            
            if (!ALLOWED_DOCUMENT_TYPES.split(",").contains(doc.mimeType)) {
                errors.add("Document ${doc.fileName} has unsupported format")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private suspend fun determineVerificationDetails(request: VerificationSubmissionRequest): VerificationDetails {
        val type = request.verificationType
        var priority = 3 // Default priority
        var claimType: String? = null
        var claimedEntityId: UUID? = null
        
        // Check if claiming a well-known handle
        if (!request.requestedHandle.isNullOrBlank()) {
            // Check against well-known figures
            val figure = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                EntativaIdService.WellKnownFigures.select { 
                    EntativaIdService.WellKnownFigures.preferredHandle eq request.requestedHandle
                }.singleOrNull()
            }
            
            if (figure != null) {
                priority = when (figure[EntativaIdService.WellKnownFigures.verificationLevel]) {
                    "ultra_high" -> 1
                    "high" -> 2
                    else -> 3
                }
                claimType = "well_known_figure"
                claimedEntityId = figure[EntativaIdService.WellKnownFigures.id].value
            } else {
                // Check against companies
                val company = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdService.WellKnownCompanies.select { 
                        EntativaIdService.WellKnownCompanies.preferredHandle eq request.requestedHandle
                    }.singleOrNull()
                }
                
                if (company != null) {
                    priority = when (company[EntativaIdService.WellKnownCompanies.verificationLevel]) {
                        "ultra_high" -> 1
                        "high" -> 2
                        else -> 3
                    }
                    claimType = "company"
                    claimedEntityId = company[EntativaIdService.WellKnownCompanies.id].value
                }
            }
        }
        
        return VerificationDetails(type, priority, claimType, claimedEntityId)
    }
    
    private suspend fun processDocumentUpload(requestId: UUID, document: DocumentUploadRequest): DocumentUploadResult {
        return try {
            // Upload to secure storage
            val fileUrl = fileStorageService.uploadDocument(document.content, document.fileName, document.mimeType)
            val fileHash = calculateFileHash(document.content)
            
            // Save document record
            val documentId = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                VerificationDocuments.insertAndGetId {
                    it[verificationRequestId] = requestId
                    it[documentType] = document.documentType
                    it[fileName] = document.fileName
                    it[fileUrl] = fileUrl
                    it[fileSize] = document.content.size
                    it[mimeType] = document.mimeType
                    it[fileHash] = fileHash
                    it[encrypted] = true
                }
            }.value
            
            DocumentUploadResult(
                success = true,
                documentId = documentId,
                documentType = document.documentType,
                fileUrl = fileUrl
            )
            
        } catch (e: Exception) {
            logger.error("❌ Failed to upload document: ${document.fileName}", e)
            DocumentUploadResult(
                success = false,
                documentType = document.documentType,
                error = "Upload failed: ${e.message}"
            )
        }
    }
    
    private fun getRequiredDocuments(verificationType: String, claimedEntityId: UUID?): List<String> {
        return when (verificationType) {
            "personal" -> listOf("government_id")
            "celebrity" -> listOf("government_id", "social_media_proof", "employment_verification")
            "company" -> listOf("business_registration", "tax_document", "domain_ownership")
            "government" -> listOf("government_id", "employment_verification", "official_letterhead")
            else -> listOf("government_id")
        }
    }
    
    private fun calculateEstimatedReviewTime(priority: Int): String {
        return when (priority) {
            1 -> "1-2 business days"
            2 -> "3-5 business days"
            3 -> "5-10 business days"
            else -> "10-15 business days"
        }
    }
    
    private fun determineBadgeType(verificationType: String): String {
        return when (verificationType) {
            "celebrity" -> "gold"
            "company" -> "business"
            "government" -> "government"
            else -> "blue"
        }
    }
    
    private suspend fun updateClaimedEntity(claimType: String, entityId: UUID, identityId: UUID) {
        when (claimType) {
            "well_known_figure" -> {
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdService.WellKnownFigures.update({ EntativaIdService.WellKnownFigures.id eq entityId }) {
                        it[claimedBy] = identityId
                        it[claimedAt] = Instant.now()
                    }
                }
            }
            "company" -> {
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdService.WellKnownCompanies.update({ EntativaIdService.WellKnownCompanies.id eq entityId }) {
                        it[claimedBy] = identityId
                        it[claimedAt] = Instant.now()
                    }
                }
            }
        }
    }
    
    private fun calculateFileHash(content: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hashedBytes = md.digest(content)
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun logVerificationEvent(requestId: UUID, action: String, details: Map<String, String>) {
        logger.info("📋 Verification event: $action for request: $requestId - Details: $details")
    }
}

// ============== DATA CLASSES ==============

data class VerificationSubmissionRequest(
    val identityId: UUID,
    val verificationType: String,
    val requestedHandle: String?,
    val documents: List<DocumentUploadRequest>,
    val notes: String?,
    val contactEmail: String,
    val contactPhone: String?
)

data class DocumentUploadRequest(
    val documentType: String,
    val fileName: String,
    val mimeType: String,
    val content: ByteArray
)

data class VerificationSubmissionResponse(
    val success: Boolean,
    val requestId: UUID? = null,
    val estimatedReviewTime: String? = null,
    val requiredDocuments: List<String> = emptyList(),
    val uploadedDocuments: List<String> = emptyList(),
    val missingDocuments: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

data class VerificationReviewRequest(
    val requestId: UUID,
    val reviewerId: UUID,
    val decision: String, // "approved" or "rejected"
    val adminNotes: String?,
    val rejectionReason: String?
)

data class VerificationReviewResponse(
    val success: Boolean,
    val decision: String? = null,
    val badgeAwarded: String? = null,
    val errors: List<String> = emptyList()
)

data class VerificationRequestDetails(
    val id: UUID,
    val identityId: UUID,
    val verificationType: String,
    val requestedHandle: String?,
    val status: String,
    val priority: Int,
    val applicantNotes: String?,
    val adminNotes: String?,
    val rejectionReason: String?,
    val createdAt: Instant,
    val reviewCompletedAt: Instant?,
    val documents: List<DocumentDetails>
)

data class DocumentDetails(
    val id: UUID,
    val documentType: String,
    val fileName: String,
    val uploadDate: Instant,
    val isVerified: Boolean,
    val verificationNotes: String?
)

data class VerificationRequestSummary(
    val id: UUID,
    val verificationType: String,
    val requestedHandle: String?,
    val priority: Int,
    val createdAt: Instant,
    val waitTime: java.time.Duration
)

data class VerificationDetails(
    val type: String,
    val priority: Int,
    val claimType: String?,
    val claimedEntityId: UUID?
)

data class DocumentUploadResult(
    val success: Boolean,
    val documentId: UUID? = null,
    val documentType: String,
    val fileUrl: String? = null,
    val error: String? = null
)

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

// Mock interfaces for dependencies
interface FileStorageService {
    suspend fun uploadDocument(content: ByteArray, fileName: String, mimeType: String): String
}

interface NotificationService {
    suspend fun notifyAdminTeam(title: String, message: String, requestId: UUID)
    suspend fun notifyApplicant(identityId: UUID, title: String, message: String, requestId: UUID)
}