package com.syncturtle.services.instance.bootstrap;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.common.contracts.instance.model.InstanceEditions;
import com.syncturtle.common.core.text.Strings;
import com.syncturtle.services.instance.configuration.property.InstanceServiceProperties;
import com.syncturtle.services.instance.event.InstanceEventFactory;
import com.syncturtle.services.instance.event.InstanceRegisteredEvent;
import com.syncturtle.services.instance.messaging.db.event.InstanceEventToPublish;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationFlagsParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.model.param.InstanceRuntimeParam;
import com.syncturtle.services.instance.repository.InstanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("setup")
@RequiredArgsConstructor
public class InstanceRegistrar {

    private final InstanceRepository instanceRepository;
    private final InstanceServiceProperties properties;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final InstanceEventFactory eventFactory;
    private final Clock clock;

    @Transactional
    public void run(String requestedMachineSignature) {
        Instance existing = instanceRepository
                .findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        String effectiveSignature = resolveEffectiveMachineSignature(existing, requestedMachineSignature);

        InstanceRegistrationParam param = buildRegistrationParam(effectiveSignature);

        if (existing == null) {
            registerNewInstance(param);
            return;
        }

        refreshExistingInstance(existing, param);
    }

    private void registerNewInstance(InstanceRegistrationParam param) {
        Instance instance = Instance.register(param);

        instance = instanceRepository.saveAndFlush(instance);

        publishRegistered(instance);
        publishCreated(instance);

        log.info(
                "Instance registered: id={} instanceId={} edition={} binaryVersion={}",
                instance.getId(),
                instance.getInstanceId(),
                instance.getEdition(),
                param.getBinary().getBinaryVersion());
    }

    private void refreshExistingInstance(Instance instance, InstanceRegistrationParam param) {
        instance.refreshRegistration(param);

        instance = instanceRepository.saveAndFlush(instance);

        publishRegistered(instance);
        publishUpdated(instance);

        log.info(
                "Instance refreshed: id={} instanceId={} edition={} binaryVersion={}",
                instance.getId(),
                instance.getInstanceId(),
                instance.getEdition(),
                param.getBinary().getBinaryVersion());
    }

    private InstanceRegistrationParam buildRegistrationParam(String machineSignature) {
        Instant now = Instant.now(clock);

        InstanceBinaryParam binary = InstanceBinaryParam.from(buildPropertiesProvider.getIfAvailable(), now);

        InstanceEdition edition = InstanceEditions.parseEdition(properties.getEdition());

        InstanceRegistrationFlagsParam flags = InstanceRegistrationFlagsParam.builder()
                .telemetryEnabled(properties.getTelemetry().isEnabled())
                .supportRequired(properties.getTelemetry().isSupportRequired())
                .test(properties.isTest())
                .build();

        InstanceRuntimeParam runtime = InstanceRuntimeParam.builder()
                .domain(resolveDomain())
                .namespace(resolveNamespace())
                .vmHost(resolveVmHost())
                .build();

        return InstanceRegistrationParam.builder()
                .instanceId(machineSignature)
                .instanceName(InstanceEditions.defaultInstanceName(edition))
                .edition(edition)
                .flags(flags)
                .runtime(runtime)
                .binary(binary)
                .registeredAt(now)
                .build();
    }

    private void publishCreated(Instance instance) {
        events.publishEvent(new InstanceEventToPublish(eventFactory.created(instance)));
    }

    private void publishUpdated(Instance instance) {
        events.publishEvent(new InstanceEventToPublish(eventFactory.updated(instance)));
    }

    private void publishRegistered(Instance instance) {
        events.publishEvent(new InstanceRegisteredEvent(instance.getId()));
    }

    private static String resolveDomain() {
        return Strings.firstNonBlank(
                System.getenv("SYNCTURTLE_DOMAIN"),
                System.getenv("PUBLIC_DOMAIN"),
                System.getenv("APP_DOMAIN")).orElse("localhost");
    }

    private static String resolveVmHost() {
        return Strings.firstNonBlank(
                System.getenv("VM_HOST"),
                System.getenv("HOSTNAME")).orElse(null);
    }

    private static String resolveNamespace() {
        return Strings.firstNonBlank(
                System.getenv("KUBERNETES_NAMESPACE"),
                System.getenv("POD_NAMESPACE")).orElse(null);
    }

    private static String resolveEffectiveMachineSignature(Instance existing, String requested) {
        if (existing != null && StringUtils.hasText(existing.getInstanceId())) {
            return existing.getInstanceId();
        }
        if (StringUtils.hasText(requested)) {
            return requested.trim();
        }
        // first time only; generate once and persist
        return UUID.randomUUID().toString().replace("-", "");
    }

}
