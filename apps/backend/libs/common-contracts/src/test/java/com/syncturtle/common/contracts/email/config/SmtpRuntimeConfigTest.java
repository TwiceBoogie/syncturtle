package com.syncturtle.common.contracts.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

class SmtpRuntimeConfigTest {

    @Test
    void jackson3DeserializesTheGeneratedBuilder() {
        String json = """
                {"host":"smtp.example.test","user":"mailer","password":"test-password",
                 "port":587,"useTls":true,"useSsl":false,"from":"sender@example.test"}
                """;
        JsonMapper mapper = JsonMapper.builder().build();

        SmtpRuntimeConfig config = mapper.readValue(json, SmtpRuntimeConfig.class);

        assertThat(SmtpRuntimeConfig.class.getAnnotation(JsonDeserialize.class)).isNotNull();
        assertThat(config.getHost()).isEqualTo("smtp.example.test");
        assertThat(config.getPort()).isEqualTo(587);
        assertThat(config.isUseTls()).isTrue();
        assertThat(config.isUseSsl()).isFalse();
    }
}
