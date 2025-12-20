package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.JwtFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {
    Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.cors.origin}")
    private String corsOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //TODO improve insecure routes
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/grant-callback").permitAll() //secured using get token param
                    .requestMatchers("/jwt/login").permitAll() //no auth required
                    .requestMatchers("/agent/subscribe/*/*/*").permitAll() //secured using get token param
                    .requestMatchers("/notify").permitAll() //TODO secure ?
                    //needed if llm request with url pointing to api (see QwenServiceImpl)
                    //.requestMatchers("/image/*/*/*").permitAll() //todo secure (ADD a unique key?)
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        String[]aCorsOrigin = corsOrigin.split(",");

        CorsConfiguration config = new CorsConfiguration();
        for(String origin : aCorsOrigin) {
            LOG.info("**** adding CORS origin: {}", origin);
            config.addAllowedOrigin(origin);
        }
        config.setAllowCredentials(true);
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