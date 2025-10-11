package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Sync Queue Table Definition for Entativa ID
 * Manages cross-platform data synchronization queue for unified profiles and handles
 * 
 * @author Neo Qiss
 * @status Production-ready sync queue with enterprise reliability and conflict resolution
 */
object SyncQueueTable : UUIDTable("sync_queue") {
    
    // Core Sync Job Information
    val jobId: Column<String> = varchar("job_id", 128).uniqueIndex()
    val correlationId: Column<String?> = varchar("correlation_id", 128).nullable().index()
    val batchId: Column<String?> = varchar("batch_id", 128).nullable().index()
    val parentJobId: Column<String?> = varchar("parent_job_id", 128).nullable()
    val jobType: Column<String> = varchar("job_type", 30) // PROFILE_SYNC, HANDLE_SYNC, SETTINGS_SYNC, CONTENT_SYNC, BULK_SYNC
    
    // Entity Information
    val entityType: Column<String> = varchar("entity_type", 30) // USER, PROFILE, HANDLE, SETTINGS, CONTENT, SESSION
    val entityId: Column<String> = varchar("entity_id", 100).index()
    val userId: Column<String?> = varchar("user_id", 100).nullable().index()
    val resourceId: Column<String?> = varchar("resource_id", 200).nullable()
    val resourceType: Column<String?> = varchar("resource_type", 50).nullable()
    val resourceVersion: Column<Long?> = long("resource_version").nullable()
    
    // Source and Target Platforms
    val sourcePlatform: Column<String> = varchar("source_platform", 50) // GALA, PIKA, PLAYPODS, SONET, ENTATIVA_ID
    val targetPlatforms: Column<String> = text("target_platforms") // JSON array of target platforms
    val syncDirection: Column<String> = varchar("sync_direction", 20).default("OUTBOUND") // INBOUND, OUTBOUND, BIDIRECTIONAL
    val platformPriority: Column<String> = text("platform_priority").default("[]") // JSON array for sync order
    val excludedPlatforms: Column<String> = text("excluded_platforms").default("[]") // JSON array
    
    // Job Status and Lifecycle
    val status: Column<String> = varchar("status", 20).default("PENDING") // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, RETRY
    val priority: Column<String> = varchar("priority", 10).default("NORMAL") // LOW, NORMAL, HIGH, URGENT, CRITICAL
    val attempts: Column<Int> = integer("attempts").default(0)
    val maxAttempts: Column<Int> = integer("max_attempts").default(3)
    val retryCount: Column<Int> = integer("retry_count").default(0)
    val maxRetries: Column<Int> = integer("max_retries").default(5)
    val lastRetryAt: Column<Instant?> = timestamp("last_retry_at").nullable()
    val nextRetryAt: Column<Instant?> = timestamp("next_retry_at").nullable()
    
    // Timing and Scheduling
    val scheduledAt: Column<Instant> = timestamp("scheduled_at").default(Instant.now())
    val startedAt: Column<Instant?> = timestamp("started_at").nullable()
    val completedAt: Column<Instant?> = timestamp("completed_at").nullable()
    val failedAt: Column<Instant?> = timestamp("failed_at").nullable()
    val cancelledAt: Column<Instant?> = timestamp("cancelled_at").nullable()
    val timeoutAt: Column<Instant?> = timestamp("timeout_at").nullable()
    val processingTimeMs: Column<Long?> = long("processing_time_ms").nullable()
    val queueTimeMs: Column<Long?> = long("queue_time_ms").nullable()
    val totalTimeMs: Column<Long?> = long("total_time_ms").nullable()
    
    // Data Payload
    val dataPayload: Column<String> = text("data_payload") // JSON object containing the data to sync
    val originalData: Column<String?> = text("original_data").nullable() // JSON backup of original data
    val transformedData: Column<String?> = text("transformed_data").nullable() // JSON after transformations
    val deltaData: Column<String?> = text("delta_data").nullable() // JSON containing only changes
    val payloadSize: Column<Long> = long("payload_size").default(0) // Size in bytes
    val payloadChecksum: Column<String?> = varchar("payload_checksum", 256).nullable()
    val compressionEnabled: Column<Boolean> = bool("compression_enabled").default(false)
    val encryptionEnabled: Column<Boolean> = bool("encryption_enabled").default(false)
    
    // Sync Configuration
    val syncMode: Column<String> = varchar("sync_mode", 20).default("INCREMENTAL") // FULL, INCREMENTAL, DELTA, SNAPSHOT
    val syncStrategy: Column<String> = varchar("sync_strategy", 20).default("MERGE") // MERGE, OVERWRITE, APPEND, UPSERT
    val conflictResolution: Column<String> = varchar("conflict_resolution", 20).default("LATEST_WINS") // LATEST_WINS, SOURCE_WINS, MANUAL, CUSTOM
    val validationEnabled: Column<Boolean> = bool("validation_enabled").default(true)
    val transformationEnabled: Column<Boolean> = bool("transformation_enabled").default(true)
    val notificationEnabled: Column<Boolean> = bool("notification_enabled").default(false)
    val rollbackEnabled: Column<Boolean> = bool("rollback_enabled").default(true)
    
    // Progress Tracking
    val totalSteps: Column<Int> = integer("total_steps").default(1)
    val completedSteps: Column<Int> = integer("completed_steps").default(0)
    val currentStep: Column<String?> = varchar("current_step", 200).nullable()
    val progressPercentage: Column<Double> = double("progress_percentage").default(0.0)
    val estimatedTimeRemaining: Column<Long?> = long("estimated_time_remaining").nullable() // seconds
    val stepsDetail: Column<String> = text("steps_detail").default("[]") // JSON array of step details
    val checkpoints: Column<String> = text("checkpoints").default("[]") // JSON array for recovery
    
    // Result and Status Information
    val result: Column<String?> = varchar("result", 20).nullable() // SUCCESS, PARTIAL_SUCCESS, FAILURE, CANCELLED
    val resultMessage: Column<String?> = text("result_message").nullable()
    val resultData: Column<String> = text("result_data").default("{}") // JSON object with sync results
    val affectedRecords: Column<Int> = integer("affected_records").default(0)
    val successfulSyncs: Column<Int> = integer("successful_syncs").default(0)
    val failedSyncs: Column<Int> = integer("failed_syncs").default(0)
    val skippedSyncs: Column<Int> = integer("skipped_syncs").default(0)
    val conflictsDetected: Column<Int> = integer("conflicts_detected").default(0)
    val conflictsResolved: Column<Int> = integer("conflicts_resolved").default(0)
    
    // Error Handling
    val errorCode: Column<String?> = varchar("error_code", 50).nullable()
    val errorMessage: Column<String?> = text("error_message").nullable()
    val errorDetails: Column<String> = text("error_details").default("{}") // JSON object
    val stackTrace: Column<String?> = text("stack_trace").nullable()
    val errorCategory: Column<String?> = varchar("error_category", 30).nullable() // NETWORK, AUTH, DATA, TIMEOUT, CONFLICT
    val isRetryable: Column<Boolean> = bool("is_retryable").default(true)
    val errorCount: Column<Int> = integer("error_count").default(0)
    val lastErrorAt: Column<Instant?> = timestamp("last_error_at").nullable()
    val errorThreshold: Column<Int> = integer("error_threshold").default(3)
    
    // Conflict Management
    val hasConflicts: Column<Boolean> = bool("has_conflicts").default(false)
    val conflictData: Column<String> = text("conflict_data").default("[]") // JSON array of conflicts
    val conflictResolutionData: Column<String> = text("conflict_resolution_data").default("{}") // JSON object
    val manualResolutionRequired: Column<Boolean> = bool("manual_resolution_required").default(false)
    val conflictResolvedBy: Column<String?> = varchar("conflict_resolved_by", 100).nullable()
    val conflictResolvedAt: Column<Instant?> = timestamp("conflict_resolved_at").nullable()
    val conflictStrategy: Column<String?> = varchar("conflict_strategy", 30).nullable()
    
    // Dependencies and Relationships
    val dependsOn: Column<String> = text("depends_on").default("[]") // JSON array of job IDs this job depends on
    val blockedBy: Column<String> = text("blocked_by").default("[]") // JSON array of job IDs blocking this job
    val blocks: Column<String> = text("blocks").default("[]") // JSON array of job IDs this job blocks
    val childJobs: Column<String> = text("child_jobs").default("[]") // JSON array of child job IDs
    val relatedJobs: Column<String> = text("related_jobs").default("[]") // JSON array of related job IDs
    val dependencyResolved: Column<Boolean> = bool("dependency_resolved").default(true)
    val waitingForDependencies: Column<Boolean> = bool("waiting_for_dependencies").default(false)
    
    // Worker and Processing Information
    val workerId: Column<String?> = varchar("worker_id", 100).nullable()
    val workerType: Column<String?> = varchar("worker_type", 30).nullable() // SYNC_WORKER, BATCH_PROCESSOR, CONFLICT_RESOLVER
    val processingNode: Column<String?> = varchar("processing_node", 100).nullable()
    val assignedAt: Column<Instant?> = timestamp("assigned_at").nullable()
    val heartbeatAt: Column<Instant?> = timestamp("heartbeat_at").nullable()
    val lockOwner: Column<String?> = varchar("lock_owner", 100).nullable()
    val lockedAt: Column<Instant?> = timestamp("locked_at").nullable()
    val lockExpiresAt: Column<Instant?> = timestamp("lock_expires_at").nullable()
    val processingTimeout: Column<Int> = integer("processing_timeout").default(300) // seconds
    
    // Performance and Monitoring
    val performanceMetrics: Column<String> = text("performance_metrics").default("{}") // JSON object
    val resourceUsage: Column<String> = text("resource_usage").default("{}") // JSON object
    val networkMetrics: Column<String> = text("network_metrics").default("{}") // JSON object
    val throughputMbps: Column<Double?> = double("throughput_mbps").nullable()
    val latencyMs: Column<Long?> = long("latency_ms").nullable()
    val memoryUsageMb: Column<Long?> = long("memory_usage_mb").nullable()
    val cpuUsagePercent: Column<Double?> = double("cpu_usage_percent").nullable()
    val networkBytesTransferred: Column<Long?> = long("network_bytes_transferred").nullable()
    
    // Compliance and Audit
    val auditTrail: Column<String> = text("audit_trail").default("[]") // JSON array of audit events
    val complianceFlags: Column<String> = text("compliance_flags").default("[]") // JSON array
    val dataClassification: Column<String> = varchar("data_classification", 20).default("INTERNAL")
    val regulatoryRequirements: Column<String> = text("regulatory_requirements").default("[]") // JSON array
    val retentionPeriodDays: Column<Int> = integer("retention_period_days").default(90)
    val auditRequired: Column<Boolean> = bool("audit_required").default(false)
    val complianceCheck: Column<Boolean> = bool("compliance_check").default(false)
    val personalDataInvolved: Column<Boolean> = bool("personal_data_involved").default(false)
    
    // Notification and Alerting
    val notifyOnSuccess: Column<Boolean> = bool("notify_on_success").default(false)
    val notifyOnFailure: Column<Boolean> = bool("notify_on_failure").default(true)
    val notifyOnConflict: Column<Boolean> = bool("notify_on_conflict").default(true)
    val notificationRecipients: Column<String> = text("notification_recipients").default("[]") // JSON array
    val alertsTriggered: Column<String> = text("alerts_triggered").default("[]") // JSON array
    val escalationLevel: Column<Int> = integer("escalation_level").default(0)
    val escalationRules: Column<String> = text("escalation_rules").default("[]") // JSON array
    val lastNotificationAt: Column<Instant?> = timestamp("last_notification_at").nullable()
    
    // Rollback and Recovery
    val rollbackData: Column<String?> = text("rollback_data").nullable() // JSON backup for rollback
    val rollbackJobId: Column<String?> = varchar("rollback_job_id", 128).nullable()
    val rollbackAvailable: Column<Boolean> = bool("rollback_available").default(false)
    val rollbackExecuted: Column<Boolean> = bool("rollback_executed").default(false)
    val rollbackAt: Column<Instant?> = timestamp("rollback_at").nullable()
    val rollbackReason: Column<String?> = text("rollback_reason").nullable()
    val recoveryPoint: Column<String?> = text("recovery_point").nullable() // JSON recovery checkpoint
    val autoRollbackEnabled: Column<Boolean> = bool("auto_rollback_enabled").default(true)
    
    // Testing and Debugging
    val isTestJob: Column<Boolean> = bool("is_test_job").default(false)
    val debugMode: Column<Boolean> = bool("debug_mode").default(false)
    val dryRun: Column<Boolean> = bool("dry_run").default(false)
    val simulationMode: Column<Boolean> = bool("simulation_mode").default(false)
    val debugData: Column<String> = text("debug_data").default("{}") // JSON object
    val traceId: Column<String?> = varchar("trace_id", 128).nullable()
    val debugLevel: Column<String> = varchar("debug_level", 10).default("INFO") // DEBUG, INFO, WARN, ERROR
    
    // Business Logic
    val businessRules: Column<String> = text("business_rules").default("[]") // JSON array of business rules
    val validationRules: Column<String> = text("validation_rules").default("[]") // JSON array
    val transformationRules: Column<String> = text("transformation_rules").default("[]") // JSON array
    val customLogic: Column<String?> = text("custom_logic").nullable() // Custom business logic
    val skipValidation: Column<Boolean> = bool("skip_validation").default(false)
    val skipTransformation: Column<Boolean> = bool("skip_transformation").default(false)
    val forceSync: Column<Boolean> = bool("force_sync").default(false)
    
    // Batch Processing
    val isBatchJob: Column<Boolean> = bool("is_batch_job").default(false)
    val batchSize: Column<Int> = integer("batch_size").default(1)
    val batchIndex: Column<Int> = integer("batch_index").default(0)
    val totalBatches: Column<Int> = integer("total_batches").default(1)
    val batchStartedAt: Column<Instant?> = timestamp("batch_started_at").nullable()
    val batchCompletedAt: Column<Instant?> = timestamp("batch_completed_at").nullable()
    val parallelProcessing: Column<Boolean> = bool("parallel_processing").default(false)
    val maxParallelJobs: Column<Int> = integer("max_parallel_jobs").default(1)
    
    // Custom Attributes and Extensions
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON object
    val metadata: Column<String> = text("metadata").default("{}") // JSON object
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val labels: Column<String> = text("labels").default("{}") // JSON object
    val annotations: Column<String> = text("annotations").default("{}") // JSON object
    val extensions: Column<String> = text("extensions").default("{}") // JSON object
    val contextData: Column<String> = text("context_data").default("{}") // JSON object
    
    // Audit Trail
    val createdAt: Column<Instant> = timestamp("created_at").default(Instant.now())
    val updatedAt: Column<Instant> = timestamp("updated_at").default(Instant.now())
    val createdBy: Column<String?> = varchar("created_by", 100).nullable()
    val updatedBy: Column<String?> = varchar("updated_by", 100).nullable()
    val version: Column<Long> = long("version").default(1)
    
    // Cleanup and Archival
    val archived: Column<Boolean> = bool("archived").default(false)
    val archivedAt: Column<Instant?> = timestamp("archived_at").nullable()
    val archiveReason: Column<String?> = varchar("archive_reason", 500).nullable()
    val purgeEligible: Column<Boolean> = bool("purge_eligible").default(false)
    val purgeAt: Column<Instant?> = timestamp("purge_at").nullable()
    val retentionExpiresAt: Column<Instant?> = timestamp("retention_expires_at").nullable()
}
