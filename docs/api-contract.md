# Frontend/Backend Contract

## Base URL
- Backend base path: `/api`
- Frontend expects JSON responses.
- Error shape is standardized.

## Auth flow
1. `POST /api/auth/register` creates a customer account.
2. `POST /api/auth/login` validates email/password.
3. If valid, backend returns `OTP_REQUIRED` and sends OTP by email.
4. `POST /api/auth/verify-otp` validates OTP and returns JWT plus current user.
5. Frontend stores JWT and sends `Authorization: Bearer <token>`.

## Roles
- `ADMIN`
- `EMPLOYEE`
- `CUSTOMER`

Frontend uses `user.role` to route:
- `ADMIN` -> `/admin`
- `EMPLOYEE` -> `/employee`
- `CUSTOMER` -> `/customer`

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

## DTOs

### Login request
```json
{
  "email": "john@example.com",
  "password": "Secret123!"
}
```

### Login response (step 1)
```json
{
  "status": "OTP_REQUIRED",
  "message": "OTP sent to email",
  "otpSessionId": "9b5f7fd7-56d9-4d18-aab1-7fba77e615cc"
}
```

### Verify OTP request
```json
{
  "otpSessionId": "9b5f7fd7-56d9-4d18-aab1-7fba77e615cc",
  "otpCode": "123456"
}
```

### Verify OTP response
```json
{
  "token": "jwt-token-value",
  "user": {
    "id": "8b73c0da-ef32-4fdf-a818-c7c07ef7d325",
    "email": "john@example.com",
    "role": "CUSTOMER",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

### Register request
```json
{
  "email": "john@example.com",
  "password": "Secret123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Account summary
```json
{
  "id": "c1f0f6a3-4c2f-42c1-a464-8113c8b68358",
  "accountNumber": "PL00123456789012345678901234",
  "currency": "PLN",
  "balance": 1500.25,
  "status": "ACTIVE"
}
```

### Transaction item
```json
{
  "id": "4c7f7111-d60f-4f73-912a-c3a82be6f4f0",
  "type": "TRANSFER",
  "title": "Rent",
  "amount": 1200.00,
  "currency": "PLN",
  "status": "COMPLETED",
  "createdAt": "2026-03-28T10:00:00Z"
}
```

## Endpoints

### Public
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/verify-otp`
- `GET /api/health`

### Authenticated
- `GET /api/me`

### Customer
- `GET /api/customer/accounts`
- `GET /api/customer/accounts/{accountId}/transactions`
- `POST /api/customer/transfers`
- `POST /api/customer/block-request`

### Employee/Admin
- `GET /api/employees/customers/{customerId}/operations`
- `PATCH /api/employees/customers/{customerId}/block`
- `PATCH /api/employees/customers/{customerId}/unlock`

### Admin only
- `POST /api/admin/employees`

## Frontend assumptions
- all protected requests include bearer token
- 401 means redirect to login
- 403 means show not authorized page
- `account_locked` type situations return 423 Locked or 403 with message
- transaction lists are displayed newest first
