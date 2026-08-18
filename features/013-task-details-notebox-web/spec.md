# Feature — Task dates, card link and rich-text details UI (ADF Fusion theme)

**ID:** features/013-task-details-notebox-web
**User Story:** US-4.2
**Version:** v1
**Status:** Approved (human approval 2026-08-18)
**Date:** 2026-08-18
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-4.2 — the **web half**: the US-4.2 regions of design screens 15 (tasks list:
  derived Start/End columns, card link) and 16 (task detail: *Derived metrics* panel with the date
  span, the *Card* value-object panel, the subtask *Card* column, the *Details* tab), wired to
  feat-012's contract. feat-011 built these screens' US-4.1 subset and explicitly fenced these
  regions off ("the screens show them; this slice does not build them"); this feature fills them.
- **FRs covered (client obligations only):**
  - **FR-12** — *display* the derived task dates (BR-07): read-only everywhere, never typed, never
    sent; every repaint comes from the served `TaskDto`, the client computes no date.
  - **FR-13** — *edit and show* the inline card on a task and on a subtask: code + optional URL on
    the forms, the code rendered as an "Open" link when a URL exists (list rows, detail panel,
    subtask rows). The client re-runs no server rule; the server's card errors surface at the fields.
  - **FR-14** — *edit and render* the task's rich-text details with the **shipped** editor and
    renderer (feat-006's `RichTextEditor` / `RichTextValue` / DOMPurify dialect — the same allow-list
    the API enforces, C-08 client share); embedded images travel as `data-image-id` references
    uploaded through the shipped images client and resolved through the authenticated binary
    endpoint.
- **BRs bound:** BR-07 (dates displayed, never editable, never serialized), BR-06 (status still
  read-only — untouched), BR-05 (no new destructive act; clearing a card/details is the client's
  stated replace state, echoed deliberately), BR-08/C-09 (all new strings en/pt).
- **NFRs bound:** NFR-08 (the list keeps the shipped `af-statusBar` pager and server order — new
  columns only), NFR-01/NFR-02 as client obligations (authenticated fetch, localized strings).
- **Primary source:** the **feat-012 contract**
  (`features/012-task-details-notebox-api/contracts/task-details.md` — additive fields on
  `TaskInput`/`SubtaskInput`/`TaskDto`/`TaskListItemDto`/`SubtaskDto`, `CardInput`/`CardDto`, the 6
  new `task.*` keys, **PUT-replace semantics extended to `card`/`details`**, the poison
  `startDate`/`endDate`, and its *Documented edges*: `""` url is invalid — send `null`; `""` details
  is stored as `""` — only `null` clears; `details` never on listing rows); design handoff
  **screen 15** (Start/End columns, card link, "status and dates are read-only everywhere") and
  **screen 16** (Derived metrics panel: Status · Start date `min(subtask start)` · End date
  `max(subtask end)`; Card panel: Code · URL · Link "Open <code>" + the *not a shared entity*
  note; Subtasks grid Card column; Details tab; the "Span … → …" footer line); the shipped web
  infrastructure (feat-011's task module — `TasksTable`, `TaskForm`, `SubtasksPanel`,
  `TaskProgress`, `lib/tasks/viewModel.ts` full-echo builders, `tasksClient`; feat-006's rich-text
  stack; `authFetch`, guard, i18n, ADF Fusion theme).
- **Contract precedence note:** the design README's task state sketch (`derived: { percent, start,
  end }`) and its line "client mirrors the API rule … recompute immediately" are *behaviour
  reference only* — the wire truth is feat-012's `TaskDto` (`startDate`/`endDate`/`card`/`details`)
  and feat-011's established rule stands: **repaint from the returned parent task, no client
  computation**. Where they differ, the contract wins.
- **Rollout constraint inherited from feat-012's audit (finding 3):** the shipped feat-011 client
  sends `TaskInput`/`SubtaskInput` **without** `card`/`details`, so under PUT-replace every old-UI
  task edit and every subtask checkbox tick clears API-set cards/details. Closing that is a
  first-class obligation of this feature (scenarios 10 and 13), and the reason the pair should reach
  `main` together.

## Summary
A tenant member sees a task's **derived date span** — start (`min` subtask start) and end (`max`
subtask end) — on the tasks list and in the detail's *Derived metrics* panel, repainted from the
server after every subtask change and never typed. They give a task or a subtask an **inline card**
(a short code and an optional link) from the same forms they already use, see the code as an
"Open" link wherever the owner is shown, and read or edit the task's **rich-text details** in the
*Details* tab with the app's existing WYSIWYG editor, whose output is the same sanitized dialect the
API enforces. Every write keeps echoing what it doesn't change — card and details included — so
nothing is cleared by accident; an empty URL or an emptied editor is sent as `null`, per the
contract's edges. All new strings en/pt.

## Scope
- **In:**
  - **Types & client (contract sync):** `TaskInput` gains `card` (`CardInput | null`) and `details`
    (`string | null`) and structurally forbids the poison dates (`startDate?: never; endDate?:
    never`, the `status?: never` idiom); `SubtaskInput` gains `card`; `TaskDto`/`TaskListItemDto`
    gain `startDate`/`endDate`/`card`, `TaskDto` and `SubtaskDto` gain `details`/`card`
    respectively; `CardInput`/`CardDto` (`{ code, url | null }`). Endpoint set unchanged.
  - **Full-echo builders (the rollout fix):** `toSubtaskInput` echoes `card`; task edit builds its
    body from the loaded task — `card` and `details` included — so a name-only edit and a checkbox
    tick carry the existing values verbatim (**scenarios 10, 13**).
  - **Derived dates (screen 15 columns · screen 16 panel):** list rows show Start/End as served
    (locale date format, "—" when null); the detail's *Derived metrics* panel shows Status (as
    shipped), Start date with the `min(subtask start)` hint, End date with the `max(subtask end)`
    hint, Priority; the subtasks grid footer shows the "Span start → end" line when both exist. No
    task date input exists anywhere; the task request type cannot carry dates.
  - **Card on the task (screen 16 panel):** the create/edit task form gains Code (≤ 60) and URL
    (≤ 2048) fields; the detail shows the *Card* panel — Code, URL, and an "Open <code>" anchor
    (`href` = the served URL, `target="_blank"`, `rel="noopener noreferrer"`) when a URL exists,
    code only otherwise, an empty state when there is no card. **Send rule (client):** the card is
    sent when either field is non-empty (so a URL without a code surfaces the *server's*
    `task.card.code.required` at the code field — the same "let the server answer" posture as the
    unchosen priority); it is `null` when both are empty; an empty URL field is sent as `null`,
    never `""` (contract edge). Server errors route to the fields: `task.card.code.required` /
    `task.card.code.too_long` → code; `task.card.url.invalid` / `task.card.url.too_long` → URL.
  - **Card on the subtask (screen 16 grid column):** the inline subtask editor gains Code + URL with
    the same send rule; the grid gains a Card column (code as link when URL, "—" when none).
  - **Rich-text details (screen 16 Details tab):** the tab renders served details through the
    shipped `RichTextValue` (client sanitizer = defence in depth; embedded `data-image-id` images
    resolve through the authenticated images endpoint) or an empty state; editing uses the shipped
    `RichTextEditor` (formatting, colour, links, code, image embed via `imagesClient` upload). Save
    sends the editor's dialect HTML in `details`; **an emptied editor is sent as `null`** so no
    `<p></p>` ghost is stored (contract: only `null` clears); the server's `task.details.too_long`
    surfaces as the form-level error. Details are read from `TaskDto` only — the list never has
    them, and the client never asks the list for them.
  - **Repaint from the response (unchanged rule):** every task or subtask mutation repaints dates,
    card, subtasks and progress from the returned `TaskDto` — one round trip, no client math.
  - **Localization:** every new string (panel titles, hints, column headers, field labels, empty
    states, the "Open" link text) in en + pt; server messages arrive localized via `Accept-Language`.
- **Out:**
  - **Screen 16 chrome not backed by an FR:** the *History* tab, the *Notes* drawer, "Mark all done"
    / "Reset" bulk actions, and "Assign group" (E3) — feat-011 already excluded them; nothing in
    US-4.2 adds them.
  - **Groups / navigation tree** (E3, FR-08/09), and screen 15's Group column/tree — E3.
  - **Client-side sorting/filtering** by dates or card — the server's order is the order.
  - **Any client-side derivation** of dates or status, any client validation that duplicates the
    server's card/URL/size rules beyond the send rule above (the server's localized answer is the
    truth), any date input for the task.
  - **Server behaviours** — sanitization (input/output), byte bound, backfill, replace semantics:
    feat-012's, consumed not re-implemented.
  - **A second rich-text stack** — the feat-006 editor/renderer/dialect is reused as-is; if the
    dialect ever changes it changes there, once.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-12 Derived dates are displayed, never typed (BR-07)
  Scenario: The detail shows the served span with its derivation hints
    Given the API serves a task with startDate 2026-08-04 and endDate 2026-08-29
    When a member opens the task detail
    Then the Derived metrics panel shows Start date 04-Aug-2026 with the min(subtask start) hint
    And End date 29-Aug-2026 with the max(subtask end) hint
    And the subtasks footer shows the span 04-Aug-2026 → 29-Aug-2026

  Scenario: A task without derived dates shows the empty marks
    Given the API serves a task whose startDate and endDate are null
    When a member opens the task detail
    Then both dates render as "—"
    And no span line is shown

  Scenario: A subtask change repaints the dates from the returned parent
    Given a task detail showing the span 04-Aug-2026 → 29-Aug-2026
    When the member edits a subtask's dates and the API returns the parent with endDate 2026-09-05
    Then the panel and the footer show the end date 05-Sep-2026 without any further request

  Scenario: Task dates cannot be sent, structurally
    Given the task edit form
    When the member saves any change
    Then the request body contains no startDate and no endDate key
    And no date input for the task exists in the form

  Scenario: The list shows the served dates per row
    Given the API serves a page where task "T1" has dates 2026-08-04/2026-08-29 and task "T2" has none
    When a member opens the tasks list
    Then T1's row shows Start 04-Aug-2026 and End 29-Aug-2026
    And T2's row shows "—" in both columns

Feature: FR-13 The inline card on task and subtask (OQ-06)
  Scenario: The detail shows the card with an Open link
    Given the API serves a task with card code "OPS-2481" and url "https://tracker.example/OPS-2481"
    When a member opens the task detail
    Then the Card panel shows Code OPS-2481, the URL, and a link "Open OPS-2481"
    And that link opens the URL in a new tab with rel noopener noreferrer

  Scenario: A code-only card shows no link
    Given the API serves a task with card code "OPS-2482" and no url
    When a member opens the task detail
    Then the Card panel shows Code OPS-2482 and no Open link

  Scenario: Creating a task with a card sends it
    Given the new-task form
    When the member fills name "Broker migration", priority High, card code "PAY-231" and url "https://tracker.example/PAY-231" and saves
    Then the POST body carries card { code: "PAY-231", url: "https://tracker.example/PAY-231" }
    And the detail repaints from the response

  Scenario: An empty URL field is sent as null, never as an empty string
    Given the new-task form with card code "PAY-232" and the URL field left empty
    When the member saves
    Then the POST body carries card { code: "PAY-232", url: null }

  Scenario: Server card errors surface at their fields
    Given the new-task form with card code left empty and url "https://tracker.example/x"
    When the member saves and the API answers 400 with task.card.code.required
    Then the localized message shows at the card code field
    And when the API answers task.card.url.invalid the message shows at the URL field

  Scenario: Editing a task echoes the card and details it does not change
    Given a task served with card "PAY-231"/"https://tracker.example/PAY-231" and details "<p>plan</p>"
    When the member opens Edit, changes only the name, and saves
    Then the PUT body carries card { code: "PAY-231", url: "https://tracker.example/PAY-231" } and details "<p>plan</p>" verbatim

  Scenario: Clearing both card fields removes the card
    Given a task served with a card
    When the member opens Edit, empties both card fields, and saves
    Then the PUT body carries card null
    And the Card panel shows its empty state after the repaint

  Scenario: A subtask carries a card under the same rules
    Given the task detail
    When the member adds a subtask "Contract review" with card code "LEG-7" and url "https://tracker.example/LEG-7"
    Then the POST body carries card { code: "LEG-7", url: "https://tracker.example/LEG-7" }
    And the subtask row shows LEG-7 as a link in the Card column

  Scenario: Ticking a subtask echoes its card
    Given a subtask served with card "LEG-7" and done false
    When the member ticks its checkbox
    Then the PUT body is the full subtask — name, both dates, done true, and card { code: "LEG-7", url: … } unchanged

  Scenario: The list shows the task card link per row
    Given the API serves a page where "T1" has card "OPS-2481" with a url and "T2" has no card
    When a member opens the tasks list
    Then T1's row shows OPS-2481 as a link and T2's row shows "—" in the Card column

Feature: FR-14 Rich-text details in the Details tab (C-08 client share)
  Scenario: Served details render through the client sanitizer
    Given the API serves details "<p>before</p><script>steal()</script><img data-image-id=\"i9\" alt=\"d\">"
    When a member opens the Details tab
    Then the text "before" is rendered, no script element exists in the DOM
    And the image is requested through the authenticated images endpoint by its data-image-id

  Scenario: Editing details uses the shipped editor and saves its dialect
    Given a task with details "<p>plan</p>"
    When the member edits the details to add a bold word and saves
    Then the PUT body carries details containing <strong>
    And the Details tab repaints from the response

  Scenario: An emptied editor clears the details
    Given a task with details "<p>plan</p>"
    When the member deletes all content in the editor and saves
    Then the PUT body carries details null
    And the Details tab shows its empty state after the repaint

  Scenario: The server's size rejection surfaces at the form
    Given the details editor
    When the member saves and the API answers 400 with code task.details.too_long
    Then the localized message shows as the form-level error and the stored details are unchanged

  Scenario: A task without details shows the empty state and sends none
    Given the API serves a task with details null
    When a member opens the Details tab
    Then the empty state is shown
    And creating a task without touching the editor sends details null

Feature: Cross-cutting UI obligations
  Scenario: Portuguese locale end to end
    Given the member's locale resolves to pt
    When they open the task detail and trigger a card code error
    Then panel titles, hints, column headers and empty states are the Portuguese catalog strings
    And the server's message at the code field is its Portuguese text

  Scenario: Guard and not-found idioms are unchanged
    Given no session
    When a client opens a task detail URL
    Then it is redirected to the login as before
    And a foreign or missing task id still shows the established not-found idiom
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies (client obligation).** All calls keep going through
  the shipped authenticated fetch and clients; images resolve through the authenticated binary
  endpoint. *Evidence:* the guard/not-found regression scenario + no new fetch path.
- [x] **C-02 · Authenticated by default** — **applies.** Same guarded routes; no new route.
  *Evidence:* the guard scenario.
- [ ] **C-03 · Least-privilege** — **n/a.** No admin surface.
- [ ] **C-04 · Personal data minimization** — **n/a.** No identity data beyond the session.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential or config.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Same origin/TLS posture.
- [x] **C-07 · Image upload safety** — **applies (client share).** Embedded images are uploaded
  only through the shipped `imagesClient` (the API validates content type and size — NFR-04) and
  rendered only via `data-image-id` through the authenticated endpoint; the client never inlines
  a binary or a foreign `src`. *Evidence:* the served-details render scenario (image resolved by
  id) + reuse of feat-006's editor image path.
- [x] **C-08 · Rich-text sanitization** — **applies (client share, defence in depth).** Every
  rendered details value passes the shipped DOMPurify allow-list — the same dialect the API
  enforces on input and output; the editor emits only that dialect. *Evidence:* the hostile-served-
  details scenario (no script in the DOM) + reuse of `sanitize.ts`/`RichTextValue`.
- [x] **C-09 · Localization completeness** — **applies.** Every new UI string in en + pt; server
  messages consumed localized. *Evidence:* the pt end-to-end scenario + the satellite's catalogs.
- [ ] **C-10 · Audit trail** — **n/a client-side.** No new destructive act.
- [ ] **C-11 · Data retention** — **n/a.**
- [ ] **C-12 · Secret values** — **n/a.**

## Open Questions
- **None new.** Two client-side decisions are declared here rather than left to implementation,
  both direct consequences of feat-012's documented contract edges: (a) an **empty URL field is
  sent as `null`** and a card is sent only when a field is non-empty (`null` when both empty); (b)
  **an emptied details editor is sent as `null`** (only `null` clears; a `<p></p>` ghost would
  otherwise be stored). Both are reversible UI conventions, flagged for the gate.
