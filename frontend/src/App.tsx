import { Link, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import VerifyOtpPage from './pages/VerifyOtpPage'
import RegisterPage from './pages/RegisterPage'
import CustomerPage from './pages/CustomerPage'
import AdminPage from './pages/AdminPage'
import EmployeePage from './pages/EmployeePage'

function Home() {
  return <Navigate to="/login" replace />
}

export default function App() {
  return (
    <div className="container">
      <nav className="nav">
        <Link to="/login">Login</Link>
        <Link to="/register">Register</Link>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/verify-otp" element={<VerifyOtpPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/customer" element={<CustomerPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/employee" element={<EmployeePage />} />
      </Routes>
    </div>
  )
}
