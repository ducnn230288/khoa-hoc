# Phase 0 Evidence Pack Operating Policy

## Purpose

This directory is the evidence pack for Phase 0. It records why the initial `docs/` operating model exists, what was created in Phase 0-A, what was intentionally not restored, and how later phases should treat this material.

## Ownership

- The implementer of a Phase 0 change is responsible for updating this evidence pack in the same working session.
- The reviewer of a Phase 0 change is responsible for checking that evidence is stored in files, not only in chat or PR comments.
- Later phase owners may append clarifications, but they must not rewrite Phase 0 history without leaving an explicit correction note.

## When To Update

Update this directory when any of the following happens:

1. The `docs/` storage layout changes.
2. A Phase 0 safety rail or prohibition changes.
3. A review finds the original Phase 0 record incomplete or inaccurate.
4. The team reopens Phase 0 decisions because later work depends on them.

## How To Update

- `phase0-plan.md`: keep the intended work and checkpoints for Phase 0-A. If reopened later, add an addendum instead of replacing the original plan.
- `phase0-execution-log.md`: append what was actually done, what files were touched, and what was verified.
- `phase0-decisions.md`: record durable decisions, their rationale, and any explicit denials.
- `phase0-risk-register.md`: update remaining risks, mitigations, and acceptance rationale when risk posture changes.
- `phase0-review.md`: store the self-review and gate judgment for this phase.
- `artifacts/`: optional place for supporting logs or screenshots if later phases need them.

## Authoritative Sources

Authority is split by purpose:

1. Executable behavior: source code and runtime configuration in the repo are authoritative until a later phase establishes approved living docs for that topic.
2. Ticket-specific scope and acceptance: approved deliverables under `docs/changes/` are authoritative when they exist.
3. Cross-cutting conventions: approved living docs under `docs/architecture/` and `docs/standards/` become authoritative only after they are intentionally written and reviewed.
4. Phase 0 process history: files in this directory are authoritative for what Phase 0 decided, created, reviewed, and accepted.

## Current Baseline At Phase 0-A

- The working tree did not contain a `docs/` directory when Phase 0-A started.
- Git history showed a previously tracked `docs/` tree with deletions in the working tree.
- The repo currently contains a backend app under `demo/` and a frontend app under `my-react-app/`.
- No root-level approved documentation pack existed in the working tree at the start of this phase.

## Handling Legacy Docs

Phase 0-A does not automatically restore legacy files that appear only in Git history. Restoring or replacing legacy documentation must be a deliberate, reviewed action in a later phase or ticket.

