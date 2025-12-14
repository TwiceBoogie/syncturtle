package com.syncturtle.testing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base class for tests that need Spring context.
 * Add shared Testcontainers wiring here
 */
@ExtendWith(SpringExtension.class)
public abstract class TestcontainersBase {
    
}
