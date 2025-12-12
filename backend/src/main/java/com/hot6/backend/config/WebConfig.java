package com.hot6.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${project.upload.path}")
    private String uploadPath; // "/data/uploads"

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")             // 브라우저 URL
                .addResourceLocations("file:" + uploadPath + "/");
        // => /images/xxx → /data/uploads/xxx
    }
}
