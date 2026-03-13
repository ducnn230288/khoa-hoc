# Gói đặc tả — {{TICKET}} ({{FEATURE_NAME}})

> Tạo: [YYYY-MM-DD] · Giai đoạn: [N]
> **Nguồn tham chiếu duy nhất cho thay đổi này.**  
> Không triển khai bất kỳ nội dung nào không được viết ở đây. Các điểm chưa rõ → Open Issues.

---

## 1. Bối cảnh / Mục đích

<!-- Vì sao thay đổi này tồn tại? Nó giải quyết vấn đề gì? -->

## 2. Phạm vi

### Trong phạm vi

-

### Ngoài phạm vi

-

## 3. Thuật ngữ

| #   | Thuật ngữ | Định nghĩa |
| --- | --------- | ---------- |
| 1   |           |            |

## 4. Hiện trạng / Trạng thái mục tiêu

| #   | Khía cạnh | Hiện trạng | Trạng thái mục tiêu |
| --- | --------- | ---------- | ------------------- |
| 1   |           |            |                     |

## 5. Chi tiết đặc tả

<!-- Các yêu cầu chức năng. Cần đủ chính xác để kỹ sư có thể triển khai mà không phải hỏi thêm. -->

## 6. Yêu cầu phi chức năng

| #   | Danh mục          | Yêu cầu |
| --- | ----------------- | ------- |
| 1   | Hiệu năng         |         |
| 2   | Bảo mật           |         |
| 3   | Tính sẵn sàng     |         |
| 4   | Khả năng quan sát |         |

## 7. Tiêu chí chấp nhận

<!-- Mỗi AC phải là một phát biểu có thể kiểm thử được. -->

| #   | ID                | Mô tả | Loại kiểm thử |
| --- | ----------------- | ----- | ------------- |
| 1   | AC-[feature]-1/v1 |       | UT/IT/E2E/BB  |
| 2   | AC-[feature]-2/v1 |       |               |

## 8. Ví dụ

### Các luồng bình thường

1.
2.

### Các luồng lỗi

1.
2.

### Các trường hợp biên

1.
2.

## 9. Wireframe ASCII (Tùy chọn)

<!-- Chỉ bao gồm khi thay đổi này thêm mới hoặc cập nhật UI/màn hình. Bỏ qua phần này nếu chỉ làm backend. -->

### Tên màn hình / luồng

```text
+--------------------------------------------------+
| Tiêu đề màn hình                                 |
|--------------------------------------------------|
| Trường / nội dung                                |
| [ Hành động chính ]   [ Hành động phụ ]          |
+--------------------------------------------------+
```

### Ghi chú

- Các trạng thái chính:
- Thông điệp xác thực / lỗi:
- Lưu ý về responsive hoặc mobile:

## 10. Các vấn đề mở

<!-- Các mục cần được con người quyết định trước khi bắt đầu triển khai. -->

| #    | Câu hỏi | Người phụ trách | Hạn chót |
| ---- | ------- | --------------- | -------- |
| OI-1 |         |                 |          |

## 11. Rủi ro

| #   | Rủi ro | Khả năng xảy ra | Mức độ ảnh hưởng | Biện pháp giảm thiểu |
| --- | ------ | --------------- | ---------------- | -------------------- |
| 1   |        |                 |                  |                      |

---

## Bảng truy vết

| #   | AC                | Màn hình/API | DB  | Logs | Quyền | Loại kiểm thử |
| --- | ----------------- | ------------ | --- | ---- | ----- | ------------- |
| 1   | AC-[feature]-1/v1 |              |     |      |       | UT · IT · E2E |
