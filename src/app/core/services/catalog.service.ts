import { Injectable, Signal, computed, inject } from '@angular/core';
import { HERO_IMAGES, PROMO_IMAGES, SERVICES, TESTIMONIALS } from '../data/mock-catalog';
import { CatalogData, CatalogKey, ServiceItem, Testimonial } from '../models/travel.models';
import { CatalogStore } from './catalog-store';

/**
 * Read-only catalogue for the PUBLIC site: only published items.
 * Components never touch the store directly, so switching to the
 * Spring Boot API later only affects CatalogStore.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly store = inject(CatalogStore);

  readonly hotels = this.published('hotels');
  readonly trips = this.published('trips');
  readonly omra = this.published('omra');
  readonly flights = this.published('flights');
  readonly services = this.published('services');

  heroImages(): string[] {
    return HERO_IMAGES;
  }

  promoImages(): typeof PROMO_IMAGES {
    return PROMO_IMAGES;
  }

  serviceShortcuts(): ServiceItem[] {
    return SERVICES;
  }

  testimonials(): Testimonial[] {
    return TESTIMONIALS;
  }

  private published<K extends CatalogKey>(key: K): Signal<CatalogData[K]> {
    const all = this.store.list(key);
    return computed(() => (all() as CatalogData[K][number][]).filter((i) => i.published) as CatalogData[K]);
  }
}
