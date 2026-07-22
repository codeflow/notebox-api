---
step: constitution.compliance
model: sonnet
effort: medium
reads: [definitions/INTAKE.md, constitution/00-principles.md]
writes: [constitution/02-compliance.md]
---

# Compliance checklist

**Goal.** A checklist every feature spec copies in and marks item by item. It exists to make
"we forgot about consent" impossible to reach production silently.

## Shape

```markdown
- [ ] **C-01 · <name>** — <what must be true> · *applies when:* <trigger> · *evidence:* <where proof lives>
```

The **applies when** clause matters: a checklist where every item applies to every feature gets
rubber-stamped. Scope each item to its trigger.

## Cover what the project actually faces

Derive from `INTAKE.md`, not from a generic list. Typical axes:

- Personal data: what is collected, lawful basis, retention, deletion path.
- Authentication and authorization on every entry point.
- Secrets: never committed; how they are injected.
- Audit trail: which actions must be reconstructible, and for how long.
- Accessibility and localization, if there is a user interface.
- Sector rules named in the definitions (payments, health, finance, minors).

## Done when

- Each item has a trigger and a named evidence location.
- Items with no legal or contractual basis are dropped, not kept "just in case".
- `features/_template/spec.template.md` references this checklist so every spec inherits it.
