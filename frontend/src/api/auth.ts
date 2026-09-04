import { apiClient, type ApiEnvelope, type AuthSession, type AuthUser } from './client'

export interface RegisterInput {
  username: string
  password: string
  confirmPassword: string
}

export interface LoginInput {
  username: string
  password: string
}

export interface ResetPasswordInput {
  username: string
  newPassword: string
  confirmPassword: string
}

export async function register(input: RegisterInput): Promise<AuthUser> {
  const { data } = await apiClient.post<ApiEnvelope<AuthUser>>('/auth/register', input)
  return data.data
}

export async function login(input: LoginInput): Promise<AuthSession> {
  const { data } = await apiClient.post<ApiEnvelope<AuthSession>>('/auth/login', input)
  return data.data
}

export async function resetPassword(input: ResetPasswordInput): Promise<void> {
  await apiClient.post('/auth/reset-password', input)
}

export async function refreshSession(): Promise<AuthSession> {
  const { data } = await apiClient.post<ApiEnvelope<AuthSession>>('/auth/refresh')
  return data.data
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

export async function getCurrentUser(): Promise<AuthUser> {
  const { data } = await apiClient.get<ApiEnvelope<AuthUser>>('/users/me')
  return data.data
}

export async function updateProfile(input: {
  nickname: string
  avatarUrl: string
}): Promise<AuthUser> {
  const { data } = await apiClient.patch<ApiEnvelope<AuthUser>>('/users/me', input)
  return data.data
}

export async function changePassword(input: {
  currentPassword: string
  newPassword: string
}): Promise<void> {
  await apiClient.put('/users/me/password', input)
}
