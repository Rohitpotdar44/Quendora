package com.RohitPotdar.myJournalApp.service;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.Service_8.JournalEntryService_10;
import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class userServiceTests {

    @Autowired
    private userRepository_13 userRepository13;
    @Autowired
    private userService_14 userService14;
    @Autowired
    private JournalEntryService_10 journalEntryService;


    @Test
    @Disabled
    public void testByUserName(){
//        assertEquals(4,2+2);
//
        User_12 user = userRepository13.findByUserName("Sanket");
        assertNotNull(!user.getAllEntries().isEmpty());
    }

    @ParameterizedTest
    @CsvSource(
            {
              "Arsalan",
              "Sanket"
            }
    )
    public void testByParameters(String name){
        assertNotNull(userRepository13.findByUserName(name),"Failed for "+name );

    }

    // ==================== AES ENCRYPTION TESTS ====================
    
    @Test
    public void testEncryptionAndDecryption() {
        // Test data
        String originalContent = "This is my secret journal entry content!";
        
        // Test encryption
        String encryptedContent = journalEntryService.encryptContent(originalContent);
        assertNotNull(encryptedContent);
        assertNotEquals(originalContent, encryptedContent);
        assertTrue(journalEntryService.isEncrypted(encryptedContent));
        
        // Test decryption
        String decryptedContent = journalEntryService.decryptContent(encryptedContent);
        assertEquals(originalContent, decryptedContent);
    }

    @Test
    public void testEmptyContent() {
        // Test with null content
        String nullContent = null;
        String encryptedNull = journalEntryService.encryptContent(nullContent);
        assertNull(encryptedNull);
        
        // Test with empty content
        String emptyContent = "";
        String encryptedEmpty = journalEntryService.encryptContent(emptyContent);
        assertEquals(emptyContent, encryptedEmpty);
    }

    @Test
    public void testSpecialCharacters() {
        // Test with special characters
        String specialContent = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        String encryptedSpecial = journalEntryService.encryptContent(specialContent);
        String decryptedSpecial = journalEntryService.decryptContent(encryptedSpecial);
        assertEquals(specialContent, decryptedSpecial);
    }

    // also has ValueSource( we specify  data type) then EnumSource , Argument Source etc

//    @ParameterizedTest
//    @ArgumentsSource(userArgumentsProvider_2.class)
//    public void testSaveNewUser(User_12 user12){
//        assertTrue(userService14.saveNewUser(user12));
//    }

}
