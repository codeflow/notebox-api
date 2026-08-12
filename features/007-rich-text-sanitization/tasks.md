# Tasks — Rich Free-text sanitization (feat-007, US-2.1 / C-08)

> Decomposition of the approved [plan.md](plan.md) / [contracts/sanitizer.md](contracts/sanitizer.md) /
> [data-model.md](data-model.md), against the approved spec (13 scenarios). Hub feature — `mvn -B verify`
> is the signal. Risk-first: the byte-identity constraint (plan §Risk) is the decision most likely to
> invalidate the plan, so the sanitizer and its corpus come first and alone.

- [x] **T-01 · jsoup + `RichTextSanitizer` port + impl + the shared corpus**
      - files: `pom.xml` (org.jsoup:jsoup), `application/content/RichTextSanitizer.java`, `infrastructure/content/JsoupRichTextSanitizer.java`, `infrastructure/content/JsoupRichTextSanitizerTest.java`
      - covers: C-08 · scenarios: all 6 hostile-markup scenarios + "dialect round-trips byte-identical" + "sanitization is idempotent" (unit level)
      - notes: Safelist = the dialect table exactly; `prettyPrint(false)` + minimal entity escaping; post-pass for colour-only `span[style]` (verbatim when already clean) and http/https-only `a[href]` with enforced `rel`; the test corpus mirrors the client's `sanitize.test.ts` case-for-case, **including the canonical dialect document byte-equal assert** — if jsoup's serializer cannot match, stop and invoke the plan's fallback (parse-don't-reserialize) before building anything on top
      - depends: — · parallel: no  *(the plan-invalidating risk lives here)*
      - verify: `mvn -B test -Dtest=JsoupRichTextSanitizerTest`

- [ ] **T-02 · Write seam — `applyText` sanitizes non-secret FREE_TEXT before bounds**
      - files: `application/annotation/AnnotationRecordService.java` (`applyText`), `application/annotation/AnnotationRecordServiceTest.java`
      - covers: FR-04, C-08 (input half), INV-S1/S6 · scenarios: "An update is sanitized exactly like a create", "Plain TEXT values are not treated as markup", "Secret values keep their shipped behaviour" (service level), "The length bound applies to the stored form"
      - notes: sanitize **before** the length check so `annotation.record.value.too_long` fires on the stored form (existing key — no i18n change); TEXT and secret values must be proven to bypass the sanitizer, not merely assumed
      - depends: T-01 · parallel: no
      - verify: `mvn -B test -Dtest=AnnotationRecordServiceTest`

- [ ] **T-03 · Read seam — DTO-only sanitization + legacy row + full verify**
      - files: `api/dto/AnnotationRecordDto.java`, `api/dto/AnnotationValueDto.java` (sanitizer parameter), `api/AnnotationRecordResource.java` (3 call sites), `api/AnnotationRecordResourceTest.java`
      - covers: C-08 (output half), INV-S2/S5 · scenarios: "A legacy hostile row cannot reach a client", hostile POST → sanitized 201 body (wire), "Secret values keep their shipped behaviour" (wire — masking/reveal untouched)
      - notes: legacy row planted via native SQL, then GET → inert body **and** a native re-read asserting the stored bytes are unchanged (INV-S5 — the `@Transactional` read must not flush an UPDATE); closes with the hub's **full `mvn -B verify`** (Docker required) proving the existing 133 tests plus the new ones green
      - depends: T-01, T-02 · parallel: no  *(final verify must see the whole feature)*
      - verify: `mvn -B verify`

## Coverage & sequencing
- **13/13 spec scenarios covered:** hostile markup ×6 (T-01 unit + T-03 wire), byte-identity + idempotence (T-01), update-like-create + TEXT/secret/bounds pins (T-02), legacy row + wire pins (T-03).
- **Dependency chain:** T-01 → T-02 → T-03. No parallelism — three small serial tasks; worktrees would cost more than they save.
- **Uncovered scenarios:** none.
