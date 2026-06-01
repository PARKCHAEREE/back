package com.solarwise.capstonebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 클래스
 * - CORS 설정
 * - RestTemplate 설정 (AI 서버와의 HTTP 통신)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 로컬 시연용으로 제한된 출처만 허용합니다 (Credentials=true 사용 시 '*' 불가)
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * AI 서버와의 비동기 통신을 위한 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        // timeout 값을 application.properties에서 주입할 수 있도록 기본값을 설정합니다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5s
        factory.setReadTimeout(10000); // 10s
        return new RestTemplate(factory);
    }

}

