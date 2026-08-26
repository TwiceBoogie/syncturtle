package com.syncturtle.services.file.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.services.file.configuration.property.FileCleanupProperties;
import com.syncturtle.services.file.configuration.property.FileKafkaProperties;
import com.syncturtle.services.file.configuration.property.FileUploadProperties;
import com.syncturtle.services.file.configuration.property.StorageProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        StorageProperties.class,
        FileUploadProperties.class,
        FileCleanupProperties.class,
        FileKafkaProperties.class
})
public class FileServicePropertiesConfiguration {
}
