#!/usr/bin/env python3
"""
Entativa ID - Universal Identity Platform
SSO, Unified Profiles, Handle Reservation across Sonet, Pika, and Gala
Futuristic metaverse-style authentication with KMP+CMP
"""

import os
from pathlib import Path

class EntativaIDGenerator:
    def __init__(self, base_path: str = "./entativa-id"):
        self.base_path = Path(base_path)
        
    def generate(self):
        """Generate complete Entativa ID system"""
        print("🌐 Generating Entativa ID System...")
        print("🔐 One Identity. Three Worlds.\n")
        
        # Root files
        root_files = [
            "README.md",
            ".gitignore",
            "settings.gradle.kts",
            "gradle.properties",
            "build.gradle.kts",
            "docker-compose.yml",
            "ARCHITECTURE.md",
            "SECURITY.md"
        ]
        
        # ============================================================
        # BACKEND - Kotlin/Ktor Identity Server
        # ============================================================
        backend_files = [
            # Main application
            "backend/src/main/kotlin/com/entativa/id/Application.kt",
            "backend/src/main/kotlin/com/entativa/id/plugins/Routing.kt",
            "backend/src/main/kotlin/com/entativa/id/plugins/Serialization.kt",
            "backend/src/main/kotlin/com/entativa/id/plugins/Security.kt",
            "backend/src/main/kotlin/com/entativa/id/plugins/Monitoring.kt",
            "backend/src/main/kotlin/com/entativa/id/plugins/WebSockets.kt",
            
            # OAuth2/OpenID Connect
            "backend/src/main/kotlin/com/entativa/id/oauth/OAuthServer.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/AuthorizationEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/TokenEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/UserInfoEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/JWKSEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/DiscoveryEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/RevocationEndpoint.kt",
            "backend/src/main/kotlin/com/entativa/id/oauth/IntrospectionEndpoint.kt",
            
            # Authentication routes
            "backend/src/main/kotlin/com/entativa/id/routes/auth/SignupRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/LoginRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/LogoutRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/PasswordResetRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/PassphraseRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/MFARoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/BiometricRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/auth/SocialAuthRoutes.kt",
            
            # Account management routes
            "backend/src/main/kotlin/com/entativa/id/routes/account/ProfileRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/HandleRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/SecurityRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/SessionRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/DeviceRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/RecoveryRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/PrivacyRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/account/DataExportRoutes.kt",
            
            # Handle system
            "backend/src/main/kotlin/com/entativa/id/routes/handle/HandleReservationRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/handle/HandleSearchRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/handle/HandleValidationRoutes.kt",
            
            # App integration routes
            "backend/src/main/kotlin/com/entativa/id/routes/apps/AppRegistrationRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/apps/AppPermissionsRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/apps/AppConnectionsRoutes.kt",
            
            # Profile sync routes
            "backend/src/main/kotlin/com/entativa/id/routes/sync/ProfileSyncRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/sync/MetadataRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/sync/PreferencesRoutes.kt",
            
            # Admin routes
            "backend/src/main/kotlin/com/entativa/id/routes/admin/UserManagementRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/admin/AuditLogRoutes.kt",
            "backend/src/main/kotlin/com/entativa/id/routes/admin/SecurityReportsRoutes.kt",
            
            # Domain models
            "backend/src/main/kotlin/com/entativa/id/domain/model/User.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/EntativaAccount.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/Handle.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/Profile.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/UnifiedProfile.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/AppProfile.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/Session.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/Device.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/OAuthClient.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/AccessToken.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/RefreshToken.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/AuthorizationCode.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/MFAMethod.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/RecoveryMethod.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/AuditLog.kt",
            "backend/src/main/kotlin/com/entativa/id/domain/model/ConnectedApp.kt",
            
            # Services - Core
            "backend/src/main/kotlin/com/entativa/id/service/AuthenticationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/UserService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/SessionService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/TokenService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/OAuthService.kt",
            
            # Services - Handle System
            "backend/src/main/kotlin/com/entativa/id/service/handle/HandleReservationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/handle/HandleValidationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/handle/HandleSyncService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/handle/HandleTransferService.kt",
            
            # Services - Profile Sync
            "backend/src/main/kotlin/com/entativa/id/service/sync/ProfileSyncService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/sync/MetadataSyncEngine.kt",
            "backend/src/main/kotlin/com/entativa/id/service/sync/ConflictResolutionService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/sync/SyncQueueService.kt",
            
            # Services - Security
            "backend/src/main/kotlin/com/entativa/id/service/security/PasswordService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/PassphraseService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/MFAService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/BiometricService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/EncryptionService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/RateLimitService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/FraudDetectionService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/security/AuditLogService.kt",
            
            # Services - Recovery
            "backend/src/main/kotlin/com/entativa/id/service/recovery/AccountRecoveryService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/recovery/EmailVerificationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/recovery/PhoneVerificationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/recovery/TrustedContactsService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/recovery/BackupCodesService.kt",
            
            # Services - Communication
            "backend/src/main/kotlin/com/entativa/id/service/notification/EmailService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/notification/SMSService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/notification/PushNotificationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/notification/NotificationTemplateService.kt",
            
            # Services - App Integration
            "backend/src/main/kotlin/com/entativa/id/service/apps/AppRegistrationService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/apps/AppPermissionService.kt",
            "backend/src/main/kotlin/com/entativa/id/service/apps/SSOService.kt",
            
            # Repositories
            "backend/src/main/kotlin/com/entativa/id/repository/UserRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/HandleRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/ProfileRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/SessionRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/TokenRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/OAuthClientRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/MFARepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/DeviceRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/AuditLogRepository.kt",
            "backend/src/main/kotlin/com/entativa/id/repository/RecoveryMethodRepository.kt",
            
            # Database tables
            "backend/src/main/kotlin/com/entativa/id/database/DatabaseFactory.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/UsersTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/HandlesTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/ProfilesTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/UnifiedProfilesTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/AppProfilesTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/SessionsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/TokensTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/OAuthClientsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/MFAMethodsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/DevicesTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/AuditLogsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/RecoveryMethodsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/ConnectedAppsTable.kt",
            "backend/src/main/kotlin/com/entativa/id/database/tables/SyncQueueTable.kt",
            
            # JWT handling
            "backend/src/main/kotlin/com/entativa/id/jwt/JWTProvider.kt",
            "backend/src/main/kotlin/com/entativa/id/jwt/JWTValidator.kt",
            "backend/src/main/kotlin/com/entativa/id/jwt/JWKSProvider.kt",
            "backend/src/main/kotlin/com/entativa/id/jwt/TokenClaims.kt",
            
            # Utils
            "backend/src/main/kotlin/com/entativa/id/utils/Crypto.kt",
            "backend/src/main/kotlin/com/entativa/id/utils/ValidationUtils.kt",
            "backend/src/main/kotlin/com/entativa/id/utils/HandleUtils.kt",
            "backend/src/main/kotlin/com/entativa/id/utils/PasswordStrength.kt",
            "backend/src/main/kotlin/com/entativa/id/utils/PassphraseGenerator.kt",
            "backend/src/main/kotlin/com/entativa/id/utils/OTPGenerator.kt",
            
            # Config
            "backend/src/main/kotlin/com/entativa/id/config/SecurityConfig.kt",
            "backend/src/main/kotlin/com/entativa/id/config/OAuthConfig.kt",
            "backend/src/main/kotlin/com/entativa/id/config/DatabaseConfig.kt",
            "backend/src/main/kotlin/com/entativa/id/config/RedisConfig.kt",
            
            # DI
            "backend/src/main/kotlin/com/entativa/id/di/AppModule.kt",
            "backend/src/main/kotlin/com/entativa/id/di/ServiceModule.kt",
            "backend/src/main/kotlin/com/entativa/id/di/RepositoryModule.kt",
            
            # Tests
            "backend/src/test/kotlin/com/entativa/id/AuthenticationTest.kt",
            "backend/src/test/kotlin/com/entativa/id/HandleReservationTest.kt",
            "backend/src/test/kotlin/com/entativa/id/ProfileSyncTest.kt",
            "backend/src/test/kotlin/com/entativa/id/OAuthFlowTest.kt",
            
            "backend/build.gradle.kts",
            "backend/src/main/resources/application.conf",
            "backend/src/main/resources/logback.xml"
        ]
        
        # ============================================================
        # SHARED KMP+CMP - UI Components & SDK
        # ============================================================
        shared_files = [
            # Main app
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/EntativaID.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/EntativaIDConfig.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/EntativaIDClient.kt",
            
            # Authentication Screens
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/WelcomeScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/SignupScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/LoginScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/HandleSelectionScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/PasswordCreationScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/PassphraseScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/MFASetupScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/BiometricSetupScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/auth/VerificationScreen.kt",
            
            # Recovery Screens
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/recovery/ForgotPasswordScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/recovery/AccountRecoveryScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/recovery/RecoveryMethodsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/recovery/TrustedContactsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/recovery/BackupCodesScreen.kt",
            
            # Accounts Center
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/AccountsCenterScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/ProfileOverviewScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/HandleManagementScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/SecurityScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/PrivacyScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/ConnectedAppsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/ActiveSessionsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/accounts/DevicesScreen.kt",
            
            # Profile Management
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/profile/EditProfileScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/profile/AvatarEditorScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/profile/BioEditorScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/profile/DisplayNameScreen.kt",
            
            # App Integration Screens
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/oauth/ConsentScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/oauth/AppAuthorizationScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/screens/oauth/PermissionsScreen.kt",
            
            # Components - Futuristic UI
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/EntativaLogo.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/HolographicCard.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/NeonButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/GlassPanel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/ParticleBackground.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/CyberTextField.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/MetaverseAvatar.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/HandleInput.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/PasswordStrengthMeter.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/BiometricPrompt.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/OTPInput.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/QRCodeScanner.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/SecurityShield.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/AppCard.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/SessionCard.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/components/DeviceCard.kt",
            
            # Bottom Sheets
            "shared/src/commonMain/kotlin/com/entativa/id/ui/bottomsheets/LoginBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/bottomsheets/SignupBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/bottomsheets/AccountSwitcherBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/bottomsheets/SecurityOptionsBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/bottomsheets/MFABottomSheet.kt",
            
            # Theme - Futuristic Metaverse
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/EntativaTheme.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/MetaverseColors.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/CyberTypography.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/NeonGradients.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/GlassmorphismShapes.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/theme/HolographicEffects.kt",
            
            # Animations
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/ParticleSystem.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/HologramFlicker.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/NeonPulse.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/GlitchEffect.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/WaveTransition.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/animations/LoadingHologram.kt",
            
            # ViewModels
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/AuthViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/SignupViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/LoginViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/HandleViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/ProfileViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/SecurityViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/AccountsCenterViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/ui/viewmodel/RecoveryViewModel.kt",
            
            # SDK - Client API
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/api/AuthAPI.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/api/ProfileAPI.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/api/HandleAPI.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/api/SecurityAPI.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/api/SyncAPI.kt",
            
            # SDK - Models
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/model/EntativaUser.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/model/UnifiedProfile.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/model/HandleInfo.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/model/AuthToken.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/model/AppConnection.kt",
            
            # SDK - Storage
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/storage/SecureStorage.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/storage/TokenStorage.kt",
            "shared/src/commonMain/kotlin/com/entativa/id/sdk/storage/ProfileCache.kt",
            
            # Platform specific
            "shared/src/androidMain/kotlin/com/entativa/id/platform/BiometricAuth.android.kt",
            "shared/src/androidMain/kotlin/com/entativa/id/platform/SecureStorage.android.kt",
            "shared/src/iosMain/kotlin/com/entativa/id/platform/BiometricAuth.ios.kt",
            "shared/src/iosMain/kotlin/com/entativa/id/platform/SecureStorage.ios.kt",
            
            # Resources
            "shared/src/commonMain/resources/fonts/CyberFont.ttf",
            "shared/src/commonMain/resources/lottie/hologram_loading.json",
            "shared/src/commonMain/resources/lottie/particle_burst.json",
            "shared/src/commonMain/resources/lottie/neon_success.json",
            
            "shared/build.gradle.kts"
        ]
        
        # ============================================================
        # SDK INTEGRATION EXAMPLES
        # ============================================================
        sdk_examples = [
            # Sonet Integration
            "examples/sonet-integration/src/main/kotlin/SonetIntegration.kt",
            "examples/sonet-integration/src/main/kotlin/SonetAuthFlow.kt",
            "examples/sonet-integration/build.gradle.kts",
            
            # Pika Integration
            "examples/pika-integration/src/main/kotlin/PikaIntegration.kt",
            "examples/pika-integration/src/main/kotlin/PikaAuthFlow.kt",
            "examples/pika-integration/build.gradle.kts",
            
            # Gala Integration
            "examples/gala-integration/src/main/kotlin/GalaIntegration.kt",
            "examples/gala-integration/src/main/kotlin/GalaAuthFlow.kt",
            "examples/gala-integration/build.gradle.kts",
        ]
        
        # ============================================================
        # MICROSERVICES
        # ============================================================
        microservices = [
            # Handle Reservation Service
            "services/handle-service/src/main/kotlin/Application.kt",
            "services/handle-service/src/main/kotlin/HandleReservationEngine.kt",
            "services/handle-service/src/main/kotlin/HandleValidator.kt",
            "services/handle-service/build.gradle.kts",
            
            # Profile Sync Service
            "services/sync-service/src/main/kotlin/Application.kt",
            "services/sync-service/src/main/kotlin/SyncCoordinator.kt",
            "services/sync-service/src/main/kotlin/ConflictResolver.kt",
            "services/sync-service/build.gradle.kts",
            
            # Notification Service
            "services/notification-service/src/main/kotlin/Application.kt",
            "services/notification-service/src/main/kotlin/NotificationDispatcher.kt",
            "services/notification-service/build.gradle.kts",
            
            # Fraud Detection Service
            "services/fraud-service/src/main/kotlin/Application.kt",
            "services/fraud-service/src/main/kotlin/FraudDetector.kt",
            "services/fraud-service/src/main/kotlin/RiskScorer.kt",
            "services/fraud-service/build.gradle.kts",
        ]
        
        # ============================================================
        # INFRASTRUCTURE
        # ============================================================
        infra_files = [
            # Kubernetes
            "infra/k8s/namespace.yaml",
            "infra/k8s
