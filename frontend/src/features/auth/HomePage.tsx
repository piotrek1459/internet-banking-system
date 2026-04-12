import {
  ArrowRightOutlined,
  AuditOutlined,
  BankOutlined,
  DownloadOutlined,
  LockOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  TeamOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { Button, Card, Col, Layout, Modal, Row, Space, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getHomeRoute, useAuth } from './AuthProvider'
import { LoginForm, OtpForm, RegisterForm } from './AuthForms'
import HeroIllustration from './HeroIllustration'

type AuthModalView = 'login' | 'register' | 'otp'

const navItems = [
  { label: 'Platform', href: '#platform' },
  { label: 'Security', href: '#security' },
  { label: 'Roles', href: '#roles' },
]

const capabilityCards = [
  {
    icon: <SafetyCertificateOutlined />,
    title: 'Security-first access',
    description: 'Customers sign in with password plus OTP before they reach any account or payment workflow.',
  },
  {
    icon: <WalletOutlined />,
    title: 'Account visibility',
    description: 'View balances, statuses, and account documents while keeping blocked and pending states visible.',
  },
  {
    icon: <SendOutlined />,
    title: 'Payments and transfers',
    description: 'Move funds, pay billers, and track every outgoing flow directly from the customer workspace.',
  },
  {
    icon: <AuditOutlined />,
    title: 'Admin operations history',
    description: 'Review security events, payment activity, and system actions from the bank administration portal.',
  },
]

const roleCards = [
  {
    icon: <BankOutlined />,
    title: 'Customer',
    description: 'Login with 2FA, review accounts, transfer money, pay bills, and export statements or history.',
  },
  {
    icon: <TeamOutlined />,
    title: 'Administrator',
    description: 'Inspect customer operation history, restore locked access, and approve or reject account block requests.',
  },
]

const footerColumns = [
  {
    title: 'Platform',
    items: ['2-factor authentication', 'Account overview', 'Transfers and payments', 'Statement downloads'],
  },
  {
    title: 'Security',
    items: ['3-attempt access blocking', 'Pending block queue', 'Admin approval flow', 'Operations history'],
  },
  {
    title: 'Demo',
    items: ['Admin: admin@bank.local', 'Customer: alice.customer@bank.local', 'OTP code: 123456', 'Local mock API'],
  },
]

export default function HomePage({ initialModal }: { initialModal?: AuthModalView }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, pendingOtpSessionId } = useAuth()
  const [activeModal, setActiveModal] = useState<AuthModalView | null>(
    pendingOtpSessionId ? 'otp' : initialModal ?? null,
  )

  useEffect(() => {
    if (user) {
      navigate(getHomeRoute(user.role), { replace: true })
    }
  }, [navigate, user])

  useEffect(() => {
    if (pendingOtpSessionId) {
      setActiveModal('otp')
      return
    }

    if (initialModal === 'otp') {
      setActiveModal('login')
      return
    }

    setActiveModal(initialModal ?? null)
  }, [initialModal, pendingOtpSessionId])

  const closeModal = () => {
    setActiveModal(null)
    if (location.pathname !== '/') {
      navigate('/', { replace: true })
    }
  }

  const modalTitle =
    activeModal === 'register' ? 'Register' : activeModal === 'otp' ? 'Verify OTP' : 'Login'

  return (
    <Layout className="auth-layout">
      <Layout.Header className="landing-header">
        <div className="landing-header-inner">
          <Space size="middle">
            <div className="landing-brand-mark">
              <BankOutlined />
            </div>
            <div>
              <Typography.Text strong className="landing-brand-text">
                Internet Banking System
              </Typography.Text>
            </div>
          </Space>

          <Space size="large" className="landing-nav-links">
            {navItems.map(item => (
              <Typography.Link key={item.href} href={item.href} className="landing-nav-link">
                {item.label}
              </Typography.Link>
            ))}
          </Space>

          <Space>
            <Button onClick={() => setActiveModal('login')}>Login</Button>
            <Button type="primary" onClick={() => setActiveModal('register')}>
              Register
            </Button>
          </Space>
        </div>
      </Layout.Header>

      <Layout.Content className="landing-content">
        <section className="landing-hero">
          <Row gutter={[32, 32]} align="middle">
            <Col xs={24} lg={11}>
              <Space direction="vertical" size="large" style={{ width: '100%' }}>
                <Tag className="landing-pill" bordered={false}>
                  Frontend prototype for secure digital banking
                </Tag>

                <div>
                  <Typography.Title className="landing-hero-title">
                    A polished banking front end for customers, security teams, and admin operations.
                  </Typography.Title>
                  <Typography.Paragraph className="landing-hero-copy">
                    Start from a strong public homepage, bring users into modal-based login and registration, then route
                    them into role-specific banking flows backed by a local mock service.
                  </Typography.Paragraph>
                </div>

                <Space wrap size="middle">
                  <Button type="primary" size="large" onClick={() => setActiveModal('login')}>
                    Login
                  </Button>
                  <Button size="large" onClick={() => setActiveModal('register')}>
                    Register
                  </Button>
                  <Button type="link" size="large" href="#platform" icon={<ArrowRightOutlined />}>
                    Explore the platform
                  </Button>
                </Space>

                <Row gutter={[16, 16]}>
                  <Col xs={24} sm={8}>
                    <Card className="landing-stat-card" bordered={false}>
                      <Typography.Text className="landing-stat-label">Login flow</Typography.Text>
                      <Typography.Title level={3} className="landing-stat-value">
                        2-step
                      </Typography.Title>
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card className="landing-stat-card" bordered={false}>
                      <Typography.Text className="landing-stat-label">Roles</Typography.Text>
                      <Typography.Title level={3} className="landing-stat-value">
                        2 active
                      </Typography.Title>
                    </Card>
                  </Col>
                  <Col xs={24} sm={8}>
                    <Card className="landing-stat-card" bordered={false}>
                      <Typography.Text className="landing-stat-label">Security rule</Typography.Text>
                      <Typography.Title level={3} className="landing-stat-value">
                        3 strikes
                      </Typography.Title>
                    </Card>
                  </Col>
                </Row>
              </Space>
            </Col>

            <Col xs={24} lg={13}>
              <div className="landing-visual-shell">
                <HeroIllustration />
              </div>
            </Col>
          </Row>
        </section>

        <section id="platform" className="landing-section">
          <div className="landing-section-heading">
            <Typography.Text className="landing-section-kicker">Platform</Typography.Text>
            <Typography.Title level={2}>Built around the exact flows you need to demo first</Typography.Title>
            <Typography.Paragraph>
              The public homepage now sets the tone, while the product itself stays focused on banking essentials:
              secure entry, customer money movement, downloadable history, and administrator control points.
            </Typography.Paragraph>
          </div>

          <Row gutter={[20, 20]}>
            {capabilityCards.map(item => (
              <Col xs={24} md={12} key={item.title}>
                <Card className="landing-feature-card" bordered={false}>
                  <div className="landing-feature-icon">{item.icon}</div>
                  <Typography.Title level={4}>{item.title}</Typography.Title>
                  <Typography.Paragraph>{item.description}</Typography.Paragraph>
                </Card>
              </Col>
            ))}
          </Row>
        </section>

        <section id="security" className="landing-section">
          <Card className="landing-highlight-card" bordered={false}>
            <Row gutter={[24, 24]} align="middle">
              <Col xs={24} lg={10}>
                <Typography.Text className="landing-section-kicker">Security</Typography.Text>
                <Typography.Title level={2}>Access protection stays visible across the whole product</Typography.Title>
                <Typography.Paragraph style={{ marginBottom: 0 }}>
                  Failed password attempts escalate toward automatic access blocking, every event is captured in the admin
                  operations history, and customers can submit urgent block requests for compromised accounts.
                </Typography.Paragraph>
              </Col>
              <Col xs={24} lg={14}>
                <div className="landing-checklist">
                  <div className="landing-checklist-item">
                    <LockOutlined />
                    <span>3 failed logins trigger account access blocking</span>
                  </div>
                  <div className="landing-checklist-item">
                    <AuditOutlined />
                    <span>Admin history captures security and transaction events</span>
                  </div>
                  <div className="landing-checklist-item">
                    <DownloadOutlined />
                    <span>Statement and history export flows are available from the customer area</span>
                  </div>
                </div>
              </Col>
            </Row>
          </Card>
        </section>

        <section id="roles" className="landing-section">
          <div className="landing-section-heading">
            <Typography.Text className="landing-section-kicker">Roles</Typography.Text>
            <Typography.Title level={2}>Two focused workspaces, one consistent product story</Typography.Title>
          </div>

          <Row gutter={[20, 20]}>
            {roleCards.map(item => (
              <Col xs={24} md={12} key={item.title}>
                <Card className="landing-role-card" bordered={false}>
                  <div className="landing-feature-icon">{item.icon}</div>
                  <Typography.Title level={4}>{item.title}</Typography.Title>
                  <Typography.Paragraph>{item.description}</Typography.Paragraph>
                </Card>
              </Col>
            ))}
          </Row>
        </section>
      </Layout.Content>

      <footer className="landing-footer">
        <div className="landing-footer-inner">
          <div className="landing-footer-top">
            <div>
              <Typography.Title level={3} style={{ color: '#f8fafc', marginBottom: 8 }}>
                Ready to test the banking flow?
              </Typography.Title>
              <Typography.Paragraph style={{ color: 'rgba(248, 250, 252, 0.78)', marginBottom: 0 }}>
                Use the demo accounts, open the login modal, and walk straight into customer or administrator scenarios.
              </Typography.Paragraph>
            </div>
            <Space wrap>
              <Button type="primary" size="large" onClick={() => setActiveModal('login')}>
                Open login
              </Button>
              <Button size="large" onClick={() => setActiveModal('register')}>
                Open register
              </Button>
            </Space>
          </div>

          <Row gutter={[24, 24]}>
            {footerColumns.map(column => (
              <Col xs={24} md={8} key={column.title}>
                <Typography.Title level={5} style={{ color: '#f8fafc' }}>
                  {column.title}
                </Typography.Title>
                <Space direction="vertical" size="small">
                  {column.items.map(item => (
                    <Typography.Text key={item} style={{ color: 'rgba(248, 250, 252, 0.78)' }}>
                      {item}
                    </Typography.Text>
                  ))}
                </Space>
              </Col>
            ))}
          </Row>

          <div className="landing-footer-bottom">
            <Typography.Text style={{ color: 'rgba(248, 250, 252, 0.56)' }}>
              Internet Banking System frontend prototype
            </Typography.Text>
            <Typography.Text style={{ color: 'rgba(248, 250, 252, 0.56)' }}>
              React, TypeScript, Vite, Ant Design, local mock service
            </Typography.Text>
          </div>
        </div>
      </footer>

      <Modal open={Boolean(activeModal)} onCancel={closeModal} footer={null} destroyOnClose title={modalTitle}>
        {activeModal === 'login' && (
          <LoginForm onOtpRequired={() => setActiveModal('otp')} onSwitchToRegister={() => setActiveModal('register')} />
        )}
        {activeModal === 'register' && (
          <RegisterForm onRegistered={() => setActiveModal('login')} onSwitchToLogin={() => setActiveModal('login')} />
        )}
        {activeModal === 'otp' && <OtpForm onVerified={closeModal} />}
      </Modal>
    </Layout>
  )
}
