import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as authApi from './api/auth'
import * as catalogApi from './api/catalog'

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    localStorage.clear()
    window.history.pushState({}, '', '/')
  })

  it('shows the 小麦中文网 home page with books from the catalog API', async () => {
    vi.spyOn(authApi, 'refreshSession').mockRejectedValue(new Error('not logged in'))
    const book = {
      id: '1',
      title: '麦田来信',
      author: '小麦编辑部',
      coverUrl: null,
      description: '原创测试故事',
      category: '现实',
      status: 'COMPLETED' as const,
      wordCount: 1000,
      chapterCount: 3,
      latestChapterId: '3',
      latestChapterTitle: '第三章',
      updatedAt: '2026-09-03T00:00:00Z',
    }
    vi.spyOn(catalogApi, 'fetchHome').mockResolvedValue({
      recommended: [book],
      recentlyUpdated: [book],
      popular: [book],
    })
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>,
    )

    expect((await screen.findAllByText('麦田来信')).length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: '小麦中文网' })).toBeInTheDocument()
  })

  it('renders chapter content and reader navigation', async () => {
    vi.spyOn(authApi, 'refreshSession').mockRejectedValue(new Error('not logged in'))
    vi.spyOn(catalogApi, 'fetchChapter').mockResolvedValue({
      id: 'chapter-2',
      book: { id: 'book-1', title: '麦田来信' },
      chapterIndex: 2,
      title: '风经过旧邮局',
      content: '第一段正文。\n\n第二段正文。',
      wordCount: 16,
      previousChapterId: 'chapter-1',
      nextChapterId: 'chapter-3',
      updatedAt: '2026-09-03T00:00:00Z',
    })
    window.history.pushState({}, '', '/book/book-1/read/chapter-2')
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: '风经过旧邮局' })).toBeInTheDocument()
    expect(screen.getByText('第一段正文。')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '上一章' })).toHaveAttribute(
      'href',
      '/book/book-1/read/chapter-1',
    )
    expect(screen.getByRole('link', { name: '下一章' })).toHaveAttribute(
      'href',
      '/book/book-1/read/chapter-3',
    )
  })
})
