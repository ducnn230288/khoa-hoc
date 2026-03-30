# Testing Standards

## Purpose

These rules define the common testing baseline for this repo. They apply even when the current codebase is still small or the available test tooling is incomplete.

## Current Baseline

- Backend test command exists: `cd demo && ./gradlew test`
- Frontend lint/build commands exist: `cd my-react-app && npm run lint && npm run build`
- Frontend unit-test and E2E runners are not yet configured in the current repo snapshot

## Rules 20-29

### Rule 20: Every accepted change needs explicit verification

A change is not complete until the implementer records how it was verified. If a test layer is unavailable, document the gap and the fallback verification used.

### Rule 21: Choose the smallest meaningful test layer first

Prefer the lowest-cost test layer that can reliably prove the behavior. Use unit tests for pure logic, integration tests for contracts and wiring, and end-to-end tests for user journeys that cross boundaries.

### Rule 22: Protect backend wiring and business rules separately

Keep a context-load or wiring test for application startup, and add focused tests for business behavior as domain and application logic appear. Do not rely on one broad Spring test to prove all behavior.

### Rule 23: Test frontend behavior through state and output

Frontend tests should assert rendered output, state transitions, validation, and user-visible effects. Avoid tests that only mirror implementation details without proving behavior.

### Rule 24: Test contracts at integration boundaries

When an API, persistence layer, or external integration is added, verify the contract at that boundary. Contract-sensitive behavior should not rely on unit tests alone.

### Rule 25: Keep tests deterministic

Avoid hidden clock, randomness, network, environment, or ordering dependencies. If a test needs fixtures, seed data, or setup state, make that setup explicit.

### Rule 26: Run the commands the repo actually supports

At minimum, run the verification commands that exist for the changed area. In the current baseline that means:

```bash
# Backend
cd demo && ./gradlew test

# Frontend
cd my-react-app && npm run lint && npm run build
```

If a command cannot run, record why.

### Rule 27: Document unverified risk honestly

When a needed test could not be added or executed, state that clearly in self-review and report artifacts. Silence is not an acceptable substitute for evidence.

### Rule 28: Fixes must carry regression protection

Bug fixes should add or strengthen the narrowest test that would fail before the fix and pass after it, unless the repo lacks the necessary test layer and that gap is explicitly logged.

### Rule 29: Keep traceability from AC to evidence

Acceptance criteria, checklist items, commands run, and observed outcomes must be traceable across the spec pack, test plan, self-review, and report.

## Repo-Specific Notes

- The backend already has a smoke test and should retain that coverage as more wiring is added.
- The frontend currently lacks a configured unit-test runner, so future tickets that need UI tests may need to introduce one deliberately and document the decision.
- Build-only verification is not enough once user-facing behavior or backend rules become non-trivial.
