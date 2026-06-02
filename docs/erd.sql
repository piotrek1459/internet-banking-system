-- JPA User entity
CREATE TABLE IF NOT EXISTS "users" (
	"id" uuid NOT NULL,
	"email" varchar(255) NOT NULL UNIQUE,
	"passwordHash" varchar(255) NOT NULL,
	"firstName" varchar(100) NOT NULL,
	"lastName" varchar(100) NOT NULL,
	"role_id" uuid NOT NULL,
	"accountStatus_id" uuid NOT NULL,
	"failedLoginAttempts" integer NOT NULL DEFAULT 0,
	"enabled" boolean NOT NULL DEFAULT true,
	"createdAt" timestamp with time zone NOT NULL,
	"lastLoginAt" timestamp with time zone,
	PRIMARY KEY ("id")
);
-- JPA BankAccount entity
CREATE TABLE IF NOT EXISTS "bank_accounts" (
	"id" uuid NOT NULL,
	"accountNumber" varchar(100) NOT NULL UNIQUE,
	"iban" varchar(100) NOT NULL UNIQUE,
	"name" varchar(200) NOT NULL,
	"type" varchar(50) NOT NULL,
	"owner" uuid NOT NULL,
	"currency" varchar(10) NOT NULL DEFAULT 'EUR',
	"balance" numeric(19,2) NOT NULL DEFAULT 0.00,
	"status_id" uuid NOT NULL,
	"createdAt" timestamp with time zone NOT NULL,
	PRIMARY KEY ("id")
);
-- JPA Transaction entity
CREATE TABLE IF NOT EXISTS "transactions" (
	"id" uuid NOT NULL,
	"owner" uuid NOT NULL,
	"account" uuid NOT NULL,
	"accountName" varchar(200) NOT NULL,
	"createdAt" timestamp with time zone NOT NULL,
	"type_id" uuid NOT NULL,
	"title" varchar(200) NOT NULL,
	"description" text,
	"amount" numeric(19,2) NOT NULL,
	"currency" varchar(10) NOT NULL,
	"direction_id" uuid NOT NULL,
	"status_id" uuid NOT NULL,
	"counterparty" varchar(200),
	"reference" varchar(200) NOT NULL,
	PRIMARY KEY ("id")
);
-- JPA BlockRequest entity
CREATE TABLE IF NOT EXISTS "block_requests" (
	"id" uuid NOT NULL,
	"user" uuid NOT NULL,
	"account" uuid NOT NULL,
	"reason" text NOT NULL,
	"requestedAt" timestamp with time zone NOT NULL,
	"status_id" uuid NOT NULL,
	PRIMARY KEY ("id")
);
-- JPA OtpSession entity
CREATE TABLE IF NOT EXISTS "otp_sessions" (
	"id" uuid NOT NULL,
	"user" uuid NOT NULL,
	"codeHash" varchar(255) NOT NULL,
	"createdAt" timestamp with time zone NOT NULL,
	"expiresAt" timestamp with time zone NOT NULL,
	"status" varchar(50) NOT NULL,
	"attempts" integer NOT NULL DEFAULT 0,
	PRIMARY KEY ("id")
);
-- JPA OperationRecord entity
CREATE TABLE IF NOT EXISTS "operation_records" (
	"id" uuid NOT NULL,
	"createdAt" timestamp with time zone NOT NULL,
	"actor" uuid,
	"actorEmail" varchar(255) NOT NULL,
	"actorRole_id" uuid NOT NULL,
	"target" varchar(255) NOT NULL,
	"type_id" uuid NOT NULL,
	"severity_id" uuid NOT NULL,
	"description" varchar(512) NOT NULL,
	PRIMARY KEY ("id")
);
-- Lookup table for User.role
CREATE TABLE IF NOT EXISTS "roles" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for User.accountStatus
CREATE TABLE IF NOT EXISTS "account_statuses" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for BankAccount.status
CREATE TABLE IF NOT EXISTS "bank_account_statuses" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for Transaction.type
CREATE TABLE IF NOT EXISTS "transaction_types" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for Transaction.direction
CREATE TABLE IF NOT EXISTS "transaction_directions" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for Transaction.status
CREATE TABLE IF NOT EXISTS "transaction_statuses" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for BlockRequest.status
CREATE TABLE IF NOT EXISTS "block_request_statuses" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for OperationRecord.type
CREATE TABLE IF NOT EXISTS "operation_types" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
-- Lookup table for OperationRecord.severity
CREATE TABLE IF NOT EXISTS "operation_severities" (
	"id" uuid NOT NULL,
	"code" varchar(50) NOT NULL UNIQUE,
	"label" varchar(100),
	PRIMARY KEY ("id")
);
ALTER TABLE "users" ADD CONSTRAINT "users_fk5" FOREIGN KEY ("role_id") REFERENCES "roles"("id");
ALTER TABLE "users" ADD CONSTRAINT "users_fk6" FOREIGN KEY ("accountStatus_id") REFERENCES "account_statuses"("id");
ALTER TABLE "bank_accounts" ADD CONSTRAINT "bank_accounts_fk5" FOREIGN KEY ("owner") REFERENCES "users"("id");
ALTER TABLE "bank_accounts" ADD CONSTRAINT "bank_accounts_fk8" FOREIGN KEY ("status_id") REFERENCES "bank_account_statuses"("id");
ALTER TABLE "transactions" ADD CONSTRAINT "transactions_fk1" FOREIGN KEY ("owner") REFERENCES "users"("id");
ALTER TABLE "transactions" ADD CONSTRAINT "transactions_fk2" FOREIGN KEY ("account") REFERENCES "bank_accounts"("id");
ALTER TABLE "transactions" ADD CONSTRAINT "transactions_fk5" FOREIGN KEY ("type_id") REFERENCES "transaction_types"("id");
ALTER TABLE "transactions" ADD CONSTRAINT "transactions_fk10" FOREIGN KEY ("direction_id") REFERENCES "transaction_directions"("id");
ALTER TABLE "transactions" ADD CONSTRAINT "transactions_fk11" FOREIGN KEY ("status_id") REFERENCES "transaction_statuses"("id");
ALTER TABLE "block_requests" ADD CONSTRAINT "block_requests_fk1" FOREIGN KEY ("user") REFERENCES "users"("id");
ALTER TABLE "block_requests" ADD CONSTRAINT "block_requests_fk2" FOREIGN KEY ("account") REFERENCES "bank_accounts"("id");
ALTER TABLE "block_requests" ADD CONSTRAINT "block_requests_fk5" FOREIGN KEY ("status_id") REFERENCES "block_request_statuses"("id");
ALTER TABLE "otp_sessions" ADD CONSTRAINT "otp_sessions_fk1" FOREIGN KEY ("user") REFERENCES "users"("id");
ALTER TABLE "operation_records" ADD CONSTRAINT "operation_records_fk2" FOREIGN KEY ("actor") REFERENCES "users"("id");
ALTER TABLE "operation_records" ADD CONSTRAINT "operation_records_fk4" FOREIGN KEY ("actorRole_id") REFERENCES "roles"("id");
ALTER TABLE "operation_records" ADD CONSTRAINT "operation_records_fk6" FOREIGN KEY ("type_id") REFERENCES "operation_types"("id");
ALTER TABLE "operation_records" ADD CONSTRAINT "operation_records_fk7" FOREIGN KEY ("severity_id") REFERENCES "operation_severities"("id");
CREATE INDEX "idx_transactions_owner" ON "transactions" ("owner");
CREATE INDEX "idx_transactions_account" ON "transactions" ("account");
CREATE INDEX "idx_transactions_created_at" ON "transactions" ("createdAt");
CREATE INDEX "idx_block_requests_user" ON "block_requests" ("user");
CREATE INDEX "idx_block_requests_account" ON "block_requests" ("account");
CREATE INDEX "idx_otp_sessions_user" ON "otp_sessions" ("user");
CREATE INDEX "idx_operation_records_actor_email" ON "operation_records" ("actorEmail");
CREATE INDEX "idx_operation_records_target" ON "operation_records" ("target");
CREATE INDEX "idx_operation_records_created_at" ON "operation_records" ("createdAt");
COMMENT ON TABLE "users" IS 'JPA User entity';
COMMENT ON TABLE "bank_accounts" IS 'JPA BankAccount entity';
COMMENT ON TABLE "transactions" IS 'JPA Transaction entity';
COMMENT ON TABLE "block_requests" IS 'JPA BlockRequest entity';
COMMENT ON TABLE "otp_sessions" IS 'JPA OtpSession entity';
COMMENT ON COLUMN "otp_sessions"."codeHash" IS 'BCrypt hash of OTP code';
COMMENT ON TABLE "operation_records" IS 'JPA OperationRecord entity';
COMMENT ON TABLE "roles" IS 'Lookup table for User.role';
COMMENT ON TABLE "account_statuses" IS 'Lookup table for User.accountStatus';
COMMENT ON TABLE "bank_account_statuses" IS 'Lookup table for BankAccount.status';
COMMENT ON TABLE "transaction_types" IS 'Lookup table for Transaction.type';
COMMENT ON TABLE "transaction_directions" IS 'Lookup table for Transaction.direction';
COMMENT ON TABLE "transaction_statuses" IS 'Lookup table for Transaction.status';
COMMENT ON TABLE "block_request_statuses" IS 'Lookup table for BlockRequest.status';
COMMENT ON TABLE "operation_types" IS 'Lookup table for OperationRecord.type';
COMMENT ON TABLE "operation_severities" IS 'Lookup table for OperationRecord.severity';
