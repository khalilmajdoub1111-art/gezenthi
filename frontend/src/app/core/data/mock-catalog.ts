import { ServiceItem, Testimonial } from '../models/travel.models';

/**
 * PROTOTYPE DATA ONLY.
 * Hotel names, prices and reviews below are fictional placeholders for the
 * client demo. Offers (hotels, trips, Omra, flights, services) now come from the
 * API / database; only static marketing content remains here.
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
