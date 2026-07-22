# INTAKE — Notebox raw-material digest

> Structured digest of the product brief for downstream steps (constitution, PRD, catalogs).
> This is the **only** artifact those steps read; if a claim is not here, it does not exist for them.
> Source was written in Portuguese; digested into English (all citations reference the English
> translation of the source). Scope tags reflect the **API-first** decision (2026-07-22): `[API]`
> belongs to this hub (notebox-api), `[WEB]` is UI behaviour routed to the `notebox-web` satellite,
> `[BOTH]` needs contracts here and rendering there.
>
> Source: `processed/definition_22072026122545.md` (the human's brief, translated to English).

## 1. Product summary

Notebox is a system for storing **annotations** (notes) and managing **tasks**. It exposes two
top-level domains — Annotations and Tasks — both organizable into user-defined **groups** shown in
a left-hand navigation tree. `notebox-api` is the backend; a `notebox-web` (react/next) front end
renders the described UI.

## 2. Claims

### 2.1 Annotations — types (schema definition)

| # | Claim | Scope | Source |
|---|---|---|---|
| C1 | The system stores annotations. Every annotation belongs to an **annotation type** that defines its schema (its fields). | API | brief § "Annotations" |
| C2 | An annotation type has: a **name** (e.g. "RabbitMQ"), an **icon** (an image representing the type), and a set of **fields**. | API | brief § "An annotation type would be" |
| C3 | The type icon image must be viewable at a **small size** to represent the type in datagrids/datatables. | BOTH | brief § "Icon: the RabbitMQ image" |
| C4 | Each **field** of a type has: an **icon** (image), a **field type** (from the fixed list below), a **field name** (text), and a **"visible for viewing"** flag (boolean). | API | brief § "The fields have the following composition" |
| C5 | **"Visible for viewing"** controls whether the field appears as a column in the datagrid/datatable listing of that type's annotations. Non-visible fields are hidden from the grid but still exist. Example: type RabbitMQ with URL, Environment, Description where Description is not visible → grid shows only URL, Environment. | BOTH | brief § "Visible for viewing" |

### 2.2 Field types (the fixed enumeration)

Each field type has a **default** value for the "visible for viewing" flag, as stated in the brief.

| # | Field type | Typical UI control | Default "visible for viewing" | Notes | Scope | Source |
|---|---|---|---|---|---|---|
| C6 | **Text** | textbox | true | — | BOTH | brief § "Text" |
| C7 | **List** | combobox | true | Options are a defined list. A per-list option renders items as **badges**; a badge's background colour is chosen from a fixed palette (red, green, blue, black, gray, yellow) and assigned per list item. | BOTH | brief § "List" |
| C8 | **Number** | textbox with a **slider** | true | For choosing a value within a scale/range. | BOTH | brief § "Number" |
| C9 | **Free text** | textarea | false | — | BOTH | brief § "Free text" |
| C10 | **Single choice** | radio | false | Must be populated with list items (options). | BOTH | brief § "Single choice" |
| C11 | **Multiple choice** | checkbox set | false | Must be populated with list items (options). | BOTH | brief § "Multiple choice" |
| C12 | **Image** | image control | false | — | BOTH | brief § "Image" |

### 2.3 Annotations — records (data entry)

| # | Claim | Scope | Source |
|---|---|---|---|
| C13 | An annotation record is an instance of a type, holding a value per field. Example: RabbitMQ record with URL (text), Environment (list: Dev/Hml/Prod), Description (text). | API | brief § "registering an annotation" |
| C14 | Annotations of a type are listed in an **editable datagrid**: one row per record, one column per **visible** field. | WEB | brief § "editable datagrid" |
| C15 | Each row exposes **Edit**, **Delete**, **Details** as icon actions. | WEB | brief § "Edit, Delete and Details must be icons" |
| C16 | **Edit** = inline edit in the datatable; opens the appropriate form control per field type. | WEB | brief § "Clicking Edit" |
| C17 | **Delete** = opens a confirmation dialog before removing the record. | WEB | brief § "Clicking Delete" |
| C18 | **Details** = opens a popup showing **all** fields of the type, including those with visible-for-viewing = false, plus Edit and Delete buttons inside the popup. | WEB | brief § "Details opens a popup" |

### 2.4 Tasks

| # | Claim | Scope | Source |
|---|---|---|---|
| C19 | A **task** has: name, start date, end date, **Card**, **priority**, **status**. | API | brief § "The structure of a task" |
| C20 | A task may be linked to a **group**. | API | brief § "Tasks can be linked to a group" |
| C21 | **Card** = an object with a code/id (e.g. `TST-3456`) and an optional URL. The task grid displays only the id, clickable to the card URL. | BOTH | brief § "The Card column" |
| C22 | The task datatable is **editable** for the task fields, and has a **collapsible sub-table** for **subtasks**. | WEB | brief § "collapsible sub-datatable" |
| C23 | A **subtask** has: name, start date, end date, an **optional Card**, and a **done** checkbox. | API | brief § "A subtask must have" |
| C24 | Completing subtasks (checking done) updates the parent task's **status progress bar**. The task's status percentage = proportion of completed subtasks; it recomputes as subtasks are added. | BOTH | brief § "progress bar of the task's status field" |
| C25 | A task's **start date** = the start date of its first subtask; its **end date** = the end date of its last subtask. | API | brief § "The task's start date and end date" |
| C26 | A task has a **details** field, not shown in the task datatable — only a Details icon opens it. | BOTH | brief § "details field that is not shown" |
| C27 | Task **details** is a rich-text (WYSIWYG) field supporting font styles, colour, underline, bold, and embedded images. The Details icon opens a popup rendering the formatted content. | BOTH | brief § "rich-text editor (WYSIWYG)" |

### 2.5 Navigation & grouping (cross-cutting)

| # | Claim | Scope | Source |
|---|---|---|---|
| C28 | A left sidebar tree ("Navigator") lists Annotations and Tasks. Under Annotations, each annotation **type** is a node (e.g. RabbitMQ). | WEB | brief § "left-hand sidebar menu" |
| C29 | Both annotations and tasks can be organized into user-created **groups** (e.g. myGroup1, myGroup2) as tree nodes. The system must allow creating a group for items. | API | brief § "organized into groups" |
| C30 | Clicking a sidebar item shows its detail on the right side of the page. | WEB | brief § "Clicking a menu item" |

### 2.6 Internationalization (i18n)

| # | Claim | Scope | Source |
|---|---|---|---|
| C31 | All system texts, messages, and content must be **internationalized**, available in at least **English and Portuguese**. Every string the system emits (labels, messages, errors, notifications) is locale-dependent. | BOTH | session decision (chat, 2026-07-22) |
| C32 | The system must provide a **management interface to edit these texts per language** — i.e. translations are editable at runtime through the product, not hardcoded. The API owns the translation store (message key → per-locale value) and exposes read/write endpoints; the UI renders the editor. | BOTH | session decision (chat, 2026-07-22) |

## 3. Constraints

| # | Constraint | Source |
|---|---|---|
| K1 | Backend stack fixed at setup: Java, Jakarta EE, JPA, CDI, MySQL. | workflow.json → project (setup 2026-07-22) |
| K2 | Front end is a separate `notebox-web` (react/next) satellite; the API must serve it. | workspace config (setup 2026-07-22) |
| K3 | Field type set is a **closed enumeration** of 7 (Text, List, Number, Free text, Single choice, Multiple choice, Image). | brief § "The type can be" |
| K4 | Badge colour palette is a **fixed set of 6**: red, green, blue, black, gray, yellow. | brief § "basic colours" |
| K5 | Image fields and type icons must support a small/thumbnail rendering. | brief § "small size" |
| K6 | Supported locales are at least **English (en)** and **Portuguese (pt)**; the system is multi-locale by design (see C31–C32). | session decision (chat, 2026-07-22) |

## 4. Decisions already made

| # | Decision | By | Source |
|---|---|---|---|
| D1 | Scope split is **API-first**: this hub owns the data model, contracts, and rules; UI behaviour routes to `notebox-web`. | Human (chat, 2026-07-22) | session decision |
| D2 | Default "visible for viewing" per field type as tabled in §2.2 (Text/List/Number = true; Free text/Single/Multiple/Image = false). | Human (brief) | brief § "The type can be" |
| D3 | The product is **internationalized (en + pt minimum)** with a runtime translation-management interface. | Human (chat, 2026-07-22) | session decision |
| D4 | **Multi-tenant** model: users belong to organizations/workspaces (tenants). All domain data — annotation types, annotations, tasks, subtasks, groups, cards — is owned by a tenant, shared within it, and isolated between tenants. Every entity carries a tenant boundary; every endpoint is authorized against it. (resolves G2) | Human (chat, 2026-07-22) | session decision |
| D5 | **i18n scope = system strings only.** UI labels, validation messages, errors, and notifications are localized via a message catalog with a runtime editor. User-created domain content (type/field/group names, list & badge item labels, task text) is stored as entered, not translated. (resolves G9) | Human (chat, 2026-07-22) | session decision |
| D6 | **Images stored as BLOBs in MySQL** — type icons, image-field values, and rich-text embedded images are persisted as binary in the database, with metadata (content type, size). Thumbnail/small rendering is derived from the stored binary. (resolves G1) | Human (chat, 2026-07-22) | session decision |
| D7 | **Task status = derived completion % only** (proportion of completed subtasks; 0% when there are none). **Priority = fixed enum**: Low, Medium, High, Critical. (resolves G3) | Human (chat, 2026-07-22) | session decision |

## 5. Contradictions

None found. The brief is internally consistent.

## 6. Gaps / open questions (raised, not resolved)

Remaining gaps must not be invented downstream — candidates to become OQs at the PRD/spec stage.
**Resolved:** G1, G2, G3, G9 → see Decisions D4–D7.

| # | Gap | Why it matters |
|---|---|---|
| ~~G1~~ | RESOLVED → D6 (BLOB in MySQL). | — |
| ~~G2~~ | RESOLVED → D4 (multi-tenant). | — |
| ~~G3~~ | RESOLVED → D7 (derived % + priority enum). | — |
| G4 | **List field options vs badge colours** — for List/Single/Multiple choice, where are the allowed options defined: on the type-field definition (design time) or free at data entry? Badge colour is per-item — stored where? | Type-definition schema. |
| G5 | **Group semantics** — can an item belong to multiple groups? Are groups nested? Are Annotation groups and Task groups the same entity or separate? | Group entity model. |
| G6 | **Subtask ordering** — "first/last subtask" for task date derivation implies an ordering (by start date? by insertion?). Undefined. | Date-derivation rule (C25). |
| G7 | **Card entity ownership** — is Card a shared entity or an inline value object on task/subtask? Can the same Card be referenced by many tasks? | Card modelling (C21). |
| G8 | **Validation & required fields** — nothing states which fields are mandatory, uniqueness (e.g. type name), or numeric slider min/max bounds. | Contract validation. |
| ~~G9~~ | RESOLVED → D5 (system strings only). | — |
| G10 | **i18n mechanics** — default/fallback locale when a translation is missing; how the request locale is chosen (`Accept-Language` header, user preference, query param); who may edit translations (admin authz, ties to D4 tenancy); whether new locales can be added at runtime beyond en/pt. | Endpoint contract + admin authz. |

## 7. Unusable inputs

None — the single input was fully digestible.
