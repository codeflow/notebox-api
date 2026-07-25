package com.notebox.api.application.crypto;

/**
 * Port for encrypting and decrypting Secret annotation-field values (FR-18, BR-10, AD-14). The
 * only implementation lives under {@code infrastructure/} — crypto primitives never appear in
 * {@code application/} or {@code domain/} (constitution §Forbidden).
 */
public interface SecretValueCipher {

    /** Encrypts plaintext with the active key version and a fresh IV. */
    EncryptedValue encrypt(String plaintext);

    /** Decrypts a value using the key version recorded on it, so rotation never breaks old rows. */
    String decrypt(EncryptedValue value);
}
