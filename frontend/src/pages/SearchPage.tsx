import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App as AntApp, Button, Card, Empty, Input, Pagination, Skeleton, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { downloadOnlineBook, importOnlineBook, searchBooks, searchOnlineBooks } from '../api/catalog'
import { addRemoteBook } from '../api/reading'
import { useAuthStore } from '../auth/authStore'
import BookCard from '../components/BookCard'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const user = useAuthStore((state) => state.user)
  const keyword = params.get('q')?.trim() ?? ''
  const page = Math.max(1, Number(params.get('page')) || 1)
  const [input, setInput] = useState(keyword)
  const results = useQuery({ queryKey: ['catalog', 'search', keyword, page], queryFn: () => searchBooks(keyword, page), enabled: !!keyword })
  const onlineResults = useQuery({ queryKey: ['crawler', 'search', keyword], queryFn: () => searchOnlineBooks(keyword), enabled: !!keyword, retry: false })
  const importer = useMutation({
    mutationFn: importOnlineBook,
    onSuccess: async (book) => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'remote-bookshelf'] })
      message.success(`《${book.title}》已导入到你的私人书库`)
      navigate(`/book/${book.bookId}`)
    },
    onError: (error) => message.error(error instanceof Error ? error.message : '导入失败'),
  })
  const downloader = useMutation({
    mutationFn: downloadOnlineBook,
    onSuccess: () => message.success('小说已下载'),
    onError: () => message.error('下载失败，请更换来源或稍后重试'),
  })
  const bookmark = useMutation({
    mutationFn: addRemoteBook,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reading', 'remote-bookshelf'] })
      message.success('已加入书架，无需先导入正文')
    },
    onError: () => message.error('加入书架失败'),
  })

  const requireLogin = (action: () => void) => {
    if (!user) navigate('/login', { state: { from: `/search?q=${encodeURIComponent(keyword)}` } })
    else action()
  }
  const submit = (value: string) => {
    const query = value.trim()
    setInput(query)
    setParams(query ? { q: query, page: '1' } : {})
  }

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="page-heading">
        <Title>搜索小说</Title>
        <Paragraph>先查找本站书目；没有匹配时，直接显示联网书源。</Paragraph>
        <Input.Search aria-label="搜索书名或作者" size="large" value={input} maxLength={100}
          placeholder="输入书名或作者" enterButton="搜索" onChange={(event) => setInput(event.target.value)} onSearch={submit} />
      </section>

      {!keyword ? <Empty description="输入关键词开始搜索" /> : null}
      {results.isPending && keyword ? <Skeleton active paragraph={{ rows: 5 }} /> : null}
      {results.isError ? <Alert type="error" showIcon message="本站搜索失败，请稍后重试" /> : null}
      {results.data && results.data.total > 0 ? (
        <section className="search-results">
          <div className="section-heading"><Title level={2}>本站书目</Title><span>{results.data.total} 本</span></div>
          <div className="book-grid">{results.data.items.map((book) => <BookCard key={book.id} book={book} />)}</div>
          {results.data.total > results.data.pageSize ? <Pagination current={results.data.page} pageSize={results.data.pageSize}
            total={results.data.total} showSizeChanger={false}
            onChange={(nextPage) => setParams({ q: keyword, page: String(nextPage) })} /> : null}
        </section>
      ) : null}

      {keyword ? (
        <section className="search-results online-search-results">
          <div className="section-heading">
            <div><Title level={2}>联网搜索</Title>
              <Paragraph type="secondary">可直接下载 TXT；登录后可加入书架，或后台导入到仅自己可见的在线书库。</Paragraph></div>
            {onlineResults.data ? <span>{onlineResults.data.length} 条</span> : null}
          </div>
          {onlineResults.isPending ? <Skeleton active paragraph={{ rows: 5 }} /> : null}
          {onlineResults.isError ? <Alert type="warning" showIcon message="联网搜索暂不可用"
            description="请确认 xmreader-sonovel-adapter 容器已启动并通过健康检查。" /> : null}
          {importer.isPending ? <Alert className="crawler-import-alert" type="info" showIcon message="后台正在导入整本小说"
            description="可以离开搜索页；任务完成后会写入你的私人书库。" /> : null}
          {onlineResults.data?.length ? (
            <div className="online-book-grid">
              {onlineResults.data.map((book) => (
                <Card key={`${book.sourceId}:${book.sourceUrl}`} className="online-book-card">
                  <Space size={[6, 6]} wrap><Tag color="blue">{book.sourceName}</Tag>
                    {book.category ? <Tag>{book.category}</Tag> : null}{book.statusText ? <Tag>{book.statusText}</Tag> : null}
                    {!book.importSupported ? <Tag color="gold">仅元数据</Tag> : null}</Space>
                  <Title level={4}>{book.title}</Title><Paragraph type="secondary">{book.author}</Paragraph>
                  <Paragraph ellipsis={{ rows: 2 }}>{book.description || '暂无简介'}</Paragraph>
                  {book.latestChapterTitle ? <Paragraph className="online-book-latest" type="secondary">最新：{book.latestChapterTitle}</Paragraph> : null}
                  <Space wrap>
                    <Button disabled={!book.importSupported}
                      loading={downloader.isPending && downloader.variables?.sourceUrl === book.sourceUrl}
                      onClick={() => downloader.mutate(book)}>{book.importSupported ? '下载 TXT' : '暂不支持下载'}</Button>
                    <Button onClick={() => requireLogin(() => bookmark.mutate(book))}>加入书架</Button>
                    <Button type="primary" loading={importer.isPending && importer.variables === book.sourceUrl}
                      disabled={!book.importSupported || (importer.isPending && importer.variables !== book.sourceUrl)}
                      onClick={() => requireLogin(() => importer.mutate(book.sourceUrl))}>导入在线阅读</Button>
                    <Button type="link" href={book.sourceUrl} target="_blank" rel="noreferrer">查看源站</Button>
                  </Space>
                </Card>
              ))}
            </div>
          ) : onlineResults.isSuccess ? <Empty description="联网书源也没有找到结果" /> : null}
        </section>
      ) : null}
    </main>
  )
}
