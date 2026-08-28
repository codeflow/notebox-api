package com.notebox.api.application.member;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.application.auth.PasswordHasher;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.error.CannotDeactivateSelfException;
import com.notebox.api.domain.error.EmailAlreadyRegisteredException;
import com.notebox.api.domain.error.MemberNotFoundException;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.User;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.CredentialLookup;
import com.notebox.api.infrastructure.persistence.UserRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Member administration (FR-17, C-03, C-10).
 *
 * <p>This is the surface OQ-11 decided on — provisioning is administered, not self-service — and
 * it is the reason the sign-in screen's "Add account" and "Forgot password?" had nothing to call.
 * Every act here is an administrative one, so every act is audited.
 */
@ApplicationScoped
public class MemberService {

    static final String ACTION_MEMBER_CREATED = "MEMBER_CREATED";
    static final String ACTION_MEMBER_PASSWORD_RESET = "MEMBER_PASSWORD_RESET";
    static final String ACTION_MEMBER_ACTIVE_CHANGED = "MEMBER_ACTIVE_CHANGED";
    static final String TARGET_USER = "USER";

    private final UserRepository users;
    private final CredentialLookup credentials;
    private final PasswordHasher passwordHasher;
    private final AuditLogRepository auditLog;
    private final TenantContext tenant;

    public MemberService(
            UserRepository users,
            CredentialLookup credentials,
            PasswordHasher passwordHasher,
            AuditLogRepository auditLog,
            TenantContext tenant) {
        this.users = users;
        this.credentials = credentials;
        this.passwordHasher = passwordHasher;
        this.auditLog = auditLog;
        this.tenant = tenant;
    }

    /** Every member of the caller's tenant. */
    @Transactional
    public List<User> list() {
        return users.listAllInTenant();
    }

    /**
     * Creates a member in the caller's own tenant.
     *
     * <p>The tenant is taken from the token, never from the request — an administrator cannot
     * provision into someone else's workspace (BR-01, C-01).
     *
     * @throws EmailAlreadyRegisteredException when the address is already in use
     */
    @Transactional
    public User create(String email, String displayName, Role role, String rawPassword) {
        String normalized = email.trim().toLowerCase();
        // Email is globally unique in the schema, so a duplicate ANYWHERE would fail on insert.
        // Checking here turns that into a clean violation instead of a constraint error — but the
        // response says only "already registered", never in which tenant (BR-01).
        if (credentials.findByEmail(normalized).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(
                UUID.randomUUID(),
                tenant.tenantId(),
                normalized,
                passwordHasher.hash(rawPassword),
                role,
                displayName.trim());
        users.persistInTenant(user);
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_MEMBER_CREATED, TARGET_USER, user.getId(),
                "role=" + role.name()));
        return user;
    }

    /**
     * Sets a member's password on their behalf — the administered answer to "Forgot password?".
     *
     * <p>The audit entry records that a reset happened and who did it; it never records the value.
     */
    @Transactional
    public void resetPassword(UUID userId, String rawPassword) {
        User user = users.findByIdInTenant(userId).orElseThrow(MemberNotFoundException::new);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_MEMBER_PASSWORD_RESET, TARGET_USER, userId));
    }

    /**
     * Activates or deactivates a member. Deactivation is how access is withdrawn without deleting
     * anything the member authored (C-11).
     */
    @Transactional
    public User setActive(UUID userId, boolean active) {
        User user = users.findByIdInTenant(userId).orElseThrow(MemberNotFoundException::new);
        if (userId.equals(tenant.userId()) && !active) {
            // Locking yourself out is not an administrative action, it is an accident.
            throw new CannotDeactivateSelfException();
        }
        user.setActive(active);
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_MEMBER_ACTIVE_CHANGED, TARGET_USER, userId,
                "active=" + active));
        return user;
    }
}
