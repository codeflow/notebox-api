# feat-025 — Tasks

- [x] **T-01 · The decoration plugin**
      - files: `lib/annotationRecords/codeBlockLineNumbers.ts`, its test
      - covers: all five scenarios · **[C-1] [C-2]**
      - a widget at each line start of each code block, renumbering on every doc change
      - probe: number visual rows instead of logical lines → the wrapped-line assertion fails
      - verify: `npx vitest run lib/annotationRecords`

- [x] **T-02 · Wired into the editor, and proved not to reach storage**
      - files: `lib/annotationRecords/editorExtensions.ts`, `RichTextEditor.test.tsx`
      - covers: *"The numbers are not part of the document"* · **C-08**
      - probe: put the number into the node's text instead of a widget → the storage assertion fails
      - depends: T-01

- [x] **T-03 · The numbers line up with the read side**
      - files: `src/styles/adf-fusion.overrides.css`
      - **why it is a task:** the read side's numbers come from a CSS counter with a 42px hanging
        indent. Two mechanisms drawing the same thing must produce the same geometry, and jsdom
        computes neither.
      - check: a browser measurement of the gutter width and the first character's x, editor vs
        read view, on the same document
      - depends: T-02
