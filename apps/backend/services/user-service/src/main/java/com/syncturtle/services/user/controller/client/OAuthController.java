package com.syncturtle.services.user.controller.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.cookie.ServletAuthCookieWriter;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.service.OAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.AUTH)
public class OAuthController {

    private final OAuthService service;
    private final ServletAuthCookieWriter cookieWriter;

    @GetMapping("/google")
    public ResponseEntity<Void> googleInitiate(@RequestParam(name = "nextPath", required = false) String nextPath) {
        String redirection = service.googleOAuthInitiate(nextPath);

        return redirect(redirection);
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state, HttpServletResponse servletResponse) {
        IssueTokenResponse response = service.googleOAuthCallback(code, state);

        writeSessionCookiesIfPresent(servletResponse, response);

        return redirect(response.getLocation());
    }

    @GetMapping("/github")
    public ResponseEntity<Void> githubInitiate(@RequestParam(name = "nextPath", required = false) String nextPath) {
        String redirection = service.githubOAuthInitiate(nextPath);

        return redirect(redirection);
    }

    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state, HttpServletResponse servletResponse) {
        IssueTokenResponse response = service.githubOAuthCallback(code, state);

        writeSessionCookiesIfPresent(servletResponse, response);

        return redirect(response.getLocation());
    }

    @GetMapping("/gitlab")
    public ResponseEntity<Void> gitlabInitiate(@RequestParam(name = "nextPath", required = false) String nextPath) {
        String redirection = service.gitlabOAuthInitiate(nextPath);

        return redirect(redirection);
    }

    @GetMapping("/gitlab/callback")
    public ResponseEntity<Void> gitlabCallback(@RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state, HttpServletResponse servletResponse) {
        IssueTokenResponse response = service.gitlabOAuthCallback(code, state);

        writeSessionCookiesIfPresent(servletResponse, response);

        return redirect(response.getLocation());
    }

    private ResponseEntity<Void> redirect(String redirection) {
        Assert.hasText(redirection, "redirection is required");

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(redirection))
                .build();
    }

    private void writeSessionCookiesIfPresent(HttpServletResponse response, IssueTokenResponse session) {
        if (session == null) {
            return;
        }

        Duration accessMaxAge = Duration.between(session.getAccessIssuedAt(), session.getAccessExpiresAt());
        Duration refreshMaxAge = Duration.between(session.getRefreshIssuedAt(), session.getRefreshExpiresAt());

        cookieWriter.setAccessTokenCookie(response, session.getAccessToken(), accessMaxAge);
        cookieWriter.setRefreshTokenCookie(response, session.getRefreshToken(), refreshMaxAge);
    }

}
