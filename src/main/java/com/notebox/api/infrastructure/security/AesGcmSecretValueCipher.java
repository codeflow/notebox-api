package com.notebox.api.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.application.crypto.EncryptedValue;
import com.notebox.api.application.crypto.SecretValueCipher;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * AES-256-GCM implementation of {@link SecretValueCipher} (AD-14): a fresh random 96-bit IV per
 * value, a 128-bit authentication tag, and a versioned keyring (mirrors the JWT-key config pattern)
 * so ciphertext written under an older key still decrypts after rotation. Fails fast at startup if
 * the active key version has no configured key or the configured key is not 256-bit.
 */
@ApplicationScoped
public class AesGcmSecretValueCipher implements SecretValueCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_PROPERTY_PREFIX = "notebox.crypto.keys.";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Map<Integer, SecretKeySpec> keyCache = new ConcurrentHashMap<>();
    private final Config config;
    private final int activeKeyVersion;

    public AesGcmSecretValueCipher(
            Config config, @ConfigProperty(name = "notebox.crypto.active-key-version") int activeKeyVersion) {
        this.config = config;
        this.activeKeyVersion = activeKeyVersion;
        keyFor(activeKeyVersion); // fail fast if the active key is missing
    }

    @Override
    public EncryptedValue encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyFor(activeKeyVersion), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(ciphertext, iv, activeKeyVersion);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to encrypt secret value", e);
        }
    }

    @Override
    public String decrypt(EncryptedValue value) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    keyFor(value.keyVersion()),
                    new GCMParameterSpec(TAG_LENGTH_BITS, value.iv()));
            byte[] plaintext = cipher.doFinal(value.ciphertext());
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to decrypt secret value", e);
        }
    }

    private SecretKeySpec keyFor(int version) {
        return keyCache.computeIfAbsent(version, v -> {
            String base64 = config.getOptionalValue(KEY_PROPERTY_PREFIX + v, String.class)
                    .orElseThrow(() -> new IllegalStateException("no crypto key configured for version " + v));
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            if (keyBytes.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException("crypto key version " + v + " must decode to "
                        + KEY_LENGTH_BYTES + " bytes (AES-256), got " + keyBytes.length);
            }
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        });
    }
}
