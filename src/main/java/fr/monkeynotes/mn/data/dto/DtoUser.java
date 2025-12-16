package fr.monkeynotes.mn.data.dto;

import fr.monkeynotes.mn.data.entity.EntityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DtoUser {
    private String username;
    private String email;
    private Set<String> roles;
    private boolean admin;

    public static DtoUser fromEntity(EntityUser user) {
        String[] roles = user.getRoles().split(",");
        Set<String> rolesSet = new HashSet<>(Arrays.asList(roles));

        return new DtoUser()
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setRoles(rolesSet)
                .setAdmin(rolesSet.contains("ADMIN"));
    }

    public String getEmail() {
        return email;
    }

    public DtoUser setEmail(String email) {
        this.email = email;
        return this;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DtoUser dtoUser = (DtoUser) o;
        return Objects.equals(username, dtoUser.username) && Objects.equals(email, dtoUser.email) && Objects.equals(roles, dtoUser.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, email, roles);
    }

    @Override
    public String toString() {
        return "DtoUser{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                '}';
    }
}
