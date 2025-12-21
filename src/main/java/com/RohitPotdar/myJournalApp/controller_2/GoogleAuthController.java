package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.entity_5.User_12;
import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoogleAuthController {
    @Value("${google.client.id}")
    private String googleClientId;

    @Autowired
    private userService_14 userService14;

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing token");
        }
        try {
            // Verify the token
            GoogleIdToken idToken = getVerifier().verify(token);
            if (idToken == null) {
                return ResponseEntity.badRequest().body("Invalid ID token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String sub = payload.getSubject();

            // Check if user exists, else create
            User_12 user = userService14.findByEmail(email);
            if (user == null) {
                user = User_12.builder()
                        .userName(email.split("@")[0] + "_google")
                        .password(sub) // Not used for Google-only login
                        .email(email)
                        .roles(Collections.singletonList("USER"))
                        .isFirstLogin(false)
                        .build();
                userService14.saveNewUser(user);
            }

            // Set up the response (modify as needed)
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Google login successful");
            resp.put("username", user.getUserName());
            resp.put("email", user.getEmail());
            resp.put("roles", user.getRoles());
            // Optionally: add JWT auth/session here
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Google login failed: " + e.getMessage());
        }
    }

    private GoogleIdTokenVerifier getVerifier() {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }
}

