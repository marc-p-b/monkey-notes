package net.kprod.mn.controller;

import net.kprod.mn.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jwt")
public class AuthController {


    private Logger LOG = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserDetailsService userRepository;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        LOG.info("Login request user: {}", request.getUsername());
        UserDetails ud = userRepository.loadUserByUsername(request.getUsername());

        if (ud != null && passwordEncoder.matches(request.getPassword(), ud.getPassword())) {
            LOG.info("Login granted");
            String token = JwtUtil.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token));
        }
        LOG.warn("Login refused");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}

class AuthRequest {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public AuthRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AuthRequest setPassword(String password) {
        this.password = password;
        return this;
    }
}

class AuthResponse {
    private String token;
    public AuthResponse(String token) { this.token = token; }

    public String getToken() {
        return token;
    }
}
