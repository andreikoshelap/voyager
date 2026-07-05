import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewEncapsulation,
  effect,
  inject,
  viewChild,
} from '@angular/core';
import maplibregl, { GeoJSONSource, Map as MlMap, MapLayerMouseEvent, Marker } from 'maplibre-gl';
import { HarbourStore } from '../../state/harbour.store';
import { TripStore } from '../../state/trip.store';
import { ActiveTrip } from '../../core/models/trip.model';
import { Harbour } from '../../core/models/harbour.model';

const TALLINN_BAY: [number, number] = [24.75, 59.48];
const TRIP_POSITION_UPDATE_MS = 1_000;

/**
 * MapLibre wrapped in a standalone component. Uses raster OSM tiles so it
 * runs with zero API keys out of the box — swap `rasterStyle()` for a
 * MapTiler vector style (see README) once you have a key, no other changes
 * needed since harbours/trips are wired through GeoJSON sources.
 *
 * UPDATED: trip markers now plot backend-computed markerLat/markerLon
 * directly — no more client-side lookup against the harbour list. A
 * CONFIRMED trip sits exactly on its destination harbour; an APPROXIMATE
 * one sits at a computed point ~1 nm off the departure harbour, styled
 * with a dashed halo so it visibly reads as "somewhere out here", not a
 * precise position — we don't collect GPS, so we never claim one.
 */
@Component({
  selector: 'vl-chart-map',
  standalone: true,
  template: `<div class="map-host" #host></div>`,
  styleUrl: './chart-map.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class ChartMapComponent implements AfterViewInit, OnDestroy {
  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  protected readonly harbourStore = inject(HarbourStore);
  private readonly tripStore = inject(TripStore);

  private map: MlMap | null = null;
  private readonly tripMarkers = new Map<number, Marker>();
  private readonly refreshTrips = () => this.tripStore.refresh(undefined);
  private tripPositionTimer: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      const harbours = this.harbourStore.harbours();
      const source = this.map?.getSource('harbours') as GeoJSONSource | undefined;
      source?.setData({
        type: 'FeatureCollection',
        features: harbours.map((h) => ({
          type: 'Feature',
          geometry: { type: 'Point', coordinates: [h.lon, h.lat] },
          properties: { id: h.id, name: h.name, hasHost: h.hasHost },
        })),
      });
      this.syncActiveTripMarkers();
    });

    effect(() => {
      const selectedId = this.harbourStore.selected()?.id ?? null;
      if (!this.map) return;
      this.map.setPaintProperty('harbour-points', 'circle-color', [
        'case',
        ['==', ['get', 'id'], selectedId ?? -1],
        '#c1502e',
        ['get', 'hasHost'],
        '#2f6f62',
        '#d9a441',
      ]);
    });

    effect(() => {
      this.syncActiveTripMarkers();
    });
  }

  ngAfterViewInit(): void {
    window.addEventListener('focus', this.refreshTrips);
    document.addEventListener('visibilitychange', this.refreshTrips);
    this.tripPositionTimer = setInterval(() => this.syncActiveTripMarkers(), TRIP_POSITION_UPDATE_MS);

    this.map = new maplibregl.Map({
      container: this.host().nativeElement,
      style: this.rasterStyle(),
      center: TALLINN_BAY,
      zoom: 10.5,
    });

    this.map.addControl(new maplibregl.NavigationControl({ showCompass: true }), 'top-right');

    this.map.on('load', () => {
      const map = this.map!;
      map.addSource('harbours', { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });

      map.addLayer({
        id: 'harbour-points',
        type: 'circle',
        source: 'harbours',
        paint: {
          'circle-radius': 8,
          'circle-color': '#d9a441',
          'circle-stroke-width': 2,
          'circle-stroke-color': '#0b2233',
        },
      });

      map.on('click', 'harbour-points', (e: MapLayerMouseEvent) => {
        const id = e.features?.[0]?.properties?.['id'];
        if (typeof id === 'number') this.harbourStore.select(id);
      });
      map.on('mouseenter', 'harbour-points', () => (map.getCanvas().style.cursor = 'pointer'));
      map.on('mouseleave', 'harbour-points', () => (map.getCanvas().style.cursor = ''));

      this.harbourStore.load();
      this.syncActiveTripMarkers();
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('focus', this.refreshTrips);
    document.removeEventListener('visibilitychange', this.refreshTrips);
    if (this.tripPositionTimer != null) {
      clearInterval(this.tripPositionTimer);
    }
    this.tripMarkers.forEach((m) => m.remove());
    this.map?.remove();
  }

  private syncActiveTripMarkers(): void {
    if (!this.map) return;

    const trips = this.tripStore.activeTrips();
    const harbours = this.harbourStore.harbours();
    const seen = new Set<number>();

    for (const trip of trips) {
      const position = this.tripPosition(trip, harbours);
      if (!position) continue;
      seen.add(trip.id);

      let marker = this.tripMarkers.get(trip.id);
      if (!marker) {
        marker = new maplibregl.Marker({ element: this.createTripBoatElement() })
          .setLngLat(position)
          .addTo(this.map);
        this.tripMarkers.set(trip.id, marker);
      } else {
        marker.setLngLat(position);
      }

      marker.getElement().style.setProperty('--trip-bearing', `${this.tripBearing(trip, harbours)}deg`);
      marker.getElement().title = `Heading to ${trip.destination}`;
    }

    for (const [tripId, marker] of this.tripMarkers) {
      if (!seen.has(tripId)) {
        marker.remove();
        this.tripMarkers.delete(tripId);
      }
    }
  }

  private createTripBoatElement(): HTMLElement {
    const el = document.createElement('div');
    el.className = 'trip-boat';
    el.innerHTML = `
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M12 2.6 19.2 20.8 12 17.2 4.8 20.8 12 2.6Z" />
      </svg>
    `;
    return el;
  }

  private tripPosition(trip: ActiveTrip, harbours: Harbour[]): [number, number] | null {
    if (trip.markerLat == null || trip.markerLon == null) return null;

    const end: [number, number] = [trip.markerLon, trip.markerLat];
    const departure = trip.departureHarbourId == null
      ? null
      : harbours.find((h) => h.id === trip.departureHarbourId) ?? null;
    if (!departure) return end;

    const startedAt = Date.parse(trip.departedAt);
    const etaReturn = Date.parse(trip.etaReturn);
    if (!Number.isFinite(startedAt) || !Number.isFinite(etaReturn) || etaReturn <= startedAt) {
      return end;
    }

    const progress = Math.min(1, Math.max(0, (Date.now() - startedAt) / (etaReturn - startedAt)));
    const start: [number, number] = [departure.lon, departure.lat];
    return [
      start[0] + (end[0] - start[0]) * progress,
      start[1] + (end[1] - start[1]) * progress,
    ];
  }

  private tripBearing(trip: ActiveTrip, harbours: Harbour[]): number {
    if (trip.markerLat == null || trip.markerLon == null || trip.departureHarbourId == null) return 0;

    const departure = harbours.find((h) => h.id === trip.departureHarbourId);
    if (!departure) return 0;

    const lon1 = this.toRad(departure.lon);
    const lon2 = this.toRad(trip.markerLon);
    const lat1 = this.toRad(departure.lat);
    const lat2 = this.toRad(trip.markerLat);
    const y = Math.sin(lon2 - lon1) * Math.cos(lat2);
    const x = Math.cos(lat1) * Math.sin(lat2)
      - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
    return (this.toDeg(Math.atan2(y, x)) + 360) % 360;
  }

  private toRad(deg: number): number {
    return deg * Math.PI / 180;
  }

  private toDeg(rad: number): number {
    return rad * 180 / Math.PI;
  }

  private rasterStyle(): maplibregl.StyleSpecification {
    return {
      version: 8,
      sources: {
        osm: {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '© OpenStreetMap contributors',
        },
      },
      layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
    };
  }
}
