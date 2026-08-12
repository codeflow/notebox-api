# Data model — Record listing (feat-008)

**No schema change, no migration.** Two invariants and one config note:

- **INV-L1 · Order is contractual.** `createdAt DESC, id DESC` — the id tiebreak makes pagination
  total under equal timestamps (OQ-20).
- **INV-L2 · Projection is display-only.** Row filtering to `visibleForViewing` fields shapes the
  grid (BR-09); the detail read returns all fields unchanged. No value rule is bypassed on the new
  path: masked secrets stay masked, rich text passes the feat-007 read seam.
- **Config:** `hibernate.default_batch_fetch_size=64` (via quarkus unsupported-properties) — batches
  the lazy value/option loads of a page; reversible; global.
