# Workflow Demo

Modern workflow approval demo built with Java 21, Spring Boot, React, and TypeScript.

This project is inspired by Japanese internal workflow systems that rely on application forms, approval routes, employee and organization master data, attachments, and approval history.

## Structure

- `backend`: Spring Boot REST API
- `frontend`: React and TypeScript UI
- `infra`: Docker Compose services for local development

## Available Features

- Basic Auth demo login with applicant and approver accounts
- Employee, organization, and position master-data previews
- Application form definitions with dynamic fields
- Draft application creation, application list, and application detail
- Submit action from `DRAFT` to `SUBMITTED`
- Approval task queue for the configured approver
- Approve and reject actions with approval history
- Attachment upload metadata and application detail file list
- Approval route timeline and workflow version snapshot
- Workflow definition view/editor and form definition editor

## Demo Accounts

| Role | Username | Password | Employee |
| --- | --- | --- | --- |
| Applicant | `demo1@growtea.co.jp` | `demo1001` | `山田 太郎` |
| Approver | `demo5@growtea.co.jp` | `demo1005` | `岩瀬 大樹` |

## Local Services

Start PostgreSQL, MinIO, and Mailpit:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Service URLs:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- MinIO console: `http://localhost:9001`
- Mailpit: `http://localhost:8025`

## Main API Endpoints

Public read endpoints:

- `GET /api/health`
- `GET /api/master-data/employees`
- `GET /api/master-data/organizations`
- `GET /api/master-data/positions`
- `GET /api/form-definitions`
- `GET /api/form-definitions/{formCode}`
- `GET /api/workflow-definitions`
- `GET /api/workflow-definitions/{workflowCode}`

Basic Auth endpoints:

- `GET /api/me`
- `GET /api/applications`
- `POST /api/applications/drafts`
- `GET /api/applications/{id}`
- `POST /api/applications/{id}/submit`
- `POST /api/applications/{id}/attachments`
- `GET /api/applications/{id}/attachments`
- `GET /api/applications/{id}/history`
- `GET /api/approval-tasks/pending`
- `POST /api/approval-tasks/{id}/approve`
- `POST /api/approval-tasks/{id}/reject`
- `POST /api/form-definitions`
- `POST /api/workflow-definitions/{workflowCode}/draft`
- `POST /api/workflow-definitions/{workflowCode}/publish`

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

Frontend checks:

```bash
cd frontend
npm run lint
npm run build
```

End-to-end workflow test:

```bash
cd frontend
npx playwright install chromium
npm run e2e
```

The E2E test starts or reuses the Vite dev server on `http://127.0.0.1:5173` and expects the Spring Boot backend to be running on `http://localhost:8080`.
