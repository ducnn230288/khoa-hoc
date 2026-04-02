# Black-box test cases — auth

> Cập nhật: 2026-04-03
> Nguồn: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/test-plan.md`
> Mục tiêu: đặc tả kiểm thử black-box theo AC, không giả định chi tiết implementation nội bộ ngoài những contract đã được chốt trong spec.

## 1. Nguyên tắc

- Chỉ kiểm thử theo hành vi quan sát được ở API/UI/DB read-only/log vận hành khi các khía cạnh đó đã được nêu trong spec.
- Với các protected business APIs không được đặt tên trong spec, tài liệu này dùng **endpoint đại diện** (`PROTECTED-POST-01`, `PROTECTED-PATCH-01`, `PROTECTED-DELETE-01`). Trước khi chạy test, QA gán chúng với endpoint mutating thực tế có auth guard trong build đang kiểm thử.
- Không mở rộng coverage vô hạn. Mỗi AC được phân tầng ưu tiên: **P0** = chặn release hoặc rủi ro bảo mật/chức năng cốt lõi; **P1** = quan trọng nhưng có thể chạy sau smoke; **P2** = nice-to-have / regression sâu.
- Khi kiểm tra timeout dài (`8h`, `2h`, `30m`), ưu tiên cơ chế time travel, điều khiển idle hoặc môi trường thử nghiệm xác định; không dùng chờ thụ động kéo dài nếu có lựa chọn khác.

## 2. Danh mục test cases theo Acceptance Criteria

### AC-auth-1/v1 — Login thành công tạo session

**Mục tiêu**: Xác nhận login hợp lệ cho user enabled trả 200, tạo server-side session và trả session cookie.

| Case ID   | Ưu tiên | Perspective                 | Tiền điều kiện                                                                                    | Data ref                                                                | Bước kiểm thử / Input                                                  | Kết quả mong đợi                                                                                                                               |
| --------- | ------- | --------------------------- | ------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| BB-A01-01 | P0      | Normal + Permission + Audit | ENV-DEV hoặc ENV-TEST; browser/client đang anonymous.                                             | USER-SEED-USER01, REQ-LOGIN-VALID-USER01                                | Gọi POST /api/v1/auth/login từ ALLOWED-ORIGIN với credentials include. | 200 OK; body có authenticated=true, username=user01, roles là mảng; response có session cookie; có audit event login success, không lộ secret. |
| BB-A01-02 | P1      | Normal + Data variation     | ENV-NONPROD; fixture user nhiều role đã được nạp.                                                 | USER-FX-MULTIROLE, REQ-LOGIN-VALID-MULTIROLE                            | Login bằng user enabled có nhiều role.                                 | 200 OK; authenticated=true; username đúng; roles là mảng có từ 2 phần tử trở lên; session cookie được cấp.                                     |
| BB-A01-03 | P1      | Recovery / Operational      | ENV-NONPROD; trước đó đã có 1 lần login thất bại cùng browser nhưng hiện không có session hợp lệ. | USER-SEED-USER01, REQ-LOGIN-INVALID-PASSWORD rồi REQ-LOGIN-VALID-USER01 | Thực hiện 1 lần login sai, sau đó login lại đúng bằng cùng client.     | Lần 1 trả 401; lần 2 trả 200 và tạo đúng 1 session hợp lệ cho client hiện tại; không có dấu hiệu session rác từ lần thất bại.                  |

### AC-auth-2/v1 — Cookie login có HttpOnly, SameSite=None, Path=/

**Mục tiêu**: Xác nhận các thuộc tính cookie bắt buộc sau login thành công.

| Case ID   | Ưu tiên | Perspective               | Tiền điều kiện                              | Data ref                     | Bước kiểm thử / Input                                               | Kết quả mong đợi                                                                                              |
| --------- | ------- | ------------------------- | ------------------------------------------- | ---------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| BB-A02-01 | P0      | Normal / Header contract  | ENV-DEV; login hợp lệ.                      | REQ-LOGIN-VALID-USER01       | Login thành công và đọc Set-Cookie trong response.                  | Set-Cookie chứa HttpOnly=true, SameSite=None, Path=/.                                                         |
| BB-A02-02 | P0      | Environment compatibility | ENV-STAGING; login hợp lệ.                  | REQ-LOGIN-VALID-STAGING-USER | Login thành công trên staging và kiểm tra Set-Cookie.               | Set-Cookie chứa HttpOnly=true, SameSite=None, Path=/.                                                         |
| BB-A02-03 | P1      | Environment compatibility | ENV-PROD; có account hợp lệ dành cho smoke. | REQ-LOGIN-VALID-PROD-USER    | Login thành công trên prod-like environment và kiểm tra Set-Cookie. | Set-Cookie vẫn giữ HttpOnly=true, SameSite=None, Path=/; không bị profile-specific override làm sai contract. |

### AC-auth-3/v1 — Cookie login có Secure=true ở staging/prod

**Mục tiêu**: Xác nhận Secure flag tại các môi trường bắt buộc.

| Case ID   | Ưu tiên | Perspective             | Tiền điều kiện                                                       | Data ref                     | Bước kiểm thử / Input    | Kết quả mong đợi       |
| --------- | ------- | ----------------------- | -------------------------------------------------------------------- | ---------------------------- | ------------------------ | ---------------------- |
| BB-A03-01 | P0      | Normal / Staging policy | ENV-STAGING; login hợp lệ qua topology tương đương HTTPS/proxy thật. | REQ-LOGIN-VALID-STAGING-USER | Login và đọc Set-Cookie. | Cookie có Secure=true. |
| BB-A03-02 | P0      | Normal / Prod policy    | ENV-PROD; login hợp lệ qua topology public/proxy thật.               | REQ-LOGIN-VALID-PROD-USER    | Login và đọc Set-Cookie. | Cookie có Secure=true. |

### AC-auth-4/v1 — Sai username/password trả 401 AUTH_INVALID_CREDENTIALS

**Mục tiêu**: Xác nhận login sai thông tin không tạo session hợp lệ.

| Case ID   | Ưu tiên | Perspective           | Tiền điều kiện                               | Data ref                   | Bước kiểm thử / Input                                                | Kết quả mong đợi                                                                                    |
| --------- | ------- | --------------------- | -------------------------------------------- | -------------------------- | -------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| BB-A04-01 | P0      | Abnormal              | ENV-NONPROD; user01 tồn tại và enabled=true. | REQ-LOGIN-INVALID-PASSWORD | Login với username đúng nhưng password sai.                          | 401 Unauthorized; error envelope có code=AUTH_INVALID_CREDENTIALS; không có session dùng được.      |
| BB-A04-02 | P0      | Abnormal              | ENV-NONPROD; username không tồn tại.         | REQ-LOGIN-UNKNOWN-USER     | Login với username không tồn tại.                                    | 401 Unauthorized; code=AUTH_INVALID_CREDENTIALS; không tạo session hợp lệ.                          |
| BB-A04-03 | P1      | Operational follow-up | Vừa thực hiện BB-A04-01 hoặc BB-A04-02.      | COOKIE-NONE                | Ngay sau login thất bại, gọi GET /api/v1/auth/csrf bằng cùng client. | 401 Unauthorized với code=AUTH_SESSION_REQUIRED; chứng minh login fail không để lại session hợp lệ. |

### AC-auth-5/v1 — User disabled trả 401 AUTH_USER_DISABLED

**Mục tiêu**: Xác nhận chỉ user enabled=true mới login thành công.

| Case ID   | Ưu tiên | Perspective           | Tiền điều kiện                                      | Data ref                                  | Bước kiểm thử / Input                       | Kết quả mong đợi                                                                                 |
| --------- | ------- | --------------------- | --------------------------------------------------- | ----------------------------------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| BB-A05-01 | P0      | Abnormal + Permission | ENV-NONPROD; fixture user disabled đã được tạo sẵn. | USER-FX-DISABLED, REQ-LOGIN-DISABLED-USER | Login bằng user disabled với password đúng. | 401 Unauthorized; code=AUTH_USER_DISABLED; không tạo session hợp lệ.                             |
| BB-A05-02 | P1      | Operational follow-up | Vừa thực hiện login disabled user.                  | COOKIE-NONE                               | Gọi GET /api/v1/auth/csrf bằng cùng client. | 401 Unauthorized với code=AUTH_SESSION_REQUIRED; không có session residual sau login bị từ chối. |

### AC-auth-6/v1 — Idle timeout theo môi trường

**Mục tiêu**: Xác nhận session hết hiệu lực đúng theo TTL từng môi trường và bị phân loại là expired.

| Case ID   | Ưu tiên | Perspective                        | Tiền điều kiện                                                                                              | Data ref                                   | Bước kiểm thử / Input                                                                           | Kết quả mong đợi                                                                |
| --------- | ------- | ---------------------------------- | ----------------------------------------------------------------------------------------------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| BB-A06-01 | P0      | Boundary / Dev before-threshold    | ENV-DEV; đã login thành công và có session hợp lệ; có khả năng điều khiển thời gian/idle một cách xác định. | SESSION-DEV-JUST-BEFORE-8H                 | Làm session idle tới sát ngưỡng 8 giờ nhưng chưa vượt ngưỡng, sau đó gọi GET /api/v1/auth/csrf. | 200 OK; session vẫn còn hiệu lực.                                               |
| BB-A06-02 | P0      | Boundary / Dev after-threshold     | ENV-DEV; đã login thành công.                                                                               | SESSION-DEV-JUST-AFTER-8H                  | Làm session idle vượt 8 giờ rồi gọi GET /api/v1/auth/csrf.                                      | 401 Unauthorized; code=AUTH_SESSION_EXPIRED.                                    |
| BB-A06-03 | P0      | Boundary / Staging after-threshold | ENV-STAGING; đã login thành công.                                                                           | SESSION-STAGING-JUST-AFTER-2H              | Làm session idle vượt 2 giờ rồi gọi GET /api/v1/auth/csrf.                                      | 401 Unauthorized; code=AUTH_SESSION_EXPIRED.                                    |
| BB-A06-04 | P0      | Boundary / Prod after-threshold    | ENV-PROD; đã login thành công.                                                                              | SESSION-PROD-JUST-AFTER-30M                | Làm session idle vượt 30 phút rồi gọi GET /api/v1/auth/csrf.                                    | 401 Unauthorized; code=AUTH_SESSION_EXPIRED.                                    |
| BB-A06-05 | P1      | Operational continuity             | Có session đã expired ở bất kỳ môi trường nào.                                                              | SESSION-EXPIRED-ANYENV, REQ-PROTECTED-POST | Dùng session cũ gọi một protected mutating endpoint đại diện.                                   | 401 Unauthorized; lỗi được phân biệt là session expired trong UI/logs vận hành. |

### AC-auth-7/v1 — GET /auth/csrf thành công với session hợp lệ

**Mục tiêu**: Xác nhận API CSRF dùng được để cấp token cho session còn hiệu lực.

| Case ID   | Ưu tiên | Perspective                       | Tiền điều kiện                                                      | Data ref                                | Bước kiểm thử / Input                             | Kết quả mong đợi                                                                             |
| --------- | ------- | --------------------------------- | ------------------------------------------------------------------- | --------------------------------------- | ------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| BB-A07-01 | P0      | Normal                            | Đã login thành công và giữ session cookie hợp lệ.                   | COOKIE-VALID-SESSION                    | Gọi GET /api/v1/auth/csrf.                        | 200 OK; body có đủ csrfToken, headerName, parameterName; các field không rỗng.               |
| BB-A07-02 | P1      | Normal / Repeatable access        | Đã login thành công và session còn hiệu lực.                        | COOKIE-VALID-SESSION                    | Gọi GET /api/v1/auth/csrf hai lần liên tiếp.      | Cả hai lần đều 200 OK; token metadata hợp lệ; test không giả định token có rotate hay không. |
| BB-A07-03 | P1      | Operational / Rehydrate precursor | Cookie session còn hiệu lực nhưng state in-memory của FE đã bị xóa. | COOKIE-VALID-SESSION, FE-MEMORY-CLEARED | Khởi tạo lại client và gọi GET /api/v1/auth/csrf. | 200 OK; client lấy lại được CSRF token mà không cần login lại.                               |

### AC-auth-8/v1 — GET /auth/csrf không có session trả 401 AUTH_SESSION_REQUIRED

**Mục tiêu**: Xác nhận endpoint CSRF không cấp token cho anonymous/không có session.

| Case ID   | Ưu tiên | Perspective                   | Tiền điều kiện                                                        | Data ref              | Bước kiểm thử / Input      | Kết quả mong đợi                                               |
| --------- | ------- | ----------------------------- | --------------------------------------------------------------------- | --------------------- | -------------------------- | -------------------------------------------------------------- |
| BB-A08-01 | P0      | Abnormal / Anonymous          | Client chưa từng login.                                               | COOKIE-NONE           | Gọi GET /api/v1/auth/csrf. | 401 Unauthorized; code=AUTH_SESSION_REQUIRED.                  |
| BB-A08-02 | P1      | Abnormal / Cleared cookie     | Trước đó từng login nhưng cookie phía client đã bị xóa.               | COOKIE-CLEARED        | Gọi GET /api/v1/auth/csrf. | 401 Unauthorized; code=AUTH_SESSION_REQUIRED.                  |
| BB-A08-03 | P1      | Boundary / Unknown session id | Client gắn cookie session giả/ngẫu nhiên không tương ứng session nào. | COOKIE-RANDOM-UNKNOWN | Gọi GET /api/v1/auth/csrf. | 401 Unauthorized; code=AUTH_SESSION_REQUIRED; không cấp token. |

### AC-auth-9/v1 — Protected mutating request không có session trả 401 AUTH_SESSION_REQUIRED

**Mục tiêu**: Xác nhận guard session chặn mọi mutating request anonymous.

| Case ID   | Ưu tiên | Perspective           | Tiền điều kiện                                    | Data ref                                         | Bước kiểm thử / Input                             | Kết quả mong đợi                              |
| --------- | ------- | --------------------- | ------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------- | --------------------------------------------- |
| BB-A09-01 | P0      | Abnormal / POST       | Có endpoint đại diện PROTECTED-POST-01 trong SUT. | REQ-PROTECTED-POST, COOKIE-NONE                  | Gọi protected POST mà không gửi session cookie.   | 401 Unauthorized; code=AUTH_SESSION_REQUIRED. |
| BB-A09-02 | P1      | Abnormal / DELETE     | Có endpoint đại diện PROTECTED-DELETE-01.         | REQ-PROTECTED-DELETE, COOKIE-NONE                | Gọi protected DELETE mà không gửi session cookie. | 401 Unauthorized; code=AUTH_SESSION_REQUIRED. |
| BB-A09-03 | P1      | Permission regression | Client vừa logout và không còn cookie hợp lệ.     | REQ-PROTECTED-PATCH, COOKIE-CLEARED-AFTER-LOGOUT | Ngay sau logout, thử gọi protected PATCH.         | 401 Unauthorized; code=AUTH_SESSION_REQUIRED. |

### AC-auth-10/v1 — Protected mutating request thiếu X-CSRF-TOKEN trả 403 AUTH_CSRF_INVALID

**Mục tiêu**: Xác nhận có session thôi là chưa đủ cho mutating request.

| Case ID   | Ưu tiên | Perspective               | Tiền điều kiện                 | Data ref                                                    | Bước kiểm thử / Input                                                                                  | Kết quả mong đợi                       |
| --------- | ------- | ------------------------- | ------------------------------ | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | -------------------------------------- |
| BB-A10-01 | P0      | Abnormal / Missing header | Đã login và có session hợp lệ. | COOKIE-VALID-SESSION, REQ-PROTECTED-POST                    | Gọi protected POST mà không gửi X-CSRF-TOKEN.                                                          | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A10-02 | P1      | Boundary / Empty header   | Đã login và có session hợp lệ. | COOKIE-VALID-SESSION, HDR-CSRF-EMPTY, REQ-PROTECTED-PATCH   | Gọi protected PATCH với header X-CSRF-TOKEN rỗng.                                                      | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A10-03 | P1      | Contract strictness       | Đã login và có session hợp lệ. | COOKIE-VALID-SESSION, PARAM-CSRF-ONLY, REQ-PROTECTED-DELETE | Gọi protected DELETE chỉ với parameterName=\_csrf hoặc header tên sai, không có X-CSRF-TOKEN đúng tên. | 403 Forbidden; code=AUTH_CSRF_INVALID. |

### AC-auth-11/v1 — Protected mutating request có CSRF sai/hết hiệu lực trả 403 AUTH_CSRF_INVALID

**Mục tiêu**: Xác nhận token phải đúng và gắn với session đang còn hiệu lực.

| Case ID   | Ưu tiên | Perspective                       | Tiền điều kiện                                              | Data ref                                                           | Bước kiểm thử / Input                                              | Kết quả mong đợi                       |
| --------- | ------- | --------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------ | -------------------------------------- |
| BB-A11-01 | P0      | Abnormal / Random token           | Đã login và có session hợp lệ.                              | COOKIE-VALID-SESSION, HDR-CSRF-RANDOM, REQ-PROTECTED-POST          | Gọi protected POST với X-CSRF-TOKEN ngẫu nhiên.                    | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A11-02 | P1      | Boundary / Token from old session | Client A login lấy token; logout hoặc tạo session mới khác. | COOKIE-NEW-SESSION, HDR-CSRF-FROM-OLD-SESSION, REQ-PROTECTED-PATCH | Dùng token lấy từ session cũ để gọi protected PATCH ở session mới. | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A11-03 | P1      | Cross-session misuse              | Có hai session hợp lệ khác nhau ở hai client khác nhau.     | COOKIE-SESSION-A, HDR-CSRF-FROM-SESSION-B, REQ-PROTECTED-DELETE    | Dùng token của session B cho request mang cookie session A.        | 403 Forbidden; code=AUTH_CSRF_INVALID. |

### AC-auth-12/v1 — Protected mutating request có session + CSRF hợp lệ được phép đi qua auth guard

**Mục tiêu**: Xác nhận auth/CSRF guard không chặn sai các request hợp lệ.

| Case ID   | Ưu tiên | Perspective     | Tiền điều kiện                                               | Data ref                                                   | Bước kiểm thử / Input                               | Kết quả mong đợi                                                                                                                                  |
| --------- | ------- | --------------- | ------------------------------------------------------------ | ---------------------------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| BB-A12-01 | P0      | Normal / POST   | Đã login và lấy được CSRF token hợp lệ.                      | COOKIE-VALID-SESSION, HDR-CSRF-VALID, REQ-PROTECTED-POST   | Gọi protected POST với session + X-CSRF-TOKEN đúng. | Request đi qua lớp auth/CSRF guard; kết quả cuối không phải 401/403 do auth/CSRF. Phản hồi business có thể là 2xx hoặc lỗi nghiệp vụ ngoài scope. |
| BB-A12-02 | P1      | Normal / PATCH  | Đã login và lấy được CSRF token hợp lệ.                      | COOKIE-VALID-SESSION, HDR-CSRF-VALID, REQ-PROTECTED-PATCH  | Gọi protected PATCH hợp lệ.                         | Không bị chặn bởi auth/CSRF guard.                                                                                                                |
| BB-A12-03 | P1      | Normal / DELETE | Đã login và lấy được CSRF token hợp lệ cho session hiện tại. | COOKIE-VALID-SESSION, HDR-CSRF-VALID, REQ-PROTECTED-DELETE | Gọi protected DELETE hợp lệ.                        | Không bị chặn bởi auth/CSRF guard; nếu thất bại chỉ vì business rule thì lỗi phải khác 401/403 auth/CSRF.                                         |

### AC-auth-13/v1 — Logout thành công hủy session hiện tại và clear/expire cookie

**Mục tiêu**: Xác nhận logout chỉ hủy session hiện tại, trả success và session cũ không còn dùng được.

| Case ID   | Ưu tiên | Perspective           | Tiền điều kiện                                                 | Data ref                                          | Bước kiểm thử / Input                                               | Kết quả mong đợi                                                                                      |
| --------- | ------- | --------------------- | -------------------------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| BB-A13-01 | P0      | Normal + Audit        | Đã login thành công và lấy CSRF token hợp lệ.                  | COOKIE-VALID-SESSION, HDR-CSRF-VALID, REQ-LOGOUT  | Gọi POST /api/v1/auth/logout.                                       | 200 OK; body tối thiểu {success:true}; response clear/expire session cookie; có audit logout success. |
| BB-A13-02 | P0      | Operational follow-up | Vừa logout thành công bằng client hiện tại.                    | COOKIE-OLD-SESSION                                | Gọi GET /api/v1/auth/csrf bằng cookie cũ/old client state.          | 401 Unauthorized; session cũ không còn hiệu lực.                                                      |
| BB-A13-03 | P1      | Permission isolation  | Hai tab/browser cùng chia sẻ session trước khi logout ở tab A. | COOKIE-SHARED-SESSION, HDR-CSRF-VALID, REQ-LOGOUT | Logout ở tab A rồi thử protected action ở tab B mà không login lại. | Tab B không thể tiếp tục dùng session cũ; protected request bị từ chối do không còn session hợp lệ.   |

### AC-auth-14/v1 — Logout không có session trả 401 AUTH_SESSION_REQUIRED

**Mục tiêu**: Xác nhận logout cũng là protected endpoint.

| Case ID   | Ưu tiên | Perspective                   | Tiền điều kiện                              | Data ref                                                  | Bước kiểm thử / Input                                           | Kết quả mong đợi                                                                            |
| --------- | ------- | ----------------------------- | ------------------------------------------- | --------------------------------------------------------- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| BB-A14-01 | P0      | Abnormal / Anonymous          | Client chưa login.                          | COOKIE-NONE, REQ-LOGOUT                                   | Gọi POST /api/v1/auth/logout.                                   | 401 Unauthorized; code=AUTH_SESSION_REQUIRED.                                               |
| BB-A14-02 | P1      | Abnormal / Cleared cookie     | Trước đó từng login nhưng cookie đã bị xóa. | COOKIE-CLEARED, REQ-LOGOUT                                | Gọi logout sau khi cookie đã bị xóa khỏi browser.               | 401 Unauthorized; code=AUTH_SESSION_REQUIRED.                                               |
| BB-A14-03 | P1      | Boundary / Unknown session id | Client gắn cookie session giả.              | COOKIE-RANDOM-UNKNOWN, HDR-CSRF-VALID-LOOKING, REQ-LOGOUT | Gọi logout với random session cookie và một header CSRF bất kỳ. | 401 Unauthorized; kiểm tra session được ưu tiên xác thực trước, code=AUTH_SESSION_REQUIRED. |

### AC-auth-15/v1 — Logout thiếu/sai CSRF trả 403 AUTH_CSRF_INVALID

**Mục tiêu**: Xác nhận logout tuân thủ cùng rule CSRF như protected mutating endpoint.

| Case ID   | Ưu tiên | Perspective               | Tiền điều kiện                    | Data ref                                              | Bước kiểm thử / Input                                   | Kết quả mong đợi                       |
| --------- | ------- | ------------------------- | --------------------------------- | ----------------------------------------------------- | ------------------------------------------------------- | -------------------------------------- |
| BB-A15-01 | P0      | Abnormal / Missing header | Đã login và session còn hiệu lực. | COOKIE-VALID-SESSION, REQ-LOGOUT                      | Gọi logout không gửi X-CSRF-TOKEN.                      | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A15-02 | P1      | Boundary / Empty header   | Đã login và session còn hiệu lực. | COOKIE-VALID-SESSION, HDR-CSRF-EMPTY, REQ-LOGOUT      | Gọi logout với X-CSRF-TOKEN rỗng.                       | 403 Forbidden; code=AUTH_CSRF_INVALID. |
| BB-A15-03 | P1      | Cross-session misuse      | Có 2 session khác nhau.           | COOKIE-SESSION-A, HDR-CSRF-FROM-SESSION-B, REQ-LOGOUT | Gọi logout của session A nhưng dùng token từ session B. | 403 Forbidden; code=AUTH_CSRF_INVALID. |

### AC-auth-16/v1 — OpenAPI/Swagger public theo môi trường

**Mục tiêu**: Xác nhận tài liệu API chỉ public ở dev/staging và không public ở prod.

| Case ID   | Ưu tiên | Perspective                    | Tiền điều kiện                                   | Data ref          | Bước kiểm thử / Input                                 | Kết quả mong đợi                                                                                              |
| --------- | ------- | ------------------------------ | ------------------------------------------------ | ----------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| BB-A16-01 | P0      | Environment / Dev exposure     | ENV-DEV; app chạy sẵn.                           | DOCS-DEV-URLS     | Truy cập /v3/api-docs và Swagger UI từ client ngoài.  | Tài liệu truy cập được; có mô tả các endpoint auth trong scope ticket.                                        |
| BB-A16-02 | P0      | Environment / Staging exposure | ENV-STAGING; app chạy sẵn.                       | DOCS-STAGING-URLS | Truy cập /v3/api-docs và Swagger UI.                  | Tài liệu truy cập được trên staging.                                                                          |
| BB-A16-03 | P0      | Environment / Prod restriction | ENV-PROD; client không có kênh đặc quyền nội bộ. | DOCS-PROD-URLS    | Truy cập Swagger UI và OpenAPI endpoint từ bên ngoài. | Tài liệu không public; request không truy cập được (ví dụ 401/403/404 tùy topology).                          |
| BB-A16-04 | P1      | Contract content smoke         | ENV-DEV hoặc ENV-STAGING.                        | DOCS-OPENAPI-AUTH | Mở tài liệu auth và rà contract mức ngoài.            | Có request/response mẫu, status code chính, thông tin session cookie + CSRF header; không mâu thuẫn với spec. |

### AC-auth-17/v1 — Auth data lưu trong PostgreSQL với các bảng tối thiểu

**Mục tiêu**: Xác nhận auth foundation thực sự dùng PostgreSQL và có schema tối thiểu theo spec.

| Case ID   | Ưu tiên | Perspective                        | Tiền điều kiện                                               | Data ref                                 | Bước kiểm thử / Input                                                    | Kết quả mong đợi                                                                  |
| --------- | ------- | ---------------------------------- | ------------------------------------------------------------ | ---------------------------------------- | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------- |
| BB-A17-01 | P0      | Operational / DB existence         | Có quyền read-only vào DB của môi trường kiểm thử.           | DB-READONLY-CHECK-TABLES                 | Kiểm tra existence của tables users, roles, user_roles trong PostgreSQL. | Cả ba bảng tồn tại.                                                               |
| BB-A17-02 | P0      | Normal / DB-backed login           | ENV-NONPROD; một user hợp lệ đã tồn tại trong DB PostgreSQL. | USER-SEED-USER01, REQ-LOGIN-VALID-USER01 | Login bằng user lưu trong DB.                                            | Login thành công; chứng minh auth data được truy xuất từ PostgreSQL-backed store. |
| BB-A17-03 | P1      | Operational / Fixture provisioning | Có quy trình nạp fixture qua DB hoặc migration chuẩn.        | USER-FX-DB-PROVISIONED                   | Nạp một user test mới qua đường dữ liệu chuẩn rồi login bằng user đó.    | Login thành công; dữ liệu auth mới provision được hệ thống nhìn thấy.             |

### AC-auth-18/v1 — Flyway khởi tạo schema auth trên DB trống

**Mục tiêu**: Xác nhận startup trên DB trống tự chạy migration mà không cần thao tác tay.

| Case ID   | Ưu tiên | Perspective                             | Tiền điều kiện                                                    | Data ref                   | Bước kiểm thử / Input                                            | Kết quả mong đợi                                                                                     |
| --------- | ------- | --------------------------------------- | ----------------------------------------------------------------- | -------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| BB-A18-01 | P0      | Normal / First boot                     | Khởi tạo môi trường với PostgreSQL trống hoàn toàn.               | DB-EMPTY-START             | Khởi động ứng dụng theo quy trình chuẩn của môi trường.          | Ứng dụng lên thành công; auth endpoints usable; không cần chạy SQL thủ công ngoài flow chuẩn.        |
| BB-A18-02 | P1      | Boundary / Restart after migrated       | DB đã được migrate bởi BB-A18-01.                                 | DB-ALREADY-MIGRATED        | Khởi động lại ứng dụng lần 2.                                    | Ứng dụng lên bình thường; không cần repair/manual step để auth usable.                               |
| BB-A18-03 | P1      | Abnormal / Migration failure visibility | Môi trường negative test kiểm soát được quyền DB hoặc script lỗi. | DB-MIGRATION-FAIL-SCENARIO | Khởi động ứng dụng trong điều kiện migration không thể hoàn tất. | Hệ thống không được báo trạng thái auth-ready giả; dấu hiệu readiness/startup phải phản ánh failure. |

### AC-auth-19/v1 — Seed user tồn tại ở dev/test và password đã hash

**Mục tiêu**: Xác nhận seed non-prod được nạp đúng ở dev/test.

| Case ID   | Ưu tiên | Perspective                             | Tiền điều kiện                       | Data ref                          | Bước kiểm thử / Input                            | Kết quả mong đợi                                                                               |
| --------- | ------- | --------------------------------------- | ------------------------------------ | --------------------------------- | ------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| BB-A19-01 | P0      | Normal / Dev seed                       | ENV-DEV mới khởi động với DB trống.  | USER-SEED-ADMIN, USER-SEED-USER01 | Login bằng admin/Admin@123 và user01/User@123.   | Cả hai tài khoản seed đăng nhập được trên dev.                                                 |
| BB-A19-02 | P0      | Normal / Test seed                      | ENV-TEST mới khởi động với DB trống. | USER-SEED-ADMIN, USER-SEED-USER01 | Login bằng hai tài khoản seed trên test profile. | Cả hai tài khoản seed đăng nhập được trên test.                                                |
| BB-A19-03 | P1      | Operational / Password hashing evidence | Có quyền read-only DB trên dev/test. | DB-CHECK-SEEDED-PASSWORD-HASH     | Đọc giá trị password lưu cho tài khoản seed.     | Giá trị lưu không trùng plaintext Admin@123/User@123; biểu hiện là hash, không phải plaintext. |

### AC-auth-20/v1 — Staging/prod không nạp seed user và non-prod Flyway location

**Mục tiêu**: Xác nhận seed non-prod bị cô lập khỏi staging/prod.

| Case ID   | Ưu tiên | Perspective                            | Tiền điều kiện                                                        | Data ref                                       | Bước kiểm thử / Input                                                   | Kết quả mong đợi                                                                                                                |
| --------- | ------- | -------------------------------------- | --------------------------------------------------------------------- | ---------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| BB-A20-01 | P0      | Environment isolation / Staging        | ENV-STAGING mới deploy từ DB trống hoặc DB sạch theo quy trình chuẩn. | REQ-LOGIN-SEED-ADMIN-STAGING, DB-CHECK-NO-SEED | Thử login bằng seed credentials mặc định và/hoặc kiểm tra DB read-only. | Seed user mặc định không khả dụng; không quan sát thấy record seed mặc định được tạo do non-prod migration.                     |
| BB-A20-02 | P0      | Environment isolation / Prod           | ENV-PROD smoke/clone với quyền kiểm thử phù hợp.                      | REQ-LOGIN-SEED-ADMIN-PROD, DB-CHECK-NO-SEED    | Thử login bằng seed credentials mặc định và/hoặc kiểm tra DB read-only. | Seed user mặc định không khả dụng.                                                                                              |
| BB-A20-03 | P1      | Operational / Flyway location evidence | Có quyền xem startup log/config manifest ở staging/prod.              | OPS-CHECK-FLYWAY-LOCATION                      | Kiểm tra evidence vận hành khi app khởi động.                           | Không có dấu hiệu nạp non-prod Flyway location; nếu cấu hình lỡ nạp non-prod location thì deployment bị xem là fail acceptance. |

### AC-auth-21/v1 — Session store chỉ là in-memory/session mặc định

**Mục tiêu**: Xác nhận auth flow không phụ thuộc Redis hay persistent session store.

| Case ID   | Ưu tiên | Perspective                                  | Tiền điều kiện                                                                      | Data ref                              | Bước kiểm thử / Input                                                                              | Kết quả mong đợi                                                             |
| --------- | ------- | -------------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| BB-A21-01 | P0      | Operational / No external session dependency | Môi trường kiểm thử chỉ provision app + PostgreSQL, không có Redis/session service. | OPS-NO-REDIS                          | Khởi động app và chạy login -> csrf -> protected flow -> logout.                                   | Flow auth hoàn tất bình thường dù không có Redis/persistent session service. |
| BB-A21-02 | P1      | Boundary / Restart invalidates session       | Đã login thành công và còn giữ session cookie; có thể recycle app process.          | COOKIE-VALID-SESSION, OPS-APP-RESTART | Khởi động lại app rồi thử dùng cookie cũ gọi GET /api/v1/auth/csrf.                                | Session cũ không còn dùng được; client phải login lại.                       |
| BB-A21-03 | P2      | Operational / Same-process continuity        | App chưa restart; session vừa được tạo.                                             | COOKIE-VALID-SESSION                  | Trong cùng vòng đời process, gọi csrf rồi protected action sau một khoảng idle ngắn còn trong TTL. | Session vẫn dùng được mà không cần hạ tầng session ngoài tiến trình.         |

### AC-auth-22/v1 — Audit log tối thiểu và không lộ secret

**Mục tiêu**: Xác nhận các sự kiện auth quan trọng đều có audit log phân biệt được nguyên nhân nhưng đã redacted.

| Case ID   | Ưu tiên | Perspective                 | Tiền điều kiện                                      | Data ref                                     | Bước kiểm thử / Input                                                | Kết quả mong đợi                                                                             |
| --------- | ------- | --------------------------- | --------------------------------------------------- | -------------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| BB-A22-01 | P0      | Audit / Login success       | Có quyền xem audit logs tương ứng request vừa chạy. | REQ-LOGIN-VALID-USER01, LOG-AUDIT-ACCESS     | Thực hiện login thành công rồi tra log theo correlation/time window. | Có audit event login success; không lộ plaintext password, full session id, full CSRF token. |
| BB-A22-02 | P0      | Audit / Invalid credentials | Có quyền xem audit logs.                            | REQ-LOGIN-INVALID-PASSWORD, LOG-AUDIT-ACCESS | Thực hiện login sai password rồi tra log.                            | Có audit event login failure, phân biệt được invalid credentials; không lộ secret.           |
| BB-A22-03 | P0      | Audit / Logout success      | Đã login và có CSRF hợp lệ.                         | REQ-LOGOUT, LOG-AUDIT-ACCESS                 | Logout thành công rồi tra log.                                       | Có audit event logout success; không lộ session/token đầy đủ.                                |
| BB-A22-04 | P1      | Audit / Session required    | Client anonymous gọi protected endpoint.            | REQ-PROTECTED-POST, LOG-AUDIT-ACCESS         | Gọi protected POST không có session rồi tra log.                     | Có audit event unauthenticated/session required access denied.                               |
| BB-A22-05 | P1      | Audit / Session expired     | Có session đã hết hạn.                              | SESSION-EXPIRED-ANYENV, LOG-AUDIT-ACCESS     | Gọi GET /api/v1/auth/csrf hoặc protected request rồi tra log.        | Có audit event phân biệt session expired, không gộp chung với session required.              |
| BB-A22-06 | P1      | Audit / CSRF invalid        | Đã login, có session hợp lệ nhưng gửi CSRF sai.     | HDR-CSRF-RANDOM, LOG-AUDIT-ACCESS            | Gọi protected mutating endpoint với token sai rồi tra log.           | Có audit event CSRF denied; log đã redacted.                                                 |

### AC-auth-23/v1 — Mọi lỗi 401/403 dùng error envelope thống nhất

**Mục tiêu**: Xác nhận cấu trúc lỗi tối thiểu nhất quán giữa các luồng auth.

| Case ID   | Ưu tiên | Perspective                        | Tiền điều kiện                                         | Data ref                                                  | Bước kiểm thử / Input            | Kết quả mong đợi                                                                |
| --------- | ------- | ---------------------------------- | ------------------------------------------------------ | --------------------------------------------------------- | -------------------------------- | ------------------------------------------------------------------------------- |
| BB-A23-01 | P0      | Contract / 401 invalid credentials | ENV-NONPROD.                                           | REQ-LOGIN-INVALID-PASSWORD                                | Login sai password.              | 401 JSON chứa đủ code, message, path, timestamp; code=AUTH_INVALID_CREDENTIALS. |
| BB-A23-02 | P0      | Contract / 401 no session          | Client anonymous.                                      | COOKIE-NONE                                               | Gọi GET /api/v1/auth/csrf.       | 401 JSON chứa đủ code, message, path, timestamp; code=AUTH_SESSION_REQUIRED.    |
| BB-A23-03 | P0      | Contract / 403 csrf invalid        | Đã login và có session hợp lệ nhưng không có/CSRF sai. | COOKIE-VALID-SESSION, REQ-PROTECTED-POST, HDR-CSRF-RANDOM | Gọi protected POST với CSRF sai. | 403 JSON chứa đủ code, message, path, timestamp; code=AUTH_CSRF_INVALID.        |
| BB-A23-04 | P1      | Contract / Path accuracy           | Đã login và có session hợp lệ.                         | COOKIE-VALID-SESSION, REQ-LOGOUT, HDR-CSRF-RANDOM         | Gọi logout với CSRF sai.         | Response 403 vẫn giữ đủ envelope; path phản ánh đúng endpoint đã gọi.           |

### AC-auth-24/v1 — Login success response luôn có roles là mảng

**Mục tiêu**: Xác nhận roles không bị null/omit.

| Case ID   | Ưu tiên | Perspective                 | Tiền điều kiện                                | Data ref                                     | Bước kiểm thử / Input                            | Kết quả mong đợi                                                          |
| --------- | ------- | --------------------------- | --------------------------------------------- | -------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------------- |
| BB-A24-01 | P0      | Normal / User has role      | ENV-NONPROD; user enabled có ít nhất 1 role.  | USER-SEED-USER01, REQ-LOGIN-VALID-USER01     | Login bằng user có role.                         | 200 OK; field roles tồn tại và có kiểu mảng.                              |
| BB-A24-02 | P0      | Boundary / Roleless user    | ENV-NONPROD; fixture roleless user tồn tại.   | USER-FX-ROLELESS, REQ-LOGIN-VALID-ROLELESS   | Login bằng user enabled nhưng không có role nào. | 200 OK; roles có giá trị [] ; không null; không bị bỏ field.              |
| BB-A24-03 | P1      | Data variation / Multi-role | ENV-NONPROD; fixture multi-role user tồn tại. | USER-FX-MULTIROLE, REQ-LOGIN-VALID-MULTIROLE | Login bằng user nhiều role.                      | roles vẫn là mảng; test không áp đặt thứ tự role nếu spec không quy định. |

### AC-auth-25/v1 — UI/API/OpenAPI chỉ dùng Username, không có email login

**Mục tiêu**: Xác nhận contract public không gợi ý hay chấp nhận email login.

| Case ID   | Ưu tiên | Perspective            | Tiền điều kiện                                        | Data ref               | Bước kiểm thử / Input                                     | Kết quả mong đợi                                                                |
| --------- | ------- | ---------------------- | ----------------------------------------------------- | ---------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------- |
| BB-A25-01 | P0      | UI contract            | Mở màn hình login mới.                                | UI-LOGIN-SCREEN        | Quan sát label, placeholder, helper text trên form login. | Chỉ xuất hiện Username và Password; không có field Email riêng.                 |
| BB-A25-02 | P0      | API contract           | Có khả năng capture request từ FE hoặc gọi trực tiếp. | REQ-LOGIN-VALID-USER01 | Quan sát request body mà FE gửi khi login.                | Request body chỉ có username và password; không có email field.                 |
| BB-A25-03 | P1      | OpenAPI contract       | ENV-DEV hoặc ENV-STAGING có Swagger/OpenAPI public.   | DOCS-OPENAPI-AUTH      | Mở schema/request sample của login endpoint.              | Chỉ mô tả username/password; không có email login.                              |
| BB-A25-04 | P2      | UX wording consistency | Đã gây ra lỗi login trên UI.                          | UI-LOGIN-ERROR-STATE   | Quan sát thông điệp lỗi và nội dung hướng dẫn.            | UI không hướng người dùng sang email login; wording vẫn bám khái niệm Username. |

### AC-auth-26/v1 — Reload app với session còn hiệu lực thì rehydrate qua GET /auth/csrf

**Mục tiêu**: Xác nhận client không cần auth-status endpoint riêng.

| Case ID   | Ưu tiên | Perspective                                 | Tiền điều kiện                                                                       | Data ref                                                           | Bước kiểm thử / Input                                    | Kết quả mong đợi                                                                                                                         |
| --------- | ------- | ------------------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------ | -------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| BB-A26-01 | P0      | Normal / Reload                             | Đã login thành công; session cookie còn hiệu lực; state in-memory bị mất khi reload. | COOKIE-VALID-SESSION, FE-MEMORY-CLEARED                            | Reload toàn bộ app rồi quan sát network.                 | Client gọi GET /api/v1/auth/csrf và nhận 200 OK; UI quay về authenticated state.                                                         |
| BB-A26-02 | P0      | Normal / Continue protected flow            | Vừa rehydrate thành công sau reload.                                                 | COOKIE-VALID-SESSION, HDR-CSRF-VALID-REFETCHED, REQ-PROTECTED-POST | Thực hiện tiếp một protected mutating action sau reload. | Action đi qua auth/CSRF guard mà không cần login lại.                                                                                    |
| BB-A26-03 | P1      | Contract / No separate auth-status endpoint | Đã login và chuẩn bị reload app; có thể theo dõi network call của FE.                | NET-TRACE-RELOAD                                                   | Reload app và xem các call auth state initialization.    | Không cần endpoint auth-status riêng như /auth/me, /auth/session, /auth/status; cơ chế rehydrate quan sát được là GET /api/v1/auth/csrf. |

### AC-auth-27/v1 — CORS credentialed request cho phép origin http://localhost:5173 và không wildcard

**Mục tiêu**: Xác nhận contract cross-site credentialed flow đúng origin và không dùng \*.

| Case ID   | Ưu tiên | Perspective                        | Tiền điều kiện                                                   | Data ref                               | Bước kiểm thử / Input                                                                             | Kết quả mong đợi                                                                                                                     |
| --------- | ------- | ---------------------------------- | ---------------------------------------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| BB-A27-01 | P0      | CORS / Preflight allowed origin    | Backend đang chạy; có công cụ gửi preflight từ origin được phép. | ORIGIN-ALLOWED, CORS-PREFLIGHT-AUTH    | Gửi OPTIONS preflight cho auth/protected endpoint từ http://localhost:5173 với headers cần thiết. | Response cho phép credentialed flow; Access-Control-Allow-Origin phản hồi đúng origin được phép; có allow headers/methods cần thiết. |
| BB-A27-02 | P0      | CORS / Actual credentialed request | Có request login thực tế từ FE chạy tại http://localhost:5173.   | ORIGIN-ALLOWED, REQ-LOGIN-VALID-USER01 | Thực hiện login thật với credentials include và quan sát response headers.                        | Flow thành công; ACAO là http://localhost:5173, Access-Control-Allow-Credentials=true; không dùng wildcard \*.                       |
| BB-A27-03 | P1      | CORS / Disallowed origin           | Có thể gửi request từ một origin ngoài phạm vi ticket.           | ORIGIN-DISALLOWED, CORS-PREFLIGHT-AUTH | Gửi preflight/actual request từ origin không được phép.                                           | Origin đó không được hỗ trợ; browser/client không nhận được contract CORS cho credentialed flow.                                     |
| BB-A27-04 | P1      | Security regression                | Có response từ allowed origin và ít nhất một negative sample.    | CORS-RESPONSE-CAPTURE                  | Rà headers của response CORS ở flow credentialed.                                                 | Không xuất hiện Access-Control-Allow-Origin=\* trong cùng flow credentialed.                                                         |

## 3. Traceability — Mapping AC ↔ black-box cases

| AC            | Mô tả ngắn                                                                        | Black-box cases                                                  |
| ------------- | --------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| AC-auth-1/v1  | Login thành công tạo session                                                      | BB-A01-01, BB-A01-02, BB-A01-03                                  |
| AC-auth-2/v1  | Cookie login có HttpOnly, SameSite=None, Path=/                                   | BB-A02-01, BB-A02-02, BB-A02-03                                  |
| AC-auth-3/v1  | Cookie login có Secure=true ở staging/prod                                        | BB-A03-01, BB-A03-02                                             |
| AC-auth-4/v1  | Sai username/password trả 401 AUTH_INVALID_CREDENTIALS                            | BB-A04-01, BB-A04-02, BB-A04-03                                  |
| AC-auth-5/v1  | User disabled trả 401 AUTH_USER_DISABLED                                          | BB-A05-01, BB-A05-02                                             |
| AC-auth-6/v1  | Idle timeout theo môi trường                                                      | BB-A06-01, BB-A06-02, BB-A06-03, BB-A06-04, BB-A06-05            |
| AC-auth-7/v1  | GET /auth/csrf thành công với session hợp lệ                                      | BB-A07-01, BB-A07-02, BB-A07-03                                  |
| AC-auth-8/v1  | GET /auth/csrf không có session trả 401 AUTH_SESSION_REQUIRED                     | BB-A08-01, BB-A08-02, BB-A08-03                                  |
| AC-auth-9/v1  | Protected mutating request không có session trả 401 AUTH_SESSION_REQUIRED         | BB-A09-01, BB-A09-02, BB-A09-03                                  |
| AC-auth-10/v1 | Protected mutating request thiếu X-CSRF-TOKEN trả 403 AUTH_CSRF_INVALID           | BB-A10-01, BB-A10-02, BB-A10-03                                  |
| AC-auth-11/v1 | Protected mutating request có CSRF sai/hết hiệu lực trả 403 AUTH_CSRF_INVALID     | BB-A11-01, BB-A11-02, BB-A11-03                                  |
| AC-auth-12/v1 | Protected mutating request có session + CSRF hợp lệ được phép đi qua auth guard   | BB-A12-01, BB-A12-02, BB-A12-03                                  |
| AC-auth-13/v1 | Logout thành công hủy session hiện tại và clear/expire cookie                     | BB-A13-01, BB-A13-02, BB-A13-03                                  |
| AC-auth-14/v1 | Logout không có session trả 401 AUTH_SESSION_REQUIRED                             | BB-A14-01, BB-A14-02, BB-A14-03                                  |
| AC-auth-15/v1 | Logout thiếu/sai CSRF trả 403 AUTH_CSRF_INVALID                                   | BB-A15-01, BB-A15-02, BB-A15-03                                  |
| AC-auth-16/v1 | OpenAPI/Swagger public theo môi trường                                            | BB-A16-01, BB-A16-02, BB-A16-03, BB-A16-04                       |
| AC-auth-17/v1 | Auth data lưu trong PostgreSQL với các bảng tối thiểu                             | BB-A17-01, BB-A17-02, BB-A17-03                                  |
| AC-auth-18/v1 | Flyway khởi tạo schema auth trên DB trống                                         | BB-A18-01, BB-A18-02, BB-A18-03                                  |
| AC-auth-19/v1 | Seed user tồn tại ở dev/test và password đã hash                                  | BB-A19-01, BB-A19-02, BB-A19-03                                  |
| AC-auth-20/v1 | Staging/prod không nạp seed user và non-prod Flyway location                      | BB-A20-01, BB-A20-02, BB-A20-03                                  |
| AC-auth-21/v1 | Session store chỉ là in-memory/session mặc định                                   | BB-A21-01, BB-A21-02, BB-A21-03                                  |
| AC-auth-22/v1 | Audit log tối thiểu và không lộ secret                                            | BB-A22-01, BB-A22-02, BB-A22-03, BB-A22-04, BB-A22-05, BB-A22-06 |
| AC-auth-23/v1 | Mọi lỗi 401/403 dùng error envelope thống nhất                                    | BB-A23-01, BB-A23-02, BB-A23-03, BB-A23-04                       |
| AC-auth-24/v1 | Login success response luôn có roles là mảng                                      | BB-A24-01, BB-A24-02, BB-A24-03                                  |
| AC-auth-25/v1 | UI/API/OpenAPI chỉ dùng Username, không có email login                            | BB-A25-01, BB-A25-02, BB-A25-03, BB-A25-04                       |
| AC-auth-26/v1 | Reload app với session còn hiệu lực thì rehydrate qua GET /auth/csrf              | BB-A26-01, BB-A26-02, BB-A26-03                                  |
| AC-auth-27/v1 | CORS credentialed request cho phép origin http://localhost:5173 và không wildcard | BB-A27-01, BB-A27-02, BB-A27-03, BB-A27-04                       |
