package com.notebox.api.infrastructure.i18n;

import java.text.MessageFormat;
import java.util.Locale;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;

import com.notebox.api.domain.MessageOverride;
import com.notebox.api.infrastructure.persistence.MessageOverrideRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Layers a tenant's own wording over the product's bundled messages (FR-16).
 *
 * <p>Resolution order: the caller's tenant override for that locale, then the bundle (which itself
 * falls back to English). Two properties matter more than the lookup:
 *
 * <ul>
 *   <li><b>It never fails.</b> An unauthenticated request has no tenant, and a database that will
 *       not answer must not turn a validation message into a stack trace — both fall through to the
 *       bundle. A message catalog that can break the response it is describing is worse than one
 *       that is merely out of date.
 *   <li><b>Absence is meaningful.</b> No override means the product's own words, not an empty
 *       string, which is what keeps BR-08's "never a raw key or blank" true.
 * </ul>
 */
@RequestScoped
@Alternative
@Priority(10)
public class TenantMessageResolver implements MessageResolver {

    private final ResourceBundleMessageResolver bundle;
    private final MessageOverrideRepository overrides;
    private final TenantContext tenant;

    public TenantMessageResolver(
            ResourceBundleMessageResolver bundle,
            MessageOverrideRepository overrides,
            TenantContext tenant) {
        this.bundle = bundle;
        this.overrides = overrides;
        this.tenant = tenant;
    }

    @Override
    public String resolve(String key, Locale locale, Object... args) {
        String template = override(key, locale);
        if (template == null) {
            return bundle.resolve(key, locale, args);
        }
        if (args == null || args.length == 0) {
            return template;
        }
        return new MessageFormat(template, locale).format(args);
    }

    private String override(String key, Locale locale) {
        if (!tenant.authenticated()) {
            return null;
        }
        try {
            return overrides.find(locale.getLanguage(), key).map(MessageOverride::getValue).orElse(null);
        } catch (RuntimeException unavailable) {
            // Deliberately swallowed: see the class comment. The bundle is always reachable.
            return null;
        }
    }
}
