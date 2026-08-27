# Audit — Login layout A: the two-pane brand card

**ID:** features/017-login-layout · **US:** US-7.1 · **Round:** 1
**Date:** 2026-08-27 · **Model:** opus/xhigh
**Scope:** `develop..feature/login-layout` — 7 commits, **12 files**, +735 / −81
**Verify at audit time:** `npm run verify` → 66 files, **487 tests**, 0 failures, build compiled
**Live pass:** recorded in `tasks.md` T-07 — geometry measured at 1280×800

## Verdict — **FAIL** · reopen `implement`

One blocker. **F-01: spec scenario 9 has no test that could fail.** The scenario is a *screen*
assertion; `tasks.md` mapped it to two *catalog* tests, which cannot see the screen at all. A probe
confirms the gap is real and not theoretical: a hardcoded English sentence added to the brand pane
ships with **all 487 tests green** and renders untranslated in Portuguese.

This is a small, cheap fix — but it is an unmet acceptance criterion in a feature whose entire
purpose is conformance, on a compliance item (**C-09**) the spec marked `applies`. Sending it back is
the honest call; patching it from inside the audit is not allowed and would hide that the tasks
artifact claimed coverage it never had.

Two non-blocking findings (F-02, F-03) are artifact drift, not code defects.

---

## 1 · Traceability — scenario → test → code

All 13 spec scenarios, followed in the real files.

| # | Scenario | Test | Status |
|---|---|---|---|
| 1 | two panes, no panel box | `is a two-pane card, brand pane first…` | ✅ |
| 2 | brand pane content | `carries the lockup, tagline, three bullets and the release line…` + `switches the WHOLE screen…` (proves catalog resolution) | ✅ |
| 3 | heading and hint | `opens the credentials pane with the heading and the hint…` | ✅ |
| 4 | combo fields, decorative icons | `wraps each field…`, `keeps the adornments decorative…`, `leaves the tab order…`, `stacks the label above its field…` | ✅ |
| 5 | action block | `places one full-width submit…`, `puts Remember me and the language control…`, `puts the two link buttons…` | ✅ |
| 6 | empty form sends no request | `blocks submission and sends no request…` (pre-existing) + `renders each field error inside its own row` | ✅ |
| 7 | malformed email sends no request | `flags a malformed email…` (pre-existing) | ✅ |
| 8 | expired-session warning placement | `puts an expired-session warning inside the credentials pane…` | ✅ |
| 9 | **every string on the screen in Portuguese** | *claimed:* `translationNotCopy`, `keysetCoverage` | ❌ **F-01** |
| 10 | language control switches the screen | `switches the WHOLE screen…` | ✅ |
| 11 | choice not persisted | `I-4 · the override is not persisted…` | ✅ |
| 12 | stored preference wins after sign-in | `I-1 · a stored preference outranks the override…` | ✅ |
| 13 | rendered geometry | live browser pass, table recorded in `tasks.md` T-07 | ✅ |

Also verified: **deviation D-1** carries its own test (`carries NO tenant identity in the brand
pane`), which the spec did not require but which makes a recorded deviation enforceable.

## 2 · Scenario honesty — four adversarial probes

Every probe reverted; the tree is clean.

| Probe | Mutation | Result |
|---|---|---|
| Precedence | `override ?? preferred` instead of `preferred ?? override` | ✅ kills **I-1 only** — targeted, not incidental |
| Allow-list | drop the two autonym entries | ✅ kills the not-copy test, naming exactly those keys |
| Adornment | give `.af-iconButton` a `tabIndex` | ✅ kills both the decorative test and the tab-order test |
| Label wiring | strip `htmlFor` from both labels | ✅ kills the label-association test |
| Catalog resolution | hardcode the pt tagline in the component | ✅ kills the language-switch test |
| **Stray English literal** | **add `Powered by Notebox — all rights reserved` to the brand pane** | ❌ **487/487 still green — F-01** |

The tab-order test deserves a note: it passed *before* the adornments existed, when it was vacuous.
It is meaningful only because the mutation confirms it now discriminates. That is the difference
between a test that is present and a test that protects something.

## 3 · Constitution and architecture

| Item | Check | Result |
|---|---|---|
| **AD-06** — UI lives in the satellite | every changed file is under `notebox-web` | ✅ |
| **AD-05** — system strings come from the catalog | no user-facing literal in `LoginForm`'s JSX outside `t()` (grepped) | ✅ |
| **BR-08 / C-09** — no raw key, no blank | `MessageKey` is `keyof typeof en`, so a missing key is a compile error; `t()` falls back to English | ✅ |
| Hydration contract | override written only from an event handler; **I-5** pins the server render; live console clean | ✅ |

## 4 · Compliance pre-flight — evidence where the spec said it would be

| Item | Claimed evidence | Found |
|---|---|---|
| **C-01** tenant isolation | no new fetch path | ✅ diff adds no `fetch`/XHR outside tests |
| **C-02** public by design | D-1 — no tenant rendered | ✅ test present |
| **C-04** data minimization | brand pane texts enumerated exhaustively | ✅ D-1 test asserts absence of `tenantId` and `displayName` |
| **C-06** encryption in transit | standing, no new transport | ✅ |
| **C-08** no `dangerouslySetInnerHTML` | diff introduces none | ✅ grep: 0 occurrences |
| **C-09** localization completeness | pt scenario + catalog coverage | ⚠️ **partial — F-01**: catalog totality holds; the *screen* sweep does not exist |

## 5 · Scope — diff against the plan's blast radius

11 of 12 changed files were named in the plan. `app/(auth)/login/page.tsx` is untouched, exactly as
the plan predicted. Two files were not in the plan's table — see **F-02**. Nothing in the diff adds
capability beyond the approved scope: the placeholders (*Remember me*, *Forgot password?*, *Add
account*) moved and stayed inert, as the spec's *Out* section requires.

## 6 · Open Questions

| OQ | Status | Who |
|---|---|---|
| **OQ-28** | answered before `plan` | **rafaelsantos**, 2026-08-26 — recorded in the catalog and folded into spec §Scope and three scenarios |
| **OQ-29** | opened during `implement`, still open | flaky pre-existing test, correctly kept out of this diff |

No OQ was closed by the implementer's own assumption. ✅

---

## Findings

### F-01 · **BLOCKER** — scenario 9 is claimed by tests that cannot observe the screen

**Where:** `features/017-login-layout/tasks.md` maps scenario 9 to `translationNotCopy.test.ts` and
`keysetCoverage.test.ts`. Both operate on the catalog objects; neither renders anything.

**Failure scenario (reproduced):** add `<div>Powered by Notebox — all rights reserved</div>` to the
brand pane. `npm run verify` → **66 files, 487 tests, all green**, build compiled. In Portuguese the
sign-in screen then shows an English sentence, and nothing anywhere reports it.

**Why the catalog tests cannot cover it:** they can only see strings that are *in* the catalog. The
failure mode C-09 is exposed to on this screen is a string that never entered the catalog at all.

**Fix:** one screen-level test that renders the login in pt and asserts every visible text node
resolves to a pt catalog value (allow-listing the autonyms and the interpolated release). Reopen
`implement` and add it as **T-08**; correct scenario 9's row in `tasks.md` at the same time.

### F-02 · low — the plan's blast radius omitted `lib/release.ts`

**Where:** `plan.md` §Blast radius lists `next.config.mjs` and `.env.example` for A5 but names no
module; `lib/release.ts` and `lib/release.test.ts` appear only in T-03.

**This is a plan that was incomplete, not scope creep.** The module serves A5 exactly and adds no
capability; it exists *because* A5's own reasoning ("the untestable seam should be one line of build
config") implies a testable module, which the plan then failed to name. `tasks.md` corrected it and
the human approved that. **Fix:** amend the plan's table so the artifact matches the code.

### F-03 · low — `data-model.md` §3 names a widget the code does not build

**Where:** the DOM contract specifies `div.nb-brandLockup → NoteboxLogo + span.nb-brandWordmark`.
The code renders `div.nb-brandLockup → NoteboxLogo`, whose own `.nb-logo-word` carries `NOTEBOX`.
`grep nb-brandWordmark` → 0 occurrences.

**The code is the better choice** — reusing the shared lockup rather than duplicating the wordmark
beside it. **Fix:** correct the contract, not the code. Left as a finding rather than a silent edit
because a contract that quietly follows the code is not a contract.

---

## What is genuinely good here

The live pass was not a formality: it caught **two real conformance misses** that all 487 unit tests
passed straight through — right-aligned labels, and a card silently shrinking to 590px because it is
a flex *item*. Both are invisible to jsdom by construction, and the plan said so in advance rather
than discovering it afterwards. The 13 pre-existing `LoginForm` tests passed **unmodified** through
all six code tasks, which is the strongest available evidence that this was a relayout and not a
behaviour change.

F-01 does not contradict that. It says the one scenario nobody built a test for is the one nobody
noticed was untested — which is exactly the class of gap this step exists to find.

---

# Round 2 — 2026-08-27

**Verify:** `npm run verify` → 66 files, **489 tests**, 0 failures, build compiled.
**Standing approval:** the human authorised the remaining work without per-step gates in chat on
2026-08-27 ("*implemente todas as telas, não precisa me perguntar nada*"). Recorded here so the gate
trail shows where the approval came from.

## Verdict — **PASS**

| Finding | Status |
|---|---|
| **F-01** blocker — scenario 9 untested | **closed.** T-08 walks every visible text node of the rendered card and requires each to be a pt catalog value. **Re-ran the original probe:** the hardcoded English sentence that passed 487/487 now fails, naming the stray string. |
| **F-02** plan blast radius omitted `lib/release.ts` | **closed.** `plan.md`'s table now lists it, attributed to this finding. |
| **F-03** `data-model.md` named a widget the code does not build | **closed.** The contract now describes what the code actually does — reuse the shared lockup — rather than the duplicated wordmark it originally specified. The code was not changed; it was already the better choice. |

T-08's exceptions are two, both named with their reason in the test: the `*` required marker
(punctuation) and the `NOTEBOX` wordmark (owned by the shared lockup, same category as
`branding.appName` on the existing allow-list). `aria-hidden` subtrees are skipped, which is correct:
the ✉/🔒 adornments carry nothing to translate.

Traceability is now complete — all 13 scenarios have a test that can fail, and scenario 9's row in
`tasks.md` was corrected from T-02 to T-08 so the artifact no longer claims coverage it never had.
