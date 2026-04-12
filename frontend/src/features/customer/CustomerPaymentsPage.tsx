import { App as AntApp, Alert, Button, Card, Form, Input, InputNumber, Select, Space, Tabs, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PageHeader from '../../shared/components/PageHeader'
import { AccountSummary, ActionResponse, CustomerAccountsResponse } from '../../types/api'

export default function CustomerPaymentsPage() {
  const { message } = AntApp.useApp()
  const [transferForm] = Form.useForm()
  const [paymentForm] = Form.useForm()
  const [accounts, setAccounts] = useState<AccountSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [resultMessage, setResultMessage] = useState('')

  const loadAccounts = async () => {
    setLoading(true)
    try {
      const response = await api.get<CustomerAccountsResponse>('/api/customer/accounts')
      setAccounts(response.items)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAccounts()
  }, [])

  const activeAccounts = accounts.filter(account => account.status === 'ACTIVE')

  const handleTransfer = async (values: {
    sourceAccountId: string
    recipientName: string
    recipientAccountNumber: string
    amount: number
    description: string
  }) => {
    setSubmitting(true)
    try {
      const response = await api.post<ActionResponse>('/api/customer/transfers', values)
      setResultMessage(response.message)
      message.success(response.message)
      transferForm.resetFields()
      await loadAccounts()
    } finally {
      setSubmitting(false)
    }
  }

  const handlePayment = async (values: {
    sourceAccountId: string
    payeeName: string
    reference: string
    amount: number
  }) => {
    setSubmitting(true)
    try {
      const response = await api.post<ActionResponse>('/api/customer/payments', values)
      setResultMessage(response.message)
      message.success(response.message)
      paymentForm.resetFields()
      await loadAccounts()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <PageHeader
        title="Payments and transfers"
        subtitle="Use active customer accounts to transfer funds or pay external billers while the backend is still mocked."
        extra={
          <Button onClick={() => void loadAccounts()} loading={loading}>
            Refresh balances
          </Button>
        }
      />

      {!activeAccounts.length && (
        <Alert
          type="warning"
          showIcon
          message="No active accounts are available for outgoing transactions."
        />
      )}

      {resultMessage && <Alert type="success" showIcon message={resultMessage} />}

      <Tabs
        items={[
          {
            key: 'transfer',
            label: 'Transfer',
            children: (
              <Card>
                <Form form={transferForm} layout="vertical" onFinish={values => void handleTransfer(values)}>
                  <Form.Item
                    label="Source account"
                    name="sourceAccountId"
                    rules={[{ required: true, message: 'Select a source account.' }]}
                  >
                    <Select
                      options={activeAccounts.map(account => ({
                        label: `${account.name} (${account.accountNumber})`,
                        value: account.id,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item
                    label="Recipient name"
                    name="recipientName"
                    rules={[{ required: true, message: 'Enter the recipient name.' }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item
                    label="Recipient account number"
                    name="recipientAccountNumber"
                    rules={[{ required: true, message: 'Enter the recipient account number.' }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item
                    label="Amount"
                    name="amount"
                    rules={[{ required: true, message: 'Enter an amount.' }]}
                  >
                    <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item
                    label="Description"
                    name="description"
                    rules={[{ required: true, message: 'Enter a transfer description.' }]}
                  >
                    <Input />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" loading={submitting} disabled={!activeAccounts.length}>
                    Submit transfer
                  </Button>
                </Form>
              </Card>
            ),
          },
          {
            key: 'payment',
            label: 'Bill payment',
            children: (
              <Card>
                <Form form={paymentForm} layout="vertical" onFinish={values => void handlePayment(values)}>
                  <Form.Item
                    label="Source account"
                    name="sourceAccountId"
                    rules={[{ required: true, message: 'Select a source account.' }]}
                  >
                    <Select
                      options={activeAccounts.map(account => ({
                        label: `${account.name} (${account.accountNumber})`,
                        value: account.id,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item
                    label="Payee"
                    name="payeeName"
                    rules={[{ required: true, message: 'Enter the payee name.' }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item
                    label="Reference"
                    name="reference"
                    rules={[{ required: true, message: 'Enter the payment reference.' }]}
                  >
                    <Input />
                  </Form.Item>
                  <Form.Item
                    label="Amount"
                    name="amount"
                    rules={[{ required: true, message: 'Enter an amount.' }]}
                  >
                    <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" loading={submitting} disabled={!activeAccounts.length}>
                    Submit payment
                  </Button>
                </Form>
              </Card>
            ),
          },
        ]}
      />

      <Card>
        <Typography.Title level={5}>Transaction rules</Typography.Title>
        <Typography.Paragraph style={{ marginBottom: 0 }}>
          Outgoing transactions are only allowed from active accounts. Pending-block and blocked accounts remain visible
          in the account area but cannot send new transfers or payments.
        </Typography.Paragraph>
      </Card>
    </Space>
  )
}
