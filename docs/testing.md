# Testing

## Overview

The project has two layers of automated tests on the backend. The frontend has a
minimal Cypress smoke test.

```
backend/src/test/
├── java/com/example/banking/
│   ├── security/
│   │   └── JwtServiceTest.java           Unit — JWT token logic
│   └── service/
│       ├── AuthServiceTest.java          Unit — registration & login
│       ├── CustomerServiceTest.java      Unit — transfers, payments, block requests
│       ├── AdminServiceTest.java         Unit — admin actions
│       ├── AuthIntegrationTest.java      Integration — auth HTTP endpoints
│       ├── CustomerIntegrationTest.java  Integration — customer endpoint access
│       ├── AdminIntegrationTest.java     Integration — admin endpoint access
│       └── SecurityIntegrationTest.java  Integration — cross-role access rules

frontend/cypress/e2e/
└── auth.cy.ts                            E2E smoke test (login page visible)
```

Run all backend tests:

```bash
cd backend && mvn test
```

---

## Unit Tests

Unit tests use **JUnit 5** with **Mockito**. Repositories and services are mocked —
no database is started. Tests focus on business rules, state transitions,
and exception throwing.

---

### JwtServiceTest

**What it tests:** JWT generation and validation logic inside `JwtService`.

| Test | Scenario | Expected result |
|---|---|---|
| `generateToken_returnsNonNullToken` | Valid user → generate token | Token string is not null |
| `generateToken_subjectIsEmail` | Extract subject from token | Matches original email |
| `generateToken_roleClaimPresent` | Extract role claim | Matches user role |
| `validateToken_validToken` | Validate freshly generated token | Returns `true` |
| `validateToken_tamperedToken` | Alter token signature | Returns `false` |
| `validateToken_emptyString` | Pass empty string | Returns `false` |
| `extractRole_returnsCorrectRole` | Extract role from ADMIN token | Returns `"ROLE_ADMIN"` |

---

### AuthServiceTest

**What it tests:** User registration, login, account locking, and OTP verification
in `AuthService`. All repositories are mocked with Mockito.

**Registration tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `register_savesUserAndCreatesAccount` | Valid new email | User saved, default `BankAccount` created, `CUSTOMER_REGISTERED` operation recorded |
| `register_throwsOnDuplicateEmail` | Email already exists | Throws `IllegalArgumentException` |

**Login tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `login_throwsIfUserNotFound` | Unknown email | Throws `EntityNotFoundException` |
| `login_throwsIfAccountBlocked` | Status ≠ ACTIVE | Throws `IllegalStateException` (→ HTTP 423) |
| `login_throwsOnWrongPassword` | Wrong password | Throws `IllegalArgumentException`, `failedLoginAttempts` incremented |
| `login_locksAccountAfter3Failures` | 3rd consecutive wrong password | Status set to `LOCKED_LOGIN_FAILURE`, `CRITICAL` operation recorded |
| `login_returnsOtpSessionOnSuccess` | Correct credentials | Returns `LoginResponse` with `otpSessionId`, `failedLoginAttempts` reset to 0 |

**OTP verification tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `verifyOtp_throwsOnExpiredSession` | OTP expired | Throws `IllegalStateException` |
| `verifyOtp_throwsOnWrongCode` | Wrong OTP code | Throws `IllegalArgumentException` |
| `verifyOtp_returnsTokenOnSuccess` | Correct code, valid session | Returns JWT, `lastLoginAt` updated, session marked `used=true` |

---

### CustomerServiceTest

**What it tests:** The transfer, payment, and block-request flows in `CustomerService`.

**Transfer tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `submitTransfer_throwsIfAccountNotFound` | Source account ID unknown | Throws `EntityNotFoundException` |
| `submitTransfer_throwsIfInsufficientBalance` | Amount > balance | Throws `IllegalArgumentException` |
| `submitTransfer_throwsIfAccountNotActive` | Account status ≠ ACTIVE | Throws `IllegalArgumentException` |
| `submitTransfer_deductsBalanceAndSavesTransaction` | Valid external transfer | Balance reduced, 1 `DEBIT` transaction saved |
| `submitTransfer_internal_creditsBothSides` | Recipient account exists in system | 2 transactions saved (DEBIT + CREDIT), recipient balance increased |

**Payment tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `submitPayment_throwsIfInsufficientBalance` | Amount > balance | Throws `IllegalArgumentException` |
| `submitPayment_deductsBalanceAndSavesTransaction` | Valid payment | Balance reduced, 1 `PAYMENT` transaction saved |

**Block request tests:**

| Test | Scenario | Expected result |
|---|---|---|
| `requestBlock_setsAccountToPendingBlock` | Active account, no existing request | Account status → `PENDING_BLOCK`, `BlockRequest` created |
| `requestBlock_throwsIfAlreadyPending` | Pending request already exists | Throws `IllegalArgumentException` |

---

### AdminServiceTest

**What it tests:** Admin actions on users, accounts, and block requests in `AdminService`.

| Test | Scenario | Expected result |
|---|---|---|
| `unlockUser_setsStatusActiveAndResetsAttempts` | Locked user | Status → `ACTIVE`, `failedLoginAttempts` → 0, `ACCESS_UNBLOCKED` recorded |
| `unlockUser_throwsIfNotFound` | Unknown user ID | Throws `EntityNotFoundException` |
| `blockAccount_setsStatusBlocked` | Active account | Status → `BLOCKED`, `ACCOUNT_BLOCKED` (CRITICAL) recorded |
| `blockAccount_autoApprovesPendingRequest` | Account with pending block request | Block request status → `APPROVED` automatically |
| `approveBlockRequest_blocksAccount` | Pending request | Request → `APPROVED`, account → `BLOCKED` |
| `approveBlockRequest_throwsIfNotPending` | Already approved request | Throws `IllegalArgumentException` |
| `rejectBlockRequest_revertsAccountToActive` | Pending request, account PENDING_BLOCK | Request → `REJECTED`, account reverted to `ACTIVE` |

---

## Integration Tests

Integration tests use `@SpringBootTest` with **MockMvc** and an **H2 in-memory database**.
They test the HTTP layer — correct status codes, authentication, and role enforcement —
without mocking the application internals.

---

### AuthIntegrationTest

Tests the public authentication endpoints with real HTTP requests.

| Test | Request | Expected HTTP status |
|---|---|---|
| `register_returns201` | `POST /api/auth/register` with valid payload | 201 Created |
| `login_returns404ForUnknownEmail` | `POST /api/auth/login` with unknown email | 404 Not Found |
| `login_returns400ForWrongPassword` | `POST /api/auth/login` with wrong password | 400 Bad Request |
| `login_returns423ForLockedAccount` | `POST /api/auth/login` with locked user | 423 Locked |
| `verifyOtp_returns404ForInvalidSession` | `POST /api/auth/verify-otp` with fake session ID | 404 Not Found |

---

### CustomerIntegrationTest

Tests that customer endpoints enforce authentication and reject incorrect roles.

| Test | Request | Expected HTTP status |
|---|---|---|
| `customer_returns401WithoutToken` | `GET /api/customer/overview` (no auth header) | 401 Unauthorized |
| `customer_returns403ForAdminRole` | `GET /api/customer/overview` as ADMIN | 403 Forbidden |

---

### AdminIntegrationTest

Tests that admin endpoints enforce authentication and reject incorrect roles.

| Test | Request | Expected HTTP status |
|---|---|---|
| `admin_returns401WithoutToken` | `GET /api/admin/dashboard` (no auth header) | 401 Unauthorized |
| `admin_returns403ForCustomerRole` | `GET /api/admin/dashboard` as CUSTOMER | 403 Forbidden |

---

### SecurityIntegrationTest

Broad security checks covering public endpoints and cross-role access.

| Test | Scenario | Expected result |
|---|---|---|
| `healthEndpoint_isPublic` | `GET /api/health` without token | 200 OK |
| `protectedEndpoint_requires401WithoutToken` | Any protected route, no token | 401 |
| `customerEndpoint_forbiddenForAdmin` | `/api/customer/**` as ADMIN | 403 |
| `adminEndpoint_forbiddenForCustomer` | `/api/admin/**` as CUSTOMER | 403 |
| `authEndpoints_arePublic` | `/api/auth/**` | Accessible without token |

---

## Frontend Tests

### Cypress — `auth.cy.ts`

Single smoke test that confirms the application loads and the login page is accessible.

| Test | What it checks |
|---|---|
| `shows login page` | Navigate to `/login`, assert "Secure sign in" heading is visible |

This is a minimal test. It verifies that the frontend build is functional and
the routing works, but does not test any user interactions.

---

## What Is Not Covered

The following areas have no automated test coverage:

| Area | Notes |
|---|---|
| OTP delivery | OTP is only logged to console (`[DEV]` prefix). No email/SMS adapter is tested. |
| CSV statement download | `CustomerService.downloadStatement()` and `downloadHistory()` have no test. |
| Admin dashboard statistics | Aggregation queries (`sumAllBalances`, `countByRole`) are not verified end-to-end. |
| Concurrent transactions | No test for race conditions when two transfers run simultaneously on the same account. |
| Database constraints | Schema-level unique constraints (IBAN, accountNumber) are not tested at integration level. |
| Pagination | Transaction history returns all records; no pagination is implemented or tested. |
| Frontend components | No React unit tests (e.g. Vitest/React Testing Library). |
| Full E2E flows | Cypress only checks page visibility — no login, transfer, or admin workflow is automated. |
| Negative amounts | No validation test for zero or negative transfer amounts. |
| Long strings / special characters | No boundary tests on name or description fields. |
