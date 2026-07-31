package com.syncturtle.services.instance.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.service.InstanceConfigurationService;

@WebMvcTest(controllers = InstanceConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InstanceConfigurationController")
class InstanceConfigurationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InstanceConfigurationService service;

    @Nested
    @DisplayName("PATCH " + EndpointPaths.API_INSTANCES + EndpointPaths.CONFIGURATIONS)
    class UpdateConfigurations {

        @Test
        @DisplayName("accepts a key-value map and returns only the changed persisted rows as a list")
        void preservesChangedRowListContract() throws Exception {
            InstanceConfigurationResponse changed = InstanceConfigurationResponse.builder()
                    .key(InstanceConfigurationKey.ENABLE_SIGNUP)
                    .value("0")
                    .build();
            when(service.configurationsUpdate(argThat(request -> request.size() == 2
                    && "0".equals(request.get(InstanceConfigurationKey.ENABLE_SIGNUP))
                    && "unchanged".equals(request.get(InstanceConfigurationKey.GITHUB_APP_NAME)))))
                    .thenReturn(List.of(changed));

            mvc.perform(patch(EndpointPaths.API_INSTANCES + EndpointPaths.CONFIGURATIONS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "ENABLE_SIGNUP": "0",
                              "GITHUB_APP_NAME": "unchanged"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].key").value("ENABLE_SIGNUP"))
                    .andExpect(jsonPath("$[0].value").value("0"));

            verify(service).configurationsUpdate(argThat(request -> request.size() == 2
                    && request.containsKey(InstanceConfigurationKey.ENABLE_SIGNUP)
                    && request.containsKey(InstanceConfigurationKey.GITHUB_APP_NAME)));
        }

        @Test
        @DisplayName("returns an empty JSON list when no effective row changed")
        void returnsEmptyListForNoEffectiveChange() throws Exception {
            when(service.configurationsUpdate(
                    argThat(request -> "1".equals(request.get(InstanceConfigurationKey.ENABLE_SIGNUP)))))
                    .thenReturn(List.of());

            mvc.perform(patch(EndpointPaths.API_INSTANCES + EndpointPaths.CONFIGURATIONS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content("""
                            {"ENABLE_SIGNUP": "1"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

    }

}
