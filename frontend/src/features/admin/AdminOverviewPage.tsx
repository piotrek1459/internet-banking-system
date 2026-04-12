import { Alert, Button, Card, Col, List, Row, Space, Statistic, Table } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { formatCurrency, formatDateTime } from '../../shared/lib/format'
import { AdminDashboardResponse } from '../../types/api'

export default function AdminOverviewPage() {
  const navigate = useNavigate()
  const [dashboard, setDashboard] = useState<AdminDashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const loadDashboard = async () => {
    setLoading(true)
    try {
      const response = await api.get<AdminDashboardResponse>('/api/admin/dashboard')
      setDashboard(response)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadDashboard()
  }, [])

  if (!dashboard) {
    return (
      <Card loading={loading}>
        <div />
      </Card>
    )
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Administration overview"
        subtitle="Track customer risk, security queues, and total holdings while the backend is still mocked."
        extra={
          <Button onClick={() => void loadDashboard()} loading={loading}>
            Refresh
          </Button>
        }
      />

      <Alert
        type="warning"
        showIcon
        message="Security-first workflow"
        description="Customer access is blocked after 3 failed password attempts, and account block requests remain pending until an administrator approves them."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} xl={6}>
          <Card>
            <Statistic title="Customers" value={dashboard.totalCustomers} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card>
            <Statistic title="Total funds" value={dashboard.totalFunds} formatter={value => formatCurrency(Number(value))} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card>
            <Statistic title="Blocked users" value={dashboard.blockedUsers} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card>
            <Statistic title="Pending block requests" value={dashboard.pendingBlockRequests} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={10}>
          <Card title="Quick actions">
            <List
              dataSource={[
                { label: 'Review customer account controls', to: '/admin/customers' },
                { label: 'Work the security queue', to: '/admin/security' },
                { label: 'Audit recent events', to: '/admin/operations' },
              ]}
              renderItem={item => (
                <List.Item
                  actions={[
                    <Button key={item.to} type="link" onClick={() => navigate(item.to)}>
                      Open
                    </Button>,
                  ]}
                >
                  {item.label}
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} xl={14}>
          <Card title="Recent high-priority events">
            <Table
              rowKey="id"
              dataSource={dashboard.recentCriticalOperations}
              pagination={false}
              columns={[
                {
                  title: 'When',
                  dataIndex: 'createdAt',
                  render: value => formatDateTime(value),
                },
                {
                  title: 'Type',
                  dataIndex: 'type',
                },
                {
                  title: 'Severity',
                  dataIndex: 'severity',
                  render: value => <StatusTag status={value} />,
                },
                {
                  title: 'Description',
                  dataIndex: 'description',
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  )
}
