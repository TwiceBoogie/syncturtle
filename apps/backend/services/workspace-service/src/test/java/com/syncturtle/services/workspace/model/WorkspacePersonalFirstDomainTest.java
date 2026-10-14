package com.syncturtle.services.workspace.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;

class WorkspacePersonalFirstDomainTest {

    @Test
    void optionalLegacyMetadataIsNormalizedAndBounded() {
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Workspace blank = Workspace.create(
                WorkspaceCreateParam.builder()
                        .name("Personal")
                        .slug("personal-1234")
                        .ownerId(owner)
                        .organizationSize("  ")
                        .build());
        assertThat(blank.getOrganizationSize()).isNull();
        Workspace legacy = Workspace.create(WorkspaceCreateParam.builder().name("Family").slug("family-1234")
                .ownerId(owner).organizationSize(" 2-10 ").build());
        assertThat(legacy.getOrganizationSize()).isEqualTo("2-10");
        assertThatThrownBy(() -> WorkspaceCreateParam.builder()
                .name("Personal")
                .slug("personal-1234")
                .ownerId(owner)
                .organizationSize("x".repeat(21))
                .build()).isInstanceOf(IllegalArgumentException.class);
    }

}
