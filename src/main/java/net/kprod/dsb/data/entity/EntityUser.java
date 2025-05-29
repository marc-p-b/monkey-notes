package net.kprod.dsb.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.ZonedDateTime;

@Entity(name="users")
public class EntityUser {

    @Id
    private String username;

    private String password;

    private String otp;

    private ZonedDateTime otpExpiresAt;

    private String roles;

    private ZonedDateTime lastNrpUsage;


    public String getUsername() {
        return username;
    }

    public EntityUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public EntityUser setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getOtp() {
        return otp;
    }

    public EntityUser setOtp(String otp) {
        this.otp = otp;
        return this;
    }

    public ZonedDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public EntityUser setOtpExpiresAt(ZonedDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
        return this;
    }

    public String getRoles() {
        return roles;
    }

    public EntityUser setRoles(String roles) {
        this.roles = roles;
        return this;
    }

    public ZonedDateTime getLastNrpUsage() {
        return lastNrpUsage;
    }

    public EntityUser setLastNrpUsage(ZonedDateTime lastNrpUsage) {
        this.lastNrpUsage = lastNrpUsage;
        return this;
    }
}
