import { App as AntApp, Alert, Button, Form, Input, Space, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getHomeRoute, useAuth } from './AuthProvider'

const demoCredentials = [
  {
    label: 'Admin demo',
    email: 'admin@bank.local',
    password: 'Admin123!',
  },
  {
    label: 'Customer demo',
    email: 'alice.customer@bank.local',
    password: 'Customer123!',
  },
]

export function LoginForm({
  onOtpRequired,
  onSwitchToRegister,
}: {
  onOtpRequired?: () => void
  onSwitchToRegister?: () => void
}) {
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const { login } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const onFinish = async (values: { email: string; password: string }) => {
    setLoading(true)
    setError('')

    try {
      const response = await login(values)
      message.success(response.message)
      if (onOtpRequired) {
        onOtpRequired()
      } else {
        navigate('/verify-otp')
      }
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Unable to sign in.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Typography.Title level={2} style={{ marginBottom: 8 }}>
          Secure sign in
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          Use email, password, and OTP to reach either the customer or bank administration portal.
        </Typography.Paragraph>
      </div>

      <Space wrap>
        {demoCredentials.map(item => (
          <Button
            key={item.label}
            onClick={() => form.setFieldsValue({ email: item.email, password: item.password })}
          >
            {item.label}
          </Button>
        ))}
      </Space>

      {error && <Alert type="error" showIcon message={error} />}

      <Form form={form} layout="vertical" onFinish={onFinish} initialValues={demoCredentials[0]}>
        <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Enter your email.' }]}>
          <Input size="large" />
        </Form.Item>
        <Form.Item
          label="Password"
          name="password"
          rules={[{ required: true, message: 'Enter your password.' }]}
        >
          <Input.Password size="large" />
        </Form.Item>
        <Button type="primary" htmlType="submit" size="large" loading={loading} block>
          Continue to OTP
        </Button>
      </Form>

      <Space direction="vertical" size="small" style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="Demo notes"
          description="OTP code is printed in backend logs. Run: docker compose logs backend | grep 'OTP for'. Customer access is blocked automatically after three failed password attempts."
        />
        {onSwitchToRegister && (
          <Button type="link" onClick={onSwitchToRegister} style={{ paddingLeft: 0 }}>
            Need a customer account? Register here
          </Button>
        )}
      </Space>
    </Space>
  )
}

export function RegisterForm({
  onRegistered,
  onSwitchToLogin,
}: {
  onRegistered?: () => void
  onSwitchToLogin?: () => void
}) {
  const [form] = Form.useForm()
  const { message } = AntApp.useApp()
  const { registerCustomer } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const onFinish = async (values: { firstName: string; lastName: string; email: string; password: string }) => {
    setLoading(true)
    setError('')

    try {
      await registerCustomer(values)
      message.success('Registration complete. You can now sign in.')
      form.resetFields()
      if (onRegistered) {
        onRegistered()
      } else if (onSwitchToLogin) {
        onSwitchToLogin()
      } else {
        navigate('/login')
      }
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'Registration failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Typography.Title level={2} style={{ marginBottom: 8 }}>
          Create customer access
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          Register a new banking customer profile before connecting the frontend to the real backend.
        </Typography.Paragraph>
      </div>

      {error && <Alert type="error" showIcon message={error} />}

      <Form form={form} layout="vertical" onFinish={onFinish}>
        <Form.Item
          label="First name"
          name="firstName"
          rules={[{ required: true, message: 'Enter the first name.' }]}
        >
          <Input size="large" />
        </Form.Item>
        <Form.Item label="Last name" name="lastName" rules={[{ required: true, message: 'Enter the last name.' }]}>
          <Input size="large" />
        </Form.Item>
        <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Enter the email.' }]}>
          <Input size="large" />
        </Form.Item>
        <Form.Item
          label="Password"
          name="password"
          rules={[{ required: true, message: 'Enter the password.' }, { min: 8, message: 'Use at least 8 characters.' }]}
        >
          <Input.Password size="large" />
        </Form.Item>
        <Button type="primary" htmlType="submit" size="large" loading={loading} block>
          Register customer
        </Button>
      </Form>

      {onSwitchToLogin && (
        <Button type="link" onClick={onSwitchToLogin} style={{ paddingLeft: 0 }}>
          Already have an account? Sign in
        </Button>
      )}
    </Space>
  )
}

export function OtpForm({ onVerified }: { onVerified?: () => void }) {
  const [form] = Form.useForm()
  const { message } = AntApp.useApp()
  const navigate = useNavigate()
  const { pendingOtpEmail, pendingOtpSessionId, user, verifyOtp } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (user) {
    return null
  }

  const onFinish = async (values: { otpCode: string }) => {
    setLoading(true)
    setError('')

    try {
      const response = await verifyOtp(values.otpCode)
      message.success('2-factor authentication completed.')
      if (onVerified) {
        onVerified()
      } else {
        navigate(getHomeRoute(response.user.role))
      }
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : 'OTP verification failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <div>
        <Typography.Title level={2} style={{ marginBottom: 8 }}>
          Verify the OTP code
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          Finish the secure login for {pendingOtpEmail ?? 'your account'} using the demo OTP code.
        </Typography.Paragraph>
      </div>

      {!pendingOtpSessionId && (
        <Alert type="warning" showIcon message="OTP session not found. Please sign in again." />
      )}
      {error && <Alert type="error" showIcon message={error} />}

      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{ otpCode: '123456' }}
        disabled={!pendingOtpSessionId}
      >
        <Form.Item
          label="OTP code"
          name="otpCode"
          rules={[{ required: true, message: 'Enter the OTP code.' }]}
        >
          <Input size="large" maxLength={6} />
        </Form.Item>
        <Button type="primary" htmlType="submit" size="large" loading={loading} block disabled={!pendingOtpSessionId}>
          Verify and continue
        </Button>
      </Form>
    </Space>
  )
}
