package net.kprod.dsb.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.List;

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
                .requestMatchers("/image/*/*").permitAll()
                .anyRequest().authenticated()  // Secure all other endpoints
            )
            .formLogin(Customizer.withDefaults()) // Enable form login
            .httpBasic(Customizer.withDefaults()); // Enable basic auth

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user1 = User.withDefaultPasswordEncoder()
                .username("marc")
                .password("outsoon4242")
                .roles("USER")
                .build();

        UserDetails user2 = User.withDefaultPasswordEncoder()
                .username("marc-test")
                .password("outsoon4242")
                .roles("USER")
                .build();

        UserDetails user3 = User.withDefaultPasswordEncoder()
                .username("celine")
                .password("outsoon4242")
                .roles("USER")
                .build();

        List<UserDetails> list = Arrays.asList(user1, user2, user3);

        return new InMemoryUserDetailsManager(list);
    }
}