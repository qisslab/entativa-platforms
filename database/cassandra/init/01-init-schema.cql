-- Entativa Cassandra Schema
-- Time-series data for feeds, analytics, and real-time features

-- Create keyspace for Entativa
CREATE KEYSPACE IF NOT EXISTS entativa 
WITH REPLICATION = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

USE entativa;

-- ===== FEED TABLES =====

-- User Feed Timeline (Time-series)
CREATE TABLE user_feed_timeline (
    user_id UUID,
    timestamp TIMESTAMP,
    content_id UUID,
    platform TEXT,
    content_type TEXT,
    author_id UUID,
    score DOUBLE,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (user_id, timestamp, content_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, content_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- Global Feed by Platform
CREATE TABLE platform_feed (
    platform TEXT,
    timestamp TIMESTAMP,
    content_id UUID,
    author_id UUID,
    engagement_score DOUBLE,
    trending_score DOUBLE,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (platform, timestamp, content_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, content_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- ===== ANALYTICS TABLES =====

-- User Activity Metrics (Time-series)
CREATE TABLE user_activity_metrics (
    user_id UUID,
    platform TEXT,
    date_bucket TEXT, -- YYYY-MM-DD
    hour_bucket INT,
    activity_type TEXT,
    count COUNTER,
    PRIMARY KEY ((user_id, platform, date_bucket), hour_bucket, activity_type)
) WITH gc_grace_seconds = 86400;

-- Content Performance Metrics
CREATE TABLE content_metrics (
    content_id UUID,
    platform TEXT,
    timestamp TIMESTAMP,
    metric_type TEXT,
    value BIGINT,
    dimensions MAP<TEXT, TEXT>,
    PRIMARY KEY (content_id, timestamp, metric_type)
) WITH CLUSTERING ORDER BY (timestamp DESC, metric_type ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- Real-time Engagement Events
CREATE TABLE engagement_events (
    content_id UUID,
    timestamp TIMESTAMP,
    user_id UUID,
    event_type TEXT,
    platform TEXT,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (content_id, timestamp, user_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, user_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- ===== SOCIAL GRAPH TABLES =====

-- User Connections (Optimized for social graph queries)
CREATE TABLE user_connections (
    user_id UUID,
    connection_type TEXT, -- 'following', 'friends', 'subscribers'
    connected_user_id UUID,
    platform TEXT,
    connection_strength DOUBLE,
    created_at TIMESTAMP,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (user_id, connection_type, connected_user_id)
) WITH gc_grace_seconds = 86400;

-- Connection Activity (For recommendation algorithms)
CREATE TABLE connection_activity (
    user_id UUID,
    connected_user_id UUID,
    platform TEXT,
    date_bucket TEXT,
    interaction_count COUNTER,
    last_interaction TIMESTAMP,
    PRIMARY KEY ((user_id, connected_user_id, platform), date_bucket)
) WITH gc_grace_seconds = 86400;

-- ===== TRENDING TABLES =====

-- Trending Content by Platform
CREATE TABLE trending_content (
    platform TEXT,
    time_window TEXT, -- 'hourly', 'daily', 'weekly'
    timestamp TIMESTAMP,
    content_id UUID,
    trending_score DOUBLE,
    engagement_velocity DOUBLE,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (platform, time_window, timestamp, content_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, content_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- Trending Topics/Hashtags
CREATE TABLE trending_topics (
    platform TEXT,
    time_window TEXT,
    timestamp TIMESTAMP,
    topic TEXT,
    mention_count COUNTER,
    unique_users_count COUNTER,
    engagement_score DOUBLE,
    PRIMARY KEY (platform, time_window, timestamp, topic)
) WITH CLUSTERING ORDER BY (timestamp DESC, topic ASC)
  AND gc_grace_seconds = 86400;

-- ===== RECOMMENDATION TABLES =====

-- User Recommendation Scores
CREATE TABLE user_recommendations (
    user_id UUID,
    platform TEXT,
    recommendation_type TEXT, -- 'content', 'users', 'topics'
    recommended_id UUID,
    score DOUBLE,
    reason_codes SET<TEXT>,
    generated_at TIMESTAMP,
    expires_at TIMESTAMP,
    PRIMARY KEY (user_id, platform, recommendation_type, recommended_id)
) WITH gc_grace_seconds = 86400;

-- Content Similarity Matrix
CREATE TABLE content_similarity (
    content_id UUID,
    platform TEXT,
    similar_content_id UUID,
    similarity_score DOUBLE,
    similarity_type TEXT, -- 'visual', 'textual', 'behavioral'
    computed_at TIMESTAMP,
    PRIMARY KEY (content_id, platform, similar_content_id)
) WITH gc_grace_seconds = 604800; -- 1 week

-- ===== NOTIFICATION TABLES =====

-- User Notifications (Time-series)
CREATE TABLE user_notifications (
    user_id UUID,
    timestamp TIMESTAMP,
    notification_id UUID,
    platform TEXT,
    notification_type TEXT,
    title TEXT,
    body TEXT,
    data MAP<TEXT, TEXT>,
    is_read BOOLEAN,
    PRIMARY KEY (user_id, timestamp, notification_id)
) WITH CLUSTERING ORDER BY (timestamp DESC, notification_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- ===== SESSION TABLES =====

-- User Sessions (For analytics and recommendations)
CREATE TABLE user_sessions (
    user_id UUID,
    session_id UUID,
    platform TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    device_info MAP<TEXT, TEXT>,
    activities LIST<FROZEN<MAP<TEXT, TEXT>>>,
    PRIMARY KEY (user_id, start_time, session_id)
) WITH CLUSTERING ORDER BY (start_time DESC, session_id ASC)
  AND gc_grace_seconds = 86400
  AND compaction = {'class': 'TimeWindowCompactionStrategy'};

-- ===== INDEXES =====

-- Secondary indexes for common query patterns
CREATE INDEX ON user_activity_metrics (platform);
CREATE INDEX ON content_metrics (platform);
CREATE INDEX ON engagement_events (platform);
CREATE INDEX ON trending_content (content_id);
CREATE INDEX ON user_recommendations (recommended_id);

-- ===== MATERIALIZED VIEWS =====

-- Daily user activity summary
CREATE MATERIALIZED VIEW daily_user_activity AS
SELECT user_id, platform, date_bucket, activity_type, count
FROM user_activity_metrics
WHERE user_id IS NOT NULL 
  AND platform IS NOT NULL 
  AND date_bucket IS NOT NULL 
  AND activity_type IS NOT NULL
PRIMARY KEY (date_bucket, platform, user_id, activity_type);

-- Top trending content by platform
CREATE MATERIALIZED VIEW top_trending_content AS
SELECT platform, time_window, content_id, trending_score, timestamp
FROM trending_content
WHERE platform IS NOT NULL 
  AND time_window IS NOT NULL 
  AND content_id IS NOT NULL 
  AND trending_score IS NOT NULL
PRIMARY KEY (platform, time_window, trending_score, content_id)
WITH CLUSTERING ORDER BY (trending_score DESC);

-- User connection counts
CREATE MATERIALIZED VIEW user_connection_counts AS
SELECT user_id, connection_type, platform, COUNT(*) as connection_count
FROM user_connections
WHERE user_id IS NOT NULL 
  AND connection_type IS NOT NULL 
  AND platform IS NOT NULL
PRIMARY KEY (platform, connection_type, user_id);

-- Comments for documentation
-- This schema is optimized for:
-- 1. Time-series queries (feeds, analytics, events)
-- 2. Social graph traversal (connections, recommendations)
-- 3. Real-time data ingestion with minimal read latency
-- 4. Horizontal scaling across multiple nodes
-- 5. Efficient tombstone handling with appropriate gc_grace_seconds