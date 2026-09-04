import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Empty, Pagination, Progress, Skeleton, Space, Typography } from 'antd'
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import {
  fetchBookshelf,
  fetchRemoteBookshelf,
  fetchReadingHistory,
  removeFromBookshelf,
  removeRemoteBook,
  removeReadingHistory,
} from '../api/reading'
import { downloadOnlineBook, importOnlineBook } from '../api/catalog'
import { useAuthStore } from '../auth/authStore'
import BookCard from '../components/BookCard'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Text, Title } = Typography

export default function LibraryPage({ view }: { view: 'bookshelf' | 'history' }) {
  const status = useAuthStore((state) => state.status)
  const user = useAuthStore((state) => state.user)
  const [shelfPage, setShelfPage] = useState(1)
  const [historyPage, setHistoryPage] = useState(1)

  if (status === 'checking') {
    return <main className="page-shell"><Skeleton active paragraph={{ rows: 12 }} /></main>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: view === 'bookshelf' ? '/bookshelf' : '/history' }} />
  }

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="page-heading">
        <Text className="eyebrow">{view === 'bookshelf' ? 'BOOKSHELF' : 'READING HISTORY'}</Text>
        <Title>{view === 'bookshelf' ? '我的书架' : '最近阅读'}</Title>
        <Paragraph>{view === 'bookshelf' ? '收藏喜欢的小说，随时继续阅读。' : '从上次停下的位置继续。'}</Paragraph>
      </section>
      {view === 'bookshelf'
        ? <BookshelfPanel page={shelfPage} onPageChange={setShelfPage} />
        : <HistoryPanel page={historyPage} onPageChange={setHistoryPage} />}
    </main>
  )
}

function BookshelfPanel({ page, onPageChange }: { page: number; onPageChange: (page: number) => void }) {
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const shelf = useQuery({ queryKey: ['reading', 'bookshelf', page], queryFn: () => fetchBookshelf(page) })
  const remove = useMutation({
    mutationFn: removeFromBookshelf,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'bookshelf'] })
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'book'] })
      message.success('已移出书架')
    },
    onError: () => message.error('移出书架失败，请稍后重试'),
  })

  const remote = useQuery({ queryKey: ['reading', 'remote-bookshelf', page], queryFn: () => fetchRemoteBookshelf(page) })
  const removeRemote = useMutation({
    mutationFn: removeRemoteBook,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'remote-bookshelf'] })
      message.success('已移出书架')
    },
  })
  const importer = useMutation({
    mutationFn: importOnlineBook,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'remote-bookshelf'] })
      await queryClient.invalidateQueries({ queryKey: ['catalog'] })
      message.success('已导入到你的私人书库')
    },
    onError: () => message.error('导入失败'),
  })
  const downloader = useMutation({ mutationFn: downloadOnlineBook, onError: () => message.error('下载失败') })

  if (shelf.isPending || remote.isPending) return <Skeleton active paragraph={{ rows: 10 }} />
  if (!shelf.data || !remote.data) return <Empty description="书架加载失败，请稍后重试" />
  if (!shelf.data?.items.length && !remote.data?.items.length) return <Empty description="书架还是空的，去发现喜欢的小说吧" />

  return (
    <>
      <div className="library-grid">
        {shelf.data.items.map((item) => (
          <div className="library-item" key={item.book.id}>
            <BookCard book={item.book} />
            <div className="library-actions">
              {item.progress ? (
                <>
                  <Progress percent={Number(item.progress.progressPercent)} size="small" />
                  <Link to={`/book/${item.book.id}/read/${item.progress.chapterId}`}>
                    继续第 {item.progress.chapterIndex} 章
                  </Link>
                </>
              ) : (
                <Text type="secondary">尚未开始阅读</Text>
              )}
              <Button danger type="link" loading={remove.isPending} onClick={() => remove.mutate(item.book.id)}>
                移出书架
              </Button>
            </div>
          </div>
        ))}
      </div>
      {remote.data?.items.length ? (
        <section className="remote-shelf-section">
          <Title level={2}>待导入的联网书目</Title>
          <div className="online-book-grid">
            {remote.data.items.map((item) => (
              <div className="online-book-card" key={item.id}>
                <Title level={4}>{item.title}</Title>
                <Paragraph type="secondary">{item.author} · {item.sourceName}</Paragraph>
                <Space wrap>
                  <Button disabled={!item.importSupported} onClick={() => downloader.mutate(item)}>下载 TXT</Button>
                  <Button type="primary" disabled={!item.importSupported || !!item.importedBookId}
                    loading={importer.isPending && importer.variables === item.sourceUrl}
                    onClick={() => importer.mutate(item.sourceUrl)}>
                    {item.importedBookId ? '已导入' : '导入在线阅读'}
                  </Button>
                  {item.importedBookId ? <Link to={`/book/${item.importedBookId}`}>开始阅读</Link> : null}
                  <Button danger type="link" onClick={() => removeRemote.mutate(item.id)}>移出书架</Button>
                </Space>
              </div>
            ))}
          </div>
        </section>
      ) : null}
      <Pagination current={page} pageSize={20} total={shelf.data.total} onChange={onPageChange} hideOnSinglePage />
    </>
  )
}

function HistoryPanel({ page, onPageChange }: { page: number; onPageChange: (page: number) => void }) {
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const history = useQuery({ queryKey: ['reading', 'history', page], queryFn: () => fetchReadingHistory(page) })
  const remove = useMutation({
    mutationFn: removeReadingHistory,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'history'] })
      message.success('已删除阅读记录')
    },
    onError: () => message.error('删除记录失败，请稍后重试'),
  })

  if (history.isPending) return <Skeleton active paragraph={{ rows: 10 }} />
  if (!history.data?.items.length) return <Empty description="还没有阅读记录" />

  return (
    <>
      <div className="history-list">
        {history.data.items.map((item) => (
          <article className="history-item" key={item.book.id}>
            <div>
              <Title level={4}><Link to={`/book/${item.book.id}`}>{item.book.title}</Link></Title>
              <Paragraph type="secondary">{item.book.author}</Paragraph>
              <Link to={`/book/${item.book.id}/read/${item.chapter.id}`}>
                继续阅读：第 {item.chapter.chapterIndex} 章 {item.chapter.title}
              </Link>
            </div>
            <Space direction="vertical" align="end">
              <Text type="secondary">{new Date(item.visitedAt).toLocaleString('zh-CN')}</Text>
              <Button danger type="link" loading={remove.isPending} onClick={() => remove.mutate(item.book.id)}>
                删除记录
              </Button>
            </Space>
          </article>
        ))}
      </div>
      <Pagination current={page} pageSize={20} total={history.data.total} onChange={onPageChange} hideOnSinglePage />
    </>
  )
}
