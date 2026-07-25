package com.notebox.api.application.crypto;

/**
 * A Secret field value encrypted at rest (AD-14): the ciphertext, the IV used to produce it, and the
 * key version so the correct key can be selected on decrypt (rotation without rewriting rows).
 */
public record EncryptedValue(byte[] ciphertext, byte[] iv, int keyVersion) {
}
