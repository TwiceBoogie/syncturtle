package com.syncturtle.services.workspace.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
// import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DataIntegrityViolationException;

import com.syncturtle.common.contracts.workspace.validation.WorkspaceSlugs;
import com.syncturtle.common.core.exceptions.SlugAlreadyExistsException;
import com.syncturtle.common.spring.mapping.CursorCodec;
import com.syncturtle.common.spring.mapping.CursorCodec.DecodedCursor;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.models.Workspace;
import com.syncturtle.services.workspace.models.WorkspaceMember;
import com.syncturtle.services.workspace.repositories.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repositories.WorkspaceRepository;
import com.syncturtle.services.workspace.repositories.projections.WorkspaceProjection;
import com.syncturtle.services.workspace.repositories.query.WorkspaceCursor;
import com.syncturtle.services.workspace.services.impl.WorkspaceServiceImpl;
import com.syncturtle.services.workspace.testsupport.builders.WorkspaceCreateRequestBuilder;
import com.syncturtle.services.workspace.testsupport.stubs.WorkspaceProjectionStub;

@ExtendWith(MockitoExtension.class)
class WorkspaceAdminServiceTest {

    @Mock
    WorkspaceRepository workspaceRepository;
    @Mock
    WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    RequestUserContext userContext;
    @Mock
    CursorCodec cursorCodec;

    @InjectMocks
    WorkspaceServiceImpl service;

    @Nested
    class WorkspaceAll {

        @Test
        void whenCursorIsNull_returnEmptyList() {
            // arrange
            when(cursorCodec.decode(null)).thenReturn(null);
            when(workspaceRepository.findWorkspacePageDesc(null, null, 6)).thenReturn(List.of());
            // act
            List<WorkspaceProjection> result = service.workspaceAll(null, 5, null);
            // assert
            assertThat(result).isEmpty();
            // verify
            verify(workspaceRepository).findWorkspacePageDesc(null, null, 6);
        }

        @Test
        void returnsListofWorkspaceProjection_usingProjectionStub() {
            // arrange
            WorkspaceProjection p1 = WorkspaceProjectionStub.builder().name("Marvel").slug("lunasnow").build();
            WorkspaceProjection p2 = WorkspaceProjectionStub.builder().name("Capsule Corp").slug("capsule-corp")
                    .build();

            when(cursorCodec.decode(null)).thenReturn(null);
            when(workspaceRepository.findWorkspacePageDesc(null, null, 6)).thenReturn(List.of(p1, p2));
            // act
            List<WorkspaceProjection> result = service.workspaceAll(null, 5, null);
            // assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSlug()).isEqualTo("lunasnow");
            assertThat(result.get(1).getName()).isEqualTo("Capsule Corp");
            // verify
            verify(workspaceRepository).findWorkspacePageDesc(null, null, 6);
        }

        @Test
        void happyPath_capturesAllArgs_andVerifiesOrder() {
            // arrange
            String rawCursor = "cursor123";
            Instant createdAt = Instant.parse("2026-02-10T12:00:00Z");
            UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
            DecodedCursor decoded = new DecodedCursor(id, createdAt);
            WorkspaceProjection p1 = WorkspaceProjectionStub.builder().slug("lunasnow").build();

            when(cursorCodec.decode(rawCursor)).thenReturn(decoded);
            when(workspaceRepository.findWorkspacePageDesc(anyString(), any(), eq(6))).thenReturn(List.of(p1));
            // act
            List<WorkspaceProjection> result = service.workspaceAll(rawCursor, 5, "  LunaSnow  ");
            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSlug()).isEqualTo("lunasnow");

            // capture all arguments passed to repository
            ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<WorkspaceCursor> cursorCaptor = ArgumentCaptor.forClass(WorkspaceCursor.class);
            ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);

            // verify call + capture args
            verify(workspaceRepository).findWorkspacePageDesc(patternCaptor.capture(), cursorCaptor.capture(),
                    limitCaptor.capture());
            // assert capture
            assertThat(patternCaptor.getValue()).isEqualTo("%lunasnow%");
            assertThat(limitCaptor.getValue()).isEqualTo(6);

            WorkspaceCursor capturedCursor = cursorCaptor.getValue();
            assertThat(capturedCursor).isNotNull();
            assertThat(capturedCursor.createdAt()).isEqualTo(createdAt);
            assertThat(capturedCursor.id()).isEqualTo(id);

            // verify interaction order
            InOrder order = inOrder(cursorCodec, workspaceRepository);
            order.verify(cursorCodec).decode(rawCursor);
            order.verify(workspaceRepository).findWorkspacePageDesc(anyString(), any(), anyInt());

            verifyNoMoreInteractions(workspaceRepository, cursorCodec);
        }

    }

    @Nested
    class WorkspaceSlugCheck {

        @Test
        void whenSlugIsNull_returnsRequireMessage_andDoesNotHitRepo() {
            // arrange
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck(null);
            // assert
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getReason()).isEqualTo("Workspace slug is required");
            // verify
            verifyNoInteractions(workspaceRepository);
        }

        @ParameterizedTest
        @CsvSource({
                "'  LunaSnow  ', lunasnow",
                "'MarVel', marvel",
                "'  CAPSULE-CORP  ', capsule-corp"
        })
        void normalizesSlug_beforeQueryingRepo(String raw, String expectedNormalized) {
            // arrange
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(expectedNormalized)).thenReturn(false);
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck(raw);
            // assert
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.getReason()).isNull();
            // verify
            ArgumentCaptor<String> slugCaptor = ArgumentCaptor.forClass(String.class);
            verify(workspaceRepository).existsBySlugIgnoreCaseAndDeletedAtIsNull(slugCaptor.capture());
            assertThat(slugCaptor.getValue()).isEqualTo(expectedNormalized);
        }

        @ParameterizedTest
        @CsvSource({
                "ab, 'Slug must be at least 3 characters.'",
                "a, 'Slug must be at least 3 characters.'",
                "'  a  ', 'Slug must be at least 3 characters.'"
        })
        void slugTooShort_returnsMessage_andDoesNotHitRepo(String raw, String expectedError) {
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck(raw);
            // assert
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getReason()).isEqualTo(expectedError);
            // verify
            verifyNoInteractions(workspaceRepository);
        }

        @ParameterizedTest
        @CsvSource({
                "-abc, 'Slug can''t start or end with a hyphen.'",
                "abc-, 'Slug can''t start or end with a hyphen.'",
                "-abc-, 'Slug can''t start or end with a hyphen.'"
        })
        void hyphenEdgeCases_returnMessage_andDoesNotHitRepo(String raw, String expectedError) {
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck(raw);
            // assert
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getReason()).isEqualTo(expectedError);
            // verify
            verifyNoInteractions(workspaceRepository);
        }

        @Test
        void reservedSlug_anyFromRestrictedSet_isRejected() {
            // arrange
            String reserved = WorkspaceSlugs.RESTRICTED.iterator().next();
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck(reserved);
            // assert
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getReason()).isEqualTo("That URL is reserved.");
            // verify
            verifyNoInteractions(workspaceRepository);
        }

        @Test
        void whenSlugAlreadyExists_repoTrue_returnsTakenMessage() {
            // arrange
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull("lunasnow")).thenReturn(true);
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck("LunaSnow");
            // assert
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getReason()).isEqualTo("That URL is taken.");
            // verify
            verify(workspaceRepository).existsBySlugIgnoreCaseAndDeletedAtIsNull("lunasnow");
            verify(workspaceMemberRepository, never()).save(any());
        }

        @Test
        void whenSlugDoesNotExist_returnsOk() {
            // arrange
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull("lunasnow")).thenReturn(false);
            // act
            WorkspaceSlugCheckResponse result = service.workspaceSlugCheck("LunaSnow");
            // assert
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.getReason()).isNull();
            // verify
            verify(workspaceRepository).existsBySlugIgnoreCaseAndDeletedAtIsNull("lunasnow");
        }

    }

    @Nested
    class workspaceCreate {

        @Test
        void happyPath_trimsAndNormalizes_persistsWorkspaceAndMember_thenLoadsProjection() {
            // arrange
            UUID userId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            WorkspaceCreateRequest request = WorkspaceCreateRequestBuilder.aWorkspaceCreateRequest()
                    .name("  Marvel  ")
                    .slug(" LunaSnow  ")
                    .organizationSize(" just myself ")
                    .companyRole("  Avenger  ")
                    .build();
            WorkspaceProjection p1 = WorkspaceProjectionStub.builder()
                    .id(workspaceId)
                    .name("Marvel")
                    .slug("lunasnow")
                    .build();

            when(userContext.getUserId()).thenReturn(userId);
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
            when(workspaceRepository.save(any())).thenAnswer(new Answer<Workspace>() {
                @Override
                public Workspace answer(InvocationOnMock invocation) throws Throwable {
                    Workspace workspace = invocation.getArgument(0);
                    // ReflectionTestUtils.setField(workspace, "id", workspaceId);
                    workspace.setId(workspaceId);
                    return workspace;
                }
            });
            when(workspaceRepository.findWorkspaceById(workspaceId)).thenReturn(Optional.of(p1));
            // act
            WorkspaceProjection result = service.workspaceCreate(request);
            // assert
            assertThat(result.getId()).isEqualTo(workspaceId);
            assertThat(result.getName()).isEqualTo("Marvel");
            assertThat(result.getSlug()).isEqualTo("lunasnow");
            // capture and assert (Workspace)
            ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
            verify(workspaceRepository).save(workspaceCaptor.capture());

            Workspace savedWorkspace = workspaceCaptor.getValue();
            assertThat(savedWorkspace.getName()).isEqualTo("Marvel");
            assertThat(savedWorkspace.getSlug()).isEqualTo("lunasnow");
            assertThat(savedWorkspace.getOrganizationSize()).isEqualTo("just myself");
            // capture and assert (WorkspaceMember)
            ArgumentCaptor<WorkspaceMember> workspaceMemberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
            verify(workspaceMemberRepository).save(workspaceMemberCaptor.capture());

            WorkspaceMember savedWorkspaceMember = workspaceMemberCaptor.getValue();
            assertThat(savedWorkspaceMember.getWorkspace()).isEqualTo(savedWorkspace);
            assertThat(savedWorkspaceMember.getMemberId()).isEqualTo(userId);
        }

        @Test
        void slugAlreadyExists_preValidation_throwsSlugAlreadyExists_andDoesNotSaveAnything() {
            // arrange
            WorkspaceCreateRequest request = WorkspaceCreateRequestBuilder.aWorkspaceCreateRequest().build();
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(true);
            // act and assert
            assertThatThrownBy(() -> service.workspaceCreate(request))
                    .isInstanceOf(SlugAlreadyExistsException.class)
                    .hasMessage("That URL is taken.");
            // verify
            verify(workspaceRepository)
                    .existsBySlugIgnoreCaseAndDeletedAtIsNull(request.getSlug().trim().toLowerCase());
            verify(workspaceRepository, never()).save(any());
            verify(workspaceMemberRepository, never()).save(any());
        }

        @Test
        void whenDbThrowsDataIntegrityViolation_wrapsAsSlugAlreadyExistsException() {
            // arrange
            WorkspaceCreateRequest request = WorkspaceCreateRequestBuilder.aWorkspaceCreateRequest().build();
            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
            when(userContext.getUserId()).thenReturn(UUID.randomUUID());
            when(workspaceRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique constraint"));
            // act + assert
            assertThatThrownBy(() -> service.workspaceCreate(request))
                    .isInstanceOf(SlugAlreadyExistsException.class)
                    .hasMessage("That URL is taken.")
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            verify(workspaceMemberRepository, never()).save(any());
            verifyNoMoreInteractions(workspaceRepository);
        }

        @Test
        void workspaceCreatedButProjectionMissing_throws_IllegalStateException() {
            // arrange
            UUID userId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            WorkspaceCreateRequest request = WorkspaceCreateRequestBuilder.aWorkspaceCreateRequest()
                    .name("  Marvel  ")
                    .slug(" LunaSnow  ")
                    .organizationSize(" just myself ")
                    .companyRole("  Avenger  ")
                    .build();

            when(workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
            when(userContext.getUserId()).thenReturn(userId);
            when(workspaceRepository.save(any())).thenAnswer(new Answer<Workspace>() {
                @Override
                public Workspace answer(InvocationOnMock invocation) throws Throwable {
                    Workspace workspace = invocation.getArgument(0);
                    workspace.setId(workspaceId);
                    return workspace;
                }
            });
            when(workspaceRepository.findWorkspaceById(any(UUID.class))).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.workspaceCreate(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Workspace created but could not be loaded.");

            verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
        }

    }

}
