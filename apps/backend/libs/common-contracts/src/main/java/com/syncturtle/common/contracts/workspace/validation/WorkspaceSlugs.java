package com.syncturtle.common.contracts.workspace.validation;

import java.util.Set;

public final class WorkspaceSlugs {

    private WorkspaceSlugs() {
    }

    public static final Set<String> RESTRICTED = Set.of("404", "accounts", "api");

}
