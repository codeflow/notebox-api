# Harness — Web (the project)

> Type-specific layers for a web frontend. Stack: `TBD`. Language: English.

## verify
```
# fill for TBD — e.g. lint + typecheck + unit + build (+ a11y)
npm run verify   # lint && typecheck && test && build
```

## Reproducible local env
- `.env.example` (use the framework's public/server var split; never expose secrets to the client).
- `npm run dev` for local; document the backend/API base URL.

## Tests
- Component/unit + critical-path E2E (Playwright/Cypress). Gherkin acceptance → E2E specs.
- Responsive checks for mobile breakpoints.

## Deploy
- Static/SSR per framework. Prefer a free, no-LB path (managed host / container scale-to-zero).
- Custom domain + managed TLS. `noindex` for private/admin apps.

## Security
- Security headers (HSTS, X-Frame-Options, nosniff, CSP where feasible).
- Auth via httpOnly cookies / BFF; secrets server-side only.
