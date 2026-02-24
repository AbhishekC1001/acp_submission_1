package com.example.acp_submission_1.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${acp.s3-endpoint}")
    private String s3Endpoint;

    @Value("${acp.dynamodb-endpoint}")
    private String dynamoDbEndpoint;

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test"));
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials())
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials())
                .build();
    }
}