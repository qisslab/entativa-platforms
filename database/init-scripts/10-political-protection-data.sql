-- Entativa ID: Political & Government Protection Seed Data
-- World Leaders, Politicians, Government Officials, and Organizations
-- Data Version: 2.0 - Government and Political Protection System

-- ============== WORLD LEADERS & HEADS OF STATE ==============

INSERT INTO protected_political_figures (
    full_name, handle, title, aliases, category, country, political_party, office_current, 
    office_history, birth_year, major_policies, social_media, official_websites, 
    verification_level, protection_reason, added_by
) VALUES 

-- Current World Leaders (as of 2024)
('Joseph Robinette Biden Jr.', 'potus', 'President of the United States', ARRAY['biden', 'joebiden', 'president'], 'head_of_state', 'United States', 'Democratic Party', 'President of the United States',
 ARRAY['Vice President (2009-2017)', 'U.S. Senator Delaware (1973-2009)'], 1942, ARRAY['Infrastructure Investment', 'Climate Action', 'COVID Relief'],
 '{"twitter": "@potus", "instagram": "@potus"}', ARRAY['https://www.whitehouse.gov'], 'premium', 'President of the United States', 'system'),

('Vladimir Vladimirovich Putin', 'kremlinrussia', 'President of Russia', ARRAY['putin', 'vladimir'], 'head_of_state', 'Russia', 'United Russia', 'President of the Russian Federation',
 ARRAY['Prime Minister (1999-2000, 2008-2012)', 'President (2000-2008, 2012-present)'], 1952, ARRAY['Economic modernization', 'Military reform'],
 '{"twitter": null, "instagram": null}', ARRAY['http://kremlin.ru'], 'premium', 'President of Russian Federation', 'system'),

('Xi Jinping', 'chinagov', 'General Secretary of CPC', ARRAY['xi', 'xijinping'], 'head_of_state', 'China', 'Communist Party of China', 'General Secretary CPC & President PRC',
 ARRAY['Vice President (2008-2013)', 'Various party positions'], 1953, ARRAY['Belt and Road Initiative', 'Common Prosperity', 'Zero-COVID'],
 '{"twitter": null, "instagram": null}', ARRAY['http://www.gov.cn'], 'premium', 'Leader of People''s Republic of China', 'system'),

('Emmanuel Jean-Michel Frédéric Macron', 'emmanuelmacron', 'President of France', ARRAY['macron', 'emmanuel'], 'head_of_state', 'France', 'Renaissance', 'President of the French Republic',
 ARRAY['Minister of Economy (2014-2016)'], 1977, ARRAY['European integration', 'Climate action', 'Economic reform'],
 '{"twitter": "@emmanuelmacron", "instagram": "@emmanuelmacron"}', ARRAY['https://www.elysee.fr'], 'premium', 'President of France', 'system'),

('Charles Philip Arthur George', 'theroyalfamily', 'King of the United Kingdom', ARRAY['kingcharles', 'charles'], 'head_of_state', 'United Kingdom', 'Non-partisan', 'King of the United Kingdom',
 ARRAY['Prince of Wales (1958-2022)'], 1948, ARRAY['Environmental conservation', 'Climate action'],
 '{"twitter": "@royalfamily", "instagram": "@theroyalfamily"}', ARRAY['https://www.royal.uk'], 'premium', 'King of the United Kingdom', 'system'),

-- Other Major Leaders
('Narendra Damodardas Modi', 'narendramodi', 'Prime Minister of India', ARRAY['modi', 'narendramodi'], 'head_of_government', 'India', 'Bharatiya Janata Party', 'Prime Minister of India',
 ARRAY['Chief Minister Gujarat (2001-2014)'], 1950, ARRAY['Digital India', 'Make in India', 'Swachh Bharat'],
 '{"twitter": "@narendramodi", "instagram": "@narendramodi"}', ARRAY['https://www.pmindia.gov.in'], 'premium', 'Prime Minister of India', 'system'),

('Jair Messias Bolsonaro', 'jairbolsonaro', 'Former President of Brazil', ARRAY['bolsonaro'], 'former_head_of_state', 'Brazil', 'Liberal Party', 'Former President',
 ARRAY['Federal Deputy (1991-2018)', 'President (2019-2022)'], 1955, ARRAY['Economic liberalization', 'Conservative social policies'],
 '{"twitter": "@jairbolsonaro", "instagram": "@jairmbolsonaro"}', ARRAY[], 'premium', 'Former President of Brazil', 'system'),

('Recep Tayyip Erdoğan', 'tcbestepe', 'President of Turkey', ARRAY['erdogan', 'recep'], 'head_of_state', 'Turkey', 'Justice and Development Party', 'President of Turkey',
 ARRAY['Prime Minister (2003-2014)', 'Mayor of Istanbul (1994-1998)'], 1954, ARRAY['Economic development', 'Infrastructure projects'],
 '{"twitter": "@tcbestepe", "instagram": "@rterdogan"}', ARRAY['https://www.tccb.gov.tr'], 'premium', 'President of Turkey', 'system');

-- ============== US POLITICAL FIGURES ==============

INSERT INTO protected_political_figures (
    full_name, handle, title, aliases, category, country, political_party, office_current, 
    office_history, birth_year, major_policies, social_media, official_websites, 
    verification_level, protection_reason, added_by
) VALUES 

-- US Congressional Leaders
('Nancy Patricia Pelosi', 'speakerpelosi', 'Speaker Emerita', ARRAY['pelosi', 'nancy'], 'legislator', 'United States', 'Democratic Party', 'U.S. Representative California',
 ARRAY['Speaker of the House (2007-2011, 2019-2023)', 'Minority Leader (2011-2019)'], 1940, ARRAY['Affordable Care Act', 'Climate action'],
 '{"twitter": "@speakerpelosi", "instagram": "@speakerpelosi"}', ARRAY['https://pelosi.house.gov'], 'premium', 'Former Speaker of the House', 'system'),

('Charles Ellis Schumer', 'chuckschumer', 'Senate Majority Leader', ARRAY['schumer', 'chuck'], 'legislator', 'United States', 'Democratic Party', 'U.S. Senator New York',
 ARRAY['U.S. Representative (1981-1999)', 'U.S. Senator (1999-present)'], 1950, ARRAY['Infrastructure', 'Immigration reform'],
 '{"twitter": "@chuckschumer", "instagram": "@chuckschumer"}', ARRAY['https://www.schumer.senate.gov'], 'premium', 'Senate Majority Leader', 'system'),

('Kevin Owen McCarthy', 'kevinmccarthy', 'Former Speaker of the House', ARRAY['mccarthy', 'kevin'], 'legislator', 'United States', 'Republican Party', 'U.S. Representative California',
 ARRAY['House Minority Leader (2019-2023)', 'Speaker (2023)'], 1965, ARRAY['Conservative economic policies'],
 '{"twitter": "@kevinmccarthy", "instagram": "@kevinomccarthy"}', ARRAY['https://kevinmccarthy.house.gov'], 'premium', 'Former Speaker of the House', 'system'),

-- Presidential Candidates & Former Presidents
('Donald John Trump', 'realdonaldtrump', 'Former President', ARRAY['trump', 'donald'], 'former_head_of_state', 'United States', 'Republican Party', 'Former President & 2024 Candidate',
 ARRAY['President (2017-2021)'], 1946, ARRAY['America First', 'Tax cuts', 'Border security'],
 '{"twitter": "@realdonaldtrump", "instagram": "@realdonaldtrump"}', ARRAY[], 'premium', '45th President of the United States', 'system'),

('Barack Hussein Obama II', 'barackobama', 'Former President', ARRAY['obama', 'barack'], 'former_head_of_state', 'United States', 'Democratic Party', 'Former President',
 ARRAY['President (2009-2017)', 'U.S. Senator Illinois (2005-2008)'], 1961, ARRAY['Affordable Care Act', 'Climate action', 'Economic recovery'],
 '{"twitter": "@barackobama", "instagram": "@barackobama"}', ARRAY[], 'premium', '44th President of the United States', 'system'),

('Hillary Diane Rodham Clinton', 'hillaryclinton', 'Former Secretary of State', ARRAY['clinton', 'hillary'], 'diplomat', 'United States', 'Democratic Party', 'Former Secretary of State',
 ARRAY['Secretary of State (2009-2013)', 'U.S. Senator New York (2001-2009)', 'First Lady (1993-2001)'], 1947, ARRAY['Healthcare reform', 'Women''s rights'],
 '{"twitter": "@hillaryclinton", "instagram": "@hillaryclinton"}', ARRAY[], 'premium', 'Former Secretary of State and Presidential candidate', 'system');

-- ============== SUPREME COURT & JUDICIARY ==============

INSERT INTO protected_political_figures (
    full_name, handle, title, aliases, category, country, political_party, office_current, 
    office_history, birth_year, major_policies, social_media, official_websites, 
    verification_level, protection_reason, added_by
) VALUES 

-- Supreme Court Justices
('John Glover Roberts Jr.', 'supremecourt', 'Chief Justice', ARRAY['roberts', 'chiefjustice'], 'judge', 'United States', 'Non-partisan', 'Chief Justice of the United States',
 ARRAY['Judge DC Circuit (2003-2005)'], 1955, ARRAY['Constitutional interpretation'],
 '{"twitter": null, "instagram": null}', ARRAY['https://www.supremecourt.gov'], 'premium', 'Chief Justice of the United States', 'system'),

('Clarence Thomas', 'clarencethomas', 'Associate Justice', ARRAY['thomas'], 'judge', 'United States', 'Non-partisan', 'Associate Justice',
 ARRAY['Judge DC Circuit (1990-1991)'], 1948, ARRAY['Originalism', 'Constitutional interpretation'],
 '{"twitter": null, "instagram": null}', ARRAY['https://www.supremecourt.gov'], 'premium', 'Senior Associate Justice', 'system');

-- ============== GOVERNMENT ORGANIZATIONS ==============

INSERT INTO protected_government_organizations (
    organization_name, handle, acronym, aliases, category, country, department, jurisdiction, 
    head_of_agency, official_websites, social_media, verification_level, protection_reason, added_by
) VALUES 

-- US Federal Agencies
('Federal Bureau of Investigation', 'fbi', 'FBI', ARRAY['fbi'], 'federal_agency', 'United States', 'Department of Justice', 'federal', 'Christopher Wray',
 ARRAY['https://www.fbi.gov'], '{"twitter": "@fbi", "instagram": "@fbi", "youtube": "@fbi"}', 'premium', 'Primary federal law enforcement agency', 'system'),

('Central Intelligence Agency', 'cia', 'CIA', ARRAY['cia'], 'federal_agency', 'United States', 'Independent', 'federal', 'William Burns',
 ARRAY['https://www.cia.gov'], '{"twitter": "@cia", "instagram": "@cia", "youtube": "@cia"}', 'premium', 'Foreign intelligence service', 'system'),

('National Aeronautics and Space Administration', 'nasa', 'NASA', ARRAY['nasa'], 'federal_agency', 'United States', 'Independent', 'federal', 'Bill Nelson',
 ARRAY['https://www.nasa.gov'], '{"twitter": "@nasa", "instagram": "@nasa", "youtube": "@nasa"}', 'premium', 'Space exploration agency', 'system'),

('Internal Revenue Service', 'irsnews', 'IRS', ARRAY['irs'], 'federal_agency', 'United States', 'Department of Treasury', 'federal', 'Daniel Werfel',
 ARRAY['https://www.irs.gov'], '{"twitter": "@irsnews", "instagram": null, "youtube": "@irsvideos"}', 'premium', 'Federal tax collection agency', 'system'),

('Department of Homeland Security', 'dhsgov', 'DHS', ARRAY['dhs', 'homeland'], 'federal_agency', 'United States', 'Department of Homeland Security', 'federal', 'Alejandro Mayorkas',
 ARRAY['https://www.dhs.gov'], '{"twitter": "@dhsgov", "instagram": "@dhsgov", "youtube": "@dhsgov"}', 'premium', 'National security department', 'system'),

('Securities and Exchange Commission', 'secgov', 'SEC', ARRAY['sec'], 'federal_agency', 'United States', 'Independent', 'federal', 'Gary Gensler',
 ARRAY['https://www.sec.gov'], '{"twitter": "@secgov", "instagram": null, "youtube": "@secgov"}', 'premium', 'Financial markets regulator', 'system'),

-- International Organizations
('United Nations', 'un', 'UN', ARRAY['unitednations'], 'international_org', 'International', 'Independent', 'international', 'António Guterres',
 ARRAY['https://www.un.org'], '{"twitter": "@un", "instagram": "@un", "youtube": "@unitednations"}', 'premium', 'International peacekeeping organization', 'system'),

('World Health Organization', 'who', 'WHO', ARRAY['worldhealth'], 'international_org', 'International', 'UN Specialized Agency', 'international', 'Tedros Adhanom',
 ARRAY['https://www.who.int'], '{"twitter": "@who", "instagram": "@who", "youtube": "@who"}', 'premium', 'Global health organization', 'system'),

('North Atlantic Treaty Organization', 'nato', 'NATO', ARRAY['nato'], 'international_org', 'International', 'Military Alliance', 'international', 'Jens Stoltenberg',
 ARRAY['https://www.nato.int'], '{"twitter": "@nato", "instagram": "@nato", "youtube": "@natochannel"}', 'premium', 'Military defense alliance', 'system'),

('European Union', 'europeanunion', 'EU', ARRAY['eu'], 'international_org', 'International', 'Political Union', 'international', 'Ursula von der Leyen',
 ARRAY['https://europa.eu'], '{"twitter": "@europeanunion", "instagram": "@europeanunion", "youtube": "@europeanunion"}', 'premium', 'European political and economic union', 'system'),

-- State & Local Government Examples
('Federal Emergency Management Agency', 'fema', 'FEMA', ARRAY['fema'], 'federal_agency', 'United States', 'Department of Homeland Security', 'federal', 'Deanne Criswell',
 ARRAY['https://www.fema.gov'], '{"twitter": "@fema", "instagram": "@fema", "youtube": "@fema"}', 'premium', 'Emergency management agency', 'system'),

('Centers for Disease Control and Prevention', 'cdcgov', 'CDC', ARRAY['cdc'], 'federal_agency', 'United States', 'Department of Health and Human Services', 'federal', 'Mandy Cohen',
 ARRAY['https://www.cdc.gov'], '{"twitter": "@cdcgov", "instagram": "@cdcgov", "youtube": "@cdcgov"}', 'premium', 'Public health protection agency', 'system');

-- ============== INTERNATIONAL LEADERS ==============

INSERT INTO protected_political_figures (
    full_name, handle, title, aliases, category, country, political_party, office_current, 
    office_history, birth_year, major_policies, social_media, official_websites, 
    verification_level, protection_reason, added_by
) VALUES 

-- European Leaders
('Ursula Gertrud von der Leyen', 'vonderleyen', 'President of European Commission', ARRAY['ursula', 'vonderleyen'], 'head_of_government', 'Germany', 'Christian Democratic Union', 'President of European Commission',
 ARRAY['German Defense Minister (2013-2019)'], 1958, ARRAY['European Green Deal', 'Digital transformation'],
 '{"twitter": "@vonderleyen", "instagram": "@ursulavonderleyen"}', ARRAY['https://ec.europa.eu'], 'premium', 'President of European Commission', 'system'),

('Olaf Scholz', 'bundeskanzler', 'Chancellor of Germany', ARRAY['scholz', 'olaf'], 'head_of_government', 'Germany', 'Social Democratic Party', 'Chancellor of Germany',
 ARRAY['Vice Chancellor (2018-2021)', 'Mayor of Hamburg (2011-2018)'], 1958, ARRAY['Climate action', 'European integration'],
 '{"twitter": "@bundeskanzler", "instagram": "@bundeskanzler"}', ARRAY['https://www.bundeskanzler.de'], 'premium', 'Chancellor of Germany', 'system'),

-- Other Global Leaders
('Volodymyr Oleksandrovych Zelenskyy', 'zelenskyyua', 'President of Ukraine', ARRAY['zelensky', 'volodymyr'], 'head_of_state', 'Ukraine', 'Servant of the People', 'President of Ukraine',
 ARRAY['Comedian and actor'], 1978, ARRAY['Defense against invasion', 'EU integration'],
 '{"twitter": "@zelenskyyua", "instagram": "@zelenskyyua"}', ARRAY['https://www.president.gov.ua'], 'premium', 'President of Ukraine during conflict', 'system'),

('Justin Pierre James Trudeau', 'justintrudeau', 'Prime Minister of Canada', ARRAY['trudeau', 'justin'], 'head_of_government', 'Canada', 'Liberal Party', 'Prime Minister of Canada',
 ARRAY['Member of Parliament (2008-present)'], 1971, ARRAY['Climate action', 'Progressive policies'],
 '{"twitter": "@justintrudeau", "instagram": "@justintrudeau"}', ARRAY['https://pm.gc.ca'], 'premium', 'Prime Minister of Canada', 'system');

-- ============== VERIFICATION UPDATES ==============

-- Update all political figures to premium protection
UPDATE protected_political_figures SET verification_level = 'premium' WHERE category IN ('head_of_state', 'head_of_government', 'former_head_of_state');
UPDATE protected_government_organizations SET verification_level = 'premium' WHERE jurisdiction IN ('federal', 'international');

-- Add creation timestamps
UPDATE protected_political_figures SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_government_organizations SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;