# Gói đặc tả — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng)

> Cập nhật: 2026-04-02 · Giai đoạn: 1  
> **Nguồn tham chiếu duy nhất cho thay đổi này.**  
> Không triển khai bất kỳ nội dung nào không được viết ở đây.

---

## 1. Bối cảnh / Mục đích

Hệ thống cần một nền tảng xác thực backend theo mô hình **server-side session** thay cho token-based auth trong phạm vi ticket này. Mục tiêu là:

- Cho phép người dùng đăng nhập bằng `username/password`
- Duy trì đăng nhập bằng **session cookie**
- Hỗ trợ frontend/backend chạy trong mô hình **cross-site integration**
- Bảo vệ các request thay đổi dữ liệu bằng **CSRF token được cấp qua API riêng**
- Chuẩn hóa nền tảng backend với **Springdoc**, **PostgreSQL** và **Flyway** để sẵn sàng cho các thay đổi tiếp theo

Ticket này tập trung vào **đặc tả hành vi và contract**. Chỉ những quyết định cần thiết để bắt đầu implementation mới được chốt ở đây; không mở rộng sang đề xuất implementation chi tiết ngoài phạm vi cần thiết để kiểm thử và triển khai.

## 2. Phạm vi

### Trong phạm vi

- Đăng nhập bằng `username/password`
- Tạo và duy trì server-side session qua cookie
- Hỗ trợ frontend origin trong phạm vi ticket: `http://localhost:5173`
- Lấy CSRF token qua API riêng sau khi login thành công hoặc sau khi app reload nhưng session còn hiệu lực
- Bắt buộc session + CSRF cho các request thay đổi dữ liệu thuộc protected endpoints
- Logout và hủy session hiện tại
- Dùng PostgreSQL làm database chính cho auth data
- Dùng Flyway để quản lý migration schema/data seed
- Có seed user mặc định cho `dev/test`
- Có tài liệu API bằng Springdoc ở môi trường được phép
- Có wireframe mức hành vi cho login/auth flow
- Chuẩn hóa error response tối thiểu cho các lỗi auth/CSRF

### Ngoài phạm vi

- Social login
- MFA/2FA
- Remember me
- JWT / refresh token
- Redis hoặc persistent session store
- Quên mật khẩu / reset mật khẩu
- RBAC/phân quyền nhiều tầng ngoài việc trả danh sách role cơ bản
- Auth status endpoint riêng như `/auth/me`, `/auth/session`, `/auth/status`
- Hỗ trợ login bằng email
- Danh sách protected business APIs ngoài module auth
- Bổ sung origin khác ngoài `http://localhost:5173` trong acceptance của ticket này

## 3. Thuật ngữ

| #   | Thuật ngữ              | Định nghĩa                                                                                                |
| --- | ---------------------- | --------------------------------------------------------------------------------------------------------- |
| 1   | Session                | Phiên đăng nhập phía server được tạo sau khi xác thực thành công.                                         |
| 2   | Session cookie         | Cookie chứa session identifier do backend cấp để browser gửi lại ở các request sau.                       |
| 3   | CSRF token             | Token chống Cross-Site Request Forgery được cấp qua API riêng và được gửi lại trong request mutating.     |
| 4   | Protected endpoint     | Endpoint yêu cầu người dùng đã xác thực; với request thay đổi dữ liệu còn phải có CSRF token hợp lệ.      |
| 5   | Mutating request       | Request HTTP dùng `POST`, `PUT`, `PATCH`, hoặc `DELETE`.                                                  |
| 6   | Cross-site integration | Mô hình frontend và backend khác site/origin, cần CORS + cookie policy phù hợp để trình duyệt gửi cookie. |
| 7   | Idle timeout           | Khoảng thời gian không hoạt động sau đó session hết hiệu lực.                                             |
| 8   | Seed user              | Dữ liệu user mặc định chỉ được tạo cho môi trường phi production để dev/test.                             |
| 9   | Error envelope         | JSON body chuẩn tối thiểu trả về khi có lỗi auth/CSRF.                                                    |

## 4. Hiện trạng / Trạng thái mục tiêu

| #   | Khía cạnh           | Hiện trạng                                                    | Trạng thái mục tiêu                                                                     |
| --- | ------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| 1   | Xác thực            | Chưa có đặc tả chuẩn hóa cho auth foundation trong `ticket-1` | Có login/logout/session flow được đặc tả rõ ràng                                        |
| 2   | Quản lý phiên       | Chưa chốt contract session cookie cho ticket                  | Dùng server-side session cookie với policy phù hợp cross-site                           |
| 3   | CSRF                | Chưa có contract chính thức cho cách FE lấy/gửi token         | Có API riêng để lấy CSRF token và rule validate cho mutating requests                   |
| 4   | DB                  | Chưa có spec schema auth tối thiểu cho ticket                 | Có schema tối thiểu `users`, `roles`, `user_roles` trên PostgreSQL                      |
| 5   | Migration           | Chưa có chuẩn migration cho auth foundation                   | Có Flyway migration khởi tạo schema và seed data cho dev/test theo cơ chế tách location |
| 6   | API docs            | Chưa chốt phạm vi publish tài liệu API theo môi trường        | Springdoc bật ở `dev/staging`, không public ở `prod`                                    |
| 7   | Error contract      | Chưa có shape lỗi tối thiểu cho `401/403`                     | Có error envelope chuẩn cho auth/CSRF                                                   |
| 8   | FE contract         | Có mơ hồ `Username / Email` vs `username`                     | Chốt chỉ dùng `username`, không hỗ trợ email login                                      |
| 9   | Session rehydration | Chưa rõ có cần auth-status endpoint riêng                     | FE dùng `GET /api/v1/auth/csrf` làm cơ chế rehydrate session state                      |

## 5. Chi tiết đặc tả

### 5.1. Quy tắc tổng quát

1. Ticket này chỉ hỗ trợ đăng nhập bằng **username/password**.
2. Login request chỉ có hai trường hợp lệ là `username` và `password`.
3. Chỉ user có `enabled = true` mới được xác thực thành công.
4. Session chỉ được tạo sau khi thông tin đăng nhập hợp lệ.
5. CSRF token chỉ được cấp khi request có session hợp lệ.
6. Các mutating request tới protected endpoints phải đồng thời thỏa mãn:
   - session hợp lệ
   - CSRF token hợp lệ
7. Logout chỉ áp dụng cho **session hiện tại** và phải làm cho session đó hết hiệu lực ngay.
8. Trong ticket này, FE không dùng endpoint auth-status riêng; cơ chế rehydrate phiên là gọi `GET /api/v1/auth/csrf`.

### 5.2. Contract API — Login

**Endpoint authoritative**: `POST /api/v1/auth/login`

**Request body**

```json
{
  "username": "user01",
  "password": "******"
}
```

**Yêu cầu xử lý**

- Backend xác thực `username/password`
- Nếu thành công, backend tạo session phía server
- Response phải bao gồm session cookie do backend cấp
- Response body phải chứa trạng thái đã xác thực, `username`, và `roles`
- `roles` là trường **bắt buộc**; nếu user không có role thì trả `[]`

**Response thành công tối thiểu**

```json
{
  "authenticated": true,
  "username": "user01",
  "roles": ["USER"]
}
```

**Response lỗi**

- Sai thông tin đăng nhập: `401 Unauthorized`
- User bị disable: `401 Unauthorized`

### 5.3. Chính sách session cookie và CORS

Session cookie phải tuân thủ các quy tắc sau:

- `HttpOnly=true`
- `SameSite=None`
- `Path=/`
- `Secure=true` ở `staging/prod`

Quy tắc CORS trong phạm vi ticket:

- Backend phải cho phép credentialed requests từ origin `http://localhost:5173`
- Backend phải bật cơ chế tương đương `allowCredentials=true`
- Không được dùng wildcard origin `*` cùng với credentialed requests
- Frontend phải gọi API với chế độ tương đương `credentials: include`

Ghi chú:

- Với `dev`, thuộc tính `Secure` phụ thuộc topology local; ticket này không ép buộc local HTTPS nhưng phải giữ nguyên contract mục tiêu ở `staging/prod`.
- Các origin khác ngoài `http://localhost:5173` không thuộc acceptance của ticket này.

### 5.4. Session timeout theo môi trường

Idle timeout của session phải theo môi trường như sau:

- `dev`: 8 giờ
- `staging`: 2 giờ
- `prod`: 30 phút

Sau khi idle timeout xảy ra, session phải bị coi là không còn hợp lệ và các request tiếp theo với session cũ phải nhận `401 Unauthorized`.

### 5.5. Contract API — Lấy CSRF token

**Endpoint authoritative**: `GET /api/v1/auth/csrf`

**Điều kiện**

- Chỉ trả token khi session hợp lệ
- Frontend gọi endpoint này sau login thành công, hoặc sau khi reload app nếu session vẫn còn hiệu lực nhưng token ở memory đã mất
- Endpoint này là cơ chế **rehydrate auth state** trong ticket hiện tại

**Response thành công**

```json
{
  "csrfToken": "generated-token-value",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

**Response lỗi**

- Không có session hợp lệ: `401 Unauthorized`
- Session đã hết hạn: `401 Unauthorized`

### 5.6. Quy tắc với protected endpoints

1. Các request `POST`, `PUT`, `PATCH`, `DELETE` tới protected endpoints phải có session hợp lệ.
2. Các request đó phải gửi CSRF token hợp lệ qua header `X-CSRF-TOKEN`.
3. Nếu thiếu session, backend phải trả `401 Unauthorized`.
4. Nếu session đã hết hiệu lực, backend phải trả `401 Unauthorized`.
5. Nếu có session nhưng thiếu CSRF token hoặc token không hợp lệ, backend phải trả `403 Forbidden`.
6. `POST /api/v1/auth/login` không thuộc nhóm mutating protected endpoint.
7. `POST /api/v1/auth/logout` là protected endpoint và phải tuân thủ rule session + CSRF.

### 5.7. Contract API — Logout

**Endpoint authoritative**: `POST /api/v1/auth/logout`

**Điều kiện thành công**

- Có session hợp lệ
- Có CSRF token hợp lệ

**Kết quả bắt buộc**

- Session hiện tại bị invalidate ở server
- Session cookie ở client bị clear hoặc expire trong response
- Response body tối thiểu:

```json
{
  "success": true
}
```

**Trường hợp lỗi**

- Không có session hợp lệ: `401 Unauthorized`
- Session đã hết hạn: `401 Unauthorized`
- Thiếu hoặc sai CSRF token: `403 Forbidden`

### 5.8. Chuẩn error response

Tất cả lỗi `401` và `403` do module auth/CSRF trong ticket này phải trả JSON body theo envelope tối thiểu sau:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid username or password.",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-04-02T10:00:00Z"
}
```

Quy tắc bắt buộc:

- `code`: mã lỗi máy đọc được
- `message`: thông điệp ngắn gọn an toàn để FE hiển thị hoặc log
- `path`: request path thực tế
- `timestamp`: thời điểm backend sinh lỗi ở định dạng ISO-8601

Bộ mã lỗi trong phạm vi ticket:

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_USER_DISABLED`
- `AUTH_SESSION_REQUIRED`
- `AUTH_SESSION_EXPIRED`
- `AUTH_CSRF_INVALID`

Quy tắc mapping:

- Login sai `username/password` -> `401` + `AUTH_INVALID_CREDENTIALS`
- Login vào user bị disable -> `401` + `AUTH_USER_DISABLED`
- Không có session ở endpoint yêu cầu auth -> `401` + `AUTH_SESSION_REQUIRED`
- Session hết hạn -> `401` + `AUTH_SESSION_EXPIRED`
- Thiếu hoặc sai CSRF token -> `403` + `AUTH_CSRF_INVALID`

### 5.9. Springdoc / OpenAPI

1. Hệ thống phải sinh được OpenAPI document cho các endpoint auth trong môi trường cho phép.
2. `dev`: bật OpenAPI + Swagger UI.
3. `staging`: bật OpenAPI + Swagger UI.
4. `prod`: không public Swagger UI; không public tài liệu API auth ra bên ngoài.
5. Tài liệu API phải mô tả được:
   - request/response mẫu
   - mã trạng thái chính
   - cơ chế session cookie + CSRF header
   - error envelope và error codes

### 5.10. PostgreSQL và schema tối thiểu

Auth data phải được lưu trong PostgreSQL.

Schema tối thiểu cần có:

#### `users`

- `id`
- `username`
- `password_hash`
- `enabled`
- `created_at`
- `updated_at`

#### `roles`

- `id`
- `code`
- `name`

#### `user_roles`

- `user_id`
- `role_id`

Ràng buộc chi tiết như unique/index/length chưa được chốt từ raw source và không được tự suy diễn thêm ở giai đoạn này, trừ các ràng buộc tối thiểu cần thiết để migration chạy thành công.

### 5.11. Flyway và seed data

Hệ thống phải có tối thiểu các nhóm migration logic sau:

- migration khởi tạo schema chung
- migration tạo auth tables
- migration seed user cho non-prod

Cơ chế chốt cho seed:

1. **Schema migrations** phải nằm ở Flyway location dùng chung cho mọi môi trường.
2. **Seed migrations cho dev/test** phải nằm ở Flyway location riêng cho non-prod.
3. Chỉ profile `dev` và `test` mới được nạp thêm non-prod Flyway location.
4. `staging` và `prod` chỉ được nạp common Flyway location, nên không nhìn thấy và không chạy seed migration.
5. Nếu cấu hình môi trường `staging` hoặc `prod` lỡ nạp non-prod Flyway location thì startup phải được xem là cấu hình sai và không đạt acceptance.
6. Password của seed user phải được hash trước khi insert.

Tài khoản seed tối thiểu cho `dev/test`:

- `admin / Admin@123`
- `user01 / User@123`

### 5.12. Session store

Trong phạm vi ticket này chỉ dùng **in-memory / session mặc định**. Không triển khai Redis, database-backed session hoặc session sharing đa instance.

## 6. Yêu cầu phi chức năng

| #   | Danh mục               | Yêu cầu                                                                                                                                                                                                                                 |
| --- | ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Hiệu năng              | Login, lấy CSRF token và logout phải hoàn thành trong ngưỡng chấp nhận được của ứng dụng web nội bộ; giai đoạn này không tự đặt SLA mới ngoài những gì cần cho kiểm thử.                                                                |
| 2   | Bảo mật                | Password phải được lưu dưới dạng hash mạnh; không log plaintext password, session id hoặc CSRF token; chỉ user `enabled=true` được login thành công.                                                                                    |
| 3   | Tính sẵn sàng          | Hệ thống phải xử lý đúng khi session hết hạn; khi PostgreSQL hoặc migration lỗi thì ứng dụng không được báo trạng thái “auth ready” giả.                                                                                                |
| 4   | Khả năng quan sát      | Phải có log audit mức tối thiểu cho login success/failure, logout success, truy cập bị từ chối do unauthenticated, truy cập bị từ chối do session expired, và truy cập bị từ chối do invalid CSRF; log không được chứa secret nhạy cảm. |
| 5   | Tương thích môi trường | Cấu hình phải tách biệt ít nhất cho `dev`, `staging`, `prod`, đặc biệt với TTL session, Secure cookie, seed data, Flyway locations và publish Swagger.                                                                                  |
| 6   | Tương thích cross-site | Backend phải hỗ trợ CORS credentialed request từ `http://localhost:5173` và không được dùng wildcard origin với credentials.                                                                                                            |
| 7   | Tính nhất quán API     | Mọi lỗi `401/403` trong module auth thuộc ticket này phải dùng cùng error envelope tối thiểu `{code,message,path,timestamp}`.                                                                                                           |

## 7. Tiêu chí chấp nhận

| #   | ID            | Mô tả                                                                                                                                                                                                                                         | Loại kiểm thử |
| --- | ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| 1   | AC-auth-1/v1  | Khi client gọi `POST /api/v1/auth/login` với `username/password` hợp lệ của user có `enabled=true`, hệ thống phải trả `200 OK`, tạo server-side session và trả session cookie trong response.                                                 | IT · E2E      |
| 2   | AC-auth-2/v1  | Session cookie được trả từ login thành công phải có `HttpOnly=true`, `SameSite=None` và `Path=/`.                                                                                                                                             | IT · E2E      |
| 3   | AC-auth-3/v1  | Ở môi trường `staging` và `prod`, session cookie được trả từ login thành công phải có `Secure=true`.                                                                                                                                          | IT · E2E      |
| 4   | AC-auth-4/v1  | Khi client gọi `POST /api/v1/auth/login` với sai `username/password`, hệ thống phải trả `401 Unauthorized`, không tạo session hợp lệ và body lỗi phải có `code=AUTH_INVALID_CREDENTIALS`.                                                     | IT · E2E      |
| 5   | AC-auth-5/v1  | Khi client gọi `POST /api/v1/auth/login` cho user có `enabled=false`, hệ thống phải trả `401 Unauthorized`, không tạo session hợp lệ và body lỗi phải có `code=AUTH_USER_DISABLED`.                                                           | IT · E2E      |
| 6   | AC-auth-6/v1  | Idle timeout của session phải được cấu hình đúng theo môi trường: `dev=8h`, `staging=2h`, `prod=30m`; sau thời gian này request dùng session cũ phải nhận `401 Unauthorized`.                                                                 | IT · E2E      |
| 7   | AC-auth-7/v1  | Khi client có session hợp lệ và gọi `GET /api/v1/auth/csrf`, hệ thống phải trả `200 OK` cùng body chứa `csrfToken`, `headerName`, `parameterName`.                                                                                            | IT · E2E      |
| 8   | AC-auth-8/v1  | Khi client không có session hợp lệ và gọi `GET /api/v1/auth/csrf`, hệ thống phải trả `401 Unauthorized` với body lỗi có `code=AUTH_SESSION_REQUIRED`.                                                                                         | IT            |
| 9   | AC-auth-9/v1  | Với mọi mutating request tới protected endpoint, nếu request không có session hợp lệ thì hệ thống phải trả `401 Unauthorized` với body lỗi có `code=AUTH_SESSION_REQUIRED`.                                                                   | IT · E2E      |
| 10  | AC-auth-10/v1 | Với mọi mutating request tới protected endpoint, nếu request có session hợp lệ nhưng thiếu header `X-CSRF-TOKEN` thì hệ thống phải trả `403 Forbidden` với body lỗi có `code=AUTH_CSRF_INVALID`.                                              | IT · E2E      |
| 11  | AC-auth-11/v1 | Với mọi mutating request tới protected endpoint, nếu request có session hợp lệ nhưng `X-CSRF-TOKEN` sai hoặc hết hiệu lực thì hệ thống phải trả `403 Forbidden` với body lỗi có `code=AUTH_CSRF_INVALID`.                                     | IT · E2E      |
| 12  | AC-auth-12/v1 | Với mutating request tới protected endpoint, nếu request có session hợp lệ và `X-CSRF-TOKEN` hợp lệ thì request phải được phép đi qua lớp auth/CSRF guard.                                                                                    | IT · E2E      |
| 13  | AC-auth-13/v1 | Khi client gọi `POST /api/v1/auth/logout` với session hợp lệ và CSRF token hợp lệ, hệ thống phải trả `200 OK`, invalidate session hiện tại và clear/expire session cookie trong response.                                                     | IT · E2E      |
| 14  | AC-auth-14/v1 | Khi client gọi `POST /api/v1/auth/logout` mà không có session hợp lệ, hệ thống phải trả `401 Unauthorized` với body lỗi có `code=AUTH_SESSION_REQUIRED`.                                                                                      | IT            |
| 15  | AC-auth-15/v1 | Khi client gọi `POST /api/v1/auth/logout` có session nhưng thiếu hoặc sai CSRF token, hệ thống phải trả `403 Forbidden` với body lỗi có `code=AUTH_CSRF_INVALID`.                                                                             | IT            |
| 16  | AC-auth-16/v1 | Ở `dev` và `staging`, hệ thống phải public được OpenAPI/Swagger cho module auth; ở `prod`, Swagger UI và tài liệu API auth không được public.                                                                                                 | IT · BB       |
| 17  | AC-auth-17/v1 | Ứng dụng phải kết nối được PostgreSQL và lưu dữ liệu auth trong các bảng tối thiểu `users`, `roles`, `user_roles`.                                                                                                                            | IT            |
| 18  | AC-auth-18/v1 | Khi khởi động trên database trống, Flyway phải chạy được migration khởi tạo schema auth mà không cần thao tác tay ngoài quy trình chuẩn của môi trường.                                                                                       | IT            |
| 19  | AC-auth-19/v1 | Ở môi trường `dev/test`, dữ liệu seed tối thiểu phải tạo được các tài khoản `admin` và `user01` với password đã được hash.                                                                                                                    | IT            |
| 20  | AC-auth-20/v1 | Ở môi trường `staging/prod`, hệ thống không được tạo seed user mặc định và không được nạp non-prod Flyway location.                                                                                                                           | IT · BB       |
| 21  | AC-auth-21/v1 | Session store của ticket này phải là in-memory/session mặc định; không được phụ thuộc Redis hoặc persistent session store để hoàn thành auth flow.                                                                                            | IT · BB       |
| 22  | AC-auth-22/v1 | Hệ thống phải ghi log audit tối thiểu cho login success/failure, logout success, `401 Unauthorized`, `401 session expired`, `403 Forbidden` do CSRF, và các log này không được chứa plaintext password, full session id hoặc full CSRF token. | UT · IT       |
| 23  | AC-auth-23/v1 | Mọi lỗi `401/403` từ module auth trong ticket này phải trả JSON với đủ bốn trường `code`, `message`, `path`, `timestamp`.                                                                                                                     | UT · IT       |
| 24  | AC-auth-24/v1 | Login response thành công phải luôn chứa trường `roles` kiểu mảng; nếu user không có role thì giá trị phải là `[]`, không được null và không được bỏ field.                                                                                   | UT · IT       |
| 25  | AC-auth-25/v1 | UI và API contract của ticket này phải dùng nhãn/field `Username`; không được xuất hiện field riêng cho email login trong màn hình login, request body và OpenAPI của module auth.                                                            | BB · E2E      |
| 26  | AC-auth-26/v1 | Sau khi app reload mà session cookie vẫn còn hiệu lực, client gọi `GET /api/v1/auth/csrf` phải nhận `200 OK` và có thể tiếp tục protected flow mà không cần endpoint auth-status riêng.                                                       | IT · E2E      |
| 27  | AC-auth-27/v1 | Backend phải chấp nhận credentialed CORS request từ origin `http://localhost:5173` và không được cấu hình wildcard origin `*` cho flow này.                                                                                                   | IT · E2E      |

## 8. Ví dụ

### Các luồng bình thường

1. **Login thành công rồi lấy CSRF token**
   - Input: `POST /api/v1/auth/login` với `username=user01`, password đúng
   - Kết quả: `200 OK`, trả session cookie và body có `roles`
   - Bước tiếp: `GET /api/v1/auth/csrf` với cookie vừa nhận
   - Kết quả: `200 OK`, body chứa `csrfToken`, `headerName`, `parameterName`

2. **Gọi protected mutating API thành công**
   - Tiền điều kiện: client đã có session hợp lệ và đã lấy CSRF token
   - Input: `POST` tới một protected endpoint với cookie + header `X-CSRF-TOKEN`
   - Kết quả: request đi qua lớp auth/CSRF guard và được xử lý tiếp

### Các luồng lỗi

1. **Sai mật khẩu khi login**
   - Input: `POST /api/v1/auth/login` với `username=user01`, password sai
   - Kết quả: `401 Unauthorized`, body có `code=AUTH_INVALID_CREDENTIALS`, không có session hợp lệ được tạo

2. **Thiếu CSRF token khi gọi protected API**
   - Tiền điều kiện: client có session hợp lệ
   - Input: `DELETE` tới protected endpoint nhưng không gửi `X-CSRF-TOKEN`
   - Kết quả: `403 Forbidden`, body có `code=AUTH_CSRF_INVALID`

### Các trường hợp biên

1. **Session vừa hết hạn do idle timeout**
   - Tiền điều kiện: session được tạo hợp lệ nhưng đã vượt quá TTL theo môi trường
   - Input: `GET /api/v1/auth/csrf` hoặc `POST /api/v1/auth/logout` với cookie cũ
   - Kết quả: `401 Unauthorized`, body có `code=AUTH_SESSION_EXPIRED`

2. **App reload sau login, token CSRF ở memory bị mất nhưng session còn hiệu lực**
   - Tiền điều kiện: browser vẫn còn session cookie hợp lệ
   - Input: FE khởi động lại và gọi `GET /api/v1/auth/csrf`
   - Kết quả: `200 OK`, cấp lại token CSRF mới để FE dùng tiếp

3. **User không có role nào được gán**
   - Input: `POST /api/v1/auth/login` với user hợp lệ nhưng không có bản ghi `user_roles`
   - Kết quả: `200 OK`, response có `roles: []`

4. **Request từ origin không nằm trong phạm vi ticket**
   - Input: Browser app chạy từ origin khác `http://localhost:5173` gửi credentialed request
   - Kết quả: request không được coi là behavior bắt buộc phải hỗ trợ trong acceptance của `ticket-1`

## 9. Open Issues

**Không còn Open Issues blocking để bắt đầu implementation.**

Các quyết định đã được chốt trong Spec Pack này:

- Mã change canonical: `ticket-1`
- Allowed frontend origin trong phạm vi ticket: `http://localhost:5173`
- Seed chỉ chạy ở `dev/test` bằng cơ chế tách Flyway location giữa common và non-prod
- Error response dùng envelope `{code,message,path,timestamp}`
- `roles` là field bắt buộc trong login success response
- Chỉ hỗ trợ `username`, không hỗ trợ email login
- Không thêm auth-status endpoint riêng; FE rehydrate qua `GET /api/v1/auth/csrf`

## 10. Rủi ro

| #   | Rủi ro                                                                                | Khả năng xảy ra           | Mức độ ảnh hưởng | Biện pháp giảm thiểu                                                                                          |
| --- | ------------------------------------------------------------------------------------- | ------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------- |
| 1   | Cookie cross-site không hoạt động đúng trên local do topology hoặc chính sách browser | Trung bình                | Cao              | Kiểm thử sớm với `http://localhost:5173`, xác nhận browser đang gửi credentials đúng cách.                    |
| 2   | Seed non-prod bị nạp nhầm vào môi trường không cho phép do cấu hình sai               | Thấp đến trung bình       | Cao              | Tách Flyway location rõ ràng và coi việc nạp non-prod location ở `staging/prod` là fail acceptance.           |
| 3   | FE hiểu sai auth rehydration vì kỳ vọng có endpoint `/me` hoặc `/status`              | Trung bình                | Trung bình       | Chốt rõ trong Spec Pack và OpenAPI rằng `GET /api/v1/auth/csrf` là cơ chế rehydrate phiên.                    |
| 4   | Một số browser/local setup xử lý `SameSite=None` khác nhau khi không có HTTPS local   | Trung bình                | Trung bình       | Ưu tiên test đúng flow ở topology gần staging; không dùng local result để phủ nhận contract của staging/prod. |
| 5   | In-memory session không phù hợp nếu triển khai nhiều instance                         | Thấp trong phạm vi ticket | Trung bình       | Ghi rõ ngoài phạm vi; không xem multi-instance session sharing là acceptance của ticket.                      |

## 11. Wireframe

### Màn hình Login

```text
+----------------------------------------------------------------------------------+
| LOGO                                                                             |
|----------------------------------------------------------------------------------|
|                                ĐĂNG NHẬP HỆ THỐNG                                |
|                                                                                  |
|  Username                                                                        |
|  +--------------------------------------------------------------------------+    |
|  | user01                                                                   |    |
|  +--------------------------------------------------------------------------+    |
|                                                                                  |
|  Password                                                                        |
|  +--------------------------------------------------------------------------+    |
|  | ********                                                                  |    |
|  +--------------------------------------------------------------------------+    |
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

### Trạng thái đang lấy CSRF token sau login thành công

```text
+----------------------------------------------------------------------------------+
| Header: My App                                                                   |
|----------------------------------------------------------------------------------|
| Đang xác thực...                                                                 |
|                                                                                  |
|  [1] Đăng nhập thành công                                                        |
|  [2] Đã tạo session                                                              |
|  [3] Đang lấy CSRF token                                                         |
|                                                                                  |
+----------------------------------------------------------------------------------+
```

### Trạng thái session hết hạn

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

### Ghi chú

- Các trạng thái chính: login thành công, đang lấy CSRF, session hết hạn, lỗi `401`, lỗi `403` do CSRF.
- Thông điệp lỗi trên UI phải map được tối thiểu từ các mã: `AUTH_INVALID_CREDENTIALS`, `AUTH_USER_DISABLED`, `AUTH_SESSION_REQUIRED`, `AUTH_SESSION_EXPIRED`, `AUTH_CSRF_INVALID`.
- Không hiển thị hoặc nhập email ở màn hình login trong ticket này.
- Lưu ý responsive hoặc mobile: chưa có đặc tả riêng từ raw source; không tự suy diễn ở giai đoạn này.

---

## Bảng truy vết

| #   | AC            | Màn hình/API                                                            | DB                             | Logs                              | Quyền                                   | Loại kiểm thử |
| --- | ------------- | ----------------------------------------------------------------------- | ------------------------------ | --------------------------------- | --------------------------------------- | ------------- |
| 1   | AC-auth-1/v1  | API `POST /api/v1/auth/login`, Screen: Login                            | `users`, `user_roles`, `roles` | login_success                     | Anonymous -> Authenticated              | IT · E2E      |
| 2   | AC-auth-2/v1  | API `POST /api/v1/auth/login`                                           | N/A                            | login_success_cookie_policy       | Anonymous -> Authenticated              | IT · E2E      |
| 3   | AC-auth-3/v1  | API `POST /api/v1/auth/login`                                           | N/A                            | login_success_cookie_policy       | Anonymous -> Authenticated              | IT · E2E      |
| 4   | AC-auth-4/v1  | API `POST /api/v1/auth/login`, Screen: Login error                      | `users`                        | login_failure_invalid_credentials | Anonymous                               | IT · E2E      |
| 5   | AC-auth-5/v1  | API `POST /api/v1/auth/login`, Screen: Login error                      | `users`                        | login_failure_disabled_user       | Anonymous                               | IT · E2E      |
| 6   | AC-auth-6/v1  | API `GET /api/v1/auth/csrf`, any protected API                          | Session config                 | session_expired                   | Authenticated -> Unauthenticated        | IT · E2E      |
| 7   | AC-auth-7/v1  | API `GET /api/v1/auth/csrf`                                             | Session-backed token context   | csrf_issued                       | Authenticated                           | IT · E2E      |
| 8   | AC-auth-8/v1  | API `GET /api/v1/auth/csrf`                                             | Session                        | unauthorized_no_session           | Anonymous                               | IT            |
| 9   | AC-auth-9/v1  | Protected `POST/PUT/PATCH/DELETE` APIs                                  | Session                        | unauthorized_no_session           | Anonymous                               | IT · E2E      |
| 10  | AC-auth-10/v1 | Protected `POST/PUT/PATCH/DELETE` APIs                                  | Session/CSRF context           | forbidden_missing_csrf            | Authenticated but untrusted request     | IT · E2E      |
| 11  | AC-auth-11/v1 | Protected `POST/PUT/PATCH/DELETE` APIs                                  | Session/CSRF context           | forbidden_invalid_csrf            | Authenticated but untrusted request     | IT · E2E      |
| 12  | AC-auth-12/v1 | Protected `POST/PUT/PATCH/DELETE` APIs                                  | N/A                            | protected_request_allowed         | Authenticated                           | IT · E2E      |
| 13  | AC-auth-13/v1 | API `POST /api/v1/auth/logout`, Screen/Header logout                    | Session                        | logout_success                    | Authenticated -> Unauthenticated        | IT · E2E      |
| 14  | AC-auth-14/v1 | API `POST /api/v1/auth/logout`                                          | Session                        | unauthorized_logout_no_session    | Anonymous                               | IT            |
| 15  | AC-auth-15/v1 | API `POST /api/v1/auth/logout`                                          | Session/CSRF context           | forbidden_logout_invalid_csrf     | Authenticated but untrusted request     | IT            |
| 16  | AC-auth-16/v1 | `/v3/api-docs`, `/swagger-ui/index.html`                                | N/A                            | api_docs_access                   | Developer/Tester only in allowed env    | IT · BB       |
| 17  | AC-auth-17/v1 | Auth repository / login path                                            | `users`, `roles`, `user_roles` | db_auth_read_write                | Service account                         | IT            |
| 18  | AC-auth-18/v1 | App startup / migration pipeline                                        | Schema history + auth tables   | flyway_migration_applied          | Service account                         | IT            |
| 19  | AC-auth-19/v1 | Startup in `dev/test`                                                   | `users`, `roles`, `user_roles` | seed_applied_non_prod             | Service account                         | IT            |
| 20  | AC-auth-20/v1 | Startup in `staging/prod`                                               | `users`, `roles`, `user_roles` | seed_skipped_prod_like            | Service account                         | IT · BB       |
| 21  | AC-auth-21/v1 | Runtime session management                                              | In-memory session only         | session_store_mode                | Service account                         | IT · BB       |
| 22  | AC-auth-22/v1 | Login/CSRF/Logout/protected APIs                                        | N/A                            | security_audit_events             | Anonymous/Authenticated/Service account | UT · IT       |
| 23  | AC-auth-23/v1 | Auth endpoints returning `401/403`                                      | N/A                            | error_response_emitted            | Anonymous/Authenticated                 | UT · IT       |
| 24  | AC-auth-24/v1 | API `POST /api/v1/auth/login` success response                          | `user_roles`, `roles`          | login_success_response            | Authenticated                           | UT · IT       |
| 25  | AC-auth-25/v1 | Screen: Login, API docs, API `POST /api/v1/auth/login`                  | `users`                        | ui_contract_validated             | Anonymous                               | BB · E2E      |
| 26  | AC-auth-26/v1 | API `GET /api/v1/auth/csrf`, app reload flow                            | Session-backed token context   | csrf_reissued_after_reload        | Authenticated                           | IT · E2E      |
| 27  | AC-auth-27/v1 | CORS preflight and credentialed auth calls from `http://localhost:5173` | N/A                            | cors_allowed_origin               | Anonymous/Authenticated                 | IT · E2E      |
