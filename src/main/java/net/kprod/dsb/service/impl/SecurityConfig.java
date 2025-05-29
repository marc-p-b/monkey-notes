package net.kprod.dsb.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/grant-callback").permitAll()  // Public endpoints
                .requestMatchers("/notify").permitAll()
                .requestMatchers("/image/*/*/*").permitAll() //todo secure
                .anyRequest().authenticated()  // Secure all other endpoints
            )
            .formLogin(Customizer.withDefaults()) // Enable form login
            .httpBasic(Customizer.withDefaults()); // Enable basic auth

        return http.build();
    }

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
    public class MethodSecurityConfig
            extends GlobalMethodSecurityConfiguration {
    }

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }


//
//    @Bean
//    public UserDetailsService userDetailsService() {
//        UserDetails user1 = User.withDefaultPasswordEncoder()
//                .username("marc")
//                .password("outsoon4242")
//                .roles("USER")
//                .build();
//
//        UserDetails user2 = User.withDefaultPasswordEncoder()
//                .username("marc-test")
//                .password("outsoon4242")
//                .roles("USER")
//                .build();
//
//        UserDetails user3 = User.withDefaultPasswordEncoder()
//                .username("celine")
//                .password("outsoon4242")
//                .roles("USER")
//                .build();
//
//        List<UserDetails> list = Arrays.asList(user1, user2, user3);
//
//        return new InMemoryUserDetailsManager(list);
//    }
}