import './App.css'
import { useAuth } from './hooks/useAuth'
import { LoginPage } from './pages/LoginPage'

function App() {
  const auth = useAuth()

  if (auth.status === 'checking') {
    return (
      <main className="app-shell">
        <section className="panel panel-progress">
          <p className="eyebrow">Auth Rehydrate</p>
          <h1>Đang xác thực...</h1>
          <p className="panel-copy">
            Ứng dụng đang kiểm tra session cookie hiện tại và xin lại CSRF token từ backend.
          </p>
          <ol className="progress-list">
            <li>Kiểm tra session cookie hiện có</li>
            <li>Xác nhận backend còn chấp nhận phiên</li>
            <li>Lấy lại CSRF token qua `GET /api/v1/auth/csrf`</li>
          </ol>
        </section>
      </main>
    )
  }

  if (!auth.authenticated) {
    return (
      <main className="app-shell">
        <LoginPage
          message={auth.message}
          onSubmit={auth.login}
          pending={auth.pending}
          sessionExpired={auth.status === 'session-expired'}
        />
      </main>
    )
  }

  return (
    <main className="app-shell">
      <section className="panel panel-hero">
        <div>
          <p className="eyebrow">Authenticated Session</p>
          <h1>{auth.username ?? 'Phiên đã được khôi phục'}</h1>
          <p className="panel-copy">
            Frontend hiện đang chạy đúng flow của ticket: dùng session cookie, xin CSRF qua API riêng,
            và không phụ thuộc vào `/auth/me` hay `/auth/status`.
          </p>
        </div>

        <div className="hero-grid">
          <article className="metric-card">
            <span className="metric-label">CSRF Header</span>
            <strong>{auth.headerName}</strong>
          </article>
          <article className="metric-card">
            <span className="metric-label">Token State</span>
            <strong>{auth.csrfToken ? 'Present' : 'Missing'}</strong>
          </article>
          <article className="metric-card">
            <span className="metric-label">Parameter Name</span>
            <strong>{auth.parameterName}</strong>
          </article>
        </div>
      </section>

      <section className="panel panel-details">
        <div className="section-head">
          <div>
            <p className="eyebrow">Current Access</p>
            <h2>Username-only contract</h2>
          </div>

          <div className="action-row">
            <button className="secondary-button" onClick={auth.rehydrate} type="button">
              Lấy lại CSRF
            </button>
            <button className="primary-button" onClick={auth.logout} type="button">
              {auth.flow === 'logging-out' ? 'Đang logout...' : 'Logout'}
            </button>
          </div>
        </div>

        <div className="role-strip">
          {(auth.roles.length > 0 ? auth.roles : ['No roles assigned']).map((role) => (
            <span className="role-chip" key={role}>
              {role}
            </span>
          ))}
        </div>

        <div className={`message-box ${auth.message ? 'visible' : ''}`} role="status">
          {auth.message ?? 'Phiên hiện tại sẵn sàng cho protected flow dùng session + CSRF.'}
        </div>

        <div className="evidence-grid">
          <article>
            <h3>Login</h3>
            <p>`POST /api/v1/auth/login` trả `authenticated`, `username`, `roles`.</p>
          </article>
          <article>
            <h3>Rehydrate</h3>
            <p>`GET /api/v1/auth/csrf` được gọi lại khi reload để khôi phục protected flow.</p>
          </article>
          <article>
            <h3>Logout</h3>
            <p>`POST /api/v1/auth/logout` luôn gửi `X-CSRF-TOKEN` cùng session cookie hiện tại.</p>
          </article>
        </div>
      </section>
    </main>
  )
}

export default App
