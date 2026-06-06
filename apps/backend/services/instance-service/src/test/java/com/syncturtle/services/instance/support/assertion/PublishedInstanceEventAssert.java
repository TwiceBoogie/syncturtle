package com.syncturtle.services.instance.support.assertion;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.AbstractAssert;
import org.springframework.test.util.ReflectionTestUtils;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.instance.event.InstanceEvent.Type;
import com.syncturtle.services.instance.messaging.db.event.InstanceEventToPublish;

public final class PublishedInstanceEventAssert
        extends AbstractAssert<PublishedInstanceEventAssert, InstanceEventToPublish> {

    private PublishedInstanceEventAssert(InstanceEventToPublish actual) {
        super(actual, PublishedInstanceEventAssert.class);
    }

    public static PublishedInstanceEventAssert assertThatPublishedEvent(InstanceEventToPublish actual) {
        return new PublishedInstanceEventAssert(actual);
    }

    public PublishedInstanceEventAssert isInstanceUpdated() {
        isNotNull();
        Object event = event();
        Object type = ReflectionTestUtils.getField(event, "type");
        if (type != Type.INSTANCE_UPDATED) {
            failWithMessage("Expected event type <%s> but was <%s>", Type.INSTANCE_UPDATED, type);
        }
        return this;
    }

    public PublishedInstanceEventAssert hasInstanceId(UUID expectedId) {
        isNotNull();
        Object actualId = ReflectionTestUtils.getField(event(), "id");
        if (!expectedId.equals(actualId)) {
            failWithMessage("Expected instance id <%s> but was <%s>", expectedId, actualId);
        }
        return this;
    }

    public PublishedInstanceEventAssert occurredAt(Instant expected) {
        isNotNull();
        Object occurredAt = ReflectionTestUtils.getField(event(), "occurredAt");
        if (!expected.equals(occurredAt)) {
            failWithMessage("Expected occurredAt <%s> but was <%s>", expected, occurredAt);
        }
        return this;
    }

    public PublishedInstanceEventAssert hasSetupDone(boolean expected) {
        isNotNull();
        Object setupDone = ReflectionTestUtils.getField(event(), "setupDone");
        if (!Boolean.valueOf(expected).equals(setupDone)) {
            failWithMessage("Expected setupDone <%s> but was <%s>", expected, setupDone);
        }
        return this;
    }

    private InstanceEvent event() {
        Object event = ReflectionTestUtils.getField(actual, "event");
        if (!(event instanceof InstanceEvent)) {
            failWithMessage("Expected wrapper to contain InstanceEvent in field 'event' but was <%s>", event);
        }
        return (InstanceEvent) event;
    }

}
