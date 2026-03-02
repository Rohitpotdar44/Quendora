package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.SecureFileRepository;
import com.RohitPotdar.myJournalApp.entity_5.SecureFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileAIService {

    private final FileService fileService;
    private final SecureFileRepository secureFileRepository;
    private final AIService aiService;
    private final FileTextExtractor fileTextExtractor;
    private final VideoFrameExtractor videoFrameExtractor;
    private final VideoTranscriptService videoTranscriptService;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileAIService(FileService fileService,
                         SecureFileRepository secureFileRepository,
                         AIService aiService,
                         FileTextExtractor fileTextExtractor,
                         VideoFrameExtractor videoFrameExtractor,
                         VideoTranscriptService videoTranscriptService,
                         CryptoService cryptoService) {
        this.fileService = fileService;
        this.secureFileRepository = secureFileRepository;
        this.aiService = aiService;
        this.fileTextExtractor = fileTextExtractor;
        this.videoFrameExtractor = videoFrameExtractor;
        this.videoTranscriptService = videoTranscriptService;
        this.cryptoService = cryptoService;
    }

    public FileAIResult analyzeFile(String fileId, String secretKey, String userName) {
        FileService.DecryptedFile decrypted = fileService.decryptAndLoad(fileId, secretKey, userName);
        SecureFile secureFile = decrypted.metadata();

        String contentType = secureFile.getContentType() != null ? secureFile.getContentType() : "application/octet-stream";
        String fileName = decrypted.originalFileName() != null ? decrypted.originalFileName() : "secure-file";
        String title = decrypted.title() != null ? decrypted.title() : fileName;

        List<String> warnings = new ArrayList<>();

        FileTextExtractor.ExtractionResult extraction = fileTextExtractor.extractText(decrypted.fileBytes(), contentType);
        warnings.addAll(extraction.warnings());

        String extractedText = extraction.text();
        String limitedText = limitText(extractedText, 6000);

        boolean isImage = contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        boolean isVideo = contentType.toLowerCase(Locale.ROOT).startsWith("video/");

        String visionCaption = "";
        String visionHighlights = "";
        List<String> visionTags = List.of();
        String videoTranscript = "";

        if (isImage && !StringUtils.hasText(limitedText)) {
            Map<String, Object> vision = aiService.generateVisionInsights(fileName, contentType, decrypted.fileBytes(), false);
            visionCaption = (String) vision.getOrDefault("caption", "");
            visionHighlights = (String) vision.getOrDefault("highlights", "");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) vision.getOrDefault("tags", List.of());
            visionTags = tags;
            if (!StringUtils.hasText(visionCaption)) {
                warnings.add("Image caption could not be generated.");
            }
        }

        if (isVideo) {
            // First, try to transcribe the video audio
            VideoTranscriptService.TranscriptionResult transcriptResult = 
                videoTranscriptService.transcribeVideo(decrypted.fileBytes(), fileName);
            warnings.addAll(transcriptResult.warnings());
            videoTranscript = transcriptResult.transcript();
            
            // Extract frame for visual analysis
            VideoFrameExtractor.FrameResult frameResult = videoFrameExtractor.extractFirstFrame(decrypted.fileBytes());
            warnings.addAll(frameResult.warnings());
            if (frameResult.jpegBytes() != null) {
                Map<String, Object> vision = aiService.generateVisionInsights(fileName, contentType, frameResult.jpegBytes(), true);
                visionCaption = (String) vision.getOrDefault("caption", "");
                String frameHighlights = (String) vision.getOrDefault("highlights", "");
                visionHighlights = frameHighlights; // Will be replaced by transcript-based highlights if available
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) vision.getOrDefault("tags", List.of());
                visionTags = tags;
            } else {
                warnings.add("Could not extract a video frame for visual analysis.");
            }
        }

        // For videos with transcript, use transcript for AI analysis
        // Otherwise use extracted text or vision caption
        String textForAi;
        if (isVideo && StringUtils.hasText(videoTranscript)) {
            // Use transcript for video analysis
            textForAi = limitText(videoTranscript, 8000); // Transcripts can be longer
        } else {
            textForAi = StringUtils.hasText(limitedText) ? limitedText : visionCaption;
        }
        
        Map<String, Object> aiInsights = aiService.generateFileInsights(title, fileName, contentType, textForAi);

        String summary = (String) aiInsights.getOrDefault("summary", "");
        String caption = (String) aiInsights.getOrDefault("caption", "");
        String highlights = (String) aiInsights.getOrDefault("highlights", "");
        String suggestedName = (String) aiInsights.getOrDefault("suggestedFileName", "");

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) aiInsights.getOrDefault("tags", List.of());
        if (tags.isEmpty() && !visionTags.isEmpty()) {
            tags = visionTags;
        }

        String ocrText = isImage ? extractedText : null;
        
        // For videos, include transcript in search text; otherwise use extracted text
        String searchText;
        if (isVideo && StringUtils.hasText(videoTranscript)) {
            searchText = videoTranscript; // Searchable transcript
        } else {
            searchText = extractedText;
        }

        if (isImage && !StringUtils.hasText(ocrText) && !StringUtils.hasText(visionCaption)) {
            warnings.add("Image OCR returned no text. Caption accuracy may be limited.");
        }
        if (isVideo && !StringUtils.hasText(videoTranscript)) {
            warnings.add("No transcript available; highlights are based on a single frame.");
        }

        String finalCaption = StringUtils.hasText(caption) ? caption : visionCaption;
        String finalHighlights = StringUtils.hasText(highlights) ? highlights : visionHighlights;

        CryptoService.EncryptedPayload summaryPayload = encryptText(summary, secretKey);
        CryptoService.EncryptedPayload captionPayload = encryptText(finalCaption, secretKey);
        CryptoService.EncryptedPayload highlightsPayload = encryptText(finalHighlights, secretKey);
        CryptoService.EncryptedPayload ocrPayload = encryptText(ocrText, secretKey);
        CryptoService.EncryptedPayload searchPayload = encryptText(searchText, secretKey);
        CryptoService.EncryptedPayload suggestedPayload = encryptText(suggestedName, secretKey);
        CryptoService.EncryptedPayload transcriptPayload = encryptText(videoTranscript, secretKey);
        CryptoService.EncryptedPayload tagsPayload = encryptJson(tags, secretKey);
        CryptoService.EncryptedPayload warningsPayload = encryptJson(warnings, secretKey);

        secureFile.setAiSummaryEncrypted(payloadOrNull(summaryPayload));
        secureFile.setAiSummaryIv(ivOrNull(summaryPayload));
        secureFile.setAiSummarySalt(saltOrNull(summaryPayload));

        secureFile.setAiCaptionEncrypted(payloadOrNull(captionPayload));
        secureFile.setAiCaptionIv(ivOrNull(captionPayload));
        secureFile.setAiCaptionSalt(saltOrNull(captionPayload));

        secureFile.setAiHighlightsEncrypted(payloadOrNull(highlightsPayload));
        secureFile.setAiHighlightsIv(ivOrNull(highlightsPayload));
        secureFile.setAiHighlightsSalt(saltOrNull(highlightsPayload));

        secureFile.setAiOcrTextEncrypted(payloadOrNull(ocrPayload));
        secureFile.setAiOcrTextIv(ivOrNull(ocrPayload));
        secureFile.setAiOcrTextSalt(saltOrNull(ocrPayload));

        secureFile.setAiSearchTextEncrypted(payloadOrNull(searchPayload));
        secureFile.setAiSearchTextIv(ivOrNull(searchPayload));
        secureFile.setAiSearchTextSalt(saltOrNull(searchPayload));

        secureFile.setAiSuggestedNameEncrypted(payloadOrNull(suggestedPayload));
        secureFile.setAiSuggestedNameIv(ivOrNull(suggestedPayload));
        secureFile.setAiSuggestedNameSalt(saltOrNull(suggestedPayload));

        secureFile.setAiTranscriptEncrypted(payloadOrNull(transcriptPayload));
        secureFile.setAiTranscriptIv(ivOrNull(transcriptPayload));
        secureFile.setAiTranscriptSalt(saltOrNull(transcriptPayload));

        secureFile.setAiTagsEncrypted(payloadOrNull(tagsPayload));
        secureFile.setAiTagsIv(ivOrNull(tagsPayload));
        secureFile.setAiTagsSalt(saltOrNull(tagsPayload));

        secureFile.setAiWarningsEncrypted(payloadOrNull(warningsPayload));
        secureFile.setAiWarningsIv(ivOrNull(warningsPayload));
        secureFile.setAiWarningsSalt(saltOrNull(warningsPayload));

        // Clear plaintext AI fields before storing
        secureFile.setAiSummary(null);
        secureFile.setAiTags(null);
        secureFile.setAiCaption(null);
        secureFile.setAiHighlights(null);
        secureFile.setAiOcrText(null);
        secureFile.setAiSearchText(null);
        secureFile.setAiTranscript(null);
        secureFile.setAiWarnings(null);
        secureFile.setAiUpdatedAt(LocalDateTime.now());
        secureFile.setAiSuggestedName(null);

        secureFileRepository.save(secureFile);

        return new FileAIResult(
                fileId,
                summary,
                tags,
                StringUtils.hasText(caption) ? caption : visionCaption,
                StringUtils.hasText(highlights) ? highlights : visionHighlights,
                suggestedName,
                ocrText,
                searchText,
                videoTranscript,
                warnings
        );
    }

    public List<SecureFile> searchFilesByContent(String userName, String query, String secretKey) {
        List<SecureFile> files = secureFileRepository.findByUserName(userName);
        if (!StringUtils.hasText(query)) {
            return files;
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalArgumentException("Secret key is required for search.");
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<SecureFile> matches = new ArrayList<>();
        for (SecureFile file : files) {
            String haystack = getDecryptedSearchText(file, secretKey);
            if (haystack.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(file);
            }
        }
        return matches;
    }

    private String limitText(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private CryptoService.EncryptedPayload encryptText(String value, String secretKey) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return cryptoService.encrypt(value.getBytes(java.nio.charset.StandardCharsets.UTF_8), secretKey);
    }

    private CryptoService.EncryptedPayload encryptJson(Object value, String secretKey) {
        if (value == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            if (!StringUtils.hasText(json) || "[]".equals(json)) {
                return null;
            }
            return cryptoService.encrypt(json.getBytes(java.nio.charset.StandardCharsets.UTF_8), secretKey);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize AI metadata for encryption.", e);
        }
    }

    private String decryptText(byte[] encrypted, byte[] iv, byte[] salt, String secretKey) {
        if (encrypted == null || iv == null || salt == null) {
            return "";
        }
        CryptoService.EncryptedPayload payload = new CryptoService.EncryptedPayload(encrypted, iv, salt);
        byte[] decrypted = cryptoService.decrypt(payload, secretKey);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String getDecryptedSearchText(SecureFile file, String secretKey) {
        if (file.getAiSearchTextEncrypted() != null) {
            return decryptText(file.getAiSearchTextEncrypted(),
                    file.getAiSearchTextIv(),
                    file.getAiSearchTextSalt(),
                    secretKey);
        }
        return file.getAiSearchText() != null ? file.getAiSearchText() : "";
    }

    private byte[] payloadOrNull(CryptoService.EncryptedPayload payload) {
        return payload != null ? payload.cipherBytes() : null;
    }

    private byte[] ivOrNull(CryptoService.EncryptedPayload payload) {
        return payload != null ? payload.iv() : null;
    }

    private byte[] saltOrNull(CryptoService.EncryptedPayload payload) {
        return payload != null ? payload.salt() : null;
    }

    public record FileAIResult(
            String fileId,
            String summary,
            List<String> tags,
            String caption,
            String highlights,
            String suggestedFileName,
            String ocrText,
            String searchText,
            String transcript,
            List<String> warnings
    ) {}
}