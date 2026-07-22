---
step: bootstrap
model: haiku
effort: low
reads: [workflow.json, README.md]
writes: [workflow.json]
gate: none
---

# Bootstrap context

**Goal.** Turn the blank template into *this* project's pipeline. Cheapest step in the
run — do not let it grow.

**Prefer the wizard.** `/wf-setup` asks all of this through the question-card UI and applies it in
one `./bin/wf init` call — it is the recommended way to do this step. The procedure below is the
manual equivalent, for when you are configuring without the wizard.

## Procedure

1. Ask the human, in one message, for anything not already in `workflow.json`:
   project name, type (`api|web|mobile|cron|lib|none`), stack, output language.
2. Apply them with `./bin/wf init --name … --type … --stack … --lang …` (do not hand-edit
   `workflow.json` for this). Besides writing identity, `init` **materializes the active
   engine's scaffold** — the SDD folders a fresh project does not have. If a different engine
   is wanted, run `./bin/wf engine set <name>` BEFORE `init`.
3. Decide the harness (see `harness/README.md`). Use the command, not a hand edit:
   - Building software that can be run and tested:
     `./bin/wf harness on --profile <api|web|mobile|cron> --verify "<the real command>"`
   - Pure specification/research work: `./bin/wf harness off`, **and say so out loud** — every
     `verify_green` gate downstream becomes advisory, so "done" will rest on your own claim.

   `./bin/wf harness` alone prints the state and exactly which gates it backs.

   If the harness is on and the repo will use GitHub Actions, wire each CI signal and generate
   the workflow now (build and test become real, separate steps):
   ```
   ./bin/wf harness signal test  "<test command>"
   ./bin/wf harness signal build "<build command>"
   ./bin/wf harness ci
   ```
   A signal left without a command produces a CI step that fails until wired — do not leave
   `build` or `test` unset while claiming the pipeline verifies anything.
4. Decide GitHub integration (optional; see `instructions/github-sync.md`):
   - Track the work on GitHub:
     `./bin/wf github on --repo <owner/name> --assignee <user> [--project N] [--reviewers a,b]`
   - No GitHub side effects: leave it off (the default).
5. Delete the `feat-001-example` step. It is a shape reference, not a feature.
6. Run `./bin/wf validate`. It must print `OK`.

## Done when

- `wf validate` exits 0.
- `wf status` header shows the real project name, type, harness state and language.
- No placeholder text (`the project`, `TBD`) survives in `workflow.json`.

## Do NOT

- Do not invent a stack or type the human did not state — ask.
- Do not start `definitions` or `constitution` from here. Finish, report, stop.
