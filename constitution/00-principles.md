# Constitution — Canonical Business Rules (BR)

> Inviolable business principles. Every feature respects these. Changes here require a PRD/BR
> change (via the PRD process), not a unilateral edit. Language: English.

**Last sync with PRD:** pre-PRD — grounded in `definitions/INTAKE.md` and session decisions of 2026-07-22.

---

## How to use
- Each BR has an immutable ID (`BR-01`, `BR-02`, …). Deprecate, never reuse.
- A feature that touches a BR must read it before planning.
- If a human instruction violates a BR: cite it, explain the impact, propose the correct path,
  and only proceed with an explicitly accepted, documented exception (recorded as an Open Question).

## Rules

### BR-01 — Tenant data isolation
**Rule.** Data belonging to one tenant (organization/workspace) is never readable or mutable by any
principal of another tenant, under any endpoint or code path.

**Because.** Notebox is multi-tenant: users belong to organizations and data is shared within a
tenant but isolated between tenants (`INTAKE.md` D4, session 2026-07-22).

**Violation.** A cross-tenant read or write leaks or corrupts another organization's private notes and
tasks — a confidentiality breach and the most damaging failure this system can have.

---

### BR-02 — Every domain entity belongs to exactly one tenant
**Rule.** Every domain record — annotation type, annotation, field, task, subtask, group, card, and
uploaded image — is owned by exactly one tenant, and every request is authorized against the caller's
tenant before any data is returned or changed.

**Because.** Ownership is the enforcement mechanism behind BR-01 (`INTAKE.md` D4).

**Violation.** An entity with no tenant, or an unauthorized access path, silently defeats isolation;
BR-01 cannot hold if any entity escapes ownership.

---

### BR-03 — Annotations conform to their type
**Rule.** An annotation always conforms to its annotation type: it may hold a value only for a field
defined on that type, and each value respects that field's declared type. An annotation cannot exist
without a type.

**Because.** The annotation type *is* the schema of its annotations; the whole product depends on a
type's fields driving data entry and grid rendering (`INTAKE.md` C1, C4, C5, C13).

**Violation.** Values for undefined or mistyped fields make records unrenderable and untrustworthy;
the type stops being a contract and the editable datagrid breaks.

---

### BR-04 — Field types are a closed set
**Rule.** A type field's kind is exactly one of the seven defined field types — Text, List, Number,
Free text, Single choice, Multiple choice, Image — and no other.

**Because.** The brief defines the field-type set as a fixed enumeration (`INTAKE.md` K3, C6–C12).

**Violation.** An unknown field type has no defined storage, validation, or rendering, corrupting both
the API contract and every consumer that switches on field type.

---

### BR-05 — Destructive deletions are explicit and irreversible
**Rule.** Deleting a record that holds user data (an annotation, task, type, group) is a deliberate,
confirmed action; the system never deletes such data as a silent side effect of another operation.

**Because.** Deletion in the product is always a confirmed, explicit act (`INTAKE.md` C17), and there is
no stated recovery mechanism — a delete is permanent.

**Violation.** Silent or cascading deletion destroys user data with no undo and no consent.

---

### BR-06 — Task status is derived, never assigned
**Rule.** A task's status (completion percentage) is always computed from its subtasks — the proportion
that are marked done, or 0% when it has none. It is never set directly by a client.

**Because.** Status is defined solely as a progress bar driven by subtask completion (`INTAKE.md` D7,
C24).

**Violation.** A manually written status desynchronizes from reality, making the progress bar lie about
how much work is actually done.

---

### BR-07 — Task dates derive from its subtasks
**Rule.** A task's start date equals the start date of its first subtask and its end date equals the end
date of its last subtask; these are computed, not independently editable on the task.

**Because.** The brief fixes task dates as a projection of the subtask span (`INTAKE.md` C25).

**Violation.** Independently edited task dates contradict the subtask timeline the task is meant to
summarize. *(Resolved 2026-07-22, OQ-05: start = min(subtask start), end = max(subtask end) — by date.)*

---

### BR-08 — No user-facing text is ever shown untranslated
**Rule.** Every system-generated user-facing string (label, validation message, error, notification) is
resolvable in every supported locale (English and Portuguese at minimum); the system never surfaces a
raw message key or an empty string in place of a translation.

**Because.** All system texts and messages must be internationalized in at least en and pt
(`INTAKE.md` C31, D5, K6).

**Violation.** A raw key or blank message reaches an end user, exposing internals and breaking the
product's stated bilingual guarantee. *(Exact fallback behaviour is unresolved — INTAKE.md G10 — and
will be settled at PRD; that no raw key is ever shown is invariant regardless.)*

---

### BR-09 — "Visible for viewing" is presentation, never access control
**Rule.** A field's "visible for viewing" flag controls only whether it appears as a column in the
datagrid listing. It is never treated as an authorization or confidentiality boundary: fields with the
flag off are still returned by the API to a properly authorized caller (the Details popup shows all
fields, visible or not).

**Because.** The flag is defined purely as a grid-display concern, and hidden fields are explicitly shown
in the details view (`INTAKE.md` C5, C18).

**Violation.** Treating the flag as access control would either wrongly expose it as a security promise
it cannot keep, or wrongly withhold data the Details view requires — corrupting both security reasoning
and the feature.

---

### BR-10 — Secret field values are confidential at rest
**Rule.** A value entered into a field flagged "Secret" is never persisted in cleartext; it is stored
encrypted at rest and returned in cleartext only to a caller holding the elevated *reveal* role, and every
reveal is recorded in the audit trail.

**Because.** Human decision 2026-07-23 (OQ-15): Text and Free-text fields may carry a "Secret" flag for
sensitive data (credentials, tokens); such values must not be readable from the database or backups, and
each disclosure must be accountable.

**Violation.** A database dump, backup, or log leak exposes secret values in cleartext, or a caller without
the reveal role reads a secret with no audit record — breaking both the confidentiality promise and its
accountability. *(Contrast BR-09: the "visible for viewing" flag is display-only and never a security
boundary; the "Secret" flag here is a genuine confidentiality control — the two are independent.)*

---

<!-- Next ID: BR-11. Keep IDs immutable; deprecate, never reuse.
     Open gaps that may yield future BRs live in definitions/INTAKE.md §6 (G4–G8, G10). -->
