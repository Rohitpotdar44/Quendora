package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.PasswordResetService;
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

    @Autowired
    private PasswordResetService passwordResetService;
    
    @Value("${admin.creation.key}")
    private String validAdminKey;

    // now to map this method to the endpoint we have to map @GetMapping(/endpoint) so control will get to this healthCheck() method and we get OK
    @GetMapping("/health-check")  // here health check is the endpoint
    public String healthCheck() {
        return "OK";
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String userName = request.get("userName");
            if (userName == null || userName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            passwordResetService.sendResetCode(userName.trim());
            return ResponseEntity.ok(Map.of("message", "Reset code sent to your registered email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send reset code: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        try {
            String userName = request.get("userName");
            String code = request.get("code");
            if (userName == null || userName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Reset code is required");
            }
            passwordResetService.verifyCode(userName.trim(), code.trim());
            return ResponseEntity.ok(Map.of("message", "Code verified"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String userName = request.get("userName");
            String code = request.get("code");
            String newPassword = request.get("newPassword");
            if (userName == null || userName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Reset code is required");
            }
            if (newPassword == null || newPassword.trim().length() < 6) {
                return ResponseEntity.badRequest().body("Password must be at least 6 characters");
            }
            passwordResetService.resetPassword(userName.trim(), code.trim(), newPassword.trim());
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/createUser")
    public ResponseEntity<?> addNewUser(@RequestBody User_12 myUser) {
        try {
            System.out.println("[POST /public/createUser] ===== CREATE USER REQUEST =====");
            System.out.println("[POST /public/createUser] Username: " + myUser.getUserName());
            System.out.println("[POST /public/createUser] Email: " + myUser.getEmail());
            
            // Check if user already exists by email or username
            if (userService_14.findByUserName(myUser.getUserName()) != null) {
                System.out.println("[POST /public/createUser] ❌ User already exists");
                return ResponseEntity.badRequest().body("User already exists");
            }
            if (myUser.getEmail() != null && userService_14.findByEmail(myUser.getEmail()) != null) {
                System.out.println("[POST /public/createUser] ❌ Email already exists");
                return ResponseEntity.badRequest().body("Email already exists");
            }
           
            
            // Generate unique key for the user (do not persist plaintext)
            System.out.println("[POST /public/createUser] Generating uniqueKey...");
            String uniqueKey = userService_14.generateUniqueKey(myUser.getEmail());
            System.out.println("[POST /public/createUser] UniqueKey generated (length: " + uniqueKey.length() + ")");
            
            myUser.setIsFirstLogin(true);
            
            System.out.println("[POST /public/createUser] Calling saveNewUserWithUniqueKey...");
            boolean success = userService_14.saveNewUserWithUniqueKey(myUser, uniqueKey);
            System.out.println("[POST /public/createUser] Save result: " + success);
            
            if (success) {
                // Fetch the saved user to get assigned roles
                System.out.println("[POST /public/createUser] Fetching saved user...");
                User_12 savedUser = userService_14.findByUserName(myUser.getUserName());
                
                if (savedUser == null) {
                    System.out.println("[POST /public/createUser] ❌ ERROR: User was saved but cannot be found!");
                    return ResponseEntity.badRequest().body("User saved but verification failed");
                }
                
                System.out.println("[POST /public/createUser] Found saved user with ID: " + savedUser.getId());
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User created successfully");
                response.put("username", savedUser.getUserName());
                response.put("email", savedUser.getEmail());
                response.put("roles", savedUser.getRoles());
                response.put("uniqueKey", uniqueKey);
                response.put("note", "Save this unique key securely. You'll need it for authentication.");
                
                System.out.println("[POST /public/createUser] ✅ User created successfully!");
                return ResponseEntity.ok(response);
            } else {
                System.out.println("[POST /public/createUser] ❌ Save returned false");
                return ResponseEntity.badRequest().body("Failed to create user");
            }
        } catch (Exception e) {
            System.out.println("[POST /public/createUser] ❌ EXCEPTION: " + e.getClass().getName());
            System.out.println("[POST /public/createUser] Message: " + e.getMessage());
            e.printStackTrace();
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
            
            // Create admin user (first login)
            User_12 adminUser = User_12.builder()
                    .userName(username)
                    .password(password)
                    .email(email)
                    .isFirstLogin(true)
                    .build();

            String adminUniqueKey = userService_14.generateUniqueKey(email != null ? email : username + "@admin.local");
            userService_14.saveAdminWithUniqueKey(adminUser, adminUniqueKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin user created successfully");
            response.put("username", username);
            response.put("note", "This is the only admin user allowed in the system");
            response.put("uniqueKey", adminUniqueKey);
            response.put("showUniqueKey", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Admin creation failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            System.out.println("[POST /public/login] ===== LOGIN REQUEST =====");
            String username = loginRequest.get("userName");
            String password = loginRequest.get("password");
            String providedUniqueKey = loginRequest.get("uniqueKey"); // Optional: user can provide their unique key
            
            if (username == null || password == null) {
                System.out.println("[POST /public/login] Missing credentials");
                return ResponseEntity.badRequest().body("Username and password are required");
            }
            
            System.out.println("[POST /public/login] User: " + username);
            System.out.println("[POST /public/login] UniqueKey provided: " + (providedUniqueKey != null));
            
            // Find user by username
            User_12 user = userService_14.findByUserName(username);
            if (user == null) {
                System.out.println("[POST /public/login] User not found");
                return ResponseEntity.badRequest().body("User not found");
            }
            
            // Verify password using BCrypt
            if (!userService_14.verifyPassword(password, user.getPassword())) {
                System.out.println("[POST /public/login] Invalid password");
                return ResponseEntity.badRequest().body("Invalid password");
            }
            
            System.out.println("[POST /public/login] Password verified");
            
            // Check if user has a uniqueKeyHash in database
            String userUniqueKeyHash = user.getUniqueKeyHash();
            System.out.println("[POST /public/login] User has uniqueKeyHash in DB: " + (userUniqueKeyHash != null && !userUniqueKeyHash.isEmpty()));
            
            // If user doesn't have a uniqueKeyHash, they registered before the uniqueKey feature
            // Generate one for them now
            String validatedUniqueKey = null;
            boolean keyWasGenerated = false;
            
            if (userUniqueKeyHash == null || userUniqueKeyHash.isEmpty()) {
                // User doesn't have uniqueKeyHash - generate new one (legacy accounts only)
                System.out.println("[POST /public/login] ⚠️ User registered before uniqueKey feature - generating new key");
                String newUniqueKey = userService_14.generateUniqueKey(user.getEmail());
                userService_14.updateUniqueKeyHash(user, newUniqueKey);
                validatedUniqueKey = newUniqueKey;
                keyWasGenerated = true;
                System.out.println("[POST /public/login] ✅ Generated new uniqueKey: " + newUniqueKey);
            } else if (providedUniqueKey != null && !providedUniqueKey.isEmpty()) {
                // User has uniqueKeyHash and provided a key - validate it
                if (userService_14.matchesUniqueKey(providedUniqueKey, userUniqueKeyHash)) {
                    System.out.println("[POST /public/login] UniqueKey validated successfully");
                    validatedUniqueKey = providedUniqueKey;
                } else {
                    System.out.println("[POST /public/login] Invalid uniqueKey provided");
                    return ResponseEntity.badRequest().body("Invalid unique key");
                }
            } else {
                // User has uniqueKeyHash but didn't provide key - login succeeds without returning key
                System.out.println("[POST /public/login] ⚠️ User has entries but no uniqueKey provided - login succeeds, entries remain encrypted");
                validatedUniqueKey = null;
            }
            
            // Update first login flag if needed
            boolean wasFirstLogin = user.getIsFirstLogin();
            if (wasFirstLogin) {
                user.setIsFirstLogin(false);
                userService_14.saveUser(user);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", username);
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            
            // Only share the uniqueKey if we had to generate one for a legacy user
            String responseUniqueKey = keyWasGenerated ? validatedUniqueKey : null;
            response.put("uniqueKey", responseUniqueKey);
            
            // Add a flag to indicate if uniqueKey is missing
            // This helps frontend show appropriate message
            if (responseUniqueKey == null) {
                response.put("uniqueKeyMissing", true);
                response.put("message", "Login successful! Use your saved unique key to encrypt or decrypt journal entries.");
            } else {
                response.put("uniqueKeyMissing", false);
                response.put("showUniqueKey", true);
                response.put("message", "Unique key generated for your account. Please save it securely.");
            }
            
            System.out.println("[POST /public/login] ✅ Login successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("[POST /public/login] ❌ Error: " + e.getMessage());
            e.printStackTrace();
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
            
            // Find user by unique key (compare against stored hash)
            User_12 user = userService_14.getAllEntries().stream()
                    .filter(u -> userService_14.matchesUniqueKey(uniqueKey, u.getUniqueKeyHash()))
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

    @PostMapping("/checkUniqueKey")
    public ResponseEntity<?> checkUniqueKey(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("userName");
            String password = request.get("password");
            String testKey = request.get("uniqueKey");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }
            
            // Find user by username
            User_12 user = userService_14.findByUserName(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            // Verify password
            if (!userService_14.verifyPassword(password, user.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid password");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("hasUniqueKeyHash", user.getUniqueKeyHash() != null && !user.getUniqueKeyHash().isEmpty());
            response.put("entryCount", user.getAllEntries() != null ? user.getAllEntries().size() : 0);
            
            if (testKey != null && !testKey.isEmpty()) {
                boolean matches = userService_14.matchesUniqueKey(testKey, user.getUniqueKeyHash());
                response.put("providedKeyMatches", matches);
                response.put("providedKeyLength", testKey.length());
            }
            
            // Show solution based on situation
            boolean hasHash = user.getUniqueKeyHash() != null && !user.getUniqueKeyHash().isEmpty();
            int entryCount = user.getAllEntries() != null ? user.getAllEntries().size() : 0;
            
            if (!hasHash) {
                response.put("solution", "User has no uniqueKey. Login without key to generate one.");
            } else if (entryCount == 0) {
                response.put("solution", "User has no entries. You can safely regenerate the key using /regenerateUniqueKey");
            } else {
                response.put("solution", "User has " + entryCount + " encrypted entries. You MUST use the original uniqueKey from registration. Check browser localStorage or the modal that was shown during signup.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Check failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/checkUserExists")
    public ResponseEntity<?> checkUserExists(@RequestParam String userName) {
        try {
            System.out.println("[GET /public/checkUserExists] Checking user: " + userName);
            User_12 user = userService_14.findByUserName(userName);
            
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                response.put("message", "User not found");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("username", user.getUserName());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("hasUniqueKeyHash", user.getUniqueKeyHash() != null && !user.getUniqueKeyHash().isEmpty());
            response.put("entryCount", user.getAllEntries() != null ? user.getAllEntries().size() : 0);
            response.put("isFirstLogin", user.getIsFirstLogin());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Check failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/resetUniqueKeyForTesting")
    public ResponseEntity<?> resetUniqueKeyForTesting(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("userName");
            String password = request.get("password");
            String confirmDelete = request.get("confirmDelete");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("Username and password are required");
            }
            
            if (!"YES_DELETE_ALL_ENTRIES".equals(confirmDelete)) {
                return ResponseEntity.badRequest().body("You must confirm deletion by setting confirmDelete: 'YES_DELETE_ALL_ENTRIES'");
            }
            
            // Find user by username
            User_12 user = userService_14.findByUserName(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            // Verify password
            if (!userService_14.verifyPassword(password, user.getPassword())) {
                return ResponseEntity.badRequest().body("Invalid password");
            }
            
            // Delete all entries
            int entryCount = user.getAllEntries() != null ? user.getAllEntries().size() : 0;
            user.getAllEntries().clear();
            userService_14.saveUser(user);
            
            // Generate new unique key
            String newUniqueKey = userService_14.generateUniqueKey(user.getEmail());
            userService_14.updateUniqueKeyHash(user, newUniqueKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All entries deleted and new uniqueKey generated");
            response.put("deletedEntries", entryCount);
            response.put("username", username);
            response.put("email", user.getEmail());
            response.put("uniqueKey", newUniqueKey);
            response.put("warning", "SAVE THIS KEY! You won't be able to see it again.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Reset failed: " + e.getMessage());
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
            
            // Check if user has any journal entries
            int entryCount = user.getAllEntries() != null ? user.getAllEntries().size() : 0;
            System.out.println("[POST /public/regenerateUniqueKey] User has " + entryCount + " entries");
            
            if (entryCount > 0) {
                // User has entries - regenerating will break decryption!
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot regenerate unique key");
                errorResponse.put("message", "You have " + entryCount + " encrypted journal entries. Regenerating your unique key will make them unreadable.");
                errorResponse.put("solution", "Please use your original unique key from when you first registered.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Generate new unique key (store only hash)
            String uniqueKey = userService_14.generateUniqueKey(user.getEmail());
            userService_14.updateUniqueKeyHash(user, uniqueKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unique key regenerated successfully");
            response.put("username", username);
            response.put("email", user.getEmail());
            response.put("uniqueKey", uniqueKey);
            response.put("note", "Your new unique key has been generated and saved. Save it securely!");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Regeneration failed: " + e.getMessage());
        }
    }
}

