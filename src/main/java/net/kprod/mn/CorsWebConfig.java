package net.kprod.mn;

import org.junit.jupiter.api.Order;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsWebConfig implements WebMvcConfigurer {


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // allow CORS for all endpoints
                //.allowedOrigins("http://localhost:8080") // your Vue dev server
                .allowedOrigins("https://ohdbcfzqnn.a.pinggy.link")
                .allowedOrigins("http://localhost:5173") // your Vue dev server
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // allowed HTTP methods
                .allowedHeaders("*") // allow all headers
                .allowCredentials(true); // allow cookies if needed
    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**");
//    }


}
