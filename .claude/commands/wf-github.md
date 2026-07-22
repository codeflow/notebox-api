---
description: Sync the current pipeline state to GitHub — issues, PRs, comments, board
argument-hint: "[status | on --repo owner/name | sync <feature-id>]"
---

Drive GitHub for the pipeline. Input: `$ARGUMENTS`. Full rules: `instructions/github-sync.md`.

1. `./bin/wf status` — render it first.
2. `./bin/wf github status` — is it enabled, and what is already linked?
   - Not enabled and the user wants it on:
     `./bin/wf github on --repo <owner/name> --assignee <user> [--project N] [--reviewers a,b]`
3. Figure out what GitHub is missing versus the pipeline:
   - A feature with no `issue` → generate it (`./bin/wf github issue <feat>`), show the human the
     `gh issue create` command **and wait for a yes**, then `./bin/wf github link --issue N`.
   - A gate just passed → `./bin/wf github comment <step> --event gate|done`, confirm, post.
   - Tasks done since last sync → one semantic commit each (`./bin/wf github commit <task>`).
   - `implement` green with no PR → `./bin/wf github pr <feat>`, confirm, open, link `--pr`.
   - `audit` passed → move the board card to Done; merge only after explicit human approval.

**Every outward action is confirmed before it runs.** Show the exact command and payload; a yes
must be explicit. Never push to the default branch; never merge before audit pass + approval.
Generate payloads with `bin/wf`, never hand-composed. On any `gh` error, report it and stop —
do not mark the pipeline step done on a failed sync.
