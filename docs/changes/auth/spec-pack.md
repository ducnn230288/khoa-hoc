# Spec Pack - auth (Authentication bằng Session Cookie + CSRF API riêng, Springdoc, PostgreSQL, Flyway)

> Single source of truth for ticket scope and acceptance after reconciling `docs/changes/auth/raw/spec.md` with repo-wide rules in `docs/standards/`.

---

## 1. Ticket Metadata

| Field | Value |
| ----- | ----- |
| Ticket | auth |
| Feature | Session-based authentication foundation |
| Story ID | AUTH-001 |
| Owner | TBD |
| Related branch | TBD |
| Last updated | 2026-03-30 |
| Status | Ready for implementation |

## 2. Background / Purpose

Ticket này thiết lập nền tảng xác thực cho hệ thống theo mô hình **server-side session** với các mục tiêu sau:

- người dùng đăng nhập bằng `username` + password và được duy trì phiên bằng session cookie;
- frontend lấy CSRF token qua API riêng thay vì nhận chung trong login response;
- backend hỗ trợ mô hình tích hợp **cross-site**;
- nền tảng backend được chuẩn hóa thêm bởi Springdoc, PostgreSQL và Flyway để các ticket sau có thể dựa vào.

Ticket này là **platform/auth foundation ticket**, không phải ticket mở rộng business flow hoặc authorization nhiều tầng.

## 3. Scope

### 3.1 In scope

1. Đăng nhập bằng `username` + password và tạo server-side session khi thành công.
2. Trả session id qua cookie theo policy của ticket.
3. Hỗ trợ lấy CSRF token qua `GET /api/v1/auth/csrf` sau khi đăng nhập thành công.
4. Yêu cầu session hợp lệ + CSRF hợp lệ cho request mutating thuộc boundary của ticket.
5. Logout qua `POST /api/v1/auth/logout` và invalidate session.
6. Cấu hình timeout session theo môi trường.
7. Tích hợp Springdoc ở dev/staging và không public ở production.
8. Dùng PostgreSQL cho dữ liệu authentication.
9. Dùng Flyway cho schema/migration auth.
10. Có dữ liệu user phục vụ dev/test theo cơ chế không vi phạm security rules.
11. Cung cấp wireframe/login flow đủ để FE triển khai UI auth cơ bản.

### 3.2 Out of scope

1. Social login.
2. MFA/2FA.
3. Remember me.
4. JWT hoặc refresh token.
5. Redis hoặc persistent session store.
6. Quên mật khẩu / reset mật khẩu.
7. Authorization nhiều tầng hoặc RBAC chi tiết vượt quá việc trả roles cơ bản trong login response.
8. Tạo mới business endpoints ngoài boundary auth chỉ để “demo” CSRF.

## 4. Terminology

| Term | Meaning |
| ---- | ------- |
| Session | Server-side authenticated state associated with a session id stored in cookie |
| Session cookie | Cookie chứa session identifier, do backend phát hành |
| CSRF token | Token chống Cross-Site Request Forgery, được lấy qua API riêng và gửi lại trong mutating requests |
| Cross-site | Frontend và backend khác origin; request cần CORS + credentials handling phù hợp |
| Mutating request | HTTP `POST`, `PUT`, `PATCH`, `DELETE` |
| Protected endpoint | Endpoint yêu cầu request đến từ user đã authenticated; với mutating endpoint còn cần CSRF hợp lệ |
| Seed user | User chỉ phục vụ dev/test/bootstrap verification, không tồn tại ở staging/prod |

## 5. Decision Summary

Các Open Issues đã được chốt như sau:

| Decision | Final choice |
| -------- | ------------ |
| Login identifier | `username` |
| Error response contract | Chuẩn JSON tối thiểu, machine-readable, không lộ thông tin nhạy cảm |
| Seed user mechanism | Username cố định trong dev/test; password cấp từ runtime secret input ngoài repo |
| Production API docs behavior | `/v3/api-docs` trả `404` |
| Allowed frontend origin for dev local | `http://localhost:5173` |

## 6. As-Is / To-Be

### 6.1 As-Is

- Repo docs mô tả backend/frontend là hai deliverable độc lập trong một repo (`docs/architecture/overview.md`).
- Repo-wide standards đã tồn tại cho coding, security và testing.
- Trong baseline docs hiện có, auth/session/CSRF/persistence chưa được chứng minh là đã tồn tại trong code snapshot được cung cấp cho phase này.
- Các executable inputs mà `overview.md` viện dẫn hiện `missing`, nên As-Is code/config chỉ có thể xác nhận ở mức tài liệu, không thể xác minh chi tiết từ source code trong gói hiện tại.

### 6.2 To-Be

Sau ticket này, hệ thống phải có một **auth foundation** với các đặc tính sau:

- có login endpoint tạo session server-side khi `username` + password hợp lệ;
- có session cookie policy phù hợp cho cross-site usage theo môi trường;
- có endpoint riêng để lấy CSRF token từ session hợp lệ;
- có logout endpoint invalidate session;
- có PostgreSQL schema tối thiểu cho users/roles;
- có Flyway migration để tạo schema auth và seed dev/test theo cơ chế an toàn;
- có tài liệu OpenAPI/Springdoc cho auth endpoints trong dev/staging;
- có UI/login flow/wireframe đủ rõ cho FE.

## 7. Detailed Specification

### 7.1 Functional boundary

Boundary chức năng của ticket này chỉ bao gồm auth foundation:

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/csrf`
- `POST /api/v1/auth/logout`
- session timeout/configuration
- auth persistence baseline (`users`, `roles`, `user_roles`)
- Springdoc exposure cho auth APIs

Ticket này **không bắt buộc** tạo thêm business endpoint mới ngoài auth để chứng minh CSRF.
`POST /api/v1/auth/logout` là mutating protected endpoint nằm trong boundary và phải tuân theo rule session + CSRF.

### 7.2 Login behavior

#### Canonical endpoint

`POST /api/v1/auth/login`

#### Request contract

```json
{
  "username": "user01",
  "password": "******"
}
```

`username` là credential identifier duy nhất trong phạm vi ticket này.
Wireframe label và validation phải phản ánh quyết định `username-only`, không mở rộng sang email login.

#### Success behavior

Khi credential hợp lệ và user có `enabled=true`:

- trả `200 OK`;
- tạo server-side session mới;
- trả session cookie;
- response body tối thiểu gồm:

```json
{
  "authenticated": true,
  "username": "user01",
  "roles": ["USER"]
}
```

#### Failure behavior

- credential không hợp lệ: `401 Unauthorized`;
- user `enabled=false`: không tạo session thành công và phải bị từ chối đăng nhập;
- để tránh user enumeration, invalid credential và disabled user phải dùng cùng một error contract an toàn ở phía client-visible response.

### 7.3 Error response contract

Tất cả lỗi auth trong boundary ticket này phải dùng chung JSON contract tối thiểu sau:

```json
{
  "status": 401,
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Authentication failed."
}
```

Quy tắc chung:

1. `status` phải khớp HTTP status thực tế.
2. `code` phải là machine-readable stable code cho FE/test automation.
3. `message` phải là thông điệp an toàn để hiển thị/log ở mức client, không làm lộ secret, token, session id, stack trace, hay chi tiết nội bộ.
4. Không được dùng response body khác shape giữa các endpoint auth trong cùng loại lỗi.

Mapping tối thiểu phải dùng:

| Scenario | HTTP status | code | message |
| -------- | ----------- | ---- | ------- |
| Login sai credential | `401` | `AUTH_INVALID_CREDENTIALS` | `Authentication failed.` |
| Login với user `enabled=false` | `401` | `AUTH_INVALID_CREDENTIALS` | `Authentication failed.` |
| Gọi `GET /api/v1/auth/csrf` khi không có session hoặc session hết hạn | `401` | `AUTH_SESSION_REQUIRED` | `Authentication is required or the session has expired.` |
| Gọi `POST /api/v1/auth/logout` khi không có session hoặc session hết hạn | `401` | `AUTH_SESSION_REQUIRED` | `Authentication is required or the session has expired.` |
| Mutating protected request thiếu CSRF | `403` | `AUTH_CSRF_INVALID` | `Security validation failed.` |
| Mutating protected request có CSRF sai hoặc hết hiệu lực | `403` | `AUTH_CSRF_INVALID` | `Security validation failed.` |

### 7.4 Session cookie policy

Session cookie do backend trả ra phải tuân theo các rule sau:

- `HttpOnly=true`
- `Path=/`
- `SameSite=None`
- `Secure=true` tại `staging` và `production`

Đối với `dev`, spec chốt mục tiêu hỗ trợ local integration với frontend origin `http://localhost:5173`.
Nếu local cần kiểm thử đúng hành vi cross-site cookie với `SameSite=None`, môi trường local phải được cấu hình sao cho behavior này test được, nhưng spec không ép buộc một giải pháp local HTTPS cụ thể.

### 7.5 Session timeout

Idle timeout của session phải khác nhau theo môi trường:

- `dev`: 8 giờ không hoạt động
- `staging`: 2 giờ không hoạt động
- `production`: 30 phút không hoạt động

Ticket này không bao gồm absolute timeout, remember me, hoặc persistent session recovery.

### 7.6 CORS / frontend origin baseline

Backend phải cho phép credentials-based integration cho frontend origin hợp lệ.
Trong phạm vi spec phase này, origin local phải được chốt là:

- `http://localhost:5173`

Quy tắc baseline:

- `allowCredentials=true`
- allowlist origin phải explicit, không dùng wildcard khi gửi cookie
- origin staging/prod phải được cấu hình theo môi trường triển khai thực tế ở phase environment setup sau này

### 7.7 CSRF token endpoint

#### Canonical endpoint

`GET /api/v1/auth/csrf`

#### Behavior

- chỉ trả token khi request có session hợp lệ;
- được FE gọi sau login thành công và/hoặc khi FE cần rehydrate security state trong lúc session vẫn còn hiệu lực;
- token được FE giữ ở memory state;
- FE gửi lại token trong mutating requests qua header.

#### Success response

```json
{
  "csrfToken": "<generated-token>",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

#### Failure response

- không có session hợp lệ hoặc session đã hết hạn: `401 Unauthorized`
- response body phải theo error contract ở mục 7.3 với code `AUTH_SESSION_REQUIRED`

### 7.8 Protected mutating requests

Trong boundary của ticket này, mutating protected request phải thỏa đồng thời:

1. có session hợp lệ;
2. có CSRF token hợp lệ.

Header chuẩn cho CSRF:

```http
X-CSRF-TOKEN: <token>
```

Khi lỗi:

- thiếu/không hợp lệ session: `401 Unauthorized` + `AUTH_SESSION_REQUIRED`
- thiếu hoặc sai CSRF token: `403 Forbidden` + `AUTH_CSRF_INVALID`

Ticket này không đặc tả behavior cho các business endpoint tương lai ngoài việc nêu baseline security rule ở trên.

### 7.9 Logout behavior

#### Canonical endpoint

`POST /api/v1/auth/logout`

#### Preconditions

- request có session hợp lệ;
- request có CSRF token hợp lệ.

#### Success behavior

- invalidate session phía server;
- trả response buộc cookie phiên phía client bị clear/expire;
- body thành công tối thiểu:

```json
{
  "success": true
}
```

#### Failure behavior

- không có session hợp lệ hoặc session đã hết hạn: `401 Unauthorized` + `AUTH_SESSION_REQUIRED`
- thiếu hoặc sai CSRF token: `403 Forbidden` + `AUTH_CSRF_INVALID`

### 7.10 Frontend screens and interaction states

#### Screen S-1: Login screen

Phải có tối thiểu:

- trường `username`;
- trường password;
- nút submit login;
- vùng hiển thị lỗi đăng nhập;
- tùy chọn “remember me” nếu hiển thị thì phải disabled hoặc ẩn vì out of scope.

#### Screen S-2: In-progress auth state

Sau submit thành công về mặt client-side validation, UI phải có khả năng hiển thị trạng thái đang:

1. xác thực account;
2. nhận session;
3. lấy CSRF token.

#### Screen S-3: Authenticated landing state

Sau login + lấy CSRF thành công, UI phải có khả năng hiển thị:

- user hiện tại;
- trạng thái session active;
- trạng thái CSRF loaded in memory;
- hành động logout.

#### Screen S-4: Session expired state

Khi backend trả `401` với code `AUTH_SESSION_REQUIRED`, UI phải có khả năng điều hướng về login hoặc hiển thị thông báo hết phiên và CTA đăng nhập lại.

#### Screen S-5: CSRF failure state

Khi backend trả `403` với code `AUTH_CSRF_INVALID`, UI phải có khả năng hiển thị lỗi bảo mật và gợi ý reload/login lại.

#### Screen inventory

| Screen ID | Name | Entry trigger | Primary user goal | Notes |
| --------- | ---- | ------------- | ----------------- | ----- |
| S-1 | Login screen | User chưa authenticated hoặc vừa bị chuyển về login state | Nhập `username` + password và submit đăng nhập | Remember me out of scope, chỉ được ẩn hoặc disabled |
| S-2 | In-progress auth state | FE pass client-side validation và đang gọi auth APIs | Nhìn thấy trạng thái đang xử lý, tránh submit lặp | Bao gồm login request + CSRF fetch tiếp theo |
| S-3 | Authenticated landing state | Login + lấy CSRF token thành công | Xác nhận user đã authenticated và có thể logout | Phải thể hiện session active + CSRF loaded |
| S-4 | Session expired state | Backend trả `401` với `AUTH_SESSION_REQUIRED` | Hiểu rằng phiên đã hết hạn và đăng nhập lại | Có CTA rõ ràng quay về login |
| S-5 | CSRF failure state | Backend trả `403` với `AUTH_CSRF_INVALID` | Hiểu rằng security validation thất bại và thực hiện recovery | Có hướng dẫn reload hoặc login lại |

#### Minimal ASCII wireframes

##### S-1. Login screen

```text
+----------------------------------------------------------------------------------+
| LOGO / APP NAME                                                                  |
|----------------------------------------------------------------------------------|
|                              DANG NHAP HE THONG                                  |
|                                                                                  |
| Username                                                                         |
| +--------------------------------------------------------------------------+     |
| | user01                                                                   |     |
| +--------------------------------------------------------------------------+     |
|                                                                                  |
| Password                                                                         |
| +--------------------------------------------------------------------------+     |
| | ********                                                                 |     |
| +--------------------------------------------------------------------------+     |
|                                                                                  |
| [ ] Remember me (disabled / hidden - out of scope)                               |
|                                                                                  |
| +------------------------------+                                                 |
| |         DANG NHAP            |                                                 |
| +------------------------------+                                                 |
|                                                                                  |
| Inline error area: [ Authentication failed. ]                                    |
+----------------------------------------------------------------------------------+
```

##### S-2. In-progress auth state

```text
+----------------------------------------------------------------------------------+
| LOGO / APP NAME                                                                  |
|----------------------------------------------------------------------------------|
| Dang dang nhap...                                                                |
|                                                                                  |
| [spinner] Dang xac thuc tai khoan                                                |
| [spinner] Dang nhan session                                                      |
| [spinner] Dang lay CSRF token                                                    |
|                                                                                  |
| Nguoi dung khong the submit lap khi dang xu ly                                   |
+----------------------------------------------------------------------------------+
```

##### S-3. Authenticated landing state

```text
+----------------------------------------------------------------------------------+
| APP HEADER                                                                       |
|----------------------------------------------------------------------------------|
| Xin chao, user01                                                                 |
| Session status: Active                                                           |
| CSRF state: Loaded in memory                                                     |
| Roles: USER                                                                      |
|                                                                                  |
| +------------------------------+                                                 |
| |          LOGOUT              |                                                 |
| +------------------------------+                                                 |
+----------------------------------------------------------------------------------+
```

##### S-4. Session expired state

```text
+----------------------------------------------------------------------------------+
| SESSION EXPIRED                                                                  |
|----------------------------------------------------------------------------------|
| Authentication is required or the session has expired.                           |
|                                                                                  |
| +------------------------------+                                                 |
| |      DANG NHAP LAI          |                                                 |
| +------------------------------+                                                 |
+----------------------------------------------------------------------------------+
```

##### S-5. CSRF failure state

```text
+----------------------------------------------------------------------------------+
| SECURITY VALIDATION FAILED                                                       |
|----------------------------------------------------------------------------------|
| Security validation failed.                                                      |
| Reload the page or sign in again before retrying the action.                     |
|                                                                                  |
| +------------------------------+   +------------------------------+              |
| |           RELOAD             |   |        DANG NHAP LAI         |              |
| +------------------------------+   +------------------------------+              |
+----------------------------------------------------------------------------------+
```

### 7.11 Frontend-to-backend interaction flow

1. User submit login form.
2. FE gọi `POST /api/v1/auth/login` với chế độ gửi cookie credentials.
3. Nếu login thành công, FE gọi `GET /api/v1/auth/csrf`.
4. FE lưu `csrfToken` trong memory state.
5. FE gửi `X-CSRF-TOKEN` ở các mutating requests trong boundary auth.
6. Khi gặp `401` với `AUTH_SESSION_REQUIRED`, FE phải chuyển người dùng về login state.
7. Khi gặp `403` với `AUTH_CSRF_INVALID`, FE phải hiển thị lỗi phù hợp và đề xuất reload hoặc login lại.

### 7.12 Data model baseline

Auth persistence baseline tối thiểu gồm ba bảng:

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

Spec phase này chỉ chốt **minimum table set**. Các ràng buộc chi tiết hơn như unique keys, indexing strategy, naming convention cụ thể, cascade policy phải bám common rules và executable implementation sau phase implementation plan.

### 7.13 Flyway baseline

Migration tối thiểu phải tồn tại theo intent sau:

- `V1__init_schema.sql`
- `V2__create_auth_tables.sql`
- `V3__seed_dev_test_users.sql`

Quy tắc behavior:

- migration auth phải chạy được trên database mới;
- seed chỉ được áp dụng cho `dev/test`;
- staging/prod không được nhận seed users mặc định.

### 7.14 Dev/test seed user mechanism

Để thỏa đồng thời usability và Rule 32, seed user cho `dev/test` được chốt như sau:

1. Chỉ seed trên `dev` và `test`.
2. Username seed cố định tối thiểu gồm:
   - `admin`
   - `user01`
3. Password ban đầu của seed users **không được** xuất hiện trong repo artifacts, docs, screenshots, logs, example payloads, hay migration SQL ở dạng plaintext.
4. Password ban đầu phải được cấp từ runtime secret input ngoài repo, ví dụ environment variables hoặc secret file local không commit.
5. Dữ liệu lưu vào DB chỉ là `password_hash`.
6. Nếu seeding được bật nhưng thiếu runtime secret input bắt buộc cho seed passwords, startup/migration ở `dev/test` phải fail fast bằng thông báo không chứa secret.
7. `staging` và `production` không được seed các tài khoản mặc định này.

Ticket này chỉ chốt **behavioral requirement** ở trên, không ép buộc một cơ chế framework cụ thể để materialize seed (placeholder, bootstrap runner, hay equivalent dev/test-only mechanism).

### 7.15 Springdoc / OpenAPI

Ở `dev` và `staging`, hệ thống phải expose tài liệu API auth để phục vụ dev/test/integration.
Tài liệu phải mô tả tối thiểu:

- login endpoint;
- csrf endpoint;
- logout endpoint;
- request/response mẫu;
- status codes chính;
- cách sử dụng session cookie + CSRF header.

Ở `production`:

- Swagger UI không được public;
- truy cập `/v3/api-docs` phải nhận `404 Not Found`.

### 7.16 Environment-specific behavior

| Concern | dev | staging | production |
| ------- | --- | ------- | ---------- |
| Session timeout | 8h idle | 2h idle | 30m idle |
| Allowed frontend origin | `http://localhost:5173` | Environment-specific allowlist | Environment-specific allowlist |
| Cross-site support | Required | Required | Required |
| Cookie Secure | Environment-dependent for local setup; not mandated by ticket text | Required | Required |
| Swagger UI | Enabled | Enabled | Not public |
| `/v3/api-docs` | Enabled | Enabled | `404` |
| Seed users | Allowed with external runtime secret input | Not allowed | Not allowed |
| Session store | In-memory/default session | In-memory/default session | In-memory/default session within this ticket scope |

## 8. Non-Functional Requirements

| Category | Requirement |
| -------- | ----------- |
| Security | Password phải được lưu dưới dạng strong hash; không log plaintext password, session id, CSRF token, hay seed password |
| Security | CORS phải chỉ cho phép allowlist origins hợp lệ và dùng `allowCredentials=true` khi cần gửi cookie |
| Security | Các decision về session/cookie/CORS/CSRF phải explicit trong config hoặc docs, không dựa vào framework defaults |
| Security | Client-visible error body phải an toàn, stable, machine-readable, và không lộ thông tin giúp enumeration hoặc lộ internals |
| Reliability | Session hết hạn phải dẫn tới hành vi nhất quán: backend trả `401` + `AUTH_SESSION_REQUIRED`, FE quay về login state |
| Compatibility | Auth flow phải tương thích với frontend gọi API có kèm credentials trong mô hình cross-site |
| Observability | Auth success/failure/logout/session-expired events phải có log vận hành đủ để support troubleshooting nhưng không lộ secret hoặc token |
| Documentation | Springdoc/OpenAPI phải phản ánh đúng auth flow thực tế trong dev/staging |
| Environment safety | Dev/test conveniences (seed users, Swagger) không được rò rỉ sang staging/prod |
| Testability | Mỗi AC phải map được sang ít nhất một loại verification khả thi theo testing baseline hiện có |

## 9. Acceptance Criteria

| AC | Statement | Test type |
| -- | --------- | --------- |
| AC-1 | Hệ thống phải expose `POST /api/v1/auth/login` như endpoint đăng nhập chính của ticket và chỉ chấp nhận `username` là credential identifier trong request contract. | IT / BB |
| AC-2 | Khi credential hợp lệ và user `enabled=true`, `POST /api/v1/auth/login` phải trả `200 OK`, tạo server-side session, và trả session cookie trong response. | IT / BB |
| AC-3 | Login response thành công phải có body JSON chứa `authenticated=true`, `username`, và `roles`. | IT / BB |
| AC-4 | Khi credential không hợp lệ, `POST /api/v1/auth/login` phải trả `401 Unauthorized`, không tạo session hợp lệ, và trả body JSON với `status=401`, `code=AUTH_INVALID_CREDENTIALS`, `message=Authentication failed.`. | IT / BB |
| AC-5 | Khi user `enabled=false`, `POST /api/v1/auth/login` phải từ chối đăng nhập, không tạo session hợp lệ, và trả cùng client-visible error contract như trường hợp sai credential. | IT / BB |
| AC-6 | Session cookie của auth flow phải có `HttpOnly=true`, `Path=/`, và `SameSite=None`. | IT / BB |
| AC-7 | Ở `staging` và `production`, session cookie của auth flow phải có `Secure=true`. | IT / BB |
| AC-8 | Session idle timeout phải được cấu hình theo môi trường: dev 8 giờ, staging 2 giờ, production 30 phút. | IT / BB |
| AC-9 | Backend phải cấu hình credentials-based cross-site integration cho origin local `http://localhost:5173` bằng allowlist explicit, không dùng wildcard origin khi gửi cookie. | IT / BB |
| AC-10 | Hệ thống phải expose `GET /api/v1/auth/csrf` để trả CSRF token qua response body khi session hợp lệ. | IT / BB |
| AC-11 | `GET /api/v1/auth/csrf` thành công phải trả JSON chứa `csrfToken`, `headerName`, và `parameterName`; trong đó `headerName` phải là `X-CSRF-TOKEN`. | IT / BB |
| AC-12 | Khi request không có session hợp lệ, `GET /api/v1/auth/csrf` phải trả `401 Unauthorized` với body JSON có `status=401`, `code=AUTH_SESSION_REQUIRED`, `message=Authentication is required or the session has expired.`. | IT / BB |
| AC-13 | `POST /api/v1/auth/logout` phải yêu cầu session hợp lệ và CSRF token hợp lệ. | IT / BB |
| AC-14 | Khi logout thành công, hệ thống phải invalidate session phía server, trả response buộc cookie phiên phía client bị clear hoặc expire, và trả body JSON `{ "success": true }`. | IT / BB |
| AC-15 | Khi mutating protected request thiếu CSRF token, hệ thống phải trả `403 Forbidden` với body JSON có `status=403`, `code=AUTH_CSRF_INVALID`, `message=Security validation failed.`. | IT / BB |
| AC-16 | Khi mutating protected request có CSRF token sai hoặc hết hiệu lực, hệ thống phải trả `403 Forbidden` với cùng client-visible error contract như trường hợp thiếu CSRF token. | IT / BB |
| AC-17 | Hệ thống phải lưu dữ liệu user phục vụ authentication trong PostgreSQL. | IT |
| AC-18 | Hệ thống phải tích hợp Flyway và tự động áp dụng migration auth theo cấu hình môi trường khi khởi động. | IT |
| AC-19 | Phải có migration tạo tối thiểu các bảng `users`, `roles`, `user_roles`. | IT |
| AC-20 | Cơ chế seed user chỉ được áp dụng cho `dev/test`, phải dùng password nhận từ runtime secret input ngoài repo, và không được áp dụng ở `staging/production`. | IT / BB |
| AC-21 | Khi seeding được bật ở `dev/test` nhưng thiếu runtime secret input bắt buộc cho seed passwords, startup/migration phải fail fast bằng thông báo không chứa secret. | IT |
| AC-22 | Hệ thống phải tích hợp Springdoc/OpenAPI cho auth endpoints ở `dev` và `staging`. | IT / BB |
| AC-23 | Ở `production`, Swagger UI không được public và truy cập `/v3/api-docs` phải trả `404 Not Found`. | IT / BB |
| AC-24 | Tài liệu API ở môi trường được bật phải mô tả ít nhất login, csrf, logout, request/response mẫu, và các mã trạng thái chính, bao gồm error codes đã chốt trong spec này. | BB |

## 10. Examples

### 10.1 Normal cases

#### N-1. Login thành công rồi lấy CSRF token

1. User nhập `username` và password hợp lệ.
2. FE gọi `POST /api/v1/auth/login` với credentials mode.
3. Backend trả `200 OK` + session cookie + body authenticated.
4. FE gọi `GET /api/v1/auth/csrf` với session cookie.
5. Backend trả `200 OK` + `csrfToken` + `X-CSRF-TOKEN` metadata.
6. FE lưu token vào memory state và chuyển sang authenticated state.

#### N-2. Logout thành công

1. User đang có session hợp lệ và CSRF token hợp lệ.
2. FE gọi `POST /api/v1/auth/logout` với cookie + `X-CSRF-TOKEN`.
3. Backend trả success response, invalidate session, và clear/expire cookie.
4. FE quay về login state.

### 10.2 Abnormal cases

#### A-1. Login thất bại vì sai credential

1. User nhập sai `username` hoặc password.
2. Backend trả `401 Unauthorized` với body:

```json
{
  "status": 401,
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Authentication failed."
}
```

3. Không có authenticated session usable cho request tiếp theo.
4. FE hiển thị lỗi đăng nhập.

#### A-2. Mutating request thiếu CSRF token

1. User đã có session hợp lệ.
2. FE gửi `POST /api/v1/auth/logout` nhưng thiếu `X-CSRF-TOKEN`.
3. Backend trả `403 Forbidden` với body:

```json
{
  "status": 403,
  "code": "AUTH_CSRF_INVALID",
  "message": "Security validation failed."
}
```

4. FE hiển thị lỗi bảo mật và gợi ý reload/login lại.

### 10.3 Boundary values

#### B-1. Session vừa hết hạn theo idle timeout

1. User đăng nhập thành công nhưng không hoạt động vượt quá timeout của môi trường.
2. FE gọi `GET /api/v1/auth/csrf` hoặc một request protected.
3. Backend trả `401 Unauthorized` với code `AUTH_SESSION_REQUIRED` vì session không còn hiệu lực.
4. FE chuyển người dùng về login state.

#### B-2. Production exposure boundary cho API docs

1. Ứng dụng chạy ở profile production.
2. Người dùng thử truy cập Swagger UI và `/v3/api-docs`.
3. Swagger UI không public.
4. `/v3/api-docs` trả `404 Not Found`.

## 11. Open Issues

Không còn Open Issue ở mức spec chặn implementation.

## 12. Risks

| Risk | Likelihood | Impact | Mitigation |
| ---- | ---------- | ------ | ---------- |
| Cross-site cookie behavior khác nhau giữa local và staging/prod vì HTTPS/domain setup | High | High | Giữ cookie/CORS rules explicit; kiểm thử riêng theo môi trường; không suy diễn từ local sang prod |
| FE/BE lệch nhau nếu implementation vô tình hỗ trợ email login trái với spec `username-only` | Medium | High | Kiểm thử contract rõ ở request validation, UI label và OpenAPI |
| In-memory session không phù hợp scale-out nhiều instance | Medium | Medium | Ghi rõ out of scope; không quảng bá như production-ready multi-instance solution |
| `overview.md` dựa trên executable sources đang missing nên As-Is code baseline có thể lệch thực tế repo | Medium | Medium | Không dùng docs đó để khẳng định chi tiết implementation hiện hữu; bám ticket/raw spec và spec pack này |
| Dev/test seed bootstrap có thể bị cấu hình thiếu runtime secret input | Medium | Medium | Fail fast với thông báo sanitize; cung cấp hướng dẫn setup dev/test ngoài repo artifacts |

## 13. Applicable Common Rules

- Architecture docs reviewed: [x] `docs/architecture/overview.md`
- Architecture docs reviewed: [x] `docs/architecture/key-flows.md`
- Coding rules to watch:
  - [x] Rule 10 - Preserve layer boundaries
  - [x] Rule 11 - Keep executable truth and docs aligned
  - [x] Rule 15 - Make environment-specific behavior explicit
  - [x] Rule 19 - Keep common base separate from ticket scope
- Testing rules to watch:
  - [x] Rule 20 - Every accepted change needs explicit verification
  - [x] Rule 24 - Test contracts at integration boundaries
  - [x] Rule 29 - Keep traceability from AC to evidence
- Security rules to watch:
  - [x] Rule 30 - Treat security-relevant defaults as explicit decisions
  - [x] Rule 32 - Do not store secrets or sensitive data in repo artifacts
  - [x] Rule 35 - Protect state-changing operations deliberately
  - [x] Rule 36 - Keep environment behavior safe by default
  - [x] Rule 40 - Stop and log open issues when security assumptions are unverified

---

## 14. Traceability Table

| AC | Screen | API | DB | Log / Audit | Permissions | Test type |
| -- | ------ | --- | -- | ----------- | ----------- | --------- |
| AC-1 | S-1 Login | `POST /api/v1/auth/login` | `users`, `user_roles`, `roles` | Auth request outcome log (sanitized) | Anonymous user may call login | IT / BB |
| AC-2 | S-1, S-2, S-3 | `POST /api/v1/auth/login` | `users`, `user_roles`, `roles` | Login success log without secrets | Anonymous -> authenticated session transition | IT / BB |
| AC-3 | S-3 | `POST /api/v1/auth/login` | `users`, `user_roles`, `roles` | Login success log without token/session id leakage | Same as AC-2 | IT / BB |
| AC-4 | S-1 | `POST /api/v1/auth/login` | `users` | Login failure log without secret leakage | Anonymous user may attempt login | IT / BB |
| AC-5 | S-1 | `POST /api/v1/auth/login` | `users.enabled` | Disabled-user login rejection log without user-enumeration leakage | Disabled user cannot authenticate | IT / BB |
| AC-6 | S-2 | `POST /api/v1/auth/login` response cookie | - | Security/config verification log as applicable | Authenticated session cookie policy | IT / BB |
| AC-7 | S-2 | `POST /api/v1/auth/login` response cookie | - | Security/config verification log as applicable | Environment-specific cookie hardening | IT / BB |
| AC-8 | S-4 | Session config affecting all auth endpoints | - | Session-expired operational log (sanitized) | Expired session must lose access | IT / BB |
| AC-9 | S-1, S-2 | CORS config for `http://localhost:5173` | - | CORS/config verification log as applicable | Origin allowlist for credentialed requests | IT / BB |
| AC-10 | S-2, S-3 | `GET /api/v1/auth/csrf` | Session-backed state | CSRF token issue event without token value | Authenticated session required | IT / BB |
| AC-11 | S-3 | `GET /api/v1/auth/csrf` | Session-backed state | Same as AC-10 | Authenticated session required | IT / BB |
| AC-12 | S-4 | `GET /api/v1/auth/csrf` | Session-backed state | Session-missing/session-expired event | Anonymous or expired session denied | IT / BB |
| AC-13 | S-3, S-5 | `POST /api/v1/auth/logout` | Session-backed state | Logout request security event | Authenticated session + valid CSRF required | IT / BB |
| AC-14 | S-3, S-1 | `POST /api/v1/auth/logout` | Session-backed state | Logout success event | Authenticated -> anonymous transition | IT / BB |
| AC-15 | S-5 | Mutating protected endpoint(s) in auth boundary | Session-backed state | CSRF missing rejection event | Missing CSRF denied | IT / BB |
| AC-16 | S-5 | Mutating protected endpoint(s) in auth boundary | Session-backed state | CSRF invalid rejection event | Invalid CSRF denied | IT / BB |
| AC-17 | - | Auth persistence used by login | `users`, `roles`, `user_roles` in PostgreSQL | Startup/persistence diagnostics without secrets | N/A | IT |
| AC-18 | - | App startup with Flyway | Flyway history + auth tables | Migration execution log | N/A | IT |
| AC-19 | - | App startup with Flyway | `users`, `roles`, `user_roles` | Migration execution log | N/A | IT |
| AC-20 | - | Environment-specific startup/config | Seed users only in dev/test | Seed execution log without plaintext credentials | Seed users unavailable in staging/prod | IT / BB |
| AC-21 | - | Dev/test startup/config when seed password inputs missing | Seed bootstrap path | Fail-fast startup/migration log without secrets | N/A | IT |
| AC-22 | - | `/v3/api-docs`, `/swagger-ui/index.html` in dev/staging | - | API-doc exposure operational log if applicable | Dev/staging access only | IT / BB |
| AC-23 | - | `/swagger-ui/index.html`, `/v3/api-docs` in production | - | Production hardening verification | Production must not expose Swagger UI and must return `404` for `/v3/api-docs` | IT / BB |
| AC-24 | - | OpenAPI document contents for auth endpoints | - | Documentation review artifact | Same as AC-22 | BB |
