# Audit — Annotation record editor (feat-006, US-2.1 web half) — ROUND 2

**Step:** `feat-006-annotation-records-notebox-web.audit` (re-run) · **Date:** 2026-08-11
**Method.** Inline opus/xhigh disposition audit of the R1 remediation (T-11…T-13, commits
`1ea31aa`/`bd1cbac`/`d9ec393`): each finding located in the tree with its guarding test, plus an
adversarial sweep for defects introduced by the remediation itself — including a pattern-level probe of
the new routing regexes against real, bare and hostile property paths. Satellite `npm run verify`
green: lint + typecheck + **277 tests (38 files), no unhandled errors** + production build.

## Verdict: **PASS**

All three R1 findings are closed and guarded. Nothing new survived verification.

### R1 disposition
| Finding | Fix (located) | Guard |
|---|---|---|
| F-01 violation-path mismatch | `routeRecordProblem` matches the path tail — `/(?:^|\.)name$/`, unanchored `/values\[(\d+)\]/` | fixtures use the REAL prefixed paths (`create.input.name`, `create.input.values[1].number`) + one bare-path regression case |
| F-02 empty-revert untested | — (test-only) | stateful wrapper: type-then-clear emits `{untouched, hasStoredValue:true}`; `toInput` on that state asserted to send the `{text:null}` echo, never `{text:''}` |
| F-03 embedded image rules untested | — (test-only) | editor reject path (message in the error slot, nothing inserted), accept path (upload called, `data-image-id` node, no `src`), ValueField oversize → zero upload calls |

### Remediation-introduced surface, swept
- **jsdom Range layout shims** (`test/setup.ts`) — the exact mitigation plan §Risks reserved for this
  case; `??=` guarded so real implementations are never overridden; geometry is irrelevant to every
  assertion in the suite. Test infrastructure, not behaviour: clean.
- **Routing-pattern probe:** `somename`/`…rename` correctly do not match the name tail; `values[i]`
  extraction is index-correct on prefixed and bare shapes. *Observation, non-blocking:* a hypothetical
  `…values[0].name` path would route to the record-name control because the name check runs first —
  unreachable today (`AnnotationValueInput` has no `name` property), worth reordering the checks if the
  wire contract ever gains one. Recorded here so a future change is deliberate.

### Check-by-check
1. **Traceability — PASS.** 29/29 scenarios now map to tests that exercise them, including the two
   on-the-control error scenarios against the real mapper's path shape and the embedded-image rules.
2. **Scenario honesty — PASS.** The F-02 test now exercises the branch its name claims, through real
   state feedback; the F-03 tests assert observable outcomes (no upload call, serialized node shape).
3. **Constitution — PASS.** Unchanged from R1 (the delta is one function + tests + a test shim).
4. **Compliance — PASS (client obligations).** Unchanged. The **API-side C-08 sanitization gap**
   remains a recorded dependency (human-deferred, OQ-19 decision record) — it does not block this
   feature's gate and **must be resolved before US-2.1 promotes to `main`**.
5. **Scope — PASS.** The R1 delta touches exactly the files the T-11…T-13 rows name, plus the
   anticipated setup shim.
6. **Open Questions — PASS.** None pending.

### Gate
`audit_pass` **opens**. Next: `publish` (push the 13 commits, CI green on the branch), then `review`
(PR → develop, human approval on GitHub). US-2.1 stays `building` until both sides are merged and the
C-08 server obligation is scoped.

---

# Audit — Annotation record editor (feat-006, US-2.1 web half) — ROUND 1

**Step:** `feat-006-annotation-records-notebox-web.audit` · **Date:** 2026-08-11
**Method.** Inline opus/xhigh adversarial audit of the T-01…T-10 implementation (10 commits,
`e65af80`→`262ba85`, on the satellite's `feature/annotation-records`): traceability over all 29 spec-v2
scenarios followed into the real test files, scenario-honesty reads of every test, constitution greps,
compliance evidence, scope-vs-plan diff, OQ check. One cross-repo contract probe: the client's
violation-routing was checked against the **hub API's actual mapper source**, not against the MSW
fixtures the client's own tests use. Satellite `npm run verify` green: lint + typecheck + **272 tests
(38 files)** + production build.

## Verdict: **FAIL — narrow (1 HIGH, 1 MEDIUM, 1 LOW)**

The implementation is substantially honest — the dialect contract, the secret state machine and the
reveal gate are real and well-guarded. The gate fails on one contract mismatch that MSW hid and two
test gaps.

### Findings

- **F-01 — HIGH · Violation routing matches only the mock's invented paths, never the real API's.**
  `routeRecordProblem` (`RecordForm.tsx`) matches `violation.field === 'name'` and the **anchored**
  `/^values\[(\d+)\]/`. But feat-005's `ConstraintViolationMapper.toViolation` emits the raw
  `violation.getPropertyPath().toString()`, and for parameter validation on
  `create(@Valid AnnotationRecordInput input)` that path is **method-and-parameter prefixed**
  (`create.input.name`, `create.input.values[0].text` — Jakarta property-path semantics), never the
  bare `name` / `values[0].text` the client expects. Nothing on either side pins the shape: the
  client's tests assert against MSW fixtures that invent bare paths, and feat-005's wire tests assert
  only `violations.code`, never `field`. **Concrete failure:** POST with an empty name against the real
  API → 400 with `field: "create.input.name"` → the client routes it to **form level**; the spec
  scenarios *"A record name is required"* (message on the name control) and *"A rejected value is shown
  against its own control, not as a page-level error"* are false in production while green in CI —
  exactly the plausible-but-wrong class this audit exists to catch. **Fix (client-side, non-breaking,
  works with both shapes):** suffix-tolerant matching (`/(?:^|\.)name$/` on the path's tail;
  `/values\[(\d+)\]/` unanchored) + test fixtures using the **real** prefixed paths.
- **F-02 — MEDIUM · The secret empty-revert test does not test its claim.**
  `SecretValueField.test.tsx` *"emptying a replaced input reverts to untouched, preserving the stored
  value"* types `'x'` and asserts `replaced` — the **revert branch is never exercised** (the
  controlled-component harness never feeds the `replaced` state back). If the `'' →
  {untouched, hasStoredValue: initial}` logic regressed to emitting `{replaced, text: ''}`, no test
  fails, and a save would **re-encrypt an empty string over the stored secret** instead of preserving
  it — silent secret destruction, the precise class INV-R4 exists to prevent. Fix: a stateful wrapper
  in the test that loops `onChange` back into `state`, then type-and-clear and assert the untouched
  echo (and the `toInput` consequence).
- **F-03 — LOW · The embedded rich-text image rules are untested.** The scenario *"An image embedded
  in rich text follows the image rules"* traces to `uploadRichImage` (`ValueField.tsx`) and the
  editor's insert/reject path — but `uploadImage` appears in tests only as an inert `vi.fn()` prop.
  Oversize/unsupported rejection (no upload sent, localized message shown in the editor's error slot)
  and the accepted-file insert (`data-image-id` node) have zero coverage. The sibling
  `ImageValueField` path is covered; this one is not.

### Check-by-check
1. **Traceability — FAIL (two scenarios):** 27/29 scenarios map to tests that exercise them. The two
   on-the-control error scenarios are green only against MSW's invented paths (F-01); the embedded
   image scenario is partially covered (F-03). The route-guard scenario is covered by the satellite's
   global `RouteGuard.test.tsx` per the established feat-004 idiom — noted, accepted.
2. **Scenario honesty — FAIL (one test):** F-02's test asserts something other than its name claims.
   Everything else read honestly: the dialect identity is pinned in both directions (editor emits ⊆
   sanitizer accepts, byte-compared), reveal/re-mask asserts behaviour (unmount) not implementation,
   the delete dialog asserts nothing-sent-before-confirm, `toInput` is pinned at the exact-wire-body
   level, and the hostile-markup fixture verifies `window.pwned` stays unset — a real execution check.
3. **Constitution — PASS.** BR-03/04 (controls derived from the closed set, options only the type's);
   BR-05 (explicit irreversible confirm); BR-08/C-09 (keyset coverage green over all 46 new keys,
   en+pt); BR-10 (stored cleartext unrepresentable client-side — grep confirms no other component
   touches `RevealResponse.value`). AD boundaries: all API access through `lib/api`; no new fetch
   outside it; `adf-fusion.css` untouched, additions in `overrides` only.
4. **Compliance — PASS (client obligations).** C-01/02/03/04/05/06/07 and the C-08/C-12 client shares
   all carry the evidence the spec named. **Recorded dependency, not a finding of this feature:** the
   **API-side C-08 input/output sanitization gap** (spec v2 §Dependency) stands — hostile markup
   POSTed straight at the merged API is stored and returned verbatim; this client's render-side
   sanitization cannot close it. It was deferred by the human for scoping (OQ-19 decision record) and
   is restated here because it must be resolved **before US-2.1 is promoted** to `main`.
5. **Scope — PASS.** The 35 changed files reconcile with plan §Blast radius; the two additions beyond
   it (`.prettierignore` for the design references, the badge swatch on LIST selects) are declared in
   commit messages and are within the plan's intent.
6. **Open Questions — PASS.** None pending. OQ-19 was decided by the human, propagated to PRD v2, and
   the spec is at v2 accordingly; no OQ was closed by implementer assumption.

### What must be reopened
`./bin/wf reopen feat-006-annotation-records-notebox-web.tasks --cascade` — the fixes need task rows
that do not exist. Bounded worklist, everything else above is fenced — do not redo it:
- **T-11** (behavioural, F-01): suffix-tolerant violation-path routing in `routeRecordProblem`
  (`/(?:^|\.)name$/` tail match; unanchored `/values\[(\d+)\]/`), with test fixtures using the real
  prefixed paths (`create.input.name`, `create.input.values[1].number`) alongside the bare ones.
- **T-12** (test-only, F-02): stateful-wrapper test proving type-then-clear emits
  `{untouched, hasStoredValue: true}` and that `toInput` then sends the `{text: null}` echo.
- **T-13** (test-only, F-03): embedded-image tests — oversize/unsupported file → localized message in
  the editor's error slot and **no** upload call; accepted file → upload + `data-image-id` node
  inserted.

### Out of scope (restated, pre-existing)
The API-side C-08 sanitization obligation (feat-005, merged) — awaiting the human's scoping decision;
blocks promotion, not this feature's remediation. The records grid/listing — US-2.2 by explicit spec
scope.
