import { expect, test } from "@playwright/test";

/**
 * TST-8: E2E auth flows — no API mocking.
 * Requires: BE on localhost:8080 (profile=dev), FE on localhost:5173.
 *
 * Covers:
 *   AC-2, AC-35  — login happy path
 *   AC-6         — login error path
 *   AC-16, AC-17 — logout flow
 *   AC-36        — protected route redirect
 */

const VALID_USER = { username: "user01", password: "User@123" };
const WRONG_PASS = { username: "user01", password: "WrongPassword" };

// ---------------------------------------------------------------------------
// AC-2, AC-35: Login with valid credentials → dashboard visible
// ---------------------------------------------------------------------------
test("login with valid credentials → dashboard visible (AC-2, AC-35)", async ({ page }) => {
  await page.goto("/login");

  await page.getByLabel(/username/i).fill(VALID_USER.username);
  await page.getByLabel(/password/i).fill(VALID_USER.password);
  await page.getByRole("button", { name: /đăng nhập/i }).click();

  // After login + CSRF fetch, should be at / with dashboard content
  await expect(page).toHaveURL("/");
  await expect(page.getByText(/welcome/i)).toBeVisible();
});

// ---------------------------------------------------------------------------
// AC-6: Login with wrong password → error message visible
// ---------------------------------------------------------------------------
test("login with wrong password → error message visible (AC-6)", async ({ page }) => {
  await page.goto("/login");

  await page.getByLabel(/username/i).fill(WRONG_PASS.username);
  await page.getByLabel(/password/i).fill(WRONG_PASS.password);
  await page.getByRole("button", { name: /đăng nhập/i }).click();

  // Error message must appear — spec: "Sai tên đăng nhập hoặc mật khẩu" area
  await expect(page.getByRole("alert")).toBeVisible();
  // Still on /login
  await expect(page).toHaveURL("/login");
});

// ---------------------------------------------------------------------------
// AC-16, AC-17: Logout → session cleared, redirected to login
// ---------------------------------------------------------------------------
test("logout → session cleared, redirected to login (AC-16, AC-17)", async ({ page }) => {
  // First login
  await page.goto("/login");
  await page.getByLabel(/username/i).fill(VALID_USER.username);
  await page.getByLabel(/password/i).fill(VALID_USER.password);
  await page.getByRole("button", { name: /đăng nhập/i }).click();
  await expect(page).toHaveURL("/");

  // Logout
  await page.getByRole("button", { name: /logout/i }).click();

  // After logout, must redirect to /login
  await expect(page).toHaveURL("/login");

  // Verify session is truly gone: navigating to / should redirect to /login
  await page.goto("/");
  await expect(page).toHaveURL("/login");
});

// ---------------------------------------------------------------------------
// AC-36: Unauthenticated access to protected route → redirected to /login
// ---------------------------------------------------------------------------
test("unauthenticated access to protected route → redirected to /login (AC-36)", async ({ page }) => {
  // Navigate directly to / without a session
  await page.goto("/");

  // Must end up at /login
  await expect(page).toHaveURL("/login");
  await expect(page.getByRole("button", { name: /đăng nhập/i })).toBeVisible();
});
