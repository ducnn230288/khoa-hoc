# Kết quả kiểm thử — auth

> Cập nhật: 2026-04-03  
> Phạm vi: FE UT, FE lint/build smoke, BE UT + API IT, E2E

---

## 1. Tóm tắt

| Suite               | Command                                              | Kết quả | Ghi chú                          |
| ------------------- | ---------------------------------------------------- | ------- | -------------------------------- |
| FE UT               | `cd my-react-app && npm test`                        | PASS    | 3 files, 10 tests pass           |
| FE lint             | `cd my-react-app && npm run lint`                    | PASS    | Không có lint error              |
| FE build            | `cd my-react-app && npm run build`                   | PASS    | Vite production build thành công |
| BE UT + API IT      | `cd demo && .\gradlew.bat test`                      | PASS    | 29 tests, 0 failures             |
| E2E browser install | `cd my-react-app && npx playwright install chromium` | PASS    | Cài Chromium cho Playwright      |
| E2E                 | `cd my-react-app && npm run test:e2e`                | PASS    | 2 tests pass                     |

## 2. Commands đã chạy

```bash
cd my-react-app && npm test
cd my-react-app && npm run build
cd demo && .\gradlew.bat test
cd my-react-app && npx playwright install chromium
cd my-react-app && npm run test:e2e
cd my-react-app && npm run lint
```

## 3. Kết quả chi tiết

### FE UT

- `LoginPage.test.tsx`: PASS
- `useAuth.test.tsx`: PASS
- `authService.test.ts`: PASS

### Backend UT / API IT

- `AuthServiceTest`: PASS
- `UserDetailsServiceImplTest`: PASS
- `GlobalAuthExceptionHandlerTest`: PASS
- `AuditLoggerTest`: PASS
- `SessionStoreModeTest`: PASS
- `AuthControllerIT`: PASS
- `AuthSessionTimeoutIT`: PASS
- `DevProfileIT`: PASS
- `StagingProfileIT`: PASS
- `ProdProfileIT`: PASS

### E2E

- `auth.spec.ts` happy path login -> reload -> logout: PASS
- `auth.spec.ts` session expired -> relogin invalid credentials: PASS

## 4. Failures đã gặp và cách xử lý

| Lần fail        | Nguyên nhân                                                                                                             | Cách xử lý                                                                                                 |
| --------------- | ----------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| FE UT run #1    | Vitest đang quét cả file Playwright và test trong `node_modules`                                                        | Cấu hình lại `vite.config.ts` với `include` chỉ vào `src/**/*.test.ts(x)` và `exclude` cho `tests/e2e/**`  |
| FE UT run #1    | DOM giữa các test không được cleanup, làm query bị duplicate elements                                                   | Thêm `cleanup()` trong `src/test/setup.ts`                                                                 |
| FE build run #1 | Type mismatch ở matcher `AuthApiError` và `vite.config.ts` đang import `defineConfig` từ `vite` thay vì `vitest/config` | Bổ sung field `message` trong expectation và chuyển `defineConfig` sang `vitest/config`                    |
| BE test run #1  | Compile fail do AssertJ `OptionalAssert` API và Mockito generic cho authorities                                         | Đổi assertion sang `hasValueSatisfying(...)` và dùng `doReturn(...).when(authentication).getAuthorities()` |
| BE test run #2  | `AuthControllerIT` preflight chỉ request `content-type`, nên response không echo `x-csrf-token`                         | Cập nhật preflight request để xin cả `content-type,x-csrf-token`                                           |
| E2E run #1      | Locator quá rộng trong Playwright strict mode                                                                           | Thu hẹp locator về `role-chip` và heading cụ thể                                                           |

## 5. Điều chỉnh implementation phát sinh từ verification

- `demo/src/main/resources/application-dev.properties`: đổi `server.servlet.session.cookie.same-site` từ `lax` sang `none` để khớp spec và để `DevProfileIT` bảo vệ đúng AC-auth-2/v1.

## 6. Tự kiểm tra theo review checklist

- Không còn thiếu perspective nào ở FE UT / BE UT / API IT / E2E cho AC-auth-1/v1 đến AC-auth-27/v1.
- RC dễ sót đã được bù thêm:
  - RC-09 / RC-19: `DevProfileIT`, `StagingProfileIT`, `ProdProfileIT`
  - RC-21: `SessionStoreModeTest`
  - RC-23 / RC-24: `GlobalAuthExceptionHandlerTest`, `AuthControllerIT`
  - RC-27 / RC-28: FE UT + Playwright suite mới

## 7. Ghi chú còn lại

- E2E dùng backend mock ở network layer để giữ suite nhẹ và ổn định; đây là chủ đích, không phải thiếu coverage backend, vì backend contract thật đã được API IT với Testcontainers PostgreSQL bao phủ.
- Không có test browser-level để “đọc” trực tiếp cookie flags từ DevTools; thay vào đó cookie contract được chốt ở API IT qua header HTTP, ổn định và bám spec hơn.
