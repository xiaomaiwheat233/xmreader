import { useMutation } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Space, Typography } from 'antd'
import axios from 'axios'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { register, type RegisterInput } from '../api/auth'
import type { ApiEnvelope } from '../api/client'
import { useAuthStore } from '../auth/authStore'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function RegisterPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const status = useAuthStore((state) => state.status)
  const mutation = useMutation({
    mutationFn: register,
    onSuccess: () => {
      message.success('注册成功，请登录')
      navigate('/login', { replace: true })
    },
    onError: (error) => message.error(errorMessage(error)),
  })

  if (status === 'authenticated') return <Navigate to="/profile" replace />

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="auth-page">
        <Card className="auth-card" variant="borderless">
        <Title level={2}>加入小麦中文网</Title>
        <Paragraph type="secondary">创建账号，准备同步书架和阅读进度</Paragraph>
        <Form<RegisterInput> layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名' },
              { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '使用 3–32 位字母、数字或下划线' },
            ]}
          >
            <Input autoComplete="username" maxLength={32} />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, min: 8, max: 72, message: '密码长度为 8–72 个字符' }]}
          >
            <Input.Password autoComplete="new-password" maxLength={72} />
          </Form.Item>
          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return !value || getFieldValue('password') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" maxLength={72} />
          </Form.Item>
          <Space orientation="vertical" className="form-actions">
            <Button type="primary" htmlType="submit" block loading={mutation.isPending}>注册</Button>
            <span>已有账号？<Link to="/login">去登录</Link></span>
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
    return error.response?.data.message ?? '注册失败，请稍后重试'
  }
  return '注册失败，请稍后重试'
}
