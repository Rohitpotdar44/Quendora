package com.RohitPotdar.myJournalApp.Service_8;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Performs AES encryption/decryption on byte arrays.
 * This service is intentionally generic so the same logic works for text and files.
 */
@Service
public class CryptoService {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final KeyDerivationService keyDerivationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoService(KeyDerivationService keyDerivationService) {
        this.keyDerivationService = keyDerivationService;
    }

    /**
     * Encrypt plain bytes with the provided secret phrase.
     */
    public EncryptedPayload encrypt(byte[] plainBytes, String secret) {
        try {
            byte[] salt = keyDerivationService.generateSalt();
            byte[] iv = generateIv();

            SecretKey aesKey = keyDerivationService.deriveKey(secret.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            return new EncryptedPayload(cipherBytes, iv, salt);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt file: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt cipher bytes using the stored IV + salt and the user provided secret.
     */
    public byte[] decrypt(EncryptedPayload payload, String secret) {
        try {
            SecretKey aesKey = keyDerivationService.deriveKey(secret.toCharArray(), payload.salt());
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, payload.iv()));
            return cipher.doFinal(payload.cipherBytes());
        } catch (Exception e) {
            throw new IllegalArgumentException("Secret key is incorrect or file is corrupted.", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Wrapper class that carries everything required to decrypt later.
     */
    public record EncryptedPayload(byte[] cipherBytes, byte[] iv, byte[] salt) {
        public EncryptedPayload {
            cipherBytes = cipherBytes != null ? Arrays.copyOf(cipherBytes, cipherBytes.length) : null;
            iv = iv != null ? Arrays.copyOf(iv, iv.length) : null;
            salt = salt != null ? Arrays.copyOf(salt, salt.length) : null;
        }
    }
}

