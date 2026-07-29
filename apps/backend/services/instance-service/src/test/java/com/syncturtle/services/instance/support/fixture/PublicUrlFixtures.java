package com.syncturtle.services.instance.support.fixture;

import com.syncturtle.common.web.property.PublicUrlProperties;

public final class PublicUrlFixtures {

    private PublicUrlFixtures() {
    }

    public static PublicUrlProperties publicUrls() {
        return new PublicUrlProperties(
                new PublicUrlProperties.Api("http://localhost:8000", ""),
                new PublicUrlProperties.Web("http://localhost:3000", "/"),
                new PublicUrlProperties.Admin("http://localhost:3001", "/god-mode/"),
                "https://syncturtle.com");
    }

}
