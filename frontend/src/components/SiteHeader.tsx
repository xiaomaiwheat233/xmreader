import { Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../auth/authStore'

export default function SiteHeader() {
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const path = location.pathname
  const linkClass = (active: boolean, action = false) =>
    ['topbar-link', action ? 'topbar-action' : '', active ? 'is-active' : ''].filter(Boolean).join(' ')

  return (
    <header className="site-header">
      <Link className="site-brand" to="/">小麦中文网</Link>
      <nav className="topbar-nav" aria-label="主要导航">
        <Link className={linkClass(path === '/')} to="/">首页</Link>
        <Link className={linkClass(path === '/bookshelf')} to="/bookshelf">书架</Link>
        <Link className={linkClass(path === '/history')} to="/history">最近阅读</Link>
      </nav>
      <div className="topbar-actions">
        {user ? <Link className={linkClass(path === '/upload', true)} to="/upload">上传</Link> : null}
        <Link className={linkClass(path === '/search', true)} to="/search">搜索</Link>
        <Link
          className={linkClass(user ? path === '/profile' : ['/login', '/register', '/forgot-password'].includes(path), true)}
          to={user ? '/profile' : '/login'}
        >
          {user ? user.nickname : '登录 / 注册'}
        </Link>
      </div>
    </header>
  )
}
