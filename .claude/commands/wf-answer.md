---
description: Answer pending Open Questions one at a time, via question cards, updating the catalog and the PRD as you go
argument-hint: "[OQ-NN | feature-id]   (empty = all pending, most severe first)"
---

Resolve Open Questions interactively. Input: `$ARGUMENTS`.

## 1. Collect

Run `./bin/wf status` (render it) and `./bin/wf oq list`.

- `$ARGUMENTS` is an `OQ-NN` → queue just that question.
- `$ARGUMENTS` is a feature/step id → queue the OQs that block it: the ones its spec lists, the
  ones named in its `blocked` note, and any `[TBD — OQ-NN]` inside its artifacts.
- Empty → queue everything pending, **blockers first** (🔴 → 🟡 → 🟢).

Nothing pending? Say so and stop — do not manufacture questions.

## 2. Ask — one card round per question, never a wall

For each queued question, ONE **AskUserQuestion** call containing exactly two questions:

1. **The OQ itself.** Header = its id (e.g. `OQ-03`). Read the full block first
   (`./bin/wf oq show OQ-NN`) and check INTAKE/PRD/constitution for context.
   - If the answer space is enumerable, offer the real options — put the one the sources favor
     first, marked "(Recommended)", and say why in its description.
   - If it is genuinely open-ended, still offer 2–3 plausible directions drawn from the sources
     as options; the human can always type their own via "Other". Never present options you
     invented without grounding — a wrong-looking option biases the answer.
2. **"Continue?"** Header `Next`. Options: "Answer the next question (N left)" / "Stop here".

This is what keeps the human unstuck: every single round has an exit.

## 3. Record — after EVERY answer, before the next card

1. `./bin/wf oq answer OQ-NN "<the decision>" --by <human>` — catalog updated, history appended.
2. **Propagate to the PRD** (highest version): flip the §Open Questions row to resolved with the
   decision, and replace inline `[TBD — OQ-NN]` markers with the decided content, citing
   `(decisão humana <date>)` as Origin. Requirements that were half-written pending this answer
   get completed now.
3. **Unblock**: if a step is `blocked` citing this OQ, `./bin/wf reopen <step-id>` so `wf next`
   can offer it again.
4. **Ripple check**: if the decision contradicts an already-approved artifact (spec, plan,
   constitution), do NOT silently rewrite it — say what it invalidates and propose
   `wf reopen <step> [--cascade]`. The human decides.
5. One confirmation line: `OQ-03 ✅ "<decision>" · PRD atualizado · restam N`.

An answer of "I don't know yet" / skip is legitimate — the question stays open, move on.

## 4. Stop

When the human picks "Stop here" (or the queue empties): render `./bin/wf status`, summarize
answered vs. remaining, and name the next pipeline action. If everything that blocked the
current step got resolved, offer to continue with `/wf-next` — do not auto-continue.
