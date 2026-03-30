# Phase 0-A Plan

## Objective

Build the minimum safe operating skeleton for `docs/` so later phases have stable storage locations, explicit evidence, and clear authority boundaries.

## Scope

- Create the required `docs/` storage locations.
- Create and fill the mandatory Phase 0 evidence pack.
- Record what existed before the change and what remains intentionally deferred.

## Files To Read

- `my-react-app/README.md`
- `my-react-app/package.json`
- `demo/settings.gradle`
- `demo/build.gradle`
- Git history references for prior docs structure:
  - `HEAD:docs/standards/templates/spec-pack.template.md`
  - `HEAD:docs/standards/templates/review-checklist.template.md`
  - `HEAD:docs/changes/auth/raw/spec.md`
- Baseline repo state from `git status --short`

## Files To Create Or Update

- `docs/architecture/.gitkeep`
- `docs/standards/.gitkeep`
- `docs/changes/.gitkeep`
- `docs/maintenance/.gitkeep`
- `docs/maintenance/phase0/README.md`
- `docs/maintenance/phase0/phase0-plan.md`
- `docs/maintenance/phase0/phase0-execution-log.md`
- `docs/maintenance/phase0/phase0-decisions.md`
- `docs/maintenance/phase0/phase0-risk-register.md`
- `docs/maintenance/phase0/phase0-review.md`
- `docs/maintenance/phase0/artifacts/.gitkeep`

## Execution Checklist

- [x] Capture repo baseline before edits.
- [x] Confirm whether `docs/` exists in the working tree.
- [x] Confirm whether legacy `docs/` content exists in Git history.
- [x] Create required storage locations for living docs, change deliverables, maintenance, and Phase 0 evidence.
- [x] Write operating policy for the Phase 0 evidence pack.
- [x] Record execution details in a dedicated log file.
- [x] Record Phase 0 decisions, denials, and prohibitions.
- [x] Record remaining risks and acceptance rationale.
- [x] Perform self-review and gate judgment for Phase 0-A.

## Checkpoints

1. Baseline checkpoint
   - Confirm the working tree has no `docs/` directory at start.
   - Confirm tracked deletions exist for older `docs/` files.

2. Storage checkpoint
   - Required paths exist:
     - `docs/architecture/`
     - `docs/standards/`
     - `docs/changes/`
     - `docs/maintenance/`
     - `docs/maintenance/phase0/`
     - `docs/maintenance/phase0/artifacts/`

3. Evidence checkpoint
   - All mandatory Phase 0 evidence files exist and contain substantive content.

4. Handoff checkpoint
   - Phase 1 can locate where to add living docs, standards, ticket deliverables, and maintenance evidence without relying on chat.

## Risks

- Legacy deleted `docs/` files may represent prior work that is not being restored in this phase.
- Authority can remain ambiguous until later phases write actual living docs in `docs/architecture/` and `docs/standards/`.
- Placeholder directories alone do not guarantee compliance; later phases must follow the operating policy.

## Completion Gate

Phase 0-A passes only if all required directories exist, all evidence files are present and populated, and the authoritative-source policy is explicit inside the repo.

