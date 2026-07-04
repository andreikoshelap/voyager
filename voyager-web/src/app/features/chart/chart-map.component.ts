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

const TALLINN_BAY: [number, number] = [24.75, 59.48];

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
  private readonly pulseMarkers = new Map<number, Marker>();

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
      const trips = this.tripStore.activeTrips();
      const source = this.map?.getSource('active-trips') as GeoJSONSource | undefined;
      source?.setData(this.activeTripsGeoJson(trips));
      if (!this.map) return;
      this.syncPulseMarkers(trips);
    });
  }

  ngAfterViewInit(): void {
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
      map.addSource('active-trips', { type: 'geojson', data: this.activeTripsGeoJson(this.tripStore.activeTrips()) });

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

      map.addLayer({
        id: 'active-trip-points',
        type: 'circle',
        source: 'active-trips',
        paint: {
          'circle-radius': 7,
          'circle-color': [
            'match',
            ['get', 'status'],
            'OVERDUE',
            '#c1502e',
            'ALERTED',
            '#c1502e',
            '#35a7ff',
          ],
          'circle-stroke-width': 2,
          'circle-stroke-color': '#ffffff',
        },
      });

      map.on('click', 'harbour-points', (e: MapLayerMouseEvent) => {
        const id = e.features?.[0]?.properties?.['id'];
        if (typeof id === 'number') this.harbourStore.select(id);
      });
      map.on('mouseenter', 'harbour-points', () => (map.getCanvas().style.cursor = 'pointer'));
      map.on('mouseleave', 'harbour-points', () => (map.getCanvas().style.cursor = ''));

      this.harbourStore.load();
      this.syncPulseMarkers(this.tripStore.activeTrips());
    });
  }

  ngOnDestroy(): void {
    this.pulseMarkers.forEach((m) => m.remove());
    this.map?.remove();
  }

  /** Reconciles DOM pulse markers with the current active-trip list. */
  private syncPulseMarkers(trips: ActiveTrip[]): void {
    const seen = new Set<number>();

    for (const trip of trips) {
      if (trip.markerLat == null || trip.markerLon == null) continue;
      seen.add(trip.id);

      const isAlarmed = trip.status === 'OVERDUE' || trip.status === 'ALERTED';
      const isApprox = trip.locationConfidence === 'APPROXIMATE';
      let marker = this.pulseMarkers.get(trip.id);

      if (!marker) {
        const el = document.createElement('div');
        el.className = 'trip-pulse';
        marker = new maplibregl.Marker({ element: el })
          .setLngLat([trip.markerLon, trip.markerLat])
          .addTo(this.map!);
        this.pulseMarkers.set(trip.id, marker);
      } else {
        marker.setLngLat([trip.markerLon, trip.markerLat]);
      }

      const el = marker.getElement();
      el.classList.toggle('trip-pulse--alarm', isAlarmed);
      el.classList.toggle('trip-pulse--approx', isApprox);
      el.title = isApprox
        ? `Approximate area — ${trip.destination}`
        : `Heading to ${trip.destination}`;
    }

    for (const [tripId, marker] of this.pulseMarkers) {
      if (!seen.has(tripId)) {
        marker.remove();
        this.pulseMarkers.delete(tripId);
      }
    }
  }

  private activeTripsGeoJson(trips: ActiveTrip[]): GeoJSON.FeatureCollection<GeoJSON.Point> {
    return {
      type: 'FeatureCollection',
      features: trips
        .filter((trip) => trip.markerLat != null && trip.markerLon != null)
        .map((trip) => ({
          type: 'Feature',
          geometry: { type: 'Point', coordinates: [trip.markerLon!, trip.markerLat!] },
          properties: {
            id: trip.id,
            destination: trip.destination,
            status: trip.status,
            locationConfidence: trip.locationConfidence,
          },
        })),
    };
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
