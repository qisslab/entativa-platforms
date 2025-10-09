package com.entativa.id.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * OAuth 2.0 Client Application Models
 * Enterprise-grade OAuth implementation for cross-platform authentication
 * 
 * @author Neo Qiss
 * @status Production-ready with comprehensive security features
 */

/**
 * OAuth client application
 */
@Serializable
data class OAuthClient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val clientId: String,
    val clientSecretHash: String,
    val redirectUris: List<String>,
    val allowedScopes: List<String> = DEFAULT_SCOPES,
    val applicationType: ApplicationType = ApplicationType.WEB,
    val ownerUserId: String,
    val isActive: Boolean = true,
    val isTrusted: Boolean = false, // Entativa first-party apps
    val isPublic: Boolean = false, // Public clients (mobile apps)
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val termsUrl: String? = null,
    val privacyUrl: String? = null,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString(),
    val lastUsedAt: String? = null,
    val usageStats: UsageStats = UsageStats()
) {
    companion object {
        val DEFAULT_SCOPES = listOf("profile:read", "email:read")
        val AVAILABLE_SCOPES = listOf(
            "profile:read", "profile:write",
            "email:read", "email:write", 
            "phone:read", "phone:write",
            "platforms:read", "platforms:write",
            "analytics:read", "verification:read",
            "admin:read", "admin:write",
            "openid", "offline_access"
        )
    }
}

/**
 * Application usage statistics
 */
@Serializable
data class UsageStats(
    val totalTokensIssued: Long = 0,
    val activeTokens: Long = 0,
    val totalUsers: Long = 0,
    val lastTokenIssuedAt: String? = null
)

/**
 * OAuth client registration request
 */
@Serializable
data class OAuthClientRequest(
    val name: String,
    val description: String? = null,
    val redirectUris: List<String>,
    val applicationType: ApplicationType = ApplicationType.WEB,
    val scopes: List<String> = OAuthClient.DEFAULT_SCOPES,
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val termsUrl: String? = null,
    val privacyUrl: String? = null
)

/**
 * OAuth client response (safe for API)
 */
@Serializable
data class OAuthClientResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val clientId: String,
    val clientSecret: String? = null, // Only returned on creation
    val redirectUris: List<String>,
    val allowedScopes: List<String>,
    val applicationType: ApplicationType,
    val isActive: Boolean,
    val isTrusted: Boolean,
    val isPublic: Boolean,
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val createdAt: String,
    val usageStats: UsageStats
)

@Serializable
enum class ApplicationType {
    WEB,
    NATIVE,
    SERVICE, // Server-to-server
    SPA      // Single Page Application
}

/**
 * OAuth authorization request
 */
@Serializable
data class AuthorizeRequest(
    val responseType: String = "code",
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>,
    val state: String? = null,
    val codeChallenge: String? = null, // PKCE
    val codeChallengeMethod: String? = null,
    val prompt: String? = null, // none, login, consent, select_account
    val maxAge: Int? = null,
    val loginHint: String? = null
)

/**
 * OAuth authorization response
 */
@Serializable
data class AuthorizeResponse(
    val success: Boolean,
    val authorizationCode: String? = null,
    val redirectUri: String? = null,
    val state: String? = null,
    val expiresIn: Int? = null,
    val error: String? = null,
    val errorDescription: String? = null,
    val errorCode: OAuthError? = null
)

/**
 * OAuth token exchange request
 */
@Serializable
data class TokenRequest(
    val grantType: String,
    val code: String? = null, // For authorization_code grant
    val redirectUri: String? = null,
    val clientId: String,
    val clientSecret: String? = null,
    val refreshToken: String? = null, // For refresh_token grant
    val username: String? = null, // For password grant
    val password: String? = null,
    val scopes: List<String>? = null,
    val codeVerifier: String? = null // PKCE
)

/**
 * OAuth token response
 */
@Serializable
data class TokenResponse(
    val success: Boolean,
    val accessToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresIn: Int? = null,
    val refreshToken: String? = null,
    val scopes: List<String>? = null,
    val idToken: String? = null, // For OpenID Connect
    val error: String? = null,
    val errorDescription: String? = null,
    val errorCode: OAuthError? = null
)

/**
 * Token introspection request
 */
@Serializable
data class IntrospectRequest(
    val token: String,
    val tokenTypeHint: String? = null // access_token or refresh_token
)

/**
 * Token introspection response
 */
@Serializable
data class IntrospectResponse(
    val active: Boolean,
    val clientId: String? = null,
    val username: String? = null,
    val scopes: List<String>? = null,
    val tokenType: String? = null,
    val exp: Long? = null, // Expiration time (Unix timestamp)
    val iat: Long? = null, // Issued at time
    val sub: String? = null, // Subject (user ID)
    val aud: String? = null, // Audience
    val iss: String? = null  // Issuer
)

/**
 * Token revocation request
 */
@Serializable
data class RevokeRequest(
    val token: String,
    val tokenTypeHint: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null
)

/**
 * Client credentials for authentication
 */
@Serializable
data class ClientCredentials(
    val clientId: String,
    val clientSecret: String
)

/**
 * OAuth consent screen information
 */
@Serializable
data class ConsentInfo(
    val clientId: String,
    val clientName: String,
    val clientDescription: String? = null,
    val logoUrl: String? = null,
    val websiteUrl: String? = null,
    val requestedScopes: List<ScopeInfo>,
    val userInfo: UserSummary,
    val previouslyApproved: Boolean = false
)

@Serializable
data class ScopeInfo(
    val scope: String,
    val name: String,
    val description: String,
    val required: Boolean = false,
    val sensitive: Boolean = false
)

@Serializable
data class UserSummary(
    val id: String,
    val eid: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null
)

/**
 * OAuth consent decision
 */
@Serializable
data class ConsentDecision(
    val approved: Boolean,
    val approvedScopes: List<String> = emptyList(),
    val rememberDecision: Boolean = false
)

/**
 * OAuth error codes
 */
@Serializable
enum class OAuthError {
    INVALID_REQUEST,
    INVALID_CLIENT,
    INVALID_GRANT,
    UNAUTHORIZED_CLIENT,
    UNSUPPORTED_GRANT_TYPE,
    INVALID_SCOPE,
    ACCESS_DENIED,
    UNSUPPORTED_RESPONSE_TYPE,
    SERVER_ERROR,
    TEMPORARILY_UNAVAILABLE
}

/**
 * OAuth client management for admin
 */
@Serializable
data class AdminClientView(
    val client: OAuthClientResponse,
    val totalUsers: Long,
    val activeTokens: Long,
    val totalTokensIssued: Long,
    val lastActivity: String? = null,
    val securityScore: Int, // 0-100 based on security practices
    val warnings: List<String> = emptyList()
)

/**
 * OAuth analytics
 */
@Serializable
data class OAuthAnalytics(
    val totalClients: Int,
    val activeClients: Int,
    val totalTokensIssued: Long,
    val activeTokens: Long,
    val topClientsByUsage: List<ClientUsage>,
    val scopeUsageStats: Map<String, Long>,
    val errorRates: Map<OAuthError, Long>
)

@Serializable
data class ClientUsage(
    val clientId: String,
    val clientName: String,
    val tokenCount: Long,
    val userCount: Long,
    val lastUsed: String
)
