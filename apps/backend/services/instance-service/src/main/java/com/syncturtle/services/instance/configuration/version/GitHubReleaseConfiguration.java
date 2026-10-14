package com.syncturtle.services.instance.configuration.version;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.syncturtle.services.instance.client.version.GitHubReleaseClient;

@Profile("!setup")
@Configuration(proxyBeanMethods = false)
public class GitHubReleaseConfiguration {

    @Bean
    GitHubReleaseClient gitHubReleaseClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        return new GitHubReleaseClient(client);
    }

}
