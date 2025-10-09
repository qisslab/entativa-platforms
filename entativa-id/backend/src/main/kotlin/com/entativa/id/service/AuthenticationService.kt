package com.entativa.id.service

import com.entativa.id.config.*
import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.Key
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.spec.SecretKeySpec

/**
 * JWT Authentication Service for Entativa ID
 * Handles token creation, validation, and session management
 * 
 * @author Neo Qiss
 * @status Production-ready with enterprise security
 */
class AuthenticationService(
    private val cacheManager: EntativaCacheManager,
    private val jwtSecret: String = "entativa_jwt_secret_2024_super_secure_key_minimum_256_bits_required_for_hs256_algorithm"
) {
    
    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)
    
    companion object {
        private const val ACCESS_TOKEN_DURATION_MINUTES = 15L
        private const val REFRESH_TOKEN_DURATION_DAYS = 30L
        private const val SESSION_CACHE_TTL_SECONDS = 3600
        private const val TOKEN_ISSUER = "entativa-id"
        private const val TOKEN_AUDIENCE = "entativa-platforms"
    }
    
    private val signingKey: Key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    
    /**
     * Generate complete authentication tokens for user
     */
    suspend fun generateTokens(user: User, clientId: String? = null, ipAddress: String? = null): AuthTokenResponse {
        return withContext(Dispatchers.IO) {
            logger.info("🎫 Generating tokens for user: ${user.eid}")
            
            val now = Instant.now()
            val accessTokenId = UUID.randomUUID().toString()
            val refreshTokenId = UUID.randomUUID().toString()
            val sessionId = UUID.randomUUID().toString()
            
            // Create access token
            val accessToken = Jwts.builder()
                .setId(accessTokenId)
                .setIssuer(TOKEN_ISSUER)
                .setAudience(TOKEN_AUDIENCE)
                .setSubject(user.id)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ACCESS_TOKEN_DURATION_MINUTES, ChronoUnit.MINUTES)))
                .claim("eid", user.eid)
                .claim("email", user.email)
                .claim("verified", user.emailVerified)
                .claim("status", user.status.name)
                .claim("verification_status", user.verificationStatus.name)
                .claim("verification_badge", user.verificationBadge?.name)
                .claim("reputation_score", user.reputationScore)
                .claim("session_id", sessionId)
                .claim("client_id", clientId)
                .claim("token_type", "access")
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact()
            
            // Create refresh token
            val refreshToken = Jwts.builder()
                .setId(refreshTokenId)
                .setIssuer(TOKEN_ISSUER)
                .setAudience(TOKEN_AUDIENCE)
                .setSubject(user.id)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS)))
                .claim("eid", user.eid)
                .claim("session_id", sessionId)
                .claim("client_id", clientId)
                .claim("token_type", "refresh")
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact()
            
            // Store session in database
            transaction {
                SessionsTable.insert {
                    it[id] = UUID.fromString(sessionId)
                    it[identityId] = UUID.fromString(user.id)
                    it[this.clientId] = clientId
                    it[accessTokenId] = accessTokenId
                    it[refreshTokenId] = refreshTokenId
                    it[this.ipAddress] = ipAddress
                    it[userAgent] = user.userAgent
                    it[createdAt] = now
                    it[expiresAt] = now.plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS)
                }
            }
            
            // Cache session
            val session = AuthSession(
                id = sessionId,
                userId = user.id,
                clientId = clientId,
                accessTokenId = accessTokenId,
                refreshTokenId = refreshTokenId,
                ipAddress = ipAddress,
                userAgent = user.userAgent,
                createdAt = now.toString(),
                expiresAt = now.plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS).toString()
            )
            
            cacheManager.cacheData("session:$sessionId", session, SESSION_CACHE_TTL_SECONDS)
            
            logger.info("✅ Tokens generated successfully for: ${user.eid}")
            
            AuthTokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                expiresIn = (ACCESS_TOKEN_DURATION_MINUTES * 60).toInt(),
                scope = "profile email",
                user = UserSummary(
                    id = user.id,
                    eid = user.eid,
                    email = user.email,
                    emailVerified = user.emailVerified,
                    status = user.status,
                    verificationStatus = user.verificationStatus,
                    verificationBadge = user.verificationBadge,
                    reputationScore = user.reputationScore
                )
            )
        }
    }
    
    /**
     * Validate and decode access token
     */
    suspend fun validateAccessToken(token: String): Result<TokenClaims> {
        return withContext(Dispatchers.IO) {
            try {
                val claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .requireIssuer(TOKEN_ISSUER)
                    .requireAudience(TOKEN_AUDIENCE)
                    .build()
                    .parseClaimsJws(token)
                    .body
                
                // Verify token type
                val tokenType = claims["token_type"] as? String
                if (tokenType != "access") {
                    return@withContext Result.failure(
                        SecurityException("Invalid token type")
                    )
                }
                
                // Check if token is blacklisted
                val tokenId = claims.id
                val isBlacklisted = cacheManager.getCachedData<Boolean>("blacklist:$tokenId")
                if (isBlacklisted == true) {
                    return@withContext Result.failure(
                        SecurityException("Token has been revoked")
                    )
                }
                
                val tokenClaims = TokenClaims(
                    jti = claims.id,
                    sub = claims.subject,
                    iss = claims.issuer,
                    aud = claims.audience.first(),
                    exp = claims.expiration.toInstant(),
                    iat = claims.issuedAt.toInstant(),
                    eid = claims["eid"] as String,
                    email = claims["email"] as String,
                    verified = claims["verified"] as Boolean,
                    status = claims["status"] as String,
                    verificationStatus = claims["verification_status"] as String,
                    verificationBadge = claims["verification_badge"] as? String,
                    reputationScore = (claims["reputation_score"] as Number).toInt(),
                    sessionId = claims["session_id"] as String,
                    clientId = claims["client_id"] as? String
                )
                
                Result.success(tokenClaims)
                
            } catch (e: JwtException) {
                logger.warn("❌ Invalid token: ${e.message}")
                Result.failure(SecurityException("Invalid token: ${e.message}"))
            } catch (e: Exception) {
                logger.error("❌ Token validation error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    suspend fun refreshTokens(refreshToken: String, ipAddress: String? = null): Result<AuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔄 Refreshing tokens")
                
                val claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .requireIssuer(TOKEN_ISSUER)
                    .requireAudience(TOKEN_AUDIENCE)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .body
                
                // Verify token type
                val tokenType = claims["token_type"] as? String
                if (tokenType != "refresh") {
                    return@withContext Result.failure(
                        SecurityException("Invalid refresh token")
                    )
                }
                
                val userId = claims.subject
                val sessionId = claims["session_id"] as String
                val clientId = claims["client_id"] as? String
                
                // Verify session exists and is valid
                val session = getSessionById(sessionId)
                if (session == null || session.userId != userId) {
                    return@withContext Result.failure(
                        SecurityException("Invalid session")
                    )
                }
                
                // Get user data
                val userRecord = transaction {
                    EntativaIdentitiesTable.select { EntativaIdentitiesTable.id eq UUID.fromString(userId) }
                        .singleOrNull()
                } ?: return@withContext Result.failure(
                    SecurityException("User not found")
                )
                
                // Check user status
                val userStatus = userRecord[EntativaIdentitiesTable.status]
                if (userStatus != "active") {
                    return@withContext Result.failure(
                        SecurityException("User account is $userStatus")
                    )
                }
                
                // Create User object from record
                val user = buildUserFromRecord(userRecord)
                
                // Generate new tokens
                val newTokens = generateTokens(user, clientId, ipAddress)
                
                // Invalidate old session
                invalidateSession(sessionId)
                
                logger.info("✅ Tokens refreshed successfully for user: ${user.eid}")
                Result.success(newTokens)
                
            } catch (e: JwtException) {
                logger.warn("❌ Invalid refresh token: ${e.message}")
                Result.failure(SecurityException("Invalid refresh token"))
            } catch (e: Exception) {
                logger.error("❌ Token refresh error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Revoke all tokens for a user
     */
    suspend fun revokeAllTokens(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚫 Revoking all tokens for user: $userId")
                
                // Get all active sessions
                val sessions = transaction {
                    SessionsTable.select { SessionsTable.identityId eq UUID.fromString(userId) }
                        .map { 
                            it[SessionsTable.id].value.toString() to 
                            Pair(it[SessionsTable.accessTokenId], it[SessionsTable.refreshTokenId])
                        }
                }
                
                // Blacklist all tokens
                for ((sessionId, tokens) in sessions) {
                    val (accessTokenId, refreshTokenId) = tokens
                    
                    // Add to blacklist cache (tokens will expire naturally)
                    cacheManager.cacheData("blacklist:$accessTokenId", true, 86400) // 24 hours
                    cacheManager.cacheData("blacklist:$refreshTokenId", true, 2592000) // 30 days
                    
                    // Invalidate session
                    invalidateSession(sessionId)
                }
                
                // Mark all sessions as revoked in database
                transaction {
                    SessionsTable.update({ SessionsTable.identityId eq UUID.fromString(userId) }) {
                        it[revokedAt] = Instant.now()
                    }
                }
                
                logger.info("✅ All tokens revoked for user: $userId")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke tokens for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Revoke specific session
     */
    suspend fun revokeSession(sessionId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🚫 Revoking session: $sessionId")
                
                // Get session details
                val sessionRecord = transaction {
                    SessionsTable.select { SessionsTable.id eq UUID.fromString(sessionId) }
                        .singleOrNull()
                } ?: return@withContext Result.failure(
                    IllegalArgumentException("Session not found")
                )
                
                val accessTokenId = sessionRecord[SessionsTable.accessTokenId]
                val refreshTokenId = sessionRecord[SessionsTable.refreshTokenId]
                
                // Blacklist tokens
                cacheManager.cacheData("blacklist:$accessTokenId", true, 86400)
                cacheManager.cacheData("blacklist:$refreshTokenId", true, 2592000)
                
                // Mark session as revoked
                transaction {
                    SessionsTable.update({ SessionsTable.id eq UUID.fromString(sessionId) }) {
                        it[revokedAt] = Instant.now()
                    }
                }
                
                // Invalidate session cache
                invalidateSession(sessionId)
                
                logger.info("✅ Session revoked: $sessionId")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to revoke session: $sessionId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user's active sessions
     */
    suspend fun getUserSessions(userId: String): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    SessionsTable.select { 
                        (SessionsTable.identityId eq UUID.fromString(userId)) and
                        (SessionsTable.revokedAt.isNull()) and
                        (SessionsTable.expiresAt greater Instant.now())
                    }.map { record ->
                        SessionInfo(
                            id = record[SessionsTable.id].value.toString(),
                            clientId = record[SessionsTable.clientId],
                            ipAddress = record[SessionsTable.ipAddress],
                            userAgent = record[SessionsTable.userAgent],
                            createdAt = record[SessionsTable.createdAt].toString(),
                            lastActivity = record[SessionsTable.lastActivity]?.toString(),
                            location = determineLocation(record[SessionsTable.ipAddress])
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get user sessions: $userId", e)
                emptyList()
            }
        }
    }
    
    /**
     * Update session activity
     */
    suspend fun updateSessionActivity(sessionId: String, ipAddress: String? = null) {
        try {
            transaction {
                SessionsTable.update({ SessionsTable.id eq UUID.fromString(sessionId) }) {
                    it[lastActivity] = Instant.now()
                    if (ipAddress != null) {
                        it[this.ipAddress] = ipAddress
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to update session activity: $sessionId", e)
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun getSessionById(sessionId: String): AuthSession? {
        // Check cache first
        val cached = cacheManager.getCachedData<AuthSession>("session:$sessionId")
        if (cached != null) {
            return cached
        }
        
        // Query database
        return try {
            transaction {
                SessionsTable.select { SessionsTable.id eq UUID.fromString(sessionId) }
                    .singleOrNull()
                    ?.let { record ->
                        AuthSession(
                            id = record[SessionsTable.id].value.toString(),
                            userId = record[SessionsTable.identityId].value.toString(),
                            clientId = record[SessionsTable.clientId],
                            accessTokenId = record[SessionsTable.accessTokenId],
                            refreshTokenId = record[SessionsTable.refreshTokenId],
                            ipAddress = record[SessionsTable.ipAddress],
                            userAgent = record[SessionsTable.userAgent],
                            createdAt = record[SessionsTable.createdAt].toString(),
                            expiresAt = record[SessionsTable.expiresAt].toString()
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to get session: $sessionId", e)
            null
        }
    }
    
    private suspend fun invalidateSession(sessionId: String) {
        cacheManager.invalidateCache("session:$sessionId")
    }
    
    private fun buildUserFromRecord(record: ResultRow): User {
        return User(
            id = record[EntativaIdentitiesTable.id].value.toString(),
            eid = record[EntativaIdentitiesTable.eid],
            email = record[EntativaIdentitiesTable.email],
            phone = record[EntativaIdentitiesTable.phone],
            passwordHash = record[EntativaIdentitiesTable.passwordHash],
            status = UserStatus.valueOf(record[EntativaIdentitiesTable.status].uppercase()),
            emailVerified = record[EntativaIdentitiesTable.emailVerified],
            phoneVerified = record[EntativaIdentitiesTable.phoneVerified],
            twoFactorEnabled = record[EntativaIdentitiesTable.twoFactorEnabled],
            profileCompleted = record[EntativaIdentitiesTable.profileCompleted],
            verificationStatus = VerificationStatus.valueOf(record[EntativaIdentitiesTable.verificationStatus].uppercase()),
            verificationBadge = record[EntativaIdentitiesTable.verificationBadge]?.let { 
                VerificationBadge.valueOf(it.uppercase()) 
            },
            verificationDate = record[EntativaIdentitiesTable.verificationDate]?.toString(),
            reputationScore = record[EntativaIdentitiesTable.reputationScore],
            failedLoginAttempts = record[EntativaIdentitiesTable.failedLoginAttempts],
            lockedUntil = record[EntativaIdentitiesTable.lockedUntil]?.toString(),
            passwordChangedAt = record[EntativaIdentitiesTable.passwordChangedAt].toString(),
            lastLoginAt = record[EntativaIdentitiesTable.lastLoginAt]?.toString(),
            createdAt = record[EntativaIdentitiesTable.createdAt].toString(),
            updatedAt = record[EntativaIdentitiesTable.updatedAt].toString(),
            createdBy = record[EntativaIdentitiesTable.createdBy],
            ipAddress = record[EntativaIdentitiesTable.ipAddress],
            userAgent = record[EntativaIdentitiesTable.userAgent],
            countryCode = record[EntativaIdentitiesTable.countryCode]
        )
    }
    
    private fun determineLocation(ipAddress: String?): String {
        // TODO: Implement IP geolocation
        return "Unknown"
    }
}
