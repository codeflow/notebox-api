# definitions/ — PRD input inbox

Drop **any text** here that defines the product: meeting transcriptions, briefs, notes, requirements —
any extension (`.txt`, `.md`, …).

Then run **`/new-prd`**. It will:
- use every file in this folder (plus anything you paste) to **generate or complement** `prd/PRD_the project_vN.md`;
- if this folder is empty and you pass nothing, ask you to paste the definition;
- if the input is too thin, ask clarifying questions;
- **archive** the consumed input into `definitions/processed/definition_DDMMYYYYHHMMSS.<ext>` and clear this inbox.

`definitions/processed/` always holds the **last processed definition**.
