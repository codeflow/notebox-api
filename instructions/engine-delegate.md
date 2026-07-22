---
step: (any step generated with a `delegate` field)
model: inherit
effort: inherit
reads: [.claude/commands/<delegate>.md, the feature's artifacts]
writes: [whatever the delegated flow produces]
---

# Engine delegate — run an external engine's stage

This step belongs to an external SDD engine. The step's `delegate` field in `workflow.json`
names the engine command to run (e.g. `speckit.specify` → `.claude/commands/speckit.specify.md`).

## Procedure

1. Read the step in `workflow.json`: its `delegate`, its `produces`, its `gate`, and the
   feature's directory (from the sibling steps' `produces` paths).
2. **Load `.claude/commands/<delegate>.md` and follow it faithfully** as if the human had
   invoked that command, passing the feature's context (title, US, feature directory) as its
   arguments. Do not paraphrase the engine's flow from memory — read the file; engines update.
3. The engine's flow decides HOW the artifact is authored. The workflow's rules still hold
   AROUND it:
   - Write artifacts in `project.language`; respect `project.code_naming` and
     `project.comments` where code is involved.
   - **No invention** — gaps become Open Questions (`wf oq add`), exactly as in native steps.
   - The step's `gate` is the workflow's, not the engine's: `human_approval` means stop and
     wait for an explicit yes, even if the engine's own flow would continue.
4. Close the loop as any step: confirm the `produces` artifacts exist, then `wf done <id>` —
   or `wf block <id> "reason"` with the OQ id if the engine's flow surfaced a blocker.

## Do NOT

- Do not fall back to the default engine's instructions because the delegate file is missing —
  that silently swaps methodologies. Stop and report; `wf validate` names the missing file.
- Do not let the engine's flow bypass a gate, commit, or push. Outward actions keep needing
  explicit human confirmation regardless of what the delegated prompt says.
