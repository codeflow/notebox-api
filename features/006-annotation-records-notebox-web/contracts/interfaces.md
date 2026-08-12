# Contracts — Annotation record editor (client interfaces)

Signatures the implementer codes against and the auditor checks. Wire shapes mirror
`features/005-annotation-records-notebox-api/contracts/rest-api.md` — the server is authoritative.

## 1. Wire types (`lib/api/types.ts` additions)

```ts
export interface AnnotationValueInput {
  fieldId: string;
  text?: string | null;        // TEXT / FREE_TEXT (rich HTML) / secret; null = masked no-op echo
  number?: number | null;      // NUMBER
  imageId?: string | null;     // IMAGE
  optionIds?: string[] | null; // SINGLE_CHOICE / LIST (exactly 1) · MULTIPLE_CHOICE (0..n)
  clearSecret?: boolean;       // secret fields only: audited erasure
}

export interface AnnotationRecordInput {
  annotationTypeId?: string;   // POST only; PUT omits (path id fixes the record)
  name: string;                // ≤120
  values: AnnotationValueInput[];  // at most one entry per defined field
}

export interface AnnotationValueDto {
  fieldId: string;
  fieldType: FieldType;
  secret: boolean;
  masked: boolean;             // true ⇒ text is null and cleartext exists server-side
  text: string | null;
  number: number | null;
  imageId: string | null;
  optionIds: string[] | null;
}

export interface AnnotationRecordDto {
  id: string;
  annotationTypeId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  values: AnnotationValueDto[];   // ordered by the type's field position; valueless fields omitted
}

export interface RevealResponse {
  recordId: string;
  fieldId: string;
  value: string;               // cleartext — display-only, never stored/logged client-side
}
```

## 2. API client (`lib/api/annotationRecordsClient.ts`)

```ts
export interface AnnotationRecordsClient {
  create(input: AnnotationRecordInput): Promise<AnnotationRecordDto>;            // POST /api/annotation-records
  get(id: string): Promise<AnnotationRecordDto>;                                 // GET  /api/annotation-records/{id}
  replace(id: string, input: AnnotationRecordInput): Promise<AnnotationRecordDto>; // PUT
  remove(id: string): Promise<void>;                                             // DELETE (204)
  reveal(recordId: string, fieldId: string): Promise<RevealResponse>;            // POST …/values/{fieldId}/reveal
}
```
All through `authFetch` (bearer attached, `ApiError` on non-2xx carrying the `Problem` /
`validation.failed` envelope). No tenant parameter exists anywhere (C-01).

## 3. View-model (`lib/annotationRecords/viewModel.ts`)

```ts
export function fromType(type: AnnotationTypeDto): EditableRecord;                       // create mode
export function fromDto(record: AnnotationRecordDto, type: AnnotationTypeDto): EditableRecord; // edit mode
export function toInput(record: EditableRecord, mode: 'create' | 'edit'): AnnotationRecordInput;
```
(`EditableRecord` / `EditableValue` / `SecretValueState` per `data-model.md`; `toInput` is the only
producer of wire bodies and is total over `SecretValueState`.)

## 4. Sanitizer (`lib/annotationRecords/sanitize.ts`)

```ts
export function sanitizeRichText(html: string): string;  // DOMPurify, allow-list per data-model.md
```
Contract: output contains only the dialect table's elements/attributes; `script`, event handlers,
`iframe`, non-http(s) URLs and `img[src]` never survive. Hostile-fixture test is part of this contract.

## 5. Components (`components/annotationRecords/`)

```ts
// Create/edit orchestrator. Routes violations[] onto controls; onSaved receives the server DTO.
export function RecordForm(props: {
  type: AnnotationTypeDto;
  initial: EditableRecord;
  mode: 'create' | 'edit';
  onSaved(record: AnnotationRecordDto): void;
  onCancel(): void;
}): ReactElement;

// One control per field — dispatch on field.fieldType (+ field.secret).
export function ValueField(props: {
  field: FieldDto;
  value: EditableValue;
  violation?: string | null;            // localized server message for this control
  onChange(next: EditableValue): void;
}): ReactElement;

// Secret three-state control: masked placeholder · replace input · explicit clear affordance.
export function SecretValueField(props: {
  field: FieldDto;
  state: SecretValueState;
  onChange(next: SecretValueState): void;
}): ReactElement;

// TipTap wrapper; emits sanitized-dialect HTML. Toolbar per design screen 13.
export function RichTextEditor(props: {
  value: string;                         // dialect HTML (sanitized before hydration)
  onChange(html: string): void;
  uploadImage(file: File): Promise<string>;  // returns imageId; pre-validated like any image value
}): ReactElement;

// Read-side renderer: sanitize → resolve data-image-id → highlight code blocks.
export function RichTextValue(props: { html: string }): ReactElement;

// Image field value: pre-validated upload + thumbnail (reuses imagesClient + feat-004 validation).
export function ImageValueField(props: {
  value: string | null;                  // imageId
  violation?: string | null;
  onChange(imageId: string | null): void;
}): ReactElement;

// Detail-view masked secret; Reveal only when role === 'ADMIN'; component-local reveal state.
export function RevealableValue(props: {
  recordId: string;
  field: FieldDto;
}): ReactElement;

// Irreversible-delete confirmation (clone of DeleteTypeDialog semantics).
export function DeleteRecordDialog(props: {
  recordName: string;
  open: boolean;
  onConfirm(): void;
  onCancel(): void;
}): ReactElement;
```

## 6. Routes

| Route | Does |
|---|---|
| `app/(app)/annotation-types/[id]/records/new/page.tsx` | fetch type → `RecordForm` (create) → detail |
| `app/(app)/annotation-types/[id]/records/[recordId]/page.tsx` | fetch type+record → detail (masked, Reveal, Delete) |
| `app/(app)/annotation-types/[id]/records/[recordId]/edit/page.tsx` | fetch type+record → `RecordForm` (edit) → detail |

All behind the existing `RouteGuard`. The type detail page gains one "New record" toolbar action.

## 7. Dependencies added (`package.json`)
`@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-underline`, `@tiptap/extension-text-style`,
`@tiptap/extension-color`, `@tiptap/extension-link`, `@tiptap/extension-image` (customized for
`data-image-id`), `@tiptap/extension-code-block-lowlight`, `lowlight`, `dompurify` (+`@types/dompurify`).
Rationale and rejected alternative in `plan.md` §5.

## 8. i18n
Namespace `annotationRecords.*` in `lib/i18n/messages/{en,pt}.ts` — form labels, toolbar actions
(each localized), masked placeholder, reveal/clear affordances and their confirmations, delete dialog
strings, local image rejections. Coverage enforced by the existing `keysetCoverage.test.ts`.
