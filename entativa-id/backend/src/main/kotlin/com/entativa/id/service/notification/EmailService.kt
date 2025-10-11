package com.entativa.id.service.notification

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.time.Instant
import java.util.*
import javax.mail.internet.MimeMessage

/**
 * Email Service for Entativa ID
 * Handles all email communications with template rendering and delivery tracking
 * 
 * @author Neo Qiss
 * @status Production-ready email service
 */
@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(EmailService::class.java)
    
    @Value("\${entativa.email.from:noreply@entativa.com}")
    private lateinit var fromEmail: String
    
    @Value("\${entativa.email.from-name:Entativa}")
    private lateinit var fromName: String
    
    @Value("\${entativa.email.reply-to:support@entativa.com}")
    private lateinit var replyToEmail: String
    
    @Value("\${entativa.web.url:https://entativa.com}")
    private lateinit var webUrl: String
    
    @Value("\${entativa.id.url:https://id.entativa.com}")
    private lateinit var idUrl: String
    
    companion object {
        private const val RATE_LIMIT_CACHE_TTL_SECONDS = 3600
        private const val MAX_EMAILS_PER_HOUR = 10
        private const val EMAIL_DELIVERY_CACHE_TTL_SECONDS = 86400 // 24 hours
    }
    
    /**
     * Send email verification
     */
    suspend fun sendEmailVerification(
        email: String,
        userName: String,
        verificationCode: String,
        expiresInMinutes: Int
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📧 Sending email verification to: $email")
                
                // Check rate limiting
                if (!checkRateLimit(email, "verification")) {
                    return@withContext Result.failure(
                        IllegalStateException("Email rate limit exceeded. Please try again later.")
                    )
                }
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("verificationCode", verificationCode)
                    setVariable("expiresInMinutes", expiresInMinutes)
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/verification", context)
                val textContent = generateTextContent("verification", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Verify your Entativa account",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "verification"
                )
                
                // Track delivery
                trackEmailDelivery(email, "verification", deliveryResult.messageId)
                updateRateLimit(email, "verification")
                
                logger.info("✅ Email verification sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send email verification: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(
        email: String,
        userName: String,
        resetUrl: String,
        expiresInMinutes: Int
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔑 Sending password reset email to: $email")
                
                if (!checkRateLimit(email, "password_reset")) {
                    return@withContext Result.failure(
                        IllegalStateException("Password reset email rate limit exceeded.")
                    )
                }
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("resetUrl", resetUrl)
                    setVariable("expiresInMinutes", expiresInMinutes)
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/password-reset", context)
                val textContent = generateTextContent("password-reset", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Reset your Entativa password",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "password_reset"
                )
                
                trackEmailDelivery(email, "password_reset", deliveryResult.messageId)
                updateRateLimit(email, "password_reset")
                
                logger.info("✅ Password reset email sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send password reset email: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send welcome email
     */
    suspend fun sendWelcomeEmail(
        email: String,
        userName: String,
        displayName: String
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🎉 Sending welcome email to: $email")
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("displayName", displayName)
                    setVariable("profileUrl", "$webUrl/profile")
                    setVariable("settingsUrl", "$idUrl/settings")
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/welcome", context)
                val textContent = generateTextContent("welcome", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Welcome to Entativa! 🎉",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "welcome"
                )
                
                trackEmailDelivery(email, "welcome", deliveryResult.messageId)
                
                logger.info("✅ Welcome email sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send welcome email: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send password change notification
     */
    suspend fun sendPasswordChangeNotification(
        email: String,
        userName: String,
        ipAddress: String,
        timestamp: String
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Sending password change notification to: $email")
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("ipAddress", ipAddress)
                    setVariable("timestamp", timestamp)
                    setVariable("securityUrl", "$idUrl/security")
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/password-changed", context)
                val textContent = generateTextContent("password-changed", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Your Entativa password was changed",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "security_notification"
                )
                
                trackEmailDelivery(email, "password_changed", deliveryResult.messageId)
                
                logger.info("✅ Password change notification sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send password change notification: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send security alert email
     */
    suspend fun sendSecurityAlert(
        email: String,
        userName: String,
        alertType: String,
        alertDetails: Map<String, String>
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚠️ Sending security alert to: $email - Type: $alertType")
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("alertType", alertType)
                    setVariable("alertDetails", alertDetails)
                    setVariable("securityUrl", "$idUrl/security")
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("timestamp", Instant.now().toString())
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/security-alert", context)
                val textContent = generateTextContent("security-alert", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Security Alert - Entativa Account",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "security_alert",
                    priority = "high"
                )
                
                trackEmailDelivery(email, "security_alert", deliveryResult.messageId)
                
                logger.info("✅ Security alert sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send security alert: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send account recovery email
     */
    suspend fun sendAccountRecoveryEmail(
        email: String,
        userName: String,
        recoveryCode: String,
        recoveryUrl: String,
        expiresInHours: Int
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔧 Sending account recovery email to: $email")
                
                if (!checkRateLimit(email, "account_recovery")) {
                    return@withContext Result.failure(
                        IllegalStateException("Account recovery email rate limit exceeded.")
                    )
                }
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("recoveryCode", recoveryCode)
                    setVariable("recoveryUrl", recoveryUrl)
                    setVariable("expiresInHours", expiresInHours)
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/account-recovery", context)
                val textContent = generateTextContent("account-recovery", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "Recover your Entativa account",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "account_recovery",
                    priority = "high"
                )
                
                trackEmailDelivery(email, "account_recovery", deliveryResult.messageId)
                updateRateLimit(email, "account_recovery")
                
                logger.info("✅ Account recovery email sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send account recovery email: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send login notification
     */
    suspend fun sendLoginNotification(
        email: String,
        userName: String,
        deviceInfo: DeviceInfo,
        ipAddress: String,
        location: String?
    ): Result<EmailDeliveryResult> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔓 Sending login notification to: $email")
                
                val context = Context().apply {
                    setVariable("userName", userName)
                    setVariable("deviceInfo", deviceInfo)
                    setVariable("ipAddress", ipAddress)
                    setVariable("location", location ?: "Unknown location")
                    setVariable("timestamp", Instant.now().toString())
                    setVariable("securityUrl", "$idUrl/security")
                    setVariable("supportEmail", replyToEmail)
                    setVariable("webUrl", webUrl)
                    setVariable("year", Calendar.getInstance().get(Calendar.YEAR))
                }
                
                val htmlContent = templateEngine.process("email/login-notification", context)
                val textContent = generateTextContent("login-notification", context)
                
                val deliveryResult = sendEmail(
                    to = email,
                    subject = "New sign-in to your Entativa account",
                    htmlContent = htmlContent,
                    textContent = textContent,
                    emailType = "login_notification"
                )
                
                trackEmailDelivery(email, "login_notification", deliveryResult.messageId)
                
                logger.info("✅ Login notification sent successfully: $email")
                Result.success(deliveryResult)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to send login notification: $email", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private suspend fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        textContent: String,
        emailType: String,
        priority: String = "normal"
    ): EmailDeliveryResult {
        return try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            helper.setFrom(fromEmail, fromName)
            helper.setTo(to)
            helper.setReplyTo(replyToEmail)
            helper.setSubject(subject)
            helper.setText(textContent, htmlContent)
            
            // Add headers
            message.setHeader("X-Email-Type", emailType)
            message.setHeader("X-Priority", if (priority == "high") "1" else "3")
            message.setHeader("X-Mailer", "Entativa ID Service")
            
            mailSender.send(message)
            
            val messageId = UUID.randomUUID().toString()
            
            EmailDeliveryResult(
                messageId = messageId,
                recipient = to,
                subject = subject,
                emailType = emailType,
                status = "sent",
                sentAt = Instant.now().toString()
            )
            
        } catch (e: Exception) {
            logger.error("❌ Failed to send email to: $to", e)
            EmailDeliveryResult(
                messageId = "failed",
                recipient = to,
                subject = subject,
                emailType = emailType,
                status = "failed",
                sentAt = Instant.now().toString(),
                error = e.message
            )
        }
    }
    
    private fun generateTextContent(template: String, context: Context): String {
        // Generate plain text version of email content
        return when (template) {
            "verification" -> """
                Hi ${context.getVariable("userName")},
                
                Please verify your email address using this code:
                ${context.getVariable("verificationCode")}
                
                This code expires in ${context.getVariable("expiresInMinutes")} minutes.
                
                If you didn't request this, please ignore this email.
                
                Best regards,
                The Entativa Team
                ${context.getVariable("supportEmail")}
            """.trimIndent()
            
            "password-reset" -> """
                Hi ${context.getVariable("userName")},
                
                You requested to reset your password. Click the link below:
                ${context.getVariable("resetUrl")}
                
                This link expires in ${context.getVariable("expiresInMinutes")} minutes.
                
                If you didn't request this, please ignore this email.
                
                Best regards,
                The Entativa Team
                ${context.getVariable("supportEmail")}
            """.trimIndent()
            
            "welcome" -> """
                Welcome to Entativa, ${context.getVariable("displayName")}!
                
                Your account has been created successfully. Here's what you can do next:
                
                1. Complete your profile: ${context.getVariable("profileUrl")}
                2. Adjust your settings: ${context.getVariable("settingsUrl")}
                3. Explore our platforms and features
                
                If you have any questions, contact us at ${context.getVariable("supportEmail")}
                
                Welcome aboard!
                The Entativa Team
            """.trimIndent()
            
            else -> "Please view this email in HTML format for the best experience."
        }
    }
    
    private suspend fun checkRateLimit(email: String, emailType: String): Boolean {
        return try {
            val key = "email_rate:$email:$emailType"
            val current = cacheManager.getCachedData<Int>(key) ?: 0
            current < MAX_EMAILS_PER_HOUR
        } catch (e: Exception) {
            logger.warn("Failed to check email rate limit", e)
            true // Allow on error
        }
    }
    
    private suspend fun updateRateLimit(email: String, emailType: String) {
        try {
            val key = "email_rate:$email:$emailType"
            val current = cacheManager.getCachedData<Int>(key) ?: 0
            cacheManager.cacheData(key, current + 1, RATE_LIMIT_CACHE_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Failed to update email rate limit", e)
        }
    }
    
    private suspend fun trackEmailDelivery(email: String, emailType: String, messageId: String) {
        try {
            val trackingKey = "email_delivery:$messageId"
            val trackingData = mapOf(
                "email" to email,
                "type" to emailType,
                "sent_at" to Instant.now().toString(),
                "status" to "sent"
            )
            cacheManager.cacheData(trackingKey, trackingData, EMAIL_DELIVERY_CACHE_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Failed to track email delivery", e)
        }
    }
}
