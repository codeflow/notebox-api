---
description: Render the pipeline — what is done, what is running, what is next
argument-hint: "[--full]  (--full also shows completed substeps)"
---

Run `./bin/wf status $ARGUMENTS` and reproduce its output at the top of your reply.

Then add a short read of the state — not a restatement of the table:

- Which step is current, and whether it is actually the right one to be on.
- What is blocking anything blocked, and who has to unblock it.
- The single next action, with the model and effort that step declares.

If a step has been `in_progress` across several turns with no artifact to show for it, say so.
That is the signal the pipeline is stalled, and it is invisible unless someone names it.
