package com.entativa.id.routes.handle

import com.entativa.id.domain.model.*
import com.entativa.id.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Handle Validation Routes for Entativa ID
 * Handles username/handle validation, availability checks, and suggestions
 * 
 * @author Neo Qiss
 * @status Production-ready handle validation endpoints
 */
fun Route.handleValidationRoutes(
    handleValidationService: HandleValidationService
) {
    val logger = LoggerFactory.getLogger("HandleValidationRoutes")
    
    route("/handle") {
        
        /**
         * Check handle availability
         * GET /handle/check/{handle}
         */
        get("/check/{handle}") {
            try {
                val handle = call.parameters["handle"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                logger.info("🔍 Handle availability check: $handle")
                
                val result = handleValidationService.checkAvailability(handle)
                
                if (result.isSuccess) {
                    val availability = result.getOrThrow()
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "handle" to handle,
                        "available" to availability.isAvailable,
                        "validation" to mapOf(
                            "valid_format" to availability.isValidFormat,
                            "meets_requirements" to availability.meetsRequirements,
                            "not_reserved" to availability.isNotReserved,
                            "not_profane" to availability.isNotProfane
                        ),
                        "issues" to availability.issues,
                        "suggestions" to if (!availability.isAvailable) {
                            handleValidationService.generateSuggestions(handle, 5)
                        } else emptyList()
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Handle availability check failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "validation_failed",
                        "message" to (error?.message ?: "Handle validation failed")
                    ))
                }
                
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
         * Validate handle format and rules
         * POST /handle/validate
         */
        post("/validate") {
            try {
                logger.info("✅ Handle validation request received")
                
                val request = call.receive<HandleValidationRequest>()
                
                val result = handleValidationService.validateHandle(request.handle)
                
                if (result.isSuccess) {
                    val validation = result.getOrThrow()
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "success" to true,
                        "handle" to request.handle,
                        "valid" to validation.isValid,
                        "score" to validation.score,
                        "checks" to mapOf(
                            "length" to validation.checks.lengthCheck,
                            "format" to validation.checks.formatCheck,
                            "characters" to validation.checks.charactersCheck,
                            "profanity" to validation.checks.profanityCheck,
                            "reserved" to validation.checks.reservedCheck,
                            "availability" to validation.checks.availabilityCheck
                        ),
                        "issues" to validation.issues,
                        "recommendations" to validation.recommendations
                    ))
                } else {
                    val error = result.exceptionOrNull()
                    logger.warn("❌ Handle validation failed: ${error?.message}")
                    
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "validation_failed",
                        "message" to (error?.message ?: "Handle validation failed")
                    ))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Handle validation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle suggestions
         * POST /handle/suggestions
         */
        post("/suggestions") {
            try {
                logger.info("💡 Handle suggestions request received")
                
                val request = call.receive<HandleSuggestionsRequest>()
                
                val suggestions = handleValidationService.generateSuggestions(
                    baseHandle = request.baseHandle,
                    count = request.count ?: 10,
                    includeNumbers = request.includeNumbers ?: true,
                    includeVariations = request.includeVariations ?: true
                )
                
                // Check availability for all suggestions
                val suggestionsWithAvailability = suggestions.map { suggestion ->
                    val availability = handleValidationService.checkAvailability(suggestion)
                    mapOf(
                        "handle" to suggestion,
                        "available" to (availability.getOrNull()?.isAvailable ?: false),
                        "score" to handleValidationService.calculateHandleScore(suggestion)
                    )
                }.filter { it["available"] as Boolean } // Only return available suggestions
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "base_handle" to request.baseHandle,
                    "suggestions" to suggestionsWithAvailability,
                    "total_found" to suggestionsWithAvailability.size,
                    "generation_criteria" to mapOf(
                        "include_numbers" to request.includeNumbers,
                        "include_variations" to request.includeVariations,
                        "requested_count" to request.count
                    )
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
         * Batch validate multiple handles
         * POST /handle/batch-validate
         */
        post("/batch-validate") {
            try {
                logger.info("📋 Batch handle validation request received")
                
                val request = call.receive<BatchHandleValidationRequest>()
                
                if (request.handles.size > 50) { // Limit batch size
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "batch_size_exceeded",
                        "message" to "Maximum 50 handles can be validated at once"
                    ))
                    return@post
                }
                
                val results = request.handles.associateWith { handle ->
                    val availability = handleValidationService.checkAvailability(handle)
                    val validation = handleValidationService.validateHandle(handle)
                    
                    mapOf(
                        "available" to (availability.getOrNull()?.isAvailable ?: false),
                        "valid" to (validation.getOrNull()?.isValid ?: false),
                        "score" to (validation.getOrNull()?.score ?: 0),
                        "issues" to (availability.getOrNull()?.issues ?: emptyList<String>())
                    )
                }
                
                val summary = mapOf(
                    "total_checked" to request.handles.size,
                    "available_count" to results.values.count { it["available"] as Boolean },
                    "valid_count" to results.values.count { it["valid"] as Boolean },
                    "average_score" to results.values.map { it["score"] as Int }.average()
                )
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "results" to results,
                    "summary" to summary
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Batch handle validation endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle validation rules and requirements
         * GET /handle/rules
         */
        get("/rules") {
            try {
                logger.info("📋 Handle rules request")
                
                val rules = handleValidationService.getValidationRules()
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "rules" to rules,
                    "examples" to mapOf(
                        "valid" to listOf("john_doe", "alice123", "dev_master", "cool.user"),
                        "invalid" to listOf("a", "user@name", "very_long_username_that_exceeds_limits", "admin")
                    ),
                    "tips" to listOf(
                        "Use 3-30 characters",
                        "Start with a letter",
                        "Use letters, numbers, dots, and underscores only",
                        "Avoid consecutive special characters",
                        "Choose something memorable and professional"
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle rules endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Check if handle matches reserved patterns
         * GET /handle/reserved-check/{handle}
         */
        get("/reserved-check/{handle}") {
            try {
                val handle = call.parameters["handle"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_handle",
                        "message" to "Handle parameter is required"
                    )
                )
                
                logger.info("🔒 Reserved handle check: $handle")
                
                val isReserved = handleValidationService.isReservedHandle(handle)
                val reservationType = if (isReserved) {
                    handleValidationService.getReservationType(handle)
                } else null
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "handle" to handle,
                    "is_reserved" to isReserved,
                    "reservation_type" to reservationType,
                    "message" to if (isReserved) {
                        "This handle is reserved and cannot be used"
                    } else {
                        "Handle is not reserved"
                    }
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Reserved handle check endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle popularity and trends
         * GET /handle/trends
         */
        get("/trends") {
            try {
                logger.info("📈 Handle trends request")
                
                val trends = handleValidationService.getHandleTrends()
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "trends" to trends,
                    "popular_patterns" to listOf(
                        "firstname_lastname",
                        "firstname.lastname", 
                        "firstname123",
                        "profession_name"
                    ),
                    "recommendations" to listOf(
                        "Avoid overly common patterns",
                        "Consider your professional brand",
                        "Keep it simple and memorable",
                        "Make it consistent across platforms"
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle trends endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
}
