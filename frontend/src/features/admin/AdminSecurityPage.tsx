import { App as AntApp, Button, Card, Col, Row, Space, Table } from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { formatDateTime } from '../../shared/lib/format'
import { ActionResponse, AdminSecurityResponse } from '../../types/api'

export default function AdminSecurityPage() {
  const { message } = AntApp.useApp()
  const [security, setSecurity] = useState<AdminSecurityResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const loadSecurity = async () => {
    setLoading(true)
    try {
      const response = await api.get<AdminSecurityResponse>('/api/admin/security')
      setSecurity(response)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadSecurity()
  }, [])

  const approveRequest = async (requestId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/block-requests/${requestId}/approve`, {})
    message.success(response.message)
    await loadSecurity()
  }

  const rejectRequest = async (requestId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/block-requests/${requestId}/reject`, {})
    message.success(response.message)
    await loadSecurity()
  }

  const unlockUser = async (userId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/users/${userId}/unlock-access`, {})
    message.success(response.message)
    await loadSecurity()
  }

  const unblockAccount = async (accountId: string) => {
    const response = await api.post<ActionResponse>(`/api/admin/accounts/${accountId}/unblock`, {})
    message.success(response.message)
    await loadSecurity()
  }

  if (!security) {
    return (
      <Card loading={loading}>
        <div />
      </Card>
    )
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Security queue"
        subtitle="Resolve blocked access, approve customer account blocking requests, and release blocked accounts."
        extra={
          <Button onClick={() => void loadSecurity()} loading={loading}>
            Refresh
          </Button>
        }
      />

      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card title="Pending customer block requests">
            <Table
              rowKey="id"
              loading={loading}
              dataSource={security.pendingRequests}
              pagination={false}
              columns={[
                {
                  title: 'Customer',
                  render: (_, record) => `${record.customerName} (${record.customerEmail})`,
                },
                {
                  title: 'Account',
                  dataIndex: 'accountNumber',
                },
                {
                  title: 'Requested',
                  dataIndex: 'requestedAt',
                  render: value => formatDateTime(value),
                },
                {
                  title: 'Reason',
                  dataIndex: 'reason',
                },
                {
                  title: 'Status',
                  dataIndex: 'status',
                  render: value => <StatusTag status={value} />,
                },
                {
                  title: 'Actions',
                  render: (_, record) => (
                    <Space>
                      <Button type="primary" size="small" onClick={() => void approveRequest(record.id)}>
                        Approve
                      </Button>
                      <Button size="small" onClick={() => void rejectRequest(record.id)}>
                        Reject
                      </Button>
                    </Space>
                  ),
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="Blocked access users">
            <Table
              rowKey="userId"
              loading={loading}
              dataSource={security.blockedUsers}
              pagination={false}
              columns={[
                {
                  title: 'Customer',
                  render: (_, record) => record.name,
                },
                {
                  title: 'Failed attempts',
                  dataIndex: 'failedLoginAttempts',
                },
                {
                  title: 'Access',
                  dataIndex: 'accessStatus',
                  render: value => <StatusTag status={value} />,
                },
                {
                  title: 'Action',
                  render: (_, record) => (
                    <Button size="small" onClick={() => void unlockUser(record.userId)}>
                      Restore access
                    </Button>
                  ),
                },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="Blocked accounts">
            <Table
              rowKey="accountId"
              loading={loading}
              dataSource={security.blockedAccounts}
              pagination={false}
              columns={[
                {
                  title: 'Customer',
                  dataIndex: 'customerName',
                },
                {
                  title: 'Account',
                  render: (_, record) => `${record.accountName} (${record.accountNumber})`,
                },
                {
                  title: 'Status',
                  render: () => <StatusTag status="BLOCKED" />,
                },
                {
                  title: 'Action',
                  render: (_, record) => (
                    <Button size="small" onClick={() => void unblockAccount(record.accountId)}>
                      Unblock account
                    </Button>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  )
}
