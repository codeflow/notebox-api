# Feature — Login layout A: the two-pane brand card (design screens 02/03)

**ID:** features/017-login-layout
**User Story:** US-7.1
**Version:** v1
**Status:** Approved (human approval 2026-08-26)
**Date:** 2026-08-26
**Project:** `notebox-web` (react/next) — routed satellite

## Origin
- **User Story:** US-7.1 — *As the product owner, I want every implemented `notebox-web` screen to match the design handoff, so the delivered UI is the one that was designed.*
- **FRs covered:** **FR-19** — `notebox-web` follows `notebox-web/design/handoff` as the visual and layout source of truth for every implemented screen. This feature covers the **login surface only** (screens 02 and 03).
- **NFRs bound:** **NFR-09** (UI conformance: no widget present in a design screen and absent app-wide, excluding deviations recorded against a screen); **NFR-02 / C-09** (every user-facing string in en **and** pt).
- **BRs bound:** none newly. The authentication behaviour this screen drives is unchanged and stays bound to its original rules.
- **Primary source:** human decision 2026-08-26 ("*Eu precisava que o notebox-web seguisse esse layout*") + AD-06 (UI lives in the satellite); `notebox-web/design/handoff/design/Notebox Screen Flows.dc.html` **screens 02 and 03**; `notebox-web/design/handoff/README.md` rows 02/03, which state layout A *"replaces the current centred `nb-loginShell` card"*; `features/_design-conformance-report.md` §C-2 (L-1).
- **Consumes (all already shipped):** the `LoginForm` component and its `validateLogin` rules, `AuthProvider`, `useTranslation` with the `en`/`pt` `MessageKey` catalogs, `src/styles/adf-fusion.overrides.css` (never `adf-fusion.css`), Vitest + Testing Library + MSW.

## Summary
The sign-in screen is re-laid-out to design **layout A**: a single **700 px two-pane card** — a
gradient **brand pane** on the left carrying the Notebox lockup, tagline, three product bullets and
a release line, and a **330 px credentials pane** on the right holding the existing form. The
credentials pane gains the design's *Sign in* heading and hint, moves each label **above** its
field, and wraps email and password in `af-comboField` with a trailing `af-iconButton` adornment —
two ADF widgets that exist in the theme sheet but are used by **no screen in the app today**. The
outer `af-panelBox` card disappears; the `<form>` becomes the card.

**This feature changes layout, not behaviour.** Validation rules, error codes, the "no request while
invalid" guarantee, the session-expired warning and the sign-in call are all untouched — the
scenarios below pin them as regressions, not as new work.

## Scope

- **In:**
  - **The two-pane card (screens 02/03).** The `<form>` itself is the bordered, shadowed, white
    card. A brand pane that grows, a credentials pane pinned to a fixed width. The centred
    single-pane `af-panelBox` layout and its `nb-loginShell .af-panelBox { flex: none; width: 350px }`
    pinning are removed.
  - **The brand pane.** Notebox logo + `NOTEBOX` wordmark, a tagline, three product bullets, and a
    footer release line above a hairline rule. Gradient background, light text.
  - **The credentials pane.** *Sign in* heading, a `.tip` hint line, then the existing
    `nb-loginFields` / `nb-loginForm` block unchanged in behaviour.
  - **Label-above rows.** `nb-loginRow` goes from the current two-column `68px 1fr` grid to a single
    column, label above field, left-aligned.
  - **`af-comboField` + `af-iconButton`.** Email and password each become a combo field with a
    trailing icon adornment (✉ / 🔒). Both classes already exist in `adf-fusion.css`; neither is
    used anywhere in the app. **Decorative only** — non-interactive and hidden from assistive tech.
  - **Full-width submit**, then the options row (*Remember me* + the locale control) and the links
    row (*Forgot password?* / *Add account*) at the pane's two edges.
  - **The locale control, working for this screen only** (**OQ-28 (b)**, decided 2026-08-26). It
    switches the sign-in screen's own language — labels, placeholders, validation errors and the new
    brand-pane copy — and that is all it does: it persists nothing, makes no request, and is
    superseded by the member's stored preference once they are in. It is the one screen where a
    person has no stored preference to fall back on, which is exactly why it earns a switcher here
    and nowhere else.
  - **New strings in en + pt** for the tagline, the three bullets, the release line, the heading and
    the hint.
- **Out:**
  - **Making the placeholder affordances work.** *Remember me*, *Forgot password?* and *Add account*
    stay exactly as non-functional as they are today; each already carries a comment naming the
    feature that will implement it. This feature moves them, it does not wire them.
  - **Locale selection beyond this screen.** Nothing is persisted, no preference is written, no
    switcher appears on any other screen, and no API is called. Runtime locale selection as a
    product capability stays **FR-15/FR-16 (E5)** — this is a login-screen affordance, not E5's
    down payment.
  - **Every other screen's chrome.** Breadcrumbs, menu bar, footer, sub-headers and the drawer dock
    are the *shell chrome* feature (report §C-1); the form widgets on screens 08/13/16/19 are the
    *form widgets* feature (§C-3/§C-4). Splitting them is deliberate — this surface has its own
    route and shares no layout with them.
  - **Authentication behaviour.** No change to `validateLogin`, to the error codes, to
    `severityFor`/`messageFor`, or to what `login()` does.
  - **Narrow-viewport reflow.** The handoff specifies a fixed-width desktop card and the app is an
    ADF Fusion desktop skin throughout; no breakpoint behaviour is designed, so none is invented.
  - **Screens 04 / 04b** (login *rationale*) — design-only, never implemented.

## Acceptance criteria (Gherkin)

> **What a test can hold, and what it cannot.** Scenarios 1–9 are DOM assertions and run in the
> Vitest/jsdom suite. **jsdom does not load the project stylesheet**, so it can prove *structure*
> (which elements exist, nested how, in what order) but **cannot** prove *geometry* (widths,
> gradient, label placement) — those are Scenario 10, checked in a real browser against exact
> computed values, with the evidence recorded in the feature. Saying this out loud is the point: a
> green unit suite alone does **not** prove this feature's headline claim.

```gherkin
Feature: FR-19 — the sign-in screen matches design handoff screens 02 and 03

  Scenario: The card is two panes, brand first, with no panel box around it
    Given an unauthenticated visitor opens the sign-in route
    When the screen renders
    Then the sign-in form contains exactly two direct panes
    And the first pane is the brand pane and the second is the credentials pane
    And no element on the screen carries the panel-box class or a panel title bar

  Scenario: The brand pane carries the lockup, tagline, bullets and release line
    Given the sign-in screen is rendered
    Then the brand pane shows the Notebox logo and the NOTEBOX wordmark
    And it shows one tagline line
    And it shows exactly three product bullets in a list
    And it shows one release line
    And every one of those texts is resolved from the message catalog, not written literally in the component

  Scenario: The credentials pane opens with the heading and the hint
    Given the sign-in screen is rendered
    Then the credentials pane shows the "Sign in" heading
    And below it a hint line styled as a tip

  Scenario: Email and password are combo fields with a decorative trailing icon
    Given the sign-in screen is rendered
    Then the email input and the password input are each wrapped in a combo field
    And each combo field ends with an icon adornment
    And neither icon adornment is reachable by keyboard
    And neither icon adornment is exposed to assistive technology
    And each field's label is associated with its input by id, and precedes it in document order

  Scenario: The action block is a full-width submit above the two link buttons
    Given the sign-in screen is rendered
    Then the submit button is the only submit control in the form
    And the options row holds the "Remember me" checkbox and the locale control
    And the links row holds the "Forgot password?" and "Add account" buttons in that order

  Scenario: Submitting an empty form still sends no request
    Given the sign-in screen is rendered with both fields empty
    When the visitor submits the form
    Then no HTTP request is made
    And a field error is shown under the email field
    And a field error is shown under the password field
    And each field error is rendered inside its own field row, not in a shared block

  Scenario: An invalid email address is rejected before any request
    Given the sign-in screen is rendered
    When the visitor enters "not-an-address" and a password and submits
    Then no HTTP request is made
    And the email field is marked invalid
    And a field error is shown under the email field

  Scenario: An expired session shows the warning inside the credentials pane
    Given the visitor arrives at the sign-in screen after the session ended
    When the screen renders
    Then a warning message is shown above the form fields, inside the credentials pane
    And it carries the localized session-expired text, not a raw key
    And it is announced as an alert

  Scenario: Every string on the screen exists in Portuguese
    Given the locale is Portuguese
    When the sign-in screen renders
    Then every visible text on the screen differs from its English value, except for entries on the recorded identical-by-design allow-list
    And no raw message key is visible anywhere on the screen

  Scenario: The locale control switches the sign-in screen's own language
    Given the sign-in screen is rendered in English
    When the visitor selects Portuguese in the locale control
    Then every visible text on the screen is re-rendered in Portuguese
    And no HTTP request is made

  Scenario: The locale choice is not persisted
    Given the visitor selected Portuguese on the sign-in screen
    When the sign-in screen is loaded again from scratch
    Then it renders in the locale derived from the browser, not the earlier choice

  Scenario: The member's stored preference wins once they are signed in
    Given the visitor selected Portuguese on the sign-in screen
    And the member's stored locale preference is English
    When the visitor signs in successfully
    Then the application renders in English

  Scenario: The rendered geometry matches the handoff  # live browser, not jsdom
    Given the sign-in screen is loaded in a browser at a desktop viewport
    When the computed styles are read from the live document
    Then the card's computed width is 700px
    And the credentials pane's computed width is 330px and its computed flex-grow is 0
    And the brand pane's computed flex-grow is 1
    And the brand pane's computed background is the linear gradient from the handoff
    And within a field row, the label's bottom edge is above the input's top edge
    And the submit button's computed width equals the credentials pane's content width
```

## Recorded deviations from the handoff

Per NFR-09 these are conformance-exempt **because they are recorded here**, not because they are
minor. Each names why the handoff cannot be followed literally.

| # | Handoff shows | Delivered | Why |
|---|---|---|---|
| D-1 | Brand footer `Release 1.0 · acme-ops` | Release identifier only; **no tenant slug** | `acme-ops` is a tenant. The visitor is **unauthenticated** on this screen — there is no session and therefore no tenant to name. Rendering a hardcoded one would be a lie on the login page of a multi-tenant product. |
| D-2 | Hardcoded English strings in the pane | Catalog keys resolved in en + pt | The handoff is a static mock and does not model i18n; C-09 governs. |

## Compliance pre-flight

- [x] **C-01 · Tenant isolation** — **applies (standing, client obligation).** This feature adds no
  data access at all; the only call on the screen is the existing `login()`. *Evidence:* no new
  fetch path is introduced (the diff adds no request).
- [x] **C-02 · Authenticated by default** — **applies (inverted).** This is the app's one
  deliberately public surface and stays so; the feature must not make it render anything that
  presumes a session. *Evidence:* deviation **D-1** — the tenant slug is not rendered.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** No admin or config surface.
- [x] **C-04 · Personal data minimization** — **applies.** The brand pane is marketing copy and a
  release string; it must carry no user or tenant identity. *Evidence:* **D-1** + the brand-pane
  scenario, which enumerates the pane's texts exhaustively.
- [ ] **C-05 · Secrets never committed** — **n/a.** No credential or config value is introduced. The
  release identifier is public build metadata, not a secret.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Same origin and TLS posture; no new
  transport surface.
- [ ] **C-07 · Image upload safety** — **n/a.** Nothing is uploaded. The logo is inline SVG authored
  in the repo; the field icons are text glyphs.
- [x] **C-08 · Rich-text sanitization** — **applies as a prohibition.** The gradient pane and the
  icon adornments must be built as elements and CSS; **no `dangerouslySetInnerHTML`** is introduced
  to paste the handoff's markup. *Evidence:* the diff introduces none.
- [x] **C-09 · Localization completeness** — **applies, and this feature strengthens it.** Tagline,
  three bullets, release line, heading and hint are all new user-facing strings and ship in en
  **and** pt. Beyond that, OQ-28 (b) makes pt *reachable* on the one screen where no stored
  preference exists to reach it with. *Evidence:* the Portuguese scenario, the locale-control
  scenario, and the satellite's catalog coverage check.
- [ ] **C-10 · Audit trail** — **n/a.** No irreversible or administrative action.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account or tenant management. (*Add
  account* remains a non-functional affordance — see *Out*.)
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** No annotation fields.

## Open Questions

**None open.** This spec opened one and it is answered.

- **OQ-28** — *The locale control on the sign-in screen.* **Answered 2026-08-26 by rafaelsantos:
  option (b)** — it works for the sign-in screen only. Rejected: **(a)** shipping it inert, because
  a `<select>` renders the changed state while nothing else changes, which is a visible falsehood
  rather than the harmless dead link that *Forgot password?* already is; **(c)** deferring to E5,
  because design conformance is precisely what this feature exists to deliver and the deferral would
  be re-reported as a gap. Folded into *Scope* and into three scenarios above.

**Not an open question:** the tenant slug in the brand footer. That is settled by **D-1** — it is
impossible, not ambiguous.
