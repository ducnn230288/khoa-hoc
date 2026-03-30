# Coding Standards

## Purpose

These rules are the common coding baseline for this repo. Rule numbers are stable identifiers so later tickets can cite them in specs, plans, reviews, and reports.

## Scope

- Applies to both `demo/` and `my-react-app/`.
- Derived from the current toolchain and architecture baseline in `docs/architecture/overview.md`.
- Common-base only; ticket-specific requirements belong in `docs/changes/{{TICKET}}/`.

## Rules 10-19

### Rule 10: Preserve layer boundaries

Put code in the layer that owns the concern. Do not place backend business logic in controllers or config classes, and do not place frontend feature logic in generic shared components without a clear reuse reason.

### Rule 11: Keep executable truth and docs aligned

When code or config becomes the new truth, update the relevant living docs in the same change. Do not let common-base docs describe capabilities that the repo does not actually implement.

### Rule 12: Build on the repo's current toolchain first

Respect the current baseline before adding new frameworks or abstractions:

- Backend: Gradle, Spring Boot, Java 25.
- Frontend: Vite, React 19, strict TypeScript, ESLint.

Additions are allowed only when they solve a concrete problem and are documented in the change deliverables.

### Rule 13: Prefer small, cohesive modules

Keep files and modules focused on one responsibility. If a file starts mixing transport, business, persistence, and presentation concerns, split it before the complexity becomes sticky.

### Rule 14: Prefer explicit types and contracts

Use explicit request/response shapes, DTOs, return types, and domain-facing contracts where the boundary matters. Avoid hidden structure in loosely typed maps, ad hoc objects, or framework-specific side channels.

### Rule 15: Make environment-specific behavior explicit

Put environment-sensitive behavior in configuration, not scattered conditionals. If a feature behaves differently across dev, staging, and prod, the difference must be traceable in config or documented rules.

### Rule 16: Avoid hidden coupling

Do not let frontend behavior depend on undocumented backend internals, and do not let one backend layer reach through another layer's abstractions just because the codebase is still small.

### Rule 17: Write for reviewability

Choose names, function boundaries, and file layout so a reviewer can understand the change without reverse engineering intent from implementation details. Add brief comments only where logic would otherwise be hard to parse.

### Rule 18: Respect safe editing practices

Do not overwrite or revert unrelated work. Recreate deleted common-base files only when the phase explicitly calls for it, and keep scope narrow when the working tree is already dirty.

### Rule 19: Keep common base separate from ticket scope

`docs/architecture/` and `docs/standards/` are for reusable guidance. Do not place ticket-specific acceptance criteria, business rules, or implementation details there.

## Repo-Specific Notes

- The backend currently has only a bootstrap class and a smoke test, so new packages should be introduced intentionally instead of dumping all code into `com.example.demo`.
- The frontend currently centers on `App.tsx`; once more than one user flow exists, introduce feature-level structure instead of growing the starter component indefinitely.
- The TypeScript config is strict. Fix type issues rather than weakening compiler options without an approved reason.
