package com.bizdevar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailConfig {
    // JavaMailSender auto-configured by Spring Boot when spring.mail.host is set
}
