# Spec Pack — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng, Springdoc, PostgreSQL, Flyway)

> Cập nhật: 2026-04-01 · Phase: 1  
> **Single source of truth for this change.**  
> Do not implement anything not written here.

---

## 1. Background / Purpose

Ticket này chuẩn hóa nền tảng authentication/backend cho hệ thống theo mô hình **server-side session** thay vì JWT. Mục tiêu là:

- cho phép người dùng đăng nhập bằng thông tin xác thực cơ bản;
- duy trì phiên bằng **session cookie**;
- hỗ trợ mô hình **frontend ↔ backend cross-site**;
- bảo vệ các request thay đổi dữ liệu bằng **CSRF token được cấp qua API riêng**;
- chuẩn hóa hạ tầng backend với **Springdoc**, **PostgreSQL**, và **Flyway** để làm nền cho các story tiếp theo.

Ở bản Spec Pack này, các điểm từng là Open Issues đã được chốt để implementation có thể bắt đầu. Tài liệu này tập trung vào **hành vi, contract, dữ liệu, ràng buộc môi trường, AC và traceability**; không đi sâu vào đề xuất implementation chi tiết.

## 2. Scope

### In scope

- Đăng nhập bằng `username/password`.
- Tạo và duy trì **server-side session** qua session cookie.
- Hỗ trợ cấu hình phục vụ **cross-site integration**.
- Cung cấp API riêng để lấy **CSRF token** sau khi đăng nhập thành công.
- Yêu cầu **CSRF token hợp lệ** cho các request thay đổi dữ liệu thuộc phạm vi được bảo vệ.
- Logout và invalidate session hiện tại.
- Tích hợp **Springdoc/OpenAPI** theo profile môi trường.
- Sử dụng **PostgreSQL** làm database chính cho user authentication data.
- Tích hợp **Flyway** để quản lý schema/migration.
- Seed user mặc định cho **dev/test**.
- Bổ sung wireframe/auth flow để FE có thể bám vào contract nghiệp vụ.

### Out of scope

- Email login hoặc generic login identifier.
- Social login.
- MFA/2FA.
- Remember me.
- JWT / refresh token.
- Redis hoặc persistent/distributed session store.
- Quên mật khẩu / reset mật khẩu.
- Phân quyền nhiều tầng/phức tạp.
- SSO, federation, hoặc external IdP.
- Endpoint bootstrap auth state riêng như `/me` hoặc `/session`.
- Hardening production infra ngoài những gì được đặc tả rõ trong tài liệu này.

## 3. Terminology

| # | Term | Definition |
| --- | ---- | ---------- |
| 1 | Server-side session | Phiên làm việc được lưu ở backend; trình duyệt chỉ giữ session identifier qua cookie |
| 2 | Session cookie | Cookie dùng để liên kết request của trình duyệt với server-side session |
| 3 | Cross-site integration | FE và BE hoạt động khác site/origin, vì vậy request cần CORS + credentials phù hợp |
| 4 | CSRF token | Token chống giả mạo request, phải gửi kèm các request thay đổi dữ liệu |
| 5 | Mutating request | Request có khả năng thay đổi dữ liệu hệ thống, gồm `POST`, `PUT`, `PATCH`, `DELETE` |
| 6 | Idle timeout | Thời gian hết hạn session tính theo thời gian không hoạt động |
| 7 | Protected endpoint | Endpoint yêu cầu session hợp lệ; với mutating endpoint còn phải có CSRF hợp lệ |
| 8 | Seed user | Tài khoản mặc định chỉ dùng cho môi trường dev/test để hỗ trợ phát triển và kiểm thử |
| 9 | OpenAPI / Swagger UI | Cơ chế mô tả API và giao diện tài liệu API ở môi trường được phép |
| 10 | Host-only cookie | Cookie không set `Domain`; cookie chỉ áp dụng cho host đã set nó |
| 11 | Error response schema | Cấu trúc JSON chuẩn cho response lỗi `401` và `403` trong phạm vi ticket này |

## 4. As-Is / To-Be

| # | Aspect | As-Is | To-Be |
| --- | ------ | ----- | ----- |
| 1 | Cơ chế auth | Chưa có nền tảng auth được chuẩn hóa trong scope ticket | Có login bằng session cookie, logout, session timeout theo môi trường |
| 2 | Bảo vệ request thay đổi dữ liệu | Chưa có contract CSRF rõ ràng | CSRF token được cấp qua API riêng và bắt buộc cho mutating protected requests |
| 3 | Hỗ trợ cross-site | Chưa có đặc tả cookie/CORS rõ ràng | Có policy cookie và allowlist origin theo môi trường |
| 4 | Tài liệu API | Chưa chuẩn hóa | Có Springdoc/OpenAPI ở dev/staging; prod disable hoàn toàn |
| 5 | Persistence auth data | Chưa chốt DB chuẩn | Dùng PostgreSQL cho user auth data |
| 6 | Schema lifecycle | Chưa có migration chuẩn hóa | Dùng Flyway để quản lý schema/migration |
| 7 | Test/dev bootstrap | Chưa có account chuẩn | Có seed user mặc định cho dev/test |
| 8 | Auth bootstrap cho FE | Chưa rõ có endpoint riêng hay không | Không thêm endpoint riêng; FE bootstrap bằng `GET /api/v1/auth/csrf` |

## 5. Detailed Specification

### 5.1. Actor và business intent

**Primary actor:** người dùng của hệ thống thông qua web frontend.  
**Supporting actor:** frontend application gọi backend API theo cơ chế gửi cookie kèm credentials.

### 5.2. Login

#### Endpoint

`POST /api/v1/auth/login`

#### Request contract

Request body phải chứa đúng hai field sau:

```json
{
  "username": "user01",
  "password": "******"
}
```

Quyết định chốt:

- Phase này **chỉ hỗ trợ `username`**.
- Không hỗ trợ `email` như login identifier.
- FE phải hiển thị nhãn input là **Username**.

#### Success behavior

Khi credential hợp lệ và user ở trạng thái được phép đăng nhập:

- trả `200 OK`;
- tạo **server-side session**;
- trả **session cookie**;
- body response tối thiểu:

```json
{
  "authenticated": true,
  "username": "user01"
}
```

Ràng buộc contract:

- `authenticated` là boolean và phải bằng `true` khi login thành công.
- `username` là string và phản ánh username đã xác thực.
- Trường `roles` **không phải field bắt buộc** của contract phase này.
- FE phải bỏ qua các field bổ sung không được đặc tả.

#### Failure behavior

- Khi credential không hợp lệ: trả `401 Unauthorized`.
- Khi user có `enabled=false`: trả `401 Unauthorized` với cùng error code/message như credential sai.
- Khi login thất bại: không được tạo session hợp lệ.

#### Standard error response schema

Tất cả response lỗi `401` và `403` trong phạm vi auth flow của ticket này phải theo schema sau:

```json
{
  "timestamp": "2026-04-01T10:15:30Z",
  "status": 401,
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid username or password.",
  "path": "/api/v1/auth/login"
}
```

Field rules:

- `timestamp`: thời điểm phát sinh lỗi theo ISO-8601.
- `status`: mã HTTP tương ứng.
- `code`: mã lỗi machine-readable.
- `message`: thông điệp human-readable ngắn, ổn định cho FE hiển thị/log.
- `path`: request path.

Error codes được chốt trong phạm vi ticket này:

- `AUTH_INVALID_CREDENTIALS` → login sai credential hoặc user bị disable.
- `AUTH_SESSION_REQUIRED` → request yêu cầu session hợp lệ nhưng không có session hoặc session đã hết hạn.
- `AUTH_CSRF_REQUIRED` → mutating protected request thiếu CSRF token.
- `AUTH_CSRF_INVALID` → mutating protected request có CSRF token sai hoặc hết hiệu lực.

### 5.3. Session cookie policy

Do có cross-site integration, session cookie phải thỏa:

- `HttpOnly=true`
- `SameSite=None`
- `Path=/`
- `Secure=true` ở `staging` và `production`
- `Domain` **không được set** trong phase này, tức dùng **host-only cookie**

Quyết định chốt bổ sung:

- Cookie name **không là public contract** của API/FE trong phase này.
- FE không được hard-code hoặc phụ thuộc vào cookie name; chỉ dựa vào cơ chế browser-managed cookie + credentials.

### 5.4. Session timeout

Session idle timeout phải theo môi trường:

- `dev`: 8 giờ
- `staging`: 2 giờ
- `production`: 30 phút

Đây là **idle timeout**, không phải absolute lifetime, và không triển khai remember-me.

### 5.5. CSRF token API

#### Endpoint

`GET /api/v1/auth/csrf`

#### Purpose

API này cấp CSRF token **riêng biệt** với login response. Frontend gọi API này sau khi login thành công và giữ token trong memory state để dùng cho các mutating request.

API này cũng là **cơ chế bootstrap auth state** cho FE trong phase này:

- nếu gọi thành công → FE có thể coi session hiện còn hiệu lực;
- nếu trả `401` → FE coi session không hợp lệ và điều hướng về login.

#### Success behavior

Chỉ khi request có **session hợp lệ**, hệ thống mới trả CSRF token.

Response body:

```json
{
  "csrfToken": "generated-token-value",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

#### Failure behavior

- Không có session hoặc session hết hạn: trả `401 Unauthorized` với error code `AUTH_SESSION_REQUIRED`.

### 5.6. Protected request behavior

Đối với các **mutating protected requests** (`POST`, `PUT`, `PATCH`, `DELETE`):

1. phải có **session hợp lệ**;
2. phải có **CSRF token hợp lệ**.

Header chuẩn để FE gửi CSRF token:

```http
X-CSRF-TOKEN: <token>
```

Khi vi phạm:

- thiếu session hoặc session không còn hiệu lực → `401 Unauthorized` + `AUTH_SESSION_REQUIRED`;
- thiếu CSRF token → `403 Forbidden` + `AUTH_CSRF_REQUIRED`;
- CSRF token không hợp lệ hoặc hết hiệu lực → `403 Forbidden` + `AUTH_CSRF_INVALID`.

> Ghi chú phạm vi: ticket này đặc tả **nguyên tắc chung** cho protected mutating endpoints. Danh sách business endpoint cụ thể ngoài auth flow không nằm trong raw input hiện tại.

### 5.7. Logout

#### Endpoint

`POST /api/v1/auth/logout`

#### Behavior matrix

| Case | Session | CSRF | Expected result |
|------|---------|------|-----------------|
| 1 | Hợp lệ | Hợp lệ | `200 OK`, invalidate session, clear/expire cookie, body thành công |
| 2 | Hợp lệ | Thiếu hoặc không hợp lệ | `403 Forbidden`, không invalidate session |
| 3 | Không có hoặc đã hết hạn | Không yêu cầu | `200 OK`, clear/expire cookie nếu client đang giữ cookie cũ, body thành công |

#### Success response

```json
{
  "success": true
}
```

Quyết định chốt:

- Logout là **idempotent** khi session không còn hợp lệ.
- Mục tiêu của FE khi logout là luôn đưa client về trạng thái logged-out, không yêu cầu phân biệt lỗi logout do session cũ.

### 5.8. CORS / credentials policy

Vì auth flow dùng cross-site cookie, backend phải hỗ trợ credentials và apply origin allowlist.

#### Rules

- `Access-Control-Allow-Credentials` phải là `true` cho các origin hợp lệ.
- Không được dùng wildcard origin (`*`) khi credentials được bật.
- Chỉ các origin trong allowlist mới được nhận CORS response hợp lệ.

#### Environment allowlist

**dev**:

- `http://localhost:3000`
- `http://127.0.0.1:3000`
- `http://localhost:5173`
- `http://127.0.0.1:5173`

**staging**:

- Danh sách origin hợp lệ phải được cấu hình explicit theo môi trường triển khai.
- Không được để trống nếu hệ thống cần phục vụ web frontend.

**production**:

- Danh sách origin hợp lệ phải được cấu hình explicit theo môi trường triển khai.
- Không được để trống nếu hệ thống cần phục vụ web frontend.
- Không được dùng wildcard origin.

### 5.9. Springdoc / OpenAPI

- `dev`: bật OpenAPI + Swagger UI
- `staging`: bật OpenAPI + Swagger UI
- `production`: **disable hoàn toàn** cả Swagger UI và OpenAPI document endpoint

Trong production, các endpoint sau không được truy cập được:

- `/swagger-ui/**`
- `/v3/api-docs`

Tài liệu API ở môi trường được bật phải mô tả tối thiểu:

- login;
- csrf;
- logout;
- request/response mẫu;
- mã trạng thái chính;
- cơ chế session cookie + CSRF header.

### 5.10. PostgreSQL data model tối thiểu

Database chính là **PostgreSQL**.

Các bảng tối thiểu trong scope auth foundation:

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

#### Business rules liên quan data

- Password phải lưu ở dạng **hash**, không lưu plaintext.
- Chỉ user có `enabled=true` mới được đăng nhập thành công.
- Có thể tồn tại liên kết user-role trong data model, nhưng response login phase này **không bắt buộc** phải trả `roles`.

### 5.11. Flyway

Hệ thống phải dùng Flyway để quản lý migration.

Migration tối thiểu được mong đợi:

- `V1__init_schema.sql`
- `V2__create_auth_tables.sql`
- `V3__seed_dev_test_users.sql`

Quy tắc seed:

- chỉ áp dụng cho `dev/test`;
- không seed ở `staging/production`.

Seed accounts chuẩn của phase này:

- `admin / Admin@123`
- `user01 / User@123`

Ràng buộc bảo mật:

- password seed phải được hash trước khi insert;
- tài khoản seed chỉ phục vụ local/dev/test;
- không được dùng ngoài môi trường phi sản xuất.

### 5.12. Frontend contract (mức nghiệp vụ)

Trình tự tương tác chuẩn:

1. User submit login form với `username/password`.
2. FE gọi `POST /api/v1/auth/login` với chế độ gửi cookie credentials.
3. Nếu login thành công, FE gọi `GET /api/v1/auth/csrf`.
4. FE lưu `csrfToken` trong memory state.
5. FE gắn `X-CSRF-TOKEN` vào mutating protected requests.
6. Nếu backend trả `401`, FE điều hướng user về login hoặc yêu cầu đăng nhập lại.
7. Nếu backend trả `403` do CSRF, FE hiển thị lỗi phù hợp và yêu cầu reload hoặc login lại.
8. Khi app reload, FE bootstrap auth state bằng `GET /api/v1/auth/csrf`; không dùng endpoint `/me` hoặc `/session` trong phase này.

### 5.13. Logging / observability contract mức tối thiểu

Để hỗ trợ vận hành và test, hệ thống phải có log nghiệp vụ ở mức tối thiểu cho:

- login thành công/thất bại;
- từ chối request do thiếu session;
- từ chối request do thiếu/không hợp lệ CSRF;
- logout thành công;
- migration startup/result;
- seed applied/skipped.

Ràng buộc:

- không log plaintext password;
- không log session id hoặc csrf token ở dạng nhạy cảm;
- log phải đủ để phân biệt `401` do session và `403` do CSRF.

## 6. Non-Functional Requirements

| # | Category | Requirement |
| --- | ------------- | ----------- |
| 1 | Security | Password phải được hash bằng encoder mạnh; không lưu plaintext password |
| 2 | Security | Session cookie phải tuân theo policy cross-site đã chốt (`HttpOnly`, `SameSite=None`, `Path=/`, `Secure` theo môi trường, `Domain` omitted) |
| 3 | Security | CORS chỉ được cho phép các origin nằm trong allowlist theo môi trường; không được dùng wildcard origin khi credentials được bật |
| 4 | Security | Không log password, session id, CSRF token ở dạng nhạy cảm |
| 5 | Availability | Auth flow phải hoạt động với session store mặc định trong phạm vi single-instance/đơn giản của ticket này |
| 6 | Performance | Login, lấy CSRF token, logout phải không yêu cầu round-trip ngoài các thành phần nằm trong scope backend hiện tại |
| 7 | Maintainability | Cấu hình phải tách theo profile môi trường (`dev`, `staging`, `production`) |
| 8 | Maintainability | Schema DB phải được quản lý bằng migration có version |
| 9 | Observability | Phải có log tối thiểu cho login, logout, session rejection, CSRF rejection, migration outcome, seed outcome |
| 10 | Documentation | OpenAPI/Swagger ở môi trường được phép phải phản ánh đúng contract auth thực tế |

## 7. AC (Acceptance Criteria)

| # | ID | Statement | Test type |
| --- | ---------------------------- | --------- | ------------ |
| 1 | AC-auth-session-csrf-1/v1 | Hệ thống phải cung cấp endpoint `POST /api/v1/auth/login` nhận request body chứa đúng hai trường `username` và `password`. | IT · E2E |
| 2 | AC-auth-session-csrf-2/v1 | Khi `username/password` hợp lệ và user có `enabled=true`, hệ thống phải trả `200 OK`, tạo server-side session và trả session cookie trong response. | IT · E2E |
| 3 | AC-auth-session-csrf-3/v1 | Response login thành công phải chứa tối thiểu `authenticated=true` và `username` là username đã xác thực. | IT · E2E |
| 4 | AC-auth-session-csrf-4/v1 | Trường `roles` không được coi là bắt buộc trong response login phase này; FE không được phụ thuộc vào sự tồn tại của trường này. | BB · E2E |
| 5 | AC-auth-session-csrf-5/v1 | Khi credential không hợp lệ, endpoint login phải trả `401 Unauthorized` với body theo standard error response schema và `code=AUTH_INVALID_CREDENTIALS`, đồng thời không tạo session hợp lệ. | IT · E2E |
| 6 | AC-auth-session-csrf-6/v1 | Khi user có `enabled=false`, endpoint login phải trả `401 Unauthorized` với `code=AUTH_INVALID_CREDENTIALS` và không tạo session hợp lệ. | IT |
| 7 | AC-auth-session-csrf-7/v1 | Session cookie được trả sau login thành công phải có `HttpOnly=true`. | IT |
| 8 | AC-auth-session-csrf-8/v1 | Session cookie được trả sau login thành công phải có `SameSite=None`. | IT |
| 9 | AC-auth-session-csrf-9/v1 | Session cookie được trả sau login thành công phải có `Path=/`. | IT |
| 10 | AC-auth-session-csrf-10/v1 | Ở profile `staging` và `production`, session cookie được trả sau login thành công phải có `Secure=true`. | IT |
| 11 | AC-auth-session-csrf-11/v1 | Session cookie trong phase này không được set `Domain` attribute. | IT |
| 12 | AC-auth-session-csrf-12/v1 | Session idle timeout phải được cấu hình đúng theo môi trường: dev=8h, staging=2h, production=30m. | IT |
| 13 | AC-auth-session-csrf-13/v1 | Hệ thống phải cung cấp endpoint `GET /api/v1/auth/csrf` và chỉ trả token khi request có session hợp lệ. | IT · E2E |
| 14 | AC-auth-session-csrf-14/v1 | Response thành công của endpoint CSRF phải chứa `csrfToken`, `headerName`, và `parameterName`. | IT |
| 15 | AC-auth-session-csrf-15/v1 | Khi không có session hoặc session đã hết hạn, endpoint `GET /api/v1/auth/csrf` phải trả `401 Unauthorized` với body theo standard error response schema và `code=AUTH_SESSION_REQUIRED`. | IT · E2E |
| 16 | AC-auth-session-csrf-16/v1 | FE phải có thể dùng `GET /api/v1/auth/csrf` làm cơ chế bootstrap auth state sau reload; ticket này không yêu cầu endpoint `/me` hoặc `/session`. | BB · E2E |
| 17 | AC-auth-session-csrf-17/v1 | Các mutating protected requests (`POST`, `PUT`, `PATCH`, `DELETE`) phải yêu cầu session hợp lệ. | IT · E2E |
| 18 | AC-auth-session-csrf-18/v1 | Các mutating protected requests phải chấp nhận CSRF token qua header `X-CSRF-TOKEN`. | IT |
| 19 | AC-auth-session-csrf-19/v1 | Khi mutating protected request không có session hợp lệ, hệ thống phải trả `401 Unauthorized` với `code=AUTH_SESSION_REQUIRED`. | IT · E2E |
| 20 | AC-auth-session-csrf-20/v1 | Khi mutating protected request thiếu CSRF token, hệ thống phải trả `403 Forbidden` với `code=AUTH_CSRF_REQUIRED`. | IT · E2E |
| 21 | AC-auth-session-csrf-21/v1 | Khi mutating protected request có CSRF token không hợp lệ hoặc hết hiệu lực, hệ thống phải trả `403 Forbidden` với `code=AUTH_CSRF_INVALID`. | IT · E2E |
| 22 | AC-auth-session-csrf-22/v1 | Hệ thống phải cung cấp endpoint `POST /api/v1/auth/logout`. | IT |
| 23 | AC-auth-session-csrf-23/v1 | Khi logout với session hợp lệ và CSRF hợp lệ, session hiện tại phải bị invalidate ở server, response phải trả `200 OK`, body `{ "success": true }`, và phải yêu cầu clear/expire session cookie ở client. | IT · E2E |
| 24 | AC-auth-session-csrf-24/v1 | Khi logout được gọi mà không có session hợp lệ, hệ thống phải vẫn trả `200 OK` với body `{ "success": true }` và clear/expire session cookie nếu client đang giữ cookie cũ. | IT · E2E |
| 25 | AC-auth-session-csrf-25/v1 | Khi logout được gọi với session hợp lệ nhưng CSRF thiếu hoặc không hợp lệ, hệ thống phải trả `403 Forbidden` theo standard error response schema và không invalidate session. | IT |
| 26 | AC-auth-session-csrf-26/v1 | Ở profile `dev` và `staging`, `/v3/api-docs` và Swagger UI phải truy cập được. | IT |
| 27 | AC-auth-session-csrf-27/v1 | Ở profile `production`, `/v3/api-docs` và `/swagger-ui/**` không được public và phải bị disable hoàn toàn. | IT · BB |
| 28 | AC-auth-session-csrf-28/v1 | Tài liệu OpenAPI ở môi trường được bật phải mô tả tối thiểu login, csrf, logout cùng request/response mẫu, mã trạng thái chính, session cookie behavior và CSRF header. | BB |
| 29 | AC-auth-session-csrf-29/v1 | Ứng dụng phải sử dụng PostgreSQL cho dữ liệu authentication và lưu password ở cột hash, không lưu plaintext password. | IT · BB |
| 30 | AC-auth-session-csrf-30/v1 | Flyway migration phải chạy được để tạo tối thiểu các bảng `users`, `roles`, `user_roles` trên database mới. | IT |
| 31 | AC-auth-session-csrf-31/v1 | Seed user mặc định phải chỉ được áp dụng ở `dev/test`, gồm tối thiểu `admin` và `user01`, và không được áp dụng ở `staging/production`. | IT · BB |
| 32 | AC-auth-session-csrf-32/v1 | Với credentials được bật, backend chỉ được trả CORS allow-origin cho các origin nằm trong allowlist theo môi trường và không được dùng wildcard origin (`*`). | IT · BB |
| 33 | AC-auth-session-csrf-33/v1 | Hệ thống phải có log tối thiểu cho login success/failure, session rejection, CSRF rejection, logout success, migration outcome, và seed outcome mà không chứa plaintext password, session id, hoặc CSRF token. | IT · BB |
| 34 | AC-auth-session-csrf-34/v1 | Mọi response lỗi `401` và `403` trong auth flow phải tuân theo standard error response schema gồm `timestamp`, `status`, `code`, `message`, `path`. | IT · E2E |

## 8. Examples

### Normal cases

1. **Login thành công + lấy CSRF token thành công**  
   - Input: `POST /api/v1/auth/login` với `username=user01`, `password` đúng, user `enabled=true`  
   - Expected: `200 OK` + session cookie hợp lệ + body có `authenticated=true`, `username=user01`  
   - Tiếp theo gọi `GET /api/v1/auth/csrf` cùng cookie  
   - Expected: `200 OK` + body có `csrfToken`, `headerName`, `parameterName`

2. **Reload app và bootstrap auth state thành công**  
   - Precondition: user đã login, session còn hiệu lực, FE bị full page refresh nên mất memory state  
   - Input: FE gọi `GET /api/v1/auth/csrf` cùng session cookie  
   - Expected: `200 OK`, FE lấy lại `csrfToken` và tiếp tục coi user là authenticated

3. **Logout thành công khi session còn hiệu lực**  
   - Precondition: user đã login thành công, session còn hiệu lực, FE đang giữ CSRF token hợp lệ  
   - Input: `POST /api/v1/auth/logout` kèm session cookie + `X-CSRF-TOKEN`  
   - Expected: session bị invalidate, response yêu cầu clear/expire cookie, body `{ "success": true }`

### Abnormal cases

1. **Sai username hoặc password**  
   - Input: `POST /api/v1/auth/login` với credential sai  
   - Expected: `401 Unauthorized`, body error chuẩn với `code=AUTH_INVALID_CREDENTIALS`, không có session hợp lệ được tạo

2. **User bị disable**  
   - Input: `POST /api/v1/auth/login` với password đúng nhưng `enabled=false`  
   - Expected: `401 Unauthorized`, body error chuẩn với `code=AUTH_INVALID_CREDENTIALS`, không tạo session hợp lệ

3. **Mutating protected request thiếu CSRF**  
   - Precondition: user có session hợp lệ  
   - Input: gọi một protected `POST/PUT/PATCH/DELETE` nhưng không gửi `X-CSRF-TOKEN`  
   - Expected: `403 Forbidden`, body error chuẩn với `code=AUTH_CSRF_REQUIRED`

4. **Lấy CSRF token khi session đã hết hạn**  
   - Input: `GET /api/v1/auth/csrf` với cookie cũ sau khi idle timeout đã vượt ngưỡng  
   - Expected: `401 Unauthorized`, body error chuẩn với `code=AUTH_SESSION_REQUIRED`

5. **Logout khi session đã hết hạn**  
   - Input: `POST /api/v1/auth/logout` với cookie cũ, không còn session hợp lệ  
   - Expected: `200 OK`, body `{ "success": true }`, response cố gắng clear/expire cookie; FE coi user đã logged-out

### Boundary values

1. **Ngay trước ngưỡng session timeout**  
   - Precondition: session đang hoạt động trong production profile  
   - Input: gọi `GET /api/v1/auth/csrf` ngay trước khi idle timeout 30 phút bị vượt qua  
   - Expected: request vẫn được xử lý theo session còn hiệu lực

2. **Ngay sau ngưỡng session timeout**  
   - Precondition: session production đã vượt idle timeout 30 phút  
   - Input: gọi `GET /api/v1/auth/csrf` với cookie cũ  
   - Expected: `401 Unauthorized` với `code=AUTH_SESSION_REQUIRED`

3. **Cookie attribute boundary theo môi trường**  
   - Input: login ở `dev` profile và ở `staging` profile  
   - Expected: cả hai đều có `HttpOnly=true`, `SameSite=None`, `Path=/`; riêng `staging` phải có `Secure=true`

4. **CORS boundary với origin không nằm trong allowlist**  
   - Input: browser app từ origin không thuộc allowlist gọi auth API với credentials  
   - Expected: backend không trả CORS allow-origin hợp lệ cho origin đó, browser không cho phép flow cross-site hoạt động

## 9. Open Issues

Không còn Open Issue chặn implementation trong phase này.

Các quyết định từng là Open Issues đã được chốt và đưa thẳng vào các section contract ở trên. Nếu phát sinh yêu cầu mới ngoài tài liệu này, phải mở ticket/spec thay đổi riêng thay vì tự mở rộng phạm vi.

## 10. Risks

| # | Risk | Likelihood | Impact | Mitigation |
| --- | ---- | ---------- | ------ | ---------- |
| 1 | Cross-site cookie không hoạt động đúng do cấu hình CORS/origin/HTTPS sai | High | High | Bám allowlist theo spec, test thật với credentials và HTTPS ở staging |
| 2 | Local dev không phản ánh đúng hành vi cookie của staging/prod | High | Medium | Tách rõ expectation giữa local và môi trường có HTTPS thật |
| 3 | In-memory session không phù hợp khi scale nhiều instance | Medium | High | Giới hạn scope hiện tại rõ ràng; deferred sang ticket khác |
| 4 | FE mất CSRF token khi full page refresh | Medium | Medium | Bootstrap lại bằng `GET /api/v1/auth/csrf` theo spec |
| 5 | FE hard-code cookie name hoặc phụ thuộc `roles` | Medium | Medium | Chốt rõ cookie name opaque và `roles` không bắt buộc trong contract |
| 6 | Staging/prod thiếu allowlist origin explicit làm cross-site flow không chạy | Medium | High | Xem allowlist origin là config bắt buộc trước deploy |
| 7 | Logout idempotent bị hiểu sai là bỏ qua CSRF mọi trường hợp | Low | Medium | Giữ ma trận hành vi rõ: session hợp lệ vẫn phải có CSRF hợp lệ |

## 11. Wireframe

### 11.1. Login screen

```text
+----------------------------------------------------------------------------------+
| LOGO                                                                             |
|----------------------------------------------------------------------------------|
|                                ĐĂNG NHẬP HỆ THỐNG                                |
|                                                                                  |
|  Username (*)                                                                    |
|  +--------------------------------------------------------------------------+    |
|  | user01                                                                   |    |
|  +--------------------------------------------------------------------------+    |
|                                                                                  |
|  Password (*)                                                                    |
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

### 11.2. Session expired / re-login state

```text
+----------------------------------------------------------------------------------+
| PHIÊN ĐĂNG NHẬP ĐÃ HẾT HẠN                                                       |
|----------------------------------------------------------------------------------|
| Phiên làm việc của bạn không còn hiệu lực. Vui lòng đăng nhập lại.               |
|                                                                                  |
|  +------------------------------+                                                |
|  |        ĐĂNG NHẬP LẠI         |                                                |
|  +------------------------------+                                                |
+----------------------------------------------------------------------------------+
```

---

## Traceability Table

| # | AC | Screen/API | DB | Logs | Permissions | Test types |
| --- | ---------------------------- | ---------- | --- | ---- | ----------- | ------------- |
| 1 | AC-auth-session-csrf-1/v1 | API: `POST /api/v1/auth/login`; Screen: Login | `users` | `auth.login.attempt` | Public | IT · E2E |
| 2 | AC-auth-session-csrf-2/v1 | API: `POST /api/v1/auth/login`; Screen: Login → Authenticated | `users` | `auth.login.success` | Public → authenticated session established | IT · E2E |
| 3 | AC-auth-session-csrf-3/v1 | API: login success response | `users` | `auth.login.success` | Public | IT · E2E |
| 4 | AC-auth-session-csrf-4/v1 | API: login response contract; Screen: authenticated shell | N/A | N/A | Public / FE contract | BB · E2E |
| 5 | AC-auth-session-csrf-5/v1 | API: `POST /api/v1/auth/login`; Screen: Login error | `users` | `auth.login.failure` | Public | IT · E2E |
| 6 | AC-auth-session-csrf-6/v1 | API: `POST /api/v1/auth/login` | `users.enabled` | `auth.login.failure` | Public | IT |
| 7 | AC-auth-session-csrf-7/v1 | API: login response cookie | N/A | N/A | Public | IT |
| 8 | AC-auth-session-csrf-8/v1 | API: login response cookie | N/A | N/A | Public | IT |
| 9 | AC-auth-session-csrf-9/v1 | API: login response cookie | N/A | N/A | Public | IT |
| 10 | AC-auth-session-csrf-10/v1 | API: login response cookie (staging/prod) | N/A | N/A | Public | IT |
| 11 | AC-auth-session-csrf-11/v1 | API: login response cookie domain behavior | N/A | N/A | Public | IT |
| 12 | AC-auth-session-csrf-12/v1 | Session config by profile | N/A | `auth.session.timeout.config` (startup/config log, nếu có) | Environment-scoped | IT |
| 13 | AC-auth-session-csrf-13/v1 | API: `GET /api/v1/auth/csrf` | N/A | `auth.csrf.issued` | Authenticated session required | IT · E2E |
| 14 | AC-auth-session-csrf-14/v1 | API: `GET /api/v1/auth/csrf` | N/A | `auth.csrf.issued` | Authenticated session required | IT |
| 15 | AC-auth-session-csrf-15/v1 | API: `GET /api/v1/auth/csrf`; Screen: Session expired | N/A | `auth.session.denied` | Authenticated session required | IT · E2E |
| 16 | AC-auth-session-csrf-16/v1 | Screen/API: app reload bootstrap via `GET /api/v1/auth/csrf` | N/A | `auth.csrf.issued` / `auth.session.denied` | Authenticated session required | BB · E2E |
| 17 | AC-auth-session-csrf-17/v1 | Protected mutating APIs (generic rule) | N/A | `auth.request.denied.no-session` | Authenticated session required | IT · E2E |
| 18 | AC-auth-session-csrf-18/v1 | Protected mutating APIs; header `X-CSRF-TOKEN` | N/A | N/A | Authenticated session + CSRF | IT |
| 19 | AC-auth-session-csrf-19/v1 | Protected mutating APIs | N/A | `auth.request.denied.no-session` | Authenticated session required | IT · E2E |
| 20 | AC-auth-session-csrf-20/v1 | Protected mutating APIs; Screen: CSRF missing | N/A | `auth.csrf.denied.missing` | Authenticated session + CSRF | IT · E2E |
| 21 | AC-auth-session-csrf-21/v1 | Protected mutating APIs; Screen: CSRF invalid | N/A | `auth.csrf.denied.invalid` | Authenticated session + CSRF | IT · E2E |
| 22 | AC-auth-session-csrf-22/v1 | API: `POST /api/v1/auth/logout` | N/A | `auth.logout.attempt` | Authenticated/anonymous caller allowed | IT |
| 23 | AC-auth-session-csrf-23/v1 | API: `POST /api/v1/auth/logout`; Screen: Authenticated → Logged out | N/A | `auth.logout.success` | Authenticated session + CSRF | IT · E2E |
| 24 | AC-auth-session-csrf-24/v1 | API: `POST /api/v1/auth/logout`; Screen: stale session → Logged out | N/A | `auth.logout.idempotent-success` | Anonymous/stale session allowed | IT · E2E |
| 25 | AC-auth-session-csrf-25/v1 | API: `POST /api/v1/auth/logout` | N/A | `auth.csrf.denied.missing` / `auth.csrf.denied.invalid` | Authenticated session + CSRF | IT |
| 26 | AC-auth-session-csrf-26/v1 | API docs: `/v3/api-docs`, `/swagger-ui/index.html` | N/A | `platform.openapi.enabled` | Environment-scoped | IT |
| 27 | AC-auth-session-csrf-27/v1 | API docs in production | N/A | `platform.openapi.disabled` | Production restriction | IT · BB |
| 28 | AC-auth-session-csrf-28/v1 | OpenAPI document content | N/A | N/A | Environment-scoped | BB |
| 29 | AC-auth-session-csrf-29/v1 | Auth persistence | `users.password_hash`, `roles`, `user_roles` | `auth.login.success/failure` (không lộ bí mật) | Public/Auth depending flow | IT · BB |
| 30 | AC-auth-session-csrf-30/v1 | Flyway startup + DB schema creation | `users`, `roles`, `user_roles` | `db.migration.success/failure` | Service startup | IT |
| 31 | AC-auth-session-csrf-31/v1 | Seed behavior by profile | `users`, `roles`, `user_roles` | `db.seed.applied` / `db.seed.skipped` | Environment-scoped | IT · BB |
| 32 | AC-auth-session-csrf-32/v1 | CORS preflight/actual auth requests | N/A | `platform.cors.allowed-origin` / `platform.cors.rejected-origin` | Origin allowlist + credentials | IT · BB |
| 33 | AC-auth-session-csrf-33/v1 | All relevant auth/platform flows | N/A | See log events above | N/A | IT · BB |
| 34 | AC-auth-session-csrf-34/v1 | All auth-flow error responses | N/A | Error logs where applicable | N/A | IT · E2E |
