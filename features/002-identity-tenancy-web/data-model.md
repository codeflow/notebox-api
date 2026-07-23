# Data model — Login & tenant-context UI

**ID:** features/002-identity-tenancy-web · **Version:** v1 · **Date:** 2026-07-22

No database — this is a client feature. The model is the **client-side types, session state, and invariants**.
There is no migration. Shapes consumed from the API mirror the fixed `feat-001` contract exactly.

## Consumed API shapes (mirror the fixed feat-001 contract)
```ts
interface LoginRequest  { email: string; password: string }
interface LoginResponse { token: string; expiresAt: string /* ISO 8601 */ }
interface Me {
  userId: string; tenantId: string;
  role: 'MEMBER' | 'ADMIN';
  displayName: string;
  locale?: string | null;
}
interface Problem { code: string; message: string; correlationId?: string }
type AuthErrorCode =
  | 'AUTH_INVALID_CREDENTIALS' | 'AUTH_REQUIRED'
  | 'AUTH_TOKEN_EXPIRED'       | 'AUTH_TOKEN_INVALID';
```

## Client-side session / auth state
```ts
interface StoredToken { token: string; expiresAt: string }         // the ONLY thing persisted
interface Session     { token: string; expiresAt: string; me: Me } // in-memory working copy

type AuthStatus = 'unknown' | 'authenticating' | 'authenticated' | 'anonymous';
interface AuthState {
  status: AuthStatus;    // 'unknown' during boot rehydrate + /me validation
  session: Session | null;
  error: Problem | null; // last auth error (login failure), for verbatim display
}
```
`unknown` is the boot state (sessionStorage rehydrate + `/me` in flight); the guard shows a placeholder until
it resolves to `authenticated`/`anonymous`.

## Invariants
- **INV-W1 (BR-01, BR-02):** `tenantId`, `displayName`, `role` are read **only** from `Me` (i.e. from `/me`).
  Never from the login form, URL, or any client input. A tenant id in URL/input is ignored.
- **INV-W2 (C-04):** the password exists only transiently in the login form's controlled state; it is **never**
  persisted, never placed in `Session`/`StoredToken`, never logged.
- **INV-W3 (C-04):** the token lives only in the `TokenStore` (sessionStorage) + in-memory `Session` + the
  `Authorization` header. It is **never** logged, never put in a URL/query string, never in console output.
- **INV-W4:** a session is invalid when `now >= expiresAt` **or** any API call returns 401 → the session is
  cleared and the member returns to login.
- **INV-W5 (BR-01):** a new login fully replaces the prior `Session`/`Me`; no identity or tenant is carried
  across logins (no tenant leakage).
- **INV-W6 (BR-08, C-09):** UI-owned strings render via `t(key)` and never surface a raw key or blank; server
  `Problem.message` renders **verbatim** (already localized) and is never re-translated.
