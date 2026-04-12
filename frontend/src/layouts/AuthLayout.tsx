import { BankOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { Card, Col, Layout, Row, Space, Typography } from 'antd'
import { Outlet } from 'react-router-dom'

const featureItems = [
  {
    icon: <SafetyCertificateOutlined />,
    title: '2-factor authentication',
    description: 'Customers complete password login and OTP verification before they can reach banking features.',
  },
  {
    icon: <LockOutlined />,
    title: 'Account protection',
    description: 'Three failed password attempts automatically block access until an admin restores it.',
  },
  {
    icon: <BankOutlined />,
    title: 'Admin operations',
    description: 'Admins can review customer history, approve block requests, and manage account status.',
  },
]

export default function AuthLayout() {
  return (
    <Layout className="auth-layout">
      <Layout.Content className="auth-layout-content">
        <Row gutter={[24, 24]} align="middle">
          <Col xs={24} lg={11}>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              <div>
                <Typography.Title style={{ marginBottom: 12 }}>Internet Banking System</Typography.Title>
                <Typography.Paragraph type="secondary" style={{ fontSize: 16, marginBottom: 0 }}>
                  A frontend-first banking workspace for secure login, account visibility, payments, history downloads,
                  and administrator-led security actions.
                </Typography.Paragraph>
              </div>
              {featureItems.map(item => (
                <Card key={item.title}>
                  <Space align="start">
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      {item.icon}
                    </Typography.Title>
                    <div>
                      <Typography.Title level={5} style={{ marginBottom: 4 }}>
                        {item.title}
                      </Typography.Title>
                      <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                        {item.description}
                      </Typography.Paragraph>
                    </div>
                  </Space>
                </Card>
              ))}
            </Space>
          </Col>
          <Col xs={24} lg={13}>
            <Outlet />
          </Col>
        </Row>
      </Layout.Content>
    </Layout>
  )
}
