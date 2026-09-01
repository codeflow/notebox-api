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

---

### 3 — The confirm dialog: button layout, and severity icons

**Screenshot supplied** (1425×736, the types list with the delete-confirmation dialog open over
it). What it shows, for when the image is gone from context:

- Dialog roughly 390px wide, centred over the grid, titled **"Delete annotation type"** in a blue
  ADF title bar.
- Body: the type's name **"Reunião de projeto"** on its own line, then
  **"This action cannot be undone."** in red.
- **The problem, as drawn:** the two controls are on SEPARATE LINES, right-aligned and stacked —
  `Cancel` on one line, and beneath it a wide button reading
  **"Are you sure you want to delete this annotation type?"**. The question is rendered as the
  confirm button's label, so the button is far wider than `Cancel` and they do not line up.
- No icon anywhere in the dialog.

**The human's words, verbatim:**

> O confirm dialog ficou meio estranho as areas dos botoes. Deveriam ficar lado a lado um leve
> espaço entre eles. O confirm dialog deveria ter um ícone pra cada tipo de alerta (icones estilo
> ADF Fusion) pra error, info e warning

Two things:

1. **Buttons side by side**, with a small gap between them — not stacked on separate lines.
2. **A severity icon per alert type** — error, info, warning — in the ADF Fusion idiom.

> Note for `start`, not a question for now: the confirm button's label is currently the whole
> question ("Are you sure you want to delete this annotation type?"), which is what makes the two
> controls so lopsided. Whether that string moves into the body (leaving a short verb on the
> button, the ADF convention) is a wording change, so it needs the human's call — it is not mine
> to decide silently while "fixing the layout".

---

### 4 — The annotation-type detail screen: fields as a grid, not as cards

**Screenshot supplied** (1436×729, `Workspace > Annotations > Annotation type`, viewing
"Reunião de projeto"). What it shows, for when the image is gone from context:

- Title `Reunião de projeto`, and a right-aligned button row:
  `View records · New record · Edit · Delete`.
- The type's three fields are rendered as **three stacked full-width cards**, each ~58px tall,
  with a pale blue body and a thin border:
  - **Assunto** — `Field type **Text**   Visible ✔`
  - **Participantes** — `Field type **Number**   Visible ✔`
  - **Notas** — `Field type **Free text**   Visible —`
- Visible is shown as a check mark `✔` when true and an em dash `—` when false. No secret column
  is drawn on this screen at all.
- Everything below the third card (~380px) is empty.

**The human's words, verbatim:**

> Essa tela onde eu vejo os campos do annotation type que pra ver, acho que seria melhor exibir
> num datagrid estilo ADF Fusion com ícones para ações no datagrid (estilo Fusion) (melhor opcao
> visual de icones para os campos visible e secret bem estilo ADF Fusion) e novamente o datagrid
> deveria ser editable inline, então não sei como você pode fazer com essa parte da alteração
> maior)

Four things, and they split across two lanes:

1. **Render the fields as an ADF Fusion data grid** instead of stacked cards. — fix lane.
2. **Row action icons in the grid, Fusion style.** — fix lane *if* the actions already exist
   elsewhere on this screen (the header already has Edit/Delete for the TYPE; per-FIELD actions
   would be new behaviour → see 4).
3. **Better icons for the `visible` and `secret` flags**, Fusion style, replacing `✔` / `—`. Note
   the screen does not currently show `secret` at all, so that column would be new. — fix lane for
   the column and the icons; it renders data the DTO already carries.
4. **Inline editing again** — the human explicitly flags the overlap: *"então não sei como você
   pode fazer com essa parte da alteração maior"*. → **OQ-35**, same feature as item 2's points
   3–4. This screen becomes one of its callers.

> The human is right to name the collision. The visual half (grid, icons, a secret column) can
> ship in this lane and is worth doing on its own; the editing half waits for OQ-35's spec. To
> decide at `start`: whether converting this screen to a grid NOW, knowing it gains inline editing
> later, is the right order — or whether it should wait so the screen is rebuilt once.

---

### 5 — The annotation-type EDIT screen: the icon picker

**Screenshot** — the first one attached was the wrong frame (the type detail screen); the human
said so and sent the right one. The correct frame (1435×770, `Workspace > Annotations > Annotation
type > Edit`, "Edit annotation type") shows:

- **Type header:** `*Name` = `Reunião de projeto` in a text input; below it `Icon` with a native
  **`Choose File` button + the text `No file chosen`**, and, well to the right of that pair and
  clearly not aligned with it, a red-bordered **`Remove icon`** button. **The saved icon is not
  displayed anywhere** — no thumbnail, no filename; the control reads exactly as it would for a
  type that never had one. That is the bug in item 1, visible in the frame.
- **`Fields`** section, three numbered cards, each with a blue header carrying the index and the
  field name and, right-aligned, three icon buttons: `↑` `↓` `✕`.
  - **1 Assunto** — Field name `Assunto`, Field type `Text` (select), **Field icon** with the same
    native `Choose File` + `No file chosen`, then the checkboxes `☑ Visible` `☐ Secret`.
  - **2 Participantes** — Field type `Number`, same `Choose File` control, plus `Minimum` and
    `Maximum` inputs (both empty), then `☑ Visible` (no Secret checkbox for Number).
  - **3 Notas** — Field type `Free text`, same `Choose File`, `☐ Visible` `☐ Secret`.
- So the per-field icon pickers use the **same native `Choose File` control** as the type's, which
  is why the human says the rules are the same there.

**The human's words, verbatim:**

> Essa tela de edição dos annotation types está quase boa, mas quando eu adiciono um ícone e salvo
> e depois volto na tela de edição o ícone não aparece, o Remove icon fica desalinhado com o campo
> e talvez substituir o Choose file por um ícone (estilo adf fusion) que represente para buscar um
> arquivo. Na area dos campos a regra é a mesma. Não cheguei a testar a parte dos ícones nos
> campos, mas as regras são as mesmas. E o botao remove icon não sei se ficaria melhor um ícone de
> remove com toolltip (tooltip com i18n) ou deixa assim.

Five things:

1. **BUG — a saved icon does not come back.** Add an icon, save, reopen the edit screen: the icon
   is not shown. **This is the only defect in the whole brief that is not cosmetic** — the data is
   saved (the list shows it) but the edit form does not render it, or does not load it.
2. **`Remove icon` is misaligned** with the field.
3. **Replace `Choose file`** with an ADF Fusion style icon meaning "browse for a file".
4. **The same rules apply to the field icons** (the per-field icon picker). The human has not
   tested that area — *"Não cheguei a testar a parte dos ícones nos campos, mas as regras são as
   mesmas"*. So: apply the same treatment, and **verify whether the same bug is there** rather
   than assuming either way.
5. **Open question from the human, not a decision for me:** *"o botao remove icon não sei se
   ficaria melhor um ícone de remove com tooltip (tooltip com i18n) ou deixa assim"* — icon +
   localized tooltip, or leave the text button. Ask at `start`.

> Item 1 is a real bug and takes priority over the cosmetics in this brief: it means a member can
> set an icon, believe it saved, and find it gone. Whether the cause is the form not loading the
> value or the value not being persisted is unknown until it is reproduced — the human observed
> the symptom, not the cause, and I have not looked yet (collect does not investigate).

---

### 6 — The NEW annotation-type screen: the same rules as item 5

**Screenshot supplied** (1431×721, `Workspace > Annotations > New`, "New annotation type"). What
it shows:

- `*Name` — an empty text input.
- `Icon` — the same native **`Choose File` + `No file chosen`** control as the edit screen. **No
  `Remove icon` button here**, which is consistent: nothing has been chosen yet.
- `Fields` section holding only an **`Add field`** button — no field cards, since the type is new.
- Footer buttons `Cancel` and `Save`, left-aligned under a horizontal rule.
- Everything below is empty.

**The human's words, verbatim:**

> As regras que eu acabei de falar tem que valer pra essa também

So item 5's treatment applies to this screen too: the `Choose File` control becomes the ADF Fusion
browse icon, and whatever is decided for the picker's layout and its remove affordance applies
here as well.

> Two notes for `start`, neither of them a decision to take now:
> - Item 5's **bug** (a saved icon not coming back) cannot occur here — there is nothing saved yet.
>   What must hold on this screen is the *preview* behaviour: once a file is chosen, the member
>   should see what they chose before saving. Whether that is already true is untested.
> - The `Cancel` / `Save` pair here is **left-aligned**, while the confirm dialog of item 3 puts
>   its buttons on the right. Worth deciding once whether form footers and dialog footers follow
>   the same alignment rule in this theme, rather than fixing each screen to a different taste.

---

### 7 — The record detail screen: replace with a side panel, and present the values well

**Screenshot supplied** (1433×671, `Workspace > Annotations > Annotation type > Records > Record`,
viewing the record "Revisão de contrato"). What it shows:

- Title `Revisão de contrato`, right-aligned buttons `Edit` and `Delete record`.
- Two label/value rows, labels right-aligned in a narrow column, values in plain text:
  - `Assunto` → `Cláusulas de SLA`
  - `Participantes` → `16`
- **`Notas` (the Free-text field) is not shown** — this record has no value for it, and the screen
  draws nothing for an empty field.
- Roughly 450px of empty white below, down to the viewport edge.

**The human's words, verbatim:**

> Essa tela podia ser substituida por um popup no estilo sidebar. E melhore também como é
> apresentado os dados, deixe bem bonito considerando que temos um richtext e blockcodes. Sempre
> estilo ADF Fusion

Two things, in different lanes:

1. **Replace the screen with a side-panel popup** — the same `af-drawer` pattern named in item 2.
   → **not fix-lane**: it removes a route (or changes what the route renders), changes how a
   record is reached from the grid, and inherits the same expand behaviour OQ-35 has to define.
   Attach to **OQ-35**, which already owns the side panel and the grid interaction.
2. **Present the values properly, "bem bonito", accounting for rich text and code blocks**, in the
   ADF Fusion idiom. → fix-lane *as presentation*: the rich-text renderer already exists
   (feat-006/007 sanitize and render it; the grid shows a plain-text preview through
   `RecordGridCell`). This item is about the DETAIL view giving a Free-text value real typography
   and a proper code block, instead of a bare label/value row.

> Worth raising at `start`, not deciding now: doing (2) inside the current screen and then moving
> everything into a panel at OQ-35 means styling the same content twice. The alternative is to do
> (2) only when the panel lands. Which order is right depends on how soon OQ-35 gets specced —
> the human's call.

---

### 8 — The record EDIT screen: side panel, a short rule, and a broken image in the rich text

**Screenshot supplied** (1430×694, `Workspace > Annotations > Annotation type > Records > Record >
Edit`, title "Reunião de projeto"). What it shows:

- Form rows, labels right-aligned: `Name` = `Alinhamento com o produto`; `Group` = `No group`
  (select); `Assunto` = `Prioridades do trimestre`; `Participantes` = `7` in a number input with
  spinner arrows.
- `Notas` — the rich-text editor. Toolbar: **B I U S**, bullet list, ordered list, quote, `<>`
  (inline code), a code-block button, a divider, a `https://` URL input, a link button and an
  image button.
- **Inside the editor body: a BROKEN IMAGE placeholder with the alt text `RabbitMQ.png`** — the
  browser's broken-image glyph, not the picture. The editor area is otherwise empty.
- Below, a horizontal rule and the buttons `Save` `Cancel`, left-aligned.
- **The rule above the buttons is visibly SHORTER than the one under the page title**: the button
  rule ends at roughly the editor's right edge (~1043px), while the title rule runs the full
  content width (~1420px).

**The human's words, verbatim:**

> Essa tela aqui acho que vale ser um popup estilo sidebar com expansão. Repare que a linha
> horizontal que separa os botoes é menor que linha horizontal de cima. E quando eu adiciono uma
> imagem no richtext parece que dá algum erro, ela não aparece e fica igual está no print

Three things:

1. **Turn this screen into an expandable side panel** → **OQ-35**, same as items 2 and 7.
2. **The button rule is shorter than the title rule.** → fix lane, and visible in the frame.
3. **BUG — an image inserted in the rich text does not render**, showing the broken-image
   placeholder with the file name as alt. → **not cosmetic.** This is the second real defect in
   the brief, alongside item 5's icon. Both involve an image that was uploaded and then fails to
   come back, which is a suspicious coincidence: they may share one cause in the image path
   (`imagesClient.fetchObjectUrl` / the binary endpoint / authentication on the `<img>` src),
   or they may be unrelated. **Not investigated — collect does not diagnose.** Both to be
   reproduced at `start`, and if they do share a cause, fixed once.

---

### 9 — The Tasks screen with nothing in it: a stray row highlight, and a bare empty state

**Screenshot supplied** (1434×686, `Workspace > Tasks`, the Tasks tab selected, tenant with no
tasks). What it shows:

- Sidebar: a single tree node **`Tasks`**, and **its row is drawn with a highlight** — a filled
  band spanning the sidebar width, the same treatment a selected node gets. There is no other node
  under it (no groups, no tasks).
- Main area: breadcrumb `Workspace > Tasks`, title `Tasks` with the new-task icon beside it, a
  horizontal rule, and then, **centred in the content area**, the plain text `No tasks yet` with a
  `New task` button directly beneath it. No icon, no panel, no border — just text on white.
- The rest of the viewport is empty.

**The human's words, verbatim:**

> Nessa tela aqui, quando não tem tasks, o item Tasks no treeview, fica com uma cor na linha. O
> texto No tasks yet melhore isso, coloque um ícone de info (estilo adf fusion) talvez dentro de
> um panel de mensagem estilo Fusion, não sei, faça a melhor experiencia visual

Two things:

1. **The `Tasks` node in the tree carries a row colour** it should not — the human is reporting it
   as wrong. Note this screen is `/tasks`, so **feat-021 (just built, in PR #21) is what makes that
   root the current page and paints `nb-treeRow-selected`**. It is very likely my own change,
   showing up here as unwanted. Two readings, and the human's wording ("fica com uma cor na
   linha") suggests they do not want it: either the highlight is wrong on a root node in general,
   or it is right but too heavy. **To settle at `start`, against feat-021's own decision** — that
   change was made deliberately (the root was "current page" for a screen reader and for nobody
   else) and reverting it blindly would undo a fix, so this needs the human, not my guess.
2. **The empty state is bare text.** Wants an info icon (ADF Fusion) and possibly a Fusion message
   panel — *"não sei, faça a melhor experiencia visual"*. → fix lane, but open-ended in the same
   way item 1's workspace screen is: propose concrete options at `start` rather than picking a
   design alone. Note the annotation-types list has its own empty state (`nb-pcEmpty`, "No
   annotation types yet" + a button) — whatever is decided should apply to both, not just this
   screen.

---

### 10 — The new-task screen: a popup, and rules narrower than the page

**Screenshot supplied** (1435×554, `Workspace > Tasks > New`, "New task"). What it shows:

- Form rows, labels right-aligned: `Name` (empty); `Priority` = `Select a priority` (select);
  `Group` = `No group` (select); `Card code` with placeholder `e.g. OPS-2481`; `Card URL` with
  placeholder `https://...`.
- Buttons `Save` `Cancel`, **right-aligned** at the end of the form block.
- **Two horizontal rules** — one under the title, one above the buttons — and **both stop at
  ~1045px** while the content area runs to ~1420px. So neither reaches the full page width, unlike
  other screens where the title rule spans everything.
- Sidebar shows only the `Tasks` node, highlighted as in item 9.

**The human's words, verbatim:**

> A tela de criar tasks poderia ser um popup e as 2 linhas horizontais não tem a largura total
> igual outras páginas

Two things:

1. **Make the create-task screen a popup** → **OQ-35** territory (the side panel), same as items 2,
   7 and 8. Note the human says "popup" here without saying sidebar; whether create-forms use the
   same expandable panel as the record views, or a different dialog, is one more thing OQ-35's
   spec has to settle.
2. **Both rules are narrower than the content area** → fix lane, and measurable: ~1045px against
   ~1420px available.

> Cross-reference for `start`: item 8 reported the *opposite* asymmetry on the record-edit screen —
> there the title rule ran full width and the button rule was short. Here **both** are short. So
> the underlying issue is probably not "one rule is wrong" but that **rule width is inconsistent
> across form screens**, which is worth fixing as one rule for all of them rather than screen by
> screen. Also note the button alignment differs again: right here, left on the record-edit and
> new-type screens (item 6's note).

---

### 11 — Field validation messages are ugly

**Screenshot supplied** (1429×571, `Workspace > Tasks > New`, the same new-task form after a failed
submit). What it shows, compared with item 10's clean state:

- `Name` empty, and **directly beneath it a full-width pink band** (running from the left edge of
  the content area at ~260px to ~1040px, the form block's width) containing the red text
  **"The task name is required."**, left-aligned at the very start of the band — i.e. under the
  LABEL column, not under the input it refers to.
- `Priority` still `Select a priority`, with a second identical pink band beneath it:
  **"The task priority is required."**
- The bands are full-bleed strips with no border, no icon, no padding to speak of; they push the
  following rows down, so the form visibly jumps when validation fires.
- `Group`, `Card code`, `Card URL` unchanged; `Save` `Cancel` right-aligned as before.

**The human's words, verbatim:**

> As mensagens de erro ficaram feios. Acho que poderia ter um ícone de erro (estilo fusion) do lado
> do campo e uma mensagem melhor formatada (estilo melhor) desse jeito ficou feio

Two things:

1. **An error icon beside the field** (Fusion style) — the human's suggested direction.
2. **Better formatted message** — the current full-width strip is the complaint.

> Notes for `start`, not decisions now:
> - This is the **third** place in the brief asking for Fusion severity icons: item 3 wants
>   error/info/warning icons in the confirm dialog, item 9 wants an info icon in the empty state,
>   and this wants an error icon on a field. **One icon set, defined once**, rather than three
>   separate answers — that is a real economy and should be settled together at `start`.
> - The message currently sits under the label column rather than aligned with the input, and it
>   reflows the form when it appears. Whether the fix is inline-beside-the-field (as the human
>   suggests), or a reserved slot that does not shift the layout, is a design choice to put to the
>   human with concrete options.
> - Validation messages are **localized** (C-09) — whatever markup carries them must keep both
>   locales working, and the existing keyset guard covers that.

---

### 12 — BUG: the first Save appears to do nothing, and a second click creates a DUPLICATE task

No screenshot — a behaviour the human observed on the new-task screen (item 11's frame).

**The human's words, verbatim:**

> Teve um comportamento nessa tela foi que eu preenchi os dados e cliquei no save as mensagens de
> erro sumiram mas ele continuou nessa tela. Ai cliquei em salvar denovo ai foi pra tela onde
> aparece a task criada e os detalhes da task como a subtask. Ai quando voltei nas tasks,
> apareceram 2 tasks com o mesmo nome

The sequence, as reported:

1. Validation errors were on screen (item 11's state).
2. Fields filled in, **Save** clicked → the error messages cleared, **but the form stayed put**;
   no navigation, no visible feedback.
3. **Save** clicked a second time → navigated to the created task's detail screen (with its
   subtasks).
4. Back on the tasks list: **two tasks with the same name**.

> **This is the most serious item in the brief, and it is not visual.** Two records were created
> where the member intended one. The first click DID create a task — it simply gave no sign of it,
> which is what made the second click reasonable. So this is not "a slow form": it is a create
> path that can duplicate data on a double submit, and the missing feedback is what invites it.
>
> Untouched by me so far — collect does not diagnose. What the fix has to establish at `start`,
> without guessing now:
> - whether the submit button is disabled while the request is in flight (the usual cause);
> - what happened to the navigation after the first success — did it fire and fail, or never fire;
> - whether the same pattern exists in the OTHER create forms (new annotation type, new record),
>   because they were built from the same shape and would carry the same defect;
> - whether anything server-side would make a duplicate submit idempotent, or whether the guard
>   has to be entirely in the client.
>
> **This one arguably does not belong in the fix lane at all** — it is a data-integrity defect, not
> a cosmetic one. Flagging it now so the decision is deliberate: fix it here as a bug, or spec it
> alongside OQ-35's editing work. The human's call at `start`.

---

### 13 — The tasks grid: an actions column, and the ONE grid that must not edit inline

**Screenshot supplied** (1422×675, `Workspace > Tasks`, the list with two tasks). What it shows:

- Sidebar: `Tasks` node (highlighted, item 9) with `Ungrouped (2)` beneath it.
- Grid columns: `Task · Group · Priority · Subtasks · Status · Start · End · Card`.
- **Two rows, both named `Teste`** — Ungrouped, Medium, 0 subtasks, a 0% progress bar, and `—` for
  Start, End and Card. **These are the duplicates from item 12**, visible here: the same task
  created twice.
- Footer: `Previous` (disabled) `Page 1/1 · 2` `Next` (disabled), and `Page size 50`.
- **There is no actions column at all** — no edit, no delete; the task name is the only control.

**The human's words, verbatim:**

> Na tela das tasks, o unico ponto é a coluna com o ícone (estilo fusion) de edição da task. Mas
> esse é único grid em que a edição não deve ser inline e sim abrir um popup pra edição, e um
> ícone para exclusão da task

Three things:

1. **Add an actions column** with a Fusion-style **edit** icon.
2. **An icon for deleting the task** as well.
3. **An explicit exception to the system-wide rule of item 2:** *"esse é único grid em que a edição
   não deve ser inline e sim abrir um popup pra edição"*. The tasks grid opens a popup; every other
   grid edits in the row.

> **This is a decision, and a valuable one — it belongs to OQ-35, and I have recorded it there.**
> Item 2 set "inline everywhere" as a system rule; this names the one grid that opts out. Without
> this, OQ-35's spec would have generalised wrongly across every screen. The reason is not stated
> and is worth capturing when it is specced — plausibly because a task carries fields the row
> cannot hold (dates, card, subtasks), but that is my inference, not the human's words.
>
> The actions column itself (items 1 and 2 above) is fix-lane work **only if** the actions it
> triggers already exist: task edit and delete screens do exist today, so an icon column that
> routes to them is presentation. The moment those icons must open the popup instead, it is OQ-35.
