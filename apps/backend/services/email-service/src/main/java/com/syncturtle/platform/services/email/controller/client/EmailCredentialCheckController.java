package com.syncturtle.platform.services.email.controller.client;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.platform.services.email.dto.request.EmailCredentialCheckRequest;
import com.syncturtle.platform.services.email.dto.response.EmailCredentialCheckResponse;
import com.syncturtle.platform.services.email.service.EmailCredentialCheckService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class EmailCredentialCheckController {

    private final EmailCredentialCheckService service;

    @PostMapping("/email-credentials-check")
    public ResponseEntity<EmailCredentialCheckResponse> checkCredentials(
            @Valid @RequestBody EmailCredentialCheckRequest request) {
        service.sendTestEmail(request.getReceiverEmail());
        return ResponseEntity.ok(new EmailCredentialCheckResponse("Email successfully sent."));
    }

}
