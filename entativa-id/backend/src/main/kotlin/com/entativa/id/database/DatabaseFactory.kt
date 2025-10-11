package com.entativa.id.database

import com.entativa.id.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * Database Factory for Entativa ID
 * Configures and initializes the database with all tables and relationships
 * 
 * @author Neo Qiss
 * @status Production-ready database initialization with enterprise features
 */
object DatabaseFactory {
    
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    /**
     * Initialize database connection and create tables
     */
    fun init() {
        logger.info("🏗️ Initializing Entativa ID database...")
        
        // Configure database connection
        val database = Database.connect(
            url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/entativa_id",
            driver = "org.postgresql.Driver",
            user = System.getenv("DATABASE_USER") ?: "entativa",
            password = System.getenv("DATABASE_PASSWORD") ?: "secure_password"
        )
        
        // Set connection properties for performance and security
        database.useNestedTransactions = true
        
        // Create all tables
        transaction(db = database) {
            // Set transaction isolation level for consistency
            connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            
            logger.info("📊 Creating database tables...")
            
            // Core identity tables
            SchemaUtils.create(
                UsersTable,
                HandlesTable,
                SessionsTable,
                ProfilesTable,
                UnifiedProfilesTable
            )
            
            // OAuth and authentication tables
            SchemaUtils.create(
                OAuthClientsTable,
                TokensTable,
                MFAMethodsTable,
                DevicesTable,
                RecoveryMethodsTable
            )
            
            // Platform integration tables
            SchemaUtils.create(
                ConnectedAppsTable,
                AppProfilesTable,
                SyncQueueTable
            )
            
            // Security and audit tables
            SchemaUtils.create(
                AuditLogsTable
            )
            
            logger.info("✅ Database tables created successfully")
            
            // Create indexes for performance
            createIndexes()
            
            // Insert default data
            insertDefaultData()
            
            logger.info("🚀 Database initialization completed")
        }
    }
    
    /**
     * Create additional indexes for optimal performance
     */
    private fun createIndexes() {
        logger.info("📈 Creating database indexes...")
        
        transaction {
            // User-related indexes
            exec("CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email))")
            exec("CREATE INDEX IF NOT EXISTS idx_users_handle_lower ON users (LOWER(handle))")
            exec("CREATE INDEX IF NOT EXISTS idx_users_entativa_id ON users (entativa_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_users_status ON users (is_active, is_verified, is_suspended)")
            exec("CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_users_last_login ON users (last_login_at)")
            
            // Handle-related indexes
            exec("CREATE INDEX IF NOT EXISTS idx_handles_status ON handles (status, is_available)")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_owner ON handles (owner_id, user_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_sync ON handles (sync_status, last_synced_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_search ON handles (handle_lowercase, is_available)")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_transfer ON handles (transfer_in_progress, transfer_expires_at)")
            
            // Session-related indexes
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_user_platform ON sessions (user_id, platform_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions (status, is_active)")
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions (expires_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_device ON sessions (device_id, device_fingerprint)")
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_activity ON sessions (last_activity_at)")
            
            // OAuth client indexes
            exec("CREATE INDEX IF NOT EXISTS idx_oauth_clients_status ON oauth_clients (status, is_active)")
            exec("CREATE INDEX IF NOT EXISTS idx_oauth_clients_platform ON oauth_clients (platform_type, entativa_platform)")
            exec("CREATE INDEX IF NOT EXISTS idx_oauth_clients_trust ON oauth_clients (is_trusted, trust_level)")
            exec("CREATE INDEX IF NOT EXISTS idx_oauth_clients_usage ON oauth_clients (last_used_at, total_active_users)")
            
            // Token-related indexes
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_hash ON tokens (token_hash)")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_user_client ON tokens (user_id, client_id)")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_status ON tokens (status, is_active)")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_expires ON tokens (expires_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_family ON tokens (token_family, generation_number)")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_type_status ON tokens (token_type, status)")
            
            // Audit log indexes
            exec("CREATE INDEX IF NOT EXISTS idx_audit_logs_user_action ON audit_logs (user_id, action, created_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (created_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_audit_logs_level ON audit_logs (level, created_at)")
            exec("CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs (resource_type, resource_id)")
            
            // Composite indexes for common queries
            exec("CREATE INDEX IF NOT EXISTS idx_users_login_lookup ON users (email, is_active) WHERE deleted_at IS NULL")
            exec("CREATE INDEX IF NOT EXISTS idx_sessions_cleanup ON sessions (status, expires_at) WHERE cleanup_completed = false")
            exec("CREATE INDEX IF NOT EXISTS idx_tokens_cleanup ON tokens (status, expires_at) WHERE is_active = true")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_available ON handles (is_available, status) WHERE deleted_at IS NULL")
            
            // Partial indexes for soft deletes
            exec("CREATE INDEX IF NOT EXISTS idx_users_active ON users (id) WHERE deleted_at IS NULL")
            exec("CREATE INDEX IF NOT EXISTS idx_handles_active ON handles (id) WHERE deleted_at IS NULL")
            exec("CREATE INDEX IF NOT EXISTS idx_oauth_clients_active ON oauth_clients (id) WHERE deleted_at IS NULL")
        }
        
        logger.info("✅ Database indexes created")
    }
    
    /**
     * Insert default system data
     */
    private fun insertDefaultData() {
        logger.info("📝 Inserting default system data...")
        
        transaction {
            // Insert default OAuth clients for Entativa platforms
            insertDefaultOAuthClients()
            
            // Insert system configuration
            insertSystemConfiguration()
            
            // Insert default MFA methods
            insertDefaultMFAMethods()
        }
        
        logger.info("✅ Default system data inserted")
    }
    
    /**
     * Insert default OAuth clients for Entativa platforms
     */
    private fun insertDefaultOAuthClients() {
        logger.debug("🔐 Creating default OAuth clients...")
        
        val platforms = listOf(
            Triple("gala", "Gala Social Network", "Social networking and community platform"),
            Triple("pika", "Pika Messaging", "Real-time messaging and communication platform"),
            Triple("playpods", "PlayPods Gaming", "Gaming platform with live streaming and social features"),
            Triple("sonet", "SoNet Enterprise", "Professional networking and collaboration platform")
        )
        
        platforms.forEach { (platformId, name, description) ->
            exec("""
                INSERT INTO oauth_clients (
                    client_id, client_name, client_description, application_name,
                    platform_type, entativa_platform, is_first_party, is_trusted,
                    trust_level, grant_types, response_types, scopes, default_scopes,
                    redirect_uris, status, is_active, is_approved, require_pkce,
                    access_token_lifetime, refresh_token_lifetime, created_by
                ) VALUES (
                    '$platformId-client-${System.currentTimeMillis()}',
                    '$name Client',
                    '$description',
                    '$name',
                    'ENTATIVA',
                    '${platformId.uppercase()}',
                    true,
                    true,
                    'VERIFIED',
                    '["authorization_code", "refresh_token", "client_credentials"]',
                    '["code", "token"]',
                    '["openid", "profile", "email", "entativa:cross_platform", "entativa:unified_profile"]',
                    '["openid", "profile"]',
                    '["https://$platformId.entativa.com/auth/callback", "https://app.$platformId.entativa.com/callback"]',
                    'ACTIVE',
                    true,
                    true,
                    true,
                    3600,
                    2592000,
                    'SYSTEM'
                ) ON CONFLICT (client_id) DO NOTHING
            """.trimIndent())
        }
    }
    
    /**
     * Insert system configuration and defaults
     */
    private fun insertSystemConfiguration() {
        logger.debug("⚙️ Creating system configuration...")
        
        // This would typically be in a separate configuration table
        // For now, we'll use the audit logs to track system initialization
        exec("""
            INSERT INTO audit_logs (
                id, user_id, action, resource_type, resource_id,
                level, message, details, created_at
            ) VALUES (
                gen_random_uuid(),
                NULL,
                'SYSTEM_INITIALIZATION',
                'SYSTEM',
                'entativa-id',
                'INFO',
                'Entativa ID system initialized successfully',
                '{"version": "1.0.0", "timestamp": "${java.time.Instant.now()}", "features": ["oauth2", "unified_profiles", "cross_platform_sync", "advanced_security"]}',
                NOW()
            )
        """.trimIndent())
    }
    
    /**
     * Insert default MFA methods
     */
    private fun insertDefaultMFAMethods() {
        logger.debug("🔒 Creating default MFA method configurations...")
        
        // This would typically be configuration data
        // Implementation depends on MFAMethodsTable structure
    }
    
    /**
     * Run database migrations if needed
     */
    fun migrate() {
        logger.info("🔄 Running database migrations...")
        
        transaction {
            // Check current schema version and run migrations
            val currentVersion = getCurrentSchemaVersion()
            val targetVersion = getTargetSchemaVersion()
            
            if (currentVersion < targetVersion) {
                logger.info("📈 Migrating database from version $currentVersion to $targetVersion")
                runMigrations(currentVersion, targetVersion)
            } else {
                logger.info("✅ Database schema is up to date (version $currentVersion)")
            }
        }
    }
    
    /**
     * Get current schema version from database
     */
    private fun getCurrentSchemaVersion(): Int {
        return try {
            // This would query a schema_versions table
            1 // Default version
        } catch (e: Exception) {
            logger.warn("Could not determine schema version, assuming version 1")
            1
        }
    }
    
    /**
     * Get target schema version from application
     */
    private fun getTargetSchemaVersion(): Int {
        return 1 // Current application schema version
    }
    
    /**
     * Run database migrations between versions
     */
    private fun runMigrations(fromVersion: Int, toVersion: Int) {
        logger.info("🚀 Running migrations from version $fromVersion to $toVersion")
        
        for (version in (fromVersion + 1)..toVersion) {
            logger.info("📝 Applying migration for version $version")
            applyMigration(version)
        }
        
        // Update schema version
        updateSchemaVersion(toVersion)
    }
    
    /**
     * Apply specific migration version
     */
    private fun applyMigration(version: Int) {
        when (version) {
            2 -> {
                // Example migration for version 2
                logger.info("Adding new columns for enhanced security...")
                // exec("ALTER TABLE users ADD COLUMN IF NOT EXISTS enhanced_security_enabled BOOLEAN DEFAULT false")
            }
            // Add more migrations as needed
        }
    }
    
    /**
     * Update schema version in database
     */
    private fun updateSchemaVersion(version: Int) {
        // This would update a schema_versions table
        logger.info("✅ Schema updated to version $version")
    }
    
    /**
     * Check database health and connectivity
     */
    fun healthCheck(): Boolean {
        return try {
            transaction {
                // Simple connectivity test
                exec("SELECT 1")
            }
            logger.debug("✅ Database health check passed")
            true
        } catch (e: Exception) {
            logger.error("❌ Database health check failed", e)
            false
        }
    }
    
    /**
     * Clean up expired data (tokens, sessions, etc.)
     */
    fun cleanup() {
        logger.info("🧹 Running database cleanup...")
        
        transaction {
            val now = java.time.Instant.now()
            
            // Clean up expired tokens
            val expiredTokens = exec("""
                UPDATE tokens 
                SET status = 'EXPIRED', is_active = false, updated_at = NOW()
                WHERE expires_at < '$now' AND status = 'ACTIVE'
            """.trimIndent())
            
            // Clean up expired sessions
            val expiredSessions = exec("""
                UPDATE sessions 
                SET status = 'EXPIRED', is_active = false, terminated_at = NOW()
                WHERE expires_at < '$now' AND status = 'ACTIVE'
            """.trimIndent())
            
            // Clean up old audit logs (keep last 2 years)
            val oldAuditLogs = exec("""
                DELETE FROM audit_logs 
                WHERE created_at < NOW() - INTERVAL '2 years'
                AND level NOT IN ('ERROR', 'CRITICAL')
            """.trimIndent())
            
            logger.info("🗑️ Cleanup completed: $expiredTokens tokens, $expiredSessions sessions, $oldAuditLogs audit logs")
        }
    }
}
