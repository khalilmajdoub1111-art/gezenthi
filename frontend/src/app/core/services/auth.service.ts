import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, firstValueFrom, throwError } from 'rxjs';
import { ApiError } from './api-error';

export interface AdminUser {
  email: string;
  displayName: string;
  role: string;
}

/**
 * Admin authentication against the Spring Boot API.
 * The session lives in an HttpOnly cookie set by the server: nothing secret is stored in JavaScript.
 * CSRF: the server sets an XSRF-TOKEN cookie; Angular's HttpClient sends it back as X-XSRF-TOKEN.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly user = signal<AdminUser | null>(null);
  private checked = false;

  /** Current admin, asking the server once per page load. */
  async me(): Promise<AdminUser | null> {
    if (this.checked) return this.user();
    try {
      this.user.set(await firstValueFrom(this.http.get<AdminUser>('/api/auth/me')));
    } catch {
      this.user.set(null);
    }
    this.checked = true;
    return this.user();
  }

  async login(email: string, password: string): Promise<AdminUser> {
    await this.ensureCsrf();
    try {
      const user = await firstValueFrom(this.http.post<AdminUser>('/api/auth/login', { email, password }));
      this.user.set(user);
      this.checked = true;
      await this.ensureCsrf(); // token is rotated by the server after login
      return user;
    } catch (err) {
      throw ApiError.from(err);
    }
  }

  async logout(): Promise<void> {
    try {
      await this.ensureCsrf();
      await firstValueFrom(this.http.post<void>('/api/auth/logout', {}));
    } catch {
      /* session may already be gone: logging out locally is enough */
    }
    this.clearSession();
  }

  /** Called when the server says the session is no longer valid. */
  clearSession(): void {
    this.user.set(null);
    this.checked = true;
  }

  private async ensureCsrf(): Promise<void> {
    await firstValueFrom(this.http.get<void>('/api/auth/csrf'));
  }
}

/** Protects /admin pages: asks the server whether the session is valid. */
export const adminGuard: CanActivateFn = async (_route, state) => {
  // inject() must run before any await (injection context).
  const auth = inject(AuthService);
  const router = inject(Router);
  if (await auth.me()) return true;
  return router.createUrlTree(['/admin/login'], { queryParams: { returnUrl: state.url } });
};

/** If an admin API call returns 401 (session expired), go back to the login page. */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && req.url.startsWith('/api/admin/')) {
        auth.clearSession();
        void router.navigate(['/admin/login'], { queryParams: { returnUrl: router.url, expired: 1 } });
      }
      return throwError(() => err);
    }),
  );
};
