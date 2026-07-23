---
description: First-run setup wizard — asks the setup questions and configures the whole project
argument-hint: (no arguments — just run it once, before anything else)
---

You are the setup wizard. This is the very first thing run in a fresh project. Drive it with the
**question-card UI** (the AskUserQuestion tool), then apply everything in one deterministic call.

## 1. Render current state

Run `./bin/wf status` and show it. If `bootstrap` is already `done`, tell the human the project
looks configured and ask whether they want to re-run setup before continuing.

**Satellite mode.** If `./bin/wf workspace` says this project is a **satellite**, the pipeline
setup lives in the hub — do NOT run the hub questions from here. Instead run the satellite's
own mini-setup (cards): its **stack**, its **verify command** (the signal `implement` runs for
features routed here — without it nothing can be verified in this repo), and its **repo**
(`owner/name`). Apply with:

```bash
./bin/wf workspace init --stack "react,next" --verify "npm test" --repo owner/users-web
```

That writes the satellite's local `workflow.json` and mirrors stack/repo to the hub. Then stop —
everything else is hub setup.

## 2. Ask, using the question-card UI

Call **AskUserQuestion** — do not ask in plain prose. The flow is **two rounds** on the fast
path; a third round exists only for people who ask for it. Propose the directory name as the
project name (confirm, don't ask open-ended); do not invent a stack — ask for it in round 1's
"Other" or leave it empty.

### Round 1 — the project (one call, 4 cards)

1. **Project type** — `api` · `web` · `mobile` · `cron` · `lib` · `none`.
2. **Output language** — governs every artifact AND your replies in the session (e.g. `en`,
   `pt_BR`). Recommend the language the human is writing to you in. From the moment they
   answer, **switch your own replies to it**.
3. **Code conventions** — one card, three options:
   - *Ecosystem default (Recommended)* — English identifiers (`findUsers`), minimal comments
     (one short comment above each public function/method), the stack's reference docs
     (Java → Javadoc, JS/TS → JSDoc/TSDoc, Python → docstrings, Go → godoc, Rust → rustdoc,
     C# → XML docs), and OpenAPI when the type is `api`. What most teams on this stack expect.
   - *Domain terms in my language* — same as the default, but business concepts from the PRD
     are named in the output language: `findUsuarios` — "find" is not a business concept;
     "Usuário" is (`code_naming: hybrid`).
   - *Customize…* — unfolds Round 3 below.
   Skip the distinction when the output language is English — the default is implied; only
   offer *Customize…* then.
4. **Harness** — "Verify with a real build/test signal?" → on/off. If on, collect the verify
   command and the per-signal `test`/`build` commands afterwards (text is fine).

### Round 2 — the surroundings (one call, 3 cards)

5. **SDD engine** — which methodology drives the feature lifecycle. Build the options from
   `./bin/wf engine list`:
   - *Workflow SDD (Recommended)* — the bundled engine: constitution, versioned PRD, catalogs,
     spec→plan→tasks→implement→audit.
   - One option per other manifest found (e.g. *GitHub Spec Kit* — say it's a draft if its
     description says so).
   - *From a GitHub URL…* — the human pastes the repo URL of an SDD tool; the apply phase
     downloads and learns it (see § 4).
   A fresh project has NO SDD folders yet — the chosen engine's scaffold materializes them.
6. **GitHub** — "Track the work on GitHub (issues, PRs, board)?" → on/off. If on, collect:
   `owner/name`; the **assignee** username; the **reviewer** for PRs (default: the human
   themselves — note that GitHub can't request the PR's author, so when they match the request
   is skipped and their approval comes via the 👍+approve protocol); an optional **milestone**
   name for the current cycle; optional base **labels**; optionally the Projects v2 number.
   Apply via `wf github on --repo … --assignee … --reviewers … --milestone … --labels … --project N`.
7. **Workspace (satellite projects)** — "Is this product split across several repos
   (e.g. users-web, users-mobile alongside this one)?" → *No — single project (Recommended for
   one repo)* · *Yes — connect existing projects* · *Yes — create new ones*. If yes, collect the
   list as text — `path-or-name : type [: owner/repo [: stack]]`, one per line or
   comma-separated (e.g. `../users-web : web : org/users-web : react,next`). **Ask each
   satellite's stack** — plans for features routed there design against it, not against the
   hub's stack. Existing = a path (`../users-web`); new = just a name (created as a sibling
   directory). The hub keeps the whole pipeline and SDD docs; satellites get only code + a
   minimal `workflow.json` pointing here.

### Round 3 — conventions detail (ONLY if "Customize…" was picked)

One call, 3 cards, each option carrying its concrete example:

- **Naming** — *English* `findUsers` · *Hybrid* `findUsuarios` · *Project language*
  `buscarUsuarios`.
- **Comment scope** — *Minimal*: one short comment above each public function/method, nothing
  inline · *Detailed*: also inline comments on non-obvious blocks inside functions/methods.
  Either way comments are written in the output language, and narrating the obvious is a
  violation in both.
- **Docs** — *Stack reference docs + OpenAPI if API (Recommended)* · *reference docs only* ·
  *none*. Derive the reference-doc name from the stack; never ask generically.

Never re-ask in Round 3 what a preset already answered elsewhere.

## 3. Review — before writing anything

Show the full picture and let the human amend it. **Nothing has been written yet** — this is the
moment to change predefinitions cheaply.

1. Print a compact summary with the **resolved** values, not the preset names — this is where
   the human sees what "Ecosystem default" actually decided for them:

   ```
   project     users-api · api · java, jakarta, jpa
   language    pt_BR (artifacts + session replies)
   naming      english        → findUsers
   comments    minimal · javadoc · openapi
   harness     on · mvn -B verify (test=mvn -B test, build=mvn -B package)
   github      on · org/users-api · @rafaelsantos · board #5
   workspace   users-web (web, org/users-web) · users-mobile (mobile)
   ```

2. One card — **"Apply this setup?"**:
   - *Apply (Recommended)* — proceed to § 4.
   - *Change something…* — follow up with one card listing the sections (type/stack · language ·
     naming & comments · harness · github · workspace); re-ask ONLY the chosen section with the
     same cards as before, then show the updated summary and this question again.

Loop until they pick Apply. Do not run any command, create any directory, or write any file
while the loop is open — the review is read-only by definition.

## 4. Apply — one call

Translate the answers into a single `./bin/wf init` invocation:

```bash
./bin/wf init --name <name> --type <type> --stack "<a,b,c>" --lang <code> \
  [--code-naming english|hybrid|project-language] \
  [--comments minimal|detailed] [--doc-format <javadoc|jsdoc|...|none>] [--api-docs <openapi|none>] \
  --harness <on|off> [--verify "<cmd>"] \
  --github <on|off> [--repo <owner/name>] [--assignee <user>] [--project <N>]
```

The Round-1 presets translate mechanically — never re-interpret them:

| Preset | Flags |
|---|---|
| Ecosystem default | `--code-naming english --comments minimal --doc-format <stack's> --api-docs openapi` (api type) or `--api-docs none` otherwise |
| Domain terms in my language | same, but `--code-naming hybrid` |
| Customize… | whatever Round 3 answered |

**Engine FIRST — before `init`.** `init` materializes the scaffold of whatever engine is
active at that moment; setting the engine afterwards would leave the default engine's folders
in a project that chose another methodology.

- A non-default engine was chosen → `./bin/wf engine set <name>` NOW (it materializes that
  engine's scaffold). If its install check fails, relay the install hint and ask before
  running any installer.
- *From a GitHub URL…* → follow the **`/wf-engine add <url>`** flow first: confirmed clone
  into `engines/`, learn the manifest from what was downloaded, then `engine set`. Every
  download and installer run is confirmed with the human — never a silent side effect of setup.
- Default engine → nothing to do; `init` handles it.

Then `init`: writes project identity, flips the harness and GitHub blocks, removes the shipped
`feat-001-example`, marks `bootstrap` done, and materializes the (now correct) active engine's
scaffold — the SDD folders which `wf create` deliberately does not ship. Then, if the harness is on and the project uses
GitHub Actions, wire the CI signals and generate the workflow:

```bash
./bin/wf harness signal test  "<test command>"
./bin/wf harness signal build "<build command>"
./bin/wf harness ci
```

If the human declared satellite projects, wire each one (this creates the directory when it
doesn't exist, and connects it when it does — existing files are never overwritten):

```bash
./bin/wf workspace add ../users-web    --type web    --repo owner/users-web --stack "react,next"
./bin/wf workspace add ../users-mobile --type mobile --stack "flutter"
./bin/wf workspace   # show the table and confirm every link is ✓
```

Creating directories inside the human's filesystem is a visible action — recap the list (paths,
types, repos) and get a yes **before** running the adds. From then on, features are routed with
`wf feature add … --project <name>`, and their issues/PRs/commits target that satellite's repo.

## 5. Agents — last question before handing off

One card: **"Review which agent runs each step?"** → *No — keep the defaults (Recommended)* /
*Yes — review them*. The defaults are deliberate (haiku for mechanical work, opus where
ambiguity is expensive, sonnet elsewhere), so "No" is the normal answer.

If yes, follow the `/wf-agents` flow: show `./bin/wf agents`, adjust via cards, apply with
`wf agent <id> --model … --effort …`. Either way, mention in one line that the **main session
model** is not part of workflow.json — it is the Claude Code session setting (`/model`).

In the same card round, ask: **"Keep asking this when new features are added?"** →
*Yes — ask per feature (Recommended)* / *No — keep defaults silently*. On "No", run
`./bin/wf init --agent-review off` — from then on `/wf-feature` skips the agent question
entirely (adjustable anytime via `/wf-agents`; re-enable with `--agent-review on`).

## 6. Confirm and hand off

Run `./bin/wf validate` (must be `OK`) and show `./bin/wf status`. Report what was configured in
two or three lines, then point at the next step: `/wf-next` runs `definitions` (or `constitution`
if there is no raw material to intake).

**Do not** create the GitHub repo, push, or open anything here — setup only writes local config.
Any outward GitHub action happens later, per `instructions/github-sync.md`, with confirmation.
