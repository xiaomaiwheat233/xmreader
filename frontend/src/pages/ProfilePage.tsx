import { useMutation } from '@tanstack/react-query'
import { App as AntApp, Avatar, Button, Card, Form, Input, Space, Spin, Typography } from 'antd'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { changePassword, logout, updateProfile } from '../api/auth'
import { useAuthStore } from '../auth/authStore'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

interface ProfileForm {
  nickname: string
  avatarUrl: string
}

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const { message } = AntApp.useApp()
  const { status, user, clearSession, setSession, accessToken } = useAuthStore()
  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updatedUser) => {
      if (accessToken) {
        setSession({ accessToken, tokenType: 'Bearer', expiresIn: 0, user: updatedUser })
      }
      message.success('资料已保存')
    },
    onError: () => message.error('资料保存失败，请检查输入'),
  })
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession()
      navigate('/', { replace: true })
    },
  })
  const passwordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: PasswordForm) =>
      changePassword({ currentPassword, newPassword }),
    onSuccess: () => {
      clearSession()
      message.success('密码已修改，请重新登录')
      navigate('/login', { replace: true })
    },
    onError: () => message.error('密码修改失败，请检查当前密码'),
  })

  if (status === 'checking') {
    return <main className="app-shell loading-page"><Spin size="large" /></main>
  }
  if (!user) return <Navigate to="/login" replace />

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="auth-page">
        <Card className="auth-card profile-card" variant="borderless">
        <Space align="center" size="middle">
          <Avatar
            size={64}
            src={
              user.avatarUrl ? (
                <img src={user.avatarUrl} alt="" referrerPolicy="no-referrer" />
              ) : undefined
            }
          >
            {user.nickname.slice(0, 1)}
          </Avatar>
          <div>
            <Title level={2}>{user.nickname}</Title>
            <Paragraph type="secondary">@{user.username} · {user.role}</Paragraph>
          </div>
        </Space>
        <Form<ProfileForm>
          layout="vertical"
          initialValues={{ nickname: user.nickname, avatarUrl: user.avatarUrl ?? '' }}
          onFinish={(values) => updateMutation.mutate(values)}
        >
          <Form.Item label="昵称" name="nickname" rules={[{ required: true, max: 64 }]}>
            <Input maxLength={64} />
          </Form.Item>
          <Form.Item label="头像地址" name="avatarUrl" rules={[{ type: 'url', message: '请输入有效的 HTTPS 地址' }]}>
            <Input placeholder="https://..." maxLength={2048} />
          </Form.Item>
          <Space wrap>
            <Button type="primary" htmlType="submit" loading={updateMutation.isPending}>保存资料</Button>
            <Button danger onClick={() => logoutMutation.mutate()} loading={logoutMutation.isPending}>退出登录</Button>
            <Link to="/bookshelf">我的书架</Link>
            <Link to="/">返回首页</Link>
          </Space>
        </Form>

        <div className="password-section">
          <Title level={3}>修改密码</Title>
          <Form<PasswordForm> layout="vertical" onFinish={(values) => passwordMutation.mutate(values)}>
            <Form.Item label="当前密码" name="currentPassword" rules={[{ required: true }]}>
              <Input.Password autoComplete="current-password" maxLength={72} />
            </Form.Item>
            <Form.Item
              label="新密码"
              name="newPassword"
              rules={[{ required: true, min: 8, max: 72, message: '密码长度为 8–72 个字符' }]}
            >
              <Input.Password autoComplete="new-password" maxLength={72} />
            </Form.Item>
            <Form.Item
              label="确认新密码"
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
            <Button type="primary" htmlType="submit" loading={passwordMutation.isPending}>
              更新密码
            </Button>
          </Form>
        </div>
        </Card>
      </section>
    </main>
  )
}
