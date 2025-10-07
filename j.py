#!/usr/bin/env python3
"""
PlayPods KMP+CMP Structure Generator
Entativa's answer to YouTube & TikTok with Pixels (short-form content)
"""

import os
from pathlib import Path

class PlayPodsGenerator:
    def __init__(self, base_path: str = "./playpods-kmp"):
        self.base_path = Path(base_path)
        
    def generate(self):
        """Generate complete PlayPods KMP+CMP structure"""
        print("▶️ Generating PlayPods KMP+CMP Structure...")
        print("🎬 Where creativity meets community\n")
        
        # Root files
        root_files = [
            "README.md",
            ".gitignore",
            "settings.gradle.kts",
            "gradle.properties",
            "build.gradle.kts"
        ]
        
        # ============================================================
        # SHARED MODULE (KMP+CMP)
        # ============================================================
        shared_files = [
            # Main app
            "shared/src/commonMain/kotlin/com/entativa/playpods/app/App.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/app/di/AppModule.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/app/navigation/Navigation.kt",
            
            # ============================================================
            # HOME FEED - Long-form videos (YouTube-style)
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/HomeScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/HomeViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/components/VideoCard.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/components/LiveBadge.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/components/CategoryTabs.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/components/StoryCircles.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/home/components/ChannelSubscriptions.kt",
            
            # Video Player
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/VideoPlayerScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/VideoPlayerViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/VideoPlayer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/PlayerControls.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/QualitySelector.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/PlaybackSpeedControl.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/CaptionsToggle.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/PictureInPicture.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/VideoInfo.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/EngagementButtons.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/CommentsSection.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/RelatedVideos.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/player/components/ChaptersTimeline.kt",
            
            # ============================================================
            # PIXELS - Short-form videos (TikTok-style)
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/PixelsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/PixelsViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/PixelPlayer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/PixelFeed.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/SwipeGestures.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/SideActions.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/PixelInfo.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/SoundAttribution.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/PixelEngagement.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/DuetButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/pixels/components/StitchButton.kt",
            
            # ============================================================
            # EXPLORE - Discovery & Trending
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/ExploreScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/ExploreViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/SearchBar.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/FilterChips.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/TrendingSection.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/CategoryGrid.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/HashtagExplorer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/explore/components/SortOptions.kt",
            
            # Search
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/SearchScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/SearchViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/components/SearchResults.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/components/RecentSearches.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/components/VoiceSearch.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/search/components/SearchFilters.kt",
            
            # ============================================================
            # CHANNELS - Creator profiles
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/ChannelsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/ChannelViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/ChannelHeader.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/SubscribeButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/ChannelTabs.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/VideosGrid.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/PixelsGrid.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/PlaylistsGrid.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/AboutSection.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/ChannelStats.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/channels/components/MembershipTiers.kt",
            
            # ============================================================
            # LIBRARY - User's saved content
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/LibraryScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/LibraryViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/HistoryList.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/WatchLaterList.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/LikedVideos.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/Playlists.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/Downloads.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/library/components/Subscriptions.kt",
            
            # ============================================================
            # UPLOAD/CREATOR STUDIO
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/UploadScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/UploadViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/VideoUploader.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/ThumbnailEditor.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/TitleDescriptionEditor.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/TagSelector.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/CategorySelector.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/PrivacySettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/SchedulePublish.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/upload/components/UploadProgress.kt",
            
            # Pixel Creator
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/PixelCreatorScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/PixelCreatorViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/VideoRecorder.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/FilterSelector.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/EffectsPanel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/SoundLibrary.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/TextOverlay.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/TransitionEditor.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/SpeedControl.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/TrimEditor.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/create/components/DraftsSaver.kt",
            
            # ============================================================
            # LIVE STREAMING
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/LiveStreamScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/LiveStreamViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/LivePlayer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/LiveChat.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/ViewerCount.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/SuperChat.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/LiveEmojis.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/live/components/StreamControls.kt",
            
            # ============================================================
            # NOTIFICATIONS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/notifications/NotificationsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/notifications/NotificationsViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/notifications/components/NotificationCard.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/notifications/components/NotificationFilters.kt",
            
            # ============================================================
            # SETTINGS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/SettingsScreen.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/SettingsViewModel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/AccountSettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/PlaybackSettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/DataSavingSettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/NotificationSettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/PrivacySettings.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/features/settings/components/ContentPreferences.kt",
            
            # ============================================================
            # DOMAIN MODELS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Video.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Pixel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Channel.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Creator.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Comment.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Playlist.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Category.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Hashtag.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/LiveStream.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Notification.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Subscription.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/WatchHistory.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Engagement.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Trend.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Sound.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Effect.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/model/Chapter.kt",
            
            # ============================================================
            # REPOSITORIES
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/VideoRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/PixelRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/ChannelRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/CommentRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/PlaylistRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/SearchRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/SubscriptionRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/NotificationRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/HistoryRepository.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/repository/LiveStreamRepository.kt",
            
            # ============================================================
            # USE CASES
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/GetHomeFeedUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/GetPixelsFeedUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/PlayVideoUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/LikeVideoUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/SubscribeToChannelUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/UploadVideoUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/CreatePixelUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/SearchContentUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/GetTrendingUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/StartLiveStreamUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/TrackWatchHistoryUseCase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/domain/usecase/GenerateRecommendationsUseCase.kt",
            
            # ============================================================
            # DATA LAYER
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/PlayPodsApi.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/NetworkClient.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/dto/VideoDto.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/dto/PixelDto.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/dto/ChannelDto.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/remote/dto/CommentDto.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/repository/VideoRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/repository/PixelRepositoryImpl.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/repository/ChannelRepositoryImpl.kt",
            
            # Local storage
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/PlayPodsDatabase.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/dao/VideoDao.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/dao/PixelDao.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/dao/HistoryDao.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/dao/DownloadDao.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/entity/VideoEntity.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/data/local/entity/PixelEntity.kt",
            
            # ============================================================
            # VIDEO PLAYER ENGINE
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/PlayPodsPlayer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/PlayerState.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/PlaybackController.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/QualityManager.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/BufferManager.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/CaptionsRenderer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/player/AdapterBitrate.kt",
            
            # ============================================================
            # UI COMPONENTS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/VideoThumbnail.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/ChannelAvatar.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/EngagementBar.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/LikeButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/ShareButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/SaveButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/CommentButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/PlayButton.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/ProgressBar.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/ViewCount.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/DurationBadge.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/VerifiedBadge.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/CategoryChip.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/LoadingShimmer.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/EmptyState.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/components/PullToRefresh.kt",
            
            # Bottom sheets
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/ShareBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/PlaylistBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/QualityBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/SpeedBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/ReportBottomSheet.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/bottomsheets/SortBottomSheet.kt",
            
            # ============================================================
            # THEME SYSTEM
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/PlayPodsTheme.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/Colors.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/Typography.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/Shapes.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/Dimensions.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/DarkTheme.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/theme/LightTheme.kt",
            
            # ============================================================
            # ANIMATIONS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/LikeAnimation.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/SubscribeAnimation.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/SwipeAnimation.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/ShimmerEffect.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/PlayerTransition.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/ui/animations/PiPAnimation.kt",
            
            # ============================================================
            # UTILS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/DateUtils.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/DurationFormatter.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/NumberFormatter.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/ValidationUtils.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/Logger.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/ShareUtils.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/DownloadManager.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/CacheManager.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/utils/BitrateCalculator.kt",
            
            # ============================================================
            # ALGORITHMS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/algorithms/RecommendationEngine.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/algorithms/TrendingAlgorithm.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/algorithms/SearchRanking.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/algorithms/FeedPersonalization.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/algorithms/ContentModeration.kt",
            
            # ============================================================
            # ANALYTICS
            # ============================================================
            "shared/src/commonMain/kotlin/com/entativa/playpods/analytics/AnalyticsTracker.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/analytics/WatchTimeTracker.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/analytics/EngagementTracker.kt",
            "shared/src/commonMain/kotlin/com/entativa/playpods/analytics/EventLogger.kt",
            
            # ============================================================
            # PLATFORM SPECIFIC
            # ============================================================
            "shared/src/androidMain/kotlin/com/entativa/playpods/platform/VideoPlayer.android.kt",
            "shared/src/androidMain/kotlin/com/entativa/playpods/platform/CameraCapture.android.kt",
            "shared/src/androidMain/kotlin/com/entativa/playpods/platform/PictureInPicture.android.kt",
            "shared/src/androidMain
