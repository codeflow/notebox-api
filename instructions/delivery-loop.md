---
step: delivery
model: inherit
effort: inherit
container_for: features
reads: [catalogs/epics.md, workflow.json]
writes: [workflow.json, features/**]
---

# Feature delivery

**Goal.** Turn the user story backlog into shipped features, one feature per step, until the
epics are delivered or explicitly deferred.

This step is a **container**: it holds no work of its own. Its children are features, appended
as the project runs. That is the mechanism behind *any SDD feature is also a step*.

## Adding a feature

```bash
./bin/wf feature add checkout-refund "Refund a completed checkout" --us US-2.3
```

**Workspace mapping — do this BEFORE creating.** When `workspace.projects` exists, first decide
which projects the story touches: an API-only rule touches the hub; a user-facing story like
create-login usually touches the API **and** each frontend. Then create the whole set in one
command — one feature per affected project, all sharing the US:

```bash
./bin/wf feature add create-login "Login de usuário" --us US-1.2 --project app-api,app-web
```

Order of work follows the dependency: the API-side feature first (its `contracts/` are what the
frontend features consume), then the satellites. Never fold two projects into one feature — each
has its own repo, PR and verify signal.

This appends a `feature` step with the full lifecycle as substeps — spec → plan → tasks →
implement → audit → publish → review — each with its own model, effort and gate already set, and creates
`features/NNN-slug/`. Do not hand-edit `workflow.json` to add features; the command keeps
numbering, ids and directories consistent.

**If `github.enabled`**, opening a feature also opens its GitHub issue (assigned to
`github.assignee`) and adds a board card — see `instructions/github-sync.md`. Generate the issue
with `./bin/wf github issue <feature-id>`, confirm the `gh` command with the human, then record
the number with `./bin/wf github link`.

## Choosing the next feature

Read `catalogs/epics.md`. Prefer, in order:

1. A US that unblocks the most other work.
2. A US whose Open Questions are all answered — a feature blocked on an OQ will stall at spec.
3. A US that exercises a risky part of the architecture early, while changing it is still cheap.

Announce the choice and the reason before creating the step.

## Loop discipline

- One feature in flight at a time unless the human asks otherwise. Parallel features share the
  catalogs and contradict each other there.
- When a feature finishes its audit, update the US status in `catalogs/epics.md` to `delivered`
  in the same turn. A stale catalog makes the next selection wrong.
- If a feature turns out to be two features, stop, split the US, and say so.

## You will be handed this step more than once

`delivery` is a container, and a container never closes itself — finishing the features created
so far proves nothing about whether the backlog is empty. So `wf next` hands this step back every
time the current features are all done. Each time, do exactly one of:

1. **Backlog still has undelivered stories** → pick the next one, `wf feature add`, and continue.
2. **Backlog is empty or the rest is deferred** → confirm with the human, then `wf done delivery`
   to open the way to `release`.

Never close the container on your own reading alone. Say which stories remain and let the human
confirm that stopping is intended — the alternative is a pipeline that quietly declares delivery
finished with half the epics untouched.

## Done when

Every US in `catalogs/epics.md` is `delivered` or `deferred` with a stated reason, the human has
confirmed, and `wf done delivery` has been run explicitly.
