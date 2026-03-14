/**
 * AR-F4: Auth state (session, CSRF token) lives in a single React context — not duplicated.
 */
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import * as authApi from "../api/authApi";

interface AuthUser {
  username: string;
  roles: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  csrfToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [csrfToken, setCsrfToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // On app init: try to fetch CSRF token — if successful, session is still valid (R-4 from impl-plan)
  useEffect(() => {
    authApi
      .fetchCsrf()
      .then((csrf) => setCsrfToken(csrf.csrfToken))
      .catch(() => {
        // No valid session — stay unauthenticated
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

  async function login(username: string, password: string) {
    const loginResp = await authApi.login({ username, password });
    const csrfResp = await authApi.fetchCsrf();
    setUser({ username: loginResp.username, roles: loginResp.roles });
    setCsrfToken(csrfResp.csrfToken);
  }

  async function logout() {
    if (csrfToken) {
      await authApi.logout(csrfToken);
    }
    setUser(null);
    setCsrfToken(null);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        csrfToken,
        isAuthenticated: user !== null,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuthContext must be used inside AuthProvider");
  return ctx;
}
