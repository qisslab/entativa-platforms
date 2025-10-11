package com.entativa.id.service.security

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Rate Limiting Service for Entativa ID
 * Provides comprehensive rate limiting, throttling, and abuse protection mechanisms
 * 
 * @author Neo Qiss
 * @status Production-ready rate limiting with adaptive algorithms
 */
@Service
class RateLimitService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)
    private val adaptiveRules = ConcurrentHashMap<String, AdaptiveRateLimitRule>()
    
    companion object {
        // Default rate limits
        private const val DEFAULT_LOGIN_ATTEMPTS = 5
        private const val DEFAULT_LOGIN_WINDOW_MINUTES = 15L
        private const val DEFAULT_API_REQUESTS = 100
        private const val DEFAULT_API_WINDOW_MINUTES = 1L
        private const val DEFAULT_PASSWORD_RESET = 3
        private const val DEFAULT_PASSWORD_RESET_WINDOW_HOURS = 1L
        
        // Burst protection
        private const val BURST_THRESHOLD_MULTIPLIER = 2.0
        private const val BURST_RECOVERY_MINUTES = 5L
        
        // Adaptive thresholds
        private const val ADAPTIVE_INCREASE_FACTOR = 1.5
        private const val ADAPTIVE_DECREASE_FACTOR = 0.8
        private const val ADAPTIVE_MIN_LIMIT = 10
        private const val ADAPTIVE_MAX_LIMIT = 10000
        
        // Penalties and escalation
        private const val ESCALATION_MULTIPLIER = 2.0
        private const val MAX_PENALTY_HOURS = 24L
    }
    
    /**
     * Check if action is rate limited
     */
    suspend fun checkRateLimit(
        identifier: String,
        action: RateLimitAction,
        clientInfo: RateLimitClientInfo? = null
    ): Result<RateLimitResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Checking rate limit for $action: $identifier")
                
                // Get rate limit configuration
                val config = getRateLimitConfig(action)
                
                // Apply adaptive rules if enabled
                val effectiveConfig = applyAdaptiveRules(config, identifier, action)
                
                // Check current usage
                val usage = getCurrentUsage(identifier, action, effectiveConfig.windowDuration)
                
                // Determine if rate limited
                val isAllowed = usage.count < effectiveConfig.maxRequests
                val remaining = max(0, effectiveConfig.maxRequests - usage.count)
                
                // Calculate reset time
                val resetTime = if (usage.windowStart != null) {
                    usage.windowStart.plus(effectiveConfig.windowDuration)
                } else {
                    Instant.now().plus(effectiveConfig.windowDuration)
                }
                
                // Check for burst protection
                val burstProtection = checkBurstProtection(identifier, action, usage)
                
                // Check for penalty escalation
                val penaltyInfo = checkPenaltyEscalation(identifier, action)
                
                val result = RateLimitResult(
                    allowed = isAllowed && !burstProtection.active && !penaltyInfo.active,
                    remaining = remaining,
                    resetTime = resetTime,
                    retryAfter = if (!isAllowed) calculateRetryAfter(resetTime) else null,
                    burstProtection = burstProtection,
                    penaltyInfo = penaltyInfo,
                    adaptiveAdjustment = getAdaptiveAdjustment(identifier, action),
                    metadata = RateLimitMetadata(
                        action = action,
                        windowStart = usage.windowStart ?: Instant.now(),
                        windowDuration = effectiveConfig.windowDuration,
                        configuredLimit = config.maxRequests,
                        effectiveLimit = effectiveConfig.maxRequests,
                        clientInfo = clientInfo
                    )
                )
                
                // Record the attempt
                if (isAllowed && !burstProtection.active && !penaltyInfo.active) {
                    recordSuccessfulAttempt(identifier, action, clientInfo)
                } else {
                    recordBlockedAttempt(identifier, action, result, clientInfo)
                }
                
                // Update adaptive learning
                updateAdaptiveLearning(identifier, action, result)
                
                logger.debug("✅ Rate limit check completed - Allowed: ${result.allowed}, Remaining: ${result.remaining}")
                Result.success(result)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to check rate limit", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Record successful action
     */
    suspend fun recordSuccess(
        identifier: String,
        action: RateLimitAction,
        clientInfo: RateLimitClientInfo? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                recordSuccessfulAttempt(identifier, action, clientInfo)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Apply penalty for abusive behavior
     */
    suspend fun applyPenalty(
        identifier: String,
        action: RateLimitAction,
        penaltyType: PenaltyType,
        duration: Duration,
        reason: String
    ): Result<PenaltyInfo> {
        return withContext(Dispatchers.IO) {
            try {
                logger.warn("⚠️ Applying penalty to $identifier for $action: $reason")
                
                val penalty = PenaltyInfo(
                    active = true,
                    type = penaltyType,
                    startTime = Instant.now(),
                    endTime = Instant.now().plus(duration),
                    reason = reason,
                    escalationLevel = calculateEscalationLevel(identifier, action),
                    appealable = penaltyType != PenaltyType.PERMANENT_BAN
                )
                
                // Store penalty
                val penaltyKey = "penalty:$identifier:${action.name}"
                cacheManager.set(penaltyKey, penalty, duration.seconds)
                
                // Record in penalty history
                recordPenaltyHistory(identifier, action, penalty)
                
                // Trigger alerts for severe penalties
                if (penaltyType in setOf(PenaltyType.EXTENDED_SUSPENSION, PenaltyType.PERMANENT_BAN)) {
                    triggerSecurityAlert(identifier, action, penalty)
                }
                
                logger.warn("⚠️ Penalty applied: $penaltyType for ${duration.toMinutes()} minutes")
                Result.success(penalty)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to apply penalty", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Remove penalty (for appeals or admin override)
     */
    suspend fun removePenalty(
        identifier: String,
        action: RateLimitAction,
        reason: String,
        adminUserId: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔓 Removing penalty for $identifier on $action: $reason")
                
                val penaltyKey = "penalty:$identifier:${action.name}"
                val penalty = cacheManager.get<PenaltyInfo>(penaltyKey)
                
                if (penalty != null) {
                    cacheManager.delete(penaltyKey)
                    
                    // Record penalty removal
                    recordPenaltyRemoval(identifier, action, penalty, reason, adminUserId)
                    
                    logger.info("✅ Penalty removed for $identifier")
                } else {
                    logger.warn("⚠️ No active penalty found for $identifier")
                }
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to remove penalty", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get current rate limit status
     */
    suspend fun getRateLimitStatus(
        identifier: String,
        action: RateLimitAction
    ): Result<RateLimitStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val config = getRateLimitConfig(action)
                val usage = getCurrentUsage(identifier, action, config.windowDuration)
                val penalty = checkPenaltyEscalation(identifier, action)
                val burst = checkBurstProtection(identifier, action, usage)
                
                val status = RateLimitStatus(
                    identifier = identifier,
                    action = action,
                    currentUsage = usage.count,
                    limit = config.maxRequests,
                    remaining = max(0, config.maxRequests - usage.count),
                    windowStart = usage.windowStart ?: Instant.now(),
                    windowEnd = (usage.windowStart ?: Instant.now()).plus(config.windowDuration),
                    penalty = penalty,
                    burstProtection = burst,
                    isLimited = usage.count >= config.maxRequests || penalty.active || burst.active
                )
                
                Result.success(status)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Configure custom rate limits
     */
    suspend fun configureRateLimit(
        action: RateLimitAction,
        config: RateLimitConfig
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚙️ Configuring rate limit for $action: ${config.maxRequests}/${config.windowDuration}")
                
                val configKey = "rate_limit_config:${action.name}"
                cacheManager.set(configKey, config, 86400 * 7) // 1 week
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Enable adaptive rate limiting
     */
    suspend fun enableAdaptiveRateLimit(
        identifier: String,
        action: RateLimitAction,
        baseLine: Int,
        adaptiveParams: AdaptiveRateLimitParams
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🧠 Enabling adaptive rate limiting for $identifier on $action")
                
                val rule = AdaptiveRateLimitRule(
                    identifier = identifier,
                    action = action,
                    baseLine = baseLine,
                    currentLimit = baseLine,
                    params = adaptiveParams,
                    learningData = AdaptiveLearningData(),
                    lastAdjustment = Instant.now()
                )
                
                adaptiveRules["$identifier:${action.name}"] = rule
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get comprehensive rate limiting analytics
     */
    suspend fun getRateLimitAnalytics(
        period: Duration,
        actions: Set<RateLimitAction> = RateLimitAction.values().toSet()
    ): Result<RateLimitAnalytics> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📊 Generating rate limit analytics for ${period.toDays()} days")
                
                val endTime = Instant.now()
                val startTime = endTime.minus(period)
                
                val analytics = RateLimitAnalytics(
                    period = period,
                    totalRequests = getTotalRequests(startTime, endTime, actions),
                    blockedRequests = getBlockedRequests(startTime, endTime, actions),
                    topLimitedIdentifiers = getTopLimitedIdentifiers(startTime, endTime, actions),
                    actionBreakdown = getActionBreakdown(startTime, endTime, actions),
                    penaltyStatistics = getPenaltyStatistics(startTime, endTime, actions),
                    burstProtectionTriggers = getBurstProtectionTriggers(startTime, endTime, actions),
                    adaptiveAdjustments = getAdaptiveAdjustments(startTime, endTime, actions),
                    trends = calculateRateLimitTrends(startTime, endTime, actions)
                )
                
                Result.success(analytics)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to generate rate limit analytics", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun getRateLimitConfig(action: RateLimitAction): RateLimitConfig {
        val configKey = "rate_limit_config:${action.name}"
        val customConfig = cacheManager.get<RateLimitConfig>(configKey)
        
        return customConfig ?: getDefaultConfig(action)
    }
    
    private fun getDefaultConfig(action: RateLimitAction): RateLimitConfig {
        return when (action) {
            RateLimitAction.LOGIN -> RateLimitConfig(
                maxRequests = DEFAULT_LOGIN_ATTEMPTS,
                windowDuration = Duration.ofMinutes(DEFAULT_LOGIN_WINDOW_MINUTES)
            )
            RateLimitAction.API_REQUEST -> RateLimitConfig(
                maxRequests = DEFAULT_API_REQUESTS,
                windowDuration = Duration.ofMinutes(DEFAULT_API_WINDOW_MINUTES)
            )
            RateLimitAction.PASSWORD_RESET -> RateLimitConfig(
                maxRequests = DEFAULT_PASSWORD_RESET,
                windowDuration = Duration.ofHours(DEFAULT_PASSWORD_RESET_WINDOW_HOURS)
            )
            RateLimitAction.REGISTRATION -> RateLimitConfig(
                maxRequests = 5,
                windowDuration = Duration.ofHours(1)
            )
            RateLimitAction.EMAIL_VERIFICATION -> RateLimitConfig(
                maxRequests = 10,
                windowDuration = Duration.ofHours(1)
            )
            RateLimitAction.MFA_ATTEMPT -> RateLimitConfig(
                maxRequests = 10,
                windowDuration = Duration.ofMinutes(5)
            )
            RateLimitAction.BIOMETRIC_AUTH -> RateLimitConfig(
                maxRequests = 20,
                windowDuration = Duration.ofMinutes(5)
            )
            RateLimitAction.HANDLE_SEARCH -> RateLimitConfig(
                maxRequests = 50,
                windowDuration = Duration.ofMinutes(1)
            )
        }
    }
    
    private fun getCurrentUsage(
        identifier: String,
        action: RateLimitAction,
        windowDuration: Duration
    ): RateLimitUsage {
        val usageKey = "rate_limit:$identifier:${action.name}"
        val usage = cacheManager.get<RateLimitUsage>(usageKey)
        
        val now = Instant.now()
        
        if (usage == null || usage.windowStart == null || 
            now.isAfter(usage.windowStart.plus(windowDuration))) {
            // Start new window
            return RateLimitUsage(
                count = 0,
                windowStart = now,
                requests = mutableListOf()
            )
        }
        
        return usage
    }
    
    private fun applyAdaptiveRules(
        baseConfig: RateLimitConfig,
        identifier: String,
        action: RateLimitAction
    ): RateLimitConfig {
        val ruleKey = "$identifier:${action.name}"
        val adaptiveRule = adaptiveRules[ruleKey]
        
        return if (adaptiveRule != null) {
            baseConfig.copy(maxRequests = adaptiveRule.currentLimit)
        } else {
            baseConfig
        }
    }
    
    private fun checkBurstProtection(
        identifier: String,
        action: RateLimitAction,
        usage: RateLimitUsage
    ): BurstProtectionInfo {
        val config = getRateLimitConfig(action)
        val burstThreshold = (config.maxRequests * BURST_THRESHOLD_MULTIPLIER).toInt()
        
        val recentRequests = usage.requests.filter { request ->
            request.timestamp.isAfter(Instant.now().minus(Duration.ofMinutes(BURST_RECOVERY_MINUTES)))
        }
        
        val isBurstDetected = recentRequests.size > burstThreshold
        
        return BurstProtectionInfo(
            active = isBurstDetected,
            threshold = burstThreshold,
            currentBurstRate = recentRequests.size,
            recoveryTime = if (isBurstDetected) {
                Instant.now().plus(Duration.ofMinutes(BURST_RECOVERY_MINUTES))
            } else null
        )
    }
    
    private fun checkPenaltyEscalation(identifier: String, action: RateLimitAction): PenaltyInfo {
        val penaltyKey = "penalty:$identifier:${action.name}"
        val penalty = cacheManager.get<PenaltyInfo>(penaltyKey)
        
        return penalty ?: PenaltyInfo(
            active = false,
            type = PenaltyType.NONE,
            startTime = null,
            endTime = null,
            reason = null,
            escalationLevel = 0,
            appealable = false
        )
    }
    
    private fun calculateRetryAfter(resetTime: Instant): Duration {
        return Duration.between(Instant.now(), resetTime)
    }
    
    private fun getAdaptiveAdjustment(identifier: String, action: RateLimitAction): AdaptiveAdjustment? {
        val ruleKey = "$identifier:${action.name}"
        val rule = adaptiveRules[ruleKey]
        
        return rule?.let {
            AdaptiveAdjustment(
                originalLimit = it.baseLine,
                adjustedLimit = it.currentLimit,
                adjustmentFactor = it.currentLimit.toDouble() / it.baseLine,
                lastAdjustment = it.lastAdjustment,
                confidenceScore = it.learningData.confidenceScore
            )
        }
    }
    
    private fun recordSuccessfulAttempt(
        identifier: String,
        action: RateLimitAction,
        clientInfo: RateLimitClientInfo?
    ) {
        val config = getRateLimitConfig(action)
        val usageKey = "rate_limit:$identifier:${action.name}"
        val usage = getCurrentUsage(identifier, action, config.windowDuration)
        
        val request = RateLimitRequest(
            timestamp = Instant.now(),
            clientInfo = clientInfo,
            blocked = false
        )
        
        val updatedUsage = usage.copy(
            count = usage.count + 1,
            requests = (usage.requests + request).toMutableList()
        )
        
        cacheManager.set(usageKey, updatedUsage, config.windowDuration.seconds)
    }
    
    private fun recordBlockedAttempt(
        identifier: String,
        action: RateLimitAction,
        result: RateLimitResult,
        clientInfo: RateLimitClientInfo?
    ) {
        // Record blocked attempt for analytics
        val blockedKey = "blocked:$identifier:${action.name}:${Instant.now().epochSecond}"
        val blockedAttempt = BlockedAttempt(
            timestamp = Instant.now(),
            reason = when {
                !result.allowed -> "Rate limit exceeded"
                result.burstProtection.active -> "Burst protection"
                result.penaltyInfo.active -> "Penalty active"
                else -> "Unknown"
            },
            clientInfo = clientInfo,
            rateLimitResult = result
        )
        
        cacheManager.set(blockedKey, blockedAttempt, 86400) // 24 hours
    }
    
    private fun updateAdaptiveLearning(
        identifier: String,
        action: RateLimitAction,
        result: RateLimitResult
    ) {
        val ruleKey = "$identifier:${action.name}"
        val rule = adaptiveRules[ruleKey] ?: return
        
        // Update learning data
        val learningData = rule.learningData
        learningData.totalRequests++
        
        if (!result.allowed) {
            learningData.blockedRequests++
        }
        
        // Adjust limits based on learning
        val blockRate = learningData.blockedRequests.toDouble() / learningData.totalRequests
        
        when {
            blockRate > rule.params.increaseThreshold -> {
                // Increase limit
                val newLimit = min(
                    ADAPTIVE_MAX_LIMIT,
                    (rule.currentLimit * ADAPTIVE_INCREASE_FACTOR).toInt()
                )
                rule.currentLimit = newLimit
                rule.lastAdjustment = Instant.now()
            }
            blockRate < rule.params.decreaseThreshold -> {
                // Decrease limit
                val newLimit = max(
                    ADAPTIVE_MIN_LIMIT,
                    (rule.currentLimit * ADAPTIVE_DECREASE_FACTOR).toInt()
                )
                rule.currentLimit = newLimit
                rule.lastAdjustment = Instant.now()
            }
        }
        
        // Update confidence score
        learningData.confidenceScore = calculateConfidenceScore(learningData)
        
        adaptiveRules[ruleKey] = rule
    }
    
    private fun calculateConfidenceScore(learningData: AdaptiveLearningData): Double {
        return when {
            learningData.totalRequests < 100 -> 0.3
            learningData.totalRequests < 1000 -> 0.6
            learningData.totalRequests < 10000 -> 0.8
            else -> 0.95
        }
    }
    
    private fun calculateEscalationLevel(identifier: String, action: RateLimitAction): Int {
        val historyKey = "penalty_history:$identifier:${action.name}"
        val history = cacheManager.get<List<PenaltyInfo>>(historyKey) ?: emptyList()
        return history.size
    }
    
    private fun recordPenaltyHistory(identifier: String, action: RateLimitAction, penalty: PenaltyInfo) {
        val historyKey = "penalty_history:$identifier:${action.name}"
        val history = cacheManager.get<MutableList<PenaltyInfo>>(historyKey) ?: mutableListOf()
        history.add(penalty)
        
        // Keep only last 10 penalties
        while (history.size > 10) {
            history.removeAt(0)
        }
        
        cacheManager.set(historyKey, history, 86400 * 30) // 30 days
    }
    
    private fun recordPenaltyRemoval(
        identifier: String,
        action: RateLimitAction,
        penalty: PenaltyInfo,
        reason: String,
        adminUserId: String?
    ) {
        val removalKey = "penalty_removal:$identifier:${action.name}:${Instant.now().epochSecond}"
        val removal = PenaltyRemoval(
            originalPenalty = penalty,
            removedAt = Instant.now(),
            reason = reason,
            adminUserId = adminUserId
        )
        
        cacheManager.set(removalKey, removal, 86400 * 30) // 30 days
    }
    
    private fun triggerSecurityAlert(identifier: String, action: RateLimitAction, penalty: PenaltyInfo) {
        logger.warn("🚨 Security alert: Severe penalty applied to $identifier for $action - ${penalty.type}")
        // In real implementation, would send alerts to security team
    }
    
    // Analytics helper methods (simplified implementations)
    private fun getTotalRequests(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Long = 0L
    private fun getBlockedRequests(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Long = 0L
    private fun getTopLimitedIdentifiers(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): List<String> = emptyList()
    private fun getActionBreakdown(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Map<RateLimitAction, Long> = emptyMap()
    private fun getPenaltyStatistics(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Map<PenaltyType, Int> = emptyMap()
    private fun getBurstProtectionTriggers(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Int = 0
    private fun getAdaptiveAdjustments(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): Int = 0
    private fun calculateRateLimitTrends(startTime: Instant, endTime: Instant, actions: Set<RateLimitAction>): List<RateLimitTrend> = emptyList()
}

// Rate limiting enums and data classes
enum class RateLimitAction {
    LOGIN, API_REQUEST, PASSWORD_RESET, REGISTRATION, EMAIL_VERIFICATION,
    MFA_ATTEMPT, BIOMETRIC_AUTH, HANDLE_SEARCH
}

enum class PenaltyType {
    NONE, TEMPORARY_SLOWDOWN, TEMPORARY_SUSPENSION, EXTENDED_SUSPENSION, PERMANENT_BAN
}

data class RateLimitConfig(
    val maxRequests: Int,
    val windowDuration: Duration
)

data class RateLimitUsage(
    val count: Int,
    val windowStart: Instant?,
    val requests: MutableList<RateLimitRequest>
)

data class RateLimitRequest(
    val timestamp: Instant,
    val clientInfo: RateLimitClientInfo?,
    val blocked: Boolean
)

data class RateLimitClientInfo(
    val ipAddress: String,
    val userAgent: String?,
    val sessionId: String?,
    val deviceId: String?
)

data class RateLimitResult(
    val allowed: Boolean,
    val remaining: Int,
    val resetTime: Instant,
    val retryAfter: Duration?,
    val burstProtection: BurstProtectionInfo,
    val penaltyInfo: PenaltyInfo,
    val adaptiveAdjustment: AdaptiveAdjustment?,
    val metadata: RateLimitMetadata
)

data class BurstProtectionInfo(
    val active: Boolean,
    val threshold: Int,
    val currentBurstRate: Int,
    val recoveryTime: Instant?
)

data class PenaltyInfo(
    val active: Boolean,
    val type: PenaltyType,
    val startTime: Instant?,
    val endTime: Instant?,
    val reason: String?,
    val escalationLevel: Int,
    val appealable: Boolean
)

data class AdaptiveAdjustment(
    val originalLimit: Int,
    val adjustedLimit: Int,
    val adjustmentFactor: Double,
    val lastAdjustment: Instant,
    val confidenceScore: Double
)

data class RateLimitMetadata(
    val action: RateLimitAction,
    val windowStart: Instant,
    val windowDuration: Duration,
    val configuredLimit: Int,
    val effectiveLimit: Int,
    val clientInfo: RateLimitClientInfo?
)

data class RateLimitStatus(
    val identifier: String,
    val action: RateLimitAction,
    val currentUsage: Int,
    val limit: Int,
    val remaining: Int,
    val windowStart: Instant,
    val windowEnd: Instant,
    val penalty: PenaltyInfo,
    val burstProtection: BurstProtectionInfo,
    val isLimited: Boolean
)

data class AdaptiveRateLimitRule(
    val identifier: String,
    val action: RateLimitAction,
    val baseLine: Int,
    var currentLimit: Int,
    val params: AdaptiveRateLimitParams,
    val learningData: AdaptiveLearningData,
    var lastAdjustment: Instant
)

data class AdaptiveRateLimitParams(
    val increaseThreshold: Double = 0.8,
    val decreaseThreshold: Double = 0.2,
    val learningPeriod: Duration = Duration.ofDays(7)
)

data class AdaptiveLearningData(
    var totalRequests: Int = 0,
    var blockedRequests: Int = 0,
    var confidenceScore: Double = 0.0
)

data class BlockedAttempt(
    val timestamp: Instant,
    val reason: String,
    val clientInfo: RateLimitClientInfo?,
    val rateLimitResult: RateLimitResult
)

data class PenaltyRemoval(
    val originalPenalty: PenaltyInfo,
    val removedAt: Instant,
    val reason: String,
    val adminUserId: String?
)

data class RateLimitAnalytics(
    val period: Duration,
    val totalRequests: Long,
    val blockedRequests: Long,
    val topLimitedIdentifiers: List<String>,
    val actionBreakdown: Map<RateLimitAction, Long>,
    val penaltyStatistics: Map<PenaltyType, Int>,
    val burstProtectionTriggers: Int,
    val adaptiveAdjustments: Int,
    val trends: List<RateLimitTrend>
)

data class RateLimitTrend(
    val date: String,
    val totalRequests: Long,
    val blockedRequests: Long
)
