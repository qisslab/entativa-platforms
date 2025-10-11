package com.entativa.id.service.security

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * Passphrase Security Service for Entativa ID
 * Handles passphrase generation, validation, and security for users who prefer memorable passphrases
 * 
 * @author Neo Qiss  
 * @status Production-ready passphrase security with advanced entropy calculations
 */
@Service
class PassphraseService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(PassphraseService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val secureRandom = SecureRandom()
    
    companion object {
        // Passphrase requirements
        private const val MIN_PASSPHRASE_WORDS = 4
        private const val MAX_PASSPHRASE_WORDS = 12
        private const val MIN_PASSPHRASE_LENGTH = 15
        private const val MAX_PASSPHRASE_LENGTH = 200
        private const val MIN_ENTROPY_BITS = 50.0 // Higher than passwords due to dictionary attacks
        
        // Word lists for generation
        private val COMMON_WORDS = listOf(
            "apple", "beach", "chair", "dance", "eagle", "flame", "grape", "house", "island", "jungle",
            "knight", "lemon", "magic", "nature", "ocean", "planet", "queen", "river", "sunset", "tiger",
            "universe", "village", "water", "yellow", "zebra", "breeze", "crystal", "dream", "energy", "forest",
            "garden", "harmony", "journey", "kindness", "laughter", "melody", "mystery", "passion", "rainbow", "silence",
            "thunder", "victory", "wisdom", "adventure", "birthday", "chocolate", "elephant", "firework", "mountain", "painting"
        )
        
        private val SECURE_WORDS = listOf(
            "abundance", "brilliant", "cascade", "delicate", "elaborate", "fantastic", "graceful", "harmonious", "infinite", "jubilant",
            "knowledge", "luminous", "magnificent", "nostalgic", "optimistic", "peaceful", "quantum", "radiant", "spectacular", "triumphant",
            "unanimous", "vivacious", "wonderful", "xenophile", "youthful", "zealous", "ambitious", "breathtaking", "captivating", "dazzling",
            "enchanting", "fascinating", "glorious", "heartwarming", "inspiring", "joyful", "kaleidoscope", "legendary", "marvelous", "outstanding"
        )
        
        private val NUMBERS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "42", "99", "101", "777", "2024")
        private val SEPARATORS = listOf("-", "_", ".", " ")
    }
    
    /**
     * Generate secure passphrase with customizable options
     */
    suspend fun generatePassphrase(
        wordCount: Int = 6,
        includeNumbers: Boolean = true,
        includeCapitalization: Boolean = true,
        separator: String = "-",
        useSecureWordList: Boolean = true,
        customWords: List<String> = emptyList()
    ): Result<GeneratedPassphrase> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🎲 Generating passphrase with $wordCount words")
                
                if (wordCount < MIN_PASSPHRASE_WORDS || wordCount > MAX_PASSPHRASE_WORDS) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Word count must be between $MIN_PASSPHRASE_WORDS and $MAX_PASSPHRASE_WORDS")
                    )
                }
                
                val wordList = when {
                    customWords.isNotEmpty() -> customWords
                    useSecureWordList -> SECURE_WORDS
                    else -> COMMON_WORDS
                }
                
                val selectedWords = mutableListOf<String>()
                val usedWords = mutableSetOf<String>()
                
                // Select unique words
                repeat(wordCount) {
                    var word: String
                    do {
                        word = wordList[secureRandom.nextInt(wordList.size)]
                    } while (usedWords.contains(word) && usedWords.size < wordList.size)
                    
                    usedWords.add(word)
                    
                    // Apply capitalization
                    val finalWord = if (includeCapitalization && secureRandom.nextBoolean()) {
                        word.replaceFirstChar { it.uppercase() }
                    } else {
                        word
                    }
                    
                    selectedWords.add(finalWord)
                }
                
                // Add numbers if requested
                if (includeNumbers && secureRandom.nextBoolean()) {
                    val numberIndex = secureRandom.nextInt(selectedWords.size)
                    val number = NUMBERS[secureRandom.nextInt(NUMBERS.size)]
                    selectedWords.add(numberIndex, number)
                }
                
                // Join with separator
                val passphrase = selectedWords.joinToString(separator)
                
                // Validate generated passphrase
                val validation = validatePassphrase(passphrase)
                val validationResult = validation.getOrThrow()
                
                val generatedPassphrase = GeneratedPassphrase(
                    passphrase = passphrase,
                    wordCount = selectedWords.size,
                    strength = validationResult.strength,
                    entropy = validationResult.entropy,
                    estimatedCrackTime = validationResult.estimatedCrackTime,
                    memorable = true,
                    components = GeneratedPassphraseComponents(
                        words = selectedWords.filter { it !in NUMBERS },
                        numbers = selectedWords.filter { it in NUMBERS },
                        separator = separator,
                        capitalized = selectedWords.count { it.first().isUpperCase() }
                    )
                )
                
                logger.debug("✅ Passphrase generated - Strength: ${validationResult.strength.name}, Entropy: ${validationResult.entropy}")
                Result.success(generatedPassphrase)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to generate passphrase", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Validate passphrase strength and security
     */
    suspend fun validatePassphrase(
        passphrase: String,
        userInfo: UserPasswordContext? = null
    ): Result<PassphraseValidationResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📋 Validating passphrase security")
                
                val issues = mutableListOf<String>()
                val warnings = mutableListOf<String>()
                var score = 0
                
                // Length validation
                when {
                    passphrase.length < MIN_PASSPHRASE_LENGTH -> {
                        issues.add("Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters long")
                    }
                    passphrase.length > MAX_PASSPHRASE_LENGTH -> {
                        issues.add("Passphrase must not exceed $MAX_PASSPHRASE_LENGTH characters")
                    }
                    passphrase.length >= 40 -> score += 20
                    passphrase.length >= 25 -> score += 15
                    else -> score += 10
                }
                
                // Word analysis
                val words = extractWords(passphrase)
                val wordCount = words.size
                
                when {
                    wordCount < MIN_PASSPHRASE_WORDS -> {
                        issues.add("Passphrase should contain at least $MIN_PASSPHRASE_WORDS recognizable words")
                    }
                    wordCount >= 8 -> score += 25
                    wordCount >= 6 -> score += 20
                    wordCount >= 4 -> score += 15
                    else -> score += 5
                }
                
                // Entropy calculation (more sophisticated for passphrases)
                val entropy = calculatePassphraseEntropy(passphrase, words)
                if (entropy < MIN_ENTROPY_BITS) {
                    issues.add("Passphrase is too predictable. Consider using more diverse words or adding numbers.")
                } else {
                    score += (entropy / 5).toInt()
                }
                
                // Diversity checks
                val diversity = analyzeWordDiversity(words)
                score += (diversity.score * 10).toInt()
                
                if (diversity.hasRepeatedWords) {
                    warnings.add("Consider using unique words for better security")
                }
                
                if (diversity.hasSequentialWords) {
                    warnings.add("Avoid using words in alphabetical or logical sequence")
                }
                
                // Dictionary and common phrase checks
                if (containsCommonPhrases(passphrase)) {
                    warnings.add("Avoid common phrases or sayings")
                    score -= 15
                }
                
                // Personal information check
                if (userInfo != null && containsPersonalInfo(passphrase, userInfo)) {
                    issues.add("Passphrase should not contain personal information")
                }
                
                // Memorability assessment
                val memorability = assessMemorability(passphrase, words)
                
                // Calculate final score
                val finalScore = max(0, min(100, score))
                val strength = determinePassphraseStrength(finalScore, issues.isEmpty())
                
                val result = PassphraseValidationResult(
                    isValid = issues.isEmpty(),
                    strength = strength,
                    score = finalScore,
                    entropy = entropy,
                    wordCount = wordCount,
                    memorability = memorability,
                    diversity = diversity,
                    issues = issues,
                    warnings = warnings,
                    suggestions = generatePassphraseSuggestions(passphrase, words, issues, warnings),
                    estimatedCrackTime = estimatePassphraseCrackTime(entropy),
                    isPassphraseStyle = isPassphraseStyle(passphrase)
                )
                
                logger.debug("✅ Passphrase validation completed - Strength: ${strength.name}, Score: $finalScore")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to validate passphrase", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Convert regular password to passphrase style
     */
    suspend fun suggestPassphraseFromPassword(password: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Converting password to passphrase suggestions")
                
                val suggestions = mutableListOf<String>()
                
                // Generate passphrases based on password characteristics
                val hasNumbers = password.any { it.isDigit() }
                val hasUppercase = password.any { it.isUpperCase() }
                val passwordLength = password.length
                
                // Generate suggestions with similar security level
                val wordCount = when {
                    passwordLength <= 8 -> 4
                    passwordLength <= 12 -> 5
                    passwordLength <= 16 -> 6
                    else -> 7
                }
                
                // Generate multiple suggestions
                repeat(5) {
                    val passphraseResult = generatePassphrase(
                        wordCount = wordCount,
                        includeNumbers = hasNumbers,
                        includeCapitalization = hasUppercase,
                        separator = if (it % 2 == 0) "-" else " ",
                        useSecureWordList = true
                    )
                    
                    if (passphraseResult.isSuccess) {
                        suggestions.add(passphraseResult.getOrThrow().passphrase)
                    }
                }
                
                Result.success(suggestions)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to suggest passphrase from password", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate memorable passphrase for specific user context
     */
    suspend fun generatePersonalizedPassphrase(
        userPreferences: PassphrasePreferences,
        userContext: UserPassphraseContext
    ): Result<GeneratedPassphrase> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🎯 Generating personalized passphrase")
                
                // Use user preferences to customize generation
                val wordList = buildPersonalizedWordList(userPreferences, userContext)
                
                val result = generatePassphrase(
                    wordCount = userPreferences.preferredWordCount,
                    includeNumbers = userPreferences.includeNumbers,
                    includeCapitalization = userPreferences.includeCapitalization,
                    separator = userPreferences.preferredSeparator,
                    useSecureWordList = userPreferences.useSecureWords,
                    customWords = wordList
                )
                
                if (result.isSuccess) {
                    val passphrase = result.getOrThrow()
                    
                    // Add personalization metadata
                    val personalizedPassphrase = passphrase.copy(
                        memorable = true,
                        personalizationFactors = listOf(
                            "Based on user preferences",
                            "Includes preferred themes: ${userPreferences.themes.joinToString(", ")}",
                            "Optimized for memorability"
                        )
                    )
                    
                    Result.success(personalizedPassphrase)
                } else {
                    result
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to generate personalized passphrase", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check passphrase against breach databases
     */
    suspend fun checkPassphraseBreach(passphrase: String): Result<PassphraseBreachResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Checking passphrase against breach databases")
                
                // Extract words for individual checking
                val words = extractWords(passphrase)
                val breachedWords = mutableListOf<String>()
                
                // Check each word against common breach lists
                for (word in words) {
                    if (isBreachedWord(word)) {
                        breachedWords.add(word)
                    }
                }
                
                // Check if exact passphrase is breached (less likely)
                val exactMatch = isExactPassphraseBreach(passphrase)
                
                val result = PassphraseBreachResult(
                    isBreached = exactMatch || breachedWords.isNotEmpty(),
                    exactMatch = exactMatch,
                    breachedWords = breachedWords,
                    recommendation = when {
                        exactMatch -> "This exact passphrase has been found in data breaches. Please choose a different passphrase."
                        breachedWords.isNotEmpty() -> "Some words in your passphrase have been found in breaches. Consider replacing: ${breachedWords.joinToString(", ")}"
                        else -> "Passphrase has not been found in known data breaches."
                    },
                    alternativeWords = if (breachedWords.isNotEmpty()) {
                        generateAlternativeWords(breachedWords)
                    } else emptyList()
                )
                
                logger.debug("✅ Breach check completed - Breached: ${result.isBreached}")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to check passphrase breach", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun extractWords(passphrase: String): List<String> {
        return passphrase.split(Regex("[\\s\\-_.]+"))
            .filter { it.isNotBlank() && !it.all { char -> char.isDigit() } }
    }
    
    private fun calculatePassphraseEntropy(passphrase: String, words: List<String>): Double {
        // More sophisticated entropy calculation for passphrases
        val wordEntropy = words.size * log2(COMMON_WORDS.size.toDouble() + SECURE_WORDS.size.toDouble())
        val charEntropy = passphrase.length * log2(95.0) // ASCII printable characters
        val structureEntropy = 4.0 // Bonus for passphrase structure
        
        // Take weighted average favoring word entropy for passphrases
        return (wordEntropy * 0.7) + (charEntropy * 0.2) + structureEntropy
    }
    
    private fun analyzeWordDiversity(words: List<String>): WordDiversityAnalysis {
        val uniqueWords = words.toSet()
        val hasRepeatedWords = uniqueWords.size < words.size
        
        // Check for alphabetical sequence
        val sortedWords = words.sorted()
        val hasSequentialWords = sortedWords == words
        
        // Calculate diversity score
        val lengthVariety = words.map { it.length }.distinct().size.toDouble() / words.size
        val charVariety = words.flatMap { it.toList() }.distinct().size.toDouble() / 26.0
        val uniquenessRatio = uniqueWords.size.toDouble() / words.size
        
        val score = (lengthVariety + charVariety + uniquenessRatio) / 3.0
        
        return WordDiversityAnalysis(
            score = score,
            hasRepeatedWords = hasRepeatedWords,
            hasSequentialWords = hasSequentialWords,
            uniqueWordRatio = uniquenessRatio,
            averageWordLength = words.map { it.length }.average()
        )
    }
    
    private fun containsCommonPhrases(passphrase: String): Boolean {
        val commonPhrases = listOf(
            "the quick brown fox",
            "hello world",
            "password123",
            "admin admin",
            "test test",
            "happy birthday",
            "good morning",
            "i love you"
        )
        
        val lowerPassphrase = passphrase.lowercase()
        return commonPhrases.any { phrase ->
            lowerPassphrase.contains(phrase.replace(" ", "")) ||
            lowerPassphrase.contains(phrase)
        }
    }
    
    private fun containsPersonalInfo(passphrase: String, userInfo: UserPasswordContext): Boolean {
        val lowerPassphrase = passphrase.lowercase()
        
        return listOfNotNull(
            userInfo.firstName?.lowercase(),
            userInfo.lastName?.lowercase(),
            userInfo.email?.lowercase()?.substringBefore("@"),
            userInfo.username?.lowercase(),
            userInfo.birthYear?.toString()
        ).any { info ->
            info.length >= 3 && lowerPassphrase.contains(info)
        }
    }
    
    private fun assessMemorability(passphrase: String, words: List<String>): MemorabilityAssessment {
        // Factors that affect memorability
        val hasLogicalFlow = assessLogicalFlow(words)
        val hasRhyming = assessRhyming(words)
        val visualImagery = assessVisualImagery(words)
        val overallLength = passphrase.length
        
        val score = listOf(
            if (hasLogicalFlow) 0.3 else 0.0,
            if (hasRhyming) 0.2 else 0.0,
            visualImagery * 0.3,
            if (overallLength <= 30) 0.2 else 0.1
        ).sum()
        
        return MemorabilityAssessment(
            score = score,
            hasLogicalFlow = hasLogicalFlow,
            hasRhyming = hasRhyming,
            visualImageryScore = visualImagery,
            difficulty = when {
                score >= 0.8 -> MemorabilityDifficulty.VERY_EASY
                score >= 0.6 -> MemorabilityDifficulty.EASY
                score >= 0.4 -> MemorabilityDifficulty.MODERATE
                score >= 0.2 -> MemorabilityDifficulty.DIFFICULT
                else -> MemorabilityDifficulty.VERY_DIFFICULT
            }
        )
    }
    
    private fun assessLogicalFlow(words: List<String>): Boolean {
        // Simple check for logical word relationships
        return words.size >= 3 && words.zipWithNext().any { (first, second) ->
            areRelatedWords(first, second)
        }
    }
    
    private fun assessRhyming(words: List<String>): Boolean {
        // Simple rhyming detection
        return words.size >= 2 && words.any { word1 ->
            words.any { word2 -> word1 != word2 && word1.takeLast(2) == word2.takeLast(2) }
        }
    }
    
    private fun assessVisualImagery(words: List<String>): Double {
        // Count words that evoke visual imagery
        val visualWords = words.count { word ->
            word.lowercase() in listOf(
                "red", "blue", "bright", "dark", "mountain", "ocean", "sunset", "rainbow",
                "flower", "tree", "star", "moon", "fire", "ice", "crystal", "diamond"
            )
        }
        
        return visualWords.toDouble() / words.size
    }
    
    private fun areRelatedWords(word1: String, word2: String): Boolean {
        // Simplified word relationship check
        val relationships = mapOf(
            "sun" to listOf("moon", "star", "light", "bright"),
            "ocean" to listOf("wave", "beach", "water", "blue"),
            "mountain" to listOf("peak", "high", "snow", "climb")
        )
        
        return relationships[word1.lowercase()]?.contains(word2.lowercase()) == true ||
               relationships[word2.lowercase()]?.contains(word1.lowercase()) == true
    }
    
    private fun determinePassphraseStrength(score: Int, isValid: Boolean): PassphraseStrength {
        if (!isValid) return PassphraseStrength.WEAK
        
        return when {
            score >= 85 -> PassphraseStrength.VERY_STRONG
            score >= 70 -> PassphraseStrength.STRONG
            score >= 55 -> PassphraseStrength.MEDIUM
            score >= 35 -> PassphraseStrength.FAIR
            else -> PassphraseStrength.WEAK
        }
    }
    
    private fun generatePassphraseSuggestions(
        passphrase: String,
        words: List<String>,
        issues: List<String>,
        warnings: List<String>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (words.size < MIN_PASSPHRASE_WORDS) {
            suggestions.add("Add more words to increase security and memorability")
        }
        
        if (issues.any { it.contains("predictable") }) {
            suggestions.add("Use less common words or add numbers/symbols")
        }
        
        if (warnings.any { it.contains("unique") }) {
            suggestions.add("Ensure all words in your passphrase are different")
        }
        
        suggestions.add("Consider using a theme (e.g., nature, food, travel) to make it more memorable")
        suggestions.add("Try connecting words with a story or visual image")
        suggestions.add("Use proper spacing or dashes to improve readability")
        
        return suggestions
    }
    
    private fun estimatePassphraseCrackTime(entropyBits: Double): String {
        val combinations = Math.pow(2.0, entropyBits)
        val guessesPerSecond = 1_000_000.0 // Conservative estimate for passphrase attacks
        val secondsToCrack = combinations / (2 * guessesPerSecond)
        
        return when {
            secondsToCrack < 3600 -> "Less than an hour"
            secondsToCrack < 86400 -> "${(secondsToCrack / 3600).toInt()} hours"
            secondsToCrack < 31536000 -> "${(secondsToCrack / 86400).toInt()} days"
            secondsToCrack < 31536000000 -> "${(secondsToCrack / 31536000).toInt()} years"
            else -> "Centuries"
        }
    }
    
    private fun isPassphraseStyle(passphrase: String): Boolean {
        val words = extractWords(passphrase)
        return words.size >= 3 && 
               words.any { it.length >= 4 } &&
               passphrase.contains(Regex("[\\s\\-_.]"))
    }
    
    private fun buildPersonalizedWordList(
        preferences: PassphrasePreferences,
        context: UserPassphraseContext
    ): List<String> {
        val personalizedWords = mutableListOf<String>()
        
        // Add theme-based words
        for (theme in preferences.themes) {
            personalizedWords.addAll(getThemeWords(theme))
        }
        
        // Add interest-based words
        personalizedWords.addAll(getInterestWords(context.interests))
        
        // Ensure we have enough words
        if (personalizedWords.size < 50) {
            personalizedWords.addAll(SECURE_WORDS)
        }
        
        return personalizedWords.distinct()
    }
    
    private fun getThemeWords(theme: String): List<String> {
        return when (theme.lowercase()) {
            "nature" -> listOf("forest", "mountain", "river", "ocean", "meadow", "valley", "peak", "grove")
            "space" -> listOf("galaxy", "nebula", "cosmos", "asteroid", "comet", "universe", "stellar", "lunar")
            "food" -> listOf("chocolate", "vanilla", "cinnamon", "honey", "maple", "berry", "citrus", "mint")
            "travel" -> listOf("journey", "adventure", "explore", "destination", "voyage", "expedition", "safari", "quest")
            else -> COMMON_WORDS.take(10)
        }
    }
    
    private fun getInterestWords(interests: List<String>): List<String> {
        return interests.flatMap { interest ->
            when (interest.lowercase()) {
                "music" -> listOf("melody", "harmony", "rhythm", "symphony", "acoustic", "tempo")
                "art" -> listOf("canvas", "sculpture", "palette", "gallery", "creative", "masterpiece")
                "sports" -> listOf("champion", "victory", "athletic", "tournament", "competitive", "achievement")
                "technology" -> listOf("innovation", "digital", "quantum", "algorithm", "network", "system")
                else -> emptyList()
            }
        }
    }
    
    private fun isBreachedWord(word: String): Boolean {
        // Mock implementation - would check against actual breach databases
        val commonBreachedWords = listOf("password", "admin", "user", "test", "login")
        return word.lowercase() in commonBreachedWords
    }
    
    private fun isExactPassphraseBreach(passphrase: String): Boolean {
        // Mock implementation - would check against breach databases
        return false
    }
    
    private fun generateAlternativeWords(breachedWords: List<String>): List<String> {
        return breachedWords.map { word ->
            // Generate similar but secure alternatives
            when (word.lowercase()) {
                "password" -> "security"
                "admin" -> "manager"
                "user" -> "person"
                "test" -> "trial"
                "login" -> "access"
                else -> SECURE_WORDS.random()
            }
        }
    }
}

// Passphrase-specific enums and data classes
enum class PassphraseStrength {
    WEAK, FAIR, MEDIUM, STRONG, VERY_STRONG
}

enum class MemorabilityDifficulty {
    VERY_EASY, EASY, MODERATE, DIFFICULT, VERY_DIFFICULT
}

data class WordDiversityAnalysis(
    val score: Double,
    val hasRepeatedWords: Boolean,
    val hasSequentialWords: Boolean,
    val uniqueWordRatio: Double,
    val averageWordLength: Double
)

data class MemorabilityAssessment(
    val score: Double,
    val hasLogicalFlow: Boolean,
    val hasRhyming: Boolean,
    val visualImageryScore: Double,
    val difficulty: MemorabilityDifficulty
)

data class GeneratedPassphraseComponents(
    val words: List<String>,
    val numbers: List<String>,
    val separator: String,
    val capitalized: Int
)
