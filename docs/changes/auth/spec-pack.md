# Spec Pack — TICKET: auth
**Story ID**: AUTH-001
**Title**: Authentication bằng Session Cookie + CSRF API riêng, tích hợp Springdoc, PostgreSQL, Flyway
**Epic**: Authentication & Platform Foundation
**Trạng thái**: ✅ Sẵn sàng triển khai (xem mục "Quyết định" cuối tài liệu)
_Cập nhật: 2026-03-13_

---

## 1. Background / Purpose

Hệ thống cần cơ chế xác thực an toàn theo mô hình **server-side session**:

- Trình duyệt giữ session id trong cookie.
- Frontend và backend tích hợp **cross-site** (khác domain/port).
- Các thao tác thay đổi dữ liệu phải được bảo vệ chống CSRF.
- CSRF token cấp qua **API riêng** (không nhúng trong login response).
- Hạ tầng backend được chuẩn hóa: Springdoc (tài liệu API), PostgreSQL (database), Flyway (migration).

---

## 2. Scope

### In scope

1. Đăng nhập bằng username/password → session cookie.
2. Duy trì phiên bằng session cookie (server-side, in-memory).
3. Cross-site integration (CORS + cookie policy).
4. Cấp CSRF token qua API riêng (`GET /api/v1/auth/csrf`).
5. Validate CSRF cho các request thay đổi dữ liệu.
6. Logout và invalidate session.
7. Tích hợp Springdoc (bật dev/staging, tắt prod).
8. PostgreSQL làm database chính (lưu users, roles).
9. Flyway migration (tự động khi khởi động).
10. Seed user mặc định cho môi trường dev/test qua Flyway V3 conditional.
11. Error response theo RFC 7807 Problem Details.
12. Wireframe ASCII cho frontend login flow.

### Out of scope

1. Social login (OAuth2/OIDC).
2. MFA / 2FA.
3. Remember me / persistent login.
4. JWT / refresh token.
5. Redis session store.
6. Quên mật khẩu / reset mật khẩu.
7. Phân quyền phức tạp nhiều tầng (RBAC nâng cao).
8. CORS origins cho staging/prod (xem Open Issues).

---

## 3. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| **Session** | Phiên làm việc phía server, định danh bằng session id ngẫu nhiên |
| **Session cookie** | Cookie trình duyệt lưu session id (`JSESSIONID` mặc định với Spring) |
| **CSRF** | Cross-Site Request Forgery — tấn công giả mạo request từ origin khác |
| **CSRF token** | Token ngẫu nhiên, gắn với session, gửi kèm request mutating để chống CSRF |
| **SameSite=None** | Cho phép cookie được gửi trong cross-site request |
| **HttpOnly** | Cookie không thể đọc bằng JavaScript |
| **Secure** | Cookie chỉ được gửi qua HTTPS |
| **Cross-site** | FE và BE chạy trên domain hoặc port khác nhau |
| **In-memory session** | Session lưu trong RAM của JVM, không persist, không share giữa nhiều instance |
| **Flyway** | Công cụ quản lý database migration theo phiên bản |
| **Springdoc** | Thư viện tự động sinh OpenAPI 3.x document từ Spring MVC annotations |
| **RFC 7807** | Chuẩn HTTP API error response: `type`, `title`, `status`, `detail`, `instance` |
| **BCrypt** | Thuật toán hash password có salt, cost factor điều chỉnh được |
| **Idle timeout** | Thời gian tối đa không hoạt động trước khi session bị hủy |

---

## 4. As-Is / To-Be

### As-Is

- Không có cơ chế xác thực (ứng dụng chưa có auth).
- Chưa có PostgreSQL, Flyway, Springdoc tích hợp.

### To-Be

```
[Browser]
   |-- POST /api/v1/auth/login --> [Spring Security]
   |<-- 200 OK + Set-Cookie: JSESSIONID --
   |
   |-- GET /api/v1/auth/csrf --> [CSRF Endpoint]
   |<-- 200 OK + { csrfToken, headerName, parameterName } --
   |
   |-- POST /api/v1/some-data + X-CSRF-TOKEN --> [Protected API]
   |<-- 200 OK --
   |
   |-- POST /api/v1/auth/logout + X-CSRF-TOKEN --> [Logout]
   |<-- 200 OK + Clear-Cookie --

[Spring Boot]
   |-- Spring Security (session + CSRF)
   |-- PostgreSQL (users, roles via JPA)
   |-- Flyway (auto-migration on startup)
   |-- Springdoc (OpenAPI docs, dev/staging only)
```

---

## 5. Spec Details

### 5.1. Login

**Endpoint**: `POST /api/v1/auth/login`

**Request body**:
```json
{
  "username": "user01",
  "password": "User@123"
}
```

**Response 200 OK** (+ `Set-Cookie: JSESSIONID=...`):
```json
{
  "authenticated": true,
  "username": "user01",
  "roles": ["USER"]
}
```

**Response 401 Unauthorized** (RFC 7807):
```json
{
  "type": "https://errors.example.com/auth/invalid-credentials",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Invalid username or password.",
  "instance": "/api/v1/auth/login"
}
```

**Cookie policy**:

| Attribute | dev | staging | prod |
|-----------|-----|---------|------|
| `HttpOnly` | true | true | true |
| `SameSite` | Lax | None | None |
| `Secure` | false (không bắt buộc) | true | true |
| `Path` | / | / | / |

---

### 5.2. CSRF Token

**Endpoint**: `GET /api/v1/auth/csrf`
Chỉ trả token khi session hợp lệ. Frontend gọi ngay sau login thành công và khi khởi động lại app nếu session còn hợp lệ.

**Response 200 OK**:
```json
{
  "csrfToken": "<generated-token>",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

**Response 401 Unauthorized** (không có session / session hết hạn):
```json
{
  "type": "https://errors.example.com/auth/session-required",
  "title": "Session Required",
  "status": 401,
  "detail": "A valid session is required to obtain a CSRF token.",
  "instance": "/api/v1/auth/csrf"
}
```

---

### 5.3. Protected APIs

Các request `POST`, `PUT`, `PATCH`, `DELETE` đến protected endpoints yêu cầu:
1. Session hợp lệ (cookie `JSESSIONID`).
2. CSRF token hợp lệ qua header `X-CSRF-TOKEN`.

**Response 401** (thiếu/hết hạn session): RFC 7807, status 401.
**Response 403** (thiếu hoặc sai CSRF token): RFC 7807, status 403.

---

### 5.4. Logout

**Endpoint**: `POST /api/v1/auth/logout`
Yêu cầu: session hợp lệ + CSRF token hợp lệ.

**Response 200 OK** (+ `Set-Cookie: JSESSIONID=; Max-Age=0`):
```json
{
  "success": true
}
```

---

### 5.5. Session Timeout (Idle)

| Môi trường | Idle timeout |
|-----------|-------------|
| dev | 8 giờ |
| staging | 2 giờ |
| prod | 30 phút |

---

### 5.6. CORS

| Attribute | Giá trị |
|-----------|--------|
| `allowedOrigins` | `http://localhost:5173` (dev), `http://staging:5173` (staging); prod xem OI-A' |
| `allowCredentials` | true |
| `allowedMethods` | GET, POST, PUT, PATCH, DELETE, OPTIONS |
| `allowedHeaders` | Content-Type, X-CSRF-TOKEN |

---

### 5.7. Database Schema

**Bảng `users`**:

| Column | Type | Constraint |
|--------|------|-----------|
| `id` | BIGSERIAL | PK |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT true |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Bảng `roles`**:

| Column | Type | Constraint |
|--------|------|-----------|
| `id` | BIGSERIAL | PK |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE |
| `name` | VARCHAR(100) | NOT NULL |

**Bảng `user_roles`**:

| Column | Type | Constraint |
|--------|------|-----------|
| `user_id` | BIGINT | FK → users.id |
| `role_id` | BIGINT | FK → roles.id |
| | | PK (user_id, role_id) |

---

### 5.8. Flyway Migrations

| File | Nội dung | Môi trường |
|------|----------|-----------|
| `V1__init_schema.sql` | Extension, schema setup | Tất cả |
| `V2__create_auth_tables.sql` | Tạo bảng users, roles, user_roles | Tất cả |
| `V3__seed_dev_test_users.sql` | Seed `admin`, `user01` với BCrypt hash | **Chỉ dev/test** (conditional theo Spring profile) |

> Cơ chế conditional V3: Flyway `spring.flyway.locations` theo profile, ví dụ dev profile include thêm `classpath:db/migration/seed`.

---

### 5.9. Password Hashing

- Thuật toán: **BCrypt**
- Cost factor: **12**
- Không được log password hoặc hash ở bất kỳ level nào.

---

### 5.10. Springdoc

| Môi trường | OpenAPI JSON | Swagger UI |
|-----------|-------------|-----------|
| dev | ✅ `/v3/api-docs` | ✅ `/swagger-ui/index.html` |
| staging | ✅ `/v3/api-docs` | ✅ `/swagger-ui/index.html` |
| prod | ❌ | ❌ |

Tài liệu phải mô tả: login, csrf, logout, cookie policy, header `X-CSRF-TOKEN`, mã trạng thái, mẫu request/response.

---

### 5.11. Wireframes (ASCII)

**Màn hình Login**:
```
+----------------------------------------------------------+
| LOGO                                                     |
|----------------------------------------------------------|
|              ĐĂNG NHẬP HỆ THỐNG                          |
|                                                          |
|  Username                                                |
|  +----------------------------------------------------+  |
|  | user01                                             |  |
|  +----------------------------------------------------+  |
|                                                          |
|  Password                                                |
|  +----------------------------------------------------+  |
|  | ********                                           |  |
|  +----------------------------------------------------+  |
|                                                          |
|  [ Sai tên đăng nhập hoặc mật khẩu ] (error area)       |
|                                                          |
|  +----------------------+                                |
|  |      ĐĂNG NHẬP       |                                |
|  +----------------------+                                |
+----------------------------------------------------------+
```

**Dashboard sau login thành công**:
```
+----------------------------------------------------------+
| My App                              user01  [Logout]     |
|----------------------------------------------------------|
|  Welcome, user01                                         |
|  Session: Active | CSRF token: Loaded                    |
|  [Create]  [Update]  [Delete]                            |
+----------------------------------------------------------+
```

**Session hết hạn**:
```
+----------------------------------------------------------+
|          PHIÊN ĐĂNG NHẬP ĐÃ HẾT HẠN                     |
|  Vui lòng đăng nhập lại để tiếp tục.                     |
|  +----------------------+                                |
|  |    ĐĂNG NHẬP LẠI     |                                |
|  +----------------------+                                |
+----------------------------------------------------------+
```

---

## 6. Non-functional Requirements

| # | Yêu cầu |
|---|---------|
| NFR-1 | Password phải hash bằng BCrypt cost factor 12 trước khi lưu. |
| NFR-2 | Không log password, session id, CSRF token ở bất kỳ level nào. |
| NFR-3 | CORS phải whitelist đúng frontend origins, không dùng wildcard khi `allowCredentials=true`. |
| NFR-4 | Cấu hình session timeout và cookie policy phải tách rõ theo Spring profile. |
| NFR-5 | Seed data V3 chỉ áp dụng cho profile `dev` và `test`, không bao giờ chạy ở `prod`. |
| NFR-6 | Swagger UI không được public ở production. |
| NFR-7 | Error response phải theo RFC 7807 Problem Details (fields: `type`, `title`, `status`, `detail`, `instance`). |
| NFR-8 | In-memory session không phù hợp cho multi-instance; phải document giới hạn này. |

---

## 7. Acceptance Criteria

### Authentication / Session

**AC-1**: Hệ thống phải cung cấp endpoint `POST /api/v1/auth/login` nhận `username` và `password`.

**AC-2**: Khi đăng nhập thành công, hệ thống phải tạo server-side session và trả session id qua `Set-Cookie` header trong response.

**AC-3**: Session cookie phải có attribute `HttpOnly=true`.

**AC-4**: Session cookie phải có cookie policy theo môi trường: `dev` dùng `SameSite=Lax`; `staging` và `prod` dùng `SameSite=None`.

**AC-5**: Ở môi trường `staging` và `prod`, session cookie phải có attribute `Secure=true`.

**AC-6**: Khi đăng nhập thất bại (sai username hoặc password), hệ thống phải trả HTTP `401 Unauthorized` theo RFC 7807 và không tạo session hợp lệ.

**AC-7**: Chỉ user có `enabled=true` mới đăng nhập thành công; user có `enabled=false` nhận `401 Unauthorized`.

**AC-8**: Session idle timeout phải được cấu hình theo môi trường: dev=8h, staging=2h, prod=30m.

### CSRF

**AC-9**: Hệ thống phải cung cấp endpoint `GET /api/v1/auth/csrf` trả CSRF token trong response body JSON với các field `csrfToken`, `headerName`, `parameterName`.

**AC-10**: Endpoint `GET /api/v1/auth/csrf` chỉ trả token khi request đính kèm session cookie hợp lệ.

**AC-11**: Khi gọi `GET /api/v1/auth/csrf` mà không có session hợp lệ, hệ thống phải trả `401 Unauthorized` theo RFC 7807.

**AC-12**: Các request `POST`, `PUT`, `PATCH`, `DELETE` đến protected endpoints phải yêu cầu CSRF token hợp lệ trong header `X-CSRF-TOKEN`.

**AC-13**: Khi request mutating thiếu header `X-CSRF-TOKEN`, hệ thống phải trả `403 Forbidden` theo RFC 7807.

**AC-14**: Khi request mutating có giá trị `X-CSRF-TOKEN` sai hoặc không khớp với session, hệ thống phải trả `403 Forbidden` theo RFC 7807.

**AC-15**: CSRF token phải có thể được gửi qua header `X-CSRF-TOKEN`.

### Logout

**AC-16**: Hệ thống phải cung cấp endpoint `POST /api/v1/auth/logout`.

**AC-17**: Khi logout thành công, session hiện tại phải bị invalidate phía server (session id không còn hợp lệ cho request tiếp theo).

**AC-18**: Khi logout thành công, response phải chứa `Set-Cookie` header clear/expire session cookie.

### Springdoc

**AC-19**: Ứng dụng phải tích hợp Springdoc và sinh OpenAPI document tại `/v3/api-docs`.

**AC-20**: Swagger UI phải truy cập được tại `/swagger-ui/index.html` khi chạy với profile `dev` hoặc `staging`.

**AC-21**: Khi chạy với profile `prod`, endpoint `/swagger-ui/index.html` và `/v3/api-docs` phải trả `404` hoặc không được public.

**AC-22**: Tài liệu API phải bao gồm mô tả của 3 endpoint auth (login, csrf, logout) với request/response mẫu và các mã trạng thái chính.

### PostgreSQL / Flyway

**AC-23**: Ứng dụng phải kết nối được với PostgreSQL theo cấu hình môi trường.

**AC-24**: Dữ liệu người dùng (bảng `users`, `roles`, `user_roles`) phải được lưu trong PostgreSQL.

**AC-25**: Flyway phải tự động chạy migration khi khởi động ứng dụng.

**AC-26**: Migration `V2__create_auth_tables.sql` phải tạo đúng các bảng `users`, `roles`, `user_roles` với constraints được mô tả.

**AC-27**: Migration `V3__seed_dev_test_users.sql` phải tạo user `admin` và `user01` với password đã BCrypt hash khi chạy với profile `dev` hoặc `test`.

**AC-28**: Migration V3 không được chạy khi chạy với profile `staging` hoặc `prod`.

### Session Store

**AC-29**: Hệ thống phải sử dụng in-memory session mặc định của Spring; không được cấu hình Redis hoặc persistent session store.

### CORS

**AC-30**: Backend phải cấu hình CORS cho phép origin `http://localhost:5173` với `allowCredentials=true`.

**AC-31**: Backend không được cấu hình CORS wildcard (`*`) khi `allowCredentials=true`.

### Error Format

**AC-32**: Tất cả error response phải theo định dạng RFC 7807 Problem Details với ít nhất các field: `type`, `title`, `status`, `detail`, `instance`.

### Frontend Routing / Dev Browser Behavior

**AC-33**: Sau khi `POST /api/v1/auth/login` thành công, frontend phải gọi `GET /api/v1/auth/csrf` với chính `JSESSIONID` vừa nhận và nhận `200 OK` trên flow HTTP thực tế (bao gồm dev proxy `localhost:5173 -> localhost:8080`).

**AC-34**: Ở môi trường `dev`, cookie session sau login phải được trình duyệt Chromium chấp nhận trên local HTTP khi frontend đi qua Vite proxy `http://localhost:5173`, không yêu cầu HTTPS local chỉ để duy trì session login cơ bản.

**AC-35**: Sau khi login thành công và frontend lấy được CSRF token, người dùng phải được điều hướng từ `/login` sang dashboard route `/`.

**AC-36**: Nếu người dùng đã authenticated mà truy cập route `/login`, frontend phải tự động redirect về `/` thay vì tiếp tục hiển thị form login.

---

## 8. Examples

### 8.1. Normal Flow 1 — Login thành công và lấy CSRF token

```
1. Client: POST /api/v1/auth/login
   Body: { "username": "user01", "password": "User@123" }

2. Server: 200 OK
   Set-Cookie: JSESSIONID=abc123; HttpOnly; SameSite=Lax; Path=/
   Body: { "authenticated": true, "username": "user01", "roles": ["USER"] }

3. Client: GET /api/v1/auth/csrf
   Cookie: JSESSIONID=abc123

4. Server: 200 OK
   Body: { "csrfToken": "xyz789", "headerName": "X-CSRF-TOKEN", "parameterName": "_csrf" }

5. Client lưu csrfToken = "xyz789" vào memory state.

6. Frontend redirect người dùng sang route "/".
```

---

### 8.2. Normal Flow 2 — Gọi protected API thành công

```
Điều kiện: user01 đã login, csrfToken = "xyz789"

1. Client: POST /api/v1/some-resource
   Cookie: JSESSIONID=abc123
   X-CSRF-TOKEN: xyz789
   Body: { ... }

2. Server: 200 OK
   Body: { ... resource created ... }
```

---

### 8.3. Normal Flow 3 — Logout thành công

```
Điều kiện: user01 đang có session, csrfToken = "xyz789"

1. Client: POST /api/v1/auth/logout
   Cookie: JSESSIONID=abc123
   X-CSRF-TOKEN: xyz789

2. Server: 200 OK
   Set-Cookie: JSESSIONID=; Max-Age=0; HttpOnly; Path=/
   Body: { "success": true }

3. Request tiếp theo với JSESSIONID=abc123 → Server trả 401.
```

---

### 8.4. Error Flow 1 — Login thất bại (sai password)

```
1. Client: POST /api/v1/auth/login
   Body: { "username": "user01", "password": "wrongpassword" }

2. Server: 401 Unauthorized
   Body: {
     "type": "https://errors.example.com/auth/invalid-credentials",
     "title": "Authentication Failed",
     "status": 401,
     "detail": "Invalid username or password.",
     "instance": "/api/v1/auth/login"
   }

Kết quả: Không có Set-Cookie header, không có session được tạo.
```

---

### 8.5. Error Flow 2 — Gọi protected API thiếu CSRF token

```
Điều kiện: user01 đã login (session hợp lệ)

1. Client: POST /api/v1/some-resource
   Cookie: JSESSIONID=abc123
   (không có X-CSRF-TOKEN header)

2. Server: 403 Forbidden
   Body: {
     "type": "https://errors.example.com/csrf/token-missing",
     "title": "CSRF Token Missing",
     "status": 403,
     "detail": "The CSRF token is missing from the request.",
     "instance": "/api/v1/some-resource"
   }
```

---

### 8.6. Edge Case 1 — Gọi /csrf khi session đã hết hạn (app reload)

```
Tình huống: User đã login trước đó, reload trình duyệt sau khi session hết hạn.
Frontend gọi lại /csrf để kiểm tra.

1. Client: GET /api/v1/auth/csrf
   Cookie: JSESSIONID=expired_or_invalid

2. Server: 401 Unauthorized
   Body: {
     "type": "https://errors.example.com/auth/session-required",
     "title": "Session Required",
     "status": 401,
     "detail": "A valid session is required to obtain a CSRF token.",
     "instance": "/api/v1/auth/csrf"
   }

Xử lý FE: Điều hướng về màn hình login.
```

---

### 8.7. Edge Case 2 — Gọi protected API với CSRF token đã lỗi thời (sai token)

```
Tình huống: CSRF token trong memory của FE không khớp với session hiện tại
(ví dụ do FE bug hoặc token cũ từ session khác).

1. Client: DELETE /api/v1/some-resource/1
   Cookie: JSESSIONID=abc123
   X-CSRF-TOKEN: old_or_wrong_token

2. Server: 403 Forbidden
   Body: {
     "type": "https://errors.example.com/csrf/token-invalid",
     "title": "CSRF Token Invalid",
     "status": 403,
     "detail": "The CSRF token provided does not match the expected value.",
     "instance": "/api/v1/some-resource/1"
   }

Xử lý FE: Hiển thị "Phiên bảo mật không hợp lệ", đề xuất tải lại trang hoặc login lại.
```

---

## 9. Open Issues

> Các mục dưới đây **chưa được chốt** và **phải được quyết định trước khi triển khai** những phần liên quan.

| ID | Câu hỏi | Tác động | Ưu tiên |
|----|---------|---------|--------|
| **OI-A'** | CORS `allowedOrigins` cho môi trường `prod` là gì? | Nếu không cấu hình đúng, FE cross-site không hoạt động ở prod | 🔴 Cao — cần trước khi deploy prod |
| **OI-B** | RFC 7807 field `type` dùng URI thực hay URI giả định (placeholder)? Convention namespace là gì? | Ảnh hưởng đến tất cả error response format | 🟡 Trung bình — có thể dùng placeholder ban đầu |
| **OI-C** | Staging/prod có cần HTTPS local cert để test cookie `Secure=true` không? Quy trình dev setup là gì? | Dev experience, onboarding | 🟡 Trung bình |

---

## 10. Risks

| # | Rủi ro | Xác suất | Tác động | Giảm thiểu |
|---|--------|---------|---------|-----------|
| R-1 | Cross-site cookie phụ thuộc CORS + HTTPS — nếu sai cấu hình, cookie không gửi được | Cao | Cao | Kiểm thử E2E sớm với đúng domain/HTTPS |
| R-2 | Local dev không giả lập HTTPS đúng → hành vi cookie khác staging/prod | Trung bình | Trung bình | Document setup local rõ ràng; dùng mkcert |
| R-3 | In-memory session mất khi restart server hoặc scale-out | Thấp (giai đoạn này) | Cao (scale-out) | Document giới hạn; plan Redis khi cần |
| R-4 | CSRF token trong FE memory mất khi reload → FE phải gọi lại `/csrf` | Trung bình | Thấp | FE phải gọi `/csrf` khi app init nếu session còn hợp lệ (đã note trong spec) |
| R-5 | Seed V3 accidentally chạy ở prod nếu profile config sai | Thấp | Cao | Guard kỹ ở Flyway location config + CI check |

---

## 11. Traceability Table

| AC | Mô tả ngắn | Screen | API Endpoint | DB | Logs | Permission | Test Type |
|----|-----------|--------|-------------|-----|------|-----------|-----------|
| AC-1 | Login endpoint tồn tại | Login screen | `POST /api/v1/auth/login` | — | — | Public | IT |
| AC-2 | Login thành công → Set-Cookie | Login screen | `POST /api/v1/auth/login` | users | — | Public | IT |
| AC-3 | Cookie HttpOnly=true | — | `POST /api/v1/auth/login` | — | — | — | IT |
| AC-4 | Cookie SameSite theo profile (dev=Lax, staging/prod=None) | — | `POST /api/v1/auth/login` | — | — | — | IT |
| AC-5 | Cookie Secure=true (staging/prod) | — | `POST /api/v1/auth/login` | — | — | — | IT |
| AC-6 | Login thất bại → 401, không tạo session | Login screen | `POST /api/v1/auth/login` | users | — | Public | IT, BB |
| AC-7 | enabled=false không login được | — | `POST /api/v1/auth/login` | users.enabled | — | — | UT, IT |
| AC-8 | Session timeout theo môi trường | — | — | — | — | Config | IT |
| AC-9 | CSRF endpoint tồn tại, trả đúng fields | — | `GET /api/v1/auth/csrf` | — | — | Authenticated | IT |
| AC-10 | CSRF chỉ trả token khi session hợp lệ | — | `GET /api/v1/auth/csrf` | — | — | Authenticated | IT |
| AC-11 | CSRF không có session → 401 | — | `GET /api/v1/auth/csrf` | — | — | — | IT, BB |
| AC-12 | Mutating API yêu cầu CSRF | — | `POST/PUT/PATCH/DELETE *` | — | — | Authenticated | IT |
| AC-13 | Thiếu CSRF header → 403 | — | Protected endpoints | — | — | — | IT, BB |
| AC-14 | Sai CSRF token → 403 | — | Protected endpoints | — | — | — | IT, BB |
| AC-15 | CSRF qua header X-CSRF-TOKEN | — | Protected endpoints | — | — | — | IT |
| AC-16 | Logout endpoint tồn tại | Dashboard | `POST /api/v1/auth/logout` | — | — | Authenticated | IT |
| AC-17 | Logout invalidates session | Dashboard | `POST /api/v1/auth/logout` | — | — | Authenticated | IT |
| AC-18 | Logout clears cookie | Dashboard | `POST /api/v1/auth/logout` | — | — | Authenticated | IT |
| AC-19 | Springdoc tích hợp, /v3/api-docs | — | `GET /v3/api-docs` | — | — | dev/staging | IT |
| AC-20 | Swagger UI accessible dev/staging | — | `GET /swagger-ui/index.html` | — | — | dev/staging | IT |
| AC-21 | Swagger UI không public ở prod | — | `GET /swagger-ui/index.html` | — | — | prod | IT |
| AC-22 | Auth endpoints có docs đầy đủ | — | `/v3/api-docs` | — | — | dev/staging | BB |
| AC-23 | Kết nối PostgreSQL theo config | — | — | PostgreSQL | — | Config | IT |
| AC-24 | User data lưu trong PostgreSQL | — | — | users, roles, user_roles | — | — | IT |
| AC-25 | Flyway tự động chạy khi startup | — | — | flyway_schema_history | — | — | IT |
| AC-26 | V2 migration tạo đúng schema | — | — | users, roles, user_roles | — | — | IT |
| AC-27 | V3 seed user dev/test | — | — | users, user_roles | — | dev/test | IT |
| AC-28 | V3 không chạy ở staging/prod | — | — | — | — | staging/prod | IT |
| AC-29 | In-memory session, không có Redis | — | — | — | — | — | IT |
| AC-30 | CORS allow localhost:5173 | — | All endpoints | — | — | — | IT, E2E |
| AC-31 | Không dùng CORS wildcard | — | All endpoints | — | — | — | IT |
| AC-32 | Error response theo RFC 7807 | — | All error paths | — | — | — | IT, BB |
| AC-33 | Login rồi gọi /csrf với JSESSIONID trả về phải thành công trên HTTP thật | Login screen | `POST /api/v1/auth/login` -> `GET /api/v1/auth/csrf` | — | — | Public -> Authenticated | IT, E2E |
| AC-34 | Dev cookie phải tương thích Chromium local qua Vite proxy | Login screen | `POST /api/v1/auth/login` | — | — | dev | E2E, Config review |
| AC-35 | Login thành công phải redirect sang "/" | Login screen | FE route transition | — | — | Public -> Authenticated | E2E, FE component test |
| AC-36 | User đã authenticated vào "/login" phải redirect về "/" | Login screen | FE route transition | — | — | Authenticated | E2E, FE component test |

**Chú thích test type**:
- **UT** = Unit Test (logic nghiệp vụ độc lập)
- **IT** = Integration Test (Spring context, database, HTTP)
- **E2E** = End-to-End (FE + BE cùng chạy)
- **BB** = Black-Box / boundary test (input/output validation)

---

## Quyết định: Sẵn sàng triển khai?

### ✅ YES — Có thể bắt đầu triển khai

**Lý do**:
- Tất cả 29 AC ban đầu + 7 AC bổ sung (AC-30 -> AC-36) đều testable và có đủ thông tin.
- Tất cả open issues quan trọng (OI-1 đến OI-5 + OI-6 đến OI-10) đã được chốt.
- Database schema, API contract, cookie policy, error format, migration plan đều được xác định rõ.

**Trước khi deploy prod** (phải giải quyết trước):
- **OI-A'**: CORS origins cho môi trường `prod`.

**Có thể bắt đầu song song với triển khai**:
- **OI-B**: Namespace cho RFC 7807 `type` URI (có thể dùng placeholder).
- **OI-C**: HTTPS local setup guide.
