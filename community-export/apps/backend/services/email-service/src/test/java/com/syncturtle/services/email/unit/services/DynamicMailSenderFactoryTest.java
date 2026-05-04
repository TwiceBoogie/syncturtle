package com.syncturtle.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.syncturtle.services.email.configurations.properties.SyncturtleConfig;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.service.DynamicMailSenderFactory;

@ExtendWith(MockitoExtension.class)
public class DynamicMailSenderFactoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    SyncturtleConfig syncturtleConfig;

    DynamicMailSenderFactory factory;

    @BeforeEach
    void setup() {
        factory = new DynamicMailSenderFactory(syncturtleConfig);
    }

    @Test
    void create_whenConfigComplete_returnsConfiguredJavaMailSender() {
        // arrange
        stubSenderTimeout();
        EmailRuntimeConfig config = completeConfig();
        // act
        JavaMailSender javaMailSender = factory.create(config);
        // assert
        assertThat(javaMailSender).isInstanceOf(JavaMailSenderImpl.class);

        JavaMailSenderImpl sender = (JavaMailSenderImpl) javaMailSender;
        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getUsername()).isEqualTo("mailer");
        assertThat(sender.getPassword()).isEqualTo("secret");

        Properties props = sender.getJavaMailProperties();
        assertThat(props.get("mail.transport.protocol")).isEqualTo("smtp");
        assertThat(props.get("mail.smtp.auth")).isEqualTo("true");
        assertThat(props.get("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.get("mail.smtp.ssl.enable")).isEqualTo("false");
        assertThat(props.get("mail.smtp.connectiontimeout")).isEqualTo(5000);
        assertThat(props.get("mail.smtp.timeout")).isEqualTo(3000);
        assertThat(props.get("mail.smtp.writetimeout")).isEqualTo(5000);
    }

    @Test
    void create_whenUsernameAndPasswordBlank_leavesCredentialsUnset() {
        // arrange
        stubSenderTimeout();
        EmailRuntimeConfig config = EmailRuntimeConfig.builder()
                .enabled(true)
                .host("smtp.example.com")
                .port(25)
                .username(" ")
                .password("")
                .from("no-reply@example.com")
                .useTls(false)
                .useSsl(false)
                .version(1L)
                .build();
        // act
        JavaMailSenderImpl sender = (JavaMailSenderImpl) factory.create(config);
        // assert
        assertThat(sender.getUsername()).isNull();
        assertThat(sender.getPassword()).isNull();
        assertThat(sender.getJavaMailProperties().get("mail.smtp.auth")).isEqualTo("false");
    }

    @Test
    void create_whenConfigIncomplete_throwsIllegalStateException() {
        // arrange
        EmailRuntimeConfig config = EmailRuntimeConfig.builder()
                .enabled(true)
                .host("")
                .port(587)
                .from("no-reply@example.com")
                .useTls(true)
                .useSsl(false)
                .version(1L)
                .build();

        // act + assert
        assertThatThrownBy(() -> factory.create(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email runtime config is incomplete");
    }

    private void stubSenderTimeout() {
        when(syncturtleConfig.getEmail().getSender().getConnectionTimeoutMs()).thenReturn(5000);
        when(syncturtleConfig.getEmail().getSender().getTimeoutMs()).thenReturn(3000);
        when(syncturtleConfig.getEmail().getSender().getWriteTimeoutMs()).thenReturn(5000);
    }

    private static EmailRuntimeConfig completeConfig() {
        return EmailRuntimeConfig.builder()
                .enabled(true)
                .host("smtp.example.com")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@example.com")
                .useTls(true)
                .useSsl(false)
                .version(7L)
                .build();
    }

}
