# Feature — Rich Free-text values sanitized on input and output

**ID:** features/007-rich-text-sanitization
**User Story:** US-2.1
**Version:** v1
**Status:** Draft
**Date:** 2026-08-12

## Origin
- **User Story:** US-2.1 — the API half's **compliance completion**. The OQ-19 decision (2026-08-11)
  made Free-text annotation values rich HTML, which extended **C-08**'s reach from FR-14 task details
  to annotation values. C-08 binds whichever feature *stores or returns* rich text to sanitize it on
  **input/output** — that is this API, which today stores and returns the markup verbatim. The gap was
  recorded in feat-006 spec v2 §Dependency, restated by both feat-006 audit rounds, and marked in
  `catalogs/epics.md` as the **promotion blocker** for everything on `develop`.
- **FRs covered:** FR-04 (record values — the Free-text value's stored and returned form is now the
  *sanitized* form). No new FR: this hardens an existing surface.
- **BRs bound:** BR-03 (a record conforms to its type — a Free-text value's content is now additionally
  bound to the dialect), BR-05 (nothing here deletes user content beyond stripping non-dialect markup —
  the stripping itself is the explicitly decided C-08 behaviour, not silent destruction).
- **Compliance driver:** **C-08** — *"sanitized on input/output to an allow-list, preventing stored XSS
  delivered to notebox-web"*.
- **Primary source:** PRD v2 §3.1 (the OQ-19 refinement note: values *"sanitized on input and output to
  an allow-list, exactly as task details are … the sanitization obligation is the API's, with
  client-side sanitization on render as defence in depth"*); the **stored-dialect contract** the web
  client froze — `notebox-web/lib/annotationRecords/sanitize.ts` and feat-006 `data-model.md`'s
  allow-list table; feat-005's REST contract (`features/005-annotation-records-notebox-api/contracts/`).

## Summary
Close the C-08 gap on the API: every rich Free-text annotation value is sanitized to the **stored
dialect allow-list** when it is written and again when it is served, so hostile markup POSTed straight
at the API — bypassing the web client entirely — can neither persist as a stored-XSS payload nor reach
any client. Dialect-clean content (everything the feat-006 editor can emit) passes through
**byte-identical**, so legitimate round-trips never corrupt. Values stored before this feature are made
inert by the read-side pass without any data migration. Nothing else about records changes: masking,
reveal, encryption, bounds and the error envelope keep their shipped behaviour.

## The dialect (the allow-list contract)
The allow-list is the one the web client already enforces on render (feat-006 `data-model.md`,
mirrored in `sanitize.ts`) — one dialect, two enforcement points:

| Allowed | Constraint |
|---|---|
| `p`, `br`, `strong`, `em`, `u`, `s`, `ol`, `ul`, `li`, `blockquote`, `code` | no attributes |
| `span` | only a colour declaration in `style` |
| `a` | `href` http/https only; `rel` forced to `noopener noreferrer`; nothing else |
| `pre` | only `data-language` |
| `img` | only `data-image-id` and `alt` — **never `src`** |

Everything else — elements, attributes, URL schemes, style properties — is stripped; text content of
stripped elements is kept, except script/style bodies which are dropped entirely.

## Scope
- **In:**
  - **Sanitize on write (FR-04, C-08):** the Free-text value stored by create and update is the
    sanitized form of what the caller sent; the response DTO echoes that sanitized form, so the caller
    sees exactly what was kept.
  - **Sanitize on read (C-08):** every path that returns a Free-text value serves the sanitized form —
    including rows written before this feature existed. No migration required for safety.
  - **Dialect fidelity:** content already inside the dialect round-trips **byte-identical** through
    write and read — the criterion that keeps the web editor's identity pin (`sanitize(getHTML())` =
    identity) true end to end.
  - **Bounds interplay:** the existing length bound applies to the **stored (sanitized) form**; the
    existing `annotation.record.value.too_long` behaviour is unchanged.
- **Out:** see **Out of scope** below.

## Acceptance criteria (Gherkin)
> "Free-text field" below means a non-secret `FREE_TEXT` field. The dialect table above is the
> allow-list under test. These scenarios become executable API tests in `implement`.

```gherkin
Feature: Hostile markup dies at the API boundary (C-08)

  Scenario: A script element is stripped on write, body and all
    Given a type with a Free-text field "Notes"
    When a client creates a record with Notes = "<p>before</p><script>steal()</script><p>after</p>"
    Then the create succeeds
    And the stored and returned Notes value contains "before" and "after"
    And it contains no script element and no trace of "steal"

  Scenario: Event-handler attributes are stripped
    When a client writes Notes = "<p onclick=\"steal()\">text</p><img data-image-id=\"i1\" onerror=\"steal()\">"
    Then the returned value keeps the paragraph text and the image reference
    And no on* attribute survives

  Scenario: javascript: and data: links lose their href but keep their text
    When a client writes Notes containing <a href="javascript:alert(1)">click</a>
    Then the returned value contains "click"
    And no javascript: URL survives anywhere in it

  Scenario: An img src is stripped while the dialect reference survives
    When a client writes Notes containing <img src="https://evil.example/x.png" data-image-id="i1" alt="d">
    Then the returned value carries data-image-id="i1" and alt="d"
    And no src attribute and no "evil.example" survive

  Scenario: Unknown elements are dropped, their text kept
    When a client writes Notes = "<iframe src=\"https://evil.example\"></iframe><marquee>hi</marquee>"
    Then the returned value contains "hi"
    And contains neither an iframe nor a marquee element

  Scenario: Style is reduced to the colour declaration
    When a client writes Notes containing <span style="color: rgb(200, 30, 30); position: fixed">warn</span>
    Then the returned span keeps its colour
    And no position declaration survives
```

```gherkin
Feature: The dialect round-trips byte-identical (FR-04)

  Scenario: A dialect-clean value is stored and served unchanged
    Given a Notes value using every dialect construct — formatting marks, lists, blockquote, a colour span, an http link with the forced rel, inline code, a pre[data-language] code block, and a data-image-id image
    When a client creates the record and reads it back
    Then the returned Notes value is byte-identical to what was sent

  Scenario: Sanitization is idempotent
    Given a Notes value that has already been sanitized once
    When it is written and read again
    Then the value does not change further

  Scenario: An update is sanitized exactly like a create
    Given a stored record with a clean Notes value
    When a client updates Notes to hostile markup
    Then the newly stored and returned value is the sanitized form
```

```gherkin
Feature: Values stored before this feature are served inert (C-08)

  Scenario: A legacy hostile row cannot reach a client
    Given a Free-text value written directly to the database containing a script element and an onerror attribute
    When a client reads the record through the API
    Then the returned value carries the surviving text and dialect markup only
    And no script element and no event-handler attribute reach the client
```

```gherkin
Feature: Everything else about records is untouched

  Scenario: Plain TEXT values are not treated as markup
    Given a type with a plain TEXT field "URL"
    When a client writes URL = "<b>not markup</b>"
    Then the stored and returned URL value is exactly "<b>not markup</b>"

  Scenario: Secret values keep their shipped behaviour
    Given a Secret field holding a value
    When the record is read and the value revealed by an ADMIN
    Then masking, reveal and the audit entry behave exactly as before this feature
    And the revealed cleartext is the stored secret, not a sanitized transform

  Scenario: The length bound applies to the stored form
    Given a Notes value whose sanitized form exceeds the column bound
    When a client writes it
    Then the write is rejected with the existing annotation.record.value.too_long behaviour
```

## Compliance pre-flight
Checklist from `constitution/02-compliance.md`, marked for this API hardening:

- **C-01 · Tenant isolation** — **applies (inherited, unchanged).** No new query; sanitization happens
  inside the existing tenant-scoped read/write paths. *Evidence:* existing isolation tests stay green.
- **C-02 · Authenticated by default** — **applies (inherited, unchanged).** No new endpoint.
- **C-03 · Least-privilege authorization** — **not applicable.** No new privileged action.
- **C-04 · Personal data minimization** — **applies.** Sanitization logs nothing of the value's
  content. *Evidence:* no-content-in-logs review.
- **C-05 · Secrets never committed** — **not applicable.** No new secret or key.
- **C-06 · Encryption in transit** — **applies (inherited, unchanged).**
- **C-07 · Image upload safety** — **not applicable.** No change to the upload path; embedded images
  remain `data-image-id` references (the dialect strips `src` — closing the one vector where a stored
  value could reach out to a remote host).
- **C-08 · Rich-text sanitization** — **applies — this feature IS the evidence.** Input/output
  allow-list sanitization of Free-text annotation values. *Evidence:* the hostile-markup scenarios
  (write side), the legacy-row scenario (read side), and the byte-identity scenarios (the allow-list
  is exact, not approximate).
- **C-09 · Localization completeness** — **not applicable (no new text).** Sanitization strips rather
  than rejects, so no new error key or message exists; existing keys are untouched.
- **C-10 · Audit trail** — **not applicable.** Sanitization is not an audited action; delete/reveal
  audits are untouched.
- **C-11 · Data retention** — **not applicable.**
- **C-12 · Encryption at rest for secret values** — **applies (guarded, unchanged).** Secret values are
  never sanitized (they are encrypted text, not rendered HTML); the "Secret values keep their shipped
  behaviour" scenario pins that this feature does not touch them.

## Out of scope
- **Client-side render sanitization** — stays in feat-006 as defence in depth, unchanged.
- **Task details rich text (FR-14, US-4.2)** — a different surface; its future feature should reuse
  the sanitizer this feature introduces, but nothing is built for it here.
- **Plain TEXT / secret values** — not markup surfaces; explicitly pinned untouched by scenarios.
- **Retro-migration of stored rows** — the read-side pass makes legacy rows inert on every exit path;
  rewriting stored data is deliberately deferred (reversible later; the safety property does not
  depend on it).
- **Rejecting hostile input with an error** — C-08 prescribes sanitize-to-allow-list, not rejection;
  a 400-on-markup policy would retroactively break any caller that ever stored non-dialect content.

## Open Questions
No new Open Question. OQ-19 (resolved 2026-08-11) is the deciding record; the dialect is the one
feat-006 froze — this spec adds no new decision surface.
