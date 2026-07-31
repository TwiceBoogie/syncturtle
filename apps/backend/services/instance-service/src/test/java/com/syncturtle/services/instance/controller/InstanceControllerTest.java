package com.syncturtle.services.instance.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.services.instance.dto.request.InstanceUpdateRequest;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.support.JsonContent;
import com.syncturtle.services.instance.support.fixture.ResponseFixtures;

@WebMvcTest(controllers = InstanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InstanceController")
class InstanceControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private InstanceService service;

    @Nested
    @DisplayName("GET " + EndpointPaths.API_INSTANCES)
    class GetInstanceSetupInfo {

        @Test
        @DisplayName("returns public instance setup info")
        void returnsPublicInstanceSetupInfo() throws Exception {
            // arrange
            // conditions
            when(service.getPublicInstance()).thenReturn(ResponseFixtures.inactiveSetupResponse());
            // act
            mvc.perform(get(EndpointPaths.API_INSTANCES).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
            // assert
            // verify
            verify(service).getPublicInstance();
        }

    }

    @Nested
    @DisplayName("PATCH " + EndpointPaths.API_INSTANCES)
    class UpdateInstance {

        @Test
        @DisplayName("accepts a valid partial update")
        void acceptsValidPartialUpdate() throws Exception {
            // arrange
            // conditions
            when(service.instanceUpdate(any(InstanceUpdateRequest.class)))
                    .thenReturn(ResponseFixtures.instanceResponse("New Name", 2L));
            // act
            mvc.perform(patch(EndpointPaths.API_INSTANCES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(JsonContent.instanceUpdate("New Name", null)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
            // assert
            // verify
            verify(service).instanceUpdate(argThat(request -> "New Name".equals(request.getInstanceName())
                    && request.getTelemetryEnabled() == null));
        }

        @Test
        @DisplayName("rejects empty update object using DTO validation")
        void rejectsEmtpyUpdateObjectUsingDtoValidation() throws Exception {
            // arrange
            // conditions
            // act
            mvc.perform(patch(EndpointPaths.API_INSTANCES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
            // assert
            // verify
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("rejects blank instanceName using DTO validation")
        void rejectsBlankInstanceNameUsingDtoValidation() throws Exception {
            // arrange
            // conditions
            // act
            mvc.perform(patch(EndpointPaths.API_INSTANCES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(JsonContent.instanceUpdate(" ", null)))
                    .andExpect(status().isBadRequest());
            // assert
            // verify
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("rejects malformed JSON before service layer")
        void rejectsMalformedJsonBeforeServiceLayer() throws Exception {
            // arrange
            // conditions
            // act
            mvc.perform(patch(EndpointPaths.API_INSTANCES)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(JsonContent.object("\"instanceName\": ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("")));
            // assert
            // verify
            verifyNoInteractions(service);
        }

    }

}
