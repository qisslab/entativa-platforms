package com.entativa.id.repository

import com.entativa.id.database.tables.ProfilesTable
import com.entativa.id.database.tables.UnifiedProfilesTable
import com.entativa.id.domain.model.*
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
 * Profile Repository for Entativa ID
 * Handles all database operations for platform-specific and unified profiles
 * 
 * @author Neo Qiss
 * @status Production-ready profile management with cross-platform integration
 */
@Repository
class ProfileRepository {
    
    private val logger = LoggerFactory.getLogger(ProfileRepository::class.java)
    
    // ==================== Platform-Specific Profiles ====================
    
    /**
     * Create a new platform-specific profile
     */
    suspend fun createProfile(profile: CreateProfileRequest): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating profile for user: ${profile.userId} on platform: ${profile.platform}")
                
                val profileId = transaction {
                    ProfilesTable.insertAndGetId {
                        it[userId] = profile.userId
                        it[platform] = profile.platform
                        it[handle] = profile.handle
                        it[displayName] = profile.displayName
                        it[bio] = profile.bio
                        it[ProfilesTable.isActive] = profile.isActive
                        it[isPublic] = profile.isPublic
                        it[isVerified] = profile.isVerified
                        it[isPrimary] = profile.isPrimary
                        it[profileType] = profile.profileType
                        it[visibility] = profile.visibility
                        it[avatarUrl] = profile.avatarUrl
                        it[bannerUrl] = profile.bannerUrl
                        it[website] = profile.website
                        it[location] = profile.location
                        it[timezone] = profile.timezone
                        it[language] = profile.language
                        it[birthDate] = profile.birthDate
                        it[gender] = profile.gender
                        it[occupation] = profile.occupation
                        it[education] = profile.education
                        it[interests] = profile.interests
                        it[customFields] = profile.customFields
                        it[createdBy] = profile.createdBy
                    }
                }
                
                val createdProfile = findById(profileId.value.toString())
                if (createdProfile.isSuccess) {
                    logger.info("✅ Profile created successfully for user: ${profile.userId}")
                    createdProfile
                } else {
                    logger.error("❌ Failed to retrieve created profile for user: ${profile.userId}")
                    Result.failure(Exception("Failed to retrieve created profile"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create profile for user: ${profile.userId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find profile by ID
     */
    suspend fun findById(id: String): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = transaction {
                    ProfilesTable.select { 
                        (ProfilesTable.id eq UUID.fromString(id)) and (ProfilesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToProfile(row)
                    }
                }
                
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(NoSuchElementException("Profile not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find profile by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find profiles by user ID
     */
    suspend fun findByUserId(userId: String): Result<List<Profile>> {
        return withContext(Dispatchers.IO) {
            try {
                val profiles = transaction {
                    ProfilesTable.select { 
                        (ProfilesTable.userId eq userId) and (ProfilesTable.deletedAt.isNull()) 
                    }.map { row ->
                        mapRowToProfile(row)
                    }
                }
                
                Result.success(profiles)
            } catch (e: Exception) {
                logger.error("❌ Failed to find profiles for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find profile by user ID and platform
     */
    suspend fun findByUserIdAndPlatform(userId: String, platform: String): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = transaction {
                    ProfilesTable.select { 
                        (ProfilesTable.userId eq userId) and 
                        (ProfilesTable.platform eq platform) and 
                        (ProfilesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToProfile(row)
                    }
                }
                
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(NoSuchElementException("Profile not found for user: $userId on platform: $platform"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find profile for user: $userId on platform: $platform", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update profile
     */
    suspend fun updateProfile(id: String, updates: UpdateProfileRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔄 Updating profile: $id")
                
                val updated = transaction {
                    ProfilesTable.update({ 
                        (ProfilesTable.id eq UUID.fromString(id)) and (ProfilesTable.deletedAt.isNull()) 
                    }) {
                        updates.displayName?.let { name -> it[displayName] = name }
                        updates.bio?.let { b -> it[bio] = b }
                        updates.avatarUrl?.let { url -> it[avatarUrl] = url }
                        updates.bannerUrl?.let { url -> it[bannerUrl] = url }
                        updates.website?.let { w -> it[website] = w }
                        updates.location?.let { l -> it[location] = l }
                        updates.timezone?.let { tz -> it[timezone] = tz }
                        updates.language?.let { lang -> it[language] = lang }
                        updates.occupation?.let { occ -> it[occupation] = occ }
                        updates.education?.let { edu -> it[education] = edu }
                        updates.interests?.let { int -> it[interests] = int }
                        updates.visibility?.let { vis -> it[visibility] = vis }
                        updates.isPublic?.let { pub -> it[isPublic] = pub }
                        updates.customFields?.let { cf -> it[customFields] = cf }
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = updates.updatedBy
                        it[version] = version + 1
                    }
                } > 0
                
                if (updated) {
                    logger.info("✅ Profile updated: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Profile not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to update profile: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update profile activity metrics
     */
    suspend fun updateActivityMetrics(
        id: String,
        loginCount: Long? = null,
        postCount: Long? = null,
        followerCount: Long? = null,
        followingCount: Long? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    ProfilesTable.update({ 
                        (ProfilesTable.id eq UUID.fromString(id)) and (ProfilesTable.deletedAt.isNull()) 
                    }) {
                        loginCount?.let { count ->
                            it[ProfilesTable.loginCount] = ProfilesTable.loginCount + count
                        }
                        postCount?.let { count ->
                            it[ProfilesTable.postCount] = ProfilesTable.postCount + count
                        }
                        followerCount?.let { count ->
                            it[ProfilesTable.followerCount] = count
                        }
                        followingCount?.let { count ->
                            it[ProfilesTable.followingCount] = count
                        }
                        it[lastActiveAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update activity metrics for profile: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update profile sync status
     */
    suspend fun updateSyncStatus(
        id: String,
        syncStatus: String,
        syncData: String? = null,
        failureReason: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    ProfilesTable.update({ 
                        (ProfilesTable.id eq UUID.fromString(id)) and (ProfilesTable.deletedAt.isNull()) 
                    }) {
                        it[ProfilesTable.syncStatus] = syncStatus
                        if (syncStatus == "SYNCED") {
                            it[lastSyncedAt] = Instant.now()
                            it[syncData] = syncData
                            it[syncRetryCount] = 0
                        } else if (syncStatus == "FAILED") {
                            it[syncFailureReason] = failureReason
                            it[syncRetryCount] = syncRetryCount + 1
                        }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update sync status for profile: $id", e)
                Result.failure(e)
            }
        }
    }
    
    // ==================== Unified Profiles ====================
    
    /**
     * Create a new unified profile
     */
    suspend fun createUnifiedProfile(profile: CreateUnifiedProfileRequest): Result<UnifiedProfile> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("📝 Creating unified profile for user: ${profile.userId}")
                
                val profileId = transaction {
                    UnifiedProfilesTable.insertAndGetId {
                        it[userId] = profile.userId
                        it[globalHandle] = profile.globalHandle
                        it[displayName] = profile.displayName
                        it[bio] = profile.bio
                        it[UnifiedProfilesTable.isActive] = profile.isActive
                        it[isPublic] = profile.isPublic
                        it[isVerified] = profile.isVerified
                        it[profileType] = profile.profileType
                        it[visibility] = profile.visibility
                        it[avatarUrl] = profile.avatarUrl
                        it[bannerUrl] = profile.bannerUrl
                        it[website] = profile.website
                        it[location] = profile.location
                        it[timezone] = profile.timezone
                        it[language] = profile.language
                        it[birthDate] = profile.birthDate
                        it[gender] = profile.gender
                        it[occupation] = profile.occupation
                        it[education] = profile.education
                        it[interests] = profile.interests
                        it[socialLinks] = profile.socialLinks
                        it[customFields] = profile.customFields
                        it[syncEnabledPlatforms] = profile.syncEnabledPlatforms
                        it[createdBy] = profile.createdBy
                    }
                }
                
                val createdProfile = findUnifiedById(profileId.value.toString())
                if (createdProfile.isSuccess) {
                    logger.info("✅ Unified profile created successfully for user: ${profile.userId}")
                    createdProfile
                } else {
                    logger.error("❌ Failed to retrieve created unified profile for user: ${profile.userId}")
                    Result.failure(Exception("Failed to retrieve created unified profile"))
                }
                
            } catch (e: Exception) {
                logger.error("❌ Failed to create unified profile for user: ${profile.userId}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find unified profile by ID
     */
    suspend fun findUnifiedById(id: String): Result<UnifiedProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = transaction {
                    UnifiedProfilesTable.select { 
                        (UnifiedProfilesTable.id eq UUID.fromString(id)) and (UnifiedProfilesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToUnifiedProfile(row)
                    }
                }
                
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(NoSuchElementException("Unified profile not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find unified profile by ID: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find unified profile by user ID
     */
    suspend fun findUnifiedByUserId(userId: String): Result<UnifiedProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = transaction {
                    UnifiedProfilesTable.select { 
                        (UnifiedProfilesTable.userId eq userId) and (UnifiedProfilesTable.deletedAt.isNull()) 
                    }.singleOrNull()?.let { row ->
                        mapRowToUnifiedProfile(row)
                    }
                }
                
                if (profile != null) {
                    Result.success(profile)
                } else {
                    Result.failure(NoSuchElementException("Unified profile not found for user: $userId"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to find unified profile for user: $userId", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update unified profile cross-platform sync
     */
    suspend fun updateUnifiedProfileSync(
        id: String,
        syncedPlatforms: String,
        syncStatus: String,
        lastSyncData: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = transaction {
                    UnifiedProfilesTable.update({ 
                        (UnifiedProfilesTable.id eq UUID.fromString(id)) and (UnifiedProfilesTable.deletedAt.isNull()) 
                    }) {
                        it[UnifiedProfilesTable.syncedPlatforms] = syncedPlatforms
                        it[UnifiedProfilesTable.syncStatus] = syncStatus
                        if (syncStatus == "SYNCED") {
                            it[lastSyncedAt] = Instant.now()
                            it[lastSyncData] = lastSyncData
                            it[totalSyncs] = totalSyncs + 1
                            it[syncRetryCount] = 0
                        } else if (syncStatus == "FAILED") {
                            it[syncRetryCount] = syncRetryCount + 1
                        }
                        it[updatedAt] = Instant.now()
                    }
                } > 0
                
                Result.success(updated)
            } catch (e: Exception) {
                logger.error("❌ Failed to update unified profile sync: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search profiles with filters
     */
    suspend fun searchProfiles(
        query: String? = null,
        platform: String? = null,
        isVerified: Boolean? = null,
        isPublic: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Profile>> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🔍 Searching profiles with query: $query")
                
                val profiles = transaction {
                    var baseQuery = ProfilesTable.select { ProfilesTable.deletedAt.isNull() }
                    
                    query?.let { q ->
                        baseQuery = baseQuery.andWhere { 
                            (ProfilesTable.displayName like "%${q}%") or
                            (ProfilesTable.handle like "%${q}%") or
                            (ProfilesTable.bio like "%${q}%")
                        }
                    }
                    
                    platform?.let { p ->
                        baseQuery = baseQuery.andWhere { ProfilesTable.platform eq p }
                    }
                    
                    isVerified?.let { verified ->
                        baseQuery = baseQuery.andWhere { ProfilesTable.isVerified eq verified }
                    }
                    
                    isPublic?.let { pub ->
                        baseQuery = baseQuery.andWhere { ProfilesTable.isPublic eq pub }
                    }
                    
                    baseQuery
                        .orderBy(ProfilesTable.displayName to SortOrder.ASC)
                        .limit(limit, offset.toLong())
                        .map { row -> mapRowToProfile(row) }
                }
                
                logger.debug("✅ Found ${profiles.size} profiles")
                Result.success(profiles)
            } catch (e: Exception) {
                logger.error("❌ Failed to search profiles", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete profile
     */
    suspend fun deleteProfile(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting profile: $id")
                
                val deleted = transaction {
                    ProfilesTable.update({ 
                        (ProfilesTable.id eq UUID.fromString(id)) and (ProfilesTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[ProfilesTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Profile soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Profile not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete profile: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Soft delete unified profile
     */
    suspend fun deleteUnifiedProfile(id: String, reason: String, deletedBy: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                logger.debug("🗑️ Soft deleting unified profile: $id")
                
                val deleted = transaction {
                    UnifiedProfilesTable.update({ 
                        (UnifiedProfilesTable.id eq UUID.fromString(id)) and (UnifiedProfilesTable.deletedAt.isNull()) 
                    }) {
                        it[deletedAt] = Instant.now()
                        it[UnifiedProfilesTable.deletedBy] = deletedBy
                        it[deletionReason] = reason
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                        it[updatedBy] = deletedBy
                    }
                } > 0
                
                if (deleted) {
                    logger.info("✅ Unified profile soft deleted: $id")
                    Result.success(true)
                } else {
                    Result.failure(NoSuchElementException("Unified profile not found: $id"))
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to delete unified profile: $id", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Map database row to Profile domain object
     */
    private fun mapRowToProfile(row: ResultRow): Profile {
        return Profile(
            id = row[ProfilesTable.id].value.toString(),
            userId = row[ProfilesTable.userId],
            platform = row[ProfilesTable.platform],
            handle = row[ProfilesTable.handle],
            displayName = row[ProfilesTable.displayName],
            bio = row[ProfilesTable.bio],
            isActive = row[ProfilesTable.isActive],
            isPublic = row[ProfilesTable.isPublic],
            isVerified = row[ProfilesTable.isVerified],
            isPrimary = row[ProfilesTable.isPrimary],
            profileType = row[ProfilesTable.profileType],
            visibility = row[ProfilesTable.visibility],
            avatarUrl = row[ProfilesTable.avatarUrl],
            bannerUrl = row[ProfilesTable.bannerUrl],
            website = row[ProfilesTable.website],
            location = row[ProfilesTable.location],
            timezone = row[ProfilesTable.timezone],
            language = row[ProfilesTable.language],
            birthDate = row[ProfilesTable.birthDate],
            gender = row[ProfilesTable.gender],
            occupation = row[ProfilesTable.occupation],
            education = row[ProfilesTable.education],
            interests = row[ProfilesTable.interests],
            customFields = row[ProfilesTable.customFields],
            followerCount = row[ProfilesTable.followerCount],
            followingCount = row[ProfilesTable.followingCount],
            postCount = row[ProfilesTable.postCount],
            loginCount = row[ProfilesTable.loginCount],
            lastActiveAt = row[ProfilesTable.lastActiveAt],
            syncStatus = row[ProfilesTable.syncStatus],
            syncData = row[ProfilesTable.syncData],
            lastSyncedAt = row[ProfilesTable.lastSyncedAt],
            syncFailureReason = row[ProfilesTable.syncFailureReason],
            syncRetryCount = row[ProfilesTable.syncRetryCount],
            reputation = row[ProfilesTable.reputation],
            trustScore = row[ProfilesTable.trustScore],
            verificationLevel = row[ProfilesTable.verificationLevel],
            verificationDocuments = row[ProfilesTable.verificationDocuments],
            verifiedAt = row[ProfilesTable.verifiedAt],
            verifiedBy = row[ProfilesTable.verifiedBy],
            createdAt = row[ProfilesTable.createdAt],
            updatedAt = row[ProfilesTable.updatedAt],
            createdBy = row[ProfilesTable.createdBy],
            updatedBy = row[ProfilesTable.updatedBy],
            version = row[ProfilesTable.version]
        )
    }
    
    /**
     * Map database row to UnifiedProfile domain object
     */
    private fun mapRowToUnifiedProfile(row: ResultRow): UnifiedProfile {
        return UnifiedProfile(
            id = row[UnifiedProfilesTable.id].value.toString(),
            userId = row[UnifiedProfilesTable.userId],
            globalHandle = row[UnifiedProfilesTable.globalHandle],
            displayName = row[UnifiedProfilesTable.displayName],
            bio = row[UnifiedProfilesTable.bio],
            isActive = row[UnifiedProfilesTable.isActive],
            isPublic = row[UnifiedProfilesTable.isPublic],
            isVerified = row[UnifiedProfilesTable.isVerified],
            profileType = row[UnifiedProfilesTable.profileType],
            visibility = row[UnifiedProfilesTable.visibility],
            avatarUrl = row[UnifiedProfilesTable.avatarUrl],
            bannerUrl = row[UnifiedProfilesTable.bannerUrl],
            website = row[UnifiedProfilesTable.website],
            location = row[UnifiedProfilesTable.location],
            timezone = row[UnifiedProfilesTable.timezone],
            language = row[UnifiedProfilesTable.language],
            birthDate = row[UnifiedProfilesTable.birthDate],
            gender = row[UnifiedProfilesTable.gender],
            occupation = row[UnifiedProfilesTable.occupation],
            education = row[UnifiedProfilesTable.education],
            interests = row[UnifiedProfilesTable.interests],
            socialLinks = row[UnifiedProfilesTable.socialLinks],
            customFields = row[UnifiedProfilesTable.customFields],
            connectedPlatforms = row[UnifiedProfilesTable.connectedPlatforms],
            syncEnabledPlatforms = row[UnifiedProfilesTable.syncEnabledPlatforms],
            syncedPlatforms = row[UnifiedProfilesTable.syncedPlatforms],
            syncStatus = row[UnifiedProfilesTable.syncStatus],
            lastSyncedAt = row[UnifiedProfilesTable.lastSyncedAt],
            lastSyncData = row[UnifiedProfilesTable.lastSyncData],
            syncRetryCount = row[UnifiedProfilesTable.syncRetryCount],
            totalSyncs = row[UnifiedProfilesTable.totalSyncs],
            aggregatedFollowerCount = row[UnifiedProfilesTable.aggregatedFollowerCount],
            aggregatedFollowingCount = row[UnifiedProfilesTable.aggregatedFollowingCount],
            aggregatedPostCount = row[UnifiedProfilesTable.aggregatedPostCount],
            globalReputation = row[UnifiedProfilesTable.globalReputation],
            globalTrustScore = row[UnifiedProfilesTable.globalTrustScore],
            verificationLevel = row[UnifiedProfilesTable.verificationLevel],
            verificationDocuments = row[UnifiedProfilesTable.verificationDocuments],
            verifiedAt = row[UnifiedProfilesTable.verifiedAt],
            verifiedBy = row[UnifiedProfilesTable.verifiedBy],
            createdAt = row[UnifiedProfilesTable.createdAt],
            updatedAt = row[UnifiedProfilesTable.updatedAt],
            createdBy = row[UnifiedProfilesTable.createdBy],
            updatedBy = row[UnifiedProfilesTable.updatedBy],
            version = row[UnifiedProfilesTable.version]
        )
    }
}
