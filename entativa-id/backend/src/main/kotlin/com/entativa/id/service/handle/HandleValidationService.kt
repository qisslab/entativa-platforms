package com.entativa.id.service.handle

import com.entativa.id.config.*
import com.entativa.id.domain.model.*
import com.entativa.id.service.VerificationService
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handle Validation Service for Entativa ID
 * Validates handle availability, format, and protection rules
 * 
 * @author Neo Qiss
 * @status Production-ready with comprehensive validation
 */
class HandleValidationService(
    private val cacheManager: EntativaCacheManager,
    private val verificationService: VerificationService
) {
    
    private val logger = LoggerFactory.getLogger(HandleValidationService::class.java)
    
    companion object {
        private const val CACHE_TTL_SECONDS = 1800 // 30 minutes
        private const val MIN_HANDLE_LENGTH = 3
        private const val MAX_HANDLE_LENGTH = 30
        
        // Reserved system handles
        private val RESERVED_HANDLES = setOf(
            "admin", "root", "system", "entativa", "support", "help", "api", "www", "mail",
            "ftp", "localhost", "test", "dev", "staging", "prod", "production", "demo",
            "null", "undefined", "delete", "remove", "update", "create", "edit", "view",
            "login", "logout", "signup", "signin", "register", "auth", "oauth", "security",
            "settings", "config", "dashboard", "profile", "account", "user", "users",
            "public", "private", "internal", "external", "official", "verified", "premium"
        )
        
        // Prohibited patterns
        private val PROHIBITED_PATTERNS = listOf(
            ".*admin.*", ".*test.*", ".*fake.*", ".*spam.*", ".*bot.*", ".*null.*",
            ".*delete.*", ".*remove.*", ".*fuck.*", ".*shit.*", ".*damn.*", ".*porn.*",
            ".*sex.*", ".*drug.*", ".*kill.*", ".*die.*", ".*hate.*", ".*nazi.*",
            ".*hitler.*", ".*terrorist.*", ".*bomb.*", ".*weapon.*", ".*gun.*"
        )
    }
    
    /**
     * Comprehensive handle validation
     */
    suspend fun validateHandle(handle: String): HandleValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Validating handle: $handle")
                
                // Check cache first
                val cached = cacheManager.getCachedData<HandleValidationResult>("validation:$handle")
                if (cached != null) {
                    return@withContext cached
                }
                
                val errors = mutableListOf<String>()
                val warnings = mutableListOf<String>()
                val suggestions = mutableListOf<String>()
                
                val normalizedHandle = handle.lowercase().trim()
                
                // Basic format validation
                val formatValidation = validateHandleFormat(normalizedHandle)
                if (!formatValidation.isValid) {
                    errors.addAll(formatValidation.errors)
                }
                
                // Check if handle is reserved
                if (isReservedHandle(normalizedHandle)) {
                    errors.add("Handle is reserved by the system")
                    suggestions.addAll(generateSystemAlternatives(handle))
                }
                
                // Check prohibited patterns
                val prohibitedCheck = checkProhibitedPatterns(normalizedHandle)
                if (!prohibitedCheck.isValid) {
                    errors.addAll(prohibitedCheck.errors)
                    suggestions.addAll(generateCleanAlternatives(handle))
                }
                
                var isAvailable = true
                var protectionResult: HandleProtectionResult? = null
                
                // Only check availability if format is valid
                if (errors.isEmpty()) {
                    // Check if handle is already taken
                    val existingUser = transaction {
                        EntativaIdentitiesTable.select { EntativaIdentitiesTable.eid eq normalizedHandle }
                            .singleOrNull()
                    }
                    
                    if (existingUser != null) {
                        isAvailable = false
                        errors.add("Handle is already taken")
                        suggestions.addAll(generateAvailabilityAlternatives(handle))
                    } else {
                        // Check protection (celebrity/company/VIP)
                        protectionResult = verificationService.checkHandleProtection(normalizedHandle)
                        if (protectionResult.isProtected) {
                            isAvailable = false
                            errors.add(protectionResult.reason ?: "Handle is protected")
                            suggestions.addAll(protectionResult.suggestedAlternatives)
                        }
                    }
                }
                
                // Generate quality warnings
                val qualityWarnings = generateQualityWarnings(normalizedHandle)
                warnings.addAll(qualityWarnings)
                
                // Add improvement suggestions if needed
                if (warnings.isNotEmpty()) {
                    suggestions.addAll(generateImprovementSuggestions(handle))
                }
                
                val result = HandleValidationResult(
                    isValid = errors.isEmpty(),
                    isAvailable = isAvailable,
                    handle = normalizedHandle,
                    errors = errors,
                    warnings = warnings,
                    suggestions = suggestions.distinct().take(5),
                    protectionInfo = protectionResult,
                    qualityScore = calculateQualityScore(normalizedHandle),
                    estimatedAvailability = if (isAvailable) "immediately" else "requires verification"
                )
                
                // Cache the result
                cacheManager.cacheData("validation:$handle", result, CACHE_TTL_SECONDS)
                
                logger.debug("✅ Handle validation complete: $handle - Valid: ${result.isValid}, Available: ${result.isAvailable}")
                result
                
            } catch (e: Exception) {
                logger.error("❌ Handle validation failed: $handle", e)
                HandleValidationResult(
                    isValid = false,
                    isAvailable = false,
                    handle = handle,
                    errors = listOf("Validation service error"),
                    warnings = emptyList(),
                    suggestions = emptyList(),
                    protectionInfo = null,
                    qualityScore = 0,
                    estimatedAvailability = "unknown"
                )
            }
        }
    }
    
    /**
     * Suggest alternative handles
     */
    suspend fun suggestAlternatives(baseHandle: String, count: Int = 10): List<HandleSuggestion> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("💡 Generating handle suggestions for: $baseHandle")
                
                val suggestions = mutableListOf<HandleSuggestion>()
                val base = baseHandle.lowercase().trim()
                
                // Generate different types of alternatives
                val alternatives = mutableSetOf<String>()
                
                // Add numbers
                for (i in 1..99) {
                    alternatives.add("$base$i")
                    if (i <= 9) {
                        alternatives.add("${base}0$i")
                    }
                }
                
                // Add current year
                alternatives.add("${base}2024")
                alternatives.add("${base}24")
                
                // Add common suffixes
                val suffixes = listOf("_official", "_real", "_verified", "_eid", "_id", "_user", 
                                    "_pro", "_plus", "_premium", "_elite", "_vip")
                suffixes.forEach { suffix ->
                    alternatives.add("$base$suffix")
                }
                
                // Add prefixes
                val prefixes = listOf("real_", "official_", "verified_", "the_", "my_", "i_am_")
                prefixes.forEach { prefix ->
                    alternatives.add("$prefix$base")
                }
                
                // Add variations
                if (base.length > 5) {
                    alternatives.add(base.dropLast(1))
                }
                alternatives.add("${base}x")
                alternatives.add("${base}_")
                
                // Check availability for alternatives
                for (alternative in alternatives.take(50)) { // Limit to avoid too many DB calls
                    if (suggestions.size >= count) break
                    
                    val validation = validateHandle(alternative)
                    if (validation.isValid && validation.isAvailable) {
                        suggestions.add(HandleSuggestion(
                            handle = alternative,
                            qualityScore = validation.qualityScore,
                            reason = determineSuggestionReason(base, alternative),
                            category = determineSuggestionCategory(base, alternative)
                        ))
                    }
                }
                
                // Sort by quality score
                val sortedSuggestions = suggestions.sortedByDescending { it.qualityScore }.take(count)
                
                logger.debug("✅ Generated ${sortedSuggestions.size} handle suggestions for: $baseHandle")
                sortedSuggestions
                
            } catch (e: Exception) {
                logger.error("❌ Failed to generate handle suggestions: $baseHandle", e)
                emptyList()
            }
        }
    }
    
    /**
     * Check if handle can be reserved for future use
     */
    suspend fun canReserveHandle(handle: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val validation = validateHandle(handle)
                
                // Can reserve if valid format but currently protected
                validation.isValid && !validation.isAvailable && 
                validation.protectionInfo?.requiresVerification == true
                
            } catch (e: Exception) {
                logger.error("❌ Failed to check handle reservation: $handle", e)
                false
            }
        }
    }
    
    /**
     * Reserve handle for verification process
     */
    suspend fun reserveHandle(handle: String, userId: String, reason: String): Result<HandleReservation> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📝 Reserving handle: $handle for user: $userId")
                
                val canReserve = canReserveHandle(handle, userId)
                if (!canReserve) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Handle cannot be reserved")
                    )
                }
                
                val reservationId = UUID.randomUUID()
                val expiresAt = java.time.Instant.now().plusSeconds(30 * 24 * 3600) // 30 days
                
                transaction {
                    HandleReservationsTable.insert {
                        it[id] = reservationId
                        it[this.handle] = handle.lowercase()
                        it[identityId] = UUID.fromString(userId)
                        it[this.reason] = reason
                        it[status] = "pending"
                        it[createdAt] = java.time.Instant.now()
                        it[this.expiresAt] = expiresAt
                    }
                }
                
                val reservation = HandleReservation(
                    id = reservationId.toString(),
                    handle = handle.lowercase(),
                    userId = userId,
                    reason = reason,
                    status = HandleReservationStatus.PENDING,
                    createdAt = java.time.Instant.now().toString(),
                    expiresAt = expiresAt.toString()
                )
                
                logger.info("✅ Handle reserved: $handle")
                Result.success(reservation)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to reserve handle: $handle", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun validateHandleFormat(handle: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Length validation
        if (handle.length < MIN_HANDLE_LENGTH) {
            errors.add("Handle must be at least $MIN_HANDLE_LENGTH characters")
        }
        
        if (handle.length > MAX_HANDLE_LENGTH) {
            errors.add("Handle must be no more than $MAX_HANDLE_LENGTH characters")
        }
        
        // Character validation
        if (!handle.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]{1,28}[a-zA-Z0-9]$".toRegex())) {
            errors.add("Handle can only contain letters, numbers, dots, underscores, and hyphens")
            errors.add("Handle must start and end with a letter or number")
        }
        
        // Consecutive special characters
        if (handle.contains("..") || handle.contains("__") || handle.contains("--")) {
            errors.add("Handle cannot contain consecutive special characters")
        }
        
        // Mixed special characters
        if (handle.contains("._") || handle.contains("_-") || handle.contains("-.")) {
            errors.add("Handle cannot mix different special characters consecutively")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private fun isReservedHandle(handle: String): Boolean {
        return RESERVED_HANDLES.contains(handle)
    }
    
    private fun checkProhibitedPatterns(handle: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        for (pattern in PROHIBITED_PATTERNS) {
            if (handle.matches(pattern.toRegex())) {
                errors.add("Handle contains prohibited content")
                break
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private fun generateQualityWarnings(handle: String): List<String> {
        val warnings = mutableListOf<String>()
        
        // Too many numbers
        val numberCount = handle.count { it.isDigit() }
        if (numberCount > handle.length / 2) {
            warnings.add("Handle contains too many numbers")
        }
        
        // Too many special characters
        val specialCount = handle.count { it in "._-" }
        if (specialCount > handle.length / 3) {
            warnings.add("Handle contains too many special characters")
        }
        
        // All numbers
        if (handle.all { it.isDigit() }) {
            warnings.add("Handle should contain at least some letters")
        }
        
        // Hard to pronounce
        val vowelCount = handle.count { it in "aeiou" }
        if (vowelCount < handle.length / 4) {
            warnings.add("Handle might be difficult to pronounce")
        }
        
        // Too short but valid
        if (handle.length == MIN_HANDLE_LENGTH) {
            warnings.add("Longer handles are generally more memorable")
        }
        
        return warnings
    }
    
    private fun calculateQualityScore(handle: String): Int {
        var score = 50 // Base score
        
        // Length bonus (sweet spot is 6-12 characters)
        when (handle.length) {
            in 6..12 -> score += 20
            in 4..5, in 13..20 -> score += 10
            else -> score -= 10
        }
        
        // Letter-to-number ratio
        val letterCount = handle.count { it.isLetter() }
        val numberCount = handle.count { it.isDigit() }
        if (letterCount > numberCount) score += 15
        
        // Vowel distribution
        val vowelCount = handle.count { it in "aeiou" }
        if (vowelCount >= handle.length / 4) score += 10
        
        // Special character penalty
        val specialCount = handle.count { it in "._-" }
        if (specialCount <= 1) score += 5
        else score -= specialCount * 3
        
        // Pronounceability bonus
        if (isPronounceableHandle(handle)) score += 15
        
        // Common word bonus
        if (containsCommonWords(handle)) score += 10
        
        return maxOf(0, minOf(100, score))
    }
    
    private fun isPronounceableHandle(handle: String): Boolean {
        val vowels = "aeiou"
        var hasVowel = false
        var consecutiveConsonants = 0
        
        for (char in handle) {
            if (char.isLetter()) {
                if (char.lowercase() in vowels) {
                    hasVowel = true
                    consecutiveConsonants = 0
                } else {
                    consecutiveConsonants++
                    if (consecutiveConsonants > 3) return false
                }
            }
        }
        
        return hasVowel
    }
    
    private fun containsCommonWords(handle: String): Boolean {
        val commonWords = setOf(
            "app", "web", "tech", "dev", "code", "data", "user", "play", "game", "music",
            "photo", "video", "news", "blog", "shop", "store", "book", "art", "design"
        )
        
        return commonWords.any { handle.contains(it) }
    }
    
    private fun generateSystemAlternatives(base: String): List<String> {
        return listOf(
            "${base}_user",
            "${base}_id",
            "my_$base",
            "${base}2024",
            "${base}_official"
        )
    }
    
    private fun generateCleanAlternatives(base: String): List<String> {
        val cleaned = base.replace(Regex("[^a-zA-Z0-9]"), "")
        return listOf(
            cleaned,
            "${cleaned}_clean",
            "${cleaned}_good",
            "${cleaned}_nice",
            "${cleaned}_user"
        )
    }
    
    private fun generateAvailabilityAlternatives(base: String): List<String> {
        return listOf(
            "${base}1",
            "${base}_",
            "${base}x",
            "${base}2024",
            "${base}_eid"
        )
    }
    
    private fun generateImprovementSuggestions(base: String): List<String> {
        return listOf(
            "${base.take(8)}_user",
            "${base}_pro",
            "${base.filter { it.isLetter() }}",
            "${base}_official",
            "${base}_verified"
        )
    }
    
    private fun determineSuggestionReason(base: String, suggestion: String): String {
        return when {
            suggestion.contains(base) && suggestion.length > base.length -> "Added suffix for uniqueness"
            suggestion.startsWith(base) -> "Added number for availability"
            suggestion.endsWith(base) -> "Added prefix for clarity"
            else -> "Alternative variation"
        }
    }
    
    private fun determineSuggestionCategory(base: String, suggestion: String): String {
        return when {
            suggestion.matches(".*\\d+$".toRegex()) -> "numbered"
            suggestion.contains("_") -> "descriptive"
            suggestion.contains("official") || suggestion.contains("real") -> "authoritative"
            suggestion.length > base.length + 3 -> "extended"
            else -> "variation"
        }
    }
    
    private data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}
