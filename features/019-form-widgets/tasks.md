# Tasks — Form widgets and states

**ID:** features/019-form-widgets · **Date:** 2026-08-27 · **Status:** Approved (standing authorisation)

- [x] **T-01 · Spin buttons and the choice group** ✔ 2026-08-27 — `ValueField` number branch becomes an
      `af-spinBox` with clamped `af-spinButtons`; choice wrappers gain `af-choiceGroup`. Five new tests
      including both bound clamps and the empty-field start. The 10 pre-existing `ValueField` tests
      passed **unmodified**.
      - covers: scenarios 1–3 · verify: `npx vitest run components/annotationRecords/ValueField.test.tsx` → 15 passed

- [x] **T-02 · Notes drawer on the task detail screen** ✔ 2026-08-27 — closed by default, dock tab with
      `aria-expanded`/`aria-controls`, body stating notes are not stored.
      - covers: scenarios 4–5 · verify: `npx vitest run components/tasks/NotesDrawer.test.tsx`

- [x] **T-03 · Verify and live pass** ✔ 2026-08-27 — `npm run verify` green: **70 files, 516 tests**
      (508 → 516), build compiled.
