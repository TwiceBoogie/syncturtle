package com.syncturtle.services.user.services;

import com.syncturtle.services.user.configurations.properties.KeyMaterial;
import com.syncturtle.services.user.dto.command.PassportTokenCommand;
import com.syncturtle.services.user.payload.IssuedPassport;

public interface PassportService {
    IssuedPassport issueAccessToken(PassportTokenCommand request);

    String currentKid();

    String issuer();

    String audience();

    KeyMaterial keyMaterial();
}
