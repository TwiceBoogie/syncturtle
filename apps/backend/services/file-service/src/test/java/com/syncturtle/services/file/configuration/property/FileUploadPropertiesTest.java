package com.syncturtle.services.file.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import com.syncturtle.services.file.policy.FileLifecyclePolicy;

class FileUploadPropertiesTest {

    @Test
    void keepsUploadCapacityConfigurableWhileBoundingProcessingLeaseToReplayHorizon() {
        FileUploadProperties properties = new FileUploadProperties(
                DataSize.ofMegabytes(8), Duration.ofMinutes(2));

        assertThat(properties.getMaxImageSize()).isEqualTo(DataSize.ofMegabytes(8));
        assertThatThrownBy(() -> new FileUploadProperties(
                DataSize.ofMegabytes(101), Duration.ofMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileUploadProperties(
                DataSize.ofMegabytes(8), FileLifecyclePolicy.UPLOAD_IDEMPOTENCY_RETENTION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
