# Review Checklist - {{TICKET}} ({{FEATURE_NAME}})

> Use this for implementation review after the change is coded.  
> Severity meanings: `Blocker` = must fix before merge, `Major` = fix in this change or log an accepted exception, `Minor` = improve now or log for follow-up.

---

## 1. Scope And Specification

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-01 | All acceptance criteria are implemented or explicitly deferred. | Blocker | [ ] | |
| RC-02 | No behavior outside approved scope was added without documentation. | Blocker | [ ] | |
| RC-03 | Open issues were not silently implemented as assumptions. | Blocker | [ ] | |

## 2. Architecture And Design

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-04 | Layer boundaries follow `docs/architecture/overview.md`. | Major | [ ] | |
| RC-05 | Dependency direction stays inward and avoids hidden coupling. | Major | [ ] | |
| RC-06 | New abstractions are justified by actual complexity. | Minor | [ ] | |

## 3. Coding Rules

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-07 | Rule 10 was respected. | Major | [ ] | |
| RC-08 | Rule 14 was respected at important boundaries. | Major | [ ] | |
| RC-09 | Rule 18 was respected and unrelated work was not reverted. | Blocker | [ ] | |
| RC-10 | Rule 19 was respected and common-base docs were not polluted with ticket details. | Major | [ ] | |

## 4. Security Rules

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-11 | Security-relevant defaults and config are explicit. | Blocker | [ ] | |
| RC-12 | Inputs are validated or constrained at the right boundary. | Blocker | [ ] | |
| RC-13 | No secrets or sensitive data were introduced into code, docs, logs, or samples. | Blocker | [ ] | |
| RC-14 | Authorization or protection of mutating operations is addressed where relevant. | Blocker | [ ] | |
| RC-15 | Errors and logs do not leak internal or sensitive details. | Major | [ ] | |

## 5. Testing Rules

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-16 | Verification evidence exists for the changed behavior. | Blocker | [ ] | |
| RC-17 | The smallest meaningful test layer was chosen. | Major | [ ] | |
| RC-18 | Missing or unrun tests are explicitly documented. | Blocker | [ ] | |
| RC-19 | Regression protection was added for defects when appropriate. | Major | [ ] | |
| RC-20 | AC-to-evidence traceability is visible across deliverables. | Major | [ ] | |

## 6. Operations And Rollback

| ID | Check | Severity | Status | Evidence |
| -- | ----- | -------- | ------ | -------- |
| RC-21 | Config and environment impacts are documented. | Major | [ ] | |
| RC-22 | Rollback procedure is realistic for this change. | Major | [ ] | |
| RC-23 | Remaining risks are stated clearly. | Major | [ ] | |

