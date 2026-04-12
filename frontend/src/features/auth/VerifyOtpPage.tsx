import { Card } from 'antd'
import { OtpForm } from './AuthForms'

export default function VerifyOtpPage() {
  return (
    <Card className="auth-card" bordered={false}>
      <OtpForm />
    </Card>
  )
}
