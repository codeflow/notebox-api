# HANDOFF — feat-005 annotation records (retomar aqui)

**Escrito:** 2026-07-25 · **Estado:** audit `fail`, corrigindo. **PR [#6](https://github.com/codeflow/notebox-api/pull/6) é DRAFT — não mergear.**

## Como retomar

```bash
cd notebox-api && claude      # o hook injeta o pipeline automaticamente
/wf-status                     # ver estado
/wf-next                       # continua em feat-005…spec (opus/high)
```

⚠️ **`mvn -B verify` exige o Docker ligado** (MySQL via Testcontainers/Dev Services). Sem Docker os
testes `@QuarkusTest` falham com *"Could not find a valid Docker environment"* / *"Failed to start
quarkus"* — não é defeito de código. `open -a Docker` e espere ~5s. O startup do Quarkus leva ~90s,
então o `verify` completo demora ~10 min. A CI do GitHub não depende disso (roda em container próprio).

## O que já está pronto e verificado (NÃO refazer)

10 commits em `feature/annotation-records`, `mvn -B verify` verde: **94 testes, 0 falhas, 0 skipped**.
O audit confirmou empiricamente, lendo a linha crua no MySQL:

- Segredos **são** ciphertext em repouso (`text_value=null`, ciphertext 28B, IV 12B, `key_version=1`).
- Masking sustenta no wire; gate ADMIN + auditoria do reveal funcionam.
- Toda query de records é tenant-scoped (**AD-03**); `javax.crypto` só em `infrastructure/security` (**AD-14**).
- `optionIds` validam contra o campo **dono**; orphanRemoval realmente apaga.
- 10 chaves i18n novas em `messages.properties` **e** `messages_pt.properties` (**C-09** ✔), **C-05** ✔
  (chave dev-only, `%prod` lê de env sem fallback).
- Nenhum dos 17 cenários está sem teste real; nenhum teste é mock/tautologia.

> O `reopen` do `implement` fez cascade em t1–t8. **Não reimplemente do zero** — use a lista abaixo
> e re-feche com `wf done` os que não têm finding.

## Decisão já tomada (não perguntar de novo)

**F4 — semântica do PUT para valor Secret** *(decisão humana 2026-07-25)* — já escrita em
`spec.md` (4 cenários novos) e `contracts/rest-api.md`:

| Payload no PUT | Comportamento |
|---|---|
| campo **omitido** | **preserva** o ciphertext (rename nunca destrói segredo) |
| `"text": "<novo>"` | **re-cifra** com a key ativa |
| `"clearSecret": true` | **apaga** + escreve **AuditLog** (BR-05, BR-10) |
| `"text": null` sem flag | **no-op preserve** — nunca `type_mismatch` (é o echo do GET mascarado) |

Campos não-secretos mantêm replace puro: omitido → valor removido.

## O que falta corrigir (audit.md tem o cenário de falha concreto de cada um)

### Bloqueadores HIGH
- **F4** → implementar a tabela acima em `AnnotationRecordService.update` + `AnnotationValueInput`
  (campo `clearSecret`) + testes dos 4 cenários novos do spec. *(spec já pronto)*
- **F1** → dois valores com o mesmo `fieldId` → HTTP 500 (`Collectors.toMap` duplicate key).
  Falta validar a invariante "no máximo um valor por campo" → erro localizado, não 500.
- **F2** → `name` > 120 chars → HTTP 500. Falta `@Size(max=120)` no `AnnotationRecordInput`
  (estava especificado em `data-model.md`, nunca foi escrito).
- **F3** → segredo > ~4080 chars → HTTP 500 (estoura `VARBINARY(4096)`). Decidir: validar tamanho
  na entrada **ou** alargar a coluna em nova migration.
- **F5 / OQ-17** → `AnnotationTypeService.replace` gera UUIDs novos de `TypeField` a cada PUT, então
  uma definição idêntica **orfana os valores de todos os records** (`GET` → `values:[]`) e o delete do
  tipo passa a dar `409 has_records`. Precisa decisão + guarda provisória. **OQ-17 está subestimado
  como 🟢 Tactical — reavaliar severidade.**

### MEDIUM / LOW
- **F6, F9–F11, F13** → em `implement` (F9: `MULTIPLE_CHOICE` 0..n sem código; F8: valores de campo
  Image sem código/teste).
- **F12** → decisão de contrato, em `spec`.
- Cláusulas de cenário não exercidas: delete "and its values are removed" (o teste apaga record sem
  valores), field-unknown "localized to the caller's locale", create "returns those three values"
  (só afere `size()==3`).

### Fora do escopo do feat-005, mas sério
- `%prod.quarkus.hibernate-orm.schema-management.strategy` é **chave não reconhecida no Quarkus 3.15.1**
  → produção roda **sem validação de schema/entidade**. Foi assim que a divergência de mapeamento do
  `text_value` (F7) passou batido. Merece issue própria.
- O PUT de annotation-type retorna `fields[].id = null`.

## Ordem sugerida

1. `/wf-next` → fechar o `spec` (F4 já escrito; decidir F12 e a severidade de OQ-17/F5).
2. `implement`: F1–F3 (as três 500s, mesma classe de defeito) → F4 → F9/F8 → cláusulas de teste.
3. `mvn -B verify` verde → re-rodar o **audit** (opus/xhigh) → só então tirar o PR #6 de draft.
