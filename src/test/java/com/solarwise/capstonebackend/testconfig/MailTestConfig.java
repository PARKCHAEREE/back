package com.solarwise.capstonebackend.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Test configuration to provide a JavaMailSender bean so Spring Test ApplicationContext can start.
 * This is a lightweight local stub (does not actually send emails during tests).
 */
@TestConfiguration
public class MailTestConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        // Minimal local settings to satisfy auto-wiring in tests. No real SMTP required for unit tests.
        impl.setHost("localhost");
        impl.setPort(25);
        return impl;
    }
}

