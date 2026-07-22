---
description: Review and adjust which agent (model/effort) runs each pipeline step
argument-hint: "[step-id | feature-id]   (empty = the whole pipeline)"
---

Review the agent assignments. Input: `$ARGUMENTS`.

## 1. Show

Run `./bin/wf agents $ARGUMENTS` and reproduce the table. Explain the two lines that confuse
people the first time:

- **(defaults)** is what steps without their own agent inherit (`wf agent defaults …` changes it).
- The **main session model** — the one talking right now — is NOT in `workflow.json`; it is the
  Claude Code session setting, chosen with the model selector (`/model`). Say which model the
  session is currently on if you know it.

## 2. Offer adjustments — cards, in a loop

One **AskUserQuestion** card: "Adjust any agent?" → *No — looks right (Recommended)* /
*Yes — pick a step*. If yes:

1. Card listing the steps most worth reconsidering (up to 4; the rest reachable via "Other" with
   the step id). For each option, show current assignment in the description.
2. For the chosen step, one call with two cards: **model** (`haiku` · `sonnet` · `opus` ·
   `fable`) and **effort** (`low` → `max`), current values marked, each option carrying the
   guidance one-liner: haiku = mechanical/high-volume; sonnet = the default; opus = ambiguity
   and irreversibility; fable = long-horizon synthesis. Raise effort for ambiguity and
   irreversibility, **not** for volume.
3. Apply: `./bin/wf agent <id> --model <m> --effort <e>`. Parents propagate to substeps that
   don't override — say so when it happens.
4. Loop back to "Adjust any agent?" until *No*.

## 3. Guardrails

- If the human picks something the guidance argues against (e.g. `haiku` on `audit`, or `max`
  on a mechanical sync), say why in one sentence, then **obey** — it is a cost/quality decision
  and it is theirs. Never silently ignore it.
- `--background on` is refused by the engine on gated steps; don't offer it for them.
- Changes touch `workflow.json` only via `wf agent` — never hand-edit here.

## 4. Close

Render `./bin/wf agents` again (scoped, if an argument was given) so the final state is visible,
and `./bin/wf validate`.
