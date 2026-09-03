import axios from 'axios'
import { useAuthStore } from '../auth/authStore'

interface RetryableRequestConfig {
  _retry?: boolean
  headers?: Record<string, string>
  url?: string
}

export const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 8_000,
  headers: {
    Accept: 'application/json',
  },
})

const refreshClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 8_000,
})

let refreshPromise: Promise<string> | null = null

apiClient.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as RetryableRequestConfig | undefined
    const isAuthenticationRequest = config?.url?.startsWith('/auth/')
    if (error.response?.status !== 401 || !config || config._retry || isAuthenticationRequest) {
      return Promise.reject(error)
    }

    config._retry = true
    refreshPromise ??= refreshClient
      .post<ApiEnvelope<AuthSession>>('/auth/refresh')
      .then(({ data }) => {
        useAuthStore.getState().setSession(data.data)
        return data.data.accessToken
      })
      .catch((refreshError) => {
        useAuthStore.getState().clearSession()
        throw refreshError
      })
      .finally(() => {
        refreshPromise = null
      })

    const accessToken = await refreshPromise
    config.headers = { ...config.headers, Authorization: `Bearer ${accessToken}` }
    return apiClient.request(config)
  },
)

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  error: { type: string; fields: Record<string, string> } | null
  requestId: string
}

export interface AuthUser {
  id: string
  username: string
  nickname: string
  avatarUrl: string | null
  role: 'USER' | 'ADMIN'
}

export interface AuthSession {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthUser
}
