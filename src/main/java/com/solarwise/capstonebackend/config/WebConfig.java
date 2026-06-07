package com.solarwise.capstonebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 💡 최종 수정: /images/** 요청을 classpath:/images/ 디렉토리와 매핑
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/images/");
    }
}
