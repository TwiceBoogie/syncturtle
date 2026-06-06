package com.syncturtle.services.instance.configuration.web.authorization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

public final class AuthorizationResponseWriter {

    public void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write("""
                {
                    "ok": false,
                    "error": {
                        "code": "%s",
                        "message": "%s"
                    }
                }
                """.formatted(escapeJson(code), escapeJson(message)));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
