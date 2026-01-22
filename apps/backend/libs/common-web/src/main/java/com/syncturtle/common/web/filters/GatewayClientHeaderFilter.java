package com.syncturtle.common.web.filters;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.constants.GatewayHeaderNames;
import com.syncturtle.common.web.context.RequestClientContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class GatewayClientHeaderFilter extends OncePerRequestFilter {

    private final RequestClientContext ctx;

    public GatewayClientHeaderFilter(RequestClientContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String clientIp = request.getHeader(GatewayHeaderNames.HDR_CLIENT_IP);
            String userAgant = request.getHeader(GatewayHeaderNames.HDR_CLIENT_UA);

            ctx.setClientIp(blankToNull(clientIp));
            ctx.setUserAgent(blankToNull(userAgant));

            filterChain.doFilter(request, response);
        } finally {
            ctx.clear();
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

}
