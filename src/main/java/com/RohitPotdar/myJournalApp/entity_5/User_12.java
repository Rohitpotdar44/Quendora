package com.RohitPotdar.myJournalApp.entity_5;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User_12 {

    private ObjectId id;
    @Indexed(unique = true) // NOW IF WE HAVE NOT HAVE TO ADD THIS INDEXING MANUALLY THEN WRITE ONE PROPERTY IN APPLI PROPS -> spring.data.mongodb.auto-index-creation=true
    @NonNull
    private String userName;
    @NonNull
    private  String password;
    private List<String> roles;


    // basically for the authentication purpose we have to make a link between the journalEntry and Users
    // we make list but to link we have to use @DBRef
    @DBRef
    private List<JournalEntry_6> allEntries = new ArrayList<>();



}
