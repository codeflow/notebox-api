# Constitution — Code Standards

> How code is written in the project. Language of docs/comments: English.

## General
- **Match surrounding code:** naming, structure, comment density, idioms.
- **Small changes:** prefer the smallest diff that satisfies the task.
- **Tests with code:** every behavioral change ships a test. Gherkin scenarios → executable tests.
- **No dead code, no commented-out blocks** left behind.

## Conventions — the explicit, enforced rules

> **This is THE place for granular code-generation rules.** Be specific; the agent treats these as
> inviolable during `/implement` and `/audit` checks against them. The examples below are editable
> defaults for `TBD` — change them to your taste.

### Naming
- Variables & functions: **`camelCase`**.
- Types / classes / interfaces: **`PascalCase`**.
- Constants: **`UPPER_SNAKE_CASE`**.
- Files/folders: <e.g. `kebab-case` for web, `PascalCase.java` for Java — set per stack>.

### Comments
- **Comment only at the function/method level** — a short doc of intent/contract (params, return,
  side effects). **No line-by-line or obvious comments** inside function bodies.
- Public API is documented; private internals must be self-explanatory through good naming.

### Errors & logging
- Consistent error shape; never swallow exceptions silently.
- Structured logging with a correlation id; never log secrets/PII.

### Config
- Via environment; documented in `.env.example`. No hardcoded config/secrets.

### Make it enforceable (don't rely on memory)
Mirror the rules above in the toolchain wired into `verify`/CI, so violations **fail the build**:
- Formatter + linter (e.g. Prettier+ESLint / Spotless+Checkstyle / Ruff) with the naming/comment rules configured.
- Pre-commit hook running the formatter/linter.
- `/audit` cross-checks code vs this file.

## Definition of Done (code)
- [ ] `verify` green (lint + typecheck + test + build — see `docs/harness-TBD.md`).
- [ ] Follows these standards and the architecture.
- [ ] Traceable to a spec item (FR/BR/US).
- [ ] No secrets committed.
