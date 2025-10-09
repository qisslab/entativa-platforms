package com.entativa.id.routes

import com.entativa.id.domain.model.*
import com.entativa.id.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Authentication Routes for Entativa ID API
 * Handles user registration, login, token refresh, and logout
 * 
 * @author Neo Qiss
 * @status Production-ready REST API endpoints
 */
fun Route.authRoutes(
    userService: UserService,
    authService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger("AuthRoutes")
    
    route("/auth") {
        
        /**
         * Register new user
         * POST /auth/register
         */
        post("/register") {
            try {
                logger.info("📝 Registration request received")
                
                val request = call.receive<CreateUserRequest>()
                val ipAddress = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"]
                    ?: call.request.origin.remoteHost
                
                // Add IP address to request
                val enrichedRequest = request.copy(
                    ipAddress = ipAddress,
                    userAgent = call.request.headers["User-Agent"]
                )
                
                val result = userService.createUser(enrichedRequest)
                
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    logger.info("✅ User registered successfully: ${request.eid}")
                    
                    call.respond(HttpStatusCode.Created, mapOf(
                        "success" to true,
                        "message" to "User registered successfully",
                        "user" to user,
                        "next_steps" to listOf(
                            "Verify your email address",
                            "Complete your profile",
                            "Enable two-factor authentication"
                        )
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Registration failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "registration_failed",
                        "message" to (error?.message ?: "Registration failed"),
                        "details" to when (error) {
                            is IllegalArgumentException -> "validation_error"
                            is SecurityException -> "security_error"
                            else -> "server_error"
                        }
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Registration endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * User login
         * POST /auth/login
         */
        post("/login") {
            try {
                logger.info("🔐 Login request received")
                
                val request = call.receive<LoginRequest>()
                val ipAddress = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"]
                    ?: call.request.origin.remoteHost
                
                val authResult = userService.authenticateUser(
                    email = request.email,
                    password = request.password,
                    ipAddress = ipAddress
                )
                
                if (authResult.isSuccess) {
                    val user = authResult.getOrThrow()
                    
                    // Generate tokens
                    val tokens = authService.generateTokens(
                        user = user,
                        clientId = request.clientId,
                        ipAddress = ipAddress
                    )
                    
                    logger.info("✅ User logged in successfully: ${user.eid}")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Login successful",
                        "tokens" to tokens,
                        "user_summary" to tokens.user
                    ))
                } else {
                    val error = authResult.exceptionOrNull()
                    logger.warn("❌ Login failed: ${error?.message}")
                    
                    val statusCode = when (error) {
                        is SecurityException -> HttpStatusCode.Unauthorized
                        else -> HttpStatusCode.BadRequest
                    }
                    
                    call.respond(statusCode, mapOf(
                        "success" to false,
                        "error" to "authentication_failed",
                        "message" to (error?.message ?: "Authentication failed")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Login endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Refresh access token
         * POST /auth/refresh
         */
        post("/refresh") {
            try {
                logger.info("🔄 Token refresh request received")
                
                val request = call.receive<RefreshTokenRequest>()
                val ipAddress = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"]
                    ?: call.request.origin.remoteHost
                
                val result = authService.refreshTokens(request.refreshToken, ipAddress)
                
                if (result.isSuccess) {
                    val tokens = result.getOrThrow()
                    logger.info("✅ Tokens refreshed successfully")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Tokens refreshed successfully",
                        "tokens" to tokens
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Token refresh failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "refresh_failed",
                        "message" to (error?.message ?: "Token refresh failed")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Token refresh endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Logout user (revoke tokens)
         * POST /auth/logout
         */
        post("/logout") {
            try {
                logger.info("🚪 Logout request received")
                
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Authorization token required"
                    ))
                    return@post
                }
                
                val token = authHeader.substring(7)
                val tokenValidation = authService.validateAccessToken(token)
                
                if (tokenValidation.isSuccess) {
                    val claims = tokenValidation.getOrThrow()
                    
                    // Get request body (optional)
                    val requestBody = try {
                        call.receive<LogoutRequest>()
                    } catch (e: Exception) {
                        LogoutRequest(revokeAllSessions = false)
                    }
                    
                    val result = if (requestBody.revokeAllSessions) {
                        // Revoke all user sessions
                        authService.revokeAllTokens(claims.sub)
                    } else {
                        // Revoke only current session
                        authService.revokeSession(claims.sessionId)
                    }
                    
                    if (result.isSuccess) {
                        logger.info("✅ User logged out successfully: ${claims.eid}")
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to if (requestBody.revokeAllSessions) {
                                "All sessions terminated successfully"
                            } else {
                                "Logged out successfully"
                            }
                        ))
                    } else {
                        logger.error("❌ Logout failed: ${result.exceptionOrNull()?.message}")
                        
                        call.respond(HttpStatusCode.InternalServerError, mapOf(
                            "success" to false,
                            "error" to "logout_failed",
                            "message" to "Failed to logout"
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Logout endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get current user sessions
         * GET /auth/sessions
         */
        get("/sessions") {
            try {
                logger.info("📋 Sessions request received")
                
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Authorization token required"
                    ))
                    return@get
                }
                
                val token = authHeader.substring(7)
                val tokenValidation = authService.validateAccessToken(token)
                
                if (tokenValidation.isSuccess) {
                    val claims = tokenValidation.getOrThrow()
                    val sessions = authService.getUserSessions(claims.sub)
                    
                    logger.info("✅ Sessions retrieved for user: ${claims.eid}")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "sessions" to sessions,
                        "total_sessions" to sessions.size,
                        "current_session" to claims.sessionId
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Sessions endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Revoke specific session
         * DELETE /auth/sessions/{sessionId}
         */
        delete("/sessions/{sessionId}") {
            try {
                val sessionId = call.parameters["sessionId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_session_id",
                        "message" to "Session ID is required"
                    )
                )
                
                logger.info("🗑️ Session revocation request: $sessionId")
                
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Authorization token required"
                    ))
                    return@delete
                }
                
                val token = authHeader.substring(7)
                val tokenValidation = authService.validateAccessToken(token)
                
                if (tokenValidation.isSuccess) {
                    val claims = tokenValidation.getOrThrow()
                    val result = authService.revokeSession(sessionId)
                    
                    if (result.isSuccess) {
                        logger.info("✅ Session revoked: $sessionId")
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to "Session revoked successfully"
                        ))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "revocation_failed",
                            "message" to "Failed to revoke session"
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Session revocation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Change password
         * POST /auth/change-password
         */
        post("/change-password") {
            try {
                logger.info("🔑 Password change request received")
                
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Authorization token required"
                    ))
                    return@post
                }
                
                val token = authHeader.substring(7)
                val tokenValidation = authService.validateAccessToken(token)
                
                if (tokenValidation.isSuccess) {
                    val claims = tokenValidation.getOrThrow()
                    val request = call.receive<ChangePasswordRequest>()
                    
                    val result = userService.changePassword(
                        userId = claims.sub,
                        currentPassword = request.currentPassword,
                        newPassword = request.newPassword
                    )
                    
                    if (result.isSuccess) {
                        logger.info("✅ Password changed for user: ${claims.eid}")
                        
                        // Optionally revoke all other sessions for security
                        if (request.revokeOtherSessions) {
                            authService.revokeAllTokens(claims.sub)
                        }
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to "Password changed successfully",
                            "sessions_revoked" to request.revokeOtherSessions
                        ))
                    } else {
                        val error = result.exceptionOrNull()
                        
                        val statusCode = when (error) {
                            is SecurityException -> HttpStatusCode.Unauthorized
                            is IllegalArgumentException -> HttpStatusCode.BadRequest
                            else -> HttpStatusCode.InternalServerError
                        }
                        
                        call.respond(statusCode, mapOf(
                            "success" to false,
                            "error" to "password_change_failed",
                            "message" to (error?.message ?: "Failed to change password")
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Password change endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
}