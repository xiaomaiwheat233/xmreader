import { apiClient } from './client'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId: string
}

export interface HealthStatus {
  status: 'UP'
  components: {
    application: 'UP'
    database: 'UP'
  }
  version: string
  timestamp: string
}

export async function fetchHealth(): Promise<HealthStatus> {
  const response = await apiClient.get<ApiResponse<HealthStatus>>('/health')
  return response.data.data
}
