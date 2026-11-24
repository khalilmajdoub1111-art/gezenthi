import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../core/services/api-error';
import { CatalogStore } from '../core/services/catalog-store';
import { Icon } from '../shared/icon/icon';
import { imageFileToDataUrl } from '../shared/image-utils';
import { EntityConfig, FieldDef, findEntity } from './admin-entities';

type Item = Record<string, unknown> & { id: string; published: boolean };

@Component({
  selector: 'app-entity-form',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    @if (loadError(); as err) {
      <div class="rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">
        {{ err }} <a [routerLink]="['/admin', entity()]" class="ml-2 underline">Retour à la liste</a>
      </div>
    } @else if (!ready()) {
      <p class="text-slate-500">Chargement…</p>
    }
    @if (ready() && cfg) {
      <a [routerLink]="['/admin', cfg.key]" class="text-sm text-slate-500 hover:text-brand-600">← {{ cfg.label }}</a>
      <h1 class="mt-1 font-display text-3xl font-bold text-brand-950">
        {{ existing ? 'Modifier' : 'Ajouter' }} {{ existing ? '« ' + existing[cfg.titleField] + ' »' : 'un ' + cfg.singular }}
      </h1>

      <form [formGroup]="form" (ngSubmit)="save()" class="mt-8 rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-100 sm:p-8" novalidate>
        <div class="grid gap-5 md:grid-cols-2">
          @for (f of cfg.fields; track f.key) {
            <div [class]="f.wide || f.type === 'checkbox' ? 'md:col-span-2' : ''">
              @switch (f.type) {
                @case ('checkbox') {
                  <label class="flex cursor-pointer items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3">
                    <input type="checkbox" [formControlName]="f.key" class="size-5 accent-brand-600" />
                    <span>
                      <span class="block text-sm font-semibold text-brand-950">{{ f.label }}</span>
                      @if (f.hint) { <span class="block text-xs text-slate-500">{{ f.hint }}</span> }
                    </span>
                  </label>
                }
                @case ('image') {
                  <span class="mb-1.5 block text-sm font-semibold text-brand-950">{{ f.label }} @if (f.required) {<span class="text-rose-500">*</span>}</span>
                  <div class="flex flex-col gap-4 sm:flex-row">
                    <div class="grid aspect-[4/3] w-full shrink-0 place-items-center overflow-hidden rounded-2xl bg-slate-100 sm:w-56">
                      @if (form.controls[f.key].value) {
                        <img [src]="form.controls[f.key].value" alt="Aperçu" class="h-full w-full object-cover" />
                      } @else {
                        <app-icon name="upload" [size]="28" class="text-slate-400" />
                      }
                    </div>
                    <div class="flex-1 space-y-3">
                      <label class="inline-flex cursor-pointer items-center gap-2 rounded-xl bg-brand-50 px-4 py-2.5 text-sm font-semibold text-brand-700 transition hover:bg-brand-100">
                        <app-icon name="upload" [size]="16" /> {{ uploading() ? 'Import en cours…' : 'Importer une photo' }}
                        <input type="file" accept="image/*" class="sr-only" (change)="upload($event, f)" />
                      </label>
                      @if (isDataUrl(form.controls[f.key].value)) {
                        <p class="flex items-center gap-2 text-sm text-slate-600">
                          Photo importée depuis votre appareil
                          <button type="button" class="font-semibold text-rose-600 hover:underline" (click)="form.controls[f.key].setValue('')">Retirer</button>
                        </p>
                      } @else {
                        <input type="url" [formControlName]="f.key" placeholder="…ou collez le lien d’une image (https://…)" [class]="inputClass" />
                      }
                      @if (uploadError()) { <p class="text-sm text-rose-600">{{ uploadError() }}</p> }
                    </div>
                  </div>
                }
                @default {
                  <label class="block">
                    <span class="mb-1.5 block text-sm font-semibold text-brand-950">{{ f.label }} @if (f.required) {<span class="text-rose-500">*</span>}</span>
                    @switch (f.type) {
                      @case ('select') {
                        <select [formControlName]="f.key" [class]="inputClass">
                          @for (o of f.options; track o.value) { <option [ngValue]="o.value">{{ o.label }}</option> }
                        </select>
                      }
                      @case ('textarea') {
                        <textarea [formControlName]="f.key" rows="4" [placeholder]="f.placeholder ?? ''" [class]="inputClass"></textarea>
                      }
                      @case ('number') {
                        <input type="number" [formControlName]="f.key" [attr.min]="f.min ?? null" [attr.max]="f.max ?? null" [attr.step]="f.step ?? 1" [placeholder]="f.placeholder ?? ''" [class]="inputClass" />
                      }
                      @default {
                        <input type="text" [formControlName]="f.key" [placeholder]="f.placeholder ?? ''" [class]="inputClass" />
                      }
                    }
                    @if (f.hint) { <span class="mt-1 block text-xs text-slate-500">{{ f.hint }}</span> }
                  </label>
                }
              }
              @if (showError(f.key)) {
                <p class="mt-1 text-xs font-medium text-rose-600">{{ errorText(f) }}</p>
              }
            </div>
          }
        </div>

        @if (saveError()) {
          <p class="mt-6 rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700 ring-1 ring-rose-200" role="alert">{{ saveError() }}</p>
        }

        <div class="mt-8 flex flex-col-reverse gap-3 border-t border-slate-100 pt-6 sm:flex-row sm:justify-end">
          <a [routerLink]="['/admin', cfg.key]" class="rounded-xl px-5 py-3 text-center font-semibold text-slate-600 ring-1 ring-slate-200 transition hover:bg-slate-50">Annuler</a>
          <button type="submit" [disabled]="saving()" class="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-6 py-3 font-semibold text-white shadow-lg shadow-brand-600/25 transition hover:bg-brand-700 disabled:opacity-60">
            <app-icon name="check" [size]="18" [stroke]="2.4" /> {{ saving() ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
        </div>
      </form>
    }
  `,
})
export class EntityForm implements OnInit {
  /** Bound from route parameters. */
  readonly entity = input.required<string>();
  readonly id = input<string>();

  private readonly store = inject(CatalogStore);
  private readonly router = inject(Router);

  protected cfg?: EntityConfig;
  protected existing?: Item;
  protected form = new FormGroup<Record<string, FormControl>>({});
  protected readonly submitted = signal(false);
  protected readonly uploading = signal(false);
  protected readonly uploadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly ready = signal(false);
  protected readonly saving = signal(false);
  protected readonly inputClass =
    'w-full rounded-xl bg-slate-50 px-4 py-3 text-sm text-brand-950 ring-1 ring-slate-200 transition placeholder:text-slate-400 focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-400';

  async ngOnInit(): Promise<void> {
    this.cfg = findEntity(this.entity());
    if (!this.cfg) {
      void this.router.navigateByUrl('/admin');
      return;
    }
    const id = this.id();
    if (id) {
      try {
        this.existing = (await this.store.get(this.cfg.key, id)) as unknown as Item;
      } catch (err) {
        this.loadError.set(ApiError.from(err).message);
        return;
      }
    }
    const source = { ...this.cfg.defaults, ...(this.existing ?? {}) };
    for (const f of this.cfg.fields) {
      const raw = source[f.key];
      const value = f.type === 'tags' ? ((raw as string[] | undefined) ?? []).join(', ') : f.type === 'checkbox' ? Boolean(raw) : (raw ?? (f.type === 'number' ? null : ''));
      this.form.addControl(f.key, new FormControl(value, this.validators(f)));
    }
    this.ready.set(true);
  }

  protected isDataUrl(value: unknown): boolean {
    return typeof value === 'string' && value.startsWith('data:');
  }

  protected showError(key: string): boolean {
    const c = this.form.controls[key];
    return !!c && c.invalid && (c.touched || this.submitted());
  }

  protected errorText(f: FieldDef): string {
    const e = this.form.controls[f.key].errors ?? {};
    if (e['required']) return 'Champ obligatoire.';
    if (e['min']) return `Minimum : ${f.min}.`;
    if (e['max']) return `Maximum : ${f.max}.`;
    if (e['pattern']) return 'Lien invalide (doit commencer par https://).';
    if (e['server']) return String(e['server']);
    return 'Valeur invalide.';
  }

  protected async upload(event: Event, f: FieldDef): Promise<void> {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    if (!file) return;
    this.uploadError.set(null);
    this.uploading.set(true);
    try {
      this.form.controls[f.key].setValue(await imageFileToDataUrl(file));
    } catch (err) {
      this.uploadError.set(err instanceof Error ? err.message : 'Import impossible.');
    } finally {
      this.uploading.set(false);
    }
  }

  protected async save(): Promise<void> {
    this.submitted.set(true);
    this.saveError.set(null);
    if (this.form.invalid || !this.cfg || this.saving()) return;

    const item: Item = { ...(this.cfg.defaults as object), ...(this.existing ?? {}) } as Item;
    for (const f of this.cfg.fields) {
      const v = this.form.controls[f.key].value;
      if (f.type === 'tags') item[f.key] = String(v ?? '').split(',').map((s) => s.trim()).filter(Boolean);
      else if (f.type === 'number') item[f.key] = v === null || v === '' ? null : Number(v);
      else if (f.type === 'text' || f.type === 'textarea') item[f.key] = String(v ?? '').trim();
      else item[f.key] = v;
    }

    this.saving.set(true);
    try {
      const saved = (await this.store.save(this.cfg.key, item as never)) as unknown as Item;
      const title = String(saved[this.cfg.titleField]);
      void this.router.navigate(['/admin', this.cfg.key], { state: { flash: `« ${title} » a été enregistré.` } });
    } catch (err) {
      const apiErr = ApiError.from(err);
      // Show server-side validation messages under the matching fields.
      for (const [field, message] of Object.entries(apiErr.fieldErrors)) {
        this.form.controls[field]?.setErrors({ server: message });
      }
      this.saveError.set(apiErr.message);
    } finally {
      this.saving.set(false);
    }
  }

  private validators(f: FieldDef): ValidatorFn[] {
    const v: ValidatorFn[] = [];
    if (f.required && f.type !== 'checkbox') v.push(Validators.required);
    if (f.min !== undefined) v.push(Validators.min(f.min));
    if (f.max !== undefined) v.push(Validators.max(f.max));
    if (f.type === 'image') v.push(Validators.pattern(/^(https:\/\/|data:image\/)/));
    return v;
  }
}
