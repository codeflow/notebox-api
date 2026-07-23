---
description: Promote develop → main — PR with the same GitHub-approval protocol as feature reviews
argument-hint: "<feature-id | slug>   (empty = a card asks which pending feature to promote)"
---

Promote the integration branch to the trunk. Input: `$ARGUMENTS`.

## 1. Resolve the feature

- `$ARGUMENTS` given → match it against the features `./bin/wf github pending` lists.
- Empty → run `./bin/wf github pending`; **ask with a card** (AskUserQuestion) which pending
  feature drives this promotion — one option per pending feature (id + PR + title), plus
  "Cancel". Nothing pending → say so and stop.

**Say the honest part out loud:** a `develop → main` PR carries EVERYTHING currently on develop,
not just the chosen feature. List what rides along (`git log --oneline origin/main..origin/develop`
or the pending list) before opening. If the human doesn't want something included, stop — that is
a cherry-pick conversation, not this flow.

## 2. Open the promotion PR

```bash
./bin/wf github pr <feature-id> --base <default_branch> --head <integration_branch>
```

Title it as a promotion (e.g. `promote: <feature title> (develop → main)`), body listing every
feature included **and a `Closes #<issue>` line for each one's linked issue** — merging into the
trunk (the repo's default branch) is the only merge GitHub auto-closes issues on, so this PR is
where every included feature's issue finally closes. Show the `gh pr create` command, get the
yes, run it. CI (build + test) runs on the PR — report `gh pr checks`.

## 3. The same approval protocol as feature reviews

Check `gh pr view <N> --json state,reviews,reactionGroups,mergedAt`:

- **Merged by the human** → that is approval; go to 4.
- **Approving review + 👍 reaction** → merge (`gh pr merge <N> --merge`, confirmed), go to 4.
- **Anything less** → question it: "Você não deu o approve no GitHub nessa PR de promoção
  (falta: 👍 / review)." STOP and wait. If the human confirms in chat explicitly, mirror it on
  GitHub (approve; if own-PR refusal → comment ✅ + 👍 reaction via `gh api`) and merge.

## 4. Close out

1. **Local sync.** The merge advanced `main` on GitHub only; the local clone is behind. Stay on
   `develop` and update the local trunk ref without switching (one confirmed batch):
   ```bash
   git fetch origin main:main && git pull origin develop
   ```
   (If the current branch IS main for some reason, a plain `git pull` there instead — but the
   working position after a promotion is develop, ready for the next feature.)
2. For EVERY feature that was on develop (they all rode along):
   `./bin/wf github link <feat> --merged main`.
3. **Confirm the issues auto-closed** (`gh issue view <N>` → closed). One missing a `Closes`
   line in the body? Close it now, with a comment linking the promotion PR.
4. Update `catalogs/epics.md` notes (delivered → on main) and move board cards.
5. Refresh `ROADMAP.md` (features now on main) and the `HANDOFF.md` state line — promotion is a
   milestone worth two lines in each.
6. `./bin/wf github pending` — should now say nothing is pending; report the final state.

## Guardrails

- The trunk is never pushed directly — promotion is always PR + protocol.
- Every `gh` mutation (create, review, comment, merge) shows its exact command and waits for
  the yes it needs. Reading (`gh pr view`, `checks`) is free.
