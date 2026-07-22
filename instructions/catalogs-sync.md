---
step: catalogs
model: sonnet
effort: medium
reads: [prd/PRD_v*.md, constitution/*.md]
writes: [catalogs/*.md]
gate: none
---

# Derive the catalogs

**Goal.** Flatten the PRD into four lookup tables so downstream steps read a row instead of a
whole document. This is a mechanical projection, not a creative step — hence `sonnet/medium`.

## The four catalogs

| File | Holds | Id |
|---|---|---|
| `requirements.md` | Functional + non-functional requirements | `FR-NN`, `NFR-NN` |
| `rules.md` | Business rules, mirrored from the constitution | `BR-NN` |
| `epics.md` | Epics and their user stories | `E-N`, `US-X.Y` |
| `open-questions.md` | Everything unresolved, with what it blocks | `OQ-NN` |

## Procedure

1. Project every PRD requirement into `requirements.md` with: id, statement, origin, status,
   and the US that will deliver it.
2. Group the requirements into epics, then decompose each epic into user stories.
   A user story is deliverable in one feature. If it is not, split it.
3. In `epics.md`, each `US-X.Y` row carries: summary, the FRs it satisfies, status
   (`todo | speccing | building | delivered | deferred`), and its feature directory once allocated.
4. Mirror the constitution's BRs into `rules.md` as references — do not restate their text, link
   the id. Two copies of a rule drift.
5. Carry forward every OQ. Never close one without recording who answered it and when.

## Id discipline

Ids are immutable. A dropped item becomes `deprecated`, keeping its number. Reusing a number
silently corrupts every artifact that cited it — including ones already merged.

## Done when

- Every FR maps to at least one US, or is explicitly marked deferred.
- Every US maps back to at least one FR. A US with no FR means the PRD is missing something.
- `wf validate` still passes.
