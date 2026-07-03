package com.syncturtle.common.contracts.messaging;

public final class KafkaTopics {
    private KafkaTopics() {
        throw new UnsupportedOperationException("Constant class");
    }

    public static final String INSTANCE_EVENTS_V1 = "instance.events.v1";
    public static final String INSTANCE_CONFIG_EVENTS_V1 = "instance-config.events.v1";
    public static final String USER_EVENTS_V1 = "user.events.v1";
    public static final String PASSWORD_EVENTS_V1 = "password.events.v1";
    public static final String WORKSPACE_EVENTS_V1 = "workspace.events.v1";
    public static final String WORKSPACE_MEMBER_EVENTS_V1 = "workspace-member.events.v1";
    public static final String WORKSPACE_MEMBER_INVITE_EVENTS_V1 = "workspace-member-invite.events.v1";
    public static final String EMAIL_EVENTS_V1 = "email.events.v1";
    public static final String INSTANCE_ADMIN_SECURITY_EVENTS_V1 = "instance-admin-security.events.v1";
}
