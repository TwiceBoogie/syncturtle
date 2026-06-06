package com.syncturtle.services.instance.model;

import java.time.Clock;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.instance.model.embedded.ConfigInfoEmbed;
import com.syncturtle.services.instance.model.embedded.RuntimeInfoEmbed;
import com.syncturtle.services.instance.model.embedded.UpdateCheckInfoEmbed;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.model.param.InstanceSetupCompletionParam;
import com.syncturtle.services.instance.model.param.UpdateCheckParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "instances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instance extends AuditedEntity {

    @Column(name = "instance_name", length = 100)
    private String instanceName;

    @Column(name = "whitelist_emails")
    private String whitelistEmails;

    @Column(name = "instance_id", nullable = false, length = 255, unique = true)
    private String instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "edition", nullable = false, length = 50)
    private InstanceEdition edition;

    @Column(name = "is_telemetry_enabled", nullable = false)
    private boolean telemetryEnabled;

    @Column(name = "is_support_required", nullable = false)
    private boolean supportRequired;

    @Column(name = "is_setup_done", nullable = false)
    private boolean setupDone;

    @Column(name = "is_signup_screen_visited", nullable = false)
    private boolean signupScreenVisited;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "is_test", nullable = false)
    private boolean test;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Embedded
    private UpdateCheckInfoEmbed updateCheck = UpdateCheckInfoEmbed.empty();

    @Embedded
    private ConfigInfoEmbed config = ConfigInfoEmbed.empty();

    @Embedded
    private RuntimeInfoEmbed runtime = RuntimeInfoEmbed.empty();

    public static Instance register(InstanceRegistrationParam param) {
        Assert.notNull(param, "registration is required");

        Instance instance = new Instance();
        instance.initializeForRegistration(param);
        return instance;
    }

    public void refreshRegistration(InstanceRegistrationParam param) {
        requireActive("Instance");
        Assert.notNull(param, "setup completion param is required");

        updateCheck.apply(UpdateCheckParam.initial(param.getBinary().getBinaryVersion(), param.getRegisteredAt()));

        telemetryEnabled = param.getFlags().isTelemetryEnabled();
        supportRequired = param.getFlags().isSupportRequired();
        test = param.getFlags().isTest();

        runtime.apply(param.getRuntime());
    }

    public void completeSetup(InstanceSetupCompletionParam param) {
        requireActive("Instance");
        Assert.notNull(param, "setup completion param is required");

        rename(param.getCompanyName());

        if (param.isTelemetryEnabled()) {
            enableTelemetry();
        } else {
            disableTelemetry();
        }

        setupDone = true;

    }

    public void rename(String instanceName) {
        requireActive("Instance");
        Assert.hasText(instanceName, "instanceName is required");

        this.instanceName = instanceName.trim();
    }

    public void enableTelemetry() {
        requireActive("Instance");

        telemetryEnabled = true;
    }

    public void disableTelemetry() {
        requireActive("Instance");

        telemetryEnabled = false;
    }

    public void markSignupScreenVisited() {
        requireActive("Instance");

        if (signupScreenVisited) {
            return;
        }

        signupScreenVisited = true;
    }

    public void verify() {
        requireActive("Instance");

        verified = true;
    }

    public void unverify() {
        requireActive("Instance");

        verified = false;
    }

    public void updateWhitelistEmails(String whitelistEmails) {
        requireActive("Instance");

        this.whitelistEmails = normalizeNullable(whitelistEmails);
    }

    public void bumpConfigVersion(Clock clock) {
        requireActive("Instance");

        config.bumpVersion(clock);
    }

    private void initializeForRegistration(InstanceRegistrationParam param) {
        instanceId = param.getInstanceId().trim();
        instanceName = param.getInstanceName();
        edition = param.getEdition();

        telemetryEnabled = param.getFlags().isTelemetryEnabled();
        supportRequired = param.getFlags().isSupportRequired();
        test = param.getFlags().isTest();

        setupDone = false;
        signupScreenVisited = false;
        verified = false;

        runtime.apply(param.getRuntime());

        updateCheck.apply(UpdateCheckParam.initial(param.getBinary().getBinaryVersion(), param.getRegisteredAt()));
        config.initialize(param.getRegisteredAt());
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
