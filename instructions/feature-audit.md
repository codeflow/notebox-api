---
step: feat-*.audit
model: opus
effort: xhigh
reads: [features/NNN-slug/**, constitution/**, catalogs/**, the diff]
writes: [features/NNN-slug/audit.md]
gate: audit_pass
---

# Audit — adversarial coherence check

**Goal.** Try to prove the feature is *not* done. Run at `opus/xhigh` because an auditor that
reasons less carefully than the implementer finds nothing.

This is not a code review for style. It checks that intent, artifacts and code still say the
same thing.

## Run every check, report each explicitly

1. **Traceability.** Every FR in the spec → a scenario → a test → code. Follow the chain in the
   real files. A broken link is a finding, even if everything passes.
2. **Scenario honesty.** Read each test and ask whether it would actually fail if the behaviour
   regressed. Tests asserting on mocks, or asserting the code does what it does, pass CI and
   protect nothing.
3. **Constitution.** Each BR the spec bound: still held? Each architecture boundary: still
   uncrossed? Grep it, do not assume it.
4. **Compliance.** Every item marked `applies` in the spec's pre-flight has its evidence where it
   was said to be.
5. **Scope.** Diff against the plan's blast radius. Files changed that the plan never mentioned
   are either scope creep or a plan that was wrong — say which.
6. **Open Questions.** Any OQ closed during implementation: who answered it, and is it recorded?
   An OQ closed by the implementer's own assumption is a finding.

## Reporting findings

Each finding gets a concrete failure scenario — the inputs and the resulting wrong behaviour.
Rank by severity. A finding you cannot make concrete is a suspicion; label it as such rather than
inflating it.

If nothing survives verification, say so plainly. A clean audit reported as clean is a real
result; manufacturing findings to look thorough destroys the signal.

## Verdict

- **pass** — the gate opens; update `catalogs/epics.md` to `delivered`.
- **pass with findings** — non-blocking issues logged as backlog items or OQs.
- **fail** — name the substep to reopen (`spec`, `plan`, `tasks` or `implement`) and why. Send it
  back with `./bin/wf reopen <substep-id> --cascade` so everything built on it is reopened too, not
  left stale; do not patch around the failure from inside the audit.

## GitHub (if enabled)

On `pass`: comment the verdict on the issue (`./bin/wf github comment <feature-id>.audit
--event audit`). Nothing merges and nothing closes here — publishing, the PR and the human's
approval are the `publish` and `review` steps; the issue itself only closes at promotion
(`/wf-promote`), when the work reaches the trunk. See `instructions/github-sync.md`.
