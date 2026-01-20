package com.RohitPotdar.myJournalApp.Service_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for transcribing video audio to text using OpenAI Whisper API via OpenRouter.
 */
@Component
public class VideoTranscriptService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.referrer:}")
    private String openaiReferrer;

    @Value("${openai.title:}")
    private String openaiTitle;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VideoTranscriptService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Transcribe video audio to text using Whisper API.
     * @param videoBytes The video file bytes
     * @param fileName Original filename for context
     * @return TranscriptionResult with transcript text and any warnings
     */
    public TranscriptionResult transcribeVideo(byte[] videoBytes, String fileName) {
        List<String> warnings = new ArrayList<>();
        
        if (videoBytes == null || videoBytes.length == 0) {
            warnings.add("Video is empty; cannot transcribe.");
            return new TranscriptionResult("", warnings);
        }

        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            warnings.add("AI API key not configured; transcription unavailable.");
            return new TranscriptionResult("", warnings);
        }

        // OpenRouter Whisper endpoint
        String whisperUrl = "https://openrouter.ai/api/v1/audio/transcriptions";
        
        File tempFile = null;
        try {
            // Create temporary file for the video
            tempFile = Files.createTempFile("video-transcribe", ".mp4").toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(videoBytes);
            }

            // Prepare multipart/form-data request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey.trim());
            
            if (openaiReferrer != null && !openaiReferrer.isBlank()) {
                headers.set("HTTP-Referer", openaiReferrer);
            }
            if (openaiTitle != null && !openaiTitle.isBlank()) {
                headers.set("X-Title", openaiTitle);
            }

            // Create multipart request body
            org.springframework.core.io.FileSystemResource fileResource = 
                new org.springframework.core.io.FileSystemResource(tempFile);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("model", "openai/whisper-1");
            body.add("language", "auto"); // Auto-detect language

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            // Call Whisper API
            ResponseEntity<String> response = restTemplate.exchange(
                whisperUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String transcript = "";
                
                // Whisper API returns either:
                // - {"text": "transcript..."} format
                // - Direct text in some cases
                if (jsonResponse.has("text")) {
                    transcript = jsonResponse.get("text").asText();
                } else if (jsonResponse.isTextual()) {
                    transcript = jsonResponse.asText();
                }

                if (transcript != null && !transcript.trim().isEmpty()) {
                    return new TranscriptionResult(transcript.trim(), warnings);
                } else {
                    warnings.add("Transcription returned empty result.");
                    return new TranscriptionResult("", warnings);
                }
            } else {
                warnings.add("Transcription API returned status: " + response.getStatusCode());
                return new TranscriptionResult("", warnings);
            }

        } catch (Exception e) {
            warnings.add("Video transcription failed: " + e.getMessage());
            e.printStackTrace();
            return new TranscriptionResult("", warnings);
        } finally {
            // Cleanup temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    tempFile.delete();
                } catch (Exception e) {
                    // Best-effort cleanup
                    System.err.println("Failed to delete temp file: " + e.getMessage());
                }
            }
        }
    }

    public record TranscriptionResult(String transcript, List<String> warnings) {}
}
