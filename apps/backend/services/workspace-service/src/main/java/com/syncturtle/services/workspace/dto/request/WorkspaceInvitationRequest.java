package com.syncturtle.services.workspace.dto.request;

import java.util.List;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class WorkspaceInvitationRequest {

    @Valid
    @NotEmpty(message = "Email list cannot be empty")
    List<EmailRoleRequest> emails;

    @Value
    @Builder
    @Jacksonized
    public static class EmailRoleRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email;

        @NotNull(message = "Role is required")
        WorkspaceRole role;
    }

}
