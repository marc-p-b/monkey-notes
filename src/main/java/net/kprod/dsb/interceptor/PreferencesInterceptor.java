package net.kprod.dsb.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class PreferencesInterceptor implements HandlerInterceptor {

    @Autowired
    private PreferencesService preferencesService;

    @Autowired
    private DriveService driveService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Optional<String> optAuthUrl = driveService.requireAuth();
        if (optAuthUrl.isPresent()) {
            response.sendRedirect("/preferences");
        }

        if(preferencesService.isParametersNotSet()) {
            response.sendRedirect("/preferences");
        }

        return true;
    }
}