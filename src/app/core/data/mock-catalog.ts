import { Destination, FlightDeal, Hotel, ServiceItem, Testimonial } from '../models/travel.models';

/**
 * PROTOTYPE DATA ONLY.
 * Hotel names, prices and reviews below are fictional placeholders for the
 * client demo. They will be replaced by data from the Spring Boot API.
 * Photos are hot-linked from Unsplash (free licence) for the prototype.
 */

const img = (id: string, w = 900) => `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=70`;

export const HERO_IMAGES: string[] = [
  img('photo-1507525428034-b723cf961d3e', 2000),
  img('photo-1566073771259-6a8506099945', 2000),
  img('photo-1520250497591-112f2f40a3f4', 2000),
];

export const SERVICES: ServiceItem[] = [
  { label: 'Séjours Tunisie', description: 'Hôtels en bord de mer', icon: 'beach', anchor: '#hotels-tunisie' },
  { label: 'Hôtels monde', description: '+500 000 hôtels', icon: 'hotel', anchor: '#voyages' },
  { label: 'Voyages organisés', description: 'Circuits tout inclus', icon: 'globe', anchor: '#voyages' },
  { label: 'Billets d’avion', description: 'Toutes compagnies', icon: 'plane', anchor: '#vols' },
  { label: 'Ferry CTN', description: 'Traversées & TRE', icon: 'ship', anchor: '#services' },
  { label: 'Visa', description: 'Dossier accompagné', icon: 'passport', anchor: '#omra-visa' },
  { label: 'Omra', description: 'Programmes encadrés', icon: 'moon', anchor: '#omra-visa' },
  { label: 'Groupes & entreprises', description: 'Séminaires, sorties', icon: 'users', anchor: '#contact' },
];

export const TUNISIA_HOTELS: Hotel[] = [
  {
    id: 'azure-bay', name: 'Azure Bay Resort & Spa', city: 'Hammamet', stars: 5, rating: 9.1, reviews: 1284,
    board: 'All inclusive soft', pricePerNight: 245, oldPrice: 299, image: img('photo-1571896349842-33c89424de2d'),
    tags: ['Plage privée', 'Spa'],
  },
  {
    id: 'palm-oasis', name: 'Palm Oasis Djerba', city: 'Djerba', stars: 4, rating: 8.7, reviews: 932,
    board: 'Tout compris', pricePerNight: 168, image: img('photo-1582719508461-905c673771fd'),
    tags: ['Aquapark', 'Famille'],
  },
  {
    id: 'corniche-pearl', name: 'Corniche Pearl Hotel', city: 'Sousse', stars: 4, rating: 8.4, reviews: 2140,
    board: 'Demi-pension', pricePerNight: 119, oldPrice: 145, image: img('photo-1551882547-ff40c63fe5fa'),
    tags: ['Centre-ville', 'Piscine'],
  },
  {
    id: 'dar-yasmine', name: 'Dar El Yasmine', city: 'Tunis', stars: 5, rating: 9.4, reviews: 486,
    board: 'Petit-déjeuner', pricePerNight: 310, image: img('photo-1590490360182-c33d57733427'),
    tags: ['Boutique', 'Vue mer'],
  },
  {
    id: 'sahara-dunes', name: 'Sahara Dunes Lodge', city: 'Tozeur', stars: 4, rating: 9.0, reviews: 377,
    board: 'Demi-pension', pricePerNight: 189, image: img('photo-1542314831-068cd1dbfeeb'),
    tags: ['Désert', 'Excursions'],
  },
  {
    id: 'marina-suites', name: 'Marina Suites Monastir', city: 'Monastir', stars: 5, rating: 8.9, reviews: 815,
    board: 'All inclusive soft', pricePerNight: 228, oldPrice: 265, image: img('photo-1566073771259-6a8506099945'),
    tags: ['Marina', 'Golf'],
  },
  {
    id: 'medina-palace', name: 'Medina Palace', city: 'Hammamet', stars: 4, rating: 8.6, reviews: 1650,
    board: 'Tout compris', pricePerNight: 142, image: img('photo-1520250497591-112f2f40a3f4'),
    tags: ['Animation', 'Kids club'],
  },
  {
    id: 'coral-mahdia', name: 'Coral Beach Mahdia', city: 'Mahdia', stars: 5, rating: 9.2, reviews: 703,
    board: 'Tout compris', pricePerNight: 215, image: img('photo-1584132967334-10e028bd69f7'),
    tags: ['Pieds dans l’eau', 'Thalasso'],
  },
];

export const DESTINATIONS: Destination[] = [
  { id: 'istanbul', name: 'Istanbul', country: 'Turquie', durationDays: 8, priceFrom: 1890, image: img('photo-1524231757912-21f4fe3a7200'), noVisa: true, badge: 'Best-seller' },
  { id: 'dubai', name: 'Dubaï', country: 'Émirats', durationDays: 6, priceFrom: 3290, image: img('photo-1512453979798-5ea266f8880c') },
  { id: 'paris', name: 'Paris', country: 'France', durationDays: 7, priceFrom: 2790, image: img('photo-1502602898657-3e91760cbb34') },
  { id: 'cairo', name: 'Le Caire & Louxor', country: 'Égypte', durationDays: 8, priceFrom: 2190, image: img('photo-1539650116574-8efeb43e2750'), badge: 'Croisière Nil' },
  { id: 'marrakech', name: 'Marrakech', country: 'Maroc', durationDays: 6, priceFrom: 1690, image: img('photo-1597212618440-806262de4f6b'), noVisa: true },
  { id: 'rome', name: 'Rome', country: 'Italie', durationDays: 6, priceFrom: 2490, image: img('photo-1552832230-c0197dd311b5') },
];

export const FLIGHT_DEALS: FlightDeal[] = [
  { city: 'Paris', airportCode: 'ORY', priceFrom: 690 },
  { city: 'Istanbul', airportCode: 'IST', priceFrom: 590 },
  { city: 'Dubaï', airportCode: 'DXB', priceFrom: 1290 },
  { city: 'Rome', airportCode: 'FCO', priceFrom: 520 },
  { city: 'Le Caire', airportCode: 'CAI', priceFrom: 740 },
  { city: 'Montréal', airportCode: 'YUL', priceFrom: 2150 },
];

export const PROMO_IMAGES = {
  earlyBooking: img('photo-1540541338287-41700207dee6', 1000),
  kidsFree: img('photo-1530053969600-caed2596d242', 1000),
  flights: img('photo-1436491865332-7a61a109cc05', 1400),
  omra: img('photo-1591604129939-f1efa4d9f7fa', 1200),
};

export const TESTIMONIALS: Testimonial[] = [
  { name: 'Amira B.', city: 'Sfax', trip: 'Séjour à Djerba', rating: 5, text: 'Réservation simple, hôtel conforme aux photos et une équipe disponible même le week-end. On repart avec Gezenthi cet été !' },
  { name: 'Mehdi K.', city: 'Tunis', trip: 'Voyage à Istanbul', rating: 5, text: 'Programme bien organisé, guide francophone et hôtel très bien situé. Excellent rapport qualité-prix.' },
  { name: 'Sonia & Karim', city: 'Paris (TRE)', trip: 'Ferry + Hammamet', rating: 5, text: 'Billets de bateau et hôtel réservés en une seule fois. Un vrai gain de temps pour nos vacances au pays.' },
];
