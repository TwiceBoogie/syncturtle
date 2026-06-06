package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.web.pagination.CursorCodec;

@AutoConfiguration
public class CursorCodecAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CursorCodec cursorCodec(ObjectMapper mapper) {
        return new CursorCodec(mapper);
    }

}
