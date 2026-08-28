# Visual pass — screen by screen, against the handoff

**Date:** 2026-08-28 · **Method:** load each screen in a browser, read computed geometry and the
rendered column set, compare against the handoff's own markup for that screen.

> **Why this exists.** `features/_design-conformance-report.md` diffed CLASS INVENTORIES. That method
> found 12 missing widgets and was worth running — but it cannot see a screen whose widgets all exist
> and whose layout is wrong. It passed over **six** real defects, listed below. Its own preamble said
> it would; this is the receipt.

## What the class diff missed, and looking found

| # | Screen | Defect | Why the diff could not see it |
|---|---|---|---|
| 1 | 02 login | centred single pane, not the two-pane card | every widget present |
| 2 | 02 login | labels right-aligned; card shrank to 590px | CSS only — jsdom loads no stylesheet |
| 3 | 12 record detail | `dt`/`dd` wrapped in a `div`, so the two-column grid laid out two fields per row | `af-nameValue` was in use |
| 4 | 07 types list | headerless two-column table where the design has seven columns | `af-table` was in use |
| 5 | 11 / 15 grids | no Group column; no Subtasks column | no class is "missing" when a column is absent |
| 6 | 16 task detail | card panel overflowed its own panel by 36px; dock drawn over it | `flex: none` sized to content |

**Four of the six needed no new data.** Two did, and got it: a record count per type and a subtask
count per task, both as one grouped query per page.

## Screens checked

| Screen | Verdict |
|---|---|
| 02 / 03 login | ✅ 700×330 measured, labels above and left, adornments inert |
| 05 workspace home | ✅ chrome complete; summary boxes built (OQ-31) |
| 06 / 07 types list | ✅ fixed — 7 columns with a header, counts, formatted date |
| 08 type builder | ✅ fixed — each field a card, sub-header present |
| 09 type detail | ✅ |
| 10 delete dialog | ✅ |
| 11 records grid | ✅ fixed — Group and Actions columns; contained horizontal scroll |
| 12 record detail | ✅ fixed — real two-column name/value grid |
| 13 record form | ✅ fixed — 7px rows, shared label column, ruled action bar |
| 14 groups | ✅ (shuttle deferred by prior decision) |
| 15 tasks list | ✅ fixed — Group and Subtasks columns |
| 16 task detail | ✅ fixed — overflow and dock lane |
| 17 message catalog | ✅ built (FR-16) |
| 18 pt locale | ✅ every screen renders from the catalog |
| 19 error states | ✅ `af-messages` on the catalog screen; validation paths unchanged |

## The standing lesson

Three separate methods were used on this feature: a class-inventory diff, a unit suite of 544 tests,
and looking at the running app. **The first two agreed the screens were fine. Only the third was
right.** Any future conformance claim should say which of the three backs it.
