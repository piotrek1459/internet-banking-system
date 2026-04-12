import {
  AccountStatus,
  AccountSummary,
  AdminCustomerSummary,
  BlockRequestDto,
  OperationRecordDto,
  Role,
  TransactionDto,
  UserDto,
} from '../types/api'

type Severity = 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS'
type OperationType =
  | 'LOGIN_FAILURE'
  | 'LOGIN_SUCCESS'
  | 'OTP_VERIFIED'
  | 'TRANSFER_CREATED'
  | 'PAYMENT_CREATED'
  | 'STATEMENT_DOWNLOADED'
  | 'HISTORY_DOWNLOADED'
  | 'ACCOUNT_BLOCK_REQUESTED'
  | 'ACCOUNT_BLOCKED'
  | 'ACCOUNT_UNBLOCKED'
  | 'ACCESS_UNBLOCKED'
  | 'CUSTOMER_REGISTERED'

export interface StoredUser {
  id: string
  email: string
  password: string
  role: Role
  firstName: string
  lastName: string
  failedLoginAttempts: number
  isAccessBlocked: boolean
  lastLoginAt: string | null
}

export interface StoredAccount {
  id: string
  userId: string
  name: string
  type: string
  accountNumber: string
  iban: string
  currency: string
  balance: number
  status: AccountStatus
}

export interface StoredTransaction extends TransactionDto {
  userId: string
}

export interface StoredOperation extends Omit<OperationRecordDto, 'actorName'> {
  actorEmail: string
}

export interface StoredBlockRequest extends Omit<BlockRequestDto, 'customerEmail' | 'customerName' | 'accountNumber'> {}

export interface OtpSession {
  id: string
  userId: string
  email: string
  expiresAt: string
}

export interface MockDatabase {
  users: StoredUser[]
  accounts: StoredAccount[]
  transactions: StoredTransaction[]
  operations: StoredOperation[]
  blockRequests: StoredBlockRequest[]
  otpSessions: OtpSession[]
}

const DATABASE_KEY = 'internet-banking.mock.db.v1'

export const OTP_CODE = '123456'

export function createId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function nowIso(offsetMinutes = 0) {
  return new Date(Date.now() + offsetMinutes * 60000).toISOString()
}

function createSeedDatabase(): MockDatabase {
  const adminId = 'user-admin'
  const aliceId = 'user-alice'
  const brianId = 'user-brian'

  const users: StoredUser[] = [
    {
      id: adminId,
      email: 'admin@bank.local',
      password: 'Admin123!',
      role: 'ADMIN',
      firstName: 'System',
      lastName: 'Administrator',
      failedLoginAttempts: 0,
      isAccessBlocked: false,
      lastLoginAt: nowIso(-60 * 20),
    },
    {
      id: aliceId,
      email: 'alice.customer@bank.local',
      password: 'Customer123!',
      role: 'CUSTOMER',
      firstName: 'Alice',
      lastName: 'Murphy',
      failedLoginAttempts: 0,
      isAccessBlocked: false,
      lastLoginAt: nowIso(-60 * 18),
    },
    {
      id: brianId,
      email: 'brian.customer@bank.local',
      password: 'Customer123!',
      role: 'CUSTOMER',
      firstName: 'Brian',
      lastName: 'Walsh',
      failedLoginAttempts: 0,
      isAccessBlocked: false,
      lastLoginAt: nowIso(-60 * 40),
    },
    {
      id: 'user-locked',
      email: 'locked.customer@bank.local',
      password: 'Customer123!',
      role: 'CUSTOMER',
      firstName: 'Locked',
      lastName: 'Customer',
      failedLoginAttempts: 3,
      isAccessBlocked: true,
      lastLoginAt: null,
    },
  ]

  const accounts: StoredAccount[] = [
    {
      id: 'acc-alice-main',
      userId: aliceId,
      name: 'Everyday Account',
      type: 'Current',
      accountNumber: '4000123412341234',
      iban: 'IE29AIBK93115212341234',
      currency: 'EUR',
      balance: 8425.18,
      status: 'ACTIVE',
    },
    {
      id: 'acc-alice-savings',
      userId: aliceId,
      name: 'Savings Vault',
      type: 'Savings',
      accountNumber: '4000432143214321',
      iban: 'IE98AIBK93115243214321',
      currency: 'EUR',
      balance: 16240.0,
      status: 'ACTIVE',
    },
    {
      id: 'acc-brian-main',
      userId: brianId,
      name: 'Family Account',
      type: 'Current',
      accountNumber: '4000567856785678',
      iban: 'IE64AIBK93115256785678',
      currency: 'EUR',
      balance: 2190.4,
      status: 'PENDING_BLOCK',
    },
  ]

  const transactions: StoredTransaction[] = [
    {
      id: 'txn-1',
      userId: aliceId,
      accountId: 'acc-alice-main',
      accountName: 'Everyday Account',
      createdAt: nowIso(-60 * 24),
      type: 'DEPOSIT',
      title: 'Salary credit',
      description: 'Monthly salary lodgement',
      amount: 3200,
      currency: 'EUR',
      direction: 'CREDIT',
      status: 'COMPLETED',
      counterparty: 'Employer Ltd',
      reference: 'SAL-APR',
    },
    {
      id: 'txn-2',
      userId: aliceId,
      accountId: 'acc-alice-main',
      accountName: 'Everyday Account',
      createdAt: nowIso(-60 * 8),
      type: 'PAYMENT',
      title: 'Electricity bill',
      description: 'Utility bill payment',
      amount: 120.45,
      currency: 'EUR',
      direction: 'DEBIT',
      status: 'COMPLETED',
      counterparty: 'City Energy',
      reference: 'UTIL-381',
    },
    {
      id: 'txn-3',
      userId: aliceId,
      accountId: 'acc-alice-main',
      accountName: 'Everyday Account',
      createdAt: nowIso(-60 * 3),
      type: 'TRANSFER',
      title: 'Transfer to savings',
      description: 'Internal transfer',
      amount: 500,
      currency: 'EUR',
      direction: 'DEBIT',
      status: 'COMPLETED',
      counterparty: 'Savings Vault',
      reference: 'INT-TRF-1',
    },
    {
      id: 'txn-4',
      userId: aliceId,
      accountId: 'acc-alice-savings',
      accountName: 'Savings Vault',
      createdAt: nowIso(-60 * 3),
      type: 'TRANSFER',
      title: 'Transfer from everyday account',
      description: 'Internal transfer',
      amount: 500,
      currency: 'EUR',
      direction: 'CREDIT',
      status: 'COMPLETED',
      counterparty: 'Everyday Account',
      reference: 'INT-TRF-1',
    },
    {
      id: 'txn-5',
      userId: brianId,
      accountId: 'acc-brian-main',
      accountName: 'Family Account',
      createdAt: nowIso(-60 * 48),
      type: 'PAYMENT',
      title: 'Insurance premium',
      description: 'Annual insurance',
      amount: 420,
      currency: 'EUR',
      direction: 'DEBIT',
      status: 'COMPLETED',
      counterparty: 'SecureCover',
      reference: 'INS-299',
    },
  ]

  const operations: StoredOperation[] = [
    {
      id: 'op-1',
      createdAt: nowIso(-60 * 50),
      actorEmail: 'system@bank.local',
      actorRole: 'ADMIN',
      target: 'brian.customer@bank.local',
      type: 'ACCOUNT_BLOCK_REQUESTED',
      severity: 'WARNING',
      description: 'Customer submitted a block request for Family Account.',
    },
    {
      id: 'op-2',
      createdAt: nowIso(-60 * 30),
      actorEmail: 'system@bank.local',
      actorRole: 'ADMIN',
      target: 'locked.customer@bank.local',
      type: 'LOGIN_FAILURE',
      severity: 'CRITICAL',
      description: 'Access blocked after 3 failed login attempts.',
    },
    {
      id: 'op-3',
      createdAt: nowIso(-60 * 12),
      actorEmail: 'alice.customer@bank.local',
      actorRole: 'CUSTOMER',
      target: 'Everyday Account',
      type: 'PAYMENT_CREATED',
      severity: 'SUCCESS',
      description: 'Electricity bill payment completed.',
    },
  ]

  const blockRequests: StoredBlockRequest[] = [
    {
      id: 'block-1',
      userId: brianId,
      accountId: 'acc-brian-main',
      reason: 'Card and online banking credentials may be compromised.',
      requestedAt: nowIso(-60 * 50),
      status: 'PENDING',
    },
  ]

  return {
    users,
    accounts,
    transactions,
    operations,
    blockRequests,
    otpSessions: [],
  }
}

export function readDb() {
  const raw = localStorage.getItem(DATABASE_KEY)

  if (!raw) {
    const seeded = createSeedDatabase()
    localStorage.setItem(DATABASE_KEY, JSON.stringify(seeded))
    return seeded
  }

  return JSON.parse(raw) as MockDatabase
}

export function writeDb(db: MockDatabase) {
  localStorage.setItem(DATABASE_KEY, JSON.stringify(db))
}

export function updateDb<T>(updater: (db: MockDatabase) => T) {
  const db = readDb()
  try {
    const result = updater(db)
    writeDb(db)
    return result
  } catch (error) {
    writeDb(db)
    throw error
  }
}

export function delay(ms = 180) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

export function getUserByToken(db: MockDatabase, authorization: string | null) {
  if (!authorization?.startsWith('Bearer mock-token:')) {
    return null
  }

  const userId = authorization.replace('Bearer mock-token:', '')
  return db.users.find(user => user.id === userId) ?? null
}

export function toUserDto(user: StoredUser): UserDto {
  return {
    id: user.id,
    email: user.email,
    role: user.role,
    firstName: user.firstName,
    lastName: user.lastName,
    failedLoginAttempts: user.failedLoginAttempts,
    isAccessBlocked: user.isAccessBlocked,
    lastLoginAt: user.lastLoginAt,
  }
}

export function toAccountSummary(account: StoredAccount): AccountSummary {
  return {
    id: account.id,
    name: account.name,
    type: account.type,
    accountNumber: account.accountNumber,
    iban: account.iban,
    currency: account.currency,
    balance: account.balance,
    status: account.status,
  }
}

export function fullName(user: StoredUser) {
  return `${user.firstName} ${user.lastName}`
}

export function pushOperation(
  db: MockDatabase,
  input: {
    actorEmail: string
    actorRole: Role
    target: string
    type: OperationType
    severity: Severity
    description: string
  },
) {
  db.operations.unshift({
    id: createId(),
    createdAt: nowIso(),
    actorEmail: input.actorEmail,
    actorRole: input.actorRole,
    target: input.target,
    type: input.type,
    severity: input.severity,
    description: input.description,
  })
}

export function toOperationDto(db: MockDatabase, operation: StoredOperation): OperationRecordDto {
  const actor = db.users.find(user => user.email === operation.actorEmail)

  return {
    id: operation.id,
    createdAt: operation.createdAt,
    actorName: actor ? fullName(actor) : operation.actorEmail,
    actorRole: operation.actorRole,
    target: operation.target,
    type: operation.type,
    severity: operation.severity,
    description: operation.description,
  }
}

export function toAdminCustomerSummary(db: MockDatabase, user: StoredUser): AdminCustomerSummary {
  const accounts = db.accounts.filter(account => account.userId === user.id)
  const pendingBlockRequests = db.blockRequests.filter(
    request => request.userId === user.id && request.status === 'PENDING',
  ).length

  return {
    userId: user.id,
    name: fullName(user),
    email: user.email,
    accessStatus: user.isAccessBlocked ? 'BLOCKED' : 'ACTIVE',
    failedLoginAttempts: user.failedLoginAttempts,
    lastLoginAt: user.lastLoginAt,
    blockedAccounts: accounts.filter(account => account.status === 'BLOCKED').length,
    pendingBlockRequests,
    accounts: accounts.map(toAccountSummary),
  }
}

export function toBlockRequestDto(db: MockDatabase, request: StoredBlockRequest): BlockRequestDto {
  const user = db.users.find(item => item.id === request.userId)
  const account = db.accounts.find(item => item.id === request.accountId)

  if (!user || !account) {
    throw new Error('Block request data is incomplete.')
  }

  return {
    id: request.id,
    userId: request.userId,
    accountId: request.accountId,
    customerName: fullName(user),
    customerEmail: user.email,
    accountNumber: account.accountNumber,
    reason: request.reason,
    requestedAt: request.requestedAt,
    status: request.status,
  }
}

export function getCustomerAccounts(db: MockDatabase, userId: string) {
  return db.accounts.filter(account => account.userId === userId)
}

export function getCustomerTransactions(db: MockDatabase, userId: string) {
  return db.transactions
    .filter(transaction => transaction.userId === userId)
    .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
}

export function createTransaction(
  db: MockDatabase,
  input: Omit<StoredTransaction, 'id' | 'createdAt'> & { createdAt?: string },
) {
  const transaction: StoredTransaction = {
    ...input,
    id: createId(),
    createdAt: input.createdAt ?? nowIso(),
  }
  db.transactions.unshift(transaction)
  return transaction
}

export function createCsv(rows: string[][]) {
  return rows
    .map(columns => columns.map(value => `"${String(value).replace(/"/g, '""')}"`).join(','))
    .join('\n')
}

export function requireAuthenticatedUser(db: MockDatabase, headers: Headers) {
  const currentUser = getUserByToken(db, headers.get('Authorization'))

  if (!currentUser) {
    throw new Error('You must sign in to continue.')
  }

  return currentUser
}

export function requireRole(user: StoredUser, roles: Role[]) {
  if (!roles.includes(user.role)) {
    throw new Error('You do not have permission to access this area.')
  }
}

export function normalizeEmail(value: string) {
  return value.trim().toLowerCase()
}
