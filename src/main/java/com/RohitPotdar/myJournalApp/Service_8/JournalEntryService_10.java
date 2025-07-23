package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.JournalEntryRepository_11;
import com.RohitPotdar.myJournalApp.entity_5.JournalEntry_6;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class JournalEntryService_10 {

    // this is called as dependency injection means we are injecting this JournalEntryRepository_11 Repository into JournalEntryService_10 service
    // Due to this spring automatically writes implementation of the interface at runtime (because interface should have some implementation)
    @Autowired
    private JournalEntryRepository_11 journalEntryRepository_11;

    @Autowired
    private userService_14 userService14;
    
    @Value("${external.encryption.key}")
    private String keyString;

    // (Post Method)
    // now if we have to save something then write method


    // previous one ( when linking is not done)
//    public void saveEntry(JournalEntry_6 journalEntry_6){          // service here uses save() method from the Repository
//        journalEntryRepository_11.save(journalEntry_6);
//    }

    @Transactional
    public void saveEntry(JournalEntry_6 journalEntry_6 ,String userName ){          // service here uses save() method from the Repository
       try {
           // Encrypt the title before saving
           if (journalEntry_6.getTitle() != null && !journalEntry_6.getTitle().isEmpty()) {
               String encryptedTitle = encryptContent(journalEntry_6.getTitle());
               journalEntry_6.setTitle(encryptedTitle);
           }
           
           // Encrypt the content before saving
           if (journalEntry_6.getContent() != null && !journalEntry_6.getContent().isEmpty()) {
               String encryptedContent = encryptContent(journalEntry_6.getContent());
               journalEntry_6.setContent(encryptedContent);
           }
           
           User_12 user = userService14.findByUserName(userName); // like getting user  / rohit
           JournalEntry_6 saved = journalEntryRepository_11.save(journalEntry_6); // saving entries
           user.getAllEntries().add(saved); // add saved entry in the user's journal entry
//           user.setUserName(null);
           // now due to this entry will be saved in the journal entries but it will nut saved in the user's joutnal entries , because we set username null before saving it
           // so due avoid this uncertainity like have of execution is running and another is not ,
           // so to avoid this we use transactional *****-> for more info see theory written
           userService14.saveUser(user); // saving user
       } catch (RuntimeException e) {
           System.out.println(e);
           throw new RuntimeException("An error occured while saving the entry" +e);
       }
    }
    // basically this method is for saving entries in mongodb as it takes JournalEntry_6 as i/p and saves it in journalEntryRepository_11 (in Mongo Repository)
    // now we  are able to use this save() method becaz of MongoRepository

    // we encountered with this error Transaction numbers are only allowed on a replica set member or mongos' on server localhost:27017.
    // because on localhost in mongodb to transaction to happen it requires replication

    // so to manage transactions we make a use of mongo atlas -> and write related info in application properties


    // Removed the saveEntry method without encryption to avoid confusion
    // All saves should go through the encrypted saveEntry method

    //



    // (Get Method)
    // to get data
    public List<JournalEntry_6> getAllEntries(){
        return journalEntryRepository_11.findAll();              // service here uses findAll() method from the Repository
    }

    // ( Get by Id method) // it is optional due to it may be null
    public Optional<JournalEntry_6>findById(ObjectId id){
            Optional<JournalEntry_6> entry = journalEntryRepository_11.findById(id);        // service here uses findById() method from the Repository
            if (entry.isPresent()) {
                JournalEntry_6 journalEntry = entry.get();
                // Decrypt the title before returning
                if (journalEntry.getTitle() != null && isEncrypted(journalEntry.getTitle())) {
                    try {
                        String decryptedTitle = decryptContent(journalEntry.getTitle());
                        journalEntry.setTitle(decryptedTitle);
                    } catch (Exception e) {
                        System.err.println("Failed to decrypt title: " + e.getMessage());
                    }
                }
                // Decrypt the content before returning
                if (journalEntry.getContent() != null && isEncrypted(journalEntry.getContent())) {
                    try {
                        String decryptedContent = decryptContent(journalEntry.getContent());
                        journalEntry.setContent(decryptedContent);
                    } catch (Exception e) {
                        // If decryption fails, keep the encrypted content
                        System.err.println("Failed to decrypt content: " + e.getMessage());
                    }
                }
            }
            return entry;
    }


    // Delete by id method

    // to avoid cascading
    @Transactional
    public boolean deleteEntry(ObjectId id, String userName){
       boolean removed=false;
        try{
            User_12 user = userService14.findByUserName(userName);
             removed = user.getAllEntries().removeIf(x -> x.getId().equals(id));
            if(removed){
                userService14.saveUser(user);
                journalEntryRepository_11.deleteById(id);
            }
        }

       catch (RuntimeException e) {
            System.out.println(e);
            throw new RuntimeException("An error occured while saving the entry" +e);
        }
        return removed;
    }

    // Put (modify) by id method
    // see we are not writing anything here because we just use .findById() (of repository)
    // for that modify purpose we write custom logic in controller itself

    // ==================== AES ENCRYPTION METHODS ====================
    
    /**
     * Generate AES key from the configured key string
     */
    private Key getKey(String keyString) throws NoSuchAlgorithmException {
        byte[] key = keyString.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Encrypt journal content using AES-256
     */
    public String encryptContent(String plainText) throws RuntimeException, IllegalArgumentException {
        if (ObjectUtils.isEmpty(keyString)) {
            throw new IllegalArgumentException("Encryption key not configured");
        }
        
        if (ObjectUtils.isEmpty(plainText)) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(keyString));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting journal content: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt journal content using AES-256
     */
    public String decryptContent(String cipherText) throws RuntimeException, IllegalArgumentException {
        if (ObjectUtils.isEmpty(keyString)) {
            throw new IllegalArgumentException("Encryption key not configured");
        }
        
        if (ObjectUtils.isEmpty(cipherText)) {
            return cipherText;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, getKey(keyString));
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting journal content: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a string is encrypted (Base64 encoded)
     */
    public boolean isEncrypted(String text) {
        if (ObjectUtils.isEmpty(text)) {
            return false;
        }
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
