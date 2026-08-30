# Visual pass — round two, 2026-08-30

**Method:** the product owner drove screen by screen in a live browser; each report was reproduced,
measured with `getComputedStyle`/`getBoundingClientRect`, fixed, and re-measured. No fix was called
done on a screenshot alone.

> **Why this exists.** Round one (`_visual-pass-report.md`) established that three evidence tiers
> exist — a class-inventory diff, the unit suite, and looking — and that only looking had caught six
> real defects. This round found the reason *why*, and it is structural: **jsdom loads no stylesheet
> and no layout engine**, so an entire family of defect is invisible to 583 passing tests. Three
> guards now close that family; the rest of this file is the evidence they were needed.

---

## 1. The defect family: markup the stylesheet never sees

Every item below shipped with the suite green. None is a "CSS tweak" — each is a name or a shape
the app wrote that the theme does not answer to.

| # | Defect | How it hid | Measured |
|---|---|---|---|
| 1 | 6 of 7 dialogs wrote `dlgFooter`; the theme defines `.dlgFoot` | jsdom asserts text and clicks, not padding | footer had no background, no separator, no button spacing |
| 2 | `ResetPasswordDialog` rendered its overlay **without `.open`** | the theme hides `.af-dialogOverlay`; the element was in the DOM, so every assertion passed | the reset control was **dead in the browser** — an admin could not reset a password at all |
| 3 | 6 `<input>` written with **no `type` attribute** | the theme styles controls by attribute (`input[type="text"]`); `el.type` reports `"text"` but attribute selectors do not match | "Display name" at **2px #767676** beside "Password" at **1px #9cafc2**, one form. Three signup fields had no border rule at all |
| 4 | `🗀` (U+1F5C0) in the tree | a glyph either renders or it does not; nothing asserts which | **renders as an empty box** — no font on the machine ships it. Confirmed two ways: bitmap-compared against a private-use codepoint, and rendered at 32px |
| 5 | 7 glyphs rendering as **colour emoji** (`🗑 🔍 🚫 🔒 🔑 👤 🌐`) | same | flat blue theme with full-colour pictures in it |
| 6 | `highlightAuto` returns an **empty tree** for text it cannot classify | no test covered an unclassifiable block | the renderer replaced the code with that tree — **the block was erased on the way to the screen** |
| 7 | a bare `<pre>` (which the editor's parser accepts) was skipped by the highlighter | selector was `pre > code` | 0 lines, no colour; the theme styles `pre`, so it looked normal |

**Pattern.** Five of the seven are the same shape: *the app writes one name, the theme answers to
another*. This is the fourth, fifth, sixth and seventh occurrence in this project, after
`af-toolbarButton` (theme: `af-toolButton`) and a batch of 53 `nb-*` hooks with no rule anywhere.

### The guards (`src/styles/classCoverage.test.ts`)

1. every themed class the app writes (`af-*`, `nb-*`, `dlg*`) has a rule in a stylesheet;
2. every `.af-dialogOverlay` renders with `.open`;
3. every `<input>` declares a `type`.

Each was **mutation-probed**: reintroduce the bug, the guard fails and names the file, while the
behavioural tests for the same components stay green — 18/18 on groups, 5/5 on members. That
contrast is the point of the guards, and the receipt that they earn their place.

---

## 2. Layout defects, measured

| Screen | Before | After |
|---|---|---|
| groups, overview | side-by-side panels declaring `flex: 1 1 0` measured **195px vs 242px** — flex items default to `min-width: auto`, so a panel holding a table refuses to shrink | `min-width: 0` + `align-items: stretch` → **501.5 × 501.5**, heights equal |
| task detail | the same row: `0 0 auto` vs `1 1 320px` — **different by design**, left alone; only the heights were equalised | verified before changing; "fixing" it would have been the regression |
| members form | one form, `Display name` **632px**, `Password` **123px** — a control that is a direct grid child stretches, one wrapped in a `<span>` does not | all controls **320 × 19** at one x |
| type builder | `Name` at **919px**; five sub-components each invented a row shape, so labels ended at **five** different x and controls began at **three** | one card per field with a title strip and icon actions; labels all at 433, controls all at 441 |
| record detail | `af-nameValue` is `max-content 1fr`, so one short field name collapsed the label column | the forms' **140px** right-aligned column |
| detail screens (×3) | title was an **unstyled `<h2>`: 22px, black, no rule** | 14px/700 `rgb(21,73,126)`, rule `1px #c8d4e0` — identical to the list screens |
| 8 pages | `af-panelBox` framed a whole page inside the content area's own border | frames removed; the two panel boxes that remain are side-by-side widgets, which is what the component is for |
| record + member forms | footers wore `af-toolbar` — a strip that sits *above* content, hence gradient and `border-bottom` | a footer is not a toolbar: transparent, `border-top` only |
| notes dock | `top: 52px` with no `bottom` collapsed the strip to **70.83px inside a 357.97px area** | `top: 0; bottom: 0`; then the dock chrome was dropped entirely — the base paints a rail for several tabs, and with one tab it read as a panel wrapped around a button |
| tab strip | top edge `#aebdcc`, bottom edge `#8fa6c0` — framed by a weak line above and a strong one below, which reads as no top border | tones matched. Deliberately **not** by adding a border to the strip: that would stack a second line and read as a thick edge |

### Things measured that turned out **not** to be defects

- **The notes tab's 14px gap** is `.af-panelMain`'s own gutter, symmetric on both sides, not the
  scrollbar. Verified: document scrollbar **0px** with and without overflow (macOS overlay).
- **The task detail's unequal panels** are unequal by declaration, as above.
- **Four "different right edges"** on the record form were cell edges, not control edges.

---

## 3. Product decisions taken this session

Both are recorded in `catalogs/requirements.md`'s changelog; the source hierarchy ranks an explicit
chat decision with the PRD.

1. **The navigator follows the tab.** C28 had the sidebar list Annotations and Tasks at all times;
   built literally, the tree was identical on every tab while the tabs changed only the right pane.
   Overview keeps both branches — the reading C28 describes, on the screen it describes.
   `INTAKE.md` is **not** amended: it records what the client said, not what was decided.
2. **Administration is not a tab.** Reached from the menu bar; the navigator lists its destinations
   once you are there. A **recorded deviation** from handoff screens 05/15, which draw a fourth tab —
   NFR-09 measures conformance against the handoff, so this is an exception, not an omission.

Both closed a defect that had been invisible: the Administration tab matched only `/groups`, so
reaching `/members` lit **no tab at all**. Tab and tree now derive their section from one function
(`lib/navigation/section.ts`), which is what makes them unable to disagree.

---

## 4. Method notes worth keeping

- **Measure before changing.** Two instincts were wrong this session and only measurement caught
  them: a form-width cap that made the form *wider* than before, and a `ResizeObserver` that never
  fired at all.
- **A ResizeObserver on a full-width flex row never fires.** The strip's own box does not change
  when its labels do. It was replaced with a dependency on the rendered text and the selected
  section — plain React, and testable without a layout engine.
- **Probe the probe.** Three mutation probes "passed", which would have meant tests that test
  nothing. The shell escaping had made the mutations no-ops. Re-run with an assertion that the
  target was found, all three failed as they should. A probe that passes deserves suspicion.
- **A test that remounts cannot test a dependency array.** The first locale-change test unmounted
  and re-rendered, so the effect re-ran either way; it was rewritten to `rerender` in place.
- **The browser pane does not deliver ResizeObserver callbacks when hidden**, and it composites no
  frames — so screenshots can silently go stale. Prefer measurement; treat a screenshot as a
  second opinion.

---

## 5. State at the close

- **583 tests** in `notebox-web` (from 566 at the start of the session), `tsc` and `eslint` clean.
- One suite run reported a **timeout** (not an assertion failure) in `TypeBuilderForm.test.tsx`;
  two subsequent full runs and three isolated runs were green. Recorded as **not reproduced**,
  cause not confirmed — most likely the dev server and the suite competing for CPU.
- The three guards are the durable outcome. Everything else in this file is a symptom they cover.
