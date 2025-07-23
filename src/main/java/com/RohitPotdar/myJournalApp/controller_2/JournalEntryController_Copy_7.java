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
        public ResponseEntity<?> getAllEntriesOfUser()
        {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
            String userName = authentication.getName();
            User_12 byUserName = userService14.findByUserName(userName);
            List<JournalEntry_6> allEntries = byUserName.getAllEntries();
            
            // Decrypt title and content for all entries
            for (JournalEntry_6 entry : allEntries) {
                // Decrypt title
                if (entry.getTitle() != null && journalEntryService_10.isEncrypted(entry.getTitle())) {
                    try {
                        String decryptedTitle = journalEntryService_10.decryptContent(entry.getTitle());
                        entry.setTitle(decryptedTitle);
                    } catch (Exception e) {
                        System.err.println("Failed to decrypt title: " + e.getMessage());
                    }
                }
                // Decrypt content
                if (entry.getContent() != null && journalEntryService_10.isEncrypted(entry.getContent())) {
                    try {
                        String decryptedContent = journalEntryService_10.decryptContent(entry.getContent());
                        entry.setContent(decryptedContent);
                    } catch (Exception e) {
                        System.err.println("Failed to decrypt content: " + e.getMessage());
                    }
                }
            }
            
            if ( allEntries!=null && ! allEntries.isEmpty()){
                return new ResponseEntity<>(allEntries,HttpStatus.OK);
            }
            return new ResponseEntity<>(allEntries,HttpStatus.NOT_FOUND);
        }





        @PostMapping
        public ResponseEntity<JournalEntry_6> createEntry(@RequestBody JournalEntry_6 myEntry ) {
           try {
               Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
               String userName = authentication.getName();
               myEntry.setLocalDateTime(LocalDateTime.now());
               journalEntryService_10.saveEntry(myEntry ,userName);
               return new ResponseEntity<>(myEntry, HttpStatus.OK);
           } catch (Exception e) {
               return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
           }
        }
        // logic is written in JournalEntryService_10


        @GetMapping("id/{myId}")
        public ResponseEntity<JournalEntry_6> getEntryById(@PathVariable ObjectId myId) {
            //return journalEntryService_10.findById(myId).orElse(null); /// before adding http status code
            // only thing to remember is the id is of only that particular user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
            String userName = authentication.getName();
            User_12 user = userService14.findByUserName(userName);
            List<JournalEntry_6> collect = user.getAllEntries().stream().filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
            if(!collect.isEmpty()){
                    Optional<JournalEntry_6> journalEntry_6 = journalEntryService_10.findById(myId);
                if (journalEntry_6.isPresent()) {
                    return new ResponseEntity<>(journalEntry_6.get(), HttpStatus.OK);
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
                @PathVariable ObjectId myId,          // 1. Takes the journal ID from URL (e.g., `/id/123`)
                @RequestBody JournalEntry_6 newEntry

        ) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
            String userName = authentication.getName();
            User_12 user = userService14.findByUserName(userName);
            List<JournalEntry_6> collect = user.getAllEntries().stream().filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
            if(!collect.isEmpty()){
                Optional<JournalEntry_6> journalEntry_6 = journalEntryService_10.findById(myId);
                if (journalEntry_6.isPresent()) {
                      JournalEntry_6 old=  journalEntry_6.get();
                    old.setTitle(
                            (newEntry.getTitle() != null && !newEntry.getTitle().equals(""))
                                    ? newEntry.getTitle()  // Use new title if valid
                                    : old.getTitle()      // Keep old title otherwise
                    );

                    // 6. Same logic for CONTENT
                    old.setContent(
                            (newEntry.getContent() != null && !newEntry.getContent().equals(""))
                                    ? newEntry.getContent()
                                    : old.getContent()
                    );
                    // 7. Save the (updated or unchanged) entry back to the database
                    journalEntryService_10.saveEntry(old, userName);  // use the method with encryption
                    return new ResponseEntity<>(old,HttpStatus.OK);
                }
                }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
    }
