# Self-Review — ticket-1 (Authentication foundation with session cookie + CSRF + PostgreSQL/Flyway)

> Date: 2026-03-31
> Filled after implementation, before human review.
> Rule: every checked item must point to evidence (command output, screenshot, log excerpt, test name, file path + line range, or PR diff note).

---

## 1. Review Scope Snapshot

- Branch / commit reviewed: 
- Reviewer / agent: 
- Reviewed artifacts: backend / frontend / docs / config / migrations / tests
- Related docs:
  - `docs/changes/ticket-1/spec-pack.md`
  - `docs/changes/ticket-1/impl-plan.md`
  - `docs/changes/ticket-1/review-checklist.md`

## 2. AC Completion Tracker

| AC | Done | Partial | N/A | Evidence / Notes |
| --- | --- | --- | --- | --- |
| AC-auth-session-csrf-1/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-2/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-3/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-4/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-5/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-6/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-7/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-8/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-9/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-10/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-11/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-12/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-13/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-14/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-15/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-16/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-17/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-18/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-19/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-20/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-21/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-22/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-23/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-24/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-25/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-26/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-27/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-28/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-29/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-30/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-31/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-32/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-33/v1 | [ ] | [ ] | [ ] | |
| AC-auth-session-csrf-34/v1 | [ ] | [ ] | [ ] | |

## 3. Checklist Items (copied from `review-checklist.md`)

> Tick only when there is concrete evidence.

| RC# | Severity | Done | Evidence / Notes |
| --- | --- | --- | --- |
| RC-01 | Blocker | [ ] | |
| RC-02 | Blocker | [ ] | |
| RC-03 | Blocker | [ ] | |
| RC-04 | Blocker | [ ] | |
| RC-05 | Blocker | [ ] | |
| RC-06 | Blocker | [ ] | |
| RC-07 | Blocker | [ ] | |
| RC-08 | Major | [ ] | |
| RC-09 | Blocker | [ ] | |
| RC-10 | Major | [ ] | |
| RC-11 | Major | [ ] | |
| RC-12 | Blocker | [ ] | |
| RC-13 | Major | [ ] | |
| RC-14 | Blocker | [ ] | |
| RC-15 | Blocker | [ ] | |
| RC-16 | Blocker | [ ] | |
| RC-17 | Blocker | [ ] | |
| RC-18 | Blocker | [ ] | |
| RC-19 | Blocker | [ ] | |
| RC-20 | Blocker | [ ] | |
| RC-21 | Blocker | [ ] | |
| RC-22 | Major | [ ] | |
| RC-23 | Major | [ ] | |
| RC-24 | Major | [ ] | |
| RC-25 | Blocker | [ ] | |
| RC-26 | Blocker | [ ] | |
| RC-27 | Major | [ ] | |
| RC-28 | Blocker | [ ] | |
| RC-29 | Major | [ ] | |
| RC-30 | Major | [ ] | |
| RC-31 | Blocker | [ ] | |
| RC-32 | Blocker | [ ] | |
| RC-33 | Major | [ ] | |
| RC-34 | Blocker | [ ] | |
| RC-35 | Major | [ ] | |
| RC-36 | Major | [ ] | |
| RC-37 | Major | [ ] | |
| RC-38 | Major | [ ] | |
| RC-39 | Minor | [ ] | |

## 4. Commands Executed

### 4.1 Backend quality / lint / static checks

| # | Command | Purpose | Result | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `cd demo && ...` | | PASS / FAIL / NOT RUN | |
| 2 | `cd demo && ...` | | PASS / FAIL / NOT RUN | |

### 4.2 Backend tests

| # | Command | Scope | Result | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `cd demo && ./gradlew test` | Unit + integration | PASS / FAIL / NOT RUN | |
| 2 | `cd demo && ./gradlew test --tests ...` | Focused regression | PASS / FAIL / NOT RUN | |

### 4.3 Frontend quality / lint / build / tests

| # | Command | Scope | Result | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `cd my-react-app && npm run lint` | Lint | PASS / FAIL / NOT RUN | |
| 2 | `cd my-react-app && npm run build` | Production build | PASS / FAIL / NOT RUN | |
| 3 | `cd my-react-app && ...` | UI test / smoke | PASS / FAIL / NOT RUN | |

### 4.4 Manual / profile-based verification

| # | Scenario | Command / environment | Result | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `dev` login success + cookie attributes | | PASS / FAIL / NOT RUN | |
| 2 | `staging` cookie `Secure=true` + docs enabled | | PASS / FAIL / NOT RUN | |
| 3 | `production` docs disabled | | PASS / FAIL / NOT RUN | |
| 4 | Missing session => `401 AUTH_SESSION_REQUIRED` | | PASS / FAIL / NOT RUN | |
| 5 | Missing CSRF => `403 AUTH_CSRF_REQUIRED` | | PASS / FAIL / NOT RUN | |
| 6 | Invalid CSRF => `403 AUTH_CSRF_INVALID` | | PASS / FAIL / NOT RUN | |
| 7 | Logout stale session => `200 { success: true }` | | PASS / FAIL / NOT RUN | |
| 8 | Disallowed origin blocked by CORS | | PASS / FAIL / NOT RUN | |

## 5. Evidence Index

| # | Type | Path / Test / Screenshot / Log reference | Notes |
| --- | --- | --- | --- |
| 1 | Code | | |
| 2 | Test | | |
| 3 | Log | | |
| 4 | Screenshot | | |

## 6. Diff Overview

### 6.1 Files changed

- [ ] Backend code
- [ ] Frontend code
- [ ] Config / properties / profiles
- [ ] DB migration / seed
- [ ] Tests
- [ ] Docs / OpenAPI annotations

### 6.2 Summary of what changed

- 
- 
- 

### 6.3 Scope guardrails re-check

- [ ] Không thêm JWT / refresh token / remember-me / MFA / social login
- [ ] Không thêm `/me` hoặc `/session`
- [ ] Không thêm production business endpoint ngoài spec
- [ ] FE không phụ thuộc cookie name
- [ ] FE không phụ thuộc field `roles`

## 7. Known Risks

| # | Risk | Impact | Mitigation / follow-up | Owner |
| --- | --- | --- | --- | --- |
| 1 | | | | |
| 2 | | | | |

## 8. Not Handled Yet

- [ ] Profile-specific deploy value chưa có đủ từ môi trường thực tế
- [ ] Kiểm tra cross-site cookie trên HTTPS thật chưa chạy
- [ ] Manual verification với browser thật chưa hoàn tất
- [ ] OpenAPI examples / notes còn thiếu
- [ ] Test coverage còn thiếu cho một số nhánh lỗi
- [ ] Khác: 

## 9. Remaining Issues Before Merge

| # | Severity | Issue | Blocking? | Planned action |
| --- | --- | --- | --- | --- |
| 1 | Blocker / Major / Minor | | Yes / No | |
| 2 | Blocker / Major / Minor | | Yes / No | |

## 10. Final Self-Assessment

- [ ] Tôi đã rà lại toàn bộ AC và không thấy hành vi ngoài spec.
- [ ] Tôi đã đối chiếu `review-checklist.md` và để lại evidence cho mọi checkbox đã tick.
- [ ] Tôi đã ghi rõ các phần chưa chạy / chưa chắc chắn thay vì đánh dấu hoàn thành mơ hồ.
- [ ] Tôi xác nhận mọi Blocker còn mở đã được liệt kê trong mục “Remaining Issues Before Merge”.
