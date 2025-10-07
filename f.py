#!/usr/bin/env python3
"""
Pika Backend Structure Generator
Kotlin/Ktor backend for the Gen Z social platform
"""

import os
from pathlib import Path

class PikaBackendGenerator:
    def __init__(self, base_path: str = "./pika-backend"):
        self.base_path = Path(base_path)
        
    def generate(self):
        """Generate complete Pika Kotlin backend structure"""
        print("⚡ Generating Pika Backend Structure...")
        print("🚀 Powering conversations\n")
        
        # Root files
        root_files = [
            "README.md",
            ".gitignore",
            "settings.gradle.kts",
            "gradle.properties",
            "build.gradle.kts",
            "docker-compose.yml",
            "Dockerfile"
        ]
        
        # Main application
        main_files = [
            # Application entry
            "src/main/kotlin/com/pika/backend/Application.kt",
            "src/main/kotlin/com/pika/backend/plugins/Routing.kt",
            "src/main/kotlin/com/pika/backend/plugins/Serialization.kt",
            "src/main/kotlin/com/pika/backend/plugins/Monitoring.kt",
            "src/main/kotlin/com/pika/backend/plugins/HTTP.kt",
            "src/main/kotlin/com/pika/backend/plugins/StatusPages.kt",
            "src/main/kotlin/com/pika/backend/plugins/WebSockets.kt",
            
            # API Routes - Yeets
            "src/main/kotlin/com/pika/backend/routes/YeetRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/FeedRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/ThreadRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/ReyeetRoutes.kt",
            
            # API Routes - Users
            "src/main/kotlin/com/pika/backend/routes/UserRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/ProfileRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/FollowRoutes.kt",
            
            # API Routes - Communities
            "src/main/kotlin/com/pika/backend/routes/CommunityRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/TopicZoneRoutes.kt",
            
            # API Routes - Interactions
            "src/main/kotlin/com/pika/backend/routes/ReactionRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/NotificationRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/MessageRoutes.kt",
            
            # API Routes - Explore
            "src/main/kotlin/com/pika/backend/routes/ExploreRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/TrendingRoutes.kt",
            "src/main/kotlin/com/pika/backend/routes/SearchRoutes.kt",
            
            # Domain models
            "src/main/kotlin/com/pika/backend/domain/model/Yeet.kt",
            "src/main/kotlin/com/pika/backend/domain/model/User.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Community.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Notification.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Message.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Reaction.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Badge.kt",
            "src/main/kotlin/com/pika/backend/domain/model/UserLevel.kt",
            "src/main/kotlin/com/pika/backend/domain/model/ReplyStreak.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Thread.kt",
            "src/main/kotlin/com/pika/backend/domain/model/Trend.kt",
            "src/main/kotlin/com/pika/backend/domain/model/FriendsNote.kt",
            
            # DTOs - Request
            "src/main/kotlin/com/pika/backend/dto/request/CreateYeetRequest.kt",
            "src/main/kotlin/com/pika/backend/dto/request/UpdateProfileRequest.kt",
            "src/main/kotlin/com/pika/backend/dto/request/CreateCommunityRequest.kt",
            "src/main/kotlin/com/pika/backend/dto/request/SendMessageRequest.kt",
            "src/main/kotlin/com/pika/backend/dto/request/AddReactionRequest.kt",
            "src/main/kotlin/com/pika/backend/dto/request/ReportContentRequest.kt",
            
            # DTOs - Response
            "src/main/kotlin/com/pika/backend/dto/response/YeetResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/UserResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/FeedResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/ThreadResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/NotificationResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/TrendingResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/StatsResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/ApiResponse.kt",
            "src/main/kotlin/com/pika/backend/dto/response/PaginatedResponse.kt",
            
            # Services - Core
            "src/main/kotlin/com/pika/backend/service/YeetService.kt",
            "src/main/kotlin/com/pika/backend/service/UserService.kt",
            "src/main/kotlin/com/pika/backend/service/CommunityService.kt",
            "src/main/kotlin/com/pika/backend/service/NotificationService.kt",
            "src/main/kotlin/com/pika/backend/service/MessageService.kt",
            "src/main/kotlin/com/pika/backend/service/FeedService.kt",
            "src/main/kotlin/com/pika/backend/service/ThreadService.kt",
            
            # Services - Features
            "src/main/kotlin/com/pika/backend/service/TrendingService.kt",
            "src/main/kotlin/com/pika/backend/service/SearchService.kt",
            "src/main/kotlin/com/pika/backend/service/ReactionService.kt",
            "src/main/kotlin/com/pika/backend/service/FollowService.kt",
            "src/main/kotlin/com/pika/backend/service/MediaService.kt",
            "src/main/kotlin/com/pika/backend/service/CacheService.kt",
            
            # Services - Gamification
            "src/main/kotlin/com/pika/backend/service/gamification/XPService.kt",
            "src/main/kotlin/com/pika/backend/service/gamification/BadgeService.kt",
            "src/main/kotlin/com/pika/backend/service/gamification/AchievementService.kt",
            "src/main/kotlin/com/pika/backend/service/gamification/StreakService.kt",
            "src/main/kotlin/com/pika/backend/service/gamification/LevelService.kt",
            
            # Services - Safety & Moderation
            "src/main/kotlin/com/pika/backend/service/safety/ContentModerationService.kt",
            "src/main/kotlin/com/pika/backend/service/safety/VibeCheckService.kt",
            "src/main/kotlin/com/pika/backend/service/safety/SpamDetectionService.kt",
            "src/main/kotlin/com/pika/backend/service/safety/AgeScopeService.kt",
            "src/main/kotlin/com/pika/backend/service/safety/ReportService.kt",
            
            # Services - Analytics
            "src/main/kotlin/com/pika/backend/service/analytics/EngagementAnalytics.kt",
            "src/main/kotlin/com/pika/backend/service/analytics/TrendAnalytics.kt",
            "src/main/kotlin/com/pika/backend/service/analytics/UserAnalytics.kt",
            
            # Repositories
            "src/main/kotlin/com/pika/backend/repository/YeetRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/UserRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/CommunityRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/NotificationRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/MessageRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/ReactionRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/FollowRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/BadgeRepository.kt",
            "src/main/kotlin/com/pika/backend/repository/TrendRepository.kt",
            
            # Database - Exposed ORM
            "src/main/kotlin/com/pika/backend/database/DatabaseFactory.kt",
            "src/main/kotlin/com/pika/backend/database/tables/YeetsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/UsersTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/CommunitiesTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/NotificationsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/MessagesTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/ReactionsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/FollowsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/BadgesTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/UserBadgesTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/StreaksTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/ReportsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/TrendsTable.kt",
            "src/main/kotlin/com/pika/backend/database/tables/FriendsNotesTable.kt",
            
            # Database migrations
            "src/main/kotlin/com/pika/backend/database/migrations/V1_InitialSchema.kt",
            "src/main/kotlin/com/pika/backend/database/migrations/V2_GamificationTables.kt",
            "src/main/kotlin/com/pika/backend/database/migrations/V3_SafetyTables.kt",
            
            # WebSocket handlers
            "src/main/kotlin/com/pika/backend/websocket/FeedWebSocket.kt",
            "src/main/kotlin/com/pika/backend/websocket/NotificationWebSocket.kt",
            "src/main/kotlin/com/pika/backend/websocket/ChatWebSocket.kt",
            "src/main/kotlin/com/pika/backend/websocket/PresenceWebSocket.kt",
            
            # Background jobs
            "src/main/kotlin/com/pika/backend/jobs/TrendingCalculationJob.kt",
            "src/main/kotlin/com/pika/backend/jobs/NotificationBatchJob.kt",
            "src/main/kotlin/com/pika/backend/jobs/ContentModerationJob.kt",
            "src/main/kotlin/com/pika/backend/jobs/StreakResetJob.kt",
            "src/main/kotlin/com/pika/backend/jobs/CacheWarmupJob.kt",
            "src/main/kotlin/com/pika/backend/jobs/AnalyticsAggregationJob.kt",
            
            # Utils
            "src/main/kotlin/com/pika/backend/utils/Extensions.kt",
            "src/main/kotlin/com/pika/backend/utils/DateUtils.kt",
            "src/main/kotlin/com/pika/backend/utils/ValidationUtils.kt",
            "src/main/kotlin/com/pika/backend/utils/HashUtils.kt",
            "src/main/kotlin/com/pika/backend/utils/Logger.kt",
            "src/main/kotlin/com/pika/backend/utils/RandomUtils.kt",
            
            # Config
            "src/main/kotlin/com/pika/backend/config/DatabaseConfig.kt",
            "src/main/kotlin/com/pika/backend/config/RedisConfig.kt",
            "src/main/kotlin/com/pika/backend/config/S3Config.kt",
            "src/main/kotlin/com/pika/backend/config/AppConfig.kt",
            "src/main/kotlin/com/pika/backend/config/RateLimitConfig.kt",
            
            # DI (Koin)
            "src/main/kotlin/com/pika/backend/di/AppModule.kt",
            "src/main/kotlin/com/pika/backend/di/ServiceModule.kt",
            "src/main/kotlin/com/pika/backend/di/RepositoryModule.kt",
            "src/main/kotlin/com/pika/backend/di/DatabaseModule.kt",
            
            # Middleware
            "src/main/kotlin/com/pika/backend/middleware/RateLimitMiddleware.kt",
            "src/main/kotlin/com/pika/backend/middleware/RequestLoggingMiddleware.kt",
            "src/main/kotlin/com/pika/backend/middleware/ErrorHandlingMiddleware.kt",
            "src/main/kotlin/com/pika/backend/middleware/CorsMiddleware.kt",
            
            # ML/AI Integration
            "src/main/kotlin/com/pika/backend/ml/VibeCheckAI.kt",
            "src/main/kotlin/com/pika/backend/ml/ContentClassifier.kt",
            "src/main/kotlin/com/pika/backend/ml/ToxicityDetector.kt",
            "src/main/kotlin/com/pika/backend/ml/RecommendationEngine.kt",
            
            # External integrations
            "src/main/kotlin/com/pika/backend/external/PushNotificationClient.kt",
            "src/main/kotlin/com/pika/backend/external/S3Client.kt",
            "src/main/kotlin/com/pika/backend/external/RedisClient.kt",
            "src/main/kotlin/com/pika/backend/external/ElasticsearchClient.kt",
            
            # Resources
            "src/main/resources/application.conf",
            "src/main/resources/logback.xml",
            "src/main/resources/db-config.properties",
        ]
        
        # Test files
        test_files = [
            "src/test/kotlin/com/pika/backend/ApplicationTest.kt",
            "src/test/kotlin/com/pika/backend/service/YeetServiceTest.kt",
            "src/test/kotlin/com/pika/backend/service/FeedServiceTest.kt",
            "src/test/kotlin/com/pika/backend/service/gamification/XPServiceTest.kt",
            "src/test/kotlin/com/pika/backend/service/safety/VibeCheckServiceTest.kt",
            "src/test/kotlin/com/pika/backend/routes/YeetRoutesTest.kt",
            "src/test/kotlin/com/pika/backend/routes/FeedRoutesTest.kt",
            "src/test/kotlin/com/pika/backend/repository/YeetRepositoryTest.kt",
            "src/test/resources/application-test.conf",
        ]
        
        # Scripts
        script_files = [
            "scripts/setup-db.sh",
            "scripts/seed-data.sh",
            "scripts/run-migrations.sh",
            "scripts/backup-db.sh",
            "scripts/deploy.sh",
        ]
        
        # CI/CD
        cicd_files = [
            ".github/workflows/backend-build.yml",
            ".github/workflows/backend-test.yml",
            ".github/workflows/backend-deploy.yml",
            ".github/workflows/security-scan.yml",
        ]
        
        # Kubernetes
        k8s_files = [
            "k8s/deployment.yaml",
            "k8s/service.yaml",
            "k8s/ingress.yaml",
            "k8s/configmap.yaml",
            "k8s/secrets.yaml",
            "k8s/hpa.yaml",
        ]
        
        # Create all files
        all_files = (root_files + main_files + test_files + 
                    script_files + cicd_files + k8s_files)
        
        for file_path in all_files:
            full_path = self.base_path / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            full_path.touch()
        
        # Make scripts executable
        for script in script_files:
            script_path = self.base_path / script
            script_path.chmod(0o755)
        
        print(f"✅ Created {len(all_files)} files")
        print(f"📁 Location: {self.base_path.absolute()}")
        print("\n📂 Structure:")
        print("  ├── src/main/kotlin/")
        print("  │   ├── routes/           # API endpoints")
        print("  │   ├── service/          # Business logic")
        print("  │   │   ├── gamification/ # XP, badges, streaks")
        print("  │   │   └── safety/       # Moderation, vibe check")
        print("  │   ├── repository/       # Data access")
        print("  │   ├── database/         # Exposed ORM tables")
        print("  │   ├── websocket/        # Real-time features")
        print("  │   ├── jobs/             # Background tasks")
        print("  │   └── ml/               # AI/ML models")
        print("  ├── src/test/             # Unit & integration tests")
        print("  ├── scripts/              # Deployment scripts")
        print("  ├── k8s/                  # Kubernetes configs")
        print("  └── docker-compose.yml    # Local dev environment")
        print("\n🔥 Stack: Ktor + Exposed + PostgreSQL + Redis + Koin")

if __name__ == "__main__":
    generator = PikaBackendGenerator()
    generator.generate()
