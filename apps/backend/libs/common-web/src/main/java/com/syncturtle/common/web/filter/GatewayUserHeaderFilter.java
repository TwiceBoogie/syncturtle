package com.syncturtle.common.web.filter;

import java.io.IOException;
import java.util.UUID;

import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class GatewayUserHeaderFilter extends OncePerRequestFilter {

    private final RequestUserContext requestUserContext;

    public GatewayUserHeaderFilter(RequestUserContext requestUserContext) {
        this.requestUserContext = requestUserContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            UUID userId = parseUuidOrNull(request.getHeader(GatewayHeaders.HDR_AUTH_USER_ID));
            if (userId != null) {
                requestUserContext.setUserId(userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            requestUserContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
