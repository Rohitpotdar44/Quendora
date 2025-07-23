package com.RohitPotdar.myJournalApp.config_16;

import com.RohitPotdar.myJournalApp.Repository_9.userRepository_13;
import com.RohitPotdar.myJournalApp.Service_8.userDetailsServiceImpl_18;
import com.RohitPotdar.myJournalApp.Service_8.userService_14;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity

//public class SecurityConfig_17 extends WebSecurityConfigurerAdapter --->we get error on WebSecurityConfigurerAdapter because it is deprecated in the latest version so we are going to figure it out {
//}

//
public class SecurityConfig_17 extends WebSecurityConfigurerAdapter
{
    @Lazy
    @Autowired
    private userDetailsServiceImpl_18 userDetailsServiceImpl18;
    
    @Autowired
    @Lazy
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/journalCopies/**", "/users/**")
                .authenticated()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()
                .httpBasic().disable() // Disable Basic Authentication
                .formLogin().disable() // Disable form login
                .logout().disable(); // Disable logout

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }


    // override this configuration method also

    // and now here we do all user related stuff
    // like encoding the password by BCryptPasswordEncoder
    // so while login user enter the password in the plaintext then it should be converted and saves on db for further auth
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsServiceImpl18).passwordEncoder(passwordEncoder());
    }
//
//    @Bean
//    public userService_14 userService(userRepository_13 userRepository, PasswordEncoder passwordEncoder) {
//        return new userDetailsServiceImpl_18(userRepository,passwordEncoder);
//    }
//
//
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

//    private static final String CONNECTION_STRING = "mongodb+srv://rj8008493:sUQGxHvJytakFKNj@cluster0.79bjqea.mongodb.net/journaldb?retryWrites=true&w=majority&appName=Cluster0";
//
//
//    @Bean
//    public MongoClient mongoClient() {
//        ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
//        MongoClientSettings settings = MongoClientSettings.builder()
//                .applyConnectionString(connectionString)
//                .build();
//        return MongoClients.create(settings);
//    }

//    @Bean
//    public MongoClient mongoClient() {
//        return MongoClients.create("mongodb+srv://rj8008493:sUQGxHvJytakFKNj@cluster0.79bjqea.mongodb.net/journaldb?retryWrites=true&w=majority&appName=Cluster0");
//    }


    }