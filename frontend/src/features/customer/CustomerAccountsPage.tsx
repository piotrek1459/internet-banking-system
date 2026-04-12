import {
  App as AntApp,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Typography,
} from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import StatusTag from '../../shared/components/StatusTag'
import { downloadTextFile } from '../../shared/lib/download'
import { formatCurrency } from '../../shared/lib/format'
import { AccountSummary, ActionResponse, CustomerAccountsResponse, DownloadFile } from '../../types/api'

export default function CustomerAccountsPage() {
  const { message } = AntApp.useApp()
  const [blockForm] = Form.useForm()
  const [accounts, setAccounts] = useState<AccountSummary[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [blockModalOpen, setBlockModalOpen] = useState(false)

  const loadAccounts = async () => {
    setLoading(true)
    try {
      const response = await api.get<CustomerAccountsResponse>('/api/customer/accounts')
      setAccounts(response.items)
      setSelectedAccountId(current =>
        current && response.items.some(item => item.id === current) ? current : response.items[0]?.id ?? null,
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAccounts()
  }, [])

  const selectedAccount = accounts.find(account => account.id === selectedAccountId) ?? null

  const downloadStatement = async (accountId: string) => {
    const file = await api.get<DownloadFile>(`/api/customer/downloads/statement?accountId=${accountId}`)
    downloadTextFile(file)
    message.success('Statement downloaded.')
  }

  const downloadHistory = async (accountId: string) => {
    const file = await api.get<DownloadFile>(`/api/customer/downloads/history?accountId=${accountId}`)
    downloadTextFile(file)
    message.success('Account history downloaded.')
  }

  const submitBlockRequest = async (values: { reason: string }) => {
    if (!selectedAccount) {
      return
    }

    const response = await api.post<ActionResponse>(`/api/customer/accounts/${selectedAccount.id}/request-block`, values)
    message.success(response.message)
    setBlockModalOpen(false)
    blockForm.resetFields()
    await loadAccounts()
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Accounts"
        subtitle="Review account details, balances, statuses, and export account-level documents."
        extra={
          <Button onClick={() => void loadAccounts()} loading={loading}>
            Refresh
          </Button>
        }
      />

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={accounts}
          pagination={false}
          onRow={record => ({
            onClick: () => setSelectedAccountId(record.id),
          })}
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
              title: 'Type',
              dataIndex: 'type',
            },
            {
              title: 'Balance',
              render: (_, record) => formatCurrency(record.balance, record.currency),
            },
            {
              title: 'Status',
              render: (_, record) => <StatusTag status={record.status} />,
            },
          ]}
        />
      </Card>

      {selectedAccount && (
        <Card title={`${selectedAccount.name} details`}>
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Descriptions bordered column={1}>
              <Descriptions.Item label="Account number">{selectedAccount.accountNumber}</Descriptions.Item>
              <Descriptions.Item label="IBAN">{selectedAccount.iban}</Descriptions.Item>
              <Descriptions.Item label="Currency">{selectedAccount.currency}</Descriptions.Item>
              <Descriptions.Item label="Balance">
                {formatCurrency(selectedAccount.balance, selectedAccount.currency)}
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <StatusTag status={selectedAccount.status} />
              </Descriptions.Item>
            </Descriptions>

            <Space wrap>
              <Button type="primary" onClick={() => void downloadStatement(selectedAccount.id)}>
                Download statement
              </Button>
              <Button onClick={() => void downloadHistory(selectedAccount.id)}>Download history</Button>
              <Button
                danger
                disabled={selectedAccount.status !== 'ACTIVE'}
                onClick={() => setBlockModalOpen(true)}
              >
                Request account block
              </Button>
            </Space>
          </Space>
        </Card>
      )}

      <Modal
        title="Request an account block"
        open={blockModalOpen}
        onCancel={() => setBlockModalOpen(false)}
        onOk={() => blockForm.submit()}
        okText="Submit request"
      >
        <Form form={blockForm} layout="vertical" onFinish={values => void submitBlockRequest(values)}>
          <Form.Item
            label="Reason"
            name="reason"
            rules={[{ required: true, message: 'Explain why this account should be blocked.' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}
