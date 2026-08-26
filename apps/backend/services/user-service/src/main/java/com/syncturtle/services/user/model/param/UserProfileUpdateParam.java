package com.syncturtle.services.user.model.param;

import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

@Getter
public final class UserProfileUpdateParam {

    private static final int MAX_ROLE_LENGTH = 300;
    private static final int MAX_USE_CASE_LENGTH = 2048;
    private static final int MAX_BILLING_ADDRESS_COUNTRY_LENGTH = 255;
    private static final int MAX_LANGUAGE_LENGTH = 255;
    // private static final int MAX_COMPANY_NAME_LENGTH = 48;

    private static final Set<String> ALLOWED_ONBOARDING_STEP_KEYS = Set.of(
            "profileComplete",
            "workspaceJoin",
            "workspaceCreate",
            "workspaceInvite");

    private final String role;
    private final UUID lastWorkspaceId;
    private final JsonNode theme;
    private final JsonNode onboardingStep;
    private final String useCase;
    private final String billingAddressCountry;
    private final JsonNode billingAddress;
    private final Boolean hasBillingAddress;
    private final String language;
    private final Boolean hasMarketingEmailConsent;

    @Builder
    private UserProfileUpdateParam(
            String role,
            UUID lastWorkspaceId,
            JsonNode theme,
            JsonNode onboardingStep,
            String useCase,
            String billingAddressCountry,
            JsonNode billingAddress,
            Boolean hasBillingAddress,
            String language,
            Boolean hasMarketingEmailConsent) {
        Assert.isTrue(
                hasAnyUpdate(role, lastWorkspaceId, theme, onboardingStep, useCase, billingAddressCountry,
                        billingAddress, hasBillingAddress, language, hasMarketingEmailConsent),
                "at least one profile update field is required");

        this.role = normalizeNullable(role, "role", MAX_ROLE_LENGTH);
        this.lastWorkspaceId = lastWorkspaceId;
        this.theme = normalizeJsonObject(theme, "theme");
        this.onboardingStep = normalizeOnboardingStep(onboardingStep);
        this.useCase = normalizeNullable(useCase, "useCase", MAX_USE_CASE_LENGTH);
        this.billingAddressCountry = normalizeNullable(billingAddressCountry, "billingAddressCountry",
                MAX_BILLING_ADDRESS_COUNTRY_LENGTH);
        this.billingAddress = normalizeJsonObject(billingAddress, "billingAddress");
        this.hasBillingAddress = hasBillingAddress;
        // this.companyName = normalizeNullable(companyName, "companyName",
        // MAX_COMPANY_NAME_LENGTH);
        this.language = normalizeNullable(language, "language", MAX_LANGUAGE_LENGTH);
        this.hasMarketingEmailConsent = hasMarketingEmailConsent;
    }

    private static boolean hasAnyUpdate(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return true;
            }
        }

        return false;
    }

    private static JsonNode normalizeOnboardingStep(JsonNode value) {
        ObjectNode normalized = normalizeJsonObject(value, "onboardingStep");
        if (normalized == null) {
            return null;
        }

        for (Entry<String, JsonNode> property : normalized.properties()) {
            String propertyName = property.getKey();
            JsonNode propertyValue = property.getValue();

            Assert.isTrue(ALLOWED_ONBOARDING_STEP_KEYS.contains(propertyName),
                    "onboardingStep contains unsupported key: " + propertyName);

            Assert.isTrue(propertyValue.isBoolean(), "onboardingStep." + propertyName + " must be boolean");
        }

        return normalized;
    }

    private static ObjectNode normalizeJsonObject(JsonNode value, String fieldName) {
        if (value == null) {
            return null;
        }

        Assert.isTrue(value.isObject(), fieldName + " must be a JSON object");

        return (ObjectNode) value.deepCopy();
    }

    private static String normalizeNullable(String value, String fieldName, int maxLength) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
