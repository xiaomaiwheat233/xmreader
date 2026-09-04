import { Spin } from 'antd'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import AuthBootstrap from './auth/AuthBootstrap'
import './App.css'

const HomePage = lazy(() => import('./pages/HomePage'))
const SearchPage = lazy(() => import('./pages/SearchPage'))
const BookDetailPage = lazy(() => import('./pages/BookDetailPage'))
const ChapterListPage = lazy(() => import('./pages/ChapterListPage'))
const ReaderPage = lazy(() => import('./pages/ReaderPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const LibraryPage = lazy(() => import('./pages/LibraryPage'))
const ProfilePage = lazy(() => import('./pages/ProfilePage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'))

export default function App() {
  return (
    <AuthBootstrap>
      <Suspense fallback={<main className="app-shell loading-page"><Spin size="large" /></main>}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/book/:id" element={<BookDetailPage />} />
          <Route path="/book/:id/chapters" element={<ChapterListPage />} />
          <Route path="/book/:bookId/read/:chapterId" element={<ReaderPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/library" element={<LibraryPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </AuthBootstrap>
  )
}
