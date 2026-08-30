# Catalog — Functional (FR) & Non-Functional (NFR) Requirements

> Master table of product requirements, derived from the PRD. IDs immutable; deprecate, never reuse.
> Language: English.

**Last sync with PRD:** v2 (2026-07-24)

## Functional Requirements

| ID | Description | Priority | Origin | Delivered by | Status |
|----|-------------|----------|--------|--------------|--------|
| FR-01 | CRUD annotation types (name, icon image, ordered fields). | Must | INTAKE C1,C2,C4 + BR-03 | US-1.1 | done |
| FR-02 | Type field declares name, field type ∈ closed set of 7, optional icon, "visible for viewing" flag with per-type defaults. | Must | INTAKE C4,C6–C12,K3,D2 + BR-04 | US-1.1 | done |
| FR-03 | List/Single/Multiple fields define options on the field definition (ordered; label + optional badge colour from fixed palette). | Must | INTAKE C7,C10,C11,K4 + OQ-03 | US-1.1 | done |
| FR-04 | CRUD annotation records conforming to their type. | Must | INTAKE C13,C16,C17 + BR-03 | US-2.1 | done |
| FR-05 | List a type's annotations returning visible fields; detail view returns all fields incl. non-visible. | Must | INTAKE C14,C18 + BR-09 | US-2.2 | done |
| FR-06 | Explicit, irreversible delete of an annotation (confirmation is a UI concern). | Must | INTAKE C17 + BR-05 | US-2.1 | done |
| FR-07 | Store images (type icons, image fields, rich-text embeds) as MySQL BLOBs; retrieve via dedicated binary endpoint; thumbnail support. | Must | INTAKE C3,C12,K5,D6 + AD-04 | US-1.1, US-2.2 | done |
| FR-08 | CRUD groups; assign an item to at most one group; groups flat, separate per domain. | Must | INTAKE C29 + OQ-04 | US-3.1 | done |
| FR-09 | Provide navigation-tree data: annotations by type and tasks, organized by group. | Must | INTAKE C28,C30 | US-3.1 | done |
| FR-10 | CRUD tasks (name, priority enum, optional card, optional rich-text details; derived status %, derived dates). | Must | INTAKE C19,D7 + BR-06,BR-07 | US-4.1 | done |
| FR-11 | Manage subtasks (name, dates, optional card, done flag); completing/adding recomputes parent status %. | Must | INTAKE C23,C24 + BR-06 | US-4.1 | done |
| FR-12 | Derive task start = min(subtask start), end = max(subtask end). | Must | INTAKE C25 + BR-07 + OQ-05 | US-4.2 | done |
| FR-13 | Attach a card as inline value object (code id + optional URL) on task/subtask. | Must | INTAKE C21 + OQ-06 | US-4.2 | done |
| FR-14 | Store/return task rich-text details (WYSIWYG), sanitized on input/output. | Must | INTAKE C26,C27 + C-08 | US-4.2 | done |
| FR-15 | Resolve/serve localized system strings; locale = preference → Accept-Language → English; fallback English; never emit raw key/blank. | Must | INTAKE C31 + BR-08,AD-05 + OQ-07 | US-5.1 | done |
| FR-16 | Translation-management: CRUD message catalog per locale, tenant-admin only; en/pt fixed in v1. | Must | INTAKE C32 + AD-05,C-03 + OQ-07 | US-5.2 | done |
| FR-17 | Multi-tenant identity & isolation: stateless JWT auth, resolve tenant from token, authorize every access. | Must | INTAKE D4 + BR-01,BR-02 + OQ-08 | US-6.1 | done |
| FR-18 | Secret field values encrypted at rest (AES-256-GCM, master key from secret manager, key-version rotation); masked on normal reads/listings; cleartext reveal only to elevated role, each reveal audited. | Must | OQ-15 (2026-07-23) + BR-10, AD-14, C-12 | US-2.1 | done |
| FR-19 | **`notebox-web` follows the design handoff** (`notebox-web/design/handoff`) as the visual and layout source of truth for every implemented screen — not only for the slice a feature's own FR names. Screens marked design-only (flow map, rationale) are excluded; a screen whose FR is unbuilt is future work, not a conformance gap. | Must | decisão humana 2026-08-26 + AD-06, D1 | US-7.1 | done |
**Validation baseline (OQ-09):** type name unique per tenant; type/field names + field type required; Number
fields carry optional per-field min/max; images PNG/JPEG/GIF/WebP; task/annotation names required; details optional.
**Type deletion (OQ-14):** deleting an annotation type is **blocked while it still owns annotation records**
(block, not cascade); enforced when record deletion lands in US-2.1.

## Non-Functional Requirements

| ID | Category | Requirement | Target | Origin |
|----|----------|-------------|--------|--------|
| NFR-01 | Security / isolation | No cross-tenant read or write on any tenant-owned endpoint. | 0 leaks; cross-tenant test per endpoint in CI. | BR-01,BR-02 + C-01 |
| NFR-02 | Localization | Every user-facing system message key resolves per locale. | 100% en + pt coverage; CI check. | BR-08 + C-09 |
| NFR-03 | Performance | Cached hot reads via Redis cache-aside. | Cache-hit read p95 < 50 ms; load test. | AD-13 + OQ-10 |
| NFR-04 | Integrity / safety | Image uploads content-type validated + size-bounded. | Max 5 MB; oversize rejected; test. | AD-04,AD-07 + C-07 + OQ-10 |
| NFR-05 | Resilience | Redis outage/cache miss degrades to MySQL without error. | Fault-injection: 0 errors. | AD-13 |
| NFR-06 | Contract | Every HTTP endpoint documented in OpenAPI, in sync. | 100% coverage; contract lint. | 03-standards |
| NFR-07 | Observability | Structured logging w/ correlation id; no secrets/PII/cross-tenant data. | Correlation id on every request; log review. | 01-arch, 02-compliance |
| NFR-08 | Performance / contract | List endpoints paginated. | Default page 50, max 200. | OQ-10 |
| NFR-09 | UI conformance | Every implemented `notebox-web` screen matches its design-handoff screen in layout and widget set. | 0 widgets present in a design screen and absent app-wide, excluding decisions recorded against a screen; gap report re-run per conformance feature. | FR-19 + AD-06 |

## Catalog history

| Date | PRD version | Change |
|------|-------------|--------|
| 2026-07-19 | v1 | Initial creation. |
| 2026-07-22 | v1 | Derived FR-01…FR-17, NFR-01…NFR-08 from approved PRD v1. |
| 2026-07-24 | v2 | Added FR-18 (secret-value encryption + audited reveal) from PRD v2 / OQ-15. Recorded OQ-14 type-delete rule. Reconciled FR-02→US-1.1 and FR-05→US-2.2 (US-1.2 deferred into US-2.2). |
| 2026-08-26 | v3 | Added **FR-19** and **NFR-09** (UI conformance to the design handoff), from a direct human decision — the source hierarchy ranks an explicit chat decision with the PRD. **Root cause they close:** no requirement covered design conformance, so each feature legitimately built only the slice its own FR named and cited the handoff for that slice; the remainder was never anyone's requirement and the delta accumulated silently across E1–E4. Scoped by the 20-screen gap report in `features/_design-conformance-report.md`. |
| 2026-08-30 | v3 | **Navigator presentation refined by direct human decision** (the source hierarchy ranks an explicit chat decision with the PRD). C28 has the sidebar tree listing Annotations and Tasks; built literally, the tree was identical on every tab while the tabs changed only the right pane, so the two chrome elements read as unrelated. **Decided:** the tree follows the level-1 tab — Overview shows both content branches (the reading C28 describes, on the screen it describes), Annotations and Tasks each show their own, and Administration lists its destinations (Groups, Members, Translations). C28 itself is unchanged: it is brief intake, not a decision record. **Also closed here:** the Administration tab matched only `/groups`, so reaching Members or Translations lit no tab at all — tab and tree now derive their section from one function (`lib/navigation/section.ts`), which is what makes them unable to disagree. |
| 2026-08-30 | v3 | **Administration removed from the level-1 tab strip** (direct human decision, same session). feat-018 had added it as a fourth tab from the handoff's screens 05/15; it is now reached from the menu bar only, with the Navigator listing its destinations once you are there. **Recorded deviation from the handoff**, not an omission — NFR-09 measures conformance against the handoff, and this tab is an explicit exception. Consequence accepted: Administration routes light no tab, which is correct for a menu destination. Also decided: the Navigator's vertical rule aligns just past the last tab; since tab widths come from translated labels (240.7px in en, 249.7px in pt) and the selected tab renders bold, the width is measured at runtime and published as `--nb-nav-strip-width` rather than hardcoded. |

## Rules
- IDs are immutable; deprecate, never reuse.
- Changes only via `/new-prd-version`.
