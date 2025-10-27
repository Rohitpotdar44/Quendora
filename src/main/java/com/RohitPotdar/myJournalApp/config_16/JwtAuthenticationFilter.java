package com.RohitPotdar.myJournalApp.config_16;

import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private userService_14 userService;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = getJwtFromRequest(request);
            System.out.println("JWT Filter - Received token: " + token);
            System.out.println("JWT Filter - Request URL: " + request.getRequestURL());

            if (StringUtils.hasText(token)) {
                String username = null;

                // Try to validate as JWT token first
                if (userService.validateSimpleToken(token)) {
                    username = userService.getUsernameFromToken(token);
                    System.out.println("JWT Filter - Valid JWT token, username: " + username);
                } else {
                    // If not a JWT token, try to find user by unique key
                    System.out.println("JWT Filter - Not a JWT token, trying unique key lookup");
                    username = findUserByUniqueKey(token);
                }

                if (username != null) {
                    System.out.println("JWT Filter - Setting authentication for user: " + username);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    System.out.println("JWT Filter - No username found, authentication not set");
                }
            } else {
                System.out.println("JWT Filter - No token provided");
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }
    
    private String findUserByUniqueKey(String uniqueKey) {
        try {
            System.out.println("JWT Filter - Looking for unique key: " + uniqueKey);
            List<User_12> allUsers = userService.getAllEntries();
            System.out.println("JWT Filter - Total users found: " + allUsers.size());
            
            for (User_12 user : allUsers) {
                System.out.println("JWT Filter - User: " + user.getUserName() + ", Unique Key: " + user.getUniqueKey());
            }
            
            String username = allUsers.stream()
                    .filter(user -> uniqueKey.equals(user.getUniqueKey()))
                    .map(user -> user.getUserName())
                    .findFirst()
                    .orElse(null);
                    
            System.out.println("JWT Filter - Found username: " + username);
            return username;
        } catch (Exception e) {
            System.out.println("JWT Filter - Exception in findUserByUniqueKey: " + e.getMessage());
            return null;
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 