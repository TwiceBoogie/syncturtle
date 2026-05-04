package com.syncturtle.services.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class EmailCheckResponse {
    private String status;
    private boolean existing;
    @JsonProperty("isPasswordExisting")
    private boolean passwordExisting;
}
