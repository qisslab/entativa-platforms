package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Audit Logs Table Definition for Entativa ID
 * Comprehensive audit logging for security, compliance, and monitoring
 * 
 * @author Neo Qiss
 * @status Production-ready audit logging with enterprise compliance features
 */
object AuditLogsTable : UUIDTable("audit_logs") {
    
    // Core Event Information
    val eventId: Column<String> = varchar("event_id", 128).uniqueIndex()
    val correlationId: Column<String?> = varchar("correlation_id", 128).nullable().index()
    val parentEventId: Column<String?> = varchar("parent_event_id", 128).nullable()
    val eventType: Column<String> = varchar("event_type", 50).index() // LOGIN, LOGOUT, DATA_ACCESS, etc.
    val action: Column<String> = varchar("action", 200).index() // Specific action taken
    val category: Column<String> = varchar("category", 30).index() // AUTHENTICATION, AUTHORIZATION, DATA, SYSTEM
    
    // Event Classification
    val level: Column<String> = varchar("level", 10).index() // DEBUG, INFO, WARN, ERROR, CRITICAL
    val severity: Column<String> = varchar("severity", 20).default("NORMAL") // LOW, NORMAL, HIGH, CRITICAL
    val risk_level: Column<String> = varchar("risk_level", 20).default("LOW") // LOW, MEDIUM, HIGH, CRITICAL
    val impact: Column<String> = varchar("impact", 20).default("LOW") // LOW, MEDIUM, HIGH, CRITICAL
    val urgency: Column<String> = varchar("urgency", 20).default("LOW") // LOW, MEDIUM, HIGH, CRITICAL
    
    // Actor Information (Who)
    val userId: Column<String?> = varchar("user_id", 100).nullable().index()
    val actorType: Column<String> = varchar("actor_type", 20).default("USER") // USER, SYSTEM, SERVICE, ADMIN, API
    val actorId: Column<String?> = varchar("actor_id", 100).nullable()
    val actorName: Column<String?> = varchar("actor_name", 200).nullable()
    val impersonatedBy: Column<String?> = varchar("impersonated_by", 100).nullable()
    val onBehalfOf: Column<String?> = varchar("on_behalf_of", 100).nullable()
    val serviceAccount: Column<String?> = varchar("service_account", 100).nullable()
    
    // Resource Information (What)
    val resourceType: Column<String?> = varchar("resource_type", 50).nullable().index()
    val resourceId: Column<String?> = varchar("resource_id", 200).nullable().index()
    val resourceName: Column<String?> = varchar("resource_name", 500).nullable()
    val resourceOwner: Column<String?> = varchar("resource_owner", 100).nullable()
    val resourcePath: Column<String?> = varchar("resource_path", 1000).nullable()
    val resourceVersion: Column<String?> = varchar("resource_version", 50).nullable()
    val dataClassification: Column<String> = varchar("data_classification", 20).default("INTERNAL") // PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
    
    // Timing Information (When)
    val timestamp: Column<Instant> = timestamp("timestamp").default(Instant.now()).index()
    val eventStartTime: Column<Instant?> = timestamp("event_start_time").nullable()
    val eventEndTime: Column<Instant?> = timestamp("event_end_time").nullable()
    val duration: Column<Long?> = long("duration").nullable() // milliseconds
    val timezone: Column<String> = varchar("timezone", 50).default("UTC")
    
    // Location Information (Where)
    val sourceIP: Column<String?> = varchar("source_ip", 45).nullable().index()
    val sourceCountry: Column<String?> = varchar("source_country", 3).nullable()
    val sourceRegion: Column<String?> = varchar("source_region", 100).nullable()
    val sourceCity: Column<String?> = varchar("source_city", 100).nullable()
    val sourceISP: Column<String?> = varchar("source_isp", 200).nullable()
    val isVPN: Column<Boolean> = bool("is_vpn").default(false)
    val isTor: Column<Boolean> = bool("is_tor").default(false)
    val isProxy: Column<Boolean> = bool("is_proxy").default(false)
    val geoLocation: Column<String?> = varchar("geo_location", 100).nullable() // lat,lng
    
    // Technical Context (How)
    val userAgent: Column<String?> = text("user_agent").nullable()
    val deviceType: Column<String?> = varchar("device_type", 50).nullable()
    val operatingSystem: Column<String?> = varchar("operating_system", 100).nullable()
    val browser: Column<String?> = varchar("browser", 100).nullable()
    val applicationName: Column<String?> = varchar("application_name", 100).nullable()
    val applicationVersion: Column<String?> = varchar("application_version", 50).nullable()
    val clientId: Column<String?> = varchar("client_id", 128).nullable()
    val sessionId: Column<String?> = varchar("session_id", 128).nullable().index()
    val requestId: Column<String?> = varchar("request_id", 128).nullable()
    val traceId: Column<String?> = varchar("trace_id", 128).nullable()
    
    // Event Details
    val message: Column<String> = text("message") // Human-readable description
    val details: Column<String> = text("details").default("{}") // JSON object with additional details
    val changes: Column<String> = text("changes").default("{}") // JSON object with before/after values
    val metadata: Column<String> = text("metadata").default("{}") // JSON object with metadata
    val context: Column<String> = text("context").default("{}") // JSON object with contextual information
    val environment: Column<String> = varchar("environment", 20).default("PRODUCTION") // DEV, STAGING, PRODUCTION
    
    // Result Information
    val result: Column<String> = varchar("result", 20).default("SUCCESS") // SUCCESS, FAILURE, PARTIAL, DENIED, ERROR
    val resultCode: Column<String?> = varchar("result_code", 20).nullable()
    val resultMessage: Column<String?> = text("result_message").nullable()
    val errorCode: Column<String?> = varchar("error_code", 50).nullable()
    val errorMessage: Column<String?> = text("error_message").nullable()
    val stackTrace: Column<String?> = text("stack_trace").nullable()
    val httpStatusCode: Column<Int?> = integer("http_status_code").nullable()
    val responseTime: Column<Long?> = long("response_time").nullable() // milliseconds
    
    // Security Information
    val securityEvent: Column<Boolean> = bool("security_event").default(false)
    val threatLevel: Column<String> = varchar("threat_level", 20).default("NONE") // NONE, LOW, MEDIUM, HIGH, CRITICAL
    val securityFlags: Column<String> = text("security_flags").default("[]") // JSON array
    val anomalyScore: Column<Double?> = double("anomaly_score").nullable()
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val fraudIndicators: Column<String> = text("fraud_indicators").default("[]") // JSON array
    val authenticationMethod: Column<String?> = varchar("authentication_method", 50).nullable()
    val authenticationFactors: Column<String> = text("authentication_factors").default("[]") // JSON array
    val authorizedBy: Column<String?> = varchar("authorized_by", 100).nullable()
    
    // Compliance Information
    val complianceEvent: Column<Boolean> = bool("compliance_event").default(false)
    val regulatoryRequirement: Column<String?> = varchar("regulatory_requirement", 100).nullable()
    val complianceFramework: Column<String> = text("compliance_framework").default("[]") // JSON array (GDPR, SOX, HIPAA, etc.)
    val dataSubjects: Column<String> = text("data_subjects").default("[]") // JSON array of affected users
    val personalDataInvolved: Column<Boolean> = bool("personal_data_involved").default(false)
    val sensitiveDataInvolved: Column<Boolean> = bool("sensitive_data_involved").default(false)
    val retentionPeriod: Column<Int> = integer("retention_period").default(2555) // days (7 years default)
    val legalHold: Column<Boolean> = bool("legal_hold").default(false)
    val dataProcessingPurpose: Column<String?> = varchar("data_processing_purpose", 200).nullable()
    val lawfulBasis: Column<String?> = varchar("lawful_basis", 100).nullable()
    
    // Business Context
    val businessProcess: Column<String?> = varchar("business_process", 200).nullable()
    val businessFunction: Column<String?> = varchar("business_function", 100).nullable()
    val costCenter: Column<String?> = varchar("cost_center", 50).nullable()
    val department: Column<String?> = varchar("department", 100).nullable()
    val project: Column<String?> = varchar("project", 200).nullable()
    val businessImpact: Column<String> = varchar("business_impact", 20).default("LOW") // LOW, MEDIUM, HIGH, CRITICAL
    val financialImpact: Column<Double?> = double("financial_impact").nullable()
    val customerImpact: Column<String> = varchar("customer_impact", 20).default("NONE") // NONE, LOW, MEDIUM, HIGH
    
    // Performance Metrics
    val performanceMetrics: Column<String> = text("performance_metrics").default("{}") // JSON object
    val resourceUsage: Column<String> = text("resource_usage").default("{}") // JSON object
    val networkMetrics: Column<String> = text("network_metrics").default("{}") // JSON object
    val databaseMetrics: Column<String> = text("database_metrics").default("{}") // JSON object
    val cacheMetrics: Column<String> = text("cache_metrics").default("{}") // JSON object
    
    // Integration Information
    val integrationName: Column<String?> = varchar("integration_name", 100).nullable()
    val integrationVersion: Column<String?> = varchar("integration_version", 50).nullable()
    val externalSystemId: Column<String?> = varchar("external_system_id", 200).nullable()
    val externalReference: Column<String?> = varchar("external_reference", 500).nullable()
    val webhookId: Column<String?> = varchar("webhook_id", 128).nullable()
    val apiKey: Column<String?> = varchar("api_key", 100).nullable() // Hashed
    val oauthClientId: Column<String?> = varchar("oauth_client_id", 128).nullable()
    
    // Data Changes
    val dataChanged: Column<Boolean> = bool("data_changed").default(false)
    val fieldsChanged: Column<String> = text("fields_changed").default("[]") // JSON array
    val oldValues: Column<String> = text("old_values").default("{}") // JSON object
    val newValues: Column<String> = text("new_values").default("{}") // JSON object
    val changeType: Column<String?> = varchar("change_type", 20).nullable() // CREATE, UPDATE, DELETE, ARCHIVE
    val changeReason: Column<String?> = text("change_reason").nullable()
    val approvedBy: Column<String?> = varchar("approved_by", 100).nullable()
    val rollbackAvailable: Column<Boolean> = bool("rollback_available").default(false)
    
    // Workflow Information
    val workflowId: Column<String?> = varchar("workflow_id", 128).nullable()
    val workflowStep: Column<String?> = varchar("workflow_step", 100).nullable()
    val workflowStatus: Column<String?> = varchar("workflow_status", 20).nullable()
    val approvalRequired: Column<Boolean> = bool("approval_required").default(false)
    val approvalStatus: Column<String?> = varchar("approval_status", 20).nullable()
    val escalated: Column<Boolean> = bool("escalated").default(false)
    val escalationReason: Column<String?> = text("escalation_reason").nullable()
    
    // Alerting Information
    val alertTriggered: Column<Boolean> = bool("alert_triggered").default(false)
    val alertLevel: Column<String?> = varchar("alert_level", 20).nullable()
    val alertRule: Column<String?> = varchar("alert_rule", 200).nullable()
    val notificationSent: Column<Boolean> = bool("notification_sent").default(false)
    val notificationRecipients: Column<String> = text("notification_recipients").default("[]") // JSON array
    val escalationPath: Column<String> = text("escalation_path").default("[]") // JSON array
    val acknowledgment: Column<String?> = varchar("acknowledgment", 100).nullable()
    val acknowledgmentTime: Column<Instant?> = timestamp("acknowledgment_time").nullable()
    val resolution: Column<String?> = text("resolution").nullable()
    val resolutionTime: Column<Instant?> = timestamp("resolution_time").nullable()
    
    // Search and Indexing
    val searchKeywords: Column<String> = text("search_keywords").default("[]") // JSON array
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val indexed: Column<Boolean> = bool("indexed").default(true)
    val searchable: Column<Boolean> = bool("searchable").default(true)
    val fullTextSearchContent: Column<String?> = text("full_text_search_content").nullable()
    
    // Retention and Archival
    val archived: Column<Boolean> = bool("archived").default(false)
    val archivedAt: Column<Instant?> = timestamp("archived_at").nullable()
    val archiveLocation: Column<String?> = varchar("archive_location", 500).nullable()
    val retentionPolicy: Column<String> = varchar("retention_policy", 50).default("STANDARD")
    val expiresAt: Column<Instant?> = timestamp("expires_at").nullable()
    val purgeEligible: Column<Boolean> = bool("purge_eligible").default(false)
    val immutable: Column<Boolean> = bool("immutable").default(true)
    
    // Integrity and Verification
    val checksum: Column<String?> = varchar("checksum", 256).nullable()
    val signature: Column<String?> = text("signature").nullable()
    val verified: Column<Boolean> = bool("verified").default(false)
    val tamperEvident: Column<Boolean> = bool("tamper_evident").default(true)
    val integrityCheck: Column<Boolean> = bool("integrity_check").default(true)
    val hashAlgorithm: Column<String> = varchar("hash_algorithm", 20).default("SHA256")
    
    // System Information
    val hostname: Column<String?> = varchar("hostname", 200).nullable()
    val serverInstance: Column<String?> = varchar("server_instance", 100).nullable()
    val processId: Column<String?> = varchar("process_id", 20).nullable()
    val threadId: Column<String?> = varchar("thread_id", 20).nullable()
    val memoryUsage: Column<Long?> = long("memory_usage").nullable()
    val cpuUsage: Column<Double?> = double("cpu_usage").nullable()
    val diskUsage: Column<Long?> = long("disk_usage").nullable()
    val networkIO: Column<Long?> = long("network_io").nullable()
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now()).index()
    val createdBy: Column<String> = varchar("created_by", 100).default("SYSTEM")
    val batchId: Column<String?> = varchar("batch_id", 128).nullable()
    val sourceSystem: Column<String> = varchar("source_system", 100).default("ENTATIVA_ID")
    val version: Column<Int> = integer("version").default(1)
}
