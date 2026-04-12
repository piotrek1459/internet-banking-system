import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute, getHomeRoute, useAuth } from './features/auth/AuthProvider'
import AdminCustomersPage from './features/admin/AdminCustomersPage'
import AdminOperationsPage from './features/admin/AdminOperationsPage'
import AdminOverviewPage from './features/admin/AdminOverviewPage'
import AdminSecurityPage from './features/admin/AdminSecurityPage'
import HomePage from './features/auth/HomePage'
import CustomerAccountsPage from './features/customer/CustomerAccountsPage'
import CustomerActivityPage from './features/customer/CustomerActivityPage'
import CustomerOverviewPage from './features/customer/CustomerOverviewPage'
import CustomerPaymentsPage from './features/customer/CustomerPaymentsPage'
import EmployeePage from './features/employee/EmployeePage'
import DashboardLayout from './layouts/DashboardLayout'

function LandingEntry() {
  const { user } = useAuth()

  if (!user) {
    return <HomePage />
  }

  return <Navigate to={getHomeRoute(user.role)} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingEntry />} />
      <Route path="/welcome" element={<HomePage />} />
      <Route path="/login" element={<HomePage initialModal="login" />} />
      <Route path="/register" element={<HomePage initialModal="register" />} />
      <Route path="/verify-otp" element={<HomePage initialModal="otp" />} />

      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/customer/overview" element={<CustomerOverviewPage />} />
          <Route path="/customer/accounts" element={<CustomerAccountsPage />} />
          <Route path="/customer/payments" element={<CustomerPaymentsPage />} />
          <Route path="/customer/activity" element={<CustomerActivityPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/overview" element={<AdminOverviewPage />} />
          <Route path="/admin/customers" element={<AdminCustomersPage />} />
          <Route path="/admin/operations" element={<AdminOperationsPage />} />
          <Route path="/admin/security" element={<AdminSecurityPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['EMPLOYEE']} />}>
        <Route element={<DashboardLayout />}>
          <Route path="/employee" element={<EmployeePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
