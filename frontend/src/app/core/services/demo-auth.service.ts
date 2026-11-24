import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

const SESSION_KEY = 'gezenthi.admin.demo';

/**
 * DEMO ONLY — this is NOT security. Anyone can enter the admin in the prototype.
 * Real authentication (Spring Security, hashed passwords, server-side sessions/JWT,
 * roles) is mandatory before going live and is planned with the backend.
 */
@Injectable({ providedIn: 'root' })
export class DemoAuthService {
  readonly loggedIn = signal(this.read());

  enter(): void {
    this.write(true);
    this.loggedIn.set(true);
  }

  logout(): void {
    this.write(false);
    this.loggedIn.set(false);
  }

  private read(): boolean {
    try {
      return sessionStorage.getItem(SESSION_KEY) === '1';
    } catch {
      return false;
    }
  }

  private write(value: boolean): void {
    try {
      if (value) sessionStorage.setItem(SESSION_KEY, '1');
      else sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* storage unavailable: in-memory only */
    }
  }
}

export const adminGuard: CanActivateFn = () =>
  inject(DemoAuthService).loggedIn() || inject(Router).createUrlTree(['/admin/login']);
