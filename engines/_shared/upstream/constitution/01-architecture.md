# Constitution — Architecture

> Architectural decisions that apply to EVERY feature. Changes require revisiting the related BR
> via the PRD process. Language: English.

**Last sync with PRD:** v1 (2026-07-19)
**Project type:** `TBD` · **Stack:** `TBD`

---

## Stack

| Layer | Technology | Notes |
|---|---|---|
| (declared at setup) | `TBD` | confirm/expand as the PRD fixes choices |

## Infrastructure — the home for "which DB / which service"

> Concrete, **canonical** infra decisions, confirmed from **PRD §5**. Each row: the concern, the choice,
> the managed service/provider, status (confirmed vs Open Question), and the NFR that records it. Mirror
> these as NFRs in `catalogs/requirements.md`. **Operational wiring** (endpoints, connection strings) lives
> in `docs/harness-TBD.md` + `.env.example` via a secret manager — never hardcode it here.

| Concern | Choice | Provider / service | Status | NFR |
|---|---|---|---|---|
| Relational DB | PostgreSQL | e.g. Neon / Cloud SQL / RDS | confirmed / OQ-XX | NFR-.. |
| Cache / ephemeral keys | Redis | e.g. Upstash / ElastiCache | … | NFR-.. |
| Object storage | S3-compatible | e.g. Cloudflare R2 / S3 / GCS | … | NFR-.. |
| Hosting / compute | container or serverless | e.g. Cloud Run / Fly / ECS | … | NFR-.. |
| Messaging / push | — | e.g. Firebase / SNS / Pub-Sub | … | NFR-.. |
| Secrets | secret manager | e.g. GCP Secret Manager / Vault | … | — |

> Undecided choices stay as **Open Questions** (`catalogs/open-questions.md`) until fixed — do not invent.

## Core principles
- <e.g., boundaries, layering, dependency direction — fill from the PRD §architecture>
- **Observability minimum:** structured logging with a correlation id; metrics for key business
  events; tracing across services when applicable.
- **Reproducible env:** a one-command local stack (see `docs/harness-TBD.md`).

## Data model (high level)
<entities, relations — kept at the level the project needs>

## External integrations
| System | Purpose | Status |
|---|---|---|
| <name> | <purpose> | <to confirm — OQ-XX> |

## Open architectural decisions
- <decision> → becomes an Open Question when a feature must decide it.
