# Running the Application & API Reference

## Documentation

| File | Contents |
|---|---|
| [architecture.md](architecture.md) | System overview, component descriptions, auth flow, data flow |
| [security.md](security.md) | JWT, OTP, account locking, RBAC, audit log |
| [testing.md](testing.md) | Test coverage, what each test does, known gaps |
| [database-schema.md](database-schema.md) | Full schema, all tables and columns, ERD |
| [api-contract.md](api-contract.md) | Request/response DTOs, error format |
| [openapi.yaml](openapi.yaml) | OpenAPI 3.0 spec (import into Postman / Swagger UI) |

---

## Demo Credentials

| User | Email | Password | Role |
|---|---|---|---|
| Admin | admin@bank.local | Admin123! | ADMIN |
| Alice Murphy | alice.customer@bank.local | Customer123! | CUSTOMER |
| Brian Walsh | brian.customer@bank.local | Customer123! | CUSTOMER |
| Locked account | locked.customer@bank.local | Customer123! | CUSTOMER (locked) |

---

## Requirements

- Docker Desktop (with Docker Compose v2)
- Ports 5173, 8080, and 5432 must be free

## Quick start

```bash
git clone <repo-url>
cd internet-banking-system
docker compose up --build
```

## Login flow (2FA)

### Step 1 — Submit credentials

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice.customer@bank.local","password":"Customer123!"}' | jq
```

Response:
```json
{
  "status": "OTP_REQUIRED",
  "message": "OTP sent to email",
  "otpSessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### Step 2 — Retrieve OTP from logs

```bash
docker compose logs backend | grep "OTP for"
# [DEV] OTP for alice.customer@bank.local: 123456
```

### Step 3 — Verify OTP and receive JWT

```bash
curl -s -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"otpSessionId":"<SESSION_ID>","otpCode":"<OTP>"}' | jq
```

The response contains a `token` field — use it as a Bearer token in all subsequent requests.

## API examples

Replace `<TOKEN>` with the JWT from Step 3.

### Customer — account overview

```bash
curl -s http://localhost:8080/api/customer/overview \
  -H "Authorization: Bearer <TOKEN>" | jq
```

### Customer — list accounts

```bash
curl -s http://localhost:8080/api/customer/accounts \
  -H "Authorization: Bearer <TOKEN>" | jq
```

### Customer — transaction history

```bash
curl -s http://localhost:8080/api/customer/activity \
  -H "Authorization: Bearer <TOKEN>" | jq
```

### Customer — create a transfer

```bash
curl -s -X POST http://localhost:8080/api/customer/transfers \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "<ACCOUNT_UUID>",
    "recipientName": "John Smith",
    "recipientAccountNumber": "PL30105000997603123456789789",
    "amount": 100.00,
    "description": "Invoice payment"
  }' | jq
```

### Customer — create a payment

```bash
curl -s -X POST http://localhost:8080/api/customer/payments \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "<ACCOUNT_UUID>",
    "payeeName": "Electric Company",
    "reference": "INV-2025-04",
    "amount": 75.50
  }' | jq
```

### Customer — download statement (CSV)

```bash
curl -s "http://localhost:8080/api/customer/downloads/statement?accountId=<ACCOUNT_UUID>" \
  -H "Authorization: Bearer <TOKEN>" | jq -r '.content' > statement.csv
```

### Customer — request account block

```bash
curl -s -X POST "http://localhost:8080/api/customer/accounts/<ACCOUNT_UUID>/request-block" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Suspicious activity"}' | jq
```

---

### Admin — dashboard

```bash
# Log in as admin first and use the admin token
curl -s http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer <ADMIN_TOKEN>" | jq
```

### Admin — list customers

```bash
curl -s http://localhost:8080/api/admin/customers \
  -H "Authorization: Bearer <ADMIN_TOKEN>" | jq
```

### Admin — operation audit log

```bash
curl -s http://localhost:8080/api/admin/operations \
  -H "Authorization: Bearer <ADMIN_TOKEN>" | jq
```

### Admin — security queue

```bash
curl -s http://localhost:8080/api/admin/security \
  -H "Authorization: Bearer <ADMIN_TOKEN>" | jq
```

### Admin — restore user access

```bash
curl -s -X POST "http://localhost:8080/api/admin/users/<USER_UUID>/unlock-access" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

### Admin — approve block request

```bash
curl -s -X POST "http://localhost:8080/api/admin/block-requests/<REQUEST_UUID>/approve" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

---

## HTTP status codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Resource created (register) |
| 400 | Bad request (validation error, wrong password, insufficient balance) |
| 401 | Missing or invalid JWT token |
| 403 | Insufficient role |
| 404 | Resource not found |
| 423 | Account or user is locked/blocked |
| 500 | Internal server error |

## Roles

| Role | Description | Access |
|------|-------------|--------|
| CUSTOMER | Bank customer | `/api/customer/**`, `/api/me` |
| ADMIN | Administrator | `/api/admin/**`, `/api/me` |
| EMPLOYEE | Bank employee (future feature) | `/api/me` |
