package com.syncturtle.common.security.provenance;

import java.util.List;

@FunctionalInterface
public interface RequestHeaderSource {
    List<String> values(String headerName);
}
