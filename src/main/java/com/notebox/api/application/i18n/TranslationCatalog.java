package com.notebox.api.application.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.TranslationDto;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.MessageOverride;
import com.notebox.api.domain.error.TranslationKeyUnknownException;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.MessageOverrideRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * The translation catalog an administrator edits at runtime (FR-16, design 17).
 *
 * <p>The set of KEYS is the product's, not the tenant's: a tenant rewords what the product says,
 * it does not invent new messages. That is why an unknown key is refused rather than created — an
 * override nothing resolves would be invisible until someone wondered why their wording never
 * appeared.
 */
@ApplicationScoped
public class TranslationCatalog {

    static final String ACTION_TRANSLATION_SET = "TRANSLATION_SET";
    static final String ACTION_TRANSLATION_CLEARED = "TRANSLATION_CLEARED";
    static final String TARGET_TRANSLATION = "TRANSLATION";

    private static final String BASE_NAME = "messages";

    private final MessageOverrideRepository overrides;
    private final AuditLogRepository auditLog;
    private final TenantContext tenant;

    public TranslationCatalog(
            MessageOverrideRepository overrides, AuditLogRepository auditLog, TenantContext tenant) {
        this.overrides = overrides;
        this.auditLog = auditLog;
        this.tenant = tenant;
    }

    /** Every key the product defines, with this tenant's wording where it has one. */
    @Transactional
    public List<TranslationDto> list(Locale locale) {
        Map<String, String> defaults = defaults(locale);
        Map<String, String> mine = overrides.listForLocale(locale.getLanguage()).stream()
                .collect(Collectors.toMap(MessageOverride::getMessageKey, MessageOverride::getValue));

        return defaults.entrySet().stream()
                .map(entry -> new TranslationDto(
                        entry.getKey(),
                        entry.getValue(),
                        mine.get(entry.getKey()),
                        mine.containsKey(entry.getKey())))
                .toList();
    }

    /**
     * Sets this tenant's wording for one key.
     *
     * @param locale the locale being reworded
     * @param key the product message key
     * @param value the tenant's wording
     * @return the row as it now stands
     * @throws TranslationKeyUnknownException when the product defines no such message
     */
    @Transactional
    public TranslationDto set(Locale locale, String key, String value) {
        Map<String, String> defaults = defaults(locale);
        if (!defaults.containsKey(key)) {
            throw new TranslationKeyUnknownException();
        }
        String language = locale.getLanguage();
        MessageOverride override = overrides.find(language, key).orElse(null);
        if (override == null) {
            override = new MessageOverride(UUID.randomUUID(), tenant.tenantId(), language, key, value);
            overrides.persistInTenant(override);
        } else {
            override.setValue(value);
        }
        // The override ROW is the audited object — a message key is not a UUID, and the audit log's
        // target is. The locale and key go in the detail, where they are readable.
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_TRANSLATION_SET, TARGET_TRANSLATION,
                override.getId(), language + ":" + key));
        return new TranslationDto(key, defaults.get(key), value, true);
    }

    /** Drops this tenant's wording so the key falls back to the product's own. */
    @Transactional
    public void clear(Locale locale, String key) {
        String language = locale.getLanguage();
        overrides.find(language, key).ifPresent(override -> {
            UUID removedId = override.getId();
            overrides.remove(override);
            auditLog.persistInTenant(new AuditLog(
                    tenant.tenantId(), tenant.userId(), ACTION_TRANSLATION_CLEARED, TARGET_TRANSLATION,
                    removedId, language + ":" + key));
        });
    }

    private Map<String, String> defaults(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);
        Map<String, String> values = new TreeMap<>();
        for (String key : bundle.keySet()) {
            values.put(key, bundle.getString(key));
        }
        return values;
    }
}
