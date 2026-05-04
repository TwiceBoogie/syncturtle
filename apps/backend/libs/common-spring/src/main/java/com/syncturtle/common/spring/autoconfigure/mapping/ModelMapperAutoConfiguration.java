package com.syncturtle.common.spring.autoconfigure.mapping;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.spring.mapping.BasicMapper;
import com.syncturtle.common.spring.mapping.CursorCodec;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ModelMapper.class)
public class ModelMapperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE);
        return mapper;
    }

    @Bean
    CursorCodec cursorCodec(ObjectMapper objectMapper) {
        return new CursorCodec(objectMapper);
    }

    @Bean
    BasicMapper basicMapper(ModelMapper mapper, CursorCodec cursorCodec) {
        return new BasicMapper(mapper, cursorCodec);
    }

}
