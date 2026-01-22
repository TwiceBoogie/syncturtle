package com.syncturtle.common.spring.web.url;

import java.net.URI;
import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;

public class ReactiveHostUrlBuilder {

    private final HostUrlBuilder core;

    public ReactiveHostUrlBuilder(HostUrlBuilder core) {
        this.core = core;
    }

    public String baseHost(ServerHttpRequest request, boolean isAdmin, boolean isSpace, boolean isApp) {
        return core.baseHost(isAdmin, isSpace, isApp);
    }

    public String buildAdminRedirectWithErrors(ServerHttpRequest request, Map<String, ?> errors) {
        String base = baseHost(request, true, false, false);
        return core.buildRedirectUriWithQuery(base, errors);
    }

    public String deriveOriginFromRequest(ServerHttpRequest request) {
        URI uri = request.getURI();
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
