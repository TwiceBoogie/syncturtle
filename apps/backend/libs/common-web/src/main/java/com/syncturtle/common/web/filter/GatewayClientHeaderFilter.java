package com.syncturtle.common.web.filter;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.syncturtle.common.core.header.GatewayHeaders;
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
            String clientIp = request.getHeader(GatewayHeaders.HDR_CLIENT_IP);
            String userAgant = request.getHeader(GatewayHeaders.HDR_CLIENT_UA);

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
