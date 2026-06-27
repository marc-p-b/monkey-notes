package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.JwtUtil;
import fr.monkeynotes.mn.data.AuthResponse;
import fr.monkeynotes.mn.data.NoAuthContextHolder;
import fr.monkeynotes.mn.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private Logger LOG= LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserDetailsService userDetailsService;

    public Optional<Authentication> getLoggedAuthentication() {
        SecurityContext sc = SecurityContextHolder.getContext();
        if(sc.getAuthentication() == null) {
            return Optional.empty();
        }
        Authentication authentication = sc.getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    public record UserData(
            String username,
            boolean admin
    ){}

    @Override
    public UserData getUserDataFromContext() {
        // return either Connected / Authenticated user session username
        // or username set in noauth context (when notifying changes from drive in a multiuser setup)

        //TODO no applicable ?

//        if(NoAuthContextHolder.getContext() != null) {
//            return NoAuthContextHolder.getContext().getUsername();
//        }

        Optional<Authentication> optionalAuthentication = getLoggedAuthentication();
        if(optionalAuthentication.isEmpty()) {
            return null;
        }

        //TODO cannot cast !
        UsernamePasswordAuthenticationToken u = (UsernamePasswordAuthenticationToken) optionalAuthentication.get();

        boolean isAdmin = u.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        UserData userData = new UserData(u.getPrincipal().toString(), isAdmin);

        return userData;
    }

    @Override
    public String getUsernameFromContext() {
        // return either Connected / Authenticated user session username
        // or username set in noauth context (when notifying changes from drive in a multiuser setup)

        if(NoAuthContextHolder.getContext() != null) {
            return NoAuthContextHolder.getContext().getUsername();
        }

        Optional<Authentication> optionalAuthentication = getLoggedAuthentication();
        if(optionalAuthentication.isEmpty()) {
            return null;
        }

        //TODO cannot cast !
        UsernamePasswordAuthenticationToken u = (UsernamePasswordAuthenticationToken) optionalAuthentication.get();
        return u.getPrincipal().toString();
    }

    @Override
    public Optional<String> getCurrentAuthToken() {
        Optional<Authentication> optionalAuthentication = getLoggedAuthentication();

        if(optionalAuthentication.isPresent()) {
            UsernamePasswordAuthenticationToken u = (UsernamePasswordAuthenticationToken) optionalAuthentication.get();
            return  Optional.of(u.getCredentials().toString());
        }
        return Optional.empty();
    }

    @Override
    public AuthResponse refreshToken() {
        String username = getUsernameFromContext();
        LOG.info("Token refresh for user: {}", username);
        UserDetails ud = userDetailsService.loadUserByUsername(username);
        return new AuthResponse(JwtUtil.generateToken(ud));
    }
}
