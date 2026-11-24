import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { Icon } from '../shared/icon/icon';

@Component({
  selector: 'app-admin-login',
  imports: [Icon, RouterLink, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="relative grid min-h-screen place-items-center overflow-hidden bg-brand-950 px-4 py-12">
      <div class="pointer-events-none absolute -left-32 -top-32 size-[28rem] rounded-full bg-brand-600/30 blur-3xl"></div>
      <div class="pointer-events-none absolute -bottom-32 -right-20 size-96 rounded-full bg-gold-400/15 blur-3xl"></div>

      <div class="relative w-full max-w-md rounded-[28px] bg-white p-8 shadow-2xl sm:p-10">
        <img src="logo.png" alt="Gezenthi Travel Agency" class="mx-auto h-24" />
        <h1 class="mt-6 text-center font-display text-2xl font-bold text-brand-950">Espace administrateur</h1>
        <p class="mt-2 text-center text-sm text-slate-500">Connectez-vous pour gérer les offres du site.</p>

        @if (expired()) {
          <p class="mt-6 rounded-2xl bg-gold-50 px-4 py-3 text-sm text-slate-700 ring-1 ring-gold-200" role="status">
            Votre session a expiré. Merci de vous reconnecter.
          </p>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="mt-6 space-y-4" novalidate>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-brand-950">Email</span>
            <input type="email" formControlName="email" autocomplete="username" [class]="inputClass" />
          </label>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-brand-950">Mot de passe</span>
            <span class="relative block">
              <input [type]="showPassword() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" [class]="inputClass + ' pr-12'" />
              <button type="button" (click)="showPassword.set(!showPassword())"
                      class="absolute inset-y-0 right-0 grid w-12 place-items-center text-slate-400 hover:text-brand-600"
                      [attr.aria-label]="showPassword() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">
                <app-icon [name]="showPassword() ? 'eye-off' : 'eye'" [size]="18" />
              </button>
            </span>
          </label>

          @if (error()) {
            <p class="rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">{{ error() }}</p>
          }

          <button
            type="submit"
            [disabled]="loading()"
            class="flex w-full items-center justify-center gap-2 rounded-2xl bg-brand-600 py-4 font-display font-semibold text-white shadow-lg shadow-brand-600/30 transition hover:bg-brand-700 disabled:opacity-60"
          >
            {{ loading() ? 'Connexion…' : 'Se connecter' }} <app-icon name="arrow-right" [size]="18" />
          </button>
        </form>
        <a routerLink="/" class="mt-5 block text-center text-sm font-medium text-slate-500 hover:text-brand-600">← Retour au site</a>
      </div>
    </div>
  `,
})
export class AdminLogin {
  /** Query params, bound by the router. */
  readonly returnUrl = input<string>();
  readonly expired = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly showPassword = signal(false);
  protected readonly inputClass =
    'w-full rounded-xl bg-slate-50 px-4 py-3 text-sm text-brand-950 ring-1 ring-slate-200 transition focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-400';

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.error.set('Saisissez votre email et votre mot de passe.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      const { email, password } = this.form.getRawValue();
      await this.auth.login(email.trim(), password);
      this.form.reset();
      await this.router.navigateByUrl(this.safeReturnUrl());
    } catch (err) {
      this.error.set(err instanceof Error ? err.message : 'Connexion impossible.');
      this.form.controls.password.reset();
    } finally {
      this.loading.set(false);
    }
  }

  /** Only allow redirects inside the admin (prevents open redirects). */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/admin') && !url.startsWith('/admin/login') && !url.includes('//') ? url : '/admin';
  }
}
