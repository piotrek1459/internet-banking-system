# Internet Banking System — Database Schema & ERD

## Overview

The system uses **PostgreSQL 16** as the production database. The schema is managed
automatically by **Hibernate JPA** (`ddl-auto: update`) — there are no SQL migration files.
Demo data is seeded at startup by `BootstrapService` when `APP_SEED_DEMO_DATA=true`.

**Total tables:** 6  
**All primary keys:** UUID (auto-generated)  
**All timestamps:** stored as `TIMESTAMP`  
**Monetary values:** `NUMERIC(19, 2)` — BigDecimal precision

---

## Table Definitions

### 1. `users`

Stores all system users: customers, employees, and admins.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Login identifier |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hash |
| `first_name` | VARCHAR(255) | NOT NULL | |
| `last_name` | VARCHAR(255) | NOT NULL | |
| `role` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `account_status` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `failed_login_attempts` | INTEGER | NOT NULL | Resets on successful login |
| `enabled` | BOOLEAN | NOT NULL | Spring Security flag |
| `created_at` | TIMESTAMP | NOT NULL | Set on insert |
| `last_login_at` | TIMESTAMP | NULLABLE | Updated on OTP verify |

**`role` enum values:** `ADMIN`, `EMPLOYEE`, `CUSTOMER`

**`account_status` enum values:**

| Value | Meaning |
|---|---|
| `ACTIVE` | Normal state |
| `LOCKED_LOGIN_FAILURE` | Locked after 3 consecutive failed logins |
| `BLOCKED_BY_BANK` | Manually blocked by admin |
| `BLOCKED_BY_CUSTOMER_REQUEST` | Customer block request was approved |

---

### 2. `bank_accounts`

Represents individual bank accounts belonging to customers.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `account_number` | VARCHAR(255) | UNIQUE, NOT NULL | Internal account number |
| `iban` | VARCHAR(255) | UNIQUE, NOT NULL | IBAN format |
| `name` | VARCHAR(255) | NOT NULL | Display name (e.g. "Savings Vault") |
| `type` | VARCHAR(255) | NOT NULL | e.g. "Current", "Savings" |
| `owner` | UUID | FK → `users.id`, NOT NULL | Account owner |
| `currency` | VARCHAR(3) | NOT NULL | ISO 4217, default `EUR` |
| `balance` | NUMERIC(19,2) | NOT NULL | Default `0.00` |
| `status` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `created_at` | TIMESTAMP | NOT NULL | Set on insert |

**`status` enum values:**

| Value | Meaning |
|---|---|
| `ACTIVE` | Normal, operational account |
| `PENDING_BLOCK` | Customer submitted a block request, awaiting admin approval |
| `BLOCKED` | Account fully blocked (by admin or approved request) |

---

### 3. `transactions`

Records every financial movement (debit or credit) on a bank account.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `owner` | UUID | FK → `users.id`, NOT NULL | User who owns the account |
| `account` | UUID | FK → `bank_accounts.id`, NOT NULL | Account involved |
| `account_name` | VARCHAR(255) | NOT NULL | Snapshot of account name at time of transaction |
| `created_at` | TIMESTAMP | NOT NULL | Set on insert |
| `type` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `title` | VARCHAR(255) | NOT NULL | Short label |
| `description` | TEXT | NULLABLE | Optional detail |
| `amount` | NUMERIC(19,2) | NOT NULL | Always positive |
| `currency` | VARCHAR(3) | NOT NULL | ISO 4217 |
| `direction` | VARCHAR(50) | NOT NULL | `DEBIT` or `CREDIT` |
| `status` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `counterparty` | VARCHAR(255) | NULLABLE | Recipient or payer name |
| `reference` | VARCHAR(255) | NOT NULL | Unique transaction reference code |

**`type` enum values:** `TRANSFER`, `PAYMENT`, `DEPOSIT`

**`direction` enum values:** `DEBIT` (money out), `CREDIT` (money in)

**`status` enum values:** `COMPLETED`, `PENDING`, `FAILED`

---

### 4. `block_requests`

Tracks customer-initiated requests to block their own bank accounts.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `user` | UUID | FK → `users.id`, NOT NULL | Customer who submitted the request |
| `account` | UUID | FK → `bank_accounts.id`, NOT NULL | Account to be blocked |
| `reason` | TEXT | NOT NULL | Customer-provided reason |
| `requested_at` | TIMESTAMP | NOT NULL | Set on insert |
| `status` | VARCHAR(50) | NOT NULL | ENUM → see below |

**`status` enum values:**

| Value | Meaning |
|---|---|
| `PENDING` | Awaiting admin review |
| `APPROVED` | Admin approved; account status set to `BLOCKED` |
| `REJECTED` | Admin rejected the request |

---

### 5. `otp_sessions`

Stores one-time password codes used for two-factor authentication.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `user` | UUID | FK → `users.id`, NOT NULL | User the OTP belongs to |
| `otp_code` | VARCHAR(255) | NOT NULL | 6-digit numeric code |
| `expires_at` | TIMESTAMP | NOT NULL | Short-lived expiry |
| `used` | BOOLEAN | NOT NULL | `true` after successful verification |

---

### 6. `operation_records`

Audit log for all security-relevant and business-relevant actions in the system.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK | Auto-generated |
| `created_at` | TIMESTAMP | NOT NULL | Set on insert |
| `actor` | UUID | FK → `users.id`, **NULLABLE** | Null for system-generated events |
| `actor_email` | VARCHAR(255) | NOT NULL | Denormalized email snapshot |
| `actor_role` | VARCHAR(50) | NOT NULL | `ADMIN`, `EMPLOYEE`, or `CUSTOMER` |
| `target` | VARCHAR(255) | NOT NULL | What was acted upon (e.g. account ID, user email) |
| `type` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `severity` | VARCHAR(50) | NOT NULL | ENUM → see below |
| `description` | VARCHAR(512) | NOT NULL | Human-readable log message |

**`type` enum values:**

| Value | Description |
|---|---|
| `LOGIN_FAILURE` | Failed login attempt |
| `LOGIN_SUCCESS` | Successful login |
| `OTP_VERIFIED` | OTP code accepted, JWT issued |
| `TRANSFER_CREATED` | Money transfer initiated |
| `PAYMENT_CREATED` | Payment initiated |
| `STATEMENT_DOWNLOADED` | Account statement exported |
| `HISTORY_DOWNLOADED` | Transaction history exported |
| `ACCOUNT_BLOCK_REQUESTED` | Customer submitted a block request |
| `ACCOUNT_BLOCKED` | Account was blocked |
| `ACCOUNT_UNBLOCKED` | Account was unblocked |
| `ACCESS_UNBLOCKED` | Locked user was unlocked by admin |
| `CUSTOMER_REGISTERED` | New customer registered |

**`severity` enum values:** `INFO`, `WARNING`, `CRITICAL`, `SUCCESS`

---

## Entity Relationships

| Relationship | Type | Foreign Key |
|---|---|---|
| `users` → `bank_accounts` | 1 : Many | `bank_accounts.owner` |
| `users` → `transactions` | 1 : Many | `transactions.owner` |
| `users` → `block_requests` | 1 : Many | `block_requests.user` |
| `users` → `otp_sessions` | 1 : Many | `otp_sessions.user` |
| `users` → `operation_records` | 1 : Many (optional) | `operation_records.actor` |
| `bank_accounts` → `transactions` | 1 : Many | `transactions.account` |
| `bank_accounts` → `block_requests` | 1 : Many | `block_requests.account` |

---

## ERD Diagram

[ERD.pdf](ERD.pdf)

---

## Demo Data (seeded on startup)

| User | Email | Role | Status |
|---|---|---|---|
| Admin | admin@bank.local | ADMIN | ACTIVE |
| Alice Murphy | alice.customer@bank.local | CUSTOMER | ACTIVE |
| Brian Walsh | brian.customer@bank.local | CUSTOMER | ACTIVE |
| Locked User | locked.customer@bank.local | CUSTOMER | LOCKED_LOGIN_FAILURE |

Alice has 2 accounts (`ACTIVE`) with 4 transactions. Brian has 1 account (`PENDING_BLOCK`) with 1 pending `BlockRequest`.
