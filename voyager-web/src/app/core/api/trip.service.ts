import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ActiveTrip } from '../models/trip.model';

@Injectable({ providedIn: 'root' })
export class TripService {
  private readonly http = inject(HttpClient);

  getActive(): Observable<ActiveTrip[]> {
    return this.http.get<ActiveTrip[]>(`${environment.apiBaseUrl}/trips/active`);
  }
}
