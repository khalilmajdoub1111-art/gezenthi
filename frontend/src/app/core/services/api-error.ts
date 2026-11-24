import { HttpErrorResponse } from '@angular/common/http';

/** Error returned by the Spring Boot API (RFC 9457 problem details), normalised for the UI. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    /** Field name → message, for validation errors (HTTP 400). */
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }

  static from(err: unknown): ApiError {
    if (err instanceof ApiError) return err;
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) return new ApiError('Serveur injoignable. Vérifiez que le backend est démarré.', 0);
      const body = (err.error ?? {}) as { detail?: string; title?: string; errors?: Record<string, string> };
      return new ApiError(body.detail ?? body.title ?? `Erreur serveur (${err.status}).`, err.status, body.errors ?? {});
    }
    return new ApiError(err instanceof Error ? err.message : 'Erreur inattendue.', -1);
  }
}
