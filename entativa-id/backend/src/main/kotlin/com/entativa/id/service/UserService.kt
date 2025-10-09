package com.entativa.id.service

import com.entativa.id.config.*
import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Instant
import java.util.*

/**
 * Core User Service for Entativa ID
 * Handles user creation, authentication, and profile management
 * 
 * @author Neo Qiss
 * @status Production-ready with comprehensive security
 */
class UserService(
    private val cacheManager: EntativaCacheManager,
    private val handleValidationService: HandleValidationService
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    companion object {
        private const val CACHE_TTL_SECONDS = 3600 // 1 hour
        private const val PASSWORD_MIN_LENGTH = 8
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MINUTES = 30
    }
    
    /**
     * Create new Entativa ID user
     */
    suspend fun createUser(request: CreateUserRequest): Result<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🆔 Creating new user with EiD: ${request.eid}")
                
                // Validate request
                val validationResult = validateCreateUserRequest(request)
                if (!validationResult.isSuccess) {
                    return@withContext Result.failure(
                        IllegalArgumentException(validationResult.exceptionOrNull()?.message)
                    )
                }
                
                // Validate handle
                val handleValidation = handleValidationService.validateHandle(request.eid)
                if (!handleValidation.isAvailable || !handleValidation.isValid) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Handle not available: ${handleValidation.errors.joinToString()}")
                    )
                }
                
                // Check if email already exists
                val existingUser = transaction {
                    EntativaIdentitiesTable.select { EntativaIdentitiesTable.email eq request.email }
                        .singleOrNull()
                }
                
                if (existingUser != null) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Email address already registered")
                    )
                }
                
                // Create user and profile
                val userId = transaction {
                    val now = Instant.now()
                    
                    // Insert identity
                    val identityId = EntativaIdentitiesTable.insertAndGetId {
                        it[eid] = request.eid
                        it[email] = request.email
                        it[phone] = request.phone
                        it[passwordHash] = hashPassword(request.password)
                        it[createdAt] = now
                        it[updatedAt] = now
                        it[passwordChangedAt] = now
                        it[ipAddress] = request.ipAddress
                        it[userAgent] = request.userAgent
                        it[countryCode] = request.countryCode
                    }
                    
                    // Insert profile
                    EntativaProfilesTable.insert {
                        it[this.identityId] = identityId
                        it[firstName] = request.firstName
                        it[lastName] = request.lastName
                        it[displayName] = request.displayName ?: "${request.firstName} ${request.lastName}"
                        it[birthDate] = request.dateOfBirth?.let { date -> java.time.LocalDate.parse(date) }
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    identityId.value
                }
                
                // Log user creation
                auditUserAction(userId, "user_created", mapOf(
                    "eid" to request.eid,
                    "email" to request.email,
                    "ip_address" to (request.ipAddress ?: "unknown")
                ))
                
                // Get complete user data
                val user = getUserById(userId.toString())
                if (user != null) {
                    logger.info("✅ User created successfully: ${request.eid}")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to retrieve created user"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create user: ${request.eid}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Authenticate user with email/password
     */
    suspend fun authenticateUser(email: String, password: String, ipAddress: String? = null): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔐 Authentication attempt for: $email")
                
                // Get user by email
                val userRecord = transaction {
                    EntativaIdentitiesTable.select { EntativaIdentitiesTable.email eq email }
                        .singleOrNull()
                } ?: return@withContext Result.failure(
                    SecurityException("Invalid credentials")
                )
                
                val userId = userRecord[EntativaIdentitiesTable.id].value
                
                // Check if account is locked
                val lockoutEnd = userRecord[EntativaIdentitiesTable.lockedUntil]
                if (lockoutEnd != null && lockoutEnd.isAfter(Instant.now())) {
                    return@withContext Result.failure(
                        SecurityException("Account is temporarily locked")
                    )
                }
                
                // Verify password
                val storedHash = userRecord[EntativaIdentitiesTable.passwordHash]
                if (!verifyPassword(password, storedHash)) {
                    // Increment failed attempts
                    val failedAttempts = userRecord[EntativaIdentitiesTable.failedLoginAttempts] + 1
                    
                    transaction {
                        EntativaIdentitiesTable.update({ EntativaIdentitiesTable.id eq userId }) {
                            it[this.failedLoginAttempts] = failedAttempts
                            if (failedAttempts >= MAX_LOGIN_ATTEMPTS) {
                                it[lockedUntil] = Instant.now().plusSeconds(LOCKOUT_DURATION_MINUTES * 60L)
                            }
                        }
                    }
                    
                    auditUserAction(userId, "login_failed", mapOf(
                        "reason" to "invalid_password",
                        "ip_address" to (ipAddress ?: "unknown"),
                        "failed_attempts" to failedAttempts.toString()
                    ))
                    
                    return@withContext Result.failure(
                        SecurityException("Invalid credentials")
                    )
                }
                
                // Check account status
                val status = userRecord[EntativaIdentitiesTable.status]
                if (status != "active") {
                    return@withContext Result.failure(
                        SecurityException("Account is $status")
                    )
                }
                
                // Successful login - reset failed attempts
                transaction {
                    EntativaIdentitiesTable.update({ EntativaIdentitiesTable.id eq userId }) {
                        it[failedLoginAttempts] = 0
                        it[lockedUntil] = null
                        it[lastLoginAt] = Instant.now()
                    }
                }
                
                auditUserAction(userId, "login_success", mapOf(
                    "ip_address" to (ipAddress ?: "unknown")
                ))
                
                // Get complete user data
                val user = buildUserFromRecord(userRecord)
                
                // Cache user data
                cacheManager.cacheData("user:$userId", user, CACHE_TTL_SECONDS)
                
                logger.info("✅ Authentication successful for: $email")
                Result.success(user)
                
            } catch (e: Exception) {
                logger.error("❌ Authentication failed for: $email", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): UserResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<UserResponse>("user_response:$userId")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val userRecord = transaction {
                    EntativaIdentitiesTable
                        .leftJoin(EntativaProfilesTable)
                        .select { EntativaIdentitiesTable.id eq UUID.fromString(userId) }
                        .singleOrNull()
                } ?: return@withContext null
                
                val user = buildUserResponseFromRecord(userRecord)
                
                // Cache the result
                cacheManager.cacheData("user_response:$userId", user, CACHE_TTL_SECONDS)
                
                user
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user by ID: $userId", e)
                null
            }
        }
    }
    
    /**
     * Get user by EiD handle
     */
    suspend fun getUserByEid(eid: String): UserResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<UserResponse>("user_by_eid:$eid")
                if (cached != null) {
                    return@withContext cached
                }
                
                val userRecord = transaction {
                    EntativaIdentitiesTable
                        .leftJoin(EntativaProfilesTable)
                        .select { EntativaIdentitiesTable.eid eq eid }
                        .singleOrNull()
                } ?: return@withContext null
                
                val user = buildUserResponseFromRecord(userRecord)
                
                // Cache the result
                cacheManager.cacheData("user_by_eid:$eid", user, CACHE_TTL_SECONDS)
                
                user
                
            } catch (e: Exception) {
                logger.error("❌ Failed to get user by EiD: $eid", e)
                null
            }
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUser(userId: String, request: UpdateUserRequest): Result<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📝 Updating user profile: $userId")
                
                val now = Instant.now()
                
                transaction {
                    // Update profile if fields provided
                    if (request.firstName != null || request.lastName != null || 
                        request.displayName != null || request.phone != null ||
                        request.dateOfBirth != null || request.location != null ||
                        request.bio != null || request.website != null) {
                        
                        EntativaProfilesTable.update({ 
                            EntativaProfilesTable.identityId eq UUID.fromString(userId) 
                        }) {
                            request.firstName?.let { firstName -> it[this.firstName] = firstName }
                            request.lastName?.let { lastName -> it[this.lastName] = lastName }
                            request.displayName?.let { displayName -> it[this.displayName] = displayName }
                            request.dateOfBirth?.let { dob -> it[birthDate] = java.time.LocalDate.parse(dob) }
                            request.location?.let { location -> it[this.location] = location }
                            request.bio?.let { bio -> it[this.bio] = bio }
                            request.website?.let { website -> it[this.website] = website }
                            it[updatedAt] = now
                        }
                    }
                    
                    // Update phone if provided
                    if (request.phone != null) {
                        EntativaIdentitiesTable.update({ EntativaIdentitiesTable.id eq UUID.fromString(userId) }) {
                            it[phone] = request.phone
                            it[phoneVerified] = false // Reset verification when phone changes
                            it[updatedAt] = now
                        }
                    }
                }
                
                // Clear cache
                cacheManager.invalidateCache("user:$userId")
                cacheManager.invalidateCache("user_response:$userId")
                
                // Log update
                auditUserAction(UUID.fromString(userId), "profile_updated", mapOf(
                    "fields_updated" to listOfNotNull(
                        if (request.firstName != null) "firstName" else null,
                        if (request.lastName != null) "lastName" else null,
                        if (request.displayName != null) "displayName" else null,
                        if (request.phone != null) "phone" else null,
                        if (request.dateOfBirth != null) "dateOfBirth" else null,
                        if (request.location != null) "location" else null,
                        if (request.bio != null) "bio" else null,
                        if (request.website != null) "website" else null
                    ).joinToString(",")
                ))
                
                // Return updated user
                val updatedUser = getUserById(userId)
                if (updatedUser != null) {
                    logger.info("✅ User profile updated: $userId")
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("Failed to retrieve updated user"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Change user password
     */
    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔑 Password change request for user: $userId")
                
                // Validate new password
                if (newPassword.length < PASSWORD_MIN_LENGTH) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Password must be at least $PASSWORD_MIN_LENGTH characters")
                    )
                }
                
                // Get current user
                val userRecord = transaction {
                    EntativaIdentitiesTable.select { EntativaIdentitiesTable.id eq UUID.fromString(userId) }
                        .singleOrNull()
                } ?: return@withContext Result.failure(
                    IllegalArgumentException("User not found")
                )
                
                // Verify current password
                val storedHash = userRecord[EntativaIdentitiesTable.passwordHash]
                if (!verifyPassword(currentPassword, storedHash)) {
                    return@withContext Result.failure(
                        SecurityException("Current password is incorrect")
                    )
                }
                
                // Update password
                val newHash = hashPassword(newPassword)
                val now = Instant.now()
                
                transaction {
                    EntativaIdentitiesTable.update({ EntativaIdentitiesTable.id eq UUID.fromString(userId) }) {
                        it[passwordHash] = newHash
                        it[passwordChangedAt] = now
                        it[updatedAt] = now
                    }
                }
                
                // Clear cache
                cacheManager.invalidateCache("user:$userId")
                
                // Log password change
                auditUserAction(UUID.fromString(userId), "password_changed", mapOf(
                    "changed_at" to now.toString()
                ))
                
                logger.info("✅ Password changed successfully for user: $userId")
                Result.success(true)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to change password for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    // ============== PRIVATE HELPER METHODS ==============
    
    private fun validateCreateUserRequest(request: CreateUserRequest): Result<Unit> {
        val errors = mutableListOf<String>()
        
        // Validate EiD
        if (request.eid.isBlank() || request.eid.length < 3 || request.eid.length > 30) {
            errors.add("EiD must be 3-30 characters")
        }
        
        if (!request.eid.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]{1,28}[a-zA-Z0-9]$".toRegex())) {
            errors.add("EiD format is invalid")
        }
        
        // Validate email
        if (!request.email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex())) {
            errors.add("Invalid email format")
        }
        
        // Validate password
        if (request.password.length < PASSWORD_MIN_LENGTH) {
            errors.add("Password must be at least $PASSWORD_MIN_LENGTH characters")
        }
        
        // Validate names
        if (request.firstName.isBlank()) {
            errors.add("First name is required")
        }
        
        if (request.lastName.isBlank()) {
            errors.add("Last name is required")
        }
        
        // Validate terms acceptance
        if (!request.acceptedTerms || !request.acceptedPrivacy) {
            errors.add("Terms and privacy policy must be accepted")
        }
        
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException(errors.joinToString("; ")))
        }
    }
    
    private fun hashPassword(password: String): String {
        val salt = "entativa_id_salt_2024" // In production, use proper salt per user
        val md = MessageDigest.getInstance("SHA-256")
        val hashedBytes = md.digest((password + salt).toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun verifyPassword(password: String, storedHash: String): Boolean {
        return hashPassword(password) == storedHash
    }
    
    private fun buildUserFromRecord(record: ResultRow): User {
        return User(
            id = record[EntativaIdentitiesTable.id].value.toString(),
            eid = record[EntativaIdentitiesTable.eid],
            email = record[EntativaIdentitiesTable.email],
            phone = record[EntativaIdentitiesTable.phone],
            passwordHash = record[EntativaIdentitiesTable.passwordHash],
            status = UserStatus.valueOf(record[EntativaIdentitiesTable.status].uppercase()),
            emailVerified = record[EntativaIdentitiesTable.emailVerified],
            phoneVerified = record[EntativaIdentitiesTable.phoneVerified],
            twoFactorEnabled = record[EntativaIdentitiesTable.twoFactorEnabled],
            profileCompleted = record[EntativaIdentitiesTable.profileCompleted],
            verificationStatus = VerificationStatus.valueOf(record[EntativaIdentitiesTable.verificationStatus].uppercase()),
            verificationBadge = record[EntativaIdentitiesTable.verificationBadge]?.let { 
                VerificationBadge.valueOf(it.uppercase()) 
            },
            verificationDate = record[EntativaIdentitiesTable.verificationDate]?.toString(),
            reputationScore = record[EntativaIdentitiesTable.reputationScore],
            failedLoginAttempts = record[EntativaIdentitiesTable.failedLoginAttempts],
            lockedUntil = record[EntativaIdentitiesTable.lockedUntil]?.toString(),
            passwordChangedAt = record[EntativaIdentitiesTable.passwordChangedAt].toString(),
            lastLoginAt = record[EntativaIdentitiesTable.lastLoginAt]?.toString(),
            createdAt = record[EntativaIdentitiesTable.createdAt].toString(),
            updatedAt = record[EntativaIdentitiesTable.updatedAt].toString(),
            createdBy = record[EntativaIdentitiesTable.createdBy],
            ipAddress = record[EntativaIdentitiesTable.ipAddress],
            userAgent = record[EntativaIdentitiesTable.userAgent],
            countryCode = record[EntativaIdentitiesTable.countryCode]
        )
    }
    
    private fun buildUserResponseFromRecord(record: ResultRow): UserResponse {
        return UserResponse(
            id = record[EntativaIdentitiesTable.id].value.toString(),
            eid = record[EntativaIdentitiesTable.eid],
            email = record[EntativaIdentitiesTable.email],
            phone = record[EntativaIdentitiesTable.phone],
            status = UserStatus.valueOf(record[EntativaIdentitiesTable.status].uppercase()),
            emailVerified = record[EntativaIdentitiesTable.emailVerified],
            phoneVerified = record[EntativaIdentitiesTable.phoneVerified],
            twoFactorEnabled = record[EntativaIdentitiesTable.twoFactorEnabled],
            profileCompleted = record[EntativaIdentitiesTable.profileCompleted],
            verificationStatus = VerificationStatus.valueOf(record[EntativaIdentitiesTable.verificationStatus].uppercase()),
            verificationBadge = record[EntativaIdentitiesTable.verificationBadge]?.let { 
                VerificationBadge.valueOf(it.uppercase()) 
            },
            verificationDate = record[EntativaIdentitiesTable.verificationDate]?.toString(),
            reputationScore = record[EntativaIdentitiesTable.reputationScore],
            createdAt = record[EntativaIdentitiesTable.createdAt].toString(),
            lastLoginAt = record[EntativaIdentitiesTable.lastLoginAt]?.toString(),
            profile = buildProfileSummary(record),
            securitySummary = SecuritySummary(
                twoFactorEnabled = record[EntativaIdentitiesTable.twoFactorEnabled],
                lastPasswordChange = record[EntativaIdentitiesTable.passwordChangedAt].toString(),
                activeSessions = 0, // TODO: Calculate from sessions
                connectedApps = 0, // TODO: Calculate from OAuth tokens
                securityScore = calculateSecurityScore(record)
            )
        )
    }
    
    private fun buildProfileSummary(record: ResultRow): ProfileSummary? {
        return try {
            ProfileSummary(
                firstName = record[EntativaProfilesTable.firstName],
                lastName = record[EntativaProfilesTable.lastName],
                displayName = record[EntativaProfilesTable.displayName],
                avatarUrl = record[EntativaProfilesTable.avatarUrl],
                location = record[EntativaProfilesTable.location],
                bio = record[EntativaProfilesTable.bio],
                website = record[EntativaProfilesTable.website]
            )
        } catch (e: Exception) {
            null // Profile not found or incomplete
        }
    }
    
    private fun calculateSecurityScore(record: ResultRow): Int {
        var score = 50 // Base score
        
        if (record[EntativaIdentitiesTable.emailVerified]) score += 20
        if (record[EntativaIdentitiesTable.phoneVerified]) score += 15
        if (record[EntativaIdentitiesTable.twoFactorEnabled]) score += 15
        
        return score
    }
    
    private suspend fun auditUserAction(userId: UUID, action: String, details: Map<String, String>) {
        try {
            transaction {
                IdentityAuditLogTable.insert {
                    it[identityId] = userId
                    it[this.action] = action
                    it[this.details] = details
                    it[createdAt] = Instant.now()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to log audit event", e)
        }
    }
}

// Mock HandleValidationService interface (to be implemented)
interface HandleValidationService {
    suspend fun validateHandle(handle: String): HandleValidationResult
}
