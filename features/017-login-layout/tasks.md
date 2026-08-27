# Tasks — Login layout A: the two-pane brand card

**ID:** features/017-login-layout · **US:** US-7.1 · **Date:** 2026-08-26
**Source:** spec.md + plan.md + data-model.md (approved 2026-08-26)
**Status:** Approved (human approval 2026-08-26)
**Project:** `notebox-web` (react/next) — routed satellite

> **Seven tasks.** Three independent openers (T-01…T-03) then a strictly sequential chain
> (T-04 → T-05 → T-06 → T-07), because T-04/T-05/T-06 all rewrite the same two files —
> `LoginForm.tsx` and `adf-fusion.overrides.css` — so none of them may run in a parallel worktree.
>
> **Ordered by risk, not just dependency.** T-01 goes first because it is the only change that
> reaches **every screen in the app** and the only one that could send the plan back: if the
> precedence order does not hold, or the override forces a hydration mismatch, the alternative
> (a nested provider) has to be reconsidered — and that is far cheaper to discover before any
> markup is rewritten than after.
>
> **The standing rule for the whole chain:** the 10 existing `LoginForm` tests are the regression
> net and **must not be edited**. They query by role and label and assert no structure, so a pure
> relayout leaves them green. If one goes red, behaviour changed — fix the component, never the
> test.

- [x] **T-01 · Locale override in the i18n provider, with the precedence pinned** ✔ 2026-08-26, `npm run verify` green (65 files, **469 tests**, 464 → 469; build compiled). The precedence was **mutation-checked**: flipping the order to `override ?? preferred` kills I-1 and nothing else, so the test is discriminating and targeted rather than merely present.
      - files: `lib/i18n/I18nProvider.tsx` (+ `override` state, `setLocale` on the context, resolution order), `lib/i18n/I18nProvider.test.tsx` (extend)
      - covers: OQ-28 (b), data-model **I-1…I-5** · scenarios: "The locale choice is not persisted", "The member's stored preference wins once they are signed in" (and the mechanism behind "The locale control switches the sign-in screen's own language")
      - depends: — · parallel: no *(shared surface; T-06 consumes it)*
      - verify: `npx vitest run lib/i18n` — one test per rank boundary. **I-1 is the important one**: with a `locale` prop set, `setLocale('pt')` must change nothing. That single assertion is the whole supersession rule, and it must fail if someone ever "fixes" the precedence by letting the override win. Then `npm run test` in full, because this file is on every screen's path.

- [x] **T-02 · Message catalog additions and the autonym allow-list** ✔ 2026-08-26, `npx vitest run lib/i18n` green (15 tests), typecheck + prettier clean. **Mutation-checked**: dropping the two allow-list entries turns the not-copy test red naming exactly those keys, so the entries are load-bearing rather than decorative.
      - files: `lib/i18n/messages/en.ts`, `lib/i18n/messages/pt.ts` (9 keys, data-model §1), `lib/i18n/translationNotCopy.test.ts` (2 allow-list entries)
      - covers: C-09, NFR-02 · scenario: "Every string on the screen exists in Portuguese"
      - depends: — · parallel: **yes** *(disjoint from T-01: catalogs and their test, not the provider)*
      - verify: `npx vitest run lib/i18n/translationNotCopy.test.ts lib/i18n/keysetCoverage.test.ts` — the keyset check proves both catalogs gained all 9 keys; the not-copy check proves the seven translatable ones actually differ. The two autonyms are allow-listed **with the reason inline**, and the suite's own "every allow-listed key is still genuinely identical" test keeps that entry honest.

- [x] **T-03 · Release identifier: one source, inlined at build** ✔ 2026-08-26, `npm run verify` green (66 files, **472 tests**, 469 → 472; build compiled). The decisive check was evaluating the config directly — it exposes `0.1.0`, matching `package.json`. **Half of this task's stated verify could not be discharged here, and was deferred rather than waived:** the version does not yet appear in any client chunk because nothing imports `appRelease()`, so tree-shaking correctly drops it. That half belonged to **T-04**, and was discharged there: with `LoginForm` importing it, the login chunk carries `{version:function(){let e="0.1.0";…}}`.
      - files: `lib/release.ts` (new — reads `NEXT_PUBLIC_APP_RELEASE`, falls back), `lib/release.test.ts` (new), `next.config.mjs` (inline from `package.json` → `version`), `.env.example`
      - covers: plan **A5**, deviation **D-1** · scenario: "The brand pane carries the lockup, tagline, bullets and release line" (its release half)
      - depends: — · parallel: **yes** *(disjoint from T-01 and T-02)*
      - verify: `npx vitest run lib/release.test.ts` for the fallback behaviour, then `npm run build` and confirm the real version string is inlined in the client chunk. **The seam is deliberate**: `next.config.mjs` cannot be unit-tested, so the untestable part is reduced to one line of config and everything the component depends on lives in a module that can be.

- [x] **T-04 · The card: two panes, brand pane, heading and hint** ✔ 2026-08-27, `npm run verify` green (66 files, **477 tests**, 472 → 477; build compiled). Written test-first: all 5 new tests were red before the component changed. **The 13 pre-existing `LoginForm` tests passed unmodified** — the evidence that this was a relayout and not a behaviour change. Also closes T-03's deferred half.
      - files: `components/LoginForm.tsx` (root becomes `nb-loginCard`; `af-panelBox`/`boxBody`/`nb-loginBody`/`nb-brand` removed; brand pane added; `aria-label` → `aria-labelledby`), `src/styles/adf-fusion.overrides.css` (shell backdrop, card, brand pane, credentials pane; **drop** the `nb-loginShell .af-panelBox` pin), `components/LoginForm.test.tsx` (append)
      - covers: data-model §3 · scenarios: "The card is two panes, brand first, with no panel box around it", "The brand pane carries the lockup, tagline, bullets and release line", "The credentials pane opens with the heading and the hint", "An expired session shows the warning inside the credentials pane"
      - depends: T-02, T-03 · parallel: no
      - verify: `npx vitest run components/LoginForm.test.tsx`. Assert the **absence** of `af-panelBox` explicitly, not just the presence of the new classes — otherwise a half-done migration that renders both passes. The expired-session scenario asserts the warning is a **descendant of the credentials pane**, which is what stops it drifting into the brand pane during the rewrite.

- [x] **T-05 · Combo fields, single-column rows, field errors in their own row** ✔ 2026-08-27, `npm run verify` green (66 files, **482 tests**, 477 → 482; build compiled). **Mutation-checked**: giving the adornment a `tabIndex` turns both the decorative test and the tab-order test red, so the keyboard claim is enforced rather than asserted. Two of the five tests here pin structure that already held (label association, errors inside their row) — kept as regression pins against the rewrite, and labelled as such rather than counted as new coverage. Also dropped `.nb-loginOptions`'s 76px indent, which existed only to align with the two-column row this task removes.
      - files: `components/LoginForm.tsx` (wrap both inputs in `af-comboField` + decorative `af-iconButton`), `src/styles/adf-fusion.overrides.css` (`nb-loginRow` → one column; combo full width; `pointer-events: none` on the adornment), `components/LoginForm.test.tsx` (append)
      - covers: data-model §3, risk **R6** · scenarios: "Email and password are combo fields with a decorative trailing icon", and the structural halves of "Submitting an empty form still sends no request" and "An invalid email address is rejected before any request"
      - depends: T-04 · parallel: no *(same two files)*
      - verify: `npx vitest run components/LoginForm.test.tsx`. Three assertions carry this task: each input still resolves via `getByLabelText` (the label association survived the wrapping), neither adornment is focusable, and neither is exposed to assistive technology. The existing empty-form and malformed-email tests must still pass **unedited** — they are what proves the wrapping did not disturb validation.

- [x] **T-06 · Action block and the locale control, wired** ✔ 2026-08-27, `npm run verify` green (66 files, **487 tests**, 482 → 487; build compiled). The switch tests render **without** a locale prop on purpose: the prop is rank 1 of the precedence and would correctly veto the override, so passing it would have produced a test that can never fail for the right reason. The no-request test is **self-validating** — the same listener that reports zero for the language change must then catch the sign-in call, otherwise the zero would only prove the counter never counts.
      - files: `components/LoginForm.tsx` (full-width submit; options row with Remember me + the locale select; links row), `src/styles/adf-fusion.overrides.css`, `components/LoginForm.test.tsx` (append)
      - covers: OQ-28 (b) at the UI · scenarios: "The action block is a full-width submit above the two link buttons", "The locale control switches the sign-in screen's own language"
      - depends: T-04, T-05, T-01, T-02 · parallel: no
      - verify: `npx vitest run components/LoginForm.test.tsx`. Selecting Português must re-render **the whole screen** in pt — assert on a brand-pane string as well as a field label, so a control that only relabels its own neighbourhood fails. Also assert **no request is made** when the locale changes: this control talks to nobody.

- [x] **T-07 · Live browser pass — the geometry jsdom cannot see** ✔ 2026-08-27, measured in a live
      browser at 1280×800 against `next dev`. `npm run verify` re-run green afterwards (66 files,
      **487 tests**, build compiled). **The pass earned its place: it caught two real conformance
      misses that every one of the 487 unit tests passed straight through.**

      | Property | Expected | Measured |
      |---|---|---|
      | `form.nb-loginCard` width | `700px` | **`700px`** (rect width also 700 — not shrunk) |
      | `.nb-loginPane` width / flex-grow | `330px` / `0` | **`330px` / `0`** |
      | `.nb-loginBrand` flex-grow | `1` | **`1`** |
      | `.nb-loginBrand` background | the handoff gradient | **`linear-gradient(160deg, rgb(84,116,155) 0%, rgb(58,92,134) 45%, rgb(31,63,99) 100%)`** — exact |
      | label vs input | label's bottom above input's top | **true**, both rows |
      | label text-align | `left` | **`left`**, both rows *(after fix 1)* |
      | submit width | = pane content width | **`286px` = `286px`** |
      | `.af-iconButton` | inert, not focusable | **`pointer-events: none`, `tabIndex < 0`** |
      | `.af-panelBox` | absent | **absent** |
      | horizontal page scroll | none | **none** |
      | React hydration warning (R4) | none | **none** — console carries only Fast Refresh and the DevTools notice |

      **Fix 1 — labels were right-aligned.** The base `.af-label` is right-aligned for the dense
      4-column `.af-form` grid, where the label sits *beside* its field. Stacked above it, that
      renders the required-asterisk floating over the input's far edge. Pinned to `left` for login rows.

      **Fix 2 — the card shrank to 590px on a narrow viewport.** `.nb-loginCard` is a flex *item* of
      `.nb-loginShell`, so it shrank below its declared `width: 700px` — inventing exactly the reflow
      the spec's *Out* section says is not designed. Pinned with `flex: none`. This is the same trap
      the retired `.af-panelBox` override worked around, one level up, and it is why the geometry
      contract is measured rather than assumed.

      **Live check of OQ-28 (b):** choosing *Português* switched the whole screen — heading `Entrar`,
      hint `Use as credenciais da sua organização.`, brand tagline and `Versão 0.1.0` — with the card
      still pinned at 700px.
      - files: `features/017-login-layout/tasks.md` (the evidence table, recorded inline against this task)
      - covers: NFR-09 · scenario: "The rendered geometry matches the handoff"
      - depends: T-06 · parallel: no
      - verify: run the satellite dev server, load the sign-in screen, and read the **computed** values from the live document against data-model §3's geometry table — card `700px`, pane `330px` / `flex-grow: 0`, brand `flex-grow: 1`, the gradient, the label's bottom edge above the input's top edge, submit width equal to the pane's content width. Record the measured values here, including any that missed. Also confirm the console carries **no React hydration warning** (risk R4). **This task is not optional bookkeeping: tiers 1–6 cannot prove this feature's headline claim, and a green suite without this pass is exactly how a non-conforming layout would ship as done.**

## Scenario coverage

All 13 spec scenarios are claimed; none is left uncovered.

| Scenario | Task |
|---|---|
| 1 · two panes, no panel box | T-04 |
| 2 · brand pane content | T-04 (release line via T-03) |
| 3 · heading and hint | T-04 |
| 4 · combo fields, decorative icons | T-05 |
| 5 · action block | T-06 |
| 6 · empty form sends no request | T-05 *(structure)* + the existing suite *(behaviour)* |
| 7 · malformed email sends no request | T-05 *(structure)* + the existing suite *(behaviour)* |
| 8 · expired-session warning placement | T-04 |
| 9 · every string in Portuguese | **T-08** *(was wrongly mapped to T-02's catalog tests — audit F-01)* |
| 10 · locale control switches the screen | T-06 *(mechanism: T-01)* |
| 11 · the choice is not persisted | T-01 |
| 12 · stored preference wins after sign-in | T-01 |
| 13 · rendered geometry | **T-07 — live browser only** |

- [x] **T-08 · Screen-level Portuguese sweep (audit F-01)** ✔ 2026-08-27, `npm run verify` green
      (66 files, **489 tests**, 487 → 489). Walks every visible text node of the rendered card and
      requires each to be a Portuguese catalog value, skipping `aria-hidden` subtrees and two named
      exceptions (the `*` marker, the `NOTEBOX` wordmark). **The F-01 probe now fails against it**,
      naming the stray string — the same mutation that passed 487/487 before.
      - files: `components/LoginForm.test.tsx`
      - covers: C-09 · scenario: "Every string on the screen exists in Portuguese"
      - depends: T-06 · parallel: no

Scenarios 6 and 7 are the one place where a task does not carry its scenario alone: the *behaviour*
they describe already ships and is already tested, and this feature's obligation is to not break it.
Those existing tests passing **unmodified** is the evidence — which is why no task lists them as
files to edit.
