package app.nook.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record R2Properties(
        String endpoint,
        String publicBucketName,
        String privateBucketName,
        String accessKey,
        String secretKey,
        String cdnBaseUrl
) {
    // accessKey/secretKey 마스킹 (자격증명 노출 방지)
    @Override
    public String toString() {
        return "R2Properties[endpoint=" + endpoint
                + ", publicBucketName=" + publicBucketName
                + ", privateBucketName=" + privateBucketName
                + ", accessKey=****"
                + ", secretKey=****"
                + ", cdnBaseUrl=" + cdnBaseUrl + "]";
    }
}
