CREATE TABLE "roles" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "account_statuses" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "bank_account_statuses" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "transaction_types" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "transaction_directions" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "transaction_statuses" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "block_request_statuses" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "operation_types" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "operation_severities" (
	"id" varchar(36) NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "currencies" (
	"id" varchar(36) NOT NULL,
	"code" varchar(10) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
CREATE TABLE "users" (
	"id" varchar(36) NOT NULL,
	"email" varchar(255) NOT NULL UNIQUE,
	"passwordHash" varchar(255) NOT NULL,
	"firstName" varchar(100) NOT NULL,
	"lastName" varchar(100) NOT NULL,
	"role_id" varchar(36) NOT NULL REFERENCES "roles"("id"),
	"accountStatus_id" varchar(36) NOT NULL REFERENCES "account_statuses"("id"),
	"failedLoginAttempts" int NOT NULL DEFAULT 0,
	"enabled" boolean NOT NULL DEFAULT true,
	"createdAt" timestamp NOT NULL,
	"lastLoginAt" timestamp,
	PRIMARY KEY ("id")
);
CREATE TABLE "bank_accounts" (
	"id" varchar(36) NOT NULL,
	"accountNumber" varchar(100) NOT NULL UNIQUE,
	"iban" varchar(100) NOT NULL UNIQUE,
	"name" varchar(200) NOT NULL,
	"type" varchar(50) NOT NULL,
	"owner" varchar(36) NOT NULL REFERENCES "users"("id"),
	"currency_id" varchar(36) NOT NULL REFERENCES "currencies"("id"),
	"balance" decimal(19,2) NOT NULL DEFAULT 0.00,
	"status_id" varchar(36) NOT NULL REFERENCES "bank_account_statuses"("id"),
	"createdAt" timestamp NOT NULL,
	PRIMARY KEY ("id")
);
CREATE TABLE "transactions" (
	"id" varchar(36) NOT NULL,
	"owner" varchar(36) NOT NULL REFERENCES "users"("id"),
	"account" varchar(36) NOT NULL REFERENCES "bank_accounts"("id"),
	"accountName" varchar(200) NOT NULL,
	"createdAt" timestamp NOT NULL,
	"type_id" varchar(36) NOT NULL REFERENCES "transaction_types"("id"),
	"title" varchar(200) NOT NULL,
	"description" varchar(1000),
	"amount" decimal(19,2) NOT NULL,
	"currency_id" varchar(36) NOT NULL REFERENCES "currencies"("id"),
	"direction_id" varchar(36) NOT NULL REFERENCES "transaction_directions"("id"),
	"status_id" varchar(36) NOT NULL REFERENCES "transaction_statuses"("id"),
	PRIMARY KEY ("id")
);
CREATE TABLE "block_requests" (
	"id" varchar(36) NOT NULL,
	"user" varchar(36) NOT NULL REFERENCES "users"("id"),
	"account" varchar(36) NOT NULL REFERENCES "bank_accounts"("id"),
	"reason" varchar(1000) NOT NULL,
	"requestedAt" timestamp NOT NULL,
	"status_id" varchar(36) NOT NULL REFERENCES "block_request_statuses"("id"),
	PRIMARY KEY ("id")
);
CREATE TABLE "otp_sessions" (
	"id" varchar(36) NOT NULL,
	"user" varchar(36) NOT NULL REFERENCES "users"("id"),
	"codeHash" varchar(255) NOT NULL,
	"createdAt" timestamp NOT NULL,
	"expiresAt" timestamp NOT NULL,
	"status" varchar(50) NOT NULL,
	"attempts" int NOT NULL DEFAULT 0,
	PRIMARY KEY ("id")
);
CREATE TABLE "operation_records" (
	"id" varchar(36) NOT NULL,
	"createdAt" timestamp NOT NULL,
	"actor" varchar(36) REFERENCES "users"("id"),
	"actorEmail" varchar(255) NOT NULL,
	"actorRole_id" varchar(36) NOT NULL REFERENCES "roles"("id"),
	"target" varchar(255) NOT NULL,
	"type_id" varchar(36) NOT NULL REFERENCES "operation_types"("id"),
	"severity_id" varchar(36) NOT NULL REFERENCES "operation_severities"("id"),
	"description" varchar(512) NOT NULL,
	PRIMARY KEY ("id")
);
