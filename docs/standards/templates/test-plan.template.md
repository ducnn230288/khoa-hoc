# Test Plan - {{TICKET}} ({{FEATURE_NAME}})

> Every acceptance criterion should map to at least one verification path.  
> If a needed test layer is not available in the repo yet, record that gap explicitly.

---

## 1. Coverage Matrix

| AC | FE unit | BE unit | Integration | E2E | Manual / black-box |
| -- | ------- | ------- | ----------- | --- | ------------------ |
| AC-1 | | | | | |

## 2. Backend Tests

| Test file or class | Behavior covered | AC | Why this layer |
| ------------------ | ---------------- | -- | -------------- |
| | | | |

## 3. Frontend Tests

| Test file or suite | Behavior covered | AC | Why this layer |
| ------------------ | ---------------- | -- | -------------- |
| | | | |

## 4. Integration And Contract Tests

| Boundary | Scenario | AC | Evidence target |
| -------- | -------- | -- | --------------- |
| | | | |

## 5. Manual Or Black-Box Checks

| Scenario | Steps | Expected result | AC |
| -------- | ----- | --------------- | -- |
| | | | |

## 6. Commands To Run

```bash
# Backend
cd demo && ./gradlew test

# Frontend
cd my-react-app && npm run lint && npm run build
```

Add ticket-specific test commands below if new tooling is introduced.

## 7. Constraints And Gaps

- Current repo baseline does not include a configured frontend unit-test runner.
- Current repo baseline does not include a configured E2E runner.
- Add any ticket-specific test limitations here.

