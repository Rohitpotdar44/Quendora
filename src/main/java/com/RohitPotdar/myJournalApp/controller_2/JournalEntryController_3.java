//package com.RohitPotdar.myJournalApp.controller_2;
//
//import com.RohitPotdar.myJournalApp.entity_5.JournalEntry_6;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//
//@RestController  // special type of component that handles http requests , special type means we are writing endpoints using methods here ( e.g /pricing)
//
//@RequestMapping("/rohitJournal")  // it applies mapping to the entire class , consider one method has mapping /abc then due to request map it will be /journal/abc  , and if not any mapping present inside then it search through whether it is @GetMapping or @PostMapping
//public class JournalEntryController_3 {
//
//    private Map<Long, JournalEntry_6> journalEntry6Map = new HashMap<>(); // LIKE TABLE CONTAINS ID AS A KEY AND JournalEntry_6 AS VALUE
//
//
//    // now since we don't have any database so for data purpose we created one list of JournalEntry_6
//    // Since it's just a method so in order to access it  through end point we have to add @GetMapping
//    @GetMapping         // localhost:8082/journal    GET
//    public List<JournalEntry_6> getAll() { // and METHODS INSIDE A CONTROLLER CLASS SHOULD BE PUBLIC SO THAT THEY CAN BE ACCESSED AND  INVOKED BY THE SPRING FRAMEWORK OR EXTERNAL HTTP REQUESTS
//
//        return new ArrayList<>(journalEntry6Map.values()); // this returns data by converting it into ArrayList
//    }
//
//
//    @PostMapping           // localhost:8082/journal    POST
//    // PostMapping means we have to create resource, or we can say that entry so it is created inside Postman -> Body
//
//    //Selecting “raw” and “JSON” in the body of a POST request in Postman indicates that the request body will contain data in JSON format, allowing the server to parse and process the incoming data accurately. This ensures that the data is transmitted and received in a structured manner, following the JSON format conventions.
//
//    /*
//    {
//    "id":1,
//    "title":"Morning",
//    "content":"Morning Was Good"
//    }
//
//    now we have to send this data inside createEntry() so we have to send @RequestBody inside createEntry()
//    IT'S LIKE SAYING, HEY SPRING, PLEASE TAKE THE “DATA FROM THE REQUEST AND TURN IT INTO A JAVA OBJECT THAT | CAN USE IN MY CODE.”
//     */
//
//
//    // commented due to it put error when it's copy file is running due to id's data type
////    public boolean createEntry(@RequestBody JournalEntry_6 myEntry) {  // here we are Requesting JournalEntry_6 from Body and cresting JournalEntry_6's instance myEntry(basically data from postman come into this)
////        journalEntry6Map.put(myEntry.getId(), myEntry); // putting in that map
////        return true;
////    }
//
//
//    // now for getting specific entry -> now there are two imp things here path variable and request parameter
//    // localhost:8082/journal/id?name=Rohit -->name is the request parameter
//    //localhost:8082/journal/id/Rohit  --> Rohit is the path variable
//    // now if we want entry of only id=2 then -- > localhost:8082/journal/id/2  , for that we have to create a var like {myId}  (2 is path variable here)
//    @GetMapping("id/{myId}")
//    public JournalEntry_6 getEntryById(@PathVariable Long myId) {
//        return journalEntry6Map.get(myId);
//    }
//
//    @DeleteMapping("id/{myId}")
//    public JournalEntry_6 deleteEntryById(@PathVariable Long myId) {
//        return journalEntry6Map.remove(myId);
//    }
//    @PutMapping("/id/{id}")
//    public JournalEntry_6  updateJournalById(@PathVariable Long id ,@RequestBody JournalEntry_6 myEntry){
//            return  journalEntry6Map.put(id, myEntry);
//    }
//
//}