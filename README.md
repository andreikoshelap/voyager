# Voyager Infra

Monorepo for `voyage-log`: a Telegram-based sailing log with a web chart UI.

The project tracks active trips, shows harbours on a MapLibre chart, lets a skipper
start trips from Telegram or from the web UI, and runs overdue/alert handling for
trips that do not check in on time.

## Applications

- `voyager-log` - Spring Boot backend, Telegram bot, PostgreSQL/Flyway schema, trip and harbour APIs.
- `voyager-web` - Angular frontend with MapLibre chart, harbour panel, Telegram deep links, and active-trip boat markers.

## Current Capabilities

- Harbour directory from Flyway migrations, including registry fields:
  `harbourCode`, `nickname`, `address`, `website`, `email`, `amenities`, `sourceUrl`.
- Telegram commands:
  - `/sail <departure> <hours> [crew]`
  - `/status [tripId]`
  - `/back [tripId]`
  - `/harbour <name|code|nickname>`
- Telegram deep link flow from web:
  `https://t.me/<bot>?start=sail_<harbourId>`.
- Multiple simultaneous active trips.
- Active trip markers move linearly from departure harbour to destination/approximate point over trip time.
- Overdue flow:
  `AT_SEA -> OVERDUE -> ALERTED`.

## Requirements

- Java 21
- Node.js/npm compatible with Angular 21
- PostgreSQL
- Telegram bot token for bot features

## Environment

The backend reads these variables:

```text
DB_URL=jdbc:postgresql://localhost:5433/voyagelog
DB_USER=voyagelog
DB_PASSWORD=voyagelog
TELEGRAM_BOT_TOKEN=123:abc
TELEGRAM_WEBHOOK_SECRET=change-me
PUBLIC_BASE_URL=https://voyage.gatto-piccolo.com
```

`.env.example` contains the database variables used by the root `docker-compose.yml`.

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d db
```

The root compose file currently has stale `api`/`web` service definitions from an older template.
Use it for `db` only until those services are cleaned up.

Run the backend in local long-polling mode:

```bash
./gradlew.bat :voyager-log:bootRun --args="--spring.profiles.active=local"
```

On Unix-like shells:

```bash
./gradlew :voyager-log:bootRun --args="--spring.profiles.active=local"
```

If the bot had a webhook configured before, remove it once before local polling:

```bash
curl "https://api.telegram.org/bot<TOKEN>/deleteWebhook"
```

Run the frontend:

```bash
cd voyager-web
npm install
npm start
```

Frontend: `http://localhost:4200`

Backend API: `http://localhost:8080/api`

## Build And Verify

Backend:

```bash
./gradlew.bat test
```

Frontend:

```bash
cd voyager-web
npm run build
```

Angular currently warns that `maplibre-gl` is not ESM. That is an optimization warning, not a build failure.

## Repo Layout

```text
.
├── voyager-log/
│   ├── src/main/java/ee/voyagelog/
│   │   ├── bot/          Telegram command dispatcher and chat state
│   │   ├── harbour/      Harbour entity, repository, API
│   │   ├── trip/         Trip entity, service, active-trip API
│   │   ├── alert/        Overdue notification ports
│   │   └── telegram/     Telegram HTTP client, webhook/long polling
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       ├── application-prod.yml
│       └── db/migration/ Flyway migrations
├── voyager-web/
│   └── src/app/
│       ├── core/         API clients and DTO models
│       ├── state/        NgRx SignalStore state
│       └── features/chart/
│           ├── chart-map.component.*
│           ├── harbour-panel.component.*
│           └── chart-page.component.*
├── gradlew.bat
├── settings.gradle
└── docker-compose.yml
```

## Harbour Lookup Rules

Telegram harbour input resolves in this order:

1. exact harbour code, e.g. `EEKJK`
2. exact nickname, e.g. `Pirita`
3. exact official name
4. official name substring

`Pirita` intentionally maps to `Pirita (Kalevi Jahtklubi)`.
`PiritaTop` maps to `Pirita sadam`.

## Multiple Active Trips

The backend allows more than one active trip at the same time, including
multiple active trips for the same skipper.

Every trip gets a unique numeric id. The bot shows it as `Рейс №<id>` after
registration; use that id for precise status and check-in commands:

```text
/status
/status 42
/back 42
```

- `/status` lists all active trips for the current skipper, newest first.
- `/status <tripId>` shows one active trip owned by the current skipper.
- `/back <tripId>` closes that specific active trip.
- `/back` without an id is kept for compatibility and closes the newest active
  trip for the current skipper.

The public active-trip API returns all active trips:

```text
GET /api/trips/active
```

The web chart renders each active trip as a boat marker. Marker position is
interpolated from departure harbour to destination/approximate point according
to `departedAt` and `etaReturn`; it is not live GPS tracking.

## Notes

- Flyway owns schema changes; Hibernate runs with `ddl-auto: validate`.
- Active-trip API returns all active trips, not only the current skipper's trips.
- `/back <tripId>` is the preferred check-in command when several trips are active.
- The bot is a convenience layer, not a replacement for normal seamanship, VHF, charged phone, life jackets, and emergency procedures.
