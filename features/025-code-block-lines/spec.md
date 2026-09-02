# feat-025 — Line numbers inside the rich-text editor's code block

## Origin

- **FR-04/FR-06** (rich text on a record) · **US-2.1**
- **Brief item 16.1**, verbatim: *"os blocos de código não mostram números de linha"*
- Deferred by fix-visual-pass with a stated reason: *"the numbers come from wrapping each line in
  `.nb-codeLine`, which `RichTextValue` does by hand on the read side. Doing it inside the editor
  needs a ProseMirror decoration — not CSS, and a real risk of breaking typing."*

**That reason was half right and worth revisiting.** It is a decoration, yes. But a ProseMirror
**widget decoration is not part of the document**: it changes no position, is not copied, and is
not typed into. The risk the note feared belongs to a different mechanism — inserting nodes — which
this does not do.

## In

- Line numbers down the left edge of every code block **in the editor**, matching the read side's
  numbering exactly.
- They renumber as the member types, on every document change.

## Out

- The read side. It already numbers, and it is what this is being matched to.
- Any change to what is **stored**. The dialect is `pre[data-language] > code` and stays that way.
- Copying: the numbers must not appear in copied text, which is a property of the mechanism rather
  than a feature.

## Assumptions

| # | Question | Decision | Rejected |
|---|---|---|---|
| **C-1** | Widget decorations, or a rendered gutter beside the block? | **Widget decorations**, one per line start. They live outside the document, so positions, undo, and copy are untouched. | An absolutely-positioned gutter — it has to re-derive line positions on every keystroke and drifts the moment a line wraps. |
| **C-2** | What about a **wrapped** long line? | **One number per logical line**, not per visual row — the same as the read side, which hangs the indent. A wrapped continuation is the same line of code. | Numbering visual rows: the numbers would then disagree with the read view of the same document. |
| **C-3** | Should the numbers be selectable? | **No.** `user-select: none`, and they are not in the document anyway, so a copy of the block yields clean code either way. | Leaving them selectable — pasting code with line numbers in it is the single most annoying thing a code block can do. |

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-04 — a code block reads the same in the editor as it does on the page

  Scenario: A code block is numbered from one
    Given the record editor with a code block of three lines
    Then the editor shows the numbers 1, 2 and 3 beside them

  Scenario: The numbers follow what is typed
    Given a code block of three lines
    When the member adds a fourth
    Then a fourth number appears
    And no number is left over when a line is removed

  Scenario: Two code blocks each count from one
    Given a document with two code blocks
    Then each starts its numbering at 1

  Scenario: The numbers are not part of the document      # [C-1] [C-3]
    Given a numbered code block
    Then the stored HTML contains no line-number text
    And the document's own text is exactly what the member typed

  Scenario: A paragraph is not numbered
    Given a document with a paragraph and a code block
    Then only the code block's lines carry numbers
```

## Compliance pre-flight

| Item | Verdict | Why |
|---|---|---|
| **C-08 Rich-text sanitization** | **applies** | The one item that matters. The decoration must not reach what is stored — if a number could survive into the saved HTML it would be new markup entering the dialect. Evidence: the "not part of the document" scenario asserts the serialized output. |
| **C-09 Localization** | not applicable | Digits, in every locale this product has. |
| Everything else | not applicable | No new data path, no new request, no new permission. |
