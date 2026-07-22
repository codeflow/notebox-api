---
step: feat-*.review
model: sonnet
effort: medium
reads: [features/NNN-slug/**, the PR state via gh, workflow.json → github]
writes: [GitHub PR via gh (confirmed), workflow.json github links, catalogs/epics.md]
gate: human_approval
---

# Review — PR to the integration branch, and the human's GitHub approval

**Goal.** Open the PR `feature/<slug>` → `github.integration_branch` (default `develop`), put it
in front of the human, and merge ONLY under the approval protocol below. The human's approval
lives on GitHub — 👍 reaction + approving review — not in the agent's memory.

## 1. Open the PR (first entry into this step)

`./bin/wf github pr <feature-id>` prints TITLE/REPO/BASE/HEAD/BODY — BASE is the integration
branch. Show the `gh pr create` command, get the yes, run it, `wf github link --pr N`.
The `pull_request` trigger runs build + test on the PR; report the checks status
(`gh pr checks N`). Then hand over: PR link, three-line summary (what changed, what's risky,
what the audit flagged), and **stop — the human reviews on GitHub.**

## 2. The approval protocol (every re-entry, e.g. via /wf-next)

Check the real state first — never assume:

```bash
gh pr view <N> --json state,reviews,reactionGroups,mergedAt
```

| State on GitHub | What you do |
|---|---|
| **Merged** | The human merging IS approval. Record and close: step 3. |
| **Approving review AND 👍 reaction** | Approved. Merge (`gh pr merge <N> --squash`, confirmed), then step 3. |
| **Anything less** (no review, or review without 👍, or 👍 without review) | **Question it before doing anything else:** "Você não deu o approve no GitHub (falta: 👍 / review). Aprova lá, ou me confirma aqui que eu registro e sigo." Then STOP and wait. |

**Chat-approval fallback.** If the human answers in chat that it is approved (explicitly), make
GitHub reflect it, then proceed:

1. Try `gh pr review <N> --approve --body "Aprovado pelo humano via sessão"`. GitHub refuses
   approving your own PR — if refused, record instead:
   `gh pr comment <N> --body "✅ Aprovado pelo humano via sessão (chat)"` and add the 👍:
   `gh api repos/{owner}/{repo}/issues/<N>/reactions -f content='+1'`.
2. Merge to the integration branch: `gh pr merge <N> --squash`.

Chat approval must be explicit ("aprovado", "pode mergear"). A question or "olhei" is not a yes.

## 3. Close out

1. `./bin/wf github link <feature-id> --merged develop` — the feature is now on the integration
   branch, pending promotion.
2. Update `catalogs/epics.md`: US → `delivered` (note: on develop). Move the board card.
3. `wf done` this step, then **run `./bin/wf github pending` and REPORT it in the chat** — every
   implementation ends by telling the human which features sit on develop awaiting promotion
   (`/wf-promote <feature>`). This report is not optional.

## Without GitHub

Review happens in-session (full diff + audit verdict + how to run locally); the gate is the same
explicit yes; `--merged` tracking does not apply.

## Do NOT

- Do not merge on the audit's pass, on silence, or on "olhei o PR". The protocol above is the
  only path to merge.
- Do not target the trunk — feature PRs go to the integration branch; the trunk only receives
  promotions (`/wf-promote`).
