package com.syncturtle.services.instance.model.param;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class InstanceRuntimeParam {

    private final String domain;
    private final String namespace;
    private final String vmHost;

    @Builder
    private InstanceRuntimeParam(
            String domain,
            String namespace,
            String vmHost) {
        this.domain = normalizeNullable(domain);
        this.namespace = normalizeNullable(namespace);
        this.vmHost = normalizeNullable(vmHost);
    }

    public static InstanceRuntimeParam empty() {
        return InstanceRuntimeParam.builder().build();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
