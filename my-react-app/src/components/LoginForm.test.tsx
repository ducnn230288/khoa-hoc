/**
 * TST-6, TST-7: LoginForm component tests using React Testing Library.
 * Mocks only src/api/authApi — not internal hooks.
 */
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as authApi from "../api/authApi";
import { AuthProvider } from "../context/AuthContext";
import LoginForm from "./LoginForm";

vi.mock("../api/authApi");

const mockLogin = vi.mocked(authApi.login);
const mockFetchCsrf = vi.mocked(authApi.fetchCsrf);

function renderLoginForm() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginForm />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("LoginForm", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    // Default: fetchCsrf fails (no session on init)
    mockFetchCsrf.mockRejectedValue(new Error("No session"));
  });

  it("renders username and password fields and submit button", () => {
    renderLoginForm();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /đăng nhập/i })).toBeInTheDocument();
  });

  it("shows error message when login fails", async () => {
    mockLogin.mockRejectedValue(Object.assign(new Error("Invalid username or password."), { status: 401 }));

    renderLoginForm();
    await userEvent.type(screen.getByLabelText(/username/i), "user01");
    await userEvent.type(screen.getByLabelText(/password/i), "wrongpass");
    await userEvent.click(screen.getByRole("button", { name: /đăng nhập/i }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(/invalid username or password/i);
    });
  });

  it("disables submit button while loading", async () => {
    // Login takes time
    mockLogin.mockImplementation(() => new Promise(() => {})); // never resolves
    mockFetchCsrf.mockResolvedValue({ csrfToken: "tok", headerName: "X-CSRF-TOKEN", parameterName: "_csrf" });

    renderLoginForm();
    await userEvent.type(screen.getByLabelText(/username/i), "user01");
    await userEvent.type(screen.getByLabelText(/password/i), "User@123");
    await userEvent.click(screen.getByRole("button", { name: /đăng nhập/i }));

    expect(screen.getByRole("button")).toBeDisabled();
  });
});
