# Architecture Overview

## Purpose

This document is the common-base architecture guide for the current repository. It captures the observed baseline from code and settings that already exist, then defines the layer and dependency conventions that later tickets must follow.

## Repository Baseline

The repository currently contains two application roots:

1. `demo/`
   - Spring Boot application
   - Java 25 toolchain
   - `spring-boot-starter-web`
   - Gradle build
   - Minimal bootstrap code and one context-load test

2. `my-react-app/`
   - Vite application
   - React 19
   - TypeScript with strict compiler options
   - ESLint with TypeScript and React Hooks rules
   - Minimal starter UI

There is no root-level orchestrator, shared library, or documented domain layer yet. Until later tickets add more structure, the backend and frontend should be treated as two independent deliverables inside one repo.

## Authoritative Inputs

The current common base is derived from these executable sources:

- `demo/build.gradle`
- `demo/settings.gradle`
- `demo/src/main/resources/application.properties`
- `demo/src/main/java/com/example/demo/DemoApplication.java`
- `demo/src/test/java/com/example/demo/DemoApplicationTests.java`
- `my-react-app/package.json`
- `my-react-app/eslint.config.js`
- `my-react-app/vite.config.ts`
- `my-react-app/tsconfig.json`
- `my-react-app/tsconfig.app.json`
- `my-react-app/tsconfig.node.json`
- `my-react-app/src/main.tsx`
- `my-react-app/src/App.tsx`

## System Context

### Backend

- Current responsibility: expose HTTP functionality and host future business capabilities.
- Current observed component: one Spring Boot bootstrap class.
- Expected evolution: controllers, application services, domain rules, persistence adapters, and configuration packages will be added by later tickets.

### Frontend

- Current responsibility: render the browser application and host future user-facing flows.
- Current observed components: `main.tsx`, `App.tsx`, CSS files, Vite entrypoint.
- Expected evolution: app shell, pages/features, shared components, API clients, and stateful interaction flows will be added by later tickets.

### Docs

- `docs/architecture/` is for living architecture guidance.
- `docs/standards/` is for shared engineering rules.
- `docs/changes/` is reserved for ticket-specific deliverables and must not be used for common-base material.

## Target Layer Structure

The repo does not yet implement these layers in full, but new work should follow this target shape.

### Backend Layers

1. Entry layer
   - Controllers, request/response DTOs, exception mapping, web configuration.
   - Responsibility: translate HTTP concerns into application commands or queries.

2. Application layer
   - Use cases, orchestration services, transaction boundaries, application DTOs.
   - Responsibility: coordinate work across domain rules and infrastructure ports.

3. Domain layer
   - Domain entities, value objects, policies, invariants, pure business rules.
   - Responsibility: own business meaning and validation that is independent of frameworks.

4. Infrastructure layer
   - Repository implementations, persistence mappings, external integrations, environment configuration.
   - Responsibility: connect the application to databases, networks, and runtime frameworks.

### Frontend Layers

1. App layer
   - Entry files, global providers, router setup, app-wide layout.
   - Responsibility: compose the overall application shell.

2. Feature/page layer
   - Screens, route-level flows, user interaction logic, feature-scoped state.
   - Responsibility: express user journeys and feature-specific behavior.

3. Shared UI and utility layer
   - Reusable components, hooks, formatting helpers, API clients, constants.
   - Responsibility: provide reusable building blocks without depending on feature-specific code.

4. Assets and styling layer
   - CSS, design tokens, images, icons, fonts.
   - Responsibility: visual presentation and static resources.

## Dependency Direction

The required dependency direction for later work is:

1. Backend
   - Entry depends on application.
   - Application depends on domain and declared ports.
   - Infrastructure depends on application and domain contracts.
   - Domain depends on no Spring, web, or persistence framework types unless there is a deliberate documented exception.

2. Frontend
   - App layer may depend on features and shared modules.
   - Features may depend on shared modules.
   - Shared modules must not depend on features or route-specific pages.
   - API clients and state helpers must not import presentation-only styling concerns.

3. Cross-project
   - Frontend depends on backend only through documented HTTP contracts, not by sharing implementation assumptions.
   - Docs depend on code/config observations, but code must not depend on docs at runtime.

## Key Components

### Backend Components Observed Now

- `DemoApplication`: Spring Boot bootstrap entrypoint.
- `application.properties`: application name only.
- `DemoApplicationTests`: minimal Spring context smoke test.

### Frontend Components Observed Now

- `main.tsx`: React mount point with `StrictMode`.
- `App.tsx`: starter component.
- `vite.config.ts`: Vite config with React plugin.
- `eslint.config.js`: lint baseline.
- `tsconfig.app.json` and `tsconfig.node.json`: strict TypeScript settings.

## Operational Implications

- New backend tickets should avoid placing business logic directly in controllers or config classes.
- New frontend tickets should avoid keeping all logic inside `App.tsx` once multiple user flows exist.
- Because there is no shared root build yet, verification commands remain app-specific.
- Future architecture docs must update this file when the repo gains new runtime modules, shared libraries, routing, persistence, or integration boundaries.
