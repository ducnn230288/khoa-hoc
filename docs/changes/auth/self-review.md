# Tự review — ticket-1 (Authentication bằng Session Cookie + CSRF API riêng)

> Ngày: 2026-04-02  
> Mục đích: tài liệu này được AI điền **sau implementation, trước review thủ công**.  
> Nguồn đối chiếu: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/changes/ticket-1/review-checklist.md`  
> Quy tắc điền: mọi checkbox chỉ được tick khi có bằng chứng đi kèm (tệp + dòng, tên test, lệnh đã chạy, log hoặc screenshot).

---

## 1. Thông tin phiên tự review

- Người điền:
- Commit / branch đang review:
- PR / changeset:
- Môi trường đã kiểm tra: [ ] dev [ ] test [ ] staging-like [ ] khác:
- Phạm vi đã triển khai so với spec: [ ] đầy đủ [ ] một phần (nêu rõ bên dưới)

## 2. Trạng thái hoàn thành AC

> Đánh dấu từng AC sau khi đã có bằng chứng. Có thể ghi thêm tên test hoặc đường dẫn bằng chứng ở cuối mỗi dòng.

### 2.1 Backend auth flow

- [ ] **AC-auth-1/v1** — Login hợp lệ trả `200`, tạo server-side session, trả session cookie.  
      Bằng chứng:
- [ ] **AC-auth-2/v1** — Session cookie có `HttpOnly=true`, `SameSite=None`, `Path=/`.  
      Bằng chứng:
- [ ] **AC-auth-3/v1** — Ở `staging/prod`, session cookie có `Secure=true`.  
      Bằng chứng:
- [ ] **AC-auth-4/v1** — Sai `username/password` trả `401` + `AUTH_INVALID_CREDENTIALS`, không tạo session hợp lệ.  
      Bằng chứng:
- [ ] **AC-auth-5/v1** — User `enabled=false` trả `401` + `AUTH_USER_DISABLED`, không tạo session hợp lệ.  
      Bằng chứng:
- [ ] **AC-auth-6/v1** — TTL session đúng theo môi trường (`dev=8h`, `staging=2h`, `prod=30m`) và session cũ hết hạn trả `401`.  
      Bằng chứng:
- [ ] **AC-auth-7/v1** — `GET /api/v1/auth/csrf` với session hợp lệ trả `200` cùng `csrfToken`, `headerName`, `parameterName`.  
      Bằng chứng:
- [ ] **AC-auth-8/v1** — `GET /api/v1/auth/csrf` không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng:
- [ ] **AC-auth-9/v1** — Protected mutating request không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng:
- [ ] **AC-auth-10/v1** — Protected mutating request có session nhưng thiếu `X-CSRF-TOKEN` trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng:
- [ ] **AC-auth-11/v1** — Protected mutating request có session nhưng CSRF sai/hết hiệu lực trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng:
- [ ] **AC-auth-12/v1** — Protected mutating request có session + CSRF hợp lệ đi qua auth/CSRF guard.  
      Bằng chứng:
- [ ] **AC-auth-13/v1** — Logout hợp lệ trả `200`, invalidate session hiện tại, clear/expire cookie.  
      Bằng chứng:
- [ ] **AC-auth-14/v1** — Logout không có session hợp lệ trả `401` + `AUTH_SESSION_REQUIRED`.  
      Bằng chứng:
- [ ] **AC-auth-15/v1** — Logout có session nhưng thiếu/sai CSRF trả `403` + `AUTH_CSRF_INVALID`.  
      Bằng chứng:

### 2.2 Docs / config / persistence

- [ ] **AC-auth-16/v1** — `dev/staging` public OpenAPI/Swagger; `prod` không public.  
      Bằng chứng:
- [ ] **AC-auth-17/v1** — PostgreSQL lưu auth data trong `users`, `roles`, `user_roles`.  
      Bằng chứng:
- [ ] **AC-auth-18/v1** — Flyway khởi tạo schema auth được trên DB trống theo quy trình chuẩn.  
      Bằng chứng:
- [ ] **AC-auth-19/v1** — `dev/test` tạo được seed users `admin`, `user01`, password đã hash.  
      Bằng chứng:
- [ ] **AC-auth-20/v1** — `staging/prod` không nạp seed users và không nạp non-prod Flyway location.  
      Bằng chứng:
- [ ] **AC-auth-21/v1** — Session store của ticket là in-memory/default session, không phụ thuộc Redis/persistent store.  
      Bằng chứng:
- [ ] **AC-auth-22/v1** — Audit logs có đủ sự kiện tối thiểu và không leak plaintext password/full session id/full CSRF token.  
      Bằng chứng:
- [ ] **AC-auth-23/v1** — Mọi lỗi `401/403` có đủ `code`, `message`, `path`, `timestamp`.  
      Bằng chứng:
- [ ] **AC-auth-24/v1** — Login success response luôn có `roles` là mảng; user không có role trả `[]`.  
      Bằng chứng:
- [ ] **AC-auth-25/v1** — UI/API/OpenAPI chỉ dùng `Username`, không có email login.  
      Bằng chứng:
- [ ] **AC-auth-26/v1** — Sau reload, session còn hiệu lực thì `GET /api/v1/auth/csrf` rehydrate được flow mà không cần auth-status endpoint riêng.  
      Bằng chứng:
- [ ] **AC-auth-27/v1** — Backend chấp nhận credentialed CORS request từ `http://localhost:5173` và không dùng wildcard origin cho flow này.  
      Bằng chứng:

---

## 3. Tự điền theo review checklist

> Tick sau khi đã kiểm tra theo `docs/changes/ticket-1/review-checklist.md`. Khi tick, ghi ít nhất một bằng chứng (file/line, test hoặc command).

### 3.1 Specification / AC

- [ ] **RC-01** — Endpoint/method/auth flow khớp spec.  
      Bằng chứng:
- [ ] **RC-02** — Login response + contract `Username`/`roles[]` đúng spec.  
      Bằng chứng:
- [ ] **RC-03** — Không triển khai ngoài phạm vi.  
      Bằng chứng:
- [ ] **RC-04** — Tất cả AC đều có bằng chứng.  
      Bằng chứng:

### 3.2 Design / Dependencies

- [ ] **RC-05** — Phân lớp đúng, không circular dependency.  
      Bằng chứng:
- [ ] **RC-06** — Dependency/config phù hợp hướng session + PostgreSQL + Flyway + Springdoc.  
      Bằng chứng:
- [ ] **RC-07** — FE contract thống nhất với `credentials: include` và `GET /api/v1/auth/csrf` rehydrate.  
      Bằng chứng:

### 3.3 Security

- [ ] **RC-08** — Hash/password/enabled user/session creation đúng rule.  
      Bằng chứng:
- [ ] **RC-09** — Cookie policy đúng theo môi trường; logout clear cookie.  
      Bằng chứng:
- [ ] **RC-10** — CORS allowlist/credentials/headers/methods đúng.  
      Bằng chứng:
- [ ] **RC-11** — Protected mutating endpoints enforce session + CSRF đầy đủ.  
      Bằng chứng:
- [ ] **RC-12** — Session invalidation/fixation semantics đúng.  
      Bằng chứng:
- [ ] **RC-13** — Seed isolation đúng theo profile.  
      Bằng chứng:
- [ ] **RC-14** — Không leak secret/PII trong log, docs, responses.  
      Bằng chứng:

### 3.4 Performance

- [ ] **RC-15** — Không N+1 / query thừa khi load user/roles.  
      Bằng chứng:
- [ ] **RC-16** — `login/csrf/logout` không có blocking I/O hoặc payload thừa.  
      Bằng chứng:
- [ ] **RC-17** — TTL test không flaky, không dùng sleep dài vô ích.  
      Bằng chứng:

### 3.5 Compatibility

- [ ] **RC-18** — Code/Swagger/FE contract đồng nhất.  
      Bằng chứng:
- [ ] **RC-19** — Hành vi đúng theo `dev/staging/prod`.  
      Bằng chứng:
- [ ] **RC-20** — Flyway startup trên DB trống + rollback path rõ.  
      Bằng chứng:
- [ ] **RC-21** — Local workaround không làm suy yếu contract `staging/prod`.  
      Bằng chứng:

### 3.6 Logs / Audit

- [ ] **RC-22** — Đủ audit events bắt buộc.  
      Bằng chứng:
- [ ] **RC-23** — Log đủ chẩn đoán nhưng không leak secret.  
      Bằng chứng:

### 3.7 Error Handling

- [ ] **RC-24** — Error envelope + code mapping đúng.  
      Bằng chứng:
- [ ] **RC-25** — `session required` vs `session expired` được phân biệt đúng chỗ.  
      Bằng chứng:
- [ ] **RC-26** — Không leak stack trace/internal details.  
      Bằng chứng:

### 3.8 Testing

- [ ] **RC-27** — Automated test coverage đủ cho auth flow/cookie/csrf/cors/flyway/swagger/errors.  
      Bằng chứng:
- [ ] **RC-28** — FE/E2E/manual coverage đủ cho username-only UI + reload rehydrate + expired session + logout.  
      Bằng chứng:
- [ ] **RC-29** — Fixtures/seed data khớp spec.  
      Bằng chứng:

### 3.9 Operations

- [ ] **RC-30** — Config vận hành bắt buộc đã được tài liệu hóa.  
      Bằng chứng:
- [ ] **RC-31** — Rollback/runbook đủ chi tiết.  
      Bằng chứng:
- [ ] **RC-32** — Có smoke checklist trước merge / trước deploy.  
      Bằng chứng:

---

## 4. Tóm tắt diff đã triển khai

### 4.1 Các tệp/chủ đề đã thay đổi

| Khu vực           | Tệp / thư mục | Mô tả thay đổi | Bằng chứng |
| ----------------- | ------------- | -------------- | ---------- |
| Backend           |               |                |            |
| Frontend          |               |                |            |
| Database / Flyway |               |                |            |
| Config / Profiles |               |                |            |
| Docs / OpenAPI    |               |                |            |
| Tests             |               |                |            |

### 4.2 Điểm cần reviewer chú ý nhất

-
-
-

---

## 5. Các lệnh đã chạy và kết quả

> Ghi cả lệnh **pass** lẫn **fail đáng chú ý**. Nếu có log file/screenshot, ghi đường dẫn vào cột “Bằng chứng”.

### 5.1 Static checks / lint / format

| #   | Khu vực  | Lệnh | Kết quả (PASS/FAIL) | Ghi chú | Bằng chứng |
| --- | -------- | ---- | ------------------- | ------- | ---------- |
| 1   | Backend  |      |                     |         |            |
| 2   | Frontend |      |                     |         |            |
| 3   | Khác     |      |                     |         |            |

### 5.2 Unit / integration / build

| #   | Khu vực       | Lệnh | Kết quả (PASS/FAIL) | Coverage / scope | Bằng chứng |
| --- | ------------- | ---- | ------------------- | ---------------- | ---------- |
| 1   | Backend UT    |      |                     |                  |            |
| 2   | Backend IT    |      |                     |                  |            |
| 3   | Frontend test |      |                     |                  |            |
| 4   | Build         |      |                     |                  |            |

### 5.3 Manual verification / curl / browser

| #   | Tình huống                       | Lệnh / bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Bằng chứng |
| --- | -------------------------------- | --------------------- | ---------------- | --------------- | ---------- |
| 1   | Login success                    |                       |                  |                 |            |
| 2   | Lấy CSRF token                   |                       |                  |                 |            |
| 3   | Protected request thiếu session  |                       |                  |                 |            |
| 4   | Protected request thiếu/sai CSRF |                       |                  |                 |            |
| 5   | Logout                           |                       |                  |                 |            |
| 6   | CORS preflight                   |                       |                  |                 |            |
| 7   | Swagger `dev/staging`            |                       |                  |                 |            |
| 8   | Swagger `prod` not public        |                       |                  |                 |            |
| 9   | Reload rehydrate                 |                       |                  |                 |            |

### 5.4 Lệnh mẫu nên ưu tiên ghi lại nếu đã chạy

```bash
# Backend
./gradlew clean test
./gradlew bootRun --args='--spring.profiles.active=dev'
./gradlew bootRun --args='--spring.profiles.active=staging'

# Frontend
npm run lint
npm run build
npm test

# Manual smoke
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user01","password":"User@123"}' -v

curl -b cookies.txt http://localhost:8080/api/v1/auth/csrf -v

curl -b cookies.txt -X POST http://localhost:8080/api/v1/auth/logout \
  -H "X-CSRF-TOKEN: <token>" -v

curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type" -v
```

---

## 6. Known risks

| #   | Risk | Mức độ | Ảnh hưởng | Mitigation hiện có | Cần theo dõi thêm |
| --- | ---- | ------ | --------- | ------------------ | ----------------- |
| 1   |      |        |           |                    |                   |
| 2   |      |        |           |                    |                   |
| 3   |      |        |           |                    |                   |

## 7. Not handled yet

| #   | Hạng mục chưa xử lý | Vì sao chưa làm | Liên quan AC/RC nào | Quyết định hiện tại |
| --- | ------------------- | --------------- | ------------------- | ------------------- |
| 1   |                     |                 |                     |                     |
| 2   |                     |                 |                     |                     |
| 3   |                     |                 |                     |                     |

## 8. Remaining issues / follow-ups

| #   | Vấn đề còn lại | Severity | Cách tái hiện / bối cảnh | Hướng xử lý đề xuất | Owner |
| --- | -------------- | -------- | ------------------------ | ------------------- | ----- |
| 1   |                |          |                          |                     |       |
| 2   |                |          |                          |                     |       |
| 3   |                |          |                          |                     |       |

---

## 9. Kết luận tự review

- [ ] Tất cả AC đã hoàn thành và có bằng chứng.
- [ ] Không còn Blocker trong `review-checklist.md`.
- [ ] Các Major còn lại (nếu có) đã được ghi rõ ở mục “Remaining issues / follow-ups”.
- [ ] Tài liệu/runbook/test evidence đủ để chuyển sang review thủ công.

**Kết luận đề xuất:** [ ] Ready for manual review [ ] Cần sửa thêm trước khi review

**Tóm tắt ngắn cho reviewer tiếp theo:**

-
