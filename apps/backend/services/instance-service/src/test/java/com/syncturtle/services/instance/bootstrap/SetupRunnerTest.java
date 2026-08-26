package com.syncturtle.services.instance.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;

import com.syncturtle.services.instance.service.InstanceSetupService;

@DisplayName("SetupRunner")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SetupRunnerTest {

    private static final String MACHINE_SIGNATURE_ARG = "machine-signature";

    @Mock
    InstanceSetupService service;
    @Mock
    ApplicationContext context;
    @Mock
    ApplicationArguments args;

    private SetupRunner runner;

    @BeforeEach
    void setup() {
        runner = new SetupRunner(service, context);
    }

    @Nested
    @DisplayName("execute(ApplicationArguments)")
    class ExecuteTests {

        @Test
        @DisplayName("runs registrar then configurator and returns zero")
        void callsPublicSetupBoundary() {
            // arrange
            // conditions
            when(args.getOptionValues(MACHINE_SIGNATURE_ARG)).thenReturn(List.of("machine-abc"));
            // act + assert
            assertThat(runner.execute(args)).isZero();
            // verify
            verify(service).setup("machine-abc");
        }

        @Test
        @DisplayName("returns failure when the setup transaction fails")
        void returnsFailureForSetupFailure() {
            // arrange
            // conditions
            when(args.getOptionValues(MACHINE_SIGNATURE_ARG)).thenReturn(null);
            doThrow(new IllegalStateException("rollback")).when(service).setup(null);
            // act + assert
            assertThat(runner.execute(args)).isOne();
            // verify
            verify(service).setup(null);
        }

    }

}
