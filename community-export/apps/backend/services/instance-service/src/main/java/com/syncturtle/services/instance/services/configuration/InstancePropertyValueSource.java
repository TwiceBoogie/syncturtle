package com.syncturtle.services.instance.services.configuration;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

public interface InstancePropertyValueSource {
    boolean skipEnvVar();

    /**
     * Return the "raw" value from app.* configuration or null/blank if not
     * configured.
     * 
     * @param key Valid instance configuration key used by this service
     * @return the "raw" value or null/blank
     */
    String getRaw(InstanceConfigurationKey key);

    /**
     * Mandatory check
     * 
     * @return secret key
     */
    String secretKey();
}
