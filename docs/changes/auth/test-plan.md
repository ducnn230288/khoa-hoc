# Kế hoạch kiểm thử — auth

> Tạo: 2026-04-03  
> Nguồn chuẩn dùng để suy ra coverage: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/changes/auth/review-checklist.md`  
> Conventions dùng cho ticket này: frontend colocate `*.test.ts(x)` trong `my-react-app/src`, backend unit dùng `*Test.java`, backend integration dùng `*IT.java`, E2E dùng Playwright trong `my-react-app/tests/e2e`. Repository không có `.claude/rules` ở root nên naming/placement/mocking policy được lấy từ `docs/standards` template và test layout hiện hữu của repo.

---

## 1. Ma trận bao phủ

| #   | AC            | FE UT | BE UT | API IT | E2E | Evidence chính                                                                                                             |
| --- | ------------- | ----- | ----- | ------ | --- | -------------------------------------------------------------------------------------------------------------------------- |
| 1   | AC-auth-1/v1  | x     | x     | x      | x   | `LoginPage.test.tsx`, `useAuth.test.tsx`, `AuthServiceTest.java`, `AuthControllerIT.java`, `auth.spec.ts`                  |
| 2   | AC-auth-2/v1  |       |       | x      |     | `DevProfileIT.java`                                                                                                        |
| 3   | AC-auth-3/v1  |       |       | x      |     | `StagingProfileIT.java`                                                                                                    |
| 4   | AC-auth-4/v1  | x     | x     | x      | x   | `useAuth.test.tsx`, `AuthServiceTest.java`, `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`, `auth.spec.ts` |
| 5   | AC-auth-5/v1  |       | x     | x      |     | `UserDetailsServiceImplTest.java`, `AuthServiceTest.java`, `AuthControllerIT.java`                                         |
| 6   | AC-auth-6/v1  | x     | x     | x      | x   | `useAuth.test.tsx`, `GlobalAuthExceptionHandlerTest.java`, `AuthSessionTimeoutIT.java`, `auth.spec.ts`                     |
| 7   | AC-auth-7/v1  | x     | x     | x      | x   | `useAuth.test.tsx`, `AuthServiceTest.java`, `AuthControllerIT.java`, `auth.spec.ts`                                        |
| 8   | AC-auth-8/v1  |       | x     | x      |     | `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                                             |
| 9   | AC-auth-9/v1  |       | x     | x      |     | `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                                             |
| 10  | AC-auth-10/v1 |       | x     | x      |     | `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                                             |
| 11  | AC-auth-11/v1 | x     | x     | x      |     | `useAuth.test.tsx`, `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                         |
| 12  | AC-auth-12/v1 |       | x     | x      | x   | `AuthServiceTest.java`, `AuthControllerIT.java`, `auth.spec.ts`                                                            |
| 13  | AC-auth-13/v1 |       | x     | x      | x   | `AuthServiceTest.java`, `AuthControllerIT.java`, `auth.spec.ts`                                                            |
| 14  | AC-auth-14/v1 |       | x     | x      |     | `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                                             |
| 15  | AC-auth-15/v1 | x     | x     | x      |     | `useAuth.test.tsx`, `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                         |
| 16  | AC-auth-16/v1 |       |       | x      |     | `DevProfileIT.java`, `StagingProfileIT.java`, `ProdProfileIT.java`                                                         |
| 17  | AC-auth-17/v1 |       | x     | x      |     | `UserDetailsServiceImplTest.java`, `AuthControllerIT.java`                                                                 |
| 18  | AC-auth-18/v1 |       |       | x      |     | `AuthControllerIT.java`, `DevProfileIT.java`, `StagingProfileIT.java`, `ProdProfileIT.java`                                |
| 19  | AC-auth-19/v1 |       |       | x      |     | `AuthControllerIT.java`, `DevProfileIT.java`                                                                               |
| 20  | AC-auth-20/v1 |       |       | x      |     | `StagingProfileIT.java`, `ProdProfileIT.java`                                                                              |
| 21  | AC-auth-21/v1 |       | x     |        |     | `SessionStoreModeTest.java`                                                                                                |
| 22  | AC-auth-22/v1 |       | x     | x      |     | `AuditLoggerTest.java`, `AuthControllerIT.java`                                                                            |
| 23  | AC-auth-23/v1 |       | x     | x      |     | `GlobalAuthExceptionHandlerTest.java`, `AuthControllerIT.java`                                                             |
| 24  | AC-auth-24/v1 |       | x     | x      |     | `AuthServiceTest.java`, `UserDetailsServiceImplTest.java`, `AuthControllerIT.java`                                         |
| 25  | AC-auth-25/v1 | x     |       | x      | x   | `LoginPage.test.tsx`, `authService.test.ts`, `DevProfileIT.java`, `StagingProfileIT.java`, `auth.spec.ts`                  |
| 26  | AC-auth-26/v1 | x     |       | x      | x   | `useAuth.test.tsx`, `AuthControllerIT.java`, `auth.spec.ts`                                                                |
| 27  | AC-auth-27/v1 | x     |       | x      |     | `authService.test.ts`, `AuthControllerIT.java`                                                                             |

## 2. FE Unit Test

| #   | Tệp kiểm thử                                    | Nội dung kiểm thử                                                                                                                                                                             | AC                                                                                    |
| --- | ----------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 1   | `my-react-app/src/pages/LoginPage.test.tsx`     | Bảo vệ UI contract dễ vỡ: chỉ có `Username` + `Password`, không có `Email`, submit dùng đúng giá trị hiện tại, state `sessionExpired/pending` render đúng heading/button/message              | AC-auth-1/v1, AC-auth-4/v1, AC-auth-6/v1, AC-auth-25/v1                               |
| 2   | `my-react-app/src/hooks/useAuth.test.tsx`       | Bảo vệ state transition quan trọng: rehydrate từ session snapshot, map `AUTH_SESSION_EXPIRED`, login thành công ghi snapshot, logout bị `AUTH_CSRF_INVALID` giữ auth state nhưng hiển thị lỗi | AC-auth-1/v1, AC-auth-6/v1, AC-auth-7/v1, AC-auth-11/v1, AC-auth-15/v1, AC-auth-26/v1 |
| 3   | `my-react-app/src/services/authService.test.ts` | Bảo vệ FE contract ở network boundary: luôn dùng `credentials: 'include'`, request login chỉ gửi `username/password`, giữ nguyên metadata của error envelope, logout dùng dynamic CSRF header | AC-auth-1/v1, AC-auth-23/v1, AC-auth-25/v1, AC-auth-27/v1                             |

Trọng tâm FE UT:

- Không mock DOM state hay session storage ngoài mức cần thiết; chỉ mock network/service boundary.
- Ưu tiên state transition, exception display và request contract thay vì “chép lại implementation”.

## 3. BE Unit Test

| #   | Lớp kiểm thử                                                                        | Nội dung kiểm thử                                                                                                                 | AC                                                                                                                  |
| --- | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| 1   | `demo/src/test/java/com/example/demo/service/AuthServiceTest.java`                  | Login success/disabled/invalid credentials, CSRF DTO contract, logout invalidates current session và clear cookie đúng attributes | AC-auth-1/v1, AC-auth-4/v1, AC-auth-7/v1, AC-auth-12/v1, AC-auth-13/v1, AC-auth-24/v1                               |
| 2   | `demo/src/test/java/com/example/demo/service/UserDetailsServiceImplTest.java`       | Load user enabled/disabled/missing, authority mapping từ domain sang security principal                                           | AC-auth-5/v1, AC-auth-17/v1, AC-auth-24/v1                                                                          |
| 3   | `demo/src/test/java/com/example/demo/exception/GlobalAuthExceptionHandlerTest.java` | Map `401/403` sang envelope `{code,message,path,timestamp}`, phân biệt `session required` với `session expired`, CSRF invalid     | AC-auth-6/v1, AC-auth-8/v1, AC-auth-9/v1, AC-auth-10/v1, AC-auth-11/v1, AC-auth-14/v1, AC-auth-15/v1, AC-auth-23/v1 |
| 4   | `demo/src/test/java/com/example/demo/audit/AuditLoggerTest.java`                    | Audit events bắt buộc và redaction secret/password/session/token                                                                  | AC-auth-22/v1                                                                                                       |
| 5   | `demo/src/test/java/com/example/demo/config/SessionStoreModeTest.java`              | Guard “không vô tình kéo Spring Session/Redis vào classpath”, giữ đúng mode in-memory/default session store                       | AC-auth-21/v1                                                                                                       |

Trọng tâm BE UT:

- Mock infrastructure/security collaborators, không mock domain object một cách vô nghĩa.
- Bao phủ boundary values, exception mapping và permission/auth semantics ở mức service/handler.

## 4. Integration Test API

| #   | Endpoint / Scope            | Kịch bản                                                                                                                                                                                                         | AC                                                                                                                                                                                                                                                      |
| --- | --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `AuthControllerIT.java`     | `POST /login` success/failure/disabled, `GET /csrf` with/without session, `POST /logout` success/no-session/missing-csrf/invalid-csrf, roles `[]`, post-logout session invalidation, credentialed CORS preflight | AC-auth-1/v1, AC-auth-4/v1, AC-auth-5/v1, AC-auth-7/v1, AC-auth-8/v1, AC-auth-9/v1, AC-auth-10/v1, AC-auth-11/v1, AC-auth-12/v1, AC-auth-13/v1, AC-auth-14/v1, AC-auth-15/v1, AC-auth-22/v1, AC-auth-23/v1, AC-auth-24/v1, AC-auth-26/v1, AC-auth-27/v1 |
| 2   | `AuthSessionTimeoutIT.java` | Expire session thật ở embedded Tomcat rồi gọi lại `GET /api/v1/auth/csrf` để bắt `AUTH_SESSION_EXPIRED`                                                                                                          | AC-auth-6/v1                                                                                                                                                                                                                                            |
| 3   | `DevProfileIT.java`         | Dev expose OpenAPI, docs không có email login, login bằng seed user, cookie `SameSite=None` và không `Secure`                                                                                                    | AC-auth-2/v1, AC-auth-16/v1, AC-auth-19/v1, AC-auth-25/v1                                                                                                                                                                                               |
| 4   | `StagingProfileIT.java`     | Staging expose OpenAPI, không nạp seed non-prod, cookie có `Secure`, docs không có email login                                                                                                                   | AC-auth-3/v1, AC-auth-16/v1, AC-auth-20/v1, AC-auth-25/v1                                                                                                                                                                                               |
| 5   | `ProdProfileIT.java`        | Prod không public `/v3/api-docs` và Swagger UI                                                                                                                                                                   | AC-auth-16/v1, AC-auth-20/v1                                                                                                                                                                                                                            |

Chính sách API IT:

- Không mock backend internals.
- Chạy app thật trên random port, gọi HTTP thật, dùng PostgreSQL thật qua Testcontainers để giữ contract auth + DB.

## 5. Kiểm thử E2E (Playwright)

| #   | Kịch bản                                                                | Các bước                                                                                                                 | Kết quả mong đợi                                                                                                              | AC                                                                      |
| --- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| 1   | Happy path: login -> csrf -> reload -> logout                           | Mở app, xác nhận chỉ có field `Username/Password`, login thành công, thấy authenticated shell, reload tab, sau đó logout | UI rehydrate đúng qua `GET /api/v1/auth/csrf`, giữ flow protected sau reload và quay về trạng thái unauthenticated sau logout | AC-auth-1/v1, AC-auth-7/v1, AC-auth-13/v1, AC-auth-25/v1, AC-auth-26/v1 |
| 2   | Abnormal representative: session expired -> relogin invalid credentials | Mở app với response đầu tiên là `AUTH_SESSION_EXPIRED`, sau đó bấm relogin nhưng login fail `AUTH_INVALID_CREDENTIALS`   | UI hiển thị expired state đúng, sau đó render lại error invalid credentials rõ ràng                                           | AC-auth-4/v1, AC-auth-6/v1, AC-auth-25/v1                               |

Chính sách E2E:

- Chạy browser thật với frontend thật.
- Mock backend ở network layer để giữ suite mỏng và ổn định; contract auth/backend thật đã được API IT sở hữu.

## 6. Các lệnh chạy kiểm thử

```bash
# FE unit tests
cd my-react-app && npm test

# FE lint + build smoke
cd my-react-app && npm run lint
cd my-react-app && npm run build

# BE unit + integration tests
cd demo && .\gradlew.bat test

# E2E browser dependency
cd my-react-app && npx playwright install chromium

# E2E
cd my-react-app && npm run test:e2e
```

## 7. Ghi chú / Ràng buộc

- Không có custom client-side validation phức tạp trong scope auth hiện tại, nên FE UT tập trung vào request contract, state transitions và exception display thay vì ép thêm validation ngoài spec.
- Perspective “cookie flags/profile semantics” được giao cho API IT, không để E2E gánh vì browser assertion ở local dễ nhiễu hơn contract HTTP.
- Sau khi đối chiếu `review-checklist.md`, không còn AC nào thiếu automated perspective; phần trade-off duy nhất là E2E cố ý mock backend để giảm flakiness, đổi lại API IT đã giữ toàn bộ behavior backend + DB thật.
