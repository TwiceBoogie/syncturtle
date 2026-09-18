package com.syncturtle.common.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.security.autoconfigure.RequestProvenanceAutoConfiguration;
import com.syncturtle.common.security.provenance.RequestProvenanceCanonicalizer;
import com.syncturtle.common.security.provenance.RequestProvenanceOutboundPolicy;
import com.syncturtle.common.security.provenance.RequestProvenanceSigner;
import com.syncturtle.common.web.client.FeignAuthErrorDecoder;
import com.syncturtle.common.web.client.RequestProvenanceFeignInterceptor;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = RequestProvenanceAutoConfiguration.class, afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnClass({
        RequestInterceptor.class,
        FeignClient.class
})
public class FeignClientAutoConfiguration {

    @Bean(name = "syncturtleRequestProvenanceInterceptor")
    @ConditionalOnBean({ RequestProvenanceSigner.class, RequestProvenanceOutboundPolicy.class })
    @ConditionalOnMissingBean(name = "syncturtleRequestProvenanceInterceptor")
    RequestInterceptor syncturtleRequestProvenanceInterceptor(
            RequestProvenanceSigner signer,
            RequestProvenanceOutboundPolicy policy,
            RequestProvenanceCanonicalizer canonicalizer) {
        return new RequestProvenanceFeignInterceptor(signer, policy, canonicalizer);
    }

    @Bean
    @ConditionalOnBean(JsonMapper.class)
    @ConditionalOnMissingBean(ErrorDecoder.class)
    @ConditionalOnClass({ ErrorDecoder.class, JsonMapper.class })
    ErrorDecoder syncturtleFeignAuthErrorDecoder(JsonMapper mapper) {
        return new FeignAuthErrorDecoder(mapper);
    }

}
