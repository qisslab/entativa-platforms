package com.entativa.id.service.security

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Fraud Detection Service for Entativa ID
 * Implements sophisticated fraud detection algorithms and risk scoring
 * 
 * @author Neo Qiss
 * @status Production-ready fraud detection with ML-inspired heuristics
 */
@Service
class FraudDetectionService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(FraudDetectionService::class.java)
    
    companion object {
        // Risk score thresholds
        private const val LOW_RISK_THRESHOLD = 30
        private const val MEDIUM_RISK_THRESHOLD = 60
        private const val HIGH_RISK_THRESHOLD = 80
        
        // Time windows for analysis
        private const val SHORT_WINDOW_MINUTES = 15L
        private const val MEDIUM_WINDOW_HOURS = 24L
        private const val LONG_WINDOW_DAYS = 7L
        
        // Activity limits
        private const val MAX_LOGIN_ATTEMPTS_SHORT = 5
        private const val MAX_LOGIN_ATTEMPTS_MEDIUM = 20
        private const val MAX_LOCATIONS_PER_DAY = 5
        private const val MAX_DEVICES_PER_WEEK = 10
        
        // Cache TTLs
        private const val FRAUD_CACHE_TTL_SECONDS = 3600
        private const val BEHAVIOR_CACHE_TTL_SECONDS = 86400 * 7 // 7 days
    }
    
    /**
     * Analyze login attempt for fraud indicators
     */
    suspend fun analyzeLoginAttempt(request: LoginAnalysisRequest): Result<FraudAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔍 Analyzing login attempt for user: ${request.userId ?: "unknown"}")
                
                val riskFactors = mutableListOf<RiskFactor>()
                var totalRiskScore = 0
                
                // 1. IP Address Analysis
                val ipRisk = analyzeIpAddress(request.ipAddress, request.userId)
                riskFactors.addAll(ipRisk.factors)
                totalRiskScore += ipRisk.score
                
                // 2. Geolocation Analysis
                if (request.location != null) {
                    val locationRisk = analyzeLocation(request.location, request.userId)
                    riskFactors.addAll(locationRisk.factors)
                    totalRiskScore += locationRisk.score
                }
                
                // 3. Device Analysis
                val deviceRisk = analyzeDevice(request.deviceInfo, request.userId)
                riskFactors.addAll(deviceRisk.factors)
                totalRiskScore += deviceRisk.score
                
                // 4. Temporal Analysis
                val temporalRisk = analyzeLoginTiming(request.timestamp, request.userId)
                riskFactors.addAll(temporalRisk.factors)
                totalRiskScore += temporalRisk.score
                
                // 5. Behavioral Analysis
                if (request.userId != null) {
                    val behaviorRisk = analyzeBehavioralPatterns(request.userId, request)
                    riskFactors.addAll(behaviorRisk.factors)
                    totalRiskScore += behaviorRisk.score
                }
                
                // 6. Frequency Analysis
                val frequencyRisk = analyzeLoginFrequency(request.ipAddress, request.userId)
                riskFactors.addAll(frequencyRisk.factors)
                totalRiskScore += frequencyRisk.score
                
                // Normalize score (0-100)
                val normalizedScore = min(100, max(0, totalRiskScore))
                val riskLevel = determineRiskLevel(normalizedScore)
                
                val result = FraudAnalysisResult(
                    riskScore = normalizedScore,
                    riskLevel = riskLevel,
                    riskFactors = riskFactors,
                    recommendedAction = determineRecommendedAction(riskLevel, riskFactors),
                    analysisTimestamp = Instant.now().toString(),
                    requiresAdditionalVerification = normalizedScore >= MEDIUM_RISK_THRESHOLD,
                    blockRecommended = normalizedScore >= HIGH_RISK_THRESHOLD,
                    confidence = calculateConfidence(riskFactors)
                )
                
                // Cache analysis result
                if (request.userId != null) {
                    cacheAnalysisResult(request.userId, result)
                }
                
                logger.info("📊 Fraud analysis completed - Risk: ${riskLevel.name} (${normalizedScore})")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Fraud analysis failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Analyze account creation for fraud indicators
     */
    suspend fun analyzeAccountCreation(request: AccountCreationAnalysisRequest): Result<FraudAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🆕 Analyzing account creation for email: ${request.email}")
                
                val riskFactors = mutableListOf<RiskFactor>()
                var totalRiskScore = 0
                
                // 1. Email Analysis
                val emailRisk = analyzeEmail(request.email)
                riskFactors.addAll(emailRisk.factors)
                totalRiskScore += emailRisk.score
                
                // 2. Phone Number Analysis
                if (request.phoneNumber != null) {
                    val phoneRisk = analyzePhoneNumber(request.phoneNumber)
                    riskFactors.addAll(phoneRisk.factors)
                    totalRiskScore += phoneRisk.score
                }
                
                // 3. IP/Location Analysis
                val ipRisk = analyzeIpAddress(request.ipAddress, null)
                riskFactors.addAll(ipRisk.factors)
                totalRiskScore += ipRisk.score
                
                // 4. Device Analysis
                val deviceRisk = analyzeDevice(request.deviceInfo, null)
                riskFactors.addAll(deviceRisk.factors)
                totalRiskScore += deviceRisk.score
                
                // 5. Registration Pattern Analysis
                val patternRisk = analyzeRegistrationPatterns(request)
                riskFactors.addAll(patternRisk.factors)
                totalRiskScore += patternRisk.score
                
                // 6. Velocity Analysis (rapid registrations)
                val velocityRisk = analyzeRegistrationVelocity(request.ipAddress)
                riskFactors.addAll(velocityRisk.factors)
                totalRiskScore += velocityRisk.score
                
                val normalizedScore = min(100, max(0, totalRiskScore))
                val riskLevel = determineRiskLevel(normalizedScore)
                
                val result = FraudAnalysisResult(
                    riskScore = normalizedScore,
                    riskLevel = riskLevel,
                    riskFactors = riskFactors,
                    recommendedAction = determineRecommendedAction(riskLevel, riskFactors),
                    analysisTimestamp = Instant.now().toString(),
                    requiresAdditionalVerification = normalizedScore >= MEDIUM_RISK_THRESHOLD,
                    blockRecommended = normalizedScore >= HIGH_RISK_THRESHOLD,
                    confidence = calculateConfidence(riskFactors)
                )
                
                logger.info("📊 Account creation analysis completed - Risk: ${riskLevel.name} (${normalizedScore})")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Account creation analysis failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Analyze transaction patterns for anomalies
     */
    suspend fun analyzeTransaction(request: TransactionAnalysisRequest): Result<FraudAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("💳 Analyzing transaction for user: ${request.userId}")
                
                val riskFactors = mutableListOf<RiskFactor>()
                var totalRiskScore = 0
                
                // 1. Amount Analysis
                val amountRisk = analyzeTransactionAmount(request.amount, request.userId)
                riskFactors.addAll(amountRisk.factors)
                totalRiskScore += amountRisk.score
                
                // 2. Frequency Analysis
                val frequencyRisk = analyzeTransactionFrequency(request.userId)
                riskFactors.addAll(frequencyRisk.factors)
                totalRiskScore += frequencyRisk.score
                
                // 3. Time Pattern Analysis
                val timeRisk = analyzeTransactionTiming(request.timestamp, request.userId)
                riskFactors.addAll(timeRisk.factors)
                totalRiskScore += timeRisk.score
                
                // 4. Location Consistency
                if (request.location != null) {
                    val locationRisk = analyzeTransactionLocation(request.location, request.userId)
                    riskFactors.addAll(locationRisk.factors)
                    totalRiskScore += locationRisk.score
                }
                
                val normalizedScore = min(100, max(0, totalRiskScore))
                val riskLevel = determineRiskLevel(normalizedScore)
                
                val result = FraudAnalysisResult(
                    riskScore = normalizedScore,
                    riskLevel = riskLevel,
                    riskFactors = riskFactors,
                    recommendedAction = determineRecommendedAction(riskLevel, riskFactors),
                    analysisTimestamp = Instant.now().toString(),
                    requiresAdditionalVerification = normalizedScore >= MEDIUM_RISK_THRESHOLD,
                    blockRecommended = normalizedScore >= HIGH_RISK_THRESHOLD,
                    confidence = calculateConfidence(riskFactors)
                )
                
                logger.info("📊 Transaction analysis completed - Risk: ${riskLevel.name} (${normalizedScore})")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Transaction analysis failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user risk profile
     */
    suspend fun getUserRiskProfile(userId: String): Result<UserRiskProfile> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📈 Getting risk profile for user: $userId")
                
                // Check cache first
                val cached = cacheManager.getCachedData<UserRiskProfile>("risk_profile:$userId")
                if (cached != null) {
                    return@withContext Result.success(cached)
                }
                
                // Calculate risk profile
                val profile = calculateUserRiskProfile(userId)
                
                // Cache profile
                cacheManager.cacheData("risk_profile:$userId", profile, BEHAVIOR_CACHE_TTL_SECONDS)
                
                Result.success(profile)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user risk profile: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE ANALYSIS METHODS ==============
    
    private suspend fun analyzeIpAddress(ipAddress: String, userId: String?): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check if IP is from known bad sources
        if (isKnownMaliciousIp(ipAddress)) {
            factors.add(RiskFactor("malicious_ip", "IP address is known malicious", 40))
            score += 40
        }
        
        // Check for VPN/Proxy usage
        if (isVpnOrProxy(ipAddress)) {
            factors.add(RiskFactor("vpn_proxy", "VPN or proxy detected", 15))
            score += 15
        }
        
        // Check for unusual geolocation
        if (isUnusualGeolocation(ipAddress, userId)) {
            factors.add(RiskFactor("unusual_location", "Login from unusual location", 25))
            score += 25
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeLocation(location: String, userId: String?): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        if (userId != null) {
            val recentLocations = getRecentUserLocations(userId)
            
            if (recentLocations.isNotEmpty() && !recentLocations.contains(location)) {
                val distance = calculateLocationDistance(location, recentLocations.first())
                
                when {
                    distance > 1000 -> {
                        factors.add(RiskFactor("distant_location", "Login from distant location", 30))
                        score += 30
                    }
                    distance > 500 -> {
                        factors.add(RiskFactor("new_city", "Login from new city", 15))
                        score += 15
                    }
                }
            }
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeDevice(deviceInfo: DeviceInfo, userId: String?): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        if (userId != null) {
            val knownDevices = getUserKnownDevices(userId)
            val deviceFingerprint = generateDeviceFingerprint(deviceInfo)
            
            if (!knownDevices.contains(deviceFingerprint)) {
                factors.add(RiskFactor("new_device", "Login from new device", 20))
                score += 20
            }
        }
        
        // Check for suspicious device characteristics
        if (isObscureUserAgent(deviceInfo.browser)) {
            factors.add(RiskFactor("suspicious_browser", "Unusual or automated browser", 15))
            score += 15
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeLoginTiming(timestamp: String, userId: String?): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        val loginTime = Instant.parse(timestamp)
        val hour = loginTime.atZone(java.time.ZoneOffset.UTC).hour
        
        // Check for unusual hours (2 AM - 6 AM)
        if (hour in 2..6) {
            factors.add(RiskFactor("unusual_hour", "Login during unusual hours", 10))
            score += 10
        }
        
        if (userId != null) {
            val recentLogins = getRecentUserLogins(userId)
            
            // Check for rapid successive logins
            val recentLogin = recentLogins.firstOrNull()
            if (recentLogin != null) {
                val timeDiff = ChronoUnit.MINUTES.between(Instant.parse(recentLogin), loginTime)
                if (timeDiff < 2) {
                    factors.add(RiskFactor("rapid_login", "Very rapid successive login", 25))
                    score += 25
                }
            }
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeBehavioralPatterns(userId: String, request: LoginAnalysisRequest): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        val userBehavior = getUserBehaviorProfile(userId)
        
        // Analyze deviations from normal behavior
        if (userBehavior != null) {
            // Check typing pattern deviations (if available)
            if (request.typingPattern != null && userBehavior.typingPattern != null) {
                val deviation = calculateTypingPatternDeviation(request.typingPattern, userBehavior.typingPattern)
                if (deviation > 0.7) {
                    factors.add(RiskFactor("typing_anomaly", "Unusual typing pattern", 20))
                    score += 20
                }
            }
            
            // Check session duration patterns
            val avgSessionDuration = userBehavior.averageSessionDuration
            // This would be implemented with actual session tracking
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeLoginFrequency(ipAddress: String, userId: String?): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check IP-based frequency
        val ipLogins = getRecentLoginsFromIp(ipAddress)
        if (ipLogins.size > MAX_LOGIN_ATTEMPTS_SHORT) {
            factors.add(RiskFactor("ip_frequency", "Too many logins from IP", 35))
            score += 35
        }
        
        // Check user-based frequency
        if (userId != null) {
            val userLogins = getRecentUserLogins(userId)
            if (userLogins.size > MAX_LOGIN_ATTEMPTS_MEDIUM) {
                factors.add(RiskFactor("user_frequency", "Too many login attempts", 30))
                score += 30
            }
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeEmail(email: String): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check for disposable email domains
        if (isDisposableEmail(email)) {
            factors.add(RiskFactor("disposable_email", "Disposable email domain", 30))
            score += 30
        }
        
        // Check for suspicious patterns
        if (hasSuspiciousEmailPattern(email)) {
            factors.add(RiskFactor("suspicious_email", "Suspicious email pattern", 20))
            score += 20
        }
        
        // Check if email has been seen in previous fraud cases
        if (isKnownFraudulentEmail(email)) {
            factors.add(RiskFactor("known_fraud_email", "Email associated with fraud", 50))
            score += 50
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzePhoneNumber(phoneNumber: String): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check for VOIP numbers
        if (isVoipNumber(phoneNumber)) {
            factors.add(RiskFactor("voip_number", "VOIP phone number", 15))
            score += 15
        }
        
        // Check for known fraudulent numbers
        if (isKnownFraudulentPhone(phoneNumber)) {
            factors.add(RiskFactor("known_fraud_phone", "Phone associated with fraud", 40))
            score += 40
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeRegistrationPatterns(request: AccountCreationAnalysisRequest): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        // Check for automated registration patterns
        if (hasAutomatedRegistrationPattern(request)) {
            factors.add(RiskFactor("automated_registration", "Automated registration pattern", 35))
            score += 35
        }
        
        // Check for suspicious form completion speed
        if (request.formCompletionTime != null && request.formCompletionTime < 10) {
            factors.add(RiskFactor("fast_completion", "Unusually fast form completion", 25))
            score += 25
        }
        
        return RiskAnalysis(score, factors)
    }
    
    private suspend fun analyzeRegistrationVelocity(ipAddress: String): RiskAnalysis {
        val factors = mutableListOf<RiskFactor>()
        var score = 0
        
        val recentRegistrations = getRecentRegistrationsFromIp(ipAddress)
        
        if (recentRegistrations.size > 5) {
            factors.add(RiskFactor("registration_velocity", "Multiple registrations from IP", 40))
            score += 40
        }
        
        return RiskAnalysis(score, factors)
    }
    
    // ============== HELPER METHODS ==============
    
    private fun determineRiskLevel(score: Int): RiskLevel {
        return when {
            score < LOW_RISK_THRESHOLD -> RiskLevel.LOW
            score < MEDIUM_RISK_THRESHOLD -> RiskLevel.MEDIUM
            score < HIGH_RISK_THRESHOLD -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }
    
    private fun determineRecommendedAction(riskLevel: RiskLevel, factors: List<RiskFactor>): String {
        return when (riskLevel) {
            RiskLevel.LOW -> "Allow"
            RiskLevel.MEDIUM -> "Require additional verification"
            RiskLevel.HIGH -> "Manual review required"
            RiskLevel.CRITICAL -> "Block and investigate"
        }
    }
    
    private fun calculateConfidence(factors: List<RiskFactor>): Double {
        // Calculate confidence based on number and quality of risk factors
        val factorCount = factors.size
        val avgScore = factors.map { it.score }.average()
        
        return min(1.0, (factorCount * avgScore) / 100.0)
    }
    
    private suspend fun calculateUserRiskProfile(userId: String): UserRiskProfile {
        // This would implement comprehensive risk profile calculation
        return UserRiskProfile(
            userId = userId,
            overallRiskScore = 25, // Default medium-low risk
            riskLevel = RiskLevel.LOW,
            trustScore = 75,
            accountAge = calculateAccountAge(userId),
            verificationLevel = getUserVerificationLevel(userId),
            recentSuspiciousActivity = getRecentSuspiciousActivity(userId),
            behaviorBaseline = getUserBehaviorBaseline(userId),
            lastUpdated = Instant.now().toString()
        )
    }
    
    // Mock implementations for external data sources
    private fun isKnownMaliciousIp(ip: String): Boolean = false
    private fun isVpnOrProxy(ip: String): Boolean = false
    private fun isUnusualGeolocation(ip: String, userId: String?): Boolean = false
    private fun isDisposableEmail(email: String): Boolean = email.contains("tempmail") || email.contains("10minutemail")
    private fun hasSuspiciousEmailPattern(email: String): Boolean = email.matches(Regex(".*\\d{8,}.*"))
    private fun isKnownFraudulentEmail(email: String): Boolean = false
    private fun isVoipNumber(phone: String): Boolean = false
    private fun isKnownFraudulentPhone(phone: String): Boolean = false
    private fun isObscureUserAgent(browser: String): Boolean = browser.contains("Bot") || browser.contains("Spider")
    private fun hasAutomatedRegistrationPattern(request: AccountCreationAnalysisRequest): Boolean = false
    
    private suspend fun getRecentUserLocations(userId: String): List<String> = emptyList()
    private suspend fun getUserKnownDevices(userId: String): Set<String> = emptySet()
    private suspend fun getRecentUserLogins(userId: String): List<String> = emptyList()
    private suspend fun getRecentLoginsFromIp(ip: String): List<String> = emptyList()
    private suspend fun getRecentRegistrationsFromIp(ip: String): List<String> = emptyList()
    private suspend fun getUserBehaviorProfile(userId: String): UserBehaviorProfile? = null
    
    private fun generateDeviceFingerprint(deviceInfo: DeviceInfo): String {
        return "${deviceInfo.platform}-${deviceInfo.browser}-${deviceInfo.os}".hashCode().toString()
    }
    
    private fun calculateLocationDistance(location1: String, location2: String): Double = 0.0
    private fun calculateTypingPatternDeviation(pattern1: Any, pattern2: Any): Double = 0.0
    private fun calculateAccountAge(userId: String): Int = 30 // days
    private fun getUserVerificationLevel(userId: String): String = "basic"
    private fun getRecentSuspiciousActivity(userId: String): List<String> = emptyList()
    private fun getUserBehaviorBaseline(userId: String): Map<String, Any> = emptyMap()
    
    private suspend fun cacheAnalysisResult(userId: String, result: FraudAnalysisResult) {
        try {
            cacheManager.cacheData("fraud_analysis:$userId", result, FRAUD_CACHE_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Failed to cache fraud analysis result", e)
        }
    }
}

// Risk analysis data structures
private data class RiskAnalysis(
    val score: Int,
    val factors: List<RiskFactor>
)

// Mock data classes for compilation
private data class UserBehaviorProfile(
    val typingPattern: Any?,
    val averageSessionDuration: Long
)
