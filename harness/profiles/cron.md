# Harness — Cron / Scheduled Job (the project)

> Type-specific layers for a scheduled/batch job. Stack: `TBD`. Language: English.

## verify
```
# fill for TBD — e.g. lint + typecheck + unit tests of the job logic
make verify
```

## Run model
- The job must be **idempotent** (safe to re-run) and **observable** (logs + metrics + exit code).
- Local: a single command runs one cycle (`make run-once`) with `.env` config.
- Schedule via the platform scheduler (cron, Cloud Scheduler, k8s CronJob). Keep the schedule in IaC/docs.

## Tests
- Unit-test the job logic with fixtures. Test the empty/zero-work case and partial-failure handling.

## Deploy
- Containerize; the scheduler invokes it (HTTP/Run job/Pub-Sub trigger).
- Auth machine-to-machine (service account / OIDC), never a long-lived shared token if avoidable.

## Security
- Least-privilege identity; secrets via secret manager.
- Alert on failure; log a run summary (processed / skipped / failed).
