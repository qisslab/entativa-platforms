package com.entativa.id.repository

import com.entativa.id.database.tables.RecoveryMethodsTable
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
 * Recovery Method Repository for Entativa ID
 * Handles all database operations for account recovery methods and validation
 * 
 * @author Neo Qiss
 * @status Production-ready recovery management with enterprise security
 */
@Repository
class RecoveryMethodRepository {
    
    private val logger = LoggerFactory.getLogger(RecoveryMethodRepository::class.java)
    
    /**
     * Create a new recovery method
     */
    suspend fun createRecoveryMethod(recoveryMethod: CreateRecoveryMethodRequest): Result<RecoveryMethod> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating recovery method: ${recoveryMethod.methodType} for user: ${recoveryMethod.userId}")
                
                val recoveryId = transaction {
                    RecoveryMethodsTable.insertAndGetId {
                        it[userId] = recoveryMethod.userId
                        it[methodType] = recoveryMethod.methodType
                        it[methodValue] = recoveryMethod.methodValue
                        it[encryptedValue] = recoveryMethod.encryptedValue
                        it[RecoveryMethodsTable.isActive] = recoveryMethod.isActive
                        it[isVerified] = recoveryMethod.isVerified
                        it[isPrimary] = recoveryMethod.isPrimary
                        it[priority] = recoveryMethod.priority
                        it[securityLevel] = recoveryMethod.securityLevel
                        it[trustScore] = recoveryMethod.trustScore
                        it[maxAttempts] = recoveryMethod.maxAttempts
                        it[expirationHours] = recoveryMethod.expirationHours
                        it[settings] = recoveryMethod.settings
                        it[metadata] = recoveryMethod.metadata
                        it[createdBy] = recoveryMethod.createdBy
                    }
                }
                
                val createdRecovery = findById(recoveryId.value.toString())
                if (createdRecovery.isSuccess) {
                    logger.info("✅ Recovery method created successfully: ${recoveryMethod.methodType}")
                    createdRecovery
                } else {
                    logger.error("❌ Failed to retrieve created recovery method")
                    Result.failure(Exception("Failed to retrieve created recovery method"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create recovery method: ${recoveryMethod.methodType}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find recovery method by ID
     */
    suspend fun findById(id: String): Result<RecoveryMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val recoveryMethod = transaction {
                    RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToRecoveryMethod(row)
                    }
                }
                
                if (recoveryMethod != null) {
                    Result.success(recoveryMethod)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find recovery method by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find recovery methods by user ID
     */
    suspend fun findByUserId(
        userId: String,
        activeOnly: Boolean = true,
        verifiedOnly: Boolean = false
    ): Result<List<RecoveryMethod>> {
        return withContext(Dispatchers.IO) {
            try {
                val recoveryMethods = transaction {
                    var query = RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }
                    
                    if (activeOnly) {
                        query = query.andWhere { RecoveryMethodsTable.isActive eq true }
                    }
                    
                    if (verifiedOnly) {
                        query = query.andWhere { RecoveryMethodsTable.isVerified eq true }
                    }
                    
                    query
                        .orderBy(RecoveryMethodsTable.priority to SortOrder.ASC, RecoveryMethodsTable.createdAt to SortOrder.DESC)
                        .map { row -> mapRowToRecoveryMethod(row) }
                }
                
                Result.success(recoveryMethods)
            } catch (e: Exception) {
                logger.error("❌ Failed to find recovery methods for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find recovery method by user ID and method type
     */
    suspend fun findByUserIdAndType(
        userId: String,
        methodType: String
    ): Result<RecoveryMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val recoveryMethod = transaction {
                    RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.methodType eq methodType) and 
                        (RecoveryMethodsTable.isActive eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToRecoveryMethod(row)
                    }
                }
                
                if (recoveryMethod != null) {
                    Result.success(recoveryMethod)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found for user: $userId, type: $methodType"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find recovery method for user: $userId, type: $methodType", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get primary recovery method for user
     */
    suspend fun getPrimaryRecoveryMethod(userId: String): Result<RecoveryMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val primaryRecovery = transaction {
                    RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isPrimary eq true) and 
                        (RecoveryMethodsTable.isActive eq true) and 
                        (RecoveryMethodsTable.isVerified eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToRecoveryMethod(row)
                    }
                }
                
                if (primaryRecovery != null) {
                    Result.success(primaryRecovery)
                } else {
                    Result.failure(NoSuchElementException("No primary recovery method found for user: $userId"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get primary recovery method for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verify recovery method
     */
    suspend fun verifyRecoveryMethod(
        id: String,
        verificationCode: String,
        verifiedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("✅ Verifying recovery method: $id")
                
                val verified = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isVerified] = true
                        it[verifiedAt] = Instant.now()
                        it[RecoveryMethodsTable.verifiedBy] = verifiedBy
                        it[verificationCode] = verificationCode
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = verifiedBy
                        it[version] = version + 1
                    }
                } > 0
                
                if (verified) {
                    logger.info("✅ Recovery method verified: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to verify recovery method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Set recovery method as primary
     */
    suspend fun setPrimaryRecoveryMethod(userId: String, recoveryId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("⭐ Setting primary recovery method for user: $userId")
                
                transaction {
                    // First, unset all other recovery methods as primary for this user
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isPrimary eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = false
                        it[updatedAt] = Instant.now()
                    }
                    
                    // Then set the specified recovery method as primary
                    val updated = RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(recoveryId)) and 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isVerified eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = true
                        it[updatedAt] = Instant.now()
                    } > 0
                    
                    if (updated) {
                        logger.info("✅ Primary recovery method set for user: $userId")
                        Result.success(true)
                    } else {
                        Result.failure(NoSuchElementException("Recovery method not found, not verified, or not owned by user"))
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to set primary recovery method: $recoveryId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Start recovery process
     */
    suspend fun startRecoveryProcess(
        id: String,
        recoveryCode: String,
        expiresAt: Instant,
        initiatedBy: String,
        ipAddress: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Starting recovery process for method: $id")
                
                val started = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and 
                        (RecoveryMethodsTable.isActive eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[activeRecoveryCode] = recoveryCode
                        it[recoveryCodeExpiresAt] = expiresAt
                        it[recoveryInitiatedAt] = Instant.now()
                        it[RecoveryMethodsTable.recoveryInitiatedBy] = initiatedBy
                        it[lastAttemptAt] = Instant.now()
                        it[lastAttemptIp] = ipAddress
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = initiatedBy
                    }
                } > 0
                
                if (started) {
                    logger.info("✅ Recovery process started for method: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found or not active: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to start recovery process for method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Validate recovery code
     */
    suspend fun validateRecoveryCode(
        id: String,
        providedCode: String,
        ipAddress: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Validating recovery code for method: $id")
                
                val recoveryMethod = transaction {
                    RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToRecoveryMethod(row)
                    }
                }
                
                if (recoveryMethod == null) {
                    return@withContext Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
                
                // Check if recovery code is still valid
                val now = Instant.now()
                if (recoveryMethod.recoveryCodeExpiresAt?.isBefore(now) == true) {
                    return@withContext Result.failure(IllegalStateException("Recovery code has expired"))
                }
                
                // Check if max attempts exceeded
                if (recoveryMethod.attemptCount >= recoveryMethod.maxAttempts) {
                    return@withContext Result.failure(IllegalStateException("Maximum recovery attempts exceeded"))
                }
                
                // Update attempt count
                transaction {
                    RecoveryMethodsTable.update({ 
                        RecoveryMethodsTable.id eq UUID.fromString(id) 
                    }) {
                        it[attemptCount] = attemptCount + 1
                        it[lastAttemptAt] = now
                        it[lastAttemptIp] = ipAddress
                        it[updatedAt] = now
                    }
                }
                
                // Validate the code
                val isValid = recoveryMethod.activeRecoveryCode == providedCode
                
                if (isValid) {
                    // Mark as used and clear the code
                    transaction {
                        RecoveryMethodsTable.update({ 
                            RecoveryMethodsTable.id eq UUID.fromString(id) 
                        }) {
                            it[activeRecoveryCode] = null
                            it[recoveryCodeExpiresAt] = null
                            it[lastUsedAt] = now
                            it[usageCount] = usageCount + 1
                            it[attemptCount] = 0 // Reset attempts after successful use
                            it[updatedAt] = now
                        }
                    }
                    
                    logger.info("✅ Recovery code validated successfully for method: $id")
                    Result.success(true)
                } else {
                    logger.warn("❌ Invalid recovery code provided for method: $id")
                    Result.success(false)
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to validate recovery code for method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update recovery method usage
     */
    suspend fun updateUsage(
        id: String,
        ipAddress: String? = null,
        location: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[lastUsedAt] = Instant.now()
                        it[usageCount] = usageCount + 1
                        ipAddress?.let { ip -> it[lastUsedIp] = ip }
                        location?.let { loc -> it[lastUsedLocation] = loc }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update recovery method usage: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Reset attempt count
     */
    suspend fun resetAttemptCount(id: String, resetBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Resetting attempt count for recovery method: $id")
                
                val updated = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[attemptCount] = 0
                        it[activeRecoveryCode] = null
                        it[recoveryCodeExpiresAt] = null
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = resetBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Attempt count reset for recovery method: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to reset attempt count for recovery method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update security attributes
     */
    suspend fun updateSecurityAttributes(
        id: String,
        securityLevel: String? = null,
        trustScore: Double? = null,
        riskFlags: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        securityLevel?.let { level -> it[RecoveryMethodsTable.securityLevel] = level }
                        trustScore?.let { score -> it[RecoveryMethodsTable.trustScore] = score }
                        riskFlags?.let { flags -> it[RecoveryMethodsTable.riskFlags] = flags }
                        it[lastSecurityCheck] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update recovery security attributes: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Deactivate recovery method
     */
    suspend fun deactivateRecoveryMethod(
        id: String,
        reason: String,
        deactivatedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Deactivating recovery method: $id")
                
                val deactivated = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isActive] = false
                        it[isPrimary] = false
                        it[activeRecoveryCode] = null
                        it[recoveryCodeExpiresAt] = null
                        it[deactivatedAt] = Instant.now()
                        it[RecoveryMethodsTable.deactivatedBy] = deactivatedBy
                        it[deactivationReason] = reason
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deactivatedBy
                    }
                } > 0
                
                if (deactivated) {
                    logger.info("✅ Recovery method deactivated: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to deactivate recovery method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get recovery statistics for user
     */
    suspend fun getRecoveryStatistics(userId: String): Result<RecoveryStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating recovery statistics for user: $userId")
                
                val stats = transaction {
                    val totalMethods = RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val activeMethods = RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isActive eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val verifiedMethods = RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isVerified eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val primaryMethod = RecoveryMethodsTable.select { 
                        (RecoveryMethodsTable.userId eq userId) and 
                        (RecoveryMethodsTable.isPrimary eq true) and 
                        (RecoveryMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row -> mapRowToRecoveryMethod(row) }
                    
                    RecoveryStatistics(
                        totalMethods = totalMethods,
                        activeMethods = activeMethods,
                        verifiedMethods = verifiedMethods,
                        hasPrimaryMethod = primaryMethod != null,
                        primaryMethodType = primaryMethod?.methodType,
                        generatedAt = Instant.now()
                    )
                }
                
                logger.debug("✅ Generated recovery statistics for user: $userId")
                Result.success(stats)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate recovery statistics for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete recovery method
     */
    suspend fun deleteRecoveryMethod(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting recovery method: $id")
                
                val deleted = transaction {
                    RecoveryMethodsTable.update({ 
                        (RecoveryMethodsTable.id eq UUID.fromString(id)) and (RecoveryMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[RecoveryMethodsTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[isPrimary] = false
                        it[activeRecoveryCode] = null
                        it[recoveryCodeExpiresAt] = null
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Recovery method soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Recovery method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete recovery method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to RecoveryMethod domain object
     */
    private fun mapRowToRecoveryMethod(row: ResultRow): RecoveryMethod {
        return RecoveryMethod(
            id = row[RecoveryMethodsTable.id].value.toString(),
            userId = row[RecoveryMethodsTable.userId],
            methodType = row[RecoveryMethodsTable.methodType],
            methodValue = row[RecoveryMethodsTable.methodValue],
            encryptedValue = row[RecoveryMethodsTable.encryptedValue],
            isActive = row[RecoveryMethodsTable.isActive],
            isVerified = row[RecoveryMethodsTable.isVerified],
            isPrimary = row[RecoveryMethodsTable.isPrimary],
            priority = row[RecoveryMethodsTable.priority],
            securityLevel = row[RecoveryMethodsTable.securityLevel],
            trustScore = row[RecoveryMethodsTable.trustScore],
            riskFlags = row[RecoveryMethodsTable.riskFlags],
            maxAttempts = row[RecoveryMethodsTable.maxAttempts],
            expirationHours = row[RecoveryMethodsTable.expirationHours],
            settings = row[RecoveryMethodsTable.settings],
            metadata = row[RecoveryMethodsTable.metadata],
            verifiedAt = row[RecoveryMethodsTable.verifiedAt],
            verifiedBy = row[RecoveryMethodsTable.verifiedBy],
            verificationCode = row[RecoveryMethodsTable.verificationCode],
            activeRecoveryCode = row[RecoveryMethodsTable.activeRecoveryCode],
            recoveryCodeExpiresAt = row[RecoveryMethodsTable.recoveryCodeExpiresAt],
            recoveryInitiatedAt = row[RecoveryMethodsTable.recoveryInitiatedAt],
            recoveryInitiatedBy = row[RecoveryMethodsTable.recoveryInitiatedBy],
            lastUsedAt = row[RecoveryMethodsTable.lastUsedAt],
            usageCount = row[RecoveryMethodsTable.usageCount],
            lastUsedIp = row[RecoveryMethodsTable.lastUsedIp],
            lastUsedLocation = row[RecoveryMethodsTable.lastUsedLocation],
            attemptCount = row[RecoveryMethodsTable.attemptCount],
            lastAttemptAt = row[RecoveryMethodsTable.lastAttemptAt],
            lastAttemptIp = row[RecoveryMethodsTable.lastAttemptIp],
            lastSecurityCheck = row[RecoveryMethodsTable.lastSecurityCheck],
            deactivatedAt = row[RecoveryMethodsTable.deactivatedAt],
            deactivatedBy = row[RecoveryMethodsTable.deactivatedBy],
            deactivationReason = row[RecoveryMethodsTable.deactivationReason],
            createdAt = row[RecoveryMethodsTable.createdAt],
            updatedAt = row[RecoveryMethodsTable.updatedAt],
            createdBy = row[RecoveryMethodsTable.createdBy],
            updatedBy = row[RecoveryMethodsTable.updatedBy],
            version = row[RecoveryMethodsTable.version]
        )
    }
}
