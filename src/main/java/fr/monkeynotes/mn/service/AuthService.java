package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.AuthResponse;
import fr.monkeynotes.mn.service.impl.AuthServiceImpl;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface AuthService {
    Optional<Authentication> getLoggedAuthentication();
    AuthServiceImpl.UserData getUserDataFromContext();
    String getUsernameFromContext();
    Optional<String> getCurrentAuthToken();
    AuthResponse refreshToken();
}
