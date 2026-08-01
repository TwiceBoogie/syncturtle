package com.syncturtle.services.workspace.service.impl;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.repository.WorkspaceMemberInviteRepository;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.service.AcceptedWorkspaceInvitationCompletionService;
import com.syncturtle.services.workspace.service.collaborator.outbox.WorkspaceOutboxWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptedWorkspaceInvitationCompletionServiceImpl implements AcceptedWorkspaceInvitationCompletionService {

    private final WorkspaceMemberInviteRepository workspaceMemberInviteRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceEventFactory workspaceEventFactory;
    private final WorkspaceOutboxWriter workspaceOutboxWriter;
    private final Clock clock;

    @Override
    @Transactional
    public void completeAcceptedInvitations(UUID userId, String email) {
        Assert.notNull(userId, "userId is required");
        Assert.hasText(email, "email is required");

        String normalizedEmail = normalizeEmail(email);

        List<WorkspaceMemberInvite> invites = workspaceMemberInviteRepository
                .findAllByEmailIgnoreCaseAndAcceptedTrueAndRespondedAtIsNotNullAndConsumedAtIsNullAndDeletedAtIsNull(
                        normalizedEmail);

        if (invites.isEmpty()) {
            return;
        }

        for (WorkspaceMemberInvite invite : invites) {
            completeAcceptedInvitation(userId, invite);
        }
    }

    private void completeAcceptedInvitation(UUID userId, WorkspaceMemberInvite invite) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(invite, "workspace member invite is required");
        Assert.state(invite.isAcceptedAwaitingConsumption(),
                "workspace member invite must be accepted and awaiting consumption");

        UUID workspaceId = invite.getWorkspaceId();

        if (workspaceMemberRepository.existsByWorkspace_IdAndMemberIdAndDeletedAtIsNull(workspaceId, userId)) {
            consumeInvite(invite);
            return;
        }

        try {
            WorkspaceMember member = createWorkspaceMember(userId, invite);

            consumeInvite(invite);

            WorkspaceMemberEvent memberEvent = workspaceEventFactory.memberCreated(member, workspaceId);
            workspaceOutboxWriter.saveWorkspaceMemberEvent(memberEvent);
        } catch (DataIntegrityViolationException exception) {
            log.info(
                    "Workspace member already exists while completing accepted invitation. workspaceId={} userId={} inviteId={}",
                    workspaceId, userId, invite.getId());

            consumeInvite(invite);
        }
    }

    private WorkspaceMember createWorkspaceMember(UUID userId, WorkspaceMemberInvite invite) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(invite, "workspace member invite is required");
        Assert.notNull(invite.getWorkspace(), "workspace is required");

        WorkspaceMemberCreateParam param = WorkspaceMemberCreateParam.builder()
                .workspace(invite.getWorkspace())
                .memberId(userId)
                .role(invite.getRole())
                .companyRole(null)
                .build();

        return workspaceMemberRepository.saveAndFlush(WorkspaceMember.create(param));
    }

    private void consumeInvite(WorkspaceMemberInvite invite) {
        Assert.notNull(invite, "workspace member invite is required");

        if (invite.isConsumed()) {
            return;
        }

        invite.consume(clock);

        WorkspaceMemberInvite savedInvite = workspaceMemberInviteRepository.saveAndFlush(invite);
        WorkspaceMemberInviteEvent inviteEvent = workspaceEventFactory.memberInviteUpdated(savedInvite);

        workspaceOutboxWriter.saveWorkspaceMemberInviteEvent(inviteEvent);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
