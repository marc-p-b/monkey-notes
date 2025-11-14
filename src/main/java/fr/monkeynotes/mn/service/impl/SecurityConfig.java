package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.JwtFilter;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //TODO improve insecure routes
        http
                .csrf(csrf -> csrf.disable())  // Disable CSRF for APIs
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/agent/subscribe/*/*/*").permitAll() //auth with get token param
                    .requestMatchers("/jwt/login").permitAll()
                    .requestMatchers("/grant-callback").permitAll() //secure by token... TODO update AuthWebhooksController
                    .requestMatchers("/notify").permitAll() //TODO more secured ?
                    .requestMatchers("/image/*/*/*").permitAll() //todo secure ?
                    .requestMatchers("/imagetemp/*/*/*").permitAll() //todo secure ?
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();


//                .allowedOrigins("https://ohdbcfzqnn.a.pinggy.link")
//                .allowedOrigins("https://notes.monkeynotes.fr")
//                //.allowedOrigins("https://notes.monkeynotes.fr/api")
//                .allowedOrigins("http://localhost:5173") //TODO dev only
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true);

        config.addAllowedOrigin("https://notes.monkeynotes.fr");
        config.setAllowCredentials(true); // if you use cookies or auth headers
        config.addAllowedOriginPattern("*"); // or restrict to your domain
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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

}