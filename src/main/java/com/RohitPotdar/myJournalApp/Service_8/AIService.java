package com.RohitPotdar.myJournalApp.Service_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIService {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${openai.model:deepseek-chat}")
    private String openaiModel;

    @Value("${openai.vision.model:openai/gpt-4o-mini}")
    private String openaiVisionModel;

    @Value("${openai.referrer:}")
    private String openaiReferrer;

    @Value("${openai.title:}")
    private String openaiTitle;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Rewrite title and content with better grammar and clarity
     * Returns a map with "title" and "content" keys
     */
    public Map<String, String> rewriteWithAI(String title, String content) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            System.out.println("[AIService] OpenAI API key not configured");
            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            result.put("content", content);
            return result;
        }

        try {
            String prompt = "Rewrite the following journal entry title and content to be more grammatically correct, meaningful, and well-written. " +
                    "Keep the same meaning and tone, but improve clarity, grammar, and flow. " +
                    "Return a JSON object with exactly two keys: \"title\" (the improved title) and \"content\" (the improved content). " +
                    "Return ONLY valid JSON, no other text.\n\n" +
                    "Original Title: " + title + "\n\n" +
                    "Original Content: " + content.substring(0, Math.min(1500, content.length()));

            String response = callOpenAI(prompt, "You are a professional writing assistant that improves journal entries while preserving the author's voice and meaning. Always return valid JSON only, no other text.", 800, 0.5f);
            String jsonPayload = extractJsonPayload(response);

            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);
            Map<String, String> result = new HashMap<>();
            
            String rewrittenTitle = jsonNode.has("title") ? jsonNode.get("title").asText() : title;
            String rewrittenContent = jsonNode.has("content") ? jsonNode.get("content").asText() : content;
            
            result.put("title", rewrittenTitle.trim());
            result.put("content", rewrittenContent.trim());
            
            return result;

        } catch (Exception e) {
            System.err.println("[AIService] Error rewriting with AI: " + e.getMessage());
            e.printStackTrace();
            // Return original if rewrite fails
            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            result.put("content", content);
            return result;
        }
    }

    public Map<String, Object> generateFileInsights(String title,
                                                    String fileName,
                                                    String contentType,
                                                    String extractedText) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", "");
        fallback.put("tags", List.of());
        fallback.put("caption", "");
        fallback.put("highlights", "");
        fallback.put("suggestedFileName", "");

        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            System.out.println("[AIService] OpenAI API key not configured");
            return fallback;
        }

        try {
            boolean isVideo = contentType != null && contentType.toLowerCase().startsWith("video/");
            String contentDescription = isVideo && extractedText.length() > 100 
                    ? "Video transcript (speech-to-text)" 
                    : "Extracted text";
            
            String prompt = "You are analyzing a user file and must return JSON only.\n" +
                    "File title: " + title + "\n" +
                    "Original file name: " + fileName + "\n" +
                    "Content type: " + contentType + "\n\n" +
                    contentDescription + " (may be empty):\n" + extractedText + "\n\n" +
                    "Return a JSON object with exactly these keys:\n" +
                    "- \"summary\": short 1-2 sentence summary of the content" + 
                        (isVideo ? " based on the video transcript" : "") + 
                        " (empty string if no text)\n" +
                    "- \"tags\": array of 3-6 short tags\n" +
                    "- \"caption\": short caption for images (empty string if not image or no OCR text)\n" +
                    "- \"highlights\": " + (isVideo 
                        ? "short bullet-point summary of key moments and topics from the video transcript (format as bullet points with •)" 
                        : "short bullet summary for videos (empty string if no transcript)") + "\n" +
                    "- \"suggestedFileName\": short filename suggestion without extension\n" +
                    "Return ONLY valid JSON, no markdown or extra text.";

            String response = callOpenAI(prompt,
                    "Return only valid JSON. Do not include markdown code fences.",
                    500,
                    0.3f);
            String jsonPayload = extractJsonPayload(response);
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);

            Map<String, Object> result = new HashMap<>();
            result.put("summary", jsonNode.has("summary") ? jsonNode.get("summary").asText() : "");

            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                jsonNode.get("tags").forEach(tag -> tags.add(tag.asText()));
            }
            result.put("tags", tags);

            result.put("caption", jsonNode.has("caption") ? jsonNode.get("caption").asText() : "");
            result.put("highlights", jsonNode.has("highlights") ? jsonNode.get("highlights").asText() : "");
            result.put("suggestedFileName", jsonNode.has("suggestedFileName") ? jsonNode.get("suggestedFileName").asText() : "");

            return result;
        } catch (Exception e) {
            System.err.println("[AIService] Error generating file insights: " + e.getMessage());
            e.printStackTrace();
            return fallback;
        }
    }

    public Map<String, Object> generateVisionInsights(String fileName,
                                                      String contentType,
                                                      byte[] imageBytes,
                                                      boolean isVideoFrame) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("caption", "");
        fallback.put("highlights", "");
        fallback.put("tags", List.of());

        if (imageBytes == null || imageBytes.length == 0) {
            return fallback;
        }
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            return fallback;
        }

        try {
            String prompt = (isVideoFrame
                    ? "You are given a single frame from a user video. Describe the scene and provide short bullet highlights about what is visible. "
                    : "You are given an image. Describe what is visible in one short caption and provide 3-6 short tags. ") +
                    "Return a JSON object with exactly these keys:\n" +
                    "- \"caption\": short caption\n" +
                    "- \"highlights\": short bullet points (empty string if not video)\n" +
                    "- \"tags\": array of 3-6 short tags\n" +
                    "Return ONLY valid JSON, no markdown.";

            String response = callOpenAIWithImage(prompt, imageBytes);
            String jsonPayload = extractJsonPayload(response);
            JsonNode jsonNode = objectMapper.readTree(jsonPayload);

            Map<String, Object> result = new HashMap<>();
            result.put("caption", jsonNode.has("caption") ? jsonNode.get("caption").asText() : "");
            result.put("highlights", jsonNode.has("highlights") ? jsonNode.get("highlights").asText() : "");

            List<String> tags = new ArrayList<>();
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                jsonNode.get("tags").forEach(tag -> tags.add(tag.asText()));
            }
            result.put("tags", tags);

            return result;
        } catch (Exception e) {
            System.err.println("[AIService] Error generating vision insights: " + e.getMessage());
            e.printStackTrace();
            return fallback;
        }
    }

    private String extractJsonPayload(String response) {
        if (response == null) {
            return "";
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAI(String userPrompt, String systemPrompt, int maxTokens, float temperature) throws Exception {
        String apiKey = openaiApiKey == null ? "" : openaiApiKey.trim();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        System.out.println("[AIService] Using AI endpoint: " + openaiApiUrl + " | model=" + openaiModel + " | keyLength=" + apiKey.length());
        if (openaiReferrer != null && !openaiReferrer.isBlank()) {
            headers.set("HTTP-Referer", openaiReferrer);
        }
        if (openaiTitle != null && !openaiTitle.isBlank()) {
            headers.set("X-Title", openaiTitle);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiModel);
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                openaiApiUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        }

        throw new Exception("Failed to get valid response from OpenAI API");
    }

    private String callOpenAIWithImage(String prompt, byte[] imageBytes) throws Exception {
        String apiKey = openaiApiKey == null ? "" : openaiApiKey.trim();
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        if (openaiReferrer != null && !openaiReferrer.isBlank()) {
            headers.set("HTTP-Referer", openaiReferrer);
        }
        if (openaiTitle != null && !openaiTitle.isBlank()) {
            headers.set("X-Title", openaiTitle);
        }

        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64);

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of("type", "image_url", "image_url", imageUrl));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiVisionModel);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", content)
        ));
        requestBody.put("max_tokens", 400);
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                openaiApiUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        }

        throw new Exception("Failed to get valid response from AI vision model");
    }
}