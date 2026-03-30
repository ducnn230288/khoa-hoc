# User Story: Authentication bằng Session Cookie + CSRF API riêng, tích hợp Springdoc, PostgreSQL, Flyway

## 1) Thông tin chung

**Story ID**: AUTH-001
**Story title**: Xây dựng cơ chế đăng nhập bằng session cookie, bảo vệ CSRF qua API riêng, và chuẩn hóa hạ tầng backend với Springdoc, PostgreSQL, Flyway

**Epic**: Authentication & Platform Foundation

**Mục tiêu**:
Thiết lập cơ chế xác thực an toàn theo mô hình **server-side session**, phù hợp với nhu cầu **tích hợp cross-site**, đồng thời chuẩn hóa tài liệu API, database và migration để sẵn sàng phát triển tiếp.

---

## 2) User Story

**Là** người dùng của hệ thống,
**tôi muốn** đăng nhập bằng username/password và được duy trì phiên làm việc bằng session cookie, sau đó frontend gọi một API riêng để lấy CSRF token,
**để** tôi có thể sử dụng hệ thống an toàn trong mô hình web app tích hợp cross-site, đồng thời nền tảng backend dễ vận hành và dễ mở rộng.

---

## 3) Business Context / Background

Hệ thống sử dụng mô hình:

- Backend quản lý session.
- Trình duyệt giữ session id trong cookie.
- Frontend và backend có nhu cầu **tích hợp cross-site**.
- Vì có thao tác thay đổi dữ liệu, hệ thống cần chống CSRF.
- CSRF token sẽ được cấp qua **API riêng**, không nhúng trong login response.
- Hệ thống cần:
  - **Springdoc** để tài liệu hóa API
  - **PostgreSQL** làm database chính
  - **Flyway** để quản lý migration
  - **In-memory session mặc định** ở giai đoạn này, chưa dùng Redis/persistent session store

---

## 4) Scope

### In scope

1. Đăng nhập bằng username/password.
2. Duy trì phiên bằng **session cookie**.
3. Hỗ trợ **cross-site integration**.
4. Cấp CSRF token qua **API riêng**.
5. Validate CSRF cho các request thay đổi dữ liệu.
6. Logout và invalidate session.
7. Tích hợp Springdoc.
8. Dùng PostgreSQL.
9. Dùng Flyway migration.
10. Seed user mặc định cho môi trường dev/test.
11. Bổ sung wireframe frontend cho login/auth flow.

### Out of scope

1. Social login.
2. MFA/2FA.
3. Remember me.
4. JWT / refresh token.
5. Redis session store.
6. Quên mật khẩu / reset mật khẩu.
7. Phân quyền phức tạp nhiều tầng.

---

## 5) Chốt các Open Issues

## OI-1. Nhu cầu tích hợp cross-site

**Đã chốt**: Có nhu cầu tích hợp **cross-site**.

### Tác động thiết kế

- Session cookie phải cấu hình:
  - `HttpOnly=true`
  - `SameSite=None`
  - `Secure=true` với staging/prod

- Backend phải cấu hình **CORS** cho phép frontend origin hợp lệ và `allowCredentials=true`
- Frontend phải gọi API với chế độ gửi cookie, ví dụ `credentials: 'include'`

---

## OI-2. Session TTL

**Đã chốt**: dùng timeout theo môi trường như sau:

- **dev**: `8 giờ` không hoạt động
- **staging**: `2 giờ` không hoạt động
- **production**: `30 phút` không hoạt động

### Lý do chốt

- **Dev 8 giờ**: thuận tiện cho lập trình và test thủ công
- **Staging 2 giờ**: đủ dài cho UAT/demo nhưng vẫn có tính kiểm soát
- **Prod 30 phút**: cân bằng giữa UX và security cho ứng dụng business thông thường

> Ghi chú: đây là **idle timeout** của session. Không triển khai remember me trong story này.

---

## OI-3. Seed user mặc định cho dev/test

**Đã chốt**: Có seed user mặc định cho môi trường **dev/test**.

### Quy ước seed đề xuất

- Chỉ seed trên profile `dev` và `test`
- Không seed trên `staging` và `prod`

### Tài khoản mẫu

- `admin / Admin@123`
- `user01 / User@123`

### Yêu cầu bảo mật

- Password phải được hash trước khi insert
- Tài khoản seed chỉ phục vụ local/dev/test
- Tài liệu dự án phải ghi rõ không dùng các tài khoản này ngoài môi trường phi sản xuất

---

## OI-4. Swagger

**Đã chốt**: Swagger chỉ bật ở **dev/staging**.

### Tác động thiết kế

- `dev`: bật OpenAPI + Swagger UI
- `staging`: bật OpenAPI + Swagger UI
- `prod`: tắt Swagger UI và không public tài liệu API

---

## OI-5. Session store

**Đã chốt**: Chỉ dùng **in-memory / session mặc định**.

### Tác động thiết kế

- Không triển khai Redis hoặc database-backed session
- Không yêu cầu session sharing giữa nhiều instance trong phạm vi story này
- Triển khai hiện tại phù hợp cho local/dev/staging đơn giản hoặc single instance production ban đầu

---

# 6) Functional Specification

## 6.1. Login bằng session cookie

### Endpoint

`POST /api/v1/auth/login`

### Request

```json
{
  "username": "user01",
  "password": "******"
}
```

### Response thành công

- HTTP `200 OK`
- Trả session cookie
- Body ví dụ:

```json
{
  "authenticated": true,
  "username": "user01",
  "roles": ["USER"]
}
```

### Cookie policy

Do có nhu cầu cross-site, cookie phiên phải theo nguyên tắc:

- `HttpOnly=true`
- `SameSite=None`
- `Secure=true` với `staging/prod`
- `Path=/`

### Ghi chú dev local

Vì cookie cross-site yêu cầu `SameSite=None` và thường đi kèm `Secure`, môi trường local nên ưu tiên:

- chạy FE/BE cùng domain local phù hợp, hoặc
- dùng HTTPS local nếu test đúng hành vi cross-site cookie

---

## 6.2. Lấy CSRF token qua API riêng

### Endpoint

`GET /api/v1/auth/csrf`

### Hành vi

- Chỉ trả token khi session hợp lệ
- Frontend gọi sau login thành công
- Frontend lưu token ở memory state và gửi lại ở các request thay đổi dữ liệu

### Response

```json
{
  "csrfToken": "generated-token-value",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

### Trường hợp lỗi

- Không có session hoặc session hết hạn: `401 Unauthorized`

---

## 6.3. Protected APIs

Các API `POST`, `PUT`, `PATCH`, `DELETE` yêu cầu đồng thời:

1. Session hợp lệ
2. CSRF token hợp lệ

### Header chuẩn

```http
X-CSRF-TOKEN: <token>
```

### Khi lỗi

- Thiếu session: `401 Unauthorized`
- Thiếu hoặc sai CSRF token: `403 Forbidden`

---

## 6.4. Logout

### Endpoint

`POST /api/v1/auth/logout`

### Điều kiện

- Có session hợp lệ
- Có CSRF token hợp lệ

### Kết quả

- Invalidate session phía server
- Clear/expire session cookie phía client
- Trả:

```json
{
  "success": true
}
```

---

## 6.5. Springdoc

### Phạm vi

- Bật ở `dev` và `staging`
- Tắt ở `prod`

### Endpoint dự kiến

- `/v3/api-docs`
- `/swagger-ui/index.html`

### Yêu cầu

- Có mô tả login / csrf / logout
- Có mẫu request/response
- Có mô tả rõ cơ chế session cookie + CSRF header

---

## 6.6. PostgreSQL

### Yêu cầu

- Dùng PostgreSQL làm database chính
- Lưu user phục vụ authentication
- Mapping tương thích PostgreSQL

### Bảng tối thiểu

`users`

- `id`
- `username`
- `password_hash`
- `enabled`
- `created_at`
- `updated_at`

`roles`

- `id`
- `code`
- `name`

`user_roles`

- `user_id`
- `role_id`

---

## 6.7. Flyway

### Migration tối thiểu

- `V1__init_schema.sql`
- `V2__create_auth_tables.sql`
- `V3__seed_dev_test_users.sql`

### Quy tắc seed

- `V3` chỉ áp dụng cho `dev/test`
- Không seed dữ liệu mặc định cho `staging/prod`

---

# 7) Business Rules

1. Chỉ user có `enabled = true` mới đăng nhập được.
2. Password phải được lưu dưới dạng hash.
3. Login thành công mới tạo session.
4. Session cookie phải là `HttpOnly`.
5. Vì có tích hợp cross-site, session cookie dùng `SameSite=None`.
6. Ở `staging/prod`, session cookie phải có `Secure=true`.
7. CSRF token chỉ được cấp khi session hợp lệ.
8. Các request thay đổi dữ liệu phải có CSRF token hợp lệ.
9. Logout phải invalidate session hiện tại.
10. Swagger chỉ được bật ở `dev/staging`.
11. Session store giai đoạn này là in-memory mặc định.
12. Seed user mặc định chỉ tồn tại ở `dev/test`.

---

# 8) Luồng nghiệp vụ

## 8.1. Login thành công

1. User nhập username/password.
2. Frontend gọi `POST /api/v1/auth/login` với `credentials: include`.
3. Backend xác thực thành công.
4. Backend tạo session.
5. Backend trả session cookie.
6. Frontend gọi `GET /api/v1/auth/csrf`.
7. Backend trả CSRF token.
8. Frontend lưu token vào state và dùng cho các request thay đổi dữ liệu.

## 8.2. Login thất bại

1. User nhập sai thông tin.
2. Backend trả `401`.
3. Không tạo session hợp lệ.
4. Frontend hiển thị lỗi đăng nhập.

## 8.3. Gọi protected API thành công

1. Frontend gửi cookie phiên.
2. Frontend gửi header `X-CSRF-TOKEN`.
3. Backend validate thành công.
4. Request được xử lý.

## 8.4. Session hết hạn

1. Frontend gửi request với cookie cũ.
2. Backend xác định session không còn hiệu lực.
3. Trả `401 Unauthorized`.
4. Frontend chuyển về màn hình login.

## 8.5. Thiếu CSRF

1. Frontend gửi request mutating nhưng thiếu token.
2. Backend trả `403 Forbidden`.
3. Frontend hiển thị lỗi phù hợp hoặc thực hiện lấy lại trạng thái auth nếu cần.

---

# 9) Acceptance Criteria

## Authentication / Session

**AC-1**: Hệ thống phải cung cấp endpoint `POST /api/v1/auth/login` để xác thực username/password.

**AC-2**: Khi đăng nhập thành công, hệ thống phải tạo server-side session và trả session id qua cookie.

**AC-3**: Session cookie phải được cấu hình `HttpOnly=true`.

**AC-4**: Do có nhu cầu tích hợp cross-site, session cookie phải được cấu hình `SameSite=None`.

**AC-5**: Ở môi trường staging và production, session cookie phải được cấu hình `Secure=true`.

**AC-6**: Khi đăng nhập thất bại, hệ thống phải trả `401 Unauthorized` và không tạo session hợp lệ.

**AC-7**: Chỉ user có `enabled=true` mới đăng nhập thành công.

**AC-8**: Session timeout phải được cấu hình theo môi trường:

- dev: 8 giờ không hoạt động
- staging: 2 giờ không hoạt động
- prod: 30 phút không hoạt động

## CSRF

**AC-9**: Hệ thống phải cung cấp endpoint `GET /api/v1/auth/csrf` để trả CSRF token qua response body.

**AC-10**: Endpoint lấy CSRF token chỉ trả token khi request có session hợp lệ.

**AC-11**: Khi không có session hợp lệ, endpoint lấy CSRF token phải trả `401 Unauthorized`.

**AC-12**: Các request `POST`, `PUT`, `PATCH`, `DELETE` đến protected endpoints phải yêu cầu CSRF token hợp lệ.

**AC-13**: Khi request protected thiếu CSRF token, hệ thống phải trả `403 Forbidden`.

**AC-14**: Khi request protected có CSRF token sai hoặc hết hiệu lực, hệ thống phải trả `403 Forbidden`.

**AC-15**: CSRF token phải có thể được gửi qua header `X-CSRF-TOKEN`.

## Logout

**AC-16**: Hệ thống phải cung cấp endpoint `POST /api/v1/auth/logout`.

**AC-17**: Khi logout thành công, session hiện tại phải bị invalidate phía server.

**AC-18**: Khi logout thành công, cookie phiên phải bị clear hoặc expire trong response.

## Springdoc

**AC-19**: Hệ thống phải tích hợp Springdoc để sinh OpenAPI document.

**AC-20**: Swagger/OpenAPI chỉ được bật ở môi trường `dev` và `staging`.

**AC-21**: Ở môi trường production, Swagger UI không được public.

**AC-22**: Các endpoint auth phải được mô tả trong tài liệu API với request/response mẫu và mã trạng thái chính.

## PostgreSQL / Flyway

**AC-23**: Ứng dụng phải kết nối được với PostgreSQL theo cấu hình môi trường.

**AC-24**: Dữ liệu người dùng phục vụ authentication phải được lưu trong PostgreSQL.

**AC-25**: Hệ thống phải tích hợp Flyway và tự động chạy migration khi khởi động ứng dụng theo cấu hình môi trường.

**AC-26**: Phải có migration tạo schema/bảng tối thiểu phục vụ authentication.

**AC-27**: Phải có migration seed user mặc định cho môi trường dev/test.

**AC-28**: Không được seed user mặc định ở staging/prod.

## Session store

**AC-29**: Hệ thống phải sử dụng in-memory/session mặc định, không triển khai persistent session store trong phạm vi story này.

---

# 10) Non-functional Requirements

1. Password dùng encoder mạnh như BCrypt.
2. Không log password, session id, CSRF token ở dạng nhạy cảm.
3. CORS phải whitelist đúng frontend origins.
4. Backend phải hỗ trợ gửi cookie cross-site với `allowCredentials=true`.
5. Tài liệu API phải phản ánh đúng auth flow thực tế.
6. Cấu hình theo profile phải tách rõ dev/staging/prod.
7. Seed data chỉ được áp dụng có kiểm soát ngoài production.

---

# 11) Wireframe ASCII cho Frontend

## 11.1. Màn hình Login

```text
+----------------------------------------------------------------------------------+
| LOGO                                                                             |
|----------------------------------------------------------------------------------|
|                                ĐĂNG NHẬP HỆ THỐNG                                |
|                                                                                  |
|  Username / Email                                                                |
|  +--------------------------------------------------------------------------+    |
|  | user01                                                                   |    |
|  +--------------------------------------------------------------------------+    |
|                                                                                  |
|  Password                                                                        |
|  +--------------------------------------------------------------------------+    |
|  | ********                                                                  |    |
|  +--------------------------------------------------------------------------+    |
|                                                                                  |
|  [ ] Ghi nhớ tài khoản trên trình duyệt này (disabled / out of scope)           |
|                                                                                  |
|  +------------------------------+                                                |
|  |         ĐĂNG NHẬP            |                                                |
|  +------------------------------+                                                |
|                                                                                  |
|  Error message area:                                                             |
|  [ Sai tên đăng nhập hoặc mật khẩu ]                                             |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

> Ghi chú: checkbox “Ghi nhớ tài khoản” chỉ hiển thị disabled hoặc ẩn, vì **remember me out of scope**.

---

## 11.2. Trạng thái đang đăng nhập và lấy CSRF token

```text
+----------------------------------------------------------------------------------+
| LOGO                                                                             |
|----------------------------------------------------------------------------------|
|                                ĐĂNG NHẬP HỆ THỐNG                                |
|                                                                                  |
|  Username / Email: user01                                                        |
|  Password: ********                                                              |
|                                                                                  |
|  +------------------------------+                                                |
|  |     ĐANG XỬ LÝ...            |                                                |
|  +------------------------------+                                                |
|                                                                                  |
|  Status:                                                                         |
|   [1] Đang xác thực tài khoản...                                                 |
|   [2] Đã tạo session...                                                          |
|   [3] Đang lấy CSRF token...                                                     |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

---

## 11.3. Trạng thái login thành công

```text
+----------------------------------------------------------------------------------+
| Header: My App                                        user01  [Logout]           |
|----------------------------------------------------------------------------------|
| Dashboard                                                                        |
|                                                                                  |
|  Welcome, user01                                                                 |
|                                                                                  |
|  Auth status:                                                                    |
|   - Session: Active                                                              |
|   - CSRF token: Loaded in memory                                                 |
|                                                                                  |
|  Available actions:                                                              |
|   [Create] [Update] [Delete]                                                     |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

---

## 11.4. Session hết hạn

```text
+----------------------------------------------------------------------------------+
| Header: My App                                                                   |
|----------------------------------------------------------------------------------|
|                              PHIÊN ĐĂNG NHẬP ĐÃ HẾT HẠN                          |
|                                                                                  |
|  Vì lý do bảo mật, phiên làm việc của bạn đã hết hạn.                            |
|  Vui lòng đăng nhập lại để tiếp tục.                                             |
|                                                                                  |
|  +------------------------------+                                                |
|  |       ĐĂNG NHẬP LẠI          |                                                |
|  +------------------------------+                                                |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

---

## 11.5. Lỗi thiếu / sai CSRF khi gọi action

```text
+----------------------------------------------------------------------------------+
| Header: My App                                        user01  [Logout]           |
|----------------------------------------------------------------------------------|
| Action Result                                                                    |
|                                                                                  |
|  [ Không thể hoàn thành thao tác. ]                                              |
|  [ Phiên bảo mật không hợp lệ hoặc đã thay đổi. ]                                |
|                                                                                  |
|  Suggested action:                                                               |
|   - Tải lại trang                                                                |
|   - Hoặc đăng nhập lại                                                           |
|                                                                                  |
|  +------------------------------+   +------------------------------+             |
|  |         TẢI LẠI              |   |        ĐĂNG NHẬP LẠI         |             |
|  +------------------------------+   +------------------------------+             |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

---

# 12) Frontend Integration Notes

## Trình tự gọi API

1. User submit form login
2. FE gọi `POST /api/v1/auth/login` với cookie credentials
3. Nếu thành công, FE gọi `GET /api/v1/auth/csrf`
4. FE lưu `csrfToken` vào memory state
5. FE gắn `X-CSRF-TOKEN` vào các request mutating
6. Khi gặp `401`, điều hướng về login
7. Khi gặp `403` do CSRF, yêu cầu reload hoặc login lại

## Pseudo-flow

```text
Login Submit
   -> POST /auth/login (with credentials)
      -> 200 OK + Set-Cookie
         -> GET /auth/csrf (with credentials)
            -> 200 OK + csrfToken
               -> Save token in memory
               -> Navigate to dashboard
```

---

# 13) Rủi ro / Lưu ý triển khai

1. Cross-site cookie phụ thuộc mạnh vào cấu hình CORS, domain và HTTPS.
2. Nếu local dev không giả lập đúng HTTPS/cross-site, hành vi cookie có thể khác staging/prod.
3. In-memory session không phù hợp cho scale-out nhiều instance về lâu dài.
4. Nếu frontend refresh toàn bộ app, CSRF token trong memory sẽ mất; FE cần gọi lại `/auth/csrf` khi khởi động app nếu session vẫn còn hợp lệ.

---

# 14) Definition of Done

1. Login tạo được session cookie đúng policy.
2. Cross-site flow hoạt động với frontend origin được cấu hình.
3. FE gọi riêng được API `/auth/csrf`.
4. Mutating API yêu cầu CSRF hợp lệ.
5. Logout invalidate session thành công.
6. PostgreSQL hoạt động ổn định cho auth module.
7. Flyway migration chạy được trên DB mới.
8. Có seed user cho dev/test.
9. Swagger chỉ truy cập được ở dev/staging.
10. Có wireframe frontend và dev note đủ để FE triển khai.
