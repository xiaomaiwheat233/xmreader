import { useEffect, useRef, type PropsWithChildren } from 'react'
import { refreshSession } from '../api/auth'
import { useAuthStore } from './authStore'

export default function AuthBootstrap({ children }: PropsWithChildren) {
  const initialized = useRef(false)
  const setSession = useAuthStore((state) => state.setSession)
  const clearSession = useAuthStore((state) => state.clearSession)

  useEffect(() => {
    if (initialized.current) return
    initialized.current = true
    refreshSession().then(setSession).catch(clearSession)
  }, [clearSession, setSession])

  return children
}
