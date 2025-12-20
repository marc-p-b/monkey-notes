package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.AuthRequest;
import fr.monkeynotes.mn.data.AuthResponse;
import fr.monkeynotes.mn.data.dto.DtoUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserService {
    DtoUser getUser(String username);
    Optional<AuthResponse> jwtLoginRequest(AuthRequest request);
    List<DtoUser> listUsers();
    void saveAllUsers(List<DtoUser> uiUsers);
    void setUserPassowrd(String user, String password);
    Set<String> setUserAsAdmin(String user);
    String createNewUser(DtoUser dtoUser);
}
