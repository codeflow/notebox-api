# Harness — API (the project)

> Type-specific layers: how to run, verify, and deploy a service/API. Stack: `TBD`. Language: English.

## verify (the green/red signal)
Define a single entry point agents and CI call:
```
# fill for TBD — e.g.:
# lint + typecheck + unit + integration + build
make verify   # or: npm run verify / mvn verify / ./gradlew check
```

## Reproducible local env
- Provide `docker-compose.yml` for dependencies (DB, cache, queue).
- `.env.example` with every required variable. Secrets never committed.
- `make up` / `make seed` to get a working stack from zero.

## Tests
- Unit + integration. Gherkin acceptance scenarios → executable API tests (request → response/status).
- Contract tests against `features/*/contracts/`.

## Deploy
- Containerize (multi-stage Dockerfile, non-root, `$PORT`).
- CI builds and deploys to staging on green; production on tag/approval.
- Health endpoint (`/health`) excluded from auth for probes.

## Security (cross-ref constitution/02-compliance.md)
- AuthN/AuthZ on every endpoint; public ones justified.
- Secrets via secret manager; least-privilege runtime identity.
- Structured logs with correlation id; never log secrets/PII.
