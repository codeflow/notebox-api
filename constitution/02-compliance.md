# Constitution — Compliance & Security

> Legal/regulatory/security constraints applied to every feature. Non-negotiable.
> Derived from `definitions/INTAKE.md` and `00-principles.md`, not a generic list. Language: English.

**Last sync with PRD:** pre-PRD — grounded in session decisions of 2026-07-22.

---

## Checklist

Every feature spec copies the items whose **applies when** trigger it hits, and marks each with named
**evidence**. A feature is not Done with an unjustified unchecked item.

- [ ] **C-01 · Tenant isolation** — every data access is scoped to the caller's tenant; no query returns
  or mutates another tenant's data · *applies when:* any endpoint reading/writing tenant-owned data (in
  practice, all of them) · *evidence:* cross-tenant authorization test + repository choke point (AD-03,
  BR-01/BR-02).
- [ ] **C-02 · Authenticated by default** — every entry point requires authentication and an authorized
  tenant context; any public endpoint is explicitly justified · *applies when:* any new endpoint ·
  *evidence:* security test asserting 401/403 on unauthenticated/foreign-tenant access.
- [ ] **C-03 · Least-privilege authorization** — administrative actions (message-catalog edits, tenant/
  user management) require an elevated role, not just tenant membership · *applies when:* feature exposes
  an admin/config surface (e.g. i18n catalog CRUD, AD-05) · *evidence:* role-based authz test.
- [ ] **C-04 · Personal data minimization** — only the user/tenant identity data the feature needs is
  collected and returned; no incidental PII in payloads or logs · *applies when:* feature stores or
  returns user or tenant identity data · *evidence:* data map in the feature spec + response-schema review.
- [ ] **C-05 · Secrets never committed** — DB credentials and any keys come from env / a secret manager;
  none in the repo, build image, or logs · *applies when:* feature needs a credential or connection
  string · *evidence:* `.env.example` entry + repo secret scan.
- [ ] **C-06 · Encryption in transit** — all endpoints served over TLS in deployed environments ·
  *applies when:* any deployed endpoint · *evidence:* deployment/ingress config.
- [ ] **C-07 · Image upload safety** — uploaded images (type icons, image fields, rich-text embeds) are
  content-type validated and size-bounded before becoming a MySQL BLOB, and served with a correct binary
  content type from the dedicated endpoint (never inline HTML) · *applies when:* feature accepts or serves
  image binaries · *evidence:* validation test (AD-04, AD-07) + content-type assertion.
- [ ] **C-08 · Rich-text sanitization** — task `details` HTML (WYSIWYG: styles, colour, bold, underline,
  embedded images) is sanitized on input/output to an allow-list, preventing stored XSS delivered to
  `notebox-web` · *applies when:* feature stores or returns rich-text content (C27) · *evidence:*
  sanitization test with hostile-markup fixtures.
- [ ] **C-09 · Localization completeness** — every user-facing system string the feature emits has a value
  in each supported locale (en, pt); no raw key or blank ever reaches a client · *applies when:* feature
  emits validation/error/notification/label text · *evidence:* message-catalog coverage check per locale
  (BR-08, AD-05).
- [ ] **C-10 · Audit trail for irreversible & admin actions** — destructive deletions (BR-05), tenant/user
  administration, and message-catalog edits record who/what/when · *applies when:* feature performs an
  irreversible delete or an administrative action · *evidence:* audit-log entry + test.
- [ ] **C-11 · Data retention & deletion path** — a defined deletion/anonymization path exists for tenant
  and user data · *applies when:* feature manages accounts, tenants, or offers data removal · *evidence:*
  deletion path documented in the spec.
- [ ] **C-12 · Encryption at rest for secret values** — values of "Secret"-flagged fields are stored
  encrypted (AES-256-GCM, master key from the secret manager) and revealed in cleartext only to the elevated
  reveal role, with each reveal audited · *applies when:* feature stores, updates, or reveals annotation
  values of Secret-flagged fields · *evidence:* ciphertext-at-rest test (stored bytes ≠ plaintext) + reveal
  authorization/audit test (BR-10, AD-14, C-05, C-10).

> **Not applicable yet (documented, not kept "just in case"):** no third-party data processors → no DPA
> item; no payments, health, or minors data in scope → no sector-specific items. Add them only when a
> feature introduces the trigger. UI **accessibility** is owned by the `notebox-web` satellite, not this
> API.

## Principles
- **Least privilege** everywhere (runtime identities, tokens, DB roles).
- **No secrets in the repo or build image.**
- **Auditability** of administrative and irreversible actions.
- Conflicts between a product requirement and compliance resolve **in favor of compliance**, unless there
  is a written decision from the accountable owner (recorded as an Open Question).

## Who decides
- Changes here require a PRD/BR change.
- Unforeseen cases escalate to the accountable data/compliance owner.
