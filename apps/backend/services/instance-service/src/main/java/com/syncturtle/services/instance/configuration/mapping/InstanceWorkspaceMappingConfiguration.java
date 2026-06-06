package com.syncturtle.services.instance.configuration.mapping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.services.instance.service.mapper.InstanceWorkspacePageMapper;
import com.syncturtle.services.instance.service.mapper.InstanceWorkspaceResponseMapper;

@Configuration(proxyBeanMethods = false)
public class InstanceWorkspaceMappingConfiguration {

    @Bean
    InstanceWorkspacePageMapper instanceWorkspacePageMapper(
            CursorCodec cursorCodec,
            InstanceWorkspaceResponseMapper mapper) {
        return new InstanceWorkspacePageMapper(cursorCodec, mapper);
    }

}
