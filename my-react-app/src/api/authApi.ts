/**
 * AR-F3: This is the ONLY module that sets `credentials: 'include'` and attaches X-CSRF-TOKEN.
 * All API calls must originate here — not from Pages or Components directly.
 */

export interface LoginRequest {
  username: string;
  password: string;
}

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

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw Object.assign(new Error(body.detail ?? res.statusText), { status: res.status, body });
  }
  return res.json() as Promise<T>;
}

export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const res = await fetch("/api/v1/auth/login", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });
  return handleResponse<LoginResponse>(res);
}

export async function fetchCsrf(): Promise<CsrfResponse> {
  const res = await fetch("/api/v1/auth/csrf", {
    method: "GET",
    credentials: "include",
  });
  return handleResponse<CsrfResponse>(res);
}

export async function logout(csrfToken: string): Promise<LogoutResponse> {
  const res = await fetch("/api/v1/auth/logout", {
    method: "POST",
    credentials: "include",
    headers: { "X-CSRF-TOKEN": csrfToken },
  });
  return handleResponse<LogoutResponse>(res);
}
