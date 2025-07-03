package net.kprod.dsb;

import net.kprod.dsb.interceptor.PreferencesInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private PreferencesInterceptor preferencesInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(preferencesInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v/preferences","/preferences/**", "/login", "/logout", "/error", "/static/**", "/grant-callback", "/notify", "/image/**", "/imagetemp/**");
    }
}
