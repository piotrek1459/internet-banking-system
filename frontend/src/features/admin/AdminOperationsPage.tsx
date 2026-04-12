import { Button, Card, Col, Input, Row, Select, Space, Table } from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { formatDateTime } from '../../shared/lib/format'
import { OperationRecordDto } from '../../types/api'

export default function AdminOperationsPage() {
  const [items, setItems] = useState<OperationRecordDto[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [severity, setSeverity] = useState('ALL')
  const [type, setType] = useState('ALL')

  const loadOperations = async () => {
    setLoading(true)
    try {
      const response = await api.get<{ items: OperationRecordDto[] }>('/api/admin/operations')
      setItems(response.items)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadOperations()
  }, [])

  const filteredItems = items.filter(item => {
    const matchesSeverity = severity === 'ALL' || item.severity === severity
    const matchesType = type === 'ALL' || item.type === type
    const query = search.trim().toLowerCase()
    const matchesSearch =
      !query ||
      item.actorName.toLowerCase().includes(query) ||
      item.target.toLowerCase().includes(query) ||
      item.description.toLowerCase().includes(query)

    return matchesSeverity && matchesType && matchesSearch
  })

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Operations history"
        subtitle="Monitor login activity, payment actions, downloads, and security operations across the banking system."
        extra={
          <Button onClick={() => void loadOperations()} loading={loading}>
            Refresh
          </Button>
        }
      />

      <Card>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={10}>
            <Input.Search
              placeholder="Search actor, target, or description"
              value={search}
              onChange={event => setSearch(event.target.value)}
            />
          </Col>
          <Col xs={24} md={7}>
            <Select
              value={severity}
              style={{ width: '100%' }}
              onChange={value => setSeverity(value)}
              options={[
                { label: 'All severities', value: 'ALL' },
                { label: 'Success', value: 'SUCCESS' },
                { label: 'Info', value: 'INFO' },
                { label: 'Warning', value: 'WARNING' },
                { label: 'Critical', value: 'CRITICAL' },
              ]}
            />
          </Col>
          <Col xs={24} md={7}>
            <Select
              value={type}
              style={{ width: '100%' }}
              onChange={value => setType(value)}
              options={[
                { label: 'All event types', value: 'ALL' },
                ...Array.from(new Set(items.map(item => item.type))).map(item => ({
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
          dataSource={filteredItems}
          pagination={{ pageSize: 8 }}
          columns={[
            {
              title: 'When',
              dataIndex: 'createdAt',
              render: value => formatDateTime(value),
            },
            {
              title: 'Actor',
              render: (_, record) => `${record.actorName} (${record.actorRole})`,
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
              title: 'Target',
              dataIndex: 'target',
            },
            {
              title: 'Description',
              dataIndex: 'description',
            },
          ]}
        />
      </Card>
    </Space>
  )
}
