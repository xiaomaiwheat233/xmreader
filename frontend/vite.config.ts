import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': apiTarget,
        '/actuator': apiTarget,
      },
    },
    test: {
      environment: 'jsdom',
      fileParallelism: false,
      maxWorkers: 1,
      pool: 'threads',
      setupFiles: './src/test/setup.ts',
    },
  }
})
