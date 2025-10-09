-- Entativa ID - Well-Known Figures and Companies Initial Data
-- This file populates the protection databases for celebrities and major companies

-- ============== WELL-KNOWN FIGURES ==============

INSERT INTO well_known_figures (name, category, preferred_handle, alternative_handles, verification_level, wikipedia_url, verified_social_accounts) VALUES

-- Tech Leaders & Entrepreneurs
('Elon Musk', 'business_leader', 'elonmusk', ARRAY['elon', 'musk', 'tesla', 'spacex'], 'ultra_high', 'https://en.wikipedia.org/wiki/Elon_Musk', '{"twitter": "elonmusk", "instagram": "elonmusk"}'),
('Jeff Bezos', 'business_leader', 'jeffbezos', ARRAY['jeff', 'bezos', 'amazon'], 'ultra_high', 'https://en.wikipedia.org/wiki/Jeff_Bezos', '{"twitter": "JeffBezos", "instagram": "jeffbezos"}'),
('Bill Gates', 'business_leader', 'billgates', ARRAY['bill', 'gates', 'microsoft'], 'ultra_high', 'https://en.wikipedia.org/wiki/Bill_Gates', '{"twitter": "BillGates", "instagram": "thisisbillgates"}'),
('Tim Cook', 'business_leader', 'timcook', ARRAY['tim', 'cook', 'apple'], 'ultra_high', 'https://en.wikipedia.org/wiki/Tim_Cook', '{"twitter": "tim_cook"}'),
('Mark Zuckerberg', 'business_leader', 'markzuckerberg', ARRAY['mark', 'zuckerberg', 'facebook', 'meta'], 'ultra_high', 'https://en.wikipedia.org/wiki/Mark_Zuckerberg', '{"facebook": "zuck", "instagram": "zuck"}'),
('Sundar Pichai', 'business_leader', 'sundarpichai', ARRAY['sundar', 'pichai', 'google'], 'ultra_high', 'https://en.wikipedia.org/wiki/Sundar_Pichai', '{"twitter": "sundarpichai"}'),
('Satya Nadella', 'business_leader', 'satyanadella', ARRAY['satya', 'nadella', 'microsoft'], 'ultra_high', 'https://en.wikipedia.org/wiki/Satya_Nadella', '{"twitter": "satyanadella"}'),
('Jensen Huang', 'business_leader', 'jensenhuang', ARRAY['jensen', 'huang', 'nvidia'], 'high', 'https://en.wikipedia.org/wiki/Jensen_Huang', '{}'),
('Lisa Su', 'business_leader', 'lisasu', ARRAY['lisa', 'su', 'amd'], 'high', 'https://en.wikipedia.org/wiki/Lisa_Su', '{"twitter": "LisaSu_AMD"}'),
('Reed Hastings', 'business_leader', 'reedhastings', ARRAY['reed', 'hastings', 'netflix'], 'high', 'https://en.wikipedia.org/wiki/Reed_Hastings', '{}'),

-- Political Leaders
('Joe Biden', 'politician', 'joebiden', ARRAY['biden', 'president', 'potus'], 'ultra_high', 'https://en.wikipedia.org/wiki/Joe_Biden', '{"twitter": "JoeBiden", "instagram": "joebiden"}'),
('Donald Trump', 'politician', 'donaldtrump', ARRAY['trump', 'realdonaldtrump'], 'ultra_high', 'https://en.wikipedia.org/wiki/Donald_Trump', '{"truthsocial": "realDonaldTrump"}'),
('Barack Obama', 'politician', 'barackobama', ARRAY['obama', 'barack'], 'ultra_high', 'https://en.wikipedia.org/wiki/Barack_Obama', '{"twitter": "BarackObama", "instagram": "barackobama"}'),
('Emmanuel Macron', 'politician', 'emmanuelmacron', ARRAY['macron', 'emmanuel'], 'ultra_high', 'https://en.wikipedia.org/wiki/Emmanuel_Macron', '{"twitter": "EmmanuelMacron"}'),
('Justin Trudeau', 'politician', 'justintrudeau', ARRAY['trudeau', 'justin'], 'ultra_high', 'https://en.wikipedia.org/wiki/Justin_Trudeau', '{"twitter": "JustinTrudeau", "instagram": "justinpjtrudeau"}'),

-- Celebrities & Actors
('Taylor Swift', 'musician', 'taylorswift', ARRAY['taylor', 'swift', 'tswift'], 'ultra_high', 'https://en.wikipedia.org/wiki/Taylor_Swift', '{"twitter": "taylorswift13", "instagram": "taylorswift"}'),
('Beyonce', 'musician', 'beyonce', ARRAY['beyonce', 'queenb'], 'ultra_high', 'https://en.wikipedia.org/wiki/Beyonc%C3%A9', '{"instagram": "beyonce"}'),
('Ariana Grande', 'musician', 'arianagrande', ARRAY['ariana', 'grande'], 'ultra_high', 'https://en.wikipedia.org/wiki/Ariana_Grande', '{"twitter": "ArianaGrande", "instagram": "arianagrande"}'),
('The Rock', 'actor', 'therock', ARRAY['rock', 'dwaynejohnson', 'dwayne'], 'ultra_high', 'https://en.wikipedia.org/wiki/Dwayne_Johnson', '{"twitter": "TheRock", "instagram": "therock"}'),
('Robert Downey Jr', 'actor', 'robertdowneyjr', ARRAY['rdj', 'ironman', 'downey'], 'ultra_high', 'https://en.wikipedia.org/wiki/Robert_Downey_Jr.', '{"twitter": "RobertDowneyJr", "instagram": "robertdowneyjr"}'),
('Leonardo DiCaprio', 'actor', 'leonardodicaprio', ARRAY['leonardo', 'dicaprio', 'leo'], 'ultra_high', 'https://en.wikipedia.org/wiki/Leonardo_DiCaprio', '{"twitter": "LeoDiCaprio", "instagram": "leonardodicaprio"}'),
('Will Smith', 'actor', 'willsmith', ARRAY['will', 'smith'], 'ultra_high', 'https://en.wikipedia.org/wiki/Will_Smith', '{"twitter": "willsmith", "instagram": "willsmith"}'),
('Oprah Winfrey', 'celebrity', 'oprah', ARRAY['oprahwinfrey', 'oprah'], 'ultra_high', 'https://en.wikipedia.org/wiki/Oprah_Winfrey', '{"twitter": "Oprah", "instagram": "oprah"}'),

-- Athletes
('LeBron James', 'athlete', 'lebronjames', ARRAY['lebron', 'kingjames'], 'ultra_high', 'https://en.wikipedia.org/wiki/LeBron_James', '{"twitter": "KingJames", "instagram": "kingjames"}'),
('Cristiano Ronaldo', 'athlete', 'cristiano', ARRAY['ronaldo', 'cr7'], 'ultra_high', 'https://en.wikipedia.org/wiki/Cristiano_Ronaldo', '{"twitter": "Cristiano", "instagram": "cristiano"}'),
('Lionel Messi', 'athlete', 'leomessi', ARRAY['messi', 'lionel'], 'ultra_high', 'https://en.wikipedia.org/wiki/Lionel_Messi', '{"instagram": "leomessi"}'),
('Serena Williams', 'athlete', 'serenawilliams', ARRAY['serena', 'williams'], 'ultra_high', 'https://en.wikipedia.org/wiki/Serena_Williams', '{"twitter": "serenawilliams", "instagram": "serenawilliams"}'),
('Tom Brady', 'athlete', 'tombrady', ARRAY['tom', 'brady', 'tb12'], 'ultra_high', 'https://en.wikipedia.org/wiki/Tom_Brady', '{"twitter": "TomBrady", "instagram": "tombrady"}'),
('Stephen Curry', 'athlete', 'stephencurry', ARRAY['steph', 'curry'], 'ultra_high', 'https://en.wikipedia.org/wiki/Stephen_Curry', '{"twitter": "StephenCurry30", "instagram": "stephencurry30"}'),

-- Scientists & Intellectuals
('Neil deGrasse Tyson', 'scientist', 'neiltyson', ARRAY['neil', 'tyson', 'astrophysicist'], 'high', 'https://en.wikipedia.org/wiki/Neil_deGrasse_Tyson', '{"twitter": "neiltyson"}'),
('Michio Kaku', 'scientist', 'michiokaku', ARRAY['michio', 'kaku'], 'high', 'https://en.wikipedia.org/wiki/Michio_Kaku', '{"twitter": "michiokaku"}'),
('Bill Nye', 'scientist', 'billnye', ARRAY['bill', 'nye', 'scienceguy'], 'high', 'https://en.wikipedia.org/wiki/Bill_Nye', '{"twitter": "BillNye"}'),

-- Journalists & Media
('Anderson Cooper', 'journalist', 'andersoncooper', ARRAY['anderson', 'cooper', 'cnn'], 'high', 'https://en.wikipedia.org/wiki/Anderson_Cooper', '{"twitter": "andersoncooper", "instagram": "andersoncooper"}'),
('Christiane Amanpour', 'journalist', 'amanpour', ARRAY['christiane', 'amanpour'], 'high', 'https://en.wikipedia.org/wiki/Christiane_Amanpour', '{"twitter": "amanpour"}'),
('Tucker Carlson', 'journalist', 'tuckercarlson', ARRAY['tucker', 'carlson'], 'high', 'https://en.wikipedia.org/wiki/Tucker_Carlson', '{"twitter": "TuckerCarlson"}'),

-- Authors
('Stephen King', 'author', 'stephenking', ARRAY['stephen', 'king'], 'high', 'https://en.wikipedia.org/wiki/Stephen_King', '{"twitter": "StephenKing"}'),
('J.K. Rowling', 'author', 'jkrowling', ARRAY['jk', 'rowling', 'harrypotter'], 'ultra_high', 'https://en.wikipedia.org/wiki/J._K._Rowling', '{"twitter": "jk_rowling"}'),
('Malcolm Gladwell', 'author', 'malcolmgladwell', ARRAY['malcolm', 'gladwell'], 'high', 'https://en.wikipedia.org/wiki/Malcolm_Gladwell', '{"twitter": "Gladwell"}'),

-- Influencers & Content Creators
('MrBeast', 'influencer', 'mrbeast', ARRAY['beast', 'jimmy'], 'high', 'https://en.wikipedia.org/wiki/MrBeast', '{"twitter": "MrBeast", "instagram": "mrbeast"}'),
('PewDiePie', 'influencer', 'pewdiepie', ARRAY['pewdie', 'felix'], 'high', 'https://en.wikipedia.org/wiki/PewDiePie', '{"twitter": "pewdiepie", "instagram": "pewdiepie"}'),
('Marques Brownlee', 'influencer', 'mkbhd', ARRAY['marques', 'brownlee', 'mkb'], 'high', 'https://en.wikipedia.org/wiki/Marques_Brownlee', '{"twitter": "MKBHD", "instagram": "mkbhd"}');

-- ============== WELL-KNOWN COMPANIES ==============

INSERT INTO well_known_companies (name, legal_name, industry, company_type, preferred_handle, alternative_handles, stock_symbol, founded_year, headquarters_country, website, verification_level, required_documents) VALUES

-- Major Tech Companies
('Apple', 'Apple Inc.', 'Technology', 'public_company', 'apple', ARRAY['appleofficial', 'applesupport'], 'AAPL', 1976, 'US', 'https://apple.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Microsoft', 'Microsoft Corporation', 'Technology', 'public_company', 'microsoft', ARRAY['microsoftofficial', 'msft'], 'MSFT', 1975, 'US', 'https://microsoft.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Google', 'Alphabet Inc.', 'Technology', 'public_company', 'google', ARRAY['googleofficial', 'alphabet'], 'GOOGL', 1998, 'US', 'https://google.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Meta', 'Meta Platforms, Inc.', 'Technology', 'public_company', 'meta', ARRAY['facebook', 'metaofficial'], 'META', 2004, 'US', 'https://meta.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Amazon', 'Amazon.com, Inc.', 'E-commerce', 'public_company', 'amazon', ARRAY['amazonofficial', 'aws'], 'AMZN', 1994, 'US', 'https://amazon.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Tesla', 'Tesla, Inc.', 'Automotive', 'public_company', 'tesla', ARRAY['teslaofficial', 'teslamotors'], 'TSLA', 2003, 'US', 'https://tesla.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('SpaceX', 'Space Exploration Technologies Corp.', 'Aerospace', 'private_company', 'spacex', ARRAY['spacexofficial'], NULL, 2002, 'US', 'https://spacex.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),
('Netflix', 'Netflix, Inc.', 'Entertainment', 'public_company', 'netflix', ARRAY['netflixofficial'], 'NFLX', 1997, 'US', 'https://netflix.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('NVIDIA', 'NVIDIA Corporation', 'Technology', 'public_company', 'nvidia', ARRAY['nvidiaofficial'], 'NVDA', 1993, 'US', 'https://nvidia.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('OpenAI', 'OpenAI, L.P.', 'Technology', 'private_company', 'openai', ARRAY['openaiofficial'], NULL, 2015, 'US', 'https://openai.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),

-- Financial Services
('JPMorgan Chase', 'JPMorgan Chase & Co.', 'Financial Services', 'public_company', 'jpmorgan', ARRAY['jpmorganchase', 'chase'], 'JPM', 1799, 'US', 'https://jpmorganchase.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'banking_license']),
('Goldman Sachs', 'The Goldman Sachs Group, Inc.', 'Financial Services', 'public_company', 'goldmansachs', ARRAY['goldman'], 'GS', 1869, 'US', 'https://goldmansachs.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'banking_license']),
('BlackRock', 'BlackRock, Inc.', 'Financial Services', 'public_company', 'blackrock', ARRAY['blackrockofficial'], 'BLK', 1988, 'US', 'https://blackrock.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),

-- Media & Entertainment
('Disney', 'The Walt Disney Company', 'Entertainment', 'public_company', 'disney', ARRAY['disneyofficial', 'waltdisney'], 'DIS', 1923, 'US', 'https://disney.com', 'ultra_high', ARRAY['business_registration', 'sec_filing', 'domain_ownership']),
('Warner Bros', 'Warner Bros. Discovery, Inc.', 'Entertainment', 'public_company', 'warnerbros', ARRAY['warner'], 'WBD', 1923, 'US', 'https://warnerbros.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),
('Sony', 'Sony Group Corporation', 'Technology', 'public_company', 'sony', ARRAY['sonyofficial'], 'SONY', 1946, 'JP', 'https://sony.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),

-- Automotive
('Toyota', 'Toyota Motor Corporation', 'Automotive', 'public_company', 'toyota', ARRAY['toyotaofficial'], 'TM', 1937, 'JP', 'https://toyota.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),
('BMW', 'Bayerische Motoren Werke AG', 'Automotive', 'public_company', 'bmw', ARRAY['bmwofficial'], 'BMWYY', 1916, 'DE', 'https://bmw.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),
('Mercedes', 'Mercedes-Benz Group AG', 'Automotive', 'public_company', 'mercedes', ARRAY['mercedesbenz'], 'MBGYY', 1926, 'DE', 'https://mercedes-benz.com', 'ultra_high', ARRAY['business_registration', 'domain_ownership']),

-- Retail & Consumer
('Walmart', 'Walmart Inc.', 'Retail', 'public_company', 'walmart', ARRAY['walmartofficial'], 'WMT', 1962, 'US', 'https://walmart.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),
('Target', 'Target Corporation', 'Retail', 'public_company', 'target', ARRAY['targetofficial'], 'TGT', 1902, 'US', 'https://target.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),
('Starbucks', 'Starbucks Corporation', 'Food & Beverage', 'public_company', 'starbucks', ARRAY['starbucksofficial'], 'SBUX', 1971, 'US', 'https://starbucks.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),
('McDonald''s', 'McDonald''s Corporation', 'Food & Beverage', 'public_company', 'mcdonalds', ARRAY['mcdonald'], 'MCD', 1940, 'US', 'https://mcdonalds.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),
('Coca Cola', 'The Coca-Cola Company', 'Food & Beverage', 'public_company', 'cocacola', ARRAY['coke', 'coca'], 'KO', 1892, 'US', 'https://coca-cola.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),

-- Airlines
('American Airlines', 'American Airlines Group Inc.', 'Transportation', 'public_company', 'americanair', ARRAY['american', 'aa'], 'AAL', 1930, 'US', 'https://aa.com', 'high', ARRAY['business_registration', 'airline_license']),
('Delta', 'Delta Air Lines, Inc.', 'Transportation', 'public_company', 'delta', ARRAY['deltaair'], 'DAL', 1924, 'US', 'https://delta.com', 'high', ARRAY['business_registration', 'airline_license']),
('Emirates', 'Emirates Airline', 'Transportation', 'private_company', 'emirates', ARRAY['emiratesair'], NULL, 1985, 'AE', 'https://emirates.com', 'high', ARRAY['business_registration', 'airline_license']),

-- News & Media
('CNN', 'Cable News Network', 'Media', 'private_company', 'cnn', ARRAY['cnnofficial'], NULL, 1980, 'US', 'https://cnn.com', 'ultra_high', ARRAY['business_registration', 'media_license', 'domain_ownership']),
('BBC', 'British Broadcasting Corporation', 'Media', 'public_company', 'bbc', ARRAY['bbcofficial'], NULL, 1922, 'GB', 'https://bbc.com', 'ultra_high', ARRAY['government_charter', 'domain_ownership']),
('Fox News', 'Fox News Media', 'Media', 'private_company', 'foxnews', ARRAY['fox'], NULL, 1996, 'US', 'https://foxnews.com', 'ultra_high', ARRAY['business_registration', 'media_license']),
('The New York Times', 'The New York Times Company', 'Media', 'public_company', 'nytimes', ARRAY['newyorktimes'], 'NYT', 1851, 'US', 'https://nytimes.com', 'ultra_high', ARRAY['business_registration', 'sec_filing']),

-- Sports Organizations
('NBA', 'National Basketball Association', 'Sports', 'nonprofit', 'nba', ARRAY['basketball'], NULL, 1946, 'US', 'https://nba.com', 'ultra_high', ARRAY['organization_charter', 'domain_ownership']),
('NFL', 'National Football League', 'Sports', 'nonprofit', 'nfl', ARRAY['football'], NULL, 1920, 'US', 'https://nfl.com', 'ultra_high', ARRAY['organization_charter', 'domain_ownership']),
('FIFA', 'Fédération Internationale de Football Association', 'Sports', 'nonprofit', 'fifa', ARRAY['football'], NULL, 1904, 'CH', 'https://fifa.com', 'ultra_high', ARRAY['organization_charter', 'domain_ownership']),

-- Government Agencies (Examples)
('NASA', 'National Aeronautics and Space Administration', 'Government', 'government', 'nasa', ARRAY['nasaofficial'], NULL, 1958, 'US', 'https://nasa.gov', 'ultra_high', ARRAY['government_charter', 'domain_ownership']),
('FBI', 'Federal Bureau of Investigation', 'Government', 'government', 'fbi', ARRAY['fbiofficial'], NULL, 1908, 'US', 'https://fbi.gov', 'ultra_high', ARRAY['government_charter', 'domain_ownership']),

-- Universities
('Harvard', 'Harvard University', 'Educational', 'educational', 'harvard', ARRAY['harvarduniversity'], NULL, 1636, 'US', 'https://harvard.edu', 'ultra_high', ARRAY['educational_charter', 'domain_ownership']),
('MIT', 'Massachusetts Institute of Technology', 'Educational', 'educational', 'mit', ARRAY['mitofficial'], NULL, 1861, 'US', 'https://mit.edu', 'ultra_high', ARRAY['educational_charter', 'domain_ownership']),
('Stanford', 'Stanford University', 'Educational', 'educational', 'stanford', ARRAY['stanforduniv'], NULL, 1885, 'US', 'https://stanford.edu', 'ultra_high', ARRAY['educational_charter', 'domain_ownership']);

-- ============== PROTECTED HANDLES SETUP ==============

-- Create protected handles for all well-known figures
INSERT INTO protected_handles (original_handle, protected_entity_type, protected_entity_id, similarity_threshold)
SELECT preferred_handle, 'well_known_figure', id, 0.85
FROM well_known_figures;

-- Create protected handles for all alternative handles of well-known figures
INSERT INTO protected_handles (original_handle, protected_entity_type, protected_entity_id, similarity_threshold)
SELECT UNNEST(alternative_handles), 'well_known_figure', id, 0.90
FROM well_known_figures 
WHERE alternative_handles IS NOT NULL AND array_length(alternative_handles, 1) > 0;

-- Create protected handles for all well-known companies
INSERT INTO protected_handles (original_handle, protected_entity_type, protected_entity_id, similarity_threshold)
SELECT preferred_handle, 'company', id, 0.85
FROM well_known_companies;

-- Create protected handles for all alternative handles of well-known companies
INSERT INTO protected_handles (original_handle, protected_entity_type, protected_entity_id, similarity_threshold)
SELECT UNNEST(alternative_handles), 'company', id, 0.90
FROM well_known_companies 
WHERE alternative_handles IS NOT NULL AND array_length(alternative_handles, 1) > 0;

-- Create protected handles for all reserved handles
INSERT INTO protected_handles (original_handle, protected_entity_type, protected_entity_id, similarity_threshold)
SELECT handle, 'reserved', id, 0.95
FROM reserved_handles;