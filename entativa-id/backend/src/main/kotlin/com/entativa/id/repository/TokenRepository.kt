package com.entativa.id.repository

import com.entativa.id.database.tables.TokensTable
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
 * Token Repository for Entativa ID
 * Handles all database operations for OAuth2 tokens, JWT tokens, and API keys
 * 
 * @author Neo Qiss
 * @status Production-ready token management with enterprise security
 */
@Repository
class TokenRepository {
    
    private val logger = LoggerFactory.getLogger(TokenRepository::class.java)
    
    /**
     * Create a new token
     */
    suspend fun createToken(token: CreateTokenRequest): Result<Token> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating token: ${token.tokenType} for user: ${token.userId}")
                
                val tokenId = transaction {
                    TokensTable.insertAndGetId {
                        it[userId] = token.userId
                        it[clientId] = token.clientId
                        it[tokenType] = token.tokenType
                        it[tokenValue] = token.tokenValue
                        it[tokenHash] = token.tokenHash
                        it[refreshToken] = token.refreshToken
                        it[refreshTokenHash] = token.refreshTokenHash
                        it[scope] = token.scope
                        it[audience] = token.audience
                        it[issuer] = token.issuer
                        it[subject] = token.subject
                        it[TokensTable.isActive] = token.isActive
                        it[isRevoked] = token.isRevoked
                        it[isExpired] = token.isExpired
                        it[expiresAt] = token.expiresAt
                        it[refreshExpiresAt] = token.refreshExpiresAt
                        it[lastUsedAt] = token.lastUsedAt
                        it[grantType] = token.grantType
                        it[deviceId] = token.deviceId
                        it[ipAddress] = token.ipAddress
                        it[userAgent] = token.userAgent
                        it[location] = token.location
                        it[sessionId] = token.sessionId
                        it[platform] = token.platform
                        it[securityLevel] = token.securityLevel
                        it[riskScore] = token.riskScore
                        it[claims] = token.claims
                        it[metadata] = token.metadata
                        it[createdBy] = token.createdBy
                    }
                }
                
                val createdToken = findById(tokenId.value.toString())
                if (createdToken.isSuccess) {
                    logger.info("✅ Token created successfully: ${token.tokenType}")
                    createdToken
                } else {
                    logger.error("❌ Failed to retrieve created token")
                    Result.failure(Exception("Failed to retrieve created token"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create token: ${token.tokenType}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find token by ID
     */
    suspend fun findById(id: String): Result<Token> {
        return withContext(Dispatchers.IO) {
            try {
                val token = transaction {
                    TokensTable.select { 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToToken(row)
                    }
                }
                
                if (token != null) {
                    Result.success(token)
                } else {
                    Result.failure(NoSuchElementException("Token not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find token by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find token by token value (hash lookup for security)
     */
    suspend fun findByTokenHash(tokenHash: String): Result<Token> {
        return withContext(Dispatchers.IO) {
            try {
                val token = transaction {
                    TokensTable.select { 
                        (TokensTable.tokenHash eq tokenHash) and 
                        (TokensTable.isActive eq true) and 
                        (TokensTable.isRevoked eq false) and 
                        (TokensTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToToken(row)
                    }
                }
                
                if (token != null) {
                    // Update last used timestamp
                    updateLastUsed(token.id)
                    Result.success(token)
                } else {
                    Result.failure(NoSuchElementException("Token not found or invalid"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find token by hash", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find token by refresh token hash
     */
    suspend fun findByRefreshTokenHash(refreshTokenHash: String): Result<Token> {
        return withContext(Dispatchers.IO) {
            try {
                val token = transaction {
                    TokensTable.select { 
                        (TokensTable.refreshTokenHash eq refreshTokenHash) and 
                        (TokensTable.isActive eq true) and 
                        (TokensTable.isRevoked eq false) and 
                        (TokensTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToToken(row)
                    }
                }
                
                if (token != null) {
                    Result.success(token)
                } else {
                    Result.failure(NoSuchElementException("Refresh token not found or invalid"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find token by refresh token hash", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find tokens by user ID
     */
    suspend fun findByUserId(
        userId: String,
        tokenType: String? = null,
        activeOnly: Boolean = true
    ): Result<List<Token>> {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = transaction {
                    var query = TokensTable.select { 
                        (TokensTable.userId eq userId) and (TokensTable.deletedAt.isNull()) 
                    }
                    
                    if (activeOnly) {
                        query = query.andWhere { 
                            (TokensTable.isActive eq true) and (TokensTable.isRevoked eq false) 
                        }
                    }
                    
                    tokenType?.let { type ->
                        query = query.andWhere { TokensTable.tokenType eq type }
                    }
                    
                    query
                        .orderBy(TokensTable.createdAt to SortOrder.DESC)
                        .map { row -> mapRowToToken(row) }
                }
                
                Result.success(tokens)
            } catch (e: Exception) {
                logger.error("❌ Failed to find tokens for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find tokens by client ID
     */
    suspend fun findByClientId(
        clientId: String,
        activeOnly: Boolean = true
    ): Result<List<Token>> {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = transaction {
                    var query = TokensTable.select { 
                        (TokensTable.clientId eq clientId) and (TokensTable.deletedAt.isNull()) 
                    }
                    
                    if (activeOnly) {
                        query = query.andWhere { 
                            (TokensTable.isActive eq true) and (TokensTable.isRevoked eq false) 
                        }
                    }
                    
                    query
                        .orderBy(TokensTable.createdAt to SortOrder.DESC)
                        .map { row -> mapRowToToken(row) }
                }
                
                Result.success(tokens)
            } catch (e: Exception) {
                logger.error("❌ Failed to find tokens for client: $clientId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update token usage
     */
    suspend fun updateLastUsed(id: String, ipAddress: String? = null, userAgent: String? = null): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    TokensTable.update({ 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[lastUsedAt] = Instant.now()
                        it[usageCount] = usageCount + 1
                        ipAddress?.let { ip -> it[TokensTable.ipAddress] = ip }
                        userAgent?.let { ua -> it[TokensTable.userAgent] = ua }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update token usage: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Revoke token
     */
    suspend fun revokeToken(
        id: String,
        reason: String,
        revokedBy: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Revoking token: $id")
                
                val revoked = transaction {
                    TokensTable.update({ 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[isRevoked] = true
                        it[isActive] = false
                        it[revokedAt] = Instant.now()
                        it[TokensTable.revokedBy] = revokedBy
                        it[revocationReason] = reason
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = revokedBy
                    }
                } > 0
                
                if (revoked) {
                    logger.info("✅ Token revoked: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Token not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke token: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Revoke all tokens for a user
     */
    suspend fun revokeAllUserTokens(
        userId: String,
        reason: String,
        revokedBy: String,
        excludeTokenIds: List<String> = emptyList()
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔒 Revoking all tokens for user: $userId")
                
                val revokedCount = transaction {
                    var query = (TokensTable.userId eq userId) and 
                               (TokensTable.isActive eq true) and 
                               (TokensTable.isRevoked eq false) and 
                               (TokensTable.deletedAt.isNull())
                    
                    if (excludeTokenIds.isNotEmpty()) {
                        val excludeUUIDs = excludeTokenIds.map { UUID.fromString(it) }
                        query = query and (TokensTable.id notInList excludeUUIDs)
                    }
                    
                    TokensTable.update({ query }) {
                        it[isRevoked] = true
                        it[isActive] = false
                        it[revokedAt] = Instant.now()
                        it[TokensTable.revokedBy] = revokedBy
                        it[revocationReason] = reason
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = revokedBy
                    }
                }
                
                logger.info("✅ Revoked $revokedCount tokens for user: $userId")
                Result.success(revokedCount)
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke user tokens: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Mark token as expired
     */
    suspend fun markAsExpired(id: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    TokensTable.update({ 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[isExpired] = true
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to mark token as expired: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clean up expired tokens
     */
    suspend fun cleanupExpiredTokens(batchSize: Int = 1000): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🧹 Cleaning up expired tokens")
                
                val cleanedCount = transaction {
                    val now = Instant.now()
                    
                    // Mark tokens as expired if they've passed their expiration time
                    val expiredCount = TokensTable.update({ 
                        (TokensTable.expiresAt less now) and 
                        (TokensTable.isExpired eq false) and 
                        (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[isExpired] = true
                        it[isActive] = false
                        it[updatedAt] = now
                    }
                    
                    // Soft delete very old expired tokens (older than 90 days)
                    val oldExpiredThreshold = now.minusSeconds(90 * 24 * 60 * 60) // 90 days
                    val deletedCount = TokensTable.update({ 
                        (TokensTable.expiresAt less oldExpiredThreshold) and 
                        (TokensTable.isExpired eq true) and 
                        (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = now
                        it[deletedBy] = "SYSTEM_CLEANUP"
                    }
                    
                    expiredCount + deletedCount
                }
                
                logger.info("✅ Cleaned up $cleanedCount expired tokens")
                Result.success(cleanedCount)
            } catch (e: Exception) {
                logger.error("❌ Failed to clean up expired tokens", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get token statistics
     */
    suspend fun getTokenStatistics(
        userId: String? = null,
        clientId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): Result<TokenStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating token statistics")
                
                val stats = transaction {
                    var baseQuery = TokensTable.select { TokensTable.deletedAt.isNull() }
                    
                    userId?.let { uid ->
                        baseQuery = baseQuery.andWhere { TokensTable.userId eq uid }
                    }
                    
                    clientId?.let { cid ->
                        baseQuery = baseQuery.andWhere { TokensTable.clientId eq cid }
                    }
                    
                    startDate?.let { start ->
                        baseQuery = baseQuery.andWhere { TokensTable.createdAt greaterEq start }
                    }
                    
                    endDate?.let { end ->
                        baseQuery = baseQuery.andWhere { TokensTable.createdAt lessEq end }
                    }
                    
                    val totalTokens = baseQuery.count()
                    val activeTokens = baseQuery.copy().andWhere { 
                        (TokensTable.isActive eq true) and (TokensTable.isRevoked eq false) 
                    }.count()
                    val revokedTokens = baseQuery.copy().andWhere { TokensTable.isRevoked eq true }.count()
                    val expiredTokens = baseQuery.copy().andWhere { TokensTable.isExpired eq true }.count()
                    
                    TokenStatistics(
                        totalTokens = totalTokens,
                        activeTokens = activeTokens,
                        revokedTokens = revokedTokens,
                        expiredTokens = expiredTokens,
                        generatedAt = Instant.now()
                    )
                }
                
                logger.debug("✅ Generated token statistics: ${stats.totalTokens} total tokens")
                Result.success(stats)
            } catch (e: Exception) {
                logger.error("❌ Failed to generate token statistics", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update token security attributes
     */
    suspend fun updateSecurityAttributes(
        id: String,
        securityLevel: String? = null,
        riskScore: Double? = null,
        fraudFlags: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    TokensTable.update({ 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }) {
                        securityLevel?.let { level -> it[TokensTable.securityLevel] = level }
                        riskScore?.let { score -> it[TokensTable.riskScore] = score }
                        fraudFlags?.let { flags -> it[TokensTable.fraudFlags] = flags }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update token security attributes: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete token
     */
    suspend fun deleteToken(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting token: $id")
                
                val deleted = transaction {
                    TokensTable.update({ 
                        (TokensTable.id eq UUID.fromString(id)) and (TokensTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[TokensTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Token soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Token not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete token: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to Token domain object
     */
    private fun mapRowToToken(row: ResultRow): Token {
        return Token(
            id = row[TokensTable.id].value.toString(),
            userId = row[TokensTable.userId],
            clientId = row[TokensTable.clientId],
            tokenType = row[TokensTable.tokenType],
            tokenValue = row[TokensTable.tokenValue],
            tokenHash = row[TokensTable.tokenHash],
            refreshToken = row[TokensTable.refreshToken],
            refreshTokenHash = row[TokensTable.refreshTokenHash],
            scope = row[TokensTable.scope],
            audience = row[TokensTable.audience],
            issuer = row[TokensTable.issuer],
            subject = row[TokensTable.subject],
            isActive = row[TokensTable.isActive],
            isRevoked = row[TokensTable.isRevoked],
            isExpired = row[TokensTable.isExpired],
            expiresAt = row[TokensTable.expiresAt],
            refreshExpiresAt = row[TokensTable.refreshExpiresAt],
            lastUsedAt = row[TokensTable.lastUsedAt],
            usageCount = row[TokensTable.usageCount],
            revokedAt = row[TokensTable.revokedAt],
            revokedBy = row[TokensTable.revokedBy],
            revocationReason = row[TokensTable.revocationReason],
            grantType = row[TokensTable.grantType],
            deviceId = row[TokensTable.deviceId],
            ipAddress = row[TokensTable.ipAddress],
            userAgent = row[TokensTable.userAgent],
            location = row[TokensTable.location],
            sessionId = row[TokensTable.sessionId],
            platform = row[TokensTable.platform],
            securityLevel = row[TokensTable.securityLevel],
            riskScore = row[TokensTable.riskScore],
            fraudFlags = row[TokensTable.fraudFlags],
            claims = row[TokensTable.claims],
            metadata = row[TokensTable.metadata],
            createdAt = row[TokensTable.createdAt],
            updatedAt = row[TokensTable.updatedAt],
            createdBy = row[TokensTable.createdBy],
            updatedBy = row[TokensTable.updatedBy],
            version = row[TokensTable.version]
        )
    }
}
