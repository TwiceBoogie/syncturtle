package com.syncturtle.services.instance.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("Instance workspace slug-check response JSON")
class InstanceWorkspaceSlugCheckResponseJsonTest {

    @Test
    @DisplayName("decodes an available slug response from the workspace service")
    void decodesAvailableSlugResponse() throws Exception {
        // arrange
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        String payload = "{\"available\":true,\"message\":null}";

        // act
        InstanceWorkspaceSlugCheckResponse response = jsonMapper.readValue(
                payload, InstanceWorkspaceSlugCheckResponse.class);

        // assert
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    @DisplayName("decodes an unavailable slug response from the workspace service")
    void decodesUnavailableSlugResponse() throws Exception {
        // arrange
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
        String payload = "{\"available\":false,\"message\":\"Slug unavailable\"}";

        // act
        InstanceWorkspaceSlugCheckResponse response = jsonMapper.readValue(
                payload, InstanceWorkspaceSlugCheckResponse.class);

        // assert
        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Slug unavailable");
    }
}
