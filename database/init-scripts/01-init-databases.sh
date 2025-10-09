#!/bin/bash
# Entativa Platforms - Multi-Database Initialization
# Creates separate databases for each platform

set -e

# Create multiple databases for different platforms
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Sonet Database (Facebook-like)
    CREATE DATABASE sonet_db;
    CREATE USER sonet_user WITH ENCRYPTED PASSWORD 'sonet_password';
    GRANT ALL PRIVILEGES ON DATABASE sonet_db TO sonet_user;

    -- Gala Database (Instagram-like)
    CREATE DATABASE gala_db;
    CREATE USER gala_user WITH ENCRYPTED PASSWORD 'gala_password';
    GRANT ALL PRIVILEGES ON DATABASE gala_db TO gala_user;

    -- Pika Database (Threads-like)
    CREATE DATABASE pika_db;
    CREATE USER pika_user WITH ENCRYPTED PASSWORD 'pika_password';
    GRANT ALL PRIVILEGES ON DATABASE pika_db TO pika_user;

    -- PlayPods Database (YouTube-like)
    CREATE DATABASE playpods_db;
    CREATE USER playpods_user WITH ENCRYPTED PASSWORD 'playpods_password';
    GRANT ALL PRIVILEGES ON DATABASE playpods_db TO playpods_user;

    -- Entativa ID Database (Identity service)
    CREATE DATABASE entativa_id_db;
    CREATE USER entativa_id_user WITH ENCRYPTED PASSWORD 'entativa_id_password';
    GRANT ALL PRIVILEGES ON DATABASE entativa_id_db TO entativa_id_user;

    -- Analytics Database (Cross-platform analytics)
    CREATE DATABASE entativa_analytics_db;
    CREATE USER analytics_user WITH ENCRYPTED PASSWORD 'analytics_password';
    GRANT ALL PRIVILEGES ON DATABASE entativa_analytics_db TO analytics_user;

    -- Create extensions for all databases
    \c sonet_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

    \c gala_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

    \c pika_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

    \c playpods_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

    \c entativa_id_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

    \c entativa_analytics_db;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
    CREATE EXTENSION IF NOT EXISTS "timescaledb" CASCADE;
EOSQL

echo "All Entativa databases created successfully!"