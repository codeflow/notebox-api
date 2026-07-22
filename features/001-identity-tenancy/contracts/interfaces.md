# Interfaces — Identity & tenancy (signatures only, no implementation)

These are the seams later features depend on. Signatures are the contract the implementer codes against
and the auditor checks. Package root `com.notebox.api`.

## TenantContext (`@RequestScoped`, package `...infrastructure.security`)
Populated by the JWT filter from the verified token; the only source of the caller's tenant/identity.
```java
public interface TenantContext {
    UUID tenantId();     // never null on an authenticated request
    UUID userId();
    Role role();         // MEMBER | ADMIN
}
```

## JwtIssuer (`@ApplicationScoped`, package `...application.auth`)
Issues a signed token for an authenticated user. Validation is handled by MicroProfile JWT (declarative).
```java
public interface JwtIssuer {
    IssuedToken issue(User user);            // RS256; claims sub/tenant/role/iss/iat/exp
    record IssuedToken(String token, Instant expiresAt) {}
}
```

## TenantScopedRepository<T> (`...infrastructure.persistence`) — AD-03 choke point
Base for every tenant-owned entity. Concrete repositories extend it; there is no other path to these
entities. Every query is filtered by `TenantContext.tenantId()`.
```java
public abstract class TenantScopedRepository<T> {
    protected abstract Class<T> entityType();
    public Optional<T> findByIdInTenant(UUID id);      // tenant predicate applied
    public List<T> listInTenant(int page, int size);   // paginated (NFR-08)
    public T persistInTenant(T entity);                // sets/validates tenant_id
    // No unscoped find/query is exposed.
}
```

## MessageResolver (`@ApplicationScoped`, package `...infrastructure.i18n`) — AD-05, BR-08
Resolves a message key to the request locale; falls back to English (OQ-07). Auth codes seeded en/pt.
```java
public interface MessageResolver {
    String resolve(String key, Locale locale, Object... args);   // never returns the raw key
}
```

## Error mapping (`...api.error`) — AD-11
Domain exceptions map to the `Problem` envelope via `ExceptionMapper`s; message localized via
`MessageResolver`. Canonical codes for this feature:

| Code | HTTP | When |
|------|------|------|
| `AUTH_INVALID_CREDENTIALS` | 401 | login with wrong email/password |
| `AUTH_REQUIRED` | 401 | protected request with no token |
| `AUTH_TOKEN_EXPIRED` | 401 | token past `exp` |
| `AUTH_TOKEN_INVALID` | 401 | signature/claims invalid |
| `RESOURCE_NOT_FOUND` | 404 | missing OR cross-tenant (no existence disclosure — BR-01) |
| `VALIDATION_FAILED` | 400 | request body fails Bean Validation (AD-07) |

## Notes
- Cross-tenant access returns `RESOURCE_NOT_FOUND` (404), identical to a genuinely missing id — the caller
  cannot distinguish "exists in another tenant" from "does not exist" (BR-01).
- `passwordHash` is never mapped into any response DTO (INV-3, C-04).
