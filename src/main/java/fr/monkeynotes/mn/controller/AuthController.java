package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.entity.EntityUser;
import fr.monkeynotes.mn.data.repository.RepositoryUser;
import fr.monkeynotes.mn.service.DriveService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController

public class AuthController {
    private Logger LOG = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserDetailsService userRepository;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private DriveService driveService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping(value = "/drive/disconnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> disconnect() {
        driveService.disconnect();
        return ResponseEntity.status(HttpStatus.OK).body("Disconnected");
    }

    @PostMapping(value = "/jwt/login", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping("/user/{user}/password")
    public ResponseEntity<String> setPassword(@PathVariable String user, @RequestParam String password) {

        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(user);
        if (!optionalEntityUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no user found");
        }

        EntityUser userEntity = optionalEntityUser.get();

        userEntity.setPassword(passwordEncoder.encode(password));
        repositoryUser.save(userEntity);

        return ResponseEntity.status(HttpStatus.OK).body("Password set");
    }

    @GetMapping("/user/create/{user}")
    public ResponseEntity<String> createUser(@PathVariable String user) {

        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(user);
        if (optionalEntityUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("user already exists");
        }
        String rndPassword = UUID.randomUUID().toString();

        EntityUser u = new EntityUser()
                .setUsername(user)
                .setPassword(new BCryptPasswordEncoder().encode(rndPassword))
                .setRoles("USER");
        repositoryUser.save(u);

        return ResponseEntity.status(HttpStatus.OK).body("User created");
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
