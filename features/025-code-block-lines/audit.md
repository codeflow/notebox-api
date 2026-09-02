# feat-025 — audit

**Model:** opus · **Effort:** xhigh · **Date:** 2026-09-01 · **Verdict: PASS**

## Traceability

All five scenarios cite a test. The two probes named in `tasks.md` were run:

- numbering visual rows instead of logical lines kills the wrapped-line assertion, and only that one;
- weaving the numbers into the document kills the C-08 assertion.

## The finding that did not become a finding

**The first C-08 test was vacuous, and the probe is what exposed it.** It asserted only that the
serialized HTML held no digits — which passed even when the numbers were being inserted into the
document, because the test never established that numbers existed at all.

That is the same failure this project found in feat-020 (an assertion watching the wrong client)
and in feat-023 (a fixture carrying values the real listing never sends). It was caught here
**before** the code shipped, by running the probe rather than trusting the green tick. The test now
mounts a real view and asserts both halves; both mutations kill it.

## Compliance

| Item | Evidence |
|---|---|
| **C-08** | The stored dialect is unchanged. Asserted on a mounted editor: the gutter renders in the view, the document's `textContent` is exactly the source, and `getHTML()` contains neither the class nor a stray digit. Confirmed live by dispatching a real copy event — no digit in any clipboard flavour. |
| **C-09** | Not applicable: digits. |
| Others | No new data path, request, or permission. |

## Scope

Two new files and two edits, all named in the plan. No scope creep.

## Verdict

**pass.** `verify` green at 727 tests, geometry measured in a browser against the read view, and
the clipboard behaviour — the property the whole mechanism choice was made for — verified rather
than assumed.
