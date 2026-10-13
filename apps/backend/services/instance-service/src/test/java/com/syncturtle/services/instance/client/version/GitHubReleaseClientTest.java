package com.syncturtle.services.instance.client.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@DisplayName("GitHub latest release client")
class GitHubReleaseClientTest {

    private static final String URL =
            "https://api.github.com/repos/TwiceBoogie/syncturtle/releases/latest";

    @Nested
    class LatestTag {

        @Test
        void readsThePublishedTagWithOnlyGenericRequestMetadata() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            String response = """
                    {"tag_name":"v1.2.3"}
                    """;
            server.expect(requestTo(URL))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("User-Agent", "SyncTurtle-version-check"))
                    .andExpect(request -> assertThat(request.getHeaders().getFirst("Authorization")).isNull())
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            assertThat(new GitHubReleaseClient(builder.build()).latestTag()).isEqualTo("v1.2.3");
            server.verify();
        }

        @Test
        void mapsNoRelease404ToAbsentTag() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThat(new GitHubReleaseClient(builder.build()).latestTag()).isNull();
            server.verify();
        }

        @Test
        void keepsServerErrorsDistinctFromNoRelease() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL)).andRespond(withServerError());

            assertThatThrownBy(() -> new GitHubReleaseClient(builder.build()).latestTag())
                    .isInstanceOf(RestClientException.class);
            server.verify();
        }

        @Test
        void keepsRateLimitDistinctFromNoRelease() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> new GitHubReleaseClient(builder.build()).latestTag())
                    .isInstanceOf(RestClientException.class);
            server.verify();
        }

        @Test
        void rejectsInvalidJson() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL))
                    .andRespond(withSuccess("{broken", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> new GitHubReleaseClient(builder.build()).latestTag())
                    .isInstanceOf(RestClientException.class);
            server.verify();
        }

        @Test
        void rejectsMissingTag() {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo(URL))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> new GitHubReleaseClient(builder.build()).latestTag())
                    .isInstanceOf(IllegalStateException.class);
            server.verify();
        }
    }
}
