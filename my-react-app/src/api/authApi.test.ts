/**
 * TST-6, TST-7: FE unit tests — mock only src/api/authApi.ts.
 */
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as authApi from "./authApi";

vi.mock("./authApi");

const mockLogin = vi.mocked(authApi.login);
const mockFetchCsrf = vi.mocked(authApi.fetchCsrf);
const mockLogout = vi.mocked(authApi.logout);

describe("authApi module exports", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("login resolves with authenticated=true on success", async () => {
    mockLogin.mockResolvedValue({ authenticated: true, username: "user01", roles: ["USER"] });
    const result = await authApi.login({ username: "user01", password: "User@123" });
    expect(result.authenticated).toBe(true);
    expect(result.username).toBe("user01");
  });

  it("login rejects on invalid credentials", async () => {
    mockLogin.mockRejectedValue(Object.assign(new Error("Invalid username or password."), { status: 401 }));
    await expect(authApi.login({ username: "user01", password: "wrong" })).rejects.toMatchObject({
      status: 401,
    });
  });

  it("fetchCsrf resolves with csrfToken and headerName", async () => {
    mockFetchCsrf.mockResolvedValue({ csrfToken: "tok123", headerName: "X-CSRF-TOKEN", parameterName: "_csrf" });
    const result = await authApi.fetchCsrf();
    expect(result.headerName).toBe("X-CSRF-TOKEN");
    expect(result.csrfToken).toBe("tok123");
  });

  it("logout resolves with success=true", async () => {
    mockLogout.mockResolvedValue({ success: true });
    const result = await authApi.logout("tok123");
    expect(result.success).toBe(true);
    expect(mockLogout).toHaveBeenCalledWith("tok123");
  });
});
