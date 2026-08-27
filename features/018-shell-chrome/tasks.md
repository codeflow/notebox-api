# Tasks — Workspace shell chrome

**ID:** features/018-shell-chrome · **US:** US-7.1 · **Date:** 2026-08-27
**Status:** Approved (standing authorisation 2026-08-27)

> Five tasks. T-01 first because the route table is the piece every other screen reads and the one
> that could send the plan back. The existing per-screen suites are the regression net for a layout
> change and must pass **unmodified**.

- [x] **T-01 · Breadcrumbs from a static route table**
      - files: `components/Breadcrumbs.tsx` (new), `components/Breadcrumbs.test.tsx` (new)
      - covers: scenarios "Every protected screen shows a breadcrumb trail", "The trail follows the route's depth", "A breadcrumb label never comes from the URL"
      - depends: — · parallel: no
      - verify: `npx vitest run components/Breadcrumbs.test.tsx`. The decisive test is the hostile segment: a crafted id must never appear as a crumb label.

- [x] **T-02 · Menu bar**
      - files: `components/MenuBar.tsx` (new) + test
      - covers: "The menu bar carries the handoff's menus"
      - depends: — · parallel: yes
      - verify: menus open, items route, unimplemented items are `disabled`.

- [x] **T-03 · Footer, branding bar, Administration tab**
      - files: `components/AppFooter.tsx` (new) + test, `components/BrandingBar.tsx` + test, `components/AppNav.tsx` + test
      - covers: "The shell ends in an application footer", "The branding bar carries the brand mark and the release", "Administration is reachable from the tab strip"
      - depends: — · parallel: yes
      - verify: existing BrandingBar/AppNav tests pass unmodified.

- [x] **T-04 · Mount the chrome in the layout**
      - files: `app/(app)/layout.tsx`, `app/(app)/layout.test.tsx`, `src/styles/adf-fusion.overrides.css`, i18n catalogs
      - covers: all of the above, app-wide
      - depends: T-01, T-02, T-03 · parallel: no
      - verify: `npm run verify` — **every existing screen suite green unmodified** (R1).

- [x] **T-05 · Toolbar separators, sub-headers, and the live pass**
      - files: the shipped page toolbars; evidence recorded here
      - covers: `af-tbSep`, `af-subHeader`, and the browser check the unit suite cannot do (R3)
      - depends: T-04 · parallel: no
      - verify: `npm run verify` + a live browser pass over the protected screens.


## Close-out — 2026-08-27

`npm run verify` green: **69 files, 508 tests** (496 → 508), build compiled. All five tasks done in
one branch (`feature/shell-chrome`, stacked on `feature/login-layout` because it reuses feat-017's
`appRelease()`).

**Live pass** — signed in against a seeded dev tenant, walked `/`, `/annotation-types`, `/groups`:
breadcrumbs, the five-menu bar, the four-tab strip with correct selection, the brand mark, the
`v0.1.0` release pill and the footer all render on every protected screen, in Portuguese.

**One existing test was edited, and it was a query, not an assertion.** `layout.test.tsx` used
`getByText('Workspace')`, which became ambiguous because the shell now legitimately says *Workspace*
in three places — the menu, the Navigator's accordion section and the breadcrumb root — exactly as
design 05 draws it. The query was narrowed to the accordion header, which is **stricter** than the
bare text match it replaced. No assertion was weakened and no other existing test changed.

**Two gaps the live pass found, both recorded rather than patched:**
- **OQ-30** — the branding bar renders the tenant **UUID** where design 05 shows the slug
  (`acme-ops`). `/me` carries no tenant name, so the honest fix is an API change, not a web one.
- **OQ-31** — design 05's Overview summary boxes need aggregate endpoints that do not exist.
