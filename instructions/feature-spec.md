---
step: feat-*.spec
model: opus
effort: high
reads: [catalogs/*.md, prd/PRD_v*.md, constitution/02-compliance.md, features/_template/spec.template.md]
writes: [features/NNN-slug/spec.md]
gate: human_approval
---

# Spec — what and why

**Goal.** State what the feature must do, in terms a test can check, without deciding how.

## Procedure

1. Start from `features/_template/spec.template.md`.
2. **Origin** — the US, the FRs covered, the BRs bound, and a citation into the PRD.
3. **Behaviour in Gherkin.** The `Feature:` describes the FR, not the user story.
   Every `Scenario:` is concrete:

   ```gherkin
   Scenario: Refund exceeds the original charge
     Given a checkout of 40.00 EUR that has been fully refunded
     When an agent requests a further refund of 5.00 EUR
     Then the request is rejected with error REFUND_EXCEEDS_CHARGE
     And the ledger balance is unchanged
   ```

   Mandatory coverage: the happy path, **at least one error path**, and every boundary named in
   the FRs. These scenarios become the executable tests in `implement` — write them as the
   contract they will literally become.
4. **Compliance pre-flight.** Copy the checklist from `constitution/02-compliance.md`, mark each
   item `applies / not applicable + why`. An unmarked item fails the audit.
5. **Out of scope.** What this feature deliberately does not do. This is what stops scope drift
   during implementation.
6. **Open Questions.** Anything the PRD and constitution do not settle becomes `[TBD — OQ-NN]`
   plus a row in `catalogs/open-questions.md`.

## The measurability test

Before submitting, reread every acceptance line and ask: *could two competent engineers disagree
about whether this passed?* If yes, it is not yet a criterion. "Responds quickly" fails.
"p95 ≤ 200 ms measured at the load balancer over a 5-minute window" passes.

## Gate

`human_approval`. Report the scenario count, the OQs opened, and the out-of-scope list.
Do not begin `plan` until the human says yes.

## Do NOT

- Do not name a table, endpoint, class or library. That is `plan`'s job, and deciding it here
  removes the plan's freedom to find a better design.
- Do not write a scenario you have no way to test.
