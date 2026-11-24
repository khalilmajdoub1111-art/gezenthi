import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DemoAuthService } from '../core/services/demo-auth.service';
import { Icon } from '../shared/icon/icon';
import { ADMIN_ENTITIES } from './admin-entities';

@Component({
  selector: 'app-admin-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block min-h-screen bg-slate-50' },
  template: `
    <!-- Sidebar (desktop) -->
    <aside class="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col bg-brand-950 text-brand-100 lg:flex">
      <a routerLink="/admin" class="m-4 rounded-2xl bg-white p-3"><img src="logo.png" alt="Gezenthi" class="mx-auto h-14" /></a>
      <nav class="flex-1 space-y-1 overflow-y-auto px-3" aria-label="Administration">
        <a routerLink="/admin" routerLinkActive="!bg-white/10 !text-white" [routerLinkActiveOptions]="{ exact: true }"
           class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition hover:bg-white/5 hover:text-white">
          <app-icon name="grid" [size]="18" /> Tableau de bord
        </a>
        <p class="px-3 pb-1 pt-5 text-[11px] font-bold uppercase tracking-wider text-brand-400">Offres</p>
        @for (e of entities; track e.key) {
          <a [routerLink]="['/admin', e.key]" routerLinkActive="!bg-white/10 !text-white"
             class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition hover:bg-white/5 hover:text-white">
            <app-icon [name]="e.icon" [size]="18" /> {{ e.label }}
          </a>
        }
      </nav>
      <div class="space-y-1 border-t border-white/10 p-3">
        <a href="/" target="_blank" rel="noopener" class="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition hover:bg-white/5 hover:text-white">
          <app-icon name="external" [size]="18" /> Voir le site
        </a>
        <button type="button" (click)="logout()" class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition hover:bg-white/5 hover:text-white">
          <app-icon name="logout" [size]="18" /> Déconnexion
        </button>
      </div>
    </aside>

    <!-- Top bar (mobile / tablet) -->
    <div class="sticky top-0 z-30 bg-brand-950 text-brand-100 lg:hidden">
      <div class="flex items-center justify-between px-4 py-3">
        <a routerLink="/admin" class="rounded-xl bg-white px-2 py-1"><img src="logo.png" alt="Gezenthi" class="h-9" /></a>
        <div class="flex gap-1">
          <a href="/" target="_blank" rel="noopener" class="grid size-10 place-items-center rounded-xl hover:bg-white/10" aria-label="Voir le site"><app-icon name="external" [size]="18" /></a>
          <button type="button" (click)="logout()" class="grid size-10 place-items-center rounded-xl hover:bg-white/10" aria-label="Déconnexion"><app-icon name="logout" [size]="18" /></button>
        </div>
      </div>
      <nav class="no-scrollbar flex gap-1 overflow-x-auto px-3 pb-3" aria-label="Administration">
        <a routerLink="/admin" routerLinkActive="!bg-gold-400 !text-brand-950" [routerLinkActiveOptions]="{ exact: true }"
           class="shrink-0 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium">Tableau de bord</a>
        @for (e of entities; track e.key) {
          <a [routerLink]="['/admin', e.key]" routerLinkActive="!bg-gold-400 !text-brand-950"
             class="shrink-0 rounded-full bg-white/10 px-4 py-1.5 text-sm font-medium">{{ e.label }}</a>
        }
      </nav>
    </div>

    <div class="lg:pl-64">
      <div class="flex items-center gap-2 border-b border-gold-200 bg-gold-50 px-4 py-2 text-xs text-slate-700 sm:px-8">
        <app-icon name="shield" [size]="14" class="text-gold-600" />
        <span><strong>Mode démo</strong> — les modifications sont enregistrées uniquement dans ce navigateur.</span>
      </div>
      <main class="mx-auto max-w-6xl px-4 py-8 sm:px-8">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AdminShell {
  private readonly auth = inject(DemoAuthService);
  private readonly router = inject(Router);
  protected readonly entities = ADMIN_ENTITIES;

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/admin/login');
  }
}
