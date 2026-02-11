package com.syncturtle.common.core.utils;

import java.util.Set;

public final class WorkspaceSlugRules {

    private WorkspaceSlugRules() {
    }

    public static final Set<String> RESTRICTED = Set.of("404", "accounts", "api");

}
