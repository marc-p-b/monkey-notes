package net.kprod.mn.service.impl;

import net.kprod.mn.data.NoAuthContextHolder;
import net.kprod.mn.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private Logger LOG= LoggerFactory.getLogger(AuthServiceImpl.class);

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

    @Override
    public String getUsernameFromContext() {
        // return either Connected / Authenticated user session username
        // or username set in noauth context (when notifying changes from drive in a multiuser setup)

        if(NoAuthContextHolder.getContext() != null) {
            return NoAuthContextHolder.getContext().getUsername();
        }
//        else {
//            LOG.warn("NoAuthContextHolder context is set");
//
//        }

        Optional<Authentication> optionalAuthentication = getLoggedAuthentication();
        if(optionalAuthentication.isEmpty()) {
            //LOG.error("No authentication found");
            return null;
        }

        //TODO cannot cast !
        UsernamePasswordAuthenticationToken u = (UsernamePasswordAuthenticationToken) optionalAuthentication.get();//.getPrincipal();
        return u.getPrincipal().toString();
    }
}
