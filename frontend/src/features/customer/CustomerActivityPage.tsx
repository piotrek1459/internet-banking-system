import { Button, Card, Col, Row, Select, Space, Table } from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { downloadTextFile } from '../../shared/lib/download'
import { formatCurrency, formatDateTime } from '../../shared/lib/format'
import { CustomerActivityResponse, DownloadFile } from '../../types/api'

export default function CustomerActivityPage() {
  const [activity, setActivity] = useState<CustomerActivityResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [selectedAccountId, setSelectedAccountId] = useState('ALL')
  const [selectedType, setSelectedType] = useState('ALL')

  const loadActivity = async () => {
    setLoading(true)
    try {
      const response = await api.get<CustomerActivityResponse>('/api/customer/activity')
      setActivity(response)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadActivity()
  }, [])

  const downloadHistory = async () => {
    const query = selectedAccountId === 'ALL' ? '' : `?accountId=${selectedAccountId}`
    const file = await api.get<DownloadFile>(`/api/customer/downloads/history${query}`)
    downloadTextFile(file)
  }

  const items =
    activity?.items.filter(item => {
      const matchesAccount = selectedAccountId === 'ALL' || item.accountId === selectedAccountId
      const matchesType = selectedType === 'ALL' || item.type === selectedType
      return matchesAccount && matchesType
    }) ?? []

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Transaction history"
        subtitle="Filter completed payments, transfers, and incoming funds, then export the current history view."
        extra={
          <Space>
            <Button onClick={() => void loadActivity()} loading={loading}>
              Refresh
            </Button>
            <Button type="primary" onClick={() => void downloadHistory()} disabled={!activity}>
              Download history
            </Button>
          </Space>
        }
      />

      <Card>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Select
              value={selectedAccountId}
              style={{ width: '100%' }}
              onChange={value => setSelectedAccountId(value)}
              options={[
                { label: 'All accounts', value: 'ALL' },
                ...(activity?.accounts.map(account => ({
                  label: `${account.name} (${account.accountNumber})`,
                  value: account.id,
                })) ?? []),
              ]}
            />
          </Col>
          <Col xs={24} md={12}>
            <Select
              value={selectedType}
              style={{ width: '100%' }}
              onChange={value => setSelectedType(value)}
              options={[
                { label: 'All transaction types', value: 'ALL' },
                ...Array.from(new Set(activity?.items.map(item => item.type) ?? [])).map(item => ({
                  label: item,
                  value: item,
                })),
              ]}
            />
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={items}
          pagination={{ pageSize: 8 }}
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
              title: 'Counterparty',
              dataIndex: 'counterparty',
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
