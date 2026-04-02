# Black-box review checklist — auth

> Cập nhật: 2026-04-03
> Dùng để review nhanh bộ black-box spec trước khi chốt/execute.

## 1. Boundaries

- [ ] Mỗi AC có ít nhất một case ở ngưỡng quan trọng nếu spec có boundary rõ (`8h`, `2h`, `30m`, no-session vs expired-session, roleless user, stale token, cookie flags theo môi trường).
- [ ] Không thêm boundary suy diễn khi spec không nêu (ví dụ length/min/max username, password complexity runtime, ordering của roles).
- [ ] Timeout cases có phân biệt **ngay trước ngưỡng** và **vượt ngưỡng** ở nơi rủi ro cao nhất.
- [ ] Các case CSRF có đủ phân tầng: thiếu header, header rỗng, token ngẫu nhiên, token từ session khác / session cũ.

## 2. Permissions / security semantics

- [ ] Bộ test phân biệt rõ Anonymous, Authenticated session hợp lệ, Disabled user, Roleless user, Multi-role user.
- [ ] Logout được coi là protected endpoint; không bị bỏ sót rule session + CSRF.
- [ ] Có case chứng minh protected mutating requests **không session** trả `401`, còn **có session nhưng thiếu/sai CSRF** trả `403`.
- [ ] Có case xác nhận seed user chỉ có ở `dev/test`, không tràn sang `staging/prod`.
- [ ] Có case xác nhận cookie/CORS policy không làm suy yếu flow credentialed cross-site.

## 3. Compatibility / environment coverage

- [ ] Coverage tối thiểu cho cả `dev`, `staging`, `prod` với các khác biệt quan trọng: TTL, Secure cookie, Swagger/OpenAPI exposure, seed data, Flyway locations.
- [ ] Không dùng một profile để suy ra profile khác khi spec yêu cầu hành vi khác nhau.
- [ ] Có case CORS cho allowed origin `http://localhost:5173` và có negative case cho disallowed origin.
- [ ] Có kiểm tra không xuất hiện wildcard `*` trong credentialed CORS flow.

## 4. Exceptions / error handling

- [ ] Mọi lỗi 401/403 trong scope auth đều được kiểm qua envelope `{code,message,path,timestamp}`.
- [ ] Các mã lỗi chính đều có case riêng: `AUTH_INVALID_CREDENTIALS`, `AUTH_USER_DISABLED`, `AUTH_SESSION_REQUIRED`, `AUTH_SESSION_EXPIRED`, `AUTH_CSRF_INVALID`.
- [ ] Có case phân biệt `session required` và `session expired`; không collapse hai tình huống này thành một expected chung.
- [ ] Các case lỗi không yêu cầu message text tuyệt đối nếu spec không chốt wording; chỉ kiểm field và semantics đã chốt.

## 5. Audit / logging / observability

- [ ] Có case cho tối thiểu 6 nhóm audit event: login success, login failure, logout success, unauthenticated access, session expired, CSRF denied.
- [ ] Expected log chỉ kiểm **event semantics** và **redaction**, không khóa cứng vào tên class/logger/message nội bộ.
- [ ] Checklist review có cách truy vết log theo correlation-id, timestamp window hoặc request metadata mà không cần biết implementation log framework.
- [ ] Có kiểm tra log không chứa plaintext password, full session id, full CSRF token.

## 6. Performance degradation / operational practicality

- [ ] Case P0 không đòi hỏi wait thủ công dài vô lý; timeout dài có chiến lược time-control hoặc môi trường điều khiển được.
- [ ] Có ít nhất một case xác nhận khi migration/DB lỗi thì hệ thống không báo auth-ready giả.
- [ ] Có case xác nhận auth flow không phụ thuộc Redis/persistent session store trong phạm vi ticket.
- [ ] P0/P1/P2 được dùng để tránh nở vô hạn số case; các case P2 thật sự là regression sâu, không phải điều kiện release tối thiểu.

## 7. Black-box discipline

- [ ] Không nhắc tới class, filter, repository, framework hook, table/column ngoài những gì spec đã public hóa.
- [ ] Protected business APIs chưa được spec đặt tên được biểu diễn bằng endpoint alias, không bịa API cụ thể.
- [ ] DB/log assertions chỉ xuất hiện ở nơi spec hoặc NFR đã biến chúng thành contract kiểm tra được.
- [ ] Không kiểm những behavior ngoài scope ticket: JWT, refresh token, remember-me, MFA, social login, email login, `/auth/me`, `/auth/status`, Redis session store.

## 8. Traceability sanity

- [ ] Mỗi AC có ít nhất 1 case P0 hoặc được giải thích rõ vì sao không cần P0.
- [ ] Bảng mapping AC ↔ case IDs ở cuối `blackbox-testcases.md` không thiếu AC nào từ spec-pack.
- [ ] Data IDs trong `test-data.md` đủ để chạy tất cả case IDs; không có case dùng fixture chưa được định nghĩa.
