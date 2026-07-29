package com.syncturtle.services.workspace.support.factories;
// package com.syncturtle.services.workspace.testsupport.factories;

// import java.time.Instant;
// import java.util.UUID;

// import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;

// public final class WorkspaceResponseFactory {

// private WorkspaceResponseFactory() {
// throw new AssertionError("no Instance.");
// }

// public static WorkspaceResponse aWorkspace() {
// WorkspaceResponse workspace = new WorkspaceResponse();
// workspace.setId(UUID.randomUUID());
// workspace.setName("Marvel");
// workspace.setLogo(null);
// workspace.setLogoAssetId(null);
// workspace.setSlug("lunasnow");
// workspace.setOrganizationSize("just myself");
// workspace.setOwner(UserResponseFactory.aUser());
// workspace.setCreatedAt(Instant.parse("2026-02-10T00:00:00Z"));
// workspace.setUpdatedAt(Instant.parse("2026-02-10T00:00:00Z"));
// workspace.setCreatedById(UUID.randomUUID());
// workspace.setUpdatedById(UUID.randomUUID());
// return workspace;
// }

// public static WorkspaceResponse aWorkspace(UUID id, String name, String slug)
// {
// WorkspaceResponse workspace = aWorkspace();
// workspace.setId(id);
// workspace.setName(name);
// workspace.setSlug(slug);
// return workspace;
// }

// public static WorkspaceResponse aWorkspaceNoOwner() {
// WorkspaceResponse workspace = aWorkspace();
// workspace.setOwner(null);
// return workspace;
// }

// }
