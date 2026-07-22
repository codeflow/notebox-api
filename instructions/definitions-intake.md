---
step: definitions
model: haiku
effort: low
subagent: Explore
background: true
reads: [definitions/**]
writes: [definitions/INTAKE.md, definitions/processed/**]
gate: none
---

# Intake raw material

**Goal.** Convert whatever the human dropped into `definitions/` — transcripts, briefs, emails,
screenshots, half-formed notes — into one structured digest the PRD step can consume without
re-reading the originals.

This step is **background** and **read-heavy**: exactly the shape that belongs in a sub-agent.
Fan out with `Explore`, keep the conclusion, discard the file contents.

## Procedure

1. Inventory `definitions/` (excluding `processed/`). If empty, mark the step `skipped` with a
   note and stop — do not fabricate inputs.
2. For each input, extract only:
   - **Claims** — statements about what the product must do.
   - **Constraints** — budget, deadline, regulation, platform, integration.
   - **Decisions already made** — with who made them, if stated.
   - **Contradictions** — two inputs disagreeing. Never silently pick a winner.
3. Write `definitions/INTAKE.md`: one table of claims with a source citation
   (`file § location`) per row, then a list of contradictions and gaps.
4. Move every fully consumed input to `definitions/processed/`.

## Output contract

`definitions/INTAKE.md` is the only artifact downstream steps read. If a claim is not in it,
it does not exist as far as the PRD is concerned.

## Done when

- Every input is either digested into `INTAKE.md` or explicitly listed as unusable and why.
- Contradictions are surfaced as questions, not resolved by the agent.

## Do NOT

- Do not interpret intent. "The client seemed to want X" is a contradiction to raise, not a claim.
- Do not delete originals. Archive them.
