---
name: web-design-adf-fusion
description: notebox-web UI must be built with the adf-fusion-style skill (Oracle ADF Faces "Fusion" look)
metadata:
  type: project
---

All `notebox-web` (react/next satellite) screens, layouts, and design must **explicitly use the
`adf-fusion-style` skill** — the user wants the web UI to have the same appearance and design as the
Oracle ADF Faces 11g "Fusion" skin (dense corporate enterprise look, blue-gray gradients, Tahoma 11px).

**Why:** Explicit human decision (2026-07-22) when scoping the first paired feature (US-6.1). It applies
to every web feature, not just the first one.

**How to apply:** In any `notebox-web` feature's spec/plan/implement steps, load the `adf-fusion-style`
skill and build the layout against that theme (reuse `adf-fusion.css` once it exists). This is a design
constraint on the satellite only; the `notebox-api` hub is unaffected. Relates to [[language-policy]].
