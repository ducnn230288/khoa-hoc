# Implementation Plan — AUTH-001

_Tạo: 2026-03-13 · Trạng thái: Draft_

---

## 1. Hướng tiếp cận

**Phương án duy nhất** (theo spec-pack): Spring Security built-in session + CSRF, không có alternative.

| Quyết định                                                   | Lý do                                             |
| ------------------------------------------------------------ | ------------------------------------------------- |
| Spring Security session (in-memory)                          | Spec AC-29: không dùng Redis/persistent store     |
| CSRF token qua API riêng (`GET /api/v1/auth/csrf`)           | Spec AC-9: không nhúng token trong login response |
| BCrypt cost 12                                               | Spec AC-NFR-1, SEC-3                              |
| Flyway V3 conditional theo `spring.flyway.locations` profile | Spec AC-27, AC-28                                 |
| Springdoc tắt prod qua Spring profile                        | Spec AC-21, SEC-8                                 |
| RFC 7807 cho tất cả error                                    | Spec AC-32, SEC-9                                 |

---

## 2. Phạm vi ảnh hưởng

### 2.1 Backend — file tạo mới

```
demo/build.gradle                                          ← thêm dependencies
demo/src/main/resources/
  application.properties                                   ← base config
  application-dev.properties                               ← dev profile
  application-staging.properties                           ← staging profile
  application-prod.properties                              ← prod profile
  db/migration/
    V1__init_schema.sql
    V2__create_auth_tables.sql
  db/migration/seed/
    V3__seed_dev_test_users.sql
demo/src/main/java/com/example/demo/
  entity/
    User.java
    Role.java
  repository/
    UserRepository.java
    RoleRepository.java
  service/
    UserDetailsServiceImpl.java
  config/
    SecurityConfig.java
    SpringdocConfig.java
  dto/
    LoginRequest.java
    LoginResponse.java
    CsrfResponse.java
    LogoutResponse.java
  exception/
    GlobalExceptionHandler.java
    ProblemDetail.java                  ← custom record (nếu không dùng Spring 6 built-in)
  controller/
    AuthController.java
```

### 2.2 Backend — file sửa

```
demo/build.gradle                       ← thêm 6 dependencies
demo/src/main/resources/application.properties  ← thêm datasource, flyway, jpa base
```

### 2.3 Frontend — file tạo mới

```
my-react-app/vite.config.ts             ← thêm proxy /api → :8080
my-react-app/src/
  api/
    authApi.ts                          ← login, csrf, logout fetch calls
  hooks/
    useAuth.ts                          ← auth state + actions
  context/
    AuthContext.tsx                     ← session + csrf context
  components/
    LoginForm.tsx
    ProtectedRoute.tsx
    Dashboard.tsx
  pages/
    LoginPage.tsx
    DashboardPage.tsx
```

### 2.4 Frontend — file sửa

```
my-react-app/src/App.tsx                ← route setup, AuthProvider wrap
my-react-app/src/main.tsx               ← không thay đổi (mount point giữ nguyên)
```

### 2.5 Database / config

| Đối tượng                           | Hành động                                         |
| ----------------------------------- | ------------------------------------------------- |
| Bảng `users`, `roles`, `user_roles` | Tạo mới qua Flyway V2                             |
| Seed rows `admin`, `user01`         | Tạo qua Flyway V3 (dev/test only)                 |
| PostgreSQL connection               | Cấu hình trong `application-{profile}.properties` |

---

## 3. Các bước triển khai

> Mỗi slice = 1 diff reviewable. Không bắt đầu slice tiếp theo khi slice trước chưa pass verify.

### S-01 — BE: Thêm dependencies vào `build.gradle`

**File**: `demo/build.gradle`

Thêm vào block `dependencies`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-validation'
runtimeOnly 'org.postgresql:postgresql'
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

**Verify**: `cd demo && ./gradlew dependencies --configuration compileClasspath` không báo lỗi resolve.

---

### S-02 — BE: Cấu hình application properties (base + 3 profiles)

**Files**:

- `demo/src/main/resources/application.properties` — base (datasource, jpa, flyway base location)
- `demo/src/main/resources/application-dev.properties` — session=8h, secure=false, springdoc=on, flyway.locations includes seed
- `demo/src/main/resources/application-staging.properties` — session=2h, secure=true, springdoc=on
- `demo/src/main/resources/application-prod.properties` — session=30m, secure=true, springdoc=off

Key properties:

```properties
# base
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:demo}
spring.datasource.username=${DB_USER:demo}
spring.datasource.password=${DB_PASSWORD:demo}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.locations=classpath:db/migration
server.servlet.session.cookie.same-site=none

# dev profile additions
server.servlet.session.timeout=8h
server.servlet.session.cookie.secure=false
spring.flyway.locations=classpath:db/migration,classpath:db/migration/seed
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true

# prod profile additions
server.servlet.session.timeout=30m
server.servlet.session.cookie.secure=true
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

**AC liên quan**: AC-5, AC-8, AC-21, AC-27, AC-28

**Verify**: App khởi động với `--spring.profiles.active=dev` không lỗi (DB chưa có cũng OK ở bước này nếu `flyway.enabled=false` tạm thời).

---

### S-03 — BE: Flyway V1 — init schema

**File**: `demo/src/main/resources/db/migration/V1__init_schema.sql`

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

**AC liên quan**: AC-25

**Verify**: Migration V1 chạy thành công, `flyway_schema_history` ghi version 1.

---

### S-04 — BE: Flyway V2 — tạo bảng auth

**File**: `demo/src/main/resources/db/migration/V2__create_auth_tables.sql`

Tạo bảng `users`, `roles`, `user_roles` theo schema trong spec-pack §5.7 với đầy đủ constraints (PK, UNIQUE, FK, NOT NULL, DEFAULT).

**AC liên quan**: AC-24, AC-26

**Verify**: Migration V2 pass, `\d users` trong psql hiển thị đúng columns và constraints.

---

### S-05 — BE: Flyway V3 — seed dev/test users

**File**: `demo/src/main/resources/db/migration/seed/V3__seed_dev_test_users.sql`

Seed `admin` (role ADMIN) và `user01` (role USER) với BCrypt hash cost 12.

> Hash phải được tính offline trước khi commit — không sinh hash trong SQL. Sử dụng BCrypt tool hoặc test util để tạo.

**AC liên quan**: AC-27, AC-28

**Verify**:

- Profile `dev`: migration V3 chạy, 2 user tồn tại trong DB.
- Profile `prod`: V3 không xuất hiện trong `flyway_schema_history`.

---

### S-06 — BE: Entity + Repository

**Files**:

- `demo/src/main/java/com/example/demo/entity/User.java` — `@Entity`, fields theo schema V2
- `demo/src/main/java/com/example/demo/entity/Role.java` — `@Entity`
- `demo/src/main/java/com/example/demo/repository/UserRepository.java` — `JpaRepository<User, Long>`, thêm `findByUsername`
- `demo/src/main/java/com/example/demo/repository/RoleRepository.java` — `JpaRepository<Role, Long>`

**AC liên quan**: AC-24, AC-7

**Verify**: Unit test (Mockito) cho `UserRepository.findByUsername` mock trả đúng entity.

---

### S-07 — BE: UserDetailsServiceImpl + BCrypt bean

**File**: `demo/src/main/java/com/example/demo/service/UserDetailsServiceImpl.java`

- Implements `UserDetailsService`
- Load user từ `UserRepository.findByUsername`
- Throw `UsernameNotFoundException` nếu không tìm thấy hoặc `enabled=false`
- `PasswordEncoder` bean (`BCryptPasswordEncoder(12)`) khai báo trong `SecurityConfig`

**AC liên quan**: AC-6, AC-7, SEC-3

**Verify**: Unit test `loadUserByUsername_withDisabledUser_throwsException`.

---

### S-08 — BE: SecurityConfig

**File**: `demo/src/main/java/com/example/demo/config/SecurityConfig.java`

Config items:

- `SecurityFilterChain`: permit `POST /api/v1/auth/login`, `GET /api/v1/auth/csrf` (sau auth), `POST /api/v1/auth/logout`; authenticate tất cả còn lại
- Session: `IF_REQUIRED`, `maximumSessions` không giới hạn, `HttpOnly=true`, `SameSite=None`
- CSRF: bật, `CookieCsrfTokenRepository` hoặc `HttpSessionCsrfTokenRepository`; custom header `X-CSRF-TOKEN`
- CORS: `allowedOrigins("http://localhost:5173")`, `allowCredentials(true)`, methods, headers theo spec §5.6
- `formLogin().disable()`, `httpBasic().disable()`
- Custom `AuthenticationEntryPoint` → RFC 7807 401
- Custom `AccessDeniedHandler` → RFC 7807 403

**AC liên quan**: AC-2, AC-3, AC-4, AC-5, AC-8, AC-12, AC-13, AC-14, AC-29, AC-30, AC-31

**Verify**: `@WebMvcTest` slice test — unauthenticated `GET /api/v1/protected` trả 401 RFC 7807.

---

### S-09 — BE: DTOs

**Files** (Java records):

- `dto/LoginRequest.java` — `username`, `password`
- `dto/LoginResponse.java` — `authenticated`, `username`, `roles`
- `dto/CsrfResponse.java` — `csrfToken`, `headerName`, `parameterName`
- `dto/LogoutResponse.java` — `success`

> Spring 6 cung cấp `org.springframework.http.ProblemDetail` built-in — dùng thay vì tạo custom record.

**AC liên quan**: AC-1, AC-9, AC-16, AC-32

**Verify**: Compile pass, records immutable, không có field thừa.

---

### S-10 — BE: AuthController

**File**: `demo/src/main/java/com/example/demo/controller/AuthController.java`

Endpoints:

- `POST /api/v1/auth/login` — authenticate, tạo session, trả `LoginResponse`
- `GET /api/v1/auth/csrf` — lấy CSRF token từ session, trả `CsrfResponse`; nếu không có session → delegate tới `AuthenticationEntryPoint`
- `POST /api/v1/auth/logout` — invalidate session, clear cookie, trả `LogoutResponse`

**AC liên quan**: AC-1, AC-2, AC-6, AC-9, AC-10, AC-11, AC-16, AC-17, AC-18

**Verify**:

- `@WebMvcTest` slice: `login_withValidCredentials_returns200WithSetCookie`
- `@WebMvcTest` slice: `login_withWrongPassword_returns401ProblemDetail`
- `@WebMvcTest` slice: `csrf_withoutSession_returns401ProblemDetail`

---

### S-11 — BE: GlobalExceptionHandler

**File**: `demo/src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`

- `@ControllerAdvice`
- Handler cho `AuthenticationException` → 401 RFC 7807
- Handler cho `AccessDeniedException` → 403 RFC 7807
- Handler cho `MethodArgumentNotValidException` → 400 RFC 7807
- Không bao giờ expose stack trace

**AC liên quan**: AC-6, AC-11, AC-13, AC-14, AC-32

**Verify**: Integration test — sai password trả đúng RFC 7807 fields (type, title, status, detail, instance).

---

### S-12 — BE: Springdoc config + API annotations

**File**: `demo/src/main/java/com/example/demo/config/SpringdocConfig.java`

- `@Profile({"dev", "staging"})` trên bean `OpenAPI`
- Mô tả 3 endpoints auth với `@Operation`, `@ApiResponse`, `@RequestBody`

**AC liên quan**: AC-19, AC-20, AC-21, AC-22

**Verify**:

- Profile `dev`: `GET /v3/api-docs` trả 200 với 3 endpoints auth có docs.
- Profile `prod`: `GET /v3/api-docs` trả 404.

---

### S-13 — FE: Vite proxy + `src/api/authApi.ts`

**Files**:

- `my-react-app/vite.config.ts` — thêm `server.proxy`: `/api` → `http://localhost:8080`
- `my-react-app/src/api/authApi.ts` — `login()`, `fetchCsrf()`, `logout()` dùng `credentials: 'include'` và đính `X-CSRF-TOKEN` cho mutating calls

**AC liên quan**: AR-F3, AC-33, AC-34

**Verify**: `npm run build` pass, `npm run lint` pass. Kiểm tra Vite proxy `/api → http://localhost:8080` đảm bảo cookie `SameSite=Lax` từ backend được Chromium chấp nhận trên `http://localhost:5173` (không cần HTTPS).

---

### S-14 — FE: `AuthContext` + `useAuth` hook

**Files**:

- `my-react-app/src/context/AuthContext.tsx` — context chứa `{ user, csrfToken, login, logout, isAuthenticated }`
- `my-react-app/src/hooks/useAuth.ts` — expose context + async actions (gọi `authApi`, update state)

**AC liên quan**: AR-F2, AR-F4

**Verify**: Vitest unit test — `useAuth` gọi `authApi.login` mock, state update đúng.

---

### S-15 — FE: Components (LoginForm, ProtectedRoute, Dashboard)

**Files**:

- `my-react-app/src/components/LoginForm.tsx` — form theo wireframe spec §5.11, gọi `useAuth().login`
- `my-react-app/src/components/ProtectedRoute.tsx` — redirect về `/login` nếu `!isAuthenticated`
- `my-react-app/src/components/Dashboard.tsx` — hiển thị username, session active, CSRF loaded, nút Logout

**AC liên quan**: AC-1, AC-16, wireframes, AC-35, AC-36

**Verify**: Vitest RTL — LoginForm hiển thị error khi `login` reject; ProtectedRoute redirect khi unauthenticated; LoginPage redirect về `/` nếu user đã authenticated (AC-36); LoginForm redirect sang `/` sau khi login + lấy CSRF token thành công (AC-35).

---

### S-16 — FE: Route setup (`App.tsx`) + `AuthProvider`

**File**: `my-react-app/src/App.tsx`

```
<AuthProvider>
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
  </Routes>
</AuthProvider>
```

**AC liên quan**: AR-F1, AR-F5, AC-35, AC-36

**Verify**: `npm run build` pass. Manual smoke: login flow end-to-end trong browser (dev profile) — sau login thành công, trình duyệt redirect sang `/` (AC-35); truy cập `/login` khi đã đăng nhập → tự redirect về `/` (AC-36).

---

## 4. Rủi ro & giảm thiểu

| #   | Rủi ro                                                                             | Giảm thiểu                                                         |
| --- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| R-1 | Cross-site cookie không gửi được nếu `SameSite=None` thiếu `Secure` ở staging/prod | Kiểm tra cookie header ngay S-08; E2E test với đúng profile        |
| R-2 | Flyway V3 accidentally chạy ở prod                                                 | Guard bằng `spring.flyway.locations` per profile; CI check profile |
| R-3 | Spring 6 `ProblemDetail` built-in vs custom record conflict                        | Dùng Spring built-in `ProblemDetail`; không tạo duplicate class    |
| R-4 | CSRF token mất khi FE reload                                                       | `useAuth` gọi `fetchCsrf` khi app init nếu session còn hợp lệ      |
| R-5 | `SecurityConfig` chặn nhầm `/api/v1/auth/login` → login không được                 | `@WebMvcTest` slice test S-08 chạy trước khi merge                 |

---

## 5. Rollback

Vì đây là greenfield (không có code/data production hiện tại):

| Tình huống          | Rollback                                                      |
| ------------------- | ------------------------------------------------------------- |
| Slice S-01 đến S-12 | Revert commit, DB được tạo mới bằng Flyway → drop và recreate |
| Slice S-13 đến S-16 | Revert commit FE, không ảnh hưởng BE                          |
| Migration đã chạy   | Flyway repair + manual DROP TABLE nếu cần (môi trường dev)    |

---

## 6. Bước xác minh tổng thể (sau tất cả slices)

1. `cd demo && ./gradlew test` — tất cả BE test pass (TST-9)
2. `cd my-react-app && npm run lint` — zero errors (TST-10)
3. `cd my-react-app && npm run build` — build thành công
4. Manual smoke test (dev profile):
   - Login `user01 / User@123` → 200 + JSESSIONID cookie
   - GET `/api/v1/auth/csrf` → 200 + csrfToken
   - POST protected endpoint với CSRF → 200
   - POST protected endpoint không CSRF → 403 RFC 7807
   - GET `/v3/api-docs` → 200 (dev)
   - Logout → session cleared
5. Manual smoke test (prod profile):
   - GET `/swagger-ui/index.html` → 404
   - V3 migration không trong `flyway_schema_history`

---

## 7. Open Issues (carry-over — không block triển khai dev)

| ID    | Câu hỏi                                                                           | Block                |
| ----- | --------------------------------------------------------------------------------- | -------------------- |
| OI-A' | CORS `allowedOrigins` cho prod                                                    | ✅ Block deploy prod |
| OI-B  | RFC 7807 `type` URI namespace — dùng `https://errors.example.com/...` placeholder | ❌                   |
| OI-C  | HTTPS local setup guide cho staging/prod cookie `Secure=true`                     | ❌                   |

---

## 8. AC Mapping Table

| AC    | Mô tả ngắn                                                                    | Slice(s)         | File(s) chính                                                    | Test type              |
| ----- | ----------------------------------------------------------------------------- | ---------------- | ---------------------------------------------------------------- | ---------------------- |
| AC-1  | `POST /api/v1/auth/login` tồn tại                                             | S-09, S-10       | `AuthController`, `LoginRequest`                                 | IT                     |
| AC-2  | Login thành công → Set-Cookie JSESSIONID                                      | S-08, S-10       | `SecurityConfig`, `AuthController`                               | IT                     |
| AC-3  | Cookie HttpOnly=true                                                          | S-08             | `SecurityConfig`                                                 | IT                     |
| AC-4  | Cookie SameSite=None                                                          | S-02, S-08       | `application.properties`, `SecurityConfig`                       | IT                     |
| AC-5  | Cookie Secure=true (staging/prod)                                             | S-02             | `application-staging.properties`, `application-prod.properties`  | IT                     |
| AC-6  | Login thất bại → 401 RFC 7807, không tạo session                              | S-10, S-11       | `AuthController`, `GlobalExceptionHandler`                       | IT, BB                 |
| AC-7  | enabled=false → 401                                                           | S-07             | `UserDetailsServiceImpl`                                         | UT, IT                 |
| AC-8  | Session timeout per environment                                               | S-02             | `application-{profile}.properties`                               | IT                     |
| AC-9  | `GET /api/v1/auth/csrf` trả đúng fields                                       | S-09, S-10       | `CsrfResponse`, `AuthController`                                 | IT                     |
| AC-10 | CSRF endpoint chỉ hoạt động khi session hợp lệ                                | S-08, S-10       | `SecurityConfig`, `AuthController`                               | IT                     |
| AC-11 | CSRF không có session → 401                                                   | S-08, S-11       | `SecurityConfig`, `GlobalExceptionHandler`                       | IT, BB                 |
| AC-12 | Mutating API yêu cầu CSRF                                                     | S-08             | `SecurityConfig`                                                 | IT                     |
| AC-13 | Thiếu CSRF header → 403                                                       | S-08, S-11       | `SecurityConfig`, `GlobalExceptionHandler`                       | IT, BB                 |
| AC-14 | Sai CSRF token → 403                                                          | S-08, S-11       | `SecurityConfig`, `GlobalExceptionHandler`                       | IT, BB                 |
| AC-15 | CSRF qua header X-CSRF-TOKEN                                                  | S-08             | `SecurityConfig`                                                 | IT                     |
| AC-16 | `POST /api/v1/auth/logout` tồn tại                                            | S-09, S-10       | `AuthController`, `LogoutResponse`                               | IT                     |
| AC-17 | Logout invalidates session                                                    | S-10             | `AuthController`                                                 | IT                     |
| AC-18 | Logout clears cookie                                                          | S-10             | `AuthController`                                                 | IT                     |
| AC-19 | Springdoc, `/v3/api-docs` available                                           | S-01, S-12       | `build.gradle`, `SpringdocConfig`                                | IT                     |
| AC-20 | Swagger UI dev/staging                                                        | S-02, S-12       | `application-dev.properties`, `SpringdocConfig`                  | IT                     |
| AC-21 | Swagger UI disabled prod                                                      | S-02, S-12       | `application-prod.properties`, `SpringdocConfig`                 | IT                     |
| AC-22 | Auth endpoints documented                                                     | S-12             | `AuthController` annotations                                     | BB                     |
| AC-23 | Kết nối PostgreSQL                                                            | S-01, S-02       | `build.gradle`, `application.properties`                         | IT                     |
| AC-24 | User data trong PostgreSQL                                                    | S-04, S-06       | `V2__create_auth_tables.sql`, `User.java`                        | IT                     |
| AC-25 | Flyway tự động chạy startup                                                   | S-01, S-02, S-03 | `build.gradle`, `application.properties`, `V1__init_schema.sql`  | IT                     |
| AC-26 | V2 tạo đúng schema                                                            | S-04             | `V2__create_auth_tables.sql`                                     | IT                     |
| AC-27 | V3 seed dev/test                                                              | S-05             | `V3__seed_dev_test_users.sql`                                    | IT                     |
| AC-28 | V3 không chạy staging/prod                                                    | S-02, S-05       | `application-{profile}.properties`                               | IT                     |
| AC-29 | In-memory session, không Redis                                                | S-08             | `SecurityConfig`                                                 | IT                     |
| AC-30 | CORS allow localhost:5173                                                     | S-08             | `SecurityConfig`                                                 | IT, E2E                |
| AC-31 | Không CORS wildcard                                                           | S-08             | `SecurityConfig`                                                 | IT                     |
| AC-32 | Error response RFC 7807                                                       | S-11             | `GlobalExceptionHandler`                                         | IT, BB                 |
| AC-33 | Login → GET /csrf thành công trên HTTP thật (dev proxy)                       | S-13, S-14       | `vite.config.ts`, `authApi.ts`, `AuthContext.tsx`                | IT, E2E                |
| AC-34 | Cookie dev Chromium-compatible qua Vite proxy (SameSite=Lax, không cần HTTPS) | S-02, S-08, S-13 | `application-dev.properties`, `SecurityConfig`, `vite.config.ts` | E2E, Config review     |
| AC-35 | Redirect sang `/` sau login + CSRF thành công                                 | S-15, S-16       | `LoginForm.tsx`, `App.tsx`                                       | FE component test, E2E |
| AC-36 | Redirect về `/` nếu user đã authenticated truy cập `/login`                   | S-15, S-16       | `LoginPage.tsx`, `App.tsx`                                       | FE component test, E2E |
