import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AuthResponse } from '../types/api'

export default function VerifyOtpPage() {
  const navigate = useNavigate()
  const [otpCode, setOtpCode] = useState('123456')
  const [error, setError] = useState('')

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const response = await api.post<AuthResponse>('/api/auth/verify-otp', {
        otpSessionId: sessionStorage.getItem('otpSessionId'),
        otpCode,
      })
      localStorage.setItem('token', response.token)
      localStorage.setItem('user', JSON.stringify(response.user))

      if (response.user.role === 'ADMIN') navigate('/admin')
      else if (response.user.role === 'EMPLOYEE') navigate('/employee')
      else navigate('/customer')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'OTP verification failed')
    }
  }

  return (
    <div className="card">
      <h2>Verify OTP</h2>
      <form onSubmit={onSubmit} className="column">
        <input value={otpCode} onChange={e => setOtpCode(e.target.value)} placeholder="OTP code" />
        <button type="submit">Verify</button>
      </form>
      {error && <p>{error}</p>}
    </div>
  )
}
