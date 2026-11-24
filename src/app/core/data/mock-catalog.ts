import { CatalogData, ServiceItem, Testimonial } from '../models/travel.models';

/**
 * PROTOTYPE DATA ONLY.
 * Hotel names, prices and reviews below are fictional placeholders for the
 * client demo. SEED_CATALOG is the starting content of the admin-managed
 * catalogue (see CatalogStore); it will come from the Spring Boot API later.
 * Photos are hot-linked from Unsplash (free licence) for the prototype.
 */

const img = (id: string, w = 900) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=70`;

/* ---------- Static marketing content (not managed in the admin yet) ---------- */

export const HERO_IMAGES: string[] = [
  img('photo-1507525428034-b723cf961d3e', 2000),
  img('photo-1566073771259-6a8506099945', 2000),
  img('photo-1520250497591-112f2f40a3f4', 2000),
];

export const PROMO_IMAGES = {
  earlyBooking: img('photo-1540541338287-41700207dee6', 1000),
  kidsFree: img('photo-1530053969600-caed2596d242', 1000),
  flights: img('photo-1436491865332-7a61a109cc05', 1400),
  omra: img('photo-1591604129939-f1efa4d9f7fa', 1200),
};

export const SERVICES: ServiceItem[] = [
  { label: 'Séjours Tunisie', description: 'Hôtels en bord de mer', icon: 'beach', anchor: '#hotels-tunisie' },
  { label: 'Hôtels monde', description: '+500 000 hôtels', icon: 'hotel', anchor: '#hotels-tunisie' },
  { label: 'Voyages organisés', description: 'Circuits tout inclus', icon: 'globe', anchor: '#voyages' },
  { label: 'Billets d’avion', description: 'Toutes compagnies', icon: 'plane', anchor: '#vols' },
  { label: 'Ferry CTN', description: 'Traversées & TRE', icon: 'ship', anchor: '#vols' },
  { label: 'Visa', description: 'Dossier accompagné', icon: 'passport', anchor: '#omra-visa' },
  { label: 'Omra', description: 'Programmes encadrés', icon: 'moon', anchor: '#omra-visa' },
  { label: 'Groupes & entreprises', description: 'Séminaires, sorties', icon: 'users', anchor: '#contact' },
];

export const TESTIMONIALS: Testimonial[] = [
  { name: 'Amira B.', city: 'Sfax', trip: 'Séjour à Djerba', rating: 5, text: 'Réservation simple, hôtel conforme aux photos et une équipe disponible même le week-end. On repart avec Gezenthi cet été !' },
  { name: 'Mehdi K.', city: 'Tunis', trip: 'Voyage à Istanbul', rating: 5, text: 'Programme bien organisé, guide francophone et hôtel très bien situé. Excellent rapport qualité-prix.' },
  { name: 'Sonia & Karim', city: 'Paris (TRE)', trip: 'Ferry + Hammamet', rating: 5, text: 'Billets de bateau et hôtel réservés en une seule fois. Un vrai gain de temps pour nos vacances au pays.' },
];

/* ---------- Admin-managed catalogue (initial content) ---------- */

export const SEED_CATALOG: CatalogData = {
  hotels: [
    { id: 'azure-bay', published: true, region: 'tunisie', name: 'Azure Bay Resort & Spa', city: 'Hammamet', stars: 5, rating: 9.1, reviews: 1284,
      board: 'All inclusive soft', pricePerNight: 245, oldPrice: 299, image: img('photo-1571896349842-33c89424de2d'), tags: ['Plage privée', 'Spa'] },
    { id: 'palm-oasis', published: true, region: 'tunisie', name: 'Palm Oasis Djerba', city: 'Djerba', stars: 4, rating: 8.7, reviews: 932,
      board: 'Tout compris', pricePerNight: 168, image: img('photo-1582719508461-905c673771fd'), tags: ['Aquapark', 'Famille'] },
    { id: 'corniche-pearl', published: true, region: 'tunisie', name: 'Corniche Pearl Hotel', city: 'Sousse', stars: 4, rating: 8.4, reviews: 2140,
      board: 'Demi-pension', pricePerNight: 119, oldPrice: 145, image: img('photo-1551882547-ff40c63fe5fa'), tags: ['Centre-ville', 'Piscine'] },
    { id: 'dar-yasmine', published: true, region: 'tunisie', name: 'Dar El Yasmine', city: 'Tunis', stars: 5, rating: 9.4, reviews: 486,
      board: 'Petit-déjeuner', pricePerNight: 310, image: img('photo-1590490360182-c33d57733427'), tags: ['Boutique', 'Vue mer'] },
    { id: 'sahara-dunes', published: true, region: 'tunisie', name: 'Sahara Dunes Lodge', city: 'Tozeur', stars: 4, rating: 9.0, reviews: 377,
      board: 'Demi-pension', pricePerNight: 189, image: img('photo-1542314831-068cd1dbfeeb'), tags: ['Désert', 'Excursions'] },
    { id: 'marina-suites', published: true, region: 'tunisie', name: 'Marina Suites Monastir', city: 'Monastir', stars: 5, rating: 8.9, reviews: 815,
      board: 'All inclusive soft', pricePerNight: 228, oldPrice: 265, image: img('photo-1566073771259-6a8506099945'), tags: ['Marina', 'Golf'] },
    { id: 'medina-palace', published: true, region: 'tunisie', name: 'Medina Palace', city: 'Hammamet', stars: 4, rating: 8.6, reviews: 1650,
      board: 'Tout compris', pricePerNight: 142, image: img('photo-1520250497591-112f2f40a3f4'), tags: ['Animation', 'Kids club'] },
    { id: 'coral-mahdia', published: true, region: 'tunisie', name: 'Coral Beach Mahdia', city: 'Mahdia', stars: 5, rating: 9.2, reviews: 703,
      board: 'Tout compris', pricePerNight: 215, image: img('photo-1584132967334-10e028bd69f7'), tags: ['Pieds dans l’eau', 'Thalasso'] },
    { id: 'bosphorus-view', published: true, region: 'monde', name: 'Bosphorus View Hotel', city: 'Istanbul', stars: 4, rating: 8.8, reviews: 2310,
      board: 'Petit-déjeuner', pricePerNight: 260, image: img('photo-1551882547-ff40c63fe5fa'), tags: ['Vue Bosphore', 'Centre'] },
    { id: 'marina-sky', published: true, region: 'monde', name: 'Marina Sky Dubaï', city: 'Dubaï', stars: 5, rating: 9.0, reviews: 1745,
      board: 'Petit-déjeuner', pricePerNight: 540, oldPrice: 620, image: img('photo-1566073771259-6a8506099945'), tags: ['Rooftop', 'Piscine'] },
    { id: 'rive-gauche', published: true, region: 'monde', name: 'Hôtel Rive Gauche', city: 'Paris', stars: 4, rating: 8.5, reviews: 980,
      board: 'Logement seul', pricePerNight: 410, image: img('photo-1590490360182-c33d57733427'), tags: ['Saint-Germain', 'Métro'] },
  ],
  trips: [
    { id: 'istanbul', published: true, name: 'Istanbul', country: 'Turquie', durationDays: 8, priceFrom: 1890, image: img('photo-1524231757912-21f4fe3a7200'), noVisa: true, badge: 'Best-seller' },
    { id: 'dubai', published: true, name: 'Dubaï', country: 'Émirats', durationDays: 6, priceFrom: 3290, image: img('photo-1512453979798-5ea266f8880c') },
    { id: 'paris', published: true, name: 'Paris', country: 'France', durationDays: 7, priceFrom: 2790, image: img('photo-1502602898657-3e91760cbb34') },
    { id: 'cairo', published: true, name: 'Le Caire & Louxor', country: 'Égypte', durationDays: 8, priceFrom: 2190, image: img('photo-1539650116574-8efeb43e2750'), badge: 'Croisière Nil' },
    { id: 'marrakech', published: true, name: 'Marrakech', country: 'Maroc', durationDays: 6, priceFrom: 1690, image: img('photo-1597212618440-806262de4f6b'), noVisa: true },
    { id: 'rome', published: true, name: 'Rome', country: 'Italie', durationDays: 6, priceFrom: 2490, image: img('photo-1552832230-c0197dd311b5') },
  ],
  omra: [
    { id: 'omra-eco', published: true, title: 'Omra Économique', durationDays: 15, departure: '12 février 2027', hotelDistance: 800, priceFrom: 4890,
      image: img('photo-1591604129939-f1efa4d9f7fa'), includes: ['Vol direct', 'Visa', 'Transferts'] },
    { id: 'omra-confort', published: true, title: 'Omra Confort', durationDays: 15, departure: '5 mars 2027', hotelDistance: 300, priceFrom: 6290,
      image: img('photo-1591604129939-f1efa4d9f7fa'), includes: ['Vol direct', 'Visa', 'Guide religieux', 'Demi-pension'] },
    { id: 'omra-ramadan', published: true, title: 'Omra Ramadan', durationDays: 20, departure: '10 février 2027', hotelDistance: 150, priceFrom: 8450,
      image: img('photo-1591604129939-f1efa4d9f7fa'), includes: ['Vol direct', 'Visa', 'Guide religieux', 'Pension complète'] },
  ],
  flights: [
    { id: 'tun-ory', published: true, city: 'Paris', airportCode: 'ORY', priceFrom: 690 },
    { id: 'tun-ist', published: true, city: 'Istanbul', airportCode: 'IST', priceFrom: 590 },
    { id: 'tun-dxb', published: true, city: 'Dubaï', airportCode: 'DXB', priceFrom: 1290 },
    { id: 'tun-fco', published: true, city: 'Rome', airportCode: 'FCO', priceFrom: 520 },
    { id: 'tun-cai', published: true, city: 'Le Caire', airportCode: 'CAI', priceFrom: 740 },
    { id: 'tun-yul', published: true, city: 'Montréal', airportCode: 'YUL', priceFrom: 2150 },
  ],
  services: [
    { id: 'visa-schengen', published: true, kind: 'Visa', title: 'Visa Schengen', priceFrom: 180, description: 'Constitution du dossier, rendez-vous et suivi.' },
    { id: 'visa-canada', published: true, kind: 'Visa', title: 'Visa Canada', priceFrom: 250, description: 'Visa visiteur, accompagnement complet.' },
    { id: 'visa-uk', published: true, kind: 'Visa', title: 'Visa Royaume-Uni', priceFrom: 220, description: 'Dossier en ligne et prise de rendez-vous.' },
    { id: 'ctn-marseille', published: true, kind: 'Ferry CTN', title: 'Tunis ⇄ Marseille', priceFrom: 450, description: 'Traversée passager, cabine ou fauteuil.' },
    { id: 'ctn-genes', published: true, kind: 'Ferry CTN', title: 'Tunis ⇄ Gênes', priceFrom: 420, description: 'Traversée passager + véhicule.' },
    { id: 'tre-ete', published: true, kind: 'TRE', title: 'Pack TRE été', priceFrom: 990, description: 'Bateau + hôtel en Tunisie pour les Tunisiens résidents à l’étranger.' },
  ],
};
