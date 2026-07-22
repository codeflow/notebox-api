# Harness — the pluggable verification layer

SDD gives you **intent you can trace**. The harness gives you **a signal you can trust**. They are
independent: you can run the pipeline with the harness off, and you can bolt the harness onto a
project that already has specs.

## Plugging it in and out

```bash
./bin/wf harness                                      # state + which gates it backs
./bin/wf harness on --profile api --verify "npm run verify"
./bin/wf harness verify "make verify"                 # change the command
./bin/wf harness off                                  # gates become advisory
```

That writes the block below into `workflow.json`. You can edit it by hand, but the command
checks the profile exists and tells you which gates just changed meaning:

```jsonc
"harness": {
  "enabled": true,
  "profile": "harness/profiles/api.md",
  "verify":  "make verify",
  "ci":      ".github/workflows/ci.yml",
  "signals": ["lint", "typecheck", "test", "build"]
}
```

## What flipping `enabled` actually changes

| | `enabled: true` | `enabled: false` |
|---|---|---|
| `verify_green` gates | Enforced — `harness.verify` must exit 0 | **Advisory** — the agent's own claim |
| `wf done` on a gated step | Blocked on a red signal | Allowed |
| Task completion | Requires a green run | Requires a human's judgement |
| Honest reporting | "verify green" | "no automated verification ran" |

Turning the harness off is a legitimate mode — specification-only work, research, a design
sprint. What is not legitimate is leaving it off while writing "verified" in a report. If there is
no signal, the pipeline must say there is no signal.

## Wiring it up

1. **Pick a profile.** `profiles/{api,web,mobile,cron}.md` describe how to run, verify and deploy
   each project type. Copy the one you need into your project and edit it — it is a starting
   point, not a spec.
2. **Define one `verify` entry point.** A single command, called identically by you, the agents
   and CI. `make verify`, `npm run verify`, `./gradlew check` — the name does not matter, the
   singularity does. Three different commands means three different definitions of "green".
3. **Wire the signals and generate CI.** Each entry in `harness.signals` is a stage in the
   GitHub Actions pipeline. Give each one a command, then generate the workflow:

   ```bash
   ./bin/wf harness signal test  "npm test"
   ./bin/wf harness signal build "npm run build"
   ./bin/wf harness ci                 # writes .github/workflows/ci.yml
   ```

   `wf harness ci` derives the workflow from the signals, so `build` and `test` appear as
   separate steps in the Actions UI. It picks a toolchain (`setup-node`/`setup-java`/…) from
   `project.stack`. A signal with no command generates a step that **fails loudly** in CI — an
   unconfigured `build` must never pass green. Re-run `wf harness ci` whenever the signals change.
4. **Run `./bin/wf validate`**, then commit and push the workflow (with confirmation) to activate
   it on GitHub.

## The design rule

`verify` must be **fast enough to run per task, and strict enough that green means shippable.**

Those pull against each other, and the resolution is not a compromise — it is layering. Keep the
per-task command fast (lint, typecheck, unit). Push the slow, expensive checks (integration,
e2e, load, security scan) into CI, and let the `audit` step read CI's result.

An agent that waits eight minutes for feedback stops running it and starts guessing. A `verify`
that passes on broken code teaches the agent that green is meaningless. Both failures end in the
same place: the loop is open and nobody notices.
