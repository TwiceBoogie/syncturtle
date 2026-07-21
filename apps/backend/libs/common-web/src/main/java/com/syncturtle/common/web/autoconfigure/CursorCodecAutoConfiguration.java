package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.web.pagination.CursorCodec;

import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({ JsonMapper.class, CursorCodec.class })
public class CursorCodecAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JsonMapper.class)
    CursorCodec cursorCodec(JsonMapper mapper) {
        return new CursorCodec(mapper);
    }

}
