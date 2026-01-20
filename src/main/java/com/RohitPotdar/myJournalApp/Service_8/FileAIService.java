package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.SecureFileRepository;
import com.RohitPotdar.myJournalApp.entity_5.SecureFile;
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

    public FileAIService(FileService fileService,
                         SecureFileRepository secureFileRepository,
                         AIService aiService,
                         FileTextExtractor fileTextExtractor,
                         VideoFrameExtractor videoFrameExtractor,
                         VideoTranscriptService videoTranscriptService) {
        this.fileService = fileService;
        this.secureFileRepository = secureFileRepository;
        this.aiService = aiService;
        this.fileTextExtractor = fileTextExtractor;
        this.videoFrameExtractor = videoFrameExtractor;
        this.videoTranscriptService = videoTranscriptService;
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

        secureFile.setAiSummary(summary);
        secureFile.setAiTags(tags);
        secureFile.setAiCaption(StringUtils.hasText(caption) ? caption : visionCaption);
        secureFile.setAiHighlights(StringUtils.hasText(highlights) ? highlights : visionHighlights);
        secureFile.setAiOcrText(ocrText);
        secureFile.setAiSearchText(searchText);
        secureFile.setAiTranscript(videoTranscript); // Store full transcript
        secureFile.setAiWarnings(warnings);
        secureFile.setAiUpdatedAt(LocalDateTime.now());
        secureFile.setAiSuggestedName(suggestedName);

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

    public List<SecureFile> searchFilesByContent(String userName, String query) {
        List<SecureFile> files = secureFileRepository.findByUserName(userName);
        if (!StringUtils.hasText(query)) {
            return files;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<SecureFile> matches = new ArrayList<>();
        for (SecureFile file : files) {
            String haystack = file.getAiSearchText() != null ? file.getAiSearchText() : "";
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
