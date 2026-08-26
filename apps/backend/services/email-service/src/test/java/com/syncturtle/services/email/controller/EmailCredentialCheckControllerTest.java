package com.syncturtle.services.email.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.syncturtle.services.email.service.EmailCredentialCheckService;

@WebMvcTest(controllers = EmailCredentialCheckController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EmailCredentialCheckController")
class EmailCredentialCheckControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailCredentialCheckService service;

    @Nested
    @DisplayName("checkCredentials(EmailCredentialCheckRequest)")
    class CheckCredentialsTests {

        @Test
        @DisplayName("delegates a valid receiver and returns the stable success response")
        void delegatesValidReceiverAndReturnsSuccess() throws Exception {
            mvc.perform(post("/api/instances/email-credentials-check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content("""
                            {"receiverEmail":"lunasnow@marvel.com"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email successfully sent."));

            verify(service).sendTestEmail("lunasnow@marvel.com");
        }

        @Test
        @DisplayName("rejects an invalid receiver before invoking the service")
        void rejectsInvalidReceiver() throws Exception {
            mvc.perform(post("/api/instances/email-credentials-check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"receiverEmail":"not-an-email"}
                            """))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("rejects a missing receiver before invoking the service")
        void rejectsMissingReceiver() throws Exception {
            mvc.perform(post("/api/instances/email-credentials-check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

    }

}
