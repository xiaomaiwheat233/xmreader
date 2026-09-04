import { useMutation } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Space, Typography } from 'antd'
import axios from 'axios'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { login, type LoginInput } from '../api/auth'
import type { ApiEnvelope } from '../api/client'
import { useAuthStore } from '../auth/authStore'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function LoginPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const status = useAuthStore((state) => state.status)
  const setSession = useAuthStore((state) => state.setSession)
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (session) => {
      setSession(session)
      message.success('登录成功')
      navigate('/profile', { replace: true })
    },
    onError: (error) => message.error(errorMessage(error)),
  })

  if (status === 'authenticated') return <Navigate to="/profile" replace />

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="auth-page">
        <Card className="auth-card" variant="borderless">
        <Title level={2}>登录小麦中文网</Title>
        <Paragraph type="secondary">继续你的阅读旅程</Paragraph>
        <Form<LoginInput> layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input autoComplete="username" maxLength={32} />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password autoComplete="current-password" maxLength={72} />
          </Form.Item>
          <Space orientation="vertical" className="form-actions">
            <Button type="primary" htmlType="submit" block loading={mutation.isPending}>登录</Button>
            <Link to="/forgot-password">忘记密码？</Link>
            <span>还没有账号？<Link to="/register">立即注册</Link></span>
            <Link to="/">返回首页</Link>
          </Space>
        </Form>
        </Card>
      </section>
    </main>
  )
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError<ApiEnvelope<never>>(error)) {
    return error.response?.data.message ?? '登录失败，请稍后重试'
  }
  return '登录失败，请稍后重试'
}
