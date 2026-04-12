import { Alert, Button, Card, Col, Row, Space, Statistic, Table, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { formatCurrency, formatDateTime } from '../../shared/lib/format'
import { CustomerOverviewResponse } from '../../types/api'

export default function CustomerOverviewPage() {
  const navigate = useNavigate()
  const [overview, setOverview] = useState<CustomerOverviewResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const loadOverview = async () => {
    setLoading(true)
    try {
      const response = await api.get<CustomerOverviewResponse>('/api/customer/overview')
      setOverview(response)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadOverview()
  }, [])

  if (!overview) {
    return (
      <Card loading={loading}>
        <div />
      </Card>
    )
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title={`Welcome back, ${overview.user.firstName}`}
        subtitle={`Last secure login: ${formatDateTime(overview.user.lastLoginAt)}`}
        extra={
          <Button onClick={() => void loadOverview()} loading={loading}>
            Refresh
          </Button>
        }
      />

      {overview.alerts.map(alert => (
        <Alert key={alert} type="warning" showIcon message={alert} />
      ))}

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Total balance" value={overview.totalBalance} formatter={value => formatCurrency(Number(value))} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Active accounts" value={overview.activeAccounts} />
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card>
            <Statistic title="Pending block requests" value={overview.pendingBlockRequests} />
          </Card>
        </Col>
      </Row>

      <Card
        title="Quick actions"
        extra={
          <Space wrap>
            <Button type="primary" onClick={() => navigate('/customer/payments')}>
              Make payment or transfer
            </Button>
            <Button onClick={() => navigate('/customer/accounts')}>Open accounts</Button>
            <Button onClick={() => navigate('/customer/activity')}>View full history</Button>
          </Space>
        }
      >
        <Row gutter={[16, 16]}>
          {overview.accounts.map(account => (
            <Col xs={24} md={12} key={account.id}>
              <Card size="small">
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{account.name}</Typography.Text>
                  <Typography.Text type="secondary">{account.accountNumber}</Typography.Text>
                  <Typography.Text>{formatCurrency(account.balance, account.currency)}</Typography.Text>
                  <StatusTag status={account.status} />
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      </Card>

      <Card title="Recent transactions">
        <Table
          rowKey="id"
          pagination={false}
          dataSource={overview.recentTransactions}
          columns={[
            {
              title: 'Date',
              dataIndex: 'createdAt',
              render: value => formatDateTime(value),
            },
            {
              title: 'Account',
              dataIndex: 'accountName',
            },
            {
              title: 'Type',
              dataIndex: 'type',
            },
            {
              title: 'Title',
              dataIndex: 'title',
            },
            {
              title: 'Status',
              dataIndex: 'status',
              render: value => <StatusTag status={value} />,
            },
            {
              title: 'Amount',
              render: (_, record) => {
                const signedValue = record.direction === 'DEBIT' ? -record.amount : record.amount
                return formatCurrency(signedValue, record.currency)
              },
            },
          ]}
        />
      </Card>
    </Space>
  )
}
