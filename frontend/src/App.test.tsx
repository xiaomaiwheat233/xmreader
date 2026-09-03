import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as healthApi from './api/health'

describe('App', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows that the local services are available after a successful health check', async () => {
    vi.spyOn(healthApi, 'fetchHealth').mockResolvedValue({
      status: 'UP',
      components: { application: 'UP', database: 'UP' },
      version: '0.1.0',
      timestamp: '2026-09-03T00:00:00Z',
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

    expect(await screen.findByText('本地服务可用')).toBeInTheDocument()
    expect(screen.getByText('Backend 0.1.0')).toBeInTheDocument()
  })
})
