export type LoginResponse = {
  authenticated: boolean
  username: string
  roles: string[]
}

export type CsrfResponse = {
  csrfToken: string
  headerName: string
  parameterName: string
}

type ErrorEnvelope = {
  code?: string
  message?: string
  path?: string
  timestamp?: string
}

export class AuthApiError extends Error {
  status: number
  code?: string
  path?: string
  timestamp?: string

  constructor(status: number, payload: ErrorEnvelope) {
    super(payload.message ?? 'Authentication request failed.')
    this.name = 'AuthApiError'
    this.status = status
    this.code = payload.code
    this.path = payload.path
    this.timestamp = payload.timestamp
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function requestJson<T>(
  path: string,
  init: RequestInit,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers,
    },
  })

  const text = await response.text()
  const payload = text ? (JSON.parse(text) as unknown) : null

  if (!response.ok) {
    throw new AuthApiError(response.status, (payload as ErrorEnvelope | null) ?? {})
  }

  return payload as T
}

export function login(username: string, password: string) {
  return requestJson<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCsrfToken() {
  return requestJson<CsrfResponse>('/api/v1/auth/csrf', {
    method: 'GET',
    headers: {},
  })
}

export function logout(csrfToken: string, headerName: string) {
  return requestJson<{ success: boolean }>('/api/v1/auth/logout', {
    method: 'POST',
    body: '',
    headers: {
      [headerName]: csrfToken,
    },
  })
}
