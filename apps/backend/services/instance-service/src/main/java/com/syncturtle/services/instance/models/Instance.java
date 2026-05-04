package com.syncturtle.services.instance.models;

import java.time.Instant;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.instance.models.embedded.ConfigInfo;
import com.syncturtle.services.instance.models.embedded.RuntimeInfo;
import com.syncturtle.services.instance.models.embedded.UpdateCheckInfo;
import com.syncturtle.services.instance.payload.RegistrationSpec;
import com.syncturtle.services.instance.payload.UpdateCheckMetadata;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "instances")
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
    private long version;

    @Embedded
    private UpdateCheckInfo updateCheck = UpdateCheckInfo.empty();

    @Embedded
    private ConfigInfo config = ConfigInfo.empty();

    @Embedded
    private RuntimeInfo runtime = RuntimeInfo.empty();

    public void initializeForRegistration(RegistrationSpec spec, Instant now) {
        setInstanceName(spec.getInstanceName());
        setEdition(spec.getEdition());
        setInstanceId(spec.getInstanceId());

        setTelemetryEnabled(spec.isTelemetryEnabled());
        setSupportRequired(spec.isSupportRequired());
        setTest(spec.isTest());
        setSetupDone(false);
        setVerified(false);

        this.runtime.apply(spec.getRuntime());

        // initial "latest == current"
        this.updateCheck.apply(new UpdateCheckMetadata(spec.getBinary().getBinaryVersion(), now));

        this.config.initializeIfMissing(now);
    }

    public void updateInstance(RegistrationSpec spec, Instant now) {
        // update check fields
        this.updateCheck.apply(new UpdateCheckMetadata(spec.getBinary().getBinaryVersion(), now));
        // flags can change via config/env
        this.telemetryEnabled = spec.isTelemetryEnabled();
        this.supportRequired = spec.isSupportRequired();
        this.test = spec.isTest();
        // Runtime can change (new node/pod)
        this.runtime.apply(spec.getRuntime());
    }

    public void markSetupDone() {
        this.setupDone = true;
    }
}
