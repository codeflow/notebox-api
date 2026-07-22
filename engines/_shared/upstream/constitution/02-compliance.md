# Constitution — Compliance & Security

> Legal/regulatory/security constraints applied to every feature. Non-negotiable.
> Language: English.

**Last sync with PRD:** v1 (2026-07-19)

---

## Pre-flight checklist (copy into every feature's spec.md that touches personal/sensitive data)

| # | Item | Status |
|---|---|---|
| ☐ | Personal/sensitive data identified, with legal basis documented | |
| ☐ | Data minimization: only what the feature needs is collected/stored | |
| ☐ | Encryption in transit (TLS) and at rest where applicable | |
| ☐ | Secrets via a secret manager / env — never hardcoded, never committed | |
| ☐ | Access control: authenticated by default; public endpoints justified | |
| ☐ | Audit trail for sensitive actions (who/what/when) | |
| ☐ | Retention period defined; deletion/anonymization path exists | |
| ☐ | Third parties receiving data have a contract/DPA | |
| ☐ | Incident-response note updated | |

> A feature does not reach Definition of Done with unjustified ☐ items.

## Principles
- **Least privilege** everywhere (runtime identities, tokens, DB roles).
- **No secrets in the repo or build image.**
- **Auditability** of administrative/sensitive actions.
- Conflicts between a product requirement and compliance resolve **in favor of compliance**,
  unless there is a written decision from the accountable owner.

## Who decides
- Changes here require a PRD/BR change.
- Unforeseen cases escalate to the accountable data/compliance owner.
