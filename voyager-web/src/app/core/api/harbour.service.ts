import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Harbour, HarbourFeatureCollection, harbourFromFeature } from '../models/harbour.model';

@Injectable({ providedIn: 'root' })
export class HarbourService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Harbour[]> {
    return this.http
      .get<HarbourFeatureCollection>(`${environment.apiBaseUrl}/harbours`)
      .pipe(map((fc) => fc.features.map(harbourFromFeature)));
  }
}
