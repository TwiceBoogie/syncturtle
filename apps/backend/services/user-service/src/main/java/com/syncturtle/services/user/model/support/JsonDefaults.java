package com.syncturtle.services.user.model.support;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class JsonDefaults {

    private static final JsonMapper MAPPER = new JsonMapper();

    private JsonDefaults() {
    }

    public static ObjectNode profileOnboarding() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("profileComplete", false);
        node.put("workspaceCreate", false);
        node.put("workspaceInvite", false);
        node.put("workspaceJoin", false);
        return node;
    }

    public static ObjectNode emptyObject() {
        return MAPPER.createObjectNode();
    }

}
