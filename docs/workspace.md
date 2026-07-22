# Workspaces — one pipeline, many projects

A real product is rarely one repository. `users-api` ships with `users-web` and `users-mobile`,
and they share the same product intent, the same constitution, the same backlog. Duplicating the
SDD structure into each repo would fork the source of truth three ways.

A **workspace** keeps ONE pipeline across all of them:

- The **hub** owns everything the pipeline needs: `workflow.json` state, `constitution/`, `prd/`,
  `catalogs/`, `features/`, `instructions/`, the harness. There is exactly one hub.
- Each **satellite** owns its code, plus a minimal kit: a `workflow.json` that points at the hub,
  `bin/wf`, the `.claude/` hook and commands, and *local* `CLAUDE.md`, `MEMORY.md`, `HANDOFF.md`.
  No SDD documents live in a satellite — ever.

## Two topologies

**Hub embedded in the main project** — the API is usually the natural owner:

```
users-api/        ← hub: pipeline + SDD docs + its own code
users-web/        ← satellite
users-mobile/     ← satellite
```

```bash
./bin/wf create ~/Projects/users-api \
  --satellites "users-web:web:org/users-web,users-mobile:mobile:org/users-mobile"
```

**Dedicated controller** — when no code project should own the pipeline:

```
users-workflow/   ← hub (controller): pipeline + SDD docs, no code
users-api/        ← satellite
users-web/        ← satellite
```

```bash
./bin/wf create ~/Projects/users-workflow --controller \
  --connect "../users-api:api:org/users-api,../users-web:web:org/users-web"
```

`--satellites` **creates** new sibling directories; `--connect` wires **existing** ones
(their files are preserved; only missing kit files are added). Both take
`name-or-path:type[:owner/repo[:stack+stack]]` — the 4th segment is the satellite's tech stack
(`+`-separated inside the comma-separated list): `users-web:web:org/users-web:react+next`.
Later additions: `./bin/wf workspace add ../users-tv --type web --stack "react,tv"`.

Each satellite's **stack** is recorded twice on purpose: in the hub's `workspace.projects[]`
(where the `plan` step of a routed feature reads it — a feature routed to `users-web` is
designed against react/next, not against the hub's stack) and in the satellite's own
`workflow.json → project.stack` (what a session opened there sees).

## Opening a session in a satellite

This is the part that makes the model work: **every `wf` command run in a satellite operates on
the hub transparently.** `state()` resolution follows `workspace.hub`, so:

- The per-turn hook renders the **hub's** pipeline (the banner shows `via <satellite>`).
- `wf next / start / done / feature add` read and write the **hub's** `workflow.json`.
- The satellite's own `CLAUDE.md` tells the agent where the SDD truth lives and to read step
  instructions from the hub.

You can open Claude Code in whichever repo you are editing; the pipeline is the same one.

## Routing features to repos

Each workspace project carries its own `repo`. Route a feature when you create it:

```bash
./bin/wf feature add login-screen "Login screen" --us US-3.1 --project users-web
```

`wf github issue|pr` for that feature then emit `REPO org/users-web` — its issue, PR, branch and
commits belong to the satellite's repository. Features without `--project` target the hub's
`github.repo`.

**Cross-cutting stories** map onto every affected project in one command — a comma list creates
one feature per target (the hub's own name, or `hub`, targets the hub), all sharing the US:

```bash
./bin/wf feature add create-login "Login" --us US-1.2 --project users-api,users-web
# → feat-001-create-login-users-api (hub) + feat-002-create-login-users-web (@users-web)
```

Work the API-side feature first: its `contracts/` are what the frontend features consume.

**Implementing in a satellite** needs the satellite's own verify signal — the hub's `verify`
proves nothing about another repo. Configure it once, from a session in the satellite
(`/wf-setup` there detects the satellite role), or directly:

```bash
cd ../users-web && ./bin/wf workspace init --stack "react,next" --verify "npm test" --repo org/users-web
```

This writes the satellite's local `workflow.json` (including `harness.verify`) and mirrors
stack/repo/type to the hub's `workspace.projects` entry. A routed feature's `implement` stage
runs THAT verify, in that directory.

## The pipeline view

There is **one pipeline** — routing shows up as an `@project` tag on the feature, and the header
counts the satellites (`sats N`) or names your vantage point (`via users-web`):

```
PIPELINE · users-api
░░░░░░░░░░░░  0/19 · harness ✔ · gh ✔ · lang en · sats 3
▸ delivery  Feature delivery
├ ▸ feat-001-login-screen  Tela de login (US-3.1)  @users-web
├ ○ feat-002-push-alerts   Alertas push (US-3.2)   @users-mobile
└ ○ feat-003-audit-log     Log de auditoria (US-4.1)
```

`feat-003` has no tag: it lands in the hub's own code. The same banner renders from any
satellite session — only the `via` fact changes.

## What lives where

| | Hub | Satellite |
|---|---|---|
| `workflow.json` | full pipeline + state | minimal: `workspace.hub` pointer, name/type, repo |
| SDD docs (constitution, PRD, catalogs, features) | ✔ | never |
| `instructions/`, harness, schema | ✔ | — |
| Code | only if the hub is also a code project | ✔ |
| `MEMORY.md` / `HANDOFF.md` | pipeline-level | local to the project |
| `bin/wf`, `.claude/` hook + commands | ✔ | copy (so sessions work there) |

## Integrity

`wf validate` on the hub also checks the workspace: project paths exist, each satellite's
`workflow.json` points back at this hub, no duplicate names, and no feature routed to an unknown
project. `wf workspace` prints the same link health at a glance.

## Honest limitations

- Satellites carry a **copy** of `bin/wf` so their hooks work standalone; if you upgrade the
  hub's copy, re-copy it to satellites (`cp hub/bin/wf sat/bin/wf`). Divergence only affects CLI
  behavior, never state — state has a single writer file in the hub.
- `wf github on` run from a satellite configures the **hub's** github block (state is the hub's).
  A satellite's own repo lives in `workspace.projects[].repo` (hub side) and in the satellite's
  local `workflow.json`; edit those to change it.
- A directory that already has a `workflow.json` refuses to become a satellite — rebinding an
  existing pipeline silently would corrupt it. Decide what that file is first.
