package com.syncturtle.services.email.support.fixture;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;

public final class EmailFixtures {

    public static final String EVENT_ID = "email-event-1001";
    public static final String SECOND_EVENT_ID = "email-event-1002";
    public static final String THIRD_EVENT_ID = "email-event-1003";
    public static final String EVENT_TYPE = EmailToSendEvent.EVENT_TYPE;
    public static final Instant OCCURRED_AT = Instant.parse("2026-07-19T13:59:00Z");
    public static final String RECIPIENT = "lunasnow@marvel.com";
    public static final String SECOND_RECIPIENT = "suestorm@marvel.com";
    public static final List<String> RECIPIENTS = List.of(RECIPIENT, SECOND_RECIPIENT);

    private EmailFixtures() {
        throw new AssertionError("EmailFixtures must not be instantiated");
    }

    public static EmailToSendEvent magicCodeEmailEvent() {
        return emailEvent(EmailTemplateType.MAGIC_CODE, MagicCode.SUBJECT, MagicCode.MODEL);
    }

    public static EmailToSendEvent magicCodeEmailEvent(String eventId) {
        return emailEvent(eventId, EmailTemplateType.MAGIC_CODE, MagicCode.SUBJECT, MagicCode.MODEL);
    }

    public static EmailDispatchParam magicCodeDispatchParam() {
        return dispatchParam(EmailTemplateType.MAGIC_CODE, MagicCode.SUBJECT, MagicCode.MODEL);
    }

    public static EmailDispatchParam passwordResetDispatchParam() {
        return dispatchParam(EmailTemplateType.PASSWORD_RESET, PasswordReset.SUBJECT, PasswordReset.MODEL);
    }

    public static EmailDispatchParam workspaceInvitationDispatchParam() {
        return dispatchParam(EmailTemplateType.WORKSPACE_INVITATION, WorkspaceInvitation.SUBJECT,
                WorkspaceInvitation.MODEL);
    }

    public static EmailDispatchParam magicCodeDispatchparam(Map<String, Object> model) {
        return dispatchParam(EmailTemplateType.MAGIC_CODE, MagicCode.SUBJECT, model);
    }

    public static RenderedEmailResult renderedMagicCodeEmail() {
        return new RenderedEmailResult(MagicCode.SUBJECT, MagicCode.HTML_BODY, MagicCode.TEXT_BODY);
    }

    private static EmailToSendEvent emailEvent(EmailTemplateType templateType, String subject,
            Map<String, Object> model) {
        return EmailToSendEvent.builder()
                .eventId(EVENT_ID)
                .occurredAt(OCCURRED_AT)
                .templateType(templateType)
                .subject(subject)
                .to(RECIPIENTS)
                .model(model)
                .build();
    }

    private static EmailToSendEvent emailEvent(String eventId, EmailTemplateType templateType, String subject,
            Map<String, Object> model) {
        return EmailToSendEvent.builder()
                .eventId(eventId)
                .occurredAt(OCCURRED_AT)
                .templateType(templateType)
                .subject(subject)
                .to(RECIPIENTS)
                .model(model)
                .build();
    }

    private static EmailDispatchParam dispatchParam(EmailTemplateType templateType, String subject,
            Map<String, Object> model) {
        return EmailDispatchParam.builder()
                .eventId(EVENT_ID)
                .templateType(templateType)
                .subject(subject)
                .recipients(RECIPIENTS)
                .model(model)
                .build();
    }

    public static final class MagicCode {

        public static final String SUBJECT = "Your Syncturtle sign-in code";
        public static final String CODE = "uqpo-zgko-rtgx";
        public static final int EXPIRES_IN_MINUTES = 10;

        public static final String HTML_BODY = """
                <p>Your sign-in code is <strong>%s</strong>.</p>
                """.formatted(CODE).stripTrailing();

        public static final String TEXT_BODY = """
                Your Syncturtle sign-in code

                The code below is valid for %d minutes.

                Sign-in code: %s
                """.formatted(EXPIRES_IN_MINUTES, CODE).stripTrailing();

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "code", CODE,
                "expiresInMinutes", EXPIRES_IN_MINUTES);

        private MagicCode() {
            throw new AssertionError("MagicCode fixture must not be instantiated");
        }

    }

    public static final class PasswordReset {

        public static final String SUBJECT = "Reset your Syncturtle password";
        public static final String WEBSITE_URL = "https://app.syncturtle.test/accounts/reset-password?token=password-reset-token";

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "websiteUrl", WEBSITE_URL);

        private PasswordReset() {
            throw new AssertionError("PasswordReset fixture must not be instantiated");
        }
    }

    public static final class WorkspaceInvitation {

        public static final String SUBJECT = "Join Avengers Tower on Syncturtle";
        public static final String INVITER_NAME = "Tony Stark";
        public static final String WORKSPACE_NAME = "Avengers Tower";
        public static final String INVITATION_URL = "https://app.syncturtle.test/workspace-invitations/workspace-token";

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "inviterName", INVITER_NAME,
                "workspaceName", WORKSPACE_NAME,
                "invitationUrl", INVITATION_URL);

        private WorkspaceInvitation() {
            throw new AssertionError("WorkspaceInvitation fixture must not be instantiated");
        }
    }

    public static final class ProjectInvitation {

        public static final String SUBJECT = "Join Project Rebirth on Syncturtle";
        public static final String INVITER_NAME = "Sue Storm";
        public static final String PROJECT_NAME = "Project Rebirth";
        public static final String PROJECT_INVITATION_URL = "https://app.syncturtle.test/project-invitations/project-token";

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "inviterName", INVITER_NAME,
                "projectName", PROJECT_NAME,
                "projectInvitationUrl", PROJECT_INVITATION_URL);

        private ProjectInvitation() {
            throw new AssertionError("ProjectInvitation fixture must not be instantiated");
        }
    }

    public static final class UserActivation {

        public static final String SUBJECT = "Your Syncturtle account is active";
        public static final String FIRST_NAME = "Luna";
        public static final String WEBSITE_URL = "https://app.syncturtle.test/sign-in";

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "firstName", FIRST_NAME,
                "websiteUrl", WEBSITE_URL);

        private UserActivation() {
            throw new AssertionError("UserActivation fixture must not be instantiated");
        }
    }

    public static final class UserDeactivation {

        public static final String SUBJECT = "Your Syncturtle account has been deactivated";
        public static final String FIRST_NAME = "Luna";
        public static final String SUPPORT_URL = "https://support.syncturtle.test";

        public static final Map<String, Object> MODEL = Map.of(
                "email", RECIPIENT,
                "firstName", FIRST_NAME,
                "supportUrl", SUPPORT_URL);

        private UserDeactivation() {
            throw new AssertionError("UserDeactivation fixture must not be instantiated");
        }
    }
}
