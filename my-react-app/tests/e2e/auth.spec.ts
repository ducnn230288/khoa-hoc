import { expect, test } from '@playwright/test'

test.describe('auth flows', () => {
  test('keeps the username-only session flow across login, reload, and logout', async ({ page }) => {
    let authenticated = false
    let csrfCounter = 0

    await page.route('**/api/v1/auth/**', async (route) => {
      const request = route.request()
      const url = new URL(request.url())

      if (url.pathname === '/api/v1/auth/login' && request.method() === 'POST') {
        authenticated = true
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            authenticated: true,
            username: 'user01',
            roles: ['USER'],
          }),
        })
        return
      }

      if (url.pathname === '/api/v1/auth/csrf' && request.method() === 'GET') {
        if (!authenticated) {
          await route.fulfill({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              code: 'AUTH_SESSION_REQUIRED',
              message: 'Authentication is required.',
              path: '/api/v1/auth/csrf',
              timestamp: '2026-04-03T00:00:00Z',
            }),
          })
          return
        }

        csrfCounter += 1
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            csrfToken: `csrf-${csrfCounter}`,
            headerName: 'X-CSRF-TOKEN',
            parameterName: '_csrf',
          }),
        })
        return
      }

      if (url.pathname === '/api/v1/auth/logout' && request.method() === 'POST') {
        authenticated = false
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true }),
        })
        return
      }

      await route.abort()
    })

    await page.goto('/')

    await expect(page.getByLabel('Username')).toBeVisible()
    await expect(page.getByLabel('Password')).toBeVisible()
    await expect(page.getByLabel(/email/i)).toHaveCount(0)

    await page.getByLabel('Username').fill('user01')
    await page.getByLabel('Password').fill('User@123')
    await page.getByRole('button', { name: /đăng nhập/i }).click()

    await expect(page.getByText('Authenticated Session')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'user01' })).toBeVisible()
    await expect(page.locator('.role-chip').filter({ hasText: 'USER' })).toBeVisible()
    await expect(page.getByText('Present')).toBeVisible()

    await page.reload()

    await expect(page.getByText('Authenticated Session')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'user01' })).toBeVisible()

    await page.getByRole('button', { name: /logout/i }).click()

    await expect(page.getByLabel('Username')).toBeVisible()
    await expect(page.getByRole('button', { name: /đăng nhập/i })).toBeVisible()
  })

  test('shows the session-expired state first, then surfaces invalid-credential errors on relogin', async ({
    page,
  }) => {
    await page.route('**/api/v1/auth/**', async (route) => {
      const request = route.request()
      const url = new URL(request.url())

      if (url.pathname === '/api/v1/auth/csrf' && request.method() === 'GET') {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'AUTH_SESSION_EXPIRED',
            message: 'Session expired.',
            path: '/api/v1/auth/csrf',
            timestamp: '2026-04-03T00:00:00Z',
          }),
        })
        return
      }

      if (url.pathname === '/api/v1/auth/login' && request.method() === 'POST') {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            code: 'AUTH_INVALID_CREDENTIALS',
            message: 'Invalid username or password.',
            path: '/api/v1/auth/login',
            timestamp: '2026-04-03T00:00:00Z',
          }),
        })
        return
      }

      await route.abort()
    })

    await page.goto('/')

    await expect(page.getByRole('button', { name: /đăng nhập lại/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /phiên đăng nhập đã hết hạn/i })).toBeVisible()

    await page.getByRole('button', { name: /đăng nhập lại/i }).click()

    await expect(page.getByRole('status')).toContainText(/sai tên đăng nhập hoặc mật khẩu/i)
    await expect(page.getByRole('heading')).toContainText(/đăng nhập hệ thống/i)
  })
})
