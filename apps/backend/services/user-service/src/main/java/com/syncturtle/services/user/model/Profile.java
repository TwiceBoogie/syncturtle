package com.syncturtle.services.user.model;

import java.time.Clock;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.user.model.support.JsonDefaults;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends AuditedEntity {

    private static final int MAX_COMPANY_NAME_LENGTH = 48;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme", nullable = false)
    private JsonNode theme;

    @Column(name = "is_tour_completed", nullable = false)
    private boolean tourCompleted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "onboarding_step", nullable = false)
    private JsonNode onboardingStep;

    @Column(name = "use_case", columnDefinition = "text")
    private String useCase;

    @Column(name = "role")
    private String role;

    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded;

    @Column(name = "last_workspace_id")
    private UUID lastWorkspaceId;

    @Column(name = "billing_address_country", nullable = false)
    private String billingAddressCountry;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address")
    private JsonNode billingAddress;

    @Getter(AccessLevel.NONE)
    @Column(name = "has_billing_address", nullable = false)
    private boolean hasBillingAddress;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "has_marketing_email_consent", nullable = false)
    private boolean marketingEmailConsent;

    public boolean hasBillingAddress() {
        return hasBillingAddress;
    }

    public static Profile create(User user, Clock clock, String companyName) {
        Assert.notNull(user, "user is required");
        Assert.notNull(clock, "clock is required");

        Profile profile = new Profile();
        profile.theme = JsonDefaults.emptyObject();
        profile.tourCompleted = false;
        profile.onboardingStep = JsonDefaults.profileOnboarding();
        profile.onboarded = false;
        profile.billingAddressCountry = "";
        profile.billingAddress = JsonDefaults.emptyObject();
        profile.hasBillingAddress = false;
        profile.companyName = normalizeNullable(companyName, "companyName", MAX_COMPANY_NAME_LENGTH);
        profile.user = user;
        profile.language = "en";
        profile.marketingEmailConsent = false;
        return profile;
    }

    private static String normalizeNullable(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }
}
