package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.net.URI;

@Configuration
public class LambdaConfig {

    @Value("${aws.endpoint-override:#{null}}")
    private String endpointOverride;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Bean
    public LambdaClient lambdaClient() {
        var builder = LambdaClient.builder()
                .region(Region.of(region));

        if (endpointOverride != null && !endpointOverride.isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride))
                   // For LocalStack, we can use dummy credentials
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("test", "test")
                   ));
        }

        return builder.build();
    }
}
