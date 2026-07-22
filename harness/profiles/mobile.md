# Harness — Mobile (the project)

> Type-specific layers for a mobile app. Stack: `TBD`. Language: English.

## verify
```
# fill for TBD — e.g. lint + typecheck + unit tests
npm run verify   # or the platform equivalent
```

## Build & distribution
- Android: release keystore stored securely (never in repo); distribution via internal track / Firebase App Distribution.
- iOS: cloud build (e.g., CI macOS runner / EAS) if no local macOS toolchain; signing certs as CI secrets.
- Config via build-time env (`.env` per variant); no hardcoded URLs/keys.

## Tests
- Unit + component. E2E with Detox/Maestro for critical flows.

## Security
- Secure local storage (Keychain/Keystore) for tokens; never hardcode encryption keys.
- E2E encryption for sensitive content where required (cross-ref constitution/02-compliance.md).
