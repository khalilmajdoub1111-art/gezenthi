import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AGENCY_CONTACT } from '../../core/data/navigation';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-footer',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <footer id="contact" class="relative overflow-hidden bg-brand-950 text-brand-100">
      <div class="pointer-events-none absolute -right-32 -top-32 size-96 rounded-full bg-brand-600/20 blur-3xl"></div>
      <div class="pointer-events-none absolute -bottom-40 -left-20 size-96 rounded-full bg-gold-400/10 blur-3xl"></div>

      <div class="relative mx-auto grid max-w-7xl gap-12 px-6 py-16 md:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_1.2fr]">
        <div>
          <div class="inline-block rounded-2xl bg-white p-3"><img src="logo.png" alt="Gezenthi Travel Agency" class="h-16" /></div>
          <p class="mt-5 max-w-xs text-sm leading-relaxed text-brand-200">
            Votre agence de voyages : séjours en Tunisie, hôtels dans le monde, circuits, billetterie, visa et Omra — au meilleur prix,
            avec un accompagnement humain.
          </p>
          <div class="mt-6 flex gap-3">
            @for (s of socials; track s) {
              <a href="#" [attr.aria-label]="s" class="grid size-10 place-items-center rounded-full bg-white/10 transition hover:bg-gold-400 hover:text-brand-950">
                <app-icon [name]="s" [size]="18" />
              </a>
            }
          </div>
        </div>

        @for (col of columns; track col.title) {
          <div>
            <h3 class="font-display text-base font-semibold text-white">{{ col.title }}</h3>
            <ul class="mt-4 space-y-2.5 text-sm">
              @for (l of col.links; track l) {
                <li><a href="#" class="transition hover:text-gold-300">{{ l }}</a></li>
              }
            </ul>
          </div>
        }

        <div>
          <h3 class="font-display text-base font-semibold text-white">Contact</h3>
          <ul class="mt-4 space-y-3 text-sm">
            <li class="flex items-start gap-3"><app-icon name="pin" [size]="18" class="mt-0.5 text-gold-300" />
              <a [href]="contact.mapUrl" target="_blank" rel="noopener noreferrer" class="hover:text-gold-300">{{ contact.address }}</a></li>
            <li class="flex items-center gap-3"><app-icon name="phone" [size]="18" class="text-gold-300" />
              <a [href]="contact.phoneHref" class="hover:text-gold-300">{{ contact.phone }}</a></li>
            <li class="flex items-center gap-3"><app-icon name="mail" [size]="18" class="text-gold-300" />
              <a [href]="'mailto:' + contact.email" class="hover:text-gold-300">{{ contact.email }}</a></li>
            <li class="flex items-center gap-3"><app-icon name="clock" [size]="18" class="text-gold-300" /> Lun – Sam · 8h30 – 18h30</li>
          </ul>
        </div>
      </div>

      <div class="relative border-t border-white/10">
        <div class="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-6 py-6 text-xs text-brand-300 sm:flex-row">
          <p>© {{ year }} Gezenthi Travel Agency. Tous droits réservés.</p>
          <div class="flex gap-5">
            <a href="#" class="hover:text-gold-300">Conditions générales</a>
            <a href="#" class="hover:text-gold-300">Confidentialité</a>
            <a href="#" class="hover:text-gold-300">Mentions légales</a>
          </div>
        </div>
      </div>
    </footer>
  `,
})
export class Footer {
  protected readonly contact = AGENCY_CONTACT;
  protected readonly year = new Date().getFullYear();
  protected readonly socials = ['facebook', 'instagram', 'tiktok', 'whatsapp'] as const;
  protected readonly columns = [
    { title: 'Nos services', links: ['Séjours en Tunisie', 'Hôtels dans le monde', "Voyages à l'étranger", 'Billets d’avion', 'Ferry CTN', 'Visa & Omra'] },
    { title: 'Gezenthi', links: ['Qui sommes-nous', 'Nos agences', 'Espace entreprises', 'Coffret cadeau', 'Blog voyage', 'FAQ'] },
  ];
}
