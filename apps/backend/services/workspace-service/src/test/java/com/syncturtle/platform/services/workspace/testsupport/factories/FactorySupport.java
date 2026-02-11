package com.syncturtle.platform.services.workspace.testsupport.factories;

import java.util.function.Consumer;

public final class FactorySupport {

    private FactorySupport() {
        throw new AssertionError("No instance");
    }

    public static <T> T with(T value, Consumer<T> block) {
        block.accept(value);
        return value;
    }

}
