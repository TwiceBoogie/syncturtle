package com.syncturtle.services.user.model.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonDefaults {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
