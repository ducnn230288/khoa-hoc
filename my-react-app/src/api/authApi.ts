export interface LoginResponse {
  authenticated: boolean;
  username: string;
  roles: string[];
}

export interface CsrfResponse {
  csrfToken: string;
  headerName: string;
  parameterName: string;
}

export interface LogoutResponse {
  success: boolean;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
}

const BASE = '/api/v1/auth';

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${BASE}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const problem: ProblemDetail = await res.json();
    throw new Error(problem.detail || 'Login failed');
  }
  return res.json();
}

export async function fetchCsrf(): Promise<CsrfResponse> {
  const res = await fetch(`${BASE}/csrf`, {
    method: 'GET',
    credentials: 'include',
  });
  if (!res.ok) {
    const problem: ProblemDetail = await res.json();
    throw new Error(problem.detail || 'Failed to fetch CSRF token');
  }
  return res.json();
}

export async function logout(csrfToken: string): Promise<LogoutResponse> {
  const res = await fetch(`${BASE}/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': csrfToken,
    },
    credentials: 'include',
  });
  if (!res.ok) {
    const problem: ProblemDetail = await res.json();
    throw new Error(problem.detail || 'Logout failed');
  }
  return res.json();
}
