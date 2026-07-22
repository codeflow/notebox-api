---
description: Check the pipeline's integrity and its agreement with what is on disk
---

Run `./bin/wf validate` and report the result.

It checks: unique ids, the `instructions/<name>-<info>.md` naming pattern, that every referenced
instruction file exists, valid models and efforts, no background step holding a gate, every
`requires` resolving, and no cycles. It also warns about instruction files nothing references.

Then check what the tool cannot — agreement between the pipeline and reality:

1. **Steps marked `done` with nothing to show.** For each, confirm its `produces` artifacts exist
   and are not empty stubs. A step closed with `--force` is exactly what this catches.
2. **Artifacts with no step.** Directories under `features/` absent from `workflow.json` mean
   somebody worked outside the pipeline.
3. **Catalog drift.** A US marked `delivered` in `catalogs/epics.md` whose feature step is still
   pending, or the reverse.
4. **Stalled steps.** `in_progress` with an old `started_at` and no artifact.

Report failures first, then warnings, then what is genuinely fine. If everything passes, say so
plainly — a clean validate reported as clean is a real result.
