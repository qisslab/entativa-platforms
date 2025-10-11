package com.entativa.id.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Devices Table Definition for Entativa ID
 * Manages user devices for security, authentication, and cross-platform experiences
 * 
 * @author Neo Qiss
 * @status Production-ready device management with enterprise security features
 */
object DevicesTable : UUIDTable("devices") {
    
    // Core Device Information
    val deviceId: Column<String> = varchar("device_id", 128).uniqueIndex() // Unique device identifier
    val userId: Column<String> = varchar("user_id", 100).index()
    val deviceName: Column<String> = varchar("device_name", 200) // User-friendly name
    val deviceType: Column<String> = varchar("device_type", 30) // MOBILE, TABLET, DESKTOP, LAPTOP, SMART_TV, CONSOLE, WATCH, IOT
    val deviceCategory: Column<String> = varchar("device_category", 20).default("PERSONAL") // PERSONAL, WORK, SHARED, PUBLIC
    
    // Device Hardware & Software
    val manufacturer: Column<String?> = varchar("manufacturer", 100).nullable() // Apple, Samsung, Google, etc.
    val model: Column<String?> = varchar("model", 200).nullable() // iPhone 15 Pro, Galaxy S24, etc.
    val modelNumber: Column<String?> = varchar("model_number", 100).nullable()
    val serialNumber: Column<String?> = varchar("serial_number", 200).nullable()
    val hardwareId: Column<String?> = varchar("hardware_id", 256).nullable()
    val chipset: Column<String?> = varchar("chipset", 100).nullable()
    val architecture: Column<String?> = varchar("architecture", 20).nullable() // x64, ARM64, etc.
    
    // Operating System Information
    val operatingSystem: Column<String> = varchar("operating_system", 50) // iOS, Android, Windows, macOS, Linux
    val osVersion: Column<String?> = varchar("os_version", 50).nullable()
    val osBuild: Column<String?> = varchar("os_build", 100).nullable()
    val kernelVersion: Column<String?> = varchar("kernel_version", 100).nullable()
    val systemLanguage: Column<String> = varchar("system_language", 10).default("en")
    val timezone: Column<String> = varchar("timezone", 50).default("UTC")
    val locale: Column<String?> = varchar("locale", 20).nullable()
    
    // Application Information
    val appVersion: Column<String?> = varchar("app_version", 50).nullable()
    val appBuild: Column<String?> = varchar("app_build", 50).nullable()
    val sdkVersion: Column<String?> = varchar("sdk_version", 50).nullable()
    val packageName: Column<String?> = varchar("package_name", 200).nullable()
    val bundleId: Column<String?> = varchar("bundle_id", 200).nullable()
    val installationId: Column<String?> = varchar("installation_id", 128).nullable()
    
    // Browser Information (for web devices)
    val browser: Column<String?> = varchar("browser", 100).nullable()
    val browserVersion: Column<String?> = varchar("browser_version", 50).nullable()
    val userAgent: Column<String?> = text("user_agent").nullable()
    val webglRenderer: Column<String?> = varchar("webgl_renderer", 200).nullable()
    val screenResolution: Column<String?> = varchar("screen_resolution", 20).nullable()
    val colorDepth: Column<Int?> = integer("color_depth").nullable()
    val cookiesEnabled: Column<Boolean?> = bool("cookies_enabled").nullable()
    val javaScriptEnabled: Column<Boolean?> = bool("java_script_enabled").nullable()
    
    // Device Capabilities
    val hasFingerprint: Column<Boolean> = bool("has_fingerprint").default(false)
    val hasFaceId: Column<Boolean> = bool("has_face_id").default(false)
    val hasNFC: Column<Boolean> = bool("has_nfc").default(false)
    val hasBluetooth: Column<Boolean> = bool("has_bluetooth").default(false)
    val hasCamera: Column<Boolean> = bool("has_camera").default(false)
    val hasMicrophone: Column<Boolean> = bool("has_microphone").default(false)
    val hasGPS: Column<Boolean> = bool("has_gps").default(false)
    val hasAccelerometer: Column<Boolean> = bool("has_accelerometer").default(false)
    val hasGyroscope: Column<Boolean> = bool("has_gyroscope").default(false)
    val supportedBiometrics: Column<String> = text("supported_biometrics").default("[]") // JSON array
    
    // Security Information
    val deviceFingerprint: Column<String> = varchar("device_fingerprint", 512).index() // Unique device fingerprint
    val fingerprintComponents: Column<String> = text("fingerprint_components").default("{}") // JSON object
    val isRooted: Column<Boolean?> = bool("is_rooted").nullable() // Android root detection
    val isJailbroken: Column<Boolean?> = bool("is_jailbroken").nullable() // iOS jailbreak detection
    val hasSecureHardware: Column<Boolean> = bool("has_secure_hardware").default(false)
    val securityLevel: Column<String> = varchar("security_level", 20).default("STANDARD") // BASIC, STANDARD, HIGH, MAXIMUM
    val encryptionSupported: Column<Boolean> = bool("encryption_supported").default(false)
    val keystore: Column<String?> = varchar("keystore", 100).nullable() // Android Keystore, iOS Keychain, etc.
    
    // Trust and Verification
    val isTrusted: Column<Boolean> = bool("is_trusted").default(false)
    val trustLevel: Column<String> = varchar("trust_level", 20).default("UNKNOWN") // UNKNOWN, LOW, MEDIUM, HIGH, VERIFIED
    val trustedAt: Column<Instant?> = timestamp("trusted_at").nullable()
    val trustScore: Column<Double> = double("trust_score").default(0.0)
    val verificationMethod: Column<String?> = varchar("verification_method", 50).nullable()
    val verifiedAt: Column<Instant?> = timestamp("verified_at").nullable()
    val attestationData: Column<String?> = text("attestation_data").nullable()
    val deviceIntegrity: Column<String> = varchar("device_integrity", 20).default("UNKNOWN") // UNKNOWN, BASIC, DEVICE, STRONG
    
    // Registration and Authentication
    val registeredAt: Column<Instant> = timestamp("registered_at").default(Instant.now())
    val firstSeenAt: Column<Instant> = timestamp("first_seen_at").default(Instant.now())
    val lastSeenAt: Column<Instant> = timestamp("last_seen_at").default(Instant.now())
    val registrationMethod: Column<String> = varchar("registration_method", 50).default("AUTOMATIC") // AUTOMATIC, MANUAL, QR_CODE, NFC
    val registrationIP: Column<String?> = varchar("registration_ip", 45).nullable()
    val lastAuthAt: Column<Instant?> = timestamp("last_auth_at").nullable()
    val authCount: Column<Long> = long("auth_count").default(0)
    val failedAuthAttempts: Column<Int> = integer("failed_auth_attempts").default(0)
    val lastFailedAuthAt: Column<Instant?> = timestamp("last_failed_auth_at").nullable()
    
    // Device Status and Lifecycle
    val status: Column<String> = varchar("status", 20).default("ACTIVE") // ACTIVE, INACTIVE, SUSPENDED, REVOKED, LOST, STOLEN
    val isActive: Column<Boolean> = bool("is_active").default(true)
    val isOnline: Column<Boolean> = bool("is_online").default(false)
    val lastOnlineAt: Column<Instant?> = timestamp("last_online_at").nullable()
    val isPrimary: Column<Boolean> = bool("is_primary").default(false)
    val isManaged: Column<Boolean> = bool("is_managed").default(false) // MDM managed device
    val managementProfile: Column<String?> = varchar("management_profile", 100).nullable()
    val complianceStatus: Column<String> = varchar("compliance_status", 20).default("COMPLIANT") // COMPLIANT, NON_COMPLIANT, UNKNOWN
    
    // Network and Location
    val lastKnownIP: Column<String?> = varchar("last_known_ip", 45).nullable()
    val lastKnownCountry: Column<String?> = varchar("last_known_country", 3).nullable()
    val lastKnownRegion: Column<String?> = varchar("last_known_region", 100).nullable()
    val lastKnownCity: Column<String?> = varchar("last_known_city", 100).nullable()
    val lastKnownISP: Column<String?> = varchar("last_known_isp", 200).nullable()
    val connectionType: Column<String?> = varchar("connection_type", 20).nullable() // WIFI, CELLULAR, ETHERNET, VPN
    val networkOperator: Column<String?> = varchar("network_operator", 100).nullable()
    val wifiSSID: Column<String?> = varchar("wifi_ssid", 200).nullable()
    val vpnDetected: Column<Boolean> = bool("vpn_detected").default(false)
    val proxyDetected: Column<Boolean> = bool("proxy_detected").default(false)
    val geoLocation: Column<String?> = varchar("geo_location", 100).nullable() // lat,lng
    val locationAccuracy: Column<Double?> = double("location_accuracy").nullable()
    
    // Push Notifications
    val pushToken: Column<String?> = text("push_token").nullable()
    val pushProvider: Column<String?> = varchar("push_provider", 30).nullable() // FCM, APNS, WNS
    val pushEnabled: Column<Boolean> = bool("push_enabled").default(false)
    val pushTokenUpdatedAt: Column<Instant?> = timestamp("push_token_updated_at").nullable()
    val notificationSettings: Column<String> = text("notification_settings").default("{}") // JSON object
    val badgeCount: Column<Int> = integer("badge_count").default(0)
    
    // Session Management
    val maxConcurrentSessions: Column<Int> = integer("max_concurrent_sessions").default(1)
    val currentSessions: Column<Int> = integer("current_sessions").default(0)
    val sessionTimeout: Column<Int> = integer("session_timeout").default(3600) // seconds
    val rememberDevice: Column<Boolean> = bool("remember_device").default(false)
    val autoLogin: Column<Boolean> = bool("auto_login").default(false)
    val biometricLogin: Column<Boolean> = bool("biometric_login").default(false)
    val offlineAccess: Column<Boolean> = bool("offline_access").default(false)
    
    // Risk and Fraud Detection
    val riskScore: Column<Double> = double("risk_score").default(0.0)
    val anomalyScore: Column<Double> = double("anomaly_score").default(0.0)
    val fraudFlags: Column<String> = text("fraud_flags").default("[]") // JSON array
    val behaviorProfile: Column<String> = text("behavior_profile").default("{}") // JSON object
    val usagePatterns: Column<String> = text("usage_patterns").default("{}") // JSON object
    val suspiciousActivity: Column<Boolean> = bool("suspicious_activity").default(false)
    val lastRiskAssessment: Column<Instant?> = timestamp("last_risk_assessment").nullable()
    val riskFactors: Column<String> = text("risk_factors").default("[]") // JSON array
    
    // Compliance and Policy
    val policyCompliant: Column<Boolean> = bool("policy_compliant").default(true)
    val policyVersion: Column<String?> = varchar("policy_version", 20).nullable()
    val lastPolicyCheck: Column<Instant?> = timestamp("last_policy_check").nullable()
    val requiredUpdates: Column<String> = text("required_updates").default("[]") // JSON array
    val securityPatch: Column<String?> = varchar("security_patch", 50).nullable()
    val patchLevel: Column<String?> = varchar("patch_level", 50).nullable()
    val vulnerabilities: Column<String> = text("vulnerabilities").default("[]") // JSON array
    
    // Performance and Health
    val performanceScore: Column<Double> = double("performance_score").default(0.0)
    val batteryLevel: Column<Int?> = integer("battery_level").nullable()
    val storageUsed: Column<Long?> = long("storage_used").nullable()
    val storageTotal: Column<Long?> = long("storage_total").nullable()
    val memoryUsed: Column<Long?> = long("memory_used").nullable()
    val memoryTotal: Column<Long?> = long("memory_total").nullable()
    val cpuUsage: Column<Double?> = double("cpu_usage").nullable()
    val networkLatency: Column<Int?> = integer("network_latency").nullable()
    val lastHealthCheck: Column<Instant?> = timestamp("last_health_check").nullable()
    
    // Privacy and Data
    val privacyLevel: Column<String> = varchar("privacy_level", 20).default("STANDARD") // MINIMAL, STANDARD, ENHANCED, MAXIMUM
    val dataCollectionConsent: Column<Boolean> = bool("data_collection_consent").default(false)
    val analyticsEnabled: Column<Boolean> = bool("analytics_enabled").default(true)
    val crashReportingEnabled: Column<Boolean> = bool("crash_reporting_enabled").default(true)
    val telemetryEnabled: Column<Boolean> = bool("telemetry_enabled").default(true)
    val locationSharingEnabled: Column<Boolean> = bool("location_sharing_enabled").default(false)
    val dataRetentionDays: Column<Int> = integer("data_retention_days").default(365)
    
    // Synchronization
    val syncEnabled: Column<Boolean> = bool("sync_enabled").default(true)
    val syncPreferences: Column<String> = text("sync_preferences").default("{}") // JSON object
    val lastSyncAt: Column<Instant?> = timestamp("last_sync_at").nullable()
    val syncVersion: Column<Long> = long("sync_version").default(1)
    val syncConflicts: Column<String> = text("sync_conflicts").default("[]") // JSON array
    val offlineChanges: Column<String> = text("offline_changes").default("[]") // JSON array
    
    // Custom Attributes and Extensions
    val customAttributes: Column<String> = text("custom_attributes").default("{}") // JSON object
    val deviceMetadata: Column<String> = text("device_metadata").default("{}") // JSON object
    val extensionData: Column<String> = text("extension_data").default("{}") // JSON object
    val thirdPartyData: Column<String> = text("third_party_data").default("{}") // JSON object
    val tags: Column<String> = text("tags").default("[]") // JSON array
    val notes: Column<String?> = text("notes").nullable()
    
    // Administrative
    val adminNotes: Column<String?> = text("admin_notes").nullable()
    val adminFlags: Column<String> = text("admin_flags").default("[]") // JSON array
    val quarantined: Column<Boolean> = bool("quarantined").default(false)
    val quarantineReason: Column<String?> = text("quarantine_reason").nullable()
    val quarantinedAt: Column<Instant?> = timestamp("quarantined_at").nullable()
    val quarantinedBy: Column<String?> = varchar("quarantined_by", 100).nullable()
    val whitelisted: Column<Boolean> = bool("whitelisted").default(false)
    val blacklisted: Column<Boolean> = bool("blacklisted").default(false)
    
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
    val deregisteredAt: Column<Instant?> = timestamp("deregistered_at").nullable()
    val deregistrationReason: Column<String?> = varchar("deregistration_reason", 500).nullable()
}
