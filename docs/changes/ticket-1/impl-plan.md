# Implementation Plan — ticket-1 (Authentication foundation with session cookie + CSRF + PostgreSQL/Flyway)

> Tạo: 2026-04-01
> Derived from: `docs/changes/ticket-1/spec-pack.md`
> Branch: `codex2`

---

## 1. Implementation Policy

### Guardrails

- Chỉ implement các hành vi đã có trong `spec-pack.md`.
- Không thêm JWT, refresh token, remember-me, `/me`, `/session`, social login, MFA, Redis/distributed session, hay RBAC nhiều tầng.
- Không để FE phụ thuộc vào cookie name hoặc field `roles`.
- Không tạo production business endpoint mới chỉ để "demo" AC cho protected mutating requests; nếu cần chứng minh nguyên tắc chung, dùng test-scoped endpoint trong integration test.

### Chosen policy and alternatives

| # | Concern | Chosen policy | Alternative considered | Why the chosen policy fits the ACs better |
| --- | --- | --- | --- | --- |
| 1 | Session auth and endpoint behavior | Dùng Spring Security theo mô hình stateful session, nhưng expose `login/csrf/logout` qua REST controller/handler tùy biến | Dùng trực tiếp `formLogin()`/logout mặc định của Spring Security | AC yêu cầu JSON contract cố định, standard error schema, API CSRF riêng, logout idempotent khi session cũ, nên cần contract tùy biến thay vì behavior mặc định |
| 2 | CSRF token delivery | Dùng session-backed CSRF token repository và trả token qua `GET /api/v1/auth/csrf` | Dùng `CookieCsrfTokenRepository` hoặc trả CSRF ngay trong login response | Spec chốt token được cấp qua API riêng sau login và FE giữ token trong memory state |
| 3 | User persistence access | Ưu tiên SQL-first repository (`JdbcTemplate`/custom query layer) bám sát schema Flyway tối thiểu | Dùng JPA/Hibernate đầy đủ | Scope auth hiện chỉ cần lookup theo `username`, kiểm tra `password_hash`, `enabled`, và lưu vai trò ở mức data model; SQL-first ít ceremony hơn và khớp chặt với migration |
| 4 | Seed strategy | Seed bằng Flyway migration có scope riêng cho `dev/test` | Seed bằng `ApplicationRunner` hoặc chạy `V3` ở mọi môi trường rồi tự no-op | Spec yêu cầu Flyway và cấm seed ở `staging/production`; profile-specific Flyway locations dễ review và dễ test hơn |
| 5 | OpenAPI gating | Bật/tắt Springdoc bằng profile/property rõ ràng | Chặn bằng filter/security nhưng vẫn để endpoint tồn tại | AC 26-28 yêu cầu dev/staging truy cập được và production disable hoàn toàn |
| 6 | FE integration | FE chỉ bám contract `login -> csrf -> mutating request -> logout`, giữ CSRF trong memory, gửi `credentials: 'include'` | Xây thêm auth bootstrap endpoint riêng hoặc local persistence cho CSRF | Spec đã chốt bootstrap bằng `GET /api/v1/auth/csrf` và không dùng `/me`/`/session` |

### Implementation posture

- Backend-first để khóa API contract, cookie policy, error schema, DB migration, và profile config trước.
- FE chỉ consume những gì backend contract đã chốt, không suy luận thêm ngoài spec.
- Mọi hành vi nhạy cảm (`401` vs `403`, cookie attributes, CORS allowlist, seed scope, Swagger prod disable) phải có integration test hoặc profile-based verification rõ ràng.

## 2. Planned Changes

1. Bổ sung nền tảng backend cho session auth, CSRF, CORS credentials, standard error responses, Springdoc, PostgreSQL và Flyway trong app `demo`.
2. Tạo schema auth tối thiểu (`users`, `roles`, `user_roles`) và seed `admin`, `user01` chỉ cho `dev/test`.
3. Thay thế FE boilerplate trong `my-react-app` bằng auth shell tối thiểu theo wireframe và flow trong spec.
4. Bổ sung test và tài liệu để chứng minh contract login/csrf/logout, cookie policy, OpenAPI gating, CORS allowlist, logging, và rollback/verification path.

## 3. Impact Analysis

### Files / modules likely to change

| # | File / module | Change type | Notes |
| --- | --- | --- | --- |
| 1 | `demo/build.gradle` | Modify | Thêm dependency cho security, validation, JDBC, PostgreSQL, Flyway, Springdoc |
| 2 | `demo/src/main/resources/application.properties` | Modify | Tách shared defaults và binding cho profile-specific config |
| 3 | `demo/src/main/resources/application-dev.properties` | Add | Session timeout dev, local CORS allowlist, docs enabled, dev DB/Flyway config |
| 4 | `demo/src/main/resources/application-staging.properties` | Add | Session timeout staging, secure cookie, explicit CORS allowlist, docs enabled |
| 5 | `demo/src/main/resources/application-production.properties` | Add | Session timeout production, secure cookie, explicit CORS allowlist, docs disabled |
| 6 | `demo/src/main/resources/db/migration/**` | Add | `V1`, `V2` common migrations; `V3` chỉ nằm trong Flyway location của `dev/test` |
| 7 | `demo/src/main/java/com/example/demo/security/**` | Add | Security filter chain, session/cookie/CORS config, properties, entry point, access denied handler |
| 8 | `demo/src/main/java/com/example/demo/auth/**` | Add | DTO, controller, service, repository, password verification, logout/csrf logic |
| 9 | `demo/src/main/java/com/example/demo/common/error/**` | Add | Error response schema và mapping cho `401/403` |
| 10 | `demo/src/main/java/com/example/demo/openapi/**` | Add | OpenAPI config/annotation support cho auth endpoints |
| 11 | `demo/src/test/java/com/example/demo/**` | Add/Modify | Integration tests cho auth, profile behavior, seed, CORS, docs; có thể có test-only protected endpoint |
| 12 | `my-react-app/src/App.tsx` | Modify | Thay UI starter bằng auth shell tối thiểu |
| 13 | `my-react-app/src/auth/**` hoặc `my-react-app/src/lib/**` | Add | FE auth client/state/types cho login, csrf bootstrap, logout |
| 14 | `my-react-app/src/App.css` | Modify | Screen states theo wireframe/login/session-expired |
| 15 | `my-react-app/src/index.css` | Modify | Baseline layout cho app auth |
| 16 | `my-react-app/vite.config.ts` | Modify if needed | Chỉ chỉnh nếu cần wiring API base URL/dev experience; ưu tiên không che mất hành vi cross-site thật |

### APIs affected

| # | API | Impact | Notes |
| --- | --- | --- | --- |
| 1 | `POST /api/v1/auth/login` | New | JSON login bằng `username/password`, trả session cookie + body tối thiểu |
| 2 | `GET /api/v1/auth/csrf` | New | Bootstrap auth state và cấp CSRF token từ session hợp lệ |
| 3 | `POST /api/v1/auth/logout` | New | Logout idempotent khi session cũ; khác nhau theo matrix session/CSRF |
| 4 | Protected mutating requests (`POST/PUT/PATCH/DELETE`) | Behavior change | Áp policy session + CSRF; trong repo hiện tại chưa có business endpoint sẵn để retrofit |
| 5 | `/v3/api-docs`, `/swagger-ui/**` | Behavior change | Bật ở `dev/staging`, disable hoàn toàn ở `production` |

### DB / persistence impact

| # | Area | Impact | Notes |
| --- | --- | --- | --- |
| 1 | PostgreSQL datasource | New runtime dependency | App `demo` hiện chưa có datasource |
| 2 | `users` table | New | Chứa `username`, `password_hash`, `enabled`, timestamps |
| 3 | `roles` table | New | Scope phase này chỉ yêu cầu tồn tại data model |
| 4 | `user_roles` table | New | Chuẩn bị cho auth foundation, chưa kéo theo RBAC phức tạp |
| 5 | Password storage | Behavior change | Chỉ lưu hash, không lưu plaintext |
| 6 | Flyway versioning | New | Schema và seed được quản lý bằng migration có version |

### Settings / environment impact

| # | Setting group | Impact | Notes |
| --- | --- | --- | --- |
| 1 | Active profile config | New | Cần tách `dev`, `staging`, `production` rõ ràng |
| 2 | Session idle timeout | New | `dev=8h`, `staging=2h`, `production=30m` |
| 3 | Cookie attributes | New | `HttpOnly`, `SameSite=None`, `Path=/`, `Secure` theo profile, host-only cookie |
| 4 | CORS allowlist | New | Dev fixed list; staging/prod cần explicit origin list |
| 5 | OpenAPI toggles | New | Bật/tắt bằng profile/property |
| 6 | Flyway locations | New | Common migrations + seed locations cho `dev/test` |
| 7 | Datasource secrets | New | Cần cơ chế inject URL/user/password theo môi trường |

### Logs / observability impact

| # | Log area | Impact | Notes |
| --- | --- | --- | --- |
| 1 | Login success/failure | New | Không log plaintext password |
| 2 | Session rejection (`401`) | New | Phải phân biệt rõ với CSRF rejection |
| 3 | CSRF rejection (`403`) | New | Phân biệt missing vs invalid nếu có thể |
| 4 | Logout success/idempotent outcome | New | Không log session id/token nhạy cảm |
| 5 | Migration outcome | New | Startup observability cho Flyway |
| 6 | Seed applied/skipped | New | Chứng minh `dev/test` vs `staging/production` |

### Permissions / authorization impact

| # | Area | Impact | Notes |
| --- | --- | --- | --- |
| 1 | Anonymous access | Behavior change | `POST /auth/login` luôn public; docs public chỉ ở `dev/staging` |
| 2 | Session-protected access | New | `GET /auth/csrf` yêu cầu session hợp lệ |
| 3 | Mutating request protection | New | `POST/PUT/PATCH/DELETE` protected cần session + CSRF |
| 4 | Logout policy | New | Endpoint callable cả khi session cũ, nhưng session hợp lệ vẫn phải qua CSRF rule |
| 5 | Role data | New but limited | Chỉ tồn tại ở schema/data model; không mở rộng thành role-based authorization ở phase này |

### Frontend impact

| # | Area | Impact | Notes |
| --- | --- | --- | --- |
| 1 | Login screen | Replace starter UI | Label phải là `Username`, không dùng email |
| 2 | Auth bootstrap | New | Reload app phải gọi `GET /api/v1/auth/csrf` |
| 3 | CSRF storage | New | Lưu trong memory state, không hard-code cookie name |
| 4 | Logout flow | New | Gọi `POST /api/v1/auth/logout`, đưa UI về logged-out |
| 5 | Error handling | New | `401` dẫn về login; `403` do CSRF hiển thị lỗi phù hợp |

## 4. Existing Code To Read First

- [x] `docs/changes/ticket-1/spec-pack.md` — single source of truth cho scope và AC.
- [ ] `docs/architecture/overview.md` — input được yêu cầu nhưng file đang thiếu trong repo; cần xác nhận có tài liệu thay thế hay chấp nhận vắng mặt.
- [x] `docs/standards/templates/impl-plan.template.md` — baseline structure cho output.
- [x] `demo/build.gradle` — backend hiện chỉ có `spring-boot-starter-web`.
- [x] `demo/src/main/resources/application.properties` — hiện chưa có profile, datasource, security hay Flyway config.
- [x] `demo/src/main/java/com/example/demo/DemoApplication.java` — package root và auto-config scope hiện tại.
- [x] `demo/src/test/java/com/example/demo/DemoApplicationTests.java` — test baseline hiện chỉ là context load.
- [x] `my-react-app/package.json` — FE dùng React 19 + Vite, chưa có auth stack.
- [x] `my-react-app/vite.config.ts` — chưa có proxy hay env-specific API wiring.
- [x] `my-react-app/src/main.tsx` — app bootstrap tối giản.
- [x] `my-react-app/src/App.tsx` — Vite starter UI, sẽ bị thay bằng auth shell.
- [x] `my-react-app/src/App.css` và `my-react-app/src/index.css` — baseline style hiện là boilerplate.

## 5. Implementation Steps

| # | Step | Files touched | Verification |
| --- | --- | --- | --- |
| 1 | Thêm dependency backend cho Spring Security, validation, JDBC, PostgreSQL, Flyway, Springdoc | `demo/build.gradle` | `./gradlew.bat test` vẫn chạy context smoke sau khi thêm dependency |
| 2 | Tách profile config và typed properties cho datasource, timeout, cookie, CORS, docs, Flyway locations | `demo/src/main/resources/application*.properties`, `demo/src/main/java/com/example/demo/security/**` | App boot được với `dev`, `staging`, `production` và bind đúng property |
| 3 | Tạo migration nền `V1__init_schema.sql` và `V2__create_auth_tables.sql` | `demo/src/main/resources/db/migration/**` | Fresh PostgreSQL schema migrate thành công và có `users`, `roles`, `user_roles` |
| 4 | Tạo `V3__seed_dev_test_users.sql` theo location chỉ dành cho `dev/test`, dùng hash tĩnh cho seed accounts | `demo/src/main/resources/db/migration/**` | `dev/test` có `admin`, `user01`; `staging/production` không có seed |
| 5 | Implement repository/service lookup user theo `username`, kiểm tra `enabled`, và password encoder | `demo/src/main/java/com/example/demo/auth/**` | Test lookup đúng cho user enabled/disabled/unknown và password hash hợp lệ |
| 6 | Tạo standard error response model và central handlers cho `401/403` với mapping `AUTH_*` | `demo/src/main/java/com/example/demo/common/error/**`, `demo/src/main/java/com/example/demo/security/**` | Response lỗi luôn có `timestamp/status/code/message/path` |
| 7 | Cấu hình security filter chain cho stateful session, session timeout, cookie policy, public/protected route rules | `demo/src/main/java/com/example/demo/security/**` | Login tạo session; endpoint CSRF yêu cầu session; mutating protected flow đi qua auth/CSRF rule |
| 8 | Implement `POST /api/v1/auth/login` đúng request/response contract và login failure behavior | `demo/src/main/java/com/example/demo/auth/**` | Login success trả `200` + cookie + body tối thiểu; login fail trả `401` schema chuẩn |
| 9 | Implement `GET /api/v1/auth/csrf` làm API cấp token và bootstrap auth state | `demo/src/main/java/com/example/demo/auth/**` | Có session thì `200` với `csrfToken/headerName/parameterName`; không có session thì `401 AUTH_SESSION_REQUIRED` |
| 10 | Implement `POST /api/v1/auth/logout` theo behavior matrix và clear/expire cookie | `demo/src/main/java/com/example/demo/auth/**`, `demo/src/main/java/com/example/demo/security/**` | Valid session + valid CSRF => `200`; stale session => `200`; valid session + bad/missing CSRF => `403` |
| 11 | Áp explicit CORS allowlist + credentials trong security layer, không dùng wildcard | `demo/src/main/java/com/example/demo/security/**`, `application*.properties` | Allowed origin nhận response hợp lệ; disallowed origin không nhận wildcard/allow-origin hợp lệ |
| 12 | Thêm Springdoc/OpenAPI config và mô tả tối thiểu cho login/csrf/logout; disable hoàn toàn ở prod | `demo/build.gradle`, `demo/src/main/java/com/example/demo/openapi/**`, `application*.properties` | `dev/staging` truy cập được docs; `production` không expose `/v3/api-docs` hay `/swagger-ui/**` |
| 13 | Bổ sung business/operational logging cho login, session reject, CSRF reject, logout, migration, seed | `demo/src/main/java/com/example/demo/auth/**`, `demo/src/main/java/com/example/demo/security/**`, startup hooks nếu cần | Log có đủ tín hiệu vận hành nhưng không lộ password/session id/csrf token |
| 14 | Viết backend integration tests, gồm test-scoped protected mutating endpoint để chứng minh rule chung mà không thêm production API ngoài spec | `demo/src/test/java/com/example/demo/**` | `./gradlew.bat test` bao phủ login/cookie/csrf/logout/error/CORS/docs/seed/profile behavior |
| 15 | Tạo FE auth client/state tối thiểu cho `login -> csrf bootstrap -> logout`, luôn gửi `credentials: 'include'` và giữ CSRF trong memory | `my-react-app/src/auth/**` hoặc `my-react-app/src/lib/**`, `my-react-app/src/App.tsx` | FE build được và request shape bám đúng contract backend |
| 16 | Thay Vite starter bằng auth shell theo wireframe: login, session-expired, CSRF error, logout | `my-react-app/src/App.tsx`, `my-react-app/src/App.css`, `my-react-app/src/index.css` | `npm run lint && npm run build`; manual flow đúng label/behavior trong spec |

## 6. Risks & Mitigations

| # | Risk | Mitigation |
| --- | --- | --- |
| 1 | `demo` đang dùng `Spring Boot 3.5.12-SNAPSHOT`, có thể phát sinh lệch version với Springdoc/security stack | Chốt dependency matrix sớm ở Step 1 và chạy context smoke trước khi làm sâu |
| 2 | Cross-site cookie behavior ở local HTTP có thể không phản ánh staging/prod HTTPS | Giữ dev expectation tách biệt, verify thật trên staging với HTTPS và allowlist đúng |
| 3 | Seed data vô tình chạy ở môi trường ngoài `dev/test` | Dùng Flyway locations theo profile, thêm test khẳng định seed absent ở `staging/production` |
| 4 | Mapping `401` vs `403` trong Spring Security dễ bị trộn nếu dùng default handler | Centralize error handling từ Step 6 và có integration test cho từng case missing session / missing CSRF / invalid CSRF |
| 5 | Không có `docs/architecture/overview.md` nên thiếu một nguồn context được yêu cầu | Ghi nhận thành open issue trong checklist; không tự suy diễn kiến trúc ngoài repo hiện có |
| 6 | Repo chưa có business protected endpoint để chứng minh AC 17-21 theo production path | Dùng test-scoped protected mutating endpoint trong integration test, không mở rộng production scope |

## 7. Rollback Procedure

1. Roll back FE deployment trước để client ngừng phụ thuộc vào auth flow mới nếu xảy ra sự cố giao diện hoặc contract.
2. Roll back backend artifact về bản trước ticket này; nếu cần, tạm disable profile config mới thay vì chỉnh tay từng endpoint trong production.
3. Với database, ưu tiên restore snapshot hoặc forward-fix hơn là xóa tay migration vì Flyway SQL migration không tự rollback. Chỉ drop `users/roles/user_roles` ở môi trường không có dữ liệu phụ thuộc và đã xác nhận an toàn.
4. Nếu seed credentials vô tình xuất hiện ở shared env, rotate/cleanup environment đó ngay cả khi app đã rollback.

## 8. Verification Procedure

### Automated

```bash
# Backend
cd demo
.\gradlew.bat test

# Frontend
cd ..\my-react-app
npm run lint
npm run build
```

### Manual / profile-based checks

1. Chạy backend với `--spring.profiles.active=dev` và xác nhận login thành công trả cookie có `HttpOnly`, `SameSite=None`, `Path=/`, không có `Domain`.
2. Chạy backend với `staging` và `production` để xác nhận timeout đúng, cookie `Secure=true`, CORS allowlist là explicit, và docs chỉ bật ở `staging`.
3. Gọi `GET /api/v1/auth/csrf` trước login để nhận `401 AUTH_SESSION_REQUIRED`; sau login thành công phải nhận `200` với `csrfToken`, `headerName`, `parameterName`.
4. Gọi logout theo đủ 3 case trong behavior matrix: valid session + valid CSRF, stale session, valid session + missing/invalid CSRF.
5. Dùng origin nằm ngoài allowlist để xác nhận backend không trả wildcard hay allow-origin hợp lệ khi credentials được bật.
6. Kiểm tra fresh database migration và seed outcome ở `dev/test` so với `staging/production`.
7. Xác nhận log có login success/failure, session reject, CSRF reject, logout success, migration outcome, seed applied/skipped và không lộ dữ liệu nhạy cảm.
8. Từ FE, xác nhận flow `login -> csrf bootstrap -> reload -> csrf bootstrap lại -> logout` hoạt động mà không cần `/me` hoặc `/session`.

## 9. Pre-Implementation Confirmation Checklist

- [ ] Xác nhận `docs/architecture/overview.md` thực sự không có và có thể xem là input thiếu; nếu có tài liệu thay thế, cung cấp link/path trước khi code.
- [ ] Xác nhận scope ticket chỉ bao phủ repo hiện tại (`demo` + `my-react-app`), không có service/module ngoài repo cần đồng bộ auth cùng lúc.
- [ ] Xác nhận việc giữ `Spring Boot 3.5.12-SNAPSHOT` và Java 25 là chủ đích; nếu không, cần chốt baseline version trước khi thêm Spring Security/Springdoc.
- [ ] Cung cấp PostgreSQL connection details và cơ chế secret/config injection cho `dev`, `staging`, `production`.
- [ ] Cung cấp explicit frontend origins cho CORS allowlist ở `staging` và `production`.
- [ ] Xác nhận local dev có chấp nhận khác biệt hành vi cookie so với HTTPS thật ở staging/prod hay cần môi trường local HTTPS riêng.
- [ ] Xác nhận không có business endpoint production nào khác cần được retrofit auth/CSRF trong ticket này; repo hiện chưa có endpoint như vậy.
- [ ] Xác nhận format password hash seed accounts sẽ được commit dưới dạng hash tĩnh trong migration, không generate plaintext ở runtime.
- [ ] Xác nhận cách public Swagger ở `staging`: public trong perimeter nội bộ hay public Internet; spec chỉ chốt enable/disable, chưa chốt perimeter truy cập.
- [ ] Xác nhận deployment pipeline có chỗ đặt profile-specific values cho timeout, origins, datasource, và OpenAPI toggles.

---

## AC Mapping Table

| # | AC | Step(s) that satisfy it | How to verify |
| --- | --- | --- | --- |
| 1 | AC-auth-session-csrf-1/v1 | 5, 7, 8, 14 | IT gửi body chỉ gồm `username/password` vào `POST /api/v1/auth/login` |
| 2 | AC-auth-session-csrf-2/v1 | 5, 7, 8, 14 | IT login success trả `200`, tạo session, có `Set-Cookie` |
| 3 | AC-auth-session-csrf-3/v1 | 8, 14 | Assert body login success có `authenticated=true` và `username` |
| 4 | AC-auth-session-csrf-4/v1 | 8, 15, 16 | FE/manual verify không phụ thuộc `roles`; response contract test bỏ qua field bổ sung |
| 5 | AC-auth-session-csrf-5/v1 | 6, 8, 14 | IT login sai credential trả `401 AUTH_INVALID_CREDENTIALS` và không tạo session |
| 6 | AC-auth-session-csrf-6/v1 | 5, 6, 8, 14 | IT login user `enabled=false` trả cùng `401 AUTH_INVALID_CREDENTIALS` |
| 7 | AC-auth-session-csrf-7/v1 | 7, 8, 14 | Inspect `Set-Cookie` có `HttpOnly` |
| 8 | AC-auth-session-csrf-8/v1 | 7, 8, 14 | Inspect `Set-Cookie` có `SameSite=None` |
| 9 | AC-auth-session-csrf-9/v1 | 7, 8, 14 | Inspect `Set-Cookie` có `Path=/` |
| 10 | AC-auth-session-csrf-10/v1 | 2, 7, 8, 14 | Run profile `staging/production` và inspect `Set-Cookie` có `Secure` |
| 11 | AC-auth-session-csrf-11/v1 | 7, 8, 14 | Inspect `Set-Cookie` không có `Domain` |
| 12 | AC-auth-session-csrf-12/v1 | 2, 7, 14 | Profile-based IT assert timeout dev/staging/production |
| 13 | AC-auth-session-csrf-13/v1 | 7, 9, 14 | IT `GET /api/v1/auth/csrf` chỉ thành công khi có session hợp lệ |
| 14 | AC-auth-session-csrf-14/v1 | 9, 14 | Assert response CSRF có `csrfToken`, `headerName`, `parameterName` |
| 15 | AC-auth-session-csrf-15/v1 | 6, 7, 9, 14 | IT `GET /auth/csrf` không có/expired session trả `401 AUTH_SESSION_REQUIRED` |
| 16 | AC-auth-session-csrf-16/v1 | 9, 15, 16 | Manual/FE flow verify reload app bootstrap qua `GET /auth/csrf` |
| 17 | AC-auth-session-csrf-17/v1 | 7, 14 | Test-scoped protected mutating endpoint yêu cầu session hợp lệ |
| 18 | AC-auth-session-csrf-18/v1 | 7, 9, 14 | IT protected mutating endpoint chấp nhận header `X-CSRF-TOKEN` |
| 19 | AC-auth-session-csrf-19/v1 | 6, 7, 14 | IT protected mutating endpoint thiếu session trả `401 AUTH_SESSION_REQUIRED` |
| 20 | AC-auth-session-csrf-20/v1 | 6, 7, 14 | IT protected mutating endpoint thiếu CSRF trả `403 AUTH_CSRF_REQUIRED` |
| 21 | AC-auth-session-csrf-21/v1 | 6, 7, 14 | IT protected mutating endpoint CSRF sai/hết hạn trả `403 AUTH_CSRF_INVALID` |
| 22 | AC-auth-session-csrf-22/v1 | 7, 10, 14 | IT xác nhận tồn tại `POST /api/v1/auth/logout` |
| 23 | AC-auth-session-csrf-23/v1 | 7, 10, 14 | IT logout valid session + valid CSRF trả `200`, invalidate session, clear cookie |
| 24 | AC-auth-session-csrf-24/v1 | 10, 14 | IT logout stale session vẫn trả `200` và clear/expire cookie |
| 25 | AC-auth-session-csrf-25/v1 | 6, 7, 10, 14 | IT logout valid session + missing/invalid CSRF trả `403` và session còn nguyên |
| 26 | AC-auth-session-csrf-26/v1 | 2, 12, 14 | Run `dev` và `staging`, verify `/v3/api-docs` và Swagger UI truy cập được |
| 27 | AC-auth-session-csrf-27/v1 | 2, 12, 14 | Run `production`, verify docs endpoints bị disable hoàn toàn |
| 28 | AC-auth-session-csrf-28/v1 | 12, 14 | Inspect generated OpenAPI doc/UI có login, csrf, logout, sample request/response, cookie + CSRF notes |
| 29 | AC-auth-session-csrf-29/v1 | 1, 3, 5, 14 | Migrate PostgreSQL và assert password lưu ở `password_hash`, không có plaintext |
| 30 | AC-auth-session-csrf-30/v1 | 3, 14 | Fresh DB migrate tạo `users`, `roles`, `user_roles` |
| 31 | AC-auth-session-csrf-31/v1 | 2, 4, 14 | Profile-based test assert seed chỉ có ở `dev/test` |
| 32 | AC-auth-session-csrf-32/v1 | 2, 11, 14 | CORS IT/manual verify explicit allowlist, credentials=true, không dùng `*` |
| 33 | AC-auth-session-csrf-33/v1 | 4, 13, 14 | Review log output và integration/manual checks cho login/logout/session/CSRF/migration/seed |
| 34 | AC-auth-session-csrf-34/v1 | 6, 8, 9, 10, 14 | IT assert mọi `401/403` trong auth flow đều theo schema chuẩn |
