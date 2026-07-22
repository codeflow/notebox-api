# Token Optimization

> How agents keep cost down without losing rigor. Language: English.

## Principles

1. **Read the minimum.** Prefer search (grep) over full-file reads. When you know the region,
   read only those lines. Never re-read a file you just edited to "verify" — the edit tool errors on failure.
2. **Delegate breadth.** Any "search many files / many naming conventions" task goes to an
   Explorer sub-agent. You keep its conclusion, not the file contents it scanned.
3. **Parallelize.** Issue independent tool calls in one turn. Spawn independent sub-agents
   concurrently and synthesize.
4. **Structured returns.** Ask sub-agents for compact, schema-shaped output (lists, verdicts,
   IDs) rather than prose recaps.
5. **Cache-aware.** Keep related work within a single context window when possible; long idle
   gaps lose the prompt cache and re-read context uncached.
6. **Bounded loops.** Discovery/verification loops stop on K consecutive empty rounds or a token
   budget — never run unbounded.
7. **Artifacts over memory.** Persist every decision in spec/plan/tasks/MEMORY so a fresh context
   can resume cheaply instead of re-deriving.

## Anti-patterns

- Reading an entire large file to find one symbol.
- Re-summarizing unchanged context every turn.
- Doing a broad codebase sweep inline instead of via a sub-agent.
- Verifying a finished edit by re-reading the whole file.

## Budgeting

For large tasks, scale fan-out to an explicit budget (e.g., "≤ N implementer agents"). Log what
was dropped if you cap coverage — silent truncation reads as "fully covered" when it isn't.
