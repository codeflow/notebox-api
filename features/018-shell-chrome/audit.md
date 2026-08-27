# Audit — Workspace shell chrome

**ID:** features/018-shell-chrome · **US:** US-7.1 · **Round:** 1 · **Date:** 2026-08-27
**Verify:** `npm run verify` → 69 files, **508 tests**, build compiled.
**Standing authorisation:** given in chat 2026-08-27 ("*não precisa me perguntar nada*").

## Verdict — **PASS WITH FINDINGS** (both findings are API gaps, logged as OQ-30 / OQ-31)

## Traceability
All 8 scenarios have a test that can fail. The load-bearing one is **"A breadcrumb label never comes
from the URL"**: `trailFor` returns `MessageKey`s, so there is no code path that can render a URL
segment — the hostile-segment test asserts both the key list and that no `script` text reaches the
DOM. C-08 holds by construction, not by escaping.

## Scenario honesty
The breadcrumb suite discriminates: an id segment resolves to its **section's** label, so a test that
merely checked "three crumbs" would pass on a broken implementation. The trail asserts the exact key
sequence instead. The menu suite pins the feature's own rule — every item under *View* is disabled,
because none of them has a destination — which is what stops a later change from wiring a menu item
to nothing and calling it done.

## Scope
Diff matches the plan's blast radius. One file outside it: `app/(app)/layout.test.tsx`, a **query**
narrowed because the shell now says "Workspace" in three places by design. Not a weakened assertion —
the narrowed form is stricter.

## Findings
### F-01 · medium — the branding bar publishes the tenant UUID (**OQ-30**)
Design 05 shows `Tenant: acme-ops`. The app renders `me.tenantId`, an opaque 36-character id, in the
chrome of **every** protected screen. Confirmed in the live pass. Not fixable here: `/me` carries no
tenant name or slug, so the fix is an API contract change. Logged rather than patched, because a
web-side workaround would mean deriving a display name the API never sent.

### F-02 · low — the Overview screen has no summary boxes (**OQ-31**)
Design 05 fills the main panel with `af-panelBox` summary cards and a *Tasks in flight* sub-header.
The numbers need aggregates the API does not expose. An API feature, not a styling gap.

## What is good
One layout edit moved **13 screens** at once, which is exactly what this cluster was scoped for, and
every pre-existing screen suite passed unmodified.
