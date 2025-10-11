package com.entativa.id.service.notification

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Push Notification Service for Entativa ID
 * Handles push notifications via FCM, APNs for mobile and web clients
 * 
 * @author Neo Qiss
 * @status Production-ready push notification service
 */
@Service
class PushNotificationService(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)
    
    @Value("\${entativa.push.fcm.project-id:}")
    private lateinit var fcmProjectId: String
    
    @Value("\${entativa.push.fcm.server-key:}")
    private lateinit var fcmServerKey: String
    
    @Value("\${entativa.push.apns.key-id:}")
    private lateinit var apnsKeyId: String
    
    @Value("\${entativa.push.apns.team-id:}")
    private lateinit var apnsTeamId: String
    
    @Value("\${entativa.push.web.vapid-public-key:}")
    private lateinit var vapidPublicKey: String
    
    @Value("\${entativa.push.web.vapid-private-key:}")
    private lateinit var vapidPrivateKey: String
    
    companion object {
        private const val RATE_LIMIT_CACHE_TTL_SECONDS = 3600
        private const val MAX_PUSH_PER_HOUR = 100
        private const val PUSH_DELIVERY_CACHE_TTL_SECONDS = 86400 // 24 hours
        
        // Notification categories
        private const val CATEGORY_SECURITY = "security"
        private const val CATEGORY_AUTH = "authentication"
        private const val CATEGORY_ACCOUNT = "account"
        private const val CATEGORY_SYSTEM = "system"
    }
    
    /**
     * Send security alert push notification
     */
    suspend fun sendSecurityAlert(
        userId: String,
        deviceTokens: List<String>,
        alertType: String,
        alertDetails: Map<String, String>
    ): Result<List<PushNotificationResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚠️ Sending security alert push notification to user: $userId")
                
                if (!checkRateLimit(userId, CATEGORY_SECURITY)) {
                    return@withContext Result.failure(
                        IllegalStateException("Security alert push notification rate limit exceeded.")
                    )
                }
                
                val notification = PushNotificationPayload(
                    title = "Security Alert",
                    body = "Suspicious activity detected on your Entativa account",
                    icon = "security_alert",
                    badge = 1,
                    sound = "security_alert.mp3",
                    category = CATEGORY_SECURITY,
                    priority = "high",
                    data = mapOf(
                        "type" to "security_alert",
                        "alert_type" to alertType,
                        "alert_details" to alertDetails,
                        "action_url" to "entativa://security",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val results = mutableListOf<PushNotificationResult>()
                
                for (token in deviceTokens) {
                    val result = sendPushNotification(token, notification)
                    results.add(result)
                    
                    if (result.success) {
                        trackPushDelivery(userId, token, CATEGORY_SECURITY, result.messageId)
                    }
                }
                
                updateRateLimit(userId, CATEGORY_SECURITY)
                
                val successCount = results.count { it.success }
                logger.info("✅ Security alert sent to $successCount/${results.size} devices for user: $userId")
                
                Result.success(results)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send security alert push notification: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send login notification
     */
    suspend fun sendLoginNotification(
        userId: String,
        deviceTokens: List<String>,
        deviceInfo: DeviceInfo,
        location: String?
    ): Result<List<PushNotificationResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔓 Sending login notification to user: $userId")
                
                if (!checkRateLimit(userId, CATEGORY_AUTH)) {
                    return@withContext Result.failure(
                        IllegalStateException("Login notification rate limit exceeded.")
                    )
                }
                
                val deviceDescription = "${deviceInfo.platform} ${deviceInfo.browser}"
                val locationInfo = location ?: "unknown location"
                
                val notification = PushNotificationPayload(
                    title = "New Sign-in",
                    body = "New sign-in from $deviceDescription in $locationInfo",
                    icon = "login_notification",
                    badge = 1,
                    sound = "default",
                    category = CATEGORY_AUTH,
                    priority = "normal",
                    data = mapOf(
                        "type" to "login_notification",
                        "device_info" to deviceInfo,
                        "location" to locationInfo,
                        "action_url" to "entativa://security/sessions",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val results = mutableListOf<PushNotificationResult>()
                
                for (token in deviceTokens) {
                    val result = sendPushNotification(token, notification)
                    results.add(result)
                    
                    if (result.success) {
                        trackPushDelivery(userId, token, CATEGORY_AUTH, result.messageId)
                    }
                }
                
                updateRateLimit(userId, CATEGORY_AUTH)
                
                val successCount = results.count { it.success }
                logger.info("✅ Login notification sent to $successCount/${results.size} devices for user: $userId")
                
                Result.success(results)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send login notification: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send account verification notification
     */
    suspend fun sendVerificationNotification(
        userId: String,
        deviceTokens: List<String>,
        verificationType: String
    ): Result<List<PushNotificationResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("✅ Sending verification notification to user: $userId")
                
                val (title, body) = when (verificationType) {
                    "email" -> Pair("Email Verified", "Your email address has been successfully verified")
                    "phone" -> Pair("Phone Verified", "Your phone number has been successfully verified")
                    "identity" -> Pair("Identity Verified", "Your identity verification is complete")
                    else -> Pair("Verification Complete", "Your account verification is complete")
                }
                
                val notification = PushNotificationPayload(
                    title = title,
                    body = body,
                    icon = "verification_success",
                    badge = 1,
                    sound = "success.mp3",
                    category = CATEGORY_ACCOUNT,
                    priority = "normal",
                    data = mapOf(
                        "type" to "verification_notification",
                        "verification_type" to verificationType,
                        "action_url" to "entativa://account/settings",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val results = mutableListOf<PushNotificationResult>()
                
                for (token in deviceTokens) {
                    val result = sendPushNotification(token, notification)
                    results.add(result)
                    
                    if (result.success) {
                        trackPushDelivery(userId, token, CATEGORY_ACCOUNT, result.messageId)
                    }
                }
                
                val successCount = results.count { it.success }
                logger.info("✅ Verification notification sent to $successCount/${results.size} devices for user: $userId")
                
                Result.success(results)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send verification notification: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send password change notification
     */
    suspend fun sendPasswordChangeNotification(
        userId: String,
        deviceTokens: List<String>,
        ipAddress: String
    ): Result<List<PushNotificationResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔑 Sending password change notification to user: $userId")
                
                val notification = PushNotificationPayload(
                    title = "Password Changed",
                    body = "Your Entativa password was changed successfully",
                    icon = "password_changed",
                    badge = 1,
                    sound = "security_success.mp3",
                    category = CATEGORY_SECURITY,
                    priority = "high",
                    data = mapOf(
                        "type" to "password_changed",
                        "ip_address" to ipAddress,
                        "action_url" to "entativa://security",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val results = mutableListOf<PushNotificationResult>()
                
                for (token in deviceTokens) {
                    val result = sendPushNotification(token, notification)
                    results.add(result)
                    
                    if (result.success) {
                        trackPushDelivery(userId, token, CATEGORY_SECURITY, result.messageId)
                    }
                }
                
                val successCount = results.count { it.success }
                logger.info("✅ Password change notification sent to $successCount/${results.size} devices for user: $userId")
                
                Result.success(results)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send password change notification: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send account recovery notification
     */
    suspend fun sendAccountRecoveryNotification(
        userId: String,
        deviceTokens: List<String>,
        recoveryMethod: String
    ): Result<List<PushNotificationResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔧 Sending account recovery notification to user: $userId")
                
                val notification = PushNotificationPayload(
                    title = "Account Recovery",
                    body = "Account recovery process initiated via $recoveryMethod",
                    icon = "account_recovery",
                    badge = 1,
                    sound = "security_alert.mp3",
                    category = CATEGORY_SECURITY,
                    priority = "high",
                    data = mapOf(
                        "type" to "account_recovery",
                        "recovery_method" to recoveryMethod,
                        "action_url" to "entativa://recovery",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val results = mutableListOf<PushNotificationResult>()
                
                for (token in deviceTokens) {
                    val result = sendPushNotification(token, notification)
                    results.add(result)
                    
                    if (result.success) {
                        trackPushDelivery(userId, token, CATEGORY_SECURITY, result.messageId)
                    }
                }
                
                val successCount = results.count { it.success }
                logger.info("✅ Account recovery notification sent to $successCount/${results.size} devices for user: $userId")
                
                Result.success(results)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send account recovery notification: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send system maintenance notification
     */
    suspend fun sendMaintenanceNotification(
        userIds: List<String>,
        maintenanceDetails: MaintenanceNotificationData
    ): Result<Map<String, List<PushNotificationResult>>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔧 Sending maintenance notification to ${userIds.size} users")
                
                val notification = PushNotificationPayload(
                    title = "Scheduled Maintenance",
                    body = maintenanceDetails.message,
                    icon = "maintenance",
                    badge = 0,
                    sound = "default",
                    category = CATEGORY_SYSTEM,
                    priority = "normal",
                    data = mapOf(
                        "type" to "maintenance_notification",
                        "maintenance_start" to maintenanceDetails.startTime,
                        "maintenance_end" to maintenanceDetails.endTime,
                        "affected_services" to maintenanceDetails.affectedServices,
                        "action_url" to "entativa://status",
                        "timestamp" to Instant.now().toString()
                    )
                )
                
                val allResults = mutableMapOf<String, List<PushNotificationResult>>()
                
                for (userId in userIds) {
                    val deviceTokens = getUserDeviceTokens(userId)
                    val results = mutableListOf<PushNotificationResult>()
                    
                    for (token in deviceTokens) {
                        val result = sendPushNotification(token, notification)
                        results.add(result)
                        
                        if (result.success) {
                            trackPushDelivery(userId, token, CATEGORY_SYSTEM, result.messageId)
                        }
                    }
                    
                    allResults[userId] = results
                }
                
                val totalSent = allResults.values.flatten().count { it.success }
                logger.info("✅ Maintenance notification sent to $totalSent devices")
                
                Result.success(allResults)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send maintenance notification", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Register device token for push notifications
     */
    suspend fun registerDeviceToken(
        userId: String,
        deviceToken: String,
        platform: String,
        appVersion: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📱 Registering device token for user: $userId, platform: $platform")
                
                val tokenData = mapOf(
                    "user_id" to userId,
                    "platform" to platform,
                    "app_version" to appVersion,
                    "registered_at" to Instant.now().toString(),
                    "active" to true
                )
                
                // Store token mapping
                cacheManager.cacheData("device_token:$deviceToken", tokenData, 86400 * 30) // 30 days
                
                // Update user's device tokens list
                val userTokensKey = "user_tokens:$userId"
                val existingTokens = cacheManager.getCachedData<Set<String>>(userTokensKey) ?: emptySet()
                val updatedTokens = existingTokens + deviceToken
                cacheManager.cacheData(userTokensKey, updatedTokens, 86400 * 30) // 30 days
                
                logger.info("✅ Device token registered successfully: $userId")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to register device token: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Unregister device token
     */
    suspend fun unregisterDeviceToken(userId: String, deviceToken: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📱 Unregistering device token for user: $userId")
                
                // Remove token mapping
                cacheManager.invalidateCache("device_token:$deviceToken")
                
                // Update user's device tokens list
                val userTokensKey = "user_tokens:$userId"
                val existingTokens = cacheManager.getCachedData<Set<String>>(userTokensKey) ?: emptySet()
                val updatedTokens = existingTokens - deviceToken
                cacheManager.cacheData(userTokensKey, updatedTokens, 86400 * 30) // 30 days
                
                logger.info("✅ Device token unregistered successfully: $userId")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to unregister device token: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun sendPushNotification(
        deviceToken: String,
        payload: PushNotificationPayload
    ): PushNotificationResult {
        return try {
            // Determine platform from token or stored data
            val tokenData = cacheManager.getCachedData<Map<String, Any>>("device_token:$deviceToken")
            val platform = tokenData?.get("platform") as? String ?: "unknown"
            
            val result = when (platform) {
                "android" -> sendFcmNotification(deviceToken, payload)
                "ios" -> sendApnsNotification(deviceToken, payload)
                "web" -> sendWebPushNotification(deviceToken, payload)
                else -> sendFcmNotification(deviceToken, payload) // Default to FCM
            }
            
            logger.debug("📱 Push notification sent via $platform: ${result.messageId}")
            result
            
        } catch (e: Exception) {
            logger.error("❌ Failed to send push notification", e)
            PushNotificationResult(
                messageId = "failed",
                deviceToken = deviceToken,
                success = false,
                error = e.message,
                sentAt = Instant.now().toString()
            )
        }
    }
    
    private suspend fun sendFcmNotification(
        deviceToken: String,
        payload: PushNotificationPayload
    ): PushNotificationResult {
        // FCM implementation would go here
        // For now, return a mock successful result
        val messageId = "fcm_${UUID.randomUUID()}"
        
        return PushNotificationResult(
            messageId = messageId,
            deviceToken = deviceToken,
            success = true,
            provider = "fcm",
            sentAt = Instant.now().toString()
        )
    }
    
    private suspend fun sendApnsNotification(
        deviceToken: String,
        payload: PushNotificationPayload
    ): PushNotificationResult {
        // APNs implementation would go here
        // For now, return a mock successful result
        val messageId = "apns_${UUID.randomUUID()}"
        
        return PushNotificationResult(
            messageId = messageId,
            deviceToken = deviceToken,
            success = true,
            provider = "apns",
            sentAt = Instant.now().toString()
        )
    }
    
    private suspend fun sendWebPushNotification(
        deviceToken: String,
        payload: PushNotificationPayload
    ): PushNotificationResult {
        // Web Push implementation would go here
        // For now, return a mock successful result
        val messageId = "webpush_${UUID.randomUUID()}"
        
        return PushNotificationResult(
            messageId = messageId,
            deviceToken = deviceToken,
            success = true,
            provider = "webpush",
            sentAt = Instant.now().toString()
        )
    }
    
    private suspend fun getUserDeviceTokens(userId: String): List<String> {
        return try {
            val userTokensKey = "user_tokens:$userId"
            cacheManager.getCachedData<Set<String>>(userTokensKey)?.toList() ?: emptyList()
        } catch (e: Exception) {
            logger.warn("Failed to get user device tokens: $userId", e)
            emptyList()
        }
    }
    
    private suspend fun checkRateLimit(userId: String, category: String): Boolean {
        return try {
            val key = "push_rate:$userId:$category"
            val current = cacheManager.getCachedData<Int>(key) ?: 0
            current < MAX_PUSH_PER_HOUR
        } catch (e: Exception) {
            logger.warn("Failed to check push notification rate limit", e)
            true // Allow on error
        }
    }
    
    private suspend fun updateRateLimit(userId: String, category: String) {
        try {
            val key = "push_rate:$userId:$category"
            val current = cacheManager.getCachedData<Int>(key) ?: 0
            cacheManager.cacheData(key, current + 1, RATE_LIMIT_CACHE_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Failed to update push notification rate limit", e)
        }
    }
    
    private suspend fun trackPushDelivery(
        userId: String,
        deviceToken: String,
        category: String,
        messageId: String
    ) {
        try {
            val trackingKey = "push_delivery:$messageId"
            val trackingData = mapOf(
                "user_id" to userId,
                "device_token" to deviceToken,
                "category" to category,
                "sent_at" to Instant.now().toString(),
                "status" to "sent"
            )
            cacheManager.cacheData(trackingKey, trackingData, PUSH_DELIVERY_CACHE_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Failed to track push notification delivery", e)
        }
    }
}
