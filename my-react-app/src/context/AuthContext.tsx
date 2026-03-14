export type { AuthUser, AuthContextType } from './authContextDef';
export { AuthContext } from './authContextDef';

import { useState, useEffect, useCallback, type ReactNode } from 'react';
import { AuthContext } from './authContextDef';
import type { AuthUser } from './authContextDef';
import * as authApi from '../api/authApi';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [csrfToken, setCsrfToken] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    authApi.fetchCsrf().then(
      (csrf) => {
        if (!ignore) setCsrfToken(csrf.csrfToken);
      },
      () => {
        if (!ignore) {
          setUser(null);
          setCsrfToken(null);
        }
      },
    );
    return () => { ignore = true; };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const response = await authApi.login(username, password);
    setUser({ username: response.username, roles: response.roles });

    const csrf = await authApi.fetchCsrf();
    setCsrfToken(csrf.csrfToken);
  }, []);

  const logout = useCallback(async () => {
    if (csrfToken) {
      await authApi.logout(csrfToken);
    }
    setUser(null);
    setCsrfToken(null);
  }, [csrfToken]);

  return (
    <AuthContext.Provider value={{ user, csrfToken, isAuthenticated: user !== null, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
