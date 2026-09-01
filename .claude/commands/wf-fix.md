---
description: Fast lane for fixes — collect a brief, implement it, then join the normal pipeline
argument-hint: "collect [notes] | start | approve | status | drop"
---

The fix lane. Input: `$ARGUMENTS` — the first word is the mode.

Render `./bin/wf status` at the top of your reply as always, then act on the mode. **`collect` is
the exception to acting at all** — see below.

> **Why this exists.** The full SDD lifecycle (spec → plan → tasks → implement → audit → publish →
> review, four of them gated) is right for a feature and far too heavy to move a button 4px. This
> lane keeps what makes the pipeline trustworthy — a written record, a green `verify`, an audit, a
> PR, the human's approval — and drops the ceremony that a small correction cannot repay.
>
> **It is not a way to skip the gates.** `verify` still has to be green, the audit still runs, and
> the PR still waits for the human on GitHub. What is skipped is *documentation proportional to a
> feature*, and the pipeline says so out loud (see `start`, step 3).

---

## `collect` — gather, do NOT act

The human is briefing you. **You are a notebook, not an engineer.**

1. Resolve the brief file: `fixes/<slug>/brief.md`, where `<slug>` is the open brief (the most
   recent directory under `fixes/` whose `brief.md` has no `**Status:** started` line). No open
   brief → create one: `fixes/YYYY-MM-DD-<two-word-topic>/brief.md`, seeded with a `# Brief` title
   and the date.
2. **Append** everything the human just gave you, verbatim where it is text, as a numbered item
   under `## Items`. Never rewrite their words into your own.
3. **Images and files.**
   - A pasted image: you can see it but you cannot save its bytes. Write a **dense factual
     description** into the brief — which screen, which element, what is wrong, any text or
     numbers legible in it, and the human's caption. That description is what survives a context
     compaction; the image itself does not.
   - A path on disk: copy it into `fixes/<slug>/assets/` and reference it.
4. Reply with **at most three lines**: which brief file, how many items it now holds, and the last
   item's first few words so the human can confirm it landed.

**Do NOT, under `collect`:** open project files, search the codebase, diagnose, propose a fix,
estimate, start a branch, or run anything beyond writing the brief. If the human's note contains a
question, record it as an item and answer it only if answering costs nothing — never turn it into
investigation. **Silence is the correct behaviour here.** The human is still thinking.

If a note is genuinely ambiguous, do not ask now — record it verbatim and mark it
`> ⚠ needs clarification at start`. Questions belong to `start`, when they can be asked all at
once against real code.

---

## `start` — implement the whole brief

1. **Read the brief file end to end.** It, not the conversation, is the source of truth — the
   conversation may have been compacted since. If `fixes/` has no open brief, say so and stop.
2. **Resolve the items into a work list.** Group by file where that helps. Anything marked
   `needs clarification` gets asked **now**, all in one question card, before any code. Anything
   the codebase contradicts gets raised now too.
3. **Register it in the pipeline, visibly as a fix:**
   - `./bin/wf feature add fix-<slug> "<one-line summary>" [--project <satellite>]`
   - `./bin/wf skip <id>.spec "fast lane — brief at fixes/<slug>/brief.md"` and the same for
     `.plan` and `.tasks`. **Skipped, never faked.** A spec written after the code is not a spec,
     and the pipeline must show which route the work took.
   - `./bin/wf start <id>.implement`
4. **Branch:** `fix/<slug>` off the integration branch, in the satellite when routed.
5. **Implement, item by item.** Per item:
   - a test when the change is testable (behaviour, state, a rule) — write it first;
   - **no test when the change is only visual** (a colour, a width, a spacing). Say so plainly in
     the record rather than inventing an assertion that cannot see it — a unit test renderer
     (jsdom and friends) computes no styles, so a test that "covers" a CSS change and cannot fail
     is worse than no test at all.
   - commit per item, or per coherent group.
6. **`verify` must be green** before you present anything. It is the same gate as always.
7. **Present the result** — this is the point of the lane:
   - the dev server up, with the touched routes warmed;
   - **for each visual item, evidence that it changed**: a screenshot, or the value measured in
     the running app (e.g. `getComputedStyle` in a browser), or both. "I changed the CSS" is not
     evidence — a guard that checks a rule *exists* says nothing about whether it does anything;
   - a table of items → what changed → how it was verified, and explicitly **what you did not do**
     and why.
8. Write `fixes/<slug>/record.md`: the items, the diff summary, the evidence, anything deferred.
   This is the whole documentation trail for the lane — it replaces spec/plan/tasks, so it has to
   carry the reasoning, not just the file list.

   **Also write `features/NNN-slug/audit.md` when the audit runs** (see `approve`): `wf done`
   looks for the artifact under `features/`, not under `fixes/`. Point it at the real documents
   rather than duplicating them — two copies drift. Found the hard way: `wf done` refuses to
   close the audit step without it.

   **And check the CI watches this branch prefix.** The lane pushes `fix/<slug>`; a workflow
   generated before this was fixed may only list `feature/**`, in which case the push triggers
   **no run at all** — and the publish gate's "green run on the branch" is then satisfied by
   silence. `wf harness ci` now emits `fix/**` and `chore/**`; an older `ci.yml` needs the lines
   added by hand.
9. **Stop.** Do not push, do not open a PR. The human looks first.

---

## `approve` — join the normal pipeline

The human has looked and said yes. From here nothing is fast-laned: the standard instructions
apply in full.

1. `./bin/wf done <id>.implement` (gate: `verify` green — re-run it if anything changed since).
2. **Audit** — `instructions/feature-audit.md`, at its declared model and effort. Audit the diff
   against the **brief**, since that is what stands in for the spec: every item addressed, nothing
   extra riding along, each `applies` compliance item evidenced. A fast lane does not get a fast
   audit — this is the step that catches what the missing spec would have caught.
3. **Publish** — `instructions/feature-publish.md`: issue, branch pushed, CI green **on the
   remote**. Propose the git batch and wait, as always.
4. **Review** — `instructions/feature-review.md`: PR to the integration branch, labels created
   first if missing, `Closes #<issue>` in the body, then **stop for the human's GitHub approval**.
   Approval in chat is mirrored to the PR before any merge. `/wf-fix approve` is **not** that
   approval — it approves the *implementation*, not the merge.
5. Close out exactly as a feature does: `--merged develop`, `catalogs/epics.md`, `HANDOFF.md`,
   `ROADMAP.md`, and the `./bin/wf github pending` report.

---

## `status` — what is in the current brief

Print the open brief's items and whether it has been started. Nothing else.

## `drop` — abandon the open brief

Requires an explicit confirmation naming the brief. Moves it to `fixes/<slug>/brief.dropped.md`
rather than deleting it — a brief the human spent time on is not yours to erase.

---

## Do NOT

- ❌ Act during `collect`. It is the one mode where doing work is the failure.
- ❌ Write spec/plan/tasks after the fact to make the pipeline look complete. Skip them, with the
  reason recorded.
- ❌ Present a visual change without evidence it renders. That is the exact failure this project
  has hit repeatedly — a green suite over a screen nobody looked at.
- ❌ Use this lane for a change that adds behaviour, a route, an endpoint, or a rule. If the work
  needs a decision anyone could disagree with, it needs a spec: say so and route it to
  `/wf-feature`.
- ❌ Merge on `/wf-fix approve`. That word approves the implementation; the merge still needs the
  human on GitHub.
