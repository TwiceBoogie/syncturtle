package com.syncturtle.services.instance.client.version;

import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

public final class GitHubReleaseClient {

    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/TwiceBoogie/syncturtle/releases/latest";

    private final RestClient restClient;

    public GitHubReleaseClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // A 404 means the public repository has no published latest release.
    public String latestTag() {
        try {
            ReleasePayload payload = restClient.get()
                    .uri(LATEST_RELEASE_URL)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "SyncTurtle-version-check")
                    .retrieve()
                    .body(ReleasePayload.class);

            if (payload == null || payload.getTagName() == null) {
                throw new IllegalStateException("GitHub latest release response has no tag_name");
            }

            return payload.getTagName();
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        }
    }

    @Getter
    @Setter
    public static final class ReleasePayload {
        @JsonProperty("tag_name")
        private String tagName;
    }

}
