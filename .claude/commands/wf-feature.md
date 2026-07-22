---
description: Add an SDD feature as a pipeline step, with its full lifecycle
argument-hint: '<slug> "Title" [--us US-1.1]   (empty = pick from the backlog)'
---

Add a feature. Input: `$ARGUMENTS`.

1. `./bin/wf status` — render it first.
2. Resolve which User Story:
   - **`--us US-X.Y` given** → confirm it exists in `catalogs/epics.md`.
   - **Free text only** → map it to an existing US, or propose a new one with the next id under
     the right epic, and add it to `catalogs/epics.md` before continuing.
   - **Empty** → read `catalogs/epics.md`, list the undelivered stories as
     `US-X.Y · summary · FRs · status`, and ask which one. Recommend one, with the reason —
     usually the story that unblocks the most other work or de-risks the architecture earliest.
3. Check its Open Questions. If an OQ blocks the story, say so now: the feature will stall at
   `spec` and it is cheaper to know before creating it.
4. **Workspace mapping** — if `workspace.projects` exists, ask FIRST (one card, multiSelect)
   which projects this story touches: the hub and/or each satellite, with your recommendation
   derived from the US (user-facing → API + frontends; internal rule → hub only). The API-side
   feature comes first in work order — its contracts are what the others consume.
5. Create it — one command, all affected projects:

   ```bash
   ./bin/wf feature add <slug> "Title" --us US-X.Y [--project app-api,app-web]
   ```

   This appends one `feature` step per target (spec → plan → tasks → implement → audit → publish → review as
   substeps, each with its model, effort and gate preset) and creates each feature directory.
   **Never hand-edit `workflow.json` to add a feature** — the command keeps ids, numbering and
   directories consistent, and hand edits drift.
6. Update the US status in `catalogs/epics.md` to `speccing` and record the feature directories.
7. `./bin/wf validate`, then report the new pipeline state.
8. The command printed the new feature's agent lineup (spec opus·high → audit opus·xhigh) and an
   `agent review prompt:` line. **If it says `off`, skip this step silently.** Otherwise ask, one
   card: **"Review the agents for this feature?"** → *No — defaults are right (Recommended)* /
   *Yes*. On yes, follow the `/wf-agents` flow scoped to this feature
   (`./bin/wf agents <feature-id>`). Worth a yes when the feature is unusually mechanical
   (spec/plan could drop to sonnet) or unusually risky (implement could rise to opus).

Do not start the spec in the same turn. Creating the step and doing the work are separate
decisions, and the human may want a different story first.
