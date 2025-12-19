package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoUser;
import fr.monkeynotes.mn.data.entity.EntityUser;
import fr.monkeynotes.mn.data.repository.RepositoryUser;
import fr.monkeynotes.mn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserDetailsService, UserService {

    @Autowired
    private RepositoryUser repositoryUser;

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
}
