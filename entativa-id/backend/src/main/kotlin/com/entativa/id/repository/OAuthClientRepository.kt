package com.entativa.id.repository

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
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
 * OAuth Client Repository for Entativa ID
 * Handles OAuth2 client registration and management
 * 
 * @author Neo Qiss
 * @status Production-ready OAuth client management
 */
@Repository
class OAuthClientRepository(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(OAuthClientRepository::class.java)
    
    companion object {
        private const val CLIENT_CACHE_TTL_SECONDS = 3600 // 1 hour
        private const val AUTHORIZATION_CODE_CACHE_TTL_SECONDS = 600 // 10 minutes
        private const val TOKEN_CACHE_TTL_SECONDS = 900 // 15 minutes
    }
    
    /**
     * Register new OAuth client
     */
    suspend fun registerClient(request: ClientRegistrationRequest): Result<OAuthClientRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📝 Registering OAuth client: ${request.name}")
                
                val clientId = UUID.randomUUID()
                val now = Instant.now()
                
                val clientRecord = transaction {
                    OAuthClientTable.insert {
                        it[id] = clientId
                        it[clientId] = request.clientId
                        it[clientSecret] = request.clientSecret
                        it[name] = request.name
                        it[description] = request.description
                        it[logoUrl] = request.logoUrl
                        it[websiteUrl] = request.websiteUrl
                        it[privacyPolicyUrl] = request.privacyPolicyUrl
                        it[termsOfServiceUrl] = request.termsOfServiceUrl
                        it[clientType] = request.clientType
                        it[ownerId] = UUID.fromString(request.ownerId)
                        it[isActive] = true
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    // Insert redirect URIs
                    request.redirectUris.forEach { uri ->
                        OAuthRedirectUriTable.insert {
                            it[this.clientId] = clientId
                            it[redirectUri] = uri
                            it[createdAt] = now
                        }
                    }
                    
                    // Insert scopes
                    request.scopes.forEach { scope ->
                        OAuthClientScopeTable.insert {
                            it[this.clientId] = clientId
                            it[scope] = scope
                            it[createdAt] = now
                        }
                    }
                    
                    // Insert grant types
                    request.grantTypes.forEach { grantType ->
                        OAuthClientGrantTypeTable.insert {
                            it[this.clientId] = clientId
                            it[grantType] = grantType
                            it[createdAt] = now
                        }
                    }
                    
                    OAuthClientRecord(
                        id = clientId.toString(),
                        clientId = request.clientId,
                        clientSecret = request.clientSecret,
                        name = request.name,
                        description = request.description,
                        logoUrl = request.logoUrl,
                        websiteUrl = request.websiteUrl,
                        privacyPolicyUrl = request.privacyPolicyUrl,
                        termsOfServiceUrl = request.termsOfServiceUrl,
                        clientType = request.clientType,
                        ownerId = request.ownerId,
                        redirectUris = request.redirectUris,
                        scopes = request.scopes,
                        grantTypes = request.grantTypes,
                        isActive = true,
                        createdAt = now.toString(),
                        updatedAt = now.toString()
                    )
                }
                
                // Cache client
                cacheManager.cacheData("oauth_client:${request.clientId}", clientRecord, CLIENT_CACHE_TTL_SECONDS)
                
                logger.info("✅ OAuth client registered: ${request.clientId}")
                Result.success(clientRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to register OAuth client: ${request.name}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find client by client ID
     */
    suspend fun findByClientId(clientId: String): OAuthClientRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<OAuthClientRecord>("oauth_client:$clientId")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val client = transaction {
                    val clientRow = OAuthClientTable.select { OAuthClientTable.clientId eq clientId and OAuthClientTable.isActive }
                        .singleOrNull()
                        ?: return@transaction null
                    
                    val clientUuid = clientRow[OAuthClientTable.id]
                    
                    // Get redirect URIs
                    val redirectUris = OAuthRedirectUriTable.select { OAuthRedirectUriTable.clientId eq clientUuid }
                        .map { it[OAuthRedirectUriTable.redirectUri] }
                    
                    // Get scopes
                    val scopes = OAuthClientScopeTable.select { OAuthClientScopeTable.clientId eq clientUuid }
                        .map { it[OAuthClientScopeTable.scope] }
                    
                    // Get grant types
                    val grantTypes = OAuthClientGrantTypeTable.select { OAuthClientGrantTypeTable.clientId eq clientUuid }
                        .map { it[OAuthClientGrantTypeTable.grantType] }
                    
                    OAuthClientRecord(
                        id = clientRow[OAuthClientTable.id].toString(),
                        clientId = clientRow[OAuthClientTable.clientId],
                        clientSecret = clientRow[OAuthClientTable.clientSecret],
                        name = clientRow[OAuthClientTable.name],
                        description = clientRow[OAuthClientTable.description],
                        logoUrl = clientRow[OAuthClientTable.logoUrl],
                        websiteUrl = clientRow[OAuthClientTable.websiteUrl],
                        privacyPolicyUrl = clientRow[OAuthClientTable.privacyPolicyUrl],
                        termsOfServiceUrl = clientRow[OAuthClientTable.termsOfServiceUrl],
                        clientType = clientRow[OAuthClientTable.clientType],
                        ownerId = clientRow[OAuthClientTable.ownerId].toString(),
                        redirectUris = redirectUris,
                        scopes = scopes,
                        grantTypes = grantTypes,
                        isActive = clientRow[OAuthClientTable.isActive],
                        createdAt = clientRow[OAuthClientTable.createdAt].toString(),
                        updatedAt = clientRow[OAuthClientTable.updatedAt].toString()
                    )
                }
                
                // Cache if found
                client?.let {
                    cacheManager.cacheData("oauth_client:$clientId", it, CLIENT_CACHE_TTL_SECONDS)
                }
                
                client
                
            } catch (e: Exception) {
                logger.error("❌ Failed to find OAuth client: $clientId", e)
                null
            }
        }
    }
    
    /**
     * Store authorization code
     */
    suspend fun storeAuthorizationCode(request: StoreAuthorizationCodeRequest): Result<AuthorizationCodeRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Storing authorization code for client: ${request.clientId}")
                
                val codeId = UUID.randomUUID()
                val now = Instant.now()
                val expiresAt = now.plusSeconds(600) // 10 minutes
                
                val codeRecord = transaction {
                    OAuthAuthorizationCodeTable.insert {
                        it[id] = codeId
                        it[code] = request.code
                        it[clientId] = request.clientId
                        it[userId] = UUID.fromString(request.userId)
                        it[redirectUri] = request.redirectUri
                        it[scopes] = request.scopes.joinToString(" ")
                        it[codeChallenge] = request.codeChallenge
                        it[codeChallengeMethod] = request.codeChallengeMethod
                        it[state] = request.state
                        it[nonce] = request.nonce
                        it[createdAt] = now
                        it[this.expiresAt] = expiresAt
                        it[isUsed] = false
                    }
                    
                    AuthorizationCodeRecord(
                        id = codeId.toString(),
                        code = request.code,
                        clientId = request.clientId,
                        userId = request.userId,
                        redirectUri = request.redirectUri,
                        scopes = request.scopes,
                        codeChallenge = request.codeChallenge,
                        codeChallengeMethod = request.codeChallengeMethod,
                        state = request.state,
                        nonce = request.nonce,
                        createdAt = now.toString(),
                        expiresAt = expiresAt.toString(),
                        isUsed = false
                    )
                }
                
                // Cache authorization code
                cacheManager.cacheData("auth_code:${request.code}", codeRecord, AUTHORIZATION_CODE_CACHE_TTL_SECONDS)
                
                logger.info("✅ Authorization code stored: ${request.clientId}")
                Result.success(codeRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to store authorization code: ${request.clientId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find and consume authorization code
     */
    suspend fun consumeAuthorizationCode(code: String, clientId: String): Result<AuthorizationCodeRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔓 Consuming authorization code for client: $clientId")
                
                // Check cache first
                val cached = cacheManager.getCachedData<AuthorizationCodeRecord>("auth_code:$code")
                if (cached != null && !cached.isUsed) {
                    // Mark as used in database
                    val now = Instant.now()
                    transaction {
                        OAuthAuthorizationCodeTable.update({ 
                            OAuthAuthorizationCodeTable.code eq code and
                            OAuthAuthorizationCodeTable.clientId eq clientId
                        }) {
                            it[isUsed] = true
                            it[usedAt] = now
                        }
                    }
                    
                    // Remove from cache
                    cacheManager.invalidateCache("auth_code:$code")
                    
                    return@withContext Result.success(cached.copy(isUsed = true))
                }
                
                // Query database
                val codeRecord = transaction {
                    val row = OAuthAuthorizationCodeTable.select { 
                        OAuthAuthorizationCodeTable.code eq code and
                        OAuthAuthorizationCodeTable.clientId eq clientId and
                        OAuthAuthorizationCodeTable.isUsed eq false and
                        OAuthAuthorizationCodeTable.expiresAt greater Instant.now()
                    }.singleOrNull()
                        ?: return@transaction null
                    
                    // Mark as used
                    OAuthAuthorizationCodeTable.update({ 
                        OAuthAuthorizationCodeTable.code eq code 
                    }) {
                        it[isUsed] = true
                        it[usedAt] = Instant.now()
                    }
                    
                    AuthorizationCodeRecord(
                        id = row[OAuthAuthorizationCodeTable.id].toString(),
                        code = row[OAuthAuthorizationCodeTable.code],
                        clientId = row[OAuthAuthorizationCodeTable.clientId],
                        userId = row[OAuthAuthorizationCodeTable.userId].toString(),
                        redirectUri = row[OAuthAuthorizationCodeTable.redirectUri],
                        scopes = row[OAuthAuthorizationCodeTable.scopes].split(" "),
                        codeChallenge = row[OAuthAuthorizationCodeTable.codeChallenge],
                        codeChallengeMethod = row[OAuthAuthorizationCodeTable.codeChallengeMethod],
                        state = row[OAuthAuthorizationCodeTable.state],
                        nonce = row[OAuthAuthorizationCodeTable.nonce],
                        createdAt = row[OAuthAuthorizationCodeTable.createdAt].toString(),
                        expiresAt = row[OAuthAuthorizationCodeTable.expiresAt].toString(),
                        isUsed = true
                    )
                }
                
                if (codeRecord != null) {
                    // Remove from cache
                    cacheManager.invalidateCache("auth_code:$code")
                    
                    logger.info("✅ Authorization code consumed: $clientId")
                    Result.success(codeRecord)
                } else {
                    Result.failure(SecurityException("Invalid or expired authorization code"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to consume authorization code: $clientId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Store access token
     */
    suspend fun storeAccessToken(request: StoreAccessTokenRequest): Result<AccessTokenRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔑 Storing access token for client: ${request.clientId}")
                
                val tokenId = UUID.randomUUID()
                val now = Instant.now()
                val expiresAt = now.plusSeconds(request.expiresInSeconds)
                
                val tokenRecord = transaction {
                    OAuthAccessTokenTable.insert {
                        it[id] = tokenId
                        it[accessToken] = request.accessToken
                        it[tokenType] = request.tokenType
                        it[clientId] = request.clientId
                        it[userId] = UUID.fromString(request.userId)
                        it[scopes] = request.scopes.joinToString(" ")
                        it[createdAt] = now
                        it[this.expiresAt] = expiresAt
                        it[isRevoked] = false
                    }
                    
                    AccessTokenRecord(
                        id = tokenId.toString(),
                        accessToken = request.accessToken,
                        tokenType = request.tokenType,
                        clientId = request.clientId,
                        userId = request.userId,
                        scopes = request.scopes,
                        createdAt = now.toString(),
                        expiresAt = expiresAt.toString(),
                        isRevoked = false
                    )
                }
                
                // Cache token
                cacheManager.cacheData("access_token:${request.accessToken}", tokenRecord, TOKEN_CACHE_TTL_SECONDS)
                
                logger.info("✅ Access token stored: ${request.clientId}")
                Result.success(tokenRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to store access token: ${request.clientId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Store refresh token
     */
    suspend fun storeRefreshToken(request: StoreRefreshTokenRequest): Result<RefreshTokenRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔄 Storing refresh token for client: ${request.clientId}")
                
                val tokenId = UUID.randomUUID()
                val now = Instant.now()
                val expiresAt = now.plusSeconds(request.expiresInSeconds)
                
                val tokenRecord = transaction {
                    OAuthRefreshTokenTable.insert {
                        it[id] = tokenId
                        it[refreshToken] = request.refreshToken
                        it[clientId] = request.clientId
                        it[userId] = UUID.fromString(request.userId)
                        it[scopes] = request.scopes.joinToString(" ")
                        it[accessTokenId] = UUID.fromString(request.accessTokenId)
                        it[createdAt] = now
                        it[this.expiresAt] = expiresAt
                        it[isRevoked] = false
                    }
                    
                    RefreshTokenRecord(
                        id = tokenId.toString(),
                        refreshToken = request.refreshToken,
                        clientId = request.clientId,
                        userId = request.userId,
                        scopes = request.scopes,
                        accessTokenId = request.accessTokenId,
                        createdAt = now.toString(),
                        expiresAt = expiresAt.toString(),
                        isRevoked = false
                    )
                }
                
                // Cache token with longer TTL for refresh tokens
                cacheManager.cacheData("refresh_token:${request.refreshToken}", tokenRecord, 86400) // 24 hours
                
                logger.info("✅ Refresh token stored: ${request.clientId}")
                Result.success(tokenRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to store refresh token: ${request.clientId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find access token
     */
    suspend fun findAccessToken(accessToken: String): AccessTokenRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<AccessTokenRecord>("access_token:$accessToken")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val token = transaction {
                    OAuthAccessTokenTable.select { 
                        OAuthAccessTokenTable.accessToken eq accessToken and
                        OAuthAccessTokenTable.isRevoked eq false and
                        OAuthAccessTokenTable.expiresAt greater Instant.now()
                    }.map { row ->
                        AccessTokenRecord(
                            id = row[OAuthAccessTokenTable.id].toString(),
                            accessToken = row[OAuthAccessTokenTable.accessToken],
                            tokenType = row[OAuthAccessTokenTable.tokenType],
                            clientId = row[OAuthAccessTokenTable.clientId],
                            userId = row[OAuthAccessTokenTable.userId].toString(),
                            scopes = row[OAuthAccessTokenTable.scopes].split(" "),
                            createdAt = row[OAuthAccessTokenTable.createdAt].toString(),
                            expiresAt = row[OAuthAccessTokenTable.expiresAt].toString(),
                            isRevoked = row[OAuthAccessTokenTable.isRevoked]
                        )
                    }.singleOrNull()
                }
                
                // Cache if found
                token?.let {
                    cacheManager.cacheData("access_token:$accessToken", it, TOKEN_CACHE_TTL_SECONDS)
                }
                
                token
                
            } catch (e: Exception) {
                logger.error("❌ Failed to find access token", e)
                null
            }
        }
    }
    
    /**
     * Revoke token
     */
    suspend fun revokeToken(token: String, tokenType: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚫 Revoking $tokenType token")
                
                val now = Instant.now()
                
                val revoked = when (tokenType) {
                    "access_token" -> transaction {
                        OAuthAccessTokenTable.update({ OAuthAccessTokenTable.accessToken eq token }) {
                            it[isRevoked] = true
                            it[revokedAt] = now
                        }
                    }
                    "refresh_token" -> transaction {
                        OAuthRefreshTokenTable.update({ OAuthRefreshTokenTable.refreshToken eq token }) {
                            it[isRevoked] = true
                            it[revokedAt] = now
                        }
                    }
                    else -> 0
                }
                
                if (revoked > 0) {
                    // Remove from cache
                    cacheManager.invalidateCache("${tokenType}:$token")
                    
                    logger.info("✅ Token revoked successfully")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("Token not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke token", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cleanup expired codes and tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🧹 Cleaning up expired OAuth tokens")
                
                val now = Instant.now()
                
                val cleaned = transaction {
                    // Cleanup authorization codes
                    val codes = OAuthAuthorizationCodeTable.deleteWhere { 
                        expiresAt less now or isUsed eq true
                    }
                    
                    // Cleanup access tokens
                    val accessTokens = OAuthAccessTokenTable.update({ 
                        OAuthAccessTokenTable.expiresAt less now and 
                        OAuthAccessTokenTable.isRevoked eq false 
                    }) {
                        it[isRevoked] = true
                        it[revokedAt] = now
                    }
                    
                    // Cleanup refresh tokens
                    val refreshTokens = OAuthRefreshTokenTable.update({ 
                        OAuthRefreshTokenTable.expiresAt less now and 
                        OAuthRefreshTokenTable.isRevoked eq false 
                    }) {
                        it[isRevoked] = true
                        it[revokedAt] = now
                    }
                    
                    codes + accessTokens + refreshTokens
                }
                
                logger.info("✅ Cleaned up $cleaned expired tokens")
                Result.success(cleaned)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to cleanup expired tokens", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get client statistics
     */
    suspend fun getClientStatistics(clientId: String, days: Int = 30): Result<ClientStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                val startDate = Instant.now().minusSeconds(days * 24 * 3600L)
                
                val stats = transaction {
                    val totalTokens = OAuthAccessTokenTable.select { 
                        OAuthAccessTokenTable.clientId eq clientId and
                        OAuthAccessTokenTable.createdAt greater startDate
                    }.count()
                    
                    val activeTokens = OAuthAccessTokenTable.select { 
                        OAuthAccessTokenTable.clientId eq clientId and
                        OAuthAccessTokenTable.isRevoked eq false and
                        OAuthAccessTokenTable.expiresAt greater Instant.now()
                    }.count()
                    
                    val uniqueUsers = OAuthAccessTokenTable.slice(OAuthAccessTokenTable.userId)
                        .select { 
                            OAuthAccessTokenTable.clientId eq clientId and
                            OAuthAccessTokenTable.createdAt greater startDate
                        }
                        .withDistinct()
                        .count()
                    
                    ClientStatistics(
                        totalTokensIssued = totalTokens,
                        activeTokens = activeTokens,
                        uniqueUsers = uniqueUsers,
                        averageDailyTokens = totalTokens / days,
                        periodDays = days
                    )
                }
                
                Result.success(stats)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get client statistics: $clientId", e)
                Result.failure(e)
            }
        }
    }
}
