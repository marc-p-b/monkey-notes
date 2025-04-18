package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Value("${app.experimental-user}")
    private String exp_user;

    @Override
    public String getConnectedUsername() {
        return exp_user;
    }
}
