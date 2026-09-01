# fixes/

Briefs for the `/wf-fix` lane — the fast route for small corrections (visual adjustments, small
bugs) that do not repay a full spec → plan → tasks lifecycle.

One directory per brief:

```
fixes/YYYY-MM-DD-<topic>/
  brief.md     what the human asked for, verbatim, appended item by item during `/wf-fix collect`
  record.md    what was actually done, with the evidence — written by `/wf-fix start`
  assets/      files the human pointed at (pasted images cannot be saved as bytes; they are
               described in brief.md instead, because a description survives a context compaction
               and an image in the conversation does not)
```

**Why on disk rather than in the conversation:** a long session gets compacted. A brief kept only
in context can lose half its items between `collect` and `start` without either side noticing.

**What this lane does not skip:** `verify` green, the audit, the PR, and the human's approval on
GitHub. What it skips is documentation sized for a feature — and the pipeline shows that
explicitly, because `spec`/`plan`/`tasks` are marked `skipped` with the brief's path as the
reason, never written after the fact.
