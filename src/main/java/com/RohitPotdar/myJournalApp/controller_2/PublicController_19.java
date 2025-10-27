package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// always write this HealthCheck class in any spring boot project (it is a good practice)
@RestController
@RequestMapping("/public")
public class PublicController_19 {


    @Autowired
    private userService_14 userService_14;
    
    @Value("${admin.creation.key}")
    private String validAdminKey;

    // now to map this method to the endpoint we have to map @GetMapping(/endpoint) so control will get to this healthCheck() method and we get OK
    @GetMapping("/health-check")  // here health check is the endpoint
    public String healthCheck() {
        return "OK";
    }


    @PostMapping("/createUser")
    public ResponseEntity<?> addNewUser(@RequestBody User_12 myUser) {
        try {
            // Check if user already exists by email or username
            if (myUser.getEmail() != null && userService_14.findByEmail(myUser.getEmail()) != null) {
                return ResponseEntity.badRequest().body("Email already exists");
            }
            if (userService_14.findByUserName(myUser.getUserName()) != null) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            
            // Generate unique key for the user
            String uniqueKey = userService_14.generateUniqueKey(myUser.getEmail());
            myUser.setUniqueKey(uniqueKey);
            myUser.setIsFirstLogin(true);
            
            boolean success = userService_14.saveNewUser(myUser);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User created successfully");
                response.put("username", myUser.getUserName());
                response.put("email", myUser.getEmail());
                response.put("uniqueKey", uniqueKey);
                response.put("note", "Save this unique key securely. You'll need it for authentication.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("Failed to create user");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("User creation failed: " + e.getMessage());
        }
    }

    @PostMapping("/createAdmin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> adminRequest) {
        try {
            String username = adminRequest.get("userName");
            String password = adminRequest.get("password");
            String adminKey = adminRequest.get("adminKey"); // Secret key for admin creation
            String email = adminRequest.get("email");
            
            if (username == null || password == null || adminKey == null) {
                return ResponseEntity.badRequest().body("Username, password, and adminKey are required");
            }
            
            // Validate admin key from application.properties
            if (!adminKey.equals(validAdminKey)) {
                return ResponseEntity.badRequest().body("Invalid admin key");
            }
            
            // Check if any admin already exists (only one admin allowed)
            if (userService_14.hasAdminUser()) {
                return ResponseEntity.badRequest().body("Admin user already exists. Only one admin is allowed.");
            }
            
            // Check if user already exists
            User_12 existingUser = userService_14.findByUserName(username);
            if (existingUser != null) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            
            // Create admin user with uniqueKey (first login)
            User_12 adminUser = User_12.builder()
                    .userName(username)
                    .password(password)
                    .email(email)
                    .uniqueKey(userService_14.generateUniqueKey(email != null ? email : username + "@admin.local"))
                    .isFirstLogin(true)
                    .build();

            userService_14.saveAdmin(adminUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin user created successfully");
            response.put("username", username);
            response.put("note", "This is the only admin user allowed in the system");
            response.put("uniqueKey", adminUser.getUniqueKey());
            response.put("showUniqueKey", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Admin creation failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("userName");
            String password = loginRequest.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }
            
            // Find user by username
            User_12 user = userService_14.findByUserName(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            // Verify password using BCrypt
            if (!userService_14.verifyPassword(password, user.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid password");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", username);
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("uniqueKey", user.getUniqueKey());
            
            // If it's the first login, show the unique key
            if (user.getIsFirstLogin()) {
                response.put("showUniqueKey", true);
                response.put("uniqueKeyMessage", "IMPORTANT: This is your unique authentication key. Save it securely as it will not be shown again!");
                
                // Mark as not first login anymore
                user.setIsFirstLogin(false);
                userService_14.saveUser(user);
            } else {
                response.put("showUniqueKey", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/validateUniqueKey")
    public ResponseEntity<?> validateUniqueKey(@RequestBody Map<String, String> request) {
        try {
            String uniqueKey = request.get("uniqueKey");
            
            if (uniqueKey == null) {
                return ResponseEntity.badRequest().body("Unique key is required");
            }
            
            // Find user by unique key
            User_12 user = userService_14.getAllEntries().stream()
                    .filter(u -> uniqueKey.equals(u.getUniqueKey()))
                    .findFirst()
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("Invalid unique key");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unique key is valid");
            response.put("username", user.getUserName());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Validation failed: " + e.getMessage());
        }
    }

    @PostMapping("/regenerateUniqueKey")
    public ResponseEntity<?> regenerateUniqueKey(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("userName");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }
            
            // Find user by username
            User_12 user = userService_14.findByUserName(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            // Verify password using BCrypt
            if (!userService_14.verifyPassword(password, user.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid password");
            }
            
            // Generate new unique key
            String uniqueKey = userService_14.generateUniqueKey(user.getEmail());
            user.setUniqueKey(uniqueKey);
            userService_14.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unique key regenerated successfully");
            response.put("username", username);
            response.put("email", user.getEmail());
            response.put("uniqueKey", uniqueKey);
            response.put("note", "Your new unique key has been generated and saved.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Regeneration failed: " + e.getMessage());
        }
    }
}

