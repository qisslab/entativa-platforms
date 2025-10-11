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
 * Handle Reservation Routes for Entativa ID
 * Handles protected handle reservations, verification processes, and appeals
 * 
 * @author Neo Qiss
 * @status Production-ready handle reservation system
 */
fun Route.handleReservationRoutes(
    handleValidationService: HandleValidationService,
    verificationService: VerificationService,
    authService: AuthenticationService
) {
    val logger = LoggerFactory.getLogger("HandleReservationRoutes")
    
    route("/handle-reservations") {
        
        /**
         * Submit handle reservation request
         * POST /handle-reservations
         */
        post {
            try {
                logger.info("📝 Handle reservation request received")
                
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
                val request = call.receive<HandleReservationRequest>()
                
                // Validate handle first
                val handleValidation = handleValidationService.validateHandle(request.handle)
                if (!handleValidation.isValid) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "invalid_handle",
                        "message" to "Handle does not meet validation requirements",
                        "validation_errors" to handleValidation.errors
                    ))
                    return@post
                }
                
                // Check if handle requires protection verification
                val protection = verificationService.checkHandleProtection(request.handle)
                if (!protection.requiresVerification) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "no_protection_required",
                        "message" to "This handle does not require reservation"
                    ))
                    return@post
                }
                
                // Submit reservation request
                val result = handleValidationService.submitReservationRequest(
                    userId = claims.sub,
                    handle = request.handle,
                    justification = request.justification,
                    evidenceUrls = request.evidenceUrls,
                    contactInfo = request.contactInfo,
                    organizationInfo = request.organizationInfo
                )
                
                if (result.isSuccess) {
                    val reservation = result.getOrThrow()
                    logger.info("✅ Handle reservation submitted: ${request.handle} by ${claims.eid}")
                    
                    call.respond(HttpStatusCode.Created, mapOf(
                        "success" to true,
                        "message" to "Handle reservation request submitted successfully",
                        "reservation" to mapOf(
                            "id" to reservation.id,
                            "handle" to reservation.handle,
                            "status" to reservation.status.name,
                            "submitted_at" to reservation.submittedAt,
                            "estimated_review_time" to "3-7 business days"
                        ),
                        "next_steps" to listOf(
                            "Upload supporting documentation",
                            "Verify your identity",
                            "Wait for admin review",
                            "Check your email for updates"
                        ),
                        "requirements" to mapOf(
                            "identity_verification" to protection.requiresVerification,
                            "documentation_needed" to when (protection.protectionType) {
                                ProtectionType.CELEBRITY -> listOf("Government ID", "Public profile verification", "Media coverage")
                                ProtectionType.BRAND -> listOf("Business registration", "Trademark documentation", "Official website")
                                ProtectionType.ORGANIZATION -> listOf("Organization charter", "Official letterhead", "Contact verification")
                                else -> listOf("Government ID", "Supporting documentation")
                            }
                        )
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Handle reservation failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "reservation_failed",
                        "message" to (error?.message ?: "Failed to submit reservation request")
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
         * Get user's reservation requests
         * GET /handle-reservations
         */
        get {
            try {
                logger.info("📋 Getting user's handle reservations")
                
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
                
                if (!tokenValidation.isSuccess) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                    return@get
                }
                
                val claims = tokenValidation.getOrThrow()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                
                val reservations = handleValidationService.getUserReservations(claims.sub, page, size)
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "reservations" to reservations.data.map { reservation ->
                        mapOf(
                            "id" to reservation.id,
                            "handle" to reservation.handle,
                            "status" to reservation.status.name,
                            "submitted_at" to reservation.submittedAt,
                            "updated_at" to reservation.updatedAt,
                            "justification" to reservation.justification,
                            "admin_notes" to reservation.adminNotes,
                            "can_appeal" to reservation.canAppeal(),
                            "can_modify" to reservation.canModify()
                        )
                    },
                    "pagination" to mapOf(
                        "page" to page,
                        "size" to size,
                        "total" to reservations.total,
                        "total_pages" to reservations.totalPages
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Get reservations endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get specific reservation details
         * GET /handle-reservations/{reservationId}
         */
        get("/{reservationId}") {
            try {
                val reservationId = call.parameters["reservationId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_reservation_id",
                        "message" to "Reservation ID is required"
                    )
                )
                
                logger.info("📄 Getting reservation details: $reservationId")
                
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
                
                if (!tokenValidation.isSuccess) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                    return@get
                }
                
                val claims = tokenValidation.getOrThrow()
                val reservation = handleValidationService.getReservation(reservationId, claims.sub)
                
                if (reservation != null) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "reservation" to mapOf(
                            "id" to reservation.id,
                            "handle" to reservation.handle,
                            "status" to reservation.status.name,
                            "submitted_at" to reservation.submittedAt,
                            "updated_at" to reservation.updatedAt,
                            "justification" to reservation.justification,
                            "evidence_urls" to reservation.evidenceUrls,
                            "contact_info" to reservation.contactInfo,
                            "organization_info" to reservation.organizationInfo,
                            "admin_notes" to reservation.adminNotes,
                            "review_history" to reservation.reviewHistory,
                            "can_appeal" to reservation.canAppeal(),
                            "can_modify" to reservation.canModify(),
                            "expires_at" to reservation.expiresAt
                        )
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf(
                        "success" to false,
                        "error" to "reservation_not_found",
                        "message" to "Reservation not found or access denied"
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Get reservation details endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Update reservation request
         * PUT /handle-reservations/{reservationId}
         */
        put("/{reservationId}") {
            try {
                val reservationId = call.parameters["reservationId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_reservation_id",
                        "message" to "Reservation ID is required"
                    )
                )
                
                logger.info("✏️ Updating reservation: $reservationId")
                
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "missing_token",
                        "message" to "Authorization token required"
                    ))
                    return@put
                }
                
                val token = authHeader.substring(7)
                val tokenValidation = authService.validateAccessToken(token)
                
                if (!tokenValidation.isSuccess) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                    return@put
                }
                
                val claims = tokenValidation.getOrThrow()
                val updateRequest = call.receive<UpdateReservationRequest>()
                
                val result = handleValidationService.updateReservation(
                    reservationId = reservationId,
                    userId = claims.sub,
                    updates = updateRequest
                )
                
                if (result.isSuccess) {
                    val updatedReservation = result.getOrThrow()
                    logger.info("✅ Reservation updated: $reservationId")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Reservation updated successfully",
                        "reservation" to mapOf(
                            "id" to updatedReservation.id,
                            "handle" to updatedReservation.handle,
                            "status" to updatedReservation.status.name,
                            "updated_at" to updatedReservation.updatedAt
                        )
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Reservation update failed: ${error?.message}")
                    
                    val statusCode = when (error) {
                        is IllegalStateException -> HttpStatusCode.BadRequest
                        is SecurityException -> HttpStatusCode.Forbidden
                        else -> HttpStatusCode.InternalServerError
                    }
                    
                    call.respond(statusCode, mapOf(
                        "success" to false,
                        "error" to "update_failed",
                        "message" to (error?.message ?: "Failed to update reservation")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Update reservation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Submit appeal for rejected reservation
         * POST /handle-reservations/{reservationId}/appeal
         */
        post("/{reservationId}/appeal") {
            try {
                val reservationId = call.parameters["reservationId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_reservation_id",
                        "message" to "Reservation ID is required"
                    )
                )
                
                logger.info("⚖️ Appeal submitted for reservation: $reservationId")
                
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
                val appealRequest = call.receive<ReservationAppealRequest>()
                
                val result = handleValidationService.submitAppeal(
                    reservationId = reservationId,
                    userId = claims.sub,
                    appealReason = appealRequest.reason,
                    additionalEvidence = appealRequest.additionalEvidence,
                    statement = appealRequest.statement
                )
                
                if (result.isSuccess) {
                    val appeal = result.getOrThrow()
                    logger.info("✅ Appeal submitted: $reservationId")
                    
                    call.respond(HttpStatusCode.Created, mapOf(
                        "success" to true,
                        "message" to "Appeal submitted successfully",
                        "appeal" to mapOf(
                            "id" to appeal.id,
                            "reservation_id" to reservationId,
                            "status" to appeal.status.name,
                            "submitted_at" to appeal.submittedAt,
                            "estimated_review_time" to "5-10 business days"
                        ),
                        "next_steps" to listOf(
                            "Appeal will be reviewed by senior administrators",
                            "Additional documentation may be requested",
                            "You will be notified of the decision via email"
                        )
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Appeal submission failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "appeal_failed",
                        "message" to (error?.message ?: "Failed to submit appeal")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Submit appeal endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Cancel reservation request
         * DELETE /handle-reservations/{reservationId}
         */
        delete("/{reservationId}") {
            try {
                val reservationId = call.parameters["reservationId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_reservation_id",
                        "message" to "Reservation ID is required"
                    )
                )
                
                logger.info("🗑️ Cancelling reservation: $reservationId")
                
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
                
                if (!tokenValidation.isSuccess) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "success" to false,
                        "error" to "invalid_token",
                        "message" to "Invalid or expired token"
                    ))
                    return@delete
                }
                
                val claims = tokenValidation.getOrThrow()
                val result = handleValidationService.cancelReservation(reservationId, claims.sub)
                
                if (result.isSuccess) {
                    logger.info("✅ Reservation cancelled: $reservationId")
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "message" to "Reservation cancelled successfully"
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Reservation cancellation failed: ${error?.message}")
                    
                    val statusCode = when (error) {
                        is IllegalStateException -> HttpStatusCode.BadRequest
                        is SecurityException -> HttpStatusCode.Forbidden
                        else -> HttpStatusCode.InternalServerError
                    }
                    
                    call.respond(statusCode, mapOf(
                        "success" to false,
                        "error" to "cancellation_failed",
                        "message" to (error?.message ?: "Failed to cancel reservation")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Cancel reservation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get reservation guidelines and requirements
         * GET /handle-reservations/guidelines
         */
        get("/guidelines") {
            try {
                logger.info("📖 Getting reservation guidelines")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "guidelines" to mapOf(
                        "overview" to "Handle reservations protect celebrity names, brands, and organizations from impersonation",
                        "eligibility" to mapOf(
                            "celebrities" to "Public figures with verified social media presence or Wikipedia page",
                            "brands" to "Registered trademarks or established business entities",
                            "organizations" to "Non-profit organizations, government entities, educational institutions"
                        ),
                        "required_documentation" to mapOf(
                            "identity_verification" to listOf("Government-issued photo ID", "Proof of association with protected entity"),
                            "celebrity_verification" to listOf("Social media verification", "Media coverage", "Official website"),
                            "brand_verification" to listOf("Business registration", "Trademark certificate", "Official letterhead"),
                            "organization_verification" to listOf("Organization charter", "Tax-exempt status", "Official contact verification")
                        ),
                        "process" to listOf(
                            "Submit reservation request with justification",
                            "Upload supporting documentation",
                            "Identity verification (if required)",
                            "Administrative review (3-7 business days)",
                            "Decision notification via email",
                            "Appeal process available for rejections"
                        ),
                        "restrictions" to listOf(
                            "Maximum 3 active reservations per user",
                            "Reservations expire after 90 days if not claimed",
                            "False claims may result in account suspension",
                            "Appeals must be submitted within 30 days"
                        ),
                        "tips" to listOf(
                            "Provide clear justification for handle ownership",
                            "Include comprehensive supporting documentation",
                            "Ensure all information is accurate and verifiable",
                            "Respond promptly to admin requests for clarification"
                        )
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Get guidelines endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
}
