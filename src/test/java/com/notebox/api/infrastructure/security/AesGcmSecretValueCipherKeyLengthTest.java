package com.notebox.api.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

/** The keyring only accepts 256-bit keys — a shorter key must fail fast, never downgrade the cipher (AD-14, audit F11). */
class AesGcmSecretValueCipherKeyLengthTest {

    private Config configWithKey(byte[] keyBytes) {
        Config config = mock(Config.class);
        when(config.getOptionalValue("notebox.crypto.keys.1", String.class))
                .thenReturn(Optional.of(Base64.getEncoder().encodeToString(keyBytes)));
        return config;
    }

    @Test
    void constructor_rejectsAKeyShorterThan256Bits() {
        Config config = configWithKey(new byte[16]);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new AesGcmSecretValueCipher(config, 1),
                "a 128-bit key must fail startup, not silently encrypt with AES-128");
        assertTrue(failure.getMessage().contains("32"), "the error names the required length");
        assertTrue(failure.getMessage().contains("version 1"), "the error names the offending key version");
    }

    @Test
    void constructor_acceptsA256BitKey() {
        Config config = configWithKey(new byte[32]);

        assertDoesNotThrow(() -> new AesGcmSecretValueCipher(config, 1));
    }
}
