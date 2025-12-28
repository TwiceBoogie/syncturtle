package com.syncturtle.platform.services.instance.services.setup;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile("setup")
@RequiredArgsConstructor
public final class SetupRunner implements ApplicationRunner {

    private final InstanceRegistrar registrar;
    private final InstanceConfigurator configurator;
    private final ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sig = resolveSignatureFromArgsOrEnv(args);
        registrar.run(sig);
        configurator.run();

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private static String resolveSignatureFromArgsOrEnv(ApplicationArguments args) {
        String fromArg = first(args.getOptionValues("machine-signature"));
        if (fromArg != null && !fromArg.isBlank()) {
            return fromArg.trim();
        }
        String fromEnv = System.getenv("MACHINE_SIGNATURE");
        return (fromEnv != null && !fromEnv.isBlank()) ? fromEnv.trim() : null;
    }

    private static String first(List<String> v) {
        return (v == null || v.isEmpty()) ? null : v.get(0);
    }
}
