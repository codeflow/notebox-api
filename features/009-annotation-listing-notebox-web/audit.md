# Audit — Records grid (feat-009, US-2.2 web half) — ROUND 1

**Step:** `feat-009-annotation-listing-notebox-web.audit` · **Date:** 2026-08-12
**Method.** Inline opus/xhigh adversarial audit of the T-01…T-03 implementation (5 commits,
`b99cdff`→`2454f74`, on the satellite's `feature/annotation-listing-web`): traceability over the 16
spec scenarios into the real tests, scenario-honesty reads, invariant greps (INV-G1…G5),
constitution/layering checks, compliance evidence, scope diff vs plan. **Plus a live pass**: the grid
was run in the browser against the real API + MySQL with seeded data — which caught one real defect
(unstyled badges) that jsdom could not see; the fix (`2454f74`, CSS-only) landed after the full
verify run and is noted as such. Satellite `npm run verify` green: **300 tests (41 files)** +
production build; the CSS-only delta after it is re-verified by CI at publish.

## Verdict: **PASS** (two non-blocking observations)

### Check-by-check
1. **Traceability — PASS.** 16/16 scenarios map to tests that exercise them. One is covered
   **compositionally and noted**: "a row opens the detail, which shows every field" asserts the
   navigation at route level, with the detail's all-fields behaviour carried by feat-006's unchanged
   suite and the projection-vs-detail pair pinned at the API by feat-008's wire test — accepted, on
   the record.
2. **Scenario honesty — PASS.** The QBE zero-request claim is asserted by counting MSW hits before
   and after typing, not assumed; the no-reveal claim queries for the affordance by accessible name;
   INV-G4 is asserted structurally (no element from the value reaches the cell container); the pager
   is tested at the boundaries the plan named (total 0 → empty state, exact multiple, last-page
   disable). The **live pass** adds evidence tests cannot: newest-first order, masking, previews and
   the QBE note verified against the real stack — and it exposed the badge-CSS gap (fourth
   live-only defect this project; the pattern is now recorded in the audits of three features).
3. **Constitution — PASS.** All API access through `lib/api` (no raw fetch in grid files);
   `adf-fusion.css` untouched (diff-verified), additions in overrides only; INV-G5 grep-verified —
   `RevealableValue` is imported only by its own file and the detail route; English identifiers,
   Javadoc-style docs on the public surface.
4. **Compliance — PASS (client obligations).** C-01 (bearer/no-tenant client test), C-02 (global
   guard idiom), C-08 render share (INV-G4: text nodes only — nothing to sanitize in a cell by
   construction), C-09 (keyset coverage green over the 9 grid keys en+pt), C-12 (masked cells, no
   cleartext fetch path in the grid).
5. **Scope — PASS.** Diff matches plan §Blast radius; the one addition beyond it is the badge CSS
   fix — live-caught, declared in its commit, within the plan's screen-11 intent.
6. **Open Questions — PASS.** None pending; the QBE page-local decision was made in the approved
   spec, not by the implementer.

### Observations (non-blocking)
- **O-01** — the QBE predicate uses a non-null assertion (`visibleFields.find(...)!`): a filter key
  for a field that stopped being visible would throw. Unreachable in the current wiring (filter keys
  are only created from rendered visible-field inputs, and the route remounts on type change) —
  worth a defensive `?? ''` if the grid ever gains persistent filter state.
- **O-02** — QBE on rich columns matches the **preview text** (what the cell displays), not the full
  value — the honest reading of "filter what you see", stated in a code comment; recorded so a
  future full-text expectation is a deliberate change.

### Gate
`audit_pass` **opens**. Next: `publish` (push the 5 commits, CI green — which also re-verifies the
CSS-only tail commit), then `review` (PR → develop). On merge, **US-2.2 is delivered on develop**,
completing the annotations epic (E2) pending promotion.
