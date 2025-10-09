-- Entativa ID: Comprehensive Celebrity Protection Seed Data
-- Entertainment, Music, Sports, and Digital Celebrity Protection
-- Data Version: 2.0 - Categorized Protection System

-- ============== ENTERTAINMENT CELEBRITIES ==============

INSERT INTO protected_entertainment_celebrities (
    full_name, handle, stage_name, aliases, category, subcategory, nationality, birth_year, 
    major_works, awards, social_media, verification_level, protection_reason, added_by
) VALUES 

-- A-List Hollywood Actors
('Leonardo Wilhelm DiCaprio', 'leonardodicaprio', 'Leonardo DiCaprio', ARRAY['leo', 'dicaprio'], 'actor', 'hollywood', 'American', 1974, 
 ARRAY['Titanic', 'The Wolf of Wall Street', 'Inception', 'The Revenant'], ARRAY['Academy Award Winner'], 
 '{"instagram": "@leonardodicaprio", "twitter": null}', 'premium', 'A-list Hollywood actor with global recognition', 'system'),

('Scarlett Ingrid Johansson', 'scarlettjohansson', 'Scarlett Johansson', ARRAY['scarjo', 'johansson'], 'actor', 'hollywood', 'American', 1984,
 ARRAY['Black Widow', 'Marriage Story', 'Lost in Translation', 'Avengers'], ARRAY['BAFTA Winner', 'Tony Award Nominee'],
 '{"instagram": "@scarlettjohanssonofficial", "twitter": null}', 'premium', 'Marvel star and acclaimed actress', 'system'),

('Dwayne Douglas Johnson', 'therock', 'Dwayne Johnson', ARRAY['rock', 'dwaynejohnson'], 'actor', 'hollywood', 'American', 1972,
 ARRAY['Fast & Furious', 'Jumanji', 'Moana', 'Black Adam'], ARRAY['People''s Choice Awards'],
 '{"instagram": "@therock", "twitter": "@TheRock"}', 'premium', 'Former wrestler turned A-list actor', 'system'),

('Ryan Thomas Gosling', 'ryangosling', 'Ryan Gosling', ARRAY['gosling'], 'actor', 'hollywood', 'Canadian', 1980,
 ARRAY['La La Land', 'Blade Runner 2049', 'The Notebook', 'First Man'], ARRAY['Golden Globe Winner'],
 '{"instagram": null, "twitter": null}', 'premium', 'Academy Award nominated actor', 'system'),

('Emma Charlotte Duerre Watson', 'emmawatson', 'Emma Watson', ARRAY['watson'], 'actor', 'hollywood', 'British', 1990,
 ARRAY['Harry Potter series', 'Beauty and the Beast', 'Little Women'], ARRAY['MTV Movie Awards'],
 '{"instagram": "@emmawatson", "twitter": "@EmmaWatson"}', 'premium', 'Harry Potter star and UN Women Goodwill Ambassador', 'system'),

-- Directors & Producers
('Christopher Edward Nolan', 'christophernolan', 'Christopher Nolan', ARRAY['nolan'], 'director', 'hollywood', 'British', 1970,
 ARRAY['Inception', 'The Dark Knight', 'Interstellar', 'Dunkirk'], ARRAY['Academy Award Winner'],
 '{"instagram": null, "twitter": null}', 'premium', 'Acclaimed filmmaker known for complex narratives', 'system'),

('Quentin Jerome Tarantino', 'quentintarantino', 'Quentin Tarantino', ARRAY['tarantino'], 'director', 'hollywood', 'American', 1963,
 ARRAY['Pulp Fiction', 'Kill Bill', 'Django Unchained', 'Once Upon a Time in Hollywood'], ARRAY['Academy Award Winner'],
 '{"instagram": null, "twitter": null}', 'premium', 'Iconic filmmaker and screenwriter', 'system'),

-- International Stars
('Shah Rukh Khan', 'iamsrk', 'Shah Rukh Khan', ARRAY['srk', 'shahrukhkhan', 'kingkhan'], 'actor', 'bollywood', 'Indian', 1965,
 ARRAY['Dilwale Dulhania Le Jayenge', 'My Name is Khan', 'Chennai Express'], ARRAY['Padma Shri', 'Filmfare Awards'],
 '{"instagram": "@iamsrk", "twitter": "@iamsrk"}', 'premium', 'Bollywood King and global icon', 'system'),

('Jackie Chan', 'jackiechan', 'Jackie Chan', ARRAY['chan'], 'actor', 'international', 'Hong Kong', 1954,
 ARRAY['Rush Hour', 'Police Story', 'Drunken Master', 'Karate Kid'], ARRAY['Honorary Academy Award'],
 '{"instagram": "@jackiechan", "twitter": "@EyeOfJackieChan"}', 'premium', 'International martial arts movie star', 'system');

-- ============== MUSIC CELEBRITIES ==============

INSERT INTO protected_music_celebrities (
    full_name, handle, stage_name, band_name, aliases, category, genre, nationality, birth_year,
    record_label, major_albums, awards, social_media, monthly_listeners, verification_level, protection_reason, added_by
) VALUES 

-- Pop Superstars
('Taylor Alison Swift', 'taylorswift', 'Taylor Swift', null, ARRAY['tswift', 'swift'], 'singer', 'pop', 'American', 1989,
 'Republic Records', ARRAY['1989', 'folklore', 'Midnights', 'Red'], ARRAY['12 Grammy Awards', 'Artist of the Decade'],
 '{"instagram": "@taylorswift", "twitter": "@taylorswift13", "tiktok": "@taylorswift"}', 75000000, 'premium', 'Global pop superstar with massive fanbase', 'system'),

('Beyoncé Giselle Knowles-Carter', 'beyonce', 'Beyoncé', 'Destiny''s Child', ARRAY['queen b', 'beyhive'], 'singer', 'pop', 'American', 1981,
 'Columbia Records', ARRAY['Lemonade', 'Renaissance', 'Dangerously in Love'], ARRAY['32 Grammy Awards', 'NAACP Awards'],
 '{"instagram": "@beyonce", "twitter": null, "tiktok": "@beyonce"}', 45000000, 'premium', 'Queen of Pop and cultural icon', 'system'),

('Ariana Grande-Butera', 'arianagrande', 'Ariana Grande', null, ARRAY['ari', 'grande'], 'singer', 'pop', 'American', 1993,
 'Republic Records', ARRAY['thank u, next', 'Positions', 'Sweetener'], ARRAY['2 Grammy Awards', 'Billboard Awards'],
 '{"instagram": "@arianagrande", "twitter": "@ArianaGrande", "tiktok": "@arianagrande"}', 65000000, 'premium', 'Pop sensation with global reach', 'system'),

-- Hip-Hop Artists
('Shawn Corey Carter', 'jayz', 'Jay-Z', null, ARRAY['hov', 'jiggaman'], 'rapper', 'hip-hop', 'American', 1969,
 'Roc Nation', ARRAY['The Blueprint', '4:44', 'Reasonable Doubt'], ARRAY['24 Grammy Awards', 'Rock Hall of Fame'],
 '{"instagram": null, "twitter": null}', 35000000, 'premium', 'Hip-hop mogul and cultural icon', 'system'),

('Aubrey Drake Graham', 'drake', 'Drake', null, ARRAY['drizzy', 'champagnepapi'], 'rapper', 'hip-hop', 'Canadian', 1986,
 'OVO Sound', ARRAY['Take Care', 'Views', 'Scorpion'], ARRAY['5 Grammy Awards', 'Billboard Artist of the Decade'],
 '{"instagram": "@champagnepapi", "twitter": "@Drake"}', 85000000, 'premium', 'Chart-topping rapper and global star', 'system'),

-- Rock Legends
('Robert Anthony Plant', 'robertplant', 'Robert Plant', 'Led Zeppelin', ARRAY['plant'], 'singer', 'rock', 'British', 1948,
 'Independent', ARRAY['Led Zeppelin IV', 'Physical Graffiti'], ARRAY['Rock Hall of Fame', 'Grammy Lifetime Achievement'],
 '{"instagram": "@robertplantofficial", "twitter": null}', 8000000, 'premium', 'Led Zeppelin frontman and rock legend', 'system'),

-- Electronic/DJ
('Adam Richard Wiles', 'calvinharris', 'Calvin Harris', null, ARRAY['harris'], 'producer', 'electronic', 'Scottish', 1984,
 'Columbia Records', ARRAY['18 Months', 'Motion', 'Funk Wav Bounces'], ARRAY['Grammy Winner', 'Brit Awards'],
 '{"instagram": "@calvinharris", "twitter": "@CalvinHarris"}', 50000000, 'premium', 'World-renowned DJ and producer', 'system');

-- ============== SPORTS FIGURES ==============

INSERT INTO protected_sports_figures (
    full_name, handle, nickname, aliases, category, sport, position, team_current, nationality, birth_year,
    major_achievements, social_media, endorsements, verification_level, protection_reason, added_by
) VALUES 

-- Basketball
('LeBron Raymone James', 'kingjames', 'King James', ARRAY['lebron', 'kingjames', 'thekid'], 'athlete', 'basketball', 'Forward', 'Los Angeles Lakers', 'American', 1984,
 ARRAY['4x NBA Champion', '4x NBA Finals MVP', '19x NBA All-Star'], '{"instagram": "@kingjames", "twitter": "@KingJames"}',
 ARRAY['Nike', 'Coca-Cola', 'Beats'], 'premium', 'NBA superstar and global sports icon', 'system'),

('Stephen Curry', 'stephencurry30', 'Steph Curry', ARRAY['curry', 'chef'], 'athlete', 'basketball', 'Point Guard', 'Golden State Warriors', 'American', 1988,
 ARRAY['4x NBA Champion', '2x NBA MVP', '9x NBA All-Star'], '{"instagram": "@stephencurry30", "twitter": "@StephenCurry30"}',
 ARRAY['Under Armour', 'JPMorgan Chase'], 'premium', 'Revolutionary basketball player', 'system'),

-- Football (American)
('Thomas Edward Patrick Brady Jr.', 'tombrady', 'Tom Brady', ARRAY['tb12', 'brady'], 'athlete', 'american_football', 'Quarterback', 'Retired', 'American', 1977,
 ARRAY['7x Super Bowl Champion', '5x Super Bowl MVP', '15x Pro Bowl'], '{"instagram": "@tombrady", "twitter": "@TomBrady"}',
 ARRAY['Under Armour', 'Subway', 'FTX'], 'premium', 'Greatest NFL quarterback of all time', 'system'),

-- Soccer/Football
('Cristiano Ronaldo dos Santos Aveiro', 'cristiano', 'Cristiano Ronaldo', ARRAY['cr7', 'ronaldo'], 'athlete', 'soccer', 'Forward', 'Al Nassr', 'Portuguese', 1985,
 ARRAY['5x Ballon d''Or', '5x Champions League', 'Euro 2016 Winner'], '{"instagram": "@cristiano", "twitter": "@Cristiano"}',
 ARRAY['Nike', 'Clear', 'Herbalife'], 'premium', 'Global soccer superstar', 'system'),

('Lionel Andrés Messi', 'leomessi', 'Leo Messi', ARRAY['messi', 'goat'], 'athlete', 'soccer', 'Forward', 'Inter Miami', 'Argentine', 1987,
 ARRAY['7x Ballon d''Or', '4x Champions League', 'World Cup 2022'], '{"instagram": "@leomessi", "twitter": null}',
 ARRAY['Adidas', 'Pepsi', 'Mastercard'], 'premium', 'Greatest soccer player of all time', 'system'),

-- Tennis
('Serena Jameka Williams', 'serenawilliams', 'Serena Williams', ARRAY['serena'], 'athlete', 'tennis', 'Professional', 'Retired', 'American', 1981,
 ARRAY['23 Grand Slam Singles', '4x Olympic Gold', 'Former World No. 1'], '{"instagram": "@serenawilliams", "twitter": "@serenawilliams"}',
 ARRAY['Nike', 'Gatorade', 'Wilson'], 'premium', 'Tennis legend and women''s sports icon', 'system');

-- ============== DIGITAL CELEBRITIES ==============

INSERT INTO protected_digital_celebrities (
    full_name, handle, screen_name, aliases, category, niche, nationality, birth_year,
    primary_platform, follower_counts, verified_platforms, major_content, social_media, verification_level, protection_reason, added_by
) VALUES 

-- YouTube Stars
('Felix Arvid Ulf Kjellberg', 'pewdiepie', 'PewDiePie', ARRAY['felix', 'pewds'], 'youtuber', 'gaming', 'Swedish', 1989,
 'YouTube', '{"youtube": 111000000, "twitter": 19000000, "instagram": 21000000}', ARRAY['YouTube', 'Twitter'],
 ARRAY['Gaming videos', 'Minecraft series', 'Meme reviews'], '{"youtube": "@PewDiePie", "twitter": "@pewdiepie", "instagram": "@pewdiepie"}',
 'premium', 'Most subscribed individual YouTuber', 'system'),

('James Stephen Donaldson', 'mrbeast', 'MrBeast', ARRAY['jimmy', 'beast'], 'youtuber', 'entertainment', 'American', 1998,
 'YouTube', '{"youtube": 120000000, "twitter": 18000000, "instagram": 35000000, "tiktok": 75000000}', ARRAY['YouTube', 'Twitter', 'TikTok'],
 ARRAY['Philanthropy videos', 'Challenge videos', 'Beast Burger'], '{"youtube": "@MrBeast", "twitter": "@MrBeast", "instagram": "@mrbeast", "tiktok": "@mrbeast"}',
 'premium', 'Philanthropic YouTube sensation', 'system'),

-- TikTok Stars
('Charli Grace D''Amelio', 'charlidamelio', 'Charli D''Amelio', ARRAY['charli'], 'tiktoker', 'dance', 'American', 2004,
 'TikTok', '{"tiktok": 150000000, "instagram": 50000000, "youtube": 9000000}', ARRAY['TikTok', 'Instagram', 'YouTube'],
 ARRAY['Dance videos', 'The D''Amelio Show'], '{"tiktok": "@charlidamelio", "instagram": "@charlidamelio", "youtube": "@CharliDAmelio"}',
 'premium', 'Most followed TikTok creator', 'system'),

-- Twitch Streamers
('Tyler Richard Blevins', 'ninja', 'Ninja', ARRAY['ninjashyper'], 'streamer', 'gaming', 'American', 1991,
 'Twitch', '{"twitch": 18000000, "youtube": 24000000, "twitter": 6000000, "instagram": 13000000}', ARRAY['Twitch', 'YouTube', 'Twitter'],
 ARRAY['Fortnite streams', 'Gaming content'], '{"twitch": "@Ninja", "youtube": "@NinjashyperNinja", "twitter": "@Ninja", "instagram": "@ninja"}',
 'premium', 'Top gaming streamer and esports personality', 'system'),

-- Beauty/Lifestyle
('James Charles Dickinson', 'jamescharles', 'James Charles', ARRAY['james'], 'youtuber', 'beauty', 'American', 1999,
 'YouTube', '{"youtube": 23000000, "instagram": 20000000, "tiktok": 36000000, "twitter": 3600000}', ARRAY['YouTube', 'Instagram', 'TikTok'],
 ARRAY['Makeup tutorials', 'Beauty collaborations'], '{"youtube": "@jamescharles", "instagram": "@jamescharles", "tiktok": "@jamescharles"}',
 'premium', 'Beauty influencer and makeup artist', 'system');

-- ============== VERIFICATION UPDATES ==============

-- Update verification levels based on follower counts and influence
UPDATE protected_music_celebrities SET verification_level = 'premium' WHERE monthly_listeners > 50000000;
UPDATE protected_digital_celebrities SET verification_level = 'premium' WHERE (follower_counts->>'youtube')::int > 20000000 OR (follower_counts->>'tiktok')::int > 50000000;

-- Add creation timestamps
UPDATE protected_entertainment_celebrities SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_music_celebrities SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_sports_figures SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;
UPDATE protected_digital_celebrities SET created_at = NOW() - INTERVAL '30 days' WHERE created_at IS NULL;