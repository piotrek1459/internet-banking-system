import { Button, Card, Result } from 'antd'

export default function EmployeePage() {
  return (
    <Card>
      <Result
        status="info"
        title="Employee portal placeholder"
        subTitle="The current frontend scope focuses on customer and admin roles. This route remains reserved for later expansion."
        extra={<Button href="/login">Back to login</Button>}
      />
    </Card>
  )
}
