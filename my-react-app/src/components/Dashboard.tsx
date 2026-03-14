import { useAuth } from "../hooks/useAuth";

/**
 * Dashboard — spec §5.11 wireframe: username, session active, CSRF loaded, Logout button.
 */
export default function Dashboard() {
  const { user, csrfToken, logout } = useAuth();

  async function handleLogout() {
    await logout();
  }

  return (
    <div className="dashboard">
      <header>
        <span>My App</span>
        <span>
          {user?.username}&nbsp;&nbsp;
          <button onClick={handleLogout}>Logout</button>
        </span>
      </header>
      <main>
        <p>Welcome, {user?.username}</p>
        <p>Session: Active | CSRF token: {csrfToken ? "Loaded" : "Not loaded"}</p>
        <div>
          <button>Create</button>
          <button>Update</button>
          <button>Delete</button>
        </div>
      </main>
    </div>
  );
}
