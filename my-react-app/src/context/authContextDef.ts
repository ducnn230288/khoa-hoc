import { createContext } from 'react';

export interface AuthUser {
  username: string;
  roles: string[];
}

export interface AuthContextType {
  user: AuthUser | null;
  csrfToken: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | null>(null);
