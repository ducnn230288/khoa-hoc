# Test Plan — AUTH-001

_Tạo: 2026-03-15 · Trạng thái: Active_

---

## 1. Mục tiêu

Đảm bảo mọi Acceptance Criteria (AC-1 → AC-36, AC-NFR-1 → NFR-8) được phủ bởi ít nhất một loại kiểm thử tự động hoặc được xác minh qua config review. Tuân theo policy của `docs/standards/testing.md` và `.claude/rules/40-testing.md`.

---

## 2. Các loại kiểm thử & vị trí

| Loại                            | Ký hiệu  | Tool                                | Vị trí                                                  | Mock policy          |
| ------------------------------- | -------- | ----------------------------------- | ------------------------------------------------------- | -------------------- |
| BE Unit Test                    | BE UT    | JUnit 5 + Mockito                   | `demo/src/test/.../service/`                            | Mock Repository      |
| BE Slice Test                   | BE Slice | `@WebMvcTest` + Mockito             | `demo/src/test/.../controller/`                         | Mock Service         |
| BE Integration Test (MockMvc)   | BE IT    | `@SpringBootTest` + Testcontainers  | `demo/src/test/.../integration/AuthIntegrationTest`     | No DB mock           |
| BE Integration Test (real HTTP) | BE HTTP  | `TestRestTemplate` + Testcontainers | `demo/src/test/.../integration/AuthHttpIntegrationTest` | No DB mock           |
| FE Unit Test                    | FE UT    | Vitest + React Testing Library      | `my-react-app/src/*/\*.test.{ts,tsx}`                   | Mock `src/api/` only |
| E2E                             | E2E      | Playwright                          | `my-react-app/e2e/`                                     | No API mock          |

---

## 3. AC → Test Mapping

### Ghi chú:

- **Config-only**: AC được xác minh bằng cách đọc `application-{profile}.properties` — không thể tự động hóa mà không chạy nhiều profile stack.
- **Covered**: có test tự động hiện tại hoặc được thêm trong phase này.
- **E2E**: yêu cầu running stack (BE + FE).

| AC    | Mô tả ngắn                                        | BE UT | BE Slice | BE IT | BE HTTP | FE UT | E2E | Config-only |
| ----- | ------------------------------------------------- | :---: | :------: | :---: | :-----: | :---: | :-: | :---------: |
| AC-1  | POST /login tồn tại                               |       |    ✓     |   ✓   |         |       |  ✓  |             |
| AC-2  | Login thành công → 200 + session                  |       |          |   ✓   |    ✓    |       |  ✓  |             |
| AC-3  | Cookie HttpOnly=true                              |       |          |       |    ✓    |       |     |             |
| AC-4  | Cookie SameSite=Lax (dev) / None (staging/prod)   |       |          |       |    ✓    |       |     |             |
| AC-5  | Cookie Secure=true staging/prod                   |       |          |       |         |       |     |      ✓      |
| AC-6  | Login thất bại → 401 RFC 7807                     |       |          |   ✓   |         |       |  ✓  |             |
| AC-7  | enabled=false → 401                               |   ✓   |          |   ✓   |         |       |     |             |
| AC-8  | Session timeout per profile                       |       |          |       |         |       |     |      ✓      |
| AC-9  | GET /csrf → fields đúng                           |       |          |   ✓   |    ✓    |       |  ✓  |             |
| AC-10 | /csrf chỉ khi session hợp lệ                      |       |          |   ✓   |         |       |     |             |
| AC-11 | /csrf không session → 401                         |       |    ✓     |   ✓   |         |       |     |             |
| AC-12 | Mutating yêu cầu CSRF token                       |       |    ✓     |   ✓   |         |       |  ✓  |             |
| AC-13 | Thiếu CSRF header → 403                           |       |          |   ✓   |         |       |     |             |
| AC-14 | Sai CSRF token → 403                              |       |          |   ✓   |         |       |     |             |
| AC-15 | CSRF qua header X-CSRF-TOKEN                      |       |          |   ✓   |    ✓    |       |     |             |
| AC-16 | POST /logout tồn tại                              |       |          |   ✓   |         |       |  ✓  |             |
| AC-17 | Logout invalidates session                        |       |          |   ✓   |         |       |  ✓  |             |
| AC-18 | Logout clears cookie (Set-Cookie expire)          |       |          |   ✓   |    ✓    |       |     |             |
| AC-19 | /v3/api-docs tồn tại (dev)                        |       |          |   ✓   |         |       |     |             |
| AC-20 | Swagger UI dev/staging accessible                 |       |          |   ✓   |         |       |     |             |
| AC-21 | Swagger UI disabled prod                          |       |          |       |         |       |     |      ✓      |
| AC-22 | Auth endpoints documented trong OpenAPI           |       |          |   ✓   |         |       |     |             |
| AC-23 | PostgreSQL connection                             |       |          |   ✓   |         |       |     |             |
| AC-24 | User data trong PostgreSQL                        |       |          |   ✓   |         |       |     |             |
| AC-25 | Flyway tự động chạy startup                       |       |          |   ✓   |         |       |     |             |
| AC-26 | V2 migration tạo đúng schema                      |       |          |   ✓   |         |       |     |             |
| AC-27 | V3 seed users (dev/test profile)                  |       |          |   ✓   |         |       |     |             |
| AC-28 | V3 không chạy staging/prod                        |       |          |       |         |       |     |      ✓      |
| AC-29 | In-memory session, không Redis                    |       |          |       |         |       |     |      ✓      |
| AC-30 | CORS allow localhost:5173                         |       |    ✓     |       |         |       |     |             |
| AC-31 | Không CORS wildcard                               |       |    ✓     |       |         |       |     |             |
| AC-32 | Error response RFC 7807 (5 fields)                |       |          |   ✓   |         |       |     |             |
| AC-33 | Login → GET /csrf pass qua Vite proxy (dev)       |       |          |       |    ✓    |       |  ✓  |             |
| AC-34 | Cookie Chromium-compatible (Lax, no HTTPS - dev)  |       |          |       |    ✓    |       |     |             |
| AC-35 | Redirect `/` sau login + CSRF                     |       |          |       |         |   ✓   |  ✓  |             |
| AC-36 | Authenticated user truy cập /login → redirect `/` |       |          |       |         |   ✓   |  ✓  |             |
| NFR-1 | BCrypt cost 12                                    |   ✓   |          |       |         |       |     |             |
| NFR-2 | Không log password/hash/CSRF                      |   ✓   |          |       |         |       |     |             |
| NFR-3 | CORS whitelist no wildcard                        |       |    ✓     |       |         |       |     |             |
| NFR-4 | Session timeout per profile                       |       |          |       |         |       |     |      ✓      |
| NFR-5 | V3 seed chỉ dev/test                              |       |          |   ✓   |         |       |     |             |
| NFR-6 | Swagger off prod                                  |       |          |       |         |       |     |      ✓      |
| NFR-7 | RFC 7807 error format                             |       |          |   ✓   |         |       |     |             |
| NFR-8 | In-memory session limitation documented           |       |          |       |         |       |     |      ✓      |

---

## 4. Chi tiết từng nhóm test

### 4.1 BE Unit Tests — `UserDetailsServiceImplTest.java`

Tuân theo: TST-3, TST-5. Naming: `method_state_expectedBehaviour`.

| Test method                                                          | AC/NFR | Mô tả                                                       |
| -------------------------------------------------------------------- | ------ | ----------------------------------------------------------- |
| `loadUserByUsername_withValidUser_returnsUserDetails`                | AC-7   | User enabled → authorities correct                          |
| `loadUserByUsername_withDisabledUser_returnsDisabledUserDetails`     | AC-7   | User disabled → isEnabled=false (Spring Security sẽ reject) |
| `loadUserByUsername_withUnknownUser_throwsUsernameNotFoundException` | AC-6   | Unknown user → exception                                    |
| `loadUserByUsername_withMultipleRoles_returnsAllAuthorities`         | AC-7   | User có nhiều roles → đủ authorities (**thêm mới**)         |
| `loadUserByUsername_neverLogsPasswordHash`                           | NFR-2  | Boundary: password hash không bị log (**thêm mới**)         |

### 4.2 BE Slice Tests — `AuthControllerTest.java`

Tuân theo: TST-4. `@WebMvcTest` + mock Service.

| Test method                                              | AC/NFR          | Mô tả                                  |
| -------------------------------------------------------- | --------------- | -------------------------------------- |
| `login_withValidCredentials_returns200WithSetCookie`     | AC-1, AC-2      | Mock auth success                      |
| `login_withBlankUsername_returns400`                     | AC-1 input val. | Bean Validation reject                 |
| `csrf_withoutSession_returns401`                         | AC-11           | No session → 401                       |
| `logout_withoutSession_returns401`                       | AC-16           | No session → 401                       |
| `protectedEndpoint_withoutSession_returns401WithRfc7807` | AC-32           | CustomEntryPoint → RFC 7807            |
| `login_cors_preflight_allowsLocalhost5173`               | AC-30, AC-31    | OPTIONS + Origin header (**thêm mới**) |

### 4.3 BE Integration Tests — `AuthIntegrationTest.java`

Tuân theo: TST-2. `@SpringBootTest` + Testcontainers. Profile `test`.

| Test method                                         | AC/NFR              | Mô tả                                                       |
| --------------------------------------------------- | ------------------- | ----------------------------------------------------------- |
| `login_withValidCredentials_returns200AndSetCookie` | AC-2                | Full stack login                                            |
| `login_success_createsSessionForSubsequentRequests` | AC-2                | Session tồn tại sau login                                   |
| `login_withWrongPassword_returns401ProblemDetail`   | AC-6                | Wrong pwd → 401 RFC 7807                                    |
| `csrf_afterLogin_returns200WithToken`               | AC-9, AC-10         | Session → CSRF fields                                       |
| `csrf_withoutSession_returns401ProblemDetail`       | AC-11               | No session → 401                                            |
| `logout_missingCsrfHeader_returns403`               | AC-13               | Missing CSRF → 403                                          |
| `logout_withValidSession_invalidatesSession`        | AC-16, AC-17, AC-18 | Logout flow                                                 |
| `login_failure_responseHasAllRfc7807Fields`         | AC-32               | RFC 7807 5 fields                                           |
| `login_withDisabledUser_returns401`                 | AC-7                | disabled=true → 401 (**thêm mới**)                          |
| `logout_withWrongCsrfToken_returns403`              | AC-14               | Wrong CSRF value → 403 (**thêm mới**)                       |
| `login_thenLogout_thenCsrf_returns401`              | AC-17               | Post-logout session invalid (**thêm mới - hiện có inline**) |
| `springdoc_apiDocs_available_inTestProfile`         | AC-19               | /v3/api-docs 200 trong test (**thêm mới**)                  |

### 4.4 BE Integration Tests (HTTP) — `AuthHttpIntegrationTest.java`

Tuân theo: TST-2. `TestRestTemplate` + real HTTP + Testcontainers.

| Test method                                              | AC/NFR            | Mô tả                                           |
| -------------------------------------------------------- | ----------------- | ----------------------------------------------- |
| `csrf_afterLogin_withReturnedSessionCookie_returns200`   | AC-2, AC-9, AC-33 | Cookie-based CSRF flow                          |
| `login_setCookieHeader_containsHttpOnly`                 | AC-3              | HttpOnly trong Set-Cookie header (**thêm mới**) |
| `login_setCookieHeader_containsSameSiteLax_inDevProfile` | AC-4, AC-34       | SameSite=Lax cho dev (**thêm mới**)             |
| `logout_setCookieHeader_clearsSession`                   | AC-18             | Set-Cookie clear sau logout (**thêm mới**)      |

### 4.5 FE Unit Tests

Tuân theo: TST-6, TST-7. Vitest + RTL. Mock chỉ `src/api/`.

#### `authApi.test.ts`

| Test                                                | AC    | Mô tả           |
| --------------------------------------------------- | ----- | --------------- |
| `login resolves with authenticated=true on success` | AC-2  | API contract    |
| `login rejects on invalid credentials (401)`        | AC-6  | Error contract  |
| `fetchCsrf resolves with csrfToken and headerName`  | AC-9  | CSRF contract   |
| `logout resolves with success=true`                 | AC-16 | Logout contract |

#### `LoginForm.test.tsx`

| Test                                                     | AC                    | Mô tả                  |
| -------------------------------------------------------- | --------------------- | ---------------------- |
| `renders username and password fields and submit button` | AC-1                  | UI render              |
| `shows error message when login fails`                   | AC-6                  | Error display boundary |
| `disables submit button while loading`                   | AC-35 (loading state) | Loading state boundary |
| `navigates to dashboard after login succeeds`            | AC-35                 | Redirect sau login     |

#### `ProtectedRoute.test.tsx`

| Test                                            | AC              | Mô tả                                        |
| ----------------------------------------------- | --------------- | -------------------------------------------- |
| `redirects to /login when not authenticated`    | AC-36 (inverse) | Unauthenticated redirect                     |
| `renders children when authenticated`           | AC-35           | Authenticated pass-through (**thêm mới**)    |
| `redirects authenticated user from /login to /` | AC-36           | Authenticated truy cập /login (**thêm mới**) |

### 4.6 E2E Tests — Playwright `e2e/auth.spec.ts`

Tuân theo: TST-8. Không mock API. Yêu cầu BE + FE chạy.

| Test                                                              | AC           | Mô tả                    |
| ----------------------------------------------------------------- | ------------ | ------------------------ |
| `login with valid credentials → dashboard visible`                | AC-2, AC-35  | Happy path chính         |
| `login with wrong password → error message visible`               | AC-6         | Error path quan trọng    |
| `logout → session cleared, redirected to login`                   | AC-16, AC-17 | Logout flow              |
| `unauthenticated access to protected route → redirected to login` | AC-36        | Protected route redirect |

---

## 5. Config-Only Verifications

Các AC sau không có automated test — được xác minh bằng cách đọc `application-{profile}.properties`:

| AC / NFR     | File kiểm tra                                                   | Item cần xác nhận                                   |
| ------------ | --------------------------------------------------------------- | --------------------------------------------------- |
| AC-5         | `application-staging.properties`, `application-prod.properties` | `server.servlet.session.cookie.secure=true`         |
| AC-8         | `application-dev.properties` / `staging` / `prod`               | `server.servlet.session.timeout` = 8h / 2h / 30m    |
| AC-21, NFR-6 | `application-prod.properties`                                   | `springdoc.api-docs.enabled=false`                  |
| AC-28, NFR-5 | `application-staging.properties`, `application-prod.properties` | `spring.flyway.locations` không include `seed` path |
| AC-29, NFR-8 | `build.gradle`                                                  | Không có Spring Session / Redis dependency          |
| NFR-4        | `application-{dev,staging,prod}.properties`                     | Session timeout per profile                         |

---

## 6. Gaps được thêm trong Phase 6

| File                                                     | Test method thêm mới                                         | AC fix                          |
| -------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------- |
| `UserDetailsServiceImplTest.java`                        | `loadUserByUsername_withMultipleRoles_returnsAllAuthorities` | AC-7 boundary                   |
| `AuthControllerTest.java`                                | `login_cors_preflight_allowsLocalhost5173`                   | AC-30, AC-31                    |
| `AuthIntegrationTest.java`                               | `login_withDisabledUser_returns401`                          | AC-7 full IT                    |
| `AuthIntegrationTest.java`                               | `logout_withWrongCsrfToken_returns403`                       | AC-14                           |
| `AuthIntegrationTest.java`                               | `springdoc_apiDocs_available_inTestProfile`                  | AC-19                           |
| `AuthHttpIntegrationTest.java`                           | `login_setCookieHeader_containsHttpOnly`                     | AC-3                            |
| `AuthHttpIntegrationTest.java`                           | `login_setCookieHeader_containsSameSiteLax_inDevProfile`     | AC-4, AC-34                     |
| `AuthHttpIntegrationTest.java`                           | `logout_setCookieHeader_clearsSession`                       | AC-18                           |
| `ProtectedRoute.test.tsx`                                | `renders children when authenticated`                        | AR-F5                           |
| `ProtectedRoute.test.tsx`                                | `redirects authenticated user from /login to /`              | AC-36                           |
| `my-react-app/playwright.config.ts` + `e2e/auth.spec.ts` | 4 E2E scenarios                                              | AC-2, AC-6, AC-16, AC-35, AC-36 |

---

## 7. Run Commands

```bash
# BE — tất cả test (yêu cầu Docker để Testcontainers)
cd demo && ./gradlew test

# FE — unit tests
cd my-react-app && npm run test

# FE — lint
cd my-react-app && npm run lint

# E2E — yêu cầu BE + FE running
cd my-react-app && npx playwright test
```

---

## 8. Completion Checklist (Phase 6)

- [x] test-plan.md tạo với AC ↔ test type mapping đầy đủ
- [x] BE gaps filled → `./gradlew test` passes (26/26)
- [x] FE UT gap filled → `npm run test` passes (10/10)
- [x] Playwright installed + E2E spec created (`playwright.config.ts` + `e2e/auth.spec.ts`)
- [x] `test-results.md` ghi lại kết quả BE + FE
- [ ] E2E executed (requires running stack — see `test-results.md §4`)
