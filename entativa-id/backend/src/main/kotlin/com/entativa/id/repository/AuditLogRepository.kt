package com.entativa.id.repository

import com.entativa.id.database.tables.AuditLogsTable
import com.entativa.id.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Audit Log Repository for Entativa ID
 * Handles all database operations for enterprise-grade audit logging and compliance
 * 
 * @author Neo Qiss
 * @status Production-ready audit system with compliance features
 */
@Repository
class AuditLogRepository {
    
    private val logger = LoggerFactory.getLogger(AuditLogRepository::class.java)
    
    /**
     * Create a new audit log entry
     */
    suspend fun createAuditLog(auditLog: CreateAuditLogRequest): Result<AuditLog> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating audit log: ${auditLog.action} by ${auditLog.userId}")
                
                val auditLogId = transaction {
                    AuditLogsTable.insertAndGetId {
                        it[userId] = auditLog.userId
                        it[sessionId] = auditLog.sessionId
                        it[action] = auditLog.action
                        it[resource] = auditLog.resource
                        it[resourceId] = auditLog.resourceId
                        it[AuditLogsTable.status] = auditLog.status
                        it[ipAddress] = auditLog.ipAddress
                        it[userAgent] = auditLog.userAgent
                        it[platform] = auditLog.platform
                        it[location] = auditLog.location
                        it[deviceId] = auditLog.deviceId
                        it[deviceFingerprint] = auditLog.deviceFingerprint
                        it[requestId] = auditLog.requestId
                        it[traceId] = auditLog.traceId
                        it[method] = auditLog.method
                        it[endpoint] = auditLog.endpoint
                        it[queryParams] = auditLog.queryParams
                        it[requestBody] = auditLog.requestBody
                        it[responseStatus] = auditLog.responseStatus
                        it[responseBody] = auditLog.responseBody
                        it[duration] = auditLog.duration
                        it[oldValues] = auditLog.oldValues
                        it[newValues] = auditLog.newValues
                        it[changes] = auditLog.changes
                        it[reason] = auditLog.reason
                        it[severity] = auditLog.severity
                        it[riskLevel] = auditLog.riskLevel
                        it[complianceFlags] = auditLog.complianceFlags
                        it[tags] = auditLog.tags
                        it[metadata] = auditLog.metadata
                        it[context] = auditLog.context
                        it[correlationId] = auditLog.correlationId
                        it[parentEventId] = auditLog.parentEventId
                        it[eventSource] = auditLog.eventSource
                        it[eventVersion] = auditLog.eventVersion
                    }
                }
                
                val createdAuditLog = findById(auditLogId.value.toString())
                if (createdAuditLog.isSuccess) {
                    logger.debug("✅ Audit log created: ${auditLog.action}")
                    createdAuditLog
                } else {
                    logger.error("❌ Failed to retrieve created audit log")
                    Result.failure(Exception("Failed to retrieve created audit log"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create audit log: ${auditLog.action}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find audit log by ID
     */
    suspend fun findById(id: String): Result<AuditLog> {
        return withContext(Dispatchers.IO) {
            try {
                val auditLog = transaction {
                    AuditLogsTable.select { 
                        AuditLogsTable.id eq UUID.fromString(id) 
                    }.singleOrNull()?.let { row ->
                        mapRowToAuditLog(row)
                    }
                }
                
                if (auditLog != null) {
                    Result.success(auditLog)
                } else {
                    Result.failure(NoSuchElementException("Audit log not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find audit log by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find audit logs by user ID
     */
    suspend fun findByUserId(
        userId: String,
        limit: Int = 100,
        offset: Int = 0,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<List<AuditLog>> {
        return withContext(Dispatchers.IO) {
            try {
                val auditLogs = transaction {
                    var query = AuditLogsTable.select { AuditLogsTable.userId eq userId }
                    
                    startDate?.let { start ->
                        query = query.andWhere { AuditLogsTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        query = query.andWhere { AuditLogsTable.createdAt lessEq end }
                    }
                    
                    query
                        .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
                        .limit(limit, offset.toLong())
                        .map { row -> mapRowToAuditLog(row) }
                }
                
                Result.success(auditLogs)
            } catch (e: Exception) {
                logger.error("❌ Failed to find audit logs for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find audit logs by session ID
     */
    suspend fun findBySessionId(sessionId: String): Result<List<AuditLog>> {
        return withContext(Dispatchers.IO) {
            try {
                val auditLogs = transaction {
                    AuditLogsTable.select { 
                        AuditLogsTable.sessionId eq sessionId 
                    }.orderBy(AuditLogsTable.createdAt to SortOrder.ASC)
                    .map { row -> mapRowToAuditLog(row) }
                }
                
                Result.success(auditLogs)
            } catch (e: Exception) {
                logger.error("❌ Failed to find audit logs for session: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search audit logs with comprehensive filters
     */
    suspend fun searchAuditLogs(
        userId: String? = null,
        action: String? = null,
        resource: String? = null,
        status: String? = null,
        severity: String? = null,
        riskLevel: String? = null,
        ipAddress: String? = null,
        platform: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        tags: List<String>? = null,
        complianceFlags: List<String>? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<AuditLog>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Searching audit logs with filters")
                
                val auditLogs = transaction {
                    var query = AuditLogsTable.selectAll()
                    
                    userId?.let { uid ->
                        query = query.andWhere { AuditLogsTable.userId eq uid }
                    }
                    
                    action?.let { act ->
                        query = query.andWhere { AuditLogsTable.action eq act }
                    }
                    
                    resource?.let { res ->
                        query = query.andWhere { AuditLogsTable.resource eq res }
                    }
                    
                    status?.let { stat ->
                        query = query.andWhere { AuditLogsTable.status eq stat }
                    }
                    
                    severity?.let { sev ->
                        query = query.andWhere { AuditLogsTable.severity eq sev }
                    }
                    
                    riskLevel?.let { risk ->
                        query = query.andWhere { AuditLogsTable.riskLevel eq risk }
                    }
                    
                    ipAddress?.let { ip ->
                        query = query.andWhere { AuditLogsTable.ipAddress eq ip }
                    }
                    
                    platform?.let { plat ->
                        query = query.andWhere { AuditLogsTable.platform eq plat }
                    }
                    
                    startDate?.let { start ->
                        query = query.andWhere { AuditLogsTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        query = query.andWhere { AuditLogsTable.createdAt lessEq end }
                    }
                    
                    tags?.forEach { tag ->
                        query = query.andWhere { AuditLogsTable.tags like "%$tag%" }
                    }
                    
                    complianceFlags?.forEach { flag ->
                        query = query.andWhere { AuditLogsTable.complianceFlags like "%$flag%" }
                    }
                    
                    query
                        .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
                        .limit(limit, offset.toLong())
                        .map { row -> mapRowToAuditLog(row) }
                }
                
                logger.debug("✅ Found ${auditLogs.size} audit logs")
                Result.success(auditLogs)
            } catch (e: Exception) {
                logger.error("❌ Failed to search audit logs", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find security events (high-risk audit logs)
     */
    suspend fun findSecurityEvents(
        startDate: Instant? = null,
        endDate: Instant? = null,
        minRiskLevel: String = "MEDIUM",
        limit: Int = 100
    ): Result<List<AuditLog>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🚨 Finding security events with risk level >= $minRiskLevel")
                
                val securityEvents = transaction {
                    var query = AuditLogsTable.select { 
                        AuditLogsTable.riskLevel inList listOf("MEDIUM", "HIGH", "CRITICAL") 
                    }
                    
                    startDate?.let { start ->
                        query = query.andWhere { AuditLogsTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        query = query.andWhere { AuditLogsTable.createdAt lessEq end }
                    }
                    
                    query
                        .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
                        .limit(limit)
                        .map { row -> mapRowToAuditLog(row) }
                }
                
                logger.debug("✅ Found ${securityEvents.size} security events")
                Result.success(securityEvents)
            } catch (e: Exception) {
                logger.error("❌ Failed to find security events", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get audit log statistics
     */
    suspend fun getAuditStatistics(
        startDate: Instant? = null,
        endDate: Instant? = null,
        userId: String? = null
    ): Result<AuditStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating audit statistics")
                
                val stats = transaction {
                    var baseQuery = AuditLogsTable.selectAll()
                    
                    startDate?.let { start ->
                        baseQuery = baseQuery.andWhere { AuditLogsTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        baseQuery = baseQuery.andWhere { AuditLogsTable.createdAt lessEq end }
                    }
                    
                    userId?.let { uid ->
                        baseQuery = baseQuery.andWhere { AuditLogsTable.userId eq uid }
                    }
                    
                    val totalEvents = baseQuery.count()
                    
                    val successEvents = AuditLogsTable.select { 
                        (AuditLogsTable.status eq "SUCCESS") and
                        if (startDate != null) AuditLogsTable.createdAt greaterEq startDate else Op.TRUE and
                        if (endDate != null) AuditLogsTable.createdAt lessEq endDate else Op.TRUE and
                        if (userId != null) AuditLogsTable.userId eq userId else Op.TRUE
                    }.count()
                    
                    val failureEvents = AuditLogsTable.select { 
                        (AuditLogsTable.status eq "FAILURE") and
                        if (startDate != null) AuditLogsTable.createdAt greaterEq startDate else Op.TRUE and
                        if (endDate != null) AuditLogsTable.createdAt lessEq endDate else Op.TRUE and
                        if (userId != null) AuditLogsTable.userId eq userId else Op.TRUE
                    }.count()
                    
                    val securityEvents = AuditLogsTable.select { 
                        (AuditLogsTable.riskLevel inList listOf("HIGH", "CRITICAL")) and
                        if (startDate != null) AuditLogsTable.createdAt greaterEq startDate else Op.TRUE and
                        if (endDate != null) AuditLogsTable.createdAt lessEq endDate else Op.TRUE and
                        if (userId != null) AuditLogsTable.userId eq userId else Op.TRUE
                    }.count()
                    
                    val uniqueUsers = AuditLogsTable.slice(AuditLogsTable.userId)
                        .select { 
                            if (startDate != null) AuditLogsTable.createdAt greaterEq startDate else Op.TRUE and
                            if (endDate != null) AuditLogsTable.createdAt lessEq endDate else Op.TRUE and
                            if (userId != null) AuditLogsTable.userId eq userId else Op.TRUE
                        }
                        .withDistinct()
                        .count()
                    
                    AuditStatistics(
                        totalEvents = totalEvents,
                        successEvents = successEvents,
                        failureEvents = failureEvents,
                        securityEvents = securityEvents,
                        uniqueUsers = uniqueUsers,
                        successRate = if (totalEvents > 0) (successEvents.toDouble() / totalEvents * 100) else 0.0,
                        failureRate = if (totalEvents > 0) (failureEvents.toDouble() / totalEvents * 100) else 0.0,
                        securityEventRate = if (totalEvents > 0) (securityEvents.toDouble() / totalEvents * 100) else 0.0,
                        generatedAt = Instant.now()
                    )
                }
                
                logger.debug("✅ Generated audit statistics: ${stats.totalEvents} total events")
                Result.success(stats)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate audit statistics", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get compliance report
     */
    suspend fun getComplianceReport(
        startDate: Instant,
        endDate: Instant,
        complianceStandard: String? = null
    ): Result<ComplianceReport> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📋 Generating compliance report for period: $startDate to $endDate")
                
                val report = transaction {
                    var query = AuditLogsTable.select { 
                        (AuditLogsTable.createdAt greaterEq startDate) and 
                        (AuditLogsTable.createdAt lessEq endDate) 
                    }
                    
                    complianceStandard?.let { standard ->
                        query = query.andWhere { 
                            AuditLogsTable.complianceFlags like "%$standard%" 
                        }
                    }
                    
                    val auditLogs = query.map { row -> mapRowToAuditLog(row) }
                    
                    val accessEvents = auditLogs.filter { it.action.contains("LOGIN") || it.action.contains("ACCESS") }
                    val dataEvents = auditLogs.filter { it.resource.contains("USER") || it.resource.contains("PROFILE") }
                    val securityEvents = auditLogs.filter { it.riskLevel in listOf("HIGH", "CRITICAL") }
                    val failureEvents = auditLogs.filter { it.status == "FAILURE" }
                    
                    ComplianceReport(
                        periodStart = startDate,
                        periodEnd = endDate,
                        totalEvents = auditLogs.size.toLong(),
                        accessEvents = accessEvents.size.toLong(),
                        dataEvents = dataEvents.size.toLong(),
                        securityEvents = securityEvents.size.toLong(),
                        failureEvents = failureEvents.size.toLong(),
                        complianceStandard = complianceStandard,
                        violations = securityEvents.filter { it.complianceFlags?.contains("VIOLATION") == true }.size.toLong(),
                        generatedAt = Instant.now(),
                        events = auditLogs
                    )
                }
                
                logger.debug("✅ Generated compliance report: ${report.totalEvents} events")
                Result.success(report)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate compliance report", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Archive old audit logs (for data retention compliance)
     */
    suspend fun archiveOldLogs(
        olderThan: Instant,
        batchSize: Int = 1000
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📦 Archiving audit logs older than: $olderThan")
                
                val archivedCount = transaction {
                    AuditLogsTable.update({ 
                        (AuditLogsTable.createdAt less olderThan) and 
                        (AuditLogsTable.archived eq false) 
                    }) {
                        it[archived] = true
                        it[archivedAt] = Instant.now()
                    }.toLong()
                }
                
                logger.info("✅ Archived $archivedCount audit logs")
                Result.success(archivedCount)
            } catch (e: Exception) {
                logger.error("❌ Failed to archive audit logs", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Permanently delete archived logs (for data retention compliance)
     */
    suspend fun deleteArchivedLogs(
        archivedBefore: Instant,
        batchSize: Int = 1000
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Permanently deleting archived logs before: $archivedBefore")
                
                val deletedCount = transaction {
                    AuditLogsTable.deleteWhere { 
                        (archived eq true) and 
                        (archivedAt?.less(archivedBefore) ?: Op.FALSE) 
                    }.toLong()
                }
                
                logger.info("✅ Permanently deleted $deletedCount archived audit logs")
                Result.success(deletedCount)
            } catch (e: Exception) {
                logger.error("❌ Failed to delete archived audit logs", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to AuditLog domain object
     */
    private fun mapRowToAuditLog(row: ResultRow): AuditLog {
        return AuditLog(
            id = row[AuditLogsTable.id].value.toString(),
            userId = row[AuditLogsTable.userId],
            sessionId = row[AuditLogsTable.sessionId],
            action = row[AuditLogsTable.action],
            resource = row[AuditLogsTable.resource],
            resourceId = row[AuditLogsTable.resourceId],
            status = row[AuditLogsTable.status],
            ipAddress = row[AuditLogsTable.ipAddress],
            userAgent = row[AuditLogsTable.userAgent],
            platform = row[AuditLogsTable.platform],
            location = row[AuditLogsTable.location],
            deviceId = row[AuditLogsTable.deviceId],
            deviceFingerprint = row[AuditLogsTable.deviceFingerprint],
            requestId = row[AuditLogsTable.requestId],
            traceId = row[AuditLogsTable.traceId],
            method = row[AuditLogsTable.method],
            endpoint = row[AuditLogsTable.endpoint],
            queryParams = row[AuditLogsTable.queryParams],
            requestBody = row[AuditLogsTable.requestBody],
            responseStatus = row[AuditLogsTable.responseStatus],
            responseBody = row[AuditLogsTable.responseBody],
            duration = row[AuditLogsTable.duration],
            oldValues = row[AuditLogsTable.oldValues],
            newValues = row[AuditLogsTable.newValues],
            changes = row[AuditLogsTable.changes],
            reason = row[AuditLogsTable.reason],
            severity = row[AuditLogsTable.severity],
            riskLevel = row[AuditLogsTable.riskLevel],
            complianceFlags = row[AuditLogsTable.complianceFlags],
            tags = row[AuditLogsTable.tags],
            metadata = row[AuditLogsTable.metadata],
            context = row[AuditLogsTable.context],
            correlationId = row[AuditLogsTable.correlationId],
            parentEventId = row[AuditLogsTable.parentEventId],
            eventSource = row[AuditLogsTable.eventSource],
            eventVersion = row[AuditLogsTable.eventVersion],
            archived = row[AuditLogsTable.archived],
            archivedAt = row[AuditLogsTable.archivedAt],
            createdAt = row[AuditLogsTable.createdAt]
        )
    }
}
