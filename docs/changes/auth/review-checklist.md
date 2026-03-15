# Review Checklist — AUTH-001

_Tạo: 2026-03-13 · Trạng thái: Ready for review_

> Severity: **Blocker** = must fix before merge · **Major** = must fix before deploy · **Minor** = should fix, can defer

---

## 1. Spec / AC Coverage

| #   | Item                                                                            | Severity | Notes                    |
| --- | ------------------------------------------------------------------------------- | -------- | ------------------------ |
| 1.1 | Tất cả 36 AC (AC-1 → AC-36) + AC-NFR-1 đã có ít nhất 1 automated test           | Blocker  | Xem AC Mapping Table §10 |
| 1.2 | Tất cả NFR (NFR-1→NFR-8) đã được cover bởi test hoặc config review              | Blocker  |                          |
| 1.3 | Không có feature nào được implement ngoài scope spec-pack §2                    | Blocker  | SEC S-4                  |
| 1.4 | Các Open Issues (OI-A', OI-B, OI-C) đã được ghi chú rõ, không bị implement ngầm | Major    |                          |
| 1.5 | Out-of-scope items (Social login, MFA, JWT, Redis…) không xuất hiện trong code  | Major    |                          |

---

## 2. Design & Dependencies

| #    | Item                                                                                            | Severity | Notes |
| ---- | ----------------------------------------------------------------------------------------------- | -------- | ----- |
| 2.1  | `build.gradle` chỉ thêm 10 dependencies đúng spec (S-01)                                        | Major    |       |
| 2.2  | Phiên bản Springdoc: `2.8.9` đúng như impl-plan S-01                                            | Major    |       |
| 2.3  | Spring Boot managed versions được dùng cho tất cả deps (trừ Springdoc) — không hardcode version | Minor    | ST-B4 |
| 2.4  | Layer dependency: Controller → Service → Repository (không bypass)                              | Blocker  | AR-B1 |
| 2.5  | Không có `HttpServletRequest`/`HttpServletResponse` trong Service                               | Major    | AR-B3 |
| 2.6  | `@Configuration` classes trong package `config/`                                                | Minor    | AR-B5 |
| 2.7  | Exception handling qua `@ControllerAdvice` trong `exception/` — không catch-and-swallow         | Major    | AR-B6 |
| 2.8  | FE: API calls chỉ từ `src/api/authApi.ts`, không gọi trực tiếp từ Page/Component                | Blocker  | AR-F1 |
| 2.9  | FE: Auth state trong 1 React context duy nhất, không duplicate                                  | Major    | AR-F4 |
| 2.10 | FE: Protected routes qua `ProtectedRoute` component, không check ad-hoc                         | Major    | AR-F5 |
| 2.11 | FE: Sau login thành công + lấy CSRF token, frontend navigate sang route `/`                     | Major    | AC-35 |
| 2.12 | FE: Nếu user đã authenticated truy cập `/login`, frontend redirect về `/`                       | Major    | AC-36 |

---

## 3. Security

| #    | Item                                                                      | Severity | Notes               |
| ---- | ------------------------------------------------------------------------- | -------- | ------------------- |
| 3.1  | Cookie `HttpOnly=true` mọi môi trường                                     | Blocker  | SEC-2, AC-3         |
| 3.2  | Cookie `Secure=true` ở staging và prod                                    | Blocker  | SEC-2, AC-5         |
| 3.3  | Cookie `SameSite=Lax` cho `dev`; `SameSite=None` cho `staging` và `prod`  | Blocker  | AC-4, AC-34         |
| 3.4  | BCrypt cost factor = 12 (không thấp hơn, không cao hơn)                   | Blocker  | SEC-3, NFR-1        |
| 3.5  | Không log password, password hash, CSRF token ở bất kỳ level nào          | Blocker  | SEC-4, NFR-2        |
| 3.6  | CORS: `allowedOrigins` là whitelist tường minh, không dùng `"*"`          | Blocker  | SEC-5, NFR-3, AC-31 |
| 3.7  | CSRF validation bật cho tất cả mutating endpoints (POST/PUT/PATCH/DELETE) | Blocker  | SEC-1, AC-12        |
| 3.8  | `formLogin().disable()` và `httpBasic().disable()` trong SecurityConfig   | Major    |                     |
| 3.9  | Role không trust từ client — derive từ DB/session                         | Blocker  | SEC-7               |
| 3.10 | Springdoc tắt hoàn toàn ở prod (`/v3/api-docs` → 404)                     | Blocker  | SEC-8, AC-21        |
| 3.11 | Stack trace không bao giờ xuất hiện trong response body                   | Blocker  | SEC-9               |
| 3.12 | Không có credentials/keys trong file được commit                          | Blocker  | SEC-10              |
| 3.13 | BCrypt hash trong V3 seed được tính offline — không sinh trong SQL        | Blocker  | impl-plan S-05      |
| 3.14 | Input validation tại Controller boundary trước khi đến Service            | Major    | SEC-6               |

---

## 4. Performance

| #   | Item                                                            | Severity | Notes        |
| --- | --------------------------------------------------------------- | -------- | ------------ |
| 4.1 | Session timeout đúng per-profile: dev=8h, staging=2h, prod=30m  | Major    | AC-8, NFR-4  |
| 4.2 | In-memory session — giới hạn single-instance được document      | Minor    | NFR-8, AC-29 |
| 4.3 | `UserDetailsServiceImpl` không gọi DB nhiều lần trong 1 request | Minor    |              |
| 4.4 | Không có N+1 query khi load user + roles                        | Minor    |              |

---

## 5. Compatibility

| #   | Item                                                                                                                                                             | Severity | Notes                            |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | -------------------------------- |
| 5.1 | Dev dùng `SameSite=Lax` (không phải None) — Chromium chấp nhận cookie trên local HTTP qua Vite proxy mà không cần HTTPS                                          | Major    | AC-34, AC-4; R-1 từ impl-plan §4 |
| 5.2 | Flyway V3 không xuất hiện trong `flyway_schema_history` khi chạy staging/prod profile                                                                            | Blocker  | AC-28, R-2                       |
| 5.3 | Spring 6 built-in `ProblemDetail` được dùng — không tạo custom duplicate class                                                                                   | Major    | R-3, impl-plan S-09              |
| 5.4 | Vite proxy `/api → http://localhost:8080` cấu hình đúng, không ảnh hưởng production build                                                                        | Major    | impl-plan S-13                   |
| 5.5 | FE: `fetchCsrf` được gọi khi app init nếu session còn hợp lệ (CSRF token không mất sau reload)                                                                   | Major    | R-4                              |
| 5.6 | Sau login thành công + lấy được CSRF token, FE gọi `GET /api/v1/auth/csrf` trả 200 OK trên HTTP thực tế qua Vite proxy                                           | Major    | AC-33                            |
| 5.7 | Cookie `JSESSIONID` được Chromium chấp nhận trên `http://localhost:5173` sau login (không cần HTTPS) nhờ `SameSite=Lax` + `Secure=false` (đúng môi trường `dev`) | Major    | AC-34                            |

---

## 6. Logging / Audit

| #   | Item                                                                                        | Severity | Notes        |
| --- | ------------------------------------------------------------------------------------------- | -------- | ------------ |
| 6.1 | Không log username, session id, CSRF token ở INFO level trở lên trong prod                  | Major    | SEC-4, NFR-2 |
| 6.2 | Không log password hoặc password hash ở bất kỳ level                                        | Blocker  | SEC-4        |
| 6.3 | Login failure được log ở WARN với user id / opaque identifier (không log password nhập vào) | Minor    |              |
| 6.4 | Logout event được log                                                                       | Minor    |              |

---

## 7. Error Handling

| #    | Item                                                                                           | Severity | Notes          |
| ---- | ---------------------------------------------------------------------------------------------- | -------- | -------------- |
| 7.1  | Tất cả error response có đủ 5 fields RFC 7807: `type`, `title`, `status`, `detail`, `instance` | Blocker  | AC-32, NFR-7   |
| 7.2  | Login thất bại → 401 RFC 7807 (type: `invalid-credentials`)                                    | Blocker  | AC-6           |
| 7.3  | User disabled → 401 RFC 7807                                                                   | Blocker  | AC-7           |
| 7.4  | CSRF endpoint không có session → 401 RFC 7807                                                  | Blocker  | AC-11          |
| 7.5  | Mutating request thiếu CSRF header → 403 RFC 7807                                              | Blocker  | AC-13          |
| 7.6  | Mutating request CSRF token sai → 403 RFC 7807                                                 | Blocker  | AC-14          |
| 7.7  | Unauthenticated access protected endpoint → 401 RFC 7807 (custom `AuthenticationEntryPoint`)   | Blocker  | impl-plan S-08 |
| 7.8  | Unauthorized (forbidden) access → 403 RFC 7807 (custom `AccessDeniedHandler`)                  | Major    |                |
| 7.9  | Bean Validation failure → 400 RFC 7807 (`MethodArgumentNotValidException`)                     | Major    | impl-plan S-11 |
| 7.10 | `GlobalExceptionHandler` không bao giờ swallow exception im lặng                               | Major    | AR-B6          |

---

## 8. Tests

| #   | Item                                                                                            | Severity | Notes         |
| --- | ----------------------------------------------------------------------------------------------- | -------- | ------------- |
| 8.1 | `cd demo && ./gradlew test` pass zero failures                                                  | Blocker  | TST-9         |
| 8.2 | `cd my-react-app && npm run lint` pass zero errors                                              | Blocker  | TST-10, ST-F5 |
| 8.3 | `cd my-react-app && npm run build` thành công                                                   | Blocker  | TST-10        |
| 8.4 | BE integration tests dùng Testcontainers PostgreSQL thật — không mock DB                        | Blocker  | TST-2         |
| 8.5 | BE unit tests dùng Mockito (không `@SpringBootTest`) — naming: `method_state_expectedBehaviour` | Major    | TST-3, TST-5  |
| 8.6 | BE `@WebMvcTest` slice tests cho `AuthController` và `SecurityConfig`                           | Major    | TST-4         |
| 8.7 | FE unit tests dùng Vitest + React Testing Library — chỉ mock `src/api/`                         | Major    | TST-6, TST-7  |
| 8.8 | Test cases cho tất cả happy path và failure path quan trọng (xem AC Mapping Table)              | Blocker  | TST-1         |
| 8.9 | Không có `@ts-ignore` hoặc `any` không có documented reason trong test files                    | Minor    | ST-F3         |

---

## 9. Operations

| #   | Item                                                                                    | Severity | Notes          |
| --- | --------------------------------------------------------------------------------------- | -------- | -------------- |
| 9.1 | Application khởi động thành công với profile `dev`                                      | Blocker  |                |
| 9.2 | Application khởi động thành công với profile `prod`                                     | Blocker  |                |
| 9.3 | Flyway migration V1, V2 chạy thành công mọi môi trường                                  | Blocker  | AC-25, AC-26   |
| 9.4 | Flyway V3 chỉ chạy ở dev/test — cơ chế là `spring.flyway.locations` per profile         | Blocker  | AC-27, AC-28   |
| 9.5 | Rollback plan được document trong impl-plan §5                                          | Minor    |                |
| 9.6 | Không có file `.env`, `*.key`, `*.pem` bị commit                                        | Blocker  | SEC-10, S-1    |
| 9.7 | `application-prod.properties` không chứa credentials thật                               | Blocker  | SEC-10         |
| 9.8 | DB connection dùng environment variables (`${DB_HOST}`, `${DB_USER}`, `${DB_PASSWORD}`) | Major    | impl-plan S-02 |

---

## 10. AC Mapping Table

> Mỗi AC cần ít nhất 1 checklist item xác nhận nó đã được implement đúng.

| AC    | Mô tả ngắn                                                       | Checklist item(s) |
| ----- | ---------------------------------------------------------------- | ----------------- |
| AC-1  | `POST /api/v1/auth/login` tồn tại                                | 8.1, 8.8          |
| AC-2  | Login thành công → Set-Cookie JSESSIONID                         | 7.2, 8.4, 8.8     |
| AC-3  | Cookie HttpOnly=true                                             | 3.1               |
| AC-4  | Cookie SameSite=None                                             | 3.3               |
| AC-5  | Cookie Secure=true staging/prod                                  | 3.2               |
| AC-6  | Login thất bại → 401 RFC 7807, no session                        | 7.2, 8.8          |
| AC-7  | enabled=false → 401                                              | 7.3, 8.5          |
| AC-8  | Session timeout per environment                                  | 4.1               |
| AC-9  | `GET /api/v1/auth/csrf` → đúng fields                            | 7.4, 8.8          |
| AC-10 | CSRF endpoint chỉ khi session hợp lệ                             | 7.4, 8.6          |
| AC-11 | CSRF no session → 401                                            | 7.4, 8.6          |
| AC-12 | Mutating API yêu cầu CSRF                                        | 3.7, 8.6          |
| AC-13 | Thiếu CSRF → 403                                                 | 7.5, 8.6          |
| AC-14 | Sai CSRF → 403                                                   | 7.6, 8.6          |
| AC-15 | CSRF qua header X-CSRF-TOKEN                                     | 3.7, 8.6          |
| AC-16 | `POST /api/v1/auth/logout` tồn tại                               | 8.1, 8.8          |
| AC-17 | Logout invalidates session                                       | 8.4, 8.8          |
| AC-18 | Logout clears cookie                                             | 8.4, 8.8          |
| AC-19 | `/v3/api-docs` available                                         | 9.1               |
| AC-20 | Swagger UI dev/staging                                           | 9.1               |
| AC-21 | Swagger UI disabled prod                                         | 3.10, 9.2         |
| AC-22 | Auth endpoints documented                                        | 8.1               |
| AC-23 | Kết nối PostgreSQL                                               | 9.1, 9.2          |
| AC-24 | User data trong PostgreSQL                                       | 9.3               |
| AC-25 | Flyway tự động chạy startup                                      | 9.3               |
| AC-26 | V2 tạo đúng schema                                               | 9.3               |
| AC-27 | V3 seed dev/test                                                 | 9.4               |
| AC-28 | V3 không chạy staging/prod                                       | 5.2, 9.4          |
| AC-29 | In-memory session, không Redis                                   | 1.5, 4.2          |
| AC-30 | CORS allow localhost:5173                                        | 3.6               |
| AC-31 | Không CORS wildcard                                              | 3.6               |
| AC-32 | Error response RFC 7807                                          | 7.1, 8.8          |
| AC-33 | Login → GET /csrf thành công trên HTTP thật (dev proxy)          | 5.6, 8.4, 8.8     |
| AC-34 | Cookie dev Chromium-compatible qua Vite proxy (Lax, không HTTPS) | 3.3, 5.1, 5.7     |
| AC-35 | Redirect sang `/` sau login thành công + CSRF                    | 2.11, 8.7, 8.8    |
| AC-36 | Redirect về `/` nếu user đã authenticated truy cập `/login`      | 2.12, 8.7, 8.8    |
| NFR-1 | BCrypt cost 12                                                   | 3.4               |
| NFR-2 | Không log password/hash/CSRF                                     | 6.1, 6.2          |
| NFR-3 | CORS whitelist, no wildcard                                      | 3.6               |
| NFR-4 | Session timeout per profile                                      | 4.1               |
| NFR-5 | V3 seed chỉ dev/test                                             | 5.2, 9.4          |
| NFR-6 | Swagger off production                                           | 3.10              |
| NFR-7 | RFC 7807 error format                                            | 7.1               |
| NFR-8 | In-memory session limitation documented                          | 4.2               |
