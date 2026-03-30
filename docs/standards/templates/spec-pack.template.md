# Spec Pack - {{TICKET}} ({{FEATURE_NAME}})

> Single source of truth for ticket scope and acceptance.  
> Use together with `docs/architecture/overview.md`, `docs/architecture/key-flows.md`, and rules 10-40 under `docs/standards/`.

---

## 1. Ticket Metadata

| Field          | Value            |
| -------------- | ---------------- |
| Ticket         | {{TICKET}}       |
| Feature        | {{FEATURE_NAME}} |
| Owner          |                  |
| Related branch |                  |
| Last updated   | YYYY-MM-DD       |

## 2. Objective

<!-- Why this change exists and what outcome it should create. -->

## 3. Baseline

### Existing code and config to read first

- [ ]
- [ ]

### Current behavior

<!-- Describe the observed "as-is" behavior from code, config, or approved docs. -->

## 4. Scope

### In scope

-

### Out of scope

-

## 5. Proposed Behavior

### Main behavior

<!-- Describe the expected behavior in enough detail that implementation can start without guesswork. -->

### Error behavior

<!-- Expected failures, validation, or fallback paths. -->

### Environment-specific behavior

<!-- Differences across dev/staging/prod if any. -->

## 6. Data, Contracts, And Interfaces

| Interface  | Change | Notes |
| ---------- | ------ | ----- |
| API        |        |       |
| UI         |        |       |
| DB         |        |       |
| Config     |        |       |
| Logs/Audit |        |       |

## 7. Frontend Screens, States, And Wireframes

<!-- Required for tickets with user-facing UI changes. If the ticket has no frontend impact, write N/A and explain why. -->

### Screen inventory

| Screen ID | Name | Entry trigger | Primary user goal | Notes |
| --------- | ---- | ------------- | ----------------- | ----- |
| S-1       |      |               |                   |       |

### Screen details

#### Screen S-1: {{SCREEN_NAME}}

- Purpose:
- Entry trigger:
- Primary actions:
- Exit or next states:

```text
+------------------------------------------------------------------------------+
| Header / navigation                                                          |
|------------------------------------------------------------------------------|
| Page title                                                                   |
|                                                                              |
| Primary content area                                                         |
|                                                                              |
| Input / summary / list / form block                                          |
|                                                                              |
| [Primary CTA]                     [Secondary CTA]                            |
|                                                                              |
| Inline validation / helper / status message area                             |
+------------------------------------------------------------------------------+
```

### Interaction states

| State ID | Trigger                    | User-visible behavior | Recovery or next action |
| -------- | -------------------------- | --------------------- | ----------------------- |
| ST-1     | Initial load               |                       |                         |
| ST-2     | Loading / submitting       |                       |                         |
| ST-3     | Empty / no data            |                       |                         |
| ST-4     | Success                    |                       |                         |
| ST-5     | Error / validation failure |                       |                         |

### Wireframe notes

- Capture every new or materially changed screen and every state that changes layout, CTA visibility, or messaging.
- Label primary CTA, secondary CTA, validation areas, error banners, loading indicators, and empty states directly in the ASCII wireframe or notes.
- If one screen has multiple materially different states, add separate wireframes or clearly annotate the differences.

## 8. Non-Functional Requirements

| Category      | Requirement |
| ------------- | ----------- |
| Performance   |             |
| Security      |             |
| Observability |             |
| Reliability   |             |
| Compatibility |             |

## 9. Acceptance Criteria

| AC   | Statement | Test type          |
| ---- | --------- | ------------------ |
| AC-1 |           | UT / IT / E2E / BB |

## 10. Examples

### Main path

1.

### Error paths

1.

### Boundary cases

1.

## 11. Open Issues

| ID   | Question | Owner | Resolution needed by |
| ---- | -------- | ----- | -------------------- |
| OI-1 |          |       |                      |

## 12. Risks

| Risk | Likelihood | Impact | Mitigation |
| ---- | ---------- | ------ | ---------- |
|      |            |        |            |

## 13. Applicable Common Rules

- Architecture docs reviewed: [ ] `overview.md`
- Architecture docs reviewed: [ ] `key-flows.md`
- Coding rules to watch:
  - [ ] Rule 10
  - [ ] Rule 19
- Testing rules to watch:
  - [ ] Rule 20
  - [ ] Rule 29
- Security rules to watch:
  - [ ] Rule 30
  - [ ] Rule 40

---

## Traceability Table

| AC   | Components/Files likely affected | Planned verification |
| ---- | -------------------------------- | -------------------- |
| AC-1 |                                  |                      |
