import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError } from './api-error';
import { NotificationLogEntry } from './booking.service';

export type InquiryType = 'HOTEL' | 'TRIP' | 'OMRA' | 'VISA' | 'FLIGHT' | 'FERRY' | 'OTHER';
export type InquiryStatus = 'NEW' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export const INQUIRY_TYPE_LABELS: Record<InquiryType, string> = {
  HOTEL: 'Hôtel', TRIP: 'Voyage organisé', OMRA: 'Omra', VISA: 'Visa', FLIGHT: 'Vol', FERRY: 'Traversée / TRE', OTHER: 'Autre',
};
export const INQUIRY_STATUS_LABELS: Record<InquiryStatus, string> = {
  NEW: 'Nouvelle', IN_PROGRESS: 'En cours', DONE: 'Traitée', CANCELLED: 'Annulée',
};

/** What a "Demande de devis" is about (pre-filled when the dialog opens). */
export interface QuoteContext {
  type: InquiryType;
  subject: string;
  offerRef?: string | null;
  dateFrom?: string | null;
  dateTo?: string | null;
  adults?: number;
  children?: number;
}

export interface CreateInquiry {
  type: InquiryType;
  subject: string;
  offerRef: string | null;
  dateFrom: string | null;
  dateTo: string | null;
  adults: number;
  children: number;
  message: string;
  customer: { civility: string; firstName: string; lastName: string; email: string; phone: string };
  whatsappOptIn: boolean;
  consent: boolean;
}

export interface InquirySummary {
  id: string;
  reference: string;
  status: InquiryStatus;
  type: InquiryType;
  subject: string;
  dateFrom: string | null;
  dateTo: string | null;
  adults: number;
  children: number;
  customerName: string;
  createdAt: string;
}

export interface InquiryDetail extends Omit<InquirySummary, 'customerName'> {
  offerRef: string | null;
  message: string | null;
  customerCivility: string;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string;
  whatsappOptIn: boolean;
  consentAt: string;
  events: { at: string; actor: string; action: string; details: string | null }[];
  notifications: NotificationLogEntry[];
  channels: { emailReady: boolean; whatsappReady: boolean; agencyEmailSet: boolean };
}

/** Quote requests API + the global "open the quote dialog" state used across the site. */
@Injectable({ providedIn: 'root' })
export class InquiryService {
  private readonly http = inject(HttpClient);

  /** Non-null = the quote dialog is open for this context. */
  readonly dialog = signal<QuoteContext | null>(null);

  open(ctx: QuoteContext): void {
    this.dialog.set(ctx);
  }

  close(): void {
    this.dialog.set(null);
  }

  create(body: CreateInquiry): Promise<{ reference: string }> {
    return this.call(this.http.post<{ reference: string }>('/api/public/inquiries', body));
  }

  list(status?: InquiryStatus | null): Promise<InquirySummary[]> {
    return this.call(this.http.get<InquirySummary[]>('/api/admin/inquiries', { params: status ? new HttpParams().set('status', status) : undefined }));
  }

  detail(id: string): Promise<InquiryDetail> {
    return this.call(this.http.get<InquiryDetail>(`/api/admin/inquiries/${id}`));
  }

  setStatus(id: string, status: InquiryStatus, note: string): Promise<InquiryDetail> {
    return this.call(this.http.post<InquiryDetail>(`/api/admin/inquiries/${id}/status`, { status, note }));
  }

  addNote(id: string, note: string): Promise<InquiryDetail> {
    return this.call(this.http.post<InquiryDetail>(`/api/admin/inquiries/${id}/notes`, { note }));
  }

  private async call<T>(obs: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(obs);
    } catch (err) {
      throw ApiError.from(err);
    }
  }
}
