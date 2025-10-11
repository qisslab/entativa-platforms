package com.entativa.id.routes.auth

import com.entativa.id.domain.model.*
import com.entativa.id.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Password Reset Routes for Entativa ID
 * Handles password reset flows with email verification and secure token validation
 * 
 * @author Neo Qiss
 * @status Production-ready password reset endpoints
 */
fun Route.passwordResetRoutes(
    userService: UserService,
    tokenService: TokenService,
    notificationService: EmailService
) {
    val logger = LoggerFactory.getLogger("PasswordResetRoutes")
    
    route("/password-reset") {
        
        /**
         * Request password reset
         * POST /auth/password-reset/request
         */
        post("/request") {
            try {
                logger.info("🔑 Password reset request received")
                
                val request = call.receive<PasswordResetRequest>()
                val ipAddress = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"]
                    ?: call.request.origin.remoteHost
                
                // Find user by email
                val user = userService.findByEmail(request.email)
                
                if (user != null) {
                    // Generate password reset token
                    val tokenResult = tokenService.generatePasswordResetToken(
                        userId = user.id,
                        email = user.email
                    )
                    
                    if (tokenResult.isSuccess) {
                        val tokenResponse = tokenResult.getOrThrow()
                        
                        // Send password reset email
                        val emailResult = notificationService.sendPasswordResetEmail(
                            email = user.email,
                            userName = user.displayName,
                            resetUrl = tokenResponse.resetUrl,
                            expiresInMinutes = tokenResponse.expiresInSeconds / 60
                        )
                        
                        if (emailResult.isSuccess) {
                            logger.info("✅ Password reset email sent: ${user.email}")
                            
                            call.respond(HttpStatusCode.OK, mapOf(
                                "success" to true,
                                "message" to "Password reset instructions sent to your email",
                                "email" to user.email.replace(Regex("(?<=..).+(?=@)"), "*".repeat(5)),
                                "expires_in_minutes" to (tokenResponse.expiresInSeconds / 60)
                            ))
                        } else {
                            logger.error("❌ Failed to send password reset email: ${user.email}")
                            
                            call.respond(HttpStatusCode.InternalServerError, mapOf(
                                "success" to false,
                                "error" to "email_delivery_failed",
                                "message" to "Failed to send reset email. Please try again."
                            ))
                        }
                    } else {
                        val error = tokenResult.exceptionOrNull()
                        logger.warn("❌ Failed to generate reset token: ${error?.message}")
                        
                        call.respond(HttpStatusCode.TooManyRequests, mapOf(
                            "success" to false,
                            "error" to "rate_limit_exceeded",
                            "message" to (error?.message ?: "Too many reset requests. Please try again later.")
                        ))
                    }
                } else {
                    // For security, always respond with success even if email doesn't exist
                    logger.info("⚠️ Password reset requested for non-existent email: ${request.email}")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "If the email exists, password reset instructions will be sent",
                        "email" to request.email.replace(Regex("(?<=..).+(?=@)"), "*".repeat(5))
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Password reset request endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Verify reset token
         * POST /auth/password-reset/verify
         */
        post("/verify") {
            try {
                logger.info("🔍 Password reset token verification")
                
                val request = call.receive<VerifyResetTokenRequest>()
                
                val tokenResult = tokenService.verifyToken(
                    token = request.token,
                    type = TokenType.PASSWORD_RESET
                )
                
                if (tokenResult.isSuccess) {
                    val tokenInfo = tokenResult.getOrThrow()
                    
                    // Get user details
                    val user = userService.findById(tokenInfo.userId)
                    
                    if (user != null) {
                        logger.info("✅ Reset token verified for user: ${user.email}")
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to "Reset token is valid",
                            "token_id" to tokenInfo.tokenId,
                            "user_email" to user.email.replace(Regex("(?<=..).+(?=@)"), "*".repeat(5)),
                            "expires_at" to tokenInfo.expiresAt
                        ))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "user_not_found",
                            "message" to "User associated with token not found"
                        ))
                    }
                } else {
                    val error = tokenResult.exceptionOrNull()
                    logger.warn("❌ Invalid reset token: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to (error?.message ?: "Invalid or expired reset token")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Reset token verification endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Reset password with token
         * POST /auth/password-reset/confirm
         */
        post("/confirm") {
            try {
                logger.info("🔒 Password reset confirmation")
                
                val request = call.receive<ConfirmPasswordResetRequest>()
                val ipAddress = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"]
                    ?: call.request.origin.remoteHost
                
                // Verify token first
                val tokenResult = tokenService.verifyToken(
                    token = request.token,
                    type = TokenType.PASSWORD_RESET
                )
                
                if (tokenResult.isSuccess) {
                    val tokenInfo = tokenResult.getOrThrow()
                    
                    // Reset password
                    val resetResult = userService.resetPassword(
                        userId = tokenInfo.userId,
                        newPassword = request.newPassword,
                        ipAddress = ipAddress
                    )
                    
                    if (resetResult.isSuccess) {
                        // Use/consume the token
                        tokenService.useToken(tokenInfo.tokenId, "password_reset")
                        
                        // Get user for notification
                        val user = userService.findById(tokenInfo.userId)
                        
                        // Send confirmation email
                        if (user != null) {
                            notificationService.sendPasswordChangeNotification(
                                email = user.email,
                                userName = user.displayName,
                                ipAddress = ipAddress,
                                timestamp = java.time.Instant.now().toString()
                            )
                        }
                        
                        logger.info("✅ Password reset successfully for user: ${tokenInfo.userId}")
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to "Password reset successfully",
                            "next_steps" to listOf(
                                "Use your new password to sign in",
                                "Consider enabling two-factor authentication",
                                "Update your password manager"
                            )
                        ))
                    } else {
                        val error = resetResult.exceptionOrNull()
                        logger.error("❌ Password reset failed: ${error?.message}")
                        
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "password_reset_failed",
                            "message" to (error?.message ?: "Failed to reset password")
                        ))
                    }
                } else {
                    val error = tokenResult.exceptionOrNull()
                    logger.warn("❌ Invalid reset token for confirmation: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to (error?.message ?: "Invalid or expired reset token")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Password reset confirmation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get password reset status
         * GET /auth/password-reset/status/{token}
         */
        get("/status/{token}") {
            try {
                val token = call.parameters["token"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Reset token is required"
                    )
                )
                
                logger.info("📊 Password reset status check")
                
                val tokenResult = tokenService.verifyToken(
                    token = token,
                    type = TokenType.PASSWORD_RESET
                )
                
                if (tokenResult.isSuccess) {
                    val tokenInfo = tokenResult.getOrThrow()
                    val expiresAt = java.time.Instant.parse(tokenInfo.expiresAt)
                    val now = java.time.Instant.now()
                    val timeRemaining = java.time.Duration.between(now, expiresAt)
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "token_valid" to true,
                        "expires_at" to tokenInfo.expiresAt,
                        "time_remaining_minutes" to maxOf(0, timeRemaining.toMinutes()),
                        "user_email" to (tokenInfo.email?.replace(Regex("(?<=..).+(?=@)"), "*".repeat(5)) ?: "")
                    ))
                } else {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "token_valid" to false,
                        "message" to "Token is invalid or has expired"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Password reset status endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Cancel password reset (invalidate token)
         * DELETE /auth/password-reset/{token}
         */
        delete("/{token}") {
            try {
                val token = call.parameters["token"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Reset token is required"
                    )
                )
                
                logger.info("🚫 Password reset cancellation")
                
                // Verify token exists and get token info
                val tokenResult = tokenService.verifyToken(
                    token = token,
                    type = TokenType.PASSWORD_RESET
                )
                
                if (tokenResult.isSuccess) {
                    val tokenInfo = tokenResult.getOrThrow()
                    
                    // Use/consume the token to invalidate it
                    val cancelResult = tokenService.useToken(tokenInfo.tokenId, "reset_cancelled")
                    
                    if (cancelResult.isSuccess) {
                        logger.info("✅ Password reset cancelled for user: ${tokenInfo.userId}")
                        
                        call.respond(HttpStatusCode.OK, mapOf(
                            "success" to true,
                            "message" to "Password reset request cancelled successfully"
                        ))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf(
                            "success" to false,
                            "error" to "cancellation_failed",
                            "message" to "Failed to cancel reset request"
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired reset token"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Password reset cancellation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
}
