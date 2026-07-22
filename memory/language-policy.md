---
name: language-policy
description: Project artifacts and Claude replies are English; the user writes prompts in Portuguese
metadata:
  type: feedback
---

All project files and artifacts (PRD, specs, constitution, catalogs, tasks, commits, comments) are authored in **English**, and Claude's session replies are in **English** too. The user will often write prompts in Portuguese, but that never switches the output language — Portuguese input is translated into English when it becomes an artifact.

**Why:** `project.language` was set to `en` at setup; the user confirmed explicitly (2026-07-22) that everything created in the project must be English even though they prompt in Portuguese.

**How to apply:** Never switch artifact or reply language based on the language of a prompt. Only an explicit request to change `project.language` does that.
