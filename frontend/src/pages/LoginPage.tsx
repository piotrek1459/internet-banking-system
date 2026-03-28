import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { LoginResponse } from '../types/api'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('admin@bank.local')
  const [password, setPassword] = useState('Admin123!')
  const [error, setError] = useState('')

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const response = await api.post<LoginResponse>('/api/auth/login', { email, password })
      sessionStorage.setItem('otpSessionId', response.otpSessionId)
      sessionStorage.setItem('loginEmail', email)
      navigate('/verify-otp')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    }
  }

  return (
    <div className="card">
      <h2>Login</h2>
      <form onSubmit={onSubmit} className="column">
        <input value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" />
        <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" />
        <button type="submit">Login</button>
      </form>
      <p>Starter OTP code is <strong>123456</strong>.</p>
      {error && <p>{error}</p>}
    </div>
  )
}
