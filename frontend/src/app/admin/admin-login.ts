import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DemoAuthService } from '../core/services/demo-auth.service';
import { Icon } from '../shared/icon/icon';

@Component({
  selector: 'app-admin-login',
  imports: [Icon, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="relative grid min-h-screen place-items-center overflow-hidden bg-brand-950 px-4 py-12">
      <div class="pointer-events-none absolute -left-32 -top-32 size-[28rem] rounded-full bg-brand-600/30 blur-3xl"></div>
      <div class="pointer-events-none absolute -bottom-32 -right-20 size-96 rounded-full bg-gold-400/15 blur-3xl"></div>

      <div class="relative w-full max-w-md rounded-[28px] bg-white p-8 shadow-2xl sm:p-10">
        <img src="logo.png" alt="Gezenthi Travel Agency" class="mx-auto h-24" />
        <h1 class="mt-6 text-center font-display text-2xl font-bold text-brand-950">Espace administrateur</h1>
        <p class="mt-2 text-center text-sm text-slate-500">Gérez les hôtels, voyages, Omra, vols et services affichés sur le site.</p>

        <div class="mt-6 flex gap-3 rounded-2xl bg-gold-50 p-4 text-sm text-gold-600 ring-1 ring-gold-200">
          <app-icon name="shield" [size]="20" class="mt-0.5 text-gold-600" />
          <p>
            <strong class="text-brand-950">Mode démonstration.</strong>
            <span class="text-slate-700"> Pas de mot de passe pour l’instant : la vraie connexion sécurisée sera ajoutée avec le serveur (Spring Boot).</span>
          </p>
        </div>

        <button
          type="button"
          (click)="enter()"
          class="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-brand-600 py-4 font-display font-semibold text-white shadow-lg shadow-brand-600/30 transition hover:bg-brand-700"
        >
          Entrer dans l’espace admin <app-icon name="arrow-right" [size]="18" />
        </button>
        <a routerLink="/" class="mt-4 block text-center text-sm font-medium text-slate-500 hover:text-brand-600">← Retour au site</a>
      </div>
    </div>
  `,
})
export class AdminLogin {
  private readonly auth = inject(DemoAuthService);
  private readonly router = inject(Router);

  enter(): void {
    this.auth.enter();
    void this.router.navigateByUrl('/admin');
  }
}
