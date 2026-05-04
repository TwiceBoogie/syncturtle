package com.syncturtle.services.user.models.support;

import java.time.ZoneId;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    private static final Set<String> VALID = ZoneId.getAvailableZoneIds();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return VALID.contains(value);
    }

}
