package com.notebox.api.application.organization;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.application.auth.JwtIssuer;
import com.notebox.api.application.auth.PasswordHasher;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.domain.error.EmailAlreadyRegisteredException;
import com.notebox.api.domain.error.OrganizationSlugTakenException;
import com.notebox.api.domain.error.SignupDisabledException;
import com.notebox.api.infrastructure.persistence.CredentialLookup;
import com.notebox.api.infrastructure.persistence.TenantRepository;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Creates an organization and its first administrator in one act.
 *
 * <p>Notebox is multi-tenant but had no way to create a tenant. Members are provisioned by an
 * administrator (OQ-11), which works for everyone EXCEPT the first administrator of a brand new
 * organization — they have nobody to provision them. This is that one entry point, and it is the
 * only unauthenticated write in the product.
 *
 * <p>It is therefore <b>configurable</b>: a deployment that provisions organizations by other means
 * sets {@code notebox.signup.enabled=false} and this endpoint refuses. The default is open, because
 * a self-hosted Notebox with no way in is not usable.
 */
@ApplicationScoped
public class OrganizationService {

    private final TenantRepository tenants;
    private final CredentialLookup credentials;
    private final PasswordHasher passwordHasher;
    private final JwtIssuer jwtIssuer;
    private final boolean signupEnabled;

    public OrganizationService(
            TenantRepository tenants,
            CredentialLookup credentials,
            PasswordHasher passwordHasher,
            JwtIssuer jwtIssuer,
            @ConfigProperty(name = "notebox.signup.enabled") boolean signupEnabled) {
        this.tenants = tenants;
        this.credentials = credentials;
        this.passwordHasher = passwordHasher;
        this.jwtIssuer = jwtIssuer;
        this.signupEnabled = signupEnabled;
    }

    /**
     * Creates the organization and signs its administrator in.
     *
     * <p>The two are created together on purpose: an organization with no administrator is a row
     * nobody can reach, and this is the only place a tenant can be born.
     *
     * @param name the organization's display name
     * @param slug its address, unique across the deployment
     * @param displayName the first administrator's name
     * @param email their address
     * @param rawPassword their password
     * @return a token, so the caller is signed in rather than sent back to the login screen
     */
    @Transactional
    public JwtIssuer.IssuedToken signUp(
            String name, String slug, String displayName, String email, String rawPassword) {
        if (!signupEnabled) {
            throw new SignupDisabledException();
        }
        String address = slug.trim().toLowerCase();
        if (tenants.slugExists(address)) {
            throw new OrganizationSlugTakenException();
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (credentials.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        Tenant tenant = tenants.persist(new Tenant(UUID.randomUUID(), name.trim(), address));
        User admin = new User(
                UUID.randomUUID(),
                tenant.getId(),
                normalizedEmail,
                passwordHasher.hash(rawPassword),
                Role.ADMIN,
                displayName.trim());
        // Persisted directly rather than through the tenant-scoped repository: there is no caller
        // tenant yet, which is the whole point of this endpoint.
        tenants.persistUser(admin);
        return jwtIssuer.issue(admin);
    }
}
