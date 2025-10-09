-- Entativa ID: Media, Journalism & Academic Protection Seed Data
-- News Organizations, Journalists, Scientists, Universities
-- Data Version: 2.0 - Media and Academic Protection System

-- ============== MAJOR NEWS ORGANIZATIONS ==============

INSERT INTO protected_media_organizations (
    organization_name, handle, aliases, category, media_type, country, founded_year,
    circulation, viewership, parent_company, major_shows, notable_journalists, 
    official_websites, social_media, verification_level, protection_reason, added_by
) VALUES 

-- Major US News Networks
('Cable News Network', 'cnn', ARRAY['cnn'], 'tv_network', 'broadcast', 'United States', 1980,
 null, 75000000, 'Warner Bros. Discovery', ARRAY['Anderson Cooper 360', 'The Situation Room', 'CNN Tonight'],
 ARRAY['Anderson Cooper', 'Christiane Amanpour', 'Wolf Blitzer'], ARRAY['https://www.cnn.com'],
 '{"twitter": "@cnn", "instagram": "@cnn", "youtube": "@cnn"}', 'premium', 'Major 24-hour news network', 'system'),

('Fox News Channel', 'foxnews', ARRAY['fox'], 'tv_network', 'broadcast', 'United States', 1996,
 null, 120000000, 'Fox Corporation', ARRAY['Tucker Carlson Tonight', 'Hannity', 'The Five'],
 ARRAY['Tucker Carlson', 'Sean Hannity', 'Laura Ingraham'], ARRAY['https://www.foxnews.com'],
 '{"twitter": "@foxnews", "instagram": "@foxnews", "youtube": "@foxnews"}', 'premium', 'Leading cable news network', 'system'),

('MSNBC', 'msnbc', ARRAY['msnbc'], 'tv_network', 'broadcast', 'United States', 1996,
 null, 80000000, 'NBCUniversal', ARRAY['The Rachel Maddow Show', 'Morning Joe', 'All In'],
 ARRAY['Rachel Maddow', 'Joe Scarborough', 'Chris Hayes'], ARRAY['https://www.msnbc.com'],
 '{"twitter": "@msnbc", "instagram": "@msnbc", "youtube": "@msnbc"}', 'premium', 'Progressive news network', 'system'),

-- Print Media
('The New York Times', 'nytimes', ARRAY['nyt'], 'newspaper', 'print', 'United States', 1851,
 8500000, null, 'The New York Times Company', ARRAY['The Daily', 'The New York Times Podcast'],
 ARRAY['Maggie Haberman', 'Nicholas Kristof', 'Maureen Dowd'], ARRAY['https://www.nytimes.com'],
 '{"twitter": "@nytimes", "instagram": "@nytimes", "youtube": "@nytimes"}', 'premium', 'Newspaper of record', 'system'),

('The Washington Post', 'washingtonpost', ARRAY['wapo'], 'newspaper', 'print', 'United States', 1877,
 3000000, null, 'Nash Holdings (Jeff Bezos)', ARRAY[], ARRAY['Bob Woodward', 'Carol Leonnig', 'David Ignatius'],
 ARRAY['https://www.washingtonpost.com'], '{"twitter": "@washingtonpost", "instagram": "@washingtonpost", "youtube": "@washingtonpost"}',
 'premium', 'Prestigious national newspaper', 'system'),

('The Wall Street Journal', 'wsj', ARRAY['wsj'], 'newspaper', 'print', 'United States', 1889,
 2800000, null, 'News Corporation', ARRAY[], ARRAY['Peggy Noonan', 'Kimberley Strassel'],
 ARRAY['https://www.wsj.com'], '{"twitter": "@wsj", "instagram": "@wsj", "youtube": "@wsj"}',
 'premium', 'Leading business newspaper', 'system'),

-- Digital Media
('BuzzFeed', 'buzzfeed', ARRAY['buzzfeed'], 'digital_media', 'digital', 'United States', 2006,
 null, 50000000, 'BuzzFeed Inc.', ARRAY['BuzzFeed News', 'Tasty'], ARRAY['Ben Smith', 'Anne Helen Petersen'],
 ARRAY['https://www.buzzfeed.com'], '{"twitter": "@buzzfeed", "instagram": "@buzzfeed", "youtube": "@buzzfeed", "tiktok": "@buzzfeed"}',
 'premium', 'Digital media and news platform', 'system'),

('Vice Media', 'vice', ARRAY['vice'], 'digital_media', 'digital', 'United States', 1994,
 null, 30000000, 'Vice Media Group', ARRAY['Vice News Tonight', 'Vice on HBO'], ARRAY['Shane Smith', 'Suroosh Alvi'],
 ARRAY['https://www.vice.com'], '{"twitter": "@vice", "instagram": "@vice", "youtube": "@vice"}',
 'premium', 'Alternative digital media company', 'system'),

-- International Media
('British Broadcasting Corporation', 'bbc', ARRAY['bbc'], 'tv_network', 'broadcast', 'United Kingdom', 1922,
 null, 400000000, 'BBC', ARRAY['BBC News', 'BBC World Service', 'Panorama'], ARRAY['Huw Edwards', 'Emily Maitlis'],
 ARRAY['https://www.bbc.com'], '{"twitter": "@bbc", "instagram": "@bbc", "youtube": "@bbc"}',
 'premium', 'UK public service broadcaster', 'system'),

('Reuters', 'reuters', ARRAY['reuters'], 'news_agency', 'digital', 'United Kingdom', 1851,
 null, null, 'Thomson Reuters', ARRAY[], ARRAY['Various correspondents worldwide'],
 ARRAY['https://www.reuters.com'], '{"twitter": "@reuters", "instagram": "@reuters", "youtube": "@reuters"}',
 'premium', 'International news agency', 'system'),

('Al Jazeera', 'aljazeera', ARRAY['aljazeeraenglish'], 'tv_network', 'broadcast', 'Qatar', 1996,
 null, 350000000, 'Al Jazeera Media Network', ARRAY['Al Jazeera English', 'The Stream'], ARRAY['Mehdi Hasan', 'Riz Khan'],
 ARRAY['https://www.aljazeera.com'], '{"twitter": "@aljazeera", "instagram": "@aljazeera", "youtube": "@aljazeeraenglish"}',
 'premium', 'International news network', 'system');

-- ============== PROMINENT JOURNALISTS ==============

INSERT INTO protected_journalists (
    full_name, handle, aliases, category, beat, current_employer, employer_history, 
    nationality, birth_year, major_stories, awards, books_authored, social_media, 
    verification_level, protection_reason, added_by
) VALUES 

-- TV News Anchors & Personalities
('Anderson Hays Cooper', 'andersoncooper', ARRAY['anderson', 'cooper'], 'anchor', 'politics', 'CNN',
 ARRAY['CNN (2001-present)', 'Channel One News'], 'American', 1967,
 ARRAY['Hurricane Katrina coverage', 'Arab Spring reporting', 'Trump presidency'], ARRAY['Emmy Awards', 'Peabody Award'],
 ARRAY['Dispatches from the Edge'], '{"twitter": "@andersoncooper", "instagram": "@andersoncooper"}',
 'premium', 'Prominent CNN anchor and journalist', 'system'),

('Rachel Anne Maddow', 'maddow', ARRAY['rachel', 'maddow'], 'anchor', 'politics', 'MSNBC',
 ARRAY['MSNBC (2008-present)', 'Air America Radio'], 'American', 1973,
 ARRAY['Trump-Russia investigation', 'COVID-19 reporting'], ARRAY['Emmy Award', 'Walter Cronkite Award'],
 ARRAY['Drift', 'Blowout'], '{"twitter": "@maddow", "instagram": "@maddow"}',
 'premium', 'MSNBC primetime host and political commentator', 'system'),

('Tucker Swanson McNear Carlson', 'tuckercarlson', ARRAY['tucker'], 'anchor', 'politics', 'Former Fox News',
 ARRAY['Fox News (2016-2023)', 'CNN', 'MSNBC'], 'American', 1969,
 ARRAY['Conservative political commentary'], ARRAY[], ARRAY['Ship of Fools', 'The Long Slide'],
 '{"twitter": "@tuckercarlson", "instagram": null}', 'premium', 'Former Fox News primetime host', 'system'),

-- Print Journalists
('Robert Upshur Woodward', 'realwoodward', ARRAY['bobwoodward', 'woodward'], 'reporter', 'investigative', 'The Washington Post',
 ARRAY['Washington Post (1971-present)'], 'American', 1943,
 ARRAY['Watergate investigation', 'Trump presidency books'], ARRAY['Pulitzer Prize', 'Presidential Medal of Freedom'],
 ARRAY['All the President''s Men', 'Fear', 'Rage'], '{"twitter": null, "instagram": null}',
 'premium', 'Legendary investigative journalist', 'system'),

('Maggie Lindsy Haberman', 'maggienyt', ARRAY['maggie'], 'reporter', 'politics', 'The New York Times',
 ARRAY['New York Times (2015-present)', 'Politico', 'New York Post'], 'American', 1973,
 ARRAY['Trump presidency coverage'], ARRAY['Pulitzer Prize'], ARRAY['Confidence Man'],
 '{"twitter": "@maggienyt", "instagram": null}', 'premium', 'White House correspondent', 'system'),

-- International Journalists
('Christiane Maria Helenius Amanpour', 'camanpour', ARRAY['christiane'], 'correspondent', 'international', 'CNN International',
 ARRAY['CNN (1983-present)', 'ABC News'], 'British-Iranian', 1958,
 ARRAY['Gulf War coverage', 'Bosnian War', 'International conflicts'], ARRAY['Emmy Awards', 'Peabody Award'],
 ARRAY[], '{"twitter": "@camanpour", "instagram": "@camanpour"}',
 'premium', 'Chief international anchor CNN', 'system'),

-- Digital Media Journalists
('Mehdi Raza Hasan', 'mehdirhasan', ARRAY['mehdi'], 'anchor', 'politics', 'MSNBC',
 ARRAY['MSNBC (2021-present)', 'Al Jazeera English', 'The Intercept'], 'British-Indian', 1979,
 ARRAY['Political interviews', 'International affairs'], ARRAY[], ARRAY['Winning the War of Words'],
 '{"twitter": "@mehdirhasan", "instagram": "@mehdirhasan"}', 'premium', 'MSNBC host and political journalist', 'system');

-- ============== NOBEL LAUREATES & SCIENTISTS ==============

INSERT INTO protected_scientists (
    full_name, handle, aliases, category, field_of_study, current_institution, institution_history,
    nationality, birth_year, nobel_prize_year, nobel_category, major_discoveries, publications_notable,
    awards, social_media, verification_level, protection_reason, added_by
) VALUES 

-- Physics Nobel Laureates
('Albert Einstein', 'einstein', ARRAY['albert'], 'physicist', 'Theoretical Physics', 'Princeton (deceased)', 
 ARRAY['ETH Zurich', 'Princeton University'], 'German-American', 1879, 1921, 'Physics',
 ARRAY['Theory of Relativity', 'Photoelectric Effect', 'Mass-Energy Equivalence'], 
 ARRAY['Special Relativity', 'General Relativity'], ARRAY['Nobel Prize Physics'],
 '{"twitter": null, "instagram": null}', 'premium', 'Greatest physicist of modern era', 'system'),

('Stephen William Hawking', 'stephenhawking', ARRAY['hawking'], 'physicist', 'Theoretical Physics', 'Cambridge (deceased)',
 ARRAY['Cambridge University', 'Oxford University'], 'British', 1942, null, null,
 ARRAY['Black hole thermodynamics', 'Hawking radiation'], ARRAY['A Brief History of Time'], 
 ARRAY['CBE', 'Presidential Medal of Freedom'], '{"twitter": "@stephenhawking", "instagram": null}',
 'premium', 'Renowned theoretical physicist', 'system'),

-- Medicine Nobel Laureates
('Jennifer Anne Doudna', 'jenniferdoudna', ARRAY['doudna'], 'biochemist', 'Biochemistry', 'UC Berkeley',
 ARRAY['UC Berkeley', 'Harvard'], 'American', 1964, 2020, 'Chemistry',
 ARRAY['CRISPR gene editing'], ARRAY['CRISPR papers'], ARRAY['Nobel Prize Chemistry', 'Breakthrough Prize'],
 '{"twitter": "@jenniferdoudna", "instagram": null}', 'premium', 'CRISPR gene editing pioneer', 'system'),

('Katalin Karikó', 'kkariko', ARRAY['kariko'], 'biochemist', 'Biochemistry', 'BioNTech',
 ARRAY['University of Pennsylvania', 'BioNTech'], 'Hungarian-American', 1955, 2023, 'Medicine',
 ARRAY['mRNA vaccine technology'], ARRAY['mRNA research papers'], ARRAY['Nobel Prize Medicine'],
 '{"twitter": "@kkariko", "instagram": null}', 'premium', 'mRNA vaccine technology pioneer', 'system'),

-- Current Leading Scientists
('Anthony Stephen Fauci', 'drfauci', ARRAY['fauci'], 'immunologist', 'Immunology', 'Georgetown University',
 ARRAY['NIAID Director (1984-2022)', 'NIH'], 'American', 1940, null, null,
 ARRAY['HIV/AIDS research', 'COVID-19 response'], ARRAY['Immunology papers'], 
 ARRAY['Presidential Medal of Freedom'], '{"twitter": null, "instagram": null}',
 'premium', 'Leading immunologist and former NIAID director', 'system'),

('Elon Musk', 'elonmusk', ARRAY['musk'], 'entrepreneur', 'Engineering/Technology', 'SpaceX/Tesla',
 ARRAY['University of Pennsylvania'], 'South African-American', 1971, null, null,
 ARRAY['Electric vehicles', 'Space exploration', 'Neural interfaces'], ARRAY['Technical papers'],
 ARRAY['Various engineering awards'], '{"twitter": "@elonmusk", "instagram": "@elonmusk"}',
 'premium', 'Technology entrepreneur and engineer', 'system');

-- ============== ACADEMIC INSTITUTIONS ==============

INSERT INTO protected_academic_institutions (
    institution_name, handle, aliases, category, institution_type, country, founded_year,
    student_population, endowment_billions, ranking_us, ranking_world, notable_alumni,
    major_programs, research_areas, official_websites, social_media, verification_level, protection_reason, added_by
) VALUES 

-- Ivy League Universities
('Harvard University', 'harvard', ARRAY['harvard'], 'university', 'private', 'United States', 1636,
 23000, 53.2, 2, 1, ARRAY['Barack Obama', 'Mark Zuckerberg', 'Bill Gates'], 
 ARRAY['Harvard Medical School', 'Harvard Business School', 'Harvard Law School'],
 ARRAY['Medicine', 'Business', 'Law', 'Liberal Arts'], ARRAY['https://www.harvard.edu'],
 '{"twitter": "@harvard", "instagram": "@harvard", "youtube": "@harvard"}', 'premium', 'Oldest and most prestigious US university', 'system'),

('Stanford University', 'stanford', ARRAY['stanford'], 'university', 'private', 'United States', 1885,
 17000, 36.5, 3, 3, ARRAY['Elon Musk', 'Larry Page', 'Sergey Brin'],
 ARRAY['Stanford Medical School', 'Stanford Business School', 'Computer Science'],
 ARRAY['Technology', 'Medicine', 'Engineering'], ARRAY['https://www.stanford.edu'],
 '{"twitter": "@stanford", "instagram": "@stanford", "youtube": "@stanford"}', 'premium', 'Leading technology and innovation university', 'system'),

('Massachusetts Institute of Technology', 'mitpics', ARRAY['mit'], 'university', 'private', 'United States', 1861,
 11000, 27.4, 4, 2, ARRAY['Buzz Aldrin', 'Kofi Annan', 'Ben Bernanke'],
 ARRAY['MIT Sloan', 'Engineering', 'Computer Science'], ARRAY['Engineering', 'Technology', 'Science'],
 ARRAY['https://web.mit.edu'], '{"twitter": "@mitpics", "instagram": "@mitpics", "youtube": "@mit"}',
 'premium', 'Premier science and technology institute', 'system'),

('Yale University', 'yale', ARRAY['yale'], 'university', 'private', 'United States', 1701,
 13000, 42.3, 5, 18, ARRAY['George W. Bush', 'Hillary Clinton', 'Meryl Streep'],
 ARRAY['Yale Law School', 'Yale Medical School', 'Liberal Arts'], ARRAY['Law', 'Medicine', 'Arts'],
 ARRAY['https://www.yale.edu'], '{"twitter": "@yale", "instagram": "@yale", "youtube": "@yale"}',
 'premium', 'Historic Ivy League institution', 'system'),

-- Public Universities
('University of California Berkeley', 'ucberkeley', ARRAY['berkeley', 'cal'], 'university', 'public', 'United States', 1868,
 45000, 6.9, 20, 10, ARRAY['Steve Wozniak', 'Eric Schmidt', 'Jennifer Doudna'],
 ARRAY['Engineering', 'Computer Science', 'Business'], ARRAY['Technology', 'Science', 'Engineering'],
 ARRAY['https://www.berkeley.edu'], '{"twitter": "@ucberkeley", "instagram": "@ucberkeley", "youtube": "@ucberkeleyofficial"}',
 'premium', 'Top public research university', 'system'),

-- International Universities
('University of Oxford', 'uniofoxford', ARRAY['oxford'], 'university', 'public', 'United Kingdom', 1096,
 24000, 8.1, null, 5, ARRAY['Stephen Hawking', 'Tony Blair', 'J.R.R. Tolkien'],
 ARRAY['Rhodes Scholarships', 'Medicine', 'Liberal Arts'], ARRAY['Arts', 'Sciences', 'Medicine'],
 ARRAY['https://www.ox.ac.uk'], '{"twitter": "@uniofoxford", "instagram": "@oxford_uni", "youtube": "@oxforduniversity"}',
 'premium', 'World''s oldest English-speaking university', 'system'),

('University of Cambridge', 'cambridge_uni', ARRAY['cambridge'], 'university', 'public', 'United Kingdom', 1209,
 24000, 9.0, null, 3, ARRAY['Stephen Hawking', 'Charles Darwin', 'Isaac Newton'],
 ARRAY['Natural Sciences', 'Mathematics', 'Engineering'], ARRAY['Sciences', 'Mathematics', 'Engineering'],
 ARRAY['https://www.cam.ac.uk'], '{"twitter": "@cambridge_uni", "instagram": "@cambridgeuniversity", "youtube": "@cambridgeuniversity"}',
 'premium', 'Historic Cambridge University', 'system');

-- ============== VERIFICATION UPDATES ==============

-- Update verification levels for major institutions and figures
UPDATE protected_media_organizations SET verification_level = 'premium' WHERE viewership > 50000000 OR circulation > 1000000;
UPDATE protected_journalists SET verification_level = 'premium' WHERE array_length(awards, 1) > 0 OR 'Pulitzer Prize' = ANY(awards);
UPDATE protected_scientists SET verification_level = 'premium' WHERE nobel_prize_year IS NOT NULL OR 'Nobel Prize' = ANY(string_to_array(awards::text, ','));
UPDATE protected_academic_institutions SET verification_level = 'premium' WHERE ranking_us <= 20 OR ranking_world <= 20 OR endowment_billions > 10.0;

-- Add creation timestamps
UPDATE protected_media_organizations SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_journalists SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_scientists SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_academic_institutions SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;