import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App as AntApp, Button, Card, Descriptions, Progress, Skeleton, Space, Tag, Typography } from 'antd'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { fetchBook } from '../api/catalog'
import { addToBookshelf, fetchReadingProgress, removeFromBookshelf } from '../api/reading'
import { useAuthStore } from '../auth/authStore'
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
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const user = useAuthStore((state) => state.user)
  const book = useQuery({
    queryKey: ['catalog', 'book', id, user?.id ?? 'anonymous'],
    queryFn: () => fetchBook(id),
    enabled: Boolean(id),
  })
  const progress = useQuery({
    queryKey: ['reading', 'progress', id, user?.id],
    queryFn: () => fetchReadingProgress(id),
    enabled: Boolean(id && user),
  })
  const shelfMutation = useMutation({
    mutationFn: async () => {
      if (!book.data) return
      if (book.data.inBookshelf) {
        await removeFromBookshelf(id)
        return 'removed' as const
      }
      await addToBookshelf(id)
      return 'added' as const
    },
    onSuccess: async (action) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'book', id] })
      await queryClient.invalidateQueries({ queryKey: ['reading', 'bookshelf'] })
      message.success(action === 'removed' ? '已移出书架' : '已加入书架')
    },
    onError: () => message.error('书架操作失败，请稍后重试'),
  })

  const readingChapterId = progress.data?.chapterId ?? book.data?.latestChapter?.id
  const handleShelf = () => {
    if (!user) {
      navigate('/login', { state: { from: `/book/${id}` } })
      return
    }
    shelfMutation.mutate()
  }

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
              {readingChapterId ? (
                <Button type="primary" size="large">
                  <Link to={`/book/${book.data.id}/read/${readingChapterId}`}>
                    {progress.data ? '继续阅读' : '开始阅读'}
                  </Link>
                </Button>
              ) : null}
              <Button size="large"><Link to={`/book/${book.data.id}/chapters`}>查看目录</Link></Button>
              <Button size="large" loading={shelfMutation.isPending} onClick={handleShelf}>
                {book.data.inBookshelf ? '移出书架' : '加入书架'}
              </Button>
            </Space>
            {progress.data ? (
              <div className="detail-progress">
                <Text type="secondary">上次读到第 {progress.data.chapterIndex} 章</Text>
                <Progress percent={Number(progress.data.progressPercent)} size="small" />
              </div>
            ) : null}
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
