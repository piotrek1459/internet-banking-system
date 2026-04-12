import { Card } from 'antd'
import { LoginForm } from './AuthForms'

export default function LoginPage() {
  return (
    <Card className="auth-card" bordered={false}>
      <LoginForm />
    </Card>
  )
}
