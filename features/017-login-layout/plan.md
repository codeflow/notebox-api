# Plan — Login layout A: the two-pane brand card

**Feature:** features/017-login-layout · **Companion:** `data-model.md`
**Status:** Approved (human approval 2026-08-26) · **Date:** 2026-08-26
**Project:** `notebox-web` (react/next) — routed satellite

## Origin
- **Spec:** `features/017-login-layout/spec.md` (approved 2026-08-26), 13 scenarios.
- **US-7.1 / FR-19 / NFR-09**; **OQ-28 (b)** answered 2026-08-26.
- **Visual truth:** `notebox-web/design/handoff` screens **02** and **03**, README rows 02/03.

## Approach

Five changes, only one of which is interesting.

### A1 · The `<form>` becomes the card *(reversible)*

`LoginForm`'s root stops being an `af-panelBox` and becomes the bordered card itself, holding exactly
two panes: a brand pane that grows and a credentials pane pinned at 330 px. `boxBody`, `nb-loginBody`
and the centred `nb-brand` lockup disappear with it. `nb-loginShell` keeps its flex centring and
gains the handoff's radial-gradient backdrop.

This also **retires a trap rather than working around it**: `nb-loginShell .af-panelBox { flex: none;
width: 350px }` exists only because the base sheet declares `.af-panelBox { flex: 1 1 0% }`, so a
plain `width` on a standalone panel is ignored. With the panel box gone, the override goes too.

### A2 · The locale override — the one decision worth the paragraph *(one-way-ish, additive)*

`I18nProvider` gains an internal override and exposes `setLocale` on its context. Resolution order
becomes, highest first:

| Rank | Source | Set by |
|---|---|---|
| 1 | `preferred` prop | `providers.tsx`, from the signed-in member's `Me.locale` |
| 2 | **override** | the sign-in screen's locale control |
| 3 | `navigator.language` | the browser, read after mount |
| 4 | `en` | fallback |

**The supersession rule falls out of the ordering and needs no code.** Before sign-in there is no
`Me`, so rank 1 is empty and the override wins. After sign-in `providers.tsx` supplies `me.locale`,
which sits *ahead* of the override — the member's stored preference takes over with nothing to clear,
no effect to fire, and no cleanup that could be forgotten. Nothing is persisted: the override is
React state and a reload returns the screen to the browser-derived locale, which is exactly what the
spec's non-persistence scenario pins.

The override is written only from an event handler, never read during render, so the existing
hydration guarantee (the navigator is consulted after mount, so server and first client render agree)
is untouched.

### A3 · Combo fields *(reversible)*

Email and password each get wrapped in `af-comboField` with a trailing `af-iconButton` adornment
(✉ / 🔒). **Both classes already exist in `adf-fusion.css`** — they are used by no screen in the app,
not absent from the theme, so no widget CSS is authored, only login-scoped sizing.

The adornment is decorative: a `<span>` (not focusable), `aria-hidden`, and `pointer-events: none`.
That last one is deliberate — the base sheet gives `.af-iconButton` `:hover` and `:active` styling
because it is normally a control, and letting it light up under the cursor would advertise an action
that does not exist.

### A4 · Rows, heading, actions *(reversible)*

`nb-loginRow` goes from `grid-template-columns: 68px 1fr` to a single column — label above field,
left-aligned. It is used by no screen but this one. The credentials pane opens with the *Sign in*
heading and a `.tip` hint; the submit button becomes full-width; the options row (Remember me +
locale control) and the links row sit at the pane's edges.

The heading reuses the **existing** `login.title` key, and the form switches from `aria-label` to
`aria-labelledby` pointing at it — same accessible name, no longer duplicated in the a11y tree.

### A5 · The release identifier *(reversible, build-config)*

`next.config.mjs` inlines `NEXT_PUBLIC_APP_RELEASE` from `package.json`'s `version` at build time, so
the string has exactly one source and the client bundle receives only the version, not the manifest.
The handoff's literal `Release 1.0 · acme-ops` is mock data: the tenant is dropped per **D-1**
(unauthenticated screen, no tenant exists) and the number is the real build version — today `0.1.0`.

## Reversibility

**This feature has no one-way decisions in the strong sense** — no schema, no migration, no public
API, no external consumer. Saying so is more useful than manufacturing gravity for CSS. The two that
come closest:

- **`I18nContextValue` gains `setLocale`** — additive, so nothing existing breaks, but every screen's
  provider now exposes it and removing it later would touch consumers. Treated as a contract and
  pinned in `data-model.md`.
- **`NEXT_PUBLIC_APP_RELEASE`** — a build-config variable, which means deploy pipelines can start
  depending on it. It is documented in `.env.example` for that reason.

Everything else — markup, CSS, message keys — is a revert away.

## Alternatives rejected

**A nested `I18nProvider` inside the login route** instead of extending the shared one. It has a
genuinely smaller blast radius: the shared provider is never touched, and the member's preference
wins after sign-in because the nested provider unmounts on navigation. Rejected because that last
part is the problem — the supersession rule would be an *emergent consequence of unmounting* rather
than a stated rule. Render the sign-in form anywhere inside the app shell (a re-authentication
modal is the obvious future case) and it silently inverts, with no test to catch it. It also forces
the login page from a server component to a client one and duplicates the navigator-reading effect.
Precedence in one place is testable; unmount timing is not.

**Hardcoding the locale autonyms** (`{ en: 'English', pt: 'Português' }`) next to the `Locale` type
instead of putting them in both catalogs. Tempting — a language's own name does not translate. But
the codebase already has the mechanism for exactly this category: `IDENTICAL_ON_PURPOSE` in
`translationNotCopy.test.ts`, which already holds `branding.appName` for the same reason. Following
the established pattern beats introducing a second, better-looking one.

**Reading `package.json` directly from the component.** Next would bundle the whole manifest into the
client, publishing the dependency list to ship one version string.

## Blast radius

| File | Change | Risk |
|---|---|---|
| `components/LoginForm.tsx` | rewritten layout; behaviour untouched | the whole feature |
| `src/styles/adf-fusion.overrides.css` | card/brand/pane rules; `nb-loginRow` to one column; drop the `af-panelBox` pin | login-scoped |
| `lib/i18n/I18nProvider.tsx` | override state + `setLocale` on the context | **every screen** |
| `lib/i18n/messages/en.ts`, `pt.ts` | new keys (see `data-model.md`) | additive |
| `lib/i18n/translationNotCopy.test.ts` | two autonym entries on the allow-list | test-only |
| `next.config.mjs`, `.env.example` | `NEXT_PUBLIC_APP_RELEASE` from the manifest | build/deploy |
| `components/LoginForm.test.tsx` | new structural + locale tests appended | test-only |
| `app/(auth)/login/page.tsx` | **unchanged** — stays a server component | — |

**The existing suite is the regression net, and it must not be edited.** All 10 current `LoginForm`
tests query by role and label — not one asserts a class or a DOM shape — so a pure relayout should
leave every one of them green. That is the single most useful signal this feature has: **if one of
them breaks, behaviour changed, and the fix is the component, never the test.**

## Risks

| # | Risk | Signal that reveals it |
|---|---|---|
| R1 | The provider change reaches every screen | full satellite suite green + explicit precedence unit tests for all four ranks |
| R2 | A behavioural regression hides behind the relayout | any of the 10 existing `LoginForm` tests going red |
| R3 | **jsdom cannot prove the headline claim** — no stylesheet, so no width, gradient or label placement | spec scenario 13: live browser, computed values read from the document, evidence recorded in the feature |
| R4 | Hydration mismatch from the new state | no React hydration warning in the console during the live pass |
| R5 | `nb-loginRow`'s grid change leaks to another screen | it is referenced only by `LoginForm`; re-grep at implement |
| R6 | The decorative adornment reads as a control | `aria-hidden` + not focusable + `pointer-events: none`, asserted in a test |

## Verification strategy

Two tiers, because one of them cannot cover the other:

1. **jsdom (Vitest + Testing Library)** — scenarios 1–12: structure, content, catalog resolution,
   validation, the locale switch and its non-persistence, and the a11y properties of the adornments.
2. **Live browser** — scenario 13 only: `700px`, `330px`, `flex-grow` 0/1, the gradient, and the
   label sitting above the input, read from the live document and recorded in the feature.

Neither tier alone discharges FR-19. The plan states this because a green tier-1 run is the most
plausible way this feature could be declared done while still not matching the design.
