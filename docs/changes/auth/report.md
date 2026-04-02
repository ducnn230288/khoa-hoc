# Báo cáo thay đổi — auth (Authentication bằng Session Cookie + CSRF API riêng)

> Tạo: 2026-04-03  
> Đối tượng đọc: kỹ sư tiếp theo, người review, người trực on-call  
> Mục tiêu: chỉ cần đọc tài liệu này vẫn nắm được thay đổi chính, ảnh hưởng, mức độ sẵn sàng và rủi ro còn lại.

---

## Nguồn đã dùng để tổng hợp

Tất cả input bắt buộc cho ticket này đều hiện có và đã được đọc:

- `docs/changes/auth/spec-pack.md`
- `docs/changes/auth/impl-plan.md`
- `docs/changes/auth/review-checklist.md`
- `docs/changes/auth/self-review.md`
- `docs/changes/auth/test-plan.md`
- `docs/changes/auth/test-results.md`
- `docs/changes/auth/blackbox-testcases.md`
- `docs/changes/auth/test-data.md`

Các loại evidence **không có artifact riêng** trong gói hiện tại và sẽ được ghi là `missing.` ở phần liên quan nếu cần phân loại sâu hơn:

- Claude self-check riêng: `missing.`
- Human review findings riêng: `missing.`
- Black-box execution results riêng, tách biệt khỏi `test-results.md`: `missing.`

---

## 1. Modification summary

Ticket `auth` dựng nền tảng xác thực theo mô hình **server-side session** thay cho hướng token-based trong phạm vi ticket. Mục tiêu của thay đổi này không chỉ là thêm form đăng nhập, mà là chốt luôn contract backend/frontend cho các phần cốt lõi của auth foundation: **login bằng `username/password`, duy trì trạng thái bằng session cookie, cấp CSRF token qua API riêng, logout hủy session hiện tại, lưu auth data trong PostgreSQL qua Flyway, và công bố API bằng Springdoc theo từng môi trường**.  
Evidence: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`

Ở mức hành vi, contract đã được chốt như sau:

- `POST /api/v1/auth/login` tạo session server-side và trả body `{authenticated, username, roles}`; `roles` luôn là mảng và không được omit.
- `GET /api/v1/auth/csrf` là cơ chế **rehydrate** phiên sau reload, thay cho việc thêm `/auth/me` hoặc `/auth/status`.
- `POST /api/v1/auth/logout` là protected endpoint; muốn logout thành công phải có cả **session hợp lệ** và **CSRF token hợp lệ**.
- Các protected mutating requests phải map lỗi rõ ràng: thiếu/hết hạn session trả `401`, thiếu/sai CSRF trả `403`, tất cả đều theo error envelope chuẩn `{code,message,path,timestamp}`.

Evidence: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/blackbox-testcases.md`, `docs/changes/auth/test-data.md`

Ở mức implementation plan, gói thay đổi này tác động đồng thời lên backend, DB, config và frontend:

- Backend thêm auth module với security config, controller/service, JPA entities/repositories, error handling và audit logging.
- DB thêm schema tối thiểu `users`, `roles`, `user_roles`; seed `admin`, `user01` chỉ chạy ở `dev/test`.
- Config thêm session timeout theo môi trường (`dev=8h`, `staging=2h`, `prod=30m`), cookie policy, CORS allowlist cho `http://localhost:5173`, Flyway locations và Springdoc policy.
- Frontend thêm login UI chỉ dùng `Username` + `Password`, `authService`, `useAuth`, rehydrate bằng `GET /api/v1/auth/csrf`, và logout flow.

Evidence: `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md`

Tổng thể, đây là một gói thay đổi **có biên độ ảnh hưởng rộng nhưng phạm vi chức năng vẫn được giữ chặt**: không thêm JWT, refresh token, email login, Redis session store, social login, MFA hay endpoint auth-status riêng. Điều đó giúp ticket hoàn thành mục tiêu nền tảng auth mà không nở scope sang RBAC hay identity profile đầy đủ.  
Evidence: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/review-checklist.md`

Về trạng thái sẵn sàng, kết quả test mới nhất cho thấy **FE UT, FE lint/build, BE UT + API IT, và E2E đều PASS**. Tuy vậy, vẫn còn một lưu ý vận hành: **browser local HTTP có thể không giữ được cookie theo tổ hợp `SameSite=None` + `Secure=false` trong dev**, nên cần một vòng smoke thật trên HTTPS local hoặc môi trường staging-like để xác nhận browser behavior end-to-end bằng topology thật. Điều này là rủi ro môi trường đã được nêu từ spec, không phải dấu hiệu implementation backend bị hỏng.  
Evidence: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/self-review.md`, `docs/changes/auth/test-results.md`

---

## 2. Impact analysis

| Khu vực | Mức ảnh hưởng | Nội dung chính | Evidence |
| --- | --- | --- | --- |
| Files / modules | Cao | Thêm auth module backend, migration/seed Flyway, profile properties, frontend auth service/hook/login page và cập nhật app flow. Các khu vực chính được nêu trong plan gồm `build.gradle`, `application*.properties`, `db/migration`, `db/seed`, `SecurityConfig`, `AuthController`, `AuthService`, `GlobalAuthExceptionHandler`, `AuditLogger`, `LoginPage.tsx`, `authService.ts`, `useAuth.ts`, `App.tsx`. | `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md` |
| Database | Cao | Thêm schema auth tối thiểu trên PostgreSQL: `users`, `roles`, `user_roles`. Flyway chịu trách nhiệm khởi tạo schema; seed non-prod nạp `admin`, `user01` với password hash, và không được nạp ở `staging/prod`. | `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md`, `docs/changes/auth/test-data.md` |
| API | Cao | Tạo mới 3 endpoint auth: `POST /api/v1/auth/login`, `GET /api/v1/auth/csrf`, `POST /api/v1/auth/logout`. Chuẩn hóa error envelope cho các lỗi `401/403`, chốt `roles` luôn là mảng và dùng `GET /api/v1/auth/csrf` để rehydrate thay vì auth-status endpoint riêng. | `docs/changes/auth/spec-pack.md`, `docs/changes/auth/blackbox-testcases.md`, `docs/changes/auth/test-data.md` |
| Settings / runtime config | Cao | Thêm timeout theo môi trường (`8h/2h/30m`), cookie policy (`HttpOnly`, `SameSite=None`, `Path=/`, `Secure=true` ở `staging/prod`), CORS allowlist cho `http://localhost:5173`, Springdoc enable/disable theo profile, và tách Flyway common/non-prod locations. | `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md` |
| Logs / audit | Trung bình | Bổ sung audit log tối thiểu cho login success/failure, logout, unauthorized access, session expired, CSRF denied. Yêu cầu không log plaintext password, full session id hay full CSRF token. | `docs/changes/auth/review-checklist.md`, `docs/changes/auth/self-review.md`, `docs/changes/auth/test-data.md` |
| Permissions / roles | Trung bình | Chỉ user `enabled=true` mới login được; response login trả `roles` từ DB và cho phép `[]`; protected mutating endpoints yêu cầu session + CSRF. RBAC trên business APIs khác vẫn ngoài scope. | `docs/changes/auth/spec-pack.md`, `docs/changes/auth/review-checklist.md`, `docs/changes/auth/blackbox-testcases.md` |
| Operations / deploy | Trung bình | Đây là thay đổi forward-only ở mức schema trong plan hiện tại; chưa có Flyway down migration riêng. Rollback dựa vào revert mã nguồn + xử lý DB phù hợp từng môi trường. | `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md` |

### Kết luận impact

Đây là gói thay đổi **nền tảng** hơn là một thay đổi cục bộ. Ảnh hưởng lớn nhất nằm ở bốn điểm: contract auth mới giữa FE/BE, schema auth mới trong PostgreSQL, chính sách session/cookie/CORS theo môi trường, và audit/error handling tiêu chuẩn hóa. Vì vậy mọi thay đổi tiếp theo đụng tới identity, business APIs protected, hoặc topology FE/BE đều sẽ phụ thuộc vào ticket này.  
Evidence: `docs/changes/auth/spec-pack.md`, `docs/changes/auth/impl-plan.md`

---

## 3. Review results

### 3.1. Claude self-check

- Artifact riêng cho Claude self-check: `missing.`
- Không có tài liệu nào trong gói hiện tại cho thấy một vòng tự review độc lập do Claude thực hiện.

### 3.2. Codex findings

Nguồn gần nhất và đầy đủ nhất là `docs/changes/auth/self-review.md` và phần “Failures đã gặp và cách xử lý” trong `docs/changes/auth/test-results.md`.

**Kết luận tổng quan của Codex tại thời điểm self-review:**

- Tất cả AC từ `AC-auth-1/v1` đến `AC-auth-27/v1` được đánh dấu đã implement và có evidence.
- Không còn blocker theo checklist review.
- Tuy nhiên self-review vẫn chưa đánh dấu “Ready for manual review”, vì tại thời điểm đó còn một khoảng trống ở browser-level local smoke khi cookie dev bị browser chặn trong topology HTTP local.

Evidence: `docs/changes/auth/self-review.md`, `docs/changes/auth/review-checklist.md`

**Các phát hiện/chỉnh sửa chính được Codex ghi nhận và đã xử lý:**

| Nhóm phát hiện | Mức độ | Cách xử lý | Evidence |
| --- | --- | --- | --- |
| FE test config ban đầu quét nhầm Playwright files và `node_modules` | Major | Giới hạn `include/exclude` trong `vite.config.ts` để FE UT chỉ chạy `src/**/*.test.ts(x)` | `docs/changes/auth/test-results.md` |
| DOM giữa các FE tests không cleanup dẫn tới duplicate elements | Major | Thêm `cleanup()` trong `src/test/setup.ts` | `docs/changes/auth/test-results.md` |
| FE build mismatch ở matcher/type config | Major | Bổ sung field `message` trong expectation và chuyển `defineConfig` sang `vitest/config` | `docs/changes/auth/test-results.md` |
| BE test compile fail do assertion/mocking | Major | Đổi assertion sang `hasValueSatisfying(...)` và chỉnh Mockito generic handling | `docs/changes/auth/test-results.md` |
| CORS preflight test chưa xin `x-csrf-token` nên response không echo header này | Major | Cập nhật preflight request để xin cả `content-type,x-csrf-token` | `docs/changes/auth/test-results.md` |
| Playwright strict locator quá rộng | Minor | Thu hẹp locator theo `role-chip` và heading cụ thể | `docs/changes/auth/test-results.md` |
| Dev cookie policy lệch spec (`same-site=lax`) | Blocker đã xử lý | Đổi `server.servlet.session.cookie.same-site` của `dev` sang `none` để khớp spec và được `DevProfileIT` bảo vệ | `docs/changes/auth/test-results.md` |

**Phát hiện còn mở sau khi đã bù thêm automation:**

- Self-review cũ ghi thiếu FE/E2E/manual coverage đủ sâu cho local browser flow.  
- `test-results.md` mới hơn cho thấy khoảng trống này đã được **bù một phần đáng kể** bằng FE UT và Playwright E2E đều PASS.  
- Tuy nhiên bằng chứng mới cũng nói rõ E2E đang dùng **backend mock ở network layer**, còn browser-level live-cookie verification trên topology local HTTP vẫn chưa phải là evidence độc lập. Vì vậy rủi ro môi trường “dev local cookie không giữ được như mong đợi” vẫn còn mở và cần một vòng smoke trên HTTPS local hoặc staging-like.

Evidence: `docs/changes/auth/self-review.md`, `docs/changes/auth/test-results.md`

### 3.3. Human review findings

- Human review findings riêng: `missing.`
- Không có artifact nào cho thấy review thủ công đã diễn ra và kết luận gì đã được chốt.

### 3.4. Đánh giá review tổng hợp

Gói thay đổi đã qua một vòng tự review kỹ và một vòng hardening qua test failures/fixes. Phần lớn phát hiện của Codex đã được chuyển hóa thành test PASS và config/implementation corrections. Điểm còn lại không phải là bug contract đã biết trong code, mà là **độ tin cậy của browser behavior trên topology dev local**. Vì vậy trạng thái hợp lý hiện tại là: **ready cho manual review có điều kiện**, với yêu cầu reviewer/on-call phải nhìn riêng mục smoke runtime trên môi trường HTTPS local hoặc staging-like.  
Evidence: `docs/changes/auth/self-review.md`, `docs/changes/auth/test-results.md`

---

## 4. Test results

### 4.1. Tóm tắt theo nhóm

| Loại kiểm thử | Command / nguồn | Kết quả | Ghi chú |
| --- | --- | --- | --- |
| FE UT | `cd my-react-app && npm test` | PASS | 3 files, 10 tests pass (`LoginPage.test.tsx`, `useAuth.test.tsx`, `authService.test.ts`) |
| FE lint / build smoke | `cd my-react-app && npm run lint` ; `cd my-react-app && npm run build` | PASS | Không có lint error; Vite production build thành công |
| BE UT | `cd demo && .\gradlew.bat test` | PASS | Bao gồm `AuthServiceTest`, `UserDetailsServiceImplTest`, `GlobalAuthExceptionHandlerTest`, `AuditLoggerTest`, `SessionStoreModeTest` |
| API IT | `cd demo && .\gradlew.bat test` | PASS | Bao gồm `AuthControllerIT`, `AuthSessionTimeoutIT`, `DevProfileIT`, `StagingProfileIT`, `ProdProfileIT`; tổng 29 tests, 0 failures |
| E2E | `cd my-react-app && npm run test:e2e` | PASS | 2 tests pass: happy path `login -> reload -> logout` và `session expired -> relogin invalid credentials` |
| Black-box | `docs/changes/auth/blackbox-testcases.md` + traceability trong spec/test docs | PARTIAL | Bộ test case và expected data có đủ, nhưng **artifact kết quả chạy black-box riêng** là `missing.`; coverage phần lớn được hấp thụ vào API IT/E2E |

Evidence: `docs/changes/auth/test-plan.md`, `docs/changes/auth/test-results.md`, `docs/changes/auth/blackbox-testcases.md`, `docs/changes/auth/test-data.md`

### 4.2. Chi tiết bao phủ

Bộ test hiện tại bao phủ khá sát với spec:

- Backend contract: login/csrf/logout, timeout theo môi trường, error envelope, Swagger policy, session store mode, migration/profile behavior.
- Frontend contract: UI chỉ dùng `Username`, auth service/useAuth logic, build/lint và E2E cho các flow chính.
- E2E đã kiểm tra cả happy path lẫn luồng session expired + relogin invalid credentials.
- Black-box assets đã có mapping đủ cho `AC-auth-1/v1` đến `AC-auth-27/v1`, kèm test data catalogue và expected result catalogue.

Evidence: `docs/changes/auth/test-plan.md`, `docs/changes/auth/test-results.md`, `docs/changes/auth/blackbox-testcases.md`, `docs/changes/auth/test-data.md`

### 4.3. Những điểm cần hiểu đúng khi đọc kết quả test

- `BE UT` và `API IT` cùng chạy dưới `./gradlew.bat test`, nên không nên hiểu là chỉ có unit test backend; thực tế integration coverage khá sâu và có Testcontainers PostgreSQL.
- E2E PASS không đồng nghĩa đã xác nhận live-cookie behavior trên topology local HTTP thật, vì `test-results.md` nói rõ suite E2E dùng backend mock ở network layer để ổn định và nhẹ hơn.
- Vì vậy, nếu câu hỏi là “contract backend đã được chứng minh chưa?” thì câu trả lời là **có**. Nhưng nếu câu hỏi là “browser trên local HTTP đã được chứng minh end-to-end với cookie thật chưa?” thì câu trả lời là **chưa hoàn toàn**.

Evidence: `docs/changes/auth/test-results.md`, `docs/changes/auth/self-review.md`

---

## 5. Remaining issues và next actions

| # | Vấn đề còn lại | Mức độ | Vì sao còn mở | Next action đề xuất | Evidence |
| --- | --- | --- | --- | --- | --- |
| 1 | Browser local HTTP có thể không giữ được dev session cookie sau login | Major | Đây là rủi ro đã được spec nêu trước; self-review từng tái hiện `login 200` nhưng `GET /api/v1/auth/csrf` sau đó `401` trên browser local vì cookie policy/topology | Chạy smoke thật trên HTTPS local hoặc môi trường staging-like, ghi lại artifact rõ ràng cho `login -> csrf -> reload -> logout` | `docs/changes/auth/spec-pack.md`, `docs/changes/auth/self-review.md` |
| 2 | Chưa có black-box execution report riêng | Medium | Có đầy đủ test cases + expected data nhưng chưa có artifact chạy thủ công/ghi kết quả riêng ngoài `test-results.md` | Nếu reviewer cần sign-off black-box độc lập, tạo thêm bảng result theo từng case hoặc ít nhất theo cluster AC | `docs/changes/auth/blackbox-testcases.md`, `docs/changes/auth/test-data.md` |
| 3 | Human review findings chưa có | Medium | Gói hiện tại dừng ở self-review + automated test results | Sau manual review, cập nhật bổ sung mục findings/resolution trực tiếp vào tài liệu này hoặc thêm artifact review riêng | `docs/changes/auth/review-checklist.md` |
| 4 | Snapshot toolchain (`Spring Boot 3.5.12-SNAPSHOT`, `Java 25`) chưa được chuẩn hóa trong ticket này | Minor | Đây là cấu hình skeleton/repo, không phải mục tiêu của ticket auth nhưng có thể tăng biến động build/runtime | Tách thành ticket/toolchain riêng; không mở rộng scope ở ticket auth | `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md` |
| 5 | Sau hard reload/new tab nếu mất `sessionStorage`, FE có thể không hiển thị lại đầy đủ `username/roles` dù protected flow vẫn rehydrate được | Minor | Spec chủ ý không thêm `/auth/me` hay `/auth/status`, nên identity snapshot phía UI bị giới hạn | Chỉ xử lý nếu có yêu cầu sản phẩm mới; cần spec mới thay vì vá ngoài scope | `docs/changes/auth/self-review.md`, `docs/changes/auth/spec-pack.md` |

### Khuyến nghị chốt trước merge/deploy

1. Chạy một vòng smoke runtime bằng topology gần thật cho cookie/session thay vì chỉ dựa vào mock-backed E2E.
2. Nếu cần sign-off formal từ QA/reviewer, ghi thêm black-box result sheet ngắn theo cluster AC thay vì chỉ giữ testcases.
3. Không mở rộng scope ticket này sang `/auth/me`, Redis session hay toolchain cleanup; các việc đó nên đi thành ticket riêng.

---

## 6. Rollback procedure

Rollback cho gói này **không có Flyway down migration tự động** trong bộ tài liệu hiện tại, nên cần xem đây là rollback theo kiểu **revert application + xử lý DB có kiểm soát**.

### Mức application

1. Revert các commit thuộc change này hoặc redeploy artifact trước khi có auth foundation mới.  
   Evidence: `docs/changes/auth/impl-plan.md`
2. Đảm bảo frontend cũng revert các thay đổi liên quan `LoginPage`, `authService`, `useAuth`, `App.tsx` để không còn phụ thuộc vào contract auth mới.  
   Evidence: `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md`

### Mức database

3. Vì change này thêm mới các bảng auth (`users`, `roles`, `user_roles`) và seed chỉ nằm ở non-prod, rollback DB phải tùy môi trường:
   - **dev/test**: có thể dùng cách mạnh như drop/recreate DB hoặc xóa các bảng auth rồi repair metadata nếu quy trình nội bộ cho phép.
   - **staging/prod-like/shared env**: ưu tiên restore từ backup/snapshot hoặc forward-fix có kiểm soát; không nên tự ý thao tác thủ công trên `flyway_schema_history` nếu chưa có runbook chuẩn.
4. Không có business data ngoài auth foundation được nêu là phải rollback riêng trong tài liệu hiện có.  
   Evidence: `docs/changes/auth/impl-plan.md`

### Mức xác minh sau rollback

5. Kiểm tra lại ứng dụng khởi động bình thường ở profile mục tiêu.
6. Xác nhận các endpoint auth mới không còn được expose nếu đã revert toàn bộ change.
7. Xác nhận frontend không còn gọi `GET /api/v1/auth/csrf`/`POST /api/v1/auth/login` theo contract mới sau rollback.

### Ghi chú rollback

- Nếu change này đã được môi trường khác hoặc feature tiếp theo phụ thuộc vào schema/auth contract mới, rollback bằng revert thẳng có thể không còn an toàn. Khi đó nên chọn **forward fix** thay vì rollback cứng.
- Vì chưa có runbook DB chi tiết hơn trong gói hiện tại, on-call nên coi bước DB rollback là **có điều kiện**, không phải thao tác mặc định một-nút-bấm.

Evidence: `docs/changes/auth/impl-plan.md`, `docs/changes/auth/self-review.md`

---

## 7. Reference links / deliverables

| # | Deliverable | Vai trò trong báo cáo |
| --- | --- | --- |
| 1 | `docs/changes/auth/spec-pack.md` | Nguồn chuẩn để hiểu scope, contract API, cookie/CORS, timeout, error envelope, open issues và rủi ro |
| 2 | `docs/changes/auth/impl-plan.md` | Dùng để tổng hợp phạm vi ảnh hưởng, các module thay đổi, rollback và verification flow |
| 3 | `docs/changes/auth/review-checklist.md` | Dùng để phân loại góc nhìn review, severity và các tiêu chí cần sign-off |
| 4 | `docs/changes/auth/self-review.md` | Nguồn chính cho Codex findings, risk còn mở, diff summary và evidence mapping tới AC/RC |
| 5 | `docs/changes/auth/test-plan.md` | Dùng để hiểu cấu trúc bao phủ test dự kiến theo FE UT / BE UT / IT / E2E |
| 6 | `docs/changes/auth/test-results.md` | Nguồn chuẩn cho kết quả test mới nhất, failures đã gặp và các fix đã thực hiện |
| 7 | `docs/changes/auth/blackbox-testcases.md` | Danh mục black-box cases theo từng AC |
| 8 | `docs/changes/auth/test-data.md` | Catalogue dữ liệu thử, expected results và mapping data ↔ case clusters |

