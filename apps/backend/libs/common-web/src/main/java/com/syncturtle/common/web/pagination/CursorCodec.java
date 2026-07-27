package com.syncturtle.common.web.pagination;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RequiredArgsConstructor
public class CursorCodec {

    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();

    private final JsonMapper objectMapper;

    public String encode(UUID id, Instant createdAt) {
        Assert.notNull(id, "id is required");
        Assert.notNull(createdAt, "createdAt is required");

        try {
            String json = objectMapper.createObjectNode()
                    .put("createdAt", createdAt.toString())
                    .put("id", id.toString())
                    .toString();

            return B64URL_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode cursor.", exception);
        }
    }

    public DecodedCursor decode(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }

        try {
            String normalizedCursor = padBase64Url(cursor);
            String text = new String(B64URL_DECODER.decode(normalizedCursor), StandardCharsets.UTF_8);

            JsonNode node = objectMapper.readTree(text);
            if (!node.hasNonNull("createdAt") || !node.hasNonNull("id")) {
                log.warn("[cursor] missing required cursor fields. cursor='{}'", cursor);
                return null;
            }

            Instant createdAt = Instant.parse(node.get("createdAt").asString());
            UUID id = UUID.fromString(node.get("id").asString());

            return new DecodedCursor(id, createdAt);
        } catch (Exception exception) {
            log.warn("[cursor] invalid cursor. Treating request as first page. cursor='{}'", cursor);
            return null;
        }
    }

    private static String padBase64Url(String cursor) {
        int mod = cursor.length() % 4;
        if (mod == 0) {
            return cursor;
        }

        return cursor + "====".substring(mod);
    }

}
