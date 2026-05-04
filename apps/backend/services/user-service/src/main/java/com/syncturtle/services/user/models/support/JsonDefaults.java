package com.syncturtle.services.user.models.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonDefaults {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonDefaults() {
    }

    public static ObjectNode profileOnboarding() {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("profileComplete", false);
        n.put("workspaceCreate", false);
        n.put("workspaceInvite", false);
        n.put("workspaceJoin", false);
        return n;
    }

    public static ObjectNode emptyObject() {
        return MAPPER.createObjectNode();
    }

}
