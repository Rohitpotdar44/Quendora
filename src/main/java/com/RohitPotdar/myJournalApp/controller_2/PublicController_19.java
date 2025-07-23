package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// always write this HealthCheck class in any spring boot project (it is a good practice)
@RestController
@RequestMapping("/public")
public class PublicController_19 {


    @Autowired
    private userService_14 userService_14;

    // now to map this method to the endpoint we have to map @GetMapping(/endpoint) so control will get to this healthCheck() method and we get OK
    @GetMapping("/health-check")  // here health check is the endpoint
    public String healthCheck() {
        return "OK";
    }


    @PostMapping("/createUser")
    public void addNewUser(@RequestBody User_12 myUser) {
        userService_14.saveNewUser( myUser);
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
            
            // Generate JWT token
            String token = userService_14.generateSimpleToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", username);
            response.put("roles", user.getRoles());
            response.put("token", token);
            response.put("tokenType", "Bearer");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }
}

