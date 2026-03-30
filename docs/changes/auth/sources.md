# Source of Truth - auth

## 1) Mục đích

Tài liệu này xác định thứ bậc nguồn tham chiếu cho ticket `auth`, chỉ rõ tài liệu nào là authoritative, tài liệu nào chỉ có tính định hướng, những điểm nào từng mâu thuẫn, và quyết định nào đã được chốt để spec đủ điều kiện triển khai.

## 2) Thứ bậc Source of Truth

### Tier 1 - Ticket-specific authoritative inputs

1. `docs/changes/auth/raw/spec.md`
   - Vai trò: nguồn ticket-specific chính cho mục tiêu business, phạm vi, API auth, session/CSRF behavior, PostgreSQL/Flyway, wireframe và acceptance criteria ban đầu.
   - Lưu ý: vẫn phải bị ràng buộc bởi common rules ở `docs/standards/` khi có xung đột về security, testing, hoặc boundary clarity.

2. `docs/changes/auth/spec-pack.md`
   - Vai trò: ticket-level source of truth đã được chuẩn hóa lại để triển khai, sau khi reconcile raw spec với common rules và các quyết định đã chốt.
   - Kể từ thời điểm này, implementation và review phải bám vào file này thay vì diễn giải lại từ raw spec.

### Tier 2 - Repo-wide mandatory rules

3. `docs/standards/security.md`
   - Vai trò: baseline bảo mật bắt buộc cho auth/session/cookie/CORS/CSRF/config/docs/logs.
   - Tác động mạnh nhất tới ticket này vì story introduce authn, session, CSRF, Swagger exposure, seed data.

4. `docs/standards/testing.md`
   - Vai trò: baseline bắt buộc để viết AC testable và traceability tới evidence.

5. `docs/standards/coding.md`
   - Vai trò: baseline về layer boundaries, executable truth, environment-specific behavior, reviewability.

### Tier 3 - Common-base architecture guidance

6. `docs/architecture/overview.md`
   - Vai trò: mô tả baseline repo, ranh giới backend/frontend, vị trí docs, và common structure.
   - Lưu ý: một phần nội dung của file này dựa trên executable sources đang `missing` trong gói hiện tại, nên chỉ dùng như định hướng common-base; không dùng làm bằng chứng cuối cho As-Is code behavior.

7. `docs/architecture/key-flows.md`
   - Vai trò: skeleton flow cho request handling, frontend interaction, config/startup, change delivery.
   - Dùng để structure spec; không override ticket behavior.

### Tier 4 - Template / format guidance

8. `docs/standards/templates/spec-pack.template.md`
   - Vai trò: format và checklist trình bày.
   - Không phải source of truth cho behavior.

## 3) Những gì được xem là authoritative cho từng chủ đề

| Chủ đề | Nguồn authoritative | Ghi chú |
| ----- | ------------------- | ------- |
| Business purpose / story scope | `docs/changes/auth/raw/spec.md` + `docs/changes/auth/spec-pack.md` | `spec-pack.md` là bản triển khai hóa cuối cùng |
| API auth endpoints (`/login`, `/csrf`, `/logout`) | `docs/changes/auth/spec-pack.md` | Dùng contract đã được chốt, không quay lại trạng thái provisional |
| Session / cookie / CSRF principles | `docs/changes/auth/spec-pack.md` + `docs/standards/security.md` | Security rules override nếu raw spec tạo rủi ro hoặc mơ hồ |
| Environment-specific behavior | `docs/changes/auth/spec-pack.md` + `docs/standards/coding.md` Rule 15 + `docs/standards/security.md` Rule 36 | Phải explicit theo môi trường |
| DB / migration baseline | `docs/changes/auth/spec-pack.md` | `overview.md` không cung cấp executable DB truth |
| Testing / traceability expectations | `docs/changes/auth/spec-pack.md` + `docs/standards/testing.md` | AC và traceability phải bám spec pack |
| Doc placement / ticket boundaries | `docs/architecture/overview.md` + `docs/standards/coding.md` Rule 19 | Ticket-specific material phải ở `docs/changes/auth/` |

## 4) Các mâu thuẫn/ambiguity đã được chốt

### D-1. Login identifier

- Trước đây: raw API text dùng `username`, wireframe ghi `Username / Email`.
- Đã chốt: **`username`** là credential identifier duy nhất trong phạm vi ticket này.

**Hệ quả cho spec/implementation**:
- request contract của `POST /api/v1/auth/login` dùng field `username`;
- FE label/validation phải phản ánh `username-only`;
- không mở rộng email login trong ticket này.

### D-2. Error contract

- Trước đây: status code đã rõ nhưng body lỗi `401/403` chưa đủ deterministic.
- Đã chốt: dùng **JSON contract tối thiểu, machine-readable, an toàn** với các field:
  - `status`
  - `code`
  - `message`

**Hệ quả cho spec/implementation**:
- FE, API docs, BB/E2E assertions và log correlation có contract thống nhất;
- không dùng các body lỗi tuỳ hứng giữa các auth endpoints.

### D-3. Seed user mechanism

- Trước đây: raw spec nêu ví dụ plaintext credentials cho seed user, xung đột Rule 32.
- Đã chốt: seed user chỉ tồn tại ở `dev/test`, với username cố định nhưng password được cấp từ runtime secret input ngoài repo.

**Hệ quả cho spec/implementation**:
- repo/docs/logs không chứa plaintext seed password;
- DB chỉ chứa `password_hash`;
- khi bật seeding mà thiếu runtime secret input bắt buộc thì phải fail fast bằng thông báo sanitize.

### D-4. Production API docs exposure

- Trước đây: raw spec chỉ nói “không public”, chưa chốt observable behavior của `/v3/api-docs`.
- Đã chốt: trong `production`, truy cập **`/v3/api-docs` phải trả `404`**.

**Hệ quả cho spec/implementation**:
- verification prod hardening có điều kiện assert rõ ràng;
- Swagger UI vẫn không được public.

### D-5. Local frontend origin

- Trước đây: origin local chưa chốt cụ thể.
- Đã chốt: origin local phục vụ integration là **`http://localhost:5173`**.

**Hệ quả cho spec/implementation**:
- CORS allowlist local có giá trị rõ ràng để kiểm thử;
- không dùng wildcard origin cho credentialed requests.

## 5) Thiếu bằng chứng / missing inputs

Các executable sources được `docs/architecture/overview.md` viện dẫn nhưng không có trong gói hiện tại nên được đánh dấu `missing`:

- `demo/build.gradle` — missing
- `demo/settings.gradle` — missing
- `demo/src/main/resources/application.properties` — missing
- `demo/src/main/java/com/example/demo/DemoApplication.java` — missing
- `demo/src/test/java/com/example/demo/DemoApplicationTests.java` — missing
- `my-react-app/package.json` — missing
- `my-react-app/eslint.config.js` — missing
- `my-react-app/vite.config.ts` — missing
- `my-react-app/tsconfig.json` — missing
- `my-react-app/tsconfig.app.json` — missing
- `my-react-app/tsconfig.node.json` — missing
- `my-react-app/src/main.tsx` — missing
- `my-react-app/src/App.tsx` — missing

**Tác động**:
- Không thể dùng `overview.md` để khẳng định As-Is code/config chi tiết cho AUTH bằng chứng từ code.
- As-Is chỉ nên mô tả ở mức “repo baseline theo docs” thay vì “đã xác minh từ executable code”.

## 6) Quy tắc dùng khi triển khai và review

1. `docs/changes/auth/spec-pack.md` là source of truth triển khai chính cho ticket này.
2. Không quay lại dùng raw spec để override những điểm đã chốt trong spec pack.
3. Common rules trong `docs/standards/` có hiệu lực bắt buộc và có thể giới hạn cách thực hiện.
4. Chỉ coi requirement là “đã chốt” nếu nó có mặt trong spec pack hoặc trong common rules liên quan.
5. Các missing executable inputs ở trên không chặn implementation của ticket này, nhưng chặn việc khẳng định As-Is code hiện hữu vượt quá những gì docs chứng minh được.

## 7) Readiness conclusion

**Kết luận**: Với các quyết định đã chốt và `spec-pack.md` hiện tại, ticket `auth` **đã đủ điều kiện bắt đầu implementation**.

### Lý do

- Không còn Open Issue ở mức chặn contract hoặc acceptance.
- API contract, error contract, cookie policy, timeout, CORS local origin, prod docs behavior, và seed-user mechanism đều đã có quyết định cụ thể.
- AC đã testable và có traceability tới API/DB/log/permissions/test type.

### Residual attention points (không phải blocker)

- Local cross-site cookie verification vẫn phụ thuộc vào setup HTTPS/domain cụ thể của môi trường dev.
- In-memory session vẫn chỉ phù hợp phạm vi ticket hiện tại, không đại diện cho thiết kế multi-instance production-ready.
