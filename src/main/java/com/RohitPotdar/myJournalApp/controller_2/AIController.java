package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * Rewrite title and content with AI for better grammar and clarity
     */
    @PostMapping("/rewrite")
    public ResponseEntity<?> rewriteWithAI(@RequestBody Map<String, String> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            String title = request.get("title");
            String content = request.get("content");
            
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Content is required");
            }

            Map<String, String> rewritten = aiService.rewriteWithAI(title, content);
            return ResponseEntity.ok(rewritten);

        } catch (Exception e) {
            System.err.println("[AIController] Error in rewrite: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to rewrite with AI: " + e.getMessage());
        }
    }
}
