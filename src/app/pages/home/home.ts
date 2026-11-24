import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CatalogService } from '../../core/services/catalog.service';
import { HotelCard } from '../../shared/hotel-card/hotel-card';
import { Icon } from '../../shared/icon/icon';
import { RevealDirective } from '../../shared/reveal.directive';
import { HeroSearch } from './hero-search/hero-search';

@Component({
  selector: 'app-home',
  imports: [HeroSearch, HotelCard, Icon, RevealDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  templateUrl: './home.html',
})
export class Home {
  private readonly catalog = inject(CatalogService);

  protected readonly heroImages = this.catalog.heroImages();
  protected readonly promo = this.catalog.promoImages();
  protected readonly services = this.catalog.services();
  protected readonly destinations = this.catalog.destinations();
  protected readonly flights = this.catalog.flightDeals();
  protected readonly testimonials = this.catalog.testimonials();
  private readonly hotels = this.catalog.tunisiaHotels();

  /* Hero slideshow */
  protected readonly slide = signal(0);

  /* Hotel city filter */
  protected readonly cities = ['Tous', ...new Set(this.hotels.map((h) => h.city))];
  protected readonly city = signal('Tous');
  protected readonly filteredHotels = computed(() =>
    this.city() === 'Tous' ? this.hotels : this.hotels.filter((h) => h.city === this.city()),
  );

  /* Flash-sale countdown (demo: ends 2 days 9 hours after page load) */
  private readonly saleEnd = Date.now() + (2 * 24 + 9) * 3_600_000;
  private readonly now = signal(Date.now());
  protected readonly countdown = computed(() => {
    const ms = Math.max(0, this.saleEnd - this.now());
    const pad = (n: number) => String(n).padStart(2, '0');
    return [
      { label: 'Jours', value: pad(Math.floor(ms / 86_400_000)) },
      { label: 'Heures', value: pad(Math.floor(ms / 3_600_000) % 24) },
      { label: 'Min', value: pad(Math.floor(ms / 60_000) % 60) },
      { label: 'Sec', value: pad(Math.floor(ms / 1000) % 60) },
    ];
  });

  protected readonly reasons = [
    { icon: 'tag' as const, title: 'Meilleur prix garanti', text: 'Des tarifs négociés directement avec nos hôtels partenaires.' },
    { icon: 'shield' as const, title: 'Réservation sécurisée', text: 'Vos données et paiements sont protégés à chaque étape.' },
    { icon: 'headset' as const, title: 'Assistance 7j/7', text: 'Une équipe joignable avant, pendant et après votre voyage.' },
    { icon: 'award' as const, title: 'Agence agréée', text: 'Licence officielle et partenaires de confiance.' },
  ];

  protected readonly stats = [
    { value: '15 000+', label: 'voyageurs satisfaits' },
    { value: '850+', label: 'hôtels partenaires' },
    { value: '40+', label: 'destinations' },
    { value: '4.9/5', label: 'note moyenne' },
  ];

  constructor() {
    const slideTimer = setInterval(() => this.slide.update((i) => (i + 1) % this.heroImages.length), 6000);
    const clockTimer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(slideTimer);
      clearInterval(clockTimer);
    });
  }
}
