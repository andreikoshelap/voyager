# Voyager Infra

Monorepo with two applications:

- `voyager-log` - Spring Boot backend
- `voyager-web` - Angular frontend

The system supports:


Files:

- `ledger-api/src/main/resources/application.yml`
- `ledger-api/src/main/resources/application-docker.yml`

## Run Locally

Backend:

```bash
cd ledger-api
./gradlew test
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=prod'
./gradlew bootRun --args='--spring.profiles.active=demo'
```

Use plain `bootRun` for the normal local run. It resolves to the default
`postman` profile and performs the external pre-debit check against Postman
Echo. Use `prod` only when you explicitly want to exercise the task-required
`httpstat.us` endpoint. Use `demo` when you want seeded demo data and no
external pre-debit check.

Frontend:

```bash
cd ledger-web
npm install
ng serve
```
Locally frontend is exposed on `http://localhost:4200`.

## Run With Docker

From repo root:

```bash
docker compose down -v
docker compose up --build -d
```

Frontend is exposed on `http://localhost:8081`.

## Repo Layout

```text

```
