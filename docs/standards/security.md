# Security Standards

## Purpose

These rules define the repo-wide security baseline. They are intentionally generic enough for any ticket, but strict enough to block unsafe assumptions.

## Scope

- Applies to backend code, frontend code, configuration, docs, logs, and review artifacts.
- Complements, but does not replace, ticket-specific security requirements.

## Rules 30-40

### Rule 30: Treat security-relevant defaults as explicit decisions

Authentication, authorization, session handling, CORS, CSRF, cookie policy, secret loading, and environment exposure must be documented when introduced. Do not rely on framework defaults without recording the decision.

### Rule 31: Validate and sanitize at boundaries

Validate external input at the boundary where it enters the system, and keep domain invariants enforced inside the domain or application layers. Never trust browser input, request bodies, query parameters, or imported files by default.

### Rule 32: Do not store secrets or sensitive data in repo artifacts

Do not commit secrets, credentials, tokens, private keys, or production-only connection details. Do not place sensitive values in docs, screenshots, logs, or example payloads.

### Rule 33: Enforce authorization per action

When protected operations appear, authorization must be checked per action, not assumed from UI visibility or client-side state alone.

### Rule 34: Separate security concerns from business convenience

Do not bury authentication or authorization logic inside unrelated business code just to make the first implementation faster. Keep security-sensitive behavior easy to inspect and review.

### Rule 35: Protect state-changing operations deliberately

For any mutating behavior, identify the required protections such as authentication, authorization, CSRF protection, replay prevention, or idempotency rules as appropriate to the flow.

### Rule 36: Keep environment behavior safe by default

Development conveniences must not silently leak into staging or production. If security posture changes by environment, the difference must be visible in config and documented in deliverables.

### Rule 37: Do not leak internals in errors or responses

Avoid exposing stack traces, secret-bearing config, database internals, or security-sensitive state in client-visible responses or logs.

### Rule 38: Review security-sensitive changes from multiple angles

Security-relevant tickets must be reviewed for input validation, authz coverage, secret handling, logging, configuration, and failure modes. One passing happy-path test is not enough.

### Rule 39: Treat dependency and config changes as security changes when relevant

New libraries, plugins, build settings, CORS rules, headers, and runtime flags can change the security posture. Review them explicitly instead of treating them as harmless plumbing.

### Rule 40: Stop and log open issues when security assumptions are unverified

If a security-relevant assumption cannot be proven from code, config, or approved docs, do not invent certainty. Record it as an open issue or risk for the ticket before implementation continues.

## Repo-Specific Notes

- The current repo snapshot does not yet implement authn/authz or persistence, so later tickets must document those concerns explicitly when they arrive.
- The frontend starter app currently has no API client layer; when one is added, treat browser-visible data and credential handling as security-relevant from day one.
- The backend currently exposes only a minimal Spring Boot app, so secure defaults need to be revisited as soon as real endpoints and integrations are introduced.
