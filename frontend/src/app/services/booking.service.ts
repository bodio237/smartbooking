import { Injectable } from '@angular/core';
import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';


export interface Booking {

  id: number;

  resourceId: number;

  userId: number;

  startTime: string;

  endTime: string;

  status: string;

  notes: string | null;
}


export interface CreateBookingRequest {

  resourceId: number;

  startTime: string;

  endTime: string;

  notes?: string;
}


export interface BookingStatusRequest {

  status: string;
}


@Injectable({
  providedIn: 'root'
})
export class BookingService {

  private readonly apiUrl =
    'http://localhost:8080/api/bookings';


  constructor(
    private http: HttpClient
  ) {}


  getResourceBookings(
    resourceId: number
  ): Observable<Booking[]> {

    return this.http.get<Booking[]>(
      `${this.apiUrl}/resource/${resourceId}`
    );
  }


  getMyBookings(): Observable<Booking[]> {

    return this.http.get<Booking[]>(
      `${this.apiUrl}/me`
    );
  }


  createBooking(
    request: CreateBookingRequest,
    idempotencyKey: string
  ): Observable<Booking> {

    return this.http.post<Booking>(
      this.apiUrl,
      request,
      {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      }
    );
  }


  cancelBooking(
    bookingId: number
  ): Observable<Booking> {

    return this.http.patch<Booking>(
      `${this.apiUrl}/${bookingId}/cancel`,
      {}
    );
  }


  changeStatus(
    bookingId: number,
    status: string
  ): Observable<Booking> {

    const request: BookingStatusRequest = {
      status
    };

    return this.http.patch<Booking>(
      `${this.apiUrl}/${bookingId}/status`,
      request
    );
  }

}