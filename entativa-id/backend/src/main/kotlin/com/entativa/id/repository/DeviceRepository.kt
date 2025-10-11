package com.entativa.id.repository

import com.entativa.id.database.tables.DevicesTable
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
 * Device Repository for Entativa ID
 * Handles all database operations for device management, tracking, and security
 * 
 * @author Neo Qiss
 * @status Production-ready device management with enterprise security
 */
@Repository
class DeviceRepository {
    
    private val logger = LoggerFactory.getLogger(DeviceRepository::class.java)
    
    /**
     * Create/Register a new device
     */
    suspend fun createDevice(device: CreateDeviceRequest): Result<Device> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📱 Registering new device for user: ${device.userId}")
                
                val deviceId = transaction {
                    DevicesTable.insertAndGetId {
                        it[userId] = device.userId
                        it[deviceFingerprint] = device.deviceFingerprint
                        it[deviceName] = device.deviceName
                        it[deviceType] = device.deviceType
                        it[DevicesTable.platform] = device.platform
                        it[osName] = device.osName
                        it[osVersion] = device.osVersion
                        it[browserName] = device.browserName
                        it[browserVersion] = device.browserVersion
                        it[DevicesTable.isActive] = device.isActive
                        it[isTrusted] = device.isTrusted
                        it[isRegistered] = device.isRegistered
                        it[isPrimary] = device.isPrimary
                        it[DevicesTable.ipAddress] = device.ipAddress
                        it[userAgent] = device.userAgent
                        it[screenResolution] = device.screenResolution
                        it[timezone] = device.timezone
                        it[language] = device.language
                        it[location] = device.location
                        it[hardwareInfo] = device.hardwareInfo
                        it[networkInfo] = device.networkInfo
                        it[securityLevel] = device.securityLevel
                        it[trustScore] = device.trustScore
                        it[riskScore] = device.riskScore
                        it[deviceMetadata] = device.deviceMetadata
                        it[registrationMethod] = device.registrationMethod
                        it[createdBy] = device.createdBy
                    }
                }
                
                val createdDevice = findById(deviceId.value.toString())
                if (createdDevice.isSuccess) {
                    logger.info("✅ Device registered successfully: ${device.deviceName}")
                    createdDevice
                } else {
                    logger.error("❌ Failed to retrieve created device")
                    Result.failure(Exception("Failed to retrieve created device"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to register device: ${device.deviceName}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find device by ID
     */
    suspend fun findById(id: String): Result<Device> {
        return withContext(Dispatchers.IO) {
            try {
                val device = transaction {
                    DevicesTable.select { 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToDevice(row)
                    }
                }
                
                if (device != null) {
                    Result.success(device)
                } else {
                    Result.failure(NoSuchElementException("Device not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find device by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find device by fingerprint
     */
    suspend fun findByFingerprint(fingerprint: String): Result<Device> {
        return withContext(Dispatchers.IO) {
            try {
                val device = transaction {
                    DevicesTable.select { 
                        (DevicesTable.deviceFingerprint eq fingerprint) and (DevicesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToDevice(row)
                    }
                }
                
                if (device != null) {
                    Result.success(device)
                } else {
                    Result.failure(NoSuchElementException("Device not found with fingerprint: $fingerprint"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find device by fingerprint", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find devices by user ID
     */
    suspend fun findByUserId(
        userId: String,
        activeOnly: Boolean = true,
        trustedOnly: Boolean = false
    ): Result<List<Device>> {
        return withContext(Dispatchers.IO) {
            try {
                val devices = transaction {
                    var query = DevicesTable.select { 
                        (DevicesTable.userId eq userId) and (DevicesTable.deletedAt.isNull()) 
                    }
                    
                    if (activeOnly) {
                        query = query.andWhere { DevicesTable.isActive eq true }
                    }
                    
                    if (trustedOnly) {
                        query = query.andWhere { DevicesTable.isTrusted eq true }
                    }
                    
                    query
                        .orderBy(DevicesTable.lastSeenAt to SortOrder.DESC)
                        .map { row -> mapRowToDevice(row) }
                }
                
                Result.success(devices)
            } catch (e: Exception) {
                logger.error("❌ Failed to find devices for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update device activity (last seen, IP, location)
     */
    suspend fun updateActivity(
        id: String,
        ipAddress: String? = null,
        location: String? = null,
        userAgent: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[lastSeenAt] = Instant.now()
                        it[loginCount] = loginCount + 1
                        ipAddress?.let { ip -> it[DevicesTable.ipAddress] = ip }
                        location?.let { loc -> it[DevicesTable.location] = loc }
                        userAgent?.let { ua -> it[DevicesTable.userAgent] = ua }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update device activity: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update device trust status
     */
    suspend fun updateTrustStatus(
        id: String,
        isTrusted: Boolean,
        trustScore: Double,
        reason: String,
        updatedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Updating device trust status: $id to $isTrusted")
                
                val updated = transaction {
                    DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[DevicesTable.isTrusted] = isTrusted
                        it[DevicesTable.trustScore] = trustScore
                        if (isTrusted) {
                            it[trustedAt] = Instant.now()
                            it[trustedBy] = updatedBy
                        } else {
                            it[unTrustedAt] = Instant.now()
                            it[unTrustedBy] = updatedBy
                        }
                        it[trustChangeReason] = reason
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = updatedBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Device trust updated: $id -> $isTrusted")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Device not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to update device trust: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update device security attributes
     */
    suspend fun updateSecurityAttributes(
        id: String,
        securityLevel: String? = null,
        riskScore: Double? = null,
        fraudFlags: String? = null,
        securityFlags: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }) {
                        securityLevel?.let { level -> it[DevicesTable.securityLevel] = level }
                        riskScore?.let { score -> it[DevicesTable.riskScore] = score }
                        fraudFlags?.let { flags -> it[DevicesTable.fraudFlags] = flags }
                        securityFlags?.let { flags -> it[DevicesTable.securityFlags] = flags }
                        it[lastSecurityCheck] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update device security attributes: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Block/Unblock device
     */
    suspend fun updateBlockStatus(
        id: String,
        isBlocked: Boolean,
        reason: String,
        blockedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🚫 ${if (isBlocked) "Blocking" else "Unblocking"} device: $id")
                
                val updated = transaction {
                    DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[DevicesTable.isBlocked] = isBlocked
                        it[DevicesTable.isActive] = !isBlocked
                        if (isBlocked) {
                            it[blockedAt] = Instant.now()
                            it[DevicesTable.blockedBy] = blockedBy
                            it[blockReason] = reason
                        } else {
                            it[unblockedAt] = Instant.now()
                            it[unblockedBy] = blockedBy
                            it[unblockReason] = reason
                        }
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = blockedBy
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Device ${if (isBlocked) "blocked" else "unblocked"}: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Device not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to update device block status: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Set device as primary for user
     */
    suspend fun setPrimaryDevice(userId: String, deviceId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("⭐ Setting primary device for user: $userId")
                
                transaction {
                    // First, unset all other devices as primary for this user
                    DevicesTable.update({ 
                        (DevicesTable.userId eq userId) and 
                        (DevicesTable.isPrimary eq true) and 
                        (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = false
                        it[updatedAt] = Instant.now()
                    }
                    
                    // Then set the specified device as primary
                    val updated = DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(deviceId)) and 
                        (DevicesTable.userId eq userId) and 
                        (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[isPrimary] = true
                        it[updatedAt] = Instant.now()
                    } > 0
                    
                    if (updated) {
                        logger.info("✅ Primary device set for user: $userId")
                        Result.success(true)
                    } else {
                        Result.failure(NoSuchElementException("Device not found or not owned by user"))
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to set primary device: $deviceId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search devices with filters
     */
    suspend fun searchDevices(
        userId: String? = null,
        deviceType: String? = null,
        platform: String? = null,
        isActive: Boolean? = null,
        isTrusted: Boolean? = null,
        isBlocked: Boolean? = null,
        minTrustScore: Double? = null,
        maxRiskScore: Double? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Device>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Searching devices with filters")
                
                val devices = transaction {
                    var query = DevicesTable.select { DevicesTable.deletedAt.isNull() }
                    
                    userId?.let { uid ->
                        query = query.andWhere { DevicesTable.userId eq uid }
                    }
                    
                    deviceType?.let { type ->
                        query = query.andWhere { DevicesTable.deviceType eq type }
                    }
                    
                    platform?.let { plat ->
                        query = query.andWhere { DevicesTable.platform eq plat }
                    }
                    
                    isActive?.let { active ->
                        query = query.andWhere { DevicesTable.isActive eq active }
                    }
                    
                    isTrusted?.let { trusted ->
                        query = query.andWhere { DevicesTable.isTrusted eq trusted }
                    }
                    
                    isBlocked?.let { blocked ->
                        query = query.andWhere { DevicesTable.isBlocked eq blocked }
                    }
                    
                    minTrustScore?.let { score ->
                        query = query.andWhere { DevicesTable.trustScore greaterEq score }
                    }
                    
                    maxRiskScore?.let { score ->
                        query = query.andWhere { DevicesTable.riskScore lessEq score }
                    }
                    
                    query
                        .orderBy(DevicesTable.lastSeenAt to SortOrder.DESC)
                        .limit(limit, offset.toLong())
                        .map { row -> mapRowToDevice(row) }
                }
                
                logger.debug("✅ Found ${devices.size} devices")
                Result.success(devices)
            } catch (e: Exception) {
                logger.error("❌ Failed to search devices", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get device statistics
     */
    suspend fun getDeviceStatistics(
        userId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<DeviceStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating device statistics")
                
                val stats = transaction {
                    var baseQuery = DevicesTable.select { DevicesTable.deletedAt.isNull() }
                    
                    userId?.let { uid ->
                        baseQuery = baseQuery.andWhere { DevicesTable.userId eq uid }
                    }
                    
                    startDate?.let { start ->
                        baseQuery = baseQuery.andWhere { DevicesTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        baseQuery = baseQuery.andWhere { DevicesTable.createdAt lessEq end }
                    }
                    
                    val totalDevices = baseQuery.count()
                    val activeDevices = baseQuery.copy().andWhere { DevicesTable.isActive eq true }.count()
                    val trustedDevices = baseQuery.copy().andWhere { DevicesTable.isTrusted eq true }.count()
                    val blockedDevices = baseQuery.copy().andWhere { DevicesTable.isBlocked eq true }.count()
                    
                    DeviceStatistics(
                        totalDevices = totalDevices,
                        activeDevices = activeDevices,
                        trustedDevices = trustedDevices,
                        blockedDevices = blockedDevices,
                        trustRate = if (totalDevices > 0) (trustedDevices.toDouble() / totalDevices * 100) else 0.0,
                        blockRate = if (totalDevices > 0) (blockedDevices.toDouble() / totalDevices * 100) else 0.0,
                        generatedAt = Instant.now()
                    )
                }
                
                logger.debug("✅ Generated device statistics: ${stats.totalDevices} total devices")
                Result.success(stats)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate device statistics", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clean up inactive devices
     */
    suspend fun cleanupInactiveDevices(
        inactiveDays: Int = 365,
        batchSize: Int = 1000
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🧹 Cleaning up inactive devices (${inactiveDays} days)")
                
                val cleanedCount = transaction {
                    val threshold = Instant.now().minusSeconds(inactiveDays * 24 * 60 * 60L)
                    
                    DevicesTable.update({ 
                        (DevicesTable.lastSeenAt less threshold) and 
                        (DevicesTable.isActive eq true) and 
                        (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[isActive] = false
                        it[deactivatedAt] = Instant.now()
                        it[deactivationReason] = "INACTIVE_CLEANUP"
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = "SYSTEM_CLEANUP"
                    }
                }
                
                logger.info("✅ Deactivated $cleanedCount inactive devices")
                Result.success(cleanedCount)
            } catch (e: Exception) {
                logger.error("❌ Failed to clean up inactive devices", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete device
     */
    suspend fun deleteDevice(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting device: $id")
                
                val deleted = transaction {
                    DevicesTable.update({ 
                        (DevicesTable.id eq UUID.fromString(id)) and (DevicesTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[DevicesTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Device soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Device not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete device: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to Device domain object
     */
    private fun mapRowToDevice(row: ResultRow): Device {
        return Device(
            id = row[DevicesTable.id].value.toString(),
            userId = row[DevicesTable.userId],
            deviceFingerprint = row[DevicesTable.deviceFingerprint],
            deviceName = row[DevicesTable.deviceName],
            deviceType = row[DevicesTable.deviceType],
            platform = row[DevicesTable.platform],
            osName = row[DevicesTable.osName],
            osVersion = row[DevicesTable.osVersion],
            browserName = row[DevicesTable.browserName],
            browserVersion = row[DevicesTable.browserVersion],
            isActive = row[DevicesTable.isActive],
            isTrusted = row[DevicesTable.isTrusted],
            isRegistered = row[DevicesTable.isRegistered],
            isPrimary = row[DevicesTable.isPrimary],
            isBlocked = row[DevicesTable.isBlocked],
            ipAddress = row[DevicesTable.ipAddress],
            userAgent = row[DevicesTable.userAgent],
            screenResolution = row[DevicesTable.screenResolution],
            timezone = row[DevicesTable.timezone],
            language = row[DevicesTable.language],
            location = row[DevicesTable.location],
            hardwareInfo = row[DevicesTable.hardwareInfo],
            networkInfo = row[DevicesTable.networkInfo],
            securityLevel = row[DevicesTable.securityLevel],
            trustScore = row[DevicesTable.trustScore],
            riskScore = row[DevicesTable.riskScore],
            fraudFlags = row[DevicesTable.fraudFlags],
            securityFlags = row[DevicesTable.securityFlags],
            deviceMetadata = row[DevicesTable.deviceMetadata],
            registrationMethod = row[DevicesTable.registrationMethod],
            firstSeenAt = row[DevicesTable.firstSeenAt],
            lastSeenAt = row[DevicesTable.lastSeenAt],
            loginCount = row[DevicesTable.loginCount],
            lastSecurityCheck = row[DevicesTable.lastSecurityCheck],
            trustedAt = row[DevicesTable.trustedAt],
            trustedBy = row[DevicesTable.trustedBy],
            unTrustedAt = row[DevicesTable.unTrustedAt],
            unTrustedBy = row[DevicesTable.unTrustedBy],
            trustChangeReason = row[DevicesTable.trustChangeReason],
            blockedAt = row[DevicesTable.blockedAt],
            blockedBy = row[DevicesTable.blockedBy],
            blockReason = row[DevicesTable.blockReason],
            unblockedAt = row[DevicesTable.unblockedAt],
            unblockedBy = row[DevicesTable.unblockedBy],
            unblockReason = row[DevicesTable.unblockReason],
            deactivatedAt = row[DevicesTable.deactivatedAt],
            deactivationReason = row[DevicesTable.deactivationReason],
            createdAt = row[DevicesTable.createdAt],
            updatedAt = row[DevicesTable.updatedAt],
            createdBy = row[DevicesTable.createdBy],
            updatedBy = row[DevicesTable.updatedBy],
            version = row[DevicesTable.version]
        )
    }
}
