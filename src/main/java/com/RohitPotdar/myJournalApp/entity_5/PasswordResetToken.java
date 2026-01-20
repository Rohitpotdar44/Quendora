package com.RohitPotdar.myJournalApp.entity_5;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    @Id
    private String id;
    private String email;
    private String codeHash;
    private LocalDateTime expiresAt;
    private boolean used;
    private int attempts;
    private LocalDateTime createdAt;
}
