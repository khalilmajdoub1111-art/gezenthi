import { Injectable } from '@angular/core';
import {
  DESTINATIONS,
  FLIGHT_DEALS,
  HERO_IMAGES,
  PROMO_IMAGES,
  SERVICES,
  TESTIMONIALS,
  TUNISIA_HOTELS,
} from '../data/mock-catalog';
import { Destination, FlightDeal, Hotel, ServiceItem, Testimonial } from '../models/travel.models';

/**
 * Single entry point for catalogue data.
 * Today it serves local mock data; when the Spring Boot API is ready,
 * only this class changes (HttpClient calls), components stay untouched.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  heroImages(): string[] {
    return HERO_IMAGES;
  }

  promoImages(): typeof PROMO_IMAGES {
    return PROMO_IMAGES;
  }

  services(): ServiceItem[] {
    return SERVICES;
  }

  tunisiaHotels(): Hotel[] {
    return TUNISIA_HOTELS;
  }

  destinations(): Destination[] {
    return DESTINATIONS;
  }

  flightDeals(): FlightDeal[] {
    return FLIGHT_DEALS;
  }

  testimonials(): Testimonial[] {
    return TESTIMONIALS;
  }
}
