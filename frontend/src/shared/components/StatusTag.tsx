import { Tag } from 'antd'

export default function StatusTag({ status }: { status: string }) {
  const normalized = status.toUpperCase()

  const colorMap: Record<string, string> = {
    ACTIVE: 'green',
    BLOCKED: 'red',
    PENDING_BLOCK: 'gold',
    PENDING: 'gold',
    COMPLETED: 'blue',
    FAILED: 'red',
    SUCCESS: 'green',
    INFO: 'default',
    WARNING: 'orange',
    CRITICAL: 'red',
  }

  return <Tag color={colorMap[normalized] ?? 'default'}>{normalized.replace(/_/g, ' ')}</Tag>
}
