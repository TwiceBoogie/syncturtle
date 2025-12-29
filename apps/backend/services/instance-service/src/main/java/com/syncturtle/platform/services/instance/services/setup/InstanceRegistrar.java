package com.syncturtle.platform.services.instance.services.setup;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.syncturtle.common.core.enums.InstanceEdition;
import com.syncturtle.common.core.events.InstanceEvent;
import com.syncturtle.common.core.events.InstanceEvent.Type;
import com.syncturtle.common.core.utils.StringHelper;
import com.syncturtle.platform.services.instance.configurations.properties.InstanceServiceProperties;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.payload.BinaryMetadata;
import com.syncturtle.platform.services.instance.payload.InstanceEventToPublish;
import com.syncturtle.platform.services.instance.payload.RegistrationSpec;
import com.syncturtle.platform.services.instance.payload.RuntimeMetadata;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("setup")
@RequiredArgsConstructor
public class InstanceRegistrar {

    private final InstanceRepository repo;
    private final InstanceServiceProperties props;
    private final ApplicationEventPublisher events;

    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Transactional
    public void run(String requestedMachineSignature) {
        Instance instance = repo.findFirstByDeletedAtIsNullOrderByCreatedAtAsc().orElse(null);

        String effectiveSignature = resolveEffectiveMachineSignature(instance, requestedMachineSignature);

        Instant now = Instant.now();
        BinaryMetadata meta = BinaryMetadata.from(buildPropertiesProvider.getIfAvailable(), now);

        boolean telemetryEnabled = props.getTelemetry().isEnabled();
        boolean supportRequired = props.getTelemetry().isSupportRequired();
        boolean isTest = props.isTest();

        InstanceEdition edition = StringHelper.parseEdition(props.getEdition());
        String instanceName = StringHelper.defaultInstanceName(edition);

        String vmHost = StringHelper.firstNonBlank(
                System.getenv("VM_HOST"),
                System.getenv("HOSTNAME")).orElse(null);

        String namespace = StringHelper.firstNonBlank(
                System.getenv("KUBERNETES_NAMESPACE"),
                System.getenv("POD_NAMESPACE")).orElse(null);

        RuntimeMetadata runtimeMetadata = new RuntimeMetadata("", namespace, vmHost);
        RegistrationSpec registrationSpec = new RegistrationSpec(effectiveSignature, instanceName, edition,
                telemetryEnabled, supportRequired, isTest, runtimeMetadata, meta);
        if (instance == null) {
            instance = new Instance();

            instance.initializeForRegistration(registrationSpec, now);
            instance = repo.saveAndFlush(instance);

            InstanceEvent evt = InstanceEvent.builder()
                    .type(Type.INSTANCE_CREATED)
                    .id(instance.getId())
                    .edition(edition)
                    .version(instance.getVersion())
                    .machineSignature(instance.getInstanceId())
                    .vmHost(vmHost)
                    .occurredAt(now)
                    .test(isTest)
                    .build();

            events.publishEvent(new InstanceEventToPublish(evt));

            log.info("Instance registered: id={} instanceId={} edition={} version={}", instance.getId(),
                    instance.getInstanceId(), edition, meta.getBinaryVersion());
        } else {
            // update the binary + health check fields
            instance.updateInstance(registrationSpec, now);
            instance = repo.save(instance);

            InstanceEvent evt = InstanceEvent.builder()
                    .type(Type.INSTANCE_UPDATED)
                    .id(instance.getId())
                    .edition(edition)
                    .version(instance.getVersion())
                    .machineSignature(instance.getInstanceId())
                    .vmHost(vmHost)
                    .occurredAt(now)
                    .test(isTest)
                    .build();

            events.publishEvent(new InstanceEventToPublish(evt));
        }
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
