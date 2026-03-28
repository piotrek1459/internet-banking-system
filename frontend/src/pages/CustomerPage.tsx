import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { AccountSummary } from '../types/api'

export default function CustomerPage() {
  const [accounts, setAccounts] = useState<AccountSummary[]>([])

  useEffect(() => {
    const email = JSON.parse(localStorage.getItem('user') || '{}').email
    fetch('/api/customer/accounts', { headers: { 'X-Demo-User': email } })
      .then(r => r.json())
      .then(setAccounts)
  }, [])

  return (
    <div className="card">
      <h2>Customer dashboard</h2>
      {accounts.map(account => (
        <div key={account.id} className="card">
          <div>{account.accountNumber}</div>
          <div>{account.balance} {account.currency}</div>
          <div>{account.status}</div>
        </div>
      ))}
    </div>
  )
}
