/**
 * Domain models shared by the UI.
 * These shapes are the contract the future Spring Boot API should return,
 * so keep them in sync with the backend DTOs once they exist.
 */

export const BOARD_TYPES = ['Tout compris', 'All inclusive soft', 'Demi-pension', 'Petit-déjeuner', 'Logement seul'] as const;
export type BoardType = (typeof BOARD_TYPES)[number];

/** Fields every manageable offer shares. */
export interface CatalogEntity {
  /** Assigned by the server (UUID). */
  id: string;
  /** Hidden from the public site when false (draft / sold out). */
  published: boolean;
  /** Last modification, set by the server (ISO date). */
  updatedAt?: string;
}

export interface Hotel extends CatalogEntity {
  name: string;
  region: 'tunisie' | 'monde';
  city: string;
  stars: 3 | 4 | 5;
  rating: number; // guest rating out of 10
  reviews: number;
  board: BoardType;
  pricePerNight: number; // TND, per person
  oldPrice?: number | null; // TND, shown struck-through when on promo
  image: string;
  tags: string[];
}

/** Organised trip abroad ("Voyages à l'étranger"). */
export interface Destination extends CatalogEntity {
  name: string;
  country: string;
  durationDays: number;
  priceFrom: number; // TND, per person
  image: string;
  noVisa?: boolean;
  badge?: string | null;
}

export interface OmraProgram extends CatalogEntity {
  title: string;
  durationDays: number;
  departure: string; // free text, e.g. "15 mars 2027"
  hotelDistance?: number | null; // metres from the Haram
  priceFrom: number; // TND, per person
  image: string;
  includes: string[];
}

export interface FlightDeal extends CatalogEntity {
  city: string;
  airportCode: string;
  priceFrom: number; // TND, return trip
}

export const SERVICE_KINDS = ['Visa', 'Ferry CTN', 'TRE'] as const;
export type ServiceKind = (typeof SERVICE_KINDS)[number];

/** On-request services: visa files, CTN ferry crossings, TRE packs. */
export interface ServiceOffer extends CatalogEntity {
  kind: ServiceKind;
  title: string;
  priceFrom?: number | null; // TND
  description: string;
}

/** Everything the admin can manage. Keys are also the admin URL segments. */
export interface CatalogData {
  hotels: Hotel[];
  trips: Destination[];
  omra: OmraProgram[];
  flights: FlightDeal[];
  services: ServiceOffer[];
}
export type CatalogKey = keyof CatalogData;

export interface ServiceItem {
  label: string;
  description: string;
  icon: IconName;
  anchor: string;
}

export interface Testimonial {
  name: string;
  city: string;
  text: string;
  trip: string;
  rating: number;
}

export interface NavLink {
  label: string;
  href: string;
}

export interface NavGroup {
  title?: string;
  links: NavLink[];
}

export interface NavItem {
  label: string;
  href: string;
  /** Optional dropdown; several groups render as a mega-menu. */
  groups?: NavGroup[];
}

export type IconName =
  | 'beach' | 'hotel' | 'globe' | 'plane' | 'ship' | 'passport' | 'moon' | 'users'
  | 'search' | 'calendar' | 'pin' | 'star' | 'phone' | 'mail' | 'menu' | 'close'
  | 'chevron-down' | 'chevron-right' | 'arrow-right' | 'shield' | 'tag' | 'headset'
  | 'award' | 'heart' | 'clock' | 'check' | 'whatsapp' | 'facebook' | 'instagram' | 'tiktok'
  | 'plus' | 'edit' | 'trash' | 'eye' | 'eye-off' | 'logout' | 'grid' | 'upload' | 'refresh' | 'external';
