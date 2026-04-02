# Source of Truth — ticket-1

> Ngày cập nhật: 2026-04-02
> Mục tiêu: xác định nguồn authoritative và ghi nhận các quyết định đã chốt để tạo `spec-pack.md` cho `ticket-1`.

## 1. Danh sách nguồn đã kiểm tra

| # | Tài liệu | Vai trò | Mức độ authoritative | Ghi chú |
|---|---|---|---|---|
| 1 | `docs/changes/auth/raw/spec.md` | Nguồn business content chi tiết duy nhất đang có trong gói | **Cao** | Là nguồn authoritative cho hành vi nghiệp vụ và contract auth được kế thừa vào `ticket-1`. |
| 2 | `docs/standards/templates/spec-pack.template.md` | Nguồn cấu trúc/format bắt buộc | **Cao** | Authoritative cho bố cục Spec Pack, không authoritative cho business content. |
| 3 | `docs/changes/ticket-1/raw/` | Vị trí input theo convention của ticket | **Có thư mục nhưng chưa có raw content riêng** | Được giữ để chuẩn hóa cấu trúc; nội dung raw hiện được tham chiếu từ nguồn `auth/raw/spec.md`. |

## 2. Quyết định chốt về nguồn

1. **Mã thay đổi canonical của gói đặc tả này là `ticket-1`.**
2. `docs/changes/auth/raw/spec.md` được chấp nhận là **nguồn business-content authoritative hiện hành** cho `ticket-1`.
3. `AUTH-001` được xem là story gốc làm phát sinh nội dung nghiệp vụ; `ticket-1` là mã đóng gói/triển khai hiện tại.
4. `docs/standards/templates/spec-pack.template.md` là nguồn authoritative cho cấu trúc section, bảng, traceability và mức chi tiết tài liệu.

## 3. Quy tắc ưu tiên khi có mâu thuẫn

1. Quyết định đã được chốt bởi người dùng trong lượt hiện tại có ưu tiên cao nhất.
2. `docs/changes/auth/raw/spec.md` là nguồn authoritative cho **business intent / functional behavior**.
3. `docs/standards/templates/spec-pack.template.md` là nguồn authoritative cho **format tài liệu**.
4. Trong `auth/raw/spec.md`, ưu tiên nội bộ như sau:
   - Phần **Open Issues đã chốt**
   - **Functional Specification** và **Business Rules**
   - **Acceptance Criteria**
   - **Business Flows / Frontend Integration Notes / Wireframe**

## 4. Mâu thuẫn / điểm không nhất quán đã phát hiện và cách xử lý

| ID | Mô tả | Nguồn liên quan | Ảnh hưởng | Cách xử lý đã chốt |
|---|---|---|---|---|
| SRC-1 | Không có raw content riêng trong `docs/changes/ticket-1/raw/*`, nhưng có `docs/changes/auth/raw/spec.md` | Cấu trúc thư mục vs tài liệu thực tế | Cao | Đã chốt `ticket-1` là mã canonical; `auth/raw/spec.md` là nguồn nội dung authoritative hiện hành cho ticket này. |
| SRC-2 | Formal endpoint là `/api/v1/auth/login` và `/api/v1/auth/csrf`, nhưng pseudo-flow cũ dùng `/auth/login` và `/auth/csrf` | `auth/raw/spec.md` | Cao | Spec Pack ưu tiên **formal endpoint có prefix `/api/v1`**; pseudo-flow không còn authoritative. |
| SRC-3 | UI wireframe cũ ghi `Username / Email`, trong khi request contract chỉ có trường `username` | `auth/raw/spec.md` | Cao | Đã chốt **chỉ hỗ trợ username** trong phạm vi `ticket-1`; email login ngoài phạm vi. |
| SRC-4 | Chưa có cơ chế rõ ràng để seed chỉ chạy ở `dev/test` | `auth/raw/spec.md` | Cao | Đã chốt seed nằm ở **Flyway location riêng cho non-prod** và chỉ được nạp ở profile `dev/test`. |
| SRC-5 | Chưa có danh sách origin cụ thể cho cross-site | `auth/raw/spec.md` và quyết định người dùng | Trung bình | Đã chốt origin bắt buộc trong phạm vi ticket là **`http://localhost:5173`**; các origin khác là cấu hình mở rộng ngoài acceptance hiện tại. |
| SRC-6 | Chưa chuẩn hóa body lỗi `401/403` | `auth/raw/spec.md` | Trung bình | Đã chốt envelope lỗi JSON tối thiểu `{code,message,path,timestamp}` với bộ mã lỗi auth/CSRF rõ ràng. |
| SRC-7 | Chưa rõ `roles` trong login response là bắt buộc hay tùy chọn | `auth/raw/spec.md` | Trung bình | Đã chốt `roles` **luôn phải có mặt** trong login response thành công; nếu user không có role thì trả mảng rỗng. |
| SRC-8 | Chưa rõ có cần endpoint auth-status/session introspection riêng hay không | `auth/raw/spec.md` | Thấp | Đã chốt **không thêm endpoint auth-status riêng trong ticket này**; FE dùng `GET /api/v1/auth/csrf` để rehydrate trạng thái phiên. |

## 5. Kết luận sử dụng nguồn

Spec Pack của `ticket-1` được dựng từ nguồn business content `docs/changes/auth/raw/spec.md`, được đóng gói dưới mã thay đổi canonical `ticket-1`, và đã được cập nhật bằng các quyết định chốt mới của người dùng cho các Open Issues.

**Kết luận:** hiện không còn mâu thuẫn nguồn nào ở mức blocking đối với việc bắt đầu implementation của `ticket-1`.
