import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError } from './api-error';

export interface RoomResult {
  id: string;
  name: string;
  totalPrice: number;
  available: boolean;
  refundable: boolean;
  cancellationDeadline: string | null;
}

export interface BoardResult {
  id: string | null;
  code: string | null;
  name: string;
  rooms: RoomResult[];
}

/** origin: 'agence' = own offer (admin), 'partenaire' = live supplier offer. ref → hotel page. */
export interface HotelResult {
  offerId: string;
  ref: string | null;
  origin: 'agence' | 'partenaire';
  name: string;
  city: string;
  stars: number | null;
  image: string | null;
  currency: string;
  fromPrice: number;
  tags: string[];
  boards: BoardResult[];
}

export interface SearchResponse {
  city: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  hotels: HotelResult[];
  warnings: string[];
}

export interface SearchCriteria {
  city: string;
  checkIn: string; // YYYY-MM-DD
  checkOut: string;
  adults: number;
  children: number[]; // ages
}

/** Hotel page content. description/notes = sanitised HTML from the server. */
export interface HotelPage {
  ref: string;
  origin: 'agence' | 'partenaire';
  name: string;
  city: string;
  stars: number | null;
  address: string | null;
  images: string[];
  description: string | null;
  notes: string | null;
  facilities: string[];
  tags: string[];
  themes: string[];
  boards: string[];
  checkIn: string | null;
  checkOut: string | null;
  latitude: string | null;
  longitude: string | null;
}

export interface PartnerCard {
  ref: string;
  name: string;
  city: string;
  stars: number | null;
  image: string;
}

/** Public hotel API: search, hotel page, single-hotel availability, partner showcase. */
@Injectable({ providedIn: 'root' })
export class HotelSearchService {
  private readonly http = inject(HttpClient);

  search(c: SearchCriteria): Promise<SearchResponse> {
    return this.get<SearchResponse>('/api/public/hotels/search', this.params(c).set('city', c.city));
  }

  hotel(ref: string): Promise<HotelPage> {
    return this.get<HotelPage>(`/api/public/hotels/${encodeURIComponent(ref)}`);
  }

  /** null = no availability for these dates. */
  availability(ref: string, c: Omit<SearchCriteria, 'city'>): Promise<HotelResult | null> {
    return this.get<HotelResult | null>(`/api/public/hotels/${encodeURIComponent(ref)}/availability`, this.params(c));
  }

  featuredPartners(limit = 8): Promise<PartnerCard[]> {
    return this.get<PartnerCard[]>('/api/public/hotels/partners/featured', new HttpParams().set('limit', limit));
  }

  private params(c: Omit<SearchCriteria, 'city'>): HttpParams {
    return new HttpParams()
      .set('checkIn', c.checkIn)
      .set('checkOut', c.checkOut)
      .set('adults', c.adults)
      .set('children', c.children.join(','));
  }

  private async get<T>(url: string, params?: HttpParams): Promise<T> {
    try {
      return await firstValueFrom(this.http.get<T>(url, { params }));
    } catch (err) {
      throw ApiError.from(err);
    }
  }
}
