---
step: feat-*.publish
model: sonnet
effort: medium
reads: [the working tree, workflow.json → github]
writes: [git branch feature/<slug>, GitHub via push (confirmed)]
gate: verify_green
---

# Publish — the feature branch on GitHub, with CI green

**Goal.** Put the audited work on GitHub as `feature/<slug>` and get the Actions pipeline
(build + test) green on that branch. This step ends with remote evidence, not local claims.

## Procedure

1. Resolve the branch name: `feature/<slug>` (the feature's slug; for routed features, work in
   the satellite and use its repo). Record it: `wf github link <feat> --branch feature/<slug>`.
2. **Propose the git batch** — exact commands, one confirmation:
   - `git switch -c feature/<slug>` off the integration branch (`github.integration_branch`,
     default `develop`; create `develop` from the trunk if it does not exist yet — say so).
   - Commits per task (`wf github commit <task-id>` generates each message) if the work is
     still uncommitted.
   - `git push -u origin feature/<slug>`.
3. The push triggers **GitHub Actions** (the generated workflow listens on `feature/**`).
   Watch it: `gh run watch` or `gh run list --branch feature/<slug> --limit 1`.
4. **Gate `verify_green` here means the REMOTE run**: the branch's Actions run (build + test)
   must conclude green. Red → read the log (`gh run view --log-failed`), fix, push again.
   Do not hand the feature to `review` with a red pipeline.

## Done when

- `feature/<slug>` exists on origin, linked in `workflow.json`.
- The latest Actions run on the branch is **green** — state the run URL in your report.

## First publish of an empty repo — the bootstrap exception

When the remote has no branches yet, there is nothing to PR against — bootstrapping is allowed,
ONCE, under these rules:

1. The **baseline** commit to the trunk contains only what precedes the feature: scaffolding,
   SDD documents, project skeleton. **The feature's implementation files never ride in the
   baseline** — they go to `feature/<slug>`, or the review PR is empty and the flow is theater.
2. Sequence (one confirmed batch): baseline → trunk · create integration branch from it ·
   feature branch from the integration branch · push all three.
3. Say explicitly that this is the bootstrap. From this moment on, the trunk only moves via
   promotion PRs (`/wf-promote`) — never a direct push again.

## An unconfigured CI signal is NOT yours to fake

If a signal (e.g. `lint`) has no command, the generated CI step fails loudly **by design**.
Do not replace it with a passing placeholder — a green check that checked nothing is the exact
lie this pipeline exists to prevent, and a polite label does not fix it. The honest options,
in order:

1. Wire the real command: `wf harness signal lint "<cmd>"`.
2. Remove the signal from `harness.signals` and regenerate (`wf harness ci`) — visible absence
   over fake presence — and open an OQ/follow-up to wire it later.

Choosing between them is the human's call, not yours: present both and wait.

## Do NOT

- Do not push to `develop` or the trunk — only to `feature/<slug>` (bootstrap excepted, above).
- Do not proceed on a red or still-running pipeline; waiting is part of this step.
- Do not neutralize a failing CI step to open the gate — see above.
- Without GitHub enabled this step is `skip` with a note — there is nowhere to publish.
