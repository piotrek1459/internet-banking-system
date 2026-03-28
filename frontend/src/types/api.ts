export type Role = 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER'

export interface UserDto {
  id: string
  email: string
  role: Role
  firstName: string
  lastName: string
}

export interface LoginResponse {
  status: 'OTP_REQUIRED'
  message: string
  otpSessionId: string
}

export interface AuthResponse {
  token: string
  user: UserDto
}

export interface AccountSummary {
  id: string
  accountNumber: string
  currency: string
  balance: number
  status: string
}
