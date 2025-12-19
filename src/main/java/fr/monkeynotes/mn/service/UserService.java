package fr.monkeynotes.mn.service;

import fr.monkeynotes.mn.data.dto.DtoUser;

public interface UserService {
    DtoUser getUser(String username);
}
