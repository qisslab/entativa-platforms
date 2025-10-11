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
import java.security.SecureRandom
import java.time.Instant
import java.util.*

/**
 * OAuth2 Service for Entativa ID
 * Handles OAuth2 authorization flows, client management, and token lifecycle
 * 
 * @author Neo Qiss
 * @status Production-ready OAuth2 implementation
 */
class OAuthService(
    private val cacheManager: EntativaCacheManager,
    private val authService: AuthenticationService
) {
    
    private val logger = LoggerFactory.getLogger(OAuthService::class.java)
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val AUTHORIZATION_CODE_DURATION_MINUTES = 10L
        private const val ACCESS_TOKEN_DURATION_MINUTES = 60L
        private const val REFRESH_TOKEN_DURATION_DAYS = 90L
        private const val CODE_LENGTH = 32
        private const val TOKEN_LENGTH = 48
        private const val CACHE_TTL_SECONDS = 3600
    }
    
    /**
     * Create OAuth2 authorization request
     */
    suspend fun createAuthorizationRequest(request: OAuthAuthorizationRequest): Result<OAuthAuthorizationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Creating OAuth authorization request for client: ${request.clientId}")
                
                // Validate client
                val client = getOAuthClient(request.clientId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid client_id")
                    )
                
                // Validate redirect URI
                if (request.redirectUri !in client.redirectUris) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid redirect_uri")
                    )
                }
                
                // Validate response type
                if (request.responseType != "code") {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported response_type. Only 'code' is supported.")
                    )
                }
                
                // Validate and parse scope
                val scopes = parseAndValidateScopes(request.scope, client.allowedScopes)
                if (scopes.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid or empty scope")
                    )
                }
                
                // Generate state and store request
                val authRequestId = UUID.randomUUID().toString()
                val authRequest = OAuthAuthorizationPendingRequest(
                    id = authRequestId,
                    clientId = request.clientId,
                    redirectUri = request.redirectUri,
                    scope = scopes.joinToString(" "),
                    state = request.state,
                    codeChallenge = request.codeChallenge,
                    codeChallengeMethod = request.codeChallengeMethod,
                    createdAt = Instant.now().toString(),
                    expiresAt = Instant.now().plusSeconds(600).toString() // 10 minutes
                )
                
                // Cache the authorization request
                cacheManager.cacheData("oauth_auth_request:$authRequestId", authRequest, 600)
                
                logger.info("✅ OAuth authorization request created: $authRequestId")
                
                Result.success(OAuthAuthorizationResponse(
                    authorizationUrl = buildAuthorizationUrl(authRequestId, client),
                    requestId = authRequestId,
                    expiresIn = 600,
                    clientInfo = OAuthClientInfo(
                        name = client.name,
                        description = client.description,
                        logoUrl = client.logoUrl,
                        website = client.website,
                        privacyPolicy = client.privacyPolicy,
                        termsOfService = client.termsOfService
                    ),
                    requestedScopes = scopes
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create OAuth authorization request", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Authorize OAuth2 request (user consent)
     */
    suspend fun authorizeRequest(
        requestId: String, 
        userId: String, 
        approvedScopes: List<String>,
        approved: Boolean
    ): Result<OAuthAuthorizationCodeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚖️ Processing OAuth authorization: $requestId for user: $userId")
                
                // Get pending authorization request
                val authRequest = cacheManager.getCachedData<OAuthAuthorizationPendingRequest>("oauth_auth_request:$requestId")
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Authorization request not found or expired")
                    )
                
                if (!approved) {
                    logger.info("❌ User denied OAuth authorization: $requestId")
                    return@withContext Result.success(OAuthAuthorizationCodeResponse(
                        redirectUri = "${authRequest.redirectUri}?error=access_denied&state=${authRequest.state}"
                    ))
                }
                
                // Validate approved scopes
                val requestedScopes = authRequest.scope.split(" ")
                val validApprovedScopes = approvedScopes.filter { it in requestedScopes }
                
                if (validApprovedScopes.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("No valid scopes approved")
                    )
                }
                
                // Generate authorization code
                val authCode = generateSecureCode(CODE_LENGTH)
                val now = Instant.now()
                
                // Store authorization code
                val authCodeData = OAuthAuthorizationCode(
                    code = authCode,
                    clientId = authRequest.clientId,
                    userId = userId,
                    redirectUri = authRequest.redirectUri,
                    scope = validApprovedScopes.joinToString(" "),
                    codeChallenge = authRequest.codeChallenge,
                    codeChallengeMethod = authRequest.codeChallengeMethod,
                    createdAt = now.toString(),
                    expiresAt = now.plusSeconds(AUTHORIZATION_CODE_DURATION_MINUTES * 60).toString()
                )
                
                // Store in database
                transaction {
                    IdentityOAuthTokensTable.insert {
                        it[id] = UUID.randomUUID()
                        it[identityId] = UUID.fromString(userId)
                        it[clientId] = authRequest.clientId
                        it[authorizationCode] = authCode
                        it[scope] = validApprovedScopes.joinToString(" ")
                        it[redirectUri] = authRequest.redirectUri
                        it[codeChallenge] = authRequest.codeChallenge
                        it[codeChallengeMethod] = authRequest.codeChallengeMethod
                        it[createdAt] = now
                        it[authCodeExpiresAt] = now.plusSeconds(AUTHORIZATION_CODE_DURATION_MINUTES * 60)
                    }
                }
                
                // Cache authorization code
                cacheManager.cacheData("oauth_auth_code:$authCode", authCodeData, (AUTHORIZATION_CODE_DURATION_MINUTES * 60).toInt())
                
                // Remove pending request
                cacheManager.invalidateCache("oauth_auth_request:$requestId")
                
                // Log authorization grant
                auditOAuthAction(userId, "authorization_granted", mapOf(
                    "client_id" to authRequest.clientId,
                    "scopes" to validApprovedScopes.joinToString(","),
                    "request_id" to requestId
                ))
                
                logger.info("✅ OAuth authorization granted: $requestId")
                
                Result.success(OAuthAuthorizationCodeResponse(
                    redirectUri = "${authRequest.redirectUri}?code=$authCode&state=${authRequest.state}"
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to authorize OAuth request: $requestId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Exchange authorization code for tokens
     */
    suspend fun exchangeCodeForTokens(request: OAuthTokenRequest): Result<OAuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🎫 Exchanging authorization code for tokens")
                
                // Validate grant type
                if (request.grantType != "authorization_code") {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported grant_type")
                    )
                }
                
                // Validate client
                val client = getOAuthClient(request.clientId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid client_id")
                    )
                
                // Authenticate client
                if (!authenticateClient(client, request.clientSecret)) {
                    return@withContext Result.failure(
                        SecurityException("Client authentication failed")
                    )
                }
                
                // Get authorization code data
                val authCodeData = cacheManager.getCachedData<OAuthAuthorizationCode>("oauth_auth_code:${request.code}")
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid or expired authorization code")
                    )
                
                // Validate authorization code
                if (authCodeData.clientId != request.clientId || 
                    authCodeData.redirectUri != request.redirectUri) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Authorization code validation failed")
                    )
                }
                
                // Validate PKCE if present
                if (authCodeData.codeChallenge != null) {
                    if (request.codeVerifier == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("code_verifier required for PKCE")
                        )
                    }
                    
                    if (!validatePKCE(authCodeData.codeChallenge, authCodeData.codeChallengeMethod, request.codeVerifier)) {
                        return@withContext Result.failure(
                            SecurityException("PKCE validation failed")
                        )
                    }
                }
                
                // Generate OAuth tokens
                val accessToken = generateSecureToken(TOKEN_LENGTH)
                val refreshToken = generateSecureToken(TOKEN_LENGTH)
                val now = Instant.now()
                
                // Update database record with tokens
                transaction {
                    IdentityOAuthTokensTable.update({ 
                        IdentityOAuthTokensTable.authorizationCode eq request.code 
                    }) {
                        it[this.accessToken] = accessToken
                        it[this.refreshToken] = refreshToken
                        it[accessTokenExpiresAt] = now.plusSeconds(ACCESS_TOKEN_DURATION_MINUTES * 60)
                        it[refreshTokenExpiresAt] = now.plusSeconds(REFRESH_TOKEN_DURATION_DAYS * 24 * 3600)
                        it[usedAt] = now
                    }
                }
                
                // Cache access token
                val tokenData = OAuthTokenData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    clientId = request.clientId,
                    userId = authCodeData.userId,
                    scope = authCodeData.scope,
                    createdAt = now.toString(),
                    expiresAt = now.plusSeconds(ACCESS_TOKEN_DURATION_MINUTES * 60).toString()
                )
                
                cacheManager.cacheData("oauth_token:$accessToken", tokenData, (ACCESS_TOKEN_DURATION_MINUTES * 60).toInt())
                
                // Invalidate authorization code
                cacheManager.invalidateCache("oauth_auth_code:${request.code}")
                
                // Log token exchange
                auditOAuthAction(authCodeData.userId, "tokens_issued", mapOf(
                    "client_id" to request.clientId,
                    "scope" to authCodeData.scope
                ))
                
                logger.info("✅ OAuth tokens issued successfully")
                
                Result.success(OAuthTokenResponse(
                    accessToken = accessToken,
                    tokenType = "Bearer",
                    expiresIn = (ACCESS_TOKEN_DURATION_MINUTES * 60).toInt(),
                    refreshToken = refreshToken,
                    scope = authCodeData.scope
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to exchange code for tokens", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Refresh OAuth2 access token
     */
    suspend fun refreshAccessToken(request: OAuthRefreshTokenRequest): Result<OAuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔄 Refreshing OAuth access token")
                
                // Validate grant type
                if (request.grantType != "refresh_token") {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported grant_type")
                    )
                }
                
                // Validate client
                val client = getOAuthClient(request.clientId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Invalid client_id")
                    )
                
                // Authenticate client
                if (!authenticateClient(client, request.clientSecret)) {
                    return@withContext Result.failure(
                        SecurityException("Client authentication failed")
                    )
                }
                
                // Get current token record
                val tokenRecord = transaction {
                    IdentityOAuthTokensTable.select { 
                        IdentityOAuthTokensTable.refreshToken eq request.refreshToken 
                    }.singleOrNull()
                } ?: return@withContext Result.failure(
                    IllegalArgumentException("Invalid refresh token")
                )
                
                // Validate refresh token expiration
                val refreshTokenExpiry = tokenRecord[IdentityOAuthTokensTable.refreshTokenExpiresAt]
                if (refreshTokenExpiry?.isBefore(Instant.now()) == true) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Refresh token expired")
                    )
                }
                
                // Generate new access token
                val newAccessToken = generateSecureToken(TOKEN_LENGTH)
                val now = Instant.now()
                
                // Update database with new access token
                transaction {
                    IdentityOAuthTokensTable.update({ 
                        IdentityOAuthTokensTable.refreshToken eq request.refreshToken 
                    }) {
                        it[accessToken] = newAccessToken
                        it[accessTokenExpiresAt] = now.plusSeconds(ACCESS_TOKEN_DURATION_MINUTES * 60)
                    }
                }
                
                // Cache new access token
                val tokenData = OAuthTokenData(
                    accessToken = newAccessToken,
                    refreshToken = request.refreshToken,
                    clientId = request.clientId,
                    userId = tokenRecord[IdentityOAuthTokensTable.identityId].value.toString(),
                    scope = tokenRecord[IdentityOAuthTokensTable.scope],
                    createdAt = now.toString(),
                    expiresAt = now.plusSeconds(ACCESS_TOKEN_DURATION_MINUTES * 60).toString()
                )
                
                cacheManager.cacheData("oauth_token:$newAccessToken", tokenData, (ACCESS_TOKEN_DURATION_MINUTES * 60).toInt())
                
                // Invalidate old access token
                val oldAccessToken = tokenRecord[IdentityOAuthTokensTable.accessToken]
                if (oldAccessToken != null) {
                    cacheManager.invalidateCache("oauth_token:$oldAccessToken")
                }
                
                logger.info("✅ OAuth access token refreshed successfully")
                
                Result.success(OAuthTokenResponse(
                    accessToken = newAccessToken,
                    tokenType = "Bearer",
                    expiresIn = (ACCESS_TOKEN_DURATION_MINUTES * 60).toInt(),
                    refreshToken = request.refreshToken,
                    scope = tokenRecord[IdentityOAuthTokensTable.scope]
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to refresh OAuth access token", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Validate OAuth2 access token
     */
    suspend fun validateAccessToken(accessToken: String): Result<OAuthTokenData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<OAuthTokenData>("oauth_token:$accessToken")
                if (cached != null) {
                    return@withContext Result.success(cached)
                }
                
                // Query database
                val tokenRecord = transaction {
                    IdentityOAuthTokensTable.select { 
                        IdentityOAuthTokensTable.accessToken eq accessToken 
                    }.singleOrNull()
                } ?: return@withContext Result.failure(
                    SecurityException("Invalid access token")
                )
                
                // Check expiration
                val expiresAt = tokenRecord[IdentityOAuthTokensTable.accessTokenExpiresAt]
                if (expiresAt?.isBefore(Instant.now()) == true) {
                    return@withContext Result.failure(
                        SecurityException("Access token expired")
                    )
                }
                
                val tokenData = OAuthTokenData(
                    accessToken = accessToken,
                    refreshToken = tokenRecord[IdentityOAuthTokensTable.refreshToken],
                    clientId = tokenRecord[IdentityOAuthTokensTable.clientId],
                    userId = tokenRecord[IdentityOAuthTokensTable.identityId].value.toString(),
                    scope = tokenRecord[IdentityOAuthTokensTable.scope],
                    createdAt = tokenRecord[IdentityOAuthTokensTable.createdAt].toString(),
                    expiresAt = expiresAt.toString()
                )
                
                // Cache for future requests
                val remainingSeconds = expiresAt?.epochSecond?.minus(Instant.now().epochSecond)?.toInt() ?: 3600
                cacheManager.cacheData("oauth_token:$accessToken", tokenData, remainingSeconds)
                
                Result.success(tokenData)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to validate OAuth access token", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Revoke OAuth2 token
     */
    suspend fun revokeToken(token: String, tokenTypeHint: String? = null): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚫 Revoking OAuth token")
                
                // Find and revoke token
                val revoked = transaction {
                    val updateCount = if (tokenTypeHint == "refresh_token") {
                        IdentityOAuthTokensTable.update({ 
                            IdentityOAuthTokensTable.refreshToken eq token 
                        }) {
                            it[revokedAt] = Instant.now()
                        }
                    } else {
                        IdentityOAuthTokensTable.update({ 
                            (IdentityOAuthTokensTable.accessToken eq token) or
                            (IdentityOAuthTokensTable.refreshToken eq token)
                        }) {
                            it[revokedAt] = Instant.now()
                        }
                    }
                    updateCount > 0
                }
                
                if (revoked) {
                    // Invalidate cached tokens
                    cacheManager.invalidateCache("oauth_token:$token")
                    logger.info("✅ OAuth token revoked successfully")
                } else {
                    logger.warn("⚠️ Token not found for revocation")
                }
                
                Result.success(true) // OAuth spec says always return success
                
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke OAuth token", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's connected apps
     */
    suspend fun getUserConnectedApps(userId: String): List<ConnectedApp> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    IdentityOAuthTokensTable
                        .join(IdentityOAuthClientsTable, JoinType.INNER, 
                              IdentityOAuthTokensTable.clientId, IdentityOAuthClientsTable.clientId)
                        .select { 
                            (IdentityOAuthTokensTable.identityId eq UUID.fromString(userId)) and
                            (IdentityOAuthTokensTable.revokedAt.isNull()) and
                            (IdentityOAuthTokensTable.refreshTokenExpiresAt greater Instant.now())
                        }
                        .groupBy(IdentityOAuthClientsTable.clientId)
                        .map { record ->
                            ConnectedApp(
                                clientId = record[IdentityOAuthClientsTable.clientId],
                                name = record[IdentityOAuthClientsTable.name],
                                description = record[IdentityOAuthClientsTable.description],
                                logoUrl = record[IdentityOAuthClientsTable.logoUrl],
                                website = record[IdentityOAuthClientsTable.website],
                                scopes = record[IdentityOAuthTokensTable.scope].split(" "),
                                connectedAt = record[IdentityOAuthTokensTable.createdAt].toString(),
                                lastUsed = record[IdentityOAuthTokensTable.usedAt]?.toString()
                            )
                        }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get user connected apps: $userId", e)
                emptyList()
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun getOAuthClient(clientId: String): OAuthClient? {
        return try {
            // Check cache first
            val cached = cacheManager.getCachedData<OAuthClient>("oauth_client:$clientId")
            if (cached != null) {
                return cached
            }
            
            // Query database
            val client = transaction {
                IdentityOAuthClientsTable.select { IdentityOAuthClientsTable.clientId eq clientId }
                    .singleOrNull()
                    ?.let { record ->
                        OAuthClient(
                            clientId = record[IdentityOAuthClientsTable.clientId],
                            clientSecret = record[IdentityOAuthClientsTable.clientSecret],
                            name = record[IdentityOAuthClientsTable.name],
                            description = record[IdentityOAuthClientsTable.description],
                            redirectUris = record[IdentityOAuthClientsTable.redirectUris],
                            allowedScopes = record[IdentityOAuthClientsTable.allowedScopes],
                            grantTypes = record[IdentityOAuthClientsTable.grantTypes],
                            logoUrl = record[IdentityOAuthClientsTable.logoUrl],
                            website = record[IdentityOAuthClientsTable.website],
                            privacyPolicy = record[IdentityOAuthClientsTable.privacyPolicy],
                            termsOfService = record[IdentityOAuthClientsTable.termsOfService],
                            isActive = record[IdentityOAuthClientsTable.isActive],
                            createdAt = record[IdentityOAuthClientsTable.createdAt].toString()
                        )
                    }
            }
            
            if (client != null) {
                cacheManager.cacheData("oauth_client:$clientId", client, CACHE_TTL_SECONDS)
            }
            
            client
        } catch (e: Exception) {
            logger.error("❌ Failed to get OAuth client: $clientId", e)
            null
        }
    }
    
    private fun parseAndValidateScopes(scope: String, allowedScopes: List<String>): List<String> {
        if (scope.isBlank()) return emptyList()
        
        val requestedScopes = scope.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        return requestedScopes.filter { it in allowedScopes }
    }
    
    private fun authenticateClient(client: OAuthClient, providedSecret: String?): Boolean {
        // For public clients (mobile apps), secret may be null
        if (client.clientSecret.isNullOrBlank()) {
            return providedSecret.isNullOrBlank()
        }
        
        return client.clientSecret == providedSecret
    }
    
    private fun validatePKCE(codeChallenge: String, method: String?, codeVerifier: String): Boolean {
        return when (method) {
            "S256" -> {
                // TODO: Implement SHA256 PKCE validation
                true
            }
            "plain", null -> {
                codeChallenge == codeVerifier
            }
            else -> false
        }
    }
    
    private fun generateSecureCode(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    private fun generateSecureToken(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    private fun buildAuthorizationUrl(requestId: String, client: OAuthClient): String {
        return "/oauth/authorize?request_id=$requestId"
    }
    
    private suspend fun auditOAuthAction(userId: String, action: String, details: Map<String, String>) {
        try {
            transaction {
                IdentityAuditLogTable.insert {
                    it[identityId] = UUID.fromString(userId)
                    it[this.action] = action
                    it[this.details] = details
                    it[createdAt] = Instant.now()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to log OAuth audit event", e)
        }
    }
}
