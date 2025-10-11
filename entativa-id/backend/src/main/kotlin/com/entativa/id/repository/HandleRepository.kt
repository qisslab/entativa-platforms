package com.entativa.id.repository

import com.entativa.id.database.tables.HandlesTable
import com.entativa.id.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * Handle Repository for Entativa ID
 * Handles all database operations related to user handles with celebrity/brand protection
 * 
 * @author Neo Qiss
 * @status Production-ready handle management with cross-platform synchronization
 */
@Repository
class HandleRepository {
    
    private val logger = LoggerFactory.getLogger(HandleRepository::class.java)
    
    /**
     * Create a new handle
     */
    suspend fun createHandle(handle: CreateHandleRequest): Result<Handle> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating new handle: ${handle.handle}")
                
                val handleId = transaction {
                    HandlesTable.insertAndGetId {
                        it[HandlesTable.handle] = handle.handle
                        it[handleLowercase] = handle.handle.lowercase()
                        it[userId] = handle.userId
                        it[ownerId] = handle.ownerId
                        it[status] = handle.status
                        it[isAvailable] = handle.isAvailable
                        it[isReserved] = handle.isReserved
                        it[isPremium] = handle.isPremium
                        it[isProtected] = handle.isProtected
                        it[isCelebrity] = handle.isCelebrity
                        it[isBrand] = handle.isBrand
                        it[isVerified] = handle.isVerified
                        it[originalOwnerId] = handle.originalOwnerId
                        it[syncedToPlatforms] = handle.syncedToPlatforms
                        it[syncStatus] = handle.syncStatus
                        it[securityLevel] = handle.securityLevel
                        it[category] = handle.category
                        it[description] = handle.description
                        it[createdBy] = handle.createdBy
                    }
                }
                
                val createdHandle = findById(handleId.value.toString())
                if (createdHandle.isSuccess) {
                    logger.info("✅ Handle created successfully: ${handle.handle}")
                    createdHandle
                } else {
                    logger.error("❌ Failed to retrieve created handle: ${handle.handle}")
                    Result.failure(Exception("Failed to retrieve created handle"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create handle: ${handle.handle}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find handle by ID
     */
    suspend fun findById(id: String): Result<Handle> {
        return withContext(Dispatchers.IO) {
            try {
                val handle = transaction {
                    HandlesTable.select { 
                        (HandlesTable.id eq UUID.fromString(id)) and (HandlesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToHandle(row)
                    }
                }
                
                if (handle != null) {
                    Result.success(handle)
                } else {
                    Result.failure(NoSuchElementException("Handle not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find handle by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find handle by handle string
     */
    suspend fun findByHandle(handle: String): Result<Handle> {
        return withContext(Dispatchers.IO) {
            try {
                val handleRecord = transaction {
                    HandlesTable.select { 
                        (HandlesTable.handleLowercase eq handle.lowercase()) and (HandlesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToHandle(row)
                    }
                }
                
                if (handleRecord != null) {
                    Result.success(handleRecord)
                } else {
                    Result.failure(NoSuchElementException("Handle not found: $handle"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find handle: $handle", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find handles by user ID
     */
    suspend fun findByUserId(userId: String): Result<List<Handle>> {
        return withContext(Dispatchers.IO) {
            try {
                val handles = transaction {
                    HandlesTable.select { 
                        (HandlesTable.userId eq userId) and (HandlesTable.deletedAt.isNull()) 
                    }.map { row ->
                        mapRowToHandle(row)
                    }
                }
                
                Result.success(handles)
            } catch (e: Exception) {
                logger.error("❌ Failed to find handles for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if handle is available
     */
    suspend fun isHandleAvailable(handle: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val available = transaction {
                    HandlesTable.select { 
                        (HandlesTable.handleLowercase eq handle.lowercase()) and 
                        (HandlesTable.deletedAt.isNull()) 
                    }.count() == 0L
                }
                
                Result.success(available)
            } catch (e: Exception) {
                logger.error("❌ Failed to check handle availability: $handle", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search handles with filters
     */
    suspend fun searchHandles(
        query: String? = null,
        isAvailable: Boolean? = null,
        isVerified: Boolean? = null,
        category: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Handle>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Searching handles with query: $query")
                
                val handles = transaction {
                    var baseQuery = HandlesTable.select { HandlesTable.deletedAt.isNull() }
                    
                    query?.let { q ->
                        baseQuery = baseQuery.andWhere { 
                            HandlesTable.handleLowercase like "%${q.lowercase()}%" 
                        }
                    }
                    
                    isAvailable?.let { available ->
                        baseQuery = baseQuery.andWhere { HandlesTable.isAvailable eq available }
                    }
                    
                    isVerified?.let { verified ->
                        baseQuery = baseQuery.andWhere { HandlesTable.isVerified eq verified }
                    }
                    
                    category?.let { cat ->
                        baseQuery = baseQuery.andWhere { HandlesTable.category eq cat }
                    }
                    
                    baseQuery
                        .orderBy(HandlesTable.handle to SortOrder.ASC)
                        .limit(limit, offset.toLong())
                        .map { row -> mapRowToHandle(row) }
                }
                
                logger.debug("✅ Found ${handles.size} handles")
                Result.success(handles)
            } catch (e: Exception) {
                logger.error("❌ Failed to search handles", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Reserve handle for celebrity/brand protection
     */
    suspend fun reserveHandle(
        handle: String,
        reservationType: String,
        reason: String,
        document: String?,
        requestedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Reserving handle: $handle for $reservationType")
                
                val updated = transaction {
                    HandlesTable.update({ 
                        (HandlesTable.handleLowercase eq handle.lowercase()) and (HandlesTable.deletedAt.isNull()) 
                    }) {
                        it[isReserved] = true
                        it[isProtected] = true
                        it[reservationType] = reservationType
                        it[reservationReason] = reason
                        it[reservationDocument] = document
                        it[status] = "RESERVED"
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = requestedBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Handle reserved: $handle")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Handle not found: $handle"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to reserve handle: $handle", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Transfer handle ownership
     */
    suspend fun transferHandle(
        handleId: String,
        fromUserId: String,
        toUserId: String,
        transferToken: String,
        approvedBy: String?
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Transferring handle: $handleId from $fromUserId to $toUserId")
                
                val updated = transaction {
                    HandlesTable.update({ 
                        (HandlesTable.id eq UUID.fromString(handleId)) and 
                        (HandlesTable.userId eq fromUserId) and 
                        (HandlesTable.deletedAt.isNull()) 
                    }) {
                        it[userId] = toUserId
                        it[ownerId] = toUserId
                        it[transferInProgress] = false
                        it[transferFromUserId] = fromUserId
                        it[transferToUserId] = toUserId
                        it[transferCompletedAt] = Instant.now()
                        it[transferApprovedBy] = approvedBy
                        it[totalTransfers] = totalTransfers + 1
                        it[lastTransferredAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = approvedBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Handle transferred: $handleId")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Handle not found or transfer not authorized"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to transfer handle: $handleId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update handle sync status
     */
    suspend fun updateSyncStatus(
        handleId: String,
        syncStatus: String,
        syncedPlatforms: String,
        failureReason: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    HandlesTable.update({ 
                        (HandlesTable.id eq UUID.fromString(handleId)) and (HandlesTable.deletedAt.isNull()) 
                    }) {
                        it[HandlesTable.syncStatus] = syncStatus
                        it[syncedToPlatforms] = syncedPlatforms
                        if (syncStatus == "SYNCED") {
                            it[lastSyncedAt] = Instant.now()
                            it[syncRetryCount] = 0
                        } else if (syncStatus == "FAILED") {
                            it[syncFailureReason] = failureReason
                            it[syncRetryCount] = syncRetryCount + 1
                        }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update sync status for handle: $handleId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update handle popularity metrics
     */
    suspend fun updatePopularityMetrics(
        handleId: String,
        searchCount: Long? = null,
        viewCount: Long? = null,
        mentionCount: Long? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    HandlesTable.update({ 
                        (HandlesTable.id eq UUID.fromString(handleId)) and (HandlesTable.deletedAt.isNull()) 
                    }) {
                        searchCount?.let { count ->
                            it[HandlesTable.searchCount] = HandlesTable.searchCount + count
                        }
                        viewCount?.let { count ->
                            it[HandlesTable.viewCount] = HandlesTable.viewCount + count
                        }
                        mentionCount?.let { count ->
                            it[HandlesTable.mentionCount] = HandlesTable.mentionCount + count
                        }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update popularity metrics for handle: $handleId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete handle
     */
    suspend fun deleteHandle(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting handle: $id")
                
                val deleted = transaction {
                    HandlesTable.update({ 
                        (HandlesTable.id eq UUID.fromString(id)) and (HandlesTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[HandlesTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[status] = "DELETED"
                        it[isAvailable] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Handle soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Handle not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete handle: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to Handle domain object
     */
    private fun mapRowToHandle(row: ResultRow): Handle {
        return Handle(
            id = row[HandlesTable.id].value.toString(),
            handle = row[HandlesTable.handle],
            handleLowercase = row[HandlesTable.handleLowercase],
            userId = row[HandlesTable.userId],
            ownerId = row[HandlesTable.ownerId],
            status = row[HandlesTable.status],
            isAvailable = row[HandlesTable.isAvailable],
            isReserved = row[HandlesTable.isReserved],
            isPremium = row[HandlesTable.isPremium],
            isProtected = row[HandlesTable.isProtected],
            isCelebrity = row[HandlesTable.isCelebrity],
            isBrand = row[HandlesTable.isBrand],
            isVerified = row[HandlesTable.isVerified],
            reservationType = row[HandlesTable.reservationType],
            reservationReason = row[HandlesTable.reservationReason],
            reservationDocument = row[HandlesTable.reservationDocument],
            reservationApprovedBy = row[HandlesTable.reservationApprovedBy],
            reservationApprovedAt = row[HandlesTable.reservationApprovedAt],
            reservationExpiresAt = row[HandlesTable.reservationExpiresAt],
            transferInProgress = row[HandlesTable.transferInProgress],
            transferFromUserId = row[HandlesTable.transferFromUserId],
            transferToUserId = row[HandlesTable.transferToUserId],
            transferRequestedAt = row[HandlesTable.transferRequestedAt],
            transferToken = row[HandlesTable.transferToken],
            transferExpiresAt = row[HandlesTable.transferExpiresAt],
            transferApprovedBy = row[HandlesTable.transferApprovedBy],
            transferCompletedAt = row[HandlesTable.transferCompletedAt],
            syncedToPlatforms = row[HandlesTable.syncedToPlatforms],
            syncStatus = row[HandlesTable.syncStatus],
            lastSyncedAt = row[HandlesTable.lastSyncedAt],
            syncFailureReason = row[HandlesTable.syncFailureReason],
            syncRetryCount = row[HandlesTable.syncRetryCount],
            originalOwnerId = row[HandlesTable.originalOwnerId],
            totalTransfers = row[HandlesTable.totalTransfers],
            lastTransferredAt = row[HandlesTable.lastTransferredAt],
            popularityScore = row[HandlesTable.popularityScore],
            searchCount = row[HandlesTable.searchCount],
            viewCount = row[HandlesTable.viewCount],
            mentionCount = row[HandlesTable.mentionCount],
            securityLevel = row[HandlesTable.securityLevel],
            requiresMFA = row[HandlesTable.requiresMFA],
            lastSecurityCheck = row[HandlesTable.lastSecurityCheck],
            fraudFlags = row[HandlesTable.fraudFlags],
            riskScore = row[HandlesTable.riskScore],
            price = row[HandlesTable.price],
            currency = row[HandlesTable.currency],
            isForSale = row[HandlesTable.isForSale],
            marketplaceListedAt = row[HandlesTable.marketplaceListedAt],
            lastSalePrice = row[HandlesTable.lastSalePrice],
            lastSaleAt = row[HandlesTable.lastSaleAt],
            description = row[HandlesTable.description],
            category = row[HandlesTable.category],
            tags = row[HandlesTable.tags],
            associatedDomains = row[HandlesTable.associatedDomains],
            socialLinks = row[HandlesTable.socialLinks],
            hasActiveAppeal = row[HandlesTable.hasActiveAppeal],
            appealReason = row[HandlesTable.appealReason],
            appealSubmittedAt = row[HandlesTable.appealSubmittedAt],
            appealResolvedAt = row[HandlesTable.appealResolvedAt],
            appealResolution = row[HandlesTable.appealResolution],
            appealResolvedBy = row[HandlesTable.appealResolvedBy],
            trademarked = row[HandlesTable.trademarked],
            trademarkNumber = row[HandlesTable.trademarkNumber],
            trademarkCountry = row[HandlesTable.trademarkCountry],
            copyrighted = row[HandlesTable.copyrighted],
            copyrightNumber = row[HandlesTable.copyrightNumber],
            legalDocuments = row[HandlesTable.legalDocuments],
            createdAt = row[HandlesTable.createdAt],
            updatedAt = row[HandlesTable.updatedAt],
            createdBy = row[HandlesTable.createdBy],
            updatedBy = row[HandlesTable.updatedBy],
            version = row[HandlesTable.version]
        )
    }
}
