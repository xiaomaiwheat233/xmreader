import { apiClient, type ApiEnvelope } from './client'
import type { BookSummary, PageData } from './catalog'

export interface ReadingProgress {
  bookId: string
  chapterId: string
  chapterIndex: number
  chapterTitle: string
  position: number
  progressPercent: number
  updatedAt: string
}

export interface BookshelfItem {
  book: BookSummary
  progress: ReadingProgress | null
  addedAt: string
}

export interface ReadingHistoryItem {
  book: BookSummary
  chapter: { id: string; chapterIndex: number; title: string }
  visitedAt: string
}

export interface SaveProgressInput {
  bookId: string
  chapterId: string
  position: number
  progressPercent: number
}

export async function fetchBookshelf(page = 1): Promise<PageData<BookshelfItem>> {
  const { data } = await apiClient.get<ApiEnvelope<PageData<BookshelfItem>>>('/bookshelf', {
    params: { page, pageSize: 20 },
  })
  return data.data
}

export async function addToBookshelf(bookId: string): Promise<void> {
  await apiClient.put(`/bookshelf/${bookId}`)
}

export async function removeFromBookshelf(bookId: string): Promise<void> {
  await apiClient.delete(`/bookshelf/${bookId}`)
}

export async function fetchReadingProgress(bookId: string): Promise<ReadingProgress | null> {
  const { data } = await apiClient.get<ApiEnvelope<ReadingProgress | null>>(`/reading-progress/${bookId}`)
  return data.data
}

export async function saveReadingProgress(input: SaveProgressInput): Promise<ReadingProgress> {
  const { data } = await apiClient.put<ApiEnvelope<ReadingProgress>>('/reading-progress', input)
  return data.data
}

export async function fetchReadingHistory(page = 1): Promise<PageData<ReadingHistoryItem>> {
  const { data } = await apiClient.get<ApiEnvelope<PageData<ReadingHistoryItem>>>('/reading-history', {
    params: { page, pageSize: 20 },
  })
  return data.data
}

export async function removeReadingHistory(bookId: string): Promise<void> {
  await apiClient.delete(`/reading-history/${bookId}`)
}
