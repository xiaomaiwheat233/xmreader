import { Space } from 'antd'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../auth/authStore'

export default function SiteHeader() {
  const user = useAuthStore((state) => state.user)

  return (
    <header className="site-header">
      <Link className="site-brand" to="/">小麦中文网</Link>
      <nav aria-label="主要导航">
        <Space size="middle">
          <Link to="/search">搜索</Link>
          {user ? (
            <Link to="/profile">{user.nickname}</Link>
          ) : (
            <Link to="/login">登录</Link>
          )}
        </Space>
      </nav>
    </header>
  )
}
