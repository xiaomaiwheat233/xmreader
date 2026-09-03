import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Card, Segmented, Skeleton, Slider, Space, Typography } from 'antd'
import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchChapter } from '../api/catalog'
import { fetchReadingProgress, saveReadingProgress } from '../api/reading'
import { useAuthStore } from '../auth/authStore'

const { Paragraph, Text, Title } = Typography
const SETTINGS_KEY = 'xmreader.reader.settings.v1'

type ReaderTheme = 'day' | 'night' | 'wheat'

interface ReaderSettings {
  fontSize: number
  lineHeight: number
  contentWidth: number
  theme: ReaderTheme
}

const defaultSettings: ReaderSettings = {
  fontSize: 20,
  lineHeight: 1.9,
  contentWidth: 760,
  theme: 'wheat',
}

export default function ReaderPage() {
  const { bookId = '', chapterId = '' } = useParams()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const [settings, setSettings] = useState<ReaderSettings>(loadSettings)
  const restoredChapter = useRef<string | null>(null)
  const lastSavedAt = useRef(0)
  const chapter = useQuery({
    queryKey: ['catalog', 'chapter', chapterId],
    queryFn: () => fetchChapter(chapterId),
    enabled: Boolean(chapterId),
  })
  const savedProgress = useQuery({
    queryKey: ['reading', 'progress', bookId, user?.id],
    queryFn: () => fetchReadingProgress(bookId),
    enabled: Boolean(bookId && user),
  })

  const persistProgress = useCallback((forceStart = false) => {
    if (!user || !chapter.data) return
    const availableScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
    const progressPercent = forceStart || availableScroll === 0
      ? 0
      : Math.min(100, Math.max(0, (window.scrollY / availableScroll) * 100))
    const position = Math.round(chapter.data.content.length * progressPercent / 100)
    lastSavedAt.current = Date.now()
    void saveReadingProgress({
      bookId,
      chapterId: chapter.data.id,
      position,
      progressPercent: Number(progressPercent.toFixed(2)),
    }).then(() => {
      void queryClient.invalidateQueries({ queryKey: ['reading'] })
    }).catch(() => undefined)
  }, [bookId, chapter.data, queryClient, user])

  useEffect(() => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
  }, [settings])

  useEffect(() => {
    if (!chapter.data) return
    document.title = `${chapter.data.title} - ${chapter.data.book.title} - 小麦中文网`
    return () => { document.title = '小麦中文网' }
  }, [chapter.data])

  useEffect(() => {
    if (!user || !chapter.data || savedProgress.isPending || restoredChapter.current === chapter.data.id) return
    restoredChapter.current = chapter.data.id
    if (savedProgress.data?.chapterId === chapter.data.id) {
      const percent = Number(savedProgress.data.progressPercent)
      requestAnimationFrame(() => {
        const availableScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
        window.scrollTo({ top: availableScroll * percent / 100 })
      })
    } else {
      persistProgress(true)
    }
  }, [chapter.data, persistProgress, savedProgress.data, savedProgress.isPending, user])

  useEffect(() => {
    if (!user || !chapter.data) return
    let timer: number | undefined
    const scheduleSave = () => {
      if (timer !== undefined) return
      const delay = Math.max(0, 10_000 - (Date.now() - lastSavedAt.current))
      timer = window.setTimeout(() => {
        persistProgress()
        timer = undefined
      }, delay)
    }
    const saveWhenHidden = () => {
      if (document.visibilityState === 'hidden') persistProgress()
    }
    window.addEventListener('scroll', scheduleSave, { passive: true })
    document.addEventListener('visibilitychange', saveWhenHidden)
    return () => {
      window.removeEventListener('scroll', scheduleSave)
      document.removeEventListener('visibilitychange', saveWhenHidden)
      if (timer !== undefined) window.clearTimeout(timer)
      persistProgress()
    }
  }, [chapter.data, persistProgress, user])

  const readerStyle = {
    '--reader-font-size': `${settings.fontSize}px`,
    '--reader-line-height': settings.lineHeight,
    '--reader-content-width': `${settings.contentWidth}px`,
  } as CSSProperties

  return (
    <main className={`reader-page reader-theme-${settings.theme}`} style={readerStyle}>
      <header className="reader-header">
        <Link className="site-brand" to="/">小麦中文网</Link>
        <Space wrap>
          <Link to={`/book/${bookId}`}>书籍详情</Link>
          <Link to={`/book/${bookId}/chapters`}>目录</Link>
        </Space>
      </header>

      <Card className="reader-settings" variant="borderless">
        <div className="setting-item">
          <Text>字号</Text>
          <Slider min={16} max={30} value={settings.fontSize} onChange={(fontSize) => setSettings({ ...settings, fontSize })} />
        </div>
        <div className="setting-item">
          <Text>行距</Text>
          <Slider min={1.5} max={2.5} step={0.1} value={settings.lineHeight} onChange={(lineHeight) => setSettings({ ...settings, lineHeight })} />
        </div>
        <div className="setting-item">
          <Text>宽度</Text>
          <Slider min={560} max={1000} step={40} value={settings.contentWidth} onChange={(contentWidth) => setSettings({ ...settings, contentWidth })} />
        </div>
        <Segmented
          value={settings.theme}
          options={[
            { label: '日间', value: 'day' },
            { label: '护眼', value: 'wheat' },
            { label: '夜间', value: 'night' },
          ]}
          onChange={(theme) => setSettings({ ...settings, theme: theme as ReaderTheme })}
        />
      </Card>

      {chapter.isPending ? <div className="reader-content"><Skeleton active paragraph={{ rows: 14 }} /></div> : null}
      {chapter.isError ? <div className="reader-content"><Alert type="error" showIcon message="章节不存在或暂时无法读取" /></div> : null}
      {chapter.data ? (
        <article className="reader-content">
          <header className="chapter-heading">
            <Text>{chapter.data.book.title}</Text>
            <Title>{chapter.data.title}</Title>
            <Text type="secondary">第 {chapter.data.chapterIndex} 章 · {chapter.data.wordCount} 字</Text>
          </header>
          <div className="chapter-body">
            {chapter.data.content.split(/\n\s*\n/).filter(Boolean).map((paragraph, index) => (
              <Paragraph key={`${chapter.data.id}-${index}`}>{paragraph}</Paragraph>
            ))}
          </div>
          <nav className="reader-navigation" aria-label="章节导航">
            {chapter.data.previousChapterId ? (
              <Button size="large" onClick={() => persistProgress()}><Link to={`/book/${bookId}/read/${chapter.data.previousChapterId}`}>上一章</Link></Button>
            ) : <Button size="large" disabled>没有上一章</Button>}
            <Button size="large"><Link to={`/book/${bookId}/chapters`}>目录</Link></Button>
            {chapter.data.nextChapterId ? (
              <Button type="primary" size="large" onClick={() => persistProgress()}><Link to={`/book/${bookId}/read/${chapter.data.nextChapterId}`}>下一章</Link></Button>
            ) : <Button type="primary" size="large" disabled>已是最后一章</Button>}
          </nav>
        </article>
      ) : null}
    </main>
  )
}

function loadSettings(): ReaderSettings {
  try {
    const value = JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? '') as Partial<ReaderSettings>
    return {
      fontSize: clamp(value.fontSize, 16, 30, defaultSettings.fontSize),
      lineHeight: clamp(value.lineHeight, 1.5, 2.5, defaultSettings.lineHeight),
      contentWidth: clamp(value.contentWidth, 560, 1000, defaultSettings.contentWidth),
      theme: value.theme === 'day' || value.theme === 'night' || value.theme === 'wheat' ? value.theme : defaultSettings.theme,
    }
  } catch {
    return defaultSettings
  }
}

function clamp(value: number | undefined, min: number, max: number, fallback: number) {
  return typeof value === 'number' && Number.isFinite(value) ? Math.min(max, Math.max(min, value)) : fallback
}
