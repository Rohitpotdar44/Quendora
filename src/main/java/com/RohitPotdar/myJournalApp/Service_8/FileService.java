package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.SecureFileRepository;
import com.RohitPotdar.myJournalApp.entity_5.SecureFile;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Handles all business logic for encrypting, storing, and decrypting files.
 */
@Service
public class FileService {

    private final SecureFileRepository secureFileRepository;
    private final CryptoService cryptoService;
    private final userService_14 userService14;
    // Maximum bytes allowed for encrypted video uploads (tunable via properties)
    private final long maxVideoBytes;

    public FileService(SecureFileRepository secureFileRepository,
                       CryptoService cryptoService,
                       userService_14 userService14,
                       @Value("${app.file.max-video-bytes:10485760}") long maxVideoBytes) {
        this.secureFileRepository = secureFileRepository;
        this.cryptoService = cryptoService;
        this.userService14 = userService14;
        this.maxVideoBytes = maxVideoBytes;
    }

    /**
     * Encrypt the uploaded file with the provided secret key and save it to MongoDB.
     * @param isSensitive If true, encrypts title. If false, stores plaintext title for easy identification.
     */
    public SecureFile encryptAndSave(MultipartFile file, String title, String secretKey, String userName, boolean isSensitive) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("Secret key is required.");
        }

        User_12 user = userService14.findByUserName(userName);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found.");
        }

        if (user.getUniqueKeyHash() == null ||
                !userService14.matchesUniqueKey(secretKey, user.getUniqueKeyHash())) {
            throw new IllegalArgumentException("Secret key is incorrect. Use the key shared during signup.");
        }

        try {
            String contentType = StringUtils.hasText(file.getContentType())
                    ? file.getContentType()
                    : "application/octet-stream";

            // Guard rail: block oversized videos before we load and encrypt them
            if (isVideoContentType(contentType) && file.getSize() > maxVideoBytes) {
                throw new IllegalArgumentException("Videos larger than 10 MB are not allowed.");
            }

            byte[] fileBytes = file.getBytes();
            CryptoService.EncryptedPayload payload = cryptoService.encrypt(fileBytes, secretKey);

            // Always encrypt title for backup storage
            CryptoService.EncryptedPayload titlePayload = encryptTextField(title, secretKey);
            CryptoService.EncryptedPayload originalNamePayload = encryptTextField(file.getOriginalFilename(), secretKey);
            CryptoService.EncryptedPayload fileSizePayload = encryptTextField(String.valueOf(file.getSize()), secretKey);

            // Format file size for display
            String displaySize = formatFileSize(file.getSize());

            SecureFile secureFile = SecureFile.builder()
                    .userId(user.getId() != null ? user.getId().toString() : null)
                    .userName(userName)
                    .contentType(contentType)
                    .encryptedData(payload.cipherBytes())
                    .iv(payload.iv())
                    .salt(payload.salt())
                    .encryptedTitle(titlePayload != null ? titlePayload.cipherBytes() : null)
                    .titleIv(titlePayload != null ? titlePayload.iv() : null)
                    .titleSalt(titlePayload != null ? titlePayload.salt() : null)
                    .encryptedOriginalFileName(originalNamePayload != null ? originalNamePayload.cipherBytes() : null)
                    .originalFileNameIv(originalNamePayload != null ? originalNamePayload.iv() : null)
                    .originalFileNameSalt(originalNamePayload != null ? originalNamePayload.salt() : null)
                    .encryptedFileSize(fileSizePayload != null ? fileSizePayload.cipherBytes() : null)
                    .fileSizeIv(fileSizePayload != null ? fileSizePayload.iv() : null)
                    .fileSizeSalt(fileSizePayload != null ? fileSizePayload.salt() : null)
                    // NEW: Sensitivity flag and optional plaintext title
                    .isSensitive(isSensitive)
                    .plaintextTitle(isSensitive ? null : title)  // Only store if NOT sensitive
                    .displayFileSize(displaySize)
                    .createdAt(LocalDateTime.now())
                    .build();

            return secureFileRepository.save(secureFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read file bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt and return the file bytes when the user supplies the correct secret key.
     */
    public DecryptedFile decryptAndLoad(String fileId, String secretKey, String userName) {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("Secret key is required.");
        }

        User_12 user = userService14.findByUserName(userName);
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found.");
        }

        SecureFile secureFile = secureFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!userName.equals(secureFile.getUserName())) {
            throw new IllegalArgumentException("You can only access your own files.");
        }

        if (user.getUniqueKeyHash() == null ||
                !userService14.matchesUniqueKey(secretKey, user.getUniqueKeyHash())) {
            throw new IllegalArgumentException("Secret key is incorrect. Unable to decrypt.");
        }

        CryptoService.EncryptedPayload payload = new CryptoService.EncryptedPayload(
                secureFile.getEncryptedData(),
                secureFile.getIv(),
                secureFile.getSalt()
        );

        byte[] decryptedBytes = cryptoService.decrypt(payload, secretKey);
        String decryptedTitle = decryptTextField(secureFile.getEncryptedTitle(),
                secureFile.getTitleIv(),
                secureFile.getTitleSalt(),
                secretKey);
        String decryptedOriginalName = decryptTextField(secureFile.getEncryptedOriginalFileName(),
                secureFile.getOriginalFileNameIv(),
                secureFile.getOriginalFileNameSalt(),
                secretKey);
        String decryptedFileSize = decryptTextField(secureFile.getEncryptedFileSize(),
                secureFile.getFileSizeIv(),
                secureFile.getFileSizeSalt(),
                secretKey);

        return new DecryptedFile(secureFile,
                decryptedBytes,
                decryptedTitle,
                decryptedOriginalName,
                decryptedFileSize);
    }

    /**
     * Simple DTO that gives both metadata and decrypted bytes back to the controller.
     */
    public record DecryptedFile(SecureFile metadata,
                                byte[] fileBytes,
                                String title,
                                String originalFileName,
                                String fileSize) {}

    private CryptoService.EncryptedPayload encryptTextField(String value, String secretKey) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return cryptoService.encrypt(value.getBytes(StandardCharsets.UTF_8), secretKey);
    }

    private String decryptTextField(byte[] encrypted,
                                    byte[] iv,
                                    byte[] salt,
                                    String secretKey) {
        if (encrypted == null || iv == null || salt == null) {
            return null;
        }
        CryptoService.EncryptedPayload payload = new CryptoService.EncryptedPayload(encrypted, iv, salt);
        byte[] decrypted = cryptoService.decrypt(payload, secretKey);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // Simple helper so we can reuse the content-type check in other places later
    private boolean isVideoContentType(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
    }
    
    /**
     * Format file size in human-readable format (B, KB, MB, GB)
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Get all files for a specific user (returns metadata only, not encrypted bytes for performance).
     */
    public java.util.List<SecureFile> getAllFilesForUser(String userName) {
        User_12 user = userService14.findByUserName(userName);
        if (user == null) {
            throw new IllegalStateException("User not found.");
        }
        
        // Find all files for this user
        return secureFileRepository.findByUserName(userName);
    }

    /**
     * Delete a file if the user owns it.
     */
    public boolean deleteFile(String fileId, String userName) {
        SecureFile file = secureFileRepository.findById(fileId).orElse(null);
        
        if (file == null) {
            return false;
        }
        
        // Check ownership
        if (!userName.equals(file.getUserName())) {
            throw new IllegalArgumentException("You can only delete your own files.");
        }
        
        secureFileRepository.delete(file);
        return true;
    }
}

