# feat-027 — Preferences and Help

## Origin

- **Brief item 18.1**: *"`Preferences` and `Help` (branding bar) do nothing and should work."*
- Deferred by fix-visual-pass because *"should work" does not say what they do* — building either
  would have been inventing product.
- **Answered by the product owner, 2026-09-01**: Preferences = interface language + theme;
  Help = an "About" page with version and links.

## In

- **Preferences**, as panel content: the interface language, saved as the member's own preference.
- **Help**, as panel content: product name, version, tenant, and the release link.
- Both open from the branding bar, which is where they already are.

## Out — and one of these needs the product owner back

- **The theme (light/dark).** Deliberately not in this feature. `adf-fusion.css` carries **169
  distinct hardcoded colours and zero CSS custom properties**: a real dark mode means tokenising
  all of them by role and then *designing* a dark ADF Fusion palette. Shipping a toggle that
  darkens the shell and leaves the widgets light is worse than shipping none — it looks broken
  rather than unfinished. **Raised as OQ-36 with its cost, for a decision.**
- Anything else behind Help. No support address exists, no documentation is written.

## Assumptions

| # | Question | Decision | Rejected |
|---|---|---|---|
| **D-1** | Where does the language preference live? | **On the member**, via `PATCH /users/{id}` — the endpoint already carries `locale`. | The session override (`setLocale`). It is **rank 2** in `I18nProvider`'s resolution and `Me.locale` is rank 1, so for a signed-in member with a stored preference the override would have had *no effect at all*. Found by reading the provider, not by trying it. |
| **D-2** | How does the app see the new language without a reload? | The auth context re-reads `me` after the save, and `I18nProvider` already takes `Me.locale` as its highest-ranked input. | A full page reload — it works, and it throws away whatever else the member had open in the panel. |
| **D-3** | What does Help contain, given nothing is written? | **Only what is true**: the product name, the release version already shown in the footer, the tenant's name and slug, and the repository link. | Inventing a support address or a documentation link that 404s. |

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-15 — a member chooses the language the interface speaks

  Scenario: Preferences opens from the branding bar
    Given a signed-in member
    When they activate "Preferences"
    Then the panel shows the interface-language control at their current language

  Scenario: Choosing a language saves it as the member's own preference    # [D-1]
    Given Preferences open, with the language at English
    When the member chooses Português
    Then their stored preference is updated
    And the interface is in Português without a page reload

  Scenario: The choice survives a reload
    Given a member whose stored preference is Português
    When they sign in again
    Then the interface is in Português

Feature: Help says what is true and nothing else                            # [D-3]

  Scenario: About shows the product's own facts
    Given a signed-in member
    When they activate "Help"
    Then the panel shows the product name, the release version and the tenant
    And it offers no link this product cannot honour
```

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-02 Authenticated** | **applies** | The save goes through `authFetch` like every other call. |
| **C-03 Least-privilege** | **applies** | A member may change **their own** preference and no one else's: the request carries the signed-in member's id, taken from the session rather than from anything on screen. Asserted. |
| **C-09 Localization** | **applies** | Every string in both panels, both catalogs. The language names are written in their own language (English, Português) — a member who cannot read the current interface must still recognise their own. |
| **C-04 Personal data** | **applies** | Help shows the tenant's name and slug, which the branding bar already shows. Nothing new is exposed. |
| Others | not applicable | No new endpoint, no upload, no rich text. |
