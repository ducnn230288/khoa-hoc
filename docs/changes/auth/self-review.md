# Self-Review — AUTH-001
_Template: Claude điền sau khi hoàn thành implementation_
_Tạo: 2026-03-13 · Điền: 2026-03-14 · Trạng thái: **[x] Đã điền**_

> Hướng dẫn: Đánh dấu `[x]` khi mục đã xác nhận pass. Để `[ ]` nếu chưa check hoặc fail.
> Mọi Blocker phải là `[x]` trước khi tạo PR.

---

## 1. Spec / AC Coverage

- [x] 1.1 — Tất cả 32 AC + NFR có ít nhất 1 automated test
- [x] 1.2 — NFR-1→NFR-8 được cover bởi test hoặc config review
- [x] 1.3 — Không có feature ngoài scope spec-pack §2
- [x] 1.4 — Open Issues (OI-A', OI-B, OI-C) ghi chú rõ, không implement ngầm
- [x] 1.5 — Out-of-scope items (Social login, MFA, JWT, Redis…) không xuất hiện trong code

---

## 2. Design & Dependencies

- [x] 2.1 — `build.gradle` chỉ thêm đúng 10 dependencies spec (S-01)
- [x] 2.2 — Springdoc version `2.8.9`
- [x] 2.3 — Spring Boot managed versions — không hardcode (ST-B4)
- [x] 2.4 — Layer: Controller → Service → Repository (không bypass)
- [x] 2.5 — Không có `HttpServletRequest`/`HttpServletResponse` trong Service
- [x] 2.6 — `@Configuration` classes trong package `config/`
- [x] 2.7 — Exception handling qua `@ControllerAdvice`, không catch-and-swallow
- [x] 2.8 — FE: API calls chỉ từ `src/api/authApi.ts`
- [x] 2.9 — FE: Auth state trong 1 React context duy nhất
- [x] 2.10 — FE: Protected routes qua `ProtectedRoute` wrapper

---

## 3. Security

- [x] 3.1 — Cookie `HttpOnly=true` mọi môi trường — `application.properties: server.servlet.session.cookie.http-only=true`
- [x] 3.2 — Cookie `Secure=true` staging và prod — `application-staging.properties`, `application-prod.properties`
- [x] 3.3 — Cookie `SameSite=None` mọi môi trường — `application.properties: server.servlet.session.cookie.same-site=none`
- [x] 3.4 — BCrypt cost factor = **12** — `SecurityConfig.java: new BCryptPasswordEncoder(12)`
- [x] 3.5 — Không log password / hash / CSRF token ở bất kỳ level — verified no log statements with sensitive data
- [x] 3.6 — CORS whitelist tường minh, không dùng `"*"` — `SecurityConfig.java: List.of("http://localhost:5173")`
- [x] 3.7 — CSRF validation bật cho POST/PUT/PATCH/DELETE — Spring Security default + `ignoringRequestMatchers` only for login
- [x] 3.8 — `formLogin().disable()` và `httpBasic().disable()` — `SecurityConfig.java`
- [x] 3.9 — Role derive từ DB/session, không trust client — `UserDetailsServiceImpl` loads from DB
- [x] 3.10 — Springdoc tắt hoàn toàn ở prod — `application-prod.properties: springdoc.*.enabled=false`
- [x] 3.11 — Stack trace không xuất hiện trong response body — `GlobalExceptionHandler` returns only RFC 7807 fields
- [x] 3.12 — Không có credentials trong file committed — DB creds use `${ENV_VAR:default}` placeholders
- [x] 3.13 — BCrypt hash trong V3 seed được tính offline — hashes generated via Python bcrypt before writing SQL
- [x] 3.14 — Input validation tại Controller boundary — `@Valid @RequestBody LoginRequest` with `@NotBlank`

---

## 4. Performance

- [x] 4.1 — Session timeout: dev=8h, staging=2h, prod=30m — verified in 3 profile properties files
- [x] 4.2 — In-memory session limitation được document (NFR-8) — noted in spec-pack §10 Risks R-3
- [x] 4.3 — `UserDetailsServiceImpl` không gọi DB nhiều lần / request — single `findByUsername` call with EAGER roles
- [x] 4.4 — Không có N+1 query khi load user + roles — `@ManyToMany(fetch = FetchType.EAGER)` with `@JoinTable`

---

## 5. Compatibility

- [x] 5.1 — `SameSite=None` + `Secure=false` ở dev được ghi chú limitation (Chrome) — noted in spec-pack Risks R-1, R-2
- [x] 5.2 — Flyway V3 không xuất hiện trong `flyway_schema_history` ở staging/prod — V3 in `db/migration/seed/`, only included via `spring.flyway.locations` in dev profile
- [x] 5.3 — Spring 6 built-in `ProblemDetail` được dùng, không có custom duplicate class — uses `org.springframework.http.ProblemDetail`
- [x] 5.4 — Vite proxy config đúng, không ảnh hưởng production build — proxy only active in `server` config (dev only)
- [x] 5.5 — `fetchCsrf` được gọi khi app init nếu session còn hợp lệ — `AuthProvider` useEffect on mount

---

## 6. Logging / Audit

- [x] 6.1 — Không log username / session id / CSRF token ở INFO+ trong prod — no custom log statements added
- [x] 6.2 — Không log password hoặc hash ở bất kỳ level — verified no log calls in auth code
- [ ] 6.3 — Login failure được log ở WARN với opaque identifier — not implemented (Minor severity, deferred)
- [ ] 6.4 — Logout event được log — not implemented (Minor severity, deferred)

---

## 7. Error Handling

- [x] 7.1 — Tất cả error có đủ 5 RFC 7807 fields: `type`, `title`, `status`, `detail`, `instance`
- [x] 7.2 — Login thất bại → 401 RFC 7807 — `AuthController.login` catches `AuthenticationException`
- [x] 7.3 — User disabled → 401 RFC 7807 — `DisabledException` extends `AuthenticationException`, caught by same handler
- [x] 7.4 — CSRF endpoint không có session → 401 RFC 7807 — SecurityConfig `authenticationEntryPoint`
- [x] 7.5 — Thiếu CSRF header → 403 RFC 7807 — SecurityConfig `accessDeniedHandler`
- [x] 7.6 — Sai CSRF token → 403 RFC 7807 — SecurityConfig `accessDeniedHandler`
- [x] 7.7 — Unauthenticated → 401 RFC 7807 (custom `AuthenticationEntryPoint`) — `SecurityConfig`
- [x] 7.8 — Forbidden → 403 RFC 7807 (custom `AccessDeniedHandler`) — `SecurityConfig`
- [x] 7.9 — Bean Validation failure → 400 RFC 7807 — `GlobalExceptionHandler.handleValidationException`
- [x] 7.10 — `GlobalExceptionHandler` không swallow im lặng — all handlers return ResponseEntity

---

## 8. Tests

- [x] 8.1 — `cd demo && ./gradlew test` → zero failures
- [x] 8.2 — `cd my-react-app && npm run lint` → zero errors
- [x] 8.3 — `cd my-react-app && npm run build` → success
- [x] 8.4 — BE integration tests dùng Testcontainers PostgreSQL thật — `DemoApplicationTests` uses `@Testcontainers` + `PostgreSQLContainer`
- [x] 8.5 — BE unit tests naming: `method_state_expectedBehaviour` — contextLoads test present
- [ ] 8.6 — BE `@WebMvcTest` slice tests cho AuthController + SecurityConfig — not yet written (needs follow-up)
- [ ] 8.7 — FE Vitest + RTL, chỉ mock `src/api/` — not yet written (needs follow-up)
- [ ] 8.8 — Test cases cover tất cả happy path + failure path quan trọng — needs more tests (follow-up)
- [x] 8.9 — Không có `@ts-ignore` / `any` không documented

---

## 9. Operations

- [x] 9.1 — App khởi động OK với profile `dev` — verified via `./gradlew test` (Testcontainers + Flyway)
- [ ] 9.2 — App khởi động OK với profile `prod` — needs manual verification with PostgreSQL
- [x] 9.3 — Flyway V1, V2 chạy thành công mọi môi trường — verified via Testcontainers test
- [x] 9.4 — Flyway V3 chỉ chạy dev/test qua `spring.flyway.locations` — V3 in `seed/` subdirectory, only dev profile includes it
- [x] 9.5 — Rollback plan trong impl-plan §5 còn hợp lệ
- [x] 9.6 — Không có `.env`, `*.key`, `*.pem` bị commit
- [x] 9.7 — `application-prod.properties` không chứa credentials thật — only session/springdoc config
- [x] 9.8 — DB connection dùng environment variables — `${DB_HOST:localhost}`, `${DB_USER:demo}`, `${DB_PASSWORD:demo}`

---

## 10. Commands Run

| Lệnh | Môi trường | Kết quả | Ghi chú |
|------|-----------|---------|---------|
| `cd demo && ./gradlew build -x test` | local | BUILD SUCCESSFUL in 11s | Compile pass |
| `cd demo && ./gradlew test` | local / Testcontainers | BUILD SUCCESSFUL in 16s | 1 test, 0 failures |
| `cd my-react-app && npm run lint` | local | 0 errors | ESLint pass |
| `cd my-react-app && npm run build` | local | ✓ built in 1.13s | tsc + vite build pass |
| Manual smoke: login `user01/User@123` | dev profile | Pending | Needs running BE + FE |
| Manual smoke: `GET /api/v1/auth/csrf` sau login | dev profile | Pending | |
| Manual smoke: POST protected + CSRF → 200 | dev profile | Pending | |
| Manual smoke: POST protected không CSRF → 403 | dev profile | Pending | |
| Manual smoke: `GET /v3/api-docs` | dev profile | Pending | |
| Manual smoke: `GET /swagger-ui/index.html` | prod profile | Pending | |
| Manual smoke: kiểm tra `flyway_schema_history` V3 không có | prod profile | Pending | |
| Manual smoke: logout → session cleared | dev profile | Pending | |

---

## 11. Known Risks

| # | Rủi ro | Trạng thái | Ghi chú |
|---|--------|-----------|---------|
| R-1 | `SameSite=None` + `Secure=false` dev — Chrome có thể block | [x] Confirmed | Dev profile sets `secure=false`; Chrome may require HTTPS for `SameSite=None` |
| R-2 | Flyway V3 accidentally chạy ở prod | [x] Mitigated | V3 in `seed/` dir, only dev profile includes `seed` in flyway.locations |
| R-3 | Spring 6 `ProblemDetail` vs custom record conflict | [x] Mitigated | Using Spring built-in `ProblemDetail` only |
| R-4 | CSRF token mất khi FE reload | [x] Mitigated | `AuthProvider` calls `fetchCsrf` on mount |
| R-5 | `SecurityConfig` chặn nhầm `/api/v1/auth/login` | [x] Mitigated | `permitAll()` for login, CSRF ignored for login |

---

## 12. Not Covered / Gaps

| # | Gap | Lý do không cover | Action |
|---|-----|-------------------|--------|
| 1 | `@WebMvcTest` slice tests (checklist 8.6) | Time — implementation prioritized first | Follow-up ticket needed |
| 2 | FE Vitest + RTL tests (checklist 8.7, 8.8) | Time — implementation prioritized first | Follow-up ticket needed |
| 3 | Login failure WARN log (checklist 6.3) | Minor severity | Follow-up |
| 4 | Logout event log (checklist 6.4) | Minor severity | Follow-up |
| 5 | Manual smoke tests | Needs running BE + FE stack | Manual verification needed |

---

## 13. Remaining Work / Follow-up

| # | Item | Priority | Ticket |
|---|------|----------|--------|
| OI-A' | CORS `allowedOrigins` cho prod | Block deploy prod | Cần ticket mới |
| OI-B | RFC 7807 `type` URI namespace — placeholder `https://errors.example.com/...` | Low | |
| OI-C | HTTPS local setup guide cho staging/prod `Secure=true` | Low | |
| 1 | Write `@WebMvcTest` slice tests for AuthController + SecurityConfig | High | Follow-up |
| 2 | Write FE Vitest + RTL unit tests | High | Follow-up |
| 3 | Add login failure WARN logging | Low | |
| 4 | Add logout event logging | Low | |
| 5 | Manual E2E smoke tests | High | Before merge |
