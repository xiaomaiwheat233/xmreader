import { useMutation } from '@tanstack/react-query'
import { App as AntApp, Button, Card, Form, Input, Space, Typography } from 'antd'
import axios from 'axios'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { resetPassword, type ResetPasswordInput } from '../api/auth'
import type { ApiEnvelope } from '../api/client'
import { useAuthStore } from '../auth/authStore'

const { Paragraph, Title } = Typography

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const status = useAuthStore((state) => state.status)
  const mutation = useMutation({
    mutationFn: resetPassword,
    onSuccess: () => {
      message.success('密码已重置，请使用新密码登录')
      navigate('/login', { replace: true })
    },
    onError: (error) => message.error(errorMessage(error)),
  })

  if (status === 'authenticated') return <Navigate to="/profile" replace />

  return (
    <main className="app-shell auth-page">
      <Card className="auth-card" variant="borderless">
        <Title level={2}>找回密码</Title>
        <Paragraph type="secondary">个人测试模式：确认用户名后即可设置新密码</Paragraph>
        <Form<ResetPasswordInput> layout="vertical" onFinish={(values) => mutation.mutate(values)}>
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
            label="新密码"
            name="newPassword"
            rules={[{ required: true, min: 8, max: 72, message: '密码长度为 8–72 个字符' }]}
          >
            <Input.Password autoComplete="new-password" maxLength={72} />
          </Form.Item>
          <Form.Item
            label="确认密码"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  return !value || getFieldValue('newPassword') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" maxLength={72} />
          </Form.Item>
          <Space orientation="vertical" className="form-actions">
            <Button type="primary" htmlType="submit" block loading={mutation.isPending}>重置密码</Button>
            <Link to="/login">返回登录</Link>
          </Space>
        </Form>
      </Card>
    </main>
  )
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError<ApiEnvelope<never>>(error)) {
    return error.response?.data.message ?? '密码重置失败，请稍后重试'
  }
  return '密码重置失败，请稍后重试'
}
