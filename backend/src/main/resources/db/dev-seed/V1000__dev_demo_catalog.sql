-- DEV/DEMO DATA ONLY (fictional hotels, prices and reviews).
-- Loaded only when 'classpath:db/dev-seed' is in spring.flyway.locations (local profile).
-- Production must NOT include this location.
-- Version 1000 keeps demo data after any real schema migration.

INSERT INTO hotel (id, region, name, city, stars, rating, reviews, board, price_per_night, old_price, image, tags, created_at) VALUES
 (gen_random_uuid(), 'tunisie', 'Azure Bay Resort & Spa', 'Hammamet', 5, 9.1, 1284, 'All inclusive soft', 245, 299, 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=900&q=70', '{"Plage privée","Spa"}', now() - interval '11 minutes'),
 (gen_random_uuid(), 'tunisie', 'Palm Oasis Djerba', 'Djerba', 4, 8.7, 932, 'Tout compris', 168, NULL, 'https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=900&q=70', '{"Aquapark","Famille"}', now() - interval '10 minutes'),
 (gen_random_uuid(), 'tunisie', 'Corniche Pearl Hotel', 'Sousse', 4, 8.4, 2140, 'Demi-pension', 119, 145, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=900&q=70', '{"Centre-ville","Piscine"}', now() - interval '9 minutes'),
 (gen_random_uuid(), 'tunisie', 'Dar El Yasmine', 'Tunis', 5, 9.4, 486, 'Petit-déjeuner', 310, NULL, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=900&q=70', '{"Boutique","Vue mer"}', now() - interval '8 minutes'),
 (gen_random_uuid(), 'tunisie', 'Sahara Dunes Lodge', 'Tozeur', 4, 9.0, 377, 'Demi-pension', 189, NULL, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=900&q=70', '{"Désert","Excursions"}', now() - interval '7 minutes'),
 (gen_random_uuid(), 'tunisie', 'Marina Suites Monastir', 'Monastir', 5, 8.9, 815, 'All inclusive soft', 228, 265, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=70', '{"Marina","Golf"}', now() - interval '6 minutes'),
 (gen_random_uuid(), 'tunisie', 'Medina Palace', 'Hammamet', 4, 8.6, 1650, 'Tout compris', 142, NULL, 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=900&q=70', '{"Animation","Kids club"}', now() - interval '5 minutes'),
 (gen_random_uuid(), 'tunisie', 'Coral Beach Mahdia', 'Mahdia', 5, 9.2, 703, 'Tout compris', 215, NULL, 'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=900&q=70', '{"Pieds dans l’eau","Thalasso"}', now() - interval '4 minutes'),
 (gen_random_uuid(), 'monde', 'Bosphorus View Hotel', 'Istanbul', 4, 8.8, 2310, 'Petit-déjeuner', 260, NULL, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=900&q=70', '{"Vue Bosphore","Centre"}', now() - interval '3 minutes'),
 (gen_random_uuid(), 'monde', 'Marina Sky Dubaï', 'Dubaï', 5, 9.0, 1745, 'Petit-déjeuner', 540, 620, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=70', '{"Rooftop","Piscine"}', now() - interval '2 minutes'),
 (gen_random_uuid(), 'monde', 'Hôtel Rive Gauche', 'Paris', 4, 8.5, 980, 'Logement seul', 410, NULL, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=900&q=70', '{"Saint-Germain","Métro"}', now() - interval '1 minutes');

INSERT INTO trip (id, name, country, duration_days, price_from, image, no_visa, badge, created_at) VALUES
 (gen_random_uuid(), 'Istanbul', 'Turquie', 8, 1890, 'https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?auto=format&fit=crop&w=900&q=70', TRUE, 'Best-seller', now() - interval '6 minutes'),
 (gen_random_uuid(), 'Dubaï', 'Émirats', 6, 3290, 'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=900&q=70', FALSE, NULL, now() - interval '5 minutes'),
 (gen_random_uuid(), 'Paris', 'France', 7, 2790, 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=900&q=70', FALSE, NULL, now() - interval '4 minutes'),
 (gen_random_uuid(), 'Le Caire & Louxor', 'Égypte', 8, 2190, 'https://images.unsplash.com/photo-1539650116574-8efeb43e2750?auto=format&fit=crop&w=900&q=70', FALSE, 'Croisière Nil', now() - interval '3 minutes'),
 (gen_random_uuid(), 'Marrakech', 'Maroc', 6, 1690, 'https://images.unsplash.com/photo-1597212618440-806262de4f6b?auto=format&fit=crop&w=900&q=70', TRUE, NULL, now() - interval '2 minutes'),
 (gen_random_uuid(), 'Rome', 'Italie', 6, 2490, 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=900&q=70', FALSE, NULL, now() - interval '1 minutes');

INSERT INTO omra_program (id, title, duration_days, departure, hotel_distance, price_from, image, includes) VALUES
 (gen_random_uuid(), 'Omra Économique', 15, '12 février 2027', 800, 4890, 'https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?auto=format&fit=crop&w=900&q=70', '{"Vol direct","Visa","Transferts"}'),
 (gen_random_uuid(), 'Omra Confort', 15, '5 mars 2027', 300, 6290, 'https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?auto=format&fit=crop&w=900&q=70', '{"Vol direct","Visa","Guide religieux","Demi-pension"}'),
 (gen_random_uuid(), 'Omra Ramadan', 20, '10 février 2027', 150, 8450, 'https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?auto=format&fit=crop&w=900&q=70', '{"Vol direct","Visa","Guide religieux","Pension complète"}');

INSERT INTO flight_deal (id, city, airport_code, price_from, created_at) VALUES
 (gen_random_uuid(), 'Paris', 'ORY', 690, now() - interval '6 minutes'),
 (gen_random_uuid(), 'Istanbul', 'IST', 590, now() - interval '5 minutes'),
 (gen_random_uuid(), 'Dubaï', 'DXB', 1290, now() - interval '4 minutes'),
 (gen_random_uuid(), 'Rome', 'FCO', 520, now() - interval '3 minutes'),
 (gen_random_uuid(), 'Le Caire', 'CAI', 740, now() - interval '2 minutes'),
 (gen_random_uuid(), 'Montréal', 'YUL', 2150, now() - interval '1 minutes');

INSERT INTO service_offer (id, kind, title, price_from, description, created_at) VALUES
 (gen_random_uuid(), 'Visa', 'Visa Schengen', 180, 'Constitution du dossier, rendez-vous et suivi.', now() - interval '6 minutes'),
 (gen_random_uuid(), 'Visa', 'Visa Canada', 250, 'Visa visiteur, accompagnement complet.', now() - interval '5 minutes'),
 (gen_random_uuid(), 'Visa', 'Visa Royaume-Uni', 220, 'Dossier en ligne et prise de rendez-vous.', now() - interval '4 minutes'),
 (gen_random_uuid(), 'Ferry CTN', 'Tunis ⇄ Marseille', 450, 'Traversée passager, cabine ou fauteuil.', now() - interval '3 minutes'),
 (gen_random_uuid(), 'Ferry CTN', 'Tunis ⇄ Gênes', 420, 'Traversée passager + véhicule.', now() - interval '2 minutes'),
 (gen_random_uuid(), 'TRE', 'Pack TRE été', 990, 'Bateau + hôtel en Tunisie pour les Tunisiens résidents à l’étranger.', now() - interval '1 minutes');
