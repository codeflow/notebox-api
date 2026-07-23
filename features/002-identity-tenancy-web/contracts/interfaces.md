# Contracts — Login & tenant-context UI (client seams)

**ID:** features/002-identity-tenancy-web · **Version:** v1 · **Date:** 2026-07-22

Signatures only — the seams the implementer codes against and the auditor checks. No implementation here.
The **consumed HTTP contract** is fixed by `feat-001` (`features/001-identity-tenancy/contracts/openapi.yaml`);
this feature only defines the client wrappers over it and its own internal module surface.

## Consumed API client interface
```ts
// lib/api/ApiError.ts
class ApiError extends Error {
  readonly problem: Problem;
  readonly status: number;
  constructor(problem: Problem, status: number);
  get code(): string;                       // problem.code
}

// lib/api/apiClient.ts
interface ApiClient {
  login(credentials: LoginRequest): Promise<LoginResponse>; // public; no bearer; throws ApiError on non-2xx
  getMe(): Promise<Me>;                                      // via authFetch; throws ApiError on 401
}

// lib/api/authFetch.ts
type SessionExpiredListener = (problem: Problem) => void;

function authFetch(input: RequestInfo, init?: RequestInit): Promise<Response>;
//  - attaches `Authorization: Bearer <token>` from TokenStore
//  - sends NO client-supplied tenant identifier (C-01)
//  - on non-2xx: parses Problem -> throws ApiError
//  - on 401 (any auth code): notifies registered SessionExpiredListener(s)
function onSessionExpired(listener: SessionExpiredListener): () => void; // returns unsubscribe
```

## Internal module / component contracts
```ts
// lib/auth/TokenStore.ts  — storage seam (sessionStorage impl; swappable to cookie/BFF)
interface TokenStore {
  get(): StoredToken | null;
  set(token: StoredToken): void;
  clear(): void;
}

// lib/auth/AuthProvider.tsx
interface AuthContextValue {
  status: AuthStatus;
  session: Session | null;
  me: Me | null;
  error: Problem | null;
  login(credentials: LoginRequest): Promise<void>; // 200 -> store token -> getMe -> authenticated
  logout(): void;                                  // clear TokenStore + session -> /login
}
function useAuth(): AuthContextValue;

// components/RouteGuard.tsx
interface RouteGuardProps { children: React.ReactNode }
//  status 'unknown'        -> ADF loading placeholder (hydration-stable)
//  anonymous + protected   -> redirect /login?redirect=<intended>
//  authenticated + /login  -> redirect (app) home
//  authenticated + protected -> render children

// components/BrandingBar.tsx  — renders tenant context in .af-branding / .af-globalLinks
//  "Conectado como {me.displayName}" + tenant context, sourced ONLY from useAuth().me; "Sair" -> logout()

// lib/i18n/useTranslation.ts
type Locale = 'en' | 'pt';
interface Translation {
  t(key: MessageKey, vars?: Record<string, string | number>): string; // never returns key/blank
  locale: Locale;
}
function useTranslation(): Translation;

// lib/validation/loginValidation.ts
interface LoginFieldErrors { email?: MessageKey; password?: MessageKey }
function validateLogin(input: LoginRequest): LoginFieldErrors; // pure; runs before any request
```

## i18n message-key surface (this feature; en + pt, both complete)
| Key | en | pt |
|---|---|---|
| `login.title` | Sign in | Entrar |
| `login.email.label` | Email | E-mail |
| `login.email.placeholder` | you@example.com | voce@exemplo.com |
| `login.password.label` | Password | Senha |
| `login.password.placeholder` | Your password | Sua senha |
| `login.submit` | Sign in | Entrar |
| `login.submitting` | Signing in… | Entrando… |
| `login.email.required` | Email is required | O e-mail é obrigatório |
| `login.password.required` | Password is required | A senha é obrigatória |
| `login.email.invalid` | Enter a valid email | Informe um e-mail válido |
| `login.error.generic` | Something went wrong. Please try again. | Algo deu errado. Tente novamente. |
| `session.expired` | Your session has expired. Please sign in again. | Sua sessão expirou. Entre novamente. |
| `session.signedInAs` | Signed in as | Conectado como |
| `branding.tenant` | Tenant | Organização |
| `branding.appName` | Notebox | Notebox |
| `app.logout` | Sign out | Sair |
| `app.loading` | Loading… | Carregando… |

**Notes.** `login.error.generic` is used only for 5xx/network (no `Problem` body). All server 401 messages
(e.g. `AUTH_INVALID_CREDENTIALS`) are rendered from `Problem.message` **verbatim** — not from this table.
The `en`/`pt` keysets must stay identical (coverage test — C-09).
