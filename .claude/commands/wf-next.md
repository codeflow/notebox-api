---
description: Execute the next eligible step in the pipeline
argument-hint: "[step-id]  (empty = whatever ./bin/wf next resolves)"
---

Execute one step. Input: `$ARGUMENTS`.

1. `./bin/wf status` — render it at the top of your reply.
2. Resolve the target: `$ARGUMENTS` if given, otherwise the first result of `./bin/wf next`.
   If `$ARGUMENTS` names a step that `wf next` does not list, **stop** and print its blockers
   (`./bin/wf show <id>`). Do not route around a gate.
3. `./bin/wf show <id>` — read the resolved model, effort, gate and produces.
4. **Approval re-entry.** If the resolved step is a `review` (or promotion) waiting on the
   human's GitHub approval, do NOT treat the /wf-next invocation as the approval. Run the
   protocol in `instructions/feature-review.md`: check the PR's real state via `gh pr view`
   (merged? approving review + 👍?); if satisfied, merge/close and continue; if not, question
   it — "você não deu o approve no GitHub" — and stop. An explicit chat approval is mirrored to
   GitHub before merging.
5. **Open Questions pre-flight.** Check `./bin/wf oq list` against this step: an OQ named in its
   `blocked` note, listed by its feature's spec, or appearing as `[TBD — OQ-NN]` in the artifacts
   it consumes. If any block THIS step, switch to the `/wf-answer` flow for exactly those
   questions (question cards, one at a time, exit offered every round) before doing the work —
   implementing on top of an open question is how invented requirements enter the code. OQs that
   don't touch this step are none of this turn's business.
6. **Read the step's `instructions` file.** This is not optional and not skippable because the
   step "looks obvious" — the constraints that make the step correct live in that file.
7. Do the work at the declared model and effort. If the step declares a `subagent_type`, delegate
   to it. If it declares `background: true`, dispatch it and continue — do not block, and do not
   predict its result.
8. Close the loop:
   - `./bin/wf done <id>` when the acceptance criteria hold and `produces` exist.
   - `./bin/wf block <id> "reason"` when an Open Question or a red signal stops it — name the
     OQ id in the reason so `/wf-answer` and the pre-flight can find it.
   - Nothing yet? Leave it `in_progress` and say exactly what remains.
9. If the step's gate is `human_approval`, present what needs approving and **stop**. Do not
   continue into the next step in the same turn.

Report: what changed on disk, the new pipeline state, and the next action.
