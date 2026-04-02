# Danh sách kiểm tra review — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng)

> Ngày: 2026-04-02  
> Nguồn tổng hợp: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/standards/templates/review-checklist.template.md`  
> Ghi chú: repository hiện chưa có `docs/changes/ticket-1/spec-pack.md`; checklist này được dựng cho **canonical change code `ticket-1`** từ bộ spec/plan hiện có.
>
> Mức độ nghiêm trọng: **Blocker** = bắt buộc phải sửa trước khi merge | **Major** = phải sửa trong PR này | **Minor** = có thể merge nếu đã ghi nhận rõ nợ kỹ thuật và biện pháp theo dõi

---

## 1. Specification / AC

| #     | Hạng mục kiểm tra                                                                                                                                                                                                                   | Severity | Trạng thái |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-01 | Endpoint, HTTP method và auth flow khớp spec: `POST /api/v1/auth/login`, `GET /api/v1/auth/csrf`, `POST /api/v1/auth/logout`; không xuất hiện endpoint auth-status riêng trong phạm vi ticket.                                      | Blocker  | [ ]        |
| RC-02 | Login success response luôn chứa đủ `authenticated`, `username`, `roles`; `roles` luôn là mảng, cho phép `[]`, không được null/omit; contract và UI chỉ dùng khái niệm `Username`, không có email login.                            | Blocker  | [ ]        |
| RC-03 | Không đưa vào PR các hành vi ngoài phạm vi đã loại trừ trong spec: JWT/refresh token, remember me, social login, MFA, origin khác `http://localhost:5173`, Redis/persistent session store, endpoint `/auth/me` hoặc `/auth/status`. | Blocker  | [ ]        |
| RC-04 | Mỗi AC trong spec đều có bằng chứng kiểm chứng rõ ràng (UT/IT/E2E/manual), không còn “implicit acceptance” hoặc suy diễn ngoài spec.                                                                                                | Blocker  | [ ]        |

## 2. Design / Dependencies

| #     | Hạng mục kiểm tra                                                                                                                                                                                                                | Severity | Trạng thái |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-05 | Phân lớp rõ ràng: controller chỉ điều phối HTTP, service xử lý auth logic, config tập trung ở security/config, repository không rò rỉ sang UI; không có circular dependency.                                                     | Major    | [ ]        |
| RC-06 | Bộ phụ thuộc và cấu hình phản ánh đúng hướng thiết kế: Spring Security theo session-based auth, PostgreSQL là auth store, Flyway quản lý migration, Springdoc được bật/tắt theo môi trường; không phát sinh phụ thuộc trái spec. | Blocker  | [ ]        |
| RC-07 | FE contract được tập trung và nhất quán: mọi call dùng `credentials: include`, rehydrate chỉ đi qua `GET /api/v1/auth/csrf`, header CSRF được dùng đúng tên từ contract, không hard-code flow khác spec.                         | Major    | [ ]        |

## 3. Security

| #     | Hạng mục kiểm tra                                                                                                                                                                                                        | Severity | Trạng thái |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- | ---------- |
| RC-08 | Chỉ user `enabled=true` mới login được; password được hash mạnh; login sai hoặc user bị disable không tạo session hợp lệ; seed password được lưu dưới dạng hash, không có plaintext trong migration/config.              | Blocker  | [ ]        |
| RC-09 | Session cookie sau login có đúng policy: `HttpOnly=true`, `SameSite=None`, `Path=/`; ở `staging/prod` có `Secure=true`; response logout clear/expire cookie đúng semantics.                                              | Blocker  | [ ]        |
| RC-10 | CORS được giới hạn đúng cho `http://localhost:5173`, `allowCredentials=true`, không dùng wildcard origin với credentialed requests, và cho phép các method/header cần thiết (`OPTIONS`, `Content-Type`, `X-CSRF-TOKEN`). | Blocker  | [ ]        |
| RC-11 | Mọi mutating protected endpoint đều enforce đồng thời session hợp lệ + CSRF hợp lệ; login được exempt đúng; logout vẫn là protected endpoint; không có lỗ hổng do bỏ sót matcher/filter chain.                           | Blocker  | [ ]        |
| RC-12 | Session semantics đúng: session fixation protection không bị vô hiệu hóa, logout chỉ hủy session hiện tại, session hết hạn không được tiếp tục dùng, session cũ không “sống lại” sau logout.                             | Blocker  | [ ]        |
| RC-13 | Seed/migration được cô lập theo môi trường: `dev/test` mới nạp non-prod location; `staging/prod` không nhìn thấy seed users; misconfiguration kiểu nạp nhầm non-prod location được xem là fail acceptance.               | Blocker  | [ ]        |
| RC-14 | Không có secret/PII/giá trị nhạy cảm trong log, diff, ví dụ OpenAPI, error response hoặc tài liệu: không log plaintext password, full session id, full CSRF token.                                                       | Blocker  | [ ]        |

## 4. Performance

| #     | Hạng mục kiểm tra                                                                                                                                        | Severity | Trạng thái |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-15 | Tra cứu user/roles cho login không tạo N+1 query hoặc round-trip thừa; nếu dùng relation roles thì đã xử lý fetch strategy/repository query phù hợp.     | Major    | [ ]        |
| RC-16 | `login`, `csrf`, `logout` không kéo theo blocking I/O ngoài DB nội bộ, không truy vấn dư thừa theo request, không serialize payload lớn không cần thiết. | Major    | [ ]        |
| RC-17 | Kiểm thử TTL/session expiry được thiết kế thực dụng (test profile timeout ngắn/mock clock) thay vì sleep dài gây flakiness và làm chậm pipeline.         | Minor    | [ ]        |

## 5. Compatibility

| #     | Hạng mục kiểm tra                                                                                                                                                                        | Severity | Trạng thái |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-18 | OpenAPI, request/response samples, status code và error codes khớp implementation; public contract không lệch giữa code, Swagger và FE usage.                                            | Blocker  | [ ]        |
| RC-19 | Hành vi theo môi trường nhất quán với spec: `dev=8h`, `staging=2h`, `prod=30m`; Swagger chỉ public ở `dev/staging`; cookie secure policy và Flyway locations thay đổi đúng theo profile. | Blocker  | [ ]        |
| RC-20 | Migration chạy được trên DB trống theo quy trình chuẩn, không cần thao tác tay ngoài plan; thay đổi schema là forward-only và có chỉ dẫn rollback/repair tương ứng.                      | Major    | [ ]        |
| RC-21 | Local-dev workaround (ví dụ topology không có HTTPS local) không làm suy yếu contract mục tiêu của `staging/prod`; mọi khác biệt local đều được tài liệu hóa rõ.                         | Minor    | [ ]        |

## 6. Logs / Audit

| #     | Hạng mục kiểm tra                                                                                                                                                          | Severity | Trạng thái |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-22 | Có audit log tối thiểu cho các event bắt buộc: login success, login failure, logout success, unauthenticated access, session expired, CSRF denied.                         | Major    | [ ]        |
| RC-23 | Log message đủ để phân biệt nguyên nhân (`invalid credentials`, `disabled user`, `session required`, `session expired`, `csrf invalid`) nhưng không leak dữ liệu nhạy cảm. | Blocker  | [ ]        |

## 7. Error Handling

| #     | Hạng mục kiểm tra                                                                                                                                                                                                                 | Severity | Trạng thái |
| ----- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-24 | Mọi lỗi `401/403` trong module auth dùng chung envelope `{code,message,path,timestamp}` và map đúng code: `AUTH_INVALID_CREDENTIALS`, `AUTH_USER_DISABLED`, `AUTH_SESSION_REQUIRED`, `AUTH_SESSION_EXPIRED`, `AUTH_CSRF_INVALID`. | Blocker  | [ ]        |
| RC-25 | Hệ thống phân biệt đúng `session required` và `session expired` ở các luồng cần thiết; không collapse mọi tình huống về cùng một mã lỗi nếu spec yêu cầu khác nhau.                                                               | Major    | [ ]        |
| RC-26 | Client không nhận stack trace, class name nội bộ hoặc thông điệp exception thô; `AuthenticationEntryPoint`, `AccessDeniedHandler` và `ControllerAdvice` nhất quán với nhau.                                                       | Major    | [ ]        |

## 8. Testing

| #     | Hạng mục kiểm tra                                                                                                                                                                                                                              | Severity | Trạng thái |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-27 | Bộ test tự động bao phủ đầy đủ các luồng chính: login success/failure/disabled, cookie flags, CSRF fetch, protected endpoint with/without session, logout, roles array, CORS, session timeout, seed isolation, Swagger gating, error envelope. | Blocker  | [ ]        |
| RC-28 | FE/E2E/manual flow bao phủ các hành vi user-facing quan trọng: form chỉ có `Username` + `Password`, rehydrate qua `GET /api/v1/auth/csrf`, session expired UX, logout quay về trạng thái unauthenticated.                                      | Major    | [ ]        |
| RC-29 | Test fixtures và seed data khớp spec: có `admin` và `user01` cho `dev/test`, password đã hash, có case user không có role, không có fixture dựa trên email login ngoài phạm vi.                                                                | Major    | [ ]        |

## 9. Operations

| #     | Hạng mục kiểm tra                                                                                                                                                                                                                | Severity | Trạng thái |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------- |
| RC-30 | Tài liệu triển khai nêu rõ config vận hành bắt buộc: datasource PostgreSQL, Flyway locations, session timeout theo profile, CORS allowlist, springdoc enablement; misconfig phải fail loud hoặc ít nhất fail acceptance rõ ràng. | Major    | [ ]        |
| RC-31 | Quy trình rollback/code revert/DB cleanup và smoke verification sau rollback đã được tài liệu hóa, đủ chi tiết cho người trực/on-call thực hiện.                                                                                 | Major    | [ ]        |
| RC-32 | Có checklist xác minh sau triển khai hoặc trước merge cho luồng `login -> csrf -> protected flow -> logout`, cộng với kiểm tra CORS preflight và Swagger exposure theo môi trường.                                               | Minor    | [ ]        |

---

## Gợi ý bằng chứng nên đính kèm khi review

- Link/tên test case hoặc class test cho từng RC mức **Blocker**.
- Snippet config theo profile (`application-dev`, `application-staging`, `application-prod`).
- Kết quả `curl`/`MockMvc`/E2E cho login, csrf, logout, CORS preflight.
- Log sample đã được redaction để chứng minh audit behavior mà không lộ secret.
- Ảnh chụp Swagger/OpenAPI ở `dev/staging` và bằng chứng không public ở `prod`.

---

## Bảng mapping từ checklist sang AC

| RC#   | AC được xác nhận                                                                                                                                                                                                                                                                                                                               |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| RC-01 | AC-auth-1/v1, AC-auth-7/v1, AC-auth-13/v1, AC-auth-25/v1                                                                                                                                                                                                                                                                                       |
| RC-02 | AC-auth-1/v1, AC-auth-24/v1, AC-auth-25/v1                                                                                                                                                                                                                                                                                                     |
| RC-03 | AC-auth-20/v1, AC-auth-21/v1, AC-auth-25/v1, AC-auth-26/v1, AC-auth-27/v1                                                                                                                                                                                                                                                                      |
| RC-04 | Tất cả AC-auth-1/v1 đến AC-auth-27/v1                                                                                                                                                                                                                                                                                                          |
| RC-05 | Hỗ trợ review thiết kế cho toàn bộ ticket (không ánh xạ 1-1 tới một AC đơn lẻ)                                                                                                                                                                                                                                                                 |
| RC-06 | AC-auth-16/v1, AC-auth-17/v1, AC-auth-18/v1, AC-auth-21/v1                                                                                                                                                                                                                                                                                     |
| RC-07 | AC-auth-7/v1, AC-auth-25/v1, AC-auth-26/v1, AC-auth-27/v1                                                                                                                                                                                                                                                                                      |
| RC-08 | AC-auth-1/v1, AC-auth-4/v1, AC-auth-5/v1, AC-auth-17/v1, AC-auth-19/v1                                                                                                                                                                                                                                                                         |
| RC-09 | AC-auth-2/v1, AC-auth-3/v1, AC-auth-13/v1                                                                                                                                                                                                                                                                                                      |
| RC-10 | AC-auth-10/v1, AC-auth-27/v1                                                                                                                                                                                                                                                                                                                   |
| RC-11 | AC-auth-9/v1, AC-auth-10/v1, AC-auth-11/v1, AC-auth-12/v1, AC-auth-13/v1, AC-auth-14/v1, AC-auth-15/v1                                                                                                                                                                                                                                         |
| RC-12 | AC-auth-6/v1, AC-auth-13/v1                                                                                                                                                                                                                                                                                                                    |
| RC-13 | AC-auth-19/v1, AC-auth-20/v1                                                                                                                                                                                                                                                                                                                   |
| RC-14 | AC-auth-22/v1                                                                                                                                                                                                                                                                                                                                  |
| RC-15 | AC-auth-1/v1, AC-auth-17/v1, AC-auth-24/v1                                                                                                                                                                                                                                                                                                     |
| RC-16 | AC-auth-1/v1, AC-auth-7/v1, AC-auth-13/v1                                                                                                                                                                                                                                                                                                      |
| RC-17 | AC-auth-6/v1                                                                                                                                                                                                                                                                                                                                   |
| RC-18 | AC-auth-16/v1, AC-auth-23/v1, AC-auth-24/v1, AC-auth-25/v1                                                                                                                                                                                                                                                                                     |
| RC-19 | AC-auth-3/v1, AC-auth-6/v1, AC-auth-16/v1, AC-auth-19/v1, AC-auth-20/v1, AC-auth-27/v1                                                                                                                                                                                                                                                         |
| RC-20 | AC-auth-17/v1, AC-auth-18/v1                                                                                                                                                                                                                                                                                                                   |
| RC-21 | AC-auth-3/v1, AC-auth-27/v1                                                                                                                                                                                                                                                                                                                    |
| RC-22 | AC-auth-22/v1                                                                                                                                                                                                                                                                                                                                  |
| RC-23 | AC-auth-22/v1, AC-auth-23/v1                                                                                                                                                                                                                                                                                                                   |
| RC-24 | AC-auth-4/v1, AC-auth-5/v1, AC-auth-8/v1, AC-auth-9/v1, AC-auth-10/v1, AC-auth-11/v1, AC-auth-14/v1, AC-auth-15/v1, AC-auth-23/v1                                                                                                                                                                                                              |
| RC-25 | AC-auth-6/v1, AC-auth-8/v1                                                                                                                                                                                                                                                                                                                     |
| RC-26 | AC-auth-23/v1                                                                                                                                                                                                                                                                                                                                  |
| RC-27 | AC-auth-1/v1, AC-auth-2/v1, AC-auth-3/v1, AC-auth-4/v1, AC-auth-5/v1, AC-auth-6/v1, AC-auth-7/v1, AC-auth-8/v1, AC-auth-9/v1, AC-auth-10/v1, AC-auth-11/v1, AC-auth-12/v1, AC-auth-13/v1, AC-auth-14/v1, AC-auth-15/v1, AC-auth-16/v1, AC-auth-18/v1, AC-auth-20/v1, AC-auth-22/v1, AC-auth-23/v1, AC-auth-24/v1, AC-auth-26/v1, AC-auth-27/v1 |
| RC-28 | AC-auth-25/v1, AC-auth-26/v1                                                                                                                                                                                                                                                                                                                   |
| RC-29 | AC-auth-19/v1, AC-auth-20/v1, AC-auth-24/v1, AC-auth-25/v1                                                                                                                                                                                                                                                                                     |
| RC-30 | AC-auth-6/v1, AC-auth-16/v1, AC-auth-17/v1, AC-auth-18/v1, AC-auth-20/v1, AC-auth-27/v1                                                                                                                                                                                                                                                        |
| RC-31 | AC-auth-18/v1, AC-auth-20/v1                                                                                                                                                                                                                                                                                                                   |
| RC-32 | AC-auth-3/v1, AC-auth-13/v1, AC-auth-16/v1, AC-auth-26/v1, AC-auth-27/v1                                                                                                                                                                                                                                                                       |

---

## Bảng mapping từ AC sang checklist

| AC            | Các RC dùng để xác nhận                                |
| ------------- | ------------------------------------------------------ |
| AC-auth-1/v1  | RC-01, RC-02, RC-08, RC-15, RC-16, RC-27               |
| AC-auth-2/v1  | RC-09, RC-27                                           |
| AC-auth-3/v1  | RC-09, RC-19, RC-21, RC-27, RC-32                      |
| AC-auth-4/v1  | RC-08, RC-24, RC-27                                    |
| AC-auth-5/v1  | RC-08, RC-24, RC-27                                    |
| AC-auth-6/v1  | RC-12, RC-17, RC-19, RC-25, RC-27, RC-30               |
| AC-auth-7/v1  | RC-01, RC-07, RC-16, RC-27                             |
| AC-auth-8/v1  | RC-24, RC-25, RC-27                                    |
| AC-auth-9/v1  | RC-11, RC-24, RC-27                                    |
| AC-auth-10/v1 | RC-10, RC-11, RC-24, RC-27                             |
| AC-auth-11/v1 | RC-11, RC-24, RC-27                                    |
| AC-auth-12/v1 | RC-11, RC-27                                           |
| AC-auth-13/v1 | RC-01, RC-09, RC-11, RC-12, RC-16, RC-27, RC-32        |
| AC-auth-14/v1 | RC-11, RC-24, RC-27                                    |
| AC-auth-15/v1 | RC-11, RC-24, RC-27                                    |
| AC-auth-16/v1 | RC-06, RC-18, RC-19, RC-27, RC-30, RC-32               |
| AC-auth-17/v1 | RC-06, RC-08, RC-15, RC-20, RC-30                      |
| AC-auth-18/v1 | RC-06, RC-20, RC-27, RC-30, RC-31                      |
| AC-auth-19/v1 | RC-08, RC-13, RC-19, RC-29                             |
| AC-auth-20/v1 | RC-03, RC-13, RC-19, RC-27, RC-29, RC-30, RC-31        |
| AC-auth-21/v1 | RC-03, RC-06                                           |
| AC-auth-22/v1 | RC-14, RC-22, RC-23, RC-27                             |
| AC-auth-23/v1 | RC-18, RC-23, RC-24, RC-26, RC-27                      |
| AC-auth-24/v1 | RC-02, RC-15, RC-18, RC-27, RC-29                      |
| AC-auth-25/v1 | RC-01, RC-02, RC-03, RC-07, RC-18, RC-28, RC-29        |
| AC-auth-26/v1 | RC-03, RC-07, RC-27, RC-28, RC-32                      |
| AC-auth-27/v1 | RC-03, RC-07, RC-10, RC-19, RC-21, RC-27, RC-30, RC-32 |
