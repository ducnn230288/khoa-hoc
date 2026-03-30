# Phase 0-A Decisions

## Decision 1: Deny Policy For Unauthorised Work

Decision:

- Deny undocumented implementation assumptions by default.
- If a behavior, rule, or scope item is not present in an authoritative source, it must not be treated as approved.
- Unknowns must be logged as open items in the relevant future deliverable instead of being silently implemented.

Rationale:

- Phase 0 exists to prevent later phases from drifting based on chat memory or intuition.
- A deny-by-default rule reduces accidental scope creep and conflicting interpretations across frontend and backend work.

## Decision 2: Evidence Must Live In Files, Not Only In Conversation

Decision:

- Work performed in Phase 0-A must be evidenced inside `docs/maintenance/phase0/`.
- Chat may summarize progress, but it is not the authoritative record for Phase 0.

Rationale:

- Later phases need durable evidence that survives beyond one conversation.
- Review, handoff, and audit become fragile if the rationale only exists in chat.

## Decision 3: Storage Locations Are Split By Purpose

Decision:

- `docs/architecture/` stores living architecture docs.
- `docs/standards/` stores living standards and conventions.
- `docs/changes/` stores ticket-scoped deliverables.
- `docs/maintenance/` stores maintenance logs and evidence packs.
- `docs/maintenance/phase0/` stores the Phase 0 evidence pack.
- `docs/maintenance/phase0/artifacts/` is reserved for optional supporting logs or screenshots.

Rationale:

- A small but explicit taxonomy reduces the chance that later work mixes long-lived standards with ticket outputs or audit evidence.
- The split supports both evolving docs and immutable-ish historical records.

## Decision 4: Legacy Docs Are Not Auto-Restored

Decision:

- Previously tracked docs visible only through Git history are not restored automatically in Phase 0-A.
- Reintroduction of legacy content must be deliberate and reviewed.

Rationale:

- The repo state at the start of Phase 0-A showed prior deletions. Auto-restoring them would risk undoing intentional user changes.
- The current phase goal is to rebuild safe storage rails, not to decide the fate of every prior doc.

## Decision 5: Authority Is Layered By Use Case

Decision:

Authority is assigned as follows:

1. Source code and runtime config are authoritative for executable behavior until approved living docs exist for the same topic.
2. Approved files under `docs/changes/` are authoritative for ticket-specific scope and acceptance.
3. Approved files under `docs/architecture/` and `docs/standards/` are authoritative for stable cross-cutting guidance once written.
4. `docs/maintenance/phase0/` is authoritative for Phase 0 rationale, review, and evidence only.

Rationale:

- This avoids falsely elevating empty placeholder directories into authoritative documentation.
- It also gives later phases a clear upgrade path from code-first truth to documented truth.

## Prohibitions Established In Phase 0-A

- Do not treat chat alone as the project record for Phase 0.
- Do not place long-lived standards inside `docs/changes/`.
- Do not place ticket-specific deliverables inside `docs/architecture/` or `docs/standards/`.
- Do not claim a doc is authoritative without stating its scope and owner.
- Do not restore deleted legacy docs wholesale without a reviewed decision.
- Do not implement future work based on undocumented assumptions when authoritative inputs are absent.

## Decision 6: Common Base Must Reflect Observed Baseline First

Decision:

- Architecture and standards written in Phase 0-B must start from the code and settings that already exist in the repo.
- Target conventions may be defined for future tickets, but they must be clearly distinguished from currently implemented behavior.

Rationale:

- The current repo is still close to starter templates, so it would be easy to document a system that does not exist yet.
- Separating observed baseline from target convention keeps the common base honest and useful.

## Decision 7: Rules 10-40 Are Stable Cross-References

Decision:

- Rules 10-40 are reserved as reusable identifiers across coding, testing, and security standards.
- Future ticket specs, plans, reviews, and reports may reference these rule numbers directly.

Rationale:

- Stable rule identifiers reduce repetition and make reviews easier to trace across documents.
- The common base becomes more reusable when later phases can cite rule numbers instead of rewriting the same guidance.
