# Feature — A subtask records when it was completed

**ID:** features/029-subtask-completion-notebox-api
**User Story:** US-4.1
**Version:** v1
**Status:** Approved (human approval 2026-09-03)
**Date:** 2026-09-03

## Origin

- **User Story:** US-4.1 — *As a tenant member, I want tasks with subtasks whose completion drives the
  task's progress, so status reflects real work.* Already **delivered** (feat-010 api, feat-011 web);
  this feature extends its aggregate rather than opening a new story.
- **FRs covered:**
  - **FR-20 (new, 2026-09-03):** *A subtask records the moment it was marked done, and that moment is
    erased when it is un-marked. Server-written and readable, never client-supplied. Subtasks completed
    before this existed carry no moment and none is invented for them.*
  - **Why a new FR and not FR-11.** FR-11 enumerates a subtask's attributes as *"name, dates, optional
    card, done flag"* and says nothing about **when** completion happened. Writing this spec against it
    would have stretched a requirement over surface it does not name — the failure mode CLAUDE.md calls
    the most expensive here. FR-20 follows the FR-19 precedent: a requirement catalogued from a direct
    human decision, with that decision cited in its own Origin column.
  - FR-11 remains the **surface being extended** (the subtask and its done flag) and is untouched.
- **BRs bound:**
  - **BR-06** (task status is derived from the subtasks *marked done*) — **untouched and explicitly
    frozen.** Completion gains a second recorded fact; it must not gain a second *input to status*. The
    percentage stays "the proportion that are marked done", never "the proportion with a recorded
    moment". feat-012 froze status derivation and this feature repeats that freeze.
  - **BR-07** (task dates derive from subtasks) — bound twice. It settles **which** end date the moment
    is judged against: the writable one is the **subtask's**, because the task's is min/max of its
    subtasks and the task input already rejects client-written dates. And it is why completing a dateless
    subtask has a **task-level** consequence once the web half writes today into that subtask's dates:
    the parent's derived span moves.
  - **BR-05** (destructive deletions are explicit and irreversible) — **considered, does not bite, and
    the resemblance is real enough to state.** Un-completing erases the moment, and that erasure is
    **not recoverable** — re-completing writes a fresh moment, it does not restore the old one. What
    takes BR-05 out of scope is its object: the rule is about deleting a *record*, and it enumerates
    them — "an annotation, task, type, group". Here no record is removed; a server-written field on a
    surviving subtask is cleared.
  - **BR-01 / BR-02** (tenant isolation and ownership; BR-02 names "subtask" in its enumeration) — the
    standing invariants every scenario inherits.
  - No other BR is triggered. BR-03/BR-04 are annotation-type rules and BR-09/BR-10 are field-flag rules,
    with no subtask surface. **BR-08** (localization) **is** triggered — see C-09.
- **Primary source:** **A direct human decision (product owner, 2026-09-02), recorded as OQ-38 and
  catalogued as FR-20.** The PRD is silent on a completion time: it fixes the done flag and the
  derivations built on it, and says nothing about the moment. Per the source hierarchy an explicit human
  decision ranks with the PRD, so it is cited as such rather than dressed up as PRD-derived.
- **Consumes:** feat-010's task aggregate and its **replace-update semantics** (an update states the full
  intended state; omissions clear — an absent `done` means false).
- **Pairs with:** **feat-030-subtask-completion-notebox-web**, which owns everything a member sees. The
  two are promoted together (see *Rollout hazard*).

## Summary

A subtask that is marked done records the moment it happened; un-marking it erases that moment. The
moment is server-written and readable, never client-supplied.

This exists for one reason, and the reason is why it must be **persisted rather than derived**: without
a stored moment, "finished after the planned date" can only be computed as *done and the end date is in
the past*, and that turns **true for a subtask finished on time** as soon as today walks past its end
date. A subtask delivered early would start accusing itself of lateness. The information is destroyed at
the moment of completion unless it is written down then.

## Scope

**In**

- Recording the completion moment when a subtask becomes done — whether it is **created** done or
  **updated** into it — and erasing it when it stops being done.
- Exposing that moment in the subtask read model, so a client can compare it with the subtask's own
  planned end date.
- Rejecting a client that supplies the moment itself, in the style BR-06 and BR-07 are already enforced:
  rejected with its own localized key, never silently ignored (a refinement under the standing OQ-09
  delegation, *"feature specs may refine"*).

**Out**

- **Any change to status derivation (BR-06) or date derivation (BR-07).** The percentage rule, its
  rounding and its triggers are frozen; so is min/max. This feature adds a fact, not an input.
- **A server-computed "late" flag.** The API reports the moment and the planned end date; the comparison
  is the web half's. *This is a spec decision, not a technicality, and it is the one thing here most
  worth overturning at the approval gate:* deriving it server-side would match the spirit of BR-06/BR-07
  and would stop two clients from disagreeing. It is left out because "late" is a product judgement that
  may acquire a grace period or a working-day rule, and freezing it into the read model now would make
  that a breaking change. Say the word and it moves in.
- **Recording that a completion was undone.** Erasing the moment leaves no trace. See C-10.
- **Backfilling subtasks completed before this feature.** The moment is unrecoverable; inventing one
  would manufacture a lateness verdict out of nothing.
- Anything a member sees: the question about a missing start date, the auto-filled end date, the
  indicator and its tooltip. All of that is feat-030.

## Acceptance criteria (Gherkin)

> **On what these scenarios assert.** Every outcome below is a value that can be read back — a moment
> present or absent, a percentage, a date, a rejection key. None asserts a literal instant or an ordering
> between two instants taken moments apart: at the resolution available, two such instants are routinely
> equal, so "later than" would pass whether or not the implementation rewrote anything. Where staleness
> matters, the erase-on-un-complete rule closes it instead — a moment cannot be stale if it is gone.

```gherkin
Feature: FR-20 — a subtask records the moment it was completed

  Scenario: Completing a subtask records the moment
    Given a task with 2 subtasks, neither done, so its status is 0%
    When one subtask is updated to done
    Then that subtask reports a completion moment
    And the task status is 50%

  Scenario: A subtask created already done records the moment
    Given a task with no subtasks
    When a subtask is added that is already done
    Then that subtask reports a completion moment
    And the task status is 100%

  Scenario: Un-completing a subtask erases the moment
    Given a subtask that is done and reports a completion moment
    When it is updated to not done
    Then the subtask reports no completion moment
    And the subtask is still present in the task, with its name and planned dates unchanged

  Scenario: Completing again records a moment, never a stale one
    Given a subtask that was completed and then un-completed, so it reports no completion moment
    When it is completed a second time
    Then the subtask reports a completion moment

  Scenario: An update that leaves done unchanged leaves the moment unchanged
    Given a subtask that is done and reports a completion moment
    When it is updated with a different name and the same done value
    Then the subtask reports the same completion moment it reported before

  Scenario: Rescheduling a completed subtask keeps its moment
    Given a subtask that is done and reports a completion moment
    When its planned end date is moved
    Then the subtask reports the same completion moment it reported before
    And it reports the new planned end date

  Scenario: Completing does not move the planned dates
    Given a subtask with a planned start date and a planned end date, in a task whose derived dates are those dates
    When the subtask is updated to done
    Then its planned start date and planned end date are unchanged
    And the task's derived start and end dates are unchanged

  Scenario: The read model carries what a lateness comparison needs
    Given a subtask that has a planned end date and was completed under this feature
    When the task is read
    Then the subtask reports its completion moment
    And it reports its planned end date

  Scenario: A subtask completed before this feature existed reports no moment
    Given a subtask that is done and carries no recorded completion moment
    When the task is read
    Then the subtask reports no completion moment
    And after an update that leaves it done, it still reports no completion moment

  Scenario: The completion moment is not client-writable
    Given a subtask that is done and reports a completion moment
    When an update supplies a completion moment of its own
    Then the request is rejected with the completion-moment-not-writable key
    And the subtask reports the same completion moment it reported before

  Scenario: The rejection resolves in the caller's locale
    Given a member whose locale resolves to Portuguese
    When an update supplies a completion moment of its own
    Then the rejection carries the Portuguese catalog text for that key
    And no raw key or empty string reaches the client

  Scenario: A subtask of another tenant cannot be completed
    Given a subtask owned by another tenant
    When an update marks it done
    Then the request is rejected as not found, and no subtask data is disclosed
    And its owner still reads it as not done, reporting no completion moment
```

**Boundaries pinned above, and why each is here**

| Boundary | Why it is a scenario and not an assumption |
|---|---|
| Created already done | FR-11 names *"completing/adding"* as the two triggers. Every other scenario enters completion through an update; without this one, the add path could ship recording nothing. |
| Re-completion | Guards against a stale moment surviving a reopen. Stated as *presence*, because erase-on-un-complete already makes staleness impossible and an ordering assertion between two near-simultaneous moments would discriminate nothing. |
| Done unchanged | Guards an existing test — a rename must not read as a re-completion. |
| Rescheduling | The moment records when work finished; moving the plan afterwards must not rewrite history. This is also the case that makes a subtask *late* after the fact, so it must be exact. |
| Planned dates unmoved | Recording a moment must not become a second way to write dates. |
| Pre-existing done subtasks | Their moment is unrecoverable. Reporting nothing is the only honest answer; the follow-up update in that scenario stops a lazy backfill from filling it in on next write. |
| Not client-writable | Poison-field precedent: BR-06 and BR-07 are enforced by *rejecting* a client write rather than ignoring it, so a client learns it was wrong instead of silently disagreeing. |
| Locale | C-09's evidence, in feat-012's shape. |
| Cross-tenant | C-01's evidence, phrased as not-found because that — not a permission error — is the established shape at every tenant-owned seam here. |

## Compliance pre-flight

- [x] **C-01 · Tenant isolation** — **applies.** The subtask write path is tenant-owned data (BR-02 names
  subtask; AD-03 makes the repository the choke point). *Evidence:* the cross-tenant scenario, landing on
  the existing foreign-tenant sweep that already exercises subtask operations.
- [x] **C-02 · Authenticated by default** — **applies (standing).** This feature adds no new entry point;
  the existing subtask endpoints carry the requirement. *Evidence:* the foreign-tenant sweep above
  exercises the subtask update path. **Gap named rather than glossed:** the existing unauthenticated
  assertion issues only a task listing, so the 401 half is not evidenced on the path this feature
  touches; `implement` extends it to that path.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** Completing a subtask is ordinary tenant
  membership, not an admin or config surface. No elevated role is introduced.
- [ ] **C-04 · Personal data minimization** — **n/a.** No user or tenant identity data is stored or
  returned. The moment is attributable to no person: neither a task nor a subtask carries an assignee or
  actor, and the subtask update path writes no audit row — so nothing joins the moment to a member.
  **This flips to *applies* the day a subtask gains an assignee or its update becomes audited**, at which
  point a completion moment becomes per-member timing data and owes a data map and a response-schema
  review. Not theoretical: feat-030's whole purpose is turning this moment into a lateness verdict.
- [ ] **C-05 · Secrets never committed** — **n/a.** No credential, key or connection string.
- [x] **C-06 · Encryption in transit** — **applies (standing).** All endpoints are served over TLS in
  deployed environments; this feature adds no transport surface. *Evidence:* deployment/ingress config —
  cross-cutting and outside this repository, not feature-specific (feat-005's wording).
- [ ] **C-07 · Image upload safety** — **n/a.** No image binary is accepted or served.
- [ ] **C-08 · Rich-text sanitization** — **n/a for new work; standing otherwise.** This feature stores
  and returns no rich-text content of its own. The task read it extends *does* still return sanitized
  details, and that seam is untouched.
- [x] **C-09 · Localization completeness** — **applies.** The rejection scenario emits a **new**
  user-facing validation string: no existing key fits, because the two poison-field keys that exist both
  describe date derivation, and reusing one would hand the member a message about a field this is not.
  *Evidence:* the Portuguese scenario above asserted on the wire, plus en+pt entries reached by the
  message-coverage check — which enumerates its keys **by hand and does not scan the code**, so a key
  omitted from it is silently uncovered and `implement` owes the entry as much as the value.
- [ ] **C-10 · Audit trail for irreversible & admin actions** — **n/a, and the reasoning is not
  reversibility.** The erased moment is genuinely unrecoverable — re-completion writes a new one rather
  than restoring the old. C-10 does not apply because it scopes itself to destructive deletions of
  *records* (BR-05), tenant/user administration and catalog edits, and this clears a server-written field
  on a surviving subtask. **Recorded because the asymmetry is real:** deleting a subtask *is* audited,
  while erasing the one unrecoverable fact on a subtask that survives is not. If the product later needs
  to know that a completion was undone, that is a C-10 change, not a bug — the audit trail already
  carries actor, action, target and time and would need no new shape.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account, tenant or removal surface. The
  moment lives and dies with its subtask, which the existing subtask deletion path already removes.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** No Secret-flagged annotation value is
  stored, updated or revealed.

## Rollout hazard

**The feat-012 / feat-014 clearing hazard does NOT repeat here** — the moment is server-written and never
appears in a client payload, so no client can clear it by omitting it. Two other hazards do, and neither
was obvious:

1. **An omitted `done` now destroys a fact, not just a flag.** Under replace-update an absent `done` means
   false, and this feature makes false *erase the moment*. Any client that updates a subtask without
   echoing `done` silently destroys a recorded completion. Before this feature the same omission merely
   flipped a boolean that the next tick would restore.
2. **Sequencing.** A moment recorded with no screen reading it, or a screen expecting one from an API that
   has not shipped, are both half-built states a member can see. So the pair is **promoted together**.
   Precedent: feat-012/013 and feat-014/015 both recorded a rollout hazard and were merged into `develop`
   as pairs; both are still awaiting promotion to `main`, which the epics catalog says must happen
   pair-wise.

## Open Questions

- **OQ-38 — Does a subtask record when it was completed, and what happens to that record when it is
  un-completed?** *Resolved 2026-09-02.* **Decision:** the moment is **persisted**, because deriving it
  is impossible without accusing on-time work of lateness once today passes its end date; and it is
  **erased** when the subtask is un-completed. Decided by rafaelsantos, 2026-09-02. Catalogued as FR-20
  and folded into the scenarios above.
- No other open question. The one judgement this spec makes on its own — leaving the late/on-time
  comparison to the client — is named in **Out of scope** with its reasoning, so the approval gate can
  overturn it without reading the whole document.
