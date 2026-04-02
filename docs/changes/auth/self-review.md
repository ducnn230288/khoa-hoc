# Tự review — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng)

> Ngày: 2026-04-02  
> Mục đích: tài liệu này được AI điền **sau implementation, trước review thủ công**.  
> Nguồn đối chiếu: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/changes/auth/review-checklist.md`  
> Quy tắc điền: mọi checkbox chỉ được tick khi có bằng chứng đi kèm (tệp + dòng, tên test, lệnh đã chạy, log hoặc browser smoke).

---

## 1. Thông tin phiên tự review

- Người điền: Codex GPT-5
- Commit / branch đang review: chưa commit / `codex3`
- PR / changeset: N/A
- Môi trường đã kiểm tra: [x] dev [x] test [x] staging-like [x] khác: `prod` profile automated
- Phạm vi đã triển khai so với spec: [x] đầy đủ [ ] một phần (nêu rõ bên dưới)

## 2. Trạng thái hoàn thành AC

### 2.1 Backend auth flow

- [x] **AC-auth-1/v1** — Login hợp lệ trả `200`, tạo server-side session, trả session cookie.  
      Bằng chứng: `AuthController.java:40-50`; `AuthService.java:47-67`; `AuthControllerIT.java:64-80`; `./gradlew.bat test` = PASS.
- [x] **AC-auth-2/v1** — Session cookie có `HttpOnly=true`, `SameSite=None`, `Path=/`.  
      Bằng chứng: `application.properties:9-10`; `application-dev.properties:1-3`; `AuthControllerIT.java:78-79`.
- [x] **AC-auth-3/v1** — Ở `staging/prod`, session cookie có `Secure=true`.  
      Bằng chứng: `application-staging.properties:1-3`; `application-prod.properties:1-3`; `StagingProfileIT.java:54-80`.
- [x] **AC-auth-4/v1** — Sai `username/password` trả `401` + `AUTH_INVALID_CREDENTIALS`, không tạo session hợp lệ.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:62-66`; `AuthControllerIT.java:82-95`.
- [x] **AC-auth-5/v1** — User `enabled=false` trả `401` + `AUTH_USER_DISABLED`, không tạo session hợp lệ.  
      Bằng chứng: `UserDetailsServiceImpl.java:21-31`; `GlobalAuthExceptionHandler.java:68-72`; `AuthControllerIT.java:97-110`.
- [x] **AC-auth-6/v1** — TTL session đúng theo môi trường (`dev=8h`, `staging=2h`, `prod=30m`) và session cũ hết hạn trả `401`.  
      Bằng chứng: `application-dev.properties:1`; `application-staging.properties:1`; `application-prod.properties:1`; `AuthSessionTimeoutIT.java:58-91`.
- [x] **AC-auth-7/v1** — `GET /api/v1/auth/csrf` với session hợp lệ trả `200` cùng `csrfToken`, `headerName`, `parameterName`.  
      Bằng chứng: `AuthController.java:52-69`; `AuthService.java:79-81`; `AuthControllerIT.java:112-133`.
- [x] **AC-auth-8/v1** — `GET /api/v1/auth/csrf` không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:38-52`; `AuthControllerIT.java:114-117`.
- [x] **AC-auth-9/v1** — Protected mutating request không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng: `SecurityConfig.java:47-74`; `AuthControllerIT.java:135-140`.
- [x] **AC-auth-10/v1** — Protected mutating request có session nhưng thiếu `X-CSRF-TOKEN` trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng: `SecurityConfig.java:47-62`; `GlobalAuthExceptionHandler.java:54-60`; `AuthControllerIT.java:149-153`.
- [x] **AC-auth-11/v1** — Protected mutating request có session nhưng CSRF sai/hết hiệu lực trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng: `SecurityConfig.java:47-62`; `AuthControllerIT.java:155-159`.
- [x] **AC-auth-12/v1** — Protected mutating request có session + CSRF hợp lệ đi qua auth/CSRF guard.  
      Bằng chứng: `AuthController.java:71-83`; `AuthControllerIT.java:162-180`.
- [x] **AC-auth-13/v1** — Logout hợp lệ trả `200`, invalidate session hiện tại, clear/expire cookie.  
      Bằng chứng: `AuthService.java:83-98`; `AuthControllerIT.java:162-180`.
- [x] **AC-auth-14/v1** — Logout không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng: `SecurityConfig.java:47-74`; `AuthControllerIT.java:135-140`.
- [x] **AC-auth-15/v1** — Logout có session nhưng thiếu/sai CSRF trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:54-60`; `AuthControllerIT.java:149-159`.

### 2.2 Docs / config / persistence

- [x] **AC-auth-16/v1** — `dev/staging` public OpenAPI/Swagger; `prod` không public.  
      Bằng chứng: `application-dev.properties:5-6`; `application-staging.properties:5-6`; `application-prod.properties:5-6`; `StagingProfileIT.java:62-80`; `ProdProfileIT.java:43-58`.
- [x] **AC-auth-17/v1** — PostgreSQL lưu auth data trong `users`, `roles`, `user_roles`.  
      Bằng chứng: `build.gradle:22-34`; `application.properties:2-8`; `V1__create_auth_tables.sql:1-22`; `./gradlew.bat test` = PASS với Testcontainers PostgreSQL.
- [x] **AC-auth-18/v1** — Flyway khởi tạo schema auth được trên DB trống theo quy trình chuẩn.  
      Bằng chứng: `application.properties:8`; `V1__create_auth_tables.sql:1-22`; `./gradlew.bat test` = PASS.
- [x] **AC-auth-19/v1** — `dev/test` tạo được seed users `admin`, `user01`, password đã hash.  
      Bằng chứng: `application-dev.properties:4`; `application-test.properties:1-4`; `R__seed_dev_test_users.sql:1-23`; `AuthControllerIT.java:64-80`.
- [x] **AC-auth-20/v1** — `staging/prod` không nạp seed users và không nạp non-prod Flyway location.  
      Bằng chứng: `application-staging.properties:4`; `application-prod.properties:4`; `FlywayLocationGuard.java:23-29`; `StagingProfileIT.java:54-80`.
- [x] **AC-auth-21/v1** — Session store của ticket là in-memory/default session, không phụ thuộc Redis/persistent store.  
      Bằng chứng: `build.gradle:22-34` (không có Redis/Spring Session); `SecurityConfig.java:63-64`.
- [x] **AC-auth-22/v1** — Audit logs có đủ sự kiện tối thiểu và không leak plaintext password/full session id/full CSRF token.  
      Bằng chứng: `AuditLogger.java:15-47`; `AuditLoggerTest.java:34-53`.
- [x] **AC-auth-23/v1** — Mọi lỗi `401/403` có đủ `code`, `message`, `path`, `timestamp`.  
      Bằng chứng: `ErrorResponse.java:1-5`; `GlobalAuthExceptionHandler.java:38-85`; `GlobalAuthExceptionHandlerTest.java:26-88`; `AuthControllerIT.java:82-159`.
- [x] **AC-auth-24/v1** — Login success response luôn có `roles` là mảng; user không có role trả `[]`.  
      Bằng chứng: `LoginResponse.java:1-5`; `AuthService.java:62-67`; `AuthControllerIT.java:183-195`.
- [x] **AC-auth-25/v1** — UI/API/OpenAPI chỉ dùng `Username`, không có email login.  
      Bằng chứng: `LoginRequest.java:1-3`; `LoginPage.tsx:25-66`; `App.tsx:97-108`; browser snapshot tại `http://localhost:5173` hiển thị chỉ `Username` + `Password`.
- [x] **AC-auth-26/v1** — Sau reload, session còn hiệu lực thì `GET /api/v1/auth/csrf` rehydrate được flow mà không cần auth-status endpoint riêng.  
      Bằng chứng: `AuthController.java:52-69`; `useAuth.ts:116-142`; `App.tsx:8-24`; `AuthControllerIT.java:112-133`.  
      Ghi chú: browser smoke local HTTP bị chặn bởi policy `SameSite=None`/`Secure=false`, xem mục “Remaining issues”.
- [x] **AC-auth-27/v1** — Backend chấp nhận credentialed CORS request từ `http://localhost:5173` và không dùng wildcard origin cho flow này.  
      Bằng chứng: `SecurityConfig.java:82-94`; `AuthControllerIT.java:197-209`; browser smoke tại `http://127.0.0.1:15173` bị chặn đúng theo allowlist.

---

## 3. Tự điền theo review checklist

### 3.1 Specification / AC

- [x] **RC-01** — Endpoint/method/auth flow khớp spec.  
      Bằng chứng: `AuthController.java:30-83`; `AuthControllerIT.java:64-209`.
- [x] **RC-02** — Login response + contract `Username`/`roles[]` đúng spec.  
      Bằng chứng: `LoginResponse.java:1-5`; `AuthService.java:62-67`; `LoginPage.tsx:35-57`; `AuthControllerIT.java:183-195`.
- [x] **RC-03** — Không triển khai ngoài phạm vi.  
      Bằng chứng: không có `/auth/me`, `/auth/status`, JWT, Redis, email login trong `SecurityConfig.java`, `AuthController.java`, `build.gradle`, `App.tsx`.
- [x] **RC-04** — Tất cả AC đều có bằng chứng.  
      Bằng chứng: mục 2 của tài liệu này + `./gradlew.bat test`, `npm run lint`, `npm run build`.

### 3.2 Design / Dependencies

- [x] **RC-05** — Phân lớp đúng, không circular dependency.  
      Bằng chứng: `AuthController.java:34-83` chỉ điều phối HTTP; `AuthService.java:29-98` giữ auth logic; `SecurityConfig.java:38-135`; `UserRepository.java:1-16`.
- [x] **RC-06** — Dependency/config phù hợp hướng session + PostgreSQL + Flyway + Springdoc.  
      Bằng chứng: `build.gradle:22-34`; `application.properties:2-13`; `SpringdocConfig.java:13-30`.
- [x] **RC-07** — FE contract thống nhất với `credentials: include` và `GET /api/v1/auth/csrf` rehydrate.  
      Bằng chứng: `authService.ts:38-83`; `useAuth.ts:116-142`; `App.tsx:103-108`.

### 3.3 Security

- [x] **RC-08** — Hash/password/enabled user/session creation đúng rule.  
      Bằng chứng: `UserDetailsServiceImpl.java:21-31`; `R__seed_dev_test_users.sql:6-23`; `AuthControllerIT.java:97-110`.
- [x] **RC-09** — Cookie policy đúng theo môi trường; logout clear cookie.  
      Bằng chứng: `application-dev.properties:1-3`; `application-staging.properties:1-3`; `application-prod.properties:1-3`; `AuthService.java:83-97`; `StagingProfileIT.java:67-80`.
- [x] **RC-10** — CORS allowlist/credentials/headers/methods đúng.  
      Bằng chứng: `SecurityConfig.java:82-94`; `AuthControllerIT.java:197-209`.
- [x] **RC-11** — Protected mutating endpoints enforce session + CSRF đầy đủ.  
      Bằng chứng: `SecurityConfig.java:47-74`; `AuthControllerIT.java:135-180`.
- [x] **RC-12** — Session invalidation/fixation semantics đúng.  
      Bằng chứng: `SecurityConfig.java:63-64,115-116`; `AuthService.java:83-98`; `AuthSessionTimeoutIT.java:58-91`.
- [x] **RC-13** — Seed isolation đúng theo profile.  
      Bằng chứng: `application-dev.properties:4`; `application-staging.properties:4`; `application-prod.properties:4`; `FlywayLocationGuard.java:23-29`; `StagingProfileIT.java:54-80`.
- [x] **RC-14** — Không leak secret/PII trong log, docs, responses.  
      Bằng chứng: `AuditLogger.java:15-47`; `AuditLoggerTest.java:34-53`; `GlobalAuthExceptionHandler.java:38-85`.

### 3.4 Performance

- [x] **RC-15** — Không N+1 / query thừa khi load user/roles.  
      Bằng chứng: `UserRepository.java:9-16` dùng `left join fetch user.roles`.
- [x] **RC-16** — `login/csrf/logout` không có blocking I/O hoặc payload thừa ngoài DB nội bộ.  
      Bằng chứng: `AuthService.java:47-98`; không có external client/dependency ngoài PostgreSQL.
- [x] **RC-17** — TTL test không flaky, không dùng sleep dài vô ích.  
      Bằng chứng: `AuthSessionTimeoutIT.java:81-91` expire session trực tiếp ở server-side, không dựa vào sleep dài.

### 3.5 Compatibility

- [x] **RC-18** — Code/Swagger/FE contract đồng nhất.  
      Bằng chứng: `AuthController.java:40-83`; `SpringdocConfig.java:13-30`; `authService.ts:61-83`; `App.tsx:97-108`.
- [x] **RC-19** — Hành vi đúng theo `dev/staging/prod`.  
      Bằng chứng: `application-dev.properties:1-6`; `application-staging.properties:1-6`; `application-prod.properties:1-6`; `StagingProfileIT.java:54-80`; `ProdProfileIT.java:43-58`.
- [x] **RC-20** — Flyway startup trên DB trống + rollback path rõ.  
      Bằng chứng: `V1__create_auth_tables.sql:1-22`; `docs/changes/auth/impl-plan.md` phần 6; `./gradlew.bat test` = PASS.
- [x] **RC-21** — Local workaround không làm suy yếu contract `staging/prod`.  
      Bằng chứng: code không nới lỏng CORS/cookie cho `staging/prod`; `application-staging.properties:1-6`; `application-prod.properties:1-6`; `StagingProfileIT.java:54-80`.

### 3.6 Logs / Audit

- [x] **RC-22** — Đủ audit events bắt buộc.  
      Bằng chứng: `AuditLogger.java:15-40`; `AuditLoggerTest.java:34-53`.
- [x] **RC-23** — Log đủ chẩn đoán nhưng không leak secret.  
      Bằng chứng: `AuditLogger.java:15-47`; `AuditLoggerTest.java:43-49`.

### 3.7 Error Handling

- [x] **RC-24** — Error envelope + code mapping đúng.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:38-85`; `GlobalAuthExceptionHandlerTest.java:26-88`.
- [x] **RC-25** — `session required` vs `session expired` được phân biệt đúng chỗ.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:42-49,74-76`; `AuthSessionTimeoutIT.java:58-91`.
- [x] **RC-26** — Không leak stack trace/internal details.  
      Bằng chứng: `GlobalAuthExceptionHandler.java:38-85`; responses trong `AuthControllerIT.java:82-159`.

### 3.8 Testing

- [x] **RC-27** — Automated test coverage đủ cho auth flow/cookie/csrf/cors/flyway/swagger/errors.  
      Bằng chứng: `AuthControllerIT.java:64-209`; `AuthSessionTimeoutIT.java:58-91`; `StagingProfileIT.java:54-80`; `ProdProfileIT.java:43-58`; `GlobalAuthExceptionHandlerTest.java:26-88`; `AuditLoggerTest.java:34-53`; `UserDetailsServiceImplTest.java:27-78`.
- [ ] **RC-28** — FE/E2E/manual coverage đủ cho username-only UI + reload rehydrate + expired session + logout.  
      Bằng chứng hiện có: browser smoke tại `http://localhost:5173` xác nhận form chỉ có `Username` + `Password`; network cho thấy `POST /api/v1/auth/login` trả `200`, nhưng browser không giữ được dev cookie do local HTTP + `SameSite=None`/`Secure=false`, nên chưa đi hết reload/logout bằng browser.
- [x] **RC-29** — Fixtures/seed data khớp spec.  
      Bằng chứng: `R__seed_dev_test_users.sql:1-23`; `AuthControllerIT.java:183-195`.

### 3.9 Operations

- [x] **RC-30** — Config vận hành bắt buộc đã được tài liệu hóa.  
      Bằng chứng: `docs/changes/auth/impl-plan.md` phần 2, 4, 7; `application*.properties`.
- [x] **RC-31** — Rollback/runbook đủ chi tiết.  
      Bằng chứng: `docs/changes/auth/impl-plan.md` phần 6.
- [x] **RC-32** — Có smoke checklist trước merge / trước deploy.  
      Bằng chứng: `docs/changes/auth/impl-plan.md` phần 7.

---

## 4. Tóm tắt diff đã triển khai

### 4.1 Các tệp/chủ đề đã thay đổi

| Khu vực           | Tệp / thư mục | Mô tả thay đổi | Bằng chứng |
| ----------------- | ------------- | -------------- | ---------- |
| Backend           | `demo/src/main/java/com/example/demo/...` | Thêm auth module hoàn chỉnh: config, controller, service, JPA, error handling, audit logging | `SecurityConfig.java`, `AuthController.java`, `AuthService.java`, `GlobalAuthExceptionHandler.java` |
| Frontend          | `my-react-app/src/...` | Thêm `authService`, `useAuth`, `LoginPage`, màn hình authenticated/rehydrate/logout | `authService.ts`, `useAuth.ts`, `LoginPage.tsx`, `App.tsx` |
| Database / Flyway | `demo/src/main/resources/db/...` | Thêm schema auth và repeatable seed non-prod | `V1__create_auth_tables.sql`, `R__seed_dev_test_users.sql` |
| Config / Profiles | `demo/src/main/resources/application*.properties` | Thêm datasource, session cookie policy, timeout, Flyway locations, Springdoc policy theo profile | `application.properties`, `application-dev.properties`, `application-staging.properties`, `application-prod.properties`, `application-test.properties` |
| Docs / OpenAPI    | `SpringdocConfig.java`, controller annotations | Khai báo OpenAPI + security schemes cho cookie session và CSRF header | `SpringdocConfig.java:13-30`, `AuthController.java:40-83` |
| Tests             | `demo/src/test/java/com/example/demo/...` | Thêm UT + IT với PostgreSQL container và HTTP runtime checks | `AuthControllerIT.java`, `AuthSessionTimeoutIT.java`, `StagingProfileIT.java`, `ProdProfileIT.java` |

### 4.2 Điểm cần reviewer chú ý nhất

- Custom CSRF matcher trong `SecurityConfig.java:47-62` được chỉnh để anonymous `POST` vào protected endpoint trả `401` trước, còn authenticated-but-no-CSRF mới trả `403`.
- Seed non-prod được chuyển sang repeatable migration idempotent (`R__seed_dev_test_users.sql:1-23`) để tránh collision version giữa `db/migration` và `db/seed`.
- Frontend rehydrate giữ đúng spec: chỉ gọi `GET /api/v1/auth/csrf`; phần hiển thị `username/roles` sau reload dựa trên snapshot trong `sessionStorage`, không thêm auth-status endpoint.
- Bootstrap hiện normalize alias JVM timezone `Asia/Saigon` sang `Asia/Ho_Chi_Minh` trong `DemoApplication.java` để tránh PostgreSQL reject startup parameter `TimeZone`.

---

## 5. Các lệnh đã chạy và kết quả

### 5.1 Static checks / lint / format

| #   | Khu vực  | Lệnh | Kết quả (PASS/FAIL) | Ghi chú | Bằng chứng |
| --- | -------- | ---- | ------------------- | ------- | ---------- |
| 1   | Backend  | `cd demo && .\gradlew.bat compileJava` | PASS | Resolve dependency + compile backend | Chạy trong turn này |
| 2   | Frontend | `cd my-react-app && npm run lint` | PASS | ESLint sạch sau khi sửa hook/type import | Chạy trong turn này |
| 3   | Khác     | `cd my-react-app && npm install` | PASS | Tạo `package-lock.json`, cài dependency chuẩn của app | Chạy trong turn này |

### 5.2 Unit / integration / build

| #   | Khu vực       | Lệnh | Kết quả (PASS/FAIL) | Coverage / scope | Bằng chứng |
| --- | ------------- | ---- | ------------------- | ---------------- | ---------- |
| 1   | Backend UT    | `cd demo && .\gradlew.bat test` | PASS | `UserDetailsServiceImplTest`, `GlobalAuthExceptionHandlerTest`, `AuditLoggerTest` | `./gradlew.bat test` = BUILD SUCCESSFUL |
| 2   | Backend IT    | `cd demo && .\gradlew.bat test` | PASS | `AuthControllerIT`, `AuthSessionTimeoutIT`, `StagingProfileIT`, `ProdProfileIT` | `./gradlew.bat test` = BUILD SUCCESSFUL |
| 3   | Frontend test | `cd my-react-app && npm run build` | PASS | TypeScript + Vite production build | `npm run build` = PASS |
| 4   | Build         | `cd demo && .\gradlew.bat testClasses` | PASS | Compile test sources trước full test run | Chạy trong turn này |

### 5.3 Manual verification / curl / browser

| #   | Tình huống | Lệnh / bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Bằng chứng |
| --- | ---------- | --------------------- | ---------------- | --------------- | ---------- |
| 1   | Login screen | Mở `http://localhost:5173` bằng Playwright | Chỉ có `Username` + `Password` | PASS | Snapshot browser hiển thị đúng hai field |
| 2   | Login success | Click `Đăng nhập` tại `http://localhost:5173` | UI chuyển sang authenticated | PARTIAL | Network ghi nhận `POST /api/v1/auth/login` = `200`, nhưng browser không giữ dev cookie |
| 3   | Lấy CSRF token | App tự gọi `GET /api/v1/auth/csrf` sau login | `200` nếu session cookie giữ được | FAIL trên local HTTP | Browser gửi `GET /api/v1/auth/csrf` = `401` sau login vì cookie bị browser chặn |
| 4   | Protected request thiếu session | Không chạy curl riêng | `401` | Covered by automated IT | `AuthControllerIT.java:135-140` |
| 5   | Protected request thiếu/sai CSRF | Không chạy curl riêng | `403` | Covered by automated IT | `AuthControllerIT.java:149-159` |
| 6   | Logout | Không đi hết browser smoke do cookie bị chặn từ bước trước | `200` + clear cookie | Covered by automated IT | `AuthControllerIT.java:162-180` |
| 7   | CORS preflight | Browser smoke từ `http://127.0.0.1:15173` và automated IT | Origin khác allowlist bị chặn; `localhost:5173` được phép | PASS | CORS block đúng trên `127.0.0.1`; `AuthControllerIT.java:197-209` pass cho `localhost:5173` |
| 8   | Swagger `dev/staging` | `Invoke-WebRequest http://localhost:8080/v3/api-docs`; `StagingProfileIT` | `200` | PASS | dev readiness check pass; `StagingProfileIT.java:62-80` |
| 9   | Swagger `prod` not public | Automated IT | `404` hoặc equivalent not public | PASS | `ProdProfileIT.java:43-58` |
| 10  | Boot với timezone alias | `cd demo && gradlew.bat -Duser.timezone=Asia/Saigon bootRun --args="--spring.profiles.active=dev --server.port=18082"` với PostgreSQL local | App khởi động, không còn `FATAL invalid value for parameter "TimeZone"` | PASS | `bootrun-tzfix.out.log` ghi `Started DemoApplication` |

### 5.4 Lệnh mẫu đã chạy

```bash
# Backend
cd demo && .\gradlew.bat compileJava
cd demo && .\gradlew.bat testClasses
cd demo && .\gradlew.bat test

# Frontend
cd my-react-app && npm install
cd my-react-app && npm run lint
cd my-react-app && npm run build
```

---

## 6. Known risks

| #   | Risk | Mức độ | Ảnh hưởng | Mitigation hiện có | Cần theo dõi thêm |
| --- | ---- | ------ | --------- | ------------------ | ----------------- |
| 1   | Browser local HTTP có thể chặn cookie `SameSite=None` + `Secure=false` ở `dev` | Major | Chặn browser smoke đầy đủ cho `login -> csrf -> reload -> logout` | Spec đã nêu rủi ro; backend vẫn giữ contract mục tiêu cho `staging/prod`; automated IT pass | Cần manual smoke trên HTTPS local hoặc môi trường gần staging |
| 2   | Repo hiện vẫn dùng `Spring Boot 3.5.12-SNAPSHOT` + `Java 25` từ skeleton ban đầu | Minor | Toolchain snapshot có thể tăng biến động ngoài scope ticket | Không đổi toolchain, chỉ bám cấu hình repo hiện tại | Team nên xác nhận policy snapshot riêng |
| 3   | Sau hard reload/new tab mà mất `sessionStorage`, FE vẫn rehydrate protected flow nhưng không còn `username/roles` để hiển thị đầy đủ | Minor | UI có thể hiện “Phiên đã được khôi phục” thay vì tên user | Không thêm `/auth/me` hay `/auth/status` để giữ scope/spec | Nếu cần identity sau hard reload, cần spec mới |

## 7. Not handled yet

| #   | Hạng mục chưa xử lý | Vì sao chưa làm | Liên quan AC/RC nào | Quyết định hiện tại |
| --- | ------------------- | --------------- | ------------------- | ------------------- |
| 1   | Browser smoke hoàn chỉnh trên local HTTP cho `login -> reload -> logout` | Cookie dev bị browser chặn vì policy `SameSite=None` / `Secure=false` | RC-28 | Giữ implementation theo spec; chuyển sang follow-up môi trường |
| 2   | Manual curl runbook riêng ngoài automated IT | `./gradlew.bat test` đã cover tương đương hầu hết flow backend | RC-32 | Có thể bổ sung sau nếu reviewer muốn artifact curl riêng |
| 3   | Chứng minh runtime `dev` cookie hoạt động trên HTTPS local | Cần topology/local cert ngoài scope ticket | AC-auth-26, RC-21, RC-28 | Để manual verification ở môi trường gần staging |

## 8. Remaining issues / follow-ups

| #   | Vấn đề còn lại | Severity | Cách tái hiện / bối cảnh | Hướng xử lý đề xuất | Owner |
| --- | -------------- | -------- | ------------------------ | ------------------- | ----- |
| 1   | Browser local smoke không giữ được dev session cookie sau `POST /api/v1/auth/login` | Major | Mở `http://localhost:5173`, login `user01/User@123`, network cho thấy login `200` nhưng `GET /api/v1/auth/csrf` tiếp theo trả `401` | Chạy manual smoke trên HTTPS local hoặc môi trường staging-like để xác nhận browser behavior thật | Team / reviewer |
| 2   | Chưa có FE test automation (unit/E2E) riêng; hiện mới có build/lint + partial browser smoke | Minor | Không có test runner frontend trong repo hiện tại | Cân nhắc thêm FE test ở ticket sau nếu team muốn CI chặt hơn | Team |
| 3   | Snapshot toolchain chưa được chuẩn hóa trong ticket này | Minor | `build.gradle` vẫn là `3.5.12-SNAPSHOT` + `Java 25` | Quyết định ở ticket/toolchain riêng, tránh nở scope tại đây | Team |

---

## 9. Kết luận tự review

- [x] Tất cả AC đã được implement và có bằng chứng code/test tương ứng.
- [x] Không còn Blocker trong `review-checklist.md`.
- [x] Các Major còn lại (nếu có) đã được ghi rõ ở mục “Remaining issues / follow-ups”.
- [x] Tài liệu/runbook/test evidence đủ để chuyển sang review thủ công.

**Kết luận đề xuất:** [ ] Ready for manual review [x] Cần sửa thêm trước khi review

**Tóm tắt ngắn cho reviewer tiếp theo:**

- Backend auth foundation hoàn chỉnh và `./gradlew.bat test` đang PASS với PostgreSQL thật qua Testcontainers.
- Frontend đã bám đúng contract `Username` + session cookie + `GET /api/v1/auth/csrf`, và `npm run lint`, `npm run build` đều PASS.
- Điểm còn lại là browser-level local smoke bị chặn bởi cookie policy ở dev HTTP; đây là rủi ro môi trường đã được spec nêu trước, không phải crash code.
