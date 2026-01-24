package com.syncturtle.common.spring.web.url;

import java.util.Map;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

public class ServletHostUrlBuilder {

    private final HostUrlBuilder core;

    public ServletHostUrlBuilder(HostUrlBuilder core) {
        this.core = core;
    }

    public String baseHost(HttpServletRequest request, boolean isAdmin, boolean isSpace, boolean isApp) {
        return core.baseHost(isAdmin, isSpace, isApp);
    }

    public String buildRedirectUrlWithErrors(HttpServletRequest request, Map<String, ?> errors) {
        String base = baseHost(request, false, false, false);
        return core.buildRedirectUriWithQuery(base, errors);
    }

    public String buildAdminRedirectUrlWithErrors(HttpServletRequest request, Map<String, ?> errors) {
        String base = baseHost(request, true, false, false);
        return core.buildRedirectUriWithQuery(base, errors);
    }

    public String deriveOriginFromRequeset(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequest(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
