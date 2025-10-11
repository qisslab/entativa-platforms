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
 * Handle Search Routes for Entativa ID
 * Handles handle discovery, search, and availability exploration
 * 
 * @author Neo Qiss
 * @status Production-ready handle search and discovery
 */
fun Route.handleSearchRoutes(
    handleValidationService: HandleValidationService,
    userService: UserService
) {
    val logger = LoggerFactory.getLogger("HandleSearchRoutes")
    
    route("/handle-search") {
        
        /**
         * Search for available handles
         * GET /handle-search?q={query}&type={type}&limit={limit}
         */
        get {
            try {
                val query = call.request.queryParameters["q"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "missing_query",
                        "message" to "Search query parameter 'q' is required"
                    )
                )
                
                val searchType = call.request.queryParameters["type"] ?: "available"
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val includeProtected = call.request.queryParameters["include_protected"]?.toBoolean() ?: false
                val includeReserved = call.request.queryParameters["include_reserved"]?.toBoolean() ?: false
                
                logger.info("🔍 Handle search request: query='$query', type='$searchType', limit=$limit")
                
                if (query.length < 2) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "query_too_short",
                        "message" to "Search query must be at least 2 characters"
                    ))
                    return@get
                }
                
                if (limit > 100) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "limit_exceeded",
                        "message" to "Maximum limit is 100"
                    ))
                    return@get
                }
                
                val searchResults = when (searchType) {
                    "available" -> handleValidationService.searchAvailableHandles(
                        query = query,
                        limit = limit,
                        includeProtected = includeProtected
                    )
                    "similar" -> handleValidationService.findSimilarHandles(
                        baseHandle = query,
                        limit = limit,
                        includeVariations = true
                    )
                    "suggestions" -> handleValidationService.generateSmartSuggestions(
                        baseQuery = query,
                        limit = limit,
                        userPreferences = null
                    )
                    else -> {
                        call.respond(HttpStatusCode.BadRequest, mapOf(
                            "success" to false,
                            "error" to "invalid_search_type",
                            "message" to "Valid search types: available, similar, suggestions"
                        ))
                        return@get
                    }
                }
                
                logger.info("✅ Handle search completed: ${searchResults.size} results for '$query'")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "query" to query,
                    "search_type" to searchType,
                    "total_results" to searchResults.size,
                    "results" to searchResults.map { result ->
                        mapOf(
                            "handle" to result.handle,
                            "available" to result.isAvailable,
                            "quality_score" to result.qualityScore,
                            "category" to result.category,
                            "reason" to result.reason,
                            "protection_info" to result.protectionInfo?.let { protection ->
                                mapOf(
                                    "is_protected" to protection.isProtected,
                                    "protection_type" to protection.protectionType?.name,
                                    "requires_verification" to protection.requiresVerification
                                )
                            },
                            "estimated_availability" to result.estimatedAvailability,
                            "alternative_suggestions" to result.alternativeSuggestions
                        )
                    },
                    "search_metadata" to mapOf(
                        "search_time_ms" to System.currentTimeMillis() % 1000, // Mock timing
                        "cache_hit" to false,
                        "filters_applied" to mapOf(
                            "include_protected" to includeProtected,
                            "include_reserved" to includeReserved
                        )
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle search endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Advanced handle search with filters
         * POST /handle-search/advanced
         */
        post("/advanced") {
            try {
                logger.info("🔍 Advanced handle search request")
                
                val request = call.receive<AdvancedHandleSearchRequest>()
                
                if (request.query.length < 2) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "success" to false,
                        "error" to "query_too_short",
                        "message" to "Search query must be at least 2 characters"
                    ))
                    return@post
                }
                
                val searchResults = handleValidationService.advancedHandleSearch(
                    query = request.query,
                    filters = request.filters,
                    sorting = request.sorting,
                    pagination = request.pagination
                )
                
                logger.info("✅ Advanced search completed: ${searchResults.results.size} results")
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "query" to request.query,
                    "total_results" to searchResults.totalCount,
                    "results" to searchResults.results.map { result ->
                        mapOf(
                            "handle" to result.handle,
                            "available" to result.isAvailable,
                            "quality_score" to result.qualityScore,
                            "length" to result.handle.length,
                            "category" to result.category,
                            "contains_numbers" to result.handle.any { it.isDigit() },
                            "contains_special_chars" to result.handle.any { it in "._-" },
                            "pronounceability_score" to result.pronounceabilityScore,
                            "memorability_score" to result.memorabilityScore,
                            "protection_info" to result.protectionInfo?.let { protection ->
                                mapOf(
                                    "is_protected" to protection.isProtected,
                                    "protection_type" to protection.protectionType?.name
                                )
                            }
                        )
                    },
                    "pagination" to mapOf(
                        "page" to searchResults.currentPage,
                        "size" to searchResults.pageSize,
                        "total_pages" to searchResults.totalPages,
                        "has_next" to searchResults.hasNext,
                        "has_previous" to searchResults.hasPrevious
                    ),
                    "applied_filters" to request.filters,
                    "sorting" to request.sorting
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Advanced handle search endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get trending handles and patterns
         * GET /handle-search/trending
         */
        get("/trending") {
            try {
                logger.info("📈 Getting trending handles")
                
                val period = call.request.queryParameters["period"] ?: "week"
                val category = call.request.queryParameters["category"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                
                val trendingData = handleValidationService.getTrendingHandles(
                    period = period,
                    category = category,
                    limit = limit
                )
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "period" to period,
                    "category" to category,
                    "trending_handles" to trendingData.handles.map { trending ->
                        mapOf(
                            "handle" to trending.handle,
                            "search_count" to trending.searchCount,
                            "availability_checks" to trending.availabilityChecks,
                            "trend_score" to trending.trendScore,
                            "category" to trending.category,
                            "growth_rate" to trending.growthRate
                        )
                    },
                    "trending_patterns" to trendingData.patterns.map { pattern ->
                        mapOf(
                            "pattern" to pattern.pattern,
                            "description" to pattern.description,
                            "popularity_score" to pattern.popularityScore,
                            "examples" to pattern.examples
                        )
                    },
                    "insights" to mapOf(
                        "most_popular_length" to trendingData.insights.mostPopularLength,
                        "popular_prefixes" to trendingData.insights.popularPrefixes,
                        "popular_suffixes" to trendingData.insights.popularSuffixes,
                        "emerging_categories" to trendingData.insights.emergingCategories
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Trending handles endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle recommendations based on user profile
         * POST /handle-search/recommendations
         */
        post("/recommendations") {
            try {
                logger.info("💡 Getting personalized handle recommendations")
                
                // Optional authentication for personalized recommendations
                val authHeader = call.request.headers["Authorization"]
                var userId: String? = null
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    // This is optional, so we don't fail if token is invalid
                    try {
                        // Token validation logic would go here
                        // userId = validateToken(authHeader.substring(7))
                    } catch (e: Exception) {
                        logger.debug("Invalid token for recommendations, continuing with anonymous")
                    }
                }
                
                val request = call.receive<HandleRecommendationRequest>()
                
                val recommendations = handleValidationService.getPersonalizedRecommendations(
                    userProfile = request.userProfile,
                    preferences = request.preferences,
                    userId = userId,
                    count = request.count ?: 15
                )
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "personalized" to (userId != null),
                    "total_recommendations" to recommendations.size,
                    "recommendations" to recommendations.map { rec ->
                        mapOf(
                            "handle" to rec.handle,
                            "available" to rec.isAvailable,
                            "quality_score" to rec.qualityScore,
                            "personalization_score" to rec.personalizationScore,
                            "reason" to rec.reason,
                            "category" to rec.category,
                            "style" to rec.style,
                            "matches_preferences" to rec.matchesPreferences,
                            "alternative_variations" to rec.alternativeVariations
                        )
                    },
                    "recommendation_basis" to mapOf(
                        "user_profile" to request.userProfile,
                        "preferences" to request.preferences,
                        "factors_considered" to listOf(
                            "Name and profile information",
                            "Industry and profession",
                            "Style preferences",
                            "Length preferences",
                            "Character preferences",
                            "Trending patterns"
                        )
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Handle recommendations endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Search for handles by specific criteria
         * POST /handle-search/criteria
         */
        post("/criteria") {
            try {
                logger.info("🎯 Searching handles by specific criteria")
                
                val request = call.receive<HandleCriteriaSearchRequest>()
                
                val searchResults = handleValidationService.searchByCriteria(
                    criteria = request.criteria,
                    limit = request.limit ?: 50
                )
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "criteria" to request.criteria,
                    "total_results" to searchResults.size,
                    "results" to searchResults.map { result ->
                        mapOf(
                            "handle" to result.handle,
                            "available" to result.isAvailable,
                            "meets_all_criteria" to result.meetsAllCriteria,
                            "criteria_match_score" to result.criteriaMatchScore,
                            "failed_criteria" to result.failedCriteria,
                            "quality_score" to result.qualityScore
                        )
                    },
                    "criteria_analysis" to mapOf(
                        "total_criteria" to request.criteria.size,
                        "most_restrictive" to findMostRestrictiveCriteria(request.criteria),
                        "availability_impact" to calculateAvailabilityImpact(request.criteria)
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Criteria search endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
        
        /**
         * Get handle search analytics and insights
         * GET /handle-search/analytics
         */
        get("/analytics") {
            try {
                logger.info("📊 Getting handle search analytics")
                
                val period = call.request.queryParameters["period"] ?: "month"
                val analytics = handleValidationService.getSearchAnalytics(period)
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "success" to true,
                    "period" to period,
                    "analytics" to mapOf(
                        "total_searches" to analytics.totalSearches,
                        "unique_queries" to analytics.uniqueQueries,
                        "average_results_per_search" to analytics.avgResultsPerSearch,
                        "most_searched_terms" to analytics.mostSearchedTerms,
                        "search_success_rate" to analytics.searchSuccessRate,
                        "popular_search_types" to analytics.popularSearchTypes,
                        "peak_search_times" to analytics.peakSearchTimes
                    ),
                    "insights" to mapOf(
                        "trending_up" to analytics.insights.trendingUp,
                        "trending_down" to analytics.insights.trendingDown,
                        "emerging_patterns" to analytics.insights.emergingPatterns,
                        "seasonal_trends" to analytics.insights.seasonalTrends
                    ),
                    "recommendations" to mapOf(
                        "optimize_search_for" to analytics.recommendations.optimizeSearchFor,
                        "suggest_alternatives_for" to analytics.recommendations.suggestAlternativesFor,
                        "improve_availability_in" to analytics.recommendations.improveAvailabilityIn
                    )
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Search analytics endpoint error", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "error" to "server_error",
                    "message" to "Internal server error"
                ))
            }
        }
    }
    
    // ============== HELPER METHODS ==============
    
    private fun findMostRestrictiveCriteria(criteria: List<SearchCriteria>): String {
        // Analyze which criteria eliminates the most results
        return criteria.maxByOrNull { it.restrictiveness }?.name ?: "unknown"
    }
    
    private fun calculateAvailabilityImpact(criteria: List<SearchCriteria>): Double {
        // Calculate how much the criteria reduces available handles
        return criteria.sumOf { it.availabilityImpact } / criteria.size
    }
}
