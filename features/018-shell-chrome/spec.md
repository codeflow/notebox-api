# Feature — Workspace shell chrome (design screens 05, 06–16, 18, 19)

**ID:** features/018-shell-chrome
**User Story:** US-7.1 · **Version:** v1
**Status:** Approved — standing authorisation given in chat 2026-08-27 ("*implemente todas as telas,
não precisa me perguntar nada*"), covering this feature's spec, plan and tasks gates.
**Date:** 2026-08-27 · **Project:** `notebox-web` (react/next) — routed satellite

## Origin
- **US-7.1 / FR-19 / NFR-09.** `features/_design-conformance-report.md` **§C-1**, the largest single
  cluster: `af-breadCrumbs` appears on **13 of 20** design screens and exists nowhere in the app.
- **Design:** handoff screen **05** (workspace home — full chrome) is the reference; the chrome it
  defines repeats on 06–16, 18 and 19.
- **Consumes:** the shipped `af-shell` / `af-panelLeft` / `af-splitter` / `af-panelMain` layout,
  `AppNav`, `BrandingBar`, `Navigator`, the i18n catalogs, `adf-fusion.overrides.css`.

## Summary
The protected shell gains the chrome the handoff draws around every screen: a **breadcrumb trail**, a
**menu bar**, an **application footer**, an upgraded **branding bar** (brand mark, release pill,
Preferences/Help links), the missing **Administration** nav tab, **toolbar separators**, and
**sub-headers** on the screens that group their content. Because all of it lives in the shared
layout, **13+ screens move at once** — that is the whole reason this cluster is one feature.

## Scope
- **In:** breadcrumbs derived from the route (never from user input); menu bar with the handoff's five
  menus; footer; branding-bar brand mark + release pill + Preferences/Help; the Administration tab;
  `af-tbSep` in the shipped toolbars; `af-subHeader` where the design groups content; every new string
  in en + pt.
- **Out:** the task-detail **notes drawer** and the form widgets (`af-comboField` beyond login,
  `af-spinButtons`, `af-choiceGroup`, `af-noteWindow`, `af-messages`) — those are **feat-019**. The
  `af-shuttle` bulk assignment stays deferred (no API). Screen 17 stays with **E5**. Menu items whose
  target does not exist yet render **disabled**, never as dead links.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-19 — the protected shell matches the handoff's chrome

  Scenario: Every protected screen shows a breadcrumb trail
    Given a member is signed in
    When any protected screen renders
    Then a breadcrumb trail is shown above the page header
    And its first crumb is the workspace root
    And its last crumb is the current screen and is marked as current

  Scenario: The trail follows the route's depth
    Given the member opens an annotation type's records
    When the screen renders
    Then the trail has one crumb per route segment, in route order
    And every crumb but the last is a link

  Scenario: A breadcrumb label never comes from the URL
    Given the member opens a route whose id segment is "<script>x</script>"
    When the screen renders
    Then no crumb renders that segment as its own label

  Scenario: The menu bar carries the handoff's menus
    Given a member is signed in
    When the shell renders
    Then a menu bar shows Workspace, Edit, View, Administration and Help
    And opening a menu reveals its items
    And an item whose destination does not exist yet is disabled

  Scenario: The shell ends in an application footer
    Given a member is signed in
    When the shell renders
    Then a footer is the last element of the shell
    And it names the product and the release

  Scenario: The branding bar carries the brand mark and the release
    Given a member is signed in
    When the shell renders
    Then the branding bar shows the Notebox mark and wordmark
    And a release pill carrying the build's version
    And the member's name and tenant, sourced from the session and never from the URL

  Scenario: Administration is reachable from the tab strip
    Given a member is signed in
    When the shell renders
    Then the tab strip offers Overview, Annotations, Tasks and Administration
    And choosing Administration opens the groups screen

  Scenario: Every string added by this feature exists in Portuguese
    Given the locale is Portuguese
    When the shell renders
    Then every visible text in the chrome resolves to a Portuguese catalog value
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies.** The branding bar's tenant text comes from the session
  (`useAuth().me`), never the URL. *Evidence:* the branding scenario + no new fetch path.
- [x] **C-02 · Authenticated by default** — **applies.** All of this chrome lives in the guarded
  `(app)` layout.
- [ ] **C-03 · Least-privilege** — **n/a.** The Administration tab is navigation placement; the API
  imposes no elevated role and the UI invents none.
- [x] **C-04 · Data minimization** — **applies.** The bar shows display name and tenant only.
- [ ] **C-05 · Secrets** — **n/a.**
- [x] **C-06 · Encryption in transit** — **applies (standing).**
- [ ] **C-07 · Image upload safety** — **n/a.** The brand mark is inline SVG in the repo.
- [x] **C-08 · Rich-text sanitization** — **applies as a prohibition.** Breadcrumb labels are plain
  text rendered by React's escaping; **no `dangerouslySetInnerHTML`**, and no crumb label is ever
  taken from a URL segment. *Evidence:* the hostile-segment scenario.
- [x] **C-09 · Localization completeness** — **applies.** Every new chrome string in en + pt.
- [ ] **C-10 / C-11 / C-12** — **n/a.** No irreversible action, no account management, no fields.

## Open Questions
None. The one judgement call — menu items with no destination — is settled in *Scope/Out*: they
render disabled rather than as dead links, so the chrome never promises what the app cannot do.
