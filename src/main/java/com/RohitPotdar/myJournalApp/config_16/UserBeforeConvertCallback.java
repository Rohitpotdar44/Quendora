package com.RohitPotdar.myJournalApp.config_16;

import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;

@Component
public class UserBeforeConvertCallback implements BeforeConvertCallback<User_12> {

    @Override
    public User_12 onBeforeConvert(User_12 user, String collection) {
        if (user == null) {
            return null;
        }
        // No-op: entity no longer contains plaintext uniqueKey field
        return user;
    }
}


