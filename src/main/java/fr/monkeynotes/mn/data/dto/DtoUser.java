package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DtoUser {
    private String username;
    private Set<String> roles;
    private boolean admin;

    public static DtoUser fromEntity(EntityUser user) {

        String[] roles = user.getRoles().split(",");
        Set<String> rolesSet = new HashSet<>(Arrays.asList(roles));

        return new DtoUser()
                .setUsername(user.getUsername())
                .setRoles(rolesSet)
                .setAdmin(rolesSet.contains("ADMIN"));
    }

    public String getUsername() {
        return username;
    }

    public DtoUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public DtoUser setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }

    public boolean isAdmin() {
        return admin;
    }

    public DtoUser setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }
}
