package com.syncturtle.services.workspace.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalInstanceWorkspaceResponse {

    @NotBlank(message = "Workspace name is required.")
    @Size(max = 80, message = "Workspace name must be 80 characters or fewer.")
    String name;

    @NotBlank(message = "Workspace slug is required.")
    @Size(max = 48, message = "Workspace slug must be 48 characters or fewer.")
    String slug;

    @NotBlank(message = "Organization size is required.")
    @Size(max = 20, message = "Organization size must be 20 characters or fewer.")
    String organizationSize;

    @Size(max = 80, message = "Company role must be 80 characters or fewer.")
    String companyName;

    String timezone;

}
