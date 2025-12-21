    // we make this copy of JournalEntryController_3
    // now initially these are all controllers that make endpoints and call services
    // and in services we write all business logic -> Service(package) ->JournalEntryService

    // Remember this pattern Controllers (call) ---> Services (calls) --->Repository --> db
    // we can also write entire logic in the controllers but the above one is the best practice

    // so are creating two separate packages Service and Repository


    package com.RohitPotdar.myJournalApp.controller_2;

    import com.RohitPotdar.myJournalApp.Service_8.JournalEntryService_10;
    import com.RohitPotdar.myJournalApp.Service_8.userService_14;
    import com.RohitPotdar.myJournalApp.entity_5.JournalEntry_6;
    import com.RohitPotdar.myJournalApp.entity_5.User_12;
    import org.bson.types.ObjectId;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.web.bind.annotation.*;

    import java.time.LocalDateTime;
    import java.util.*;
    import java.util.stream.Collectors;
    import java.util.stream.Stream;

    @RestController

    @RequestMapping("/journalCopies")

    public class JournalEntryController_Copy_7 {

        // now we create instance of services so other can use it
        @Autowired   // here in short we implement the services in the controller
        private JournalEntryService_10 journalEntryService_10;

        @Autowired
        private userService_14 userService14;

        // now in JournalEntryController_Copy_7 we write methods that curd operations on the journal entries present in it
        // but we have to make crud operations according to the user
        // so change that methods
        // commented ones are previous ones




//        @GetMapping
//        public ResponseEntity<?> getAllEntries()
//        {
//            List<JournalEntry_6> allEntries = journalEntryService_10.getAllEntries();
//            if ( allEntries!=null && ! allEntries.isEmpty()){
//                return new ResponseEntity<>(allEntries,HttpStatus.OK);
//            }
//            return new ResponseEntity<>(allEntries,HttpStatus.NOT_FOUND);
//        }

        @GetMapping
        public ResponseEntity<?> getAllEntriesOfUser(@RequestParam(value = "decrypt", required = false, defaultValue = "false") boolean decrypt,
                                                     @RequestHeader(value = "Authorization", required = false) String authorization)
        {
            try {
                System.out.println("[GET /journalCopies] ===== GET ALL ENTRIES REQUEST =====");
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
                
                if (authentication == null) {
                    System.out.println("[GET /journalCopies] ERROR: No authentication found in SecurityContext");
                    return new ResponseEntity<>("Authentication required", HttpStatus.UNAUTHORIZED);
                }
                
                String userName = authentication.getName();
                System.out.println("[GET /journalCopies] Authenticated user: " + userName);
                System.out.println("[GET /journalCopies] Authorization header: " + (authorization != null ? "present" : "null"));
                
                User_12 byUserName = userService14.findByUserName(userName);
                if (byUserName == null) {
                    System.out.println("[GET /journalCopies] ERROR: User not found in database: " + userName);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
                
                List<JournalEntry_6> allEntries = byUserName.getAllEntries();
                System.out.println("[GET /journalCopies] User has " + allEntries.size() + " entries");
                
                // ALWAYS return encrypted entries by default
                // Entries should only be decrypted on the frontend when user explicitly unlocks them
                System.out.println("[GET /journalCopies] Returning " + allEntries.size() + " ENCRYPTED entries");
                
                if (allEntries != null && !allEntries.isEmpty()) {
                    return new ResponseEntity<>(allEntries, HttpStatus.OK);
                }
                
                System.out.println("[GET /journalCopies] No entries found, returning empty list");
                return new ResponseEntity<>(allEntries, HttpStatus.OK);
            
            } catch (Exception e) {
                System.out.println("[GET /journalCopies] ===== ERROR =====");
                System.out.println("[GET /journalCopies] Exception: " + e.getClass().getName());
                System.out.println("[GET /journalCopies] Message: " + e.getMessage());
                e.printStackTrace();
                return new ResponseEntity<>("Error fetching entries: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }





        @PostMapping
        public ResponseEntity<?> createEntry(@RequestBody Map<String, Object> request) {
           try {
               System.out.println("[POST /journalCopies] ===== CREATE ENTRY REQUEST =====");
               Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
               String userName = authentication.getName();
               System.out.println("[POST /journalCopies] Authenticated user: " + userName);
               
               // Extract entry data and uniqueKey
               String uniqueKey = (String) request.get("uniqueKey");
               System.out.println("[POST /journalCopies] uniqueKey received: " + (uniqueKey != null ? "YES (length=" + uniqueKey.length() + ")" : "NO"));
               
               if (uniqueKey == null || uniqueKey.isEmpty()) {
                   System.out.println("[POST /journalCopies] ERROR: uniqueKey is null or empty");
                   return ResponseEntity.badRequest().body("Unique key is required to encrypt entries");
               }
               
               Map<String, String> entryData = (Map<String, String>) request.get("entry");
               if (entryData == null) {
                   System.out.println("[POST /journalCopies] ERROR: entry data is null");
                   return ResponseEntity.badRequest().body("Entry data is required");
               }
               
               System.out.println("[POST /journalCopies] Entry title: " + entryData.get("title"));
               System.out.println("[POST /journalCopies] Entry content length: " + (entryData.get("content") != null ? entryData.get("content").length() : 0));
               
               JournalEntry_6 myEntry = new JournalEntry_6();
               myEntry.setTitle(entryData.get("title"));
               myEntry.setContent(entryData.get("content"));
               myEntry.setLocalDateTime(LocalDateTime.now());
               
               System.out.println("[POST /journalCopies] Calling saveEntry...");
               journalEntryService_10.saveEntry(myEntry, userName, uniqueKey);
               System.out.println("[POST /journalCopies] Entry saved successfully!");
               
               return new ResponseEntity<>(myEntry, HttpStatus.OK);
           } catch (Exception e) {
               System.out.println("[POST /journalCopies] ===== ERROR =====");
               System.out.println("[POST /journalCopies] Exception: " + e.getClass().getName());
               System.out.println("[POST /journalCopies] Message: " + e.getMessage());
               e.printStackTrace();
               return new ResponseEntity<>("Error creating entry: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
           }
        }
        // logic is written in JournalEntryService_10


        @GetMapping("id/{myId}")
        public ResponseEntity<JournalEntry_6> getEntryById(@PathVariable ObjectId myId,
                                                          @RequestParam(value = "decrypt", required = false, defaultValue = "false") boolean decrypt,
                                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
            //return journalEntryService_10.findById(myId).orElse(null); /// before adding http status code
            // only thing to remember is the id is of only that particular user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
            String userName = authentication.getName();
            User_12 user = userService14.findByUserName(userName);
            List<JournalEntry_6> collect = user.getAllEntries().stream().filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
            if(!collect.isEmpty()){
                    Optional<JournalEntry_6> journalEntry_6 = journalEntryService_10.findById(myId);
                if (journalEntry_6.isPresent()) {
                    JournalEntry_6 entry = journalEntry_6.get();
                    boolean headerRequestsDecryption = false;
                    try {
                        String configuredKey = "PdRgUkXp2s5v8y/B"; // TODO: load from application.properties
                        if (authorization != null && authorization.startsWith("Bearer ")) {
                            String token = authorization.substring(7).trim();
                            headerRequestsDecryption = !token.isEmpty() || configuredKey.equals(token);
                            System.out.println("[GET /journalCopies/id] Authorization header seen. Token length=" + token.length() + ", willDecrypt=" + headerRequestsDecryption);
                        } else {
                            System.out.println("[GET /journalCopies/id] No Authorization header or not Bearer format");
                        }
                    } catch (Exception e) { System.out.println("[GET /journalCopies/id] Header parse error: " + e.getMessage()); }
                    if (decrypt || headerRequestsDecryption) {
                        String providedUniqueKey = extractBearerToken(authorization);
                        boolean hasValidUniqueKey = providedUniqueKey != null
                                && userService14.matchesUniqueKey(providedUniqueKey, user.getUniqueKeyHash());

                        if ((decrypt || hasValidUniqueKey) && !hasValidUniqueKey) {
                            return ResponseEntity.badRequest().body(null);
                        }

                        if (hasValidUniqueKey) {
                            JournalEntry_6 decrypted = decryptSingleEntryWithUserKey(entry, providedUniqueKey);
                            return new ResponseEntity<>(decrypted, HttpStatus.OK);
                        }

                        return new ResponseEntity<>(entry, HttpStatus.OK);
                    }
                }
            }

                return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        }
//        @DeleteMapping("id/{myId}")
//        public ResponseEntity<?> deleteEntryById(@PathVariable ObjectId myId) {
//
//             journalEntryService_10.deleteEntry(myId);
//                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        } //////// now if we run this method then it delete that particular entry from the db but in that respected particular user still contains it's id , called as cascade delete
        // so now we are fixing this
        // main logic is written in services del method
        @DeleteMapping("id/{myId}")
        public ResponseEntity<?> deleteEntryById(@PathVariable ObjectId myId ) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
            String userName = authentication.getName();

            boolean removed= journalEntryService_10.deleteEntry(myId,userName);
            if(removed){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }


        @PutMapping("/id/{myId}")
        public ResponseEntity<?> updateJournalById(
                @PathVariable ObjectId myId,
                @RequestBody Map<String, Object> request
        ) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            User_12 user = userService14.findByUserName(userName);
            List<JournalEntry_6> collect = user.getAllEntries().stream().filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
            
            if(!collect.isEmpty()){
                Optional<JournalEntry_6> journalEntry_6 = journalEntryService_10.findById(myId);
                if (journalEntry_6.isPresent()) {
                    // Extract data from request
                    Map<String, String> entryData = (Map<String, String>) request.get("entry");
                    String uniqueKey = (String) request.get("uniqueKey");
                    
                    if (uniqueKey == null || uniqueKey.isEmpty()) {
                        return ResponseEntity.badRequest().body("Unique key is required to encrypt updates");
                    }
                    
                    JournalEntry_6 old = journalEntry_6.get();
                    
                    // Update with new encrypted content
                    if (entryData.get("title") != null && !entryData.get("title").isEmpty()) {
                        old.setTitle(entryData.get("title"));
                    }
                    
                    if (entryData.get("content") != null && !entryData.get("content").isEmpty()) {
                        old.setContent(entryData.get("content"));
                    }
                    
                    // Save with encryption
                    journalEntryService_10.saveEntry(old, userName, uniqueKey);
                    return new ResponseEntity<>(old, HttpStatus.OK);
                }
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        private String extractBearerToken(String authorization) {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7).trim();
                return token.isEmpty() ? null : token;
            }
            return null;
        }

        private List<JournalEntry_6> decryptEntriesWithUserKey(List<JournalEntry_6> entries, String uniqueKey) {
            List<JournalEntry_6> decrypted = new ArrayList<>();
            for (JournalEntry_6 entry : entries) {
                decrypted.add(decryptSingleEntryWithUserKey(entry, uniqueKey));
            }
            return decrypted;
        }

        private JournalEntry_6 decryptSingleEntryWithUserKey(JournalEntry_6 entry, String uniqueKey) {
            JournalEntry_6 copy = new JournalEntry_6();
            copy.setId(entry.getId());
            copy.setLocalDateTime(entry.getLocalDateTime());
            copy.setTitle(decryptFieldIfNeeded(entry.getTitle(), uniqueKey));
            copy.setContent(decryptFieldIfNeeded(entry.getContent(), uniqueKey));
            return copy;
        }

        private String decryptFieldIfNeeded(String value, String uniqueKey) {
            if (value == null) {
                return null;
            }
            try {
                if (journalEntryService_10.isEncrypted(value)) {
                    return journalEntryService_10.decryptWithUserKey(value, uniqueKey);
                }
            } catch (Exception e) {
                System.out.println("[journalCopies] Decrypt error: " + e.getMessage());
            }
            return value;
        }

        @PostMapping("/decrypt")
        public ResponseEntity<?> decryptEntry(@RequestBody Map<String, Object> request) {
            try {
                System.out.println("[POST /journalCopies/decrypt] ===== DECRYPT REQUEST =====");
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String userName = authentication.getName();
                System.out.println("[POST /journalCopies/decrypt] User: " + userName);
                
                // Get the user's unique key from request
                String userUniqueKey = (String) request.get("uniqueKey");
                System.out.println("[POST /journalCopies/decrypt] uniqueKey received: " + (userUniqueKey != null ? "YES (length=" + userUniqueKey.length() + ")" : "NO"));
                
                if (userUniqueKey == null || userUniqueKey.trim().isEmpty()) {
                    System.out.println("[POST /journalCopies/decrypt] ERROR: uniqueKey is null or empty");
                    return ResponseEntity.badRequest().body("Unique key is required for decryption");
                }
                
                // Get the entry data
                Map<String, Object> entryData = (Map<String, Object>) request.get("entryData");
                if (entryData == null) {
                    System.out.println("[POST /journalCopies/decrypt] ERROR: entryData is null");
                    return ResponseEntity.badRequest().body("Entry data is required");
                }
                
                String encryptedTitle = (String) entryData.get("title");
                String encryptedContent = (String) entryData.get("content");
                System.out.println("[POST /journalCopies/decrypt] Encrypted title length: " + (encryptedTitle != null ? encryptedTitle.length() : 0));
                System.out.println("[POST /journalCopies/decrypt] Encrypted content length: " + (encryptedContent != null ? encryptedContent.length() : 0));
                
                // Decrypt with user's unique key
                String decryptedTitle = encryptedTitle;
                String decryptedContent = encryptedContent;
                
                System.out.println("[POST /journalCopies/decrypt] Starting decryption...");
                
                if (encryptedTitle != null && journalEntryService_10.isEncrypted(encryptedTitle)) {
                    System.out.println("[POST /journalCopies/decrypt] Decrypting title...");
                    decryptedTitle = journalEntryService_10.decryptWithUserKey(encryptedTitle, userUniqueKey);
                    System.out.println("[POST /journalCopies/decrypt] Title decrypted: " + decryptedTitle);
                }
                
                if (encryptedContent != null && journalEntryService_10.isEncrypted(encryptedContent)) {
                    System.out.println("[POST /journalCopies/decrypt] Decrypting content...");
                    decryptedContent = journalEntryService_10.decryptWithUserKey(encryptedContent, userUniqueKey);
                    System.out.println("[POST /journalCopies/decrypt] Content decrypted (length: " + decryptedContent.length() + ")");
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("title", decryptedTitle);
                response.put("content", decryptedContent);
                response.put("message", "Entry decrypted successfully");
                
                System.out.println("[POST /journalCopies/decrypt] ✅ Decryption successful!");
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                System.out.println("[POST /journalCopies/decrypt] ===== ERROR =====");
                System.out.println("[POST /journalCopies/decrypt] Exception: " + e.getClass().getName());
                System.out.println("[POST /journalCopies/decrypt] Message: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest().body("Decryption failed: " + e.getMessage());
            }
        }
    }

