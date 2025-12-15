package com.RohitPotdar.myJournalApp.config_16;

import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Component
public class DataSanitizer implements ApplicationRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Query query = new Query(Criteria.where("uniqueKey").exists(true));
            Update update = new Update().unset("uniqueKey");
            mongoTemplate.updateMulti(query, update, User_12.class);
        } catch (Exception ignored) {
            // Best-effort cleanup; ignore failures to avoid blocking startup
        }
    }
}


