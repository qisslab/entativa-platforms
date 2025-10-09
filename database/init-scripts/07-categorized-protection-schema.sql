-- Entativa ID: Comprehensive Categorized Protection Database
-- Multiple specialized databases for well-known figures, businesses, and entities
-- Schema Version: 2.0 - Categorized Protection System

-- ============== CELEBRITY PROTECTION DATABASES ==============

-- Entertainment Celebrities (Movies, TV, Theater)
CREATE TABLE protected_entertainment_celebrities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    stage_name VARCHAR(255),
    aliases TEXT[], -- Array of known aliases/nicknames
    category VARCHAR(50) NOT NULL, -- actor, director, producer, screenwriter
    subcategory VARCHAR(50), -- hollywood, bollywood, independent, theater
    nationality VARCHAR(100),
    birth_year INTEGER,
    imdb_id VARCHAR(50),
    major_works TEXT[], -- Array of notable films/shows
    awards TEXT[], -- Oscars, Emmys, etc.
    social_media JSONB, -- Twitter, Instagram, TikTok handles
    verification_level VARCHAR(20) DEFAULT 'protected', -- protected, verified, premium
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Music Industry (Artists, Producers, Labels)
CREATE TABLE protected_music_celebrities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    stage_name VARCHAR(255),
    band_name VARCHAR(255),
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- singer, rapper, producer, songwriter, band
    genre VARCHAR(100), -- pop, rock, hip-hop, country, etc.
    nationality VARCHAR(100),
    birth_year INTEGER,
    spotify_id VARCHAR(100),
    apple_music_id VARCHAR(100),
    record_label VARCHAR(200),
    major_albums TEXT[],
    awards TEXT[], -- Grammys, Billboard, etc.
    social_media JSONB,
    monthly_listeners INTEGER, -- Spotify monthly listeners
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Sports Figures (Athletes, Coaches, Teams)
CREATE TABLE protected_sports_figures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(255),
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- athlete, coach, commentator, team_owner
    sport VARCHAR(100) NOT NULL, -- basketball, football, soccer, tennis, etc.
    position VARCHAR(100),
    team_current VARCHAR(200),
    team_history TEXT[],
    nationality VARCHAR(100),
    birth_year INTEGER,
    major_achievements TEXT[], -- Championships, records, medals
    social_media JSONB,
    endorsements TEXT[], -- Major brand partnerships
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Social Media Influencers & Content Creators
CREATE TABLE protected_digital_celebrities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    screen_name VARCHAR(255),
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- youtuber, tiktoker, streamer, podcaster, blogger
    niche VARCHAR(100), -- gaming, beauty, tech, lifestyle, education
    nationality VARCHAR(100),
    birth_year INTEGER,
    primary_platform VARCHAR(50), -- YouTube, TikTok, Twitch, Instagram
    follower_counts JSONB, -- Followers on each platform
    verified_platforms TEXT[], -- Which platforms they're verified on
    major_content TEXT[], -- Popular videos, series, content
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== POLITICAL & GOVERNMENT FIGURES ==============

-- World Leaders & Politicians
CREATE TABLE protected_political_figures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200), -- President, Prime Minister, Senator, etc.
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- head_of_state, legislator, judge, diplomat
    country VARCHAR(100) NOT NULL,
    political_party VARCHAR(200),
    office_current VARCHAR(300),
    office_history TEXT[],
    birth_year INTEGER,
    major_policies TEXT[],
    social_media JSONB,
    official_websites TEXT[],
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Government Agencies & Organizations
CREATE TABLE protected_government_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    acronym VARCHAR(50),
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- federal_agency, state_agency, international_org
    country VARCHAR(100) NOT NULL,
    department VARCHAR(200),
    jurisdiction VARCHAR(100), -- federal, state, local, international
    head_of_agency VARCHAR(255),
    official_websites TEXT[],
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== BUSINESS & CORPORATE ENTITIES ==============

-- Fortune 500 Companies & Major Corporations
CREATE TABLE protected_corporations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    legal_name VARCHAR(255),
    aliases TEXT[], -- Known brands, subsidiaries
    category VARCHAR(50) NOT NULL, -- technology, finance, retail, healthcare, etc.
    industry VARCHAR(100),
    stock_symbol VARCHAR(10),
    market_cap_billions DECIMAL(10,2),
    founded_year INTEGER,
    headquarters_country VARCHAR(100),
    headquarters_city VARCHAR(100),
    ceo_name VARCHAR(255),
    major_brands TEXT[],
    subsidiaries TEXT[],
    official_websites TEXT[],
    social_media JSONB,
    fortune_500_rank INTEGER,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Business Leaders & Executives
CREATE TABLE protected_business_leaders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- ceo, founder, investor, entrepreneur
    company_current VARCHAR(255),
    company_history TEXT[],
    title VARCHAR(200),
    nationality VARCHAR(100),
    birth_year INTEGER,
    net_worth_billions DECIMAL(10,2),
    major_achievements TEXT[],
    companies_founded TEXT[],
    investments TEXT[], -- Major investment portfolio
    social_media JSONB,
    forbes_ranking INTEGER,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Financial Institutions & Banks
CREATE TABLE protected_financial_institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    legal_name VARCHAR(255),
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- bank, credit_union, investment_firm, insurance
    institution_type VARCHAR(100), -- commercial, investment, central, fintech
    country VARCHAR(100) NOT NULL,
    assets_billions DECIMAL(15,2),
    founded_year INTEGER,
    regulatory_status TEXT[], -- FDIC insured, SEC registered, etc.
    major_services TEXT[],
    subsidiaries TEXT[],
    official_websites TEXT[],
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== MEDIA & JOURNALISM ==============

-- News Organizations & Media Companies
CREATE TABLE protected_media_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- newspaper, tv_network, radio, digital_media
    media_type VARCHAR(100), -- print, broadcast, streaming, podcast
    country VARCHAR(100) NOT NULL,
    founded_year INTEGER,
    circulation INTEGER, -- For newspapers/magazines
    viewership INTEGER, -- For TV/streaming
    parent_company VARCHAR(255),
    major_shows TEXT[], -- Popular programs/content
    notable_journalists TEXT[],
    official_websites TEXT[],
    social_media JSONB,
    press_freedom_rating VARCHAR(50),
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Journalists & Media Personalities
CREATE TABLE protected_journalists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- reporter, anchor, columnist, correspondent
    beat VARCHAR(100), -- politics, sports, tech, entertainment, etc.
    current_employer VARCHAR(255),
    employer_history TEXT[],
    nationality VARCHAR(100),
    birth_year INTEGER,
    major_stories TEXT[], -- Notable reporting/coverage
    awards TEXT[], -- Pulitzer, Emmy, etc.
    books_authored TEXT[],
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== ACADEMIC & SCIENTIFIC FIGURES ==============

-- Nobel Laureates & Renowned Scientists
CREATE TABLE protected_scientists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- physicist, chemist, biologist, mathematician
    field_of_study VARCHAR(200),
    current_institution VARCHAR(255),
    institution_history TEXT[],
    nationality VARCHAR(100),
    birth_year INTEGER,
    nobel_prize_year INTEGER,
    nobel_category VARCHAR(100),
    major_discoveries TEXT[],
    publications_notable TEXT[],
    awards TEXT[],
    social_media JSONB,
    h_index INTEGER, -- Academic citation index
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Universities & Academic Institutions
CREATE TABLE protected_academic_institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- university, college, research_institute
    institution_type VARCHAR(100), -- public, private, ivy_league, community
    country VARCHAR(100) NOT NULL,
    founded_year INTEGER,
    student_population INTEGER,
    endowment_billions DECIMAL(10,2),
    ranking_us INTEGER, -- US News ranking
    ranking_world INTEGER, -- World university ranking
    notable_alumni TEXT[],
    major_programs TEXT[],
    research_areas TEXT[],
    official_websites TEXT[],
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== TECHNOLOGY & INNOVATION ==============

-- Tech Startups & Unicorns
CREATE TABLE protected_tech_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(255) NOT NULL,
    handle VARCHAR(100) NOT NULL UNIQUE,
    aliases TEXT[],
    category VARCHAR(50) NOT NULL, -- saas, fintech, biotech, ai, blockchain
    business_model VARCHAR(100), -- b2b, b2c, marketplace, platform
    valuation_billions DECIMAL(10,2),
    founded_year INTEGER,
    headquarters_country VARCHAR(100),
    funding_stage VARCHAR(50), -- seed, series_a, unicorn, public
    major_investors TEXT[],
    key_products TEXT[],
    founders TEXT[],
    official_websites TEXT[],
    social_media JSONB,
    verification_level VARCHAR(20) DEFAULT 'protected',
    protection_reason TEXT,
    added_by VARCHAR(100),
    verified_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============== INDEXES FOR PERFORMANCE ==============

-- Entertainment Celebrities
CREATE INDEX idx_entertainment_celebrities_handle ON protected_entertainment_celebrities(handle);
CREATE INDEX idx_entertainment_celebrities_category ON protected_entertainment_celebrities(category);
CREATE INDEX idx_entertainment_celebrities_name ON protected_entertainment_celebrities(full_name);

-- Music Celebrities
CREATE INDEX idx_music_celebrities_handle ON protected_music_celebrities(handle);
CREATE INDEX idx_music_celebrities_genre ON protected_music_celebrities(genre);
CREATE INDEX idx_music_celebrities_name ON protected_music_celebrities(full_name);

-- Sports Figures
CREATE INDEX idx_sports_figures_handle ON protected_sports_figures(handle);
CREATE INDEX idx_sports_figures_sport ON protected_sports_figures(sport);
CREATE INDEX idx_sports_figures_name ON protected_sports_figures(full_name);

-- Digital Celebrities
CREATE INDEX idx_digital_celebrities_handle ON protected_digital_celebrities(handle);
CREATE INDEX idx_digital_celebrities_platform ON protected_digital_celebrities(primary_platform);
CREATE INDEX idx_digital_celebrities_name ON protected_digital_celebrities(full_name);

-- Political Figures
CREATE INDEX idx_political_figures_handle ON protected_political_figures(handle);
CREATE INDEX idx_political_figures_country ON protected_political_figures(country);
CREATE INDEX idx_political_figures_name ON protected_political_figures(full_name);

-- Corporations
CREATE INDEX idx_corporations_handle ON protected_corporations(handle);
CREATE INDEX idx_corporations_category ON protected_corporations(category);
CREATE INDEX idx_corporations_name ON protected_corporations(company_name);

-- Business Leaders
CREATE INDEX idx_business_leaders_handle ON protected_business_leaders(handle);
CREATE INDEX idx_business_leaders_company ON protected_business_leaders(company_current);
CREATE INDEX idx_business_leaders_name ON protected_business_leaders(full_name);

-- Media Organizations
CREATE INDEX idx_media_organizations_handle ON protected_media_organizations(handle);
CREATE INDEX idx_media_organizations_type ON protected_media_organizations(media_type);
CREATE INDEX idx_media_organizations_name ON protected_media_organizations(organization_name);

-- Scientists
CREATE INDEX idx_scientists_handle ON protected_scientists(handle);
CREATE INDEX idx_scientists_field ON protected_scientists(field_of_study);
CREATE INDEX idx_scientists_name ON protected_scientists(full_name);

-- Academic Institutions
CREATE INDEX idx_academic_institutions_handle ON protected_academic_institutions(handle);
CREATE INDEX idx_academic_institutions_type ON protected_academic_institutions(institution_type);
CREATE INDEX idx_academic_institutions_name ON protected_academic_institutions(institution_name);

-- ============== UNIFIED SEARCH VIEW ==============

-- Create a unified view for searching across all protected entities
CREATE VIEW protected_entities_unified AS
SELECT 
    'entertainment' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_entertainment_celebrities
UNION ALL
SELECT 
    'music' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_music_celebrities
UNION ALL
SELECT 
    'sports' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_sports_figures
UNION ALL
SELECT 
    'digital' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_digital_celebrities
UNION ALL
SELECT 
    'political' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_political_figures
UNION ALL
SELECT 
    'business' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_business_leaders
UNION ALL
SELECT 
    'corporation' as entity_type,
    id,
    company_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_corporations
UNION ALL
SELECT 
    'media' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_journalists
UNION ALL
SELECT 
    'scientist' as entity_type,
    id,
    full_name as name,
    handle,
    category,
    aliases,
    verification_level,
    created_at
FROM protected_scientists;

-- Index for the unified view
CREATE INDEX idx_protected_entities_unified_handle ON protected_entities_unified(handle);
CREATE INDEX idx_protected_entities_unified_type ON protected_entities_unified(entity_type);
CREATE INDEX idx_protected_entities_unified_name ON protected_entities_unified(name);