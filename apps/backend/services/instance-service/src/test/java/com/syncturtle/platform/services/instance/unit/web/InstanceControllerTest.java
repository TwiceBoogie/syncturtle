package com.syncturtle.platform.services.instance.unit.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.syncturtle.platform.services.instance.application.query.InstanceQueryHandler;
import com.syncturtle.platform.services.instance.controllers.client.InstanceController;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.utils.validation.AuthFormValidator;

@ExtendWith(MockitoExtension.class)
public class InstanceControllerTest {

    @Mock
    InstanceQueryHandler query;
    @Mock
    AuthFormValidator authValidator;

    @InjectMocks
    InstanceController controller;

    @Test
    void instanceAdminSignup_shouldRedirectWithCustomHeaders() throws Exception {
        // arrange
        UUID userId = UUID.randomUUID();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        // conditions
        when(query.instanceAdminSignup(any()))
                .thenReturn(new InstanceAdminSignupResult(
                        userId,
                        1L,
                        1L,
                        UUID.randomUUID(),
                        "INSTANCE_ADMIN",
                        "https://admin.syncturtle.com/god-mode/general/"));
        // act
        mvc.perform(post("/api/instances/admins/sign-up")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("firstName", "Sal")
                .param("lastName", "Sebastian")
                .param("email", "admin@example.com")
                .param("password", "VeryStrongPassword!2026#OK")
                .param("companyName", "SyncTurtle")
                .param("telemetryEnabled", "true"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://admin.syncturtle.com/god-mode/general/"));
    }

}
