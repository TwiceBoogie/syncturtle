package com.syncturtle.services.workspace.configuration.asset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.core.asset.AssetContentUrlFactory;

@Configuration(proxyBeanMethods = false)
public class AssetContentUrlFactoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AssetContentUrlFactory assetContentUrlFactory() {
        return new AssetContentUrlFactory();
    }

}
