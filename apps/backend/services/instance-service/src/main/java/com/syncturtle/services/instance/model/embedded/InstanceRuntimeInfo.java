package com.syncturtle.services.instance.model.embedded;

import org.springframework.util.Assert;

import com.syncturtle.services.instance.model.param.InstanceRuntimeParam;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceRuntimeInfo {

    @Column(name = "domain")
    private String domain;

    @Column(name = "namespace", length = 255)
    private String namespace;

    @Column(name = "vm_host")
    private String vmHost;

    public void apply(InstanceRuntimeParam param) {
        Assert.notNull(param, "runtime param is required");

        domain = param.getDomain();
        namespace = param.getNamespace();
        vmHost = param.getVmHost();
    }

    public static InstanceRuntimeInfo empty() {
        return new InstanceRuntimeInfo();
    }

}
