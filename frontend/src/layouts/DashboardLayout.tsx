import {
  AuditOutlined,
  BankOutlined,
  DashboardOutlined,
  HistoryOutlined,
  LogoutOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  TeamOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { Avatar, Button, Layout, Menu, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { formatDateTime } from '../shared/lib/format'

export default function DashboardLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout, user } = useAuth()
  const [collapsed, setCollapsed] = useState(false)

  if (!user) {
    return null
  }

  const menuItems: MenuProps['items'] =
    user.role === 'ADMIN'
      ? [
          { key: '/admin/overview', icon: <DashboardOutlined />, label: 'Overview' },
          { key: '/admin/customers', icon: <TeamOutlined />, label: 'Customers' },
          { key: '/admin/operations', icon: <AuditOutlined />, label: 'Operations' },
          { key: '/admin/security', icon: <SafetyCertificateOutlined />, label: 'Security' },
        ]
      : user.role === 'EMPLOYEE'
        ? [{ key: '/employee', icon: <TeamOutlined />, label: 'Employee' }]
        : [
            { key: '/customer/overview', icon: <DashboardOutlined />, label: 'Overview' },
            { key: '/customer/accounts', icon: <WalletOutlined />, label: 'Accounts' },
            { key: '/customer/payments', icon: <SendOutlined />, label: 'Payments' },
            { key: '/customer/activity', icon: <HistoryOutlined />, label: 'History' },
          ]

  const selectedKey = (menuItems ?? [])
    .map(item => String(item?.key))
    .find(item => location.pathname === item || location.pathname.startsWith(`${item}/`))

  return (
    <Layout className="dashboard-layout">
      <Layout.Sider
        theme="light"
        width={250}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
      >
        <div className="sidebar-brand">
          <Space>
            <BankOutlined style={{ fontSize: 20 }} />
            {!collapsed && <Typography.Text strong>Internet Banking</Typography.Text>}
          </Space>
        </div>
        <Menu
          mode="inline"
          selectedKeys={selectedKey ? [selectedKey] : []}
          items={menuItems}
          onClick={({ key }) => navigate(String(key))}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="dashboard-header">
          <Space size="middle">
            <Avatar>{user.firstName[0]}</Avatar>
            <div>
              <Typography.Text strong>{`${user.firstName} ${user.lastName}`}</Typography.Text>
              <div>
                <Tag color={user.role === 'ADMIN' ? 'geekblue' : user.role === 'EMPLOYEE' ? 'purple' : 'green'}>
                  {user.role}
                </Tag>
                <Typography.Text type="secondary">Last login: {formatDateTime(user.lastLoginAt)}</Typography.Text>
              </div>
            </div>
          </Space>
          <Button
            icon={<LogoutOutlined />}
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            Sign out
          </Button>
        </Layout.Header>
        <Layout.Content className="dashboard-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
