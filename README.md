# Internet Banking Starter

Minimal starter repository for a local Internet Banking System using:
- React + TypeScript + Vite
- Java 21 + Spring Boot 3
- PostgreSQL
- Docker Compose

## Included
- minimal role-aware frontend shell
- minimal Spring Boot REST API skeleton
- seed-ready admin bootstrap via env vars
- API contract for frontend/backend in `docs/api-contract.md`
- OpenAPI starter in `docs/openapi.yaml`

## Run locally

```bash
docker compose up --build
```

Frontend: `http://localhost:5173`
Backend: `http://localhost:8080`
DB: `localhost:5432`

## Default admin bootstrap
Set in `docker-compose.yml`:
- email: `admin@bank.local`
- password: `Admin123!`

## Notes
This is intentionally minimal. It is meant to be a clean starting point, not a production-ready banking system.
