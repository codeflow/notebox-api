package com.notebox.api.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import com.notebox.api.application.crypto.EncryptedValue;
import com.notebox.api.application.crypto.SecretValueCipher;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** AES-256-GCM encrypt/decrypt round-trip and ciphertext-at-rest guarantees (FR-18, AD-14, C-12). */
@QuarkusTest
class AesGcmSecretValueCipherTest {

    @Inject
    SecretValueCipher cipher;

    @Test
    void encrypt_producesCiphertextDifferentFromPlaintextWithIvAndKeyVersion() {
        String plaintext = "s3cr3t-token";

        EncryptedValue encrypted = cipher.encrypt(plaintext);

        assertNotEquals(plaintext, new String(encrypted.ciphertext(), StandardCharsets.UTF_8),
                "the stored bytes must never equal the plaintext");
        assertEquals(12, encrypted.iv().length, "GCM IV is 96 bits");
        assertEquals(1, encrypted.keyVersion(), "the active key version is tagged on the value");
    }

    @Test
    void decrypt_recoversTheOriginalPlaintext() {
        String plaintext = "s3cr3t-token";
        EncryptedValue encrypted = cipher.encrypt(plaintext);

        String decrypted = cipher.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_usesAFreshIvPerCall() {
        EncryptedValue first = cipher.encrypt("same-value");
        EncryptedValue second = cipher.encrypt("same-value");

        assertNotEquals(java.util.Base64.getEncoder().encodeToString(first.iv()),
                java.util.Base64.getEncoder().encodeToString(second.iv()), "IV must be fresh per encryption");
    }

    @Test
    void decrypt_rejectsATamperedCiphertext() {
        EncryptedValue encrypted = cipher.encrypt("s3cr3t-token");
        byte[] tampered = encrypted.ciphertext().clone();
        tampered[0] ^= 0x01;
        EncryptedValue corrupted = new EncryptedValue(tampered, encrypted.iv(), encrypted.keyVersion());

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(corrupted),
                "GCM authentication must reject a tampered ciphertext");
    }
}
