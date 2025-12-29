package com.syncturtle.platform.services.instance.models.embedded;

import com.syncturtle.platform.services.instance.payload.RuntimeMetadata;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuntimeInfo {

    @Column(name = "domain")
    private String domain;

    @Column(name = "namespace", length = 255)
    private String namespace;

    @Column(name = "vm_host")
    private String vmHost;

    public void apply(RuntimeMetadata rm) {
        this.domain = rm.getDomain();
        this.namespace = rm.getNamespace();
        this.vmHost = rm.getVmHost();
    }

    public static RuntimeInfo empty() {
        RuntimeInfo ri = new RuntimeInfo();
        return ri;
    }
}
