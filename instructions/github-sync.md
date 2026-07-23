---
step: (cross-cutting — not a pipeline stage)
reads: [workflow.json]
writes: [workflow.json, GitHub via gh]
gate: none
---

# GitHub sync

**Goal.** Materialize the pipeline on GitHub: one issue per feature, a Projects v2 card that
tracks its status, comments at each gate, semantic commits per task, one PR per feature, and
README/CHANGELOG updates at release.

Active only when `github.enabled` is true in `workflow.json`. If it is false, skip everything
here silently — no issues, no PRs, no comments.

## The division of labour — read this first

`bin/wf` is **offline**. It generates the exact payloads and records the resulting numbers. It
never touches the network. **You** run `gh`, and only after the human confirms.

```
wf github issue <feat>      → prints TITLE / ASSIGNEE / BODY  →  you run: gh issue create ...
wf github comment <step>    → prints the comment body         →  you run: gh issue comment ...
wf github commit <task>     → prints a Conventional Commit     →  you run: git commit -m ...
wf github pr <feat>         → prints TITLE / BASE / HEAD / BODY →  you run: gh pr create ...
wf github changelog <feat>  → prints a CHANGELOG entry          →  you edit the file
wf github link <feat> --issue N --pr M --branch B --card ID     →  records the mapping
```

Always generate the payload with `bin/wf` rather than composing it yourself. That keeps issue
bodies, commit scopes and PR text consistent, and keeps the checklist in sync with real task
status.

## Applying the payload's metadata — every field, every time

The payload prints ASSIGNEE / LABELS / MILESTONE / PROJECT / TYPE / REVIEWERS lines. None of
them is decorative:

| Line | How to apply |
|---|---|
| LABELS | **Create missing ones first** — `gh label create <l> --force` is idempotent; a create with a nonexistent label fails. Then `--label a --label b`. |
| ASSIGNEE | `--assignee <user>`. |
| MILESTONE | `--milestone "<name>"`. Missing on the repo? Create it (`gh api .../milestones -f title=…`) with confirmation, or drop with a note. |
| PROJECT | `--project "<title>"` on create, or `gh project item-add` after. |
| TYPE | Issue Types are org-level, GraphQL-only. Best-effort: `gh api graphql` setting `issueTypeId`; unsupported on the repo → skip with one note, never fail the flow. |
| REVIEWERS | `--reviewer <user>` on PRs. **`(none configured — ask at submission)` means ASK** — see the review step. GitHub refuses the PR's own author as reviewer: when they match, skip the request with a note (the 👍+approve protocol still covers the approval). |
| Relationships (issue body) | The generated section links same-US siblings. For true parent/child, `gh api graphql` sub-issue mutation — best-effort. |
| Development | Filled by linking the branch to the issue — the publish step uses `gh issue develop` (below). |

## Permission — every outward action is confirmed

Creating an issue, commenting, opening or merging a PR, and pushing are **outward-facing and
hard to undo**. Before each one:

1. Show the human the exact `gh`/`git` command and the payload it will send.
2. Wait for an explicit yes. Silence, "looks good", or a follow-up question is not a yes.
3. Run it. Then record the result with `wf github link`.

**Never** push to `github.default_branch`. **Never** merge a PR before the feature's `audit`
verdict is `pass` and the human has approved. **Never** create standing automation (Actions
secrets, webhooks, branch-protection changes) from here.

## Where each action fires in the pipeline

| Pipeline moment | GitHub action |
|---|---|
| `bootstrap`, if github on | Confirm the repo exists (`gh repo view`); create the Projects v2 board if `project.enabled` and none is set. |
| Feature created (`delivery`) | Open the issue (`wf github issue`), assign it, add it to the board. `wf github link --issue N --card ID`. |
| `spec` / `plan` / `tasks` reach their gate | Comment `--event gate`. On approval, comment `--event done`. |
| `implement` starts | Work happens locally; commits per task prepared (`wf github commit <task>`). |
| Each task done | One semantic commit. Update the issue checklist. |
| `audit` verdict `pass` | Comment the verdict on the issue. Nothing is pushed yet. |
| `publish` step | Branch `feature/<slug>` off the **integration branch** (`integration_branch`, default develop), push (confirmed) → Actions runs build+test on the branch; gate = that run green. `wf github link --branch feature/<slug>`. |
| `review` step | PR `feature/<slug>` → integration branch; CI on the PR; hand to the human. Merge ONLY per the approval protocol (merged, or 👍+approving review, or explicit chat approval mirrored to GitHub). Then `wf github link --merged develop` and **report `wf github pending` in chat**. |
| `/wf-promote` | PR integration → trunk with the same protocol; on merge, `--merged main` for every feature that rode along. |
| `release` | Update README and CHANGELOG (`wf github changelog`), commit, open the release PR. |

## Semantic commits

`wf github commit <task-id>` prints `type(scope): subject` where scope is the feature slug and
subject is the task title. Default type is `feat`; override with `--type fix|docs|test|refactor|chore`
or set `commit_type` on the task in `workflow.json`. It appends `Refs #<issue>` when the feature
is linked. You add the body describing what you actually did this session — the header is
generated so it stays consistent, the body is yours because only you know the change.

## The Projects v2 board

`gh project` speaks to Projects v2. Typical calls (confirm each):

```bash
gh project item-add <number> --owner <owner> --url <issue-url>     # add a card
gh project item-edit --id <card-id> --field-id <status> --single-select-option-id <col>  # move it
```

Store the returned item id with `wf github link --card <id>`. Move the card when the feature's
status changes: Todo when the issue opens, In Progress when `implement` starts, Done on audit
pass. If Projects v2 field ids are not discoverable non-interactively, say so and leave the card
where it is rather than guessing at field ids.

## Failure honesty

If a `gh` call fails (auth, permissions, rate limit), report the real error and stop — do not
retry blindly and do not mark the pipeline step done. The pipeline state and GitHub can diverge;
`wf github status` shows the mapping so you can reconcile them.
