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

## Do NOT

- Do not push to `develop` or the trunk — only to `feature/<slug>`.
- Do not proceed on a red or still-running pipeline; waiting is part of this step.
- Without GitHub enabled this step is `skip` with a note — there is nowhere to publish.
