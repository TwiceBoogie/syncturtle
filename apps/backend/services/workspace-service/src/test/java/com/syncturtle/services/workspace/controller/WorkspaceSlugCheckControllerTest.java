package com.syncturtle.services.workspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.service.WorkspaceService;

class WorkspaceSlugCheckControllerTest {

    @Test
    void delegatesOnePublicOperationAndReturnsBoundaryResponse() {
        // arrange
        WorkspaceService service = mock(WorkspaceService.class);
        WorkspaceSlugCheckResponse response = WorkspaceSlugCheckResponse.available();
        // conditions
        when(service.workspaceSlugCheck("personal-1234")).thenReturn(response);
        // act
        WorkspaceSlugCheckResponse actual = new WorkspaceController(service).checkSlug("personal-1234");
        // assert
        assertThat(actual).isSameAs(response);
        // verify
        verify(service).workspaceSlugCheck("personal-1234");
        verifyNoMoreInteractions(service);
    }

}
