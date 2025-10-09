package com.entativa.id.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Database Configuration for Entativa ID
 * Configures PostgreSQL connection with optimized settings
 * 
 * @author Neo Qiss
 * @status Production-ready with connection pooling
 */

private val logger = LoggerFactory.getLogger("DatabaseConfig")

fun Application.configureDatabases() {
    val database = Database.connect(createHikariDataSource())
    
    // Create tables if they don't exist
    transaction(database) {
        SchemaUtils.createMissingTablesAndColumns(
            EntativaIdentitiesTable,
            EntativaProfilesTable,
            WellKnownFiguresTable,
            WellKnownCompaniesTable,
            ReservedHandlesTable,
            ProtectedHandlesTable,
            VerificationRequestsTable,
            VerificationDocumentsTable,
            OAuthApplicationsTable,
            OAuthTokensTable,
            AuthorizationCodesTable,
            TwoFactorAuthTable,
            IdentityAuditLogTable,
            HandleChangeHistoryTable,
            PlatformAccountsTable
        )
    }
    
    logger.info("✅ Database configured and tables created/verified")
}

private fun createHikariDataSource(): DataSource {
    val config = HikariConfig().apply {
        jdbcUrl = System.getenv("DATABASE_URL") 
            ?: "jdbc:postgresql://localhost:5432/entativa_id_db"
        username = System.getenv("DATABASE_USER") ?: "entativa_user"
        password = System.getenv("DATABASE_PASSWORD") ?: "entativa_password"
        driverClassName = "org.postgresql.Driver"
        
        // Connection pool settings optimized for EiD workloads
        maximumPoolSize = System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: 20
        minimumIdle = System.getenv("DB_MIN_IDLE")?.toIntOrNull() ?: 5
        connectionTimeout = 30000 // 30 seconds
        idleTimeout = 600000 // 10 minutes
        maxLifetime = 1800000 // 30 minutes
        leakDetectionThreshold = 60000 // 1 minute
        
        // PostgreSQL optimizations
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        addDataSourceProperty("useServerPrepStmts", "true")
        addDataSourceProperty("useLocalSessionState", "true")
        addDataSourceProperty("rewriteBatchedStatements", "true")
        addDataSourceProperty("cacheResultSetMetadata", "true")
        addDataSourceProperty("cacheServerConfiguration", "true")
        addDataSourceProperty("elideSetAutoCommits", "true")
        addDataSourceProperty("maintainTimeStats", "false")
        
        // Health check
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000
        
        // Pool name for monitoring
        poolName = "EntativaIdPool"
    }
    
    return HikariDataSource(config)
}

// Database table definitions using Exposed ORM
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.json

object EntativaIdentitiesTable : UUIDTable("entativa_identities") {
    val eid = varchar("eid", 100).uniqueIndex()
    val email = varchar("email", 320).uniqueIndex()
    val phone = varchar("phone", 20).nullable().uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val lastLoginAt = timestamp("last_login_at").nullable()
    val status = varchar("status", 20).default("active")
    val emailVerified = bool("email_verified").default(false)
    val phoneVerified = bool("phone_verified").default(false)
    val twoFactorEnabled = bool("two_factor_enabled").default(false)
    val profileCompleted = bool("profile_completed").default(false)
    val verificationStatus = varchar("verification_status", 20).default("none")
    val verificationBadge = varchar("verification_badge", 20).nullable()
    val verificationDate = timestamp("verification_date").nullable()
    val reputationScore = integer("reputation_score").default(1000)
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    val passwordChangedAt = timestamp("password_changed_at")
    val createdBy = varchar("created_by", 50).default("self_registration")
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val countryCode = char("country_code", 2).nullable()
}

object EntativaProfilesTable : UUIDTable("entativa_profiles") {
    val identityId = reference("identity_id", EntativaIdentitiesTable).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val displayName = varchar("display_name", 150).nullable()
    val bio = text("bio").nullable()
    val website = varchar("website", 500).nullable()
    val location = varchar("location", 200).nullable()
    val birthDate = date("birth_date").nullable()
    val gender = varchar("gender", 20).nullable()
    val pronouns = varchar("pronouns", 50).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val bannerUrl = varchar("banner_url", 500).nullable()
    val profession = varchar("profession", 200).nullable()
    val company = varchar("company", 200).nullable()
    val industry = varchar("industry", 100).nullable()
    val profileVisibility = varchar("profile_visibility", 20).default("public")
    val birthDateVisibility = varchar("birth_date_visibility", 20).default("private")
    val locationVisibility = varchar("location_visibility", 20).default("public")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object WellKnownFiguresTable : UUIDTable("well_known_figures") {
    val name = varchar("name", 200)
    val category = varchar("category", 50)
    val preferredHandle = varchar("preferred_handle", 100).uniqueIndex()
    val alternativeHandles = json<List<String>>("alternative_handles", String::class).nullable()
    val verificationLevel = varchar("verification_level", 20).default("high")
    val wikipediaUrl = varchar("wikipedia_url", 500).nullable()
    val verifiedSocialAccounts = json<Map<String, String>>("verified_social_accounts", String::class).nullable()
    val isActive = bool("is_active").default(true)
    val claimedBy = reference("claimed_by", EntativaIdentitiesTable).nullable()
    val claimedAt = timestamp("claimed_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object WellKnownCompaniesTable : UUIDTable("well_known_companies") {
    val name = varchar("name", 200)
    val legalName = varchar("legal_name", 300).nullable()
    val industry = varchar("industry", 100)
    val companyType = varchar("company_type", 50).nullable()
    val preferredHandle = varchar("preferred_handle", 100).uniqueIndex()
    val alternativeHandles = json<List<String>>("alternative_handles", String::class).nullable()
    val stockSymbol = varchar("stock_symbol", 10).nullable()
    val foundedYear = integer("founded_year").nullable()
    val headquartersCountry = char("headquarters_country", 2).nullable()
    val website = varchar("website", 500).nullable()
    val linkedinUrl = varchar("linkedin_url", 500).nullable()
    val verificationLevel = varchar("verification_level", 20).default("high")
    val requiredDocuments = json<List<String>>("required_documents", String::class).nullable()
    val isActive = bool("is_active").default(true)
    val claimedBy = reference("claimed_by", EntativaIdentitiesTable).nullable()
    val claimedAt = timestamp("claimed_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object ReservedHandlesTable : UUIDTable("reserved_handles") {
    val handle = varchar("handle", 100).uniqueIndex()
    val reservationType = varchar("reservation_type", 30)
    val platform = varchar("platform", 20).nullable()
    val reason = text("reason")
    val reservedUntil = timestamp("reserved_until").nullable()
    val canBeReleased = bool("can_be_released").default(false)
    val createdAt = timestamp("created_at")
    val createdBy = varchar("created_by", 100).default("system")
}

object ProtectedHandlesTable : UUIDTable("protected_handles") {
    val originalHandle = varchar("original_handle", 100)
    val protectedEntityType = varchar("protected_entity_type", 20)
    val protectedEntityId = uuid("protected_entity_id").nullable()
    val similarityThreshold = decimal("similarity_threshold", 3, 2).default(0.85.toBigDecimal())
    val createdAt = timestamp("created_at")
}

object VerificationRequestsTable : UUIDTable("verification_requests") {
    val identityId = reference("identity_id", EntativaIdentitiesTable)
    val verificationType = varchar("verification_type", 20)
    val requestedHandle = varchar("requested_handle", 100).nullable()
    val claimType = varchar("claim_type", 30).nullable()
    val claimedEntityId = uuid("claimed_entity_id").nullable()
    val status = varchar("status", 20).default("pending")
    val priority = integer("priority").default(3)
    val assignedReviewer = uuid("assigned_reviewer").nullable()
    val reviewStartedAt = timestamp("review_started_at").nullable()
    val reviewCompletedAt = timestamp("review_completed_at").nullable()
    val adminNotes = text("admin_notes").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val applicantNotes = text("applicant_notes").nullable()
    val contactEmail = varchar("contact_email", 320).nullable()
    val contactPhone = varchar("contact_phone", 20).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object VerificationDocumentsTable : UUIDTable("verification_documents") {
    val verificationRequestId = reference("verification_request_id", VerificationRequestsTable)
    val documentType = varchar("document_type", 50)
    val fileName = varchar("file_name", 255)
    val fileUrl = varchar("file_url", 500)
    val fileSize = integer("file_size").nullable()
    val mimeType = varchar("mime_type", 100).nullable()
    val uploadDate = timestamp("upload_date")
    val isVerified = bool("is_verified").default(false)
    val verifiedBy = uuid("verified_by").nullable()
    val verifiedAt = timestamp("verified_at").nullable()
    val verificationNotes = text("verification_notes").nullable()
    val fileHash = varchar("file_hash", 64).nullable()
    val encrypted = bool("encrypted").default(true)
    val createdAt = timestamp("created_at")
}

object OAuthApplicationsTable : UUIDTable("oauth_applications") {
    val name = varchar("name", 200)
    val clientId = varchar("client_id", 64).uniqueIndex()
    val clientSecretHash = varchar("client_secret_hash", 255)
    val redirectUris = json<List<String>>("redirect_uris", String::class)
    val allowedScopes = json<List<String>>("allowed_scopes", String::class)
    val applicationType = varchar("application_type", 20).default("web")
    val ownerIdentityId = reference("owner_identity_id", EntativaIdentitiesTable)
    val isActive = bool("is_active").default(true)
    val isTrusted = bool("is_trusted").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object OAuthTokensTable : UUIDTable("oauth_tokens") {
    val accessTokenHash = varchar("access_token_hash", 255).uniqueIndex()
    val refreshTokenHash = varchar("refresh_token_hash", 255).nullable().uniqueIndex()
    val identityId = reference("identity_id", EntativaIdentitiesTable)
    val applicationId = reference("application_id", OAuthApplicationsTable)
    val scopes = json<List<String>>("scopes", String::class)
    val accessTokenExpiresAt = timestamp("access_token_expires_at")
    val refreshTokenExpiresAt = timestamp("refresh_token_expires_at").nullable()
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at").nullable()
    val usageCount = integer("usage_count").default(0)
    val revoked = bool("revoked").default(false)
    val revokedAt = timestamp("revoked_at").nullable()
    val revokedBy = reference("revoked_by", EntativaIdentitiesTable).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
}

object AuthorizationCodesTable : UUIDTable("authorization_codes") {
    val code = varchar("code", 128).uniqueIndex()
    val identityId = reference("identity_id", EntativaIdentitiesTable)
    val applicationId = reference("application_id", OAuthApplicationsTable)
    val redirectUri = varchar("redirect_uri", 500)
    val scopes = json<List<String>>("scopes", String::class)
    val expiresAt = timestamp("expires_at")
    val used = bool("used").default(false)
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at")
}

object TwoFactorAuthTable : UUIDTable("two_factor_auth") {
    val identityId = reference("identity_id", EntativaIdentitiesTable).uniqueIndex()
    val method = varchar("method", 20)
    val secretKey = varchar("secret_key", 255).nullable()
    val backupCodes = json<List<String>>("backup_codes", String::class).nullable()
    val isEnabled = bool("is_enabled").default(false)
    val verifiedAt = timestamp("verified_at").nullable()
    val lastUsedAt = timestamp("last_used_at").nullable()
    val failedAttempts = integer("failed_attempts").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object IdentityAuditLogTable : UUIDTable("identity_audit_log") {
    val identityId = reference("identity_id", EntativaIdentitiesTable).nullable()
    val action = varchar("action", 50)
    val details = json<Map<String, String>>("details", String::class).nullable()
    val performedBy = reference("performed_by", EntativaIdentitiesTable).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val gdprLawfulBasis = varchar("gdpr_lawful_basis", 50).nullable()
    val dataRetentionPolicy = varchar("data_retention_policy", 50).nullable()
    val createdAt = timestamp("created_at")
}

object HandleChangeHistoryTable : UUIDTable("handle_change_history") {
    val identityId = reference("identity_id", EntativaIdentitiesTable)
    val oldEid = varchar("old_eid", 100)
    val newEid = varchar("new_eid", 100)
    val reason = varchar("reason", 200).nullable()
    val requiresApproval = bool("requires_approval").default(false)
    val approvedBy = reference("approved_by", EntativaIdentitiesTable).nullable()
    val approvedAt = timestamp("approved_at").nullable()
    val createdAt = timestamp("created_at")
}

object PlatformAccountsTable : UUIDTable("platform_accounts") {
    val identityId = reference("identity_id", EntativaIdentitiesTable)
    val platform = varchar("platform", 20)
    val platformUserId = uuid("platform_user_id")
    val joinedAt = timestamp("joined_at")
    val status = varchar("status", 20).default("active")
    val lastActiveAt = timestamp("last_active_at").nullable()
    val platformMetadata = json<Map<String, String>>("platform_metadata", String::class).nullable()
    
    init {
        uniqueIndex(identityId, platform)
        uniqueIndex(platform, platformUserId)
    }
}
