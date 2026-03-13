# Test Plan — {{TICKET}} ({{FEATURE_NAME}})

> Every AC must be covered by at least one test type.

---

## 1. Coverage Matrix

| AC   | FE UT | BE UT | API IT | E2E | Black-box |
| ---- | ----- | ----- | ------ | --- | --------- |
| AC-1 |       |       |        |     |           |

## 2. FE Unit Tests

| Test file | What it tests | AC  |
| --------- | ------------- | --- |
|           |               |     |

**Focus areas:** form validation, state transitions, error display, conditional rendering.

## 3. BE Unit Tests

| Test class | What it tests | AC  |
| ---------- | ------------- | --- |
|            |               |     |

**Focus areas:** boundary values, exception paths, permission logic in use cases/domain.

## 4. API Integration Tests

| Endpoint | Scenarios                                  | AC  |
| -------- | ------------------------------------------ | --- |
|          | happy path, auth failure, validation error |     |

## 5. E2E Tests (Playwright)

| Scenario           | Steps | Expected result | AC  |
| ------------------ | ----- | --------------- | --- |
| Main flow (normal) |       |                 |     |
| Key error path     |       |                 |     |

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
