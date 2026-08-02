package app.nook.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record R2Properties(
        String endpoint,
        String bucketName,
        String accessKey,
        String secretKey,
        String cdnBaseUrl
) {}
