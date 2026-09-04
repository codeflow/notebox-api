# Fix record — grid actions, row banding and the panel's confirmations

**Date:** 2026-09-02 → 2026-09-03
**Satellite:** notebox-web
**Branch:** `fix/grid-actions-and-confirmations` → `develop`
**Verify:** green, 779 tests (768 at the start of the session)

Reported item by item from the running app during a live session. Nothing here came from the
suite: every defect below was invisible to it, and several were invisible *because* of it —
the covered path worked and the uncovered one was dead.

## Items

| # | Reported as | What it actually was | Evidence |
|---|---|---|---|
| 1 | "os datagrids têm cores diferentes para coluna par e ímpar" | The handoff bands **rows**, not columns. The rule existed in our stylesheet since feat-002; nothing emitted the class, so nine grids were flat white. | `rgb(244,247,250)` measured on odd rows, identical to the handoff |
| 2 | "cliquei na linha do grid na exclusão, não exibiu confirmação e foi pra tela de subtask" | The Delete icon was wired to `router.push(/tasks/<id>)` — the same handler as opening the task. Never confirmed, never deleted. | network trace: no DELETE; `confirmacaoNaTela: NENHUMA` |
| 3 | "clico no botão de edit no topo, ele abre o popup mas dá um refresh" | It pushed to the panel's kept route (a `PanelRoute`), costing two navigations to open a panel already mounted in the layout. | `telaRemontou: false` after the fix, path unchanged |
| 4 | (found by audit) | `task-edit` completed with `router.refresh()`, which cannot reach a client-fetched grid. The row kept the old name. | API returned `Teste RENOMEADO`, grid read `Teste` |
| 5 | (found by audit — **a regression introduced in this same session**) | `open` read `pathname` with `[show]` as its only dependency, capturing the hard-load path forever. Lint had warned; I missed it in my own verify output. | panel opened at `/tasks`, returning to `/` left it up |
| 6 | "o popup dentro do popup podia ser o mesmo de confirmação que temos hoje" | The in-panel confirmation had **no title** — the drawer header still read "Groups" while the body asked about deleting. | 9 confirmation surfaces measured; the 3 in-panel ones now match the 6 stacked ones number for number |
| 7 | "precisa ter um espaço maior entre Card Code e Card URL" | `.nb-cardFields` declared `gap: 4px` while the forms space fields at 7px and 8px. | measured 4px → 7px (task) / 8px (subtask) |
| 8 | "uniformiza pra delete" | The subtask grid said "Remove"/"Remover"; every other grid said Delete/Excluir. | verified in both locales in the browser |

## Not done, and why

- **`annotationRecords.delete.action` still reads "Delete record"** while its four siblings read
  "Delete". Same word, more verbose — a different axis from the one reported. Flagged, not changed.
- **Two defects the audit confirmed and left standing**, both needing a product decision:
  closing the panel over a half-built annotation type destroys it silently (`TypeBuilderForm`
  never reports dirty, so INV-P3's guard never arms — it is verified for `task-new` only), and
  the translation catalog's "Restore the product wording" fires an unconditional DELETE with no
  confirmation.

## How the work was checked

A 18-agent adversarial sweep traced **113 user-invocable controls across 7 surfaces** to the body
of the handler that finally runs, then had every suspicion attacked by a verifier instructed to
refute it. Six claims fell. Five survived; two of them are items 4 and 5 above.

Every test written here was killed by a deliberate regression before being kept. Three coverage
guards were added, each carrying its own emptiness check so that breaking the detection fails
the suite rather than passing it vacuously.

## Pipeline note

This batch was implemented before it could be registered: `wf`'s sequential-sibling rule blocks a
new feature while any earlier one is unfinished, and feat-023…028 were all waiting on the human's
GitHub approval. Those five merged on 2026-09-03 and the queue is now clear.
