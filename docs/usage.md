# Usage — from empty directory to shipped feature

Step by step, with what you type, what the agent does, and where it stops to wait for you.

Nothing here is optional reading for the agent: every stage is governed by a file in
`instructions/`, and the agent reads it before acting.

---

## Step 0 · Install

### New project

From a copy of the template, run `wf create` — it clones the template cleaned (no `.git` history,
no `.github`, no `.claude/settings.local.json`, no `*.tmp`) and inits a fresh repo:

```bash
./bin/wf create ~/Projects/billing-api   # or `./bin/wf create` to be prompted for the path
cd ~/Projects/billing-api
```

The last path component is the project name. Add `--no-git` to skip the `git init`.

The new project is **engine-agnostic core only** — no `constitution/`, `prd/`, `catalogs/`,
`definitions/` or `features/` yet. The SDD engine you pick in `/wf-setup` materializes them
(its scaffold), so a Spec Kit project never gets the default engine's folder layout and
vice versa.

**Multiple repos (api + web + mobile)?** Create a workspace instead — one pipeline in a hub,
satellites for the other projects: `--satellites` (create), `--connect` (wire existing) or
`--controller` (dedicated pipeline project). Full model: [`workspace.md`](workspace.md).

### Existing project

Copy only the workflow layer — everything else is additive. The SDD folders are NOT copied
(they no longer live in the template root); the engine's scaffold materializes them when you
run `/wf-setup` or `wf init`:

```bash
P=~/Projects/existing-app
cp -R workflow/{workflow.json,workflow.schema.json,bin,instructions,engines,\
harness,docs,AGENTS.md} "$P"/
```

Three files need **merging, not copying**:

| File | Why | What to do |
|---|---|---|
| `CLAUDE.md` | You probably have one | Paste `§ 0 Rule zero` at the **top** of yours. That section is what keeps the pipeline alive. |
| `.claude/settings.json` | You probably have one | Merge the `hooks` block only. Overwriting drops your permissions. |
| `.gitignore` | — | Append, don't replace. |

Then point `harness.verify` at the command the project actually has (`npm test`,
`./gradlew check`), not the template's placeholder.

### Verify the install

```bash
./bin/wf status     # the pipeline renders
./bin/wf validate   # → OK  18 steps, all instruction files present, no cycles
```

Open Claude Code in the directory. The `UserPromptSubmit` hook injects the pipeline on every
turn — you should see it at the top of the agent's first reply. If you don't, the hook isn't
wired and everything below degrades to the agent's goodwill.

---

## Step 1 · Setup · the wizard

```
/wf-setup
```

The wizard asks — through Claude's **question-card UI**, not plain prose — for the project type,
output language (which also governs the agent's replies in the session), the **code naming**
policy (`english` `findUsers` · `hybrid` `findUsuarios` · `project-language` `buscarUsuarios`),
the **comments** policy (minimal/detailed scope, stack-appropriate reference docs like Javadoc,
and OpenAPI for APIs), whether to use the harness (and its verify/test/build commands), whether to use GitHub (repo,
assignee, board), and whether the product spans **satellite projects** (existing paths to
connect, or new names to create — see [`workspace.md`](workspace.md)). It applies everything in one `./bin/wf init` call: project
identity, the harness and GitHub blocks, CI generation, and it removes the example feature and
marks `bootstrap` done.

This *is* the bootstrap step, done nicely. Everything it does is also plain CLI, if you prefer:

```bash
./bin/wf init --name billing --type api --stack "node,typescript" --lang pt_BR \
  --harness on --verify "npm run verify" --github off
```

Runs cheaply — it is data entry, not reasoning.

**Decide the language now.** Everything downstream — PRD, specs, plans, comments, commits — is
written in `project.language`. Changing it after the PRD exists means translating everything or
living with a bilingual repo.

**Decide the harness now too:**

```bash
./bin/wf harness on --profile api --verify "npm run verify"   # or: harness off
./bin/wf harness                                              # state + gates it backs
```

`on` means `verify_green` gates are checked against a real command. `off` is legitimate for
research or specification-only work, but then every "done" rests on the agent's own claim — and
the pipeline is required to say so instead of implying a check that never ran.

You can flip it later at any time; it is one command, not a migration.

**Generate the GitHub Actions pipeline** with a real `build` and `test` step:

```bash
./bin/wf harness signal test  "npm test"        # wire each signal's command
./bin/wf harness signal build "npm run build"
./bin/wf harness ci                              # writes .github/workflows/ci.yml
```

Each `harness.signals` entry (`lint`, `typecheck`, `test`, `build` by default) becomes a separate
step in the workflow, with a toolchain picked from `project.stack`. A signal left without a
command produces a step that **fails in CI until wired** — so an unconfigured `build` can't pass
green. Commit and push the file (confirmed) to activate it.

**Decide GitHub too (optional):**

```bash
./bin/wf github on --repo owner/name --assignee octocat --project 3   # or leave off
```

On, the pipeline opens an issue per feature, tracks a Projects v2 card, comments at gates, makes
semantic commits, opens a PR per feature, and updates README/CHANGELOG at release. Every outward
action is confirmed with you first, and `gh` must be installed and authenticated. Off (default),
none of that happens. Full flow: `instructions/github-sync.md`.

✅ **Done when** `wf validate` passes and the banner shows your real project name.

---

## Step 2 · Definitions · `haiku/low` · background

Drop everything you have into `definitions/` first — transcripts, briefs, emails, screenshots,
half-formed notes. Then:

```
/wf-next
```

This step is **background**: it dispatches an `Explore` sub-agent and the pipeline continues.
The digest lands in `definitions/INTAKE.md` — a table of claims, each with a source citation,
plus contradictions and gaps.

The agent will **not** resolve contradictions. Two inputs that disagree become a question for
you, because picking a winner silently is how an invented requirement enters the PRD.

Nothing to intake? The agent marks the step `skipped`. It will not fabricate inputs.

✅ **Done when** every input is digested or explicitly listed as unusable, and consumed files are
archived to `definitions/processed/`.

---

## Step 3 · Constitution · `opus/high` · 🚦 waits for you

```
/wf-next
```

Four substeps: principles (BR), architecture boundaries (AD), compliance checklist, code
standards. Runs on `opus` because this is the highest-leverage step in the whole pipeline — every
downstream artifact inherits its errors.

The bar for a rule: **testable, consequential, project-specific.** "Nothing under `domain/` may
import from `infra/`" is a rule — it is a grep. "Code should be clean" is not.

Ten sharp rules beat sixty vague ones. The constitution is read on every feature, so every dead
line costs tokens forever.

🚦 **The agent stops here and waits for an explicit yes.** Not silence, not "looks good". Review
what it deleted from the scaffolding and which rules it could not ground in a source.

---

## Step 4 · PRD · `opus/high` · 🚦 waits for you

```
/wf-next
```

Reads `INTAKE.md` and the constitution, writes `prd/PRD_v1.md`: problem, measurable outcome,
scope in **and out**, users and jobs, FRs, NFRs, constraints, open questions.

Every requirement carries an **Origin** — the intake row or the human decision it came from.

**Watch the Open Questions in the report.** Anything the sources don't cover becomes
`[TBD — OQ-NN]`, never a plausible-sounding guess. A PRD that reads complete but contains
invented requirements is the most expensive failure mode in this pipeline, because everything
downstream inherits it and nobody knows which parts to distrust.

The out-of-scope list is worth as much as the in-scope one. It is what stops the rework.

🚦 **Waits for your approval.**

---

## Step 5 · Catalogs · `sonnet/medium`

```
/wf-next
```

Mechanical projection of the PRD into four lookup tables, so later steps read a row instead of a
whole document:

| File | Ids |
|---|---|
| `catalogs/requirements.md` | `FR-NN`, `NFR-NN` |
| `catalogs/rules.md` | `BR-NN` (references, not copies) |
| `catalogs/epics.md` | `E-N`, `US-X.Y` |
| `catalogs/open-questions.md` | `OQ-NN` |

**Ids are immutable forever.** Dropped items are marked `deprecated`, keeping their number.
Reusing a number silently corrupts every artifact that cited it — including merged ones.

No gate: it is a projection, not a decision.

---

## Step 6 · Delivery — the feature loop

This repeats per feature until the backlog is empty. **Every feature is a step**, appended under
the `delivery` container.

### 6.1 · Create the feature

```
/wf-feature
```

Empty → the agent lists undelivered user stories and recommends one, with a reason. Or be
explicit:

```bash
./bin/wf feature add refund "Refund a completed checkout" --us US-2.3
```

That appends a `feature` step with all five lifecycle substeps preset, and creates
`features/001-refund/`. **Never hand-edit `workflow.json` to add a feature** — the command keeps
ids, numbering and directories in agreement, and those three drifting apart is what makes a
pipeline stop being trustworthy.

### 6.2 · Spec · `opus/high` · 🚦

What the feature must do, in Gherkin, without deciding how.

```gherkin
Scenario: Refund exceeds the original charge
  Given a checkout of 40.00 EUR that has been fully refunded
  When an agent requests a further refund of 5.00 EUR
  Then the request is rejected with error REFUND_EXCEEDS_CHARGE
  And the ledger balance is unchanged
```

These scenarios **become the tests** in step 6.5. Write them as the contract they will literally
become.

The measurability test: *could two competent engineers disagree about whether this passed?*
"Responds quickly" fails. "p95 ≤ 200 ms at the load balancer over 5 minutes" passes.

The agent will not name a table, endpoint or library here — that is the plan's job, and deciding
it now removes the plan's freedom to find a better design.

### 6.3 · Plan · `opus/high` · 🚦

How. Produces `plan.md`, `data-model.md`, `contracts/`.

Before designing anything, the agent sends an `Explore` sub-agent to find what the codebase
already does. **Reusing the existing pattern beats introducing a better one.**

Each decision is marked **reversible** or **one-way**. Effort goes to the one-way ones: schema
shape, public contract, migrations — anything another team will start depending on.

🚦 Review the **blast radius** especially. That is the part you are best positioned to catch the
agent being wrong about.

### 6.4 · Tasks · `sonnet/medium` · 🚦

Decomposition of an approved plan — mechanical, hence `sonnet`.

A task is atomic when it has one outcome (no "and" in the title), is independently verifiable,
and **ships its test with it**. Each task cites the Gherkin scenario it makes pass; a task citing
none is either unnecessary or the spec has a hole.

Tasks are mirrored into `workflow.json` as substeps of `implement` — this is the fourth level of
nesting, and where `parallel: yes` tasks get `isolation: worktree`.

### 6.5 · Implement · `sonnet/high` · 🚦 verify green

Per task: `wf start` → test first → code → run `harness.verify` → green → `wf done`.

Model stays `sonnet`; only effort rises. **Raise effort for ambiguity and irreversibility, not
for volume** — a thousand-line mechanical refactor is still `sonnet/medium`; fifty lines touching
an auth boundary is `opus/high`.

Parallel tasks run as concurrent sub-agents in separate worktrees, merged only when each one's
own `verify` is green.

Two behaviors worth knowing about:

- **If the plan turns out to be wrong mid-task**, the agent stops and marks the task `blocked`
  rather than improvising a different design. Silent redesign detaches the code from the approved
  artifacts, and the audit finds it much later at much greater cost.
- **A bug spotted elsewhere does not ride along** in the diff. It becomes a backlog item. Unrelated
  fixes in a feature diff are how reviews stop being reviews.

The agent will not weaken a test to make `verify` pass.

### 6.6 · Audit · `opus/xhigh` · 🚦

Adversarial. Runs at the highest effort in the pipeline, above the implementer's, on purpose:
a reviewer who reasons less carefully than the author finds nothing and certifies everything.

It checks traceability (FR → scenario → test → code, following real files), whether the tests
would **actually fail** on regression, constitution compliance by grep rather than assumption,
scope against the plan's blast radius, and whether any Open Question was closed by the
implementer's own assumption.

Verdict: `pass` · `pass with findings` · `fail` (names the substep to reopen).

A clean audit reported as clean is a real result. The instructions explicitly forbid manufacturing
findings to look thorough.

---

## Step 7 · Release · `sonnet/medium`

```
/wf-next
```

Writes `HANDOFF.md`, `ROADMAP.md`, `MEMORY.md`.

The test for `HANDOFF.md`: **could you resume tomorrow from this file alone, with the
conversation gone?** If not, it isn't finished.

---

## Day to day

| Command | Use |
|---|---|
| `/wf-setup` | First-run wizard — asks the setup questions and configures everything (run once) |
| `/wf-status` | Where am I? (the hook already shows it; this comments on it) |
| `/wf-next` | Do the next step — the command you use most |
| `/wf-feature` | Start a feature |
| `/wf-answer` | Answer pending Open Questions — one card at a time, catalog + PRD updated as you go |
| `/wf-agents` | Review/adjust which model·effort runs each step (also offered at the end of setup and after each new feature — silence the per-feature prompt with `wf init --agent-review off`) |
| `/wf-engine` | Show/switch the SDD engine; `learn <prefix>` builds a manifest from an installed engine |
| `/wf-help` | The guide: the flow, every command, and what to do right now |
| `/wf-github` | Sync the current state to GitHub (issues, PRs, comments, board) |
| `/wf-validate` | Does the pipeline still match what's on disk? |

Direct CLI, when you'd rather not go through the agent:

```bash
./bin/wf status --full          # including completed substeps
./bin/wf show <id>              # one step, and why it's blocked
./bin/wf block <id> "reason"    # park it
./bin/wf skip  <id> "reason"    # not doing this one
```

---

## When things go wrong

**"Nothing eligible"** — something is blocked. `./bin/wf show <id>` prints the reason chain.
Blockers are inherited: a child can't start while an ancestor can't.

**The agent stopped and is waiting** — it hit a `human_approval` gate. That is the design. Say
yes explicitly; it won't infer approval from a follow-up question.

**A step is `done` but produced nothing** — someone used `--force`. `/wf-validate` catches this.
Reaching for `--force` almost always means the step isn't actually done; the guard is the
cheapest reviewer you have.

**A step has been `in_progress` for many turns** — the pipeline is stalled. `/wf-status` is
instructed to name this, because it's invisible otherwise.

**You need to change something already done (e.g. update the PRD)** — reopen the step:

```bash
./bin/wf reopen prd --cascade
```

`reopen` sends a done step back to `pending`. `--cascade` also reopens everything derived from it
(catalogs, features…), because changing the PRD makes them stale — without cascade the pipeline
would keep reporting them done. Without `--cascade` it reopens only that step and **warns** which
downstream steps are now stale. Deliberately `skipped` steps are left alone. For a real PRD change,
the intended pattern is a new version (`PRD_v2`) re-derived downstream, not editing v1 in place.

**You lost the context / it got summarized** — nothing is lost. State lives in `workflow.json`,
decisions live in the artifacts. Open a new session; the hook re-injects the pipeline and the
agent resumes from `wf next`. This is the whole reason state is a file and not a conversation.

**The agent is working on the wrong step** — say so. It is instructed never to route around the
pipeline: if it thinks the pipeline is wrong, it must propose the edit rather than ignore it.
