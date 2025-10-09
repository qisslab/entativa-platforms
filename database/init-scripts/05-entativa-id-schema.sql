CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Core Entativa ID Database Schema
-- Unified identity management across all platforms

-- ============== CORE IDENTITY TABLES ==============

-- Global Entativa Identity - One record per real person/entity
CREATE TABLE entativa_identities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eid VARCHAR(100) UNIQUE NOT NULL, -- The unique Entativa ID (e.g., @neoqiss)
    email VARCHAR(320) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deactivated', 'pending_verification')),
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    profile_completed BOOLEAN DEFAULT FALSE,
    
    -- Verification and reputation
    verification_status VARCHAR(20) DEFAULT 'none' CHECK (verification_status IN ('none', 'pending', 'verified', 'celebrity', 'company', 'government')),
    verification_badge VARCHAR(20) CHECK (verification_badge IN ('blue', 'gold', 'business', 'government')),
    verification_date TIMESTAMP WITH TIME ZONE,
    reputation_score INTEGER DEFAULT 1000,
    
    -- Security
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Metadata
    created_by VARCHAR(50) DEFAULT 'self_registration',
    ip_address INET,
    user_agent TEXT,
    country_code CHAR(2),
    
    -- Indexing
    CONSTRAINT valid_eid_format CHECK (eid ~ '^[a-zA-Z0-9][a-zA-Z0-9._-]{2,98}[a-zA-Z0-9]$')
);

-- Profile information linked to identity
CREATE TABLE entativa_profiles (
    identity_id UUID PRIMARY KEY REFERENCES entativa_identities(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150),
    bio TEXT,
    website VARCHAR(500),
    location VARCHAR(200),
    birth_date DATE,
    gender VARCHAR(20),
    pronouns VARCHAR(50),
    avatar_url VARCHAR(500),
    banner_url VARCHAR(500),
    
    -- Professional info
    profession VARCHAR(200),
    company VARCHAR(200),
    industry VARCHAR(100),
    
    -- Privacy settings
    profile_visibility VARCHAR(20) DEFAULT 'public' CHECK (profile_visibility IN ('public', 'friends', 'private')),
    birth_date_visibility VARCHAR(20) DEFAULT 'private' CHECK (birth_date_visibility IN ('public', 'friends', 'private')),
    location_visibility VARCHAR(20) DEFAULT 'public' CHECK (location_visibility IN ('public', 'friends', 'private')),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Platform-specific accounts (one EiD can have accounts on multiple platforms)
CREATE TABLE platform_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identity_id UUID NOT NULL REFERENCES entativa_identities(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL CHECK (platform IN ('sonet', 'gala', 'pika', 'playpods')),
    platform_user_id UUID NOT NULL, -- References the user ID in the platform's database
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deactivated')),
    last_active_at TIMESTAMP WITH TIME ZONE,
    
    -- Platform-specific metadata
    platform_metadata JSONB DEFAULT '{}',
    
    UNIQUE(identity_id, platform),
    UNIQUE(platform, platform_user_id)
);

-- ============== HANDLE MANAGEMENT TABLES ==============

-- Well-known figures database (celebrities, influencers, politicians, etc.)
CREATE TABLE well_known_figures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('celebrity', 'politician', 'athlete', 'musician', 'actor', 'author', 'influencer', 'journalist', 'scientist', 'business_leader', 'other')),
    preferred_handle VARCHAR(100) NOT NULL,
    alternative_handles TEXT[], -- Array of alternative handles this person might want
    verification_level VARCHAR(20) DEFAULT 'high' CHECK (verification_level IN ('ultra_high', 'high', 'medium')),
    
    -- External verification
    wikipedia_url VARCHAR(500),
    verified_social_accounts JSONB DEFAULT '{}', -- Twitter, Instagram, etc.
    imdb_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    claimed_by UUID REFERENCES entativa_identities(id), -- NULL if unclaimed
    claimed_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(preferred_handle)
);

-- Well-known companies and organizations
CREATE TABLE well_known_companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(300),
    industry VARCHAR(100) NOT NULL,
    company_type VARCHAR(50) CHECK (company_type IN ('public_company', 'private_company', 'nonprofit', 'government', 'educational', 'startup')),
    
    -- Handle management
    preferred_handle VARCHAR(100) NOT NULL,
    alternative_handles TEXT[],
    
    -- Company info
    stock_symbol VARCHAR(10),
    founded_year INTEGER,
    headquarters_country CHAR(2),
    website VARCHAR(500),
    linkedin_url VARCHAR(500),
    
    -- Verification requirements
    verification_level VARCHAR(20) DEFAULT 'high' CHECK (verification_level IN ('ultra_high', 'high', 'medium')),
    required_documents TEXT[] DEFAULT ARRAY['business_registration', 'tax_id', 'domain_ownership'],
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    claimed_by UUID REFERENCES entativa_identities(id),
    claimed_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(preferred_handle)
);

-- Reserved handles (Entativa-specific and future platform handles)
CREATE TABLE reserved_handles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    handle VARCHAR(100) NOT NULL UNIQUE,
    reservation_type VARCHAR(30) NOT NULL CHECK (reservation_type IN ('entativa_system', 'platform_specific', 'future_expansion', 'trademark_protection', 'abuse_prevention')),
    platform VARCHAR(20) CHECK (platform IN ('all', 'sonet', 'gala', 'pika', 'playpods')),
    reason TEXT NOT NULL,
    reserved_until TIMESTAMP WITH TIME ZONE, -- NULL for permanent reservation
    can_be_released BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100) DEFAULT 'system'
);

-- Handle similarity detection and protection
CREATE TABLE protected_handles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    original_handle VARCHAR(100) NOT NULL,
    protected_entity_type VARCHAR(20) NOT NULL CHECK (protected_entity_type IN ('well_known_figure', 'company', 'reserved')),
    protected_entity_id UUID, -- References well_known_figures, well_known_companies, or reserved_handles
    similarity_threshold DECIMAL(3,2) DEFAULT 0.85, -- Minimum similarity to trigger protection
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============== VERIFICATION TABLES ==============

-- Verification requests and workflow
CREATE TABLE verification_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identity_id UUID NOT NULL REFERENCES entativa_identities(id) ON DELETE CASCADE,
    verification_type VARCHAR(20) NOT NULL CHECK (verification_type IN ('personal', 'celebrity', 'company', 'government')),
    
    -- Request details
    requested_handle VARCHAR(100),
    claim_type VARCHAR(30) CHECK (claim_type IN ('well_known_figure', 'company', 'general_verification')),
    claimed_entity_id UUID, -- References well_known_figures or well_known_companies
    
    -- Status tracking
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'under_review', 'approved', 'rejected', 'requires_more_info')),
    priority INTEGER DEFAULT 3 CHECK (priority BETWEEN 1 AND 5), -- 1 = highest priority
    
    -- Review process
    assigned_reviewer UUID,
    review_started_at TIMESTAMP WITH TIME ZONE,
    review_completed_at TIMESTAMP WITH TIME ZONE,
    admin_notes TEXT,
    rejection_reason TEXT,
    
    -- Applicant information
    applicant_notes TEXT,
    contact_email VARCHAR(320),
    contact_phone VARCHAR(20),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Document submissions for verification
CREATE TABLE verification_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL CHECK (document_type IN ('government_id', 'passport', 'business_registration', 'tax_document', 'domain_ownership', 'social_media_proof', 'employment_verification', 'other')),
    
    -- Document details
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size INTEGER,
    mime_type VARCHAR(100),
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Verification status
    is_verified BOOLEAN DEFAULT FALSE,
    verified_by UUID,
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_notes TEXT,
    
    -- Security
    file_hash VARCHAR(64), -- SHA-256 hash
    encrypted BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============== AUTHENTICATION & SECURITY TABLES ==============

-- OAuth applications and API keys
CREATE TABLE oauth_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    client_id VARCHAR(64) UNIQUE NOT NULL,
    client_secret_hash VARCHAR(255) NOT NULL,
    
    -- Application details
    redirect_uris TEXT[] NOT NULL,
    allowed_scopes TEXT[] DEFAULT ARRAY['profile:read'],
    application_type VARCHAR(20) DEFAULT 'web' CHECK (application_type IN ('web', 'native', 'service')),
    
    -- Owner
    owner_identity_id UUID NOT NULL REFERENCES entativa_identities(id) ON DELETE CASCADE,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_trusted BOOLEAN DEFAULT FALSE, -- Entativa first-party apps
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- OAuth tokens
CREATE TABLE oauth_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    access_token_hash VARCHAR(255) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255) UNIQUE,
    
    -- Token details
    identity_id UUID NOT NULL REFERENCES entativa_identities(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES oauth_applications(id) ON DELETE CASCADE,
    scopes TEXT[] NOT NULL,
    
    -- Expiration
    access_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Usage tracking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE,
    usage_count INTEGER DEFAULT 0,
    
    -- Security
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID REFERENCES entativa_identities(id),
    
    -- Client info
    ip_address INET,
    user_agent TEXT
);

-- Two-factor authentication
CREATE TABLE two_factor_auth (
    identity_id UUID PRIMARY KEY REFERENCES entativa_identities(id) ON DELETE CASCADE,
    method VARCHAR(20) NOT NULL CHECK (method IN ('totp', 'sms', 'email')),
    secret_key VARCHAR(255), -- Encrypted TOTP secret
    backup_codes TEXT[], -- Encrypted backup codes
    
    -- Status
    is_enabled BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    
    -- Usage tracking
    last_used_at TIMESTAMP WITH TIME ZONE,
    failed_attempts INTEGER DEFAULT 0,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============== AUDIT AND COMPLIANCE TABLES ==============

-- Identity audit log
CREATE TABLE identity_audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identity_id UUID REFERENCES entativa_identities(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    details JSONB DEFAULT '{}',
    
    -- Context
    performed_by UUID REFERENCES entativa_identities(id),
    ip_address INET,
    user_agent TEXT,
    
    -- Compliance
    gdpr_lawful_basis VARCHAR(50),
    data_retention_policy VARCHAR(50),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Handle change history
CREATE TABLE handle_change_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identity_id UUID NOT NULL REFERENCES entativa_identities(id) ON DELETE CASCADE,
    old_eid VARCHAR(100) NOT NULL,
    new_eid VARCHAR(100) NOT NULL,
    reason VARCHAR(200),
    
    -- Approval process
    requires_approval BOOLEAN DEFAULT FALSE,
    approved_by UUID REFERENCES entativa_identities(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============== INDEXES FOR PERFORMANCE ==============

-- Core identity indexes
CREATE INDEX idx_entativa_identities_email ON entativa_identities(email);
CREATE INDEX idx_entativa_identities_phone ON entativa_identities(phone);
CREATE INDEX idx_entativa_identities_status ON entativa_identities(status);
CREATE INDEX idx_entativa_identities_verification_status ON entativa_identities(verification_status);
CREATE INDEX idx_entativa_identities_created_at ON entativa_identities(created_at);

-- Handle search indexes
CREATE INDEX idx_entativa_identities_eid_trgm ON entativa_identities USING gin(eid gin_trgm_ops);
CREATE INDEX idx_well_known_figures_handle_trgm ON well_known_figures USING gin(preferred_handle gin_trgm_ops);
CREATE INDEX idx_well_known_companies_handle_trgm ON well_known_companies USING gin(preferred_handle gin_trgm_ops);

-- Platform accounts indexes
CREATE INDEX idx_platform_accounts_identity_id ON platform_accounts(identity_id);
CREATE INDEX idx_platform_accounts_platform ON platform_accounts(platform);
CREATE INDEX idx_platform_accounts_status ON platform_accounts(status);

-- Verification indexes
CREATE INDEX idx_verification_requests_status ON verification_requests(status);
CREATE INDEX idx_verification_requests_priority ON verification_requests(priority);
CREATE INDEX idx_verification_requests_type ON verification_requests(verification_type);

-- OAuth indexes
CREATE INDEX idx_oauth_tokens_identity_id ON oauth_tokens(identity_id);
CREATE INDEX idx_oauth_tokens_application_id ON oauth_tokens(application_id);
CREATE INDEX idx_oauth_tokens_expires_at ON oauth_tokens(access_token_expires_at);

-- Audit indexes
CREATE INDEX idx_identity_audit_log_identity_id ON identity_audit_log(identity_id);
CREATE INDEX idx_identity_audit_log_action ON identity_audit_log(action);
CREATE INDEX idx_identity_audit_log_created_at ON identity_audit_log(created_at);

-- ============== INITIAL DATA ==============

-- Insert reserved Entativa handles
INSERT INTO reserved_handles (handle, reservation_type, platform, reason) VALUES
-- Core Entativa handles
('entativa', 'entativa_system', 'all', 'Core Entativa brand handle'),
('eid', 'entativa_system', 'all', 'Entativa ID abbreviation'),
('admin', 'entativa_system', 'all', 'Administrative account'),
('support', 'entativa_system', 'all', 'Customer support account'),
('help', 'entativa_system', 'all', 'Help and documentation'),
('api', 'entativa_system', 'all', 'API documentation and updates'),
('dev', 'entativa_system', 'all', 'Developer relations'),
('security', 'entativa_system', 'all', 'Security team communications'),
('legal', 'entativa_system', 'all', 'Legal and compliance'),
('privacy', 'entativa_system', 'all', 'Privacy policy and updates'),

-- Platform-specific handles
('sonet', 'platform_specific', 'all', 'Sonet platform handle'),
('gala', 'platform_specific', 'all', 'Gala platform handle'),
('pika', 'platform_specific', 'all', 'Pika platform handle'),
('playpods', 'platform_specific', 'all', 'PlayPods platform handle'),

-- Abuse prevention
('abuse', 'abuse_prevention', 'all', 'Prevent abuse-related handles'),
('spam', 'abuse_prevention', 'all', 'Prevent spam-related handles'),
('fake', 'abuse_prevention', 'all', 'Prevent fake account indicators'),
('bot', 'abuse_prevention', 'all', 'Prevent bot-related handles'),
('test', 'abuse_prevention', 'all', 'Prevent test account confusion'),

-- Future expansion
('marketplace', 'future_expansion', 'all', 'Future marketplace feature'),
('creator', 'future_expansion', 'all', 'Creator program'),
('verified', 'future_expansion', 'all', 'Verification program'),
('premium', 'future_expansion', 'all', 'Premium features');

-- Create function to update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Add triggers for updated_at
CREATE TRIGGER update_entativa_identities_updated_at BEFORE UPDATE ON entativa_identities FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_entativa_profiles_updated_at BEFORE UPDATE ON entativa_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_well_known_figures_updated_at BEFORE UPDATE ON well_known_figures FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_well_known_companies_updated_at BEFORE UPDATE ON well_known_companies FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_verification_requests_updated_at BEFORE UPDATE ON verification_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_oauth_applications_updated_at BEFORE UPDATE ON oauth_applications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_two_factor_auth_updated_at BEFORE UPDATE ON two_factor_auth FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();