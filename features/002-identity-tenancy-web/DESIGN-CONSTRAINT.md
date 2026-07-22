# Design constraint — notebox-web (ADF Fusion theme)

> Read this before writing the spec, plan, or implementation for this feature.

**All UI for this feature (and every notebox-web feature) MUST be built with the `adf-fusion-style`
skill.** The user requires the web layout and design to match the Oracle ADF Faces 11g "Fusion" skin
(dense corporate enterprise look, blue-gray gradients, Tahoma 11px).

- Load the `adf-fusion-style` skill at the spec/plan/implement steps and design against that theme.
- Reuse `adf-fusion.css` once it exists; keep new screens consistent with it.
- Source: explicit human decision 2026-07-22 (see memory `web-design-adf-fusion`).

This constrains presentation only; the auth/tenant **contract** is defined by the API feature
`feat-001-identity-tenancy` (its `contracts/`), which this feature consumes.
