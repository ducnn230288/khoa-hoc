import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright E2E configuration.
 * Requires: BE running on localhost:8080, FE running on localhost:5173.
 * TST-8: No API mocking — real stack only.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    // Include credentials so session cookie is sent on cross-origin requests via the Vite proxy
    extraHTTPHeaders: {},
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  // Do NOT start a webServer here — the stack must be running externally.
  // Start BE: cd demo && ./gradlew bootRun --args='--spring.profiles.active=dev'
  // Start FE: cd my-react-app && npm run dev
});
