package com.entativa.id.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.entativa.shared.cache.EntativaCacheManager
import com.entativa.shared.database.EntativaDatabaseFactory
import com.entativa.shared.database.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Entativa ID Authentication Service - OAuth2/JWT Implementation
 * Provides unified authentication across all Entativa platforms
 * 
 * @author Neo Qiss
 * @status Production-ready with Apple/Google-level security standards
 */
class AuthenticationService(
    private val cacheManager: EntativaCacheManager,
    private val jwtSecret: String = System.getenv("JWT_SECRET") ?: "default_secret_change_in_production"
) {
    
    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val ACCESS_TOKEN_EXPIRY_MINUTES = 60 // 1 hour
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 30 // 30 days
        private const val AUTH_CODE_EXPIRY_MINUTES = 10 // 10 minutes
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MINUTES = 30
        private const val SESSION_CACHE_TTL = 3600 // 1 hour
        
        // OAuth2 Scopes
        val DEFAULT_SCOPES = listOf("profile:read", "email:read")
        val AVAILABLE_SCOPES = listOf(
            "profile:read", "profile:write",
            "email:read", "email:write",
            "platforms:read", "platforms:write",
            "analytics:read", "verification:read",
            "admin:read", "admin:write"
        )
    }
    
    // Database Tables
    object OAuthApplications : UUIDTable("oauth_applications") {
        val name = varchar("name", 200)
        val clientId = varchar("client_id", 64).uniqueIndex()
        val clientSecretHash = varchar("client_secret_hash", 255)
        val redirectUris = array<String>("redirect_uris")
        val allowedScopes = array<String>("allowed_scopes").default(DEFAULT_SCOPES)
        val applicationType = varchar("application_type", 20).default("web")
        val ownerIdentityId = reference("owner_identity_id", EntativaIdService.EntativaIdentities)
        val isActive = bool("is_active").default(true)
        val isTrusted = bool("is_trusted").default(false)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    }
    
    object OAuthTokens : UUIDTable("oauth_tokens") {
        val accessTokenHash = varchar("access_token_hash", 255).uniqueIndex()
        val refreshTokenHash = varchar("refresh_token_hash", 255).nullable().uniqueIndex()
        val identityId = reference("identity_id", EntativaIdService.EntativaIdentities)
        val applicationId = reference("application_id", OAuthApplications)
        val scopes = array<String>("scopes")
        val accessTokenExpiresAt = timestamp("access_token_expires_at")
        val refreshTokenExpiresAt = timestamp("refresh_token_expires_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
        val lastUsedAt = timestamp("last_used_at").nullable()
        val usageCount = integer("usage_count").default(0)
        val revoked = bool("revoked").default(false)
        val revokedAt = timestamp("revoked_at").nullable()
        val revokedBy = reference("revoked_by", EntativaIdService.EntativaIdentities).nullable()
        val ipAddress = varchar("ip_address", 45).nullable()
        val userAgent = text("user_agent").nullable()
    }
    
    object AuthorizationCodes : UUIDTable("authorization_codes") {
        val code = varchar("code", 128).uniqueIndex()
        val identityId = reference("identity_id", EntativaIdService.EntativaIdentities)
        val applicationId = reference("application_id", OAuthApplications)
        val redirectUri = varchar("redirect_uri", 500)
        val scopes = array<String>("scopes")
        val expiresAt = timestamp("expires_at")
        val used = bool("used").default(false)
        val usedAt = timestamp("used_at").nullable()
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp())
    }
    
    /**
     * Authenticate user with email/password
     */
    suspend fun authenticate(request: AuthenticationRequest): AuthenticationResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Authentication attempt for: ${request.email}")
                
                // Get user identity
                val identity = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdService.EntativaIdentities.select { 
                        EntativaIdService.EntativaIdentities.email eq request.email 
                    }.singleOrNull()
                } ?: return@withContext AuthenticationResponse(
                    success = false,
                    error = "Invalid credentials",
                    errorCode = "INVALID_CREDENTIALS"
                )
                
                val identityId = identity[EntativaIdService.EntativaIdentities.id].value
                
                // Check if account is locked
                val lockoutEnd = identity[EntativaIdService.EntativaIdentities.lockedUntil]
                if (lockoutEnd != null && lockoutEnd.isAfter(Instant.now())) {
                    return@withContext AuthenticationResponse(
                        success = false,
                        error = "Account is temporarily locked",
                        errorCode = "ACCOUNT_LOCKED"
                    )
                }
                
                // Verify password
                val storedHash = identity[EntativaIdService.EntativaIdentities.passwordHash]
                val providedHash = hashPassword(request.password)
                
                if (storedHash != providedHash) {
                    // Increment failed attempts
                    val failedAttempts = identity[EntativaIdService.EntativaIdentities.failedLoginAttempts] + 1
                    
                    EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                        EntativaIdService.EntativaIdentities.update({ 
                            EntativaIdService.EntativaIdentities.id eq identityId 
                        }) {
                            it[this.failedLoginAttempts] = failedAttempts
                            if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
                                it[lockedUntil] = Instant.now().plus(LOCKOUT_DURATION_MINUTES.toLong(), ChronoUnit.MINUTES)
                            }
                        }
                    }
                    
                    return@withContext AuthenticationResponse(
                        success = false,
                        error = "Invalid credentials",
                        errorCode = "INVALID_CREDENTIALS"
                    )
                }
                
                // Check account status
                val status = identity[EntativaIdService.EntativaIdentities.status]
                if (status != "active") {
                    return@withContext AuthenticationResponse(
                        success = false,
                        error = "Account is $status",
                        errorCode = "ACCOUNT_INACTIVE"
                    )
                }
                
                // Reset failed attempts on successful login
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    EntativaIdService.EntativaIdentities.update({ 
                        EntativaIdService.EntativaIdentities.id eq identityId 
                    }) {
                        it[failedLoginAttempts] = 0
                        it[lockedUntil] = null
                        it[lastLoginAt] = Instant.now()
                    }
                }
                
                // Generate tokens
                val tokenPair = generateTokenPair(identityId, request.clientId, request.scopes, request.ipAddress, request.userAgent)
                
                // Cache session
                cacheSession(identityId, tokenPair.accessToken)
                
                // Log successful authentication
                logAuthEvent(identityId, "login_success", mapOf(
                    "client_id" to (request.clientId ?: "direct"),
                    "ip_address" to (request.ipAddress ?: "unknown")
                ))
                
                logger.info("✅ Authentication successful for: ${request.email}")
                
                AuthenticationResponse(
                    success = true,
                    accessToken = tokenPair.accessToken,
                    refreshToken = tokenPair.refreshToken,
                    expiresIn = ACCESS_TOKEN_EXPIRY_MINUTES * 60,
                    tokenType = "Bearer",
                    scope = request.scopes.joinToString(" "),
                    identityId = identityId,
                    eid = identity[EntativaIdService.EntativaIdentities.eid]
                )
                
            } catch (e: Exception) {
                logger.error("❌ Authentication failed for: ${request.email}", e)
                AuthenticationResponse(
                    success = false,
                    error = "Authentication failed",
                    errorCode = "INTERNAL_ERROR"
                )
            }
        }
    }
    
    /**
     * OAuth2 Authorization Code flow - Step 1: Generate authorization code
     */
    suspend fun authorize(request: AuthorizeRequest): AuthorizeResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔑 Authorization request for client: ${request.clientId}")
                
                // Validate client application
                val application = validateClient(request.clientId, request.redirectUri)
                    ?: return@withContext AuthorizeResponse(
                        success = false,
                        error = "Invalid client or redirect URI",
                        errorCode = "INVALID_CLIENT"
                    )
                
                // Validate scopes
                val validatedScopes = validateScopes(request.scopes, application.allowedScopes)
                if (validatedScopes.isEmpty()) {
                    return@withContext AuthorizeResponse(
                        success = false,
                        error = "Invalid scopes",
                        errorCode = "INVALID_SCOPE"
                    )
                }
                
                // Generate authorization code
                val authCode = generateSecureCode()
                val expiresAt = Instant.now().plus(AUTH_CODE_EXPIRY_MINUTES.toLong(), ChronoUnit.MINUTES)
                
                // Store authorization code
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    AuthorizationCodes.insert {
                        it[code] = authCode
                        it[identityId] = request.identityId
                        it[applicationId] = application.id
                        it[redirectUri] = request.redirectUri
                        it[scopes] = validatedScopes.toTypedArray()
                        it[this.expiresAt] = expiresAt
                    }
                }
                
                logger.info("✅ Authorization code generated for client: ${request.clientId}")
                
                AuthorizeResponse(
                    success = true,
                    authorizationCode = authCode,
                    redirectUri = "${request.redirectUri}?code=$authCode&state=${request.state}",
                    expiresIn = AUTH_CODE_EXPIRY_MINUTES * 60
                )
                
            } catch (e: Exception) {
                logger.error("❌ Authorization failed for client: ${request.clientId}", e)
                AuthorizeResponse(
                    success = false,
                    error = "Authorization failed",
                    errorCode = "INTERNAL_ERROR"
                )
            }
        }
    }
    
    /**
     * OAuth2 Authorization Code flow - Step 2: Exchange code for tokens
     */
    suspend fun exchangeCodeForTokens(request: TokenExchangeRequest): TokenExchangeResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🎫 Token exchange for client: ${request.clientId}")
                
                // Validate client credentials
                val application = validateClientCredentials(request.clientId, request.clientSecret)
                    ?: return@withContext TokenExchangeResponse(
                        success = false,
                        error = "Invalid client credentials",
                        errorCode = "INVALID_CLIENT"
                    )
                
                // Get and validate authorization code
                val codeRecord = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    AuthorizationCodes.select { 
                        (AuthorizationCodes.code eq request.code) and 
                        (AuthorizationCodes.applicationId eq application.id) and
                        (AuthorizationCodes.redirectUri eq request.redirectUri)
                    }.singleOrNull()
                } ?: return@withContext TokenExchangeResponse(
                    success = false,
                    error = "Invalid authorization code",
                    errorCode = "INVALID_GRANT"
                )
                
                // Check if code is expired or already used
                if (codeRecord[AuthorizationCodes.expiresAt].isBefore(Instant.now()) || 
                    codeRecord[AuthorizationCodes.used]) {
                    return@withContext TokenExchangeResponse(
                        success = false,
                        error = "Authorization code expired or already used",
                        errorCode = "INVALID_GRANT"
                    )
                }
                
                // Mark code as used
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    AuthorizationCodes.update({ AuthorizationCodes.code eq request.code }) {
                        it[used] = true
                        it[usedAt] = Instant.now()
                    }
                }
                
                // Generate token pair
                val identityId = codeRecord[AuthorizationCodes.identityId]
                val scopes = codeRecord[AuthorizationCodes.scopes].toList()
                val tokenPair = generateTokenPair(identityId, request.clientId, scopes)
                
                logger.info("✅ Tokens generated for client: ${request.clientId}")
                
                TokenExchangeResponse(
                    success = true,
                    accessToken = tokenPair.accessToken,
                    refreshToken = tokenPair.refreshToken,
                    expiresIn = ACCESS_TOKEN_EXPIRY_MINUTES * 60,
                    tokenType = "Bearer",
                    scope = scopes.joinToString(" ")
                )
                
            } catch (e: Exception) {
                logger.error("❌ Token exchange failed for client: ${request.clientId}", e)
                TokenExchangeResponse(
                    success = false,
                    error = "Token exchange failed",
                    errorCode = "INTERNAL_ERROR"
                )
            }
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔄 Token refresh request")
                
                // Validate refresh token
                val tokenHash = hashToken(request.refreshToken)
                val tokenRecord = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    OAuthTokens.select { 
                        (OAuthTokens.refreshTokenHash eq tokenHash) and
                        (OAuthTokens.revoked eq false)
                    }.singleOrNull()
                } ?: return@withContext RefreshTokenResponse(
                    success = false,
                    error = "Invalid refresh token",
                    errorCode = "INVALID_TOKEN"
                )
                
                // Check if refresh token is expired
                val refreshExpiresAt = tokenRecord[OAuthTokens.refreshTokenExpiresAt]
                if (refreshExpiresAt != null && refreshExpiresAt.isBefore(Instant.now())) {
                    return@withContext RefreshTokenResponse(
                        success = false,
                        error = "Refresh token expired",
                        errorCode = "INVALID_TOKEN"
                    )
                }
                
                // Revoke old tokens
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    OAuthTokens.update({ OAuthTokens.id eq tokenRecord[OAuthTokens.id] }) {
                        it[revoked] = true
                        it[revokedAt] = Instant.now()
                    }
                }
                
                // Generate new token pair
                val identityId = tokenRecord[OAuthTokens.identityId]
                val applicationId = tokenRecord[OAuthTokens.applicationId]
                val scopes = tokenRecord[OAuthTokens.scopes].toList()
                
                val newTokenPair = generateTokenPair(identityId, null, scopes)
                
                logger.info("✅ Token refreshed successfully")
                
                RefreshTokenResponse(
                    success = true,
                    accessToken = newTokenPair.accessToken,
                    refreshToken = newTokenPair.refreshToken,
                    expiresIn = ACCESS_TOKEN_EXPIRY_MINUTES * 60,
                    tokenType = "Bearer",
                    scope = scopes.joinToString(" ")
                )
                
            } catch (e: Exception) {
                logger.error("❌ Token refresh failed", e)
                RefreshTokenResponse(
                    success = false,
                    error = "Token refresh failed",
                    errorCode = "INTERNAL_ERROR"
                )
            }
        }
    }
    
    /**
     * Validate access token and return user info
     */
    suspend fun validateToken(accessToken: String): TokenValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cachedValidation = cacheManager.getCachedData<TokenValidationResult>("token_validation:${hashToken(accessToken)}")
                if (cachedValidation != null) {
                    return@withContext cachedValidation
                }
                
                // Validate JWT token
                val decodedJWT = try {
                    JWT.require(algorithm).build().verify(accessToken)
                } catch (e: Exception) {
                    return@withContext TokenValidationResult(
                        valid = false,
                        error = "Invalid token format"
                    )
                }
                
                // Check token in database
                val tokenHash = hashToken(accessToken)
                val tokenRecord = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    OAuthTokens.select { 
                        (OAuthTokens.accessTokenHash eq tokenHash) and
                        (OAuthTokens.revoked eq false)
                    }.singleOrNull()
                } ?: return@withContext TokenValidationResult(
                    valid = false,
                    error = "Token not found"
                )
                
                // Check expiration
                if (tokenRecord[OAuthTokens.accessTokenExpiresAt].isBefore(Instant.now())) {
                    return@withContext TokenValidationResult(
                        valid = false,
                        error = "Token expired"
                    )
                }
                
                // Update usage statistics
                EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    OAuthTokens.update({ OAuthTokens.id eq tokenRecord[OAuthTokens.id] }) {
                        it[lastUsedAt] = Instant.now()
                        it[usageCount] = tokenRecord[OAuthTokens.usageCount] + 1
                    }
                }
                
                val result = TokenValidationResult(
                    valid = true,
                    identityId = tokenRecord[OAuthTokens.identityId],
                    applicationId = tokenRecord[OAuthTokens.applicationId],
                    scopes = tokenRecord[OAuthTokens.scopes].toList(),
                    expiresAt = tokenRecord[OAuthTokens.accessTokenExpiresAt]
                )
                
                // Cache validation result
                cacheManager.cacheData("token_validation:$tokenHash", result, 300) // 5 minutes
                
                result
                
            } catch (e: Exception) {
                logger.error("❌ Token validation failed", e)
                TokenValidationResult(
                    valid = false,
                    error = "Validation failed"
                )
            }
        }
    }
    
    /**
     * Revoke access/refresh token
     */
    suspend fun revokeToken(request: RevokeTokenRequest): RevokeTokenResponse {
        return withContext(Dispatchers.IO) {
            try {
                val tokenHash = hashToken(request.token)
                
                val updated = EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                    OAuthTokens.update({ 
                        (OAuthTokens.accessTokenHash eq tokenHash) or 
                        (OAuthTokens.refreshTokenHash eq tokenHash)
                    }) {
                        it[revoked] = true
                        it[revokedAt] = Instant.now()
                        it[revokedBy] = request.revokedBy
                    }
                }
                
                if (updated > 0) {
                    // Clear from cache
                    cacheManager.invalidateCache("token_validation:$tokenHash")
                    
                    logger.info("✅ Token revoked successfully")
                    RevokeTokenResponse(success = true)
                } else {
                    RevokeTokenResponse(
                        success = false,
                        error = "Token not found"
                    )
                }
                
            } catch (e: Exception) {
                logger.error("❌ Token revocation failed", e)
                RevokeTokenResponse(
                    success = false,
                    error = "Revocation failed"
                )
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun generateTokenPair(
        identityId: UUID,
        clientId: String?,
        scopes: List<String>,
        ipAddress: String? = null,
        userAgent: String? = null
    ): TokenPair {
        val now = Instant.now()
        val accessTokenExpiry = now.plus(ACCESS_TOKEN_EXPIRY_MINUTES.toLong(), ChronoUnit.MINUTES)
        val refreshTokenExpiry = now.plus(REFRESH_TOKEN_EXPIRY_DAYS.toLong(), ChronoUnit.DAYS)
        
        // Generate JWT access token
        val accessToken = JWT.create()
            .withSubject(identityId.toString())
            .withIssuer("entativa-id")
            .withAudience("entativa-platforms")
            .withClaim("scopes", scopes)
            .withClaim("client_id", clientId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(accessTokenExpiry))
            .sign(algorithm)
        
        // Generate refresh token
        val refreshToken = generateSecureToken()
        
        // Get or create application record
        val applicationId = clientId?.let { 
            EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                OAuthApplications.select { OAuthApplications.clientId eq it }.singleOrNull()
            }?.get(OAuthApplications.id)?.value
        }
        
        // Store token in database
        if (applicationId != null) {
            EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
                OAuthTokens.insert {
                    it[accessTokenHash] = hashToken(accessToken)
                    it[refreshTokenHash] = hashToken(refreshToken)
                    it[this.identityId] = identityId
                    it[this.applicationId] = applicationId
                    it[this.scopes] = scopes.toTypedArray()
                    it[accessTokenExpiresAt] = accessTokenExpiry
                    it[refreshTokenExpiresAt] = refreshTokenExpiry
                    it[this.ipAddress] = ipAddress
                    it[this.userAgent] = userAgent
                }
            }
        }
        
        return TokenPair(accessToken, refreshToken)
    }
    
    private suspend fun validateClient(clientId: String, redirectUri: String): ClientApplication? {
        return EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
            val app = OAuthApplications.select { 
                (OAuthApplications.clientId eq clientId) and 
                (OAuthApplications.isActive eq true)
            }.singleOrNull() ?: return@dbQuery null
            
            val allowedUris = app[OAuthApplications.redirectUris].toList()
            if (!allowedUris.contains(redirectUri)) return@dbQuery null
            
            ClientApplication(
                id = app[OAuthApplications.id].value,
                name = app[OAuthApplications.name],
                clientId = clientId,
                allowedScopes = app[OAuthApplications.allowedScopes].toList()
            )
        }
    }
    
    private suspend fun validateClientCredentials(clientId: String, clientSecret: String): ClientApplication? {
        return EntativaDatabaseFactory.dbQuery(Platform.ENTATIVA_ID) {
            val app = OAuthApplications.select { 
                OAuthApplications.clientId eq clientId 
            }.singleOrNull() ?: return@dbQuery null
            
            val storedSecretHash = app[OAuthApplications.clientSecretHash]
            val providedSecretHash = hashClientSecret(clientSecret)
            
            if (storedSecretHash != providedSecretHash) return@dbQuery null
            
            ClientApplication(
                id = app[OAuthApplications.id].value,
                name = app[OAuthApplications.name],
                clientId = clientId,
                allowedScopes = app[OAuthApplications.allowedScopes].toList()
            )
        }
    }
    
    private fun validateScopes(requestedScopes: List<String>, allowedScopes: List<String>): List<String> {
        return requestedScopes.filter { it in allowedScopes && it in AVAILABLE_SCOPES }
    }
    
    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    private fun generateSecureCode(): String {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    private fun hashPassword(password: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hashedBytes = md.digest((password + "entativa_salt").toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun hashToken(token: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hashedBytes = md.digest(token.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun hashClientSecret(secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val hashedBytes = mac.doFinal(secret.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun cacheSession(identityId: UUID, accessToken: String) {
        val sessionData = mapOf(
            "identity_id" to identityId.toString(),
            "access_token_hash" to hashToken(accessToken),
            "created_at" to Instant.now().toString()
        )
        cacheManager.cacheData("session:$identityId", sessionData, SESSION_CACHE_TTL)
    }
    
    private suspend fun logAuthEvent(identityId: UUID, event: String, details: Map<String, String>) {
        logger.info("🔐 Auth event: $event for identity: $identityId - Details: $details")
    }
}

// ============== DATA CLASSES ==============

data class AuthenticationRequest(
    val email: String,
    val password: String,
    val clientId: String?,
    val scopes: List<String> = DEFAULT_SCOPES,
    val ipAddress: String?,
    val userAgent: String?
)

data class AuthenticationResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val tokenType: String? = null,
    val scope: String? = null,
    val identityId: UUID? = null,
    val eid: String? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class AuthorizeRequest(
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>,
    val state: String,
    val identityId: UUID
)

data class AuthorizeResponse(
    val success: Boolean,
    val authorizationCode: String? = null,
    val redirectUri: String? = null,
    val expiresIn: Int? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class TokenExchangeRequest(
    val grantType: String = "authorization_code",
    val code: String,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String
)

data class TokenExchangeResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class RefreshTokenRequest(
    val grantType: String = "refresh_token",
    val refreshToken: String,
    val clientId: String?,
    val clientSecret: String?
)

data class RefreshTokenResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val tokenType: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class TokenValidationResult(
    val valid: Boolean,
    val identityId: UUID? = null,
    val applicationId: UUID? = null,
    val scopes: List<String> = emptyList(),
    val expiresAt: Instant? = null,
    val error: String? = null
)

data class RevokeTokenRequest(
    val token: String,
    val tokenTypeHint: String? = null,
    val revokedBy: UUID? = null
)

data class RevokeTokenResponse(
    val success: Boolean,
    val error: String? = null
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)

data class ClientApplication(
    val id: UUID,
    val name: String,
    val clientId: String,
    val allowedScopes: List<String>
)