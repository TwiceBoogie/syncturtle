package com.syncturtle.services.instance.support.assertion;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.AbstractAssert;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent.Type;

public final class PublishedInstanceEventAssert extends AbstractAssert<PublishedInstanceEventAssert, InstanceEvent> {

    private PublishedInstanceEventAssert(InstanceEvent actual) {
        super(actual, PublishedInstanceEventAssert.class);
    }

    public static PublishedInstanceEventAssert assertThatPublishedEvent(InstanceEvent actual) {
        return new PublishedInstanceEventAssert(actual);
    }

    public PublishedInstanceEventAssert isInstanceUpdated() {
        isNotNull();
        Type type = actual.getType();
        if (type != Type.INSTANCE_UPDATED) {
            failWithMessage("Expected event type <%s> but was <%s>", Type.INSTANCE_UPDATED, type);
        }
        return this;
    }

    public PublishedInstanceEventAssert hasInstanceId(UUID expectedId) {
        isNotNull();
        UUID actualId = actual.getId();
        if (!expectedId.equals(actualId)) {
            failWithMessage("Expected instance id <%s> but was <%s>", expectedId, actualId);
        }
        return this;
    }

    public PublishedInstanceEventAssert occurredAt(Instant expected) {
        isNotNull();
        Instant occurredAt = actual.getOccurredAt();
        if (!expected.equals(occurredAt)) {
            failWithMessage("Expected occurredAt <%s> but was <%s>", expected, occurredAt);
        }
        return this;
    }

    public PublishedInstanceEventAssert hasSetupDone(boolean expected) {
        isNotNull();
        boolean setupDone = actual.isSetupDone();
        if (!Boolean.valueOf(expected).equals(setupDone)) {
            failWithMessage("Expected setupDone <%s> but was <%s>", expected, setupDone);
        }
        return this;
    }

}
