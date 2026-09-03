import { useQuery } from '@tanstack/react-query'
import { Button, Card, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchHealth } from './api/health'
import './App.css'

const { Paragraph, Text, Title } = Typography

export default function App() {
  const health = useQuery({
    queryKey: ['system', 'health'],
    queryFn: fetchHealth,
    retry: 1,
    refetchInterval: 30_000,
  })

  const status = health.isPending
    ? { color: 'processing', label: '正在连接后端' }
    : health.isSuccess
      ? { color: 'success', label: '本地服务可用' }
      : { color: 'error', label: '后端或数据库未启动' }

  return (
    <main className="app-shell">
      <section className="hero" aria-labelledby="page-title">
        <Text className="eyebrow">LOCAL-FIRST READING PLATFORM</Text>
        <Title id="page-title">NovelHub</Title>
        <Paragraph className="subtitle">
          多源小说聚合阅读平台正在搭建中。当前页面用于验证 React、Spring Boot 与 MySQL
          的本地连接链路。
        </Paragraph>
        <Space size="middle" wrap>
          <Tag color={status.color}>{status.label}</Tag>
          {health.data ? <Text type="secondary">Backend {health.data.version}</Text> : null}
        </Space>
      </section>

      <Card className="status-card" variant="borderless">
        <Space orientation="vertical" size="middle">
          <Title level={3}>开发环境状态</Title>
          <Paragraph>
            健康检查会执行真实数据库查询。启动 MySQL 和后端后，此处应显示“本地服务可用”。
          </Paragraph>
          <Space wrap>
            <Button type="primary" onClick={() => health.refetch()} loading={health.isFetching}>
              重新检查
            </Button>
            <Link to="/search">搜索页（下一阶段）</Link>
          </Space>
        </Space>
      </Card>
    </main>
  )
}
