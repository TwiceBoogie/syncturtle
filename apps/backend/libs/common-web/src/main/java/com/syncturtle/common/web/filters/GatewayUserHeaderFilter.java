package com.syncturtle.common.web.filters;

import java.io.IOException;
import java.util.UUID;

import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.constants.RequestHeaderNames;
import com.syncturtle.common.web.context.RequestUserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class GatewayUserHeaderFilter extends OncePerRequestFilter {

    private final RequestUserContext ctx;

    public GatewayUserHeaderFilter(RequestUserContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String raw = request.getHeader(RequestHeaderNames.AUTH_USER_ID);
            UUID userId = parseUuidOrNull(raw);
            if (userId != null) {
                ctx.setUserId(userId);
            }

            filterChain.doFilter(request, response);
        } finally {
            ctx.clear();
        }
    }

    private static UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
