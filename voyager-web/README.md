# voyage-web

Angular chart UI for voyage-log: MapLibre map of Tallinn bay harbours,
NgRx SignalStore state, deep links into the Telegram bot for berth
requests and trip logging.

## Stack

Angular 21 (standalone, zoneless) · @ngrx/signals · maplibre-gl · SCSS

## Design concept

Chart-plotter aesthetic, not a generic admin dashboard: dark navy chart
background, paper-cream instrument panel, amber/teal/coral for
state (default / hosted / selected). Coordinates and depths render in
IBM Plex Mono, like sounding numbers on a paper chart.

## Structure

```
src/app/
  core/
    models/        harbour.model.ts, trip.model.ts — mirror backend DTOs
    api/            HarbourService, TripService (HttpClient)
  state/
    harbour.store.ts   NgRx SignalStore: list + selection + load()
    trip.store.ts       NgRx SignalStore: 30s poll of /api/trips/active
  features/chart/
    chart-map.component.ts     MapLibre map, reads/writes HarbourStore
    harbour-panel.component.ts  floating info panel, reads HarbourStore
    chart-page.component.ts     container: full-bleed map + panel
```

State flows one way: `ChartMapComponent` calls `harbourStore.load()` once
on map `load`, then an `effect()` pushes `harbourStore.harbours()` into
the MapLibre GeoJSON source on every change. A marker click calls
`harbourStore.select(id)`; the panel reads `harbourStore.selected()` —
no `@Input`/`@Output` wiring between the two components.

## Running

```bash
npm install
npm start
```

Expects the backend on `http://localhost:8080` (see `environments/environment.ts`).
Make sure CORS on the backend includes `http://localhost:4200`
(`voyage.cors-allowed-origins` in `application.yml`).

## Map tiles

Ships with raster OpenStreetMap tiles — zero API keys, works immediately.
For the sharper vector look from the mockup, swap `rasterStyle()` in
`chart-map.component.ts` for a MapTiler vector style:

```ts
style: `https://api.maptiler.com/maps/streets-v2-dark/style.json?key=${YOUR_KEY}`
```

Free tier covers this project's traffic comfortably.

## Telegram deep links

`harbour-panel.component.ts` builds `t.me/<bot>?start=berth_<id>` and
`?start=sail_<id>`. The bot's `/start` handler needs to parse that
payload — not implemented yet on the backend, tracked in voyage-log's
own roadmap.

## Not yet built

- Boats-at-sea layer on the map (TripStore data is polled but not rendered
  as markers yet — `chart-map.component.ts` has a placeholder effect)
- `/start <payload>` handling in the bot for the deep links above
- Mobile bottom-sheet variant of the panel (currently a fixed top-left card)
