import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AGENCY_CONTACT } from '../core/data/navigation';
import { Icon } from '../shared/icon/icon';
import { Footer } from './footer/footer';
import { Header } from './header/header';

/** Shell of the public website (header + page + footer). The admin has its own shell. */
@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, Header, Footer, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <app-header />
    <main>
      <router-outlet />
    </main>
    <app-footer />

    <a
      [href]="contact.phoneHref"
      aria-label="Contacter un conseiller"
      class="fixed bottom-5 right-5 z-40 grid size-14 place-items-center rounded-full bg-gold-400 text-brand-950 shadow-xl shadow-brand-950/25 transition hover:scale-110"
    >
      <span class="absolute inset-0 animate-ping rounded-full bg-gold-400/40"></span>
      <app-icon name="headset" [size]="26" class="relative" />
    </a>
  `,
})
export class PublicLayout {
  protected readonly contact = AGENCY_CONTACT;
}
