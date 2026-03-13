# Review Checklist — {{TICKET}} ({{FEATURE_NAME}})

> Severity: **Blocker** = must fix before merge | **Major** = fix in this PR | **Minor** = fix or log as tech-debt

---

## 1. Spec / AC

| #     | Item                                                            | Severity | Status |
| ----- | --------------------------------------------------------------- | -------- | ------ |
| RC-01 | All AC are implemented and demonstrably satisfied               | Blocker  | [ ]    |
| RC-02 | No behaviour outside spec-pack scope was added                  | Blocker  | [ ]    |
| RC-03 | Open Issues are still listed; none implemented without approval | Blocker  | [ ]    |

## 2. Design / Dependencies

| #     | Item                                                              | Severity | Status |
| ----- | ----------------------------------------------------------------- | -------- | ------ |
| RC-04 | Layer boundaries respected (no domain logic in controllers, etc.) | Major    | [ ]    |
| RC-05 | No circular dependencies introduced                               | Major    | [ ]    |
| RC-06 | Public API / contract matches spec                                | Blocker  | [ ]    |

## 3. Security

| #     | Item                                                  | Severity | Status |
| ----- | ----------------------------------------------------- | -------- | ------ |
| RC-07 | All user inputs validated and sanitised               | Blocker  | [ ]    |
| RC-08 | Authorization checked on every endpoint/action        | Blocker  | [ ]    |
| RC-09 | No secrets, keys, or PII in logs, diffs, or responses | Blocker  | [ ]    |
| RC-10 | No SQL injection / XSS / command injection risk       | Blocker  | [ ]    |

## 4. Performance

| #     | Item                                                     | Severity | Status |
| ----- | -------------------------------------------------------- | -------- | ------ |
| RC-11 | No N+1 query introduced                                  | Major    | [ ]    |
| RC-12 | No unnecessary large payload or blocking I/O on hot path | Major    | [ ]    |

## 5. Compatibility

| #     | Item                                     | Severity | Status |
| ----- | ---------------------------------------- | -------- | ------ |
| RC-13 | Existing APIs remain backward compatible | Blocker  | [ ]    |
| RC-14 | DB migration is reversible               | Major    | [ ]    |

## 6. Logging / Audit

| #     | Item                                             | Severity | Status |
| ----- | ------------------------------------------------ | -------- | ------ |
| RC-15 | Key operations (create/update/delete) are logged | Major    | [ ]    |
| RC-16 | Log messages contain no PII                      | Blocker  | [ ]    |

## 7. Error Handling

| #     | Item                                                            | Severity | Status |
| ----- | --------------------------------------------------------------- | -------- | ------ |
| RC-17 | All exceptions are caught and return meaningful error responses | Major    | [ ]    |
| RC-18 | Client receives no internal stack trace                         | Major    | [ ]    |

## 8. Tests

| #     | Item                                                       | Severity | Status |
| ----- | ---------------------------------------------------------- | -------- | ------ |
| RC-19 | FE unit tests cover form validation and state transitions  | Major    | [ ]    |
| RC-20 | BE unit tests cover boundary values and exceptions         | Major    | [ ]    |
| RC-21 | API integration tests cover auth + happy path + error path | Major    | [ ]    |
| RC-22 | E2E test covers the main user story (normal + key error)   | Major    | [ ]    |

## 9. Operations

| #     | Item                                                    | Severity | Status |
| ----- | ------------------------------------------------------- | -------- | ------ |
| RC-23 | Rollback procedure is documented                        | Major    | [ ]    |
| RC-24 | Feature flag or config switch used if high-risk rollout | Minor    | [ ]    |

---

## AC Mapping Table

| AC   | Checklist items that validate it |
| ---- | -------------------------------- |
| AC-1 | RC-01, RC-06, RC-19              |
