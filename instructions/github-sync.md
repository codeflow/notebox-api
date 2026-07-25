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

## Applying the payload's metadata — every field, every time (MANDATORY)

The payload prints ASSIGNEE / LABELS / MILESTONE / PROJECT / TYPE / PRIORITY / REVIEWERS lines.
None of them is decorative, and **Project, Type, Priority and Development are a required standard
on every issue AND every PR** — not best-effort. A blank Type/Priority, an issue not on the board,
or a branch/PR not linked in Development is a defect, the same as a failing test.

**The rule when a field cannot be applied:** if the only reason is a missing prerequisite — the
`gh` token lacks `project`/`read:project` scope, or the Projects v2 board does not exist yet —
**STOP and surface the prerequisite to the human; do not silently skip it.** See *Required one-time
setup* below. Only genuinely unsupported cases (e.g. the repo's org has Issue Types disabled) are
skipped, and then with an explicit note.

| Line | How to apply |
|---|---|
| LABELS | **Create missing ones first** — `gh label create <l> --force` is idempotent; a create with a nonexistent label fails. Then `--label a --label b`. |
| ASSIGNEE | `--assignee <user>`. |
| MILESTONE | `--milestone "<name>"`. Missing on the repo? Create it (`gh api .../milestones -f title=…`) with confirmation, or drop with a note. |
| PROJECT | **Mandatory.** `--project "<title>"` on create, or `gh project item-add <number> --owner <owner> --url <url>` after. Board missing / no scope → STOP, surface the prereq (do not skip). Record the item id: `wf github link --card <id>`. |
| TYPE | **Mandatory** (Bug / Feature / Task — from the step kind: feature→Feature, fix→Bug, chore→Task). Set on create with `--type "<Type>"` (gh ≥ 2.9x) or after with `gh api graphql` setting `issueTypeId`. Org has Issue Types disabled → skip with one explicit note. |
| PRIORITY | **Mandatory.** Prefer the org's **native issue field** (many orgs — including this one — standardize on native issue fields, NOT a Projects custom field). Discover them with `GET /orgs/<owner>/issue-fields`; set on an issue/PR with `PATCH /repos/<owner>/<repo>/issues/<n>` body `{"issue_field_values":[{"field_id":<id>,"value":"<OptionName>"}]}` — **value is the option NAME string**, not an id. Ids + default live in `github.issue_fields`. If the org has no native issue fields, fall back to a Projects v2 single-select (`gh project item-edit … --single-select-option-id`). |
| REVIEWERS | `--reviewer <user>` on PRs. **`(none configured — ask at submission)` means ASK** — see the review step. GitHub refuses the PR's own author as reviewer: when they match, skip the request with a note (the 👍+approve protocol still covers the approval). |
| Relationships (issue body) | The generated section links same-US siblings. For true parent/child, `gh api graphql` sub-issue mutation — best-effort. |
| Development | **Mandatory.** Link the branch/PR to the issue's *Development* section. For a branch created via the issue use `gh issue develop <N> --base <integration> --name feature/<slug>`. For a branch that already exists, the linkage is populated by the review PR carrying `Closes #<N>` in its body — so the PR body MUST reference the issue. |

**Same fields on the PR.** When the review step opens the PR, apply PROJECT, TYPE and PRIORITY to
the PR item too (a PR is a board item like an issue), plus `Closes #<issue>` in the body so the
issue's Development section links the PR and the issue closes on merge.

## Required one-time setup (prerequisites for the standard above)

The Project/Priority automation needs, once per workspace — these are the **human's** actions,
surface them and wait, never fake around them:

1. **`gh` scopes:** `gh auth refresh -s project,read:project` (Projects v2 read+write). Without it
   `gh project …` fails with *missing required scopes*.
2. **The Projects v2 board exists**, is **linked to the repository**, and is recorded in
   `workflow.json → github.project` (`enabled: true`, `owner`, `number`, `project_id`, the
   discovered `priority_field_id` + option ids, `status_field_id` + option ids, and
   `default_priority`). If it does not exist, create it (with confirmation) once scope is granted:
   `gh project create --owner <owner> --title "<repo>"`; then **link it to the repo** so it shows
   in the repository's *Projects* tab and not only at the org level —
   `gh project link <number> --owner <owner> --repo <owner>/<repo>` (an org/user Project v2 does
   NOT appear inside a repo until linked); then discover its field ids
   (`gh project field-list <number> --owner <owner>`; create a `Priority` single-select field if
   the default board lacks one — `gh project field-create`), and store all ids in config.

Until both are in place, issue/PR creation still proceeds (Type + labels + assignee), but the
Project/Priority/Development steps are reported as **blocked on the prerequisite**, not done.

## gh authentication — required scopes (prerequisite for everything below)

Every GitHub action here runs through `gh` on the **active** account, which must be authenticated
**and hold all of these token scopes** (and the corresponding org permission), or the create calls
fail partway:

| Scope | Needed for |
|---|---|
| `repo` | issues, PRs, labels, commits, branches |
| `workflow` | pushing branches that carry `.github/workflows/*` |
| `read:org` | resolving org membership, assignees, reviewers |
| `project` | creating/editing Projects v2 boards, items, fields |
| `read:project` | reading board/field ids |

One-time grant (choose the account that owns the repos, e.g. the assignee — NOT a different
logged-in account):

```bash
gh auth refresh -h github.com -s repo,workflow,read:org,project,read:project
gh auth status --active     # confirm the ACTIVE account lists every scope above
```

Native **issue Types** and **issue fields** (Priority/Effort/dates) additionally require the org to
have them enabled (`GET /orgs/<owner>/issue-types`, `GET /orgs/<owner>/issue-fields`) and the actor
to be an org member. If `gh` reports *missing required scopes* or an org-permission error, **STOP
and surface it** — do not silently skip the field. Two logged-in accounts is a common trap: verify
`gh auth status --active` points at the account with these scopes before starting.

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
status changes: **Todo** when the issue opens, **In Progress** when `implement` starts and
**through `publish`/`review`**, and **Done only at merge/promotion** — when the issue legitimately
closes.

> ⚠️ **Do NOT set Status = Done before the PR merges.** A Projects v2 board has a built-in
> *"when Status → Done, close the issue"* workflow (enabled by default on new boards), so setting
> Done at `audit`/`review` **auto-closes the still-open issue** — premature, since the issue must
> close only at promotion (per the table above). Keep the card **In Progress** until the merge,
> then set Done (the merge closes the issue anyway). If you must reflect audit-pass on the board,
> use a non-terminal column, never Done.

If Projects v2 field ids are not discoverable non-interactively, say so and leave the card where
it is rather than guessing at field ids.

## Failure honesty

If a `gh` call fails (auth, permissions, rate limit), report the real error and stop — do not
retry blindly and do not mark the pipeline step done. The pipeline state and GitHub can diverge;
`wf github status` shows the mapping so you can reconcile them.
