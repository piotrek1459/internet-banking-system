import { FormEvent, useState } from 'react'
import { api } from '../api/client'

export default function RegisterPage() {
  const [form, setForm] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  })
  const [message, setMessage] = useState('')

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    await api.post('/api/auth/register', form)
    setMessage('Customer registered. You can now log in.')
  }

  return (
    <div className="card">
      <h2>Register customer</h2>
      <form onSubmit={onSubmit} className="column">
        <input placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
        <input type="password" placeholder="Password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} />
        <input placeholder="First name" value={form.firstName} onChange={e => setForm({ ...form, firstName: e.target.value })} />
        <input placeholder="Last name" value={form.lastName} onChange={e => setForm({ ...form, lastName: e.target.value })} />
        <button type="submit">Create customer account</button>
      </form>
      {message && <p>{message}</p>}
    </div>
  )
}
