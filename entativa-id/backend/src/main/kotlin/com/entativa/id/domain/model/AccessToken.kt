package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Token Domain Models for Entativa ID
 * Comprehensive OAuth2 and JWT token management with enterprise security
 * 
 * @author Neo Qiss
 * @status Production-ready token system with security features
 */

/**
 * Token - Main token entity for OAuth2 and JWT management
 */
@Serializable
data class Token(
    val id: String,
    val userId: String,
    val clientId: String? = null,
    val tokenType: String, // ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, API_KEY
    val tokenValue: String? = null, // Only for display/logs, actual value is hashed
    val tokenHash: String,
    val refreshToken: String? = null,
    val refreshTokenHash: String? = null,
    val scope: String? = null,
    val audience: String? = null,
    val issuer: String? = null,
    val subject: String? = null,
    val isActive: Boolean = true,
    val isRevoked: Boolean = false,
    val isExpired: Boolean = false,
    val expiresAt: Instant? = null,
    val refreshExpiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val usageCount: Long = 0,
    val revokedAt: Instant? = null,
    val revokedBy: String? = null,
    val revocationReason: String? = null,
    val grantType: String? = null, // authorization_code, refresh_token, client_credentials
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val sessionId: String? = null,
    val platform: String? = null,
    val securityLevel: String = "STANDARD", // STANDARD, HIGH, CRITICAL
    val riskScore: Double = 0.0,
    val fraudFlags: String? = null,
    val claims: String? = null, // JSON claims for JWT tokens
    val metadata: String? = null, // Additional metadata
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String? = null,
    val version: Long = 1
)

/**
 * Create token request
 */
@Serializable
data class CreateTokenRequest(
    val userId: String,
    val clientId: String? = null,
    val tokenType: String,
    val tokenValue: String? = null,
    val tokenHash: String,
    val refreshToken: String? = null,
    val refreshTokenHash: String? = null,
    val scope: String? = null,
    val audience: String? = null,
    val issuer: String? = null,
    val subject: String? = null,
    val isActive: Boolean = true,
    val isRevoked: Boolean = false,
    val isExpired: Boolean = false,
    val expiresAt: Instant? = null,
    val refreshExpiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val grantType: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val sessionId: String? = null,
    val platform: String? = null,
    val securityLevel: String = "STANDARD",
    val riskScore: Double = 0.0,
    val fraudFlags: String? = null,
    val claims: String? = null,
    val metadata: String? = null,
    val createdBy: String
)

/**
 * Token response for API
 */
@Serializable
data class TokenResponse(
    val id: String,
    val tokenType: String,
    val scope: String? = null,
    val isActive: Boolean,
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val usageCount: Long,
    val deviceId: String? = null,
    val platform: String? = null,
    val securityLevel: String,
    val createdAt: Instant
)

/**
 * Token statistics
 */
@Serializable
data class TokenStatistics(
    val totalTokens: Long,
    val activeTokens: Long,
    val revokedTokens: Long,
    val expiredTokens: Long,
    val generatedAt: Instant
)

/**
 * JWT Claims for Entativa ID tokens
 */
@Serializable
data class EntativaJWTClaims(
    // Standard JWT claims
    val iss: String, // Issuer
    val sub: String, // Subject (user ID)
    val aud: String, // Audience
    val exp: Long, // Expiration time
    val nbf: Long, // Not before
    val iat: Long, // Issued at
    val jti: String, // JWT ID
    
    // Entativa-specific claims
    val eid: String, // Entativa ID handle
    val email: String? = null,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val verificationStatus: String = "NONE",
    val verificationBadge: String? = null,
    val reputationScore: Int = 1000,
    val securityLevel: String = "STANDARD",
    val platforms: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val deviceId: String? = null,
    val sessionId: String? = null,
    val ipAddress: String? = null,
    val location: String? = null,
    val clientId: String? = null,
    val scope: String? = null,
    val grantType: String? = null,
    val tokenType: String = "access_token",
    
    // Profile information
    val profile: ProfileClaims? = null,
    
    // Platform-specific claims
    val platformData: Map<String, String> = emptyMap()
)

/**
 * Profile claims for JWT
 */
@Serializable
data class ProfileClaims(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val timezone: String? = null,
    val language: String? = null
)

/**
 * OAuth2 Authorization Code
 */
@Serializable
data class AuthorizationCode(
    val id: String,
    val code: String,
    val codeHash: String,
    val userId: String,
    val clientId: String,
    val redirectUri: String,
    val scope: String? = null,
    val state: String? = null,
    val codeChallenge: String? = null, // For PKCE
    val codeChallengeMethod: String? = null, // For PKCE
    val isUsed: Boolean = false,
    val usedAt: Instant? = null,
    val expiresAt: Instant,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val sessionId: String? = null,
    val createdAt: Instant,
    val createdBy: String
)

/**
 * Create authorization code request
 */
@Serializable
data class CreateAuthorizationCodeRequest(
    val code: String,
    val codeHash: String,
    val userId: String,
    val clientId: String,
    val redirectUri: String,
    val scope: String? = null,
    val state: String? = null,
    val codeChallenge: String? = null,
    val codeChallengeMethod: String? = null,
    val expiresAt: Instant,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val location: String? = null,
    val sessionId: String? = null,
    val createdBy: String
)

/**
 * Token validation result
 */
@Serializable
data class TokenValidationResult(
    val isValid: Boolean,
    val token: Token? = null,
    val claims: EntativaJWTClaims? = null,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val securityIssues: List<String> = emptyList()
)

/**
 * Token refresh request
 */
@Serializable
data class TokenRefreshRequest(
    val refreshToken: String,
    val clientId: String? = null,
    val scope: String? = null,
    val deviceId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * Token refresh response
 */
@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val scope: String? = null
)

/**
 * Token revocation request
 */
@Serializable
data class TokenRevocationRequest(
    val token: String,
    val tokenTypeHint: String? = null, // access_token or refresh_token
    val clientId: String? = null,
    val reason: String? = null
)

/**
 * Bulk token operation request
 */
@Serializable
data class BulkTokenOperationRequest(
    val operation: TokenOperation,
    val filters: TokenFilters,
    val reason: String,
    val performedBy: String
)

@Serializable
enum class TokenOperation {
    REVOKE,
    EXTEND_EXPIRY,
    UPDATE_SECURITY_LEVEL,
    MARK_SUSPICIOUS,
    CLEANUP_EXPIRED
}

/**
 * Token search filters
 */
@Serializable
data class TokenFilters(
    val userId: String? = null,
    val clientId: String? = null,
    val tokenType: String? = null,
    val isActive: Boolean? = null,
    val isRevoked: Boolean? = null,
    val platform: String? = null,
    val securityLevel: String? = null,
    val minRiskScore: Double? = null,
    val maxRiskScore: Double? = null,
    val createdAfter: Instant? = null,
    val createdBefore: Instant? = null,
    val expiresAfter: Instant? = null,
    val expiresBefore: Instant? = null
)

/**
 * API Key - Special type of token for API access
 */
@Serializable
data class ApiKey(
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val keyPrefix: String, // First 8 characters for identification
    val keyHash: String,
    val scope: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val isActive: Boolean = true,
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val usageCount: Long = 0,
    val rateLimit: Int? = null, // Requests per minute
    val allowedIps: List<String> = emptyList(),
    val allowedDomains: List<String> = emptyList(),
    val metadata: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val version: Long = 1
)

/**
 * Create API key request
 */
@Serializable
data class CreateApiKeyRequest(
    val userId: String,
    val name: String,
    val description: String? = null,
    val scope: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val expiresAt: Instant? = null,
    val rateLimit: Int? = null,
    val allowedIps: List<String> = emptyList(),
    val allowedDomains: List<String> = emptyList(),
    val metadata: String? = null,
    val createdBy: String
)

/**
 * API key response
 */
@Serializable
data class ApiKeyResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val keyPrefix: String,
    val scope: List<String>,
    val permissions: List<String>,
    val isActive: Boolean,
    val expiresAt: Instant? = null,
    val lastUsedAt: Instant? = null,
    val usageCount: Long,
    val rateLimit: Int? = null,
    val createdAt: Instant
)

/**
 * Token enums and constants
 */
object TokenTypes {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val ID_TOKEN = "id_token"
    const val API_KEY = "api_key"
    const val AUTHORIZATION_CODE = "authorization_code"
}

object GrantTypes {
    const val AUTHORIZATION_CODE = "authorization_code"
    const val REFRESH_TOKEN = "refresh_token"
    const val CLIENT_CREDENTIALS = "client_credentials"
    const val PASSWORD = "password"
    const val DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code"
}

object SecurityLevels {
    const val STANDARD = "STANDARD"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}

object Scopes {
    const val OPENID = "openid"
    const val PROFILE = "profile"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val ENTATIVA_ID = "entativa:id"
    const val ENTATIVA_PROFILE = "entativa:profile"
    const val SONET_READ = "sonet:read"
    const val SONET_WRITE = "sonet:write"
    const val GALA_READ = "gala:read"
    const val GALA_WRITE = "gala:write"
    const val PIKA_READ = "pika:read"
    const val PIKA_WRITE = "pika:write"
    const val PLAYPODS_READ = "playpods:read"
    const val PLAYPODS_WRITE = "playpods:write"
}
