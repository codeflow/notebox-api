---
step: release
model: sonnet
effort: medium
reads: [workflow.json, features/**, catalogs/**]
writes: [HANDOFF.md, ROADMAP.md, MEMORY.md]
gate: none
---

# Handoff, roadmap and memory

**Goal.** Leave the project resumable by someone — or some context — that was not here.

> This step is the **final comprehensive pass**, not the only writer: HANDOFF/ROADMAP get a
> small refresh at every feature close (review step) and at every promotion, and MEMORY grows
> opportunistically whenever a durable decision lands. Here you make them pass the cold-start
> test as a whole.

## HANDOFF.md

Written for a reader with zero context. It must answer, without them opening anything else:

- What state is the project in right now?
- What was just finished, and what is the next step (`wf next` output)?
- What is blocked, on whom, and since when?
- What would surprise a newcomer? Non-obvious decisions, workarounds, traps.
- How do I run and verify this locally, from a clean checkout?

Test it honestly: could you resume tomorrow from this file alone, with the conversation gone?
If not, it is not finished.

## ROADMAP.md

What is delivered, what is next, what is deferred — each deferral with its reason. Deferrals
without reasons come back as the same argument three months later.

## MEMORY.md

The index of durable facts, one line each. Record only what is **non-obvious and not derivable**
from the code or git history:

- Product constraints that are not visible in the code.
- Decisions plus their rationale — especially rejected alternatives.
- External references: dashboards, tickets, contacts.
- Working-style feedback from the human.

Do **not** record: code structure, what a function does, a bug you already fixed, anything
already in `CLAUDE.md`. Convert relative dates to absolute ones — "last week" is worthless to a
future reader.

## GitHub (if enabled)

Update the CHANGELOG from the delivered features: `./bin/wf github changelog <feature-id>` prints
a Keep-a-Changelog entry per feature. Refresh the README where the delivered work changed how the
project is used. Commit these (`docs:`/`chore:` type) and open a release PR — confirmed with the
human, never pushed to the default branch directly. See `instructions/github-sync.md`.

## Done when

- `HANDOFF.md` passes the cold-start test above.
- `ROADMAP.md` matches the real state in `workflow.json` — no step marked delivered that
  `wf status` shows as pending.
- `MEMORY.md` gained only facts that survive the filter.
