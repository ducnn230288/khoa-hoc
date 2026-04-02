import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('renders the username-only contract and does not show an email field', () => {
    render(
      <LoginPage
        message={null}
        onSubmit={vi.fn().mockResolvedValue(undefined)}
        pending={false}
        sessionExpired={false}
      />,
    )

    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Khu vực hiển thị lỗi xác thực sẽ xuất hiện tại đây.',
    )
  })

  it('submits the current username and password values', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)

    render(
      <LoginPage
        message={null}
        onSubmit={onSubmit}
        pending={false}
        sessionExpired={false}
      />,
    )

    await user.clear(screen.getByLabelText('Username'))
    await user.type(screen.getByLabelText('Username'), 'admin')
    await user.clear(screen.getByLabelText('Password'))
    await user.type(screen.getByLabelText('Password'), 'Admin@123')
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(onSubmit).toHaveBeenCalledWith('admin', 'Admin@123')
  })

  it('switches to the expired-session copy and disables submit while pending', () => {
    render(
      <LoginPage
        message={'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.'}
        onSubmit={vi.fn().mockResolvedValue(undefined)}
        pending={true}
        sessionExpired={true}
      />,
    )

    expect(screen.getByRole('heading')).toHaveTextContent('Phiên đăng nhập đã hết hạn')
    expect(screen.getByRole('button', { name: 'Đang xác thực...' })).toBeDisabled()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.',
    )
  })
})
