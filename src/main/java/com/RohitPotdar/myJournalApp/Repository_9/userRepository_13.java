package com.RohitPotdar.myJournalApp.Repository_9;

import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface userRepository_13 extends MongoRepository<User_12, ObjectId> {

    //  now we have to declare one method which returns Username ,  for that userName purpose
    User_12 findByUserName(String username); //
    // now create respective method in the services

    void deleteByUserName(String username);

}
