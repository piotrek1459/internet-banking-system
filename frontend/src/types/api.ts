export type Role = 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER'
export type AccountStatus = 'ACTIVE' | 'PENDING_BLOCK' | 'BLOCKED'
export type AccessStatus = 'ACTIVE' | 'BLOCKED'
export type TransactionDirection = 'DEBIT' | 'CREDIT'
export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'FAILED'
export type TransactionType = 'TRANSFER' | 'PAYMENT' | 'DEPOSIT'
export type OperationSeverity = 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS'
export type BlockRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface UserDto {
  id: string
  email: string
  role: Role
  firstName: string
  lastName: string
  failedLoginAttempts: number
  isAccessBlocked: boolean
  lastLoginAt: string | null
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  status: 'OTP_REQUIRED'
  message: string
  otpSessionId: string
}

export interface VerifyOtpRequest {
  otpSessionId: string
  otpCode: string
}

export interface AuthResponse {
  token: string
  user: UserDto
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
}

export interface AccountSummary {
  id: string
  name: string
  type: string
  accountNumber: string
  iban: string
  currency: string
  balance: number
  status: AccountStatus
}

export interface TransactionDto {
  id: string
  accountId: string
  accountName: string
  createdAt: string
  type: TransactionType
  title: string
  description: string
  amount: number
  currency: string
  direction: TransactionDirection
  status: TransactionStatus
  counterparty?: string
  reference: string
}

export interface CustomerOverviewResponse {
  user: UserDto
  totalBalance: number
  activeAccounts: number
  pendingBlockRequests: number
  accounts: AccountSummary[]
  recentTransactions: TransactionDto[]
  alerts: string[]
}

export interface CustomerAccountsResponse {
  items: AccountSummary[]
}

export interface CustomerActivityResponse {
  accounts: AccountSummary[]
  items: TransactionDto[]
}

export interface TransferRequest {
  sourceAccountId: string
  recipientName: string
  recipientAccountNumber: string
  amount: number
  description: string
}

export interface PaymentRequest {
  sourceAccountId: string
  payeeName: string
  reference: string
  amount: number
}

export interface ActionResponse {
  message: string
}

export interface DownloadFile {
  fileName: string
  mimeType: string
  content: string
}

export interface OperationRecordDto {
  id: string
  createdAt: string
  actorName: string
  actorRole: Role
  target: string
  type: string
  severity: OperationSeverity
  description: string
}

export interface AdminDashboardResponse {
  totalCustomers: number
  totalFunds: number
  blockedUsers: number
  blockedAccounts: number
  pendingBlockRequests: number
  recentCriticalOperations: OperationRecordDto[]
}

export interface AdminCustomerSummary {
  userId: string
  name: string
  email: string
  accessStatus: AccessStatus
  failedLoginAttempts: number
  blockedAccounts: number
  pendingBlockRequests: number
  lastLoginAt: string | null
  accounts: AccountSummary[]
}

export interface BlockRequestDto {
  id: string
  userId: string
  accountId: string
  customerName: string
  customerEmail: string
  accountNumber: string
  reason: string
  requestedAt: string
  status: BlockRequestStatus
}

export interface AdminSecurityResponse {
  blockedUsers: AdminCustomerSummary[]
  pendingRequests: BlockRequestDto[]
  blockedAccounts: {
    accountId: string
    accountName: string
    accountNumber: string
    customerName: string
  }[]
}
