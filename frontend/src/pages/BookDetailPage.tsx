import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Skeleton, Space, Tag, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { fetchBook } from '../api/catalog'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Text, Title } = Typography

const statusLabels = {
  ONGOING: '连载中',
  COMPLETED: '已完结',
  PAUSED: '暂停更新',
  UNKNOWN: '状态未知',
}

export default function BookDetailPage() {
  const { id = '' } = useParams()
  const book = useQuery({ queryKey: ['catalog', 'book', id], queryFn: () => fetchBook(id), enabled: Boolean(id) })

  return (
    <main className="page-shell">
      <SiteHeader />
      {book.isPending ? <Skeleton active paragraph={{ rows: 10 }} /> : null}
      {book.isError ? <Alert type="error" showIcon message="小说不存在或暂时无法读取" /> : null}
      {book.data ? (
        <article className="book-detail">
          <div className="detail-cover" aria-hidden="true">{book.data.title.slice(0, 1)}</div>
          <div className="detail-content">
            <Space wrap>
              <Tag>{book.data.category ?? '未分类'}</Tag>
              <Tag color={book.data.status === 'COMPLETED' ? 'green' : 'gold'}>{statusLabels[book.data.status]}</Tag>
            </Space>
            <Title>{book.data.title}</Title>
            <Text className="detail-author">作者：{book.data.author}</Text>
            <Paragraph className="detail-description">{book.data.description}</Paragraph>
            <Space wrap size="middle">
              {book.data.latestChapter ? (
                <Button type="primary" size="large">
                  <Link to={`/book/${book.data.id}/read/${book.data.latestChapter.id}`}>开始阅读</Link>
                </Button>
              ) : null}
              <Button size="large"><Link to={`/book/${book.data.id}/chapters`}>查看目录</Link></Button>
            </Space>
          </div>
          <Card className="detail-info" variant="borderless">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="章节">{book.data.chapterCount}</Descriptions.Item>
              <Descriptions.Item label="字数">{book.data.wordCount}</Descriptions.Item>
              <Descriptions.Item label="来源">{book.data.source.name}</Descriptions.Item>
              <Descriptions.Item label="最新章节">{book.data.latestChapter?.title ?? '暂无'}</Descriptions.Item>
            </Descriptions>
          </Card>
        </article>
      ) : null}
    </main>
  )
}
