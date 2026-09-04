import { apiClient, type ApiEnvelope } from './client'

export interface BookSummary {
  id: string
  title: string
  author: string
  coverUrl: string | null
  description: string | null
  category: string | null
  status: 'ONGOING' | 'COMPLETED' | 'PAUSED' | 'UNKNOWN'
  wordCount: number
  chapterCount: number
  latestChapterId: string | null
  latestChapterTitle: string | null
  updatedAt: string
}

export interface PageData<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface HomeData {
  recommended: BookSummary[]
  recentlyUpdated: BookSummary[]
  popular: BookSummary[]
}

export interface BookDetail {
  id: string
  title: string
  author: string
  coverUrl: string | null
  description: string | null
  category: string | null
  status: BookSummary['status']
  wordCount: number
  chapterCount: number
  latestChapter: { id: string; title: string; chapterIndex: number } | null
  source: { key: string; name: string }
  inBookshelf: boolean
  lastCrawledAt: string | null
  updatedAt: string
}

export interface ChapterSummary {
  id: string
  chapterIndex: number
  title: string
  wordCount: number
  publishedAt: string | null
}

export interface ChapterDetail {
  id: string
  book: { id: string; title: string }
  chapterIndex: number
  title: string
  content: string
  wordCount: number
  previousChapterId: string | null
  nextChapterId: string | null
  updatedAt: string
}

export interface OnlineBookCandidate {
  sourceId: number
  sourceName: string
  sourceUrl: string
  title: string
  author: string
  description: string | null
  coverUrl?: string | null
  category: string | null
  latestChapterTitle: string | null
  updatedAtText?: string | null
  statusText: string | null
  wordCountText?: string | null
  importSupported: boolean
}

export interface ImportedBook {
  bookId: string
  title: string
  importedChapterCount: number
}

export interface ImportTask {
  taskId: string
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  bookId: string | null
  title: string | null
  importedChapterCount: number | null
  errorMessage: string | null
}

export async function fetchHome(): Promise<HomeData> {
  const { data } = await apiClient.get<ApiEnvelope<HomeData>>('/home')
  return data.data
}

export async function searchBooks(query: string, page = 1): Promise<PageData<BookSummary>> {
  const { data } = await apiClient.get<ApiEnvelope<PageData<BookSummary>>>('/search', {
    params: { q: query, page, pageSize: 12 },
  })
  return data.data
}

export async function searchOnlineBooks(query: string): Promise<OnlineBookCandidate[]> {
  const { data } = await apiClient.get<ApiEnvelope<OnlineBookCandidate[]>>('/crawler/search', {
    params: { q: query },
    timeout: 45_000,
  })
  return data.data
}

export async function importOnlineBook(sourceUrl: string): Promise<ImportedBook> {
  const started = await apiClient.post<ApiEnvelope<ImportTask>>('/crawler/imports', { sourceUrl })
  let task = started.data.data
  while (task.status === 'QUEUED' || task.status === 'RUNNING') {
    await new Promise((resolve) => window.setTimeout(resolve, 1500))
    const response = await apiClient.get<ApiEnvelope<ImportTask>>(`/crawler/imports/${task.taskId}`)
    task = response.data.data
  }
  if (task.status === 'FAILED' || !task.bookId) {
    throw new Error(task.errorMessage || '导入失败')
  }
  return { bookId: task.bookId, title: task.title || '', importedChapterCount: task.importedChapterCount || 0 }
}

export async function downloadOnlineBook(book: OnlineBookCandidate): Promise<void> {
  const response = await apiClient.post<Blob>('/crawler/downloads', { sourceUrl: book.sourceUrl }, {
    responseType: 'blob', timeout: 3_650_000,
  })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `${book.title.replace(/[\\/:*?"<>|]/g, '_')}.txt`
  link.click()
  URL.revokeObjectURL(url)
}

export async function uploadNovel(input: { title?: string; author?: string; file: File }): Promise<ImportedBook> {
  const form = new FormData()
  if (input.title) form.append('title', input.title)
  if (input.author) form.append('author', input.author)
  form.append('file', input.file)
  const { data } = await apiClient.post<ApiEnvelope<ImportedBook>>('/books/uploads', form, { timeout: 60_000 })
  return data.data
}

export async function fetchBook(bookId: string): Promise<BookDetail> {
  const { data } = await apiClient.get<ApiEnvelope<BookDetail>>(`/books/${bookId}`)
  return data.data
}

export async function fetchChapters(bookId: string, page = 1): Promise<PageData<ChapterSummary>> {
  const { data } = await apiClient.get<ApiEnvelope<PageData<ChapterSummary>>>(`/books/${bookId}/chapters`, {
    params: { page, pageSize: 100 },
  })
  return data.data
}

export async function fetchAllChapters(bookId: string): Promise<ChapterSummary[]> {
  const pageSize = 200
  const firstPage = await apiClient.get<ApiEnvelope<PageData<ChapterSummary>>>(`/books/${bookId}/chapters`, {
    params: { page: 1, pageSize },
  })
  const items = [...firstPage.data.data.items]
  const total = firstPage.data.data.total
  for (let page = 2; items.length < total; page += 1) {
    const { data } = await apiClient.get<ApiEnvelope<PageData<ChapterSummary>>>(`/books/${bookId}/chapters`, {
      params: { page, pageSize },
    })
    if (!data.data.items.length) break
    items.push(...data.data.items)
  }
  return items
}

export async function fetchChapter(chapterId: string): Promise<ChapterDetail> {
  const { data } = await apiClient.get<ApiEnvelope<ChapterDetail>>(`/chapters/${chapterId}`)
  return data.data
}
