# Review Checklist — ticket-1 (Authentication foundation with session cookie + CSRF + PostgreSQL/Flyway)

> Date: 2026-03-31
> Severity: **Blocker** = must fix before merge | **Major** = fix in this PR | **Minor** = fix in this PR if feasible, otherwise log clearly in self-review / follow-up
> Sources: `docs/changes/ticket-1/spec-pack.md`, `docs/changes/ticket-1/impl-plan.md`, `docs/standards/templates/review-checklist.template.md`

---

## 1. Specification / AC

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-01 | Chỉ implement các hành vi nằm trong `spec-pack.md`; không mở rộng scope sang JWT, refresh token, remember-me, MFA, social login, `/me`, `/session`, Redis/distributed session, RBAC nhiều tầng | Blocker | [ ] |
| RC-02 | `POST /api/v1/auth/login` nhận đúng contract `username/password`; không thêm login identifier khác như `email` trong phase này | Blocker | [ ] |
| RC-03 | Login success trả đúng contract tối thiểu: `200`, tạo server-side session, body có `authenticated=true` và `username` | Blocker | [ ] |
| RC-04 | Login failure cho sai credential hoặc `enabled=false` trả cùng schema lỗi chuẩn `401 AUTH_INVALID_CREDENTIALS`; không tạo session hợp lệ | Blocker | [ ] |
| RC-05 | `GET /api/v1/auth/csrf` là cơ chế cấp CSRF riêng và cũng là bootstrap auth state; không dồn CSRF vào login response | Blocker | [ ] |
| RC-06 | `POST /api/v1/auth/logout` bám đúng behavior matrix: valid session + valid CSRF => `200`; stale/no session => vẫn `200`; valid session + missing/invalid CSRF => `403` và không invalidate session | Blocker | [ ] |
| RC-07 | Tất cả `401/403` trong auth flow dùng cùng standard error response schema (`timestamp/status/code/message/path`) và đúng mã lỗi theo spec | Blocker | [ ] |
| RC-08 | FE contract bám đúng spec: label input là **Username**, không phụ thuộc cookie name, không phụ thuộc field `roles`, bootstrap lại bằng `GET /api/v1/auth/csrf` sau reload | Major | [ ] |

## 2. Design / Dependencies

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-09 | Thiết kế giữ mô hình stateful session nhất quán end-to-end; không trộn thêm JWT hoặc cơ chế auth thứ hai trong cùng flow | Blocker | [ ] |
| RC-10 | Layer boundaries rõ ràng: controller/filter/handler chỉ lo HTTP/security wiring; auth logic, mapping lỗi, persistence, config profile không bị trộn lẫn bừa bãi | Major | [ ] |
| RC-11 | Dependency và config mới là tối thiểu và phù hợp: Spring Security/session, Springdoc, PostgreSQL, Flyway; không thêm thư viện/session store ngoài spec | Major | [ ] |
| RC-12 | Profile-based configuration tách rõ `dev`, `staging`, `production` cho timeout, cookie secure, CORS allowlist, OpenAPI toggle, datasource/seed behavior | Blocker | [ ] |
| RC-13 | Không tạo production business endpoint mới chỉ để “demo” AC 17-21; nếu cần chứng minh rule protected mutating request thì dùng test-scoped endpoint trong test | Major | [ ] |
| RC-14 | Thiết kế migration/data model khớp scope auth foundation: `users`, `roles`, `user_roles`, versioned Flyway migrations, seed tách riêng theo môi trường | Blocker | [ ] |

## 3. Security

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-15 | Password luôn được lưu/seed ở dạng hash mạnh (`password_hash`), không lưu plaintext ở DB, migration, config, fixture, response, log | Blocker | [ ] |
| RC-16 | Session cookie policy đúng spec: `HttpOnly=true`, `SameSite=None`, `Path=/`, host-only cookie (không set `Domain`) | Blocker | [ ] |
| RC-17 | Ở `staging` và `production`, session cookie có `Secure=true`; khác biệt local/dev được kiểm soát bằng profile thay vì hard-code | Blocker | [ ] |
| RC-18 | Mutating protected requests bắt buộc có session hợp lệ và CSRF hợp lệ; header chuẩn FE gửi là `X-CSRF-TOKEN` | Blocker | [ ] |
| RC-19 | CORS dùng explicit origin allowlist theo môi trường, bật credentials đúng cách, và không dùng wildcard origin (`*`) khi credentials được bật | Blocker | [ ] |
| RC-20 | Không log password, session id, csrf token, cookie value, connection secret hoặc dữ liệu nhạy cảm khác; response lỗi cũng không làm lộ chi tiết nội bộ | Blocker | [ ] |
| RC-21 | User `enabled=false` không thể đăng nhập; behavior và log không làm lộ thông tin giúp enumeration user | Blocker | [ ] |

## 4. Performance

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-22 | Login / lấy CSRF / logout không thực hiện round-trip hoặc xử lý thừa ngoài những thành phần nằm trong scope backend hiện tại | Major | [ ] |
| RC-23 | Không sinh N+1 query hoặc join/lazy loading không cần thiết cho auth data đơn giản; không kéo role/payload thừa nếu contract không yêu cầu | Major | [ ] |
| RC-24 | Startup migration/seed không làm chậm đáng kể hoặc gây side effect không kiểm soát; seed chỉ chạy ở `dev/test` | Major | [ ] |

## 5. Compatibility

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-25 | Public API/JSON contract tương thích với spec: request/response field names, error codes, cookie/CSRF notes, không đổi shape ngoài tài liệu | Blocker | [ ] |
| RC-26 | FE/backend cross-site flow tương thích với browser-managed cookie + `credentials: 'include'`; FE không hard-code cookie name | Blocker | [ ] |
| RC-27 | OpenAPI/Swagger bật ở `dev` và `staging`, tắt hoàn toàn ở `production`; tài liệu phản ánh đúng login/csrf/logout contract thực tế | Major | [ ] |
| RC-28 | Fresh database migrate tạo đủ bảng tối thiểu; seed chỉ tồn tại ở `dev/test`, không rò sang `staging/production` | Blocker | [ ] |

## 6. Logs / Audit

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-29 | Có log mức tối thiểu cho: login success/failure, session rejection, CSRF rejection, logout success/idempotent outcome, migration outcome, seed applied/skipped | Major | [ ] |
| RC-30 | Log đủ để phân biệt `401` do session với `403` do CSRF và đủ để vận hành/debug mà không chứa dữ liệu nhạy cảm | Major | [ ] |

## 7. Error Handling

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-31 | Mapping lỗi HTTP đúng spec: missing/expired session => `401 AUTH_SESSION_REQUIRED`; missing CSRF => `403 AUTH_CSRF_REQUIRED`; invalid/expired CSRF => `403 AUTH_CSRF_INVALID` | Blocker | [ ] |
| RC-32 | Logout idempotent chỉ áp dụng cho stale/no session; không được nới lỏng CSRF cho trường hợp session còn hợp lệ | Blocker | [ ] |
| RC-33 | Client không nhận stack trace, exception class name, SQL error, config secret hoặc internal diagnostics trong response | Major | [ ] |

## 8. Testing

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-34 | Backend integration tests bao phủ login/cookie/csrf/logout/error-schema/CORS/docs/seed/profile behavior và test-scoped protected mutating endpoint | Blocker | [ ] |
| RC-35 | FE/unit/integration/manual flow bao phủ tối thiểu: login -> csrf bootstrap -> protected action -> reload bootstrap -> logout, cùng các state session-expired và CSRF error | Major | [ ] |
| RC-36 | Có kiểm thử profile-based cho `dev`, `staging`, `production` đối với timeout, cookie secure, docs toggle, allowlist, seed behavior | Major | [ ] |

## 9. Operations

| RC# | Check item | Severity | Status |
| --- | --- | --- | --- |
| RC-37 | Quy trình rollback rõ ràng cho FE, backend và database/Flyway; nếu migration không rollback được thì có forward-fix/restore plan | Major | [ ] |
| RC-38 | Giá trị triển khai theo môi trường (origins, datasource, secrets, timeout, docs toggle) có chỗ cấu hình rõ ràng trong pipeline/runtime config | Major | [ ] |
| RC-39 | Tài liệu review/self-review ghi rõ known risks, phần chưa xử lý, issue còn lại và các check manual cần chạy sau implementation | Minor | [ ] |

---

## AC Mapping (each check validates which ACs)

| RC# | AC được xác nhận |
| --- | --- |
| RC-01 | AC-auth-session-csrf-1/v1, AC-auth-session-csrf-13/v1, AC-auth-session-csrf-16/v1, AC-auth-session-csrf-17/v1, AC-auth-session-csrf-22/v1 |
| RC-02 | AC-auth-session-csrf-1/v1 |
| RC-03 | AC-auth-session-csrf-2/v1, AC-auth-session-csrf-3/v1 |
| RC-04 | AC-auth-session-csrf-5/v1, AC-auth-session-csrf-6/v1 |
| RC-05 | AC-auth-session-csrf-13/v1, AC-auth-session-csrf-14/v1, AC-auth-session-csrf-16/v1 |
| RC-06 | AC-auth-session-csrf-22/v1, AC-auth-session-csrf-23/v1, AC-auth-session-csrf-24/v1, AC-auth-session-csrf-25/v1 |
| RC-07 | AC-auth-session-csrf-5/v1, AC-auth-session-csrf-15/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1, AC-auth-session-csrf-34/v1 |
| RC-08 | AC-auth-session-csrf-4/v1, AC-auth-session-csrf-16/v1 |
| RC-09 | AC-auth-session-csrf-2/v1, AC-auth-session-csrf-13/v1, AC-auth-session-csrf-17/v1, AC-auth-session-csrf-23/v1 |
| RC-10 | AC-auth-session-csrf-3/v1, AC-auth-session-csrf-5/v1, AC-auth-session-csrf-15/v1, AC-auth-session-csrf-23/v1 |
| RC-11 | AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-30/v1 |
| RC-12 | AC-auth-session-csrf-10/v1, AC-auth-session-csrf-12/v1, AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-31/v1, AC-auth-session-csrf-32/v1 |
| RC-13 | AC-auth-session-csrf-17/v1, AC-auth-session-csrf-18/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1 |
| RC-14 | AC-auth-session-csrf-29/v1, AC-auth-session-csrf-30/v1, AC-auth-session-csrf-31/v1 |
| RC-15 | AC-auth-session-csrf-29/v1, AC-auth-session-csrf-31/v1 |
| RC-16 | AC-auth-session-csrf-7/v1, AC-auth-session-csrf-8/v1, AC-auth-session-csrf-9/v1, AC-auth-session-csrf-11/v1 |
| RC-17 | AC-auth-session-csrf-10/v1 |
| RC-18 | AC-auth-session-csrf-17/v1, AC-auth-session-csrf-18/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1, AC-auth-session-csrf-23/v1, AC-auth-session-csrf-25/v1 |
| RC-19 | AC-auth-session-csrf-32/v1 |
| RC-20 | AC-auth-session-csrf-33/v1, AC-auth-session-csrf-34/v1 |
| RC-21 | AC-auth-session-csrf-6/v1 |
| RC-22 | AC-auth-session-csrf-2/v1, AC-auth-session-csrf-13/v1, AC-auth-session-csrf-23/v1 |
| RC-23 | AC-auth-session-csrf-3/v1, AC-auth-session-csrf-29/v1 |
| RC-24 | AC-auth-session-csrf-30/v1, AC-auth-session-csrf-31/v1 |
| RC-25 | AC-auth-session-csrf-1/v1, AC-auth-session-csrf-3/v1, AC-auth-session-csrf-14/v1, AC-auth-session-csrf-22/v1, AC-auth-session-csrf-23/v1, AC-auth-session-csrf-34/v1 |
| RC-26 | AC-auth-session-csrf-2/v1, AC-auth-session-csrf-4/v1, AC-auth-session-csrf-16/v1, AC-auth-session-csrf-32/v1 |
| RC-27 | AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-28/v1 |
| RC-28 | AC-auth-session-csrf-29/v1, AC-auth-session-csrf-30/v1, AC-auth-session-csrf-31/v1 |
| RC-29 | AC-auth-session-csrf-33/v1 |
| RC-30 | AC-auth-session-csrf-33/v1, AC-auth-session-csrf-34/v1 |
| RC-31 | AC-auth-session-csrf-15/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1, AC-auth-session-csrf-25/v1, AC-auth-session-csrf-34/v1 |
| RC-32 | AC-auth-session-csrf-24/v1, AC-auth-session-csrf-25/v1 |
| RC-33 | AC-auth-session-csrf-34/v1 |
| RC-34 | AC-auth-session-csrf-1/v1, AC-auth-session-csrf-2/v1, AC-auth-session-csrf-5/v1, AC-auth-session-csrf-7/v1, AC-auth-session-csrf-10/v1, AC-auth-session-csrf-13/v1, AC-auth-session-csrf-15/v1, AC-auth-session-csrf-17/v1, AC-auth-session-csrf-18/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1, AC-auth-session-csrf-22/v1, AC-auth-session-csrf-23/v1, AC-auth-session-csrf-24/v1, AC-auth-session-csrf-25/v1, AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-29/v1, AC-auth-session-csrf-30/v1, AC-auth-session-csrf-31/v1, AC-auth-session-csrf-32/v1, AC-auth-session-csrf-33/v1, AC-auth-session-csrf-34/v1 |
| RC-35 | AC-auth-session-csrf-4/v1, AC-auth-session-csrf-16/v1, AC-auth-session-csrf-19/v1, AC-auth-session-csrf-20/v1, AC-auth-session-csrf-21/v1, AC-auth-session-csrf-23/v1, AC-auth-session-csrf-24/v1 |
| RC-36 | AC-auth-session-csrf-10/v1, AC-auth-session-csrf-12/v1, AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-31/v1, AC-auth-session-csrf-32/v1 |
| RC-37 | AC-auth-session-csrf-30/v1, AC-auth-session-csrf-31/v1 |
| RC-38 | AC-auth-session-csrf-10/v1, AC-auth-session-csrf-12/v1, AC-auth-session-csrf-26/v1, AC-auth-session-csrf-27/v1, AC-auth-session-csrf-31/v1, AC-auth-session-csrf-32/v1 |
| RC-39 | AC-auth-session-csrf-33/v1 |
