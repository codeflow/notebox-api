package com.notebox.api.application.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

/**
 * Argon2id password hashing (BR-04-independent; C-04/C-05). Hashes are encoded as
 * {@code argon2id$m=<kib>,t=<iters>,p=<par>$<saltB64>$<hashB64>} so parameters travel with the hash.
 */
@ApplicationScoped
public class PasswordHasher {

    private static final int MEMORY_KIB = 65536;
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return "argon2id$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public boolean verify(String password, String encoded) {
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !"argon2id".equals(parts[0])) {
            return false;
        }
        int memory = MEMORY_KIB;
        int iterations = ITERATIONS;
        int parallelism = PARALLELISM;
        for (String p : parts[1].split(",")) {
            String[] kv = p.split("=");
            switch (kv[0]) {
                case "m" -> memory = Integer.parseInt(kv[1]);
                case "t" -> iterations = Integer.parseInt(kv[1]);
                case "p" -> parallelism = Integer.parseInt(kv[1]);
                default -> { }
            }
        }
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] salt = b64.decode(parts[2]);
        byte[] expected = b64.decode(parts[3]);
        byte[] actual = derive(password, salt, memory, iterations, parallelism);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] derive(String password, byte[] salt, int memory, int iterations, int parallelism) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memory)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] hash = new byte[HASH_LENGTH];
        generator.generateBytes(password.getBytes(java.nio.charset.StandardCharsets.UTF_8), hash);
        return hash;
    }
}
