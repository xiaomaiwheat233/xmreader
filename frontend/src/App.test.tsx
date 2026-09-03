import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as authApi from './api/auth'
import * as catalogApi from './api/catalog'
import * as readingApi from './api/reading'
import { useAuthStore } from './auth/authStore'

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
    localStorage.clear()
    window.history.pushState({}, '', '/')
    useAuthStore.setState({ accessToken: null, user: null, status: 'checking' })
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

  it('shows the authenticated reader bookshelf and saved progress', async () => {
    const user = {
      id: 'user-1',
      username: 'wheat_reader',
      nickname: '小麦读者',
      avatarUrl: null,
      role: 'USER' as const,
    }
    vi.spyOn(authApi, 'refreshSession').mockResolvedValue({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user,
    })
    vi.spyOn(readingApi, 'fetchBookshelf').mockResolvedValue({
      items: [{
        book: {
          id: 'book-1',
          title: '麦田来信',
          author: '小麦编辑部',
          coverUrl: null,
          description: '原创测试故事',
          category: '现实',
          status: 'COMPLETED',
          wordCount: 1000,
          chapterCount: 3,
          latestChapterId: 'chapter-3',
          latestChapterTitle: '第三章',
          updatedAt: '2026-09-03T00:00:00Z',
        },
        progress: {
          bookId: 'book-1',
          chapterId: 'chapter-2',
          chapterIndex: 2,
          chapterTitle: '风经过旧邮局',
          position: 100,
          progressPercent: 42.5,
          updatedAt: '2026-09-03T00:00:00Z',
        },
        addedAt: '2026-09-03T00:00:00Z',
      }],
      page: 1,
      pageSize: 20,
      total: 1,
    })
    vi.spyOn(readingApi, 'fetchReadingHistory').mockResolvedValue({
      items: [], page: 1, pageSize: 20, total: 0,
    })
    window.history.pushState({}, '', '/library')
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: '我的阅读' })).toBeInTheDocument()
    expect(await screen.findByText('麦田来信')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: '继续第 2 章' })).toHaveAttribute(
      'href',
      '/book/book-1/read/chapter-2',
    )
  })
})
