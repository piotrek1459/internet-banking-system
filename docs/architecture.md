# System Architecture

## Overview

Internet Banking System is a full-stack web application with a clear separation between
a React single-page frontend, a Spring Boot REST backend, and a PostgreSQL database.
All three services are orchestrated via Docker Compose.

```
Browser (React SPA)
      │  HTTPS REST  (JSON)
      ▼
Spring Boot API  :8080
      │  JPA / Hibernate
      ▼
PostgreSQL 16    :5432
```

---

## Components

### Frontend — React SPA

| Technology | Version | Purpose |
|---|---|---|
| React | 18.3 | UI rendering |
| TypeScript | 5.6 | Type safety |
| React Router | v6 | Client-side routing |
| Ant Design | 5.x | UI component library |
| Vite | 5.4 | Build tool / dev server |
| Cypress | 13.x | End-to-end testing |

The frontend is served by Nginx in production (Docker). In development, Vite dev server
proxies API calls to the backend.

**Key source directories:**

| Path | Contents |
|---|---|
| `src/features/auth/` | Login, Register, OTP pages + AuthProvider context |
| `src/features/customer/` | Customer dashboard pages |
| `src/features/admin/` | Admin dashboard pages |
| `src/api/client.ts` | HTTP client wrapper |
| `src/types/api.ts` | TypeScript types mirroring backend DTOs |

### Backend — Spring Boot

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.3.5 | Application framework |
| Spring Security | — | Authentication & authorisation |
| Spring Data JPA | — | ORM / repository layer |
| Hibernate | — | JPA implementation |
| JJWT | 0.12.6 | JWT generation and validation |
| Lombok | — | Boilerplate reduction |
| Maven | 3.9.8 | Build tool |

**Key source directories:**

| Path | Contents |
|---|---|
| `controller/` | REST controllers (AuthController, CustomerController, AdminController) |
| `service/` | Business logic (AuthService, CustomerService, AdminService, OperationService) |
| `security/` | SecurityConfig, JwtService, JwtAuthenticationFilter |
| `model/` | JPA entities |
| `repository/` | Spring Data JPA repositories |
| `dto/` | Request and response DTOs |

### Database — PostgreSQL 16

Schema is managed automatically by Hibernate (`ddl-auto: update`).
No SQL migration files are used — tables are created and updated on application startup.

See [database-schema.md](database-schema.md) for the full schema and ERD.

---

## Authentication Flow

The system uses two-factor authentication: password + OTP, then issues a JWT.

```
Client                          Backend
  │                               │
  │  POST /api/auth/login         │
  │  { email, password }          │
  │ ─────────────────────────── ► │  1. Verify credentials
  │                               │  2. Check account is ACTIVE
  │                               │  3. Generate 6-digit OTP (logged to console in dev)
  │                               │  4. Create OtpSession (expires in 5 min)
  │ ◄ ─────────────────────────── │
  │  { status: OTP_REQUIRED,      │
  │    otpSessionId }             │
  │                               │
  │  POST /api/auth/verify-otp    │
  │  { otpSessionId, otpCode }    │
  │ ─────────────────────────── ► │  5. Find session (must be unused, not expired)
  │                               │  6. Validate OTP code
  │                               │  7. Mark session as used
  │                               │  8. Issue JWT (1 hour expiry)
  │ ◄ ─────────────────────────── │
  │  { token, user }              │
  │                               │
  │  GET /api/customer/overview   │
  │  Authorization: Bearer <JWT>  │
  │ ─────────────────────────── ► │  9. JwtAuthenticationFilter validates token
  │                               │  10. Check user account is still ACTIVE
  │                               │  11. Inject SecurityContext
  │ ◄ ─────────────────────────── │  12. Controller handles request
```

**Failed login locking:**
After 3 consecutive wrong passwords, `account_status` is set to `LOCKED_LOGIN_FAILURE`.
The account is locked at step 10 even if a valid JWT is presented.
An admin must manually unlock the account via `POST /api/admin/users/{id}/unlock-access`.

---

## Request Lifecycle

```
HTTP Request
     │
     ▼
JwtAuthenticationFilter
  - Extract Bearer token
  - Validate signature & expiry
  - Check user account_status == ACTIVE
  - Set SecurityContext
     │
     ▼
Spring Security AuthorizationFilter
  - Match URL pattern against role rules
  - 401 if unauthenticated, 403 if wrong role
     │
     ▼
Controller (@RestController)
  - Validate request DTO (Bean Validation)
  - Call service
     │
     ▼
Service (@Service, @Transactional)
  - Business logic
  - Repository calls
  - Record OperationRecord via OperationService
     │
     ▼
GlobalExceptionHandler (@ControllerAdvice)
  - Maps exceptions to HTTP status codes
  - Returns standardised error JSON
```

**Endpoint access rules:**

| Path pattern | Requirement |
|---|---|
| `GET /api/health` | Public |
| `POST /api/auth/**` | Public |
| `GET /api/me` | Any authenticated user |
| `/api/customer/**` | Role `CUSTOMER` only |
| `/api/admin/**` | Role `ADMIN` only |
| Everything else | Any authenticated user |

---

## Data Flow — Transfer Example

A customer initiating a transfer triggers the following chain:

```
Frontend (CustomerPaymentsPage)
  │  POST /api/customer/transfers
  │  { sourceAccountId, recipientAccountNumber, amount, ... }
  ▼
CustomerController.createTransfer()
  ▼
CustomerService.submitTransfer()
  1. Load source BankAccount, verify ownership
  2. Assert account status == ACTIVE
  3. Assert balance >= amount
  4. Deduct amount from source balance → save
  5. Create DEBIT Transaction (TRANSFER, COMPLETED)
  6. Look up recipient account by number
     a. Found & ACTIVE → credit recipient balance, create CREDIT Transaction
     b. Not found → external transfer, no credit leg recorded
  7. OperationService.record(TRANSFER_CREATED, INFO)
  ▼
Response: { message: "operation completed" }
```

---

## Error Handling

All exceptions are caught by `GlobalExceptionHandler` and returned as:

```json
{
  "timestamp": "2026-04-19T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient balance",
  "path": "/api/customer/transfers"
}
```

| Exception | HTTP Status |
|---|---|
| `IllegalArgumentException` | 400 Bad Request |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `EntityNotFoundException` | 404 Not Found |
| `IllegalStateException` | 423 Locked |
| `AccessDeniedException` | 403 Forbidden |
| Any other `Exception` | 500 Internal Server Error |

---

## Demo Data

On startup with `APP_SEED_DEMO_DATA=true`, `BootstrapService` creates:

| User | Email | Password | Role | Status |
|---|---|---|---|---|
| Admin | admin@bank.local | Admin123! | ADMIN | ACTIVE |
| Alice Murphy | alice.customer@bank.local | Customer123! | CUSTOMER | ACTIVE |
| Brian Walsh | brian.customer@bank.local | Customer123! | CUSTOMER | ACTIVE (account PENDING_BLOCK) |
| Locked User | locked.customer@bank.local | Customer123! | CUSTOMER | LOCKED_LOGIN_FAILURE |

Alice has 2 accounts with realistic transaction history.
Brian has 1 account with a pending block request already in the queue.
The locked user demonstrates the account-locking flow without any accounts.

---

## Docker Compose

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  frontend    │     │   backend    │     │      db      │
│  :5173→80   │────►│   :8080      │────►│   :5432      │
│  Nginx SPA   │     │ Spring Boot  │     │ PostgreSQL16  │
└──────────────┘     └──────────────┘     └──────────────┘
```

Start order is enforced: `db` must be healthy before `backend` starts,
`backend` must be up before `frontend` starts.

See [RUNNING.md](RUNNING.md) for startup instructions.
