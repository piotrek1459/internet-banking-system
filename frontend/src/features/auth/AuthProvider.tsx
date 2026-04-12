import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { api } from '../../api/client'
import { AuthResponse, LoginRequest, LoginResponse, RegisterRequest, Role, UserDto, VerifyOtpRequest } from '../../types/api'

interface AuthContextValue {
  user: UserDto | null
  token: string | null
  pendingOtpSessionId: string | null
  pendingOtpEmail: string | null
  login: (payload: LoginRequest) => Promise<LoginResponse>
  verifyOtp: (otpCode: string) => Promise<AuthResponse>
  registerCustomer: (payload: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readStoredUser() {
  const raw = localStorage.getItem('user')
  return raw ? (JSON.parse(raw) as UserDto) : null
}

function clearPendingOtp() {
  sessionStorage.removeItem('otpSessionId')
  sessionStorage.removeItem('loginEmail')
}

export function getHomeRoute(role: Role) {
  if (role === 'ADMIN') {
    return '/admin/overview'
  }

  if (role === 'EMPLOYEE') {
    return '/employee'
  }

  return '/customer/overview'
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(() => readStoredUser())
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'))
  const [pendingOtpSessionId, setPendingOtpSessionId] = useState<string | null>(
    () => sessionStorage.getItem('otpSessionId'),
  )
  const [pendingOtpEmail, setPendingOtpEmail] = useState<string | null>(() => sessionStorage.getItem('loginEmail'))

  const login = async (payload: LoginRequest) => {
    const response = await api.post<LoginResponse>('/api/auth/login', payload)
    sessionStorage.setItem('otpSessionId', response.otpSessionId)
    sessionStorage.setItem('loginEmail', payload.email)
    setPendingOtpSessionId(response.otpSessionId)
    setPendingOtpEmail(payload.email)
    return response
  }

  const verifyOtp = async (otpCode: string) => {
    const otpSessionId = pendingOtpSessionId ?? sessionStorage.getItem('otpSessionId')

    if (!otpSessionId) {
      throw new Error('OTP session not found. Please sign in again.')
    }

    const payload: VerifyOtpRequest = {
      otpSessionId,
      otpCode,
    }

    const response = await api.post<AuthResponse>('/api/auth/verify-otp', payload)
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(response.user))
    setToken(response.token)
    setUser(response.user)
    setPendingOtpEmail(null)
    setPendingOtpSessionId(null)
    clearPendingOtp()
    return response
  }

  const registerCustomer = async (payload: RegisterRequest) => {
    await api.post('/api/auth/register', payload)
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    clearPendingOtp()
    setUser(null)
    setToken(null)
    setPendingOtpEmail(null)
    setPendingOtpSessionId(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        pendingOtpSessionId,
        pendingOtpEmail,
        login,
        verifyOtp,
        registerCustomer,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}

export function PublicOnlyRoute() {
  const { user } = useAuth()

  if (user) {
    return <Navigate to={getHomeRoute(user.role)} replace />
  }

  return <Outlet />
}

export function ProtectedRoute({ allowedRoles }: { allowedRoles: Role[] }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={getHomeRoute(user.role)} replace />
  }

  return <Outlet />
}
