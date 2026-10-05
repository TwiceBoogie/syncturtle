package com.syncturtle.common.kafka.topic;

/** Shared dead-letter topic naming contract for publishers and topic owners. */
public final class KafkaDeadLetterTopics {

    public static final String SUFFIX = ".DLT";

    private KafkaDeadLetterTopics() {
    }
}
