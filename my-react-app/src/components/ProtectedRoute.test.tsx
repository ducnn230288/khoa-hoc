/**
 * TST-6: ProtectedRoute — redirects unauthenticated users to /login (AR-F5).
 * Also covers AC-36: authenticated user on /login → redirect to /.
 */
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useNavigate } from "react-router-dom";
import { beforeEach, describe, it, vi } from "vitest";
import * as authApi from "../api/authApi";
import { AuthProvider } from "../context/AuthContext";
import LoginPage from "../pages/LoginPage";
import ProtectedRoute from "./ProtectedRoute";

vi.mock("../api/authApi");

const mockFetchCsrf = vi.mocked(authApi.fetchCsrf);
const mockLogin = vi.mocked(authApi.login);

function renderProtectedRoute(initialPath = "/") {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <div>Protected Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

// Dashboard stub that exposes a button to navigate to /login
function DashboardWithLoginLink() {
  const navigate = useNavigate();
  return <button onClick={() => navigate("/login")}>back-to-login</button>;
}

describe("ProtectedRoute", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  // AR-F5: unauthenticated user on / → redirect to /login
  it("redirects to /login when not authenticated", async () => {
    mockFetchCsrf.mockRejectedValue(new Error("No session"));
    renderProtectedRoute("/");
    await screen.findByText("Login Page");
  });

  // AC-36: user who is already authenticated visiting /login must be redirected to /
  it("redirects authenticated user from /login to / (AC-36)", async () => {
    mockLogin.mockResolvedValue({ authenticated: true, username: "user01", roles: ["USER"] });
    mockFetchCsrf
      .mockRejectedValueOnce(new Error("No session")) // init: no existing session
      .mockResolvedValue({ csrfToken: "tok", headerName: "X-CSRF-TOKEN", parameterName: "_csrf" });

    render(
      <MemoryRouter initialEntries={["/login"]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<DashboardWithLoginLink />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    // Step 1: complete login to become authenticated
    await userEvent.type(await screen.findByLabelText(/username/i), "user01");
    await userEvent.type(screen.getByLabelText(/password/i), "User@123");
    await userEvent.click(screen.getByRole("button", { name: /đăng nhập/i }));

    // Step 2: post-login we are at / (Dashboard)
    const backBtn = await screen.findByRole("button", { name: /back-to-login/i });

    // Step 3: try to navigate to /login while authenticated
    await userEvent.click(backBtn);

    // Step 4: LoginPage sees isAuthenticated=true → Navigate to /
    await screen.findByRole("button", { name: /back-to-login/i });
  });
});
