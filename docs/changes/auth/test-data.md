# Test data — auth

> Cập nhật: 2026-04-03
> Dùng chung cho: `blackbox-testcases.md`

## 1. Quy ước

- Tài liệu này chỉ định **test data cần có** để chạy bộ black-box; không ngụ ý thiết kế bảng, lớp hay seed mechanism ngoài những gì spec đã chốt.
- Ký hiệu `FX` = fixture do môi trường test chuẩn bị; `SEED` = dữ liệu được spec yêu cầu; `REQ` = request payload; `EXP` = expected result catalogue.
- Với protected business APIs chưa được spec đặt tên, dùng alias và bind vào endpoint thực tế trước khi thực thi.

## 2. Precondition data

### 2.1. Environment / topology

| Data ID           | Loại              | Giá trị / mô tả                                                                                                             | Dùng cho case tiêu biểu                               |
| ----------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| ENV-DEV           | Environment       | Profile `dev`; session idle timeout mục tiêu 8 giờ; OpenAPI/Swagger public; allowed origin `http://localhost:5173`.         | BB-A02-01, BB-A06-01, BB-A16-01, BB-A19-01            |
| ENV-TEST          | Environment       | Profile `test`; có seed non-prod; dùng cho xác nhận seed user và flow phi production.                                       | BB-A19-02                                             |
| ENV-NONPROD       | Environment alias | Alias dùng cho môi trường phi production nơi có thể chuẩn bị fixture (`dev`, `test` hoặc môi trường QA tương đương).        | BB-A01-02, BB-A04-01, BB-A05-01, BB-A24-01            |
| ENV-STAGING       | Environment       | Profile `staging`; session idle timeout mục tiêu 2 giờ; `Secure=true`; OpenAPI/Swagger public; không có seed non-prod.      | BB-A02-02, BB-A03-01, BB-A06-03, BB-A16-02, BB-A20-01 |
| ENV-PROD          | Environment       | Profile `prod`; session idle timeout mục tiêu 30 phút; `Secure=true`; Swagger/OpenAPI không public; không có seed non-prod. | BB-A02-03, BB-A03-02, BB-A06-04, BB-A16-03, BB-A20-02 |
| ALLOWED-ORIGIN    | Origin            | `http://localhost:5173`                                                                                                     | BB-A01-01, BB-A27-01, BB-A27-02                       |
| ORIGIN-ALLOWED    | Origin alias      | Alias cùng giá trị với `ALLOWED-ORIGIN`, dùng riêng trong các case CORS.                                                    | BB-A27-01, BB-A27-02                                  |
| ORIGIN-DISALLOWED | Origin            | Một origin ngoài phạm vi ticket, ví dụ `http://localhost:3000` hoặc `https://evil.example`                                  | BB-A27-03                                             |

### 2.2. User / permission fixtures

| Data ID                | Loại         | Giá trị / mô tả                                                                                                                                        | Dùng cho case tiêu biểu                    |
| ---------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------ |
| USER-SEED-ADMIN        | Seed user    | `username=admin`, `password=Admin@123`; chỉ bắt buộc có ở `dev/test`. Role cụ thể không bị giả định ngoài việc login phải hợp lệ nếu seed đã được nạp. | BB-A19-01, BB-A19-02                       |
| USER-SEED-USER01       | Seed user    | `username=user01`, `password=User@123`; chỉ bắt buộc có ở `dev/test`; là tài khoản smoke mặc định cho login success.                                   | BB-A01-01, BB-A02-01, BB-A04-01, BB-A19-01 |
| USER-FX-DISABLED       | Fixture user | User dành cho test với `enabled=false` và password hợp lệ đã biết trước.                                                                               | BB-A05-01                                  |
| USER-FX-ROLELESS       | Fixture user | User dành cho test với `enabled=true`, password hợp lệ, không có role nào được gán.                                                                    | BB-A24-02                                  |
| USER-FX-MULTIROLE      | Fixture user | User dành cho test với `enabled=true`, password hợp lệ, có từ 2 role trở lên.                                                                          | BB-A01-02, BB-A24-03                       |
| USER-FX-DB-PROVISIONED | Fixture user | User được nạp qua DB/migration chuẩn để xác nhận app đọc auth data từ PostgreSQL.                                                                      | BB-A17-03                                  |

### 2.3. Endpoint aliases / session-state data

| Data ID                             | Loại           | Giá trị / mô tả                                                                           | Dùng cho case tiêu biểu                    |
| ----------------------------------- | -------------- | ----------------------------------------------------------------------------------------- | ------------------------------------------ |
| PROTECTED-POST-01                   | Endpoint alias | Một protected `POST` thực tế có auth+CSRF guard trong build đang test.                    | BB-A09-01, BB-A10-01, BB-A11-01, BB-A12-01 |
| PROTECTED-PATCH-01                  | Endpoint alias | Một protected `PATCH` thực tế có auth+CSRF guard.                                         | BB-A09-03, BB-A10-02, BB-A11-02, BB-A12-02 |
| PROTECTED-DELETE-01                 | Endpoint alias | Một protected `DELETE` thực tế có auth+CSRF guard.                                        | BB-A09-02, BB-A10-03, BB-A11-03, BB-A12-03 |
| COOKIE-NONE                         | Session state  | Không gửi session cookie.                                                                 | BB-A08-01, BB-A09-01, BB-A14-01            |
| COOKIE-VALID-SESSION                | Session state  | Session cookie vừa được tạo từ login thành công và còn trong TTL.                         | BB-A07-01, BB-A10-01, BB-A12-01            |
| COOKIE-CLEARED                      | Session state  | Cookie đã bị xóa khỏi client sau khi từng có login.                                       | BB-A08-02, BB-A14-02                       |
| COOKIE-CLEARED-AFTER-LOGOUT         | Session state  | Client không còn cookie sau logout success.                                               | BB-A09-03                                  |
| COOKIE-RANDOM-UNKNOWN               | Session state  | Cookie session giả/ngẫu nhiên không ánh xạ tới session hợp lệ nào.                        | BB-A08-03, BB-A14-03                       |
| COOKIE-OLD-SESSION                  | Session state  | Cookie cũ giữ lại sau logout để xác minh session đã bị vô hiệu hóa ở server.              | BB-A13-02                                  |
| COOKIE-NEW-SESSION                  | Session state  | Session mới được tạo sau lần login mới/re-login, khác với session đã cấp token cũ.        | BB-A11-02                                  |
| COOKIE-SHARED-SESSION               | Session state  | Hai tab/browser cùng chia sẻ một session trước khi logout.                                | BB-A13-03                                  |
| COOKIE-SESSION-A / COOKIE-SESSION-B | Session state  | Hai session độc lập để kiểm tra CSRF cross-session misuse.                                | BB-A11-03, BB-A15-03                       |
| FE-MEMORY-CLEARED                   | Client state   | State/auth snapshot in-memory đã mất do reload/app restart phía browser nhưng cookie còn. | BB-A07-03, BB-A26-01                       |

### 2.4. Boundary/time data

| Data ID                       | Mô tả                                                | Giá trị mong muốn | Dùng cho case tiêu biểu |
| ----------------------------- | ---------------------------------------------------- | ----------------- | ----------------------- |
| SESSION-DEV-JUST-BEFORE-8H    | Session idle ngay trước ngưỡng hết hạn ở dev         | < 8 giờ idle      | BB-A06-01               |
| SESSION-DEV-JUST-AFTER-8H     | Session idle vừa vượt ngưỡng ở dev                   | > 8 giờ idle      | BB-A06-02               |
| SESSION-STAGING-JUST-AFTER-2H | Session idle vừa vượt ngưỡng ở staging               | > 2 giờ idle      | BB-A06-03               |
| SESSION-PROD-JUST-AFTER-30M   | Session idle vừa vượt ngưỡng ở prod                  | > 30 phút idle    | BB-A06-04               |
| SESSION-EXPIRED-ANYENV        | Session bất kỳ đã expired theo TTL profile hiện hành | Expired           | BB-A06-05, BB-A22-05    |

### 2.5. Request / header data

| Data ID                      | Loại             | Payload / giá trị                                                        | Ghi chú                                                           |
| ---------------------------- | ---------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| REQ-LOGIN-VALID-USER01       | Request body     | `{"username":"user01","password":"User@123"}`                            | Dùng ở dev/test hoặc non-prod đã nạp fixture tương đương.         |
| REQ-LOGIN-VALID-MULTIROLE    | Request body     | `{"username":"qa_multi_role","password":"QaMulti@123"}`                  | Tên user cụ thể có thể thay đổi, miễn fixture giữ đúng tính chất. |
| REQ-LOGIN-DISABLED-USER      | Request body     | `{"username":"qa_disabled","password":"QaDisabled@123"}`                 | Fixture disabled user.                                            |
| REQ-LOGIN-VALID-ROLELESS     | Request body     | `{"username":"qa_roleless","password":"QaRoleless@123"}`                 | Fixture roleless user.                                            |
| REQ-LOGIN-INVALID-PASSWORD   | Request body     | `{"username":"user01","password":"WrongPass!"}`                          | Sai password của user hợp lệ.                                     |
| REQ-LOGIN-UNKNOWN-USER       | Request body     | `{"username":"unknown_user","password":"Whatever@123"}`                  | User không tồn tại.                                               |
| REQ-LOGIN-VALID-STAGING-USER | Request body     | Request login hợp lệ bằng tài khoản được provision hợp lệ trên staging.  | Không giả định seed user tồn tại.                                 |
| REQ-LOGIN-VALID-PROD-USER    | Request body     | Request login hợp lệ bằng tài khoản smoke được cấp phép trên prod/clone. | Chỉ dùng ở môi trường cho phép.                                   |
| REQ-LOGIN-SEED-ADMIN-STAGING | Request body     | `{"username":"admin","password":"Admin@123"}`                            | Dùng để chứng minh seed mặc định không nên hoạt động ở staging.   |
| REQ-LOGIN-SEED-ADMIN-PROD    | Request body     | `{"username":"admin","password":"Admin@123"}`                            | Dùng để chứng minh seed mặc định không nên hoạt động ở prod.      |
| REQ-LOGOUT                   | Request          | `POST /api/v1/auth/logout`                                               | Protected endpoint yêu cầu session + CSRF.                        |
| REQ-PROTECTED-POST           | Request          | Mutating request tới `PROTECTED-POST-01`                                 | Bind endpoint thật trước khi chạy.                                |
| REQ-PROTECTED-PATCH          | Request          | Mutating request tới `PROTECTED-PATCH-01`                                | Bind endpoint thật trước khi chạy.                                |
| REQ-PROTECTED-DELETE         | Request          | Mutating request tới `PROTECTED-DELETE-01`                               | Bind endpoint thật trước khi chạy.                                |
| HDR-CSRF-VALID               | Header           | `X-CSRF-TOKEN: <token lấy từ GET /api/v1/auth/csrf của cùng session>`    | Header hợp lệ.                                                    |
| HDR-CSRF-VALID-REFETCHED     | Header           | `X-CSRF-TOKEN: <token lấy lại sau reload/rehydrate>`                     | Dùng cho AC26.                                                    |
| HDR-CSRF-EMPTY               | Header           | `X-CSRF-TOKEN:` (rỗng)                                                   | Boundary dữ liệu header.                                          |
| HDR-CSRF-RANDOM              | Header           | `X-CSRF-TOKEN: random-invalid-token`                                     | Negative test.                                                    |
| HDR-CSRF-FROM-OLD-SESSION    | Header           | Token hợp lệ ở session cũ nhưng không còn thuộc session hiện tại.        | Cross-session / stale token.                                      |
| HDR-CSRF-FROM-SESSION-B      | Header           | Token được lấy từ session B.                                             | Dùng với cookie của session A.                                    |
| HDR-CSRF-VALID-LOOKING       | Header           | Một chuỗi token-looking nhưng không gắn với session hợp lệ.              | Dùng ở negative cases.                                            |
| PARAM-CSRF-ONLY              | Request modifier | Gửi `_csrf=<value>` hoặc header tên khác thay cho `X-CSRF-TOKEN`.        | Dùng để xác nhận header contract là authoritative.                |

### 2.6. Operational evidence data

| Data ID                       | Mô tả                    | Ghi chú                                                                                                                         | Dùng cho case tiêu biểu |
| ----------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| DB-READONLY-CHECK-TABLES      | Read-only DB observation | Cho phép QA xác nhận existence của `users`, `roles`, `user_roles`.                                                              | BB-A17-01               |
| DB-CHECK-SEEDED-PASSWORD-HASH | Read-only DB observation | Cho phép QA xem giá trị password lưu cho seed users mà không đổi dữ liệu.                                                       | BB-A19-03               |
| DB-CHECK-NO-SEED              | Read-only DB observation | Cho phép QA xác nhận staging/prod không có seed users mặc định.                                                                 | BB-A20-01, BB-A20-02    |
| DB-EMPTY-START                | Deployment precondition  | DB trống hoàn toàn trước lần khởi động đầu tiên.                                                                                | BB-A18-01               |
| DB-ALREADY-MIGRATED           | Deployment precondition  | DB đã chạy migration thành công trước đó.                                                                                       | BB-A18-02               |
| DB-MIGRATION-FAIL-SCENARIO    | Negative environment     | Môi trường kiểm soát được failure của migration (ví dụ quyền DB sai) để kiểm tra readiness/error handling.                      | BB-A18-03               |
| OPS-CHECK-FLYWAY-LOCATION     | Operational evidence     | Xem startup log/config manifest để xác nhận không nạp non-prod Flyway location ở staging/prod.                                  | BB-A20-03               |
| OPS-NO-REDIS                  | Operational topology     | Không provision Redis hoặc persistent session store.                                                                            | BB-A21-01               |
| OPS-APP-RESTART               | Operational step         | Recycle/restart tiến trình app giữa lúc client vẫn giữ session cookie cũ.                                                       | BB-A21-02               |
| LOG-AUDIT-ACCESS              | Operational evidence     | Quyền truy cập audit log theo correlation-id, timestamp hoặc request window.                                                    | BB-A22-01 tới BB-A22-06 |
| DOCS-DEV-URLS                 | Docs endpoint            | `/v3/api-docs`, `/swagger-ui/**` trên dev.                                                                                      | BB-A16-01               |
| DOCS-STAGING-URLS             | Docs endpoint            | `/v3/api-docs`, `/swagger-ui/**` trên staging.                                                                                  | BB-A16-02               |
| DOCS-PROD-URLS                | Docs endpoint            | Swagger/OpenAPI endpoint trên prod để xác nhận không public.                                                                    | BB-A16-03               |
| DOCS-OPENAPI-AUTH             | Docs content             | Mô tả endpoint login/csrf/logout trong OpenAPI/Swagger.                                                                         | BB-A16-04, BB-A25-03    |
| CORS-PREFLIGHT-AUTH           | HTTP request             | `OPTIONS` preflight với `Origin`, `Access-Control-Request-Method`, `Access-Control-Request-Headers: content-type,x-csrf-token`. | BB-A27-01, BB-A27-03    |
| CORS-RESPONSE-CAPTURE         | HTTP observation         | Capture headers của actual response ở allowed origin.                                                                           | BB-A27-04               |
| NET-TRACE-RELOAD              | Browser/network trace    | Quan sát các auth-related calls khi app reload.                                                                                 | BB-A26-03               |
| UI-LOGIN-SCREEN               | UI observation           | Quan sát form login public.                                                                                                     | BB-A25-01               |
| UI-LOGIN-ERROR-STATE          | UI observation           | Quan sát wording sau khi cố ý gây lỗi login.                                                                                    | BB-A25-04               |

## 3. Expected results catalogue

### 3.1. API / screen results

| Expected ID                  | Mô tả mong đợi                                                                                                                      | Dùng cho case tiêu biểu         |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- |
| EXP-LOGIN-200                | `POST /api/v1/auth/login` trả `200 OK`; body có `authenticated=true`, `username`, `roles` (mảng); có session cookie trong response. | BB-A01-01, BB-A24-01            |
| EXP-CSRF-200                 | `GET /api/v1/auth/csrf` trả `200 OK`; body có `csrfToken`, `headerName`, `parameterName`.                                           | BB-A07-01, BB-A26-01            |
| EXP-LOGOUT-200               | `POST /api/v1/auth/logout` trả `200 OK`; body tối thiểu `{success:true}`; response clear/expire session cookie.                     | BB-A13-01                       |
| EXP-ERR-401-INVALID-CREDS    | `401 Unauthorized`; envelope đủ `code,message,path,timestamp`; `code=AUTH_INVALID_CREDENTIALS`.                                     | BB-A04-01, BB-A23-01            |
| EXP-ERR-401-USER-DISABLED    | `401 Unauthorized`; envelope đủ `code,message,path,timestamp`; `code=AUTH_USER_DISABLED`.                                           | BB-A05-01                       |
| EXP-ERR-401-SESSION-REQUIRED | `401 Unauthorized`; envelope đủ `code,message,path,timestamp`; `code=AUTH_SESSION_REQUIRED`.                                        | BB-A08-01, BB-A09-01, BB-A14-01 |
| EXP-ERR-401-SESSION-EXPIRED  | `401 Unauthorized`; envelope đủ `code,message,path,timestamp`; `code=AUTH_SESSION_EXPIRED`.                                         | BB-A06-02, BB-A06-03, BB-A06-04 |
| EXP-ERR-403-CSRF-INVALID     | `403 Forbidden`; envelope đủ `code,message,path,timestamp`; `code=AUTH_CSRF_INVALID`.                                               | BB-A10-01, BB-A11-01, BB-A15-01 |
| EXP-UI-USERNAME-ONLY         | UI login chỉ hiển thị `Username` và `Password`; không có `Email`.                                                                   | BB-A25-01                       |
| EXP-REHYDRATE-OK             | Sau reload, app lấy lại auth state bằng `GET /api/v1/auth/csrf` khi cookie còn hiệu lực.                                            | BB-A26-01, BB-A26-02            |

### 3.2. Cookie / CORS expectations

| Expected ID          | Mô tả mong đợi                                                                                                        | Dùng cho case tiêu biểu |
| -------------------- | --------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| EXP-COOKIE-BASE      | Login success response có session cookie với `HttpOnly=true`, `SameSite=None`, `Path=/`.                              | BB-A02-01 tới BB-A02-03 |
| EXP-COOKIE-SECURE    | Ở `staging/prod`, session cookie có `Secure=true`.                                                                    | BB-A03-01, BB-A03-02    |
| EXP-COOKIE-CLEARED   | Logout success clear/expire session cookie để client không tiếp tục dùng session hiện tại.                            | BB-A13-01               |
| EXP-CORS-ALLOWED     | Allowed origin nhận `Access-Control-Allow-Origin: http://localhost:5173` và `Access-Control-Allow-Credentials: true`. | BB-A27-01, BB-A27-02    |
| EXP-CORS-NO-WILDCARD | Không có `Access-Control-Allow-Origin: *` trong credentialed flow.                                                    | BB-A27-02, BB-A27-04    |
| EXP-CORS-DISALLOWED  | Origin ngoài phạm vi không nhận contract CORS cho credentialed flow.                                                  | BB-A27-03               |

### 3.3. DB / log / operational expectations

| Expected ID                 | Mô tả mong đợi                                                                     | Dùng cho case tiêu biểu |
| --------------------------- | ---------------------------------------------------------------------------------- | ----------------------- |
| EXP-DB-TABLES-EXIST         | PostgreSQL có tối thiểu các bảng `users`, `roles`, `user_roles`.                   | BB-A17-01               |
| EXP-DB-SEED-PRESENT-NONPROD | Ở `dev/test`, seed users `admin`, `user01` tồn tại và login được.                  | BB-A19-01, BB-A19-02    |
| EXP-DB-SEED-ABSENT-PRODLIKE | Ở `staging/prod`, không có seed users mặc định do non-prod migration.              | BB-A20-01, BB-A20-02    |
| EXP-DB-PASSWORD-HASHED      | Password seed được lưu dưới dạng hash; không bằng plaintext.                       | BB-A19-03               |
| EXP-MIGRATION-AUTOBOOT      | Khởi động trên DB trống tự chạy migration và auth usable mà không cần SQL tay.     | BB-A18-01               |
| EXP-NO-FALSE-READY          | Khi migration/DB lỗi, hệ thống không được báo auth-ready giả.                      | BB-A18-03               |
| EXP-AUDIT-REDACTED          | Các event audit bắt buộc xuất hiện và đã redacted secret/identifier nhạy cảm.      | BB-A22-01 tới BB-A22-06 |
| EXP-NO-REDIS-DEPENDENCY     | Không cần Redis/persistent session store để hoàn tất auth flow trong scope ticket. | BB-A21-01               |

## 4. Mapping data sets ↔ case clusters

| Nhóm case                       | Data tối thiểu phải chuẩn bị                                                                                      |
| ------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Login / logout / csrf core flow | ENV-DEV hoặc ENV-TEST, USER-SEED-USER01, COOKIE-VALID-SESSION, HDR-CSRF-VALID                                     |
| Negative login                  | USER-SEED-USER01, REQ-LOGIN-INVALID-PASSWORD, REQ-LOGIN-UNKNOWN-USER, USER-FX-DISABLED                            |
| Roles contract                  | USER-FX-ROLELESS, USER-FX-MULTIROLE                                                                               |
| Timeout / session lifecycle     | SESSION-DEV-JUST-BEFORE-8H, SESSION-DEV-JUST-AFTER-8H, SESSION-STAGING-JUST-AFTER-2H, SESSION-PROD-JUST-AFTER-30M |
| CORS / cookie                   | ALLOWED-ORIGIN, ORIGIN-DISALLOWED, CORS-PREFLIGHT-AUTH                                                            |
| DB / migration / seed / ops     | DB-EMPTY-START, DB-READONLY-CHECK-TABLES, DB-CHECK-SEEDED-PASSWORD-HASH, OPS-CHECK-FLYWAY-LOCATION, OPS-NO-REDIS  |
| Audit / observability           | LOG-AUDIT-ACCESS, request correlation hoặc timestamp window                                                       |
