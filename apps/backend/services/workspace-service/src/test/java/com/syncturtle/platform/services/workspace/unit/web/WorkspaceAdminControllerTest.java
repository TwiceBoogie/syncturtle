package com.syncturtle.platform.services.workspace.unit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.syncturtle.platform.services.workspace.application.query.WorkspaceQueryHandler;
import com.syncturtle.platform.services.workspace.controllers.client.WorkspaceAdminController;
import com.syncturtle.platform.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.platform.services.workspace.testsupport.factories.CursorPageResponseFactory;
import com.syncturtle.platform.services.workspace.testsupport.factories.WorkspaceResponseFactory;

@WebMvcTest(WorkspaceAdminController.class)
class WorkspaceAdminControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WorkspaceQueryHandler query;

    @Nested
    class WorkspaceAll {

        @Test
        void get_workspaceAll_returnsCursorPage() throws Exception {
            // arrange
            WorkspaceResponse r1 = WorkspaceResponseFactory.aWorkspace();
            WorkspaceResponse r2 = WorkspaceResponseFactory.aWorkspace(UUID.randomUUID(), "Capsule Corp",
                    "capsule-corp");
            when(query.workspaceAll(anyString(), anyInt(), anyString()))
                    .thenReturn(CursorPageResponseFactory.pageWithNext(List.of(r1, r2), 5, "createdBy123"));
            // act + assert
            mvc.perform(get("/api/instances/workspaces/")
                    .param("cursor", "c1")
                    .param("perPage", "5")
                    .param("search", "Luna"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.perPage").value(5))
                    .andExpect(jsonPath("$.nextCursor").value("createdBy123"))
                    .andExpect(jsonPath("$.results[0].slug").value("lunasnow"))
                    .andExpect(jsonPath("$.results[1].slug").value("capsule-corp"));

            // capture
            ArgumentCaptor<String> cursorCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> perPageCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
            // verify
            verify(query).workspaceAll(cursorCaptor.capture(), perPageCaptor.capture(), searchCaptor.capture());
            // assert captured
            assertThat(cursorCaptor.getValue()).isEqualTo("c1");
            assertThat(perPageCaptor.getValue()).isEqualTo(5);
            assertThat(searchCaptor.getValue()).isEqualTo("Luna");
        }

        @Test
        void get_workspaceAll_missingParam_returns200() throws Exception {
            // arrange
            when(query.workspaceAll(isNull(), eq(10), isNull()))
                    .thenReturn(CursorPageResponseFactory.page(List.of(), 10));
            // act + assert
            mvc.perform(get("/api/instances/workspaces/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.perPage").value(10))
                    .andExpect(jsonPath("$.nextCursor").doesNotExist())
                    .andExpect(jsonPath("$.results", hasSize(0)));
        }

    }

}
