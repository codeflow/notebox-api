# PRD — the project

**Version:** v1
**Date:** 2026-07-19
**Status:** Draft
**Type:** TBD · **Stack:** TBD
**Language:** English (en)

> The living source of truth for product intent. Update via the PRD process; the repo syncs with
> `/new-prd-version`. Keep IDs immutable across versions.

## 0. Changelog
| Version | Date | Main changes |
|---------|------|--------------|
| v1 | 2026-07-19 | Initial draft. |

## 1. Vision, Goals & Personas
### 1.1 Vision
<one-paragraph product vision>

### 1.2 Business goals
| # | Goal | Key indicator |
|---|------|---------------|
| G-01 | <goal> | <metric> |

### 1.3 Personas
- **<persona>** — context, pains, goals, usage scenario.

## 2. User Stories & Flows
### 2.1 User stories (by epic)
- **E1** — US-1.1: As a <persona>, I want <capability> so that <value>.

### 2.2 Main flows
<step-by-step primary flows>

## 3. Functional & Non-Functional Requirements
### 3.1 Functional (FR)
| ID | Description | Priority | Origin (BR + source) |
|----|-------------|----------|----------------------|
| FR-01 | <description> | Must | BR-01 + <source> |

### 3.2 Non-Functional (NFR)
| ID | Category | Description | Target |
|----|----------|-------------|--------|
| NFR-01 | <category> | <requirement> | <measurable> |

## 4. Acceptance Criteria (Gherkin)
```gherkin
Feature: <FR-01 title>
  Scenario: <happy path>
    Given <precondition>
    When <action>
    Then <measurable outcome>
  Scenario: <error path>
    Given <precondition>
    When <invalid action>
    Then <expected error/status>
```

## 5. Architecture & Stack
<high-level architecture; confirm `TBD` choices>

## 6. Success Metrics / KPIs
| Metric | How measured | Target | Cadence |
|--------|--------------|--------|---------|

## 7. Roadmap & Phases
<phased delivery>

## 8. Assumptions & Open Questions
| ID | Item | Status |
|----|------|--------|
| OQ-01 | <open question> | open |

## 9. Traceability
### 9.1 BR → FR map
| BR | Theme | FRs |
|----|-------|-----|
| BR-01 | <theme> | FR-01 |
