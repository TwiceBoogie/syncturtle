package com.syncturtle.services.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public final class WorkspaceCreateRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Limit your name to 80 characters.")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 48, message = "Limit your URL to 48 characters.")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Use lowercase letters, numbers, and hyphens only.")
    private String slug;

    @NotBlank(message = "Organization size is required")
    private String organizationSize;

    private String companyRole;
}
