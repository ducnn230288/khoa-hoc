import { useAuth } from '../hooks/useAuth';

export function Dashboard() {
  const { user, csrfToken, logout } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // Session already expired — just clear state
    }
  };

  return (
    <div>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>My App</span>
        <div>
          <span>{user?.username}</span>
          <button onClick={handleLogout} style={{ marginLeft: '1rem' }}>Logout</button>
        </div>
      </header>
      <main>
        <p>Welcome, {user?.username}</p>
        <p>Session: Active | CSRF token: {csrfToken ? 'Loaded' : 'Not loaded'}</p>
      </main>
    </div>
  );
}
