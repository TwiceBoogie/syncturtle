package com.syncturtle.common.spring.mapping;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CursorCodec {

    private static final Base64.Encoder B64URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    public String encode(UUID id, Instant createdAt) {
        if (id == null || createdAt == null) {
            return null;
        }

        try {
            String json = objectMapper.createObjectNode()
                    .put("createdAt", createdAt.toString())
                    .put("id", id.toString())
                    .toString();

            return B64URL_ENC.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("[cursor] encode failed", e);
            return null;
        }
    }

    public DecodedCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            int mod = cursor.length() % 4;
            if (mod != 0) {
                cursor = cursor + "====".substring(mod);
            }

            String text = new String(B64URL_DEC.decode(cursor), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(text);

            if (!node.hasNonNull("createdAt") || !node.hasNonNull("id")) {
                log.warn("[cursor] missing field -> {}", text);
                return null;
            }

            Instant createdAt = Instant.parse(node.get("createdAt").asText());
            UUID id = UUID.fromString(node.get("id").asText());

            return new DecodedCursor(id, createdAt);
        } catch (Exception e) {
            log.warn("[cursor] decode failed, treating as first page. cursor='{}'", cursor, e);
            return null;
        }
    }

    public record DecodedCursor(UUID id, Instant createdAt) {
    }

}
