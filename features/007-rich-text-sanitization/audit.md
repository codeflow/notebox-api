# Audit — Rich Free-text sanitization (feat-007, US-2.1 / C-08) — ROUND 1

**Step:** `feat-007-rich-text-sanitization.audit` · **Date:** 2026-08-12
**Method.** Inline opus/xhigh adversarial audit of the T-01…T-03 implementation (3 commits,
`1accc14`→`2b7dfa8`, on `feature/rich-text-sanitization`): traceability over the 13 spec scenarios into
the real test files, scenario-honesty reads, constitution/layering greps, compliance evidence, scope
diff vs plan blast radius, OQ check. Obfuscation sweep on the sanitizer (entity-encoded `javascript:`,
mXSS-style double-encoding) reasoned against jsoup's decode-then-validate pipeline and pinned by the
idempotence test. `mvn -B verify` green on the full suite: **BUILD SUCCESS**.

## Verdict: **PASS** (one non-blocking observation)

### Check-by-check
1. **Traceability — PASS.** 13/13 scenarios map to tests that exercise them: the six hostile scenarios
   in the unit corpus (case-for-case mirror of the client's `sanitize.test.ts`) plus a wire replay;
   byte-identity and idempotence in the corpus; update-like-create, TEXT-verbatim, secret-verbatim and
   bounds-on-the-stored-form at the service; the legacy row at the wire with a native re-read. The
   "secret values keep their shipped behaviour" wire clause rests on the existing masking/reveal wire
   tests, unchanged and green in the full verify — accepted as covered, noted.
2. **Scenario honesty — PASS.** The two strongest guards are structural: the canonical dialect document
   is asserted **byte-equal** (an approximate sanitizer cannot pass), and the legacy-row test asserts
   the stored bytes after the GET — a sanitizing read that flushed an UPDATE would fail it, which is
   exactly the failure INV-S5 names. The bounds test proves sanitize-before-measure in both directions
   (a 70KB stripped attribute passes; an over-long clean form still rejects).
3. **Constitution — PASS.** Layering: `org.jsoup` imports exist **only** under `infrastructure/`
   (grep-verified); the application and API layers see the port. AD-03 untouched (no new query). Code
   standards: Javadoc on the public surface, English identifiers, comments state constraints.
4. **Compliance — PASS.** **C-08's evidence now exists where the spec said it would**: input scenarios
   (write seam), output scenarios (read seam + legacy row), allow-list exactness (byte-identity).
   C-09: zero new message keys (grep-verified — sanitization strips, never rejects). C-12: the
   secret-verbatim test pins that encrypted values never pass through the sanitizer.
5. **Scope — PASS with one observation.** The diff matches plan §Blast radius except one file the plan
   missed: `AnnotationTypeEditGuardTest` (4 mechanical call-site updates forced by the DTO parameter —
   a consequence the plan should have listed, not scope creep). Feature docs and `workflow.json` ride
   in the commits per repo convention.
6. **Open Questions — PASS.** None pending; none closed by implementer assumption.

### Adversarial sweep, findings refuted
- **Entity-obfuscated URLs** (`&#106;avascript:`): jsoup decodes entities at parse, so the Safelist's
  protocol check sees the decoded value — blocked. Pinned indirectly by the idempotence test's
  entity-bearing fixture.
- **Serializer drift between the two dialect enforcers**: the shared-corpus discipline (this feature's
  corpus mirrors the client's case-for-case, both including the canonical byte-equal document) makes a
  one-sided dialect change fail tests on the side that changed. No finding.
- **`parseBodyFragment` context tricks** (table/select fragment parsing quirks): the dialect has no
  table/select elements; fragments normalize into body context and the Cleaner drops what the Safelist
  does not know. No probe produced a survivor.

### Observation (non-blocking)
Plan §Blast radius omitted the `AnnotationTypeEditGuardTest` caller — recorded so the next plan lists
test callers of changed signatures.

### Gate
`audit_pass` **opens**. With this feature merged, the **C-08 promotion blocker on US-2.1 is closed** —
after review, `/wf-promote` has no remaining compliance obstacle from the audits. Next: `publish`
(push + CI), then `review` (PR → develop, human approval on GitHub).
