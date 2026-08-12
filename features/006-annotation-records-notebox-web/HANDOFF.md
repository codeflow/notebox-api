# HANDOFF — feat-006 annotation record editor, web (resume here)

**Updated:** 2026-08-12 · **State:** **DONE — merged into the satellite's `develop`.** Audit Round 2
verdict **pass** (277 tests, 38 files), CI green, PR
[#7](https://github.com/codeflow/notebox-web/pull/7) squash-merged as `1f6e34a` (2026-08-12).
US-2.1 is now **delivered on develop** (both halves); promotion to `main` pending.

## How to resume

```bash
cd notebox-api && claude      # the hook injects the pipeline automatically
/wf-status                     # see the state
/wf-next                       # continues at the next eligible step
```

`wf next` now points at **release** (handoff, roadmap and memory) — the last pipeline stage.
`wf github pending` lists **6 features on `develop` awaiting `/wf-promote`**.

## What shipped (13 commits, `e65af80` → `d9ec393`, in `notebox-web`)

The record experience: WYSIWYG rich-text Free-text values locked to a sanitizer **dialect contract**
(`lib/annotationRecords/sanitize.ts` — the editor provably cannot emit outside it, hostile markup
renders inert); the **secret state machine** (masked by default, ADMIN-only reveal that re-masks on
navigation, explicit clear with undo; illegal PUT shapes unrepresentable — untouched → `{text:null}`
echo, never invented cleartext); type-derived controls for all seven field types; suffix-tolerant
violation routing (works with the real `create.input.*` Jakarta paths AND bare paths); the
irreversible delete dialog; three routes behind the existing guard; en+pt complete.

Audit Round 1 failed on a real bug MSW had hidden (violation-path contract mismatch) + two test
gaps; T-11…T-13 closed all three, Round 2 passed with one non-blocking observation (a hypothetical
`values[i].name` path would hit the name-check first — unreachable in the current wire contract).

## THE open decision — blocks `/wf-promote` of US-2.1

**API-side C-08 sanitization (OQ-19 dependency).** The human's OQ-19 decision made Free-text values
rich HTML; C-08 binds whichever feature *stores or returns* markup to sanitize input/output — that is
the **merged API (feat-005)**, which stores and returns it verbatim. The client sanitizes on render
(defence in depth), but a direct API call bypasses it entirely. Options recorded in feat-006 spec v2
§Dependency: (1) follow-up API feature with allow-list sanitization + hostile fixtures, (2) reopen
feat-005 --cascade, (3) accept as recorded risk. **Human's call; unscoped as of this writing.**

## Also pending

- The `lint` CI signal in the **hub** is still a placeholder (`echo`) — wire Checkstyle/Spotless or
  drop the signal before promotion (flagged since feat-005).
- Type PUT returns `fields[].id`/`options[].id` as `null` for newly created children (feat-003,
  pre-existing) — the web client works around it by re-fetching.
- The satellite's local `feature/annotation-records` branch and its remote may still exist after the
  merge — delete when convenient.
