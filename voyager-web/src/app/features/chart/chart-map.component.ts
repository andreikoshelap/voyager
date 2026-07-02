import { AfterViewInit, Component, ElementRef, OnDestroy, effect, inject, viewChild } from '@angular/core';
import maplibregl, { GeoJSONSource, Map as MlMap, MapLayerMouseEvent } from 'maplibre-gl';
import { HarbourStore } from '../../state/harbour.store';
import { TripStore } from '../../state/trip.store';

const TALLINN_BAY: [number, number] = [24.75, 59.48];

/**
 * MapLibre wrapped in a standalone component. Uses raster OSM tiles so it
 * runs with zero API keys out of the box — swap `rasterStyle()` for a
 * MapTiler vector style (see README) once you have a key, no other changes
 * needed since harbours/trips are wired through GeoJSON sources.
 */
@Component({
  selector: 'vl-chart-map',
  standalone: true,
  template: `<div class="map-host" #host></div>`,
  styleUrl: './chart-map.component.scss',
})
export class ChartMapComponent implements AfterViewInit, OnDestroy {
  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  protected readonly harbourStore = inject(HarbourStore);
  private readonly tripStore = inject(TripStore);

  private map: MlMap | null = null;

  constructor() {
    // Push harbours/trips into the map's GeoJSON sources whenever the
    // stores change — sources are created once in ngAfterViewInit, this
    // effect only ever calls setData() on them.
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
      // trips don't render a shape yet in this skeleton — count is surfaced
      // in the panel; a dedicated boat layer is a natural next step.
      this.tripStore.activeTrips();
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
        id: 'harbour-labels',
        type: 'symbol',
        source: 'harbours',
        layout: {
          'text-field': ['get', 'name'],
          'text-font': ['Noto Sans Regular'],
          'text-size': 12,
          'text-offset': [0, 1.4],
          'text-anchor': 'top',
        },
        paint: {
          'text-color': '#f3eedd',
          'text-halo-color': '#0b2233',
          'text-halo-width': 1.2,
        },
      });

      map.on('click', 'harbour-points', (e: MapLayerMouseEvent) => {
        const id = e.features?.[0]?.properties?.['id'];
        if (typeof id === 'number') this.harbourStore.select(id);
      });
      map.on('mouseenter', 'harbour-points', () => (map.getCanvas().style.cursor = 'pointer'));
      map.on('mouseleave', 'harbour-points', () => (map.getCanvas().style.cursor = ''));

      this.harbourStore.load();
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
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
