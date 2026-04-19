# Security Model

## Overview

The application uses a layered security model:

1. **Two-factor authentication** — password + time-limited OTP before a JWT is issued
2. **JWT-based stateless sessions** — every request is self-contained
3. **Role-based access control (RBAC)** — three roles with strict endpoint separation
4. **Account locking** — automatic after repeated failures, manual admin unlock
5. **Account blocking** — customer-initiated or admin-initiated workflow
6. **Audit logging** — every security-relevant action is recorded with severity

---

## Two-Factor Authentication (2FA)

Login is a two-step process. A JWT is only issued after both steps succeed.

### Step 1 — Credentials

`POST /api/auth/login` with `{ email, password }`.

1. Email is looked up in the database.
2. Account status is checked — must be `ACTIVE` (see [Account Locking](#account-locking)).
3. Password is verified against the stored BCrypt hash.
4. If the password is correct:
   - `failedLoginAttempts` is reset to 0.
   - A random 6-digit OTP code is generated.
   - An `OtpSession` row is created: `expiresAt = now + 5 minutes`, `used = false`.
   - The OTP code is written to the application log (`[DEV] OTP for <email>: <code>`).
   - The response contains `{ status: "OTP_REQUIRED", otpSessionId }`.
5. If the password is wrong, `failedLoginAttempts` is incremented (see below).

### Step 2 — OTP Verification

`POST /api/auth/verify-otp` with `{ otpSessionId, otpCode }`.

1. `OtpSession` is looked up by ID — must exist and `used = false`.
2. `expiresAt` is checked — must not have passed.
3. `otpCode` is compared — must match exactly.
4. On success:
   - `used` is set to `true`.
   - `user.lastLoginAt` is updated.
   - A JWT is generated and returned with the user profile.
5. On failure: `IllegalArgumentException` → HTTP 400.

### OTP in Production

In this implementation, OTP codes are printed to the server log. A production system
would replace `BootstrapService`/`AuthService` logging with an email or SMS adapter.

---

## JWT Tokens

### Generation

Issued by `JwtService.generateToken()` after successful OTP verification.

| Field | Value |
|---|---|
| Algorithm | HMAC-SHA256 (`HS256`) |
| Subject | User email |
| Claims | `role` (e.g. `"ROLE_CUSTOMER"`), `userId` (UUID string) |
| Issued at | Current instant |
| Expiry | 1 hour |
| Secret | `APP_JWT_SECRET` environment variable (must be ≥ 32 chars) |

### Validation — `JwtAuthenticationFilter`

This filter runs before every request on protected routes:

1. Reads the `Authorization: Bearer <token>` header.
2. Passes the token to `JwtService.validateToken()` — checks signature and expiry.
3. Extracts the email claim and loads the user from the database.
4. Checks `user.accountStatus == ACTIVE` — a locked or blocked user is rejected even with a valid token.
5. Sets `SecurityContextHolder` with the user and role.

If any step fails, the filter does not set a SecurityContext and Spring Security
returns 401.

### Token Storage (Frontend)

The JWT is stored in `localStorage` under the key `"token"`. It is attached to
every API request as `Authorization: Bearer <token>` by `api/client.ts`.

---

## Role-Based Access Control

Three roles are assigned at registration or by initial data seeding:

| Role | Assigned to | API prefix |
|---|---|---|
| `CUSTOMER` | Regular bank clients | `/api/customer/**` |
| `ADMIN` | Bank staff with full access | `/api/admin/**` |
| `EMPLOYEE` | Bank staff with limited access | `/api/employee/**` |

**Access rules** (evaluated in order by Spring Security):

```
/api/health          → public
/api/auth/**         → public
/api/me              → authenticated (any role)
/api/customer/**     → CUSTOMER only
/api/admin/**        → ADMIN only
everything else      → authenticated
```

Accessing a route with the wrong role returns HTTP 403 Forbidden.
Accessing a protected route without a token returns HTTP 401 Unauthorized.

---

## Account Locking

Tracks repeated login failures to prevent brute-force attacks.

| Event | Effect |
|---|---|
| Wrong password | `failedLoginAttempts` incremented by 1 |
| 3rd consecutive wrong password | Status → `LOCKED_LOGIN_FAILURE`, `failedLoginAttempts` stays at 3 |
| Correct password | `failedLoginAttempts` reset to 0 |

A locked account (`LOCKED_LOGIN_FAILURE`) is rejected at two points:
- During login (step 1 of 2FA) — `IllegalStateException` → HTTP 423.
- During JWT validation (`JwtAuthenticationFilter`) — rejects subsequent requests.

**Unlocking** requires an admin action: `POST /api/admin/users/{userId}/unlock-access`.
This resets `accountStatus → ACTIVE` and `failedLoginAttempts → 0`.

---

## Account Blocking

Bank accounts (not user logins) can be blocked through two separate paths.

### Customer-Initiated Block

```
Customer → POST /api/customer/accounts/{accountId}/request-block
  1. Account must be ACTIVE (no existing PENDING request)
  2. Account status → PENDING_BLOCK
  3. BlockRequest created (status: PENDING) with customer's reason

Admin → POST /api/admin/block-requests/{requestId}/approve
  1. BlockRequest → APPROVED
  2. Account status → BLOCKED

Admin → POST /api/admin/block-requests/{requestId}/reject
  1. BlockRequest → REJECTED
  2. Account status reverted to ACTIVE
```

Only one pending request per account is allowed at a time.

### Admin-Initiated Block

`POST /api/admin/accounts/{accountId}/block`

1. Account status immediately → `BLOCKED`.
2. Any existing `PENDING` block request for that account is automatically `APPROVED`.

### Effect of a Blocked Account

Transfers and payments from a `BLOCKED` or `PENDING_BLOCK` account throw
`IllegalArgumentException`. The check is in `CustomerService` before any balance
mutation.

### Unblocking

`POST /api/admin/accounts/{accountId}/unblock` → account status → `ACTIVE`.

---

## Audit Log

All security-relevant actions are recorded in the `operation_records` table by
`OperationService`. The admin can view the full log at `GET /api/admin/operations`.

**Recorded events and their severity:**

| Event type | Severity | Trigger |
|---|---|---|
| `LOGIN_FAILURE` | WARNING | Wrong password |
| `LOGIN_SUCCESS` | SUCCESS | OTP verified, JWT issued |
| `OTP_VERIFIED` | INFO | OTP code accepted |
| `CUSTOMER_REGISTERED` | INFO | New customer created |
| `TRANSFER_CREATED` | INFO | Transfer submitted |
| `PAYMENT_CREATED` | INFO | Payment submitted |
| `STATEMENT_DOWNLOADED` | INFO | CSV statement exported |
| `HISTORY_DOWNLOADED` | INFO | Transaction history exported |
| `ACCOUNT_BLOCK_REQUESTED` | WARNING | Customer submitted block request |
| `ACCOUNT_BLOCKED` | CRITICAL | Account blocked (admin or approved request) |
| `ACCOUNT_UNBLOCKED` | INFO | Account unblocked |
| `ACCESS_UNBLOCKED` | INFO | Locked user unlocked by admin |

Each record stores: who performed the action (`actor` UUID + `actorEmail`),
their role, what was acted upon (`target`), and a human-readable `description`.
`actor` is nullable for system-generated events.

---

## Password Storage

Passwords are hashed with `BCryptPasswordEncoder` (Spring Security default).
Plain-text passwords are never stored or logged.

---

## CSRF & Session

CSRF protection is **disabled** — the API is stateless and uses Bearer tokens,
not cookies, so CSRF is not applicable.

Session creation policy is set to `STATELESS` — Spring never creates or uses
an HTTP session.

---

## Known Limitations

| Area | Limitation |
|---|---|
| OTP delivery | OTP is only logged to console. A real delivery channel (email, SMS) is not implemented. |
| Concurrent transfers | No database-level locking on balance updates. Concurrent transfers on the same account could create a race condition. |
| Token revocation | Issued JWTs cannot be invalidated before expiry (no token blacklist or short-lived refresh flow). |
| Rate limiting | No rate limiting on login or OTP endpoints — brute-force is only slowed by the 3-attempt lock. |
| JWT secret rotation | The JWT secret is a static environment variable with no rotation mechanism. |
