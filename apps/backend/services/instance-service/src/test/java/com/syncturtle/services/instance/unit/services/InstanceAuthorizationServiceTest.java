package com.syncturtle.services.instance.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.services.instance.services.authz.InstanceAuthorizationService;

@ExtendWith(MockitoExtension.class)
public class InstanceAuthorizationServiceTest {

    @Mock
    InstanceAdminRepository instanceAdminRepository;

    @InjectMocks
    InstanceAuthorizationService service;

    @Test
    void isInstanceAdmin_whenNullUserId_returnsFalse() {
        // arrange
        // conditions
        // act
        boolean result = service.isInstanceAdmin(null, 15);
        // assertions
        assertThat(result).isFalse();
        // verify
        verifyNoInteractions(instanceAdminRepository);
    }

    @Test
    void isInstanceAdmin_whenRepoSaysYes_returnsTrue() {
        // arrange
        UUID userId = UUID.randomUUID();
        // conditions
        when(instanceAdminRepository.existsByUserIdAndRoleGreaterThanEqual(userId, 15)).thenReturn(true);
        // act
        boolean result = service.isInstanceAdmin(userId, 15);
        // assertions
        assertThat(result).isTrue();
        // verify
        verify(instanceAdminRepository).existsByUserIdAndRoleGreaterThanEqual(userId, 15);
    }

    @Test
    void isInstanceAdmin_whenUserId_butLowRole_returnsFalse() {
        // arrange
        UUID userId = UUID.randomUUID();
        // conditions
        when(instanceAdminRepository.existsByUserIdAndRoleGreaterThanEqual(userId, 0)).thenReturn(false);
        // act
        boolean result = service.isInstanceAdmin(userId, 0);
        // assertions
        assertThat(result).isFalse();
        // verify
        verify(instanceAdminRepository).existsByUserIdAndRoleGreaterThanEqual(userId, 0);
    }

}
