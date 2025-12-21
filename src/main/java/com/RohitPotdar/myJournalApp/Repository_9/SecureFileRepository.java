package com.RohitPotdar.myJournalApp.Repository_9;

import com.RohitPotdar.myJournalApp.entity_5.SecureFile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Simple Mongo repository to store and fetch encrypted files.
 */
public interface SecureFileRepository extends MongoRepository<SecureFile, String> {

    List<SecureFile> findByUserName(String userName);
}

