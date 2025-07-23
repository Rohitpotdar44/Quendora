package com.RohitPotdar.myJournalApp.Service_8;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class userDetailsServiceImpl_18 implements UserDetailsService
{
    @Autowired
    private userRepository_13 userRepository13;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User_12 user = userRepository13.findByUserName(username);
        if (user != null) {
            // now it ask for UserDetails so
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUserName())
                    .password(user.getPassword())
                    .roles(user.getRoles().toArray(new String[0]))  // if the size of array is  greater than 0 then it will resize itself
                    .build();

        } else {
                throw new UsernameNotFoundException("User not found"+username);
        }
    }
}
