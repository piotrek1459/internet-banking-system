import { expect, Page, test } from '@playwright/test'

const adminUser = {
  email: 'admin@bank.local',
  password: 'Admin123!',
}

const customerUser = {
  email: 'alice.customer@bank.local',
  password: 'Customer123!',
}

type CustomerOverviewOptions = {
  firstName?: string
  accountName?: string | RegExp
}

test.describe.configure({ mode: 'serial' })

test.beforeEach(async ({ page }) => {
  await resetBrowserState(page)
})

test('Home page: opens login and register modals', async ({ page }) => {
  await test.step('Open the public landing page', async () => {
    await page.goto('/')
    await waitForHomePage(page)
  })

  await test.step('Open the login modal from the navigation', async () => {
    await page.getByRole('button', { name: 'Login' }).first().click()
    await waitForLoginModal(page)
  })

  await test.step('Switch to the register modal', async () => {
    await page.getByRole('button', { name: /Need a customer account/i }).click()
    await waitForRegisterModal(page)
  })
})

test('Register page: creates a new customer account', async ({ page }) => {
  const registeredUser = {
    firstName: 'Playwright',
    lastName: 'Tester',
    email: `playwright.customer.${Date.now()}@bank.local`,
    password: 'Customer123!',
  }

  await test.step('Register a new customer from the public modal', async () => {
    await page.goto('/')
    await waitForHomePage(page)
    await page.getByRole('button', { name: 'Register' }).first().click()
    await waitForRegisterModal(page)

    await page.getByLabel('First name').fill(registeredUser.firstName)
    await page.getByLabel('Last name').fill(registeredUser.lastName)
    await page.getByLabel('Email').fill(registeredUser.email)
    await page.getByLabel('Password').fill(registeredUser.password)
    await page.getByRole('button', { name: 'Register customer' }).click()

    await expect(page.getByText('Registration complete. You can now sign in.')).toBeVisible()
    await waitForLoginModal(page)
  })

  await test.step('Sign in with the newly registered customer', async () => {
    await page.getByLabel('Email').fill(registeredUser.email)
    await page.getByLabel('Password').fill(registeredUser.password)
    await page.getByRole('button', { name: 'Continue to OTP' }).click()
    await verifyOtp(page)
    await waitForCustomerOverviewPage(page, {
      firstName: registeredUser.firstName,
      accountName: 'Starter Current Account',
    })
  })
})

test('Login page: completes customer two factor authentication', async ({ page }) => {
  await loginAs(page, customerUser)
  await waitForCustomerOverviewPage(page)
})

test('Login page: blocks customer access after three failed attempts', async ({ page }) => {
  await test.step('Trigger three failed login attempts for the same customer', async () => {
    await page.goto('/')
    await waitForHomePage(page)
    await page.getByRole('button', { name: 'Login' }).first().click()
    await waitForLoginModal(page)
    await page.getByLabel('Email').fill(customerUser.email)

    await submitWrongPassword(page, 'Attempt 1 of 3')
    await submitWrongPassword(page, 'Attempt 2 of 3')
    await submitWrongPassword(page, 'Account blocked after 3 failed login attempts')
  })

  await test.step('Sign in as admin and confirm the operation was recorded', async () => {
    await page.getByRole('button', { name: 'Admin demo' }).click()
    await page.getByRole('button', { name: 'Continue to OTP' }).click()
    await verifyOtp(page)
    await waitForAdminOverviewPage(page)

    await page.getByRole('menuitem', { name: 'Operations' }).click()
    await waitForAdminOperationsPage(page)
    await expect(page.getByText('Access blocked after 3 failed login attempts.').first()).toBeVisible()
    await expect(page.getByText('LOGIN_FAILURE').first()).toBeVisible()
  })
})

test('Customer page: creates transfer and shows it in history', async ({ page }) => {
  await loginAs(page, customerUser)

  await test.step('Create a transfer from an active customer account', async () => {
    await page.getByRole('button', { name: 'Make payment or transfer' }).click()
    await waitForCustomerPaymentsPage(page)

    await chooseFirstSelectOption(page, /Everyday Account/)
    await page.getByLabel('Recipient name').fill('Savings Vault')
    await page.getByLabel('Recipient account number').fill('4000432143214321')
    await page.getByLabel('Amount').fill('25')
    await page.getByLabel('Description').fill('Playwright transfer check')
    await page.getByRole('button', { name: 'Submit transfer' }).click()
    await expect(page.getByText('Transfer completed successfully.').first()).toBeVisible()
  })

  await test.step('Confirm the transfer appears in transaction history', async () => {
    await page.getByRole('menuitem', { name: 'History' }).click()
    await waitForCustomerHistoryPage(page)
    await expect(page.getByText('Transfer to Savings Vault')).toBeVisible()
    await expect(page.getByText('COMPLETED').first()).toBeVisible()
  })
})

test('Customer page: downloads account statement', async ({ page }) => {
  await loginAs(page, customerUser)

  await test.step('Open account details and download a statement file', async () => {
    await page.getByRole('button', { name: 'Open accounts' }).click()
    await waitForCustomerAccountsPage(page)

    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: 'Download statement' }).click()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toContain('statement.csv')
  })
})

test('Admin page: approves pending account block request', async ({ page }) => {
  await loginAs(page, adminUser)

  await test.step('Open the security queue and approve the pending request', async () => {
    await page.getByRole('menuitem', { name: 'Security' }).click()
    await waitForAdminSecurityPage(page)
    await expect(page.getByText('Brian Walsh')).toBeVisible()
    await page.getByRole('button', { name: 'Approve' }).first().click()
    await expect(page.getByText('Block request approved.')).toBeVisible()
    await waitForAdminSecurityPage(page)
  })
})

test('Admin page: restores blocked customer access', async ({ page }) => {
  await loginAs(page, adminUser)

  await test.step('Restore a seed customer account that starts blocked', async () => {
    await page.getByRole('menuitem', { name: 'Security' }).click()
    await waitForAdminSecurityPage(page)
    await expect(page.getByText('Locked Customer')).toBeVisible()
    await page.getByRole('button', { name: 'Restore access' }).first().click()
    await expect(page.getByText('Customer access restored.')).toBeVisible()
    await waitForAdminSecurityPage(page)
  })
})

test('Admin page: renders customer account controls', async ({ page }) => {
  await loginAs(page, adminUser)

  await test.step('Open customer administration after every section finishes loading', async () => {
    await page.getByRole('menuitem', { name: 'Customers' }).click()
    await waitForAdminCustomersPage(page)
  })

  await test.step('Open a customer management drawer with account controls', async () => {
    await page.getByPlaceholder('Search by name, email, or account number').fill('Alice')
    await waitForAppToSettle(page)
    await expect(page.getByText('Alice Murphy')).toBeVisible()
    await page.getByRole('button', { name: 'Manage' }).first().click()
    await expect(page.getByRole('dialog', { name: /Alice Murphy management/ })).toBeVisible()
    await expect(page.getByText('Everyday Account')).toBeVisible()
    await expect(page.getByRole('columnheader', { name: 'Balance' })).toBeVisible()
  })
})

async function resetBrowserState(page: Page) {
  await page.goto('/')
  await waitForHomePage(page)
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  })
}

async function loginAs(page: Page, user: { email: string; password: string }) {
  await test.step(`Sign in as ${user.email}`, async () => {
    await page.goto('/')
    await waitForHomePage(page)
    await page.getByRole('button', { name: 'Login' }).first().click()
    await waitForLoginModal(page)
    await page.getByLabel('Email').fill(user.email)
    await page.getByLabel('Password').fill(user.password)
    await page.getByRole('button', { name: 'Continue to OTP' }).click()
    await verifyOtp(page)

    if (user.email === adminUser.email) {
      await waitForAdminOverviewPage(page)
      return
    }

    await waitForCustomerOverviewPage(page)
  })
}

async function verifyOtp(page: Page) {
  await waitForOtpModal(page)
  await page.getByLabel('OTP code').fill('123456')
  await page.getByRole('button', { name: 'Verify and continue' }).click()
}

async function submitWrongPassword(page: Page, expectedMessage: string) {
  await waitForLoginModal(page)
  await page.getByLabel('Password').fill('WrongPassword123!')
  await page.getByRole('button', { name: 'Continue to OTP' }).click()
  await expect(page.getByText(expectedMessage)).toBeVisible()
}

async function chooseFirstSelectOption(page: Page, optionName: RegExp) {
  const combobox = page.getByRole('combobox', { name: /Source account/ })
  await combobox.click()
  await page.locator('.ant-select-item-option', { hasText: optionName }).first().click()
  await expect(page.locator('.ant-select-selection-item').first()).toContainText(optionName)
}

async function waitForAppToSettle(page: Page) {
  await page.waitForLoadState('domcontentloaded')
  await page.waitForLoadState('networkidle', { timeout: 3_000 }).catch(() => undefined)
  await expect(page.locator(visibleLoadingSelector())).toHaveCount(0, { timeout: 10_000 })
}

function visibleLoadingSelector() {
  return [
    '.ant-card-loading:visible',
    '.ant-card-loading-content:visible',
    '.ant-skeleton:visible',
    '.ant-skeleton-active:visible',
    '.ant-spin-spinning:visible',
    '.ant-btn-loading:visible',
    '.ant-select-loading:visible',
    '[aria-busy="true"]:visible',
  ].join(', ')
}

async function waitForHomePage(page: Page) {
  await waitForAppToSettle(page)
  await expect(page.getByRole('heading', { name: /A polished banking front end/i })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Platform', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Login' }).first()).toBeEnabled()
  await expect(page.getByRole('button', { name: 'Register' }).first()).toBeEnabled()
  await expect(page.getByRole('contentinfo')).toContainText('Ready to test the banking flow?')
}

async function waitForLoginModal(page: Page) {
  await waitForAppToSettle(page)
  await expect(page.getByRole('dialog', { name: 'Login' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Secure sign in' })).toBeVisible()
  await expect(page.getByLabel('Email')).toBeVisible()
  await expect(page.getByLabel('Password')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Continue to OTP' })).toBeEnabled()
}

async function waitForRegisterModal(page: Page) {
  await waitForAppToSettle(page)
  await expect(page.getByRole('dialog', { name: 'Register' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Create customer access' })).toBeVisible()
  await expect(page.getByLabel('First name')).toBeVisible()
  await expect(page.getByLabel('Last name')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Register customer' })).toBeEnabled()
}

async function waitForOtpModal(page: Page) {
  await waitForAppToSettle(page)
  await expect(page.getByRole('dialog', { name: 'Verify OTP' })).toBeVisible()
  await expect(page.getByLabel('OTP code')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Verify and continue' })).toBeEnabled()
}

async function waitForCustomerOverviewPage(page: Page, options: CustomerOverviewOptions = {}) {
  const firstName = options.firstName ?? 'Alice'
  const accountName = options.accountName ?? 'Everyday Account'

  await expect(page).toHaveURL(/\/customer\/overview$/)
  await waitForAppToSettle(page)
  await expect(page.getByRole('heading', { name: `Welcome back, ${firstName}` })).toBeVisible()
  await expect(page.getByText(accountName).first()).toBeVisible()
  await expect(page.getByText('Recent transactions')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Make payment or transfer' })).toBeEnabled()
}

async function waitForCustomerPaymentsPage(page: Page) {
  await expect(page).toHaveURL(/\/customer\/payments$/)
  await waitForAppToSettle(page)
  await expect(page.getByRole('heading', { name: 'Payments and transfers' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: /Source account/ })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Submit transfer' })).toBeEnabled()
}

async function waitForCustomerHistoryPage(page: Page) {
  await expect(page).toHaveURL(/\/customer\/activity$/)
  await waitForAppToSettle(page)
  await expect(page.getByRole('heading', { name: 'Transaction history' })).toBeVisible()
  await expect(page.getByText('Download history')).toBeVisible()
}

async function waitForCustomerAccountsPage(page: Page) {
  await expect(page).toHaveURL(/\/customer\/accounts$/)
  await waitForAppToSettle(page)
  await expect(page.getByRole('heading', { name: 'Accounts' })).toBeVisible()
  await expect(page.getByText('Everyday Account details')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Download statement' }).first()).toBeEnabled()
}

async function waitForAdminOverviewPage(page: Page) {
  await expect(page).toHaveURL(/\/admin\/overview$/)
  await waitForAppToSettle(page)
  const main = page.getByRole('main')
  await expect(page.getByRole('heading', { name: 'Administration overview' })).toBeVisible()
  await expect(main.getByText('Security-first workflow')).toBeVisible()
  await expect(main.getByText('Customers')).toBeVisible()
  await expect(main.getByText('Total funds')).toBeVisible()
  await expect(main.getByText('Blocked users')).toBeVisible()
  await expect(main.getByText('Pending block requests')).toBeVisible()
  await expect(main.locator('.ant-statistic')).toHaveCount(4)
  await expect(main.getByText('Quick actions')).toBeVisible()
  await expect(main.getByText('Review customer account controls')).toBeVisible()
  await expect(main.getByText('Work the security queue')).toBeVisible()
  await expect(main.getByText('Audit recent events')).toBeVisible()
  await expect(main.getByText('Recent high-priority events')).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Description' })).toBeVisible()
}

async function waitForAdminSecurityPage(page: Page) {
  await expect(page).toHaveURL(/\/admin\/security$/)
  await waitForAppToSettle(page)
  const main = page.getByRole('main')
  await expect(page.getByRole('heading', { name: 'Security queue' })).toBeVisible()
  await expect(main.getByText('Pending customer block requests', { exact: true })).toBeVisible()
  await expect(main.getByText('Blocked access users', { exact: true })).toBeVisible()
  await expect(main.getByText('Blocked accounts', { exact: true })).toBeVisible()
  await expect(main.locator('.ant-table')).toHaveCount(3)
  await expect(main.getByRole('columnheader', { name: 'Requested' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Failed attempts' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Account' }).first()).toBeVisible()
  await expect(main.getByRole('button', { name: 'Refresh' })).toBeEnabled()
}

async function waitForAdminOperationsPage(page: Page) {
  await expect(page).toHaveURL(/\/admin\/operations$/)
  await waitForAppToSettle(page)
  const main = page.getByRole('main')
  await expect(page.getByRole('heading', { name: 'Operations history' })).toBeVisible()
  await expect(page.getByPlaceholder('Search actor, target, or description')).toBeVisible()
  await expect(main.getByText('All severities')).toBeVisible()
  await expect(main.getByText('All event types')).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'When' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Actor' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Type' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Severity' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Description' })).toBeVisible()
  await expect(main.locator('.ant-table-row').first()).toBeVisible()
  await expect(main.getByRole('button', { name: 'Refresh' })).toBeEnabled()
}

async function waitForAdminCustomersPage(page: Page) {
  await expect(page).toHaveURL(/\/admin\/customers$/)
  await waitForAppToSettle(page)
  const main = page.getByRole('main')
  await expect(page.getByRole('heading', { name: 'Customer administration' })).toBeVisible()
  await expect(page.getByPlaceholder('Search by name, email, or account number')).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Customer' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Access' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Failed attempts' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Accounts' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Pending requests' })).toBeVisible()
  await expect(main.getByRole('columnheader', { name: 'Actions' })).toBeVisible()
  await expect(main.locator('.ant-table-row').first()).toBeVisible()
  await expect(main.getByRole('button', { name: 'Refresh' })).toBeEnabled()
}
