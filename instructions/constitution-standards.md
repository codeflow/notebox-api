---
step: constitution.standards
model: sonnet
effort: medium
reads: [constitution/01-architecture.md, harness/profiles/*.md]
writes: [constitution/03-code-standards.md]
---

# Code standards

**Goal.** Only the conventions a machine cannot infer from the surrounding code.

## The filter

Before writing a rule, ask: *would an agent reading three neighbouring files already do this?*
If yes, delete it. Agents match surrounding style by default; restating it burns tokens on
every feature forever.

Keep only:

- Rules the linter/formatter cannot express, but reviewers enforce anyway.
- Naming that carries meaning (id prefixes, event names, error taxonomies).
- **The identifier-language rule.** Materialize `workflow.json → project.code_naming` here as a
  concrete, checkable rule with examples from THIS domain:
  - `english` → "all identifiers in English: `findUsers`, `RefundService`."
  - `hybrid` → "technical vocabulary in English, domain terms in `project.language`:
    `findUsuarios`, `EstornoService`. Domain terms are the nouns of the PRD — list them here as
    they appear (Usuário, Cobrança, Estorno…), and grow the list as the catalog grows. A domain
    term half-translated (`findUsers` next to `criarUsuario`) is a violation."
  - `project-language` → "everything in `project.language`: `buscarUsuarios`. Framework/keyword
    identifiers stay as the platform requires."
  The audit greps identifiers against this rule; without the written list, "hybrid" is
  unenforceable opinion.
- **The comments policy.** Materialize `workflow.json → project.comments` here as concrete rules:
  - Language: comments are written in `project.language`, always.
  - Scope `minimal` → one short comment above each public function/method stating what it does
    and why it exists; no inline comments except where the code cannot express a constraint.
  - Scope `detailed` → the above, plus inline comments on non-obvious blocks inside
    functions/methods (algorithmic choices, invariants, edge cases). Never narrate the obvious —
    `i++ // increment i` is a violation in BOTH scopes.
  - `reference_docs` set → public API surface carries the format's structured header (Javadoc,
    JSDoc, docstrings…), with params/returns/errors filled, in `project.language`.
  - `api_docs` set (e.g. openapi) → every HTTP endpoint is covered by the contract doc, updated
    in the same feature that changes the endpoint — a drifted spec fails the audit.
- Error handling policy: what is thrown, what is returned, what is logged, what is swallowed.
- Test conventions: where tests live, what a test is named, what "unit" means here.
- The forbidden list: patterns previously banned, each with the incident that banned it.

## Delegate to tooling

Whatever a formatter or linter can enforce, configure it and reference the config file here
instead of prose. Point at the `verify` command from `harness.verify`.

## Done when

Every remaining rule survives the filter above, and each one names either the tool that enforces
it or the reason no tool can.
