# Test Plan — {{TICKET}} ({{FEATURE_NAME}})

> Tạo: [YYYY-MM-DD]
> Every AC must be covered by at least one test type.

---

## 1. Coverage Matrix

| #   | AC                | FE UT | BE UT | API IT | E2E | Black-box |
| --- | ----------------- | ----- | ----- | ------ | --- | --------- |
| 1   | AC-[feature]-1/v1 |       |       |        |     |           |

## 2. FE Unit Tests

| #   | Test file | What it tests | AC  |
| --- | --------- | ------------- | --- |
| 1   |           |               |     |

**Focus areas:** form validation, state transitions, error display, conditional rendering.

## 3. BE Unit Tests

| #   | Test class | What it tests | AC  |
| --- | ---------- | ------------- | --- |
| 1   |            |               |     |

**Focus areas:** boundary values, exception paths, permission logic in use cases/domain.

## 4. API Integration Tests

| #   | Endpoint | Scenarios                                  | AC  |
| --- | -------- | ------------------------------------------ | --- |
| 1   |          | happy path, auth failure, validation error |     |

## 5. E2E Tests (Playwright)

| #   | Scenario           | Steps | Expected result | AC  |
| --- | ------------------ | ----- | --------------- | --- |
| 1   | Main flow (normal) |       |                 |     |
| 2   | Key error path     |       |                 |     |

## 6. Test Execution Commands

```bash
# FE unit tests
cd my-react-app && npm test

# BE unit tests
cd demo && ./gradlew test

# E2E
cd my-react-app && npx playwright test
```

## 7. Notes / Constraints

<!-- Mocking policy, test data requirements, known flaky areas -->
