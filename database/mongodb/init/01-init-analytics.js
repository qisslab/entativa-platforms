// Entativa MongoDB Initialization
// Analytics and media metadata collections

// Switch to analytics database
use entativa_analytics;

// Create collections for different analytics types

// User Analytics Collection
db.createCollection("user_analytics", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["userId", "platform", "timestamp", "eventType"],
         properties: {
            userId: {
               bsonType: "string",
               description: "User ID across all platforms"
            },
            platform: {
               enum: ["sonet", "gala", "pika", "playpods"],
               description: "Platform where event occurred"
            },
            timestamp: {
               bsonType: "date",
               description: "Event timestamp"
            },
            eventType: {
               bsonType: "string",
               description: "Type of user event"
            },
            metadata: {
               bsonType: "object",
               description: "Event-specific metadata"
            }
         }
      }
   }
});

// Content Analytics Collection
db.createCollection("content_analytics", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["contentId", "platform", "timestamp", "metricType"],
         properties: {
            contentId: {
               bsonType: "string",
               description: "Content ID"
            },
            platform: {
               enum: ["sonet", "gala", "pika", "playpods"],
               description: "Platform where content exists"
            },
            timestamp: {
               bsonType: "date",
               description: "Metric timestamp"
            },
            metricType: {
               bsonType: "string",
               description: "Type of content metric"
            },
            value: {
               bsonType: "number",
               description: "Metric value"
            },
            dimensions: {
               bsonType: "object",
               description: "Metric dimensions (country, device, etc.)"
            }
         }
      }
   }
});

// Media Metadata Collection
db.createCollection("media_metadata", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["mediaId", "platform", "mediaType", "uploadedAt"],
         properties: {
            mediaId: {
               bsonType: "string",
               description: "Unique media identifier"
            },
            platform: {
               enum: ["sonet", "gala", "pika", "playpods"],
               description: "Platform where media is stored"
            },
            mediaType: {
               enum: ["image", "video", "audio", "pixel"],
               description: "Type of media"
            },
            uploadedAt: {
               bsonType: "date",
               description: "Upload timestamp"
            },
            fileSize: {
               bsonType: "long",
               description: "File size in bytes"
            },
            dimensions: {
               bsonType: "object",
               description: "Width, height, duration, etc."
            },
            processingStatus: {
               enum: ["pending", "processing", "completed", "failed"],
               description: "Processing status"
            },
            urls: {
               bsonType: "object",
               description: "URLs for different resolutions/formats"
            },
            aiAnalysis: {
               bsonType: "object",
               description: "AI-generated content analysis"
            }
         }
      }
   }
});

// Feed Analytics Collection
db.createCollection("feed_analytics", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["userId", "platform", "timestamp", "feedType"],
         properties: {
            userId: {
               bsonType: "string",
               description: "User viewing the feed"
            },
            platform: {
               enum: ["sonet", "gala", "pika", "playpods"],
               description: "Platform feed"
            },
            timestamp: {
               bsonType: "date",
               description: "Feed view timestamp"
            },
            feedType: {
               bsonType: "string",
               description: "Type of feed (home, explore, trending, etc.)"
            },
            contentShown: {
               bsonType: "array",
               description: "Array of content IDs shown"
            },
            interactions: {
               bsonType: "array",
               description: "User interactions with feed items"
            },
            sessionDuration: {
               bsonType: "number",
               description: "Time spent viewing feed"
            }
         }
      }
   }
});

// Real-time Events Collection (TTL enabled)
db.createCollection("realtime_events", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["eventId", "platform", "timestamp", "eventType"],
         properties: {
            eventId: {
               bsonType: "string",
               description: "Unique event identifier"
            },
            platform: {
               enum: ["sonet", "gala", "pika", "playpods"],
               description: "Platform where event occurred"
            },
            timestamp: {
               bsonType: "date",
               description: "Event timestamp"
            },
            eventType: {
               bsonType: "string",
               description: "Type of real-time event"
            },
            data: {
               bsonType: "object",
               description: "Event data payload"
            },
            expiresAt: {
               bsonType: "date",
               description: "When this event expires"
            }
         }
      }
   }
});

// Create indexes for performance

// User Analytics Indexes
db.user_analytics.createIndex({ "userId": 1, "timestamp": -1 });
db.user_analytics.createIndex({ "platform": 1, "timestamp": -1 });
db.user_analytics.createIndex({ "eventType": 1, "timestamp": -1 });
db.user_analytics.createIndex({ "timestamp": -1 });

// Content Analytics Indexes
db.content_analytics.createIndex({ "contentId": 1, "timestamp": -1 });
db.content_analytics.createIndex({ "platform": 1, "timestamp": -1 });
db.content_analytics.createIndex({ "metricType": 1, "timestamp": -1 });

// Media Metadata Indexes
db.media_metadata.createIndex({ "mediaId": 1 });
db.media_metadata.createIndex({ "platform": 1, "mediaType": 1 });
db.media_metadata.createIndex({ "uploadedAt": -1 });
db.media_metadata.createIndex({ "processingStatus": 1 });

// Feed Analytics Indexes
db.feed_analytics.createIndex({ "userId": 1, "timestamp": -1 });
db.feed_analytics.createIndex({ "platform": 1, "feedType": 1, "timestamp": -1 });

// Real-time Events Indexes
db.realtime_events.createIndex({ "eventId": 1 });
db.realtime_events.createIndex({ "platform": 1, "timestamp": -1 });
db.realtime_events.createIndex({ "eventType": 1, "timestamp": -1 });
db.realtime_events.createIndex({ "expiresAt": 1 }, { expireAfterSeconds: 0 });

// Create users for different services
db.createUser({
   user: "analytics_service",
   pwd: "analytics_password",
   roles: [
      { role: "readWrite", db: "entativa_analytics" }
   ]
});

db.createUser({
   user: "media_service", 
   pwd: "media_password",
   roles: [
      { role: "readWrite", db: "entativa_analytics" }
   ]
});

print("MongoDB analytics database initialized successfully!");
print("Collections created: user_analytics, content_analytics, media_metadata, feed_analytics, realtime_events");
print("Indexes created for optimal query performance");
print("Users created: analytics_service, media_service");