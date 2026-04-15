# Internet Banking System

A full-stack internet banking application built with React + TypeScript (frontend) and Java 21 + Spring Boot 3 (backend).

**Technologies:** React 18 + TypeScript + Vite + Ant Design, Java 21 + Spring Boot 3, PostgreSQL, Docker Compose

## Running the application

**Requirement:** Docker Desktop (with Docker Compose)

```bash
docker compose up --build
```

Once running:

| Service | Address |
|---------|---------|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api |
| PostgreSQL | localhost:5432 |

> The first build may take a few minutes (pulling images, compiling).

## Demo credentials

The system automatically creates demo accounts on startup.

| Email | Password | Role | Notes |
|-------|----------|------|-------|
| admin@bank.local | Admin123! | Administrator | Full admin access |
| alice.customer@bank.local | Customer123! | Customer | 2 accounts, sample transactions |
| brian.customer@bank.local | Customer123! | Customer | Account with pending block request |
| locked.customer@bank.local | Customer123! | Customer | Access locked (3 failed login attempts) |

### Login flow (2FA)

1. Enter email and password on the `/login` page
2. Retrieve the OTP code from the backend logs:
   ```bash
   docker compose logs backend | grep "OTP for"
   ```
3. Enter the 6-digit code on the `/verify-otp` page

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| APP_JWT_SECRET | change-me-... | JWT secret (change in production!) |
| APP_SEED_DEMO_DATA | true | `false` = disable demo data seeding |
| APP_ADMIN_EMAIL | admin@bank.local | Bootstrap admin email |
| APP_ADMIN_PASSWORD | Admin123! | Bootstrap admin password |

## Stopping the application

```bash
docker compose down
```

To also remove the database volume:
```bash
docker compose down -v
```

## Project structure

```
internet-banking-system/
├── backend/          # Spring Boot 3, Java 21
├── frontend/         # React 18 + TypeScript + Vite
├── docs/             # API documentation
│   ├── api-contract.md
│   ├── openapi.yaml
│   └── RUNNING.md    # Detailed startup guide and curl examples
└── docker-compose.yml
```

## API documentation

- [docs/api-contract.md](docs/api-contract.md) — endpoint descriptions and DTOs
- [docs/openapi.yaml](docs/openapi.yaml) — OpenAPI 3.0 specification
- [docs/RUNNING.md](docs/RUNNING.md) — curl examples for every endpoint

## Running backend tests

```bash
mkdir -p ~/.m2 && docker run --rm \
  -v "$(pwd)/backend:/app" \
  -v "$HOME/.m2:/root/.m2" \
  -w /app \
  maven:3.9.8-eclipse-temurin-21 \
  mvn test
```

`mkdir -p ~/.m2` creates the Maven cache directory if it does not exist yet (required before the first run).
Tests use an H2 in-memory database (`@ActiveProfiles("test")`) — no running PostgreSQL required.
