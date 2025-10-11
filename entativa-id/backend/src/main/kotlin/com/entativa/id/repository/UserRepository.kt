package com.entativa.id.repository

import com.entativa.id.domain.model.*
import com.entativa.shared.cache.EntativaCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

/**
 * User Repository for Entativa ID
 * Handles all user data access operations with comprehensive protection
 * 
 * @author Neo Qiss
 * @status Production-ready user data access
 */
@Repository
class UserRepository(
    private val cacheManager: EntativaCacheManager
) {
    
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)
    
    companion object {
        private const val USER_CACHE_TTL_SECONDS = 3600 // 1 hour
        private const val USER_LIST_CACHE_TTL_SECONDS = 300 // 5 minutes
    }
    
    /**
     * Create new user with comprehensive protection
     */
    suspend fun createUser(request: CreateUserRequest): Result<UserRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("👤 Creating new user with email: ${request.email}")
                
                val userId = UUID.randomUUID()
                val now = Instant.now()
                
                val userRecord = transaction {
                    // Insert into primary identity table
                    val identityId = IdentityTable.insertAndGetId {
                        it[id] = userId
                        it[email] = request.email
                        it[emailVerified] = false
                        it[phone] = request.phone
                        it[phoneVerified] = false
                        it[passwordHash] = request.passwordHash
                        it[createdAt] = now
                        it[updatedAt] = now
                        it[status] = UserStatus.ACTIVE
                        it[protectionLevel] = ProtectionLevel.STANDARD
                    }
                    
                    // Insert into protection tracking
                    IdentityProtectionTrackingTable.insert {
                        it[identityId] = userId
                        it[protectionLevel] = ProtectionLevel.STANDARD
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    // Insert into basic profile
                    BasicProfileTable.insert {
                        it[identityId] = userId
                        it[firstName] = request.firstName
                        it[lastName] = request.lastName
                        it[displayName] = request.displayName ?: "${request.firstName} ${request.lastName}"
                        it[bio] = ""
                        it[avatarUrl] = ""
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    // Insert into preferences with defaults
                    UserPreferenceTable.insert {
                        it[identityId] = userId
                        it[language] = request.preferredLanguage ?: "en"
                        it[timezone] = request.timezone ?: "UTC"
                        it[theme] = "auto"
                        it[notifications] = true
                        it[privacy] = "friends"
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    // Create user record
                    UserRecord(
                        id = userId.toString(),
                        email = request.email,
                        emailVerified = false,
                        phone = request.phone,
                        phoneVerified = false,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        displayName = request.displayName ?: "${request.firstName} ${request.lastName}",
                        bio = "",
                        avatarUrl = "",
                        status = UserStatus.ACTIVE,
                        protectionLevel = ProtectionLevel.STANDARD,
                        preferredLanguage = request.preferredLanguage ?: "en",
                        timezone = request.timezone ?: "UTC",
                        createdAt = now.toString(),
                        updatedAt = now.toString(),
                        lastLoginAt = null
                    )
                }
                
                // Cache user
                cacheManager.cacheData("user:${userId}", userRecord, USER_CACHE_TTL_SECONDS)
                cacheManager.cacheData("user:email:${request.email}", userRecord, USER_CACHE_TTL_SECONDS)
                
                logger.info("✅ User created successfully: $userId")
                Result.success(userRecord)
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create user: ${request.email}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find user by ID with caching
     */
    suspend fun findById(userId: String): UserRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<UserRecord>("user:$userId")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val user = transaction {
                    (IdentityTable innerJoin BasicProfileTable innerJoin UserPreferenceTable)
                        .select { IdentityTable.id eq UUID.fromString(userId) }
                        .map { row ->
                            UserRecord(
                                id = row[IdentityTable.id].toString(),
                                email = row[IdentityTable.email],
                                emailVerified = row[IdentityTable.emailVerified],
                                phone = row[IdentityTable.phone],
                                phoneVerified = row[IdentityTable.phoneVerified],
                                firstName = row[BasicProfileTable.firstName],
                                lastName = row[BasicProfileTable.lastName],
                                displayName = row[BasicProfileTable.displayName],
                                bio = row[BasicProfileTable.bio],
                                avatarUrl = row[BasicProfileTable.avatarUrl],
                                status = row[IdentityTable.status],
                                protectionLevel = row[IdentityTable.protectionLevel],
                                preferredLanguage = row[UserPreferenceTable.language],
                                timezone = row[UserPreferenceTable.timezone],
                                createdAt = row[IdentityTable.createdAt].toString(),
                                updatedAt = row[IdentityTable.updatedAt].toString(),
                                lastLoginAt = row[IdentityTable.lastLoginAt]?.toString()
                            )
                        }
                        .singleOrNull()
                }
                
                // Cache if found
                user?.let {
                    cacheManager.cacheData("user:$userId", it, USER_CACHE_TTL_SECONDS)
                }
                
                user
                
            } catch (e: Exception) {
                logger.error("❌ Failed to find user by ID: $userId", e)
                null
            }
        }
    }
    
    /**
     * Find user by email with caching
     */
    suspend fun findByEmail(email: String): UserRecord? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = cacheManager.getCachedData<UserRecord>("user:email:$email")
                if (cached != null) {
                    return@withContext cached
                }
                
                // Query database
                val user = transaction {
                    (IdentityTable innerJoin BasicProfileTable innerJoin UserPreferenceTable)
                        .select { IdentityTable.email eq email }
                        .map { row ->
                            UserRecord(
                                id = row[IdentityTable.id].toString(),
                                email = row[IdentityTable.email],
                                emailVerified = row[IdentityTable.emailVerified],
                                phone = row[IdentityTable.phone],
                                phoneVerified = row[IdentityTable.phoneVerified],
                                firstName = row[BasicProfileTable.firstName],
                                lastName = row[BasicProfileTable.lastName],
                                displayName = row[BasicProfileTable.displayName],
                                bio = row[BasicProfileTable.bio],
                                avatarUrl = row[BasicProfileTable.avatarUrl],
                                status = row[IdentityTable.status],
                                protectionLevel = row[IdentityTable.protectionLevel],
                                preferredLanguage = row[UserPreferenceTable.language],
                                timezone = row[UserPreferenceTable.timezone],
                                createdAt = row[IdentityTable.createdAt].toString(),
                                updatedAt = row[IdentityTable.updatedAt].toString(),
                                lastLoginAt = row[IdentityTable.lastLoginAt]?.toString()
                            )
                        }
                        .singleOrNull()
                }
                
                // Cache if found
                user?.let {
                    cacheManager.cacheData("user:email:$email", it, USER_CACHE_TTL_SECONDS)
                    cacheManager.cacheData("user:${it.id}", it, USER_CACHE_TTL_SECONDS)
                }
                
                user
                
            } catch (e: Exception) {
                logger.error("❌ Failed to find user by email: $email", e)
                null
            }
        }
    }
    
    /**
     * Update user profile information
     */
    suspend fun updateProfile(userId: String, updates: ProfileUpdateRequest): Result<UserRecord> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📝 Updating profile for user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                transaction {
                    // Update basic profile
                    BasicProfileTable.update({ BasicProfileTable.identityId eq userUuid }) {
                        updates.firstName?.let { firstName -> it[BasicProfileTable.firstName] = firstName }
                        updates.lastName?.let { lastName -> it[BasicProfileTable.lastName] = lastName }
                        updates.displayName?.let { displayName -> it[BasicProfileTable.displayName] = displayName }
                        updates.bio?.let { bio -> it[BasicProfileTable.bio] = bio }
                        updates.avatarUrl?.let { avatarUrl -> it[BasicProfileTable.avatarUrl] = avatarUrl }
                        it[updatedAt] = now
                    }
                    
                    // Update identity table if needed
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[updatedAt] = now
                    }
                }
                
                // Invalidate cache
                cacheManager.invalidateCache("user:$userId")
                
                // Return updated user
                val updatedUser = findById(userId)
                if (updatedUser != null) {
                    logger.info("✅ Profile updated successfully: $userId")
                    Result.success(updatedUser)
                } else {
                    Result.failure(IllegalStateException("Failed to retrieve updated user"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update profile: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updatePreferences(userId: String, preferences: UserPreferenceUpdateRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("⚙️ Updating preferences for user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    UserPreferenceTable.update({ UserPreferenceTable.identityId eq userUuid }) {
                        preferences.language?.let { language -> it[UserPreferenceTable.language] = language }
                        preferences.timezone?.let { timezone -> it[UserPreferenceTable.timezone] = timezone }
                        preferences.theme?.let { theme -> it[UserPreferenceTable.theme] = theme }
                        preferences.notifications?.let { notifications -> it[UserPreferenceTable.notifications] = notifications }
                        preferences.privacy?.let { privacy -> it[UserPreferenceTable.privacy] = privacy }
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("user:$userId")
                    
                    logger.info("✅ Preferences updated successfully: $userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update preferences: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user status
     */
    suspend fun updateStatus(userId: String, status: UserStatus): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔄 Updating status for user: $userId to $status")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[IdentityTable.status] = status
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("user:$userId")
                    
                    logger.info("✅ Status updated successfully: $userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update status: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update last login timestamp
     */
    suspend fun updateLastLogin(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[lastLoginAt] = now
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("user:$userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to update last login: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verify user email
     */
    suspend fun verifyEmail(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("✅ Verifying email for user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[emailVerified] = true
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("user:$userId")
                    
                    logger.info("✅ Email verified successfully: $userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to verify email: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verify user phone
     */
    suspend fun verifyPhone(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("📱 Verifying phone for user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[phoneVerified] = true
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate cache
                    cacheManager.invalidateCache("user:$userId")
                    
                    logger.info("✅ Phone verified successfully: $userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to verify phone: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search users by query with pagination
     */
    suspend fun searchUsers(
        query: String, 
        page: Int = 0, 
        size: Int = 20
    ): Result<PaginatedResult<UserSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🔍 Searching users with query: $query")
                
                val offset = page * size
                
                val (users, total) = transaction {
                    val searchCondition = (BasicProfileTable.firstName like "%$query%") or
                            (BasicProfileTable.lastName like "%$query%") or
                            (BasicProfileTable.displayName like "%$query%") or
                            (IdentityTable.email like "%$query%")
                    
                    val users = (IdentityTable innerJoin BasicProfileTable)
                        .select(searchCondition and (IdentityTable.status eq UserStatus.ACTIVE))
                        .limit(size, offset.toLong())
                        .map { row ->
                            UserSearchResult(
                                id = row[IdentityTable.id].toString(),
                                email = row[IdentityTable.email],
                                displayName = row[BasicProfileTable.displayName],
                                firstName = row[BasicProfileTable.firstName],
                                lastName = row[BasicProfileTable.lastName],
                                avatarUrl = row[BasicProfileTable.avatarUrl]
                            )
                        }
                    
                    val total = (IdentityTable innerJoin BasicProfileTable)
                        .select(searchCondition and (IdentityTable.status eq UserStatus.ACTIVE))
                        .count()
                    
                    Pair(users, total)
                }
                
                logger.info("✅ Found ${users.size} users for query: $query")
                
                Result.success(PaginatedResult(
                    data = users,
                    page = page,
                    size = size,
                    total = total,
                    totalPages = (total + size - 1) / size
                ))
                
            } catch (e: Exception) {
                logger.error("❌ Failed to search users: $query", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete user account (soft delete)
     */
    suspend fun deleteUser(userId: String, reason: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("🗑️ Soft deleting user: $userId")
                
                val now = Instant.now()
                val userUuid = UUID.fromString(userId)
                
                val updated = transaction {
                    IdentityTable.update({ IdentityTable.id eq userUuid }) {
                        it[status] = UserStatus.DELETED
                        it[updatedAt] = now
                    }
                }
                
                if (updated > 0) {
                    // Invalidate all caches
                    cacheManager.invalidateCache("user:$userId")
                    
                    // Log deletion
                    IdentityAuditLogTable.insert {
                        it[identityId] = userUuid
                        it[action] = "account_deleted"
                        it[details] = mapOf("reason" to reason)
                        it[createdAt] = now
                    }
                    
                    logger.info("✅ User deleted successfully: $userId")
                    Result.success(true)
                } else {
                    Result.failure(IllegalArgumentException("User not found"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to delete user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if email exists
     */
    suspend fun emailExists(email: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    IdentityTable.select { IdentityTable.email eq email }
                        .count() > 0
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to check email existence: $email", e)
                false
            }
        }
    }
    
    /**
     * Get user count by status
     */
    suspend fun getUserCountByStatus(status: UserStatus): Long {
        return withContext(Dispatchers.IO) {
            try {
                transaction {
                    IdentityTable.select { IdentityTable.status eq status }
                        .count()
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to get user count by status: $status", e)
                0L
            }
        }
    }
}
