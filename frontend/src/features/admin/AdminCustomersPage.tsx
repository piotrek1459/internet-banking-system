import {
  App as AntApp,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Input,
  Row,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { formatCurrency, formatDateTime } from '../../shared/lib/format'
import { ActionResponse, AdminCustomerSummary } from '../../types/api'

export default function AdminCustomersPage() {
  const { message } = AntApp.useApp()
  const [customers, setCustomers] = useState<AdminCustomerSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null)

  const loadCustomers = async (preserveSelected = true) => {
    setLoading(true)
    try {
      const response = await api.get<{ items: AdminCustomerSummary[] }>('/api/admin/customers')
      setCustomers(response.items)
      if (!preserveSelected) {
        setSelectedCustomerId(null)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadCustomers(false)
  }, [])

  const selectedCustomer = customers.find(customer => customer.userId === selectedCustomerId) ?? null
  const filteredCustomers = customers.filter(customer => {
    const value = search.trim().toLowerCase()
    if (!value) {
      return true
    }

    return (
      customer.name.toLowerCase().includes(value) ||
      customer.email.toLowerCase().includes(value) ||
      customer.accounts.some(account => account.accountNumber.includes(value))
    )
  })

  const unlockUser = async (userId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/users/${userId}/unlock-access`, {})
    message.success(response.message)
    await loadCustomers()
  }

  const blockAccount = async (accountId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/accounts/${accountId}/block`, {})
    message.success(response.message)
    await loadCustomers()
  }

  const unblockAccount = async (accountId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/accounts/${accountId}/unblock`, {})
    message.success(response.message)
    await loadCustomers()
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Customer administration"
        subtitle="Search customers, inspect account status, restore blocked access, and apply manual account controls."
        extra={
          <Button onClick={() => void loadCustomers()} loading={loading}>
            Refresh
          </Button>
        }
      />

      <Card>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Input.Search
              value={search}
              onChange={event => setSearch(event.target.value)}
              placeholder="Search by name, email, or account number"
            />
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          rowKey="userId"
          loading={loading}
          dataSource={filteredCustomers}
          pagination={{ pageSize: 6 }}
          columns={[
            {
              title: 'Customer',
              dataIndex: 'name',
              render: (_, record) => (
                <Space direction="vertical" size={0}>
                  <Typography.Text strong>{record.name}</Typography.Text>
                  <Typography.Text type="secondary">{record.email}</Typography.Text>
                </Space>
              ),
            },
            {
              title: 'Access',
              dataIndex: 'accessStatus',
              render: value => <StatusTag status={value} />,
            },
            {
              title: 'Failed attempts',
              dataIndex: 'failedLoginAttempts',
            },
            {
              title: 'Accounts',
              render: (_, record) => <Tag>{record.accounts.length}</Tag>,
            },
            {
              title: 'Pending requests',
              dataIndex: 'pendingBlockRequests',
              render: value => <Tag color={value > 0 ? 'gold' : 'default'}>{value}</Tag>,
            },
            {
              title: 'Last login',
              dataIndex: 'lastLoginAt',
              render: value => formatDateTime(value),
            },
            {
              title: 'Actions',
              render: (_, record) => (
                <Button type="link" onClick={() => setSelectedCustomerId(record.userId)}>
                  Manage
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Drawer
        title={selectedCustomer ? `${selectedCustomer.name} management` : 'Customer management'}
        open={Boolean(selectedCustomer)}
        width={640}
        onClose={() => setSelectedCustomerId(null)}
      >
        {selectedCustomer && (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Email">{selectedCustomer.email}</Descriptions.Item>
              <Descriptions.Item label="Access">
                <StatusTag status={selectedCustomer.accessStatus} />
              </Descriptions.Item>
              <Descriptions.Item label="Failed attempts">
                {selectedCustomer.failedLoginAttempts}
              </Descriptions.Item>
              <Descriptions.Item label="Pending block requests">
                {selectedCustomer.pendingBlockRequests}
              </Descriptions.Item>
              <Descriptions.Item label="Last login">
                {formatDateTime(selectedCustomer.lastLoginAt)}
              </Descriptions.Item>
            </Descriptions>

            {selectedCustomer.accessStatus === 'BLOCKED' && (
              <Button type="primary" onClick={() => void unlockUser(selectedCustomer.userId)}>
                Restore access
              </Button>
            )}

            <Card title="Accounts">
              <Table
                rowKey="id"
                dataSource={selectedCustomer.accounts}
                pagination={false}
                columns={[
                  {
                    title: 'Account',
                    render: (_, record) => (
                      <Space direction="vertical" size={0}>
                        <Typography.Text strong>{record.name}</Typography.Text>
                        <Typography.Text type="secondary">{record.accountNumber}</Typography.Text>
                      </Space>
                    ),
                  },
                  {
                    title: 'Balance',
                    render: (_, record) => formatCurrency(record.balance, record.currency),
                  },
                  {
                    title: 'Status',
                    render: (_, record) => <StatusTag status={record.status} />,
                  },
                  {
                    title: 'Action',
                    render: (_, record) =>
                      record.status === 'BLOCKED' ? (
                        <Button size="small" onClick={() => void unblockAccount(record.id)}>
                          Unblock account
                        </Button>
                      ) : (
                        <Button danger size="small" onClick={() => void blockAccount(record.id)}>
                          Block account
                        </Button>
                      ),
                  },
                ]}
              />
            </Card>
          </Space>
        )}
      </Drawer>
    </Space>
  )
}
