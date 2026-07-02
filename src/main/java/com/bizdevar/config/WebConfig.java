package com.bizdevar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(System.getProperty("user.dir"));
        if (base.getFileName() != null && "backend".equalsIgnoreCase(base.getFileName().toString())) {
            base = base.getParent();
        }
        Path uploads = base.resolve("uploads");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploads.toUri().toString().endsWith("/")
                        ? uploads.toUri().toString()
                        : uploads.toUri().toString() + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = props.getCors().getOrigins().toArray(new String[0]);
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
