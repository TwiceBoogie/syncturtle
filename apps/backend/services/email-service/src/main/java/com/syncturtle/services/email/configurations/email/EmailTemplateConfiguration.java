package com.syncturtle.services.email.configurations.email;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration(proxyBeanMethods = false)
public class EmailTemplateConfiguration {

    @Bean("emailTemplateEngine")
    TemplateEngine emailTemplateEngine(ClassLoaderTemplateResolver emailHtmlTemplateResolver,
            ClassLoaderTemplateResolver emailTextTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolvers(Set.of(emailHtmlTemplateResolver, emailTextTemplateResolver));
        return engine;
    }

    @Bean
    ClassLoaderTemplateResolver emailHtmlTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);
        resolver.setOrder(1);
        resolver.setResolvablePatterns(Set.of("email/html/*"));
        return resolver;
    }

    @Bean
    ClassLoaderTemplateResolver emailTextTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        resolver.setCheckExistence(true);
        resolver.setOrder(2);
        resolver.setResolvablePatterns(Set.of("email/text/*"));
        return resolver;
    }
}
