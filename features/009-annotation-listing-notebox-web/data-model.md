# Data model — Records grid (feat-009)

Client-only; no storage. Grid state and invariants:

```ts
interface GridState { page: number; size: 50 | 100 | 200; filter: Record<string, string>; }
```

- **INV-G1 · Served order is never reordered.** The grid renders `PageDto.items` as received
  (newest first is the server's contract, OQ-20).
- **INV-G2 · Pager facts come only from `PageDto`.** No client-side count invention; boundary
  arithmetic (`total 0`, exact multiples, last page) is pure derivation.
- **INV-G3 · The QBE filter never triggers a request.** It is a predicate over the loaded page,
  with the page-local note visible while active.
- **INV-G4 · Rich values enter cells only through `plainTextPreview` (a string).** No `innerHTML`
  path exists in the grid; the detail's `RichTextValue` remains the only rich renderer.
- **INV-G5 · No reveal from the grid.** Secret cells render the mask key; `RevealableValue` is not
  imported by any grid file.
