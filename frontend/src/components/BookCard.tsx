import { Card, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import type { BookSummary } from '../api/catalog'

const { Paragraph, Text, Title } = Typography

const statusLabels: Record<BookSummary['status'], string> = {
  ONGOING: '连载中',
  COMPLETED: '已完结',
  PAUSED: '暂停更新',
  UNKNOWN: '状态未知',
}

export default function BookCard({ book }: { book: BookSummary }) {
  return (
    <Link className="book-card-link" to={`/book/${book.id}`}>
      <Card className="book-card" variant="borderless">
        <div className="book-cover" aria-hidden="true">
          <span>{book.title.slice(0, 1)}</span>
        </div>
        <div className="book-card-content">
          <div>
            <Tag variant="filled">{book.category ?? '未分类'}</Tag>
            <Tag variant="filled" color={book.status === 'COMPLETED' ? 'green' : 'gold'}>
              {statusLabels[book.status]}
            </Tag>
          </div>
          <Title level={4}>{book.title}</Title>
          <Text type="secondary">{book.author}</Text>
          <Paragraph ellipsis={{ rows: 2 }}>{book.description}</Paragraph>
          <Text className="book-meta" type="secondary">{book.chapterCount} 章 · {book.wordCount} 字</Text>
        </div>
      </Card>
    </Link>
  )
}
