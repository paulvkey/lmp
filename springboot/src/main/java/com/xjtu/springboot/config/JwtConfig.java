package com.xjtu.springboot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    // 密钥（必须保密，生产环境用环境变量注入）
    private String secretKey;
    // Token过期时间（单位：分钟）
    private long expiration;
}
