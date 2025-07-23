package com.RohitPotdar.myJournalApp.entity_5;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

// now to map these fields with collections in mongodb we have use @Document Annotation
// it tells us these documents contains fields to map
@Document(collection = "journalEntry_6")            // if we not write this then it search for JournalEntry_6 then
@Data
@NoArgsConstructor
public class JournalEntry_6 {
// now entry has it's id , title  , content
    // now to map id as primary key give @Id annotation
    @Id
    private ObjectId id;
    @NonNull
    private  String title;
    private String content;
    private LocalDateTime localDateTime;



}
