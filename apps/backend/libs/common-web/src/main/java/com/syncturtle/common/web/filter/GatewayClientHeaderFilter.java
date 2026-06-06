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

    private final RequestClientContext requestClientContext;

    public GatewayClientHeaderFilter(RequestClientContext requestClientContext) {
        this.requestClientContext = requestClientContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            requestClientContext.setClientIp(blankToNull(request.getHeader(GatewayHeaders.HDR_CLIENT_IP)));
            requestClientContext.setUserAgent(blankToNull(request.getHeader(GatewayHeaders.HDR_CLIENT_UA)));

            filterChain.doFilter(request, response);
        } finally {
            requestClientContext.clear();
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

}
