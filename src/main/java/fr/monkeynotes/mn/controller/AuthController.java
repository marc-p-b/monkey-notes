package fr.monkeynotes.mn.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.dto.DtoPreferences;
import fr.monkeynotes.mn.data.dto.DtoUser;
import fr.monkeynotes.mn.data.entity.EntityUser;
import fr.monkeynotes.mn.data.repository.RepositoryUser;
import fr.monkeynotes.mn.service.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
            String token = JwtUtil.generateToken(ud);
            return ResponseEntity.ok(new AuthResponse(token));
        }
        LOG.warn("Login refused");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/list") public ResponseEntity<List<DtoUser>> getUsers() {

        List<DtoUser> list = repositoryUser.findAll().stream()
                .map(u -> DtoUser.fromEntity(u))
                .toList();

        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/user/list/save") public ResponseEntity<String> saveUsers(@RequestBody List<DtoUser> uiUsers) {
        //map of users from ui
        Map<String, DtoUser> mapUiUsers = uiUsers.stream()
                .map(u -> {
                    //ui only change admin flag ; had to update roles
                    Set<String> roles = new HashSet<>();
                    roles.add("USER");
                    if (u.isAdmin()) {
                        roles.add("ADMIN");
                    }
                    u.setRoles(roles);
                    return u;
                })
                .collect(Collectors.toMap(u->u.getUsername(), u->u));

        List<EntityUser> users2Update = repositoryUser.findAll().stream()
            .filter(dbUser -> {
                int h = Objects.hash(dbUser.getUsername(), dbUser.getEmail(), Arrays.stream(dbUser.getRoles().split(",")).collect(Collectors.toSet()));
                //exclude if hash equals
                return mapUiUsers.get(dbUser.getUsername()).hashCode() != h;
            })
            .map(dbUser -> {
                //update entity
                DtoUser uiUser = mapUiUsers.get(dbUser.getUsername());
                return dbUser
                        .setEmail(uiUser.getEmail())
                        .setRoles("USER" + (uiUser.isAdmin() ? ",ADMIN" : ""));
            })
            .toList();

        repositoryUser.saveAll(users2Update);

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/user/{user}/remove")
    public ResponseEntity<String> removeUser(@PathVariable String user) {

        return ResponseEntity.status(HttpStatus.OK).body("User " + user + " removed");
    }

    @PostMapping("/user/{user}/password")
    public ResponseEntity<String> setPassword(@PathVariable String user, @RequestBody String json) {

        DocumentContext ctx = JsonPath.parse(json);
        String password = ctx.read("$.password");

        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(user);
        if (!optionalEntityUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no user found");
        }

        EntityUser userEntity = optionalEntityUser.get();

        userEntity.setPassword(passwordEncoder.encode(password));
        repositoryUser.save(userEntity);

        return ResponseEntity.status(HttpStatus.OK).body("Password set");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{user}/toggleAdmin")
    public ResponseEntity<String> toggleAdmin(@PathVariable String user) {

        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(user);
        if (!optionalEntityUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no user found");
        }

        EntityUser userEntity = optionalEntityUser.get();

        String[] roles = userEntity.getRoles().split(",");
        Set<String> rolesSet = new HashSet<>(Arrays.asList(roles));
        if(rolesSet.contains("ADMIN")) {
            userEntity.setRoles("USER");
        } else {
            userEntity.setRoles("ADMIN,USER");
        }
        repositoryUser.save(userEntity);

        return ResponseEntity.status(HttpStatus.OK).body(rolesSet.contains("ADMIN") ? "Admin role set" : "Admin role removed");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/user/create")
    public ResponseEntity<String> createUser(@RequestBody DtoUser dtoUser) {

        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(dtoUser.getUsername());
        if (optionalEntityUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("user already exists");
        }
        String rndPassword = UUID.randomUUID().toString();

        String roles = "USER" + (dtoUser.isAdmin() ? ",ADMIN" : "");

        EntityUser u = new EntityUser()
                .setUsername(dtoUser.getUsername())
                .setEmail(dtoUser.getEmail())
                .setPassword(new BCryptPasswordEncoder().encode(rndPassword))
                .setRoles(roles);
        repositoryUser.save(u);

        return ResponseEntity.status(HttpStatus.OK).body(rndPassword);
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
