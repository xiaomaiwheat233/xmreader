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
  category: string | null
  latestChapterTitle: string | null
  updatedAtText: string | null
  statusText: string | null
  wordCountText: string | null
}

export interface ImportedBook {
  bookId: string
  title: string
  importedChapterCount: number
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
  const { data } = await apiClient.post<ApiEnvelope<ImportedBook>>(
    '/crawler/imports',
    { sourceUrl },
    { timeout: 190_000 },
  )
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

export async function fetchChapter(chapterId: string): Promise<ChapterDetail> {
  const { data } = await apiClient.get<ApiEnvelope<ChapterDetail>>(`/chapters/${chapterId}`)
  return data.data
}
