package com.syncturtle.services.file.configuration.s3;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.syncturtle.services.file.configuration.property.StorageProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
public class S3ClientConfiguration {

    @Bean
    AwsCredentialsProvider awsCredentialsProvider(StorageProperties properties) {
        Assert.notNull(properties, "storage properties is required");

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }

    @Bean
    S3Configuration s3Configuration(StorageProperties properties) {
        Assert.notNull(properties, "storage properties are required");

        return S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isUsePathStyle())
                .build();
    }

    @Bean
    S3Client s3Client(StorageProperties properties, AwsCredentialsProvider credentialsProvider,
            S3Configuration s3Configuration) {
        Assert.notNull(properties, "storage properties are required");
        Assert.notNull(credentialsProvider, "credentials provider is required");
        Assert.notNull(s3Configuration, "s3 configuration is required");

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);

        applyEndpoint(builder, properties.getEndpoint());

        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(StorageProperties properties, AwsCredentialsProvider credentialsProvider,
            S3Configuration s3Configuration) {
        Assert.notNull(properties, "storage properties are required");
        Assert.notNull(credentialsProvider, "credentials provider is required");
        Assert.notNull(s3Configuration, "s3 configuration is required");

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration);

        applyEndpoint(builder, properties.getPresignEndpoint());

        return builder.build();
    }

    private static void applyEndpoint(S3ClientBuilder builder, URI endpoint) {
        Assert.notNull(builder, "S3 client builder is required");

        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
    }

    private static void applyEndpoint(S3Presigner.Builder builder, URI endpoint) {
        Assert.notNull(builder, "S3 presigner builder is required");

        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
    }

}
