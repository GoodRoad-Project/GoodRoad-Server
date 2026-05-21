package goodroad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
@Configuration
public class S3Config {

    @Value("${yandex.storage.endpoint}")
    private String endpoint;

    @Value("${yandex.storage.region}")
    private String region;

    @Value("${yandex.storage.access-key}")
    private String accessKey;

    @Value("${yandex.storage.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
    }
}