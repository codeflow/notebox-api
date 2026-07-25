# Contract — Annotation type builder UI (client interfaces)

**Feature:** features/004-annotation-types-web · **Project:** notebox-web (react/next, TS strict)
**Version:** v1 · **Status:** Draft · **Date:** 2026-07-23

> The artifacts `implement` codes against and `audit` checks against — **signatures only, no bodies**
> (plan `Do NOT`). These are the *client* contracts: the wire-DTO mirrors, the view-model, the two API-client
> modules, error routing, validation, and the message-key namespace. The **network** contract is fixed and
> owned by feat-003 (`features/003-annotation-types/contracts/rest-api.md`) — this file consumes it. All types
> live under the single `@/*` root alias.

## 1. Wire DTO mirrors — `lib/api/types.ts` (ADD; server authoritative — "shapes only")

```ts
export type FieldType =
  | 'TEXT' | 'LIST' | 'NUMBER' | 'FREE_TEXT'
  | 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'IMAGE';

export type BadgeColour = 'RED' | 'GREEN' | 'BLUE' | 'BLACK' | 'GRAY' | 'YELLOW';

export interface OptionInput { label: string; badgeColour?: BadgeColour | null; }        // LIST colour only
export interface OptionDto   { id: string; label: string; badgeColour: BadgeColour | null; }

export interface FieldInput {
  name: string;
  fieldType: FieldType;
  iconImageId?: string | null;
  visibleForViewing?: boolean;          // omit => server applies D2 default
  secret?: boolean;                     // TEXT / FREE_TEXT only; default false
  numberMin?: number | null;            // NUMBER only
  numberMax?: number | null;            // NUMBER only
  options?: OptionInput[];              // LIST / SINGLE_CHOICE / MULTIPLE_CHOICE only
}
export interface FieldDto {
  id: string; name: string; fieldType: FieldType; iconImageId: string | null;
  visibleForViewing: boolean; secret: boolean;
  numberMin: number | null; numberMax: number | null; options: OptionDto[];
}

export interface AnnotationTypeInput { name: string; iconImageId?: string | null; fields: FieldInput[]; }
export interface AnnotationTypeDto   { id: string; name: string; iconImageId: string | null; createdAt: string; fields: FieldDto[]; }

export interface ImageRefDto { id: string; contentType: string; sizeBytes: number; }

// EXTEND the existing Problem (feat-002) — additive, backward compatible
export interface Violation { field: string; code: string; message: string; }
export interface Problem { code: string; message: string; correlationId?: string; violations?: Violation[]; }
```

> `parseProblem` (`lib/api/ApiError.ts`) is **extended** to preserve `violations` when present (today it
> returns only `{code,message}` and would drop them). Additive; existing auth error handling is unchanged.

## 2. View-model — `lib/annotationTypes/viewModel.ts` (NEW)

```ts
import type { MessageKey } from '@/lib/i18n/messages/en';
import type { AnnotationTypeInput, AnnotationTypeDto, BadgeColour, FieldType } from '@/lib/api/types';

export interface TypeErrorSlots   { name?: MessageKey; form?: MessageKey; }
export interface FieldErrorSlots  { name?: MessageKey; fieldType?: MessageKey; numberBounds?: MessageKey; secret?: MessageKey; icon?: MessageKey; options?: MessageKey; }
export interface OptionErrorSlots { label?: MessageKey; badgeColour?: MessageKey; }

export interface EditableOption { clientId: string; label: string; badgeColour: BadgeColour | null; errors: OptionErrorSlots; }
export interface EditableField {
  clientId: string; serverId: string | null;
  name: string; fieldType: FieldType;
  iconImageId: string | null; iconObjectUrl: string | null;
  visibleForViewing: boolean; visibleForViewingTouched: boolean;
  secret: boolean;
  numberMin: string; numberMax: string;      // raw input; parsed in toInput()
  options: EditableOption[]; errors: FieldErrorSlots;
}
export interface EditableType {
  clientId: string; serverId: string | null;
  name: string; iconImageId: string | null; iconObjectUrl: string | null;
  fields: EditableField[]; errors: TypeErrorSlots;
}

// Pure mapping + seeds (no I/O)
export function emptyType(): EditableType;
export function emptyField(fieldType?: FieldType): EditableField;
export function emptyOption(): EditableOption;
export function fromDto(dto: AnnotationTypeDto): EditableType;
export function toInput(t: EditableType): AnnotationTypeInput;   // narrows per fieldType (see data-model.md)
export const VISIBLE_DEFAULTS: Readonly<Record<FieldType, boolean>>;   // D2 seed table
```

## 3. API clients — `lib/api/` (NEW; thin over `authFetch` + `ApiError`, the `apiClient` idiom)

```ts
// lib/api/annotationTypesClient.ts
export interface AnnotationTypesClient {
  list(): Promise<AnnotationTypeDto[]>;                          // GET  /api/annotation-types
  get(id: string): Promise<AnnotationTypeDto>;                   // GET  /api/annotation-types/{id}
  create(input: AnnotationTypeInput): Promise<AnnotationTypeDto>;// POST /api/annotation-types            -> 201
  replace(id: string, input: AnnotationTypeInput): Promise<AnnotationTypeDto>; // PUT full-replace        -> 200
  remove(id: string): Promise<void>;                            // DELETE /api/annotation-types/{id}      -> 204
}
export const annotationTypesClient: AnnotationTypesClient;

// lib/api/imagesClient.ts
export interface ImagesClient {
  upload(contentType: string, bytes: ArrayBuffer | Blob): Promise<ImageRefDto>; // POST /api/images (raw bytes)
  fetchObjectUrl(id: string): Promise<string>;    // GET /api/images/{id} (authenticated) -> blob -> object URL
  revokeObjectUrl(url: string): void;             // URL.revokeObjectURL wrapper (leak guard)
}
export const imagesClient: ImagesClient;
```

> Every method throws `ApiError` on non-2xx (existing pattern). `imagesClient.fetchObjectUrl` must go through
> `authFetch` (bearer required, tenant-scoped) — a bare `<img src>` cannot carry the token, hence the object URL
> (AD-04, plan §6).

## 4. Server-error routing — `lib/validation/violationRouting.ts` (NEW)

```ts
import type { Problem } from '@/lib/api/types';
import type { EditableType } from '@/lib/annotationTypes/viewModel';

// Applies Problem.violations[] onto the matching EditableType error slots (by parsing
// indexed paths like "fields[1].name", "fields[2].options[0].label"). Returns a new EditableType
// (immutable update). Unparseable/unmatched paths land on the type-level `form` slot — never dropped.
export function routeViolations(type: EditableType, problem: Problem): EditableType;

// Maps a business Problem.code to where its (already-localized) message should show.
export type ErrorPlacement =
  | { target: 'typeName' } | { target: 'form' }
  | { target: 'fieldIcon'; clientId: string };
export function placeBusinessError(code: string): ErrorPlacement;
```

Business-code placement (message shown **verbatim** from `Problem.message`; client keys only for pre-upload
rejections that have no server message):

| Server code | Placement |
|---|---|
| `annotation.type.name.taken` (409) | type name field |
| `annotation.type.not_found` (404) | form-level — must make clear the change was **not** saved |
| `annotation.image.too_large` (400) | the icon control being uploaded |
| `annotation.image.type.unsupported` (400) | the icon control being uploaded |
| `validation.failed` (400) + `violations[]` | routed per-violation via `routeViolations` |

## 5. Client pre-validation — `lib/validation/annotationTypeValidation.ts` (NEW; `loginValidation` idiom)

```ts
import type { EditableType } from '@/lib/annotationTypes/viewModel';
import type { MessageKey } from '@/lib/i18n/messages/en';

// Pure, synchronous. Writes MessageKeys into the aggregate's error slots and returns it (immutable),
// plus a boolean gate — mirrors validateLogin/isSubmittable.
export function validateType(type: EditableType): EditableType;      // fills errors slots
export function isSubmittable(type: EditableType): boolean;          // true when no slot is set

// Icon pre-check (content-type + size) before any upload request.
export const ALLOWED_IMAGE_TYPES: readonly string[];  // ['image/png','image/jpeg','image/gif','image/webp']
export const MAX_IMAGE_BYTES: number;                 // 5 * 1024 * 1024
export function validateIconFile(file: File): MessageKey | null;     // null = ok
```

Client-enforced pre-checks (server remains authoritative): type name required; each field name required; field
type required (always set via the select); `numberMin ≤ numberMax` when both present; each option label
required; Secret allowed only on TEXT/FREE_TEXT (enforced by rendering, re-checked here); icon content-type ∈
allowed set and size ≤ 5 MB.

## 6. Message-key namespace — `lib/i18n/messages/en.ts` + `pt.ts` (ADD; flat dot-keys, keysets identical)

New client-owned keys (server error messages are shown verbatim, **not** re-keyed). Namespaces:
`annotationType.*` (screens/actions/labels), `field.*` (field editor), `option.*` (option editor),
`fieldType.<TYPE>` (the seven type labels), `badgeColour.<COLOUR>` (palette labels), and client-only validation
messages under `annotationType.validation.*` / `image.validation.*`.

```
annotationType.list.title            annotationType.list.new           annotationType.list.empty
annotationType.action.view|edit|delete
annotationType.builder.title.new|edit   annotationType.name.label|required
annotationType.icon.label|choose|remove
annotationType.delete.title|confirm|irreversible|cancel
annotationType.save|cancel|saving
annotationType.notFound            annotationType.saveFailed
field.add|remove|moveUp|moveDown   field.name.label|required   field.type.label
field.visible.label                field.number.min|max|bounds.invalid
field.secret.label                 field.icon.label
option.add|remove|moveUp|moveDown  option.label.label|required  option.colour.label
fieldType.TEXT|LIST|NUMBER|FREE_TEXT|SINGLE_CHOICE|MULTIPLE_CHOICE|IMAGE
badgeColour.RED|GREEN|BLUE|BLACK|GRAY|YELLOW
image.validation.tooLarge          image.validation.typeUnsupported
```

> `pt.ts` is typed `Record<MessageKey, string>`, so any key added to `en` **must** be mirrored or the build
> fails (C-09 coverage is compile-enforced; a runtime keyset test also asserts it).

## 7. Component prop contracts — `components/annotationTypes/*` (NEW; named-export `'use client'`)

Controlled `value` + `onChange` throughout (lifted state in `TypeBuilderForm`; no state library).

```ts
export function AnnotationTypeList(props: { types: AnnotationTypeDto[]; onNew(): void;
  onView(id: string): void; onEdit(id: string): void; onDelete(id: string): void; }): JSX.Element;

export function TypeBuilderForm(props: { initial: EditableType; mode: 'create' | 'edit';
  onSaved(saved: AnnotationTypeDto): void; onCancel(): void; }): JSX.Element;

export function FieldEditor(props: { field: EditableField; onChange(next: EditableField): void;
  onRemove(): void; onMoveUp(): void; onMoveDown(): void; }): JSX.Element;

export function OptionsEditor(props: { fieldType: FieldType; options: EditableOption[];
  onChange(next: EditableOption[]): void; }): JSX.Element;

export function BadgeColourPicker(props: { value: BadgeColour | null;
  onChange(next: BadgeColour | null): void; }): JSX.Element;

export function FieldIconEditor(props: { imageId: string | null; objectUrl: string | null;
  onUploaded(ref: ImageRefDto, objectUrl: string): void; onRemove(): void;
  onError(key: MessageKey): void; }): JSX.Element;

export function DeleteTypeDialog(props: { typeName: string;
  onConfirm(): void; onCancel(): void; }): JSX.Element;
```

(Sibling editors — `TypeHeaderEditor`, `FieldList`, `FieldTypeSelect`, `VisibleForViewingToggle`,
`NumberBoundsEditor`, `SecretToggle`, `OptionRow`, `IconThumbnail` — follow the same controlled
`value`/`onChange` shape; their props are the obvious subset and are pinned at implement time.)

## 8. Test contract — `test/msw/handlers.ts` (ADD factories + fixtures)

Factory handlers (the existing `server.use(...)` per-case idiom), leading-wildcard URLs:

```ts
export function typesListSuccess(types?: AnnotationTypeDto[]): RequestHandler; // GET  *​/annotation-types
export function typeGetSuccess(type: AnnotationTypeDto): RequestHandler;       // GET  *​/annotation-types/:id
export function typeCreateSuccess(saved?: AnnotationTypeDto): RequestHandler;  // POST *​/annotation-types -> 201
export function typeReplaceSuccess(saved?: AnnotationTypeDto): RequestHandler; // PUT  *​/annotation-types/:id
export function typeDeleteSuccess(): RequestHandler;                           // DELETE -> 204
export function typeNameTaken(): RequestHandler;                               // 409 annotation.type.name.taken
export function typeNotFound(): RequestHandler;                                // 404 annotation.type.not_found
export function typeValidationFailed(violations: Violation[]): RequestHandler; // 400 validation.failed
export function imageUploadSuccess(ref?: ImageRefDto): RequestHandler;         // POST *​/images -> 201
export function imageTooLarge(): RequestHandler;                               // 400 annotation.image.too_large
export function imageTypeUnsupported(): RequestHandler;                        // 400 annotation.image.type.unsupported
export const TYPE_RABBITMQ: AnnotationTypeDto;                                 // shared fixture
```

Every one of the spec's 30 scenarios maps to a Vitest + Testing-Library test seeding these handlers; the
seven-field-type coverage and the D2-default `Scenario Outline` become **table tests**. `viewModel` (toInput
narrowing / fromDto), `annotationTypeValidation`, and `violationRouting` also carry direct unit tests.
