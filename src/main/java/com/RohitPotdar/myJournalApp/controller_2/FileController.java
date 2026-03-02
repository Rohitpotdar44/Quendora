package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.FileAIService;
import com.RohitPotdar.myJournalApp.Service_8.FileService;
import com.RohitPotdar.myJournalApp.entity_5.SecureFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller that exposes the two new APIs:
 * 1. Upload + encrypt
 * 2. Decrypt + download
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final FileAIService fileAIService;

    public FileController(FileService fileService, FileAIService fileAIService) {
        this.fileService = fileService;
        this.fileAIService = fileAIService;
    }

    /**
     * Get all files for the authenticated user (metadata only, no encrypted bytes).
     */
    @GetMapping
    public ResponseEntity<?> getAllFiles(Authentication authentication) {
        try {
            String userName = authentication.getName();
            List<SecureFile> files = fileService.getAllFilesForUser(userName);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Accepts a multipart file + title + secret key + sensitivity flag and stores the encrypted bytes.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("title") String title,
                                        @RequestParam("secretKey") String secretKey,
                                        @RequestParam(value = "isSensitive", defaultValue = "false") boolean isSensitive,
                                        Authentication authentication) {
        try {
            String userName = authentication.getName();
            SecureFile saved = fileService.encryptAndSave(file, title, secretKey, userName, isSensitive);

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", saved.getId());
            response.put("message", "File encrypted and stored successfully.");
            response.put("uploadedAt", saved.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Decrypt only the metadata (title, filename, size) without downloading the full file.
     */
    @PostMapping("/metadata/{fileId}")
    public ResponseEntity<?> decryptMetadata(@PathVariable String fileId,
                                             @RequestBody SecretKeyRequest request,
                                             Authentication authentication) {
        try {
            String userName = authentication.getName();
            FileService.DecryptedFile decrypted = fileService.decryptAndLoad(fileId, request.secretKey(), userName);

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("title", decrypted.title());
            response.put("originalFileName", decrypted.originalFileName());
            response.put("fileSize", decrypted.fileSize());
            response.put("contentType", decrypted.metadata().getContentType());
            response.put("message", "Metadata decrypted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Decrypts the stored binary and streams it back once the correct key is provided.
     */
    @PostMapping("/decrypt/{fileId}")
    public ResponseEntity<?> decryptFile(@PathVariable String fileId,
                                         @RequestBody SecretKeyRequest request,
                                         Authentication authentication) {
        try {
            String userName = authentication.getName();
            FileService.DecryptedFile decrypted = fileService.decryptAndLoad(fileId, request.secretKey(), userName);

            SecureFile metadata = decrypted.metadata();
            MediaType mediaType = metadata.getContentType() != null
                    ? MediaType.parseMediaType(metadata.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;

            String downloadName = decrypted.originalFileName() != null
                    ? decrypted.originalFileName()
                    : "secure-file";

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadName + "\"")
                    .header("X-Decrypted-Title", decrypted.title() != null ? decrypted.title() : "Untitled")
                    .header("X-Decrypted-FileName", downloadName)
                    .header("X-Decrypted-Size", decrypted.fileSize() != null ? decrypted.fileSize() : "Unknown")
                    .body(decrypted.fileBytes());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete a file by ID (only if user owns it and secret key is provided).
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId,
                                        @RequestBody SecretKeyRequest request,
                                        Authentication authentication) {
        try {
            String userName = authentication.getName();
            
            // Secret key is compulsory for deletion
            if (request.secretKey() == null || request.secretKey().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Secret key is required for deletion");
            }
            
            boolean deleted = fileService.deleteFile(fileId, userName, request.secretKey().trim());
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found or not authorized");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/ai/analyze/{fileId}")
    public ResponseEntity<?> analyzeFile(@PathVariable String fileId,
                                         @RequestBody SecretKeyRequest request,
                                         Authentication authentication) {
        try {
            String userName = authentication.getName();
            FileAIService.FileAIResult result = fileAIService.analyzeFile(fileId, request.secretKey(), userName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/ai/search")
    public ResponseEntity<?> searchFiles(@RequestBody SearchRequest request,
                                         Authentication authentication) {
        try {
            String userName = authentication.getName();
            return ResponseEntity.ok(
                    fileAIService.searchFilesByContent(userName, request.query(), request.secretKey())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Simple request body that only needs the secret key for decryption.
     */
    public record SecretKeyRequest(String secretKey) {}

    public record SearchRequest(String query, String secretKey) {}
}

