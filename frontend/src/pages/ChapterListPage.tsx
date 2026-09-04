import { useQuery } from '@tanstack/react-query'
import { Alert, List, Skeleton, Typography } from 'antd'
import { Link, useParams } from 'react-router-dom'
import { fetchAllChapters, fetchBook } from '../api/catalog'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function ChapterListPage() {
  const { id = '' } = useParams()
  const book = useQuery({ queryKey: ['catalog', 'book', id], queryFn: () => fetchBook(id), enabled: Boolean(id) })
  const chapters = useQuery({
    queryKey: ['catalog', 'book', id, 'chapters'],
    queryFn: () => fetchAllChapters(id),
    enabled: Boolean(id),
  })

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="page-heading">
        <Title>{book.data?.title ?? '章节目录'}</Title>
        <Paragraph>{book.data ? `${book.data.author} · 共 ${book.data.chapterCount} 章` : '按章节顺序浏览'}</Paragraph>
      </section>
      {book.isError || chapters.isError ? <Alert type="error" showIcon message="目录暂时无法读取" /> : null}
      {chapters.isPending ? <Skeleton active paragraph={{ rows: 10 }} /> : null}
      {chapters.data ? (
        <List
          className="chapter-list"
          dataSource={chapters.data}
          renderItem={(chapter) => (
            <List.Item extra={<span>{chapter.wordCount} 字</span>}>
              <Link to={`/book/${id}/read/${chapter.id}`}>
                <span className="chapter-index">{String(chapter.chapterIndex).padStart(2, '0')}</span>
                {chapter.title}
              </Link>
            </List.Item>
          )}
        />
      ) : null}
    </main>
  )
}
