# Implementation Plan - auth (Session-based authentication foundation)

> Derived from `docs/changes/auth/spec-pack.md`.
> Must stay consistent with `docs/architecture/*` and rules 10-40.
> This plan stays inside the ticket boundary: session-cookie auth, CSRF API riêng, PostgreSQL, Flyway, Springdoc, và UI auth tối thiểu.

---

## 1. Policy

### 1.1 Scope guardrails

Chỉ triển khai những gì spec đã chốt:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/logout`
- session cookie + session timeout + CORS/credentials baseline
- PostgreSQL + Flyway cho auth persistence baseline
- seed user cho `dev/test` theo runtime secret input ngoài repo
- Springdoc/OpenAPI cho `dev/staging`, không public ở `production`
- UI auth tối thiểu để FE thể hiện login / in-progress / authenticated / session-expired / CSRF-failure states

Không làm ngoài spec:

- không thêm social login, JWT, refresh token, remember-me, MFA, password reset
- không thêm RBAC/business authorization ngoài việc trả roles cơ bản trong login response
- không tạo business endpoint mới chỉ để demo CSRF
- không đưa secret/password/token/session id vào repo artifacts, docs, logs, example payloads

### 1.2 Chosen implementation policy

#### Policy A - Backend security foundation

**Chọn:** dùng Spring Security theo mô hình **session-based authentication + explicit CSRF + custom auth endpoints**.

**Lý do chọn:**

- bám sát AC-1 đến AC-16 hơn cách tự viết interceptor/filter thủ công;
- framework đã có primitives đúng bài toán: session auth, CSRF repository, cookie/session hardening, logout handling;
- giảm rủi ro bỏ sót bảo vệ cho mutating endpoints và giúp test contract ở integration layer rõ hơn;
- phù hợp Rule 30, Rule 35, Rule 36 vì security behavior được đặt explicit trong config thay vì rải rác.

**So sánh phương án:**

| Option | Ưu điểm | Nhược điểm | Kết luận |
| ------ | ------- | ---------- | -------- |
| Spring Security với custom JSON endpoints | Bám AC tốt, ít plumbing tự chế, dễ kiểm soát session/CSRF/CORS/error mapping | Cần thêm cấu hình và test wiring rõ ràng | **Chọn** |
| Tự viết servlet filters/interceptors/auth service thuần | Linh hoạt bề mặt code | Dễ lệch framework defaults, dễ sót 401/403/session/CSRF semantics, khó review hơn | Không chọn |

#### Policy B - Persistence and migrations

**Chọn:** PostgreSQL là nguồn dữ liệu auth chính; Flyway quản lý schema baseline; auth tables được tạo qua migration versioned.

**Lý do chọn:**

- AC-17 đến AC-19 yêu cầu rõ PostgreSQL + Flyway + bảng `users`, `roles`, `user_roles`;
- versioned migrations là executable truth tốt nhất cho Rule 11;
- giúp rollback/traceability tốt hơn so với tạo bảng bằng runtime bootstrap mơ hồ.

#### Policy C - Dev/test seed mechanism

**Chọn:** dùng **profile-scoped Flyway Java migration hoặc equivalent Flyway-executed seed step cho `dev/test`** để materialize `V3__seed_dev_test_users`, đọc runtime secret input ngoài repo, hash bằng application-approved encoder trước khi insert.

**Lý do chọn:**

- vẫn giữ được intent migration `V3__seed_dev_test_users` trong spec;
- không cần để plaintext password trong SQL hay repo;
- fail-fast được khi thiếu secret input bắt buộc;
- không để seed logic sống ngoài Flyway rồi mất traceability với AC-18 đến AC-21.

**So sánh phương án:**

| Option | Ưu điểm | Nhược điểm | Kết luận |
| ------ | ------- | ---------- | -------- |
| Flyway Java migration / Flyway-executed dev-test-only seed | Giữ seed trong migration timeline, dễ trace AC, không cần plaintext trong repo, fail-fast rõ | Cần cấu hình profile/location cẩn thận | **Chọn** |
| App bootstrap runner sau startup | Code đơn giản hơn cho việc hash password | Seed không còn là migration truth, dễ lệch với Flyway lifecycle/spec intent | Không chọn |
| SQL seed migration với plaintext hoặc hardcoded hash trong repo | Dễ viết | Vi phạm Rule 32 hoặc làm mờ ý nghĩa runtime secret input | Loại bỏ |

#### Policy D - Environment-specific behavior

**Chọn:** mọi khác biệt môi trường phải đi qua config/profile tường minh (`dev`, `staging`, `production`) thay vì `if/else` rải trong business code.

Các concern phải explicit theo config:

- session timeout
- cookie secure behavior
- allowed frontend origins
- Flyway seed locations cho `dev/test`
- Springdoc exposure
- production `/v3/api-docs = 404`

#### Policy E - Frontend structure

**Chọn:** tách auth thành feature flow tối thiểu thay vì tiếp tục nhồi logic vào `App.tsx`.

**Lý do chọn:**

- bám `overview.md` về feature/page layer;
- giữ reviewability tốt hơn khi UI cần 5 screen states;
- dễ map state machine của auth flow với AC hơn so với starter component đơn khối.

Tuy vậy, plan sẽ **không** kéo thêm router/state framework mới nếu chưa thật cần, để bám Rule 12 và giữ scope nhỏ.

### 1.3 Contract policy derived from AC

1. Login request chỉ nhận `username` + `password`; không chấp nhận email identifier.
2. Success response của login phải trả đúng shape `authenticated`, `username`, `roles`.
3. Tất cả lỗi auth trong boundary phải dùng unified JSON error contract.
4. `GET /api/v1/auth/csrf` chỉ hoạt động khi session hợp lệ.
5. `POST /api/v1/auth/logout` bắt buộc session hợp lệ + CSRF hợp lệ.
6. FE chỉ giữ `csrfToken` ở memory state; session id chỉ nằm trong cookie.
7. Logging chỉ ghi outcome và metadata an toàn; tuyệt đối không log password/token/session id.

---

## 2. Existing code cần đọc trước khi sửa

Danh sách này được lập trước để tránh đọc lan man ngoài boundary auth.

### 2.1 Backend / config baseline

- [x] `demo/build.gradle`
- [x] `demo/settings.gradle`
- [x] `demo/src/main/resources/application.properties`
- [x] `demo/src/main/java/com/example/demo/DemoApplication.java`
- [x] `demo/src/test/java/com/example/demo/DemoApplicationTests.java`

### 2.2 Frontend baseline

- [x] `my-react-app/package.json`
- [x] `my-react-app/vite.config.ts`
- [x] `my-react-app/tsconfig.json`
- [x] `my-react-app/tsconfig.app.json`
- [x] `my-react-app/src/main.tsx`
- [x] `my-react-app/src/App.tsx`
- [ ] `my-react-app/src/App.css`
- [ ] `my-react-app/src/index.css`

### 2.3 Authoritative docs / rules

- [x] `docs/changes/auth/spec-pack.md`
- [x] `docs/architecture/overview.md`
- [x] `docs/architecture/key-flows.md`
- [x] `docs/standards/coding.md`
- [x] `docs/standards/security.md`
- [x] `docs/standards/testing.md`
- [x] `docs/standards/templates/impl-plan.template.md`

### 2.4 Baseline findings after reading

- Backend hiện chỉ có bootstrap app + smoke test; chưa có security, persistence, profile config, DTO, controller, service, repository, migration, hay API docs wiring.
- Frontend hiện chỉ là Vite starter UI; chưa có auth feature structure, API client, error-state handling, hay credentialed API flow.
- Vì baseline còn rất mỏng, impact analysis phải mô tả rõ những module mới sẽ được thêm vào thay vì giả định có sẵn hạ tầng auth.

---

## 3. Impact analysis

### 3.1 Affected APIs

| API / surface | Impact | Reason |
| ------------- | ------ | ------ |
| `POST /api/v1/auth/login` | Add new endpoint | AC-1 đến AC-5 |
| `GET /api/v1/auth/csrf` | Add new endpoint | AC-10 đến AC-12 |
| `POST /api/v1/auth/logout` | Add new endpoint | AC-13 đến AC-16 |
| `/v3/api-docs` | Add environment-specific exposure/hardening | AC-22 đến AC-24 |
| `/swagger-ui/**` | Add environment-specific exposure/hardening | AC-22, AC-23 |

### 3.2 Affected backend modules / files

| Area | Existing code cần đọc | Planned impact |
| ---- | --------------------- | -------------- |
| Build/dependencies | `demo/build.gradle` | Add Spring Security, validation/persistence support if needed, PostgreSQL driver, Flyway, Springdoc |
| App bootstrap | `DemoApplication.java` | Keep as bootstrap only; avoid business logic here |
| Configuration | `application.properties` | Split/extend into explicit environment config for session, CORS, DB, docs exposure |
| Web entry layer | none existing | Add auth controller, request/response DTOs, exception/error mapping |
| Security configuration | none existing | Add SecurityFilterChain, CORS config, CSRF config, session management, auth entry points/access denied handling |
| Application layer | none existing | Add login/logout/csrf orchestration services/use cases |
| Domain model | none existing | Add user/role concepts and auth-related invariants kept framework-light |
| Infrastructure persistence | none existing | Add repository implementations / JPA mappings or equivalent persistence adapter for PostgreSQL |
| Migration layer | none existing | Add Flyway migrations V1/V2/V3 |
| Seed bootstrap | none existing | Add dev/test-only seed path with fail-fast validation and sanitized errors |
| Observability/logging | none existing | Add sanitized auth outcome logging |
| Tests | `DemoApplicationTests.java` | Keep smoke test and add focused integration tests for contracts/config |

### 3.3 Affected frontend modules / files

| Area | Existing code cần đọc | Planned impact |
| ---- | --------------------- | -------------- |
| App composition | `src/main.tsx`, `src/App.tsx` | Replace starter UI with auth flow composition |
| Styling | `src/App.css`, `src/index.css` | Adapt styles for auth screens/states |
| Feature modules | none existing | Add auth feature/page, view state, API client, typed contracts |
| Error handling | none existing | Add explicit handling for `AUTH_INVALID_CREDENTIALS`, `AUTH_SESSION_REQUIRED`, `AUTH_CSRF_INVALID` |
| Session/CSRF state | none existing | Add in-memory client state for authenticated user + csrf token |

### 3.4 Affected data model / DB

| DB object | Impact | Notes |
| --------- | ------ | ----- |
| `users` | New table | Includes `username`, `password_hash`, `enabled`, timestamps |
| `roles` | New table | Minimal baseline for role codes/names |
| `user_roles` | New table | Join table for login response role resolution |
| `flyway_schema_history` | New operational table | Managed by Flyway |

### 3.5 Affected settings / environment inputs

| Setting / input | Impact | Notes |
| --------------- | ------ | ----- |
| DB connection settings | Add | Needed for PostgreSQL in each environment |
| Session timeout per profile | Add | dev 8h / staging 2h / production 30m |
| Allowed frontend origins | Add | Explicit allowlist; dev includes `http://localhost:5173` |
| Cookie/security flags | Add | Path `/`, `SameSite=None`, `HttpOnly=true`, `Secure` by environment |
| Springdoc toggles | Add | dev/staging enabled; production hidden/404 |
| Seed runtime secret inputs | Add | dev/test only; required for seed materialization |
| Seed enablement / Flyway locations | Add | Must not bleed into staging/prod |

### 3.6 Affected logs / observability

| Event | Impact | Guardrail |
| ----- | ------ | --------- |
| Login success | Add sanitized log | No password / session id / CSRF token |
| Login failure | Add sanitized log | No user enumeration detail in client-visible path |
| Session required / expired | Add sanitized log | No token leakage |
| CSRF rejection | Add sanitized log | Reason category only; no token value |
| Logout success | Add sanitized log | No session id leakage |
| Seed failure due to missing secrets | Add startup failure log | Message must not contain secret value |

### 3.7 Affected permission model

| Surface | Permission expectation |
| ------- | ---------------------- |
| `POST /api/v1/auth/login` | Anonymous allowed |
| `GET /api/v1/auth/csrf` | Authenticated session required |
| `POST /api/v1/auth/logout` | Authenticated session + valid CSRF required |
| Swagger/OpenAPI in dev/staging | Accessible for integration/dev use |
| Swagger/OpenAPI in production | Not public |

### 3.8 Planned file/module changes

| File / module | Change type | Reason |
| ------------- | ----------- | ------ |
| `demo/build.gradle` | Modify | Add dependencies required by auth, persistence, migrations, docs |
| `demo/src/main/resources/application.properties` | Modify or split | Introduce explicit config baseline |
| `demo/src/main/resources/application-dev.properties` | Add | Dev-specific session/CORS/docs/seed config |
| `demo/src/main/resources/application-staging.properties` | Add | Staging-specific hardening and docs exposure |
| `demo/src/main/resources/application-prod.properties` | Add | Production hardening, docs disabled/404 |
| `demo/src/main/java/.../auth/...` | Add | Auth entry/application/domain/infrastructure modules |
| `demo/src/main/resources/db/migration/common/V1__init_schema.sql` | Add | Schema baseline |
| `demo/src/main/resources/db/migration/common/V2__create_auth_tables.sql` | Add | Auth tables |
| `demo/src/main/java/.../migration/V3__seed_dev_test_users.*` or equivalent profile-scoped migration resource | Add | Dev/test-only seed materialization |
| `demo/src/test/java/...` auth integration tests | Add | Verify contracts, cookies, CSRF, profile behavior |
| `my-react-app/src/App.tsx` | Modify | Replace starter UI with auth composition |
| `my-react-app/src/auth/*` or equivalent feature folder | Add | Auth screens/state/API client/types |
| `my-react-app/src/App.css` and/or new auth CSS | Modify/Add | Support required wireframe states |
| `docs/changes/auth/impl-plan.md` | Add | Ticket-specific implementation plan |

---

## 4. Implementation steps

Nguyên tắc: **1 step = nhỏ tới mức có thể review độc lập**.

| Step | Description | Files touched | Verification |
| ---- | ----------- | ------------- | ------------ |
| 1 | Update backend build to include only the minimum dependencies required for session auth, PostgreSQL, Flyway, and Springdoc. | `demo/build.gradle` | `cd demo && ./gradlew test` still starts/compiles |
| 2 | Introduce explicit profile-based configuration skeleton for `dev`, `staging`, `production` (session timeout, CORS origins, docs toggles, DB settings placeholders). | `application*.properties` | Startup/config binding review; targeted config tests if added |
| 3 | Add backend package structure that respects entry/application/domain/infrastructure boundaries for auth. | new `demo/src/main/java/...` packages | Code review against Rule 10 |
| 4 | Add auth DTOs and unified error response contract for login/csrf/logout boundary. | auth request/response/error classes | Integration tests for JSON shapes |
| 5 | Implement persistence baseline for users/roles/user_roles and repository contracts/adapters targeting PostgreSQL. | entity/repository/adapter files | Repository or integration tests against DB boundary |
| 6 | Add Flyway schema migrations `V1__init_schema` and `V2__create_auth_tables`. | Flyway SQL resources | App startup against fresh DB; inspect created tables |
| 7 | Add dev/test-only `V3__seed_dev_test_users` materialization with runtime-secret validation, hashing path, and fail-fast sanitized errors. | profile-scoped migration resources / migration class / config | Startup tests for dev/test success and missing-secret failure |
| 8 | Implement login use case and endpoint using `username` only, disabled-user rejection, unified 401 contract, and session creation. | auth service/controller/security wiring | Integration tests for AC-1 to AC-5 |
| 9 | Configure session cookie policy, session timeout, CORS allowlist, and authenticated access rules for auth endpoints. | security config + properties | Cookie/CORS/session integration tests |
| 10 | Implement `GET /api/v1/auth/csrf` contract and CSRF handling so FE can fetch token after login. | security config + csrf endpoint | Integration tests for AC-10 to AC-12 |
| 11 | Implement logout endpoint requiring session + CSRF, invalidating session, and clearing/expiring cookie. | logout handler/controller/security config | Integration tests for AC-13 to AC-16 |
| 12 | Integrate Springdoc for dev/staging and harden production to hide Swagger UI and return `404` for `/v3/api-docs`. | build/config/docs wiring | Profile-based endpoint tests/manual checks |
| 13 | Replace frontend starter UI with auth feature flow covering login, in-progress, authenticated, session-expired, and CSRF-failure states. | `App.tsx`, auth feature files, CSS | `npm run lint && npm run build`; manual flow checks |
| 14 | Wire FE API calls for login -> csrf -> authenticated state, credentials mode, memory-only CSRF storage, logout, and error recovery behavior. | auth API/state files | Manual contract check against backend + UI state transitions |
| 15 | Add/extend backend integration tests and retain smoke test; document verification evidence and remaining test gaps honestly. | backend tests, docs if needed | `./gradlew test` |
| 16 | Update ticket docs if executable truth changed from assumptions during implementation, keeping common-base docs untouched unless baseline truly changed. | `docs/changes/auth/*` only unless baseline changes | Review against Rule 11 and Rule 19 |

---

## 5. Verification procedure

### 5.1 Minimum repo commands

```bash
# Backend
cd demo && ./gradlew test

# Frontend
cd my-react-app && npm run lint && npm run build
```

### 5.2 Ticket-specific backend verification

1. Start backend in `dev` with PostgreSQL reachable and required seed secrets present.
2. Verify Flyway applies schema migrations on empty DB.
3. Verify `users`, `roles`, `user_roles` exist.
4. Verify login with seeded `user01` succeeds and returns session cookie.
5. Verify login with wrong password returns `401` + `AUTH_INVALID_CREDENTIALS`.
6. Verify login with disabled user returns the same client-visible contract as invalid credential.
7. Verify cookie contains `HttpOnly`, `Path=/`, `SameSite=None`; verify `Secure` in staging/prod profiles.
8. Verify `GET /api/v1/auth/csrf` returns token only when session is valid.
9. Verify `GET /api/v1/auth/csrf` without/after-expired session returns `401` + `AUTH_SESSION_REQUIRED`.
10. Verify `POST /api/v1/auth/logout` without CSRF returns `403` + `AUTH_CSRF_INVALID`.
11. Verify `POST /api/v1/auth/logout` with valid session + CSRF returns `{ "success": true }` and clears/expires cookie.
12. Verify dev/staging expose Springdoc; production hides Swagger UI and `/v3/api-docs` returns `404`.
13. Verify startup in `dev/test` fails fast with sanitized error when required seed secret inputs are missing.
14. Verify staging/prod profiles do not materialize default seed users.

### 5.3 Ticket-specific frontend verification

1. Login screen shows `username`, password, submit, and login error area.
2. UI shows in-progress state after submit and prevents duplicate submit while processing.
3. On successful login, FE fetches CSRF token and renders authenticated state with username, roles, session active, CSRF loaded.
4. On `401/AUTH_SESSION_REQUIRED`, FE redirects or resets to login/session-expired state.
5. On `403/AUTH_CSRF_INVALID`, FE shows CSRF-failure state with reload/login-again guidance.
6. FE stores CSRF token in memory only and sends `X-CSRF-TOKEN` on mutating auth requests.

### 5.4 Preferred automated test split

| Area | Preferred layer | Why |
| ---- | --------------- | --- |
| Login / csrf / logout contracts | Backend integration tests | Boundary-sensitive HTTP + security behavior |
| Cookie flags / session rules / profile behavior | Backend integration tests | Config-sensitive, not suitable for unit tests alone |
| Flyway migrations / seed fail-fast | Backend integration tests or startup tests per profile | Need executable evidence |
| Frontend state transitions | Build/lint + manual verification in this phase baseline | No frontend unit-test runner configured yet |

---

## 6. Risks

| Risk | Impact | Mitigation |
| ---- | ------ | ---------- |
| Spring Boot snapshot + Java 25 + new dependencies (Spring Security, Springdoc, Flyway, PostgreSQL driver) may surface compatibility issues | High | Introduce dependencies incrementally; verify build after each dependency/config step |
| Cross-site cookie behavior differs between local and hardened environments because local HTTPS/domain topology is not fixed by spec | High | Keep cookie/CORS policy explicit, verify by profile, document local limitations honestly |
| Seed implementation accidentally leaks secrets via logs, migration placeholders, or example docs | High | Centralize seed input validation, sanitize exceptions/logs, never print runtime values |
| Production docs hardening could be incomplete if Swagger UI is hidden but `/v3/api-docs` still returns content | High | Add explicit production check/assertion for `404` on `/v3/api-docs` |
| Frontend may drift into email-login or starter-UI shortcuts that violate `username-only` and required states | Medium | Keep typed request contract and UI labels aligned with spec; include manual verification checklist |
| Over-concentrating logic in security config or `App.tsx` reduces reviewability | Medium | Split by layers/features early; keep config focused on wiring only |
| Seed users present outside `dev/test` due to mis-scoped Flyway locations/profiles | High | Make seed path profile-scoped and test negative case for staging/prod |
| Current repo has no frontend test runner, so UI regressions rely heavily on lint/build/manual checks | Medium | Keep FE flow simple, typed, and document verification limits honestly |

---

## 7. Rollback

1. Revert auth-specific code, config, migrations, and frontend auth feature files as one ticket-scoped change.
2. Remove new backend dependencies if the ticket is backed out entirely.
3. Drop auth tables and seed data only in non-production environments where this ticket was introduced and rollback is explicitly approved.
4. Disable Springdoc exposure and auth profile settings introduced by this ticket.
5. Restore frontend from auth flow back to the prior starter state only if the ticket is fully rolled back.
6. Re-run backend/frontend verification commands to confirm the repo returns to pre-ticket baseline.

Rollback guardrail: nếu migration đã chạy trên shared environment, rollback DB phải theo change-management riêng; không tự ý xóa dữ liệu ở môi trường nhiều người dùng.

---

## 8. Rule checks

| Rule | How the plan satisfies it |
| ---- | -------------------------- |
| Rule 10 | Tách entry/application/domain/infrastructure; không đặt business auth logic trong controller/config |
| Rule 11 | Schema/config/API docs là executable truth; impl-plan yêu cầu cập nhật ticket docs nếu truth thay đổi |
| Rule 12 | Ưu tiên toolchain hiện có: Spring Boot, Gradle, React, Vite, strict TS; không kéo thêm framework FE không cần thiết |
| Rule 13 | Chia step nhỏ và chia module theo trách nhiệm rõ ràng |
| Rule 14 | Dùng explicit DTOs và unified error contract |
| Rule 15 | Tất cả khác biệt môi trường đi qua profile/config rõ ràng |
| Rule 16 | FE chỉ phụ thuộc HTTP contract; không dựa backend internals không tài liệu hóa |
| Rule 17 | Steps nhỏ, file/module naming rõ ràng, dễ review |
| Rule 18 | Scope chỉ quanh auth foundation; không chạm docs common-base nếu không thật sự đổi baseline |
| Rule 19 | Ticket-specific details chỉ ở `docs/changes/auth/*` |
| Rule 20 | Có verification procedure rõ cho từng vùng thay đổi |
| Rule 24 | Boundary-sensitive behavior được ưu tiên test ở integration layer |
| Rule 29 | Có AC mapping table phía dưới |
| Rule 30 | Session/cookie/CORS/CSRF/docs exposure đều là explicit decisions |
| Rule 32 | Seed passwords/secrets không được commit vào repo artifacts |
| Rule 35 | Mutating protected operations yêu cầu session + CSRF rõ ràng |
| Rule 36 | Dev/test conveniences bị chặn khỏi staging/prod bằng profile config |
| Rule 37 | Unified sanitized error/log policy tránh lộ internals |
| Rule 39 | Dependency/config changes được xem là security changes và có verification riêng |
| Rule 40 | Những giả định chưa xác minh được chuyển xuống checklist xác nhận trước implementation |

---

## 9. AC mapping table

| AC | Planned step(s) | Where satisfied | Verification approach |
| -- | --------------- | --------------- | --------------------- |
| AC-1 | 8 | Login controller/request DTO/validation | Backend integration test for `POST /api/v1/auth/login` request contract |
| AC-2 | 8, 9 | Login use case + session creation + cookie issuance | Integration test asserting `200`, session creation, `Set-Cookie` |
| AC-3 | 8 | Login success response DTO | Integration test for response JSON |
| AC-4 | 8 | Unified 401 error mapping for invalid credential | Integration test for wrong credential |
| AC-5 | 8 | Disabled-user rejection using same client-visible contract | Integration test for `enabled=false` user |
| AC-6 | 9 | Cookie policy config | Integration test inspecting cookie attributes |
| AC-7 | 2, 9 | Staging/prod cookie hardening via profile config | Profile-based integration/manual check |
| AC-8 | 2, 9 | Session timeout per profile | Config/startup verification + targeted timeout/config test |
| AC-9 | 2, 9 | Explicit CORS allowlist with credentials | Integration test / config verification |
| AC-10 | 10 | CSRF endpoint | Integration test for authenticated session |
| AC-11 | 10 | CSRF response DTO / headerName contract | Integration test for JSON body |
| AC-12 | 10 | Session-required error handling on CSRF endpoint | Integration test for missing/expired session |
| AC-13 | 11 | Logout endpoint security requirements | Integration test for session + CSRF preconditions |
| AC-14 | 11 | Logout success flow and cookie clearing | Integration test for `{ "success": true }` + cookie expiry |
| AC-15 | 11 | Missing-CSRF rejection for mutating protected request | Integration test for logout without CSRF |
| AC-16 | 11 | Invalid/expired-CSRF rejection | Integration test for invalid CSRF |
| AC-17 | 5 | PostgreSQL-backed persistence layer | DB-backed integration verification |
| AC-18 | 6, 7 | Flyway integration on startup | Startup/integration test |
| AC-19 | 6 | Auth tables migration | Fresh DB migration verification |
| AC-20 | 7 | Dev/test-only seed path with runtime secret input | Profile-based startup/integration test |
| AC-21 | 7 | Fail-fast seed validation with sanitized message | Negative startup test |
| AC-22 | 12 | Springdoc enabled in dev/staging | Profile-based endpoint/manual check |
| AC-23 | 12 | Production hardening for Swagger UI and `/v3/api-docs` | Production-profile endpoint test |
| AC-24 | 12, 16 | OpenAPI content aligned with real auth endpoints and errors | Manual doc review + endpoint content check |

---

## 10. Checklist cần xác nhận trước khi implementation bắt đầu

### 10.1 Technical confirmations

- [ ] Chốt package naming/backend module layout cho auth (`entry`, `application`, `domain`, `infrastructure`) để tránh tạo cấu trúc rồi đổi giữa chừng.
- [ ] Chốt cách backend kết nối PostgreSQL ở local/dev/test trong repo này (container cục bộ, DB cài sẵn, hoặc equivalent) để verification khả thi.
- [ ] Chốt naming chính xác cho Spring profiles (`dev`, `staging`, `production`; và nếu có `test`, quan hệ của `test` với seed path thế nào).
- [ ] Chốt strategy profile-scoped Flyway locations để `V3__seed_dev_test_users` không bao giờ chạy ở staging/prod.
- [ ] Chốt class/package cho unified auth error handling để tránh phân tán 401/403 contract.
- [ ] Chốt cơ chế test backend cho cookie/CORS/CSRF/profile behavior (MockMvc/WebTestClient/Testcontainers hoặc equivalent phù hợp với toolchain hiện có).

### 10.2 Security confirmations

- [ ] Xác nhận nguồn runtime secret input cho seed users ở dev/test là gì và cách cung cấp ngoài repo.
- [ ] Xác nhận secret input không xuất hiện trong docs, logs, screenshots, shell history chia sẻ, sample `.properties`, hay migration SQL.
- [ ] Xác nhận logging policy cho auth events chỉ chứa metadata an toàn.
- [ ] Xác nhận production phải trả `404` thật cho `/v3/api-docs`, không chỉ “ẩn link Swagger UI”.

### 10.3 Frontend confirmations

- [ ] Chốt FE sẽ giữ CSRF token trong memory state, không dùng localStorage/sessionStorage.
- [ ] Chốt auth flow không cần router framework ở ticket này; nếu cần route transition, phải vẫn giữ scope nhỏ.
- [ ] Chốt UI copy bám đúng spec messages cho các trạng thái lỗi bảo mật/hết phiên.

### 10.4 Evidence / delivery confirmations

- [ ] Chốt danh sách AC nào có thể tự động hóa ngay bằng backend integration tests.
- [ ] Ghi rõ phần FE nào chỉ có thể verify bằng lint/build/manual do chưa có test runner.
- [ ] Nếu trong lúc implement phát hiện baseline repo khác với docs common-base, phải log lại như ticket evidence trước khi sửa docs common-base.

### 10.5 Thông tin còn thiếu nhưng không đủ để chặn việc viết impl-plan

1. Repo hiện chưa có quy ước package naming/backend module naming cụ thể ngoài architecture overview.
2. Repo chưa có cấu hình PostgreSQL local/test hiện hữu, nên implementation sẽ phải bổ sung hoặc tài liệu hóa rõ cách chạy verification.
3. Repo chưa có frontend test runner, nên bằng chứng cho FE state transitions sẽ dựa vào lint/build/manual verification trừ khi ticket sau bổ sung test layer.
4. Cách chính xác để materialize `V3__seed_dev_test_users` trong Flyway cần được chốt sớm ở bước implementation để tránh drift giữa SQL migration intent và Java hashing requirement.

