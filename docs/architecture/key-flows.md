# Key Flows

## Purpose

This document records the common skeleton of the major flows that later tickets should fit into. These are architecture-level flows, not ticket-specific business flows.

## Flow 1: Backend Request Handling

Use this shape for new backend endpoints unless a documented exception is approved.

1. HTTP request enters a controller or request handler.
2. Boundary validation checks request shape and mandatory inputs.
3. The controller maps the request into an application command or query.
4. The application layer orchestrates the use case.
5. Domain rules validate invariants and decide business outcomes.
6. Infrastructure adapters perform persistence or external I/O when needed.
7. The application layer maps the result into a response model.
8. The controller returns an HTTP response with no framework internals leaked to callers.

## Flow 2: Frontend Interaction Handling

Use this shape for new user-facing flows unless a lighter path is clearly sufficient.

1. The app entrypoint mounts providers and the app shell.
2. A page or feature component renders the current state.
3. A user action triggers event handling inside the feature or a dedicated hook.
4. The feature prepares a request through a shared client or helper.
5. The UI enters a loading, success, or error state based on the result.
6. The page rerenders from state rather than mutating the DOM directly.
7. Shared UI components stay presentation-focused and reusable.

## Flow 3: Configuration And Startup

### Backend startup

1. Gradle resolves dependencies and the Java toolchain.
2. Spring Boot starts from `DemoApplication`.
3. Runtime configuration is loaded from `application.properties` and later environment-specific sources.
4. Beans are wired and the application context becomes available.
5. Smoke tests should continue to verify that the application context can load.

### Frontend startup

1. Vite serves or builds the React application.
2. `main.tsx` mounts the app with `createRoot`.
3. The app shell renders the initial UI state.
4. Later tickets may add routing, providers, and API bootstrap logic above feature components.

## Flow 4: Change Delivery

This is the expected documentation and implementation path for future ticket work.

1. Start from the common base in `docs/architecture/` and `docs/standards/`.
2. Create ticket deliverables under `docs/changes/{{TICKET}}/` in Phase 1 or later.
3. Write the spec pack before implementation begins.
4. Write the implementation plan before editing code.
5. Implement the change with verification evidence.
6. Fill self-review, test plan/results, and report artifacts before handoff.

## Flow 5: Defect Fix Or Regression Response

1. Reproduce the issue in the smallest reliable way.
2. Identify the layer where the invariant broke.
3. Fix the issue at the correct layer instead of masking symptoms at an outer layer.
4. Add or extend tests close to the failure boundary.
5. Record the remaining risk if full coverage is still not possible.

## Skeleton For Future Business Flows

When a new user journey appears, describe it in this order:

1. Trigger
2. Preconditions
3. Main path
4. Error paths
5. State transitions
6. Side effects
7. Observability or audit outputs
