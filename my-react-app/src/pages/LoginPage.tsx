import { useState } from 'react'
import type { FormEvent } from 'react'

type LoginPageProps = {
  pending: boolean
  sessionExpired: boolean
  message: string | null
  onSubmit: (username: string, password: string) => Promise<void>
}

export function LoginPage({
  pending,
  sessionExpired,
  message,
  onSubmit,
}: LoginPageProps) {
  const [username, setUsername] = useState('user01')
  const [password, setPassword] = useState('User@123')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await onSubmit(username, password)
  }

  return (
    <section className="panel panel-login">
      <p className="eyebrow">Session Cookie + CSRF</p>
      <h1>{sessionExpired ? 'Phiên đăng nhập đã hết hạn' : 'Đăng nhập hệ thống'}</h1>
      <p className="panel-copy">
        {sessionExpired
          ? 'Vì lý do bảo mật, phiên làm việc trước đó không còn hiệu lực. Hãy đăng nhập lại để tiếp tục.'
          : 'Chỉ dùng Username và Password. Sau khi login thành công, ứng dụng sẽ tự lấy CSRF token qua API riêng.'}
      </p>

      <form className="login-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Username</span>
          <input
            autoComplete="username"
            name="username"
            onChange={(event) => setUsername(event.target.value)}
            placeholder="user01"
            value={username}
          />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            autoComplete="current-password"
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder="******"
            type="password"
            value={password}
          />
        </label>

        <button className="primary-button" disabled={pending} type="submit">
          {pending ? 'Đang xác thực...' : sessionExpired ? 'Đăng nhập lại' : 'Đăng nhập'}
        </button>
      </form>

      <div className={`message-box ${message ? 'visible' : ''}`} role="status">
        {message ?? 'Khu vực hiển thị lỗi xác thực sẽ xuất hiện tại đây.'}
      </div>
    </section>
  )
}
