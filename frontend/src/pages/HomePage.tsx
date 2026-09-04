import { Button, Typography } from 'antd'
import { Link } from 'react-router-dom'
import SiteHeader from '../components/SiteHeader'

const { Title } = Typography

export default function HomePage() {
  return (
    <main className="page-shell home-page">
      <SiteHeader />
      <section className="home-hero" aria-labelledby="page-title">
        <Title id="page-title">小麦中文网</Title>
        <Button type="primary" size="large"><Link to="/search">搜索小说</Link></Button>
      </section>
    </main>
  )
}
