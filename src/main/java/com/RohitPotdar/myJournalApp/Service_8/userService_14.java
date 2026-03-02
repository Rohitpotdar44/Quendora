package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Component
public class userService_14 {

// don't forget to perform the dependency injection
    @Autowired
    private userRepository_13 userRepository_13;


    // (Post Method)
    // now if we have to save something then write method

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean saveNewUser(User_12 user) {
       try {
           String pwd = user.getPassword();

           // Encode only if it's not already encoded
           if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
               user.setPassword(passwordEncoder.encode(pwd));
           }

           // No plaintext unique key is handled on entity

           user.setRoles(Arrays.asList("USER"));
           userRepository_13.save(user);
           // Double-safety: ensure plaintext field is not present in DB
           forceUnsetUniqueKey(user.getUserName());

           System.out.println("Saved password: " + user.getPassword());
           return true;
       } catch (Exception e) {
           return false;
       }
    }


    public void saveUser(User_12 user){
        userRepository_13.save(user);
        forceUnsetUniqueKey(user.getUserName());
    }

    public void saveAdmin(User_12 user){
        String pwd = user.getPassword();

        // Encode only if it's not already encoded
        if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(pwd));
        }
        user.setRoles(Arrays.asList("USER","ADMIN"));
        userRepository_13.save(user);
        forceUnsetUniqueKey(user.getUserName());
    }

    // basically this method is for saving entries in mongodb as it takes JournalEntry_6 as i/p and saves it in journalEntryRepository_11 (in Mongo Repository)
    // now we  are able to use this save() method becaz of MongoRepository

    // (Get Method)
    // to get data
    public List<User_12> getAllEntries(){
        return userRepository_13.findAll();              // service here uses findAll() method from the Repository
    }




    // ( Get by Id method) // it is optional due to it may be null
    public Optional<User_12> findById(ObjectId id){
        return userRepository_13.findById(id);        // service here uses findById() method from the Repository
    }


    // Detele by id method

    public void deleteEntry(ObjectId id){
        userRepository_13.deleteById(id);
    }

    // Put (modify) by id method
    // see we are not writing anything here because we just use .findById() (of repository)
    // for that modify purpose we write custom logic in controller itself

    public User_12 findByUserName(String username){
        return userRepository_13.findByUserName(username);
    }

    // ==================== JWT TOKEN METHODS ====================
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    /**
     * Generate a simple JWT-like token (for demonstration purposes)
     * In production, use proper JWT library
     */
    public String generateSimpleToken(String username) {
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + jwtExpirationMs;
        
        // Simple token format: username.timestamp.expiration
        return username + "." + currentTime + "." + expirationTime;
    }
    
    /**
     * Validate simple token
     */
    public boolean validateSimpleToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String username = parts[0];
            Long.parseLong(parts[1]); // timestamp (not used but parsed for validation)
            long expiration = Long.parseLong(parts[2]);
            
            // Check if token is expired
            if (System.currentTimeMillis() > expiration) {
                return false;
            }
            
            // Check if user exists
            return findByUserName(username) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract username from simple token
     */
    public String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                return parts[0];
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Verify password using BCrypt
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Create a new user and set the uniqueKey hash without storing the plaintext
     */
    public boolean saveNewUserWithUniqueKey(User_12 user, String plainUniqueKey) {
        try {
            System.out.println("[saveNewUserWithUniqueKey] Starting save for user: " + user.getUserName());
            
            String pwd = user.getPassword();
            if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
                System.out.println("[saveNewUserWithUniqueKey] Encoding password...");
                user.setPassword(passwordEncoder.encode(pwd));
            }
            
            if (plainUniqueKey != null && !plainUniqueKey.isEmpty()) {
                System.out.println("[saveNewUserWithUniqueKey] Hashing uniqueKey...");
                user.setUniqueKeyHash(hashUniqueKey(plainUniqueKey));
            }
            
            user.setRoles(Arrays.asList("USER"));
            System.out.println("[saveNewUserWithUniqueKey] Saving user to database...");
            
            User_12 savedUser = userRepository_13.save(user);
            System.out.println("[saveNewUserWithUniqueKey] User saved with ID: " + savedUser.getId());
            
            System.out.println("[saveNewUserWithUniqueKey] Forcing unset of uniqueKey field...");
            forceUnsetUniqueKey(user.getUserName());
            
            System.out.println("[saveNewUserWithUniqueKey] ✅ Successfully saved user: " + user.getUserName());
            return true;
        } catch (Exception e) {
            System.out.println("[saveNewUserWithUniqueKey] ❌ ERROR saving user: " + user.getUserName());
            System.out.println("[saveNewUserWithUniqueKey] Exception: " + e.getClass().getName());
            System.out.println("[saveNewUserWithUniqueKey] Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create admin and set the uniqueKey hash without storing the plaintext
     */
    public void saveAdminWithUniqueKey(User_12 user, String plainUniqueKey) {
        String pwd = user.getPassword();
        if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(pwd));
        }
        if (plainUniqueKey != null && !plainUniqueKey.isEmpty()) {
            user.setUniqueKeyHash(hashUniqueKey(plainUniqueKey));
        }
        user.setRoles(Arrays.asList("USER","ADMIN"));
        userRepository_13.save(user);
        forceUnsetUniqueKey(user.getUserName());
    }

    /**
     * Update only the stored uniqueKey hash for an existing user
     */
    public void updateUniqueKeyHash(User_12 user, String plainUniqueKey) {
        if (plainUniqueKey != null && !plainUniqueKey.isEmpty()) {
            user.setUniqueKeyHash(hashUniqueKey(plainUniqueKey));
        }
        userRepository_13.save(user);
        forceUnsetUniqueKey(user.getUserName());

    }

    public void updatePassword(User_12 user, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository_13.save(user);
    }

    private void forceUnsetUniqueKey(String userName) {
        try {
            Query q = new Query(Criteria.where("userName").is(userName));
            Update u = new Update().unset("uniqueKey");
            mongoTemplate.updateFirst(q, u, User_12.class);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    /**
     * Hash a unique key using the application's password encoder (BCrypt)
     */
    public String hashUniqueKey(String uniqueKey) {
        return passwordEncoder.encode(uniqueKey);
    }

    /**
     * Verify a provided unique key against the stored hash
     */
    public boolean matchesUniqueKey(String rawUniqueKey, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawUniqueKey, storedHash);
    }
    
    /**
     * Check if any admin user exists in the system
     * Returns true if at least one user has ADMIN role
     */
    public boolean hasAdminUser() {
        try {
            return getAllEntries().stream()
                    .anyMatch(user -> user.getRoles() != null && user.getRoles().contains("ADMIN"));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== UNIQUE KEY GENERATION METHODS ====================
    
    /**
     * Generate a unique key using user's email ID
     * Uses SHA-256 hashing with timestamp for uniqueness
     */
    public String generateUniqueKey(String email) {
        try {
            long timestamp = System.currentTimeMillis();
            String input = email + "_" + timestamp + "_MyJournalApp2024";
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string and take first 16 characters
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().substring(0, 16).toUpperCase();
        } catch (Exception e) {
            // Fallback to simple hash if SHA-256 fails
            return email.replaceAll("[^a-zA-Z0-9]", "").toUpperCase() + "_" + 
                   String.valueOf(System.currentTimeMillis()).substring(8);
        }
    }
    
    /**
     * Find user by email
     */
    public User_12 findByEmail(String email) {
        return userRepository_13.findByEmail(email);
    }




}
