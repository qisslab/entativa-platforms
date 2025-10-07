#!/usr/bin/env python3
"""
Sonet KMP+CMP Structure Generator
Facebook competitor - Your social network, your way
"""

import os
from pathlib import Path

class SonetKMPGenerator:
    def __init__(self, base_path: str = "./sonet-kmp"):
        self.base_path = Path(base_path)
        
    def generate(self):
        """Generate complete Sonet KMP+CMP structure"""
        print("🌐 Generating Sonet KMP+CMP Structure...")
        print("🤝 Your social network. Your way.\n")
        
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
            "shared/src/commonMain/kotlin/com/sonet/app/App.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/di/AppModule.kt",
            
            # Core features - Feed
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/FeedScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/FeedViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/components/PostCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/components/StoryBar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/components/CreatePostBox.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/feed/components/FeedFilter.kt",
            
            # Post creation
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/ComposeScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/ComposeViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/PostTypeSelector.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/TextEditor.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/MediaPicker.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/LocationPicker.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/TagPeople.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/PrivacySelector.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/compose/components/AlbumCreator.kt",
            
            # Post detail
            "shared/src/commonMain/kotlin/com/sonet/app/features/post/PostDetailScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/post/PostViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/post/components/CommentSection.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/post/components/ReactionBar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/post/components/ShareSheet.kt",
            
            # Profile
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/ProfileScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/ProfileViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/EditProfileScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/ProfileHeader.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/CoverPhoto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/AboutSection.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/FriendsGrid.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/PhotosGrid.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/profile/components/PostsList.kt",
            
            # Friends
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/FriendsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/FriendsViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/SuggestionsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/RequestsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/components/FriendCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/friends/components/RequestCard.kt",
            
            # Groups
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/GroupsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/GroupsViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/GroupDetailScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/CreateGroupScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/components/GroupCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/components/GroupFeed.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/components/MembersList.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/groups/components/GroupSettings.kt",
            
            # Pages
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/PagesScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/PagesViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/PageDetailScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/CreatePageScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/components/PageCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/components/PageHeader.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/pages/components/PageInsights.kt",
            
            # Global Pages
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/GlobalPageScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/GlobalPageViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/RegionalPageScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/components/RegionSelector.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/components/ContentDistribution.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/globalpages/components/RegionalSettings.kt",
            
            # Events
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/EventsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/EventsViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/EventDetailScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/CreateEventScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/components/EventCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/components/EventCalendar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/events/components/AttendeesList.kt",
            
            # Marketplace
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/MarketplaceScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/MarketplaceViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/ListingScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/CreateListingScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/components/ListingCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/components/CategoryFilter.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/marketplace/components/PriceRange.kt",
            
            # Watch (Videos)
            "shared/src/commonMain/kotlin/com/sonet/app/features/watch/WatchScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/watch/WatchViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/watch/components/VideoPlayer.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/watch/components/VideoCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/watch/components/VideoFeed.kt",
            
            # Stories
            "shared/src/commonMain/kotlin/com/sonet/app/features/stories/StoriesScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/stories/StoriesViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/stories/CreateStoryScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/stories/components/StoryViewer.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/stories/components/StoryCreator.kt",
            
            # Memories
            "shared/src/commonMain/kotlin/com/sonet/app/features/memories/MemoriesScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/memories/MemoriesViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/memories/components/MemoryCard.kt",
            
            # Messages
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/MessagesScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/MessagesViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/ChatScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/ChatViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/components/MessageBubble.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/components/ConversationCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/components/VoiceMessage.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/messages/components/VideoCall.kt",
            
            # Notifications
            "shared/src/commonMain/kotlin/com/sonet/app/features/notifications/NotificationsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/notifications/NotificationsViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/notifications/components/NotificationCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/notifications/components/NotificationFilter.kt",
            
            # Search
            "shared/src/commonMain/kotlin/com/sonet/app/features/search/SearchScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/search/SearchViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/search/components/SearchBar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/search/components/SearchFilters.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/search/components/SearchResults.kt",
            
            # Settings
            "shared/src/commonMain/kotlin/com/sonet/app/features/settings/SettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/settings/SettingsViewModel.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/settings/PrivacySettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/settings/SecuritySettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/features/settings/components/SettingsSection.kt",
            
            # Domain models
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Post.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/User.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Friend.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Group.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Page.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Event.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Story.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Comment.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Reaction.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Message.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/Notification.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/MarketplaceListing.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/GlobalPage.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/model/RegionalPage.kt",
            
            # Repositories
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/PostRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/UserRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/FriendRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/GroupRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/PageRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/EventRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/MessageRepository.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/repository/MarketplaceRepository.kt",
            
            # Use cases
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/CreatePostUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/GetFeedUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/SendFriendRequestUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/CreateGroupUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/CreatePageUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/CreateEventUseCase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/domain/usecase/SendMessageUseCase.kt",
            
            # Data layer
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/SonetApi.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/NetworkClient.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/dto/PostDto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/dto/UserDto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/dto/GroupDto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/dto/PageDto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/remote/dto/EventDto.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/repository/PostRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/repository/UserRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/repository/GroupRepositoryImpl.kt",
            
            # Local storage
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/SonetDatabase.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/dao/PostDao.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/dao/UserDao.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/dao/MessageDao.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/entity/PostEntity.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/data/local/entity/UserEntity.kt",
            
            # UI components
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/SonetButton.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/SonetTextField.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/SonetTopBar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/SonetBottomBar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/SonetCard.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/UserAvatar.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/CoverImage.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/ReactionButton.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/ShareButton.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/CommentButton.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/MediaViewer.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/AlbumViewer.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/components/VideoPlayer.kt",
            
            # Theme system
            "shared/src/commonMain/kotlin/com/sonet/app/ui/theme/SonetTheme.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/theme/Color.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/theme/Typography.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/theme/Shapes.kt",
            
            # Animations
            "shared/src/commonMain/kotlin/com/sonet/app/ui/animations/ReactionAnimation.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/animations/LikeAnimation.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/animations/ShareAnimation.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/ui/animations/NotificationAnimation.kt",
            
            # Utils
            "shared/src/commonMain/kotlin/com/sonet/app/utils/DateUtils.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/utils/StringUtils.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/utils/ValidationUtils.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/utils/Logger.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/utils/MediaUtils.kt",
            "shared/src/commonMain/kotlin/com/sonet/app/utils/PermissionUtils.kt",
            
            # Platform-specific
            "shared/src/androidMain/kotlin/com/sonet/app/platform/Camera.android.kt",
            "shared/src/androidMain/kotlin/com/sonet/app/platform/MediaPicker.android.kt",
            "shared/src/androidMain/kotlin/com/sonet/app/platform/LocationProvider.android.kt",
            "shared/src/androidMain/kotlin/com/sonet/app/platform/ShareSheet.android.kt",
            "shared/src/androidMain/kotlin/com/sonet/app/platform/Notifications.android.kt",
            "shared/src/iosMain/kotlin/com/sonet/app/platform/Camera.ios.kt",
            "shared/src/iosMain/kotlin/com/sonet/app/platform/MediaPicker.ios.kt",
            "shared/src/iosMain/kotlin/com/sonet/app/platform/LocationProvider.ios.kt",
            "shared/src/iosMain/kotlin/com/sonet/app/platform/ShareSheet.ios.kt",
            "shared/src/iosMain/kotlin/com/sonet/app/platform/Notifications.ios.kt",
            
            # Resources
            "shared/src/commonMain/resources/drawable/logo.xml",
            "shared/src/commonMain/resources/drawable/reaction_like.xml",
            "shared/src/commonMain/resources/drawable/reaction_love.xml",
            "shared/src/commonMain/resources/drawable/reaction_haha.xml",
            "shared/src/commonMain/resources/drawable/reaction_wow.xml",
            "shared/src/commonMain/resources/drawable/reaction_sad.xml",
            "shared/src/commonMain/resources/drawable/reaction_angry.xml",
            
            # Tests
            "shared/src/commonTest/kotlin/com/sonet/app/FeedViewModelTest.kt",
            "shared/src/commonTest/kotlin/com/sonet/app/PostRepositoryTest.kt",
            "shared/src/commonTest/kotlin/com/sonet/app/GroupServiceTest.kt",
            "shared/src/commonTest/kotlin/com/sonet/app/GlobalPageTest.kt",
            
            # Build
            "shared/build.gradle.kts"
        ]
        
        # Android app
        android_files = [
            "androidApp/src/main/kotlin/com/sonet/app/MainActivity.kt",
            "androidApp/src/main/kotlin/com/sonet/app/SonetApplication.kt",
            "androidApp/src/main/AndroidManifest.xml",
            "androidApp/src/main/res/values/strings.xml",
            "androidApp/src/main/res/values/colors.xml",
            "androidApp/src/main/res/values/themes.xml",
            "androidApp/src/main/res/drawable/ic_launcher.xml",
            "androidApp/src/main/res/xml/network_security_config.xml",
            "androidApp/build.gradle.kts",
            "androidApp/proguard-rules.pro"
        ]
        
        # iOS app
        ios_files = [
            "iosApp/iosApp/ContentView.swift",
            "iosApp/iosApp/SonetApp.swift",
            "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json",
            "iosApp/iosApp/Info.plist"
        ]
        
        # Desktop app
        desktop_files = [
            "desktopApp/src/jvmMain/kotlin/com/sonet/app/main.kt",
            "desktopApp/build.gradle.kts"
        ]
        
        # CI/CD
        cicd_files = [
            ".github/workflows/android-build.yml",
            ".github/workflows/ios-build.yml",
            ".github/workflows/tests.yml",
            ".github/workflows/deploy.yml"
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
        print("  │   ├── features/     # Feed, Groups, Pages, Events, etc.")
        print("  │   ├── domain/       # Models, repos, use cases")
        print("  │   ├── data/         # Network, local storage")
        print("  │   └── ui/           # Components, theme, animations")
        print("  ├── androidApp/       # Android target")
        print("  ├── iosApp/           # iOS target")
        print("  └── desktopApp/       # Desktop target")
        print("\n🌐 Special Features:")
        print("  👥 Friends & Friend requests")
        print("  👪 Groups & Communities")
        print("  📄 Pages (basic & global)")
        print("  📅 Events management")
        print("  🛒 Marketplace")
        print("  📺 Watch (video feed)")
        print("  💬 Messaging & Video calls")
        print("  📖 Stories & Memories")

if __name__ == "__main__":
    generator = SonetKMPGenerator()
    generator.generate()
