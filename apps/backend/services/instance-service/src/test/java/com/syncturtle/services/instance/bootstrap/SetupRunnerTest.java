package com.syncturtle.services.instance.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;

@DisplayName("SetupRunner")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SetupRunnerTest {

    private static final String MACHINE_SIGNATURE_ARG = "machine-signature";

    @Mock
    InstanceRegistrar registrar;
    @Mock
    InstanceConfigurator configurator;
    @Mock
    ApplicationContext context;
    @Mock
    ApplicationArguments args;
    private SetupRunner runner;

    @BeforeEach
    void setup() {
        runner = new SetupRunner(registrar, configurator, context);
    }

    @Nested
    @DisplayName("execute(ApplicationArguments)")
    class ExecuteTests {

        @Test
        @DisplayName("runs registrar then configurator and returns zero")
        void runsRegistrarThanConfiguratorAndReturnsZero() {
            // arrange
            // conditions
            when(args.getOptionValues(eq(MACHINE_SIGNATURE_ARG))).thenReturn(List.of("machine-abc"));
            // act
            int actual = runner.execute(args);
            // assert
            assertThat(actual).isZero();
            // verify
            InOrder order = inOrder(registrar, configurator);
            order.verify(registrar).run("machine-abc");
            order.verify(configurator).run();

            verify(args).getOptionValues(MACHINE_SIGNATURE_ARG);
            verifyNoMoreInteractions(registrar, configurator, args);
            verifyNoInteractions(context);
        }

        @Test
        @DisplayName("passes null machine signature when no arg exists")
        void passesNullMachineSignatureWhenNoArgExists() {
            // arrange
            // conditions
            when(args.getOptionValues(MACHINE_SIGNATURE_ARG)).thenReturn(null);
            // act
            int actual = runner.execute(args);
            // assert
            assertThat(actual).isZero();
            // verify
            InOrder order = inOrder(registrar, configurator);
            order.verify(registrar).run(null);
            order.verify(configurator).run();
            verifyNoInteractions(context);
        }

        @Test
        @DisplayName("returns one when registrar fails and does not run configurator")
        void returnsOneWhenRegistrarFailsAndDoesNotRunConfigurator() {
            // arrange
            // conditions
            when(args.getOptionValues(MACHINE_SIGNATURE_ARG)).thenReturn(List.of("machine-abc"));
            doThrow(new IllegalStateException("database unavailable"))
                    .when(registrar)
                    .run("machine-abc");
            // act
            int actual = runner.execute(args);
            // assert
            assertThat(actual).isOne();
            // verify
            verify(registrar).run("machine-abc");
            verify(configurator, never()).run();
            verifyNoInteractions(context);
        }

        @Test
        @DisplayName("returns one when configurator fails after registrar succeeds")
        void returnsOneWhenConfiguratorFailsAfterRegistrarSucceeds() {
            // arrange
            // conditions
            when(args.getOptionValues(MACHINE_SIGNATURE_ARG)).thenReturn(List.of("machine-abc"));
            doThrow(new IllegalStateException("missing encryption key"))
                    .when(configurator)
                    .run();
            // act
            int actual = runner.execute(args);
            // assert
            assertThat(actual).isOne();
            // verify
            InOrder order = inOrder(registrar, configurator);
            order.verify(registrar).run("machine-abc");
            order.verify(configurator).run();
            verifyNoInteractions(context);
        }

    }

}
