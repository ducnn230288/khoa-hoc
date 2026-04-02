import { useEffect, useState, useTransition } from 'react'

import {
  AuthApiError,
  getCsrfToken,
  login as loginRequest,
  logout as logoutRequest,
} from '../services/authService'

type AuthStatus = 'checking' | 'unauthenticated' | 'authenticated' | 'session-expired'
type AuthFlow = 'idle' | 'logging-in' | 'rehydrating' | 'logging-out'

type AuthSnapshot = {
  username: string
  roles: string[]
}

type AuthState = {
  status: AuthStatus
  flow: AuthFlow
  username: string | null
  roles: string[]
  csrfToken: string | null
  headerName: string
  parameterName: string
  message: string | null
}

const SNAPSHOT_KEY = 'auth.snapshot'

const EMPTY_STATE: AuthState = {
  status: 'checking',
  flow: 'rehydrating',
  username: null,
  roles: [],
  csrfToken: null,
  headerName: 'X-CSRF-TOKEN',
  parameterName: '_csrf',
  message: null,
}

function readSnapshot(): AuthSnapshot | null {
  const rawValue = window.sessionStorage.getItem(SNAPSHOT_KEY)
  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue) as AuthSnapshot
  } catch {
    window.sessionStorage.removeItem(SNAPSHOT_KEY)
    return null
  }
}

function writeSnapshot(snapshot: AuthSnapshot) {
  window.sessionStorage.setItem(SNAPSHOT_KEY, JSON.stringify(snapshot))
}

function clearSnapshot() {
  window.sessionStorage.removeItem(SNAPSHOT_KEY)
}

function mapMessage(code?: string) {
  switch (code) {
    case 'AUTH_INVALID_CREDENTIALS':
      return 'Sai tên đăng nhập hoặc mật khẩu.'
    case 'AUTH_USER_DISABLED':
      return 'Tài khoản này hiện đang bị vô hiệu hóa.'
    case 'AUTH_SESSION_REQUIRED':
      return 'Bạn cần đăng nhập để bắt đầu phiên làm việc.'
    case 'AUTH_SESSION_EXPIRED':
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.'
    case 'AUTH_CSRF_INVALID':
      return 'CSRF token không còn hợp lệ. Hãy làm mới phiên xác thực.'
    default:
      return 'Không thể hoàn tất bước xác thực. Vui lòng thử lại.'
  }
}

function authenticatedState(
  snapshot: AuthSnapshot | null,
  csrfToken: string,
  headerName: string,
  parameterName: string,
): AuthState {
  return {
    status: 'authenticated',
    flow: 'idle',
    username: snapshot?.username ?? null,
    roles: snapshot?.roles ?? [],
    csrfToken,
    headerName,
    parameterName,
    message: null,
  }
}

function unauthenticatedState(code?: string): AuthState {
  return {
    status: code === 'AUTH_SESSION_EXPIRED' ? 'session-expired' : 'unauthenticated',
    flow: 'idle',
    username: null,
    roles: [],
    csrfToken: null,
    headerName: 'X-CSRF-TOKEN',
    parameterName: '_csrf',
    message: mapMessage(code),
  }
}

export function useAuth() {
  const [state, setState] = useState<AuthState>(EMPTY_STATE)
  const [isPending, startTransition] = useTransition()

  async function rehydrate() {
    startTransition(() => {
      setState((current) => ({
        ...current,
        status: 'checking',
        flow: 'rehydrating',
        message: null,
      }))
    })

    try {
      const csrf = await getCsrfToken()
      startTransition(() => {
        setState(authenticatedState(readSnapshot(), csrf.csrfToken, csrf.headerName, csrf.parameterName))
      })
    } catch (error) {
      clearSnapshot()
      const code = error instanceof AuthApiError ? error.code : undefined
      startTransition(() => {
        setState(unauthenticatedState(code))
      })
    }
  }

  useEffect(() => {
    void rehydrate()
  }, [])

  async function login(username: string, password: string) {
    startTransition(() => {
      setState((current) => ({
        ...current,
        flow: 'logging-in',
        message: null,
        status: 'unauthenticated',
      }))
    })

    try {
      const session = await loginRequest(username, password)
      const csrf = await getCsrfToken()
      writeSnapshot({
        username: session.username,
        roles: session.roles,
      })
      startTransition(() => {
        setState(
          authenticatedState(
            { username: session.username, roles: session.roles },
            csrf.csrfToken,
            csrf.headerName,
            csrf.parameterName,
          ),
        )
      })
    } catch (error) {
      clearSnapshot()
      const code = error instanceof AuthApiError ? error.code : undefined
      startTransition(() => {
        setState(unauthenticatedState(code))
      })
    }
  }

  async function logout() {
    if (!state.csrfToken) {
      startTransition(() => {
        setState((current) => ({
          ...current,
          message: mapMessage('AUTH_CSRF_INVALID'),
        }))
      })
      return
    }

    startTransition(() => {
      setState((current) => ({
        ...current,
        flow: 'logging-out',
        message: null,
      }))
    })

    try {
      await logoutRequest(state.csrfToken, state.headerName)
      clearSnapshot()
      startTransition(() => {
        setState(unauthenticatedState('AUTH_SESSION_REQUIRED'))
      })
    } catch (error) {
      if (error instanceof AuthApiError && error.code === 'AUTH_CSRF_INVALID') {
        startTransition(() => {
          setState((current) => ({
            ...current,
            flow: 'idle',
            message: mapMessage(error.code),
          }))
        })
        return
      }

      clearSnapshot()
      const code = error instanceof AuthApiError ? error.code : undefined
      startTransition(() => {
        setState(unauthenticatedState(code))
      })
    }
  }

  return {
    ...state,
    pending: isPending || state.flow !== 'idle',
    authenticated: state.status === 'authenticated',
    login,
    logout,
    rehydrate: () => {
      void rehydrate()
    },
  }
}
