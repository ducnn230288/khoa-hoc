import { afterEach, describe, expect, it, vi } from 'vitest'

import { AuthApiError, getCsrfToken, login, logout } from './authService'

const fetchMock = vi.fn<typeof fetch>()

describe('authService', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('sends login requests with credentials included and the username-only payload', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ authenticated: true, username: 'user01', roles: ['USER'] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const response = await login('user01', 'User@123')

    expect(response).toEqual({ authenticated: true, username: 'user01', roles: ['USER'] })
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/v1/auth/login', {
      body: JSON.stringify({ username: 'user01', password: 'User@123' }),
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      method: 'POST',
    })
  })

  it('preserves error envelope metadata when the backend rejects a request', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'AUTH_SESSION_EXPIRED',
          message: 'Session expired.',
          path: '/api/v1/auth/csrf',
          timestamp: '2026-04-03T00:00:00Z',
        }),
        {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getCsrfToken()).rejects.toEqual(
      expect.objectContaining<AuthApiError>({
        name: 'AuthApiError',
        message: 'Session expired.',
        status: 401,
        code: 'AUTH_SESSION_EXPIRED',
        path: '/api/v1/auth/csrf',
        timestamp: '2026-04-03T00:00:00Z',
      }),
    )
  })

  it('sends the negotiated CSRF header on logout requests', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await logout('csrf-token', 'X-CSRF-TOKEN')

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/v1/auth/logout', {
      body: '',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': 'csrf-token',
      },
      method: 'POST',
    })
  })
})
