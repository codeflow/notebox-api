# Plan — Workspace shell chrome

**Feature:** features/018-shell-chrome · **Status:** Approved (standing authorisation 2026-08-27)
**Date:** 2026-08-27 · **Project:** `notebox-web`

## Approach

**Everything lands in the shared layout, which is the entire point.** `app/(app)/layout.tsx` already
owns the branding bar, tab strip, Navigator and splitter; adding the trail, menu bar and footer there
moves all 13 screens in one edit instead of thirteen.

### A1 · Breadcrumbs from the route, never from the URL text *(the one decision worth stating)*
A `Breadcrumbs` client component reads `usePathname()` and maps segments through a **static route
table** to catalog keys. Dynamic segments (`[id]`, `[recordId]`) are **not** rendered as labels —
they resolve to the section's generic label ("Annotation type", "Record", "Task"). That is deliberate
and is what the hostile-segment scenario pins: a crumb label can only ever be a string this repo
wrote, so a crafted URL cannot put text on the page. It also keeps C-08 true by construction rather
than by escaping.

Rendered inside `af-panelMain`, above whatever `af-panelHeader` the page supplies.

### A2 · Menu bar *(reversible)*
`MenuBar` renders the handoff's five menus. Items route where a route exists and are **disabled**
otherwise, so the chrome never advertises what the app cannot do. Keyboard: each menu is a button,
its popup a list; `Escape` closes.

### A3 · Footer, branding upgrade, Administration tab *(reversible)*
`AppFooter` closes the shell. `BrandingBar` gains the real `NoteboxMark`, the `release` pill fed by
`appRelease()` (already shipped in feat-017 — reused, not re-derived), and Preferences/Help links
that are disabled placeholders. `AppNav` gains the fourth tab, routing to `/groups`.

### A4 · Toolbar separators and sub-headers *(reversible)*
`af-tbSep` goes into the shipped toolbars where the handoff groups their buttons; `af-subHeader`
labels the grouped regions the design names ("Fields", "Assign to a group", "Details").

## Reversibility
No one-way decisions: no schema, no migration, no public contract. The closest is the **route table**
in A1 — every screen's trail reads from it, so its shape is what a future screen must extend.

## Alternatives rejected
**Per-page breadcrumbs**, each page declaring its own trail. Rejected: it is thirteen edits instead of
one, and it puts the label decision next to the page that knows the record's *name* — which is
exactly how a URL segment or a fetched title ends up rendered as a crumb. Centralising it makes the
"labels come only from our own catalog" rule enforceable in one place.

## Blast radius
| File | Change |
|---|---|
| `app/(app)/layout.tsx` | mount `MenuBar`, `Breadcrumbs`, `AppFooter` |
| `components/Breadcrumbs.tsx` + test | **new** — route table + trail |
| `components/MenuBar.tsx` + test | **new** |
| `components/AppFooter.tsx` + test | **new** |
| `components/BrandingBar.tsx` + test | mark, release pill, links |
| `components/AppNav.tsx` + test | Administration tab |
| `lib/i18n/messages/en.ts`, `pt.ts` | new chrome strings |
| `src/styles/adf-fusion.overrides.css` | chrome rules |
| page toolbars (annotation types, records, tasks) | `af-tbSep`, `af-subHeader` |

## Risks
| # | Risk | Signal |
|---|---|---|
| R1 | The layout change reaches every protected screen | full satellite suite; the existing per-screen tests must pass unmodified |
| R2 | A crumb label leaks a URL segment | the hostile-segment test |
| R3 | jsdom cannot see the chrome's geometry | live browser pass at the end, across screens |
| R4 | A new chrome string ships untranslated | the screen-level pt sweep, extended to the shell |
