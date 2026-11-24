/**
 * Domain models shared by the UI.
 * These shapes are the contract the future Spring Boot API should return,
 * so keep them in sync with the backend DTOs once they exist.
 */

export type BoardType = 'Tout compris' | 'All inclusive soft' | 'Demi-pension' | 'Petit-déjeuner';

export interface Hotel {
  id: string;
  name: string;
  city: string;
  stars: 3 | 4 | 5;
  rating: number; // guest rating out of 10
  reviews: number;
  board: BoardType;
  pricePerNight: number; // TND, per person
  oldPrice?: number; // TND, shown struck-through when on promo
  image: string;
  tags: string[];
}

export interface Destination {
  id: string;
  name: string;
  country: string;
  durationDays: number;
  priceFrom: number; // TND, per person
  image: string;
  noVisa?: boolean;
  badge?: string;
}

export interface FlightDeal {
  city: string;
  airportCode: string;
  priceFrom: number; // TND, return trip
}

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
  | 'award' | 'heart' | 'clock' | 'check' | 'whatsapp' | 'facebook' | 'instagram' | 'tiktok';
