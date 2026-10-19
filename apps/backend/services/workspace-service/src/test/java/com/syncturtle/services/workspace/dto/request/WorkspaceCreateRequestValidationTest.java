package com.syncturtle.services.workspace.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class WorkspaceCreateRequestValidationTest {

    @Test
    void organizationIsOptionalButSlugAndLegacyLengthAreValidated() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            WorkspaceCreateRequest solo = WorkspaceCreateRequest.builder().name("Personal").slug("personal-1234")
                    .build();
            assertThat(validator.validate(solo)).isEmpty();
            WorkspaceCreateRequest invalidSlug = WorkspaceCreateRequest.builder().name("Personal").slug("INVALID_SLUG")
                    .build();
            assertThat(validator.validate(invalidSlug)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("slug");
            WorkspaceCreateRequest oversizedLegacy = WorkspaceCreateRequest.builder().name("Personal")
                    .slug("personal-1234").organizationSize("x".repeat(21)).build();
            assertThat(validator.validate(oversizedLegacy)).extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("organizationSize");
        }
    }

}
