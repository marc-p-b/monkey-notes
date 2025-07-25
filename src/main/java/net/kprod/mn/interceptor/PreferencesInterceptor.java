package net.kprod.mn.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.kprod.mn.service.DriveService;
import net.kprod.mn.service.PreferencesService;
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
        if (optAuthUrl.isPresent() || preferencesService.isParametersNotSet()) {
            response.sendRedirect("/v/preferences");
        }
        return true;
    }
}