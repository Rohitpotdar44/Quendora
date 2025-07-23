package com.RohitPotdar.myJournalApp.controller_2;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class userController_15 {

    @Autowired
    private userService_14 userService_14;

    @Autowired
    private userRepository_13 userRepository13;

    // get all is in admin
//    @GetMapping
//    public List<User_12> getAllUsers(){
//        return userService_14.getAllEntries();
//    }
    // post  is in public controller

    @PutMapping
    // Now Earlier we update the user by using it's id we so require path variable also
    // here we are updating the user by using it's userName so we require RequestBody only
    // so now we have to update repository and services accordingly
    public ResponseEntity<?> updateUser( @RequestBody User_12 myUser ) {
    //     now the concept here is in springboot the details of the authenticated user is managed and stored by SecurityContextHolder so we are making use of it
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // get that authenticated user
        String name = authentication.getName();
        User_12 userInDb = userService_14.findByUserName(name);
            userInDb.setUserName(myUser.getUserName()); // setting username from written in body
            userInDb.setPassword(myUser.getPassword()); // same for password
            userService_14.saveNewUser(userInDb); // basically it performs updation by saving new content on previous one
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @DeleteMapping
    public ResponseEntity<?> deleteUserByName(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userRepository13.deleteByUserName(authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
