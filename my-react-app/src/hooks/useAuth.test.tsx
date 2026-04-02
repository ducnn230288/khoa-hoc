import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { useAuth } from './useAuth'
import {
  AuthApiError,
  getCsrfToken,
  login as loginRequest,
  logout as logoutRequest,
} from '../services/authService'

vi.mock('../services/authService', async () => {
  const actual = await vi.importActual<typeof import('../services/authService')>('../services/authService')
  return {
    ...actual,
    getCsrfToken: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  }
})

function HookHarness() {
  const auth = useAuth()

  return (
    <section>
      <div data-testid="status">{auth.status}</div>
      <div data-testid="username">{auth.username ?? 'anonymous'}</div>
      <div data-testid="roles">{auth.roles.join(',') || 'none'}</div>
      <div data-testid="token">{auth.csrfToken ?? 'missing'}</div>
      <div data-testid="header">{auth.headerName}</div>
      <div data-testid="message">{auth.message ?? 'none'}</div>
      <button onClick={() => void auth.login('user01', 'User@123')} type="button">
        login
      </button>
      <button onClick={() => void auth.logout()} type="button">
        logout
      </button>
      <button onClick={auth.rehydrate} type="button">
        rehydrate
      </button>
    </section>
  )
}

const mockedGetCsrfToken = vi.mocked(getCsrfToken)
const mockedLoginRequest = vi.mocked(loginRequest)
const mockedLogoutRequest = vi.mocked(logoutRequest)

describe('useAuth', () => {
  afterEach(() => {
    window.sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('rehydrates the session snapshot after a successful csrf refresh', async () => {
    window.sessionStorage.setItem(
      'auth.snapshot',
      JSON.stringify({ username: 'user01', roles: ['USER', 'AUDITOR'] }),
    )
    mockedGetCsrfToken.mockResolvedValue({
      csrfToken: 'csrf-1',
      headerName: 'X-CSRF-TOKEN',
      parameterName: '_csrf',
    })

    render(<HookHarness />)

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('username')).toHaveTextContent('user01')
    expect(screen.getByTestId('roles')).toHaveTextContent('USER,AUDITOR')
    expect(screen.getByTestId('token')).toHaveTextContent('csrf-1')
    expect(screen.getByTestId('header')).toHaveTextContent('X-CSRF-TOKEN')
  })

  it('maps an expired session to the dedicated unauthenticated state and message', async () => {
    mockedGetCsrfToken.mockRejectedValue(
      new AuthApiError(401, {
        code: 'AUTH_SESSION_EXPIRED',
        message: 'Session expired.',
        path: '/api/v1/auth/csrf',
        timestamp: '2026-04-03T00:00:00Z',
      }),
    )

    render(<HookHarness />)

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('session-expired'))
    expect(screen.getByTestId('message')).toHaveTextContent(
      'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.',
    )
    expect(screen.getByTestId('token')).toHaveTextContent('missing')
  })

  it('stores the session snapshot after login and rehydrates the authenticated state', async () => {
    const user = userEvent.setup()
    mockedGetCsrfToken
      .mockRejectedValueOnce(
        new AuthApiError(401, {
          code: 'AUTH_SESSION_REQUIRED',
          message: 'Authentication is required.',
          path: '/api/v1/auth/csrf',
          timestamp: '2026-04-03T00:00:00Z',
        }),
      )
      .mockResolvedValueOnce({
        csrfToken: 'csrf-after-login',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      })
    mockedLoginRequest.mockResolvedValue({
      authenticated: true,
      username: 'user01',
      roles: ['USER'],
    })

    render(<HookHarness />)
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))

    await user.click(screen.getByRole('button', { name: 'login' }))

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('username')).toHaveTextContent('user01')
    expect(window.sessionStorage.getItem('auth.snapshot')).toBe(
      JSON.stringify({ username: 'user01', roles: ['USER'] }),
    )
  })

  it('keeps the authenticated state when logout fails with an invalid csrf token', async () => {
    const user = userEvent.setup()
    window.sessionStorage.setItem('auth.snapshot', JSON.stringify({ username: 'user01', roles: ['USER'] }))
    mockedGetCsrfToken.mockResolvedValue({
      csrfToken: 'csrf-1',
      headerName: 'X-CSRF-TOKEN',
      parameterName: '_csrf',
    })
    mockedLogoutRequest.mockRejectedValue(
      new AuthApiError(403, {
        code: 'AUTH_CSRF_INVALID',
        message: 'CSRF token is missing or invalid.',
        path: '/api/v1/auth/logout',
        timestamp: '2026-04-03T00:00:00Z',
      }),
    )

    render(<HookHarness />)
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))

    await user.click(screen.getByRole('button', { name: 'logout' }))

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(mockedLogoutRequest).toHaveBeenCalledWith('csrf-1', 'X-CSRF-TOKEN')
    expect(screen.getByTestId('message')).toHaveTextContent(
      'CSRF token không còn hợp lệ. Hãy làm mới phiên xác thực.',
    )
  })
})
