package com.entativa.id.repository

import com.entativa.id.database.tables.MFAMethodsTable
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
 * MFA Repository for Entativa ID
 * Handles all database operations for Multi-Factor Authentication methods
 * 
 * @author Neo Qiss
 * @status Production-ready MFA management with enterprise security
 */
@Repository
class MFARepository {
    
    private val logger = LoggerFactory.getLogger(MFARepository::class.java)
    
    /**
     * Create a new MFA method
     */
    suspend fun createMFAMethod(mfaMethod: CreateMFAMethodRequest): Result<MFAMethod> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating MFA method: ${mfaMethod.methodType} for user: ${mfaMethod.userId}")
                
                val mfaId = transaction {
                    MFAMethodsTable.insertAndGetId {
                        it[userId] = mfaMethod.userId
                        it[methodType] = mfaMethod.methodType
                        it[methodValue] = mfaMethod.methodValue
                        it[encryptedSecret] = mfaMethod.encryptedSecret
                        it[backupCodes] = mfaMethod.backupCodes
                        it[MFAMethodsTable.isActive] = mfaMethod.isActive
                        it[isVerified] = mfaMethod.isVerified
                        it[isPrimary] = mfaMethod.isPrimary
                        it[isDefault] = mfaMethod.isDefault
                        it[priority] = mfaMethod.priority
                        it[deviceId] = mfaMethod.deviceId
                        it[platform] = mfaMethod.platform
                        it[securityLevel] = mfaMethod.securityLevel
                        it[trustScore] = mfaMethod.trustScore
                        it[settings] = mfaMethod.settings
                        it[metadata] = mfaMethod.metadata
                        it[createdBy] = mfaMethod.createdBy
                    }
                }
                
                val createdMFA = findById(mfaId.value.toString())
                if (createdMFA.isSuccess) {
                    logger.info("✅ MFA method created successfully: ${mfaMethod.methodType}")
                    createdMFA
                } else {
                    logger.error("❌ Failed to retrieve created MFA method")
                    Result.failure(Exception("Failed to retrieve created MFA method"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create MFA method: ${mfaMethod.methodType}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find MFA method by ID
     */
    suspend fun findById(id: String): Result<MFAMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val mfaMethod = transaction {
                    MFAMethodsTable.select { 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToMFAMethod(row)
                    }
                }
                
                if (mfaMethod != null) {
                    Result.success(mfaMethod)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find MFA method by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find MFA methods by user ID
     */
    suspend fun findByUserId(
        userId: String,
        activeOnly: Boolean = true,
        verifiedOnly: Boolean = false
    ): Result<List<MFAMethod>> {
        return withContext(Dispatchers.IO) {
            try {
                val mfaMethods = transaction {
                    var query = MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and (MFAMethodsTable.deletedAt.isNull()) 
                    }
                    
                    if (activeOnly) {
                        query = query.andWhere { MFAMethodsTable.isActive eq true }
                    }
                    
                    if (verifiedOnly) {
                        query = query.andWhere { MFAMethodsTable.isVerified eq true }
                    }
                    
                    query
                        .orderBy(MFAMethodsTable.priority to SortOrder.ASC, MFAMethodsTable.createdAt to SortOrder.DESC)
                        .map { row -> mapRowToMFAMethod(row) }
                }
                
                Result.success(mfaMethods)
            } catch (e: Exception) {
                logger.error("❌ Failed to find MFA methods for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find MFA method by user ID and method type
     */
    suspend fun findByUserIdAndType(
        userId: String,
        methodType: String
    ): Result<MFAMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val mfaMethod = transaction {
                    MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.methodType eq methodType) and 
                        (MFAMethodsTable.isActive eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToMFAMethod(row)
                    }
                }
                
                if (mfaMethod != null) {
                    Result.success(mfaMethod)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found for user: $userId, type: $methodType"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find MFA method for user: $userId, type: $methodType", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get primary MFA method for user
     */
    suspend fun getPrimaryMFAMethod(userId: String): Result<MFAMethod> {
        return withContext(Dispatchers.IO) {
            try {
                val primaryMFA = transaction {
                    MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isPrimary eq true) and 
                        (MFAMethodsTable.isActive eq true) and 
                        (MFAMethodsTable.isVerified eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToMFAMethod(row)
                    }
                }
                
                if (primaryMFA != null) {
                    Result.success(primaryMFA)
                } else {
                    Result.failure(NoSuchElementException("No primary MFA method found for user: $userId"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get primary MFA method for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verify MFA method
     */
    suspend fun verifyMFAMethod(
        id: String,
        verificationCode: String,
        verifiedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("✅ Verifying MFA method: $id")
                
                val verified = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isVerified] = true
                        it[verifiedAt] = Instant.now()
                        it[MFAMethodsTable.verifiedBy] = verifiedBy
                        it[verificationCode] = verificationCode
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = verifiedBy
                        it[version] = version + 1
                    }
                } > 0
                
                if (verified) {
                    logger.info("✅ MFA method verified: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to verify MFA method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Set MFA method as primary
     */
    suspend fun setPrimaryMFAMethod(userId: String, mfaId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("⭐ Setting primary MFA method for user: $userId")
                
                transaction {
                    // First, unset all other MFA methods as primary for this user
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isPrimary eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = false
                        it[updatedAt] = Instant.now()
                    }
                    
                    // Then set the specified MFA method as primary
                    val updated = MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(mfaId)) and 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isVerified eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = true
                        it[updatedAt] = Instant.now()
                    } > 0
                    
                    if (updated) {
                        logger.info("✅ Primary MFA method set for user: $userId")
                        Result.success(true)
                    } else {
                        Result.failure(NoSuchElementException("MFA method not found, not verified, or not owned by user"))
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to set primary MFA method: $mfaId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update MFA method usage
     */
    suspend fun updateUsage(
        id: String,
        ipAddress: String? = null,
        deviceId: String? = null,
        location: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[lastUsedAt] = Instant.now()
                        it[usageCount] = usageCount + 1
                        ipAddress?.let { ip -> it[lastUsedIp] = ip }
                        deviceId?.let { device -> it[lastUsedDevice] = device }
                        location?.let { loc -> it[lastUsedLocation] = loc }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update MFA method usage: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Use backup code
     */
    suspend fun useBackupCode(
        id: String,
        backupCode: String,
        usedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔑 Using backup code for MFA method: $id")
                
                val updated = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[usedBackupCodes] = usedBackupCodes?.let { used -> "$used,$backupCode" } ?: backupCode
                        it[backupCodeUsedAt] = Instant.now()
                        it[backupCodeUsedBy] = usedBy
                        it[lastUsedAt] = Instant.now()
                        it[usageCount] = usageCount + 1
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = usedBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Backup code used for MFA method: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to use backup code for MFA method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Regenerate backup codes
     */
    suspend fun regenerateBackupCodes(
        id: String,
        newBackupCodes: String,
        regeneratedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Regenerating backup codes for MFA method: $id")
                
                val updated = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[backupCodes] = newBackupCodes
                        it[usedBackupCodes] = null
                        it[backupCodesRegeneratedAt] = Instant.now()
                        it[backupCodesRegeneratedBy] = regeneratedBy
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = regeneratedBy
                        it[version] = version + 1
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Backup codes regenerated for MFA method: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to regenerate backup codes for MFA method: $id", e)
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
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        securityLevel?.let { level -> it[MFAMethodsTable.securityLevel] = level }
                        trustScore?.let { score -> it[MFAMethodsTable.trustScore] = score }
                        riskFlags?.let { flags -> it[MFAMethodsTable.riskFlags] = flags }
                        it[lastSecurityCheck] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update MFA security attributes: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Deactivate MFA method
     */
    suspend fun deactivateMFAMethod(
        id: String,
        reason: String,
        deactivatedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Deactivating MFA method: $id")
                
                val deactivated = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[isActive] = false
                        it[isPrimary] = false
                        it[isDefault] = false
                        it[deactivatedAt] = Instant.now()
                        it[MFAMethodsTable.deactivatedBy] = deactivatedBy
                        it[deactivationReason] = reason
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deactivatedBy
                    }
                } > 0
                
                if (deactivated) {
                    logger.info("✅ MFA method deactivated: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to deactivate MFA method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get MFA statistics for user
     */
    suspend fun getMFAStatistics(userId: String): Result<MFAStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating MFA statistics for user: $userId")
                
                val stats = transaction {
                    val totalMethods = MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and (MFAMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val activeMethods = MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isActive eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val verifiedMethods = MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isVerified eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }.count()
                    
                    val primaryMethod = MFAMethodsTable.select { 
                        (MFAMethodsTable.userId eq userId) and 
                        (MFAMethodsTable.isPrimary eq true) and 
                        (MFAMethodsTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row -> mapRowToMFAMethod(row) }
                    
                    MFAStatistics(
                        totalMethods = totalMethods,
                        activeMethods = activeMethods,
                        verifiedMethods = verifiedMethods,
                        hasPrimaryMethod = primaryMethod != null,
                        primaryMethodType = primaryMethod?.methodType,
                        generatedAt = Instant.now()
                    )
                }
                
                logger.debug("✅ Generated MFA statistics for user: $userId")
                Result.success(stats)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate MFA statistics for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete MFA method
     */
    suspend fun deleteMFAMethod(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting MFA method: $id")
                
                val deleted = transaction {
                    MFAMethodsTable.update({ 
                        (MFAMethodsTable.id eq UUID.fromString(id)) and (MFAMethodsTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[MFAMethodsTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[isPrimary] = false
                        it[isDefault] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ MFA method soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("MFA method not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete MFA method: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to MFAMethod domain object
     */
    private fun mapRowToMFAMethod(row: ResultRow): MFAMethod {
        return MFAMethod(
            id = row[MFAMethodsTable.id].value.toString(),
            userId = row[MFAMethodsTable.userId],
            methodType = row[MFAMethodsTable.methodType],
            methodValue = row[MFAMethodsTable.methodValue],
            encryptedSecret = row[MFAMethodsTable.encryptedSecret],
            backupCodes = row[MFAMethodsTable.backupCodes],
            usedBackupCodes = row[MFAMethodsTable.usedBackupCodes],
            isActive = row[MFAMethodsTable.isActive],
            isVerified = row[MFAMethodsTable.isVerified],
            isPrimary = row[MFAMethodsTable.isPrimary],
            isDefault = row[MFAMethodsTable.isDefault],
            priority = row[MFAMethodsTable.priority],
            deviceId = row[MFAMethodsTable.deviceId],
            platform = row[MFAMethodsTable.platform],
            securityLevel = row[MFAMethodsTable.securityLevel],
            trustScore = row[MFAMethodsTable.trustScore],
            riskFlags = row[MFAMethodsTable.riskFlags],
            settings = row[MFAMethodsTable.settings],
            metadata = row[MFAMethodsTable.metadata],
            verifiedAt = row[MFAMethodsTable.verifiedAt],
            verifiedBy = row[MFAMethodsTable.verifiedBy],
            verificationCode = row[MFAMethodsTable.verificationCode],
            lastUsedAt = row[MFAMethodsTable.lastUsedAt],
            usageCount = row[MFAMethodsTable.usageCount],
            lastUsedIp = row[MFAMethodsTable.lastUsedIp],
            lastUsedDevice = row[MFAMethodsTable.lastUsedDevice],
            lastUsedLocation = row[MFAMethodsTable.lastUsedLocation],
            lastSecurityCheck = row[MFAMethodsTable.lastSecurityCheck],
            backupCodeUsedAt = row[MFAMethodsTable.backupCodeUsedAt],
            backupCodeUsedBy = row[MFAMethodsTable.backupCodeUsedBy],
            backupCodesRegeneratedAt = row[MFAMethodsTable.backupCodesRegeneratedAt],
            backupCodesRegeneratedBy = row[MFAMethodsTable.backupCodesRegeneratedBy],
            deactivatedAt = row[MFAMethodsTable.deactivatedAt],
            deactivatedBy = row[MFAMethodsTable.deactivatedBy],
            deactivationReason = row[MFAMethodsTable.deactivationReason],
            createdAt = row[MFAMethodsTable.createdAt],
            updatedAt = row[MFAMethodsTable.updatedAt],
            createdBy = row[MFAMethodsTable.createdBy],
            updatedBy = row[MFAMethodsTable.updatedBy],
            version = row[MFAMethodsTable.version]
        )
    }
}
