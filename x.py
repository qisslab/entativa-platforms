#!/usr/bin/env python3
"""
Pika to Bee Rebranding Script
Renames files, folders, and replaces content throughout the codebase
"""

import os
import re
from pathlib import Path
from typing import Dict, List, Tuple

class BeeRebrander:
    def __init__(self, base_path: str = "./pika-kmp"):
        self.base_path = Path(base_path)
        self.new_base_path = Path("./bee-kmp")
        
        # Mapping of old terms to new terms
        self.term_mappings: Dict[str, str] = {
            # App name
            "Pika": "Bee",
            "pika": "bee",
            "PIKA": "BEE",
            
            # Core concepts
            "Yeet": "Buzz",
            "yeet": "buzz",
            "Yeets": "Buzzes",
            "yeets": "buzzes",
            "yeeting": "buzzing",
            "Yeeted": "Buzzed",
            "yeeted": "buzzed",
            
            # Interactions
            "Reyeet": "Rebuzz",
            "reyeet": "rebuzz",
            "Reyeets": "Rebuzzes",
            "reyeets": "rebuzzes",
            
            # Features
            "Friends Notes": "Hive Notes",
            "FriendsNotes": "HiveNotes",
            "friends_notes": "hive_notes",
            "friendsNotes": "hiveNotes",
            
            "Topic Zones": "Hive Sections",
            "TopicZones": "HiveSections",
            "topic_zones": "hive_sections",
            "topicZones": "hiveSections",
            
            # UI Elements
            "Lightning": "Sting",
            "lightning": "sting",
            
            # Branding
            "Where conversations spark": "Where conversations buzz",
            "⚡": "🐝",
            
            # Package names
            "com.pika.app": "com.bee.app",
            "com/pika/app": "com/bee/app",
        }
        
        # File extensions to process
        self.processable_extensions = {
            '.kt', '.kts', '.java', '.swift', '.xml', '.json', 
            '.yaml', '.yml', '.md', '.txt', '.gradle', '.properties',
            '.conf', '.toml', '.html', '.css', '.js', '.ts'
        }
        
        # Folders/files to skip
        self.skip_patterns = {
            '.git', '.gradle', 'build', '.idea', 'node_modules',
            '__pycache__', '.DS_Store', '*.class', '*.jar'
        }
        
        self.stats = {
            'files_renamed': 0,
            'folders_renamed': 0,
            'files_modified': 0,
            'replacements_made': 0
        }
    
    def should_skip(self, path: Path) -> bool:
        """Check if path should be skipped"""
        for pattern in self.skip_patterns:
            if pattern in str(path):
                return True
        return False
    
    def replace_in_content(self, content: str) -> Tuple[str, int]:
        """Replace all occurrences in content and return count"""
        modified_content = content
        total_replacements = 0
        
        # Sort by length (longest first) to avoid partial replacements
        sorted_mappings = sorted(
            self.term_mappings.items(), 
            key=lambda x: len(x[0]), 
            reverse=True
        )
        
        for old_term, new_term in sorted_mappings:
            # Count occurrences
            count = modified_content.count(old_term)
            if count > 0:
                total_replacements += count
                modified_content = modified_content.replace(old_term, new_term)
        
        return modified_content, total_replacements
    
    def rename_path_component(self, name: str) -> str:
        """Rename a file or folder name"""
        new_name = name
        
        # Apply replacements (case-sensitive)
        for old_term, new_term in self.term_mappings.items():
            if old_term in new_name:
                new_name = new_name.replace(old_term, new_term)
        
        return new_name
    
    def process_file(self, file_path: Path) -> None:
        """Process a single file - rename and modify content"""
        if self.should_skip(file_path):
            return
        
        # Check if file extension should be processed
        if file_path.suffix not in self.processable_extensions:
            # Just rename if needed, don't modify content
            new_name = self.rename_path_component(file_path.name)
            if new_name != file_path.name:
                new_path = file_path.parent / new_name
                print(f"  📄 Renaming file: {file_path.name} → {new_name}")
                file_path.rename(new_path)
                self.stats['files_renamed'] += 1
            return
        
        try:
            # Read file content
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # Replace content
            modified_content, replacements = self.replace_in_content(content)
            
            # Write back if modified
            if replacements > 0:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(modified_content)
                print(f"  ✏️  Modified: {file_path.name} ({replacements} replacements)")
                self.stats['files_modified'] += 1
                self.stats['replacements_made'] += replacements
            
            # Rename file if needed
            new_name = self.rename_path_component(file_path.name)
            if new_name != file_path.name:
                new_path = file_path.parent / new_name
                print(f"  📄 Renaming: {file_path.name} → {new_name}")
                file_path.rename(new_path)
                self.stats['files_renamed'] += 1
                
        except Exception as e:
            print(f"  ⚠️  Error processing {file_path}: {e}")
    
    def process_directory(self, dir_path: Path) -> None:
        """Process all files in directory recursively"""
        if self.should_skip(dir_path):
            return
        
        print(f"\n📁 Processing directory: {dir_path.name}")
        
        # First, process all files
        for item in sorted(dir_path.iterdir()):
            if item.is_file():
                self.process_file(item)
        
        # Then process subdirectories recursively
        for item in sorted(dir_path.iterdir()):
            if item.is_dir():
                self.process_directory(item)
        
        # Finally, rename the directory itself if needed
        new_name = self.rename_path_component(dir_path.name)
        if new_name != dir_path.name and dir_path != self.base_path:
            new_path = dir_path.parent / new_name
            print(f"  📂 Renaming directory: {dir_path.name} → {new_name}")
            dir_path.rename(new_path)
            self.stats['folders_renamed'] += 1
    
    def rename_package_structure(self) -> None:
        """Rename package structure from com/pika/app to com/bee/app"""
        print("\n🔄 Renaming package structure...")
        
        # Find all com/pika/app directories
        for root, dirs, files in os.walk(self.base_path):
            root_path = Path(root)
            
            if 'pika' in dirs and self.should_skip(root_path) is False:
                pika_path = root_path / 'pika'
                bee_path = root_path / 'bee'
                
                if pika_path.exists():
                    print(f"  📦 Moving package: {pika_path} → {bee_path}")
                    pika_path.rename(bee_path)
                    self.stats['folders_renamed'] += 1
    
    def rename_root_directory(self) -> None:
        """Rename the root project directory"""
        if self.base_path.exists():
            print(f"\n📁 Renaming root directory: {self.base_path} → {self.new_base_path}")
            self.base_path.rename(self.new_base_path)
            self.base_path = self.new_base_path
            print(f"  ✅ Root renamed successfully")
    
    def create_rebrand_summary(self) -> None:
        """Create a summary document of the rebranding"""
        summary_path = self.base_path / "REBRAND_SUMMARY.md"
        
        summary_content = f"""# Bee Rebranding Summary

**Date:** {self.__class__.__name__} execution completed

## Overview
Successfully rebranded from **Pika** to **Bee** 🐝

## Statistics
- **Files Renamed:** {self.stats['files_renamed']}
- **Folders Renamed:** {self.stats['folders_renamed']}
- **Files Modified:** {self.stats['files_modified']}
- **Total Replacements:** {self.stats['replacements_made']}

## Key Changes

### App Identity
- **Pika** → **Bee**
- **Package:** com.pika.app → com.bee.app
- **Tagline:** "Where conversations spark" → "Where conversations buzz"

### Terminology Updates
| Old Term | New Term |
|----------|----------|
| Yeet | Buzz |
| Reyeet | Rebuzz |
| Friends Notes | Hive Notes |
| Topic Zones | Hive Sections |
| Lightning | Sting |

### Visual Identity
- **Icon:** ⚡ → 🐝
- **Colors:** Yellow/black theme (bee-inspired)
- **Patterns:** Hexagon/honeycomb designs

## Next Steps

1. **Update Assets:**
   - [ ] App icons (iOS, Android, Desktop)
   - [ ] Splash screens
   - [ ] Marketing materials
   - [ ] Social media branding

2. **Update Documentation:**
   - [ ] API documentation
   - [ ] User guides
   - [ ] Developer documentation
   - [ ] README files

3. **Update External Services:**
   - [ ] Firebase/Analytics projects
   - [ ] App Store listings
   - [ ] Google Play Store
   - [ ] Domain names (bee.social, getbee.app)
   - [ ] Social media handles

4. **Update Backend:**
   - [ ] Database schema (if needed)
   - [ ] API endpoints (if hardcoded)
   - [ ] Environment variables
   - [ ] CI/CD pipelines

5. **Legal/Business:**
   - [ ] Trademark registration for "Bee Social"
   - [ ] Update company documents
   - [ ] Update Terms of Service
   - [ ] Update Privacy Policy

## Verification Checklist

Run these commands to verify the rebrand:

```bash
# Search for any remaining "Pika" references
grep -r "Pika" . --exclude-dir=.git --exclude-dir=build

# Search for "pika" in lowercase
grep -r "pika" . --exclude-dir=.git --exclude-dir=build

# Check package structure
find . -type d -name "pika"

# Verify build
./gradlew clean build
```

## Rollback Plan

If issues arise:
1. Restore from version control (git)
2. Re-run script with inverse mappings
3. Rebuild all artifacts

---

**Generated by BeeRebrander**
"""
        
        with open(summary_path, 'w', encoding='utf-8') as f:
            f.write(summary_content)
        
        print(f"\n📝 Created rebrand summary: {summary_path}")
    
    def rebrand(self) -> None:
        """Execute the full rebranding process"""
        print("🐝 Starting Pika → Bee Rebranding Process...")
        print("=" * 60)
        
        if not self.base_path.exists():
            print(f"❌ Error: Base path '{self.base_path}' does not exist!")
            return
        
        # Step 1: Process all files and directories
        print("\n📋 Phase 1: Processing files and directories...")
        self.process_directory(self.base_path)
        
        # Step 2: Rename package structure
        print("\n📋 Phase 2: Renaming package structure...")
        self.rename_package_structure()
        
        # Step 3: Rename root directory
        print("\n📋 Phase 3: Renaming root directory...")
        self.rename_root_directory()
        
        # Step 4: Create summary
        print("\n📋 Phase 4: Creating rebrand summary...")
        self.create_rebrand_summary()
        
        # Print final statistics
        print("\n" + "=" * 60)
        print("✅ REBRANDING COMPLETE!")
        print("=" * 60)
        print(f"\n📊 Final Statistics:")
        print(f"  • Files Renamed:      {self.stats['files_renamed']}")
        print(f"  • Folders Renamed:    {self.stats['folders_renamed']}")
        print(f"  • Files Modified:     {self.stats['files_modified']}")
        print(f"  • Total Replacements: {self.stats['replacements_made']}")
        print(f"\n🐝 Welcome to Bee Social!")
        print(f"📁 New location: {self.base_path.absolute()}")
        print(f"\n💡 Next steps:")
        print(f"  1. Review REBRAND_SUMMARY.md")
        print(f"  2. Update app icons and assets")
        print(f"  3. Test build: ./gradlew clean build")
        print(f"  4. Update external services (Firebase, stores, etc.)")

class BeeKMPGenerator:
    """Generate fresh Bee KMP structure"""
    def __init__(self, base_path: str = "./bee-kmp"):
        self.base_path = Path(base_path)
    
    def generate(self):
        """Generate complete Bee KMP+CMP structure"""
        print("🐝 Generating Bee KMP+CMP Structure...")
        print("🍯 Where conversations buzz\n")
        
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
            "shared/src/commonMain/kotlin/com/bee/app/App.kt",
            "shared/src/commonMain/kotlin/com/bee/app/di/AppModule.kt",
            
            # Core features - Feed
            "shared/src/commonMain/kotlin/com/bee/app/features/feed/FeedScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/feed/FeedViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/feed/components/BuzzCard.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/feed/components/HiveNotes.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/feed/components/TrendingWords.kt",
            
            # Compose (Create Buzz)
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/ComposeScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/ComposeViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/components/BuzzEditor.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/components/FontPicker.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/components/ColorGradient.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/compose/components/EmojiPicker.kt",
            
            # Thread view
            "shared/src/commonMain/kotlin/com/bee/app/features/thread/ThreadScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/thread/ThreadViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/thread/components/ThreadTree.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/thread/components/ReplyCard.kt",
            
            # Explore
            "shared/src/commonMain/kotlin/com/bee/app/features/explore/ExploreScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/explore/ExploreViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/explore/components/TrendingSection.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/explore/components/HiveSections.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/explore/components/ThreadOfTheDay.kt",
            
            # Profile
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/ProfileScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/ProfileViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/components/ProfileHeader.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/components/StatsCard.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/components/BuzzesList.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/profile/components/BadgeGrid.kt",
            
            # Settings & Customization
            "shared/src/commonMain/kotlin/com/bee/app/features/settings/SettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/settings/SettingsViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/settings/components/PalettePicker.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/settings/components/SafetyControls.kt",
            
            # Communities (Hive Sections)
            "shared/src/commonMain/kotlin/com/bee/app/features/communities/CommunitiesScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/communities/CommunityViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/communities/components/CommunityCard.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/communities/components/CommunityFeed.kt",
            
            # Notifications
            "shared/src/commonMain/kotlin/com/bee/app/features/notifications/NotificationsScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/notifications/NotificationsViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/notifications/components/NotificationCard.kt",
            
            # Messages/DMs
            "shared/src/commonMain/kotlin/com/bee/app/features/messages/MessagesScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/messages/MessagesViewModel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/messages/ChatScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/messages/ChatViewModel.kt",
            
            # Onboarding
            "shared/src/commonMain/kotlin/com/bee/app/features/onboarding/OnboardingScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/onboarding/PaletteSelectionScreen.kt",
            "shared/src/commonMain/kotlin/com/bee/app/features/onboarding/InterestsScreen.kt",
            
            # Domain models
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/Buzz.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/User.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/Community.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/Notification.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/Palette.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/Badge.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/UserLevel.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/model/ReplyStreak.kt",
            
            # Repositories
            "shared/src/commonMain/kotlin/com/bee/app/domain/repository/BuzzRepository.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/repository/UserRepository.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/repository/CommunityRepository.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/repository/NotificationRepository.kt",
            
            # Use cases
            "shared/src/commonMain/kotlin/com/bee/app/domain/usecase/CreateBuzzUseCase.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/usecase/RebuzzUseCase.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/usecase/GetFeedUseCase.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/usecase/GetThreadUseCase.kt",
            "shared/src/commonMain/kotlin/com/bee/app/domain/usecase/VibeCheckUseCase.kt",
            
            # Data layer
            "shared/src/commonMain/kotlin/com/bee/app/data/remote/BeeApi.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/remote/NetworkClient.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/remote/dto/BuzzDto.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/remote/dto/UserDto.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/repository/BuzzRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/repository/UserRepositoryImpl.kt",
            
            # Local storage
            "shared/src/commonMain/kotlin/com/bee/app/data/local/BeeDatabase.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/local/dao/BuzzDao.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/local/dao/UserDao.kt",
            "shared/src/commonMain/kotlin/com/bee/app/data/local/entity/BuzzEntity.kt",
            
            # UI components
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/BeeButton.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/BeeTextField.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/BeeTopBar.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/BeeBottomBar.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/StingButton.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/ReactionPicker.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/UserAvatar.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/BadgeIcon.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/components/StreakIndicator.kt",
            
            # Theme system
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/BeeTheme.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/ColorPalette.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/HoneycombTheme.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/Palettes.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/Typography.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/theme/Shapes.kt",
            
            # Animations
            "shared/src/commonMain/kotlin/com/bee/app/ui/animations/StingAnimation.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/animations/BuzzAnimation.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/animations/HexagonAnimation.kt",
            "shared/src/commonMain/kotlin/com/bee/app/ui/animations/MascotAnimation.kt",
            
            # Utils
            "shared/src/commonMain/kotlin/com/bee/app/utils/DateUtils.kt",
            "shared/src/commonMain/kotlin/com/bee/app/utils/StringUtils.kt",
            "shared/src/commonMain/kotlin/com/bee/app/utils/ValidationUtils.kt",
            "shared/src/commonMain/kotlin/com/bee/app/utils/Logger.kt",
            "shared/src/commonMain/kotlin/com/bee/app/utils/HapticFeedback.kt",
            
            # Gamification
            "shared/src/commonMain/kotlin/com/bee/app/gamification/XPManager.kt",
            "shared/src/commonMain/kotlin/com/bee/app/gamification/AchievementEngine.kt",
            "shared/src/commonMain/kotlin/com/bee/app/gamification/BadgeUnlocker.kt",
            "shared/src/commonMain/kotlin/com/bee/app/gamification/StreakTracker.kt",
            
            # Safety
            "shared/src/commonMain/kotlin/com/bee/app/safety/ContentFilter.kt",
            "shared/src/commonMain/kotlin/com/bee/app/safety/AgeScopeManager.kt",
            "shared/src/commonMain/kotlin/com/bee/app/safety/VibeCheckAI.kt",
            "shared/src/commonMain/kotlin/com/bee/app/safety/ModerationTools.kt",
            
            # Platform-specific
            "shared/src/androidMain/kotlin/com/bee/app/platform/HapticFeedback.android.kt",
            "shared/src/androidMain/kotlin/com/bee/app/platform/ShareSheet.android.kt",
            "shared/src/iosMain/kotlin/com/bee/app/platform/HapticFeedback.ios.kt",
            "shared/src/iosMain/kotlin/com/bee/app/platform/ShareSheet.ios.kt",
            
            # Resources
            "shared/src/commonMain/resources/drawable/logo.xml",
            "shared/src/commonMain/resources/drawable/mascot.xml",
            "shared/src/commonMain/resources/drawable/bee_icon.xml",
            "shared/src/commonMain/resources/drawable/hexagon.xml",
            
            # Tests
            "shared/src/commonTest/kotlin/com/bee/app/BuzzViewModelTest.kt",
            "shared/src/commonTest/kotlin/com/bee/app/VibeCheckTest.kt",
            "shared/src/commonTest/kotlin/com/bee/app/XPManagerTest.kt",
            
            # Build
            "shared/build.gradle.kts"
        ]
        
        # Android app
        android_files = [
            "androidApp/src/main/kotlin/com/bee/app/MainActivity.kt",
            "androidApp/src/main/kotlin/com/bee/app/BeeApplication.kt",
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
            "iosApp/iosApp/BeeApp.swift",
            "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json",
            "iosApp/iosApp/Info.plist"
        ]
        
        # Desktop app
        desktop_files = [
            "desktopApp/src/jvmMain/kotlin/com/bee/app/main.kt",
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
        print
