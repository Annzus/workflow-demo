# Workflow Demo

Modern workflow approval demo built with Java 21, Spring Boot, React, and TypeScript.

## Structure

- `backend`: Spring Boot REST API
- `frontend`: React and TypeScript UI
- `infra`: Docker Compose services for local development

## Local Services

Start PostgreSQL, MinIO, and Mailpit:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Service URLs:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- MinIO console: `http://localhost:9001`
- Mailpit: `http://localhost:8025`

## First Checks

Backend tests use an isolated in-memory database. They do not require the local Compose PostgreSQL service.

```bash
cd backend
./mvnw test
```

Backend health:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend dev server:

```bash
cd frontend
npm run dev
```
