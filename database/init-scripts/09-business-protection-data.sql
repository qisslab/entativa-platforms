-- Entativa ID: Business Entity Protection Seed Data
-- Corporations, Business Leaders, Financial Institutions, Tech Companies
-- Data Version: 2.0 - Business Entity Protection System

-- ============== MAJOR CORPORATIONS (Fortune 500) ==============

INSERT INTO protected_corporations (
    company_name, handle, legal_name, aliases, category, industry, stock_symbol, market_cap_billions, 
    founded_year, headquarters_country, headquarters_city, ceo_name, major_brands, subsidiaries, 
    official_websites, social_media, fortune_500_rank, verification_level, protection_reason, added_by
) VALUES 

-- Technology Giants
('Apple Inc.', 'apple', 'Apple Inc.', ARRAY['apple', 'appletv', 'iphone', 'ipad', 'mac'], 'technology', 'Consumer Electronics', 'AAPL', 2800.00, 
 1976, 'United States', 'Cupertino', 'Tim Cook', ARRAY['iPhone', 'iPad', 'Mac', 'Apple Watch', 'Apple TV'], ARRAY['Beats', 'Shazam'],
 ARRAY['https://www.apple.com'], '{"twitter": "@Apple", "instagram": "@apple", "youtube": "@Apple"}', 3, 'premium', 'Global technology leader', 'system'),

('Microsoft Corporation', 'microsoft', 'Microsoft Corporation', ARRAY['msft', 'windows', 'xbox'], 'technology', 'Software', 'MSFT', 2400.00,
 1975, 'United States', 'Redmond', 'Satya Nadella', ARRAY['Windows', 'Office', 'Azure', 'Xbox'], ARRAY['LinkedIn', 'GitHub', 'Activision Blizzard'],
 ARRAY['https://www.microsoft.com'], '{"twitter": "@Microsoft", "instagram": "@microsoft", "youtube": "@Microsoft"}', 14, 'premium', 'Software and cloud computing giant', 'system'),

('Alphabet Inc.', 'google', 'Alphabet Inc.', ARRAY['google', 'youtube', 'gmail', 'android'], 'technology', 'Internet Services', 'GOOGL', 1600.00,
 1998, 'United States', 'Mountain View', 'Sundar Pichai', ARRAY['Google Search', 'YouTube', 'Gmail', 'Android'], ARRAY['YouTube', 'DeepMind', 'Waymo'],
 ARRAY['https://www.google.com'], '{"twitter": "@Google", "instagram": "@google", "youtube": "@Google"}', 1, 'premium', 'Internet and AI technology leader', 'system'),

('Amazon.com Inc.', 'amazon', 'Amazon.com Inc.', ARRAY['aws', 'prime', 'alexa'], 'technology', 'E-commerce/Cloud', 'AMZN', 1300.00,
 1994, 'United States', 'Seattle', 'Andy Jassy', ARRAY['Amazon Prime', 'AWS', 'Alexa', 'Kindle'], ARRAY['Whole Foods', 'Twitch', 'IMDb'],
 ARRAY['https://www.amazon.com'], '{"twitter": "@amazon", "instagram": "@amazon", "youtube": "@amazon"}', 2, 'premium', 'E-commerce and cloud computing leader', 'system'),

('Meta Platforms Inc.', 'meta', 'Meta Platforms Inc.', ARRAY['facebook', 'instagram', 'whatsapp'], 'technology', 'Social Media', 'META', 800.00,
 2004, 'United States', 'Menlo Park', 'Mark Zuckerberg', ARRAY['Facebook', 'Instagram', 'WhatsApp', 'Messenger'], ARRAY['Instagram', 'WhatsApp', 'Oculus'],
 ARRAY['https://www.meta.com'], '{"twitter": "@Meta", "instagram": "@meta", "youtube": "@Meta"}', 31, 'premium', 'Social media and metaverse platform', 'system'),

-- Electric Vehicles & Space
('Tesla Inc.', 'tesla', 'Tesla Inc.', ARRAY['teslamotors', 'model3', 'models'], 'automotive', 'Electric Vehicles', 'TSLA', 800.00,
 2003, 'United States', 'Austin', 'Elon Musk', ARRAY['Model S', 'Model 3', 'Model X', 'Model Y'], ARRAY['SolarCity', 'Tesla Energy'],
 ARRAY['https://www.tesla.com'], '{"twitter": "@Tesla", "instagram": "@tesla", "youtube": "@Tesla"}', 47, 'premium', 'Electric vehicle and clean energy leader', 'system'),

('SpaceX', 'spacex', 'Space Exploration Technologies Corp.', ARRAY['spacex', 'starship', 'falcon'], 'aerospace', 'Space Technology', null, 150.00,
 2002, 'United States', 'Hawthorne', 'Elon Musk', ARRAY['Falcon 9', 'Dragon', 'Starship', 'Starlink'], ARRAY['Starlink'],
 ARRAY['https://www.spacex.com'], '{"twitter": "@SpaceX", "instagram": "@spacex", "youtube": "@SpaceX"}', null, 'premium', 'Private space exploration company', 'system'),

-- Traditional Corporations
('Walmart Inc.', 'walmart', 'Walmart Inc.', ARRAY['walmart'], 'retail', 'Retail', 'WMT', 400.00,
 1962, 'United States', 'Bentonville', 'Doug McMillon', ARRAY['Walmart', 'Sam''s Club'], ARRAY['Sam''s Club', 'Walmart+'],
 ARRAY['https://www.walmart.com'], '{"twitter": "@Walmart", "instagram": "@walmart", "youtube": "@walmart"}', 1, 'premium', 'World''s largest retailer', 'system'),

('Berkshire Hathaway Inc.', 'berkshirehathaway', 'Berkshire Hathaway Inc.', ARRAY['berkshire'], 'finance', 'Investment', 'BRK.A', 700.00,
 1965, 'United States', 'Omaha', 'Warren Buffett', ARRAY['GEICO', 'BNSF Railway'], ARRAY['GEICO', 'Dairy Queen', 'See''s Candies'],
 ARRAY['https://www.berkshirehathaway.com'], '{"twitter": null, "instagram": null, "youtube": null}', 6, 'premium', 'Investment conglomerate led by Warren Buffett', 'system'),

-- International Giants
('Saudi Aramco', 'saudiaramco', 'Saudi Arabian Oil Company', ARRAY['aramco'], 'energy', 'Oil & Gas', null, 2000.00,
 1933, 'Saudi Arabia', 'Dhahran', 'Amin Nasser', ARRAY['Saudi Aramco'], ARRAY['SABIC'],
 ARRAY['https://www.aramco.com'], '{"twitter": "@Saudi_Aramco", "instagram": "@saudiaramco", "youtube": "@SaudiAramco"}', null, 'premium', 'World''s largest oil company', 'system'),

('Toyota Motor Corporation', 'toyota', 'Toyota Motor Corporation', ARRAY['toyota'], 'automotive', 'Automotive', 'TM', 250.00,
 1937, 'Japan', 'Toyota City', 'Akio Toyoda', ARRAY['Camry', 'Corolla', 'Prius', 'RAV4'], ARRAY['Lexus', 'Daihatsu'],
 ARRAY['https://www.toyota.com'], '{"twitter": "@Toyota", "instagram": "@toyota", "youtube": "@Toyota"}', 10, 'premium', 'World''s largest automaker', 'system');

-- ============== BUSINESS LEADERS & ENTREPRENEURS ==============

INSERT INTO protected_business_leaders (
    full_name, handle, aliases, category, company_current, company_history, title, nationality, birth_year,
    net_worth_billions, major_achievements, companies_founded, investments, social_media, forbes_ranking, 
    verification_level, protection_reason, added_by
) VALUES 

-- Tech Billionaires
('Elon Reeve Musk', 'elonmusk', ARRAY['elon', 'musk'], 'entrepreneur', 'Tesla, SpaceX, X Corp', ARRAY['PayPal', 'Tesla', 'SpaceX', 'Neuralink', 'X'], 'CEO', 'South African-American', 1971,
 250.00, ARRAY['Founded PayPal', 'CEO of Tesla', 'CEO of SpaceX', 'Richest person in world'], ARRAY['PayPal', 'Tesla', 'SpaceX', 'Neuralink', 'The Boring Company', 'X'],
 ARRAY['Tesla', 'SpaceX', 'Neuralink', 'OpenAI (former)'], '{"twitter": "@elonmusk", "instagram": "@elonmusk"}', 1, 'premium', 'World''s richest person and tech visionary', 'system'),

('Jeffrey Preston Bezos', 'jeffbezos', ARRAY['bezos'], 'entrepreneur', 'Blue Origin', ARRAY['Amazon'], 'Executive Chairman', 'American', 1964,
 170.00, ARRAY['Founded Amazon', 'Richest person 2017-2021', 'Space tourism pioneer'], ARRAY['Amazon', 'Blue Origin'],
 ARRAY['Amazon', 'Blue Origin', 'Washington Post'], '{"twitter": "@JeffBezos", "instagram": "@jeffbezos"}', 2, 'premium', 'Amazon founder and space entrepreneur', 'system'),

('William Henry Gates III', 'billgates', ARRAY['bill', 'gates'], 'entrepreneur', 'Bill & Melinda Gates Foundation', ARRAY['Microsoft'], 'Co-Chair', 'American', 1955,
 130.00, ARRAY['Co-founded Microsoft', 'Philanthropist', 'Global health advocate'], ARRAY['Microsoft'],
 ARRAY['Microsoft', 'TerraPower', 'Various startups'], '{"twitter": "@BillGates", "instagram": "@thisisbillgates"}', 5, 'premium', 'Microsoft co-founder and philanthropist', 'system'),

('Warren Edward Buffett', 'warrenbuffett', ARRAY['buffett', 'oracleofomaha'], 'investor', 'Berkshire Hathaway', ARRAY['Berkshire Hathaway'], 'CEO', 'American', 1930,
 118.00, ARRAY['Oracle of Omaha', 'Value investing pioneer', 'Berkshire Hathaway CEO'], ARRAY['Berkshire Hathaway (transformed)'],
 ARRAY['Numerous public companies via Berkshire'], '{"twitter": null, "instagram": null}', 6, 'premium', 'Legendary investor and business leader', 'system'),

('Mark Elliot Zuckerberg', 'zuck', ARRAY['zuckerberg', 'mark'], 'entrepreneur', 'Meta Platforms', ARRAY['Facebook/Meta'], 'CEO', 'American', 1984,
 100.00, ARRAY['Founded Facebook', 'Built social media empire', 'Metaverse pioneer'], ARRAY['Facebook/Meta'],
 ARRAY['Meta', 'Various startups'], '{"twitter": null, "instagram": "@zuck"}', 7, 'premium', 'Facebook founder and Meta CEO', 'system'),

-- International Business Leaders
('Bernard Jean Étienne Arnault', 'bernardarnault', ARRAY['arnault'], 'businessman', 'LVMH', ARRAY['LVMH'], 'Chairman & CEO', 'French', 1949,
 200.00, ARRAY['Built LVMH luxury empire', 'Art collector', 'Richest person in Europe'], ARRAY['LVMH (transformed)'],
 ARRAY['LVMH brands', 'Various luxury companies'], '{"twitter": null, "instagram": null}', 3, 'premium', 'Luxury goods magnate and art patron', 'system'),

('Gautam Shantilal Adani', 'gautamadani', ARRAY['adani'], 'businessman', 'Adani Group', ARRAY['Adani Group'], 'Chairman', 'Indian', 1962,
 90.00, ARRAY['Built Adani conglomerate', 'Infrastructure development', 'Renewable energy'], ARRAY['Adani Group'],
 ARRAY['Port operations', 'Energy', 'Infrastructure'], '{"twitter": "@gautam_adani", "instagram": null}', 4, 'premium', 'Indian business magnate and infrastructure leader', 'system');

-- ============== FINANCIAL INSTITUTIONS ==============

INSERT INTO protected_financial_institutions (
    institution_name, handle, legal_name, aliases, category, institution_type, country, assets_billions, 
    founded_year, regulatory_status, major_services, subsidiaries, official_websites, social_media, 
    verification_level, protection_reason, added_by
) VALUES 

-- Major US Banks
('JPMorgan Chase & Co.', 'jpmorgan', 'JPMorgan Chase & Co.', ARRAY['chase', 'jpmorganchase'], 'bank', 'commercial', 'United States', 3200.00,
 1799, ARRAY['FDIC Insured', 'Federal Reserve Member'], ARRAY['Investment Banking', 'Consumer Banking', 'Asset Management'],
 ARRAY['Chase Bank', 'J.P. Morgan', 'J.P. Morgan Private Bank'], ARRAY['https://www.jpmorganchase.com'],
 '{"twitter": "@jpmorgan", "instagram": "@jpmorgan", "youtube": "@jpmorgan"}', 'premium', 'Largest US bank by assets', 'system'),

('Bank of America Corporation', 'bankofamerica', 'Bank of America Corporation', ARRAY['bofa', 'boa'], 'bank', 'commercial', 'United States', 2800.00,
 1904, ARRAY['FDIC Insured', 'Federal Reserve Member'], ARRAY['Consumer Banking', 'Investment Banking', 'Wealth Management'],
 ARRAY['Merrill Lynch', 'Bank of America Private Bank'], ARRAY['https://www.bankofamerica.com'],
 '{"twitter": "@bankofamerica", "instagram": "@bankofamerica", "youtube": "@bankofamerica"}', 'premium', 'Major US consumer and investment bank', 'system'),

('Wells Fargo & Company', 'wellsfargo', 'Wells Fargo & Company', ARRAY['wells'], 'bank', 'commercial', 'United States', 1900.00,
 1852, ARRAY['FDIC Insured', 'Federal Reserve Member'], ARRAY['Consumer Banking', 'Commercial Banking', 'Mortgage Services'],
 ARRAY['Wells Fargo Bank', 'Wells Fargo Advisors'], ARRAY['https://www.wellsfargo.com'],
 '{"twitter": "@wellsfargo", "instagram": "@wellsfargo", "youtube": "@wellsfargo"}', 'premium', 'Historic US banking institution', 'system'),

-- Investment Firms
('Goldman Sachs Group Inc.', 'goldmansachs', 'The Goldman Sachs Group Inc.', ARRAY['gs', 'goldman'], 'bank', 'investment', 'United States', 500.00,
 1869, ARRAY['SEC Registered', 'FINRA Member'], ARRAY['Investment Banking', 'Securities Trading', 'Asset Management'],
 ARRAY['Goldman Sachs Bank USA', 'Marcus'], ARRAY['https://www.goldmansachs.com'],
 '{"twitter": "@goldmansachs", "instagram": "@goldmansachs", "youtube": "@goldmansachs"}', 'premium', 'Premier investment banking firm', 'system'),

('BlackRock Inc.', 'blackrock', 'BlackRock Inc.', ARRAY['blk'], 'investment_firm', 'asset_management', 'United States', 10000.00,
 1988, ARRAY['SEC Registered', 'Investment Adviser'], ARRAY['Asset Management', 'ETFs', 'Investment Solutions'],
 ARRAY['iShares', 'Aladdin'], ARRAY['https://www.blackrock.com'],
 '{"twitter": "@blackrock", "instagram": "@blackrock", "youtube": "@blackrock"}', 'premium', 'World''s largest asset manager', 'system');

-- ============== TECHNOLOGY COMPANIES ==============

INSERT INTO protected_tech_companies (
    company_name, handle, aliases, category, business_model, valuation_billions, founded_year, 
    headquarters_country, funding_stage, major_investors, key_products, founders, official_websites, 
    social_media, verification_level, protection_reason, added_by
) VALUES 

-- Major Tech Companies
('NVIDIA Corporation', 'nvidia', ARRAY['nvda'], 'ai', 'b2b', 1200.00, 1993,
 'United States', 'public', ARRAY['Public shareholders'], ARRAY['GPUs', 'AI chips', 'CUDA', 'GeForce'],
 ARRAY['Jensen Huang', 'Chris Malachowsky', 'Curtis Priem'], ARRAY['https://www.nvidia.com'],
 '{"twitter": "@nvidia", "instagram": "@nvidia", "youtube": "@nvidia"}', 'premium', 'AI and graphics processing leader', 'system'),

('Advanced Micro Devices', 'amd', ARRAY['amd'], 'semiconductor', 'b2b', 230.00, 1969,
 'United States', 'public', ARRAY['Public shareholders'], ARRAY['Ryzen CPUs', 'Radeon GPUs', 'EPYC'],
 ARRAY['Jerry Sanders', 'John Carey'], ARRAY['https://www.amd.com'],
 '{"twitter": "@amd", "instagram": "@amd", "youtube": "@amd"}', 'premium', 'CPU and GPU manufacturer', 'system'),

-- Cloud & SaaS
('Salesforce Inc.', 'salesforce', ARRAY['sfdc'], 'saas', 'b2b', 200.00, 1999,
 'United States', 'public', ARRAY['Public shareholders'], ARRAY['CRM', 'Sales Cloud', 'Service Cloud', 'Slack'],
 ARRAY['Marc Benioff', 'Parker Harris'], ARRAY['https://www.salesforce.com'],
 '{"twitter": "@salesforce", "instagram": "@salesforce", "youtube": "@salesforce"}', 'premium', 'Leading CRM and cloud platform', 'system'),

('Adobe Inc.', 'adobe', ARRAY['adbe'], 'software', 'b2b', 240.00, 1982,
 'United States', 'public', ARRAY['Public shareholders'], ARRAY['Photoshop', 'Illustrator', 'Creative Cloud', 'Acrobat'],
 ARRAY['John Warnock', 'Charles Geschke'], ARRAY['https://www.adobe.com'],
 '{"twitter": "@adobe", "instagram": "@adobe", "youtube": "@adobe"}', 'premium', 'Creative software industry leader', 'system'),

-- Emerging Tech
('OpenAI', 'openai', ARRAY['chatgpt'], 'ai', 'b2b', 90.00, 2015,
 'United States', 'unicorn', ARRAY['Microsoft', 'Khosla Ventures'], ARRAY['ChatGPT', 'GPT-4', 'DALL-E', 'API'],
 ARRAY['Sam Altman', 'Elon Musk', 'Ilya Sutskever'], ARRAY['https://www.openai.com'],
 '{"twitter": "@openai", "instagram": null, "youtube": "@OpenAI"}', 'premium', 'Leading AI research and deployment company', 'system'),

('Stripe Inc.', 'stripe', ARRAY['stripe'], 'fintech', 'b2b', 95.00, 2010,
 'United States', 'unicorn', ARRAY['Sequoia Capital', 'General Catalyst'], ARRAY['Payment processing', 'Stripe Connect', 'Billing'],
 ARRAY['Patrick Collison', 'John Collison'], ARRAY['https://www.stripe.com'],
 '{"twitter": "@stripe", "instagram": "@stripe", "youtube": "@stripe"}', 'premium', 'Leading online payment processor', 'system');

-- ============== VERIFICATION UPDATES ==============

-- Update verification levels based on market cap and influence
UPDATE protected_corporations SET verification_level = 'premium' WHERE market_cap_billions > 100.00;
UPDATE protected_business_leaders SET verification_level = 'premium' WHERE net_worth_billions > 50.00;
UPDATE protected_financial_institutions SET verification_level = 'premium' WHERE assets_billions > 500.00;
UPDATE protected_tech_companies SET verification_level = 'premium' WHERE valuation_billions > 50.00;

-- Add creation timestamps
UPDATE protected_corporations SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_business_leaders SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_financial_institutions SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_tech_companies SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;