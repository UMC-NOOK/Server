package app.nook.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(R2Properties r2) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(r2.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKey(), r2.secretKey())
                ))
                .build();
    }

    @Bean
    public S3Client s3Client(R2Properties r2) {
        return S3Client.builder()
                .endpointOverride(URI.create(r2.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKey(), r2.secretKey())
                ))
                .build();
    }
}
