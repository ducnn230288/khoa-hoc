# Sources of Truth — ticket-1

> Cập nhật: 2026-04-01  
> Mục đích: xác định nguồn authoritative cho đặc tả triển khai và ghi lại các quyết định đã chốt để implementation có thể bắt đầu.

---

## 1. Danh sách nguồn đặc tả

| # | Source | Vai trò | Mức độ authoritative | Ghi chú sử dụng |
|---|--------|---------|----------------------|-----------------|
| 1 | `docs/changes/ticket-1/spec-pack.md` | **Đặc tả triển khai chính thức** cho ticket này sau khi đã chốt các quyết định còn mở | **Primary** | Tất cả implementation, test case, review và handoff phải bám vào file này |
| 2 | `docs/changes/ticket-1/raw/spec.md` | Nguồn business intent và phạm vi ban đầu | **Upstream business input** | Dùng để truy vết bối cảnh nghiệp vụ và các quyết định ban đầu; khi có khác biệt với `spec-pack.md` thì `spec-pack.md` thắng |
| 3 | `docs/standards/templates/spec-pack.template.md` | Chuẩn cấu trúc tài liệu | **Structural** | Authoritative cho bố cục tài liệu và các section bắt buộc; không authoritative cho nghiệp vụ |

---

## 2. Quy tắc ưu tiên khi có khác biệt

1. **Implementation / contract / AC / traceability / testability**: ưu tiên `docs/changes/ticket-1/spec-pack.md`.
2. **Business intent gốc**: tham chiếu `docs/changes/ticket-1/raw/spec.md` khi cần giải thích lý do tồn tại của yêu cầu.
3. **Cấu trúc tài liệu**: ưu tiên `docs/standards/templates/spec-pack.template.md`.
4. Những gì **không có** trong `spec-pack.md` thì **không được coi là đã chốt để triển khai**.

---

## 3. Các quyết định đã được chốt để bỏ trạng thái Open Issue

| ID | Chủ đề | Quyết định chốt | Lý do |
|----|--------|-----------------|-------|
| D-1 | Login identifier | Chỉ hỗ trợ `username` trong phase này | Khớp raw spec, tránh mở rộng scope và giảm mơ hồ contract |
| D-2 | Error response schema | Chuẩn hóa body lỗi cho `401/403` với các field `timestamp`, `status`, `code`, `message`, `path` | Đủ cho FE xử lý, logging, E2E test; tránh body lỗi tùy hứng |
| D-3 | OpenAPI ở production | Disable hoàn toàn cả Swagger UI và `/v3/api-docs` ở production | An toàn mặc định, ít rủi ro security hơn |
| D-4 | CORS origins | Dev dùng allowlist local cố định; staging/prod bắt buộc cấu hình allowlist explicit, không cho wildcard | Đủ chặt cho cross-site + credentials |
| D-5 | Cookie name/domain strategy | Cookie name không là public contract; FE coi là opaque. Domain attribute bị bỏ qua (host-only cookie) trong phase này | Giảm coupling FE/BE và giảm rủi ro cookie lan sai domain |
| D-6 | Logout khi session không hợp lệ | Logout là idempotent: nếu không có session hợp lệ thì vẫn trả thành công và clear cookie nếu có | Giảm phức tạp FE, không cần phân nhánh lỗi logout vô ích |
| D-7 | `roles` trong login response | Không phải field bắt buộc của contract phase này | Tránh khóa cứng contract vào phân quyền đang out of scope |
| D-8 | Endpoint bootstrap auth state | Không bổ sung `/me` hoặc `/session` trong phase này; FE bootstrap bằng `GET /api/v1/auth/csrf` | Không mở rộng scope, vẫn đủ để xác định trạng thái phiên |

---

## 4. Mâu thuẫn đã được xử lý

| ID | Mâu thuẫn trước đây | Cách xử lý hiện tại |
|----|----------------------|---------------------|
| C-1 | `username/password` trong raw spec vs nhãn wireframe `Username / Email` | Wireframe được sửa theo contract `Username` |
| C-2 | Chưa rõ error body | Đã chuẩn hóa schema lỗi trong spec-pack |
| C-3 | Chưa rõ `/v3/api-docs` ở prod | Đã chốt disable hoàn toàn |
| C-4 | Chưa rõ CORS origin list | Đã chốt rule allowlist theo môi trường |
| C-5 | Chưa rõ cookie name/domain | Đã chốt cookie name opaque, domain omitted |
| C-6 | Chưa rõ logout khi session không hợp lệ | Đã chốt logout idempotent |
| C-7 | `roles` là ví dụ hay contract | Đã chốt không bắt buộc |
| C-8 | Có cần endpoint bootstrap auth state riêng | Đã chốt không thêm endpoint mới |

---

## 5. Phán định về độ sẵn sàng của nguồn hiện tại

**Kết luận:** bộ nguồn hiện tại **đủ để bắt đầu implementation** cho ticket này, với điều kiện implementation và test suite tuân thủ `docs/changes/ticket-1/spec-pack.md` là nguồn đặc tả duy nhất.

Lý do:

- Contract chính của login/csrf/logout đã được chốt.
- Các điểm từng chặn FE/BE contract, security config và testability đã được quyết định rõ.
- Không còn Open Issue nào ở mức phải dừng implementation cho phase này.
