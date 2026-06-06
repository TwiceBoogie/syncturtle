package com.syncturtle.services.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalInstanceWorkspaceCreateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Limit your name to 80 characters.")
    String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 48, message = "Limit your URL to 48 characters.")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Use lowercase letters, numbers, and hyphens only.")
    String slug;

    @NotBlank(message = "Organization size is required")
    String organizationSize;

    String companyRole;
}
