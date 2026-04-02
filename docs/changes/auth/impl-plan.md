# Kế hoạch triển khai — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng)

> Tạo ngày: 2026-04-02  
> Được suy ra từ: `docs/changes/auth/spec-pack.md`  
> Nhánh: `feature/ticket-1-auth-session-csrf`

---

## 1. Cách tiếp cận

### So sánh phương án

| Tiêu chí     | Option A — Spring Security native session + custom CSRF endpoint (CHỌN) | Option B — CookieCsrfTokenRepository         |
| ------------ | ----------------------------------------------------------------------- | -------------------------------------------- |
| Phù hợp spec | Khớp hoàn toàn: CSRF token trả qua `GET /api/v1/auth/csrf`              | CSRF token nằm trong cookie, không khớp spec |
| Phức tạp     | Thấp — dùng cơ chế mặc định của Spring Security                         | Trung bình — cần FE tự đọc cookie            |
| Bảo mật      | Tốt — CSRF token chỉ accessible qua session-protected API               | Tốt nhưng token access rộng hơn cần thiết    |
| Rủi ro       | Thấp — Spring Security đảm bảo session lifecycle                        | Thấp nhưng không khớp contract               |

**Phương án được chọn: Option A**

### Mô tả cách tiếp cận

1. **Session management**: Dùng Spring Security `HttpSession` in-memory (Tomcat embedded) — không cần Redis hay Spring Session. Session timeout được cấu hình qua `server.servlet.session.timeout` per-profile.

2. **CSRF**: Dùng `HttpSessionCsrfTokenRepository` mặc định của Spring Security. CSRF token được lưu trong session và trả về qua `GET /api/v1/auth/csrf` dạng JSON `{csrfToken, headerName, parameterName}`. Client gửi token qua header `X-CSRF-TOKEN`.

3. **Authentication**: REST controller nhận `POST /api/v1/auth/login`, xác thực bằng `AuthenticationManager`, tạo session sau khi thành công (`SecurityContextHolder` + `HttpSessionSecurityContextRepository`).

4. **CORS**: Cấu hình `CorsConfigurationSource` bean — allow origin `http://localhost:5173`, `allowCredentials=true`, không dùng wildcard `*`.

5. **Cookie policy**: Cấu hình `server.servlet.session.cookie.*` — `httpOnly=true`, `sameSite=NONE`, `path=/`, `secure=true` (staging/prod), `secure=false` (dev).

6. **Flyway locations**: Tách thành hai location — `classpath:db/migration` (common, tất cả env) và `classpath:db/seed` (chỉ profile `dev` và `test`).

7. **Error envelope**: `@ExceptionHandler` + `AuthenticationEntryPoint` + `AccessDeniedHandler` trả chuẩn `{code, message, path, timestamp}`.

8. **Springdoc**: Bật ở `dev` và `staging`, tắt ở `prod` qua property `springdoc.api-docs.enabled`.

---

## 2. Phạm vi ảnh hưởng

### Các tệp / mô-đun cần thay đổi

| #   | Tệp                                                               | Loại thay đổi | Ghi chú                                                                  |
| --- | ----------------------------------------------------------------- | ------------- | ------------------------------------------------------------------------ |
| 1   | `demo/build.gradle`                                               | Sửa           | Thêm dependencies: security, jpa, postgresql, flyway, springdoc          |
| 2   | `demo/src/main/resources/application.properties`                  | Sửa           | Thêm datasource, flyway common location, springdoc base path             |
| 3   | `demo/src/main/resources/application-dev.properties`              | Thêm          | Session timeout 8h, cookie non-secure, Springdoc bật, seed location      |
| 4   | `demo/src/main/resources/application-staging.properties`          | Thêm          | Session timeout 2h, cookie secure, Springdoc bật, không có seed location |
| 5   | `demo/src/main/resources/application-prod.properties`             | Thêm          | Session timeout 30m, cookie secure, Springdoc tắt                        |
| 6   | `demo/src/main/resources/db/migration/V1__create_auth_tables.sql` | Thêm          | Tạo bảng `users`, `roles`, `user_roles`                                  |
| 7   | `demo/src/main/resources/db/seed/V1__seed_dev_test_users.sql`     | Thêm          | Seed admin + user01 (chỉ non-prod)                                       |
| 8   | `…/config/SecurityConfig.java`                                    | Thêm          | Spring Security filter chain, CORS, session, CSRF, error handlers        |
| 9   | `…/controller/AuthController.java`                                | Thêm          | Login, CSRF token, Logout endpoints                                      |
| 10  | `…/entity/User.java`                                              | Thêm          | JPA entity bảng `users`                                                  |
| 11  | `…/entity/Role.java`                                              | Thêm          | JPA entity bảng `roles`                                                  |
| 12  | `…/repository/UserRepository.java`                                | Thêm          | Spring Data JPA                                                          |
| 13  | `…/dto/LoginRequest.java`                                         | Thêm          | `{username, password}`                                                   |
| 14  | `…/dto/LoginResponse.java`                                        | Thêm          | `{authenticated, username, roles}`                                       |
| 15  | `…/dto/CsrfResponse.java`                                         | Thêm          | `{csrfToken, headerName, parameterName}`                                 |
| 16  | `…/dto/ErrorResponse.java`                                        | Thêm          | `{code, message, path, timestamp}`                                       |
| 17  | `…/service/UserDetailsServiceImpl.java`                           | Thêm          | Spring Security `UserDetailsService` impl                                |
| 18  | `…/exception/GlobalAuthExceptionHandler.java`                     | Thêm          | `AuthenticationEntryPoint`, `AccessDeniedHandler`, `@ControllerAdvice`   |
| 19  | `…/audit/AuditLogger.java`                                        | Thêm          | Structured audit log — login, logout, 401, 403, session expired          |
| 20  | `…/config/SpringdocConfig.java`                                   | Thêm          | Cấu hình OpenAPI per-env                                                 |
| 21  | `my-react-app/src/pages/LoginPage.tsx`                            | Thêm          | LoginUI theo wireframe spec                                              |
| 22  | `my-react-app/src/services/authService.ts`                        | Thêm          | login, logout, getCsrfToken                                              |
| 23  | `my-react-app/src/hooks/useAuth.ts`                               | Thêm          | Auth state + CSRF rehydration                                            |
| 24  | `my-react-app/src/App.tsx`                                        | Sửa           | Tích hợp LoginPage, routing, rehydration flow                            |

> `…` = `demo/src/main/java/com/example/demo`

### Các khu vực ảnh hưởng khác

| #   | Hạng mục      | Mức ảnh hưởng | Ghi chú                                                                     |
| --- | ------------- | ------------- | --------------------------------------------------------------------------- |
| 1   | Lược đồ DB    | Cao           | Tạo mới toàn bộ 3 bảng auth — không có bảng hiện hữu                        |
| 2   | Hợp đồng API  | Cao           | Tạo mới 3 endpoints: `/login`, `/csrf`, `/logout`                           |
| 3   | Cấu hình      | Cao           | 4 file properties mới, cấu hình CORS, session, Flyway locations             |
| 4   | Nhật ký       | Trung bình    | Thêm audit log cho auth events; không làm thay đổi logger hiện tại          |
| 5   | Quyền/Vai trò | Thấp          | Chỉ lưu và trả địa danh `roles` từ DB; chưa có RBAC guard trên business API |
| 6   | Thành phần FE | Trung bình    | Thêm Login page, auth service, useAuth hook; sửa App.tsx routing            |

---

## 3. Mã hiện có cần đọc trước khi bắt đầu

- [x] `demo/build.gradle` — xác nhận Spring Boot version (3.5.x), Java 25
- [x] `demo/src/main/resources/application.properties` — hiện chỉ có `spring.application.name`
- [x] `demo/src/main/java/com/example/demo/DemoApplication.java` — entry point, base package `com.example.demo`
- [x] `my-react-app/src/App.tsx` — cấu trúc hiện tại để biết nơi tích hợp
- [x] `my-react-app/package.json` — FE dependencies hiện tại

> Tất cả đã được đọc. Chưa có code auth nào — tất cả là tạo mới.

---

## 4. Các bước triển khai

> Nguyên tắc: 1 step = 1 đơn vị PR review được (thường 1–3 file, 1 concern rõ ràng).

### Nhóm A — Hạ tầng (Backend dependencies + config + DB)

| #   | Bước                                                                                                                                                                                                                                                                       | Tệp được chỉnh sửa               | Cách xác minh                                                    |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | ---------------------------------------------------------------- |
| A1  | Thêm dependencies vào `build.gradle`: `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `org.postgresql:postgresql`, `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`, `org.springdoc:springdoc-openapi-starter-webmvc-ui`              | `demo/build.gradle`              | `./gradlew dependencies` không lỗi; app compiles                 |
| A2  | Cập nhật `application.properties`: cấu hình datasource URL/username/password (từ env vars), `spring.jpa.hibernate.ddl-auto=validate`, Flyway common location `classpath:db/migration`, Springdoc base path                                                                 | `application.properties`         | `./gradlew build` (với DB không cần connect ở compile time)      |
| A3  | Tạo `application-dev.properties`: `server.servlet.session.timeout=8h`, `server.servlet.session.cookie.secure=false`, `server.servlet.session.cookie.same-site=none`, `spring.flyway.locations=classpath:db/migration,classpath:db/seed`, `springdoc.api-docs.enabled=true` | `application-dev.properties`     | Profile `dev` load đúng — xác nhận qua `@Value` hoặc log startup |
| A4  | Tạo `application-staging.properties`: `session.timeout=2h`, `cookie.secure=true`, `flyway.locations=classpath:db/migration` (không có seed), `springdoc.enabled=true`                                                                                                      | `application-staging.properties` | —                                                                |
| A5  | Tạo `application-prod.properties`: `session.timeout=30m`, `cookie.secure=true`, `flyway.locations=classpath:db/migration` (không có seed), `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`                                                        | `application-prod.properties`    | —                                                                |

### Nhóm B — Database migrations

| #   | Bước                                                                                                                                                                                                                                                                                              | Tệp được chỉnh sửa                              | Cách xác minh                                                                        |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- | ------------------------------------------------------------------------------------ |
| B1  | Tạo `V1__create_auth_tables.sql`: tạo bảng `users` (`id`, `username` unique not null, `password_hash` not null, `enabled` not null default true, `created_at`, `updated_at`), `roles` (`id`, `code` unique not null, `name`), `user_roles` (`user_id` FK→users, `role_id` FK→roles, PK composite) | `db/migration/V1__create_auth_tables.sql`       | Flyway chạy thành công trên DB trắng; `./gradlew test`                               |
| B2  | Tạo `V1__seed_dev_test_users.sql` trong `db/seed/`: insert user `admin` (BCrypt hash của `Admin@123`), `user01` (BCrypt hash của `User@123`), insert role `USER`, gán `user01` role `USER`; gán `admin` role `ADMIN`                                                                              | `db/seed/V1__seed_dev_test_users.sql`           | Chỉ chạy với profile `dev` hoặc `test`; profile `staging`/`prod` không thấy file này |
| B3  | Viết integration test xác nhận Flyway không nạp seed location khi profile là `staging`                                                                                                                                                                                                            | `DemoApplicationTests.java` hoặc test class mới | Test pass; startup với profile `staging` không có `admin`/`user01`                   |

### Nhóm C — JPA Entities + Repositories

| #   | Bước                                                                                                               | Tệp được chỉnh sửa                     | Cách xác minh                                                             |
| --- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------- | ------------------------------------------------------------------------- |
| C1  | Tạo `User.java` entity map bảng `users`; `Role.java` entity map bảng `roles`; quan hệ `@ManyToMany` (`user_roles`) | `entity/User.java`, `entity/Role.java` | `./gradlew build` không lỗi; schema validate pass với `ddl-auto=validate` |
| C2  | Tạo `UserRepository.java` với method `findByUsername(String username)`                                             | `repository/UserRepository.java`       | Compile pass; unit test với `@DataJpaTest`                                |

### Nhóm D — DTOs

| #   | Bước                                                                                                                                                                                                                                           | Tệp được chỉnh sửa | Cách xác minh |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ------------- |
| D1  | Tạo `LoginRequest.java` (`username`, `password`), `LoginResponse.java` (`authenticated`, `username`, `roles`), `CsrfResponse.java` (`csrfToken`, `headerName`, `parameterName`), `ErrorResponse.java` (`code`, `message`, `path`, `timestamp`) | `dto/*.java`       | Compile pass  |

### Nhóm E — Security config

| #   | Bước                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Tệp được chỉnh sửa                    | Cách xác minh                                                                  |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- | ------------------------------------------------------------------------------ |
| E1  | Tạo `UserDetailsServiceImpl.java`: implements `UserDetailsService`, dùng `UserRepository` để load user, throw `UsernameNotFoundException` nếu không tìm thấy hoặc disabled                                                                                                                                                                                                                                                                                                                                                   | `service/UserDetailsServiceImpl.java` | Unit test: load user hợp lệ, load user disabled, load user không tồn tại       |
| E2  | Tạo `SecurityConfig.java`: cấu hình `SecurityFilterChain` — (1) CORS bean với `http://localhost:5173` + `allowCredentials=true`, (2) CSRF với `HttpSessionCsrfTokenRepository`, (3) session creation `IF_REQUIRED`, session fixation protection, (4) `AuthenticationEntryPoint` trả error envelope 401, (5) `AccessDeniedHandler` trả error envelope 403, (6) permit path `/api/v1/auth/login`, (7) require authenticated cho `/api/v1/auth/csrf` và `/api/v1/auth/logout`, (8) không bật Spring Security default login page | `config/SecurityConfig.java`          | Integration test: unauthenticated→401 envelope, CORS preflight trả đúng header |
| E3  | Cấu hình `AuthenticationManager` bean trong `SecurityConfig.java` dùng `DaoAuthenticationProvider` với `UserDetailsServiceImpl` và `BCryptPasswordEncoder`                                                                                                                                                                                                                                                                                                                                                                   | `config/SecurityConfig.java`          | Unit test: authenticate đúng/sai/disabled                                      |

### Nhóm F — Controllers

| #   | Bước                                                                                                                                                                                                                                                          | Tệp được chỉnh sửa               | Cách xác minh                                                                       |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | ----------------------------------------------------------------------------------- |
| F1  | Tạo `AuthController.java` — handler `POST /api/v1/auth/login`: nhận `LoginRequest`, gọi `AuthenticationManager.authenticate()`, tạo session (`HttpSession`, `SecurityContextHolder`), trả `LoginResponse` 200                                                 | `controller/AuthController.java` | IT: login thành công → 200 + session cookie + body `{authenticated,username,roles}` |
| F2  | Thêm handler `GET /api/v1/auth/csrf` vào `AuthController.java`: inject `CsrfToken` từ request attribute, trả `CsrfResponse` 200; nếu không có session → Spring Security trả 401 trước khi vào controller                                                      | `controller/AuthController.java` | IT: có session → 200 + `csrfToken`; không có session → 401 từ `EntryPoint`          |
| F3  | Thêm handler `POST /api/v1/auth/logout` vào `AuthController.java`: có session + CSRF hợp lệ (guard bởi Spring Security filter), gọi `SecurityContextHolder.clearContext()`, `session.invalidate()`, xóa session cookie trong response, trả `{"success":true}` | `controller/AuthController.java` | IT: logout → 200 + cookie cleared; không có session → 401; CSRF sai → 403           |

### Nhóm G — Error handling + Audit logging

| #   | Bước                                                                                                                                                                                                                                                                                                                                                                                                                 | Tệp được chỉnh sửa                          | Cách xác minh                                                                           |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- | --------------------------------------------------------------------------------------- |
| G1  | Tạo `GlobalAuthExceptionHandler.java`: implements `AuthenticationEntryPoint` (sinh `ErrorResponse` 401 với code `AUTH_SESSION_REQUIRED` hoặc `AUTH_SESSION_EXPIRED`), implements `AccessDeniedHandler` (sinh `ErrorResponse` 403 code `AUTH_CSRF_INVALID`); `@ExceptionHandler` trong `@ControllerAdvice` cho `BadCredentialsException`→401+`AUTH_INVALID_CREDENTIALS`, `DisabledException`→401+`AUTH_USER_DISABLED` | `exception/GlobalAuthExceptionHandler.java` | UT: từng code phát sinh đúng HTTP status và JSON body; IT: toàn bộ error flow           |
| G2  | Tạo `AuditLogger.java`: log các event sau (dùng SLF4J, không log plaintext secret): `LOGIN_SUCCESS(username)`, `LOGIN_FAILURE(username, reason)`, `LOGOUT_SUCCESS(username)`, `ACCESS_DENIED_UNAUTHENTICATED(path)`, `ACCESS_DENIED_SESSION_EXPIRED(path)`, `ACCESS_DENIED_CSRF(path)`                                                                                                                               | `audit/AuditLogger.java`                    | UT: mỗi event log đúng level và message; kiểm tra không log password/session/csrf token |

### Nhóm H — Springdoc

| #   | Bước                                                                                                                                                                                         | Tệp được chỉnh sửa            | Cách xác minh                                                                                  |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------- |
| H1  | Tạo `SpringdocConfig.java`: cấu hình `OpenAPI` bean với mô tả endpoints và security scheme (session cookie + CSRF header); Springdoc bật/tắt theo property từ `application-{env}.properties` | `config/SpringdocConfig.java` | IT: `GET /v3/api-docs` trả 200 ở dev; ở prod cấu hình `springdoc.api-docs.enabled=false` = 404 |

### Nhóm I — Tests

| #   | Bước                                                                                                                                                           | Tệp được chỉnh sửa                                      | Cách xác minh                                |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- | -------------------------------------------- |
| I1  | Viết unit tests cho `UserDetailsServiceImpl` (load user/disabled/not found), `GlobalAuthExceptionHandler` (từng error code), `AuditLogger` (không leak secret) | `test/…/service/`, `test/…/exception/`, `test/…/audit/` | `./gradlew test` pass                        |
| I2  | Viết integration tests cho auth flow: AC-auth-1,4,5,7,8,9,10,11,12,13,14,15,22,23,24,26,27                                                                     | `test/…/controller/AuthControllerIT.java`               | IT pass; spring-security-test dùng `MockMvc` |
| I3  | Viết integration test cho session timeout (mock clock hoặc set timeout ngắn trong test profile)                                                                | `test/…/AuthSessionTimeoutIT.java`                      | AC-auth-6 pass                               |
| I4  | Viết integration test cho Flyway seed isolation: xác nhận profile `staging` không tạo seed users                                                               | `test/…/FlywayProfileIT.java`                           | AC-auth-20 pass                              |

### Nhóm J — Frontend

| #   | Bước                                                                                                                                                                                      | Tệp được chỉnh sửa            | Cách xác minh                                              |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------- |
| J1  | Tạo `authService.ts`: `login(username, password)`, `logout()`, `getCsrfToken()` — gọi API với `credentials: 'include'`, gửi `X-CSRF-TOKEN` header cho mutating calls                      | `src/services/authService.ts` | Manual test hoặc jest unit test mock fetch                 |
| J2  | Tạo `useAuth.ts` hook: state `{authenticated, username, roles, csrfToken}`, `login()`, `logout()`, `rehydrate()` — gọi `GET /api/v1/auth/csrf` khi mount, xử lý 401 (set unauthenticated) | `src/hooks/useAuth.ts`        | Manuel test: reload page → state khôi phục nếu session còn |
| J3  | Tạo `LoginPage.tsx`: form với field `Username` và `Password` (chỉ hai field — không có email), error message area theo wireframe, submit gọi `authService.login()`                        | `src/pages/LoginPage.tsx`     | Render đúng, không có email field (AC-auth-25)             |
| J4  | Sửa `App.tsx`: tích hợp `useAuth`, redirect logic (unauthenticated → Login page), session expired notification theo wireframe spec                                                        | `src/App.tsx`                 | E2E: flow đầy đủ login → use app → logout                  |

---

## 5. Rủi ro & Biện pháp giảm thiểu

| #   | Rủi ro                                                                                                                            | Khả năng                                          | Biện pháp giảm thiểu                                                                                                                                                                                      |
| --- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| R1  | `SameSite=None` cookie không hoạt động trên local dev do thiếu HTTPS                                                              | Trung bình                                        | Test với `http://localhost:5173` + Chrome; nếu browser chặn, dùng flag `--unsafely-treat-insecure-origin-as-secure` hoặc accept CSRF flow chỉ verify ở staging. Không hạ lower contract của staging/prod. |
| R2  | Spring Security 6 CSRF tokenAttributeHandler không tự load token vào attribute nếu không có `XorCsrfTokenRequestAttributeHandler` | Trung bình                                        | Cần bật `XorCsrfTokenRequestAttributeHandler` hoặc inject `CsrfToken` trực tiếp từ `HttpServletRequest.getAttribute("org.springframework.security.web.csrf.CsrfToken")`. Xác minh sớm ở Step F2.          |
| R3  | Seed migration lỡ chạy ở `staging` do misconfiguration `spring.flyway.locations`                                                  | Thấp                                              | E2E test kiểm tra profile `staging` không có bản ghi `admin`/`user01`. CI gate phải fail nếu seed location xuất hiện ở staging config.                                                                    |
| R4  | `@ManyToMany` lazy load gây `LazyInitializationException` khi lấy roles trong `UserDetailsService`                                | Thấp-Trung bình                                   | Dùng `JOIN FETCH` trong `UserRepository.findByUsername()` hoặc `FetchType.EAGER` có kiểm soát.                                                                                                            |
| R5  | FE gọi API bị blocked bởi CORS preflight do thiếu `OPTIONS` method hoặc `Access-Control-Allow-Headers`                            | Trung bình                                        | Cấu hình `CorsConfiguration` để allow method `OPTIONS` và header `X-CSRF-TOKEN`. Test với Postman/curl trước khi FE test.                                                                                 |
| R6  | BCrypt hash của seed user bị commit plaintext vào SQL                                                                             | Không áp dụng — đây là hash, không phải plaintext | Hash phải được pre-generated. Tuy nhiên: cần note rằng seed user password sẽ công khai (vì đây là dev seed) — đây là thiết kế đúng.                                                                       |
| R7  | Session fixation attack — session ID không được regenerate sau login                                                              | Thấp                                              | Spring Security mặc định đã bật `session-fixation=migrateSession`. Xác nhận trong SecurityConfig không bị tắt.                                                                                            |

---

## 6. Quy trình hoàn tác

Tất cả thay đổi trong ticket này là tạo mới (không sửa business logic hiện có). Không có rollback phức tạp trên distributed system vì không có thay đổi breaking.

1. **Revert code**: `git revert` commit(s) thuộc branch `feature/ticket-1-auth-session-csrf` hoặc `git checkout main -- .`
2. **Database rollback**: Xóa bảng `users`, `roles`, `user_roles` bằng Flyway repair + delete migration records, hoặc drop & recreate DB trên dev/staging
3. **Không có thay đổi business data** cần rollback (seed chỉ ở non-prod)
4. **FE rollback**: Revert `App.tsx` về trạng thái trước — không có FE business logic bị phụ thuộc vào auth ở giai đoạn này

---

## 7. Quy trình xác minh

> Thực hiện tuần tự sau khi hoàn thành tất cả implementation steps.

```bash
# 1. Build và unit tests
cd demo && ./gradlew clean test

# 2. Chạy app với profile dev (cần PostgreSQL running)
cd demo && ./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. Kiểm tra Flyway chạy thành công (xem log startup: "Successfully applied N migrations")

# 4. Manual / curl tests
# Login thành công
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user01","password":"User@123"}' -v

# Lấy CSRF token
curl -b cookies.txt http://localhost:8080/api/v1/auth/csrf -v

# Logout
curl -b cookies.txt -X POST http://localhost:8080/api/v1/auth/logout \
  -H "X-CSRF-TOKEN: <token-từ-bước-trên>" -v

# 5. Kiểm tra CORS header
curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" -v
# → Phải thấy: Access-Control-Allow-Origin: http://localhost:5173
# → Access-Control-Allow-Credentials: true

# 6. Kiểm tra profile staging không có Swagger
cd demo && ./gradlew bootRun --args='--spring.profiles.active=staging'
curl http://localhost:8080/swagger-ui/index.html
# → Phải trả 404 hoặc 403

# 7. Frontend
cd my-react-app && npm run lint && npm run build

# 8. E2E manual:
# - Mở http://localhost:5173, thấy form Login với field Username + Password (không có email)
# - Đăng nhập với user01/User@123 → thành công
# - Reload trang → session rehydrate qua GET /api/v1/auth/csrf
# - Logout → redirect về Login
```

---

## Bảng ánh xạ AC

| #   | AC            | Mô tả ngắn                                                                  | Bước đáp ứng | Cách xác minh                                                             |
| --- | ------------- | --------------------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------- |
| 1   | AC-auth-1/v1  | Login thành công → 200 + session cookie + body                              | F1           | IT: POST /login valid creds                                               |
| 2   | AC-auth-2/v1  | Cookie: HttpOnly, SameSite=None, Path=/                                     | A3, E2       | IT: response header Set-Cookie                                            |
| 3   | AC-auth-3/v1  | Cookie: Secure=true ở staging/prod                                          | A4, A5, E2   | IT: staging profile → Secure flag present                                 |
| 4   | AC-auth-4/v1  | Login sai creds → 401 + AUTH_INVALID_CREDENTIALS                            | G1, F1       | IT + UT                                                                   |
| 5   | AC-auth-5/v1  | Login user disabled → 401 + AUTH_USER_DISABLED                              | G1, E1, B2   | IT: login với user enabled=false                                          |
| 6   | AC-auth-6/v1  | Idle timeout đúng theo env                                                  | A3, A4, A5   | IT: test profile timeout ngắn → 401 sau hết hạn (I3)                      |
| 7   | AC-auth-7/v1  | GET /csrf với session hợp lệ → 200 + csrfToken body                         | F2           | IT                                                                        |
| 8   | AC-auth-8/v1  | GET /csrf không có session → 401 + AUTH_SESSION_REQUIRED                    | E2, G1, F2   | IT                                                                        |
| 9   | AC-auth-9/v1  | Mutating endpoint không session → 401 + AUTH_SESSION_REQUIRED               | E2, G1       | IT: POST protected endpoint no session                                    |
| 10  | AC-auth-10/v1 | Protected endpoint có session, thiếu X-CSRF-TOKEN → 403 + AUTH_CSRF_INVALID | E2, G1       | IT                                                                        |
| 11  | AC-auth-11/v1 | Protected endpoint, CSRF sai → 403 + AUTH_CSRF_INVALID                      | E2, G1       | IT                                                                        |
| 12  | AC-auth-12/v1 | Protected endpoint session + CSRF hợp lệ → pass through                     | E2, F1–F3    | IT                                                                        |
| 13  | AC-auth-13/v1 | Logout thành công → 200, session invalidate, cookie clear                   | F3           | IT                                                                        |
| 14  | AC-auth-14/v1 | Logout không có session → 401 + AUTH_SESSION_REQUIRED                       | E2, G1       | IT                                                                        |
| 15  | AC-auth-15/v1 | Logout CSRF sai → 403 + AUTH_CSRF_INVALID                                   | E2, G1       | IT                                                                        |
| 16  | AC-auth-16/v1 | Springdoc bật dev/staging; tắt prod                                         | H1, A3–A5    | IT: GET /v3/api-docs per profile                                          |
| 17  | AC-auth-17/v1 | PostgreSQL + bảng users, roles, user_roles                                  | A2, B1       | IT: datasource connect + schema validate                                  |
| 18  | AC-auth-18/v1 | Flyway chạy migration trên DB trắng                                         | B1           | IT: startup on empty DB                                                   |
| 19  | AC-auth-19/v1 | Seed admin + user01 ở dev/test với hashed password                          | B2           | IT: profile dev → bản ghi tồn tại, password BCrypt                        |
| 20  | AC-auth-20/v1 | Không seed ở staging/prod, không nạp non-prod location                      | A4, A5, B3   | IT: staging startup → không có seed user (I4)                             |
| 21  | AC-auth-21/v1 | In-memory session, không Redis                                              | A2, E2       | BB: không có Redis dependency trong build.gradle                          |
| 22  | AC-auth-22/v1 | Audit log các auth events, không log secret                                 | G2           | UT: log messages không chứa password/session/token (I1)                   |
| 23  | AC-auth-23/v1 | Mọi 401/403 auth → JSON {code, message, path, timestamp}                    | G1           | UT + IT                                                                   |
| 24  | AC-auth-24/v1 | Login response luôn có `roles` ([] nếu không có role)                       | F1, B1, B2   | UT: user không có role → roles:[]                                         |
| 25  | AC-auth-25/v1 | Chỉ field Username, không có email field                                    | J3, D1       | BB: LoginPage không render email field; LoginRequest không có email field |
| 26  | AC-auth-26/v1 | Reload app → GET /csrf rehydrate session state                              | J2, F2       | E2E: reload browser → auth state khôi phục                                |
| 27  | AC-auth-27/v1 | CORS credentialed từ localhost:5173, không wildcard                         | E2           | IT: CORS preflight check + curl headers                                   |

---

## Checklist trước khi bắt đầu implementation

> Xác nhận tất cả items dưới đây trước khi bắt đầu code. Các `[ ]` chưa được check là blocking.

### Môi trường

- [ ] PostgreSQL instance sẵn sàng cho môi trường `dev` (URL, username, password đã có)
- [ ] Biến môi trường DB được truyền vào (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) hoặc quyết định dùng hardcode trong `application-dev.properties` (không ổn cho security)
- [ ] Java 25 toolchain đã cài trên máy dev (build.gradle yêu cầu Java 25)
- [ ] Gradle Wrapper version tương thích với Spring Boot 3.5.x (kiểm tra `gradle-wrapper.properties`)

### Thư viện

- [ ] Xác nhận `springdoc-openapi-starter-webmvc-ui` version tương thích Spring Boot 3.5.x (thường là `2.7.x`; cần kiểm tra Maven Central)
- [ ] Xác nhận `flyway-database-postgresql` module name chính xác cho Flyway version đi kèm Spring Boot 3.5.x

### Quyết định kỹ thuật cần confirm (open issues nhỏ chưa block nhưng cần thống nhất)

- [ ] **CSRF token handler**: Xác nhận dùng `XorCsrfTokenRequestAttributeHandler` (Spring Security 6 mặc định) hay cần custom handler để inject token vào controller. Thử nghiệm Quick PoC trước Step F2.
- [ ] **Session cookie SameSite trên local dev**: Confirm FE test được CORS + credentialed cookie trên `localhost:5173` → `localhost:8080` mà không cần HTTPS (hoặc chấp nhận CSRF chỉ test ở staging).
- [ ] **BCrypt pre-hashed password cho seed**: Cần generate hash hay cho phép `DelegatingPasswordEncoder` với prefix `{bcrypt}` trong SQL seed? Phải dùng Bcrypt hash — cần tool để gen (Spring CLI `spring encodepassword`, hoặc script).
- [ ] **Spring Boot version**: `3.5.12-SNAPSHOT` là snapshot build. Confirm team đồng ý dùng snapshot hay cần downgrade stable (`3.4.x`). Snapshot có thể gây instability.
- [ ] **Java 25 vs Java 21**: Java 25 chưa release (dự kiến Sep 2026 là Java 25). Hiện tại Java 21 là LTS. Confirm toolchain requirement, hoặc đây chỉ là cấu hình demo.

### Ngoài phạm vi — không implementation

- Không implement Redis session
- Không implement `/auth/me` hoặc `/auth/status` endpoint
- Không implement email login
- Không implement origin ngoài `http://localhost:5173`
- Không implement RBAC guard trên business API khác
