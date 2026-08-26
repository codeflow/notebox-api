# Design conformance — screen-by-screen gap report

**Date:** 2026-08-26 · **Method:** class-inventory diff of every `<section data-screen-label>` in
`notebox-web/design/handoff/design/Notebox Screen Flows.dc.html` against every `className` used in
`components/**` and `app/**`, plus every selector defined in `adf-fusion.overrides.css`.
**Purpose:** scope the design-conformance features against real deltas rather than sampling.

> **What this method catches and what it misses.** A missing class means a **widget that exists in
> no screen of the app**. It does **not** catch a screen whose widgets all exist but whose *layout*
> differs — the login is exactly that case, and it is called out separately below. Both signals were
> used.

## Per screen

| # | Screen | Design classes | Missing | Delta |
|---|---|---|---|---|
| 01 | Flow map | 4 | 1 | design-only, not implemented |
| **02** | **Login (layout A)** | 9 | 2 | **layout differs — see L-1**; `af-comboField`, `af-iconButton` |
| **03** | **Login validation** | 14 | 2 | same layout gap; validation itself present |
| 04 / 04b | Login rationale | 18 / 15 | 2 / 3 | design-only, not implemented |
| **05** | **Workspace home** | 37 | **11** | **largest gap** — menu bar, breadcrumbs, drawer dock, footer, sub-headers |
| 06 | Types list (as-is) | 11 | 1 | baseline; breadcrumbs only |
| 07 | Types list (proposed) | 7 | 1 | close; `af-tbSep` only |
| **08** | **Type builder** | 34 | 5 | `af-train`/`af-trainStop` header, `af-spinButtons`, `af-subHeader`, breadcrumbs |
| 09 | Type detail | 18 | 1 | breadcrumbs only |
| 10 | Delete type dialog | 12 | 1 | breadcrumbs only |
| 11 | Annotations grid | 25 | 2 | breadcrumbs, `af-tbSep` |
| 12 | Annotation detail | 12 | 1 | breadcrumbs only |
| **13** | **Annotation create/edit** | 21 | **7** | `af-choiceGroup`, `af-comboField`, `af-noteWindow`, `af-spinButtons`, `af-iconButton` |
| **14** | **Groups** | 20 | 5 | `af-shuttle*` (deferred by decision), `af-subHeader`, breadcrumbs |
| 15 | Tasks list | 26 | 2 | breadcrumbs, `af-tbSep` |
| **16** | **Task detail** | 32 | **10** | drawer/dock/tabs, `af-note`, `af-comboField`, breadcrumbs |
| 17 | Translation catalog | 16 | 3 | **not built — E5/FR-16** |
| 18 | Portuguese locale | 15 | 2 | breadcrumbs, `af-tbSep` |
| 19 | Error & degraded states | 21 | 3 | `af-messages`, `af-noteWindow`, `af-spinButtons` |

## The deltas, clustered

### C-1 · App-wide chrome — the single biggest win
| Widget | Screens |
|---|---|
| `af-breadCrumbs` | **13** |
| `af-tbSep` | 8 |
| `af-subHeader` | 4 |
| `af-footer` | 2 |
| `af-menuBar` / `af-menu` / `af-menuItem` / `af-menuPopup` / `af-menuSep` | 05 |

Breadcrumbs alone appear on 13 of 20 screens and exist **nowhere** in the app. With the menu bar and
footer these are one coherent piece of work in the layout, and fixing them moves every screen at once.

### C-2 · Login layout (L-1)
The class diff is nearly clean — the widgets exist. The **layout** does not match: the app renders a
centred single-pane `af-panelBox`, while screen 02 specifies a **two-pane card, 700×~330, with a left
brand pane** (`linear-gradient(160deg,#54749b,#3a5c86 45%,#1f3f63)`) and a 330px right form. The
handoff README states layout A *"replaces the current centred `nb-loginShell` card"*. Confirmed
visually in the live run.

### C-3 · Form widgets missing across the input screens
`af-comboField` (6 screens), `af-iconButton` (6), `af-spinButtons` (3), `af-choiceGroup`,
`af-noteWindow`, `af-messages`. These carry real behaviour (number spinners, choice groups,
server-violation note windows), so this cluster is more than styling.

### C-4 · Task detail drawer (screen 16)
`af-drawer`, `af-drawerDock`, `af-drawerTab`, `af-note`, `af-tab` — the Notes drawer, closed by
default and toggled from the dock. Shares `af-drawerDock`/`af-drawerTab` with screen 05.

### C-5 · Out of scope by prior decision
- **`af-shuttle*` (screen 14)** — the bulk-assignment shuttle, deferred in feat-015's spec because
  the API exposes no bulk surface. Recorded on the design's own screen-14 row.
- **Screen 17** — the translation catalog is FR-16 / E5, genuinely unbuilt future work, not a
  conformance gap.
- **Screens 01, 04, 04b** — design-only (flow map and rationale), never to be implemented.

## What already conforms

The ADF theme itself, the `af-table` grids with QBE filter rows and `af-statusBar` pagers, the
dialogs (`af-dialogOverlay`/`af-dialog`), the task list with `af-progress`, the annotation grids, and
the Navigator built in feat-015. **This is a conformance gap, not a rewrite** — 46 components exist
and most are on-theme.

## Proposed feature split

| Feature | Covers | Why grouped |
|---|---|---|
| **feat-017** workspace shell chrome | C-1 (breadcrumbs, menu bar, toolbar separators, sub-headers, footer, drawer dock) | one layout, 13+ screens move at once |
| **feat-018** login layout A | C-2 | isolated surface, visually the most wrong, own route |
| **feat-019** form widgets & states | C-3 + C-4 (screens 08, 13, 16, 19) | shares the input/validation stack; carries behaviour |

Screen 17 stays with **E5**. The shuttle stays deferred.

## Root cause — why this accumulated silently

**No FR or NFR covers design conformance.** The PRD assigns UI rendering to `notebox-web` (AD-06) but
never states that the UI matches the handoff. Each feature legitimately built only what its own FR
named and cited the design for that slice; the remainder was never anyone's requirement.

feat-015's spec is the clearest instance: it scoped out screen 05's menu bar, toolbar, breadcrumbs,
summary row, drawer dock and footer with the reason *"no originating FR"* — correct against the
catalogs, wrong against the human's intent, and recorded as settled instead of raised as a question.
That is the process failure to fix, not just the pixels.
