import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError } from './api-error';

export interface OfferSummary {
  hotelName: string;
  city: string;
  stars: number | null;
  image: string | null;
  checkIn: string;
  checkOut: string;
  nights: number;
  adults: number;
  childAges: number[];
  boardName: string;
  roomName: string;
  totalPrice: number;
  currency: string;
  refundable: boolean;
  cancellationDeadline: string | null;
  available: boolean;
}

export interface PassengerIn {
  type: 'ADULT' | 'CHILD';
  civility: 'Mr' | 'Mrs' | 'Ms' | null;
  firstName: string;
  lastName: string;
}

export interface CreateBooking {
  offerId: string;
  board: number;
  room: number;
  customer: { civility: string; firstName: string; lastName: string; email: string; phone: string };
  passengers: PassengerIn[];
  comment: string;
  consent: boolean;
}

export type BookingStatus = 'NEW' | 'IN_PROGRESS' | 'PRICE_CHECKED' | 'CONFIRMED' | 'CANCELLED' | 'REJECTED';

export interface BookingSummary {
  id: string;
  reference: string;
  status: BookingStatus;
  origin: 'agence' | 'partenaire';
  hotelName: string;
  city: string;
  checkIn: string;
  checkOut: string;
  customerName: string;
  totalPrice: number;
  currency: string;
  createdAt: string;
}

export interface BookingDetail extends Omit<BookingSummary, 'customerName'> {
  hotelRef: string | null;
  stars: number | null;
  adults: number;
  childAges: number[];
  boardName: string;
  roomName: string;
  refundable: boolean;
  cancellationDeadline: string | null;
  customerCivility: string;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string;
  comment: string | null;
  consentAt: string;
  supplierName: string | null;
  supplierBookingId: string | null;
  supplierState: string | null;
  supplierPrice: number | null;
  voucherUrl: string | null;
  supplierBookingEnabled: boolean;
  passengers: { position: number; type: string; civility: string | null; firstName: string; lastName: string; age: number | null }[];
  events: { at: string; actor: string; action: string; details: string | null }[];
}

export interface SupplierCheckResult {
  available: boolean;
  supplierPrice: number | null;
  currency: string | null;
  onRequest: boolean;
  sellPrice: number | null;
  message: string | null;
}

export const STATUS_LABELS: Record<BookingStatus, string> = {
  NEW: 'Nouvelle',
  IN_PROGRESS: 'En cours',
  PRICE_CHECKED: 'Prix vérifié',
  CONFIRMED: 'Confirmée',
  CANCELLED: 'Annulée',
  REJECTED: 'Refusée',
};

/** Booking API: public (offer summary + request) and admin (workflow + supplier actions). */
@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);

  offer(offerId: string, board: number, room: number): Promise<OfferSummary> {
    return this.call(this.http.get<OfferSummary>(`/api/public/offers/${encodeURIComponent(offerId)}`,
      { params: new HttpParams().set('board', board).set('room', room) }));
  }

  create(body: CreateBooking): Promise<{ reference: string; status: string }> {
    return this.call(this.http.post<{ reference: string; status: string }>('/api/public/bookings', body));
  }

  list(status?: BookingStatus | null): Promise<BookingSummary[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.call(this.http.get<BookingSummary[]>('/api/admin/bookings', { params }));
  }

  detail(id: string): Promise<BookingDetail> {
    return this.call(this.http.get<BookingDetail>(`/api/admin/bookings/${id}`));
  }

  setStatus(id: string, status: BookingStatus, note: string): Promise<BookingDetail> {
    return this.call(this.http.post<BookingDetail>(`/api/admin/bookings/${id}/status`, { status, note }));
  }

  addNote(id: string, note: string): Promise<BookingDetail> {
    return this.call(this.http.post<BookingDetail>(`/api/admin/bookings/${id}/notes`, { note }));
  }

  supplierCheck(id: string): Promise<SupplierCheckResult> {
    return this.call(this.http.post<SupplierCheckResult>(`/api/admin/bookings/${id}/supplier/check`, {}));
  }

  supplierBook(id: string): Promise<BookingDetail> {
    return this.call(this.http.post<BookingDetail>(`/api/admin/bookings/${id}/supplier/book`, {}));
  }

  private async call<T>(obs: import('rxjs').Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(obs);
    } catch (err) {
      throw ApiError.from(err);
    }
  }
}
