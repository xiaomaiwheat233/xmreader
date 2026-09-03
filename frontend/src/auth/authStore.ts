import { create } from 'zustand'
import type { AuthSession, AuthUser } from '../api/client'

type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

interface AuthState {
  accessToken: string | null
  user: AuthUser | null
  status: AuthStatus
  setSession: (session: AuthSession) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  status: 'checking',
  setSession: (session) =>
    set({ accessToken: session.accessToken, user: session.user, status: 'authenticated' }),
  clearSession: () => set({ accessToken: null, user: null, status: 'anonymous' }),
}))
