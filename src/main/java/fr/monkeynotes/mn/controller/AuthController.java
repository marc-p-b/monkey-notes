package fr.monkeynotes.mn.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import fr.monkeynotes.mn.data.AuthRequest;
import fr.monkeynotes.mn.data.AuthResponse;
import fr.monkeynotes.mn.data.dto.DtoUser;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.DriveService;
import fr.monkeynotes.mn.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
public class AuthController {
    private Logger LOG = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private DriveService driveService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @GetMapping(value = "/drive/disconnect", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> disconnect() {
        driveService.disconnect();
        return ResponseEntity.status(HttpStatus.OK).body("Disconnected");
    }

    @GetMapping(value = "/user/whoami", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> whoami() {
        String username = authService.getUsernameFromContext();
        return ResponseEntity.status(HttpStatus.OK).body(username);
    }

    @PostMapping(value = "/jwt/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        LOG.info("Login request user: {}", request.getUsername());

        Optional<AuthResponse> response = userService.jwtLoginRequest(request);
        if(response.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(response.get());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/list") public ResponseEntity<List<DtoUser>> getUsers() {
        List<DtoUser> list = userService.listUsers();
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/user/list/save") public ResponseEntity<String> saveUsers(@RequestBody List<DtoUser> uiUsers) {
        userService.saveAllUsers(uiUsers);

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/user/{user}/remove")
    public ResponseEntity<String> removeUser(@PathVariable String user) {

        //TODO
        return ResponseEntity.status(HttpStatus.OK).body("User " + user + " removed");
    }

    @PostMapping("/user/{user}/password")
    public ResponseEntity<String> setPassword(@PathVariable String user, @RequestBody String json) {
        DocumentContext ctx = JsonPath.parse(json);
        String password = ctx.read("$.password");

        userService.setUserPassowrd(user, password);

        return ResponseEntity.status(HttpStatus.OK).body("Password set");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{user}/toggleAdmin")
    public ResponseEntity<String> toggleAdmin(@PathVariable String user) {
        Set<String> rolesSet = userService.setUserAsAdmin(user);

        return ResponseEntity.status(HttpStatus.OK).body(rolesSet.contains("ADMIN") ? "Admin role set" : "Admin role removed");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/user/create")
    public ResponseEntity<String> createUser(@RequestBody DtoUser dtoUser) {
        String rndPassword = userService.createNewUser(dtoUser);

        return ResponseEntity.status(HttpStatus.OK).body(rndPassword);
    }
}




