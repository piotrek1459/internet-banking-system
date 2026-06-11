# Demo Guide — Internet Banking System

Step-by-step walkthrough for presenting the application. Covers every major feature
in a logical order, with exact credentials and actions at each step.

---

## 1. Setup

### Requirements

- Docker Desktop (with Compose v2)
- Ports `5173`, `8080`, `5432` must be free

### Start

```bash
git clone <repo-url>
cd internet-banking-system
docker compose up --build
```

Wait until the backend prints `Started BankingApplication` in the logs (~30 s on first run).

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Health check | http://localhost:8080/api/health |

### Getting the OTP code during the demo

In development, OTP codes are printed to the backend log. Open a second terminal:

```bash
docker compose logs -f backend | grep "OTP for"
```

Every login attempt prints a line like:

```
[DEV] OTP for alice.customer@bank.local: 847291
```

---

## 2. Demo Accounts

| Who | Email | Password | State |
|---|---|---|---|
| Customer (Alice) | alice.customer@bank.local | Customer123! | Active, 2 accounts, transactions |
| Customer (Brian) | brian.customer@bank.local | Customer123! | Active, account in pending-block queue |
| Locked customer | locked.customer@bank.local | Customer123! | Locked — 3 failed login attempts |
| Administrator | admin@bank.local | Admin123! | Full admin access |

---

## 3. Customer Flow

### 3.1 Registration

> Show that anyone can create an account.

1. Go to http://localhost:5173
2. Click **Get Started** or **Register**.
3. Fill in: first name, last name, email, password.
4. Submit — the account is created and a default bank account is opened automatically.
5. You are redirected to the login screen.

---

### 3.2 Two-Factor Login (2FA)

> Show the two-step authentication — password first, then OTP.

1. Click **Sign in**.
2. Enter **alice.customer@bank.local** / **Customer123!** and submit.
3. The system asks for a 6-digit code — check the backend log for the OTP.
4. Enter the code and submit.
5. You are redirected to the customer dashboard.

**What this demonstrates:** The system never issues a session token until both the
password and the one-time code are verified. OTP sessions expire after 5 minutes.

---

### 3.3 Dashboard Overview

> Show the customer home screen.

The overview page displays:

- **Total balance** across all accounts.
- **Number of active accounts**.
- **Pending block requests** counter (if any).
- **Account cards** — each shows name, type, IBAN, balance, and current status.
- **Recent transactions** — last few movements with type, amount, and direction.
- **Last login timestamp** — visible in the welcome message.

---

### 3.4 Account Details

> Show individual account management.

1. Navigate to **Accounts**.
2. Select an account from the list.
3. The detail panel shows: account number, IBAN, currency, balance, status.
4. Available actions:
   - **Download Statement** — exports a CSV file with transactions for that account.
   - **Download History** — exports the full transaction history.
   - **Request Block** — opens a form to submit a block request with a reason.

---

### 3.5 Transfer

> Show money moving between accounts.

1. Navigate to **Payments** → **Transfer** tab.
2. Select a source account (only ACTIVE accounts are shown).
3. Fill in:
   - **Recipient name**: John Smith
   - **Recipient account number**: any valid account number (e.g. Alice's second account number from the Accounts page — this will be an internal transfer)
   - **Amount**: 50
   - **Description**: Demo transfer
4. Submit.
5. Go back to **Activity** — the new debit transaction appears immediately.
   If the recipient account belongs to another Alice account, a matching credit
   transaction is also visible.

**What to highlight:** The balance is checked before the transfer is accepted.
Try entering an amount larger than the account balance — the system returns a
`400 Insufficient balance` error.

---

### 3.6 Bill Payment

> Show an outgoing payment to an external payee.

1. Navigate to **Payments** → **Bill Payment** tab.
2. Fill in:
   - **Payee name**: Electric Company
   - **Reference**: INV-2026-05
   - **Amount**: 75
3. Submit.
4. Verify the new PAYMENT transaction in **Activity**.

---

### 3.7 Transaction History & Filters

> Show how a customer reviews their history.

1. Navigate to **Activity**.
2. Use the **account filter** to show transactions for a single account.
3. Use the **type filter** to show only TRANSFER or PAYMENT records.
4. Click **Download History** — a CSV file is saved locally.

---

### 3.8 Account Block Request

> Show the customer-initiated block workflow.

1. Navigate to **Accounts**.
2. Select Alice's first account (must be ACTIVE).
3. Click **Request Block**.
4. Enter a reason: "Suspicious activity on account."
5. Submit — the account status changes to **PENDING BLOCK**.

The block is not applied immediately. An administrator must approve it.
This is demonstrated in the admin flow below.

---

## 4. Failed Login Locking

> Show automatic account locking after 3 failed attempts.

1. Sign out.
2. Try to sign in as **locked.customer@bank.local** / **Customer123!**.
3. The system returns **HTTP 423 Locked** — the account is already locked.
4. To demonstrate the locking process live:
   - Use a fresh registered account.
   - Enter a wrong password three times in a row.
   - On the third attempt, the account is locked and a `CRITICAL` event is recorded.

An admin must unlock the account (shown in section 5.4).

---

## 5. Admin Flow

Sign out and log in as **admin@bank.local** / **Admin123!** using the same 2FA flow.

---

### 5.1 Admin Dashboard

> Show the system-wide overview.

The overview page shows:

- **Total customers** registered.
- **Total funds** — sum of all account balances.
- **Blocked users** — customers locked due to failed logins or bank blocks.
- **Pending block requests** — accounts awaiting admin decision.
- **Recent high-severity events** — latest WARNING and CRITICAL operations from the audit log.

---

### 5.2 Customer Management

> Show the admin's view of individual customers.

1. Navigate to **Customers**.
2. Use the search bar — search by name, email, or account number.
3. Click on a customer row to open the detail drawer.
4. The drawer shows:
   - Customer profile and access status.
   - All accounts with balances and current statuses.
   - Per-account actions: **Block** or **Unblock**.
5. For Alice's account that is now PENDING BLOCK, click **Block** → the account is
   immediately blocked and any pending block request is auto-approved.

---

### 5.3 Security Queue

> Show the admin handling the block request submitted by Alice.

1. Navigate to **Security**.
2. The page has three sections:
   - **Pending block requests** — Alice's request is listed here.
   - **Blocked access** — the locked customer appears here.
   - **Blocked accounts** — any manually or request-blocked accounts.
3. On Alice's pending request: click **Approve**.
   - The request status changes to APPROVED.
   - The account status changes to BLOCKED.
4. Alternatively, click **Reject** — the account reverts to ACTIVE.

---

### 5.4 Unlock a Locked User

> Show restoring access after too many failed login attempts.

1. Stay on **Security** → **Blocked access** section.
2. Find the locked customer (locked.customer@bank.local).
3. Click **Restore access**.
   - `account_status` is set back to ACTIVE.
   - `failedLoginAttempts` is reset to 0.
4. The customer can now log in again.

---

### 5.5 Audit Log

> Show full system auditability.

1. Navigate to **Operations**.
2. The table lists every recorded action in the system, newest first.
3. Use the **severity filter** to show only CRITICAL events — the account lock events appear.
4. Use the **type filter** to show only TRANSFER_CREATED events.
5. Use the **search bar** to find actions by actor email or description.

**What to highlight:** Every action — successful logins, failed attempts, transfers,
block requests, admin interventions — is recorded with a timestamp, actor, severity,
and description. This is the complete audit trail.

---

## 6. End-to-End Tests (Playwright)

The project includes an automated Playwright test suite that covers the full demo
flow above without manual interaction.

```bash
cd frontend
npm install
npx playwright test
```

The suite runs against the live stack (Docker Compose must be running) and covers:

| Test | What it does |
|---|---|
| Home page | Verifies the landing page loads |
| Registration | Creates a new customer account |
| Customer login (2FA) | Full login with OTP verification |
| Failed login locking | Three wrong passwords → account locked |
| Transfer | Submits a transfer, checks transaction history |
| Statement download | Downloads CSV, verifies file |
| Admin block approval | Admin approves a block request |
| Admin access restore | Admin unlocks a locked user |
| Admin customer drawer | Opens customer detail, checks accounts |

---

## 7. Feature Checklist

| Feature | Where to show it |
|---|---|
| User registration | Section 3.1 |
| Two-factor authentication (OTP) | Section 3.2 |
| Customer dashboard | Section 3.3 |
| Account details & downloads | Section 3.4 |
| Money transfer | Section 3.5 |
| Bill payment | Section 3.6 |
| Transaction history with filters | Section 3.7 |
| Customer-initiated account block | Section 3.8 |
| Automatic login lock (3 attempts) | Section 4 |
| Admin system overview | Section 5.1 |
| Admin customer management | Section 5.2 |
| Block request approval / rejection | Section 5.3 |
| Locked user access restoration | Section 5.4 |
| Full audit log | Section 5.5 |
| Automated E2E tests | Section 6 |
