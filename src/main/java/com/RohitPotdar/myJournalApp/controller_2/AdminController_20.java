package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin")
public class AdminController_20 {

    @Autowired
    private userService_14 userService_14;
    @GetMapping("/all-entries")
   public ResponseEntity<?> getAllUsers(){
        List<User_12> allEntries = userService_14.getAllEntries();
        if(allEntries !=null && !allEntries.isEmpty()){
            return  new ResponseEntity<>(allEntries, HttpStatus.OK);
        }
        return new ResponseEntity<>(allEntries, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create-admin")
    public void createAdmin(@RequestBody User_12 user12) {
        userService_14.saveAdmin(user12);
    }
}
