# Change Report — {{TICKET}} ({{FEATURE_NAME}})

> Audience: next engineer, reviewer, on-call.  
> Must be understandable from the body alone. Include paths to evidence.

---

## 1. Change Summary

<!-- 1-page overview: what changed, why, and what it affects. -->

**Ticket:** {{TICKET}}  
**Branch:** {{BRANCH_NAME}}  
**Date:** YYYY-MM-DD  
**Author:** (Claude-assisted)

### What changed

-

### Why

-

---

## 2. Impact Scope

| Area              | Detail |
| ----------------- | ------ |
| Files changed     |        |
| DB schema         |        |
| API contract      |        |
| Config            |        |
| Logs              |        |
| Permissions/Roles |        |

---

## 3. Review Results

### Claude self-check (`docs/changes/{{TICKET}}/self-review.md`)

- Blockers found:
- Majors found:
- All AC satisfied: Yes / No / Partial

### Codex review (if run)

- Overall verdict: Approve / Request changes
- Key findings:
  -

### Human review findings

| Finding | Severity | Action taken |
| ------- | -------- | ------------ |
|         |          |              |

---

## 4. Test Results

| Test type | Command               | Result      | Notes |
| --------- | --------------------- | ----------- | ----- |
| FE UT     | `npm test`            | PASS / FAIL |       |
| BE UT     | `./gradlew test`      | PASS / FAIL |       |
| API IT    |                       |             |       |
| E2E       | `npx playwright test` | PASS / FAIL |       |
| Black-box | manual                | PASS / FAIL |       |

Full details: `docs/changes/{{TICKET}}/test-results.md`

---

## 5. Remaining Tasks / Next Actions

| Task | Owner | Due |
| ---- | ----- | --- |
|      |       |     |

---

## 6. Rollback Procedure

<!-- Steps to undo this change if it causes a production issue. -->

1.

---

## 7. Deliverables Index

| File                                            | Purpose                               |
| ----------------------------------------------- | ------------------------------------- |
| `docs/changes/{{TICKET}}/spec-pack.md`          | Single source of truth for specs & AC |
| `docs/changes/{{TICKET}}/impl-plan.md`          | Implementation approach & steps       |
| `docs/changes/{{TICKET}}/review-checklist.md`   | Review perspectives                   |
| `docs/changes/{{TICKET}}/self-review.md`        | Claude self-check results             |
| `docs/changes/{{TICKET}}/test-plan.md`          | Test coverage plan                    |
| `docs/changes/{{TICKET}}/test-results.md`       | Test execution results                |
| `docs/changes/{{TICKET}}/blackbox-testcases.md` | Black-box test cases                  |
