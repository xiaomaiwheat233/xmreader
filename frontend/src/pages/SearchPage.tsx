import { useMutation, useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Empty, Input, Pagination, Skeleton, Space, Tag, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { importOnlineBook, searchBooks, searchOnlineBooks } from '../api/catalog'
import BookCard from '../components/BookCard'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const keyword = params.get('q')?.trim() ?? ''
  const page = Math.max(1, Number(params.get('page')) || 1)
  const [input, setInput] = useState(keyword)
  const results = useQuery({
    queryKey: ['catalog', 'search', keyword, page],
    queryFn: () => searchBooks(keyword, page),
    enabled: keyword.length > 0,
  })
  const onlineResults = useQuery({
    queryKey: ['crawler', 'search', keyword],
    queryFn: () => searchOnlineBooks(keyword),
    enabled: keyword.length > 0,
    retry: false,
  })
  const importer = useMutation({
    mutationFn: importOnlineBook,
    onSuccess: (book) => navigate(`/book/${book.bookId}`),
  })

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
        <Paragraph>优先检索小麦中文网本地书库，也可以从联网书源导入前 5 章试读。</Paragraph>
        <Input.Search
          aria-label="搜索书名或作者"
          size="large"
          value={input}
          maxLength={100}
          placeholder="输入书名或作者"
          enterButton="搜索"
          onChange={(event) => setInput(event.target.value)}
          onSearch={submit}
        />
      </section>

      {!keyword ? <Empty description="输入关键词开始搜索" /> : null}
      {results.isPending && keyword ? <Skeleton active paragraph={{ rows: 8 }} /> : null}
      {results.isError ? <Alert type="error" showIcon message="搜索失败，请稍后重试" /> : null}
      {results.data ? (
        <section className="search-results">
          <div className="section-heading">
            <Title level={2}>“{keyword}”的搜索结果</Title>
            <span>{results.data.total} 本</span>
          </div>
          {results.data.items.length ? (
            <div className="book-grid">
              {results.data.items.map((book) => <BookCard key={book.id} book={book} />)}
            </div>
          ) : <Empty description="没有找到匹配的小说" />}
          {results.data.total > results.data.pageSize ? (
            <Pagination
              current={results.data.page}
              pageSize={results.data.pageSize}
              total={results.data.total}
              showSizeChanger={false}
              onChange={(nextPage) => setParams({ q: keyword, page: String(nextPage) })}
            />
          ) : null}
        </section>
      ) : null}

      {keyword ? (
        <section className="search-results online-search-results">
          <div className="section-heading">
            <div>
              <Title level={2}>联网书源</Title>
              <Paragraph type="secondary">结果来自独立采集适配器，无需登录即可导入前 5 章试读；请仅采集有权访问的内容。</Paragraph>
            </div>
            {onlineResults.data ? <span>{onlineResults.data.length} 条</span> : null}
          </div>
          {onlineResults.isPending ? <Skeleton active paragraph={{ rows: 5 }} /> : null}
          {onlineResults.isError ? (
            <Alert
              type="warning"
              showIcon
              message="联网搜索暂不可用"
              description="请确认 xmreader-sonovel-adapter 容器已经启动并通过健康检查。"
            />
          ) : null}
          {importer.isError ? (
            <Alert className="crawler-import-alert" type="error" showIcon message="导入失败，请更换来源或稍后重试" />
          ) : null}
          {onlineResults.data?.length ? (
            <div className="online-book-grid">
              {onlineResults.data.map((book) => {
                const importing = importer.isPending && importer.variables === book.sourceUrl
                return (
                  <Card key={`${book.sourceId}:${book.sourceUrl}`} className="online-book-card">
                    <Space size={[6, 6]} wrap>
                      <Tag color="blue">{book.sourceName}</Tag>
                      {book.category ? <Tag>{book.category}</Tag> : null}
                      {book.statusText ? <Tag>{book.statusText}</Tag> : null}
                    </Space>
                    <Title level={4}>{book.title}</Title>
                    <Paragraph type="secondary">{book.author}</Paragraph>
                    <Paragraph ellipsis={{ rows: 2 }}>{book.description || '暂无简介'}</Paragraph>
                    {book.latestChapterTitle ? (
                      <Paragraph className="online-book-latest" type="secondary">
                        最新：{book.latestChapterTitle}
                      </Paragraph>
                    ) : null}
                    <Button
                      type="primary"
                      loading={importing}
                      disabled={importer.isPending && !importing}
                      onClick={() => importer.mutate(book.sourceUrl)}
                    >
                      导入前 5 章
                    </Button>
                  </Card>
                )
              })}
            </div>
          ) : onlineResults.isSuccess ? <Empty description="联网书源也没有找到结果" /> : null}
        </section>
      ) : null}
    </main>
  )
}
