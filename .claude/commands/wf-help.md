---
description: Explain the workflow — the flow, every slash command, and what to do right now
argument-hint: "[command-name]   (empty = the full guide)"
---

Explain how this pipeline works. Input: `$ARGUMENTS`. Answer in `project.language`.

If `$ARGUMENTS` names a command (e.g. `wf-answer`), explain just that one in depth — what it
does, when to use it, and one concrete example — then stop.

Otherwise give the full guide, **anchored in the project's real state** (run `./bin/wf status`
first and tailor the "where you are" part to it — this is a guide, not a man page dump):

## 1. The flow, in one picture

```
/wf-setup   →  once, at the start: type · language · conventions · harness · engine · GitHub · workspace
               (the chosen SDD engine materializes the SDD folders — a fresh project has none)
/wf-next    →  the only command you need after that. Runs the next step:
               definitions → constitution 🚦 → PRD 🚦 → catalogs
               → per feature: spec 🚦 → plan 🚦 → tasks 🚦 → implement (verify) → audit → publish (CI) → review (PR→develop, 👍+approve) 🚦
/wf-promote →  develop → main, same approval protocol; `wf github pending` lists what awaits
               → release
"aprovado"  →  at every 🚦 the agent STOPS and waits for your explicit yes
```

State lives in `workflow.json`, never in the conversation — losing the session loses nothing;
reopen and `/wf-next` resumes. The banner above every reply is that state.

## 2. The slash commands

| Command | What it does | When |
|---|---|---|
| `/wf-setup` | First-run wizard: cards for type, language, code conventions, comments, harness, GitHub, workspace; review screen; then applies everything | Once, in a fresh project |
| `/wf-next` | Executes the next eligible step at its declared model/effort; stops at gates | Constantly — 90% of usage |
| `/wf-status` | Renders the pipeline and comments on it (stalls, blockers, what's next) | When you want a read, not just the banner |
| `/wf-feature` | Adds a feature (spec→plan→tasks→implement→audit) — guided pick from the backlog, or by US id; routes to a satellite repo with `--project` | Start of each feature |
| `/wf-answer` | Answers pending Open Questions one card at a time; updates the catalog and the PRD as you go; every round has an exit | When OQs accumulate, or when `/wf-next` hits one |
| `/wf-agents` | Reviews/adjusts which model·effort runs each step | After setup, after a new feature, or anytime |
| `/wf-engine` | Shows/switches the SDD engine; `learn` builds a manifest from an installed tool; `add <url>` downloads + learns one | Switching methodology, or after installing/upgrading an SDD tool |
| `/wf-github` | Syncs pipeline state to GitHub: issues, PRs, comments, board — every outward action confirmed first | When GitHub integration is on |
| `/wf-promote` | Promotes develop → main: PR + the same 👍/approve protocol; empty arg opens a card with the pending features | After features accumulate on develop |
| `/wf-validate` | Integrity check: pipeline vs. disk (ids, files, gates, workspace links, steps done with nothing to show) | Periodically; after manual edits |
| `/wf-help` | This guide | — |

## 3. The safety rails worth knowing

- **Gates are real**: `human_approval` needs your explicit yes — silence or "looks good" doesn't
  count. `verify_green` needs the harness command to actually exit 0.
- **No invention**: anything the sources don't cover becomes an Open Question, never a guess.
- **GitHub never fires alone**: every issue/PR/push/merge shows you the exact command first.
- **Going back**: `./bin/wf reopen <step> [--cascade]` — without cascade it warns what became
  stale; with it, everything derived reopens too.

## 4. Close with "where you are"

End by pointing at the concrete next action for THIS project, based on the banner: e.g. "you
haven't run setup — start with `/wf-setup`", or "constitution is waiting for your approval",
or "3 OQs pending — `/wf-answer` before the spec".

Full references: `docs/usage.md` (walkthrough), `docs/workflow-json.md` (every field and CLI
verb), `docs/workspace.md` (multi-repo), `harness/README.md` (verification), `CLAUDE.md` (the
rules the agent lives under).
