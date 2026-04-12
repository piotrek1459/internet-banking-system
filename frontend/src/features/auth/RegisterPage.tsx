import { Card } from 'antd'
import { RegisterForm } from './AuthForms'

export default function RegisterPage() {
  return (
    <Card className="auth-card" bordered={false}>
      <RegisterForm />
    </Card>
  )
}
