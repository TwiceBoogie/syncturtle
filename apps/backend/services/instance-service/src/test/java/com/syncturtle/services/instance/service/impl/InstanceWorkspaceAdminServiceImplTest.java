package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.common.web.pagination.DecodedCursor;
import com.syncturtle.services.instance.client.WorkspaceClient;
import com.syncturtle.services.instance.dto.request.InstanceWorkspaceCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceSlugCheckResponse;
import com.syncturtle.services.instance.mapper.InstanceWorkspacePageMapper;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;
import com.syncturtle.services.instance.repository.projection.InstanceIdProjection;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;
import com.syncturtle.services.instance.service.InstanceWorkspaceAdminService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceWorkspaceAdminService")
class InstanceWorkspaceAdminServiceImplTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    WorkspaceRepository workspaceRepository;
    @Mock
    WorkspaceClient workspaceClient;
    @Mock
    CursorCodec cursorCodec;
    @Mock
    InstanceWorkspacePageMapper pageMapper;

    @Mock
    InstanceIdProjection instanceIdProjection;
    @Mock
    InstanceWorkspaceSlugCheckResponse slugCheckResponse;
    @Mock
    InstanceWorkspaceCreateRequest createRequest;
    @Mock
    InstanceWorkspaceResponse workspaceResponse;
    @Mock
    InstanceWorkspaceProjection workspaceProjectionOne;
    @Mock
    InstanceWorkspaceProjection workspaceProjectionTwo;
    @Mock
    CursorPageResponse<InstanceWorkspaceResponse> pageResponse;

    private InstanceWorkspaceAdminService service;

    @BeforeEach
    void setup() {
        service = new InstanceWorkspaceAdminServiceImpl(instanceRepository, workspaceRepository, workspaceClient,
                cursorCodec, pageMapper);
    }

    @Nested
    @DisplayName("checkSlug(UUID, String)")
    class CheckSlugTests {

        @Test
        @DisplayName("rejects null current user id before hitting dependencies")
        void rejectsNullCurrentUserIdBeforeHittingDependencies() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.checkSlug(null, "demo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentUserId is required");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("rejects blank slug before hitting dependencies")
        void rejectsBlankSlugBeforeHittingDependencies() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.checkSlug(currentUserId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("slug is required");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("throws when instance has not been initialized")
        void throwsWhenInstanceHasNotBeenInitialized() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.checkSlug(currentUserId, "demo"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Instance has not been initialized.");
            // verify
            verifyNoInteractions(workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("checks slug againts workspace service using current instance id")
        void checksSlugAgaintsWorkspaceServiceUsingCurrentInstanceId() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            UUID instanceId = UUID.randomUUID();
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(workspaceClient.checkSlug(instanceId, "demo")).thenReturn(slugCheckResponse);
            // act
            InstanceWorkspaceSlugCheckResponse actual = service.checkSlug(currentUserId, "demo");
            // assert
            assertThat(actual).isSameAs(slugCheckResponse);
            // verify
            verify(workspaceClient).checkSlug(instanceId, "demo");
            verifyNoInteractions(workspaceRepository, cursorCodec, pageMapper);
        }

    }

    @Nested
    @DisplayName("getWorkspaces(UUID, String, int, String)")
    class GetWorkspacesTests {

        @Test
        @DisplayName("rejects null current user id before hitting dependencies")
        void rejectsNullCurrentUserIdBeforeHittingDependencies() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.getWorkspaces(null, null, 25, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentUserId is required");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("rejects non-positive perPage before hitting dependencies")
        void rejectsNonPositivePerPageBeforeHittingDependencies() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.getWorkspaces(currentUserId, null, 0, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("perPage must be greater than 0");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("rejects perPage over maximum before hitting dependencies")
        void rejectsPerPageOverMaximumBeforeHittingDependencies() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.getWorkspaces(currentUserId, null, 101, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("perPage must be less than or equal to 100");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("loads first page with normalized search pattern and limit plus one")
        void loadsFirstPageWithNormalizedSearchPatternAndLimitPlusOne() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            UUID instanceId = UUID.randomUUID();

            List<InstanceWorkspaceProjection> projections = List.of(workspaceProjectionOne, workspaceProjectionTwo);
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(cursorCodec.decode(null)).thenReturn(null);
            when(workspaceRepository.findWorkspacePageDesc("%demo%", null, null, 26)).thenReturn(projections);
            when(pageMapper.mapToPageResponse(same(projections), eq(25))).thenReturn(pageResponse);
            // act
            CursorPageResponse<InstanceWorkspaceResponse> actual = service.getWorkspaces(currentUserId, null, 25,
                    " Demo ");
            // assert
            assertThat(actual).isSameAs(pageResponse);
            // verify
            verify(cursorCodec).decode(null);
            verify(workspaceRepository).findWorkspacePageDesc("%demo%", null, null, 26);
            verify(pageMapper).mapToPageResponse(same(projections), eq(25));
            verifyNoInteractions(workspaceClient);
        }

        @Test
        @DisplayName("loads next page using decoded cursor position")
        void loadsNextPageUsingDecodedCursorPosition() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            UUID instanceId = UUID.randomUUID();
            UUID cursorId = UUID.randomUUID();
            Instant cursorCreatedAt = Instant.parse("2026-01-01T00:00:00Z");

            DecodedCursor decodedCursor = new DecodedCursor(cursorId, cursorCreatedAt);
            List<InstanceWorkspaceProjection> projections = List.of(workspaceProjectionOne);
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(cursorCodec.decode("cursor-token")).thenReturn(decodedCursor);
            when(workspaceRepository.findWorkspacePageDesc(isNull(), eq(cursorCreatedAt), eq(cursorId), eq(11)))
                    .thenReturn(projections);
            when(pageMapper.mapToPageResponse(same(projections), eq(10))).thenReturn(pageResponse);
            // act
            CursorPageResponse<InstanceWorkspaceResponse> actual = service.getWorkspaces(currentUserId, "cursor-token",
                    10, " ");
            // assert
            assertThat(actual).isSameAs(pageResponse);
            // verify
            verify(cursorCodec).decode(eq("cursor-token"));
            verify(workspaceRepository).findWorkspacePageDesc(isNull(), eq(cursorCreatedAt), eq(cursorId), eq(11));
            verify(pageMapper).mapToPageResponse(same(projections), eq(10));
        }

    }

    @Nested
    @DisplayName("createWorkspace(UUID, InstanceWorkspaceCreateRequest)")
    class CreateWorkspaceTests {

        @Test
        @DisplayName("rejects null current user id before hitting dependencies")
        void rejectsNullCurrentUserIdBeforeHittingDependencies() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.createWorkspace(null, createRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentUserId is required");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("rejects null request before hitting dependencies")
        void rejectsNullRequestBeforeHittingDependencies() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.createWorkspace(currentUserId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("workspace create request is required");
            // verify
            verifyNoInteractions(instanceRepository, workspaceRepository, workspaceClient, cursorCodec, pageMapper);
        }

        @Test
        @DisplayName("creates workspace through workspace service using current instance id")
        void createsWorkspaceThroughWorkspaceServiceUsingCurrentInstanceId() {
            // arrange
            UUID currentUserId = UUID.randomUUID();
            UUID instanceId = UUID.randomUUID();
            // conditions
            when(instanceIdProjection.getId()).thenReturn(instanceId);
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class))
                    .thenReturn(Optional.of(instanceIdProjection));
            when(workspaceClient.createWorkspace(instanceId, createRequest)).thenReturn(workspaceResponse);
            // act
            InstanceWorkspaceResponse actual = service.createWorkspace(currentUserId, createRequest);
            // assert
            assertThat(actual).isSameAs(workspaceResponse);
            // verify
            verify(workspaceClient).createWorkspace(instanceId, createRequest);
            verifyNoInteractions(workspaceRepository, cursorCodec, pageMapper);
        }

    }

}
