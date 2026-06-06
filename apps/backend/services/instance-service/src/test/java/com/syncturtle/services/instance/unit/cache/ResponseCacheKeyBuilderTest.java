package com.syncturtle.services.instance.unit.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.syncturtle.common.cache.response.ResponseCacheKeyBuilder;

@ExtendWith(MockitoExtension.class)
class ResponseCacheKeyBuilderTest {

    private final ResponseCacheKeyBuilder cacheKeyBuilder = new ResponseCacheKeyBuilder();

    @Test
    void canonicalVariantInput_shouldSortQueryParamsAndValues() {
        // arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/instances");
        request.addParameter("b", "2");
        request.addParameter("a", "2");
        request.addParameter("a", "1");
        // conditions
        // act
        String canonical = cacheKeyBuilder.canonicalVariantInput(request, null);
        // assertions
        assertThat(canonical).isEqualTo("GET /api/instances?a=1&a=2&b=2");
        // verify
    }

    @Test
    void variantHashHex_shouldReturnStableHex() {
        // arrange
        // conditions
        // act
        String h1 = cacheKeyBuilder.variantHashHex("abc", 8);
        String h2 = cacheKeyBuilder.variantHashHex("abc", 8);
        // assertions
        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(16);
        // verify
    }

}
