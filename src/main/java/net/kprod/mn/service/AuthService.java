package net.kprod.mn.service;

import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface AuthService {
    Optional<Authentication> getLoggedAuthentication();
    String getUsernameFromContext();
    Optional<String> getCurrentAuthToken();
}
