package com.syncturtle.services.instance.bootstrap;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("setup")
@RequiredArgsConstructor
public final class SetupRunner implements ApplicationRunner {

    private static final String MACHINE_SIGNATURE_ARG = "machine-signature";
    private static final String MACHINE_SIGNATURE_ENV = "MACHINE_SIGNATURE";

    private final InstanceRegistrar registrar;
    private final InstanceConfigurator configurator;
    private final ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = execute(args);
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    int execute(ApplicationArguments args) {
        try {
            String machineSignature = resolveMachineSignature(args);

            registrar.run(machineSignature);
            configurator.run();

            log.info("Instance setup completed successfully");

            return 0;
        } catch (Exception exception) {
            log.error("Instance setup failed: {}", exception.getMessage(), exception);

            return 1;
        }
    }

    private static String resolveMachineSignature(ApplicationArguments args) {
        String fromArg = first(args.getOptionValues(MACHINE_SIGNATURE_ARG));

        if (StringUtils.hasText(fromArg)) {
            return fromArg.trim();
        }

        String fromEnv = System.getenv(MACHINE_SIGNATURE_ENV);

        if (StringUtils.hasText(fromEnv)) {
            return fromEnv.trim();
        }

        return null;
    }

    private static String first(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }
}
