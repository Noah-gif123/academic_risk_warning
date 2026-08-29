package com.example.academic_risk_warning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** Token 有效期（小时） */
    private int tokenExpireHours = 24;
}
