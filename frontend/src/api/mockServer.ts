import {
  ActionResponse,
  AdminDashboardResponse,
  AdminSecurityResponse,
  AuthResponse,
  CustomerAccountsResponse,
  CustomerActivityResponse,
  CustomerOverviewResponse,
  DownloadFile,
  LoginRequest,
  LoginResponse,
  PaymentRequest,
  RegisterRequest,
  TransferRequest,
  VerifyOtpRequest,
} from '../types/api'
import {
  OTP_CODE,
  MockDatabase,
  createCsv,
  createId,
  createTransaction,
  delay,
  fullName,
  getCustomerAccounts,
  getCustomerTransactions,
  normalizeEmail,
  nowIso,
  pushOperation,
  readDb,
  requireAuthenticatedUser,
  requireRole,
  toAccountSummary,
  toAdminCustomerSummary,
  toBlockRequestDto,
  toOperationDto,
  toUserDto,
  updateDb,
} from './mockData'

interface MockRequest {
  method: string
  headers: Headers
  body?: BodyInit | null
}

function parseBody(body?: BodyInit | null) {
  if (!body) {
    return undefined
  }

  if (typeof body === 'string') {
    return JSON.parse(body)
  }

  return body
}

function getRouteParams(pathname: string, pattern: RegExp) {
  const match = pathname.match(pattern)
  return match?.slice(1) ?? null
}

function handleLogin(body: LoginRequest): LoginResponse {
  return updateDb(db => {
    const user = db.users.find(item => item.email === normalizeEmail(body.email))

    if (!user) {
      throw new Error('Invalid email or password.')
    }

    if (user.isAccessBlocked) {
      throw new Error('This access is blocked. Please contact the bank administrator.')
    }

    if (user.password !== body.password) {
      user.failedLoginAttempts += 1

      pushOperation(db, {
        actorEmail: user.email,
        actorRole: user.role,
        target: user.email,
        type: 'LOGIN_FAILURE',
        severity: user.failedLoginAttempts >= 3 ? 'CRITICAL' : 'WARNING',
        description:
          user.failedLoginAttempts >= 3
            ? 'Access blocked after 3 failed login attempts.'
            : `Failed login attempt ${user.failedLoginAttempts} of 3.`,
      })

      if (user.failedLoginAttempts >= 3) {
        user.isAccessBlocked = true
        throw new Error('Account blocked after 3 failed login attempts. Contact the administrator.')
      }

      throw new Error(`Invalid email or password. Attempt ${user.failedLoginAttempts} of 3.`)
    }

    user.failedLoginAttempts = 0
    db.otpSessions = db.otpSessions.filter(session => session.userId !== user.id)

    const otpSessionId = createId()
    db.otpSessions.push({
      id: otpSessionId,
      userId: user.id,
      email: user.email,
      expiresAt: nowIso(10),
    })

    return {
      status: 'OTP_REQUIRED',
      message: 'Password accepted. Complete the 2-factor OTP check with 123456.',
      otpSessionId,
    }
  })
}

function handleVerifyOtp(body: VerifyOtpRequest): AuthResponse {
  return updateDb(db => {
    const session = db.otpSessions.find(item => item.id === body.otpSessionId)

    if (!session) {
      throw new Error('OTP session not found. Please sign in again.')
    }

    if (new Date(session.expiresAt).getTime() < Date.now()) {
      db.otpSessions = db.otpSessions.filter(item => item.id !== body.otpSessionId)
      throw new Error('OTP session expired. Please sign in again.')
    }

    if (body.otpCode !== OTP_CODE) {
      throw new Error('The OTP code is invalid.')
    }

    const user = db.users.find(item => item.id === session.userId)

    if (!user) {
      throw new Error('User not found.')
    }

    user.lastLoginAt = nowIso()
    db.otpSessions = db.otpSessions.filter(item => item.id !== body.otpSessionId)

    pushOperation(db, {
      actorEmail: user.email,
      actorRole: user.role,
      target: user.email,
      type: 'OTP_VERIFIED',
      severity: 'SUCCESS',
      description: 'OTP verified successfully.',
    })

    pushOperation(db, {
      actorEmail: user.email,
      actorRole: user.role,
      target: user.email,
      type: 'LOGIN_SUCCESS',
      severity: 'SUCCESS',
      description: 'User signed in successfully.',
    })

    return {
      token: `mock-token:${user.id}`,
      user: toUserDto(user),
    }
  })
}

function handleRegister(body: RegisterRequest): ActionResponse {
  return updateDb(db => {
    if (db.users.some(user => user.email === normalizeEmail(body.email))) {
      throw new Error('An account with this email already exists.')
    }

    const userId = createId()
    const email = normalizeEmail(body.email)
    db.users.push({
      id: userId,
      email,
      password: body.password,
      role: 'CUSTOMER',
      firstName: body.firstName.trim(),
      lastName: body.lastName.trim(),
      failedLoginAttempts: 0,
      isAccessBlocked: false,
      lastLoginAt: null,
    })

    db.accounts.push({
      id: createId(),
      userId,
      name: 'Starter Current Account',
      type: 'Current',
      accountNumber: `4000${Math.floor(100000000000 + Math.random() * 900000000000)}`,
      iban: `IE${Math.floor(10 + Math.random() * 89)}AIBK931152${Math.floor(10000000 + Math.random() * 90000000)}`,
      currency: 'EUR',
      balance: 250,
      status: 'ACTIVE',
    })

    pushOperation(db, {
      actorEmail: email,
      actorRole: 'CUSTOMER',
      target: email,
      type: 'CUSTOMER_REGISTERED',
      severity: 'SUCCESS',
      description: 'Customer registration completed.',
    })

    return {
      message: 'Customer registered successfully. You can now sign in.',
    }
  })
}

function buildCustomerOverview(db: MockDatabase, userId: string): CustomerOverviewResponse {
  const user = db.users.find(item => item.id === userId)

  if (!user) {
    throw new Error('User not found.')
  }

  const accounts = getCustomerAccounts(db, user.id).map(toAccountSummary)
  const recentTransactions = getCustomerTransactions(db, user.id).slice(0, 6)
  const totalBalance = accounts.reduce((sum, account) => sum + account.balance, 0)
  const pendingRequests = db.blockRequests.filter(
    request => request.userId === user.id && request.status === 'PENDING',
  ).length
  const alerts = [
    ...accounts
      .filter(account => account.status === 'BLOCKED')
      .map(account => `${account.name} is blocked and cannot be used for new payments.`),
    ...accounts
      .filter(account => account.status === 'PENDING_BLOCK')
      .map(account => `${account.name} has a pending block request awaiting admin review.`),
  ]

  return {
    user: toUserDto(user),
    totalBalance,
    activeAccounts: accounts.filter(account => account.status === 'ACTIVE').length,
    pendingBlockRequests: pendingRequests,
    accounts,
    recentTransactions,
    alerts,
  }
}

function handleStatementDownload(headers: Headers, accountId: string): DownloadFile {
  return updateDb(db => {
    const user = requireAuthenticatedUser(db, headers)
    requireRole(user, ['CUSTOMER'])

    const account = db.accounts.find(item => item.id === accountId && item.userId === user.id)

    if (!account) {
      throw new Error('Account not found.')
    }

    const transactions = getCustomerTransactions(db, user.id).filter(item => item.accountId === account.id)
    const content = createCsv([
      ['Date', 'Type', 'Title', 'Counterparty', 'Amount', 'Currency', 'Status', 'Reference'],
      ...transactions.map(item => [
        item.createdAt,
        item.type,
        item.title,
        item.counterparty ?? '',
        String(item.direction === 'DEBIT' ? -item.amount : item.amount),
        item.currency,
        item.status,
        item.reference,
      ]),
    ])

    pushOperation(db, {
      actorEmail: user.email,
      actorRole: 'CUSTOMER',
      target: account.name,
      type: 'STATEMENT_DOWNLOADED',
      severity: 'INFO',
      description: `Statement downloaded for ${account.name}.`,
    })

    return {
      fileName: `${account.name.replace(/\s+/g, '-').toLowerCase()}-statement.csv`,
      mimeType: 'text/csv;charset=utf-8',
      content,
    }
  })
}

function handleHistoryDownload(headers: Headers, accountId?: string | null): DownloadFile {
  return updateDb(db => {
    const user = requireAuthenticatedUser(db, headers)
    requireRole(user, ['CUSTOMER'])

    const accounts = getCustomerAccounts(db, user.id)
    const allowedIds = new Set(accounts.map(account => account.id))
    const transactions = getCustomerTransactions(db, user.id).filter(
      item => !accountId || item.accountId === accountId,
    )

    if (accountId && !allowedIds.has(accountId)) {
      throw new Error('Account not found.')
    }

    pushOperation(db, {
      actorEmail: user.email,
      actorRole: 'CUSTOMER',
      target: accountId ?? 'All accounts',
      type: 'HISTORY_DOWNLOADED',
      severity: 'INFO',
      description: 'Transaction history downloaded.',
    })

    const content = createCsv([
      ['Date', 'Account', 'Type', 'Title', 'Amount', 'Direction', 'Status', 'Counterparty'],
      ...transactions.map(item => [
        item.createdAt,
        item.accountName,
        item.type,
        item.title,
        String(item.amount),
        item.direction,
        item.status,
        item.counterparty ?? '',
      ]),
    ])

    return {
      fileName: accountId ? 'account-history.csv' : 'full-history.csv',
      mimeType: 'text/csv;charset=utf-8',
      content,
    }
  })
}

async function dispatch<T>(url: string, request: MockRequest): Promise<T> {
  const requestUrl = new URL(url, 'http://mock.bank.local')
  const pathname = requestUrl.pathname
  const method = request.method.toUpperCase()
  const body = parseBody(request.body)

  if (method === 'POST' && pathname === '/api/auth/login') {
    return handleLogin(body as LoginRequest) as T
  }

  if (method === 'POST' && pathname === '/api/auth/verify-otp') {
    return handleVerifyOtp(body as VerifyOtpRequest) as T
  }

  if (method === 'POST' && pathname === '/api/auth/register') {
    return handleRegister(body as RegisterRequest) as T
  }

  if (method === 'GET' && pathname === '/api/customer/overview') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['CUSTOMER'])
    return buildCustomerOverview(db, user.id) as T
  }

  if (method === 'GET' && pathname === '/api/customer/accounts') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['CUSTOMER'])
    const response: CustomerAccountsResponse = {
      items: getCustomerAccounts(db, user.id).map(toAccountSummary),
    }
    return response as T
  }

  if (method === 'GET' && pathname === '/api/customer/activity') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['CUSTOMER'])
    const response: CustomerActivityResponse = {
      accounts: getCustomerAccounts(db, user.id).map(toAccountSummary),
      items: getCustomerTransactions(db, user.id),
    }
    return response as T
  }

  if (method === 'POST' && pathname === '/api/customer/transfers') {
    return updateDb(db => {
      const user = requireAuthenticatedUser(db, request.headers)
      requireRole(user, ['CUSTOMER'])
      const payload = body as TransferRequest
      const sourceAccount = db.accounts.find(account => account.id === payload.sourceAccountId && account.userId === user.id)

      if (!sourceAccount) {
        throw new Error('Source account not found.')
      }

      if (sourceAccount.status !== 'ACTIVE') {
        throw new Error('Only active accounts can create transfers.')
      }

      if (payload.amount <= 0) {
        throw new Error('Transfer amount must be greater than 0.')
      }

      if (sourceAccount.balance < payload.amount) {
        throw new Error('Insufficient balance for this transfer.')
      }

      sourceAccount.balance -= payload.amount
      createTransaction(db, {
        userId: user.id,
        accountId: sourceAccount.id,
        accountName: sourceAccount.name,
        type: 'TRANSFER',
        title: `Transfer to ${payload.recipientName}`,
        description: payload.description,
        amount: payload.amount,
        currency: sourceAccount.currency,
        direction: 'DEBIT',
        status: 'COMPLETED',
        counterparty: payload.recipientAccountNumber,
        reference: createId().slice(0, 8).toUpperCase(),
      })

      const internalTarget = db.accounts.find(
        account => account.accountNumber === payload.recipientAccountNumber && account.status === 'ACTIVE',
      )

      if (internalTarget) {
        internalTarget.balance += payload.amount
        createTransaction(db, {
          userId: internalTarget.userId,
          accountId: internalTarget.id,
          accountName: internalTarget.name,
          type: 'TRANSFER',
          title: `Transfer from ${sourceAccount.name}`,
          description: payload.description,
          amount: payload.amount,
          currency: internalTarget.currency,
          direction: 'CREDIT',
          status: 'COMPLETED',
          counterparty: user.email,
          reference: createId().slice(0, 8).toUpperCase(),
        })
      }

      pushOperation(db, {
        actorEmail: user.email,
        actorRole: 'CUSTOMER',
        target: sourceAccount.name,
        type: 'TRANSFER_CREATED',
        severity: 'SUCCESS',
        description: `Transfer of ${payload.amount.toFixed(2)} ${sourceAccount.currency} created.`,
      })

      return { message: 'Transfer completed successfully.' } as ActionResponse
    }) as T
  }

  if (method === 'POST' && pathname === '/api/customer/payments') {
    return updateDb(db => {
      const user = requireAuthenticatedUser(db, request.headers)
      requireRole(user, ['CUSTOMER'])
      const payload = body as PaymentRequest
      const sourceAccount = db.accounts.find(account => account.id === payload.sourceAccountId && account.userId === user.id)

      if (!sourceAccount) {
        throw new Error('Source account not found.')
      }

      if (sourceAccount.status !== 'ACTIVE') {
        throw new Error('Only active accounts can create payments.')
      }

      if (payload.amount <= 0) {
        throw new Error('Payment amount must be greater than 0.')
      }

      if (sourceAccount.balance < payload.amount) {
        throw new Error('Insufficient balance for this payment.')
      }

      sourceAccount.balance -= payload.amount
      createTransaction(db, {
        userId: user.id,
        accountId: sourceAccount.id,
        accountName: sourceAccount.name,
        type: 'PAYMENT',
        title: `Payment to ${payload.payeeName}`,
        description: payload.reference,
        amount: payload.amount,
        currency: sourceAccount.currency,
        direction: 'DEBIT',
        status: 'COMPLETED',
        counterparty: payload.payeeName,
        reference: payload.reference,
      })

      pushOperation(db, {
        actorEmail: user.email,
        actorRole: 'CUSTOMER',
        target: sourceAccount.name,
        type: 'PAYMENT_CREATED',
        severity: 'SUCCESS',
        description: `Payment of ${payload.amount.toFixed(2)} ${sourceAccount.currency} sent to ${payload.payeeName}.`,
      })

      return { message: 'Payment completed successfully.' } as ActionResponse
    }) as T
  }

  const customerBlockMatch = getRouteParams(pathname, /^\/api\/customer\/accounts\/([^/]+)\/request-block$/)
  if (method === 'POST' && customerBlockMatch) {
    return updateDb(db => {
      const user = requireAuthenticatedUser(db, request.headers)
      requireRole(user, ['CUSTOMER'])
      const account = db.accounts.find(item => item.id === customerBlockMatch[0] && item.userId === user.id)
      const reason = String((body as { reason: string }).reason ?? '').trim()

      if (!account) {
        throw new Error('Account not found.')
      }

      if (account.status === 'BLOCKED') {
        throw new Error('This account is already blocked.')
      }

      const pendingRequest = db.blockRequests.find(
        requestItem => requestItem.accountId === account.id && requestItem.status === 'PENDING',
      )

      if (pendingRequest) {
        throw new Error('A block request is already pending for this account.')
      }

      account.status = 'PENDING_BLOCK'
      db.blockRequests.unshift({
        id: createId(),
        userId: user.id,
        accountId: account.id,
        reason,
        requestedAt: nowIso(),
        status: 'PENDING',
      })

      pushOperation(db, {
        actorEmail: user.email,
        actorRole: 'CUSTOMER',
        target: account.name,
        type: 'ACCOUNT_BLOCK_REQUESTED',
        severity: 'WARNING',
        description: `Customer requested an account block: ${reason}`,
      })

      return { message: 'Block request submitted. An administrator must approve it.' } as ActionResponse
    }) as T
  }

  if (method === 'GET' && pathname === '/api/customer/downloads/statement') {
    return handleStatementDownload(request.headers, requestUrl.searchParams.get('accountId') ?? '') as T
  }

  if (method === 'GET' && pathname === '/api/customer/downloads/history') {
    return handleHistoryDownload(request.headers, requestUrl.searchParams.get('accountId')) as T
  }

  if (method === 'GET' && pathname === '/api/admin/dashboard') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['ADMIN'])
    const customers = db.users.filter(item => item.role === 'CUSTOMER')
    const response: AdminDashboardResponse = {
      totalCustomers: customers.length,
      totalFunds: db.accounts.reduce((sum, account) => sum + account.balance, 0),
      blockedUsers: customers.filter(item => item.isAccessBlocked).length,
      blockedAccounts: db.accounts.filter(account => account.status === 'BLOCKED').length,
      pendingBlockRequests: db.blockRequests.filter(requestItem => requestItem.status === 'PENDING').length,
      recentCriticalOperations: db.operations
        .filter(operation => operation.severity === 'CRITICAL' || operation.severity === 'WARNING')
        .slice(0, 6)
        .map(operation => toOperationDto(db, operation)),
    }
    return response as T
  }

  if (method === 'GET' && pathname === '/api/admin/customers') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['ADMIN'])
    return {
      items: db.users
        .filter(item => item.role === 'CUSTOMER')
        .map(item => toAdminCustomerSummary(db, item)),
    } as T
  }

  if (method === 'GET' && pathname === '/api/admin/operations') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['ADMIN'])
    return {
      items: db.operations.map(operation => toOperationDto(db, operation)),
    } as T
  }

  if (method === 'GET' && pathname === '/api/admin/security') {
    const db = readDb()
    const user = requireAuthenticatedUser(db, request.headers)
    requireRole(user, ['ADMIN'])
    const response: AdminSecurityResponse = {
      blockedUsers: db.users
        .filter(item => item.role === 'CUSTOMER' && item.isAccessBlocked)
        .map(item => toAdminCustomerSummary(db, item)),
      pendingRequests: db.blockRequests
        .filter(requestItem => requestItem.status === 'PENDING')
        .map(requestItem => toBlockRequestDto(db, requestItem)),
      blockedAccounts: db.accounts
        .filter(account => account.status === 'BLOCKED')
        .map(account => {
          const owner = db.users.find(userItem => userItem.id === account.userId)
          return {
            accountId: account.id,
            accountName: account.name,
            accountNumber: account.accountNumber,
            customerName: owner ? fullName(owner) : 'Unknown customer',
          }
        }),
    }
    return response as T
  }

  const unlockMatch = getRouteParams(pathname, /^\/api\/admin\/users\/([^/]+)\/unlock-access$/)
  if (method === 'POST' && unlockMatch) {
    return updateDb(db => {
      const admin = requireAuthenticatedUser(db, request.headers)
      requireRole(admin, ['ADMIN'])
      const customer = db.users.find(item => item.id === unlockMatch[0] && item.role === 'CUSTOMER')

      if (!customer) {
        throw new Error('Customer not found.')
      }

      customer.isAccessBlocked = false
      customer.failedLoginAttempts = 0
      pushOperation(db, {
        actorEmail: admin.email,
        actorRole: 'ADMIN',
        target: customer.email,
        type: 'ACCESS_UNBLOCKED',
        severity: 'SUCCESS',
        description: 'Administrator restored customer access.',
      })
      return { message: 'Customer access restored.' } as ActionResponse
    }) as T
  }

  const blockAccountMatch = getRouteParams(pathname, /^\/api\/admin\/accounts\/([^/]+)\/block$/)
  if (method === 'POST' && blockAccountMatch) {
    return updateDb(db => {
      const admin = requireAuthenticatedUser(db, request.headers)
      requireRole(admin, ['ADMIN'])
      const account = db.accounts.find(item => item.id === blockAccountMatch[0])

      if (!account) {
        throw new Error('Account not found.')
      }

      account.status = 'BLOCKED'
      db.blockRequests = db.blockRequests.map(requestItem =>
        requestItem.accountId === account.id && requestItem.status === 'PENDING'
          ? { ...requestItem, status: 'APPROVED' }
          : requestItem,
      )
      pushOperation(db, {
        actorEmail: admin.email,
        actorRole: 'ADMIN',
        target: account.accountNumber,
        type: 'ACCOUNT_BLOCKED',
        severity: 'CRITICAL',
        description: `Administrator blocked ${account.name}.`,
      })
      return { message: 'Account blocked successfully.' } as ActionResponse
    }) as T
  }

  const unblockAccountMatch = getRouteParams(pathname, /^\/api\/admin\/accounts\/([^/]+)\/unblock$/)
  if (method === 'POST' && unblockAccountMatch) {
    return updateDb(db => {
      const admin = requireAuthenticatedUser(db, request.headers)
      requireRole(admin, ['ADMIN'])
      const account = db.accounts.find(item => item.id === unblockAccountMatch[0])

      if (!account) {
        throw new Error('Account not found.')
      }

      account.status = 'ACTIVE'
      pushOperation(db, {
        actorEmail: admin.email,
        actorRole: 'ADMIN',
        target: account.accountNumber,
        type: 'ACCOUNT_UNBLOCKED',
        severity: 'SUCCESS',
        description: `Administrator unblocked ${account.name}.`,
      })
      return { message: 'Account unblocked successfully.' } as ActionResponse
    }) as T
  }

  const approveMatch = getRouteParams(pathname, /^\/api\/admin\/block-requests\/([^/]+)\/approve$/)
  if (method === 'POST' && approveMatch) {
    return updateDb(db => {
      const admin = requireAuthenticatedUser(db, request.headers)
      requireRole(admin, ['ADMIN'])
      const requestItem = db.blockRequests.find(item => item.id === approveMatch[0])

      if (!requestItem) {
        throw new Error('Block request not found.')
      }

      requestItem.status = 'APPROVED'
      const account = db.accounts.find(item => item.id === requestItem.accountId)
      if (account) {
        account.status = 'BLOCKED'
      }
      pushOperation(db, {
        actorEmail: admin.email,
        actorRole: 'ADMIN',
        target: account?.accountNumber ?? requestItem.accountId,
        type: 'ACCOUNT_BLOCKED',
        severity: 'CRITICAL',
        description: 'Administrator approved a customer block request.',
      })
      return { message: 'Block request approved.' } as ActionResponse
    }) as T
  }

  const rejectMatch = getRouteParams(pathname, /^\/api\/admin\/block-requests\/([^/]+)\/reject$/)
  if (method === 'POST' && rejectMatch) {
    return updateDb(db => {
      const admin = requireAuthenticatedUser(db, request.headers)
      requireRole(admin, ['ADMIN'])
      const requestItem = db.blockRequests.find(item => item.id === rejectMatch[0])

      if (!requestItem) {
        throw new Error('Block request not found.')
      }

      requestItem.status = 'REJECTED'
      const account = db.accounts.find(item => item.id === requestItem.accountId)
      if (account?.status === 'PENDING_BLOCK') {
        account.status = 'ACTIVE'
      }
      pushOperation(db, {
        actorEmail: admin.email,
        actorRole: 'ADMIN',
        target: account?.accountNumber ?? requestItem.accountId,
        type: 'ACCOUNT_UNBLOCKED',
        severity: 'INFO',
        description: 'Administrator rejected a customer block request.',
      })
      return { message: 'Block request rejected.' } as ActionResponse
    }) as T
  }

  throw new Error(`Mock route not found for ${method} ${pathname}`)
}

export const mockServer = {
  async request<T>(url: string, request: MockRequest): Promise<T> {
    await delay()
    return dispatch<T>(url, request)
  },
}
