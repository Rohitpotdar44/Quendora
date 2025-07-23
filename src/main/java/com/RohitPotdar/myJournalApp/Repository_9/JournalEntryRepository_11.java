package com.RohitPotdar.myJournalApp.Repository_9;
//Controller ➡️ Service ➡️ Repository ➡️ Database


// Spring Data MongoDB( which is a Persistence Provider) provides us the interface called as MongoRepository
// so whenever we create any Repository then extends it with MongoRepository

// MongoRepository is a Spring Data interface that provides built-in methods to perform CRUD operations on a MongoDB collection without writing boilerplate code.
//Saves time by providing built-in methods like save(), findAll(), deleteById(), etc.

// NOW SEEE FROM NOW WE ARE DEALING WITH DATABASE SO NOW WE HAVE TO MAP FIELDS WITH DB USING ORM AND ALL

// FROM NOW CONSIDER ***JournalEntryController_Copy_7*** FILE DON'T CONSIDER JournalEntryController_3 BECAUSE IT IS FOR STATIC DATA

import com.RohitPotdar.myJournalApp.entity_5.JournalEntry_6;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;


public interface JournalEntryRepository_11 extends MongoRepository<JournalEntry_6, ObjectId> {

    // here JournalEntry_6 is the collection and obj is the id type

    // now doing this only we can use this repository

    // now go to services
}

