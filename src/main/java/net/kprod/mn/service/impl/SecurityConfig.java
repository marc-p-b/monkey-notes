package net.kprod.mn.service.impl;

import net.kprod.mn.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {



//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//            .csrf(csrf -> csrf.disable())  // Disable CSRF for APIs
//                .cors(Customizer.withDefaults())
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/grant-callback").permitAll()  // Public endpoints
//                .requestMatchers("/notify").permitAll()
//                .requestMatchers("/image/*/*/*").permitAll() //todo secure
//                .requestMatchers("/imagetemp/*/*/*").permitAll() //todo secure
//                    .requestMatchers("/jwt/*").permitAll() //JWT TEST
//                .anyRequest().authenticated()  // Secure all other endpoints
//            )
//
//            .formLogin(Customizer.withDefaults()) // Enable form login
//            .httpBasic(Customizer.withDefaults()); // Enable basic auth
//
//        return http.build();
//    }



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())  // Disable CSRF for APIs
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/agent/subscribe/*/*/*").permitAll() //auth with get token param
                        .requestMatchers("/jwt/login").permitAll()
                        .requestMatchers("/grant-callback").permitAll()  // Public endpoints
                        .requestMatchers("/notify").permitAll()
                        .requestMatchers("/image/*/*/*").permitAll() //todo secure
                        .requestMatchers("/imagetemp/*/*/*").permitAll() //todo secure

                        .anyRequest().authenticated()  // Secure all other endpoints
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);



        return http.build();
    }


//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//
//
//        http.csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/api/auth/**").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
//    }

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