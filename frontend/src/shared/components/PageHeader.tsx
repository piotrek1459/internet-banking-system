import type { ReactNode } from 'react'
import { Space, Typography } from 'antd'

interface PageHeaderProps {
  title: string
  subtitle?: string
  extra?: ReactNode
}

export default function PageHeader({ title, subtitle, extra }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div>
        <Typography.Title level={2} style={{ marginBottom: 8 }}>
          {title}
        </Typography.Title>
        {subtitle && (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {subtitle}
          </Typography.Paragraph>
        )}
      </div>
      {extra && <Space wrap>{extra}</Space>}
    </div>
  )
}
