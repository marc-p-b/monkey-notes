package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.AuthRequest;
import fr.monkeynotes.mn.data.AuthResponse;
import fr.monkeynotes.mn.data.dto.DtoUser;
import fr.monkeynotes.mn.data.entity.EntityUser;
import fr.monkeynotes.mn.data.repository.RepositoryUser;
import fr.monkeynotes.mn.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    private Logger LOG = LoggerFactory.getLogger(UserService.class);
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private RepositoryUser repositoryUser;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<EntityUser> optUser = this.repositoryUser.findByUsernameEquals(username);
        if(optUser.isPresent() == false) {
            throw new UsernameNotFoundException("Unknown user "+ username);
        }
        EntityUser user = optUser.get();

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRoles().split(","))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    @Override
    public DtoUser getUser(String username) throws UsernameNotFoundException {
        Optional<EntityUser> user = repositoryUser.findByUsernameEquals(username);
        if(user.isPresent() == false) {
            throw new UsernameNotFoundException("Unknown user "+ username);
        }
        return DtoUser.fromEntity(user.get());
    }

    @Override
    public Optional<AuthResponse> jwtLoginRequest(AuthRequest request) {
        UserDetails ud = this.loadUserByUsername(request.getUsername());

        if (ud != null && passwordEncoder.matches(request.getPassword(), ud.getPassword())) {
            LOG.info("Login username {} granted", request.getUsername());
            String token = JwtUtil.generateToken(ud);
            return Optional.of(new AuthResponse(token));
        }
        LOG.warn("Login username {} refused", request.getUsername());
        return Optional.empty();
    }

    @Override
    public List<DtoUser> listUsers() {
        List<DtoUser> list = repositoryUser.findAll().stream()
                .map(u -> DtoUser.fromEntity(u))
                .toList();
        return list;
    }

    @Override
    public void saveAllUsers(List<DtoUser> uiUsers) {
        //map of users from ui
        Map<String, DtoUser> mapUiUsers = uiUsers.stream()
            .map(u -> {
                //ui only change admin flag ; had to update roles
                Set<String> roles = new HashSet<>();
                roles.add(ROLE_USER);
                if (u.isAdmin()) {
                    roles.add(ROLE_ADMIN);
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
                        .setRoles(ROLE_USER + (uiUser.isAdmin() ? ("," + ROLE_ADMIN) : ""));
            })
            .toList();

        repositoryUser.saveAll(users2Update);
    }

    @Override
    public void setUserPassowrd(String username, String password) {
        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(username);
        if(optionalEntityUser.isPresent() == false) {
            throw new UsernameNotFoundException("Unknown user "+ username);
        }

        EntityUser userEntity = optionalEntityUser.get();

        userEntity.setPassword(passwordEncoder.encode(password));
        repositoryUser.save(userEntity);
    }

    @Override
    public Set<String> setUserAsAdmin(String username) {
        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(username);
        if(optionalEntityUser.isPresent() == false) {
            throw new UsernameNotFoundException("Unknown user "+ username);
        }

        EntityUser userEntity = optionalEntityUser.get();

        String[] roles = userEntity.getRoles().split(",");
        Set<String> rolesSet = new HashSet<>(Arrays.asList(roles));
        if(rolesSet.contains(ROLE_ADMIN)) {
            userEntity.setRoles(ROLE_USER);
        } else {
            userEntity.setRoles(ROLE_ADMIN + "," + ROLE_USER);
        }
        repositoryUser.save(userEntity);
        return rolesSet;
    }

    @Override
    public String createNewUser(DtoUser dtoUser) {
        Optional<EntityUser> optionalEntityUser = repositoryUser.findByUsernameEquals(dtoUser.getUsername());
        if(optionalEntityUser.isPresent() == true) {
            LOG.warn("User {} already exists", dtoUser.getUsername());
            throw new UsernameNotFoundException("User already exists" );
        }

        String rndPassword = UUID.randomUUID().toString();

        String roles = ROLE_USER + (dtoUser.isAdmin() ? "," + ROLE_ADMIN : "");

        EntityUser u = new EntityUser()
                .setUsername(dtoUser.getUsername())
                .setEmail(dtoUser.getEmail())
                .setPassword(new BCryptPasswordEncoder().encode(rndPassword))
                .setRoles(roles);
        repositoryUser.save(u);
        return rndPassword;
    }
}
