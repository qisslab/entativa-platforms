package com.entativa.id.routes.handle

import com.entativa.id.domain.model.*
import com.entativa.id.service.*
import com.entativa.id.service.handle.HandleValidationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Handle Management Routes for Entativa ID API
 * Handles handle validation, availability checking, and reservations
 * 
 * @author Neo Qiss
 * @status Production-ready REST API endpoints
 */
fun Route.handleRoutes(
    handleValidationService: HandleValidationService,
    verificationService: VerificationService,
    authService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger("HandleRoutes")
    
    route("/handles") {
        
        /**
         * Check handle availability and validation
         * GET /handles/{handle}/check
         */
        get("/{handle}/check") {
            try {
                val handle = call.parameters["handle"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                logger.info("🔍 Handle check request: $handle")
                
                val validation = handleValidationService.validateHandle(handle)
                
                logger.info("✅ Handle check completed: $handle - Valid: ${validation.isValid}, Available: ${validation.isAvailable}")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "handle" to validation.handle,
                    "validation" to mapOf(
                        "is_valid" to validation.isValid,
                        "is_available" to validation.isAvailable,
                        "quality_score" to validation.qualityScore,
                        "estimated_availability" to validation.estimatedAvailability
                    ),
                    "errors" to validation.errors,
                    "warnings" to validation.warnings,
                    "suggestions" to validation.suggestions,
                    "protection_info" to validation.protectionInfo?.let { protection ->
                        mapOf(
                            "is_protected" to protection.isProtected,
                            "protection_type" to protection.protectionType?.name,
                            "category" to protection.category?.name,
                            "reason" to protection.reason,
                            "requires_verification" to protection.requiresVerification,
                            "similarity_score" to protection.similarityScore
                        )
                    }
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle check endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle suggestions
         * GET /handles/{handle}/suggestions
         */
        get("/{handle}/suggestions") {
            try {
                val handle = call.parameters["handle"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 10
                val maxCount = minOf(count, 25) // Limit to 25 suggestions
                
                logger.info("💡 Handle suggestions request: $handle (count: $maxCount)")
                
                val suggestions = handleValidationService.suggestAlternatives(handle, maxCount)
                
                logger.info("✅ Generated ${suggestions.size} suggestions for: $handle")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "base_handle" to handle,
                    "suggestions" to suggestions.map { suggestion ->
                        mapOf(
                            "handle" to suggestion.handle,
                            "quality_score" to suggestion.qualityScore,
                            "reason" to suggestion.reason,
                            "category" to suggestion.category
                        )
                    },
                    "total_suggestions" to suggestions.size
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle suggestions endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Reserve handle for verification
         * POST /handles/{handle}/reserve
         */
        post("/{handle}/reserve") {
            try {
                val handle = call.parameters["handle"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                logger.info("📝 Handle reservation request: $handle")
                
                // Authenticate user
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
                
                if (!tokenValidation.isSuccess) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                    return@post
                }
                
                val claims = tokenValidation.getOrThrow()
                val request = call.receive<ReserveHandleRequest>()
                
                // Check if handle can be reserved
                val canReserve = handleValidationService.canReserveHandle(handle, claims.sub)
                if (!canReserve) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "cannot_reserve",
                        "message" to "Handle cannot be reserved"
                    ))
                    return@post
                }
                
                val result = handleValidationService.reserveHandle(
                    handle = handle,
                    userId = claims.sub,
                    reason = request.reason
                )
                
                if (result.isSuccess) {
                    val reservation = result.getOrThrow()
                    logger.info("✅ Handle reserved: $handle for user: ${claims.eid}")
                    
                    call.respond(HttpStatusCode.Created, mapOf(
                        "success" to true,
                        "message" to "Handle reserved successfully",
                        "reservation" to mapOf(
                            "id" to reservation.id,
                            "handle" to reservation.handle,
                            "status" to reservation.status.name,
                            "created_at" to reservation.createdAt,
                            "expires_at" to reservation.expiresAt
                        ),
                        "next_steps" to listOf(
                            "Submit verification documents",
                            "Provide proof of identity or authority",
                            "Wait for admin review (3-7 business days)"
                        )
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Handle reservation failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "reservation_failed",
                        "message" to (error?.message ?: "Failed to reserve handle")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Handle reservation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Check handle protection details
         * GET /handles/{handle}/protection
         */
        get("/{handle}/protection") {
            try {
                val handle = call.parameters["handle"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                logger.info("🛡️ Handle protection check: $handle")
                
                val protection = verificationService.checkHandleProtection(handle)
                
                logger.info("✅ Protection check completed: $handle - Protected: ${protection.isProtected}")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "handle" to handle,
                    "protection" to mapOf(
                        "is_protected" to protection.isProtected,
                        "protection_type" to protection.protectionType?.name,
                        "category" to protection.category?.name,
                        "reason" to protection.reason,
                        "requires_verification" to protection.requiresVerification,
                        "similarity_score" to protection.similarityScore,
                        "suggested_alternatives" to protection.suggestedAlternatives
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle protection endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Bulk handle check
         * POST /handles/check-bulk
         */
        post("/check-bulk") {
            try {
                logger.info("📋 Bulk handle check request")
                
                val request = call.receive<BulkHandleCheckRequest>()
                
                if (request.handles.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "empty_handles",
                        "message" to "At least one handle is required"
                    ))
                    return@post
                }
                
                if (request.handles.size > 50) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "too_many_handles",
                        "message" to "Maximum 50 handles allowed per request"
                    ))
                    return@post
                }
                
                val results = mutableMapOf<String, Map<String, Any>>()
                
                for (handle in request.handles) {
                    val validation = handleValidationService.validateHandle(handle)
                    results[handle] = mapOf(
                        "is_valid" to validation.isValid,
                        "is_available" to validation.isAvailable,
                        "quality_score" to validation.qualityScore,
                        "errors" to validation.errors,
                        "warnings" to validation.warnings
                    )
                }
                
                logger.info("✅ Bulk check completed for ${request.handles.size} handles")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "total_checked" to request.handles.size,
                    "results" to results,
                    "summary" to mapOf(
                        "valid_count" to results.values.count { it["is_valid"] == true },
                        "available_count" to results.values.count { 
                            it["is_valid"] == true && it["is_available"] == true 
                        },
                        "protected_count" to results.values.count { 
                            it["is_valid"] == true && it["is_available"] == false 
                        }
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Bulk handle check endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle validation rules and guidelines
         * GET /handles/validation-rules
         */
        get("/validation-rules") {
            try {
                logger.info("📖 Handle validation rules request")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "validation_rules" to mapOf(
                        "length" to mapOf(
                            "minimum" to 3,
                            "maximum" to 30,
                            "recommended" to "6-12 characters"
                        ),
                        "format" to mapOf(
                            "allowed_characters" to "letters, numbers, dots, underscores, hyphens",
                            "must_start_with" to "letter or number",
                            "must_end_with" to "letter or number",
                            "pattern" to "^[a-zA-Z0-9][a-zA-Z0-9._-]{1,28}[a-zA-Z0-9]$"
                        ),
                        "restrictions" to mapOf(
                            "no_consecutive_special_chars" to true,
                            "no_mixed_special_chars" to true,
                            "reserved_handles" to "system, admin, support, etc.",
                            "prohibited_content" to "profanity, offensive terms"
                        ),
                        "quality_factors" to mapOf(
                            "pronounceability" to "easier to remember and share",
                            "vowel_distribution" to "improves readability",
                            "length_sweet_spot" to "6-12 characters optimal",
                            "letter_number_ratio" to "more letters than numbers preferred"
                        )
                    ),
                    "protection_system" to mapOf(
                        "celebrity_protection" to "handles similar to celebrities are protected",
                        "company_protection" to "major company names are reserved",
                        "similarity_threshold" to "85% similarity triggers protection",
                        "verification_required" to "protected handles require verification"
                    ),
                    "tips" to listOf(
                        "Choose a memorable and pronounceable handle",
                        "Avoid excessive numbers or special characters",
                        "Consider your brand or identity",
                        "Check suggestions if your preferred handle is taken",
                        "Reserve protected handles through verification process"
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Validation rules endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
}