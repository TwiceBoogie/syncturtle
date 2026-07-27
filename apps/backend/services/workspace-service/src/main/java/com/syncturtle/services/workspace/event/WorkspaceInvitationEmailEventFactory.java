package com.syncturtle.services.workspace.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.workspace.model.UserLite;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceInvitationEmailEventFactory {

    private final PublicUrlResolver hostResolver;

    public EmailToSendEvent workspaceInvitation(Workspace workspace, WorkspaceMemberInvite invitation,
            UserLite invitedBy) {
        Assert.notNull(workspace, "workspace is required");
        Assert.notNull(invitation, "workspace invitation is required");
        Assert.notNull(invitedBy, "invitedBy is required");

        String inviteUrl = hostResolver.userAppWithQuery("/workspace-invitations/accept",
                Map.of("token", invitation.getToken()));

        return EmailToSendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .templateType(EmailTemplateType.WORKSPACE_INVITATION)
                .subject(invitedBy.getEmail() + " invited you to join " + workspace.getName())
                .to(List.of(invitation.getEmail()))
                .model(Map.of(
                        "workspaceId", workspace.getId().toString(),
                        "workspaceName", workspace.getName(),
                        "workspaceSlug", workspace.getSlug(),
                        "invitedByEmail", invitedBy.getEmail(),
                        "inviteUrl", inviteUrl,
                        "token", invitation.getToken()))
                .build();
    }

}
