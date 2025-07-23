package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class userService_14 {

// don't forget to perform the dependency injection
    @Autowired
    private userRepository_13 userRepository_13;


    // (Post Method)
    // now if we have to save something then write method

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean saveNewUser(User_12 user) {
       try {
           String pwd = user.getPassword();

           // Encode only if it's not already encoded
           if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
               user.setPassword(passwordEncoder.encode(pwd));
           }

           user.setRoles(Arrays.asList("USER"));
           userRepository_13.save(user);

           System.out.println("Saved password: " + user.getPassword());
           return true;
       } catch (Exception e) {
           return false;
       }
    }


    public void saveUser(User_12 user){
        userRepository_13.save(user);
    }

    public void saveAdmin(User_12 user){
        String pwd = user.getPassword();

        // Encode only if it's not already encoded
        if (!pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
            user.setPassword(passwordEncoder.encode(pwd));
        }
        user.setRoles(Arrays.asList("USER","ADMIN"));
        userRepository_13.save(user);
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
            long timestamp = Long.parseLong(parts[1]);
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




}
