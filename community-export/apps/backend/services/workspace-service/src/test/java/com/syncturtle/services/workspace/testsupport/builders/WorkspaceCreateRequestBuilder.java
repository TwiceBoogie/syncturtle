package com.syncturtle.services.workspace.testsupport.builders;

import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true)
@NoArgsConstructor(staticName = "aWorkspaceCreateRequest")
public final class WorkspaceCreateRequestBuilder {
    private String name = "Marvel";
    private String slug = "LunaSnow";
    private String organizationSize = "just myself";
    private String companyRole = "";

    public WorkspaceCreateRequest build() {
        WorkspaceCreateRequest request = new WorkspaceCreateRequest();
        request.setName(name);
        request.setSlug(slug);
        request.setOrganizationSize(organizationSize);
        request.setCompanyRole(companyRole);
        return request;
    }
}
