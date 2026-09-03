import { useQuery } from '@tanstack/react-query'
import { Alert, Empty, Input, Pagination, Skeleton, Typography } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { searchBooks } from '../api/catalog'
import BookCard from '../components/BookCard'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const keyword = params.get('q')?.trim() ?? ''
  const page = Math.max(1, Number(params.get('page')) || 1)
  const [input, setInput] = useState(keyword)
  const results = useQuery({
    queryKey: ['catalog', 'search', keyword, page],
    queryFn: () => searchBooks(keyword, page),
    enabled: keyword.length > 0,
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
        <Paragraph>按书名或作者检索小麦中文网本地书库。</Paragraph>
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
    </main>
  )
}
