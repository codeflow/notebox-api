# Audit — Annotation records (feat-005, US-2.1)

**Step:** `feat-005-annotation-records-notebox-api.audit` · **Model/effort:** opus/xhigh · **Date:** 2026-07-25
**Auditor mandate:** try to prove the feature is *not* done. Six checks below plus seven targeted risk
probes, each reported explicitly.
**Diff audited:** `develop...feature/annotation-records` @ `840562e` (9 commits, 46 files).
`mvn -B verify` is green (94 tests). Verify-green is not the question — the question is whether
spec→scenario→test→code is intact and whether behaviour would actually fail on regression.

**Method note.** Findings F1–F5, F7 and the positive results in §Verified-good were reproduced empirically
with four throwaway probe tests run against the real MySQL container (`mvn -B test -Dtest=…`), then deleted;
the repository is unchanged by this audit (only the coordinator's own uncommitted `workflow.json` state
edit remains). Observed request/response pairs are quoted verbatim below.

## Verdict: **FAIL**

The core of the feature is genuinely sound: secret values really are ciphertext at rest (verified against
the raw MySQL row), masking really holds at the wire, the reveal role gate and audit really work, tenant
isolation really works, and the OQ-14 guard really blocks. But three independent request shapes return
**HTTP 500 with a leaked stack trace** instead of a localized `Problem` — the exact defect class that
verdicted feat-003's first pass FAIL — and the `PUT` contract makes it **impossible to edit a record that
holds a secret without silently destroying that secret**. Both must be fixed before this contract is
published to feat-006.

**Findings: 5 HIGH · 4 MEDIUM · 5 LOW.** Reopen **`implement`** (F1–F3, F6, F9–F11, F13) and **`spec`**
(F4, F5/OQ-17, F12 — they are contract decisions, not code bugs).

---

## Findings

### F1 — HIGH (blocking) · Two values for the same field → HTTP 500 + stack trace

`spec.md` §Scope ("an unordered set of **values**, at most one per defined field") and `data-model.md`
("at most one value per field id") declare an invariant that **nothing enforces**.
`AnnotationRecordService.toValues` iterates the input list without de-duplicating, and
`AnnotationRecordDto.from:22-23` then reduces the values with
`Collectors.toMap(AnnotationValue::getTypeFieldId, …)`, which throws on a duplicate key.

**Concrete failure.** `POST /api/annotation-records`
`{"annotationTypeId":"…","name":"dup","values":[{"fieldId":"F","text":"a"},{"fieldId":"F","text":"b"}]}`
→ observed **`500`** with body
`{"details":"… java.lang.IllegalStateException: Duplicate key 21ba485b-… (attempted merging values
com.notebox.api.domain.AnnotationValue@63ffb7c7 and …)","stack":"java.lang.IllegalStateException: …"}`.

Breaks constitution 03 §Errors on two counts — *"One error shape on the wire"* and *"Never leak stack
traces, SQL, or entity internals"* — and leaks entity class names and identity hashes. (The `stack`/
`details` body is Quarkus' dev/test error handler; in `%prod` the body is only an error id, but the
off-contract **500 itself** is production-visible and the client gets no `code` to act on.)
**Right fix:** a declarative edge constraint (constitution 03 prefers custom constraint annotations, e.g.
`@AtMostOneValuePerField`) → 400 with a dot-namespaced key.

### F2 — HIGH (blocking) · Record name over 120 chars → HTTP 500 + stack trace

`data-model.md` specifies `name` as `@NotBlank`, **`@Size(max=120)`**. `AnnotationRecordInput:14` carries
only `@NotBlank`; the column is `VARCHAR(120)`.

**Concrete failure.** `POST /api/annotation-records` with `"name"` = `"x".repeat(200)` → observed
**`500`**; server log `Data truncation: Data too long for column 'name' at row 1 [insert into
annotation_record …]`. A planned validation simply never happened. (feat-003's `AnnotationTypeInput` shares
the omission — pre-existing — but here it was explicitly specified in the approved data model.)

### F3 — HIGH (blocking) · A long secret value overflows the ciphertext column → HTTP 500

`secret_ciphertext` is `VARBINARY(4096)`; `data-model.md` says it is "sized for the GCM-encrypted form of a
**bounded** secret text value" — but no bound on `AnnotationValueInput.text` was ever added.

**Concrete failure.** `POST /api/annotation-records` with a Secret TEXT value of 5 000 chars → observed
**`500`**; log `Data truncation: Data too long for column 'secret_ciphertext' at row 1`. The same family
covers a non-secret value over 65 535 bytes against `text_value TEXT`. User-visible effect: a paste of a
long PEM/token into a Secret field fails with an opaque server error.

### F4 — HIGH (blocking) · `PUT` silently destroys a secret value, and there is no way to preserve it

`AnnotationRecordService.update` → `record.replaceValues(...)`: full-replacement semantics. A client can
never resend a secret, because the ordinary read masks it (`text:null`, by design). Both available client
behaviours are wrong:

**Concrete failure A (silent data loss).** `GET /api/annotation-records/{id}` returns
`…{"fieldId":"KEY","fieldType":"TEXT","secret":true,"masked":true,"text":null}`. The client edits the name
and PUTs back the payload it can construct — the URL value only —
`PUT {"name":"prod-renamed","values":[{"fieldId":"URL","text":"amqp://h"}]}` → observed **`200`**, response
`values` now holds one entry. The secret is gone: `POST …/values/KEY/reveal` → **`400
{"code":"annotation.record.reveal.not_secret"}`**. No audit entry, no warning, unrecoverable.

**Concrete failure B (cannot save at all).** If instead the client echoes the masked value back —
`PUT {"name":"prod2","values":[{"fieldId":"KEY","text":null}]}` → observed **`400
{"code":"annotation.record.value.type_mismatch"}`**. So the record cannot be renamed at all while keeping
its secret.

This breaches **BR-05** ("the system never deletes such data as a silent side effect of another
operation") and the accountability half of **BR-10/C-10** (the destruction of a secret is not audited),
and it hands feat-006 an unimplementable edit form. It is a **spec-level** gap: the 17 scenarios contain
no "update a record that holds a secret value" case, and `contracts/rest-api.md` is silent. A decision is
required (preserve-on-omit sentinel, explicit `"clearSecret":true`, or PATCH), not just a patch.

### F5 — HIGH · Editing an annotation type silently wipes every record's values (OQ-17 under-rated)

`AnnotationTypeService.replace` → `replaceFields(toFields(...))` builds **new `TypeField` instances**, so
`@PrePersist` assigns **new UUIDs** on every type PUT. Record values join to the type by
`AnnotationValue.typeFieldId` (a soft reference — no FK, per `data-model.md`), so every value of every
record of that type is instantly orphaned.

**Concrete failure.** Create type `{URL:TEXT, "API key":TEXT secret}`, create a record with both values,
then `PUT /api/annotation-types/{id}` with a **byte-identical field definition** (a no-op edit from the
user's point of view) → observed `200`. Then `GET /api/annotation-records/{recordId}` → observed
**`200 {… "name":"prod","values":[]}`** — every value, including the secret, has vanished from every read.
The orphaned ciphertext row survives and is still revealable through the **stale** field id
(`POST …/values/{oldFieldId}/reveal` → `200 {"value":"s3cr3t-token"}`), an id the client can no longer
discover. And `DELETE /api/annotation-types/{id}` then returns **`409 annotation.type.has_records`** — the
OQ-14 guard now locks the user into an empty-looking record they cannot clear by deleting the type.

Two aggravating details: the type PUT response returns `fields[].id = null` (the DTO is built before
flush — a pre-existing feat-003 defect, see §Out of scope), so a client cannot even re-map ids from the
response; and the OQ-14 decision the human made specifically to protect records is fully bypassable by a
type PUT. The plan *did* record this in §Risk and opened **OQ-17** — the process was followed — but OQ-17
is rated **🟢 Tactical**, its Description/Suggested-path fields are unfilled template placeholders, and no
interim guard ships. Silent, total, unrecoverable loss of the data US-2.1 exists to store is not tactical.

### F6 — MEDIUM · The reveal audit entry does not record *which* value was revealed

Spec: *"an audit entry records who revealed **which value** on which record and when."*
`AuditLog` (`domain/AuditLog.java`) has only `action / targetType / targetId / actorUserId / at` — no
detail column — and `AnnotationRecordService:128-129` passes `targetId = recordId`. The `fieldId` is
dropped.

**Concrete failure.** An admin reveals the `API key` of record R, then the `DB password` of the same
record R. The audit trail holds two rows identical except for `at`. A forensic question "who saw the DB
password?" is unanswerable — exactly the accountability BR-10 requires. `AnnotationRecordRevealTest` only
asserts `countForTarget(recordId) == 1`, so this cannot fail a test either.

### F7 — MEDIUM · Entity mapping does not match the V3 migration, and the prod guard against that is a dead config key

`AnnotationValue.textValue` is `@Lob String` with no `length`; the migration declares `text_value TEXT`.
Proven by running a feat-005 test with Hibernate validation enabled:

```
Schema-validation: wrong column type encountered in column [text_value] in table [annotation_value];
found [text (Types#LONGVARCHAR)], but expecting [tinytext (Types#CLOB)]
```

Two consequences. (a) If schema validation is ever switched on, the application **fails to start**.
(b) If Hibernate ever generates this schema (any environment not driven by Flyway), it creates
`tinytext` — 255 bytes — and a 300-character Free-text value fails, defeating the `TEXT`/65 535 intent of
`data-model.md`. Everything else in V3 matches the entities (names, nullability, CHAR(36)/DATETIME(6)/
DECIMAL(38,10)/VARBINARY, `ON DELETE CASCADE` mirroring `orphanRemoval`).

**Pre-existing amplifier (out of feat-005's scope, worth escalating):**
`application.properties:15` sets `%prod.quarkus.hibernate-orm.schema-management.strategy=validate`, but
Quarkus **3.15.1** does not know that key — the probe run printed
`Unrecognized configuration key "quarkus.hibernate-orm.schema-management.strategy" was provided; it will
be ignored`. The correct key here is `quarkus.hibernate-orm.database.generation`. So prod runs with **no**
entity/schema validation at all, which is why this drift shipped unnoticed.

### F8 — MEDIUM · Two compliance evidences claimed by the spec do not exist

- **C-07** claims *"reuse of the validated image path + content-type assertion."* **No test in this
  feature touches an Image-field value.** `AnnotationRecordService.applyImage` (and therefore
  `annotation.record.value.image.not_found`, which the i18n test only proves *exists as a string*) is
  entirely unexercised: an inverted `if (!images.existsInTenant(...))` would ship green.
- **C-01** claims cross-tenant tests for *"create/read/update/delete/**reveal**."* There is **no
  cross-tenant reveal test.** I verified the behaviour by hand — a foreign tenant's ADMIN gets
  `404 annotation.record.not_found` — so the code is right, but the claimed guard is absent.

Also unexercised by any feat-005 test: the `FREE_TEXT` and `LIST` field kinds (LIST rides on the
SINGLE_CHOICE branch), and the cipher's documented startup fail-fast on a missing active key.

### F9 — MEDIUM · Empty `MULTIPLE_CHOICE` selection rejected, contradicting the approved contract

`contracts/rest-api.md` (`optionIds` — "MULTIPLE_CHOICE (0..n)") and `data-model.md`
("MULTIPLE_CHOICE = 0..n") both allow an empty set; `applyOptions:202` rejects it
(`optionIds.isEmpty() → type_mismatch`) because SINGLE_CHOICE and MULTIPLE_CHOICE share one guard.

**Concrete failure.** `POST` a record with `{"fieldId":"TAGS","optionIds":[]}` → observed **`400
annotation.record.value.type_mismatch`**. A feat-006 form that submits an empty multi-select (the natural
"no tags" state) cannot save. Either the code or both approved artifacts is wrong; they cannot both stand.

### F10 — LOW · Test-honesty gaps (none tautological, but several under-asserted)

No test in the diff asserts on a mock or on itself; all hit the real DB. But:
- `create_persistsValuesForDefinedFields` asserts only `record.getValues().size() == 3` — a regression
  that wrote the right *number* of wrong values (e.g. field/payload mis-dispatch) passes.
- `update_changesARecordsValues` asserts `getValues().get(0)` is 5673 but **never asserts
  `size() == 1`**. Since `replaceValues` mints new value rows, an orphan-removal regression would leave
  two rows and the assertion would pass or fail by list order — a flaky test, not a guard. (Orphan
  removal does work: I confirmed a replaced value's row is really gone in a fresh transaction.)
- `delete_removesTheRecordAndWritesOneAuditEntry` deletes a record created with `List.of()` values, so the
  scenario clause *"the record **and its values** are removed"* is never exercised.
- Both audit assertions use `countForTarget(id)` only; the `action` string
  (`ANNOTATION_RECORD_SECRET_REVEALED` vs `…_DELETED`) is never asserted, so mislabelling is invisible.
- The field-unknown scenario's second clause, *"the message is localized to the caller's locale"*, has no
  wire test (no request with `Accept-Language: pt` asserting the pt message on any record error).
- The wire masking test asserts the secret field's `text` is null but not that the **whole body** is free
  of the cleartext. (I checked: it is — `cleartext-in-read=false`.)

### F11 — LOW · Master-key length is never validated → silent AES-128

`AesGcmSecretValueCipher.keyFor` base64-decodes the configured key and wraps it in `SecretKeySpec` with no
length check. **Concrete failure:** an operator sets `NOTEBOX_CRYPTO_KEY_1` to a base64 16-byte value; the
app starts, encrypts happily with **AES-128**-GCM, and AD-14's "AES-256" is silently violated with no
signal anywhere. (The committed dev default is correct — 32 bytes.)

### F12 — LOW · Reveal keys off ciphertext presence, not the field's Secret flag

`reveal` checks `value.isSecret()` (i.e. "a ciphertext exists"), never `field.isSecret()`.
**Concrete failure:** a TEXT field "Notes" holds the plaintext `hello`; an admin later flags that field
Secret via the type PUT. Ordinary reads now return `masked:true, text:null` (masking keys off the *field*),
while `reveal` returns `400 annotation.record.reveal.not_secret` (keying off the *value*). The value is
permanently unreadable through any endpoint while its **cleartext stays in `text_value`** — a BR-10 hole
(a value in a Secret-flagged field persisted in cleartext) that neither the spec nor OQ-17 covers.
Mirrored the other way: un-flagging a field makes an existing ciphertext value invisible on read yet still
revealable.

### F13 — LOW · `optionIds:[null]` → NPE → 500

`AnnotationValueInput.optionIdsOrEmpty()` uses `Set.copyOf`, which NPEs on a null element (it tolerates
duplicates, so that path is safe). Same envelope-breach family as F1–F3, lower likelihood.

### F14 — LOW · Artifact hygiene

- **OQ-17 was committed with unfilled template placeholders**: `**Description:** <what is unknown and why
  it matters>` and `**Suggested path:** <how to resolve>` (`catalogs/open-questions.md`). The content lives
  only in the *Impact* line. An OQ that does not state its own question is not actionable.
- `spec.md` still reads `**Status:** Draft` although commit `02f4792` records it as approved.
- `plan.md` §Blast radius names 3 new domain exceptions; 9 shipped (the extra 6 are required by the
  approved error table — the **plan was incomplete**, this is not scope creep). It also claims "i18n
  coverage test picks up new keys automatically", whereas a new `AnnotationRecordMessageCoverageTest` with
  a **hardcoded** key list was written (as `tasks.md` T-03 correctly specifies) — so a *future* key added
  without editing that list is not covered.

---

## Check-by-check results

### 1. Traceability — FAIL (no scenario is untested; three clauses are)

Every FR reaches code and tests: **FR-04** → `AnnotationRecordResource.create/get/replace` →
`AnnotationRecordService.create/get/update` → `toValues/toValue/apply*` → `AnnotationRecordServiceTest`
(7 tests) + `AnnotationRecordResourceTest` + `AnnotationRecordRepositoryTest`. **FR-06** →
`AnnotationRecordService.delete` + `AuditLog` → `AnnotationRecordDeleteTest`. **FR-18** →
`applyText`/`AesGcmSecretValueCipher` (write), `AnnotationValueDto.from` (mask), `reveal` (disclose) →
`AesGcmSecretValueCipherTest`, `AnnotationRecordRevealTest`, `AnnotationRecordResourceTest`.

| # | Scenario | Test method | Production path | Verdict |
|---|---|---|---|---|
| 1 | Create with values for defined fields | `AnnotationRecordServiceTest.create_persistsValuesForDefinedFields` + `…ResourceTest.create_thenReadMasksTheSecretValue` | `create` → `toValues` → `persistInTenant` | weak assert (F10) |
| 2 | Reject value for undefined field | `create_rejectsAValueForAFieldTheTypeDoesNotDefine` | `toValues:144` → `AnnotationRecordFieldUnknownException` | ok; localization clause untested (F10) |
| 3 | Reject data ≠ field type | `create_rejectsAValueWhoseDataDoesNotMatchTheFieldType` | `applyNumber:179` | ok |
| 4 | Number outside bounds | `create_rejectsANumberValueOutsideTheFieldBounds` | `applyNumber:183-185` | ok |
| 5 | Choice must be a predefined option | `create_choiceValueMustReferenceAPredefinedOption` | `applyOptions:205-208` | ok |
| 6 | Multiple choice accepts several | `create_multipleChoiceAcceptsSeveralPredefinedOptions` | `applyOptions(…, MAX_VALUE)` | ok (but F9 for the empty case) |
| 7 | Reject empty name | `…ResourceTest.blankNameIsRejectedWithLocalizedViolation` | `@NotBlank` → `ConstraintViolationMapper` | ok (but F2 for the max case) |
| 8 | Update changes values | `update_changesARecordsValues` | `update` → `replaceValues` | under-asserted (F10) |
| 9 | Delete removes record + values, audited | `AnnotationRecordDeleteTest.delete_removesTheRecordAndWritesOneAuditEntry` | `delete` → `remove` + `AuditLog` | "…and its values" unexercised (F10) |
| 10 | Secret stored as ciphertext | `AesGcmSecretValueCipherTest.encrypt_producesCiphertext…` + `get_neverDecryptsASecretValue…` | `applyText` → `cipher.encrypt` → `setSecret` | property true (verified in DB) but no test reads the stored row |
| 11 | Ordinary read masks the secret | `…ResourceTest.create_thenReadMasksTheSecretValue` | `AnnotationValueDto.from:25-27` | ok |
| 12 | Reveal to elevated role, audited | `AnnotationRecordRevealTest.reveal_returnsCleartextToAdmin…` + resource test | `reveal` → `cipher.decrypt` + `AuditLog` | ok, but "which value" not audited (F6) |
| 13 | Reveal denied to a plain member | `reveal_isDeniedToAPlainMember` + resource test (403 + code) | `reveal:115-117` | ok |
| 14 | Type delete blocked while records exist | `AnnotationTypeDeleteGuardTest.delete_isRejectedWhileTheTypeOwnsRecords` | `AnnotationTypeService.delete` → `existsByType` | ok |
| 15 | Empty type still deletable | `delete_stillSucceedsForAnEmptyType` | same | ok |
| 16 | Cannot create against another tenant's type | `foreignTenantCannotCreateAgainstAnotherTenantsType` | `requireType` → `findByIdInTenant` | ok ("no record created" implied by rollback, not asserted) |
| 17 | Cannot read/update/delete another tenant's record | `foreignTenantCannotReadUpdateOrDeleteARecord` + `AnnotationRecordRepositoryTest.crossTenantReadReturnsEmpty` | `get` → `findByIdInTenant` | ok (asserts `annotation.record.not_found`, not an incidental 404) |

**Scenarios with NO genuine test: none.** The chain is unbroken for all 17. What fails this check is the
inverse direction: behaviour the artifacts *promise* with no test and, in three cases, no code —
F1 (at-most-one-value invariant), F2 (`@Size(max=120)`), F8 (Image values), F9 (`MULTIPLE_CHOICE` 0..n).

### 2. Scenario honesty — PASS with findings

No test in the diff asserts on a mock, asserts a tautology, or restates the implementation. Every service
test drives the real `AnnotationRecordService` against a real MySQL (`@TestTransaction` + `em.flush()/
clear()` so reads come from the database, not the persistence context), and the e2e tests go over HTTP
with real JWTs and two real tenants. Specifically, as instructed:

- **Ciphertext at rest — property TRUE, test only indirect.** No committed test reads the stored column;
  the claim rests on a cipher unit test (`ciphertext != plaintext` on the cipher's *return value*) plus
  `get_neverDecryptsASecretValue…` (after `flush`+`clear`, the reloaded value has `isSecret()==true` and
  `getTextValue()==null`). I closed the gap myself with a native query on the real row:
  `text_value=[null] ciphertext_len=28 iv_len=12 key_version=1 ciphertext_contains_plaintext=false`.
  **C-12's core property holds in the database** — but a regression that reintroduced a cleartext column
  (e.g. a "searchable value" added for US-2.2's listing) would ship green, because nothing asserts on the
  raw row.
- **Masking — honest.** The wire test asserts `masked:true` **and** `text:null` for the secret field
  **and** the plaintext value of a sibling non-secret field, so it fails if masking is dropped *or* if
  masking accidentally blanks everything. I additionally confirmed the whole GET body contains no
  cleartext. Not asserted: the whole-body property itself.
- **Reveal audit — honest.** `auditLog.countForTarget(recordId)` is a real JPA count over the real table,
  and the negative test asserts `0` rows on a forbidden reveal, so both the write and the
  same-transaction ordering would fail on regression. Weaknesses: the `action` string is unasserted
  (F10) and the `fieldId` is simply not recorded (F6).
- **Cross-tenant — honest, not incidental 404s.** The repository test persists a record for tenant B and
  then reads it as tenant A through the same choke point (isolation, not a wrong id); the e2e test uses a
  record it just created successfully and asserts the foreign tenant gets `404` **with `code =
  annotation.record.not_found`**. The PUT/DELETE legs assert status only, but the record demonstrably
  exists, so a 404 can only be the tenant predicate.

### 3. Constitution — FAIL (BR-05/BR-10 via F4; 03-code-standards §Errors via F1–F3)

- **BR-03 (records conform to their type) — held for the declared cases.** `toValues` resolves every
  input `fieldId` against `type.getFields()` and rejects the unknown; `toValue` dispatches on
  `fieldType` across all seven kinds with no default fall-through. Gaps: the at-most-one-value-per-field
  half of the rule is unenforced (F1), and BR-03 is silently *broken by data* after a type edit (F5).
- **BR-05 (explicit, irreversible delete) — VIOLATED by F4.** Record delete itself is exemplary: a
  dedicated `DELETE`, re-fetched through the tenant choke point, `em.remove` + `AuditLog` in one
  `@Transactional`. But `PUT` destroys a secret value as a silent, unaudited side effect.
- **BR-10 (secret values confidential at rest) — held at rest, holed at the edges.** Cleartext is never
  persisted (verified on the raw row) and never logged; F12 describes the flag-flip hole, F4 the
  accountability hole.
- **AD-03 — PASS (grep-verified).** `AnnotationRecordRepository extends TenantScopedRepository`; the only
  query it adds, `existsByType`, filters `e.tenantId = :tenant` from `TenantContext`. No `EntityManager`
  or JPQL appears anywhere outside `infrastructure/persistence/` in the diff; `AnnotationValue` is
  aggregate-internal with no `tenant_id` and no repository, reachable only through the root. Tenant is
  read exclusively from `TenantContext`, never from a request body.
- **AD-14 boundary — PASS (grep-verified).** `javax.crypto.*`, `Cipher`, `SecretKeySpec`,
  `GCMParameterSpec` and every `notebox.crypto.*` key read appear **only** in
  `infrastructure/security/AesGcmSecretValueCipher.java`. `application/crypto/` holds a pure port plus a
  DTO record; `domain` has no crypto import and no key access. The `application`→`infrastructure`
  direction is through the interface (`AnnotationRecordService` depends on `SecretValueCipher`).
  No logger exists in any new class, nothing prints a plaintext, exception messages are constant
  strings ("failed to encrypt/decrypt secret value"), no cache is involved, and the OpenAPI document
  contains no secret sample (checked: `openapi-contains-s3cr3t=false`). Note: `%prod` values keep
  `quarkus.hibernate-orm.log.sql=false`, so no bind parameters reach the log. *(`application/auth/
  PasswordHasher` uses BouncyCastle Argon2 in `application/` — pre-existing feat-001, not master-key
  crypto, out of AD-14's scope.)*
- **03-code-standards — MIXED.** i18n keys are lowercase dot-namespaced (`annotation.record.value.
  number.out_of_bounds`), never SCREAMING_SNAKE; the only enum-ish addition is `ErrorCategory.FORBIDDEN`,
  which is a Java enum constant, not a wire key. Nine **specific** domain exceptions extend the
  `DomainException` base, one per condition — no catch-all with a string code. `@Transactional` on all
  writes; mapping only in `@Provider` mappers; DTOs carry no `byte[]`. **Fails §Errors** on F1–F3 (500 +
  stack trace + no wire `code`) and on the constitution's stated preference for *declarative* Bean
  Validation with custom constraint annotations at the edge for exactly the invariants F1/F2 name.
  Comments: `minimal` scope respected — every new public class and non-trivial method has a one-line
  Javadoc stating what and why, no narration; but **no `@param`/`@return`/`@throws` anywhere** in the
  new code, which §Comments requires ("Public surface carries Javadoc with `@param`/`@return`/`@throws`
  filled"). This is uniform pre-existing drift (feat-001/003 are identical and passed audit), so it is
  reported as precedent looseness, not a feat-005 regression.

### 4. Compliance — FAIL (C-07 and C-01's reveal leg have no evidence; C-09/C-05/C-12 pass)

| Item | Claimed evidence | Verified? |
|---|---|---|
| C-01 tenant isolation | cross-tenant tests create/read/update/delete/**reveal** | **partial** — reveal leg missing (F8); the rest present and honest |
| C-02 authenticated by default | 401 on unauthenticated | **yes** — `@Authenticated` on the class; `unauthenticatedIsRejected` asserts 401 |
| C-03 least privilege | reveal allowed to role, forbidden to member | **yes** — service test + e2e 403 with the localized code |
| C-05 secrets never committed | `.env.example` entry + no key in logs | **yes** — see below |
| C-06 encryption in transit | deployment/ingress (declared cross-cutting) | **not verifiable in repo** — no ingress/deploy config exists; accepted only because the spec itself scopes it out |
| C-07 image upload safety | reuse of validated image path + content-type assertion | **NO** — no test touches an Image-field value (F8) |
| C-09 localization completeness | per-locale catalog check | **yes** — all 10 new keys (9 `annotation.record.*` + `annotation.type.has_records`) present in **both** `messages.properties` and `messages_pt.properties`, and `AnnotationRecordMessageCoverageTest` asserts each in both. Weakness: hardcoded list (F14) and no wire test of a pt message (F10) |
| C-10 audit trail | audit entry + test for delete and reveal | **yes** for existence/atomicity; **incomplete content** (F6) |
| C-11 retention/deletion path | delete removes record + values | **partial** — delete works and DB `ON DELETE CASCADE` + JPA cascade both cover values, but no test covers a record *with* values (F10) |
| C-12 encryption at rest + reveal authz/audit | ciphertext-at-rest test + reveal authz/audit test | **property yes, evidence partial** — I confirmed on the raw MySQL row; no committed test reads the stored bytes (see check 2) |

**C-05 in detail — no real key material is committed.** `application.properties:53-55` ships
`notebox.crypto.keys.1=${NOTEBOX_CRYPTO_KEY_1:5+PDNZ…1s8=}` — a 32-byte base64 dev/test default, directly
above the comment "dev/test default only — %prod overrides from the secret manager via env (C-05). Never
commit a real key" — plus `%prod.notebox.crypto.keys.1=${NOTEBOX_CRYPTO_KEY_1}` with **no fallback**, so
prod cannot start on the committed key (the constructor resolves the active key eagerly). `.env.example`
adds `NOTEBOX_CRYPTO_KEY_1=change-me-base64-aes-256-key`. This mirrors the accepted JWT dev-key precedent
exactly and is what the approved contract prescribed. Residual: any *non-prod* environment that forgets
the env var encrypts under a key that is public in git (inherent to the pattern), and F11 (no length
check).

### 5. Scope — PASS with a documented plan gap

Every `src/` file in the diff is in the plan's blast radius, and every planned file exists. No unplanned
production file was touched: the only non-artifact edits outside the new files are exactly the four the
plan names (`AnnotationTypeService.delete` +guard, `ErrorCategory` +FORBIDDEN, `DomainExceptionMapper`
+403, `application.properties`/`.env.example` +crypto) plus `OpenApiCoverageTest`. `catalogs/
open-questions.md` (OQ-17) and `workflow.json` are bookkeeping.

**Planned but not delivered:** `@Size(max=120)` on the record name (F2); the "at most one value per field"
invariant (F1); the bounded secret value implied by the `VARBINARY(4096)` sizing note (F3); the
`MULTIPLE_CHOICE` 0..n rule (F9); the plan's claim that the existing i18n test picks up keys automatically
(F14). **Under-specified rather than crept:** 9 domain exceptions vs 3 listed (F14). Two intentional,
harmless deviations: the image key is the new `annotation.record.value.image.not_found` rather than reusing
`annotation.image.not_found` (the approved contract table says the new key — contract wins), and
`AnnotationValue` has no `@OrderColumn` (values are an unordered set, ordered at DTO assembly — matches
the data model).

### 6. Open Questions — PASS on process, FAIL on rating

- **OQ-14 (human decision: block type delete while records exist) — implemented as decided.**
  `AnnotationTypeService.delete` calls `records.existsByType(id)` **before** removing, throws
  `AnnotationTypeHasRecordsException` (`CONFLICT` → 409, key `annotation.type.has_records`), and the
  empty-type path is untouched. The guard is **tenant-scoped**: `existsByType` filters
  `e.tenantId = :tenant`, so a foreign tenant's records can neither block nor unblock a delete. (The
  scoping is correct by code and by the choke-point design; no test covers the *foreign* case — the
  `existsByType` test uses one tenant only.)
- **OQ-15 (crypto scheme) — matches AD-14 exactly**: AES-256-GCM, fresh 12-byte IV per value, 128-bit
  tag, versioned keyring with the key version stored per value so decrypt selects by the stored version.
- **Reveal role (human decision: Tenant administrator) — matches**: `tenant.role() != Role.ADMIN` →
  `SecretRevealForbiddenException` (403). No new role or permission was invented; the plan's rationale for
  an explicit check over `@RolesAllowed` (to carry the localized key) is recorded and correct.
- **OQ-17 — opened, not silently assumed** (`catalogs/open-questions.md`, history row 2026-07-25), and the
  plan's §Risk names it. **But** its severity (🟢 Tactical) is wrong by an order of magnitude (F5) and it
  ships with placeholder text (F14).
- **No OQ was closed by implementer assumption.** However, F4 (PUT vs secrets) and F12 (Secret-flag flip)
  are *undecided* semantics resolved silently in code — F4 in the most destructive direction available.
  They should have been OQs.

---

## Targeted risk probes (as instructed)

| Probe | Result |
|---|---|
| Does `PUT` re-encrypt / preserve secrets? What if the field is omitted? | **Neither.** Values sent are re-encrypted fresh (new IV per write — correct); an omitted secret is silently destroyed, and an echoed masked value is a 400. **F4.** |
| Are orphaned `AnnotationValue` rows really removed on update? | **Yes** — verified: after a PUT that drops a value, a read in a fresh transaction shows it gone (the `NOT NULL` FK forbids nulling, so the row is deleted; `ON DELETE CASCADE` backs JPA up for `annotation_value_option`). Not asserted by any test (**F10**). |
| Is `selectedOptionIds` validated against the **owning** field's options? | **Yes — correct.** `applyOptions` builds `known` from `field.getOptions()` only. Verified: posting field A's value with an option id belonging to sibling field B of the same type → `400 annotation.record.value.option.unknown`. No finding. |
| Does reveal leak cleartext into logs / OpenAPI / an error message? | **No.** No logger in the reveal path; exception messages are constants; the OpenAPI document contains no secret sample; the 500 bodies of F1–F3 carry class names and SQL statements but **no bind parameters**, so no plaintext escapes there either. |
| Is the type-delete guard tenant-scoped? | **Yes** — `existsByType` filters `tenantId` (see check 6). |
| Does V3 match `data-model.md`, and the entities match V3? | V3 is **byte-for-byte the data model's SQL**. Entity↔migration matches everywhere except `text_value` (**F7**). |
| Ciphertext at rest, read from the actual column | `text_value=null`, `secret_ciphertext`=28 bytes (12-char plaintext + 16-byte GCM tag), `secret_iv`=12 bytes, `secret_key_version=1`, ciphertext does not contain the plaintext. **The feature's central promise holds.** |

## Verified good (worth stating plainly)

The parts most likely to be wrong in a feature like this are right: the crypto seam is clean and
correctly confined; the key is versioned per value so rotation works; GCM authentication rejects tampering
(tested); masking is decided by the *field's* Secret flag at DTO assembly so a new endpoint cannot forget
it; the reveal is `POST` (no query-string logging, no caching) and gated before any data is loaded; the
audit write shares the action's transaction; tenant scoping is structural, not per-endpoint; the error
taxonomy extension is one enum constant plus one switch arm; and all ten new message keys are complete in
both locales.

## What must be reopened

- **`implement`** — F1, F2, F3 (edge validation so no request shape returns 500: at-most-one-value-per-
  field, `@Size` on the name, a bound on secret/text length), F6 (record the revealed `fieldId`), F9
  (align `MULTIPLE_CHOICE` with the contract, or amend the contract), F7 (`textValue` mapping vs `TEXT`),
  F10 (tighten the four under-asserted tests; add an Image-value test and a cross-tenant reveal test for
  F8), F11 (validate key length), F13.
- **`spec`** (then plan/tasks/implement) — F4: decide and specify how `PUT` treats a secret value the
  client cannot resend, with a Gherkin scenario and an audit rule for destroying one; F12: decide the
  Secret-flag-flip semantics; F5/OQ-17: re-rate the open question and decide whether feat-005 ships an
  interim guard (the symmetric case to the OQ-14 decision the human already made).

## Out of scope but surfaced (pre-existing, not feat-005 defects)

1. `PUT /api/annotation-types/{id}` returns `fields[].id = null` — the DTO is built before flush, so a
   client cannot learn the new field ids from the response. feat-003.
2. `%prod.quarkus.hibernate-orm.schema-management.strategy` is an **unrecognized** key in Quarkus 3.15.1,
   so prod runs with no entity/schema validation (see F7). feat-001/003.
3. No `@param`/`@return`/`@throws` on the public surface anywhere in the codebase, though
   `constitution/03-code-standards.md` §Comments requires them.
4. `AnnotationTypeInput.name` also lacks `@Size(max=120)`, so feat-003's type create/replace has F2's 500
   too.

## GitHub

On a `fail` verdict nothing is posted or merged. The audit issue comment
(`./bin/wf github comment feat-005-annotation-records-notebox-api.audit --event audit`) is **not run
here** — a public post needs an explicit go-ahead from the coordinator. `catalogs/epics.md` US-2.1 stays
`speccing`; it should not advance until the reopened substeps close.

VERDICT: fail
