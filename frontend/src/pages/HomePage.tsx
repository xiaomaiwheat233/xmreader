import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Empty, Skeleton, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { fetchHome } from '../api/catalog'
import BookCard from '../components/BookCard'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Text, Title } = Typography

export default function HomePage() {
  const home = useQuery({ queryKey: ['catalog', 'home'], queryFn: fetchHome })

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="home-hero" aria-labelledby="page-title">
        <Text className="eyebrow">XMREADER · ORIGINAL FIXTURES</Text>
        <Title id="page-title">小麦中文网</Title>
        <Paragraph>
          在一页页安静的文字里，找到值得继续读下去的故事。当前内容为原创模拟数据，用于验证完整阅读链路。
        </Paragraph>
        <Space wrap>
          <Button type="primary" size="large"><Link to="/search">搜索小说</Link></Button>
          <Button size="large"><a href="#recommended">浏览推荐</a></Button>
        </Space>
      </section>

      {home.isError ? (
        <Alert type="error" showIcon message="暂时无法读取书库" description="请确认 MySQL 与后端服务已经启动。" />
      ) : null}
      {home.isPending ? <Skeleton active paragraph={{ rows: 8 }} /> : null}
      {home.data ? (
        <>
          <BookSection id="recommended" title="为你推荐" books={home.data.recommended} />
          <BookSection title="最近更新" books={home.data.recentlyUpdated} />
          <BookSection title="热门小说" books={home.data.popular} />
        </>
      ) : null}
    </main>
  )
}

function BookSection({ id, title, books }: { id?: string; title: string; books: Awaited<ReturnType<typeof fetchHome>>['recommended'] }) {
  return (
    <section id={id} className="book-section">
      <div className="section-heading">
        <Title level={2}>{title}</Title>
        <Link to="/search">查看全部</Link>
      </div>
      {books.length ? (
        <div className="book-grid">{books.map((book) => <BookCard key={book.id} book={book} />)}</div>
      ) : <Empty description="书库暂无内容" />}
    </section>
  )
}
