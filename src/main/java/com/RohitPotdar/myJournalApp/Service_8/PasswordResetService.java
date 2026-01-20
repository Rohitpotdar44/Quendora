package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.PasswordResetTokenRepository;
import com.RohitPotdar.myJournalApp.entity_5.PasswordResetToken;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final userService_14 userService14;
    private final SendGridEmailService sendGridEmailService;

    @Value("${app.reset-code.ttl-minutes:10}")
    private int ttlMinutes;

    @Value("${app.reset-code.max-attempts:5}")
    private int maxAttempts;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                userService_14 userService14,
                                SendGridEmailService sendGridEmailService) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService14 = userService14;
        this.sendGridEmailService = sendGridEmailService;
    }

    public void sendResetCode(String email) {
        User_12 user = userService14.findByEmail(email);
        if (user == null) {
            // Don't reveal user existence
            return;
        }

        String code = generateCode();
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .attempts(0)
                .used(false)
                .build();
        tokenRepository.save(token);

        sendGridEmailService.sendResetCode(email, code, ttlMinutes);
    }

    public void verifyCode(String email, String code) {
        PasswordResetToken token = tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Reset code not found. Please request a new code."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset code has expired. Please request a new code.");
        }

        if (token.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException("Too many attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new IllegalArgumentException("Invalid reset code.");
        }
    }

    public void resetPassword(String email, String code, String newPassword) {
        PasswordResetToken token = tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Reset code not found. Please request a new code."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset code has expired. Please request a new code.");
        }

        if (token.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException("Too many attempts. Please request a new code.");
        }

        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new IllegalArgumentException("Invalid reset code.");
        }

        User_12 user = userService14.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        userService14.updatePassword(user, newPassword);
        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
