/**
 * TST-6: ProtectedRoute — redirects unauthenticated users to /login (AR-F5).
 */
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, it, vi } from "vitest";
import * as authApi from "../api/authApi";
import { AuthProvider } from "../context/AuthContext";
import ProtectedRoute from "./ProtectedRoute";

vi.mock("../api/authApi");

const mockFetchCsrf = vi.mocked(authApi.fetchCsrf);

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

describe("ProtectedRoute", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("redirects to /login when not authenticated", async () => {
    mockFetchCsrf.mockRejectedValue(new Error("No session"));
    renderProtectedRoute("/");
    // After auth check finishes, should show login
    await screen.findByText("Login Page");
  });
});
