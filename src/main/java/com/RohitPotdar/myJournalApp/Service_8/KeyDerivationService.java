package com.RohitPotdar.myJournalApp.Service_8;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Turns the user provided secret string into a strong AES key using PBKDF2.
 * We never store the derived key, only use it in-memory for encryption/decryption.
 */
@Service
public class KeyDerivationService {

    private static final String DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int ITERATIONS = 65_536;
    private static final int SALT_LENGTH_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a new random salt for every encryption operation.
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Derive a SecretKey for AES from the human readable secret and salt.
     */
    public SecretKey deriveKey(char[] secretKeyChars, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(secretKeyChars, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(DERIVATION_ALGORITHM);
            byte[] encoded = skf.generateSecret(spec).getEncoded();
            clearChars(secretKeyChars);
            return new SecretKeySpec(encoded, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key: " + e.getMessage(), e);
        }
    }

    /**
     * Small helper to clear the char array that held the secret phrase.
     */
    private void clearChars(char[] chars) {
        if (chars == null) {
            return;
        }
        for (int i = 0; i < chars.length; i++) {
            chars[i] = 0;
        }
    }
}

