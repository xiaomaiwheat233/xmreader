import { useMutation, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Form, Input, Skeleton, Typography, Upload } from 'antd'
import type { UploadFile } from 'antd'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { uploadNovel } from '../api/catalog'
import { useAuthStore } from '../auth/authStore'
import SiteHeader from '../components/SiteHeader'

const { Paragraph, Title } = Typography

export default function UploadPage() {
  const status = useAuthStore((state) => state.status)
  const user = useAuthStore((state) => state.user)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const [files, setFiles] = useState<UploadFile[]>([])
  const upload = useMutation({
    mutationFn: uploadNovel,
    onSuccess: async (book) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] })
      message.success('小说已上传，现已作为本站公开书目展示')
      navigate(`/book/${book.bookId}`)
    },
    onError: () => message.error('上传失败，请确认文件为 UTF-8 编码且不超过 20MB'),
  })
  if (status === 'checking') return <main className="page-shell"><Skeleton active /></main>
  if (!user) return <Navigate to="/login" replace state={{ from: '/upload' }} />

  return (
    <main className="page-shell">
      <SiteHeader />
      <section className="page-heading">
        <Title>上传小说到本站</Title>
        <Paragraph>上传内容会公开展示。当前支持不超过 20MB 的 UTF-8 TXT，并会自动识别“第…章”等章节标题。</Paragraph>
      </section>
      <Form className="auth-card upload-card" layout="vertical" onFinish={(values) => {
        const file = files[0]?.originFileObj
        if (!file) { message.warning('请先选择 TXT 文件'); return }
        upload.mutate({ title: values.title, author: values.author, file })
      }}>
        <Form.Item name="title" label="书名（可选）"><Input maxLength={255} placeholder="留空时使用文件名" /></Form.Item>
        <Form.Item name="author" label="作者（可选）"><Input maxLength={128} placeholder="留空时显示未知作者" /></Form.Item>
        <Form.Item label="小说文件" required>
          <Upload accept=".txt,text/plain" maxCount={1} fileList={files} beforeUpload={() => false}
            onChange={({ fileList }) => setFiles(fileList)}><Button>选择 TXT 文件</Button></Upload>
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={upload.isPending}>上传并公开</Button>
      </Form>
    </main>
  )
}
