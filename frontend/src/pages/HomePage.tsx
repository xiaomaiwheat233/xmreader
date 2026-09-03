import { useQuery } from '@tanstack/react-query'
import { Button, Card, Space, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchHealth } from '../api/health'
import { useAuthStore } from '../auth/authStore'

const { Paragraph, Text, Title } = Typography

export default function HomePage() {
  const user = useAuthStore((state) => state.user)
  const authStatus = useAuthStore((state) => state.status)
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
        <Text className="eyebrow">XMREADER · LOCAL-FIRST READING</Text>
        <Title id="page-title">小麦中文网</Title>
        <Paragraph className="subtitle">
          聚合开放、合规的小说内容，专注清爽的检索与阅读体验。当前本地版本已完成基础服务与用户认证能力。
        </Paragraph>
        <Space size="middle" wrap>
          <Tag color={status.color}>{status.label}</Tag>
          {health.data ? <Text type="secondary">Backend {health.data.version}</Text> : null}
        </Space>
      </section>

      <Card className="status-card" variant="borderless">
        <Space orientation="vertical" size="middle">
          <Title level={3}>{user ? `欢迎回来，${user.nickname}` : '开始使用小麦中文网'}</Title>
          <Paragraph>
            {user
              ? '你的登录状态已安全恢复，可以进入个人中心管理资料。'
              : '注册或登录后，即可在后续版本中同步书架、阅读进度和最近阅读。'}
          </Paragraph>
          <Space wrap>
            <Button type="primary" onClick={() => health.refetch()} loading={health.isFetching}>
              重新检查
            </Button>
            {authStatus === 'authenticated' ? (
              <Link to="/profile">个人中心</Link>
            ) : (
              <>
                <Link to="/login">登录</Link>
                <Link to="/register">注册</Link>
              </>
            )}
          </Space>
        </Space>
      </Card>
    </main>
  )
}
