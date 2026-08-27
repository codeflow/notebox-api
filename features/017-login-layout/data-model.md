# Data model — Login layout A (notebox-web)

**ID:** features/017-login-layout · **Companion of:** `plan.md` · **Date:** 2026-08-26

**There is no persistence and no wire.** This feature adds no API call, no stored value and no
entity. What follows is what the implementer actually codes against: the catalog additions, the
i18n context contract, the locale precedence, and the DOM structure the audit checks.

## 1 · Message catalog additions

Appended to `lib/i18n/messages/en.ts` and `lib/i18n/messages/pt.ts`. Portuguese follows the terms the
catalog already established — **tenant → `Organização`** (`branding.tenant`), **annotation type →
`tipo de anotação`**.

| Key | en | pt |
|---|---|---|
| `login.hint` | Use your workspace credentials. | Use as credenciais da sua organização. |
| `login.brand.tagline` | Typed notes and subtask-driven tasks, one workspace per tenant. | Notas tipadas e tarefas movidas por subtarefas, um espaço de trabalho por organização. |
| `login.brand.point.1` | Define your own annotation types and fields | Defina seus próprios tipos de anotação e campos |
| `login.brand.point.2` | Tasks whose progress is computed, not typed | Tarefas com progresso calculado, não digitado |
| `login.brand.point.3` | English and Português, edited at runtime | English e Português, editados em tempo de execução |
| `login.brand.release` | Release {version} | Versão {version} |
| `login.locale.label` | Language | Idioma |
| `login.locale.en` | English | English |
| `login.locale.pt` | Português | Português |

**Reused, not re-added:** `login.title` (`Sign in` / `Entrar`) becomes the visible heading as well as
the form's accessible name. No new key.

`login.brand.release` uses the provider's existing `{var}` interpolation — `t('login.brand.release',
{ version })`.

### Allow-list additions

`login.locale.en` and `login.locale.pt` are **identical in both catalogs by design** and go into
`IDENTICAL_ON_PURPOSE` in `lib/i18n/translationNotCopy.test.ts`, each with the reason inline:

```ts
'login.locale.en', // "English" — an autonym: a language is named in its own language
'login.locale.pt', // "Português" — same
```

That is the point of the control. Someone who cannot read the current language has to be able to
find their own; translating `Português` into `Portuguese` in the English catalog would defeat it.

## 2 · The i18n context contract

`lib/i18n/I18nProvider.tsx` — the only shared surface this feature changes.

```ts
export type Locale = 'en' | 'pt';

export interface I18nContextValue {
  t(key: MessageKey, vars?: Record<string, string | number>): string;
  locale: Locale;
  /**
   * Overrides the locale for this browsing session. Ranks BELOW an explicit `locale` prop, so a
   * signed-in member's stored preference always wins. Persists nothing.
   */
  setLocale(locale: Locale): void;
}
```

**Additive.** Every existing consumer destructures `t` and `locale` and is unaffected.

### Resolution order — the invariant

| Rank | Source | Present when |
|---|---|---|
| 1 | `locale` prop (`Me.locale` via `providers.tsx`) | signed in with a stored preference |
| 2 | override, set by `setLocale` | the visitor used the sign-in screen's control |
| 3 | `navigator.language`, read after mount | always, client-side |
| 4 | `'en'` | fallback |

Invariants the tests must pin, one per rank boundary:

- **I-1** — rank 1 beats rank 2: with a `locale` prop set, `setLocale` changes nothing. *This is the
  "member's stored preference wins after sign-in" rule, and it is enforced by ordering alone — no
  clearing, no effect, nothing to forget.*
- **I-2** — rank 2 beats rank 3: with no `locale` prop, `setLocale('pt')` switches the tree to pt
  regardless of `navigator.language`.
- **I-3** — rank 3 beats rank 4: unchanged from today.
- **I-4** — the override is **not persisted**: a fresh mount resolves from rank 3 again.
- **I-5** — the override is never read during render and is written only from an event handler, so
  the server render and the first client render still agree (no hydration mismatch).

## 3 · DOM structure contract

What `implement` builds and `audit` checks. Class names are the contract; text comes from §1.

```
div.nb-loginShell                          ← radial-gradient backdrop, centres the card
└ form.nb-loginCard                        ← the card itself: border, white, shadow. NO af-panelBox
  ├ div.nb-loginBrand                      ← grows (flex: 1), gradient, light text
  │ ├ div.nb-brandLockup   → NoteboxLogo  (its own .nb-logo-word carries NOTEBOX — audit F-03:
│ │                          the shared lockup is reused rather than the wordmark duplicated) ("NOTEBOX")
  │ ├ div.nb-brandTagline  → login.brand.tagline
  │ ├ ul.nb-brandPoints    → 3 × li  (login.brand.point.1..3)
  │ └ div.nb-brandRelease  → login.brand.release, above a hairline rule
  └ div.nb-loginPane                       ← flex: none; width: 330px
    ├ h1#login-heading.nb-loginHeading     → login.title   (form is aria-labelledby this)
    ├ div.tip                              → login.hint
    ├ div.nb-loginFields
    │ ├ div.nb-msg.nb-msg-{error|warning}[role=alert]   ← only when there is a server/auth error
    │ └ div.nb-loginForm
    │   └ div.nb-loginRow          × 2     ← ONE column: label above field
    │     ├ label.af-label[for]    → span.req + login.{email,password}.label
    │     ├ span.af-comboField
    │     │ ├ input[id]                    ← unchanged: type, autoComplete, af-error, aria-invalid
    │     │ └ span.af-iconButton[aria-hidden=true]   ← ✉ / 🔒 — decorative
    │     └ div.nb-fieldError              ← only when that field is invalid
    ├ div.nb-loginOptions                  ← Remember me  ·  locale control
    │ ├ label.nb-remember    → input[type=checkbox]
    │ └ span.af-selectWrap   → select[aria-label=login.locale.label]  → 2 × option
    ├ button.af-button[type=submit]        ← full pane width
    └ div.nb-loginActions                  ← Forgot password?  ·  Add account (both button.nb-link)
```

**Removed:** `af-panelBox`, `boxBody`, `nb-loginBody`, `nb-brand` (the centred lockup), and the
`nb-loginShell .af-panelBox { flex: none; width: 350px }` rule.
**Unchanged:** every input's `id`, `type`, `autoComplete`, `af-error` class and `aria-invalid`, so
the existing role/label-based tests keep passing untouched.

### Geometry contract — live browser only

jsdom loads no stylesheet, so these are checked in a real browser and recorded (spec scenario 13):

| Element | Computed property | Expected |
|---|---|---|
| `form.nb-loginCard` | `width` | `700px` |
| `div.nb-loginPane` | `width` / `flex-grow` | `330px` / `0` |
| `div.nb-loginBrand` | `flex-grow` | `1` |
| `div.nb-loginBrand` | `background-image` | `linear-gradient(160deg, #54749b 0%, #3a5c86 45%, #1f3f63 100%)` |
| `label.af-label` vs its `input` | bounding boxes | label's bottom edge above the input's top edge |
| `button[type=submit]` | `width` | equals the pane's content width |

## 4 · Build-time configuration

| Name | Source | Consumer |
|---|---|---|
| `NEXT_PUBLIC_APP_RELEASE` | `package.json` → `version`, inlined by `next.config.mjs` | `login.brand.release`'s `{version}` |

Documented in `.env.example`. Reading the manifest happens at build time on the server so only the
version string reaches the client bundle, never the dependency list.
