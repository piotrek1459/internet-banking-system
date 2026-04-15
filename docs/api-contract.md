# Frontend/Backend API Contract

## Base URL
- Backend base path: `/api`
- Frontend expects JSON responses.
- Error shape is standardized.

## Auth flow
1. `POST /api/auth/register` — creates a customer account (HTTP 201).
2. `POST /api/auth/login` — validates email/password; returns OTP session ID.
3. Backend generates random 6-digit OTP (logged to console in dev mode).
4. `POST /api/auth/verify-otp` — validates OTP, returns JWT + UserDto.
5. Frontend stores JWT in localStorage, sends `Authorization: Bearer <token>`.

## Roles
- `ADMIN` → `/admin/**`
- `CUSTOMER` → `/customer/**`
- `EMPLOYEE` → `/employee` (placeholder, future feature)

## Standard error response
```json
{
  "timestamp": "2026-03-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/login"
}
```

HTTP status codes:
- `400` — bad request (validation, wrong password, insufficient balance)
- `401` — missing / invalid / expired JWT
- `403` — authenticated but insufficient role
- `404` — resource not found
- `423` — account/user is blocked or locked

## DTOs

### UserDto
```json
{
  "id": "uuid",
  "email": "alice@bank.local",
  "role": "CUSTOMER",
  "firstName": "Alice",
  "lastName": "Murphy",
  "failedLoginAttempts": 0,
  "isAccessBlocked": false,
  "lastLoginAt": "2026-04-10T10:00:00Z"
}
```

### AccountSummary
```json
{
  "id": "uuid",
  "name": "Everyday Account",
  "type": "Current",
  "accountNumber": "PL10105000997603123456789123",
  "iban": "IE29AIBK93115212341234",
  "currency": "EUR",
  "balance": 8425.18,
  "status": "ACTIVE"
}
```
Status values: `ACTIVE`, `PENDING_BLOCK`, `BLOCKED`

### TransactionDto
```json
{
  "id": "uuid",
  "accountId": "uuid",
  "accountName": "Everyday Account",
  "createdAt": "2026-04-10T10:00:00Z",
  "type": "TRANSFER",
  "title": "Transfer to Bob",
  "description": "For invoice",
  "amount": 250.00,
  "currency": "EUR",
  "direction": "DEBIT",
  "status": "COMPLETED",
  "counterparty": "Bob Smith",
  "reference": "TXN-ABCD1234"
}
```

### OperationRecordDto
```json
{
  "id": "uuid",
  "createdAt": "2026-04-10T10:00:00Z",
  "actorName": "Alice Murphy",
  "actorRole": "CUSTOMER",
  "target": "PL10105000997603123456789123",
  "type": "TRANSFER_CREATED",
  "severity": "INFO",
  "description": "Transfer of 250.00 EUR to Bob Smith"
}
```

### BlockRequestDto
```json
{
  "id": "uuid",
  "userId": "uuid",
  "accountId": "uuid",
  "customerName": "Brian Walsh",
  "customerEmail": "brian@bank.local",
  "accountNumber": "PL30105000997603123456789789",
  "reason": "Card may be compromised",
  "requestedAt": "2026-04-10T10:00:00Z",
  "status": "PENDING"
}
```

### ActionResponse
```json
{ "message": "Transfer completed successfully." }
```

### DownloadFileResponse
```json
{
  "fileName": "statement-PL10105000997603123456789123.csv",
  "mimeType": "text/csv;charset=utf-8",
  "content": "Date,Title,...\n..."
}
```

---

## Endpoints

### Public (no auth)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new customer (201) |
| POST | `/api/auth/login` | Step 1: validate credentials, get OTP session |
| POST | `/api/auth/verify-otp` | Step 2: validate OTP, get JWT |
| GET | `/api/health` | Health check |

### Authenticated (any valid JWT)
| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/api/me` | `UserDto` | Current user profile |

### Customer endpoints (requires `CUSTOMER` role)
| Method | Path | Request / Response | Description |
|--------|------|--------------------|-------------|
| GET | `/api/customer/overview` | → `CustomerOverviewResponse` | Dashboard |
| GET | `/api/customer/accounts` | → `{items: AccountSummary[]}` | All accounts |
| GET | `/api/customer/activity` | → `{accounts: [], items: TransactionDto[]}` | Transaction history |
| POST | `/api/customer/transfers` | `TransferRequest` → `ActionResponse` | Create transfer |
| POST | `/api/customer/payments` | `PaymentRequest` → `ActionResponse` | Create payment |
| POST | `/api/customer/accounts/{accountId}/request-block` | `{reason}` → `ActionResponse` | Request account block |
| GET | `/api/customer/downloads/statement?accountId={uuid}` | → `DownloadFileResponse` | Download statement CSV |
| GET | `/api/customer/downloads/history?accountId={uuid}` | → `DownloadFileResponse` | Download history CSV |

**TransferRequest:**
```json
{
  "sourceAccountId": "uuid",
  "recipientName": "Jan Kowalski",
  "recipientAccountNumber": "PL...",
  "amount": 100.00,
  "description": "Za fakturę"
}
```

**PaymentRequest:**
```json
{
  "sourceAccountId": "uuid",
  "payeeName": "Electric Company",
  "reference": "INV-2025-04",
  "amount": 75.50
}
```

**CustomerOverviewResponse:**
```json
{
  "user": { ...UserDto },
  "totalBalance": 24665.18,
  "activeAccounts": 2,
  "pendingBlockRequests": 0,
  "accounts": [ ...AccountSummary[] ],
  "recentTransactions": [ ...TransactionDto[] ],
  "alerts": []
}
```

### Admin endpoints (requires `ADMIN` role)
| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/api/admin/dashboard` | `AdminDashboardResponse` | Statistics overview |
| GET | `/api/admin/customers` | `{items: AdminCustomerSummary[]}` | All customers |
| GET | `/api/admin/operations` | `{items: OperationRecordDto[]}` | Audit log |
| GET | `/api/admin/security` | `AdminSecurityResponse` | Security queue |
| POST | `/api/admin/users/{userId}/unlock-access` | `ActionResponse` | Restore user access |
| POST | `/api/admin/accounts/{accountId}/block` | `ActionResponse` | Block account |
| POST | `/api/admin/accounts/{accountId}/unblock` | `ActionResponse` | Unblock account |
| POST | `/api/admin/block-requests/{requestId}/approve` | `ActionResponse` | Approve block request |
| POST | `/api/admin/block-requests/{requestId}/reject` | `ActionResponse` | Reject block request |

**AdminDashboardResponse:**
```json
{
  "totalCustomers": 3,
  "totalFunds": 26855.58,
  "blockedUsers": 1,
  "blockedAccounts": 0,
  "pendingBlockRequests": 1,
  "recentCriticalOperations": [ ...OperationRecordDto[] ]
}
```

**AdminSecurityResponse:**
```json
{
  "blockedUsers": [ ...AdminCustomerSummary[] ],
  "pendingRequests": [ ...BlockRequestDto[] ],
  "blockedAccounts": [
    { "accountId": "uuid", "accountName": "...", "accountNumber": "...", "customerName": "..." }
  ]
}
```

## Frontend assumptions
- All protected requests include `Authorization: Bearer <token>` header
- `401` → redirect to login page
- `403` → show not authorized page
- `423` → account or user locked — show appropriate message
- Transaction lists are displayed newest first
- JWT token expiry: 1 hour
