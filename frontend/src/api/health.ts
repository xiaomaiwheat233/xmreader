import { apiClient, type ApiEnvelope } from './client'

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
  const response = await apiClient.get<ApiEnvelope<HealthStatus>>('/health')
  return response.data.data
}
