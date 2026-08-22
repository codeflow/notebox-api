# Audit — Task dates, card link and rich-text details UI (feat-013, US-4.2 web)

**Date:** 2026-08-21 · **Auditor:** session (opus·xhigh discipline) + independent adversarial
sub-agent with fresh context for the author-bias-exposed checks (traceability, scenario honesty,
attack pass, test re-run), the feat-010/012 precedent. **Method:** all six mandated checks ran
explicitly. Constitution/compliance/scope/OQ checks ran in the session by grep over the satellite's
`git diff develop..HEAD` (28 files, +1507/−100; five per-task commits `f7c85db..4ef3493`).
Checks 1–2 and the sub-agent's independent re-run are recorded below. **Live pass** (T-05): real
API on `develop` at V6, seeded member — create-with-card → detail panels → subtask add derives the
span → tick keeps the card → details edit/clear → list columns; zero console errors.

## Check 1 — Traceability: PASS
The sub-agent mapped all **22 scenarios** to named tests and the production path each drives — no
broken link (22/22; four rated *weak*, all closed by the hardening below). Highlights: the
send-rule/echo scenarios are pinned **pure** (`viewModel.test`) *and* **on the wire** through
MSW `onBody` captures with non-null values (`integration`, `SubtasksPanel.test`, `TaskForm.test`),
so a builder that dropped `card`/`details` fails on a missing JSON key, not a `null`; dates are
proven served-not-computed by fixtures whose task dates are **underivable** from their subtasks;
the read-side sanitizer is proven with hostile served markup; guard/not-found regression rides
`RouteGuard.test.tsx` (10 tests, ran) + the integration not-found case. Full 22-row table in the
sub-agent run. Rows rated weak before hardening: 8/13 (URL-typed body never captured — **W1**),
3 ("no further request" not asserted as a count — W3), 21 (the pt *card-error* leg not literal —
W4).

## Check 2 — Scenario honesty: PASS with findings
Regression by regression, the suite catches: a builder dropping `card`/`details` (exact `toEqual`
with non-null values); `url: ''` (pure + form); `<p></p>` sent (pure + tab); client-computed dates
(underivable fixtures); a skipped sanitizer (`<script>` assertion); `card.code` routed onto `name`
(routing units + aria-invalid). The `TaskDetailsTab` suite **stubs `RichTextEditor`** (jsdom cannot
host TipTap — feat-006 precedent); the sub-agent verified headless that the real editor's
`clearContent()`/`setContent('')` yield `<p></p>` → `isEmptyRichText` → `null` ✓, and that the stub
hides nothing the real editor would break (the real editor ignores `value` after mount — safe,
the mode switch remounts; async mount leaves Save enabled before ready — harmless). Weaknesses
that survived, none blocking: **W1** URL input → body wiring never captured (→ H1); **W2**
`<p></p><p></p>` / `<p><br></p>` were *not* treated as empty (→ H2, attack 6); W3 (count not
asserted — accepted; the response-repaint joint is the same `run()`/`onTaskUpdated` path proven by
the tick case); **W4** pt card-error leg (→ H5); W5 page-level edit→save→repaint asserted as
`onTaskUpdated(saved)` (jsdom cannot type into TipTap — accepted, the live pass covered it); W6
`next/navigation` mocked, guard delegated to `RouteGuard.test` — as the spec declared.

## Check 3 — Constitution (client obligations): PASS
- **INV-W1 / BR-07 (dates displayed, never derived or sent):** the task module contains no date
  arithmetic — the only `Math.max` is feat-011's pager page-count (`TasksTable.tsx:53`);
  `formatIsoDate` formats a served string, derives nothing. `TaskInput` carries
  `startDate?: never; endDate?: never` next to `status?: never` (`lib/api/types.ts:189-191`), and
  `toTaskInput` emits exactly `name / priority / card / details` — no task date or status key can
  be constructed. No task date input exists in any form (integration test asserts it).
- **BR-06 untouched:** status still display-only; `TaskProgress`/`PriorityLabel` unchanged.
- **INV-W2 (replace semantics honoured — the feat-012 rollout fix):** `SubtaskInput.card` is a
  *required* key (omission = compile error); `toSubtaskInput` echoes `card`; the task edit page
  seeds `TaskForm` from `taskToDraft(task)` and the details tab builds its body from
  `taskToDraft(task) + details` — card and details ride every update verbatim unless changed.
- **INV-W3/W4 (contract edges):** `toCardInput` never emits `url: ''` (null), is null iff both
  fields blank; `toDetails` maps `''`/`<p></p>` → null. Both pure, both unit-pinned.
- **C-08 one dialect (AD-level):** `innerHTML` and DOMPurify exist only in the shipped
  `RichTextValue`/`sanitize.ts`; `TaskDetailsTab` composes `RichTextValue`/`RichTextEditor`
  unchanged; the editor/renderer/sanitizer/extensions files are untouched.
- **AD (client) — one fetch path:** no raw `fetch(` in the task module or `tasksClient`; every
  call goes through the shipped `authFetch`; images only via `imagesClient` (renderer) and
  `useRichImageUpload` (editor).
- **Code standards:** English identifiers, Javadoc-style doc comments on every new public
  component/function, `next lint` + prettier clean (part of `npm run verify`).

## Check 4 — Compliance: PASS
Every item marked `applies` in the spec's pre-flight has its evidence where the spec said:
- **C-01/C-02 (client obligation):** no new route, no new fetch path; `RouteGuard.test.tsx` and
  the integration suite's not-found case still run (the spec's regression evidence).
- **C-06 (standing):** same origin/TLS posture.
- **C-07 (client share):** embedded images are uploaded only through `useRichImageUpload`
  (`validateIconFile` pre-validation → `imagesClient.upload`, behaviour identical to the code it
  was extracted from — `ValueField` now imports it) and rendered only via `data-image-id` through
  `imagesClient.fetchObjectUrl`; `TaskDetailsTab.test` asserts the by-id fetch.
- **C-08 (client share):** every rendered details value passes `sanitizeRichText` inside
  `RichTextValue` — `TaskDetailsTab.test` plants `<script>`/hostile markup in the served DTO and
  asserts no script element reaches the DOM; the integration suite renders real served details.
- **C-09:** 27 new keys line-parallel in en and pt (`keysetCoverage.test.ts` pins parity); the
  integration suite's pt case asserts the new regions' Portuguese strings and no raw `tasks.` key.

## Check 5 — Scope: PASS (one deviation, recorded at the moment it happened)
`git diff --name-only develop..HEAD` matches the plan's blast radius: the five new source files
(`CardLink`, `CardFields`, `TaskDetailsTab`, `useRichImageUpload`, `formatDate`) + their tests, and
the enumerated modified set (types, viewModel, `TaskForm`/`SubtasksPanel`/`TasksTable` + tests,
`ValueField` import-only, the three task pages, the integration suite, MSW handlers, en/pt,
overrides CSS). Every file the plan marked **explicitly untouched** is untouched: `tasksClient.ts`,
`authFetch.ts`, `imagesClient.ts`, `RichTextEditor`, `RichTextValue`, `sanitize.ts`,
`editorExtensions.ts`, `TaskProgress`, `PriorityLabel`, both delete dialogs, `RouteGuard`, the base
`adf-fusion.css`. Exactly one file beyond the enumeration — `lib/api/tasksClient.test.ts` — a
type-forced widening of four input literals (the required `card` key), recorded on T-01; the
module it tests is untouched. Not scope creep.

## Check 6 — Open Questions: PASS
No OQ was opened or closed during implementation. The two client conventions (empty URL → `null`;
emptied editor → `null`) were declared in the **approved spec**, and the three plan decisions
(details edited inline in the Details tab; display-only Card panel; `dd-MMM-yyyy` incl. subtask
cells) were approved at the plan gate — none is an implementer assumption.

## Attack pass (session) — findings, ranked

1. **`""` details from another client is normalised to `null` on the next edit — suspicion-level,
   contract-consistent.** feat-012 stores `""` as `""` (its documented edge); this client's
   `taskToDraft` → `toDetails('')` → `null`, so a name-only edit of such a row clears `""` to
   `null`. Both are "no details"; this client never produces `""` itself; only rows written by a
   client that sends `""` are affected. Not a defect against either contract — noted for
   completeness; no action.
2. **Inactive tab panel uses the `hidden` attribute** so the subtask inline editor survives a tab
   switch (plan) — correct for AT and for RTL role queries (hidden panels are excluded by
   `getByRole`); `getByText` would still see hidden text, which is why the integration assertions
   scope to the visible panels. Observation, no action.

## Attack pass (sub-agent, fresh context) — merged findings, ranked
Nothing blocking survived. Findings:
3. **non-blocking** — `toCardInput` maps only the exact `''` url to `null`; `{ code: 'X', url: ' ' }`
   or a trailing space is sent verbatim → 400 `task.card.url.invalid` at the URL field. Per plan
   alternative 6 (no trimming); UX nit → contract edge (H6).
4. **non-blocking** — `formatIsoDate` threw `RangeError` on malformed ISO and printed the split day
   verbatim; unreachable via the API's `LocalDate`. → guarded (H4).
5. **suspicion (neutralised)** — `CardLink` renders `href` verbatim with no scheme guard; the API
   rejects non-http(s)/authority-less URLs, React rewrites `javascript:` hrefs, and no card row
   predates V6 — no path found.
6. **non-blocking** — Details empty state was keyed on `=== null`; the API's storable `""`/`<p></p>`
   rendered a blank renderer → keyed on `isEmptyRichText` (H2).
7. **non-blocking** — Details-tab save error collapses field violations to the top-level message;
   reachable only via a hypothetical legacy row → contract edge (H6).
8. **non-blocking** — ghost content: Enter in an empty editor then Save stored `<p></p><p></p>` /
   `<p><br></p>` (exact-match emptiness) → normalised (H2).
9. **non-blocking** — stale doc comments: `edit/page.tsx` ("name and priority") → fixed (H3);
   `tasksClient.ts:39` ("Updates name and priority only") — the file is on the plan's
   **explicitly-untouched** list, so it stays as recorded backlog rather than a boundary breach.
10. **verified OK** — `hidden` tab panels agree with the base `.af-tabPanel{display:none}/.active`
    rules; both panels mounted so the subtask editor survives a switch (`aria-controls` a11y nit
    → backlog); save body built from the current `task` prop (not stale after a tick); routing
    tails match `update.input.card.code` / `addSubtask.input.card.url` before `name`; 28 keys
    identical en/pt; `useRichImageUpload` byte-equivalent to the removed inline code and still
    exercised by `ValueField.test`; 6-column grids with consistent `colSpan`s; every mutation
    call site goes through the echo builders; details read only from `TaskDto`.

**Independent re-run (sub-agent):** `npx vitest run` 55 files / 397 passed / 0 failed; `tsc` exit 0;
`next lint` + prettier clean (one pre-existing warning outside the diff). Session gate at implement
close: `npm run verify` 397 ✓ + the live pass.

## Verdict: **PASS with findings**

No spec/plan/contract divergence, no constitution or compliance breach, no scope creep (one
recorded type-forced test widening), no OQ closed by assumption; nothing blocking survived either
attack pass. The findings split three ways:

**Hardening (same day, before publish — the feat-010/012 precedent), all landed, verify green at
402 tests (397 → 402):**
- **H1 ✔** URL-typed bodies captured on the wire: `TaskForm.test` POSTs a typed URL verbatim with
  the code; `SubtasksPanel.test` does the same through the inline editor (W1, rows 8/13).
- **H2 ✔** `isEmptyRichText` normalises empty documents (`''`, `<p></p>`, stacked empty
  paragraphs, break-only paragraphs — text or real markup stays content); `toDetails` inherits
  it, and the Details tab's empty state is keyed on it (attack 6 + 4); unit + tab tests.
- **H3 ✔** `edit/page.tsx` doc comment corrected (attack 9); the `tasksClient.ts` comment is
  backlog (plan-untouched file).
- **H4 ✔** `formatIsoDate` returns a non-`yyyy-MM-dd` value verbatim instead of throwing (attack
  4); unit test.
- **H5 ✔** the pt *card-error* leg of scenario 21 is literal: the server's Portuguese
  `task.card.code.required` message at the code field (W4).
- **H6 ✔** `contracts/interfaces.md` documented edges: no client trimming (attack 3), the
  empty-document normalisation and its render consequence, the details-tab error collapse (attack
  7), the tabs a11y nit, the ISO guard.

**Backlog (recorded, not actioned here):** `tasksClient.ts:39` stale comment (plan-untouched file);
`aria-controls` on the detail tabs; the `LoginForm` under-load timing flake observed once during
T-04 (pre-existing, untouched component); a page-level edit→save→repaint case would need a real
editor driver (jsdom cannot type into TipTap) — the live pass is the evidence today.

Gate `audit_pass` opens. `catalogs/epics.md`: US-4.2 stays `building` with the web audit noted —
the story is `delivered` only when feat-013 merges into `develop` (US-2.1/US-4.1 rule). Issue #12
gets the verdict comment; nothing merges or closes here (publish → review → promotion).
