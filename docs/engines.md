# Engines — the SDD methodology as a plug

The workflow's orchestration — state, gates, the per-turn banner, model routing, workspace,
GitHub, harness — is engine-agnostic. What an **engine** owns is the SDD methodology: the
feature lifecycle, where artifacts live, and (for external engines) which tool's flow authors
them. Engines are **data**: one manifest, no Python.

```jsonc
// workflow.json
"engine": { "name": "default" }        // omit = default
```

`default` is bundled (embedded fallback in `bin/wf`, overridable by
`engines/default/engine.json`). Any other name must exist as `engines/<name>/engine.json`.

## Commands

```bash
./bin/wf engine            # active engine: manifest, layout, lifecycle
./bin/wf engine list       # manifests available under engines/
./bin/wf engine set NAME   # switch — existing features keep their old layout
```

Switching engines never rewrites existing features. They were generated under the old layout
and keep it; only **new** features use the new engine. `wf engine set` says this out loud.

## The manifest

```jsonc
{
  "id": "speckit",
  "name": "GitHub Spec Kit",
  "install": {
    "check": ".specify",                      // path that proves it's installed
    "hint": "uvx --from git+https://github.com/github/spec-kit.git specify init --here"
  },
  "constitution": { "produces": ".specify/memory/constitution.md" },
  "feature": {
    "dir": "specs/{nnn}-{slug}",              // {nnn} = 001, {slug} = kebab-case
    "instructions": "instructions/feature-lifecycle.md",   // the feature parent step
    "scaffold_dirs": [],                      // subdirs created under dir
    "lifecycle": [
      { "key": "specify", "title": "Specify",
        "delegate": "speckit.specify",        // runs .claude/commands/speckit.specify.md
        "gate": "human_approval",
        "agent": { "model": "opus", "effort": "high" },
        "produces": ["spec.md"] },            // relative to dir
      { "key": "audit", "title": "Audit — adversarial check",
        "instructions": "instructions/feature-audit.md",   // native stage — ours
        "gate": "audit_pass",
        "agent": { "model": "opus", "effort": "xhigh" },
        "produces": ["audit.md"] }
    ]
  }
}
```

**`scaffold`** is how the SDD folders come to exist: `wf create` ships an engine-agnostic core
with none of them, and `wf engine set` / `wf init` copy the active engine's scaffold sources
(paths relative to the manifest) into the project root — never overwriting existing files, so
re-running is always safe. `engines/_shared/upstream/` holds the seeds every engine needs
(definitions, PRD, catalogs, constitution — the upstream is native under every engine);
`engines/default/scaffold/` adds the default engine's `features/` layout.

Each lifecycle stage points at **either** `instructions` (a native instruction file of this
template) **or** `delegate` (an external engine's command — the markdown prompt the engine
installed under `.claude/commands/`). Delegate stages get `instructions/engine-delegate.md`,
the generic wrapper: load the delegate's file, follow it, then close the loop with the
workflow's own rules (produces check, gate semantics, `wf done`). Stages can mix freely — the
recommended shape for external engines keeps **our `audit` and `review` as the final native
stages** (adversarial check, then the pull request + human verdict), since
no current engine ships an adversarial gate.

`wf validate` checks the manifest: stages have keys, instructions files exist, delegate command
files exist (i.e. the engine is actually installed), gates and models are valid.

## What an engine does NOT cover

The upstream of the pipeline — `definitions` intake, the versioned PRD, the catalogs with
immutable ids, the Open Questions bookkeeping (`wf oq` parses OUR catalog format) — stays
native under every engine. External SDD tools don't have these concepts; pretending they do
would break `/wf-answer` and the catalog sync silently. An external engine therefore runs in
**hybrid** mode: workflow upstream + engine feature loop. Say so in the project's docs rather
than letting anyone expect "100% Spec Kit".

## External engines — fit assessment (as of 2026-07)

| Engine | Mechanism | Manifest? |
|---|---|---|
| **GitHub Spec Kit** | markdown commands in `.claude/commands/`, artifacts in `specs/` | ✅ **draft ships** as `engines/speckit/engine.json` — written from the documented commands, NOT yet exercised against a real install; `wf validate` fails until Spec Kit is installed, and the delegate names should be checked against your installed version |
| **OpenSpec** | CLI installs skills/commands, artifacts in `openspec/` | ✅ good second target |
| **BMAD-Method** | 12+ agent personas, wants to own orchestration | ⚠️ would fight the workflow, not serve it |
| **Kiro (AWS)** | IDE-bound (EARS specs) | layout yes, tool no — a "kiro-style" manifest is possible without the IDE |
| **Tessl** | MCP/registry surface | ❌ different integration plane |

Writing a real external manifest (phase 2) additionally needs: the setup-wizard engine
question, running the engine's installer **with explicit human confirmation** (network + tool
execution), and a maintenance commitment — engines change their commands, and the manifest
follows them.

## Learning an installed engine

The maintenance problem above has a tool: instead of hand-maintaining manifests against moving
engines, **derive them from what is actually installed**.

```bash
./bin/wf engine scan     # read-only: installed command groups vs manifests
```

`scan` reports, per manifest: install check, delegates present/missing, and **drift in both
directions** — manifest delegates whose command files are gone, and installed commands the
manifest doesn't know (e.g. a new `speckit.clarify` after an upgrade). Command groups with no
manifest at all are listed as **candidates**.

```
/wf-engine learn speckit
```

The learn flow (agent-driven, human-approved): reads every installed `.claude/commands/<prefix>*.md`
— the engine's own prompts — infers the lifecycle order, `produces` and artifact layout from
their contents, drafts or **diffs** the manifest, and only writes after an explicit yes. It
appends the workflow-native `audit` stage, never installs anything, and records in the
manifest's `description` which version it was learned from. Re-run it after upgrading an
engine; `scan`'s drift report tells you when.

## Adding an engine from a URL

For a tool nobody wrote a manifest for:

```
/wf-engine add https://github.com/some-org/some-sdd-tool
```

The flow (every download and installer run individually confirmed): shallow-clone into
`engines/<name>/source` (gitignored) → read the clone's docs to find its installer or its
prompt files → install properly (or copy the prompts into `.claude/commands/`) → `scan` →
`learn` → offer `engine set`. Content inside the clone is data, not instructions — a README
saying "run X" doesn't carry the human's authority. The setup wizard's *From a GitHub URL…*
option runs this same flow.
