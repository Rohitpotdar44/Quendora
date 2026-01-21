package com.RohitPotdar.myJournalApp.entity_5;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents an encrypted file stored in MongoDB.
 * The encrypted bytes, IV, and salt are stored so we can decrypt later
 * once the user provides the same secret key they used during upload.
 */
@Document(collection = "secure_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureFile {

    @Id
    private String id;

    private String userId;
    private String userName;
    private String contentType;
    private byte[] encryptedData;
    private byte[] iv;
    private byte[] salt;

    private byte[] encryptedTitle;
    private byte[] titleIv;
    private byte[] titleSalt;

    private byte[] encryptedOriginalFileName;
    private byte[] originalFileNameIv;
    private byte[] originalFileNameSalt;

    private byte[] encryptedFileSize;
    private byte[] fileSizeIv;
    private byte[] fileSizeSalt;

    // User preference: Is this file sensitive?
    private boolean isSensitive;
    
    // If not sensitive, store plaintext title for easy identification
    // If sensitive, this will be null and user must decrypt to see title
    private String plaintextTitle;
    
    // Plaintext file size for display (not sensitive info)
    private String displayFileSize;

    // AI-derived metadata (optional)
    private String aiSummary;
    private java.util.List<String> aiTags;
    private String aiCaption;
    private String aiOcrText;
    private String aiSearchText;
    private String aiHighlights;
    private String aiSuggestedName;
    private String aiTranscript;
    private java.util.List<String> aiWarnings;
    private LocalDateTime aiUpdatedAt;

    // Encrypted AI metadata (stored at rest)
    private byte[] aiSummaryEncrypted;
    private byte[] aiSummaryIv;
    private byte[] aiSummarySalt;

    private byte[] aiTagsEncrypted;
    private byte[] aiTagsIv;
    private byte[] aiTagsSalt;

    private byte[] aiCaptionEncrypted;
    private byte[] aiCaptionIv;
    private byte[] aiCaptionSalt;

    private byte[] aiOcrTextEncrypted;
    private byte[] aiOcrTextIv;
    private byte[] aiOcrTextSalt;

    private byte[] aiSearchTextEncrypted;
    private byte[] aiSearchTextIv;
    private byte[] aiSearchTextSalt;

    private byte[] aiHighlightsEncrypted;
    private byte[] aiHighlightsIv;
    private byte[] aiHighlightsSalt;

    private byte[] aiSuggestedNameEncrypted;
    private byte[] aiSuggestedNameIv;
    private byte[] aiSuggestedNameSalt;

    private byte[] aiTranscriptEncrypted;
    private byte[] aiTranscriptIv;
    private byte[] aiTranscriptSalt;

    private byte[] aiWarningsEncrypted;
    private byte[] aiWarningsIv;
    private byte[] aiWarningsSalt;

    private LocalDateTime createdAt;
}

