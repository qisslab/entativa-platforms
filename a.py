#!/usr/bin/env python3
"""
Pika KMP+CMP Structure Generator
Twitter/X & Threads competitor with Gen Z energy
"""

import os
from pathlib import Path

class PikaKMPGenerator:
    def __init__(self, base_path: str = "./pika-kmp"):
        self.base_path = Path(base_path)
        
    def generate(self):
        """Generate complete Pika KMP+CMP structure"""
        print("⚡ Generating Pika KMP+CMP Structure...")
        print("💬 Where conversations spark\n")
        
        # Root files
        root_files = [
            "README.md",
            ".gitignore",
            "settings.gradle.kts",
            "gradle.properties",
            "build.gradle.kts"
        ]
        
        # Shared module (KMP+CMP)
        shared_files = [
            # Main app
            "shared/src/commonMain/kotlin/com/pika/app/App.kt",
            "shared/src/commonMain/kotlin/com/pika/app/di/AppModule.kt",
            
            # Core features - Feed
            "shared/src/commonMain/kotlin/com/pika/app/features/feed/FeedScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/feed/FeedViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/feed/components/YeetCard.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/feed/components/FriendsNotes.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/feed/components/TrendingWords.kt",
            
            # Compose (Create Yeet)
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/ComposeScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/ComposeViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/components/YeetEditor.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/components/FontPicker.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/components/ColorGradient.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/compose/components/EmojiPicker.kt",
            
            # Thread view
            "shared/src/commonMain/kotlin/com/pika/app/features/thread/ThreadScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/thread/ThreadViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/thread/components/ThreadTree.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/thread/components/ReplyCard.kt",
            
            # Explore
            "shared/src/commonMain/kotlin/com/pika/app/features/explore/ExploreScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/explore/ExploreViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/explore/components/TrendingSection.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/explore/components/TopicZones.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/explore/components/ThreadOfTheDay.kt",
            
            # Profile
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/ProfileScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/ProfileViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/components/ProfileHeader.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/components/StatsCard.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/components/YeetsList.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/profile/components/BadgeGrid.kt",
            
            # Settings & Customization
            "shared/src/commonMain/kotlin/com/pika/app/features/settings/SettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/settings/SettingsViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/settings/components/PalettePicker.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/settings/components/SafetyControls.kt",
            
            # Communities (Topic Zones)
            "shared/src/commonMain/kotlin/com/pika/app/features/communities/CommunitiesScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/communities/CommunityViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/communities/components/CommunityCard.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/communities/components/CommunityFeed.kt",
            
            # Notifications
            "shared/src/commonMain/kotlin/com/pika/app/features/notifications/NotificationsScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/notifications/NotificationsViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/notifications/components/NotificationCard.kt",
            
            # Messages/DMs
            "shared/src/commonMain/kotlin/com/pika/app/features/messages/MessagesScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/messages/MessagesViewModel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/messages/ChatScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/messages/ChatViewModel.kt",
            
            # Onboarding
            "shared/src/commonMain/kotlin/com/pika/app/features/onboarding/OnboardingScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/onboarding/PaletteSelectionScreen.kt",
            "shared/src/commonMain/kotlin/com/pika/app/features/onboarding/InterestsScreen.kt",
            
            # Domain models
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/Yeet.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/User.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/Community.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/Notification.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/Palette.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/Badge.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/UserLevel.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/model/ReplyStreak.kt",
            
            # Repositories
            "shared/src/commonMain/kotlin/com/pika/app/domain/repository/YeetRepository.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/repository/UserRepository.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/repository/CommunityRepository.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/repository/NotificationRepository.kt",
            
            # Use cases
            "shared/src/commonMain/kotlin/com/pika/app/domain/usecase/CreateYeetUseCase.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/usecase/ReyeetUseCase.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/usecase/GetFeedUseCase.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/usecase/GetThreadUseCase.kt",
            "shared/src/commonMain/kotlin/com/pika/app/domain/usecase/VibeCheckUseCase.kt",
            
            # Data layer
            "shared/src/commonMain/kotlin/com/pika/app/data/remote/PikaApi.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/remote/NetworkClient.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/remote/dto/YeetDto.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/remote/dto/UserDto.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/repository/YeetRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/repository/UserRepositoryImpl.kt",
            
            # Local storage
            "shared/src/commonMain/kotlin/com/pika/app/data/local/PikaDatabase.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/local/dao/YeetDao.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/local/dao/UserDao.kt",
            "shared/src/commonMain/kotlin/com/pika/app/data/local/entity/YeetEntity.kt",
            
            # UI components
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/PikaButton.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/PikaTextField.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/PikaTopBar.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/PikaBottomBar.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/LightningButton.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/ReactionPicker.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/UserAvatar.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/BadgeIcon.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/components/StreakIndicator.kt",
            
            # Theme system
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/PikaTheme.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/ColorPalette.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/MonochromeTheme.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/Palettes.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/Typography.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/theme/Shapes.kt",
            
            # Animations
            "shared/src/commonMain/kotlin/com/pika/app/ui/animations/LightningAnimation.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/animations/SparkAnimation.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/animations/RippleAnimation.kt",
            "shared/src/commonMain/kotlin/com/pika/app/ui/animations/MascotAnimation.kt",
            
            # Utils
            "shared/src/commonMain/kotlin/com/pika/app/utils/DateUtils.kt",
            "shared/src/commonMain/kotlin/com/pika/app/utils/StringUtils.kt",
            "shared/src/commonMain/kotlin/com/pika/app/utils/ValidationUtils.kt",
            "shared/src/commonMain/kotlin/com/pika/app/utils/Logger.kt",
            "shared/src/commonMain/kotlin/com/pika/app/utils/HapticFeedback.kt",
            
            # Gamification
            "shared/src/commonMain/kotlin/com/pika/app/gamification/XPManager.kt",
            "shared/src/commonMain/kotlin/com/pika/app/gamification/AchievementEngine.kt",
            "shared/src/commonMain/kotlin/com/pika/app/gamification/BadgeUnlocker.kt",
            "shared/src/commonMain/kotlin/com/pika/app/gamification/StreakTracker.kt",
            
            # Safety
            "shared/src/commonMain/kotlin/com/pika/app/safety/ContentFilter.kt",
            "shared/src/commonMain/kotlin/com/pika/app/safety/AgeScopeManager.kt",
            "shared/src/commonMain/kotlin/com/pika/app/safety/VibeCheckAI.kt",
            "shared/src/commonMain/kotlin/com/pika/app/safety/ModerationTools.kt",
            
            # Platform-specific
            "shared/src/androidMain/kotlin/com/pika/app/platform/HapticFeedback.android.kt",
            "shared/src/androidMain/kotlin/com/pika/app/platform/ShareSheet.android.kt",
            "shared/src/iosMain/kotlin/com/pika/app/platform/HapticFeedback.ios.kt",
            "shared/src/iosMain/kotlin/com/pika/app/platform/ShareSheet.ios.kt",
            
            # Resources
            "shared/src/commonMain/resources/drawable/logo.xml",
            "shared/src/commonMain/resources/drawable/mascot.xml",
            "shared/src/commonMain/resources/drawable/lightning.xml",
            
            # Tests
            "shared/src/commonTest/kotlin/com/pika/app/YeetViewModelTest.kt",
            "shared/src/commonTest/kotlin/com/pika/app/VibeCheckTest.kt",
            "shared/src/commonTest/kotlin/com/pika/app/XPManagerTest.kt",
            
            # Build
            "shared/build.gradle.kts"
        ]
        
        # Android app
        android_files = [
            "androidApp/src/main/kotlin/com/pika/app/MainActivity.kt",
            "androidApp/src/main/kotlin/com/pika/app/PikaApplication.kt",
            "androidApp/src/main/AndroidManifest.xml",
            "androidApp/src/main/res/values/strings.xml",
            "androidApp/src/main/res/values/colors.xml",
            "androidApp/src/main/res/values/themes.xml",
            "androidApp/src/main/res/drawable/ic_launcher.xml",
            "androidApp/build.gradle.kts",
            "androidApp/proguard-rules.pro"
        ]
        
        # iOS app
        ios_files = [
            "iosApp/iosApp/ContentView.swift",
            "iosApp/iosApp/PikaApp.swift",
            "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json",
            "iosApp/iosApp/Info.plist"
        ]
        
        # Desktop app
        desktop_files = [
            "desktopApp/src/jvmMain/kotlin/com/pika/app/main.kt",
            "desktopApp/build.gradle.kts"
        ]
        
        # CI/CD
        cicd_files = [
            ".github/workflows/android-build.yml",
            ".github/workflows/ios-build.yml",
            ".github/workflows/tests.yml"
        ]
        
        # Docker & deployment
        deployment_files = [
            "Dockerfile",
            ".dockerignore"
        ]
        
        # Create all files
        all_files = (root_files + shared_files + android_files + 
                    ios_files + desktop_files + cicd_files + deployment_files)
        
        for file_path in all_files:
            full_path = self.base_path / file_path
            full_path.parent.mkdir(parents=True, exist_ok=True)
            full_path.touch()
        
        print(f"✅ Created {len(all_files)} empty files")
        print(f"📁 Location: {self.base_path.absolute()}")
        print("\n📂 Structure:")
        print("  ├── shared/           # KMP+CMP core")
        print("  │   ├── features/     # Feed, Compose, Thread, etc.")
        print("  │   ├── domain/       # Models, repos, use cases")
        print("  │   ├── data/         # Network, local storage")
        print("  │   ├── ui/           # Components, theme, animations")
        print("  │   ├── gamification/ # XP, badges, streaks")
        print("  │   └── safety/       # Content filter, vibe check")
        print("  ├── androidApp/       # Android target")
        print("  ├── iosApp/           # iOS target")
        print("  └── desktopApp/       # Desktop target")

if __name__ == "__main__":
    generator = PikaKMPGenerator()
    generator.generate()
