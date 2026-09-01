# Brief — visual pass

**Opened:** 2026-08-31
**Status:** collecting

## Items

### 1 — Overview / workspace screen

**Screenshot supplied** (1436×737, the Overview tab, logged in as the Live Pass tenant). What it
shows, for when the image is gone from context:

- Menu bar `Workspace · Edit · View · Administration · Help`; level-1 tabs `Overview` (selected)
  `Annotations` `Tasks`.
- Left sidebar: `▼ Workspace` header, then the tree — `Annotations` → `Ungrouped (13)` →
  `Reunião de projeto (13)`, and a `Tasks` root. Sidebar occupies the full column height with a
  large empty area below the tree.
- Breadcrumb `Workspace`, then the page title **"Notebox — Live Pass"**.
- Two `af-panelBox` cards side by side, each roughly 570px wide, sitting in the top ~270px:
  - **Annotations**: Types defined 2 · Records 13 · With images 0 · Secret fields 0
  - **Tasks**: Open tasks 0 · Subtasks done 0 / 0 · Ends this week 0
- Below the two cards, roughly 450px of empty white down to the footer
  `Notebox · Release 0.1.0`.

**The human's words, verbatim:**

> Bom primeiro, a tela inicial do Notebox, a tela do workspace. Acho que não precisa desse texto:
> — Live Pass
>
> Melhore a tela do workspace. Deixe ela bem profissional considerando o estilo web do ADF Fusion.
> Melhore ela da melhor forma possível. O ícone de pastinha que representa o grupo, ela é meio
> achatada. A altura dela é menor que as demais

Three things in one item:

1. **Drop `— Live Pass` from the title.** The heading reads "Notebox — Live Pass"; the tenant name
   is appended to the product name.
2. **Improve the workspace screen — "bem profissional", in the ADF Fusion idiom, "da melhor forma
   possível".**
   > ⚠ needs clarification at start — this is open-ended by design ("the best way possible"), and
   > the lane's rule is that anything needing a decision others could disagree with needs a spec.
   > At `start`: propose a concrete list of changes and get them chosen, rather than redesigning
   > the screen on my own judgement.
3. **The group folder icon is squashed** — "meio achatada", its height smaller than the others.
   In the tree, `Ungrouped` carries a folder glyph next to the annotation and task icons.

---

### 2 — Annotations screen (types list), and a system-wide grid-editing rule

**Screenshot supplied** (1433×729, the Annotations tab, types list with a band open). What it
shows, for when the image is gone from context:

- Branding bar `NOTEBOX Notebox v0.1.0`, right side `Signed in as Live Pass | Tenant: Feat020 Live
  Pass (feat020-live) | Preferences | Help | Sign out`.
- Tabs `Overview · Annotations` (selected) `· Tasks`. Sidebar tree: `Annotations` (row appears
  highlighted/current) → `Ungrouped (13)` → `Reunião de projeto (13)`.
- Breadcrumb `Workspace > Annotations`, title `Annotation types` with the new-type icon beside it.
- Outer grid, columns: (disclosure) · Icon · Name · Fields · Visible in grid · Records · Created ·
  Actions. Two rows: `Contato` (2 / 2 / 0 / 31-Aug-2026) collapsed with `⊞`; `Reunião de projeto`
  (3 / 2 / 13 / 31-Aug-2026) expanded with `⊟`. Row actions are three icon buttons
  (view / edit / delete).
- The open band beneath `Reunião de projeto`: an inner grid `Name · Assunto · Participantes` with
  10 rows (Revisão de contrato … Alinhamento com o produto, values 16 down to 7), closing with the
  `See all 13 records` link.

**The human's words, verbatim:**

> Na tela de annotations, a altura das linhas do grid pai poderiam ser menores, no subgrid a borda
> de cima da linha das colunas não aparece. Um detalhe, regra pra todo o sistema, a edição no
> datagrid (em qualquer um em qualquer tela, deve ser uma edição na própria linha (nesse caso os
> campos que estão marcados como visible true). E deve haver uma marcação para editar os campos
> que não são visible e ai nesse caso deve abrir um popup com todos os campos. Os popups devem ser
> naquele estilo sidebar com opção para expandir.

Four things in one item:

1. **Outer grid row height** — the parent rows could be shorter.
2. **The band's header row has no top border** — the inner grid's column header is missing its top
   edge.
3. **System-wide rule: inline editing in every data grid, on every screen.** Editing happens in
   the row itself, for the fields marked `visibleForViewing`.
4. **A marker to edit the non-visible fields**, which opens a popup with **all** fields. Popups
   should be in "aquele estilo sidebar com opção para expandir" — a side panel that can be
   expanded.

> **Routed out of this lane, 2026-08-31: registered as OQ-35**, to be specced as its own feature
> once this visual pass ships. The six open decisions are listed there. Items 3 and 4 are **not
> fix-lane work**. They add behaviour
> (an editing mode, a new panel, a save path per row) across every grid in the system, they touch
> BR-05's confirmation rules and the Secret-field path (C-12), and "aquele estilo sidebar" refers
> to a pattern I must not assume I know. Under this lane's own rule — *anything that adds
> behaviour, a route, or a rule needs a spec* — these belong in `/wf-feature`, not here. Items 1
> and 2 are true fix-lane work.

**Clarification on "aquele estilo sidebar" (asked and answered during collect, 2026-08-31):**

The human asked whether I knew the popup-sidebar pattern. Checked the codebase rather than
assuming: the project **already has one** — `af-drawer` + `af-drawerDock` + `af-drawerTab`,
implemented in `components/tasks/NotesDrawer.tsx` (design screen 16), opening from a tab on the
right edge. `components/groups/GroupsPanel.tsx` and `components/tasks/SubtasksPanel.tsx` are
panels of a different kind (inline, not overlay).

What that drawer does NOT have is the **expand** state the human asked for: today it is
open/closed only, with no second width. So the reuse target exists; the expandable behaviour is
new. Still `/wf-feature` work, not fix-lane — but the starting point is `af-drawer`, not a new
pattern.
