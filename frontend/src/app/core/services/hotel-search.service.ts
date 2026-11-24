import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError } from './api-error';

export interface RoomResult {
  name: string;
  totalPrice: number;
  available: boolean;
  refundable: boolean;
  cancellationDeadline: string | null;
}

export interface BoardResult {
  code: string | null;
  name: string;
  rooms: RoomResult[];
}

/** origin: 'agence' = own offer (admin), 'partenaire' = live supplier offer. */
export interface HotelResult {
  offerId: string;
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

/** Unified hotel search (own hotels + suppliers), GET /api/public/hotels/search. */
@Injectable({ providedIn: 'root' })
export class HotelSearchService {
  private readonly http = inject(HttpClient);

  async search(c: SearchCriteria): Promise<SearchResponse> {
    const params = new HttpParams()
      .set('city', c.city)
      .set('checkIn', c.checkIn)
      .set('checkOut', c.checkOut)
      .set('adults', c.adults)
      .set('children', c.children.join(','));
    try {
      return await firstValueFrom(this.http.get<SearchResponse>('/api/public/hotels/search', { params }));
    } catch (err) {
      throw ApiError.from(err);
    }
  }
}
