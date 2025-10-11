package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Handles Table Definition for Entativa ID
 * Manages unique user handles across all Entativa platforms with reservations and transfers
 * 
 * @author Neo Qiss
 * @status Production-ready handle management with celebrity/brand protection
 */
object HandlesTable : UUIDTable("handles") {
    
    // Core Handle Information
    val handle: Column<String> = varchar("handle", 64).uniqueIndex()
    val handleLowercase: Column<String> = varchar("handle_lowercase", 64).uniqueIndex()
    val userId: Column<String> = varchar("user_id", 100).index()
    val ownerId: Column<String> = varchar("owner_id", 100).index() // Current owner (may differ from original user)
    
    // Handle Status
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, RESERVED, TRANSFERRED, SUSPENDED, DELETED
    val isAvailable: Column<Boolean> = bool("is_available").default(false)
    val isReserved: Column<Boolean> = bool("is_reserved").default(false)
    val isPremium: Column<Boolean> = bool("is_premium").default(false)
    val isProtected: Column<Boolean> = bool("is_protected").default(false)
    val isCelebrity: Column<Boolean> = bool("is_celebrity").default(false)
    val isBrand: Column<Boolean> = bool("is_brand").default(false)
    val isVerified: Column<Boolean> = bool("is_verified").default(false)
    
    // Reservation Details
    val reservationType: Column<String?> = varchar("reservation_type", 20).nullable() // CELEBRITY, BRAND, ORGANIZATION, TRADEMARK
    val reservationReason: Column<String?> = text("reservation_reason").nullable()
    val reservationDocument: Column<String?> = varchar("reservation_document", 500).nullable()
    val reservationApprovedBy: Column<String?> = varchar("reservation_approved_by", 100).nullable()
    val reservationApprovedAt: Column<Instant?> = timestamp("reservation_approved_at").nullable()
    val reservationExpiresAt: Column<Instant?> = timestamp("reservation_expires_at").nullable()
    
    // Transfer Management
    val transferInProgress: Column<Boolean> = bool("transfer_in_progress").default(false)
    val transferFromUserId: Column<String?> = varchar("transfer_from_user_id", 100).nullable()
    val transferToUserId: Column<String?> = varchar("transfer_to_user_id", 100).nullable()
    val transferRequestedAt: Column<Instant?> = timestamp("transfer_requested_at").nullable()
    val transferToken: Column<String?> = varchar("transfer_token", 255).nullable()
    val transferExpiresAt: Column<Instant?> = timestamp("transfer_expires_at").nullable()
    val transferApprovedBy: Column<String?> = varchar("transfer_approved_by", 100).nullable()
    val transferCompletedAt: Column<Instant?> = timestamp("transfer_completed_at").nullable()
    
    // Platform Synchronization
    val syncedToPlatforms: Column<String> = text("synced_to_platforms").default("[]") // JSON array of platform names
    val syncStatus: Column<String> = varchar("sync_status", 20).default("SYNCED") // SYNCED, PENDING, FAILED, PARTIAL
    val lastSyncedAt: Column<Instant?> = timestamp("last_synced_at").nullable()
    val syncFailureReason: Column<String?> = text("sync_failure_reason").nullable()
    val syncRetryCount: Column<Int> = integer("sync_retry_count").default(0)
    
    // Handle History & Analytics
    val originalOwnerId: Column<String> = varchar("original_owner_id", 100).index()
    val totalTransfers: Column<Int> = integer("total_transfers").default(0)
    val lastTransferredAt: Column<Instant?> = timestamp("last_transferred_at").nullable()
    val popularityScore: Column<Double> = double("popularity_score").default(0.0)
    val searchCount: Column<Long> = long("search_count").default(0)
    val viewCount: Column<Long> = long("view_count").default(0)
    val mentionCount: Column<Long> = long("mention_count").default(0)
    
    // Security & Validation
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val requiresMFA: Column<Boolean> = bool("requires_mfa").default(false)
    val lastSecurityCheck: Column<Instant?> = timestamp("last_security_check").nullable()
    val fraudFlags: Column<String?> = text("fraud_flags").nullable() // JSON array
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    
    // Pricing & Monetization
    val price: Column<Double?> = double("price").nullable()
    val currency: Column<String> = varchar("currency", 3).default("USD")
    val isForSale: Column<Boolean> = bool("is_for_sale").default(false)
    val marketplaceListedAt: Column<Instant?> = timestamp("marketplace_listed_at").nullable()
    val lastSalePrice: Column<Double?> = double("last_sale_price").nullable()
    val lastSaleAt: Column<Instant?> = timestamp("last_sale_at").nullable()
    
    // Content & Branding
    val description: Column<String?> = text("description").nullable()
    val category: Column<String?> = varchar("category", 50).nullable() // PERSONAL, BUSINESS, ENTERTAINMENT, SPORTS, etc.
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val associatedDomains: Column<String> = text("associated_domains").default("[]") // JSON array
    val socialLinks: Column<String> = text("social_links").default("{}") // JSON object
    
    // Appeal & Dispute Management
    val hasActiveAppeal: Column<Boolean> = bool("has_active_appeal").default(false)
    val appealReason: Column<String?> = text("appeal_reason").nullable()
    val appealSubmittedAt: Column<Instant?> = timestamp("appeal_submitted_at").nullable()
    val appealResolvedAt: Column<Instant?> = timestamp("appeal_resolved_at").nullable()
    val appealResolution: Column<String?> = text("appeal_resolution").nullable()
    val appealResolvedBy: Column<String?> = varchar("appeal_resolved_by", 100).nullable()
    
    // Compliance & Legal
    val trademarked: Column<Boolean> = bool("trademarked").default(false)
    val trademarkNumber: Column<String?> = varchar("trademark_number", 100).nullable()
    val trademarkCountry: Column<String?> = varchar("trademark_country", 3).nullable()
    val copyrighted: Column<Boolean> = bool("copyrighted").default(false)
    val copyrightNumber: Column<String?> = varchar("copyright_number", 100).nullable()
    val legalDocuments: Column<String> = text("legal_documents").default("[]") // JSON array
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // Soft Delete
    val deletedAt: Column<Instant?> = timestamp("deleted_at").nullable()
    val deletedBy: Column<String?> = varchar("deleted_by", 100).nullable()
    val deletionReason: Column<String?> = varchar("deletion_reason", 500).nullable()
    
    // Handle Validation Rules
    val validationRules: Column<String> = text("validation_rules").default("{}") // JSON object
    val customValidation: Column<String?> = text("custom_validation").nullable() // JSON rules
    val violatesPolicy: Column<Boolean> = bool("violates_policy").default(false)
    val policyViolationReason: Column<String?> = text("policy_violation_reason").nullable()
    
    // Performance & Caching
    val cacheVersion: Column<Long> = long("cache_version").default(1)
    val lastCacheUpdate: Column<Instant?> = timestamp("last_cache_update").nullable()
    val searchOptimized: Column<Boolean> = bool("search_optimized").default(true)
    val indexVersion: Column<Int> = integer("index_version").default(1)
}
